package com.gateway.repository;

import com.gateway.database.DatabaseConnectionPool;
import com.gateway.domain.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogRepository {
    private static final Logger logger = LoggerFactory.getLogger(LogRepository.class);

    public void batchInsert(List<RequestLog> logs) throws SQLException {
        if (logs.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO request_logs " +
                "(gateway_id, route_id, target_id, method, path, status_code, latency_ms, error_message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (RequestLog log : logs) {
                stmt.setObject(1, log.getGatewayId());
                stmt.setObject(2, log.getRouteId());
                stmt.setObject(3, log.getTargetId());
                stmt.setString(4, log.getMethod());
                stmt.setString(5, log.getPath());
                stmt.setInt(6, log.getStatusCode());
                stmt.setInt(7, log.getLatencyMs());
                stmt.setString(8, log.getErrorMessage());
                stmt.addBatch();
            }

            stmt.executeBatch();
            logger.debug("Inserted {} request logs", logs.size());
        }
    }

    public List<RequestLog> findByGatewayId(UUID gatewayId, int limit, Integer statusCode)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM request_logs WHERE gateway_id = ?"
        );

        if (statusCode != null) {
            sql.append(" AND status_code = ?");
        }

        sql.append(" ORDER BY created_at DESC LIMIT ?");

        List<RequestLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            stmt.setObject(paramIndex++, gatewayId);

            if (statusCode != null) {
                stmt.setInt(paramIndex++, statusCode);
            }

            stmt.setInt(paramIndex, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        }

        return logs;
    }

    public List<RequestLog> findByRouteId(UUID routeId, int limit) throws SQLException {
        String sql = "SELECT * FROM request_logs WHERE route_id = ? " +
                "ORDER BY created_at DESC LIMIT ?";

        List<RequestLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, routeId);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        }

        return logs;
    }

    private RequestLog mapRow(ResultSet rs) throws SQLException {
        return new RequestLog(
                rs.getLong("id"),
                (UUID) rs.getObject("gateway_id"),
                (UUID) rs.getObject("route_id"),
                (UUID) rs.getObject("target_id"),
                rs.getString("method"),
                rs.getString("path"),
                rs.getInt("status_code"),
                rs.getInt("latency_ms"),
                rs.getString("error_message"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}