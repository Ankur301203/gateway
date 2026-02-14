package com.gateway.repository;

import com.gateway.database.DatabaseConnectionPool;
import com.gateway.domain.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GatewayRepository {
    private static final Logger logger = LoggerFactory.getLogger(GatewayRepository.class);

    public Gateway create(UUID userId, String name, String description) throws SQLException {
        String sql = "INSERT INTO gateways (user_id, name, description) VALUES (?, ?, ?) RETURNING *";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            stmt.setString(2, name);
            stmt.setString(3, description);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Gateway gateway = mapRow(rs);
                logger.info("Created gateway: {} for user: {}", gateway.getId(), userId);
                return gateway;
            }

            throw new SQLException("Failed to create gateway");
        }
    }

    public List<Gateway> findByUserId(UUID userId) throws SQLException {
        String sql = "SELECT * FROM gateways WHERE user_id = ? ORDER BY created_at DESC";
        List<Gateway> gateways = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                gateways.add(mapRow(rs));
            }
        }

        return gateways;
    }

    public Optional<Gateway> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM gateways WHERE id = ?";

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

    public Optional<Gateway> findByIdAndUserId(UUID id, UUID userId) throws SQLException {
        String sql = "SELECT * FROM gateways WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }

        return Optional.empty();
    }

    public boolean delete(UUID id, UUID userId) throws SQLException {
        String sql = "DELETE FROM gateways WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.setObject(2, userId);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                logger.info("Deleted gateway: {}", id);
                return true;
            }

            return false;
        }
    }

    private Gateway mapRow(ResultSet rs) throws SQLException {
        return new Gateway(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("user_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}