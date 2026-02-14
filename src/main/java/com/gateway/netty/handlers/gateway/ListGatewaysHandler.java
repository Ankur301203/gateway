package com.gateway.netty.handlers.gateway;

import com.gateway.domain.Gateway;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.GatewayService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListGatewaysHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ListGatewaysHandler.class);
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
            List<Gateway> gateways = gatewayService.getUserGateways(userId);

            JsonArray array = new JsonArray();
            for (Gateway gateway : gateways) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", gateway.getId().toString());
                obj.addProperty("name", gateway.getName());
                obj.addProperty("description", gateway.getDescription());
                obj.addProperty("created_at", gateway.getCreatedAt().toString());
                array.add(obj);
            }

            JsonObject response = new JsonObject();
            response.add("gateways", array);
            response.addProperty("count", gateways.size());

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (Exception e) {
            logger.error("Error listing gateways", e);
            return internalErrorResponse("Failed to list gateways");
        }
    }
}