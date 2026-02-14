package com.gateway.domain;

import java.time.Instant;
import java.util.UUID;

public class RequestLog {
    private final Long id;
    private final UUID gatewayId;
    private final UUID routeId;
    private final UUID targetId;
    private final String method;
    private final String path;
    private final int statusCode;
    private final int latencyMs;
    private final String errorMessage;
    private final Instant createdAt;

    public RequestLog(Long id, UUID gatewayId, UUID routeId, UUID targetId,
                      String method, String path, int statusCode, int latencyMs,
                      String errorMessage, Instant createdAt) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.routeId = routeId;
        this.targetId = targetId;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    // Constructor for creating new logs (without id and createdAt)
    public RequestLog(UUID gatewayId, UUID routeId, UUID targetId,
                      String method, String path, int statusCode, int latencyMs,
                      String errorMessage) {
        this(null, gatewayId, routeId, targetId, method, path,
                statusCode, latencyMs, errorMessage, null);
    }

    public Long getId() {
        return id;
    }

    public UUID getGatewayId() {
        return gatewayId;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "RequestLog{" +
                "id=" + id +
                ", gatewayId=" + gatewayId +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", statusCode=" + statusCode +
                ", latencyMs=" + latencyMs +
                '}';
    }
}