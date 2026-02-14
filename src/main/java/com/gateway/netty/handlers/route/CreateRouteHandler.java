package com.gateway.netty.handlers.route;

import com.gateway.domain.Gateway;
import com.gateway.domain.Route;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.GatewayService;
import com.gateway.service.RouteService;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class CreateRouteHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateRouteHandler.class);
    private final RouteService routeService = new RouteService();
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
            // Extract gateway ID from path: /api/v1/gateways/{id}/routes
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

            // Parse request body
            JsonObject json = parseJsonBody(ctx);

            if (!json.has("path") || !json.has("method")) {
                return badRequestResponse("Path and method are required");
            }

            String path = json.get("path").getAsString();
            String method = json.get("method").getAsString();
            Integer timeoutMs = json.has("timeout_ms") ? json.get("timeout_ms").getAsInt() : null;

            // Create route
            Route route = routeService.createRoute(gatewayUuid, path, method, timeoutMs);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("id", route.getId().toString());
            response.addProperty("gateway_id", route.getGatewayId().toString());
            response.addProperty("path", route.getPath());
            response.addProperty("method", route.getMethod());
            response.addProperty("timeout_ms", route.getTimeoutMs());
            response.addProperty("created_at", route.getCreatedAt().toString());

            logger.info("Route created: {} {} for gateway: {}", method, path, gatewayUuid);

            return ResponseBuilder.json(HttpResponseStatus.CREATED)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Route creation failed: {}", e.getMessage());
            return badRequestResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating route", e);
            return internalErrorResponse("Failed to create route");
        }
    }
}