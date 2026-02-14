package com.gateway.netty.handlers.logs;

import com.gateway.domain.Gateway;
import com.gateway.domain.RequestLog;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.repository.LogRepository;
import com.gateway.service.GatewayService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetLogsHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetLogsHandler.class);
    private final LogRepository logRepository = new LogRepository();
    private final GatewayService gatewayService = new GatewayService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        // Authenticate
        Optional<UUID> userIdOpt = authenticateRequest(ctx);
        if (userIdOpt.isEmpty()) {
            return unauthorizedResponse();
        }

        UUID userId = userIdOpt.get();

        try {
            // Extract gateway ID from path: /api/v1/gateways/{id}/logs
            String gatewayId = extractPathVariable(ctx.getPath(), 4);

            if (gatewayId == null) {
                return badRequestResponse("Invalid gateway ID");
            }

            UUID gatewayUuid = UUID.fromString(gatewayId);

            // Verify gateway belongs to user
            Optional<Gateway> gatewayOpt = gatewayService.getGateway(gatewayUuid, userId);
            if (gatewayOpt.isEmpty()) {
                return notFoundResponse("Gateway not found");
            }

            // Parse query parameters
            int limit = 100; // default
            Integer statusCode = null;

            String limitParam = ctx.getQueryParam("limit");
            if (limitParam != null) {
                try {
                    limit = Integer.parseInt(limitParam);
                    if (limit < 1 || limit > 1000) {
                        limit = 100;
                    }
                } catch (NumberFormatException e) {
                    limit = 100;
                }
            }

            String statusParam = ctx.getQueryParam("status");
            if (statusParam != null) {
                try {
                    statusCode = Integer.parseInt(statusParam);
                } catch (NumberFormatException e) {
                    // Ignore invalid status code
                }
            }

            // Get logs
            List<RequestLog> logs = logRepository.findByGatewayId(gatewayUuid, limit, statusCode);

            // Build response
            JsonArray array = new JsonArray();
            for (RequestLog log : logs) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", log.getId());
                obj.addProperty("method", log.getMethod());
                obj.addProperty("path", log.getPath());
                obj.addProperty("status_code", log.getStatusCode());
                obj.addProperty("latency_ms", log.getLatencyMs());

                if (log.getRouteId() != null) {
                    obj.addProperty("route_id", log.getRouteId().toString());
                }

                if (log.getTargetId() != null) {
                    obj.addProperty("target_id", log.getTargetId().toString());
                }

                if (log.getErrorMessage() != null) {
                    obj.addProperty("error_message", log.getErrorMessage());
                }

                obj.addProperty("created_at", log.getCreatedAt().toString());
                array.add(obj);
            }

            JsonObject response = new JsonObject();
            response.add("logs", array);
            response.addProperty("count", logs.size());
            response.addProperty("limit", limit);

            if (statusCode != null) {
                response.addProperty("filter_status", statusCode);
            }

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequestResponse("Invalid gateway ID format");
        } catch (Exception e) {
            logger.error("Error getting logs", e);
            return internalErrorResponse("Failed to get logs");
        }
    }
}