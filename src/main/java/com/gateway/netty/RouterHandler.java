package com.gateway.netty;

import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.netty.handlers.auth.*;
import com.gateway.netty.handlers.gateway.*;
import com.gateway.netty.handlers.logs.GetLogsHandler;
import com.gateway.netty.handlers.proxy.ProxyHandler;
import com.gateway.netty.handlers.route.*;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(RouterHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // Parse request into context
            RequestContext reqCtx = RequestContext.from(request);

            logger.info("{} {}", reqCtx.getMethod(), reqCtx.getPath());

            // Route to appropriate handler
            BaseHandler handler = matchRoute(reqCtx);

            if (handler == null) {
                sendNotFound(ctx, reqCtx.getPath());
                return;
            }

            // Execute handler
            FullHttpResponse response = handler.handle(reqCtx);

            // Send response
            sendResponse(ctx, response);

        } catch (Exception e) {
            logger.error("Error handling request", e);
            sendError(ctx, e);
        }
    }

    private BaseHandler matchRoute(RequestContext ctx) {
        String path = ctx.getPath();
        String method = ctx.getMethod();

        // Health check endpoint
        if (path.equals("/health") && method.equals("GET")) {
            return new HealthCheckHandler();
        }

        // PROXY TRAFFIC - highest priority
        if (path.startsWith("/gateway/")) {
            return new ProxyHandler();
        }

        // AUTH ENDPOINTS
        if (path.equals("/api/v1/auth/register") && method.equals("POST")) {
            return new RegisterHandler();
        }
        if (path.equals("/api/v1/auth/login") && method.equals("POST")) {
            return new LoginHandler();
        }

        // GATEWAY MANAGEMENT
        if (path.equals("/api/v1/gateways") && method.equals("POST")) {
            return new CreateGatewayHandler();
        }
        if (path.equals("/api/v1/gateways") && method.equals("GET")) {
            return new ListGatewaysHandler();
        }
        if (path.matches("/api/v1/gateways/[a-f0-9-]+") && method.equals("GET")) {
            return new GetGatewayHandler();
        }
        if (path.matches("/api/v1/gateways/[a-f0-9-]+") && method.equals("DELETE")) {
            return new DeleteGatewayHandler();
        }

        // ROUTE MANAGEMENT
        if (path.matches("/api/v1/gateways/[a-f0-9-]+/routes") && method.equals("POST")) {
            return new CreateRouteHandler();
        }
        if (path.matches("/api/v1/gateways/[a-f0-9-]+/routes") && method.equals("GET")) {
            return new ListRoutesHandler();
        }
        if (path.matches("/api/v1/routes/[a-f0-9-]+") && method.equals("GET")) {
            return new GetRouteHandler();
        }
        if (path.matches("/api/v1/routes/[a-f0-9-]+") && method.equals("DELETE")) {
            return new DeleteRouteHandler();
        }

        // TARGET MANAGEMENT
        if (path.matches("/api/v1/routes/[a-f0-9-]+/targets") && method.equals("POST")) {
            return new AddTargetHandler();
        }
        if (path.matches("/api/v1/routes/[a-f0-9-]+/targets") && method.equals("GET")) {
            return new ListTargetsHandler();
        }
        if (path.matches("/api/v1/targets/[a-f0-9-]+") && method.equals("DELETE")) {
            return new DeleteTargetHandler();
        }
        if (path.matches("/api/v1/targets/[a-f0-9-]+/toggle") && method.equals("PATCH")) {
            return new ToggleTargetHandler();
        }

        // LOGS
        if (path.matches("/api/v1/gateways/[a-f0-9-]+/logs") && method.equals("GET")) {
            return new GetLogsHandler();
        }

        return null;
    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendNotFound(ChannelHandlerContext ctx, String path) {
        FullHttpResponse response = ResponseBuilder.json(HttpResponseStatus.NOT_FOUND)
                .body(String.format("{\"error\": \"Route not found: %s\"}", path))
                .build();
        sendResponse(ctx, response);
    }

    private void sendError(ChannelHandlerContext ctx, Exception e) {
        FullHttpResponse response = ResponseBuilder.json(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                .body(String.format("{\"error\": \"%s\"}", e.getMessage().replace("\"", "\\\"")))
                .build();
        sendResponse(ctx, response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in channel handler", cause);
        ctx.close();
    }

    // Simple health check handler
    private static class HealthCheckHandler extends BaseHandler {
        @Override
        public FullHttpResponse handle(RequestContext ctx) {
            return ResponseBuilder.ok()
                    .jsonContent()
                    .body("{\"status\": \"healthy\"}")
                    .build();
        }
    }
}