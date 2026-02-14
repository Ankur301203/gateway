package com.gateway.netty.handlers.auth;

import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.AuthService;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LoginHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
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

            // Login
            Map<String, String> tokens = authService.login(email, password);

            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("access_token", tokens.get("access_token"));
            response.addProperty("token_type", tokens.get("token_type"));
            response.addProperty("expires_in", tokens.get("expires_in"));

            logger.info("User logged in: {}", email);

            return ResponseBuilder.json(HttpResponseStatus.OK)
                    .body(response.toString())
                    .build();

        } catch (IllegalArgumentException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseBuilder.json(HttpResponseStatus.UNAUTHORIZED)
                    .body("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            logger.error("Error during login", e);
            return internalErrorResponse("Login failed");
        }
    }
}