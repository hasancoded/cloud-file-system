package com.soft40051.app.files;

import com.soft40051.app.database.DB;
import com.soft40051.app.database.Logger;
import java.sql.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * File Partitioning and Aggregation Service
 * Implements chunked file storage with CRC32 integrity verification
 * 
 * Design:
 * - Fixed-size chunks (512KB default, configurable)
 * - CRC32 checksum per chunk for integrity
 * - Distributed storage across multiple containers (simulated)
 * - Metadata tracking in MySQL
 * - Transparent reassembly on read
 * 
 * Benefits:
 * - Parallel upload/download capability
 * - Fault tolerance (chunk-level redundancy possible)
 * - Efficient large file handling
 * - Integrity verification at chunk level
 * 
 * @author SOFT40051 Submission
 * @version 1.0
 */
public class FilePartitioner {
    
    // 512KB chunks (academic submission - smaller for demo purposes)
    private static final int CHUNK_SIZE = 512 * 1024; // 512KB
    
    // Simulated container names for distribution
    private static final String[] STORAGE_CONTAINERS = {
        "soft40051-file-server",      // Primary
        "soft40051-file-server-2",    // Secondary (simulated)
        "soft40051-file-server-3"     // Tertiary (simulated)
    };
    
    /**
     * File chunk metadata
     */
    public static class FileChunk {
        public int chunkId;
        public String filename;
        public int chunkIndex;
        public int chunkSize;
        public long crc32Checksum;
        public String storageContainer;
        public String chunkPath;
        
        public FileChunk(String filename, int chunkIndex, int chunkSize, 
                        long crc32, String container, String path) {
            this.filename = filename;
            this.chunkIndex = chunkIndex;
            this.chunkSize = chunkSize;
            this.crc32Checksum = crc32;
            this.storageContainer = container;
            this.chunkPath = path;
        }
    }
    
    /**
     * Initialize chunk metadata table
     */
    public static void initializePartitioning() throws Exception {
        try (Connection con = DB.connect();
             Statement stmt = con.createStatement()) {
            
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS file_chunks (" +
                "   id INT AUTO_INCREMENT PRIMARY KEY," +
                "   file_id INT NOT NULL," +
                "   filename VARCHAR(255) NOT NULL," +
                "   chunk_index INT NOT NULL," +
                "   chunk_size INT NOT NULL," +
                "   crc32_checksum BIGINT NOT NULL," +
                "   storage_container VARCHAR(100)," +
                "   chunk_path VARCHAR(500)," +
                "   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "   FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE," +
                "   UNIQUE KEY unique_chunk (file_id, chunk_index)," +
                "   INDEX idx_filename (filename)" +
                ")"
            );
            
            System.out.println("[FilePartitioner] Chunk metadata table initialized");
        }
    }
    
    /**
     * Partition file into chunks and store with checksums
     * @param filename Original filename
     * @param content File content
     * @param owner File owner
     * @return Number of chunks created
     */
    public static int partitionAndStore(String filename, String content, String owner) throws Exception {
        byte[] data = content.getBytes();
        int totalChunks = (int) Math.ceil((double) data.length / CHUNK_SIZE);
        
        // Get file ID from metadata
        int fileId = getFileId(filename);
        if (fileId == -1) {
            throw new Exception("File metadata not found: " + filename);
        }
        
        List<FileChunk> chunks = new ArrayList<>();
        
        // Create chunks
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, data.length);
            byte[] chunkData = Arrays.copyOfRange(data, start, end);
            
            // Calculate CRC32 checksum
            CRC32 crc = new CRC32();
            crc.update(chunkData);
            long checksum = crc.getValue();
            
            // Distribute chunks across containers (round-robin)
            String container = STORAGE_CONTAINERS[i % STORAGE_CONTAINERS.length];
            String chunkPath = "/files/" + filename + ".chunk" + i;
            
            // Store chunk to Docker (simulated for now - all go to primary)
            String chunkFilename = filename + ".chunk" + i;
            DockerFileStorage.upload(chunkFilename, new String(chunkData));
            
            FileChunk chunk = new FileChunk(
                filename, i, chunkData.length, checksum, container, chunkPath
            );
            chunks.add(chunk);
            
