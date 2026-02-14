package com.gateway.netty.handlers.route;

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

public class DeleteRouteHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(DeleteRouteHandler.class);
    private final RouteService routeService = new RouteService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        // Authenticate
        Optional<UUID> userIdOpt = authenticateRequest(ctx);
        if (userIdOpt.isEmpty()) {
            return unauthorizedResponse();
        }

        try {
            // Extract route ID
            String routeId = extractPathVariable(ctx.getPath(), 4);

            if (routeId == null) {
                return badRequestResponse("Invalid route ID");
            }

            UUID routeUuid = UUID.fromString(routeId);

            boolean deleted = routeService.deleteRoute(routeUuid);

            if (!deleted) {
                return notFoundResponse("Route not found");
            }

            JsonObject response = new JsonObject();
            response.addProperty("message", "Route deleted successfully");

            logger.info("Route deleted: {}", routeUuid);

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequestResponse("Invalid route ID format");
        } catch (Exception e) {
            logger.error("Error deleting route", e);
            return internalErrorResponse("Failed to delete route");
        }
    }
}