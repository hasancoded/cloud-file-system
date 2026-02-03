package com.soft40051.app.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class VerifySQLite {

    public static void main(String[] args) throws Exception {
        
        Connection con = SQLiteCache.connect();
        Statement stmt = con.createStatement();
        
        ResultSet rs = stmt.executeQuery("SELECT * FROM sessions");
        
        System.out.println("\n--- Sessions in SQLite ---");
        while (rs.next()) {
            System.out.println("ID: " + rs.getInt("id"));
            System.out.println("Username: " + rs.getString("username"));
            System.out.println("Token: " + rs.getString("token"));
            System.out.println("Timestamp: " + rs.getString("timestamp"));
            System.out.println("---");
        }
        
        con.close();
    }
}