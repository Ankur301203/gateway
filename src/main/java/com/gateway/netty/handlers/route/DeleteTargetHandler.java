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

public class DeleteTargetHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(DeleteTargetHandler.class);
    private final RouteService routeService = new RouteService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        // Authenticate
        Optional<UUID> userIdOpt = authenticateRequest(ctx);
        if (userIdOpt.isEmpty()) {
            return unauthorizedResponse();
        }

        try {
            // Extract target ID from path: /api/v1/targets/{id}
            String targetId = extractPathVariable(ctx.getPath(), 4);

            if (targetId == null) {
                return badRequestResponse("Invalid target ID");
            }

            UUID targetUuid = UUID.fromString(targetId);

            boolean deleted = routeService.deleteTarget(targetUuid);

            if (!deleted) {
                return notFoundResponse("Target not found");
            }

            JsonObject response = new JsonObject();
            response.addProperty("message", "Target deleted successfully");

            logger.info("Target deleted: {}", targetUuid);

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequestResponse("Invalid target ID format");
        } catch (Exception e) {
            logger.error("Error deleting target", e);
            return internalErrorResponse("Failed to delete target");
        }
    }
}