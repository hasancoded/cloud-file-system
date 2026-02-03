package com.soft40051.app.terminal;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Local Terminal Emulation Service
 * Implements secure shell command execution
 * 
 * Supported Commands:
 * - ls: List directory contents
 * - cp: Copy files
 * - mv: Move/rename files
 * - rm: Remove files
 * - cat: Display file content
 * 
 * Security Features:
 * - Command whitelist
 * - Path validation (no directory traversal)
 * - Argument sanitization
 * - Execution timeout
 * 
 * @author SOFT40051 Submission
 * @version 1.0
 */
public class TerminalService {
    
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "ls", "cp", "mv", "rm", "cat", "pwd", "echo"
    );
    
    private static final long COMMAND_TIMEOUT_MS = 10000; // 10 seconds
    private static final String WORKING_DIRECTORY = System.getProperty("user.dir");
    
    /**
     * Command execution result
     */
    public static class CommandResult {
        public boolean success;
        public String output;
        public String error;
        public int exitCode;
        
        public CommandResult(boolean success, String output, String error, int exitCode) {
            this.success = success;
            this.output = output;
            this.error = error;
            this.exitCode = exitCode;
        }
        
        @Override
        public String toString() {
            return success ? output : "Error: " + error;
        }
    }
    
    /**
     * Execute terminal command with security validation
     * @param command Full command string (e.g., "ls -l /files")
     * @return Command execution result
     */
    public static CommandResult executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new CommandResult(false, "", "Empty command", -1);
        }
        
        // Parse command
        String[] parts = command.trim().split("\\s+");
        String baseCommand = parts[0].toLowerCase();
        
        // Validate command is allowed
        if (!ALLOWED_COMMANDS.contains(baseCommand)) {
            return new CommandResult(false, "", 
                "Command not allowed: " + baseCommand + ". Allowed: " + ALLOWED_COMMANDS, -1);
        }
        
        // Validate arguments (prevent injection)
        for (int i = 1; i < parts.length; i++) {
            if (!isValidArgument(parts[i])) {
                return new CommandResult(false, "", 
                    "Invalid argument: " + parts[i], -1);
            }
        }
        
        // Execute based on OS
        try {
            if (isWindows()) {
                return executeWindows(parts);
            } else {
                return executeUnix(parts);
            }
        } catch (Exception e) {
            return new CommandResult(false, "", e.getMessage(), -1);
        }
    }
    
    /**
     * Execute command on Windows
     */
    private static CommandResult executeWindows(String[] args) throws Exception {
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "ls":
                return executeWindowsCommand("cmd", "/c", "dir", args.length > 1 ? args[1] : ".");
            case "cat":
                if (args.length < 2) {
                    return new CommandResult(false, "", "Usage: cat <filename>", -1);
                }
                return executeWindowsCommand("cmd", "/c", "type", args[1]);
            case "cp":
                if (args.length < 3) {
                    return new CommandResult(false, "", "Usage: cp <source> <dest>", -1);
                }
                return executeWindowsCommand("cmd", "/c", "copy", args[1], args[2]);
            case "mv":
                if (args.length < 3) {
                    return new CommandResult(false, "", "Usage: mv <source> <dest>", -1);
                }
                return executeWindowsCommand("cmd", "/c", "move", args[1], args[2]);
            case "rm":
                if (args.length < 2) {
                    return new CommandResult(false, "", "Usage: rm <filename>", -1);
                }
                return executeWindowsCommand("cmd", "/c", "del", args[1]);
            case "pwd":
                return new CommandResult(true, WORKING_DIRECTORY, "", 0);
            case "echo":
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                return new CommandResult(true, message, "", 0);
            default:
                return new CommandResult(false, "", "Command not implemented", -1);
        }
    }
    
    /**
     * Execute command on Unix/Linux/Mac
     */
    private static CommandResult executeUnix(String[] args) throws Exception {
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "ls":
                return executeUnixCommand("ls", args.length > 1 ? 
                    new String[]{"ls", "-la", args[1]} : new String[]{"ls", "-la"});
            case "cat":
                if (args.length < 2) {
                    return new CommandResult(false, "", "Usage: cat <filename>", -1);
                }
                return executeUnixCommand("cat", new String[]{"cat", args[1]});
            case "cp":
                if (args.length < 3) {
                    return new CommandResult(false, "", "Usage: cp <source> <dest>", -1);
                }
                return executeUnixCommand("cp", new String[]{"cp", args[1], args[2]});
            case "mv":
                if (args.length < 3) {
                    return new CommandResult(false, "", "Usage: mv <source> <dest>", -1);
                }
                return executeUnixCommand("mv", new String[]{"mv", args[1], args[2]});
            case "rm":
                if (args.length < 2) {
                    return new CommandResult(false, "", "Usage: rm <filename>", -1);
                }
                return executeUnixCommand("rm", new String[]{"rm", args[1]});
            case "pwd":
                return new CommandResult(true, WORKING_DIRECTORY, "", 0);
            case "echo":
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                return new CommandResult(true, message, "", 0);
            default:
                return new CommandResult(false, "", "Command not implemented", -1);
        }
    }
    
    /**
     * Execute Windows command via ProcessBuilder
     */
    private static CommandResult executeWindowsCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(WORKING_DIRECTORY));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Wait with timeout
        boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, 
                                          java.util.concurrent.TimeUnit.MILLISECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            return new CommandResult(false, "", "Command timeout", -1);
        }
        
        int exitCode = process.exitValue();
        return new CommandResult(exitCode == 0, output.toString(), "", exitCode);
    }
    
    /**
     * Execute Unix command via ProcessBuilder
     */
    private static CommandResult executeUnixCommand(String baseCmd, String[] fullCommand) 
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.directory(new File(WORKING_DIRECTORY));
        pb.redirectErrorStream(false);
        
        Process process = pb.start();
        
        // Read stdout
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Read stderr
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        
        // Wait with timeout
        boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, 
                                          java.util.concurrent.TimeUnit.MILLISECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            return new CommandResult(false, "", "Command timeout", -1);
        }
        
        int exitCode = process.exitValue();
        return new CommandResult(exitCode == 0, output.toString(), 
                                error.toString(), exitCode);
    }
    
    /**
     * Validate argument (prevent injection attacks)
     */
    private static boolean isValidArgument(String arg) {
        // Reject dangerous characters
        if (arg.contains(";") || arg.contains("|") || arg.contains("&") ||
            arg.contains(">") || arg.contains("<") || arg.contains("`") ||
            arg.contains("$") || arg.contains("\\n") || arg.contains("\\r")) {
            return false;
        }
        
        // Reject path traversal attempts
        if (arg.contains("..")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if running on Windows
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    /**
     * Get allowed commands
     */
    public static Set<String> getAllowedCommands() {
        return new HashSet<>(ALLOWED_COMMANDS);
    }
}