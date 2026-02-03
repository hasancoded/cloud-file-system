package com.soft40051.app.gui;

public class SessionManager {
    private static String currentUsername;
    private static String currentRole;

    public static void login(String username, String role) {
        currentUsername = username;
        currentRole = role;
    }

    public static void logout() {
        currentUsername = null;
        currentRole = null;
    }

    public static String getUsername() {
        return currentUsername;
    }

    public static String getRole() {
        return currentRole;
    }

    public static boolean isLoggedIn() {
        return currentUsername != null;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(currentRole);
    }
}