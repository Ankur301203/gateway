package com.gateway;

import com.gateway.config.AppConfig;
import com.gateway.database.DatabaseConnectionPool;
import com.gateway.netty.NettyServer;
import com.gateway.service.HealthCheckService;
import com.gateway.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting GatewayaaS...");

        try {
            // 1. Initialize database connection pool
            logger.info("Initializing database connection pool");
            DatabaseConnectionPool.initialize();
            logger.info("Database initialized successfully");

            // 2. Start background services
            logger.info("Starting background services");

            HealthCheckService healthCheckService = new HealthCheckService();
            healthCheckService.start();

            LogService logService = LogService.getInstance();
            logService.start();

            logger.info("Background services started successfully");

            // 3. Create Netty server
            int port = AppConfig.getServerPort();
            NettyServer server = new NettyServer(port);

            // 4. Register graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received, initiating graceful shutdown");

                // Stop accepting new requests
                server.shutdown();

                // Stop background services
                healthCheckService.stop();
                logService.stop();

                // Close database connections
                DatabaseConnectionPool.close();

                logger.info("Graceful shutdown completed");
            }));

            // 5. Start server (blocking)
            logger.info("All systems ready, starting HTTP server");
            server.start();

        } catch (Exception e) {
            logger.error("Fatal error during startup", e);
            System.exit(1);
        }
    }
}