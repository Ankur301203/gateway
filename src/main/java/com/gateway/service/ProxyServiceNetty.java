package com.gateway.service;

import com.gateway.config.AppConfig;
import com.gateway.domain.Route;
import com.gateway.domain.RouteTarget;
import com.gateway.repository.RouteRepository;
import com.gateway.repository.TargetRepository;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProxyServiceNetty {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServiceNetty.class);

    private static final EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();
    private static final int MAX_CONTENT_LENGTH = AppConfig.getInt(
            "proxy.max.content.length", 10 * 1024 * 1024
    );
    private static final int CONNECT_TIMEOUT_MS = AppConfig.getInt(
            "proxy.connect.timeout.ms", 5000
    );

    private static final SslContext SSL_CONTEXT = buildSslContext();

    private final RouteRepository routeRepo = new RouteRepository();
    private final TargetRepository targetRepo = new TargetRepository();
    private final LoadBalancerService loadBalancer = LoadBalancerService.getInstance();
    private final LogService logService = LogService.getInstance();

    public void forwardAsync(String gatewayId, String path, String method,
                             Map<String, String> headers, String body,
                             ProxyCallback callback) {
        long startTime = System.currentTimeMillis();
        UUID gatewayUuid = UUID.fromString(gatewayId);

        try {
            Optional<Route> routeOpt = routeRepo.findByGatewayAndPathMatchAndMethod(
                    gatewayUuid, path, method
            );

            if (routeOpt.isEmpty()) {
                logger.warn("No route found for gateway={}, path={}, method={}",
                        gatewayId, path, method);
                callback.onSuccess(new ProxyResult(404, "{\"error\": \"Route not found\"}", Map.of()));
                return;
            }

            Route route = routeOpt.get();

            List<RouteTarget> targets = targetRepo.findHealthyByRouteId(route.getId());
            if (targets.isEmpty()) {
                logger.warn("No healthy targets for route={}", route.getId());
                logService.logAsync(gatewayUuid, route.getId(), null,
                        method, path, 503, 0, "No healthy targets");
                callback.onSuccess(new ProxyResult(503,
                        "{\"error\": \"Service unavailable - no healthy backends\"}", Map.of()));
                return;
            }

            RouteTarget target = loadBalancer.selectTarget(route.getId(), targets);
            String targetUrl = target.getTargetUrl() + path;

            URI uri = URI.create(targetUrl);
            boolean useSsl = "https".equalsIgnoreCase(uri.getScheme());
            String host = uri.getHost();
            int port = uri.getPort();
            if (port == -1) {
                port = useSsl ? 443 : 80;
            }

            if (host == null) {
                throw new IllegalArgumentException("Invalid target URL: " + targetUrl);
            }

            String requestPath = buildRequestPath(uri);
            int timeoutSeconds = Math.max(1,
                    (int) Math.ceil(route.getTimeoutMs() / 1000.0));

            int finalPort1 = port;
            Bootstrap bootstrap = new Bootstrap()
                    .group(CLIENT_GROUP)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (useSsl) {
                                p.addLast("ssl", SSL_CONTEXT.newHandler(ch.alloc(), host, finalPort1));
                            }
                            p.addLast("codec", new HttpClientCodec());
                            p.addLast("readTimeout", new ReadTimeoutHandler(timeoutSeconds));
                            p.addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                            p.addLast("handler", new SimpleChannelInboundHandler<FullHttpResponse>() {
                                private boolean completed = false;

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
                                    if (completed) {
                                        return;
                                    }
                                    completed = true;

                                    long latency = System.currentTimeMillis() - startTime;
                                    logService.logAsync(gatewayUuid, route.getId(), target.getId(),
                                            method, path, msg.status().code(), (int) latency, null);

                                    Map<String, String> responseHeaders = new HashMap<>();
                                    msg.headers().forEach(entry -> {
                                        String key = entry.getKey();
                                        if (!key.startsWith(":") &&
                                                !key.equalsIgnoreCase("connection") &&
                                                !key.equalsIgnoreCase("keep-alive") &&
                                                !key.equalsIgnoreCase("transfer-encoding") &&
                                                !key.equalsIgnoreCase("upgrade") &&
                                                !key.equalsIgnoreCase("proxy-connection")) {
                                            responseHeaders.put(key, entry.getValue());
                                        }
                                    });

                                    String responseBody = msg.content().toString(CharsetUtil.UTF_8);
                                    callback.onSuccess(new ProxyResult(
                                            msg.status().code(),
                                            responseBody,
                                            responseHeaders
                                    ));
                                    ctx.close();
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    if (completed) {
                                        return;
                                    }
                                    completed = true;
                                    handleFailure(gatewayUuid, route, target, method, path,
                                            startTime, cause, callback);
                                    ctx.close();
                                }
                            });
                        }
                    });

            int finalPort = port;
            bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    handleFailure(gatewayUuid, route, target, method, path,
                            startTime, future.cause(), callback);
                    return;
                }

                ByteBuf content = body == null ? Unpooled.EMPTY_BUFFER :
                        Unpooled.copiedBuffer(body, CharsetUtil.UTF_8);

                FullHttpRequest outbound = new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1,
                        HttpMethod.valueOf(method),
                        requestPath,
                        content
                );

                String hostHeader = finalPort == 80 || finalPort == 443 ? host : host + ":" + finalPort;
                outbound.headers().set(HttpHeaderNames.HOST, hostHeader);
                outbound.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                outbound.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

                headers.forEach((key, value) -> {
                    if (key == null) {
                        return;
                    }
                    String lowerKey = key.toLowerCase();
                    if (!lowerKey.equals("host") &&
                            !lowerKey.equals("connection") &&
                            !lowerKey.equals("content-length")) {
                        outbound.headers().set(key, value);
                    }
                });

                outbound.headers().set("X-Forwarded-For", "gateway");
                outbound.headers().set("X-Gateway-ID", gatewayId);
                outbound.headers().set("X-Route-ID", route.getId().toString());

                future.channel().writeAndFlush(outbound).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            });

        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static String buildRequestPath(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getRawQuery();
        if (query != null && !query.isEmpty()) {
            return path + "?" + query;
        }
        return path;
    }

    private void handleFailure(UUID gatewayUuid, Route route, RouteTarget target,
                               String method, String path, long startTime,
                               Throwable error, ProxyCallback callback) {
        logger.error("Error forwarding to target: {}", target.getTargetUrl(), error);

        try {
            targetRepo.incrementFailureCount(target.getId());
        } catch (Exception ex) {
            logger.error("Failed to increment failure count", ex);
        }

        long latency = System.currentTimeMillis() - startTime;
        logService.logAsync(gatewayUuid, route.getId(), target.getId(),
                method, path, 502, (int) latency, error.getMessage());

        callback.onFailure(error);
    }

    private static SslContext buildSslContext() {
        try {
            return SslContextBuilder.forClient()
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }

    public interface ProxyCallback {
        void onSuccess(ProxyResult result);
        void onFailure(Throwable error);
    }

    public static class ProxyResult {
        public final int statusCode;
        public final String body;
        public final Map<String, String> headers;

        public ProxyResult(int statusCode, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
    }
}
