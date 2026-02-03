package com.soft40051.app.web.controller;

import com.soft40051.app.database.DB;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server-Sent Events Controller for Real-Time Activity Feed
 * 
 * Streams live updates to the React dashboard
 */
@RestController
@RequestMapping("/api/stream")
public class ActivityStreamController {

    // Store active emitters
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    
    // Scheduled executor for periodic updates
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Track last event timestamp to detect new events
    private Timestamp lastEventTime = new Timestamp(System.currentTimeMillis());

    public ActivityStreamController() {
        // Start periodic check for new events
        scheduler.scheduleAtFixedRate(this::checkAndBroadcastNewEvents, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * GET /api/stream/activity
     * SSE endpoint for real-time activity updates
     */
    @GetMapping(value = "/activity", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamActivity() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of(
                    "message", "Connected to activity stream",
                    "timestamp", System.currentTimeMillis()
                )));
            
            // Send recent activities immediately
            List<Map<String, Object>> recentActivities = getRecentActivities(10);
            emitter.send(SseEmitter.event()
                .name("initial")
                .data(recentActivities));
                
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        
        return emitter;
    }

    /**
     * GET /api/stream/metrics
     * SSE endpoint for real-time metrics updates (every 5 seconds)
     */
    @GetMapping(value = "/metrics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        ScheduledExecutorService metricsScheduler = Executors.newSingleThreadScheduledExecutor();
        metricsScheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> metrics = getQuickMetrics();
                emitter.send(SseEmitter.event()
                    .name("metrics")
                    .data(metrics));
            } catch (Exception e) {
                emitter.completeWithError(e);
                metricsScheduler.shutdown();
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        emitter.onCompletion(metricsScheduler::shutdown);
        emitter.onTimeout(metricsScheduler::shutdown);
        emitter.onError(e -> metricsScheduler.shutdown());
        
        return emitter;
    }

    /**
     * POST /api/stream/broadcast
     * Manually broadcast an event (for testing)
     */
    @PostMapping("/broadcast")
    public Map<String, Object> broadcast(@RequestBody Map<String, Object> event) {
        broadcastEvent("custom", event);
        return Map.of("success", true, "recipients", emitters.size());
    }

    // Broadcast event to all connected clients
    public void broadcastEvent(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        
        emitters.removeAll(deadEmitters);
    }

    // Check for new events and broadcast
    private void checkAndBroadcastNewEvents() {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT username, event_type, description, timestamp " +
                 "FROM event_logs WHERE timestamp > ? ORDER BY timestamp ASC")) {
            
            ps.setTimestamp(1, lastEventTime);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("user", rs.getString("username"));
                    activity.put("action", formatEventType(rs.getString("event_type")));
                    activity.put("details", rs.getString("description"));
                    activity.put("time", rs.getTimestamp("timestamp").toString());
                    activity.put("icon", getEventIcon(rs.getString("event_type")));
                    
                    broadcastEvent("activity", activity);
                    
                    lastEventTime = rs.getTimestamp("timestamp");
                }
            }
        } catch (Exception e) {
            System.err.println("[ActivityStream] Error checking events: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> getRecentActivities(int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT username, event_type, description, timestamp " +
                 "FROM event_logs ORDER BY timestamp DESC LIMIT ?")) {
            
            ps.setInt(1, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("user", rs.getString("username"));
                    activity.put("action", formatEventType(rs.getString("event_type")));
                    activity.put("details", rs.getString("description"));
                    activity.put("time", rs.getTimestamp("timestamp").toString());
                    activity.put("icon", getEventIcon(rs.getString("event_type")));
                    activities.add(activity);
                }
            }
        } catch (Exception e) {
            System.err.println("[ActivityStream] Error getting activities: " + e.getMessage());
        }
        
        return activities;
    }

    private Map<String, Object> getQuickMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try (Connection con = DB.connect()) {
            // File count
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM files");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) metrics.put("totalFiles", rs.getInt(1));
            }
            
            // User count
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) metrics.put("totalUsers", rs.getInt(1));
            }
            
            // Active users (last hour)
            try (PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(DISTINCT username) FROM event_logs " +
                     "WHERE timestamp > DATE_SUB(NOW(), INTERVAL 1 HOUR)");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) metrics.put("activeUsers", rs.getInt(1));
            }
        } catch (Exception e) {
            System.err.println("[ActivityStream] Error getting metrics: " + e.getMessage());
        }
        
        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("containers", com.soft40051.app.scaling.ScalingService.getContainerCount());
        
        return metrics;
    }

    private String formatEventType(String eventType) {
        if (eventType == null) return "unknown";
        return eventType.replace("_", " ").toLowerCase();
    }

    private String getEventIcon(String eventType) {
        if (eventType == null) return "activity";
        
        if (eventType.contains("LOGIN")) return "user";
        if (eventType.contains("FILE")) return "file";
        if (eventType.contains("UPLOAD")) return "upload";
        if (eventType.contains("DELETE")) return "trash";
        if (eventType.contains("SHARE")) return "share";
        if (eventType.contains("SCALE")) return "server";
        if (eventType.contains("ML") || eventType.contains("PREDICT")) return "brain";
        
        return "activity";
    }
}
