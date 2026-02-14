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

public class CreateGatewayHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateGatewayHandler.class);
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
            // Parse request body
            JsonObject json = parseJsonBody(ctx);

            if (!json.has("name")) {
                return badRequestResponse("Gateway name is required");
            }

            String name = json.get("name").getAsString();
            String description = json.has("description") ? json.get("description").getAsString() : null;

            // Create gateway
            Gateway gateway = gatewayService.createGateway(userId, name, description);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("id", gateway.getId().toString());
            response.addProperty("user_id", gateway.getUserId().toString());
            response.addProperty("name", gateway.getName());
            response.addProperty("description", gateway.getDescription());
            response.addProperty("created_at", gateway.getCreatedAt().toString());

            logger.info("Gateway created: {} by user: {}", gateway.getId(), userId);

            return ResponseBuilder.json(HttpResponseStatus.CREATED)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Gateway creation failed: {}", e.getMessage());
            return badRequestResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating gateway", e);
            return internalErrorResponse("Failed to create gateway");
        }
    }
}