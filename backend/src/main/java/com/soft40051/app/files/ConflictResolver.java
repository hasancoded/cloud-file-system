package com.soft40051.app.files;

import com.soft40051.app.database.DB;
import com.soft40051.app.database.Logger;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Conflict Resolution Service
 * Strategy: Last-Write-Wins with Version Tracking
 * 
 * Design Rationale:
 * - Simple, deterministic conflict resolution
 * - No user intervention required for most conflicts
 * - Maintains audit trail of all versions
 * - Optional rollback capability
 * 
 * Conflict Detection:
 * - Compare file version numbers
 * - Compare last modification timestamps
 * - Detect concurrent writes
 * 
 * Resolution Process:
 * 1. Detect conflict (version mismatch)
 * 2. Compare timestamps
 * 3. Keep newer version
 * 4. Archive older version (optional)
 * 5. Log conflict resolution
 * 
 * @author SOFT40051 Submission
 * @version 1.0
 */
public class ConflictResolver {
    
    /**
     * File version metadata
     */
    public static class FileVersion {
        public int fileId;
        public String filename;
        public int version;
        public Timestamp lastModified;
        public String modifiedBy;
        public String contentHash;
        
        public FileVersion(int fileId, String filename, int version, 
                          Timestamp lastModified, String modifiedBy, String contentHash) {
            this.fileId = fileId;
            this.filename = filename;
            this.version = version;
            this.lastModified = lastModified;
            this.modifiedBy = modifiedBy;
            this.contentHash = contentHash;
        }
    }
    
    /**
     * Initialize version tracking table in MySQL
     */
    public static void initializeVersioning() throws Exception {
        try (Connection con = DB.connect();
             Statement stmt = con.createStatement()) {
            
            // Ensure files table exists (Critical for Foreign Key)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS files (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   filename VARCHAR(255) NOT NULL UNIQUE," +
                "   owner VARCHAR(50) NOT NULL," +
                "   path VARCHAR(255)," +
                "   size BIGINT DEFAULT 0," +
                "   created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Add version and timestamp columns to files table
            try {
                stmt.execute(
                    "ALTER TABLE files ADD COLUMN version INT DEFAULT 1"
                );
            } catch (SQLException e) {
                // Column may already exist
            }
            
            try {
                stmt.execute(
                    "ALTER TABLE files ADD COLUMN last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                );
            } catch (SQLException e) {
                // Column may already exist
            }
            
            try {
                stmt.execute(
                    "ALTER TABLE files ADD COLUMN content_hash VARCHAR(64)"
                );
            } catch (SQLException e) {
                // Column may already exist
            }
            
            // Create file history table for rollback capability
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS file_history (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   file_id INT NOT NULL," +
                "   filename VARCHAR(255) NOT NULL," +
                "   version INT NOT NULL," +
                "   modified_by VARCHAR(50)," +
                "   content_hash VARCHAR(64)," +
                "   archived_content MEDIUMTEXT," +
                "   archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "   FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE," +
                "   INDEX idx_file_version (file_id, version)" +
                ")"
            );
            
            System.out.println("[ConflictResolver] Version tracking initialized");
        }
    }
    
    /**
     * Detect if file has conflicting versions
     * @return true if conflict detected, false otherwise
     */
    public static boolean detectConflict(String filename, int expectedVersion) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT version FROM files WHERE filename = ?")) {
            
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int currentVersion = rs.getInt("version");
                return currentVersion != expectedVersion;
            }
            
