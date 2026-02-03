package com.soft40051.app.files;

import com.soft40051.app.database.DB;
import com.soft40051.app.database.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileMetadata {

    public static void register(String filename, String owner) throws Exception {
        Connection con = DB.connect();
        
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO files (filename, owner) VALUES (?, ?)"
        );
        
        ps.setString(1, filename);
        ps.setString(2, owner);
        ps.executeUpdate();
        
        con.close();
        
        Logger.log(owner, "FILE_REGISTERED", "File metadata stored: " + filename);
    }

    public static void remove(String filename) throws Exception {
        Connection con = DB.connect();
        
        PreparedStatement ps = con.prepareStatement(
            "DELETE FROM files WHERE filename = ?"
        );
        
        ps.setString(1, filename);
        int rowsAffected = ps.executeUpdate();
        
        con.close();
        
        if (rowsAffected > 0) {
            Logger.log("SYSTEM", "FILE_DELETED", "File metadata removed: " + filename);
        }
    }

    public static boolean exists(String filename) throws Exception {
        Connection con = DB.connect();
        
        PreparedStatement ps = con.prepareStatement(
            "SELECT COUNT(*) FROM files WHERE filename = ?"
        );
        
        ps.setString(1, filename);
        ResultSet rs = ps.executeQuery();
        
        rs.next();
        boolean result = rs.getInt(1) > 0;
        
        con.close();
        return result;
    }

    public static String getOwner(String filename) throws Exception {
        Connection con = DB.connect();
        
        PreparedStatement ps = con.prepareStatement(
            "SELECT owner FROM files WHERE filename = ?"
        );
        
        ps.setString(1, filename);
        ResultSet rs = ps.executeQuery();
        
        String owner = null;
        if (rs.next()) {
            owner = rs.getString("owner");
        }
        
        con.close();
        return owner;
    }

    public static List<String> listByOwner(String owner) throws Exception {
        Connection con = DB.connect();
        
        PreparedStatement ps = con.prepareStatement(
            "SELECT filename FROM files WHERE owner = ?"
        );
        
        ps.setString(1, owner);
        ResultSet rs = ps.executeQuery();
        
        List<String> files = new ArrayList<>();
        while (rs.next()) {
            files.add(rs.getString("filename"));
        }
        
        con.close();
        return files;
    }

    public static List<String> listAll() throws Exception {
        Connection con = DB.connect();
        
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT filename, owner FROM files"
        );
        
        List<String> files = new ArrayList<>();
        while (rs.next()) {
            files.add(rs.getString("filename") + " (owner: " + rs.getString("owner") + ")");
        }
        
        con.close();
        return files;
    }
}