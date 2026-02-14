package com.gateway.service;

import com.gateway.domain.Gateway;
import com.gateway.repository.GatewayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GatewayService {
    private static final Logger logger = LoggerFactory.getLogger(GatewayService.class);
    private final GatewayRepository gatewayRepository = new GatewayRepository();

    public Gateway createGateway(UUID userId, String name, String description) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Gateway name is required");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("Gateway name must be less than 100 characters");
        }

        return gatewayRepository.create(userId, name, description);
    }

    public List<Gateway> getUserGateways(UUID userId) throws SQLException {
        return gatewayRepository.findByUserId(userId);
    }

    public Optional<Gateway> getGateway(UUID gatewayId, UUID userId) throws SQLException {
        return gatewayRepository.findByIdAndUserId(gatewayId, userId);
    }

    public Optional<Gateway> getGatewayById(UUID gatewayId) throws SQLException {
        return gatewayRepository.findById(gatewayId);
    }

    public boolean deleteGateway(UUID gatewayId, UUID userId) throws SQLException {
        return gatewayRepository.delete(gatewayId, userId);
    }
}