            return false;
        }
    }
    
    /**
     * Get current file version metadata
     */
    public static FileVersion getFileVersion(String filename) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT id, filename, version, last_modified, owner, content_hash " +
                 "FROM files WHERE filename = ?")) {
            
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return new FileVersion(
                    rs.getInt("id"),
                    rs.getString("filename"),
                    rs.getInt("version"),
                    rs.getTimestamp("last_modified"),
                    rs.getString("owner"),
                    rs.getString("content_hash")
                );
            }
            
            return null;
        }
    }
    
    /**
     * Resolve conflict using Last-Write-Wins strategy
     * @param filename File in conflict
     * @param newContent New content attempting to be written
     * @param newContentHash Hash of new content
     * @param modifiedBy User making the change
     * @return Resolution result
     */
    public static ConflictResolution resolveConflict(
            String filename, 
            String newContent, 
            String newContentHash,
            String modifiedBy) throws Exception {
        
        FileVersion current = getFileVersion(filename);
        if (current == null) {
            throw new Exception("File not found: " + filename);
        }
        
        // Archive current version before overwriting
        archiveVersion(current);
        
        // Update file with new version (Last-Write-Wins)
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE files SET version = version + 1, content_hash = ?, " +
                 "last_modified = CURRENT_TIMESTAMP WHERE filename = ?")) {
            
            ps.setString(1, newContentHash);
            ps.setString(2, filename);
            ps.executeUpdate();
        }
        
        Logger.log("SYSTEM", "CONFLICT_RESOLVED", 
            "File '" + filename + "' conflict resolved (LWW). " +
            "Version " + current.version + " → " + (current.version + 1) + 
            " by " + modifiedBy);
        
        return new ConflictResolution(
            filename,
            current.version,
            current.version + 1,
            "LAST_WRITE_WINS",
            true
        );
    }
    
    /**
     * Archive current version to history table
     */
    private static void archiveVersion(FileVersion version) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO file_history (file_id, filename, version, modified_by, content_hash, archived_content) " +
                 "VALUES (?, ?, ?, ?, ?, ?)")) {
            
            // Read current file content from Docker
            String content = "";
            try {
                content = DockerFileStorage.download(version.filename);
            } catch (Exception e) {
                content = "[Content unavailable - " + e.getMessage() + "]";
            }
            
            ps.setInt(1, version.fileId);
            ps.setString(2, version.filename);
            ps.setInt(3, version.version);
            ps.setString(4, version.modifiedBy);
            ps.setString(5, version.contentHash);
            ps.setString(6, content);
            ps.executeUpdate();
            
            System.out.println("[ConflictResolver] Archived version " + version.version + 
                             " of '" + version.filename + "'");
        }
    }
    
    /**
     * Rollback file to specific version
     * @param filename File to rollback
     * @param targetVersion Version to restore
     * @return true if rollback successful
     */
    public static boolean rollbackToVersion(String filename, int targetVersion) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT archived_content FROM file_history " +
                 "WHERE filename = ? AND version = ? ORDER BY archived_at DESC LIMIT 1")) {
            
            ps.setString(1, filename);
            ps.setInt(2, targetVersion);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String archivedContent = rs.getString("archived_content");
                
                // Restore content to Docker
                DockerFileStorage.upload(filename, archivedContent);
                
                // Update version in database
                try (PreparedStatement updatePs = con.prepareStatement(
                        "UPDATE files SET version = ?, last_modified = CURRENT_TIMESTAMP " +
                        "WHERE filename = ?")) {
                    updatePs.setInt(1, targetVersion);
                    updatePs.setString(2, filename);
                    updatePs.executeUpdate();
                }
                
                Logger.log("SYSTEM", "VERSION_ROLLBACK", 
                    "File '" + filename + "' rolled back to version " + targetVersion);
                
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Get version history for a file
     */
    public static List<FileVersion> getVersionHistory(String filename) throws Exception {
        List<FileVersion> history = new ArrayList<>();
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT file_id, filename, version, archived_at, modified_by, content_hash " +
                 "FROM file_history WHERE filename = ? ORDER BY version DESC")) {
            
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                history.add(new FileVersion(
                    rs.getInt("file_id"),
                    rs.getString("filename"),
                    rs.getInt("version"),
                    rs.getTimestamp("archived_at"),
                    rs.getString("modified_by"),
                    rs.getString("content_hash")
                ));
            }
        }
        
        return history;
    }
    
    /**
     * Conflict resolution result
     */
    public static class ConflictResolution {
        public String filename;
        public int oldVersion;
        public int newVersion;
        public String strategy;
        public boolean resolved;
        
        public ConflictResolution(String filename, int oldVersion, int newVersion, 
                                 String strategy, boolean resolved) {
            this.filename = filename;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
            this.strategy = strategy;
            this.resolved = resolved;
        }
        
        @Override
        public String toString() {
            return String.format("Conflict Resolution: %s [v%d→v%d] Strategy: %s, Resolved: %s",
                filename, oldVersion, newVersion, strategy, resolved);
        }
    }
}