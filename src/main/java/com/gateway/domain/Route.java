package com.gateway.domain;

import java.time.Instant;
import java.util.UUID;

public class Route {
    private final UUID id;
    private final UUID gatewayId;
    private final String path;
    private final String method;
    private final int timeoutMs;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Route(UUID id, UUID gatewayId, String path, String method,
                 int timeoutMs, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.gatewayId = gatewayId;
        this.path = path;
        this.method = method;
        this.timeoutMs = timeoutMs;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGatewayId() {
        return gatewayId;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", gatewayId=" + gatewayId +
                ", path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}