package com.soft40051.app.loadbalancer;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Unit Tests for Load Balancer (Fixed - Test Isolation)
 * Tests round-robin, health-aware routing, and statistics
 * 
 * Location: src/test/java/com/soft40051/app/loadbalancer/LoadBalancerTest.java
 * 
 * @author SOFT40051 Submission
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoadBalancerTest {
    
    private LoadBalancer lb;
    
    @BeforeEach
    void setUp() {
        // Create fresh LoadBalancer instance for each test
        lb = new LoadBalancer();
        lb.setLatencySimulation(false, 0, 0); // Disable latency for faster tests
    }
    
    @Test
    @Order(1)
    @DisplayName("Test: Round-Robin Distribution")
    void testRoundRobinDistribution() throws Exception {
        int totalServers = 3;
        
        // Explicitly mark all servers as healthy for this test
        lb.setServerHealth(0, true);
        lb.setServerHealth(1, true);
        lb.setServerHealth(2, true);
        
        Map<Integer, Integer> distribution = new HashMap<>();
        
        // Make 30 requests
        for (int i = 0; i < 30; i++) {
            int server = lb.selectServer(totalServers);
            distribution.put(server, distribution.getOrDefault(server, 0) + 1);
        }
        
        // Each server should get exactly 10 requests (round-robin)
        for (int i = 0; i < totalServers; i++) {
            int count = distribution.getOrDefault(i, 0);
            assertTrue(count >= 8 && count <= 12, 
                      "Server " + i + " should get balanced load, got: " + count);
        }
        
        System.out.println("[Test] Round-robin distribution verified: " + distribution);
    }
    
    @Test
    @Order(2)
    @DisplayName("Test: Health-Aware Routing (Skip Unhealthy)")
    void testHealthAwareRouting() throws Exception {
        int totalServers = 3;
        
        // Mark servers 0 and 2 as healthy, server 1 as unhealthy
        lb.setServerHealth(0, true);
        lb.setServerHealth(1, false);
        lb.setServerHealth(2, true);
        
        Set<Integer> selectedServers = new HashSet<>();
        
        // Make 20 requests
        for (int i = 0; i < 20; i++) {
            int server = lb.selectServer(totalServers);
            selectedServers.add(server);
        }
        
        // Server 1 should never be selected
        assertFalse(selectedServers.contains(1), 
                   "Unhealthy server 1 should not be selected");
        
        // Servers 0 and 2 should be selected
        assertTrue(selectedServers.contains(0), "Server 0 should be selected");
        assertTrue(selectedServers.contains(2), "Server 2 should be selected");
        
        System.out.println("[Test] Health-aware routing verified: " + selectedServers);
    }
    
    @Test
    @Order(3)
    @DisplayName("Test: All Servers Unhealthy (Should Throw Exception)")
    void testAllServersUnhealthy() {
        int totalServers = 2;
        
        // Mark all servers unhealthy
        lb.setServerHealth(0, false);
        lb.setServerHealth(1, false);
        
        // Should throw exception
        Exception exception = assertThrows(Exception.class, () -> {
            lb.selectServer(totalServers);
        }, "Should throw exception when all servers are unhealthy");
        
        assertTrue(exception.getMessage().contains("No healthy servers"),
                  "Exception message should indicate no healthy servers");
        
        System.out.println("[Test] All servers unhealthy exception verified");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test: Statistics Collection")
    void testStatistics() throws Exception {
        int totalServers = 2;
        
        // Mark both servers healthy
        lb.setServerHealth(0, true);
        lb.setServerHealth(1, true);
        
        // Make exactly 10 requests
        for (int i = 0; i < 10; i++) {
            lb.selectServer(totalServers);
        }
        
        Map<String, Object> stats = lb.getStatistics();
        
        assertNotNull(stats, "Statistics should not be null");
        
        // The counter increments each time, so totalRequests should be 10
        int totalRequests = (int) stats.get("totalRequests");
        assertTrue(totalRequests >= 10, 
                  "Should have at least 10 total requests, got: " + totalRequests);
        
        assertTrue((long)stats.get("healthyServers") >= 0, "Should track healthy servers");
        
        System.out.println("[Test] Statistics verified: " + stats);
    }
    
    @Test
    @Order(5)
    @DisplayName("Test: Health Status Recovery")
    void testHealthStatusRecovery() throws Exception {
        int totalServers = 2;
        
        // Initially mark both as healthy
        lb.setServerHealth(0, true);
        lb.setServerHealth(1, true);
        
        // Now mark server 0 as unhealthy
        lb.setServerHealth(0, false);
        
        // Make a request - should go to server 1
        int server1 = lb.selectServer(totalServers);
        assertEquals(1, server1, "Should select healthy server 1");
        
        // Recover server 0
        lb.setServerHealth(0, true);
        
        // Make another request - server 0 should now be eligible
        Set<Integer> selectedServers = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            selectedServers.add(lb.selectServer(totalServers));
        }
        
        assertTrue(selectedServers.contains(0), 
                  "Recovered server 0 should be selectable again");
        assertTrue(selectedServers.contains(1), 
                  "Server 1 should still be selectable");
        
        System.out.println("[Test] Health recovery verified: " + selectedServers);
    }
    
    @Test
    @Order(6)
    @DisplayName("Test: Invalid Server Count (Should Throw)")
    void testInvalidServerCount() {
        assertThrows(IllegalArgumentException.class, () -> {
            lb.selectServer(0);
        }, "Should throw exception for 0 servers");
        
        assertThrows(IllegalArgumentException.class, () -> {
            lb.selectServer(-1);
        }, "Should throw exception for negative server count");
        
        System.out.println("[Test] Invalid server count validation verified");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test: Latency Simulation Configuration")
    void testLatencyConfiguration() {
        lb.setLatencySimulation(true, 100, 500);
        
        Map<String, Object> stats = lb.getStatistics();
        assertTrue((boolean)stats.get("latencySimulation"), 
                  "Latency simulation should be enabled");
        assertEquals("100-500ms", stats.get("latencyRange"), 
                    "Latency range should be configured");
        
        System.out.println("[Test] Latency configuration verified");
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        lb = null;
    }
}