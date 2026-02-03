package com.soft40051.app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Database Connection Manager - Enhanced with Schema Initialization
 */
public class DB {

    public static Connection connect() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try {
            // First try to connect directly to the database
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/cloudfs",
                "root",
                ""
            );
        } catch (Exception e) {
            // If database doesn't exist, connect to server and create it
            if (e.getMessage() != null && e.getMessage().contains("Unknown database")) {
                System.out.println("[DB] Database 'cloudfs' not found. Creating it...");
                try (Connection serverCon = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/", "root", "")) {
                    try (Statement stmt = serverCon.createStatement()) {
                        stmt.execute("CREATE DATABASE cloudfs");
                        System.out.println("[DB] Database 'cloudfs' created successfully.");
                    }
                }
                // Retry connection to the new database
                return DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/cloudfs",
                    "root",
                    ""
                );
            }
            throw e;
        }
    }
    
    /**
     * Initialize MySQL schema with sync-compatible tables
     * Should be called once during system initialization
     */
    public static void initializeSchema() throws Exception {
        try (Connection con = connect();
             Statement stmt = con.createStatement()) {
            
            // 1. Create users table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   username VARCHAR(50) NOT NULL UNIQUE," +
                "   password VARCHAR(255) NOT NULL," +
                "   role VARCHAR(20) NOT NULL," +
                "   created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // 2. Create event_logs table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS event_logs (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   username VARCHAR(50) NOT NULL," +
                "   event_type VARCHAR(50) NOT NULL," +
                "   description TEXT," +
                "   timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // 3. Create sessions table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   username VARCHAR(50) NOT NULL UNIQUE," +
                "   token VARCHAR(255) NOT NULL," +
                "   timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "   INDEX idx_username (username)," +
                "   INDEX idx_timestamp (timestamp)" +
                ")"
            );
            
            // 4. Create files table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS files (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   filename VARCHAR(255) NOT NULL," +
                "   owner VARCHAR(50) NOT NULL," +
                "   content LONGTEXT," +
                "   created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "   version INT DEFAULT 1," +
                "   UNIQUE KEY unique_file (filename, owner)," +
                "   INDEX idx_owner (owner)" +
                ")"
            );
            
            // 5. Create file_permissions table for sharing
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS file_permissions (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   file_id INT NOT NULL," +
                "   shared_with VARCHAR(50) NOT NULL," +
                "   permission VARCHAR(20) NOT NULL," +
                "   created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "   FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE," +
                "   UNIQUE KEY unique_permission (file_id, shared_with)," +
                "   INDEX idx_shared_with (shared_with)" +
                ")"
            );
            
            System.out.println("[OK] MySQL schema verified (users, event_logs, sessions, files, file_permissions)");
        }
    }
}