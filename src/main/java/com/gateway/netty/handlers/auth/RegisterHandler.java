package com.gateway.netty.handlers.auth;

import com.gateway.domain.User;
import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.AuthService;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(RegisterHandler.class);
    private final AuthService authService = new AuthService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        try {
            // Parse request body
            JsonObject json = parseJsonBody(ctx);

            if (!json.has("email") || !json.has("password")) {
                return badRequestResponse("Email and password are required");
            }

            String email = json.get("email").getAsString();
            String password = json.get("password").getAsString();

            // Register user
            User user = authService.register(email, password);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("id", user.getId().toString());
            response.addProperty("email", user.getEmail());
            response.addProperty("created_at", user.getCreatedAt().toString());

            logger.info("User registered: {}", email);

            return ResponseBuilder.json(HttpResponseStatus.CREATED)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return badRequestResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error during registration", e);
            return internalErrorResponse("Registration failed");
        }
    }
}