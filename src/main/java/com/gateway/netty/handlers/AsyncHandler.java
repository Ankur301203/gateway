package com.gateway.netty.handlers;

import com.gateway.http.RequestContext;
import io.netty.channel.ChannelHandlerContext;

public interface AsyncHandler {
    void handleAsync(ChannelHandlerContext ctx, RequestContext reqCtx);
}
