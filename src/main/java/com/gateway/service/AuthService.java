package com.gateway.service;

import com.gateway.domain.User;
import com.gateway.repository.UserRepository;
import com.gateway.util.JwtUtil;
import com.gateway.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository = new UserRepository();

    public User register(String email, String password) throws SQLException {
        // Validate email
        if (email == null || email.isBlank() || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }

        // Validate password
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash password
        String passwordHash = PasswordUtil.hashPassword(password);

        // Create user
        return userRepository.create(email, passwordHash);
    }

    public Map<String, String> login(String email, String password) throws SQLException {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userOpt.get();

        // Verify password
        if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Generate JWT token
        String token = JwtUtil.generateToken(user.getId(), user.getEmail());

        Map<String, String> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        response.put("expires_in", String.valueOf(3600)); // 1 hour

        logger.info("User logged in: {}", email);

        return response;
    }

    public Optional<UUID> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            if (JwtUtil.validateToken(token)) {
                UUID userId = JwtUtil.getUserIdFromToken(token);
                return Optional.of(userId);
            }
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}