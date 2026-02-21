package com.gateway.service;

import com.gateway.config.AppConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitService {
    private static final RateLimitService INSTANCE = new RateLimitService();

    private final boolean enabled;
    private final int ratePerSecond;
    private final int burstCapacity;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private RateLimitService() {
        this.enabled = Boolean.parseBoolean(AppConfig.get("rate.limit.enabled", "true"));
        int configuredRate = AppConfig.getInt("rate.limit.rps", 50);
        int configuredBurst = AppConfig.getInt("rate.limit.burst", 100);

        this.ratePerSecond = Math.max(1, configuredRate);
        this.burstCapacity = Math.max(this.ratePerSecond, configuredBurst);
    }

    public static RateLimitService getInstance() {
        return INSTANCE;
    }

    public RateLimitResult check(UUID gatewayId, UUID routeId, String clientKey) {
        if (!enabled) {
            return RateLimitResult.disabled();
        }

        String key = gatewayId + ":" + routeId + ":" + clientKey;
        TokenBucket bucket = buckets.computeIfAbsent(
                key, k -> new TokenBucket(ratePerSecond, burstCapacity)
        );
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int ratePerSecond;
        private final int capacity;
        private double tokens;
        private long lastRefillNanos;

        private TokenBucket(int ratePerSecond, int capacity) {
            this.ratePerSecond = ratePerSecond;
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized RateLimitResult tryConsume() {
            refill();
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                int remaining = (int) Math.floor(tokens);
                return RateLimitResult.allowed(capacity, remaining, 0);
            }

            int resetSeconds = computeResetSeconds();
            return RateLimitResult.denied(capacity, 0, resetSeconds);
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0d;
            if (elapsedSeconds <= 0.0d) {
                return;
            }
            tokens = Math.min(capacity, tokens + elapsedSeconds * ratePerSecond);
            lastRefillNanos = now;
        }

        private int computeResetSeconds() {
            if (ratePerSecond <= 0) {
                return 1;
            }
            double deficit = 1.0d - tokens;
            if (deficit <= 0.0d) {
                return 0;
            }
            return (int) Math.ceil(deficit / ratePerSecond);
        }
    }

    public static class RateLimitResult {
        public final boolean enabled;
        public final boolean allowed;
        public final int limit;
        public final int remaining;
        public final int resetSeconds;

        private RateLimitResult(boolean enabled, boolean allowed, int limit,
                                int remaining, int resetSeconds) {
            this.enabled = enabled;
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = remaining;
            this.resetSeconds = resetSeconds;
        }

        public static RateLimitResult disabled() {
            return new RateLimitResult(false, true, 0, 0, 0);
        }

        public static RateLimitResult allowed(int limit, int remaining, int resetSeconds) {
            return new RateLimitResult(true, true, limit, remaining, resetSeconds);
        }

        public static RateLimitResult denied(int limit, int remaining, int resetSeconds) {
            return new RateLimitResult(true, false, limit, remaining, resetSeconds);
        }

        public Map<String, String> toHeaders() {
            if (!enabled) {
                return Map.of();
            }
            return Map.of(
                    "X-RateLimit-Limit", String.valueOf(limit),
                    "X-RateLimit-Remaining", String.valueOf(remaining),
                    "X-RateLimit-Reset", String.valueOf(resetSeconds)
            );
        }
    }
}
