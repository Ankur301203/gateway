package com.gateway.netty.handlers.route;

import com.gateway.domain.Route;
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

public class GetRouteHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetRouteHandler.class);
    private final RouteService routeService = new RouteService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        // Authenticate
        Optional<UUID> userIdOpt = authenticateRequest(ctx);
        if (userIdOpt.isEmpty()) {
            return unauthorizedResponse();
        }

        try {
            // Extract route ID from path: /api/v1/routes/{id}
            String routeId = extractPathVariable(ctx.getPath(), 4);

            if (routeId == null) {
                return badRequestResponse("Invalid route ID");
            }

            UUID routeUuid = UUID.fromString(routeId);

            Optional<Route> routeOpt = routeService.getRoute(routeUuid);

            if (routeOpt.isEmpty()) {
                return notFoundResponse("Route not found");
            }

            Route route = routeOpt.get();

            JsonObject response = new JsonObject();
            response.addProperty("id", route.getId().toString());
            response.addProperty("gateway_id", route.getGatewayId().toString());
            response.addProperty("path", route.getPath());
            response.addProperty("method", route.getMethod());
            response.addProperty("timeout_ms", route.getTimeoutMs());
            response.addProperty("created_at", route.getCreatedAt().toString());

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequestResponse("Invalid route ID format");
        } catch (Exception e) {
            logger.error("Error getting route", e);
            return internalErrorResponse("Failed to get route");
        }
    }
}