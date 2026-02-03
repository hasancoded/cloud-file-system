package com.soft40051.app.files;

import com.soft40051.app.database.DB;
import com.soft40051.app.database.Logger;
import java.sql.*;

public class FilePermissions {

    // Grant permission to a user for a file by filename
    public static void grant(String filename, String username, String permission) throws Exception {
        int fileId = getFileId(filename);
        if (fileId == -1) {
            Logger.log("SYSTEM", "PERMISSION_FAILED", "File not found: " + filename);
            throw new Exception("File not found: " + filename);
        }

        Connection con = DB.connect();

        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO file_permissions (file_id, shared_with, permission) VALUES (?, ?, ?)"
        );
        ps.setInt(1, fileId);
        ps.setString(2, username);
        ps.setString(3, permission);
        ps.executeUpdate();

        con.close();
        
        Logger.log("SYSTEM", "PERMISSION_GRANTED", 
            "File '" + filename + "' shared with " + username + " (" + permission + ")");
    }

    // Revoke a user's permission for a file by filename
    public static void revoke(String filename, String username) throws Exception {
        int fileId = getFileId(filename);
        if (fileId == -1) {
            Logger.log("SYSTEM", "REVOKE_FAILED", "File not found: " + filename);
            throw new Exception("File not found: " + filename);
        }

        Connection con = DB.connect();

        PreparedStatement ps = con.prepareStatement(
            "DELETE FROM file_permissions WHERE file_id = ? AND shared_with = ?"
        );
        ps.setInt(1, fileId);
        ps.setString(2, username);
        int rowsAffected = ps.executeUpdate();

        con.close();
        
        if (rowsAffected > 0) {
            Logger.log("SYSTEM", "PERMISSION_REVOKED", 
                "Permission revoked for " + username + " on file '" + filename + "'");
        } else {
            Logger.log("SYSTEM", "REVOKE_FAILED", "No permission found to revoke");
        }
    }

    // Check if a user has a specific permission for a file
    public static boolean hasPermission(String filename, String username, String permission) throws Exception {
        int fileId = getFileId(filename);
        if (fileId == -1) {
            return false;
        }

        Connection con = DB.connect();

        PreparedStatement ps = con.prepareStatement(
            "SELECT COUNT(*) FROM file_permissions WHERE file_id = ? AND shared_with = ? AND permission = ?"
        );
        ps.setInt(1, fileId);
        ps.setString(2, username);
        ps.setString(3, permission);

        ResultSet rs = ps.executeQuery();
        rs.next();
        boolean result = rs.getInt(1) > 0;

        con.close();
        return result;
    }

    // Helper method to get file ID from filename
    private static int getFileId(String filename) throws Exception {
        Connection con = DB.connect();
        PreparedStatement ps = con.prepareStatement(
            "SELECT id FROM files WHERE filename = ?"
        );
        ps.setString(1, filename);
        ResultSet rs = ps.executeQuery();
        int id = -1;
        if (rs.next()) {
            id = rs.getInt("id");
        }
        con.close();
        return id;
    }
}