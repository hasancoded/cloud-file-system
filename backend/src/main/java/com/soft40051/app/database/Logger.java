package com.soft40051.app.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Logger {

    /**
     * Logs an event to the event_logs table.
     *
     * @param username    The user who triggered the event
     * @param eventType   Type of event (e.g., LOGIN, FILE_UPLOAD, PERMISSION_CHANGE)
     * @param description Optional description/details of the event
     */
    public static void log(String username, String eventType, String description) {
        String sql = "INSERT INTO event_logs (username, event_type, description) VALUES (?, ?, ?)";

        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, eventType);
            ps.setString(3, description);
            ps.executeUpdate();

        } catch (Exception e) {
            // Logging failure should not crash the app
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
}