package com.gateway.netty.handlers.route;

import com.gateway.domain.Gateway;
import com.gateway.domain.Route;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.GatewayService;
import com.gateway.service.RouteService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListRoutesHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListRoutesHandler.class);
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
            // Extract gateway ID
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

            List<Route> routes = routeService.getGatewayRoutes(gatewayUuid);

            JsonArray array = new JsonArray();
            for (Route route : routes) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", route.getId().toString());
                obj.addProperty("path", route.getPath());
                obj.addProperty("method", route.getMethod());
                obj.addProperty("timeout_ms", route.getTimeoutMs());
                obj.addProperty("created_at", route.getCreatedAt().toString());
                array.add(obj);
            }

            JsonObject response = new JsonObject();
            response.add("routes", array);
            response.addProperty("count", routes.size());

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (Exception e) {
            logger.error("Error listing routes", e);
            return internalErrorResponse("Failed to list routes");
        }
    }
}