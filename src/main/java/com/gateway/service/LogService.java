package com.gateway.service;

import com.gateway.config.AppConfig;
import com.gateway.domain.RequestLog;
import com.gateway.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    private static final LogService INSTANCE = new LogService();

    private final BlockingQueue<RequestLog> logQueue;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final LogRepository logRepo = new LogRepository();

    private final int batchSize;
    private final int flushInterval;

    private volatile boolean running = false;

    private LogService() {
        this.batchSize = AppConfig.getLogBatchSize();
        this.flushInterval = AppConfig.getLogFlushInterval();
        this.logQueue = new LinkedBlockingQueue<>(AppConfig.getLogBufferSize());
    }

    public static LogService getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (running) {
            logger.warn("Log service already running");
            return;
        }

        running = true;

        scheduler.scheduleAtFixedRate(
                this::flushLogs,
                flushInterval,
                flushInterval,
                TimeUnit.SECONDS
        );

        logger.info("Log service started (batch size: {}, flush interval: {}s)",
                batchSize, flushInterval);
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        logger.info("Stopping log service");

        // Flush remaining logs
        flushLogs();

        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Log service stopped");
    }

    public void logAsync(UUID gatewayId, UUID routeId, UUID targetId,
                         String method, String path, int statusCode,
                         int latencyMs, String errorMessage) {

        RequestLog log = new RequestLog(
                gatewayId, routeId, targetId,
                method, path, statusCode, latencyMs, errorMessage
        );

        boolean added = logQueue.offer(log);

        if (!added) {
            logger.warn("Log queue full, dropping log entry for gateway: {}", gatewayId);
        }
    }

    private void flushLogs() {
        List<RequestLog> batch = new ArrayList<>(batchSize);
        logQueue.drainTo(batch, batchSize);

        if (batch.isEmpty()) {
            return;
        }

        try {
            logRepo.batchInsert(batch);
            logger.debug("Flushed {} request logs", batch.size());
        } catch (Exception e) {
            logger.error("Error flushing logs", e);

            // Try to re-queue logs on failure (if space available)
            for (RequestLog log : batch) {
                logQueue.offer(log);
            }
        }
    }

    public int getQueueSize() {
        return logQueue.size();
    }

    public boolean isRunning() {
        return running;
    }
}