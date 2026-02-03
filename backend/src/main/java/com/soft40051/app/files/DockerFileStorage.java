package com.soft40051.app.files;

import java.io.*;

public class DockerFileStorage {

    private static final String CONTAINER = "soft40051-file-server";
    private static final String STORAGE_PATH = "/files/";

    // Upload file to Docker container
    public static void upload(String filename, String content) throws Exception {
        
        // Create temp file locally
        File tempFile = new File(filename);
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        // Copy to Docker container
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "cp", 
            filename, 
            CONTAINER + ":" + STORAGE_PATH + filename
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        // Clean up local temp file
        tempFile.delete();
        
        if (exitCode != 0) {
            throw new Exception("Failed to upload file to container");
        }
    }

    // Download file from Docker container
    public static String download(String filename) throws Exception {
        
        // Copy from Docker container to local
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "cp",
            CONTAINER + ":" + STORAGE_PATH + filename,
            filename
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new Exception("Failed to download file from container");
        }
        
        // Read file content
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        // Clean up local file
        new File(filename).delete();
        
        return content.toString();
    }

    // Delete file from Docker container
    public static void deleteFromContainer(String filename) throws Exception {
        
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "exec",
            CONTAINER,
            "rm", "-f",
            STORAGE_PATH + filename
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new Exception("Failed to delete file from container");
        }
    }
}