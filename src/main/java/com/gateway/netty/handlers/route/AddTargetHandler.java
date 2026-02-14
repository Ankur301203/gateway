package com.gateway.netty.handlers.route;

import com.gateway.domain.RouteTarget;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.RouteService;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class AddTargetHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(AddTargetHandler.class);
    private final RouteService routeService = new RouteService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        // Authenticate
        Optional<UUID> userIdOpt = authenticateRequest(ctx);
        if (userIdOpt.isEmpty()) {
            return unauthorizedResponse();
        }

        try {
            // Extract route ID from path: /api/v1/routes/{id}/targets
            String routeId = extractPathVariable(ctx.getPath(), 4);

            if (routeId == null) {
                return badRequestResponse("Invalid route ID");
            }

            UUID routeUuid = UUID.fromString(routeId);

            // Verify route exists
            Optional<com.gateway.domain.Route> routeOpt = routeService.getRoute(routeUuid);
            if (routeOpt.isEmpty()) {
                return notFoundResponse("Route not found");
            }

            // Parse request body
            JsonObject json = parseJsonBody(ctx);

            if (!json.has("target_url")) {
                return badRequestResponse("Target URL is required");
            }

            String targetUrl = json.get("target_url").getAsString();
            Integer weight = json.has("weight") ? json.get("weight").getAsInt() : null;

            // Add target
            RouteTarget target = routeService.addTarget(routeUuid, targetUrl, weight);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("id", target.getId().toString());
            response.addProperty("route_id", target.getRouteId().toString());
            response.addProperty("target_url", target.getTargetUrl());
            response.addProperty("is_active", target.isActive());
            response.addProperty("health_status", target.getHealthStatus());
            response.addProperty("weight", target.getWeight());
            response.addProperty("created_at", target.getCreatedAt().toString());

            logger.info("Target added: {} for route: {}", targetUrl, routeUuid);

            return ResponseBuilder.json(HttpResponseStatus.CREATED)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Target creation failed: {}", e.getMessage());
            return badRequestResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error adding target", e);
            return internalErrorResponse("Failed to add target");
        }
    }
}