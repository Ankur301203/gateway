package com.gateway.domain;

import java.time.Instant;
import java.util.UUID;

public class Gateway {
    private final UUID id;
    private final UUID userId;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Gateway(UUID id, UUID userId, String name, String description,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Gateway{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}