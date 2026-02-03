package com.soft40051.app.web.controller;

import com.soft40051.app.auth.AuthService;
import com.soft40051.app.database.DB;
import com.soft40051.app.web.config.JwtAuthFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication REST Controller
 * 
 * Handles login, register, and logout operations
 * Returns JWT tokens for authenticated sessions
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtAuthFilter jwtAuthFilter;

    public AuthController(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();
        
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            response.put("success", false);
            response.put("message", "Username and password required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean authenticated = AuthService.login(username, password);
            
            if (authenticated) {
                // Get user role
                String role = getUserRole(username);
                String token = jwtAuthFilter.generateToken(username, role);
                
                response.put("success", true);
                response.put("token", token);
                response.put("username", username);
                response.put("role", role);
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid credentials");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Authentication error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * POST /api/auth/register
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userData) {
        Map<String, Object> response = new HashMap<>();
        
        String username = userData.get("username");
        String password = userData.get("password");
        String role = userData.getOrDefault("role", "USER");

        if (username == null || password == null) {
            response.put("success", false);
            response.put("message", "Username and password required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            AuthService.register(username, password, role);
            response.put("success", true);
            response.put("message", "User registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/auth/logout
     * Logout user (client should discard token)
     */
    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtAuthFilter.extractUsername(token);
                AuthService.logout(username);
            } catch (Exception e) {
                // Token might be invalid, but logout is still successful
            }
        }
        
        response.put("success", true);
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me
     * Get current user info from token
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtAuthFilter.extractUsername(token);
                String role = getUserRole(username);
                
                response.put("success", true);
                response.put("username", username);
                response.put("role", role);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Invalid token");
                return ResponseEntity.status(401).body(response);
            }
        }
        
        response.put("success", false);
        response.put("message", "No token provided");
        return ResponseEntity.status(401).body(response);
    }

    private String getUserRole(String username) {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement("SELECT role FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (Exception e) {
            System.err.println("[AuthController] Error getting user role: " + e.getMessage());
        }
        return "USER";
    }
}
