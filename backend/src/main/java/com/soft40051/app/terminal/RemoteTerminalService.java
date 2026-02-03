package com.soft40051.app.terminal;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.Properties;

/**
 * Remote Terminal Emulation Service
 * Implements SSH-based command execution in Docker containers
 * 
 * Features:
 * - SSH connection via JSch
 * - Execute commands in running Docker containers
 * - Secure authentication
 * - Session management
 * 
 * Use Cases:
 * - Remote container management
 * - File operations in distributed storage
 * - Health checks and diagnostics
 * 
 * Note: Docker containers must have SSH server installed
 * Setup: docker exec <container> apt-get install openssh-server
 * 
 * @author SOFT40051 Submission
 * @version 1.0
 */
public class RemoteTerminalService {
    
    private static final int SSH_PORT = 22;
    private static final int COMMAND_TIMEOUT = 10000; // 10 seconds
    
    /**
     * SSH Session wrapper
     */
    public static class SSHSession {
        private Session session;
        private String host;
        private int port;
        private String username;
        
        public SSHSession(Session session, String host, int port, String username) {
            this.session = session;
            this.host = host;
            this.port = port;
            this.username = username;
        }
        
        public Session getSession() {
            return session;
        }
        
        public void disconnect() {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        
        public boolean isConnected() {
            return session != null && session.isConnected();
        }
    }
    
    /**
     * Connect to Docker container via SSH
     * 
     * @param host Container IP or hostname (usually "localhost" with port forwarding)
     * @param port SSH port (default 22, or mapped port)
     * @param username SSH username
     * @param password SSH password
     * @return SSH session
     */
    public static SSHSession connect(String host, int port, String username, String password) 
            throws Exception {
        
        JSch jsch = new JSch();
        
        // Disable strict host key checking (for demo purposes - insecure in production)
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig(config);
        
        // Connect with timeout
        session.connect(COMMAND_TIMEOUT);
        
        System.out.println("[RemoteTerminal] Connected to " + host + ":" + port);
        
        return new SSHSession(session, host, port, username);
    }
    
    /**
     * Execute command on remote container
     * 
     * @param sshSession Active SSH session
     * @param command Command to execute
     * @return Command result
     */
    public static TerminalService.CommandResult executeRemote(SSHSession sshSession, String command) {
        if (!sshSession.isConnected()) {
            return new TerminalService.CommandResult(false, "", "Not connected", -1);
        }
        
        try {
            Channel channel = sshSession.getSession().openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            
            // Read output
            InputStream in = channel.getInputStream();
            channel.connect();
            
            StringBuilder output = new StringBuilder();
            byte[] tmp = new byte[1024];
            
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            int exitCode = channel.getExitStatus();
            channel.disconnect();
            
            return new TerminalService.CommandResult(
                exitCode == 0,
                output.toString(),
                "",
                exitCode
            );
            
        } catch (Exception e) {
            return new TerminalService.CommandResult(
                false,
                "",
                "SSH execution error: " + e.getMessage(),
                -1
            );
        }
    }
    
    /**
     * Execute command in Docker container via docker exec (alternative to SSH)
     * More practical for this project since containers may not have SSH
     * 
     * @param containerName Docker container name
     * @param command Command to execute
     * @return Command result
     */
    public static TerminalService.CommandResult executeInContainer(String containerName, String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerName, "sh", "-c", command
            );
            
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
            boolean finished = process.waitFor(COMMAND_TIMEOUT, 
                                              java.util.concurrent.TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new TerminalService.CommandResult(false, "", "Command timeout", -1);
            }
            
            int exitCode = process.exitValue();
            return new TerminalService.CommandResult(
                exitCode == 0,
                output.toString(),
                error.toString(),
                exitCode
            );
            
        } catch (Exception e) {
            return new TerminalService.CommandResult(
                false,
                "",
                "Docker exec error: " + e.getMessage(),
                -1
            );
        }
    }
    
    /**
     * List files in Docker container
     */
    public static TerminalService.CommandResult listFilesInContainer(String containerName, String path) {
        return executeInContainer(containerName, "ls -la " + path);
    }
    
    /**
     * Read file content from Docker container
     */
    public static TerminalService.CommandResult catFileInContainer(String containerName, String filepath) {
        return executeInContainer(containerName, "cat " + filepath);
    }
    
    /**
     * Delete file from Docker container
     */
    public static TerminalService.CommandResult deleteFileInContainer(String containerName, String filepath) {
        return executeInContainer(containerName, "rm -f " + filepath);
    }
    
    /**
     * Get container health status via remote check
     */
    public static TerminalService.CommandResult checkContainerHealth(String containerName) {
        return executeInContainer(containerName, "echo 'HEALTHY'");
    }
    
    /**
     * Copy file to Docker container (via docker cp)
     */
    public static TerminalService.CommandResult copyToContainer(
            String localPath, String containerName, String remotePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "cp", localPath, containerName + ":" + remotePath
            );
            
            Process process = pb.start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT, 
                                              java.util.concurrent.TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new TerminalService.CommandResult(false, "", "Timeout", -1);
            }
            
            int exitCode = process.exitValue();
            return new TerminalService.CommandResult(
                exitCode == 0,
                "File copied successfully",
                "",
                exitCode
            );
            
        } catch (Exception e) {
            return new TerminalService.CommandResult(false, "", e.getMessage(), -1);
        }
    }
    
    /**
     * Copy file from Docker container (via docker cp)
     */
    public static TerminalService.CommandResult copyFromContainer(
            String containerName, String remotePath, String localPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "cp", containerName + ":" + remotePath, localPath
            );
            
            Process process = pb.start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT, 
                                              java.util.concurrent.TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return new TerminalService.CommandResult(false, "", "Timeout", -1);
            }
            
            int exitCode = process.exitValue();
            return new TerminalService.CommandResult(
                exitCode == 0,
                "File copied successfully",
                "",
                exitCode
            );
            
        } catch (Exception e) {
            return new TerminalService.CommandResult(false, "", e.getMessage(), -1);
        }
    }
}