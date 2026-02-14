package com.gateway.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {
    void handle(ChannelHandlerContext ctx, FullHttpRequest request);
}
