package com.gateway.netty;

import com.gateway.config.AppConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public NettyServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        int bossThreads = AppConfig.getBossThreads();
        int workerThreads = AppConfig.getWorkerThreads();

        // Boss group accepts incoming connections
        bossGroup = new NioEventLoopGroup(bossThreads);

        // Worker group handles I/O operations
        // 0 = default (available processors * 2)
        workerGroup = new NioEventLoopGroup(workerThreads);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            logger.info("Starting Netty server on port {}", port);
            logger.info("Boss threads: {}, Worker threads: {}",
                    bossThreads, workerThreads == 0 ? "default" : workerThreads);

            // Bind and start accepting connections
            channelFuture = bootstrap.bind(port).sync();
            logger.info("Gateway server started successfully on port {}", port);

            // Wait until server socket is closed
            channelFuture.channel().closeFuture().sync();

        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        logger.info("Shutting down Netty server");

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        logger.info("Netty server shut down successfully");
    }
}