package com.gateway.handlers;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class HealthHandler implements RequestHandler {

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {

        String json = "{\"status\":\"ok\"}";
        byte[] content = json.getBytes(StandardCharsets.UTF_8);

        FullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(content)
                );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);

        ctx.writeAndFlush(response);
    }
}
