package com.gateway.netty.handlers.gateway;

import com.gateway.domain.Gateway;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.GatewayService;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class GetGatewayHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetGatewayHandler.class);
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
            // Extract gateway ID from path: /api/v1/gateways/{id}
            String gatewayId = extractPathVariable(ctx.getPath(), 4);

            if (gatewayId == null) {
                return badRequestResponse("Invalid gateway ID");
            }

            UUID gatewayUuid = UUID.fromString(gatewayId);

            Optional<Gateway> gatewayOpt = gatewayService.getGateway(gatewayUuid, userId);

            if (gatewayOpt.isEmpty()) {
                return notFoundResponse("Gateway not found");
            }

            Gateway gateway = gatewayOpt.get();

            JsonObject response = new JsonObject();
            response.addProperty("id", gateway.getId().toString());
            response.addProperty("user_id", gateway.getUserId().toString());
            response.addProperty("name", gateway.getName());
            response.addProperty("description", gateway.getDescription());
            response.addProperty("created_at", gateway.getCreatedAt().toString());
            response.addProperty("updated_at", gateway.getUpdatedAt().toString());

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequestResponse("Invalid gateway ID format");
        } catch (Exception e) {
            logger.error("Error getting gateway", e);
            return internalErrorResponse("Failed to get gateway");
        }
    }
}