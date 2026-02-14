package com.gateway.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

public class ManualMigrations {
    private static final Logger logger = LoggerFactory.getLogger(ManualMigrations.class);

    public static void runMigrations(Connection connection) throws Exception {
        logger.info("Running database migrations");

        try (Statement stmt = connection.createStatement()) {

            // Enable UUID extension
            logger.info("Creating UUID extension");
            stmt.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");

            // Migration 1: Create users table
            logger.info("Creating users table");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");

            // Create trigger function
            stmt.execute("""
                CREATE OR REPLACE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = CURRENT_TIMESTAMP;
                    RETURN NEW;
                END;
                $$ language 'plpgsql'
            """);

            stmt.execute("""
                DROP TRIGGER IF EXISTS update_users_updated_at ON users
            """);

            stmt.execute("""
                CREATE TRIGGER update_users_updated_at 
                BEFORE UPDATE ON users
                FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()
            """);

            // Migration 2: Create gateways table
            logger.info("Creating gateways table");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS gateways (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gateways_user_id ON gateways(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gateways_created_at ON gateways(created_at DESC)");

            stmt.execute("""
                DROP TRIGGER IF EXISTS update_gateways_updated_at ON gateways
            """);

            stmt.execute("""
                CREATE TRIGGER update_gateways_updated_at 
                BEFORE UPDATE ON gateways
                FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()
            """);

            // Migration 3: Create routes table
            logger.info("Creating routes table");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS routes (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    gateway_id UUID NOT NULL REFERENCES gateways(id) ON DELETE CASCADE,
                    path VARCHAR(255) NOT NULL,
                    method VARCHAR(10) NOT NULL,
                    timeout_ms INTEGER NOT NULL DEFAULT 30000,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT unique_gateway_path_method UNIQUE(gateway_id, path, method),
                    CONSTRAINT valid_method CHECK (method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'))
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_routes_gateway_id ON routes(gateway_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_routes_lookup ON routes(gateway_id, path, method)");

            stmt.execute("""
                DROP TRIGGER IF EXISTS update_routes_updated_at ON routes
            """);

            stmt.execute("""
                CREATE TRIGGER update_routes_updated_at 
                BEFORE UPDATE ON routes
                FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()
            """);

            // Migration 4: Create route_targets table
            logger.info("Creating route_targets table");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS route_targets (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
                    target_url VARCHAR(500) NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT true,
                    health_status VARCHAR(20) NOT NULL DEFAULT 'unknown',
                    last_health_check TIMESTAMP,
                    consecutive_failures INTEGER NOT NULL DEFAULT 0,
                    weight INTEGER NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT valid_health_status CHECK (health_status IN ('healthy', 'unhealthy', 'unknown')),
                    CONSTRAINT valid_weight CHECK (weight > 0)
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_route_targets_route_id ON route_targets(route_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_route_targets_health ON route_targets(route_id, is_active, health_status)");

            stmt.execute("""
                DROP TRIGGER IF EXISTS update_route_targets_updated_at ON route_targets
            """);

            stmt.execute("""
                CREATE TRIGGER update_route_targets_updated_at 
                BEFORE UPDATE ON route_targets
                FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()
            """);

            // Migration 5: Create request_logs table
            logger.info("Creating request_logs table");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS request_logs (
                    id BIGSERIAL PRIMARY KEY,
                    gateway_id UUID NOT NULL,
                    route_id UUID,
                    target_id UUID,
                    method VARCHAR(10) NOT NULL,
                    path VARCHAR(500) NOT NULL,
                    status_code INTEGER NOT NULL,
                    latency_ms INTEGER NOT NULL,
                    error_message TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_request_logs_gateway_id ON request_logs(gateway_id, created_at DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_request_logs_route_id ON request_logs(route_id, created_at DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_request_logs_created_at ON request_logs(created_at DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_request_logs_status ON request_logs(status_code, created_at DESC)");

            logger.info("Database migrations completed successfully");
        }
    }
}