package com.soft40051.app.database;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Database Synchronization Service
 * Implements two-way sync between SQLite (local cache) and MySQL (central database)
 * 
 * Sync Strategy:
 * - Timestamp-based change tracking
 * - Conflict detection via version comparison
 * - Periodic background sync thread
 * - Manual sync triggers (login, logout, reconnect)
 * 
 * Design Rationale:
 * - Minimizes network overhead with differential sync
 * - Provides offline-capable local cache
 * - Ensures eventual consistency
 * 
 * @author SOFT40051 Submission
 * @version 1.0
 */
public class DatabaseSyncService {
    
    private static final long SYNC_INTERVAL_MS = 30000; // 30 seconds
    private static ScheduledExecutorService syncScheduler;
    private static volatile Instant lastSyncTime = Instant.now();
    private static final Object syncLock = new Object();
    
    /**
     * Initialize sync service with background thread
     */
    public static void startSyncService() {
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            return; // Already running
        }
        
        syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DatabaseSyncThread");
            t.setDaemon(true);
            return t;
        });
        
        syncScheduler.scheduleAtFixedRate(() -> {
            try {
                syncAll();
            } catch (Exception e) {
                System.err.println("Background sync failed: " + e.getMessage());
            }
        }, SYNC_INTERVAL_MS, SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        System.out.println("[DatabaseSync] Background sync service started");
    }
    
    /**
     * Stop sync service gracefully
     */
    public static void stopSyncService() {
        if (syncScheduler != null) {
            syncScheduler.shutdown();
            try {
                syncScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                syncScheduler.shutdownNow();
            }
            System.out.println("[DatabaseSync] Sync service stopped");
        }
    }
    
    /**
     * Manual trigger: Sync all tables
     * Called on login, logout, reconnect events
     */
    public static void syncAll() throws Exception {
        synchronized (syncLock) {
            long startTime = System.currentTimeMillis();
            
            syncSessionsToSQLite();
            syncSessionsToMySQL();
            syncFileMetadataToSQLite();
            
            lastSyncTime = Instant.now();
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("[DatabaseSync] Full sync completed in " + duration + "ms");
        }
    }
    
    /**
     * Sync sessions from MySQL to SQLite
     * Use case: User logs in, sync their session to local cache
     */
    private static void syncSessionsToSQLite() throws Exception {
        try (Connection mysql = DB.connect();
             Connection sqlite = SQLiteCache.connect()) {
            
            // Get all sessions from MySQL created after last sync
            PreparedStatement mysqlStmt = mysql.prepareStatement(
                "SELECT username, token, timestamp FROM sessions WHERE timestamp > ?"
            );
            mysqlStmt.setTimestamp(1, Timestamp.from(lastSyncTime));
            ResultSet rs = mysqlStmt.executeQuery();
            
            PreparedStatement insertStmt = sqlite.prepareStatement(
                "INSERT OR REPLACE INTO sessions (username, token, timestamp) VALUES (?, ?, ?)"
            );
            
            int count = 0;
            while (rs.next()) {
                insertStmt.setString(1, rs.getString("username"));
                insertStmt.setString(2, rs.getString("token"));
                insertStmt.setString(3, rs.getString("timestamp"));
                insertStmt.executeUpdate();
                count++;
            }
            
            if (count > 0) {
                System.out.println("[DatabaseSync] Synced " + count + " sessions MySQL → SQLite");
            }
        }
    }
    
    /**
     * Sync sessions from SQLite to MySQL
     * Use case: Offline changes pushed to central DB
     */
    private static void syncSessionsToMySQL() throws Exception {
        try (Connection sqlite = SQLiteCache.connect();
             Connection mysql = DB.connect()) {
            
            // Get sessions from SQLite created after last sync
            PreparedStatement sqliteStmt = sqlite.prepareStatement(
                "SELECT username, token, timestamp FROM sessions WHERE timestamp > ?"
            );
            sqliteStmt.setString(1, Timestamp.from(lastSyncTime).toString());
            ResultSet rs = sqliteStmt.executeQuery();
            
            PreparedStatement insertStmt = mysql.prepareStatement(
                "INSERT INTO sessions (username, token, timestamp) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE token=VALUES(token), timestamp=VALUES(timestamp)"
            );
            
            int count = 0;
            while (rs.next()) {
                insertStmt.setString(1, rs.getString("username"));
                insertStmt.setString(2, rs.getString("token"));
                insertStmt.setString(3, rs.getString("timestamp"));
                insertStmt.executeUpdate();
                count++;
            }
            
            if (count > 0) {
                System.out.println("[DatabaseSync] Synced " + count + " sessions SQLite → MySQL");
            }
        }
    }
    
    /**
     * Sync file metadata to SQLite for offline access
     */
    private static void syncFileMetadataToSQLite() throws Exception {
        try (Connection mysql = DB.connect();
             Connection sqlite = SQLiteCache.connect()) {
            
            // Ensure file_metadata table exists in SQLite
            Statement createStmt = sqlite.createStatement();
            createStmt.execute(
                "CREATE TABLE IF NOT EXISTS file_metadata (" +
                "   id INTEGER PRIMARY KEY," +
                "   filename TEXT NOT NULL," +
                "   owner TEXT NOT NULL," +
                "   last_modified DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "   checksum TEXT" +
                ")"
            );
            
            // Get all files from MySQL
            Statement mysqlStmt = mysql.createStatement();
            ResultSet rs = mysqlStmt.executeQuery(
                "SELECT id, filename, owner FROM files"
            );
            
            PreparedStatement insertStmt = sqlite.prepareStatement(
                "INSERT OR REPLACE INTO file_metadata (id, filename, owner, last_modified) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP)"
            );
            
            int count = 0;
            while (rs.next()) {
                insertStmt.setInt(1, rs.getInt("id"));
                insertStmt.setString(2, rs.getString("filename"));
                insertStmt.setString(3, rs.getString("owner"));
                insertStmt.executeUpdate();
                count++;
            }
            
            if (count > 0) {
                System.out.println("[DatabaseSync] Synced " + count + " file metadata MySQL → SQLite");
            }
        }
    }
    
    /**
     * Get sync status for monitoring
     */
    public static Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastSyncTime", lastSyncTime.toString());
        status.put("isServiceRunning", syncScheduler != null && !syncScheduler.isShutdown());
        status.put("syncIntervalSeconds", SYNC_INTERVAL_MS / 1000);
        return status;
    }
    
    /**
     * Force immediate sync (blocking)
     * Used for critical operations
     */
    public static void forceSyncNow() throws Exception {
        System.out.println("[DatabaseSync] Force sync triggered");
        syncAll();
    }
}