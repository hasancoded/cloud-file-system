package com.soft40051.app.hostmanager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HealthCheck {

    public static String checkContainer(String name) throws Exception {
        
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "inspect", 
            "--format={{.State.Running}}", 
            name
        );
        
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        
        String output = reader.readLine();
        process.waitFor();
        
        if ("true".equals(output)) {
            return "HEALTHY";
        } else {
            return "UNHEALTHY";
        }
    }
}