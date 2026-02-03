package com.soft40051.app.web.controller;

import com.soft40051.app.database.DB;
import com.soft40051.app.files.FileService;
import com.soft40051.app.files.FileMetadata;
import com.soft40051.app.files.FilePermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * File Management REST Controller
 * 
 * Handles file CRUD operations, upload, download, and sharing
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    /**
     * GET /api/files
     * List all files accessible to the current user
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listFiles(Authentication auth) {
        List<Map<String, Object>> files = new ArrayList<>();
        String username = auth != null ? auth.getName() : "anonymous";
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT f.id, f.filename, f.owner, f.created_at, f.updated_at, " +
                 "LENGTH(COALESCE(f.content, '')) as size " +
                 "FROM files f " +
                 "WHERE f.owner = ? OR EXISTS (" +
                 "  SELECT 1 FROM file_permissions fp WHERE fp.file_id = f.id AND fp.shared_with = ?" +
                 ") " +
                 "ORDER BY f.updated_at DESC")) {
            
            ps.setString(1, username);
            ps.setString(2, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> file = new HashMap<>();
                    file.put("id", rs.getInt("id"));
                    file.put("filename", rs.getString("filename"));
                    file.put("owner", rs.getString("owner"));
                    file.put("size", rs.getLong("size"));
                    file.put("uploadDate", rs.getTimestamp("created_at").toString());
                    file.put("modifiedDate", rs.getTimestamp("updated_at").toString());
                    file.put("type", getFileType(rs.getString("filename")));
                    files.add(file);
                }
            }
        } catch (Exception e) {
            System.err.println("[FileController] Error listing files: " + e.getMessage());
        }
        
        return ResponseEntity.ok(files);
    }

    /**
     * POST /api/files/upload
     * Upload a new file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        
        Map<String, Object> response = new HashMap<>();
        String username = auth != null ? auth.getName() : "anonymous";
        
        try {
            String filename = file.getOriginalFilename();
            String content = new String(file.getBytes());
            
            FileService.create(filename, content, username);
            
            // Get the file ID
            int fileId = getFileId(filename, username);
            
            response.put("success", true);
            response.put("fileId", fileId);
            response.put("filename", filename);
            response.put("size", file.getSize());
            response.put("chunksUploaded", 1);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * POST /api/files/create
     * Create a new text file
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createFile(
            @RequestBody Map<String, String> fileData,
            Authentication auth) {
        
        Map<String, Object> response = new HashMap<>();
        String username = auth != null ? auth.getName() : "anonymous";
        
        String filename = fileData.get("filename");
        String content = fileData.getOrDefault("content", "");
        
        if (filename == null || filename.isEmpty()) {
            response.put("success", false);
            response.put("message", "Filename is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            FileService.create(filename, content, username);
            int fileId = getFileId(filename, username);
            
            response.put("success", true);
            response.put("fileId", fileId);
            response.put("filename", filename);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/files/{id}
     * Get file details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFile(@PathVariable int id, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        String username = auth != null ? auth.getName() : "anonymous";
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT * FROM files WHERE id = ?")) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String owner = rs.getString("owner");
                    String filename = rs.getString("filename");
                    
                    // Check permission
                    if (!owner.equals(username) && !hasReadPermission(id, username)) {
                        response.put("success", false);
                        response.put("message", "Access denied");
                        return ResponseEntity.status(403).body(response);
                    }
                    
                    response.put("success", true);
                    response.put("id", id);
                    response.put("filename", filename);
                    response.put("owner", owner);
                    response.put("content", rs.getString("content"));
                    response.put("createdAt", rs.getTimestamp("created_at").toString());
                    response.put("updatedAt", rs.getTimestamp("updated_at").toString());
                    
                    return ResponseEntity.ok(response);
                }
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        response.put("success", false);
        response.put("message", "File not found");
        return ResponseEntity.status(404).body(response);
    }

    /**
     * DELETE /api/files/{id}
     * Delete a file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable int id, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        String username = auth != null ? auth.getName() : "anonymous";
        
        try {
            // Get filename and verify ownership
            String filename = getFilenameById(id, username);
            if (filename == null) {
                response.put("success", false);
                response.put("message", "File not found or access denied");
                return ResponseEntity.status(404).body(response);
            }
            
            FileService.delete(filename, username);
            response.put("success", true);
            response.put("message", "File deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * POST /api/files/{id}/share
     * Share a file with another user
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, Object>> shareFile(
            @PathVariable int id,
            @RequestBody Map<String, String> shareData,
            Authentication auth) {
        
        Map<String, Object> response = new HashMap<>();
        String username = auth != null ? auth.getName() : "anonymous";
        String shareWith = shareData.get("username");
        String permission = shareData.getOrDefault("permission", "READ");
        
        try {
            String filename = getFilenameById(id, username);
            if (filename == null) {
                response.put("success", false);
                response.put("message", "File not found or access denied");
                return ResponseEntity.status(404).body(response);
            }
            
            FilePermissions.grant(filename, shareWith, permission);
            response.put("success", true);
            response.put("message", "File shared with " + shareWith);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Helper methods
    private int getFileId(String filename, String owner) {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT id FROM files WHERE filename = ? AND owner = ?")) {
            ps.setString(1, filename);
            ps.setString(2, owner);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("[FileController] Error getting file ID: " + e.getMessage());
        }
        return -1;
    }

    private String getFilenameById(int id, String username) {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT filename FROM files WHERE id = ? AND owner = ?")) {
            ps.setInt(1, id);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("filename");
            }
        } catch (Exception e) {
            System.err.println("[FileController] Error getting filename: " + e.getMessage());
        }
        return null;
    }

    private boolean hasReadPermission(int fileId, String username) {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT 1 FROM file_permissions WHERE file_id = ? AND shared_with = ?")) {
            ps.setInt(1, fileId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String getFileType(String filename) {
        if (filename == null) return "other";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt")) {
            return "document";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")) {
            return "image";
        } else if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")) {
            return "video";
        }
        return "other";
    }
}
