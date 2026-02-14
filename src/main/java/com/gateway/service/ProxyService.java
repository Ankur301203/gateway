package com.gateway.service;

import com.gateway.domain.Route;
import com.gateway.domain.RouteTarget;
import com.gateway.repository.RouteRepository;
import com.gateway.repository.TargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.*;

public class ProxyService {
    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    private final RouteRepository routeRepo = new RouteRepository();
    private final TargetRepository targetRepo = new TargetRepository();
    private final LoadBalancerService loadBalancer = LoadBalancerService.getInstance();
    private final LogService logService = LogService.getInstance();

    private final HttpClient httpClient;

    public ProxyService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public ProxyResult forward(String gatewayId, String path, String method,
                               Map<String, String> headers, String body) throws Exception {

        long startTime = System.currentTimeMillis();
        UUID gatewayUuid = UUID.fromString(gatewayId);

        // 1. Find matching route
        Optional<Route> routeOpt = routeRepo.findByGatewayAndPathMatchAndMethod(
                gatewayUuid, path, method
        );

        if (routeOpt.isEmpty()) {
            logger.warn("No route found for gateway={}, path={}, method={}",
                    gatewayId, path, method);
            return new ProxyResult(404, "{\"error\": \"Route not found\"}", Map.of());
        }

        Route route = routeOpt.get();

        // 2. Get healthy targets
        List<RouteTarget> targets = targetRepo.findHealthyByRouteId(route.getId());

        if (targets.isEmpty()) {
            logger.warn("No healthy targets for route={}", route.getId());
            logService.logAsync(gatewayUuid, route.getId(), null,
                    method, path, 503, 0, "No healthy targets");
            return new ProxyResult(503, "{\"error\": \"Service unavailable - no healthy backends\"}", Map.of());
        }

        // 3. Load balance
        RouteTarget target = loadBalancer.selectTarget(route.getId(), targets);

        logger.info("Forwarding {} {} to {}", method, path, target.getTargetUrl());

        // 4. Build target URL
        String targetUrl = target.getTargetUrl() + path;

        // 5. Forward request
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofMillis(route.getTimeoutMs()));

            // Copy headers (except Host, Connection, Content-Length)
            headers.forEach((key, value) -> {
                String lowerKey = key.toLowerCase();
                if (!lowerKey.equals("host") &&
                        !lowerKey.equals("connection") &&
                        !lowerKey.equals("content-length")) {
                    requestBuilder.header(key, value);
                }
            });

            // Add proxy headers
            requestBuilder.header("X-Forwarded-For", "gateway");
            requestBuilder.header("X-Gateway-ID", gatewayId);
            requestBuilder.header("X-Route-ID", route.getId().toString());

            // Set method and body
            HttpRequest.BodyPublisher bodyPublisher = (body == null || body.isEmpty())
                    ? BodyPublishers.noBody()
                    : BodyPublishers.ofString(body);

            requestBuilder.method(method, bodyPublisher);

            // Send request
            HttpResponse<String> httpResponse = httpClient.send(
                    requestBuilder.build(),
                    BodyHandlers.ofString()
            );

            // Calculate latency
            long latency = System.currentTimeMillis() - startTime;

            // Log async
            logService.logAsync(gatewayUuid, route.getId(), target.getId(),
                    method, path, httpResponse.statusCode(), (int) latency, null);

            // Extract headers (filter out HTTP/2 pseudo-headers and hop-by-hop headers)
            Map<String, String> responseHeaders = new HashMap<>();
            httpResponse.headers().map().forEach((key, values) -> {
                // Skip HTTP/2 pseudo-headers (start with :)
                // Skip hop-by-hop headers that shouldn't be forwarded
                if (!values.isEmpty() &&
                        !key.startsWith(":") &&
                        !key.equalsIgnoreCase("connection") &&
                        !key.equalsIgnoreCase("keep-alive") &&
                        !key.equalsIgnoreCase("transfer-encoding") &&
                        !key.equalsIgnoreCase("upgrade") &&
                        !key.equalsIgnoreCase("proxy-connection")) {
                    responseHeaders.put(key, values.get(0));
                }
            });

            logger.info("Proxied request completed: {} {} -> {} ({}ms)",
                    method, path, httpResponse.statusCode(), latency);

            return new ProxyResult(
                    httpResponse.statusCode(),
                    httpResponse.body(),
                    responseHeaders
            );

        } catch (Exception e) {
            logger.error("Error forwarding to target: {}", target.getTargetUrl(), e);

            // Increment failure count
            try {
                targetRepo.incrementFailureCount(target.getId());
            } catch (Exception ex) {
                logger.error("Failed to increment failure count", ex);
            }

            long latency = System.currentTimeMillis() - startTime;
            logService.logAsync(gatewayUuid, route.getId(), target.getId(),
                    method, path, 502, (int) latency, e.getMessage());

            throw e;
        }
    }

    public static class ProxyResult {
        public final int statusCode;
        public final String body;
        public final Map<String, String> headers;

        public ProxyResult(int statusCode, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
    }
}