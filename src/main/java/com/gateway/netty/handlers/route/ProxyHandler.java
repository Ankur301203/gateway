package com.gateway.netty.handlers.route;

import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.AsyncHandler;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.ProxyServiceNetty;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyHandler extends BaseHandler implements AsyncHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final ProxyServiceNetty proxyService = new ProxyServiceNetty();

    @Override
    public FullHttpResponse handle(RequestContext ctx) {
        return ResponseBuilder.json(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"Proxy handler should be invoked asynchronously\"}")
                .build();
    }

    @Override
    public void handleAsync(ChannelHandlerContext nettyCtx, RequestContext ctx) {
        // Extract gateway ID from path: /gateway/{gatewayId}/rest/of/path
        String path = ctx.getPath();
        String[] parts = path.split("/", 4); // ["", "gateway", "{id}", "rest"]

        if (parts.length < 3) {
            logger.warn("Invalid gateway URL format: {}", path);
            FullHttpResponse response = ResponseBuilder.json(HttpResponseStatus.BAD_REQUEST)
                    .body("{\"error\": \"Invalid gateway URL format. Expected: /gateway/{id}/path\"}")
                    .build();
            nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        String gatewayId = parts[2];
        String targetPath = "/" + (parts.length > 3 ? parts[3] : "");

        try {
            java.util.UUID.fromString(gatewayId);
        } catch (IllegalArgumentException e) {
            FullHttpResponse response = ResponseBuilder.json(HttpResponseStatus.BAD_REQUEST)
                    .body("{\"error\": \"Invalid gateway ID format\"}")
                    .build();
            nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        logger.info("Proxying request: gateway={}, path={}, method={}",
                gatewayId, targetPath, ctx.getMethod());

        proxyService.forwardAsync(
                gatewayId,
                targetPath,
                ctx.getMethod(),
                ctx.getHeaders(),
                ctx.getBody(),
                new ProxyServiceNetty.ProxyCallback() {
                    @Override
                    public void onSuccess(ProxyServiceNetty.ProxyResult result) {
                        HttpResponseStatus status = HttpResponseStatus.valueOf(result.statusCode);
                        ResponseBuilder builder = ResponseBuilder.status(status);
                        result.headers.forEach(builder::header);
                        builder.header("X-Gateway-Proxy", "GatewayaaS/1.0");

                        FullHttpResponse response = builder.body(result.body).build();
                        nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        logger.error("Proxy error for gateway {}", gatewayId, error);
                        HttpResponseStatus status = HttpResponseStatus.BAD_GATEWAY;
                        String message = "Bad gateway - " + error.getMessage();

                        if (error instanceof io.netty.handler.timeout.ReadTimeoutException) {
                            status = HttpResponseStatus.GATEWAY_TIMEOUT;
                            message = "Gateway timeout - backend service did not respond";
                        }

                        FullHttpResponse response = ResponseBuilder.json(status)
                                .body("{\"error\": \"" + message.replace("\"", "\\\"") + "\"}")
                                .build();
                        nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    }
                }
        );
    }
}
