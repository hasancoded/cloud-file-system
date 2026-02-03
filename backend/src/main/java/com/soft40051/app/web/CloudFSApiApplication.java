package com.soft40051.app.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot REST API Application
 * 
 * Runs alongside the JavaFX GUI on port 8080
 * Provides REST endpoints for the React dashboard
 * 
 * @author CloudFileSystem Team
 * @version 1.0
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = "com.soft40051.app")
public class CloudFSApiApplication {

    public static void main(String[] args) {
        // Initialize database schema before starting Spring Boot
        try {
            com.soft40051.app.database.DB.initializeSchema();
            System.out.println("[CloudFS API] Database schema initialized");
        } catch (Exception e) {
            System.err.println("[CloudFS API] Warning: Could not initialize database: " + e.getMessage());
        }
        
        SpringApplication.run(CloudFSApiApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  CloudFS REST API Started");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================\n");
    }
}
