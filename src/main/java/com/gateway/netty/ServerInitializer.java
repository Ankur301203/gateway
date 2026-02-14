package com.gateway.netty;

import com.gateway.config.AppConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger logger = LoggerFactory.getLogger(ServerInitializer.class);

    private static final int MAX_CONTENT_LENGTH = AppConfig.getInt(
            "proxy.max.content.length", 10 * 1024 * 1024
    );

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // Read timeout (60 seconds)
        pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(60, TimeUnit.SECONDS));

        // HTTP codec - decodes HTTP requests and encodes responses
        pipeline.addLast("httpServerCodec", new HttpServerCodec());

        // Aggregates HTTP message fragments into FullHttpRequest
        pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));

        // Support for chunked responses (large files, streaming)
        pipeline.addLast("chunkedWriteHandler", new ChunkedWriteHandler());

        // Compression (gzip)
        pipeline.addLast("httpContentCompressor", new HttpContentCompressor());

        // Our custom router handler
        pipeline.addLast("routerHandler", new RouterHandler());

        logger.debug("Channel pipeline initialized for {}", ch.remoteAddress());
    }
}