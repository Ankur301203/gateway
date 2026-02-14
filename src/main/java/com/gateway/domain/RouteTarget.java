package com.gateway.domain;

import java.time.Instant;
import java.util.UUID;

public class RouteTarget {
    private final UUID id;
    private final UUID routeId;
    private final String targetUrl;
    private final boolean isActive;
    private final String healthStatus;
    private final Instant lastHealthCheck;
    private final int consecutiveFailures;
    private final int weight;
    private final Instant createdAt;
    private final Instant updatedAt;

    public RouteTarget(UUID id, UUID routeId, String targetUrl, boolean isActive,
                       String healthStatus, Instant lastHealthCheck, int consecutiveFailures,
                       int weight, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.routeId = routeId;
        this.targetUrl = targetUrl;
        this.isActive = isActive;
        this.healthStatus = healthStatus;
        this.lastHealthCheck = lastHealthCheck;
        this.consecutiveFailures = consecutiveFailures;
        this.weight = weight;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public Instant getLastHealthCheck() {
        return lastHealthCheck;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public int getWeight() {
        return weight;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isHealthy() {
        return isActive && "healthy".equals(healthStatus);
    }

    @Override
    public String toString() {
        return "RouteTarget{" +
                "id=" + id +
                ", routeId=" + routeId +
                ", targetUrl='" + targetUrl + '\'' +
                ", isActive=" + isActive +
                ", healthStatus='" + healthStatus + '\'' +
                ", consecutiveFailures=" + consecutiveFailures +
                '}';
    }
}