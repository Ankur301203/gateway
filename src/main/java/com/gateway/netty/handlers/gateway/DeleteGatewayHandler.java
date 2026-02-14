package com.gateway.netty.handlers.gateway;

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

public class DeleteGatewayHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(DeleteGatewayHandler.class);
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
            // Extract gateway ID from path
            String gatewayId = extractPathVariable(ctx.getPath(), 4);

            if (gatewayId == null) {
                return badRequestResponse("Invalid gateway ID");
            }

            UUID gatewayUuid = UUID.fromString(gatewayId);

            boolean deleted = gatewayService.deleteGateway(gatewayUuid, userId);

            if (!deleted) {
                return notFoundResponse("Gateway not found");
            }

            JsonObject response = new JsonObject();
            response.addProperty("message", "Gateway deleted successfully");

            logger.info("Gateway deleted: {} by user: {}", gatewayUuid, userId);

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            return badRequestResponse("Invalid gateway ID format");
        } catch (Exception e) {
            logger.error("Error deleting gateway", e);
            return internalErrorResponse("Failed to delete gateway");
        }
    }
}