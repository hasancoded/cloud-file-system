package com.soft40051.app.auth;

import com.soft40051.app.database.DB;
import com.soft40051.app.database.Logger;
import com.soft40051.app.security.PasswordUtil;
import java.sql.*;

public class AuthService {

    public static boolean login(String username, String password) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT password FROM users WHERE username=?")) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Logger.log(username, "LOGIN_FAILED", "User not found");
                    return false;
                }
                
                boolean success = rs.getString(1).equals(PasswordUtil.hash(password));
                
                if (success) {
                    Logger.log(username, "LOGIN_SUCCESS", "User logged in successfully");
                } else {
                    Logger.log(username, "LOGIN_FAILED", "Incorrect password");
                }
                
                return success;
            }
        }
    }

    public static void createDefaultAdmin() throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement check = con.prepareStatement(
                     "SELECT * FROM users WHERE username='admin'");
             ResultSet rs = check.executeQuery()) {

            if (!rs.next()) {
                try (PreparedStatement insert = con.prepareStatement(
                        "INSERT INTO users (username,password,role) VALUES (?,?,?)")) {

                    insert.setString(1, "admin");
                    insert.setString(2, PasswordUtil.hash("admin"));
                    insert.setString(3, "ADMIN");
                    insert.executeUpdate();
                    
                    Logger.log("SYSTEM", "USER_CREATED", "Default admin account created");
                }
            }
        }
    }

    public static void register(String username, String password, String role) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement check = con.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE username = ?")) {

            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    Logger.log("SYSTEM", "REGISTRATION_FAILED", "Username already exists: " + username);
                    throw new Exception("Username already exists");
                }
            }

            try (PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {

                insert.setString(1, username);
                insert.setString(2, PasswordUtil.hash(password));
                insert.setString(3, role);
                insert.executeUpdate();
                
                Logger.log("SYSTEM", "USER_CREATED", "New user registered: " + username + " as " + role);
            }
        }
    }

    public static void deleteUser(String username) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM users WHERE username = ?")) {

            ps.setString(1, username);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                Logger.log("SYSTEM", "USER_DELETED", "User account deleted: " + username);
            } else {
                Logger.log("SYSTEM", "DELETE_FAILED", "User not found: " + username);
            }
        }
    }

    public static void updatePassword(String username, String newPassword) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET password = ? WHERE username = ?")) {

            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setString(2, username);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                Logger.log(username, "PASSWORD_UPDATED", "Password changed successfully");
            } else {
                Logger.log("SYSTEM", "UPDATE_FAILED", "User not found: " + username);
            }
        }
    }

    public static void promoteToAdmin(String username) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET role = 'ADMIN' WHERE username = ?")) {

            ps.setString(1, username);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                Logger.log("SYSTEM", "ROLE_CHANGED", "User promoted to ADMIN: " + username);
            } else {
                Logger.log("SYSTEM", "PROMOTION_FAILED", "User not found: " + username);
            }
        }
    }

    public static void demoteToUser(String username) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET role = 'USER' WHERE username = ?")) {

            ps.setString(1, username);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                Logger.log("SYSTEM", "ROLE_CHANGED", "User demoted to USER: " + username);
            } else {
                Logger.log("SYSTEM", "DEMOTION_FAILED", "User not found: " + username);
            }
        }
    }
    
    public static void logout(String username) {
        Logger.log(username, "LOGOUT", "User logged out");
    }
}