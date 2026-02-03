package com.soft40051.app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SQLite Local Cache - Enhanced with Sync Support
 * Provides offline-capable local storage with MySQL synchronization
 */
public class SQLiteCache {

    private static final String DB_URL = "jdbc:sqlite:local.db";

    public static Connection connect() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initCache() throws Exception {
        Connection con = connect();
        Statement stmt = con.createStatement();
        
        // Sessions table with timestamp for sync tracking
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS sessions (" +
            "   id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "   username TEXT NOT NULL UNIQUE," +
            "   token TEXT NOT NULL," +
            "   timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        // File metadata cache for offline access
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS file_metadata (" +
            "   id INTEGER PRIMARY KEY," +
            "   filename TEXT NOT NULL," +
            "   owner TEXT NOT NULL," +
            "   last_modified DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "   checksum TEXT" +
            ")"
        );
        
        // Sync status tracking
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS sync_status (" +
            "   id INTEGER PRIMARY KEY CHECK (id = 1)," +
            "   last_sync_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "   sync_count INTEGER DEFAULT 0" +
            ")"
        );
        
        // Initialize sync status if not exists
        stmt.execute(
            "INSERT OR IGNORE INTO sync_status (id, sync_count) VALUES (1, 0)"
        );
        
        con.close();
        System.out.println("[OK] SQLite cache initialized with sync support");
    }

    public static void storeSession(String username, String token) throws Exception {
        Connection con = connect();
        
        PreparedStatement stmt = con.prepareStatement(
            "INSERT OR REPLACE INTO sessions (username, token, timestamp) VALUES (?, ?, CURRENT_TIMESTAMP)"
        );
        
        stmt.setString(1, username);
        stmt.setString(2, token);
        stmt.executeUpdate();
        
        con.close();
    }
    
    /**
     * Get session token from cache
     */
    public static String getSessionToken(String username) throws Exception {
        Connection con = connect();
        
        PreparedStatement stmt = con.prepareStatement(
            "SELECT token FROM sessions WHERE username = ?"
        );
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        
        String token = null;
        if (rs.next()) {
            token = rs.getString("token");
        }
        
        con.close();
        return token;
    }
    
    /**
     * Clear session from cache
     */
    public static void clearSession(String username) throws Exception {
        Connection con = connect();
        
        PreparedStatement stmt = con.prepareStatement(
            "DELETE FROM sessions WHERE username = ?"
        );
        stmt.setString(1, username);
        stmt.executeUpdate();
        
        con.close();
    }
    
    /**
     * Update sync status after successful sync
     */
    public static void updateSyncStatus() throws Exception {
        Connection con = connect();
        
        Statement stmt = con.createStatement();
        stmt.execute(
            "UPDATE sync_status SET last_sync_time = CURRENT_TIMESTAMP, sync_count = sync_count + 1 WHERE id = 1"
        );
        
        con.close();
    }
}