package com.gateway.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    // Server config
    public static int getServerPort() {
        return getInt("server.port", 8080);
    }

    public static int getBossThreads() {
        return getInt("server.boss.threads", 1);
    }

    public static int getWorkerThreads() {
        return getInt("server.worker.threads", 0); // 0 = default (CPU cores * 2)
    }

    // Database config
    public static String getDbUrl() {
        return get("db.url");
    }

    public static String getDbUsername() {
        return get("db.username");
    }

    public static String getDbPassword() {
        return get("db.password");
    }

    public static int getDbPoolMaximum() {
        return getInt("db.pool.maximum", 20);
    }

    public static int getDbPoolMinimum() {
        return getInt("db.pool.minimum", 5);
    }

    // JWT config
    public static String getJwtSecret() {
        return get("jwt.secret");
    }

    public static long getJwtExpirationMs() {
        return getLong("jwt.expiration.ms");
    }

    // Health check config
    public static int getHealthCheckInterval() {
        return getInt("healthcheck.interval.seconds", 30);
    }

    public static int getHealthCheckTimeout() {
        return getInt("healthcheck.timeout.seconds", 5);
    }

    public static int getHealthCheckUnhealthyThreshold() {
        return getInt("healthcheck.unhealthy.threshold", 3);
    }

    public static int getHealthCheckHealthyThreshold() {
        return getInt("healthcheck.healthy.threshold", 2);
    }

    // Log service config
    public static int getLogBufferSize() {
        return getInt("log.buffer.size", 10000);
    }

    public static int getLogBatchSize() {
        return getInt("log.batch.size", 100);
    }

    public static int getLogFlushInterval() {
        return getInt("log.flush.interval.seconds", 5);
    }
}