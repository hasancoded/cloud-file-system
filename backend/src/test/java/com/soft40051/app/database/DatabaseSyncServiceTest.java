package com.soft40051.app.database;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

/**
 * Unit Tests for Database Synchronization Service
 * Tests sync operations, status tracking, and error handling
 * 
 * @author SOFT40051 Submission
 */
public class DatabaseSyncServiceTest {
    
    @BeforeAll
    static void setUp() throws Exception {
        // Initialize databases
        SQLiteCache.initCache();
        DB.initializeSchema();
        System.out.println("[Test] Databases initialized");
    }
    
    @AfterAll
    static void tearDown() {
        DatabaseSyncService.stopSyncService();
        System.out.println("[Test] Sync service stopped");
    }
    
    @Test
    @DisplayName("Test: Start Sync Service")
    void testStartSyncService() {
        assertDoesNotThrow(() -> {
            DatabaseSyncService.startSyncService();
        }, "Starting sync service should not throw exception");
        
        Map<String, Object> status = DatabaseSyncService.getSyncStatus();
        assertTrue((boolean)status.get("isServiceRunning"), 
                  "Sync service should be running");
    }
    
    @Test
    @DisplayName("Test: Stop Sync Service")
    void testStopSyncService() {
        DatabaseSyncService.startSyncService();
        
        assertDoesNotThrow(() -> {
            DatabaseSyncService.stopSyncService();
        }, "Stopping sync service should not throw exception");
        
        // Service should stop gracefully
        System.out.println("[Test] Service stopped successfully");
    }
    
    @Test
    @DisplayName("Test: Manual Sync Trigger")
    void testManualSyncTrigger() {
        assertDoesNotThrow(() -> {
            DatabaseSyncService.forceSyncNow();
        }, "Manual sync should complete without errors");
    }
    
    @Test
    @DisplayName("Test: Sync Status Reporting")
    void testSyncStatusReporting() {
        Map<String, Object> status = DatabaseSyncService.getSyncStatus();
        
        assertNotNull(status, "Status should not be null");
        assertTrue(status.containsKey("lastSyncTime"), 
                  "Status should contain lastSyncTime");
        assertTrue(status.containsKey("isServiceRunning"), 
                  "Status should contain isServiceRunning");
        assertTrue(status.containsKey("syncIntervalSeconds"), 
                  "Status should contain syncIntervalSeconds");
        
        System.out.println("[Test] Sync status: " + status);
    }
    
    @Test
    @DisplayName("Test: SQLite Session Storage")
    void testSQLiteSessionStorage() throws Exception {
        String testUsername = "synctest";
        String testToken = "token123";
        
        // Store session in SQLite
        assertDoesNotThrow(() -> {
            SQLiteCache.storeSession(testUsername, testToken);
        }, "Storing session should succeed");
        
        // Retrieve session
        String retrievedToken = SQLiteCache.getSessionToken(testUsername);
        assertEquals(testToken, retrievedToken, "Retrieved token should match");
        
        // Cleanup
        SQLiteCache.clearSession(testUsername);
    }
    
    @Test
    @DisplayName("Test: Session Cleanup")
    void testSessionCleanup() throws Exception {
        String testUsername = "cleanuptest";
        String testToken = "token456";
        
        // Store and then clear
        SQLiteCache.storeSession(testUsername, testToken);
        SQLiteCache.clearSession(testUsername);
        
        // Should return null after cleanup
        String retrievedToken = SQLiteCache.getSessionToken(testUsername);
        assertNull(retrievedToken, "Token should be null after cleanup");
    }
    
    @Test
    @DisplayName("Test: Sync Status Update")
    void testSyncStatusUpdate() throws Exception {
        assertDoesNotThrow(() -> {
            SQLiteCache.updateSyncStatus();
        }, "Updating sync status should succeed");
    }
    
    @Test
    @DisplayName("Test: Multiple Sync Calls")
    void testMultipleSyncCalls() {
        // Should handle multiple sync calls gracefully
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                DatabaseSyncService.forceSyncNow();
                Thread.sleep(100); // Small delay between syncs
            }
        }, "Multiple sync calls should not cause errors");
    }
}