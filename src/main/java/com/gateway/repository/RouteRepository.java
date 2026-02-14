package com.gateway.repository;

import com.gateway.database.DatabaseConnectionPool;
import com.gateway.domain.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RouteRepository {
    private static final Logger logger = LoggerFactory.getLogger(RouteRepository.class);

    public Route create(UUID gatewayId, String path, String method, int timeoutMs) throws SQLException {
        String sql = "INSERT INTO routes (gateway_id, path, method, timeout_ms) " +
                "VALUES (?, ?, ?, ?) RETURNING *";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, gatewayId);
            stmt.setString(2, path);
            stmt.setString(3, method);
            stmt.setInt(4, timeoutMs);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Route route = mapRow(rs);
                logger.info("Created route: {} {} for gateway: {}", method, path, gatewayId);
                return route;
            }

            throw new SQLException("Failed to create route");
        }
    }

    public List<Route> findByGatewayId(UUID gatewayId) throws SQLException {
        String sql = "SELECT * FROM routes WHERE gateway_id = ? ORDER BY created_at DESC";
        List<Route> routes = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, gatewayId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                routes.add(mapRow(rs));
            }
        }

        return routes;
    }

    public Optional<Route> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM routes WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }

        return Optional.empty();
    }

    public Optional<Route> findByGatewayAndPathAndMethod(UUID gatewayId, String path, String method)
            throws SQLException {
        String sql = "SELECT * FROM routes WHERE gateway_id = ? AND path = ? AND method = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, gatewayId);
            stmt.setString(2, path);
            stmt.setString(3, method);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }

        return Optional.empty();
    }

    // NEW METHOD - add this
    public Optional<Route> findByGatewayAndPathMatchAndMethod(UUID gatewayId, String requestPath, String method)
            throws SQLException {

        // First try exact match using the OLD method
        Optional<Route> exactMatch = findByGatewayAndPathAndMethod(gatewayId, requestPath, method);
        if (exactMatch.isPresent()) {
            logger.debug("Found exact route match for path: {}", requestPath);
            return exactMatch;
        }

        // Then try prefix match - find longest matching prefix
        String sql = "SELECT * FROM routes " +
                "WHERE gateway_id = ? " +
                "AND ? LIKE CONCAT(path, '%') " +
                "AND method = ? " +
                "ORDER BY LENGTH(path) DESC " +
                "LIMIT 1";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, gatewayId);
            stmt.setString(2, requestPath);
            stmt.setString(3, method);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Route route = mapRow(rs);
                logger.debug("Found prefix route match: {} for request path: {}",
                        route.getPath(), requestPath);
                return Optional.of(route);
            }
        }

        logger.debug("No route match found for path: {}", requestPath);
        return Optional.empty();
    }

    public boolean delete(UUID id) throws SQLException {
        String sql = "DELETE FROM routes WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                logger.info("Deleted route: {}", id);
                return true;
            }

            return false;
        }
    }

    private Route mapRow(ResultSet rs) throws SQLException {
        return new Route(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("gateway_id"),
                rs.getString("path"),
                rs.getString("method"),
                rs.getInt("timeout_ms"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}