package com.gateway.database;

import com.gateway.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionPool.class);
    private static HikariDataSource dataSource;

    public static void initialize() {
        logger.info("Initializing database connection pool");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.getDbUrl());
        config.setUsername(AppConfig.getDbUsername());
        config.setPassword(AppConfig.getDbPassword());
        config.setMaximumPoolSize(AppConfig.getDbPoolMaximum());
        config.setMinimumIdle(AppConfig.getDbPoolMinimum());
        config.setConnectionTimeout(AppConfig.getInt("db.connection.timeout", 30000));
        config.setIdleTimeout(AppConfig.getInt("db.idle.timeout", 600000));
        config.setMaxLifetime(AppConfig.getInt("db.max.lifetime", 1800000));

        // Performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);

        logger.info("Database connection pool initialized successfully");

        // Run migrations
        runMigrations();
    }

    private static void runMigrations() {
        logger.info("Running database migrations");

        try (Connection conn = dataSource.getConnection()) {
            ManualMigrations.runMigrations(conn);
            logger.info("Database migrations completed successfully");
        } catch (Exception e) {
            logger.error("Failed to run database migrations", e);
            throw new RuntimeException("Database migration failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Database pool not initialized");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
        }
    }

    public static boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
}