            // Store chunk metadata
            storeChunkMetadata(fileId, chunk);
        }
        
        Logger.log(owner, "FILE_PARTITIONED", 
            "File '" + filename + "' partitioned into " + totalChunks + " chunks");
        
        return totalChunks;
    }
    
    /**
     * Reassemble file from chunks with integrity verification
     * @param filename File to reassemble
     * @return Complete file content
     * @throws Exception if chunk missing or checksum mismatch
     */
    public static String reassembleAndVerify(String filename) throws Exception {
        List<FileChunk> chunks = getChunkMetadata(filename);
        
        if (chunks.isEmpty()) {
            throw new Exception("No chunks found for file: " + filename);
        }
        
        // Sort by chunk index
        chunks.sort(Comparator.comparingInt(c -> c.chunkIndex));
        
        StringBuilder content = new StringBuilder();
        
        for (FileChunk chunk : chunks) {
            // Download chunk
            String chunkFilename = filename + ".chunk" + chunk.chunkIndex;
            String chunkContent = DockerFileStorage.download(chunkFilename);
            
            // Verify CRC32 checksum
            CRC32 crc = new CRC32();
            crc.update(chunkContent.getBytes());
            long actualChecksum = crc.getValue();
            
            if (actualChecksum != chunk.crc32Checksum) {
                Logger.log("SYSTEM", "CHUNK_CORRUPTION", 
                    "Chunk " + chunk.chunkIndex + " of '" + filename + 
                    "' failed integrity check. Expected CRC32: " + chunk.crc32Checksum + 
                    ", Got: " + actualChecksum);
                throw new Exception("Chunk corruption detected in " + filename + 
                                  " (chunk " + chunk.chunkIndex + ")");
            }
            
            content.append(chunkContent);
        }
        
        return content.toString();
    }
    
    /**
     * Check if file is partitioned (has chunks)
     */
    public static boolean isPartitioned(String filename) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT COUNT(*) FROM file_chunks WHERE filename = ?")) {
            
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
    
    /**
     * Delete all chunks for a file
     */
    public static void deleteChunks(String filename) throws Exception {
        List<FileChunk> chunks = getChunkMetadata(filename);
        
        for (FileChunk chunk : chunks) {
            String chunkFilename = filename + ".chunk" + chunk.chunkIndex;
            try {
                DockerFileStorage.deleteFromContainer(chunkFilename);
            } catch (Exception e) {
                System.err.println("Failed to delete chunk: " + chunkFilename);
            }
        }
        
        // Delete metadata
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM file_chunks WHERE filename = ?")) {
            
            ps.setString(1, filename);
            ps.executeUpdate();
        }
        
        Logger.log("SYSTEM", "CHUNKS_DELETED", 
            "Deleted " + chunks.size() + " chunks for '" + filename + "'");
    }
    
    /**
     * Get chunk statistics for a file
     */
    public static Map<String, Object> getChunkStats(String filename) throws Exception {
        List<FileChunk> chunks = getChunkMetadata(filename);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChunks", chunks.size());
        stats.put("totalSize", chunks.stream().mapToInt(c -> c.chunkSize).sum());
        stats.put("avgChunkSize", chunks.isEmpty() ? 0 : 
                  chunks.stream().mapToInt(c -> c.chunkSize).average().orElse(0));
        
        // Container distribution
        Map<String, Long> distribution = new HashMap<>();
        for (FileChunk chunk : chunks) {
            distribution.merge(chunk.storageContainer, 1L, Long::sum);
        }
        stats.put("containerDistribution", distribution);
        
        return stats;
    }
    
    // ========== Helper Methods ==========
    
    private static void storeChunkMetadata(int fileId, FileChunk chunk) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO file_chunks (file_id, filename, chunk_index, chunk_size, " +
                 "crc32_checksum, storage_container, chunk_path) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            
            ps.setInt(1, fileId);
            ps.setString(2, chunk.filename);
            ps.setInt(3, chunk.chunkIndex);
            ps.setInt(4, chunk.chunkSize);
            ps.setLong(5, chunk.crc32Checksum);
            ps.setString(6, chunk.storageContainer);
            ps.setString(7, chunk.chunkPath);
            ps.executeUpdate();
        }
    }
    
    private static List<FileChunk> getChunkMetadata(String filename) throws Exception {
        List<FileChunk> chunks = new ArrayList<>();
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT chunk_index, chunk_size, crc32_checksum, storage_container, chunk_path " +
                 "FROM file_chunks WHERE filename = ? ORDER BY chunk_index")) {
            
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                chunks.add(new FileChunk(
                    filename,
                    rs.getInt("chunk_index"),
                    rs.getInt("chunk_size"),
                    rs.getLong("crc32_checksum"),
                    rs.getString("storage_container"),
                    rs.getString("chunk_path")
                ));
            }
        }
        
        return chunks;
    }
    
    private static int getFileId(String filename) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT id FROM files WHERE filename = ?")) {
            
            ps.setString(1, filename);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }
}