package com.soft40051.app.web.controller;

import com.soft40051.app.auth.AuthService;
import com.soft40051.app.database.DB;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * User Management REST Controller
 * 
 * Admin-only endpoints for managing users
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * GET /api/users
     * List all users (admin only)
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication auth) {
        List<Map<String, Object>> users = new ArrayList<>();
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT u.id, u.username, u.role, u.created_at, " +
                 "(SELECT COUNT(*) FROM files f WHERE f.owner = u.username) as files_owned, " +
                 "(SELECT MAX(timestamp) FROM event_logs e WHERE e.username = u.username AND e.event_type = 'LOGIN_SUCCESS') as last_login " +
                 "FROM users u ORDER BY u.created_at DESC")) {
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role"));
                    user.put("filesOwned", rs.getInt("files_owned"));
                    user.put("createdAt", rs.getTimestamp("created_at").toString());
                    
                    java.sql.Timestamp lastLogin = rs.getTimestamp("last_login");
                    user.put("lastLogin", lastLogin != null ? lastLogin.toString() : "Never");
                    
                    users.add(user);
                }
            }
        } catch (Exception e) {
            System.err.println("[UserController] Error listing users: " + e.getMessage());
        }
        
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id}
     * Get user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable int id) {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT * FROM users WHERE id = ?")) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    response.put("success", true);
                    response.put("id", rs.getInt("id"));
                    response.put("username", rs.getString("username"));
                    response.put("role", rs.getString("role"));
                    response.put("createdAt", rs.getTimestamp("created_at").toString());
                    return ResponseEntity.ok(response);
                }
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        response.put("success", false);
        response.put("message", "User not found");
        return ResponseEntity.status(404).body(response);
    }

    /**
     * POST /api/users/{id}/promote
     * Promote user to admin
     */
    @PostMapping("/{id}/promote")
    public ResponseEntity<Map<String, Object>> promoteUser(@PathVariable int id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = getUsernameById(id);
            if (username == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }
            
            AuthService.promoteToAdmin(username);
            response.put("success", true);
            response.put("message", "User promoted to admin");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * POST /api/users/{id}/demote
     * Demote admin to user
     */
    @PostMapping("/{id}/demote")
    public ResponseEntity<Map<String, Object>> demoteUser(@PathVariable int id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = getUsernameById(id);
            if (username == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }
            
            AuthService.demoteToUser(username);
            response.put("success", true);
            response.put("message", "User demoted to regular user");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * DELETE /api/users/{id}
     * Delete a user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable int id, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        String currentUser = auth != null ? auth.getName() : null;
        
        try {
            String username = getUsernameById(id);
            if (username == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }
            
            // Prevent self-deletion
            if (username.equals(currentUser)) {
                response.put("success", false);
                response.put("message", "Cannot delete yourself");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Prevent deleting admin user
            if (username.equals("admin")) {
                response.put("success", false);
                response.put("message", "Cannot delete the admin user");
                return ResponseEntity.badRequest().body(response);
            }
            
            AuthService.deleteUser(username);
            response.put("success", true);
            response.put("message", "User deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String getUsernameById(int id) {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        } catch (Exception e) {
            System.err.println("[UserController] Error getting username: " + e.getMessage());
        }
        return null;
    }
}
