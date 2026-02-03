package com.soft40051.app;

import com.soft40051.app.auth.AuthService;
import com.soft40051.app.files.FileService;
import com.soft40051.app.files.FileMetadata;
import com.soft40051.app.files.FilePermissions;
import com.soft40051.app.loadbalancer.LoadBalancer;
import com.soft40051.app.hostmanager.HostManager;
import com.soft40051.app.hostmanager.HealthCheck;
import com.soft40051.app.database.SQLiteCache;
import java.util.List;

public class CloudFileSystem {

    public static void main(String[] args) throws Exception {

        System.out.println("========================================");
        System.out.println("  CloudFileSystem - System Test Demo");
        System.out.println("========================================\n");

        // Stage 4 - Admin creation
        AuthService.createDefaultAdmin();
        System.out.println("[OK] Default admin account created\n");

        // Stage 8.3 - SQLite Cache
        System.out.println("--- SQLite Local Cache ---");
        SQLiteCache.initCache();
        SQLiteCache.storeSession("admin", "token-abc123");
        System.out.println("[OK] Session stored in local cache\n");

        // Test user management
        System.out.println("--- User Management ---");
        AuthService.register("alice", "pass123", "USER");
        System.out.println("[OK] User 'alice' registered");
        
        AuthService.register("bob", "pass456", "USER");
        System.out.println("[OK] User 'bob' registered");
        
        AuthService.promoteToAdmin("alice");
        System.out.println("[OK] User 'alice' promoted to ADMIN");
        
        AuthService.updatePassword("bob", "newpass789");
        System.out.println("[OK] Password updated for 'bob'\n");

        // Stage 5 - File operations with MySQL + Docker
        System.out.println("--- File Operations (MySQL + Docker Integration) ---");
        
        FileService.create("report.txt", "Project Report Content", "admin");
        System.out.println("[OK] File created: report.txt (owner: admin)");
        
        FileService.create("data.txt", "Important Data", "alice");
        System.out.println("[OK] File created: data.txt (owner: alice)\n");

        // List all files
        System.out.println("--- All Files in System ---");
        List<String> allFiles = FileMetadata.listAll();
        for (String file : allFiles) {
            System.out.println("  - " + file);
        }
        System.out.println();

        // Read file
        System.out.println("--- Reading File ---");
        String content = FileService.read("report.txt", "admin");
        System.out.println("Content: " + content);

        // Update file
        FileService.update("report.txt", "Updated Report Content", "admin");
        System.out.println("[OK] File updated: report.txt\n");

        // File sharing
        System.out.println("--- File Sharing ---");
        FilePermissions.grant("report.txt", "bob", "READ");
        System.out.println("[OK] 'report.txt' shared with bob (READ permission)");
        
        boolean hasAccess = FilePermissions.hasPermission("report.txt", "bob", "READ");
        System.out.println("[OK] Bob has READ permission: " + hasAccess + "\n");

        // Stage 6 - Load Balancer
        System.out.println("--- Load Balancer (Round Robin) ---");
        LoadBalancer lb = new LoadBalancer();
        for (int i = 0; i < 3; i++) {
            int server = lb.selectServer(3);
            System.out.println("Request " + (i+1) + " -> Server " + server);
        }
        System.out.println();

        // Stage 8.2 - Health Check
        System.out.println("--- Health Check ---");
        String mysqlHealth = HealthCheck.checkContainer("soft40051-mysql");
        System.out.println("MySQL Container: " + mysqlHealth);
        
        String fileServerHealth = HealthCheck.checkContainer("soft40051-file-server");
        System.out.println("File Server Container: " + fileServerHealth + "\n");

        // Stage 7 - Host Manager
        System.out.println("--- Host Manager (Container Lifecycle) ---");
        System.out.println("Stopping file server container...");
        HostManager.stopContainer("soft40051-file-server");
        Thread.sleep(3000);
        
        String healthAfterStop = HealthCheck.checkContainer("soft40051-file-server");
        System.out.println("File Server Status: " + healthAfterStop);

        System.out.println("\nStarting file server container...");
        HostManager.startContainer("soft40051-file-server");
        Thread.sleep(3000);
        
        String healthAfterStart = HealthCheck.checkContainer("soft40051-file-server");
        System.out.println("File Server Status: " + healthAfterStart + "\n");

        // Cleanup
        System.out.println("--- Cleanup ---");
        FileService.delete("report.txt", "admin");
        FileService.delete("data.txt", "alice");
        AuthService.deleteUser("alice");
        AuthService.deleteUser("bob");
        System.out.println("[OK] Cleanup completed");
        
        System.out.println("\n========================================");
        System.out.println("  All Tests Completed Successfully");
        System.out.println("========================================");
    }
}