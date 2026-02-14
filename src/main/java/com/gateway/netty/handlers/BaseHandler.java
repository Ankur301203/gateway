package com.gateway.netty.handlers;

import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.service.AuthService;
import com.gateway.util.JsonUtil;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Optional;
import java.util.UUID;

public abstract class BaseHandler {
    protected final AuthService authService = new AuthService();

    public abstract FullHttpResponse handle(RequestContext ctx) throws Exception;

    protected Optional<UUID> authenticateRequest(RequestContext ctx) {
        String authHeader = ctx.getHeader("authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }

    protected FullHttpResponse unauthorizedResponse() {
        return ResponseBuilder.json(HttpResponseStatus.UNAUTHORIZED)
                .body(JsonUtil.createErrorResponse("Unauthorized - Invalid or missing token").toString())
                .build();
    }

    protected FullHttpResponse badRequestResponse(String message) {
        return ResponseBuilder.json(HttpResponseStatus.BAD_REQUEST)
                .body(JsonUtil.createErrorResponse(message).toString())
                .build();
    }

    protected FullHttpResponse notFoundResponse(String message) {
        return ResponseBuilder.json(HttpResponseStatus.NOT_FOUND)
                .body(JsonUtil.createErrorResponse(message).toString())
                .build();
    }

    protected FullHttpResponse internalErrorResponse(String message) {
        return ResponseBuilder.json(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                .body(JsonUtil.createErrorResponse(message).toString())
                .build();
    }

    protected String extractPathVariable(String path, int position) {
        String[] parts = path.split("/");
        if (parts.length > position) {
            return parts[position];
        }
        return null;
    }

    protected JsonObject parseJsonBody(RequestContext ctx) {
        try {
            return JsonUtil.parse(ctx.getBody());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON body");
        }
    }
}