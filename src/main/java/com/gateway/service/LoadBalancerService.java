package com.gateway.service;

import com.gateway.domain.RouteTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LoadBalancerService {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerService.class);

    // Singleton instance
    private static final LoadBalancerService INSTANCE = new LoadBalancerService();

    // Route ID -> Counter
    private final ConcurrentHashMap<UUID, AtomicLong> counters = new ConcurrentHashMap<>();

    // Private constructor for singleton
    private LoadBalancerService() {}

    // Get singleton instance
    public static LoadBalancerService getInstance() {
        return INSTANCE;
    }

    public RouteTarget selectTarget(UUID routeId, List<RouteTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("No targets available");
        }

        logger.info("LoadBalancer: Received {} targets for route {}", targets.size(), routeId);
        for (int i = 0; i < targets.size(); i++) {
            logger.info("  Target {}: {}", i, targets.get(i).getTargetUrl());
        }

        if (targets.size() == 1) {
            logger.info("Only 1 target available, returning: {}", targets.get(0).getTargetUrl());
            return targets.get(0);
        }

        // Get or create counter for this route
        AtomicLong counter = counters.computeIfAbsent(routeId, k -> new AtomicLong(0));

        // Round-robin selection
        long currentCount = counter.getAndIncrement();
        long index = currentCount % targets.size();

        RouteTarget selected = targets.get((int) index);

        logger.info("LoadBalancer: Counter={}, Index={}, Selected={}",
                currentCount, index, selected.getTargetUrl());

        return selected;
    }

    public void resetCounter(UUID routeId) {
        counters.remove(routeId);
        logger.debug("Reset load balancer counter for route: {}", routeId);
    }

    public long getCounterValue(UUID routeId) {
        AtomicLong counter = counters.get(routeId);
        return counter != null ? counter.get() : 0;
    }
}