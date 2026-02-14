package com.gateway.service;

import com.gateway.config.AppConfig;
import com.gateway.domain.RouteTarget;
import com.gateway.repository.TargetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class HealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final ExecutorService workerPool = Executors.newFixedThreadPool(10);
    private final TargetRepository targetRepo = new TargetRepository();

    private final HttpClient httpClient;
    private final int healthCheckInterval;
    private final int healthCheckTimeout;
    private final int unhealthyThreshold;
    private final int healthyThreshold;

    private volatile boolean running = false;

    public HealthCheckService() {
        this.healthCheckInterval = AppConfig.getHealthCheckInterval();
        this.healthCheckTimeout = AppConfig.getHealthCheckTimeout();
        this.unhealthyThreshold = AppConfig.getHealthCheckUnhealthyThreshold();
        this.healthyThreshold = AppConfig.getHealthCheckHealthyThreshold();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(healthCheckTimeout))
                .build();
    }

    public void start() {
        if (running) {
            logger.warn("Health check service already running");
            return;
        }

        running = true;

        scheduler.scheduleAtFixedRate(
                this::performHealthChecks,
                0,
                healthCheckInterval,
                TimeUnit.SECONDS
        );

        logger.info("Health check service started (interval: {}s, timeout: {}s)",
                healthCheckInterval, healthCheckTimeout);
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        logger.info("Stopping health check service");

        scheduler.shutdown();
        workerPool.shutdown();

        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Health check service stopped");
    }

    private void performHealthChecks() {
        try {
            List<RouteTarget> targets = targetRepo.findAllActive();

            if (targets.isEmpty()) {
                logger.debug("No active targets to health check");
                return;
            }

            logger.info("Performing health checks on {} targets", targets.size());

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (RouteTarget target : targets) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> checkTarget(target),
                        workerPool
                );
                futures.add(future);
            }

            // Wait for all checks to complete (with timeout)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(healthCheckTimeout * 2L, TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.error("Error during health check cycle", e);
        }
    }

    private void checkTarget(RouteTarget target) {
        try {
            // For public APIs, just check if the base URL is reachable
            // Don't append /health since most APIs don't have it
            String healthUrl = target.getTargetUrl();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(healthCheckTimeout))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Consider 2xx, 3xx, and even 404 as "healthy" (server is responding)
            // Only mark unhealthy on connection failures or 5xx errors
            if (response.statusCode() < 500) {
                handleHealthyResponse(target);  // FIXED: was handleHealthyTarget
            } else {
                logger.debug("Target {} returned server error: {}",
                        target.getTargetUrl(), response.statusCode());
                handleUnhealthyResponse(target);  // FIXED: was handleUnhealthyTarget
            }

        } catch (Exception e) {
            // Only mark unhealthy on actual connection failures
            logger.debug("Health check failed for {}: {}",
                    target.getTargetUrl(), e.getMessage());
            handleUnhealthyResponse(target);  // FIXED: was handleUnhealthyTarget
        }
    }

    private void handleHealthyResponse(RouteTarget target) {
        try {
            // Reset failure count
            targetRepo.resetFailureCount(target.getId());

            // If was unhealthy, mark as healthy
            if ("unhealthy".equals(target.getHealthStatus())) {
                targetRepo.updateHealthStatus(target.getId(), "healthy");
                logger.info("Target recovered: {}", target.getTargetUrl());
            } else if ("unknown".equals(target.getHealthStatus())) {
                targetRepo.updateHealthStatus(target.getId(), "healthy");
                logger.info("Target is healthy: {}", target.getTargetUrl());
            }

        } catch (Exception e) {
            logger.error("Error handling healthy response for target: {}",
                    target.getId(), e);
        }
    }

    private void handleUnhealthyResponse(RouteTarget target) {
        try {
            int failures = targetRepo.incrementFailureCount(target.getId());

            // Mark unhealthy after threshold
            if (failures >= unhealthyThreshold &&
                    !"unhealthy".equals(target.getHealthStatus())) {

                targetRepo.updateHealthStatus(target.getId(), "unhealthy");
                logger.warn("Target marked unhealthy after {} failures: {}",
                        failures, target.getTargetUrl());
            }

        } catch (Exception e) {
            logger.error("Error handling unhealthy response for target: {}",
                    target.getId(), e);
        }
    }

    public boolean isRunning() {
        return running;
    }
}