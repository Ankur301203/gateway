package com.gateway.service;

import com.gateway.domain.Route;
import com.gateway.domain.RouteTarget;
import com.gateway.repository.RouteRepository;
import com.gateway.repository.TargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RouteService {
    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);
    private final RouteRepository routeRepository = new RouteRepository();
    private final TargetRepository targetRepository = new TargetRepository();

    private static final List<String> VALID_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
    );

    public Route createRoute(UUID gatewayId, String path, String method, Integer timeoutMs)
            throws SQLException {

        // Validate inputs
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with /");
        }

        if (method == null || !VALID_METHODS.contains(method.toUpperCase())) {
            throw new IllegalArgumentException("Invalid HTTP method");
        }

        int timeout = (timeoutMs != null && timeoutMs > 0) ? timeoutMs : 30000;

        return routeRepository.create(gatewayId, path, method.toUpperCase(), timeout);
    }

    public List<Route> getGatewayRoutes(UUID gatewayId) throws SQLException {
        return routeRepository.findByGatewayId(gatewayId);
    }

    public Optional<Route> getRoute(UUID routeId) throws SQLException {
        return routeRepository.findById(routeId);
    }

    public RouteTarget addTarget(UUID routeId, String targetUrl, Integer weight) throws SQLException {
        // Validate URL
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("Target URL is required");
        }

        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Target URL must start with http:// or https://");
        }

        // Remove trailing slash
        if (targetUrl.endsWith("/")) {
            targetUrl = targetUrl.substring(0, targetUrl.length() - 1);
        }

        int targetWeight = (weight != null && weight > 0) ? weight : 1;

        return targetRepository.create(routeId, targetUrl, targetWeight);
    }

    public List<RouteTarget> getRouteTargets(UUID routeId) throws SQLException {
        return targetRepository.findByRouteId(routeId);
    }

    public boolean deleteRoute(UUID routeId) throws SQLException {
        return routeRepository.delete(routeId);
    }

    public boolean deleteTarget(UUID targetId) throws SQLException {
        return targetRepository.delete(targetId);
    }

    public boolean toggleTargetActive(UUID targetId) throws SQLException {
        return targetRepository.toggleActive(targetId);
    }
}