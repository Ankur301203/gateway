package com.gateway.server;

import com.gateway.handlers.HealthHandler;
import com.gateway.handlers.RequestHandler;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.util.HashMap;
import java.util.Map;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Map<String, RequestHandler> routes = new HashMap<>();

    public RouterHandler() {
        registerRoutes();
    }

    private void registerRoutes() {
        routes.put("GET:/health", new HealthHandler());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                FullHttpRequest request) {

        String key = request.method().name() + ":" + request.uri();

        RequestHandler handler = routes.get(key);

        if (handler != null) {
            handler.handle(ctx, request);
        } else {
            sendNotFound(ctx);
        }
    }

    private void sendNotFound(ChannelHandlerContext ctx) {
        FullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.NOT_FOUND
                );
        ctx.writeAndFlush(response);
    }
}
