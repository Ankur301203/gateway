package com.gateway.netty.handlers.route;

import com.gateway.domain.RouteTarget;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
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

public class ListTargetsHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListTargetsHandler.class);
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

            List<RouteTarget> targets = routeService.getRouteTargets(routeUuid);

            JsonArray array = new JsonArray();
            for (RouteTarget target : targets) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", target.getId().toString());
                obj.addProperty("target_url", target.getTargetUrl());
                obj.addProperty("is_active", target.isActive());
                obj.addProperty("health_status", target.getHealthStatus());
                obj.addProperty("consecutive_failures", target.getConsecutiveFailures());
                obj.addProperty("weight", target.getWeight());

                if (target.getLastHealthCheck() != null) {
                    obj.addProperty("last_health_check", target.getLastHealthCheck().toString());
                }

                obj.addProperty("created_at", target.getCreatedAt().toString());
                array.add(obj);
            }

            JsonObject response = new JsonObject();
            response.add("targets", array);
            response.addProperty("count", targets.size());

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (Exception e) {
            logger.error("Error listing targets", e);
            return internalErrorResponse("Failed to list targets");
        }
    }
}