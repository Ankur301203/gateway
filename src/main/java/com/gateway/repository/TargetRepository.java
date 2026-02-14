package com.gateway.repository;

import com.gateway.database.DatabaseConnectionPool;
import com.gateway.domain.RouteTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TargetRepository {
    private static final Logger logger = LoggerFactory.getLogger(TargetRepository.class);

    public RouteTarget create(UUID routeId, String targetUrl, int weight) throws SQLException {
        String sql = "INSERT INTO route_targets (route_id, target_url, weight) " +
                "VALUES (?, ?, ?) RETURNING *";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, routeId);
            stmt.setString(2, targetUrl);
            stmt.setInt(3, weight);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                RouteTarget target = mapRow(rs);
                logger.info("Created target: {} for route: {}", targetUrl, routeId);
                return target;
            }

            throw new SQLException("Failed to create target");
        }
    }

    public List<RouteTarget> findByRouteId(UUID routeId) throws SQLException {
        String sql = "SELECT * FROM route_targets WHERE route_id = ? ORDER BY created_at";
        List<RouteTarget> targets = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, routeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                targets.add(mapRow(rs));
            }
        }

        return targets;
    }

    public List<RouteTarget> findHealthyByRouteId(UUID routeId) throws SQLException {
        String sql = "SELECT * FROM route_targets " +
                "WHERE route_id = ? AND is_active = true AND health_status = 'healthy' " +
                "ORDER BY created_at";
        List<RouteTarget> targets = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, routeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                targets.add(mapRow(rs));
            }
        }

        return targets;
    }

    public List<RouteTarget> findAllActive() throws SQLException {
        String sql = "SELECT * FROM route_targets WHERE is_active = true";
        List<RouteTarget> targets = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                targets.add(mapRow(rs));
            }
        }

        return targets;
    }

    public Optional<RouteTarget> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM route_targets WHERE id = ?";

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

    public int incrementFailureCount(UUID id) throws SQLException {
        String sql = "UPDATE route_targets " +
                "SET consecutive_failures = consecutive_failures + 1, " +
                "    last_health_check = CURRENT_TIMESTAMP " +
                "WHERE id = ? " +
                "RETURNING consecutive_failures";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("consecutive_failures");
            }

            return 0;
        }
    }

    public void resetFailureCount(UUID id) throws SQLException {
        String sql = "UPDATE route_targets " +
                "SET consecutive_failures = 0, " +
                "    last_health_check = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.executeUpdate();
        }
    }

    public void updateHealthStatus(UUID id, String status) throws SQLException {
        String sql = "UPDATE route_targets " +
                "SET health_status = ?, " +
                "    last_health_check = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setObject(2, id);
            stmt.executeUpdate();

            logger.info("Updated target {} health status to: {}", id, status);
        }
    }

    public boolean toggleActive(UUID id) throws SQLException {
        String sql = "UPDATE route_targets " +
                "SET is_active = NOT is_active " +
                "WHERE id = ? " +
                "RETURNING is_active";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                boolean isActive = rs.getBoolean("is_active");
                logger.info("Toggled target {} active status to: {}", id, isActive);
                return isActive;
            }

            return false;
        }
    }

    public boolean delete(UUID id) throws SQLException {
        String sql = "DELETE FROM route_targets WHERE id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                logger.info("Deleted target: {}", id);
                return true;
            }

            return false;
        }
    }

    private RouteTarget mapRow(ResultSet rs) throws SQLException {
        Timestamp lastCheck = rs.getTimestamp("last_health_check");

        return new RouteTarget(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("route_id"),
                rs.getString("target_url"),
                rs.getBoolean("is_active"),
                rs.getString("health_status"),
                lastCheck != null ? lastCheck.toInstant() : null,
                rs.getInt("consecutive_failures"),
                rs.getInt("weight"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}