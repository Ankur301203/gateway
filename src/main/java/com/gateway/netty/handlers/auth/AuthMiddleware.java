package com.gateway.netty.handlers.auth;

import com.gateway.http.RequestContext;
import com.gateway.service.AuthService;

import java.util.Optional;
import java.util.UUID;

public class AuthMiddleware {
    private static final AuthService authService = new AuthService();

    public static Optional<UUID> authenticate(RequestContext ctx) {
        String authHeader = ctx.getHeader("authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }

    public static boolean isAuthenticated(RequestContext ctx) {
        return authenticate(ctx).isPresent();
    }
}