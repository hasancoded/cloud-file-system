package com.soft40051.app.auth;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for AuthService
 * Tests user authentication, registration, and management
 * 
 * @author SOFT40051 Submission
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthServiceTest {
    
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass123";
    
    @BeforeAll
    static void setUp() throws Exception {
        // Ensure default admin exists
        AuthService.createDefaultAdmin();
        System.out.println("[Test] Setup completed");
    }
    
    @AfterAll
    static void tearDown() throws Exception {
        // Cleanup test user if exists
        try {
            AuthService.deleteUser(TEST_USERNAME);
        } catch (Exception e) {
            // Ignore if user doesn't exist
        }
        System.out.println("[Test] Teardown completed");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test: Admin Login Success")
    void testAdminLoginSuccess() throws Exception {
        boolean result = AuthService.login("admin", "admin");
        assertTrue(result, "Admin should be able to login with default credentials");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test: Admin Login Failure (Wrong Password)")
    void testAdminLoginFailure() throws Exception {
        boolean result = AuthService.login("admin", "wrongpassword");
        assertFalse(result, "Login should fail with incorrect password");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test: User Registration")
    void testUserRegistration() throws Exception {
        // Clean up first if exists
        try {
            AuthService.deleteUser(TEST_USERNAME);
        } catch (Exception e) {
            // Ignore
        }
        
        // Register new user
        assertDoesNotThrow(() -> {
            AuthService.register(TEST_USERNAME, TEST_PASSWORD, "USER");
        }, "User registration should succeed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test: Duplicate User Registration (Should Fail)")
    void testDuplicateRegistration() throws Exception {
        // Try to register same user again
        Exception exception = assertThrows(Exception.class, () -> {
            AuthService.register(TEST_USERNAME, TEST_PASSWORD, "USER");
        });
        
        assertTrue(exception.getMessage().contains("already exists"),
                   "Should throw exception for duplicate username");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test: User Login After Registration")
    void testUserLoginAfterRegistration() throws Exception {
        boolean result = AuthService.login(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(result, "Registered user should be able to login");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test: Promote User to Admin")
    void testPromoteToAdmin() throws Exception {
        assertDoesNotThrow(() -> {
            AuthService.promoteToAdmin(TEST_USERNAME);
        }, "User promotion should succeed");
        
        // Verify by checking role (would need getUserRole method)
        System.out.println("[Test] User promoted to ADMIN");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test: Demote Admin to User")
    void testDemoteToUser() throws Exception {
        assertDoesNotThrow(() -> {
            AuthService.demoteToUser(TEST_USERNAME);
        }, "User demotion should succeed");
        
        System.out.println("[Test] User demoted to USER");
    }
    
    @Test
    @Order(8)
    @DisplayName("Test: Update Password")
    void testUpdatePassword() throws Exception {
        String newPassword = "newpass456";
        
        assertDoesNotThrow(() -> {
            AuthService.updatePassword(TEST_USERNAME, newPassword);
        }, "Password update should succeed");
        
        // Verify new password works
        boolean loginResult = AuthService.login(TEST_USERNAME, newPassword);
        assertTrue(loginResult, "Should be able to login with new password");
        
        // Restore original password for other tests
        AuthService.updatePassword(TEST_USERNAME, TEST_PASSWORD);
    }
    
    @Test
    @Order(9)
    @DisplayName("Test: Delete User")
    void testDeleteUser() throws Exception {
        assertDoesNotThrow(() -> {
            AuthService.deleteUser(TEST_USERNAME);
        }, "User deletion should succeed");
        
        // Verify user no longer exists (login should fail)
        boolean loginResult = AuthService.login(TEST_USERNAME, TEST_PASSWORD);
        assertFalse(loginResult, "Deleted user should not be able to login");
    }
    
    @Test
    @Order(10)
    @DisplayName("Test: Login Non-Existent User")
    void testLoginNonExistentUser() throws Exception {
        boolean result = AuthService.login("nonexistent", "password");
        assertFalse(result, "Login should fail for non-existent user");
    }
}