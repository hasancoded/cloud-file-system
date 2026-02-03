package com.soft40051.app.scaling;

import com.soft40051.app.hostmanager.HostManager;
import com.soft40051.app.hostmanager.HealthCheck;
import com.soft40051.app.loadbalancer.LoadBalancer;
import com.soft40051.app.ml.LoadPrediction;
import com.soft40051.app.ml.LoadPredictionService;
import com.soft40051.app.ml.LoadContext;
import com.soft40051.app.ml.PredictionMetrics;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

/**
 * Dynamic Container Scaling Service with ML-Based Prediction
 * Implements elasticity for cloud file storage system
 * 
 * Scaling Strategy:
 * - PRIMARY: ML-based proactive scaling (predicts load 30min ahead)
 * - FALLBACK: Reactive scaling (traditional threshold-based)
 * - Monitor load metrics (file operations, simulated CPU)
 * - Scale up when predicted/actual load exceeds threshold
 * - Scale down when predicted/actual load drops below threshold
 * - Minimum 1 container, maximum 5 containers
 * 
 * ML Integration:
 * - Uses Python Flask ML service for predictions
 * - Tracks prediction accuracy (RMSE, MAE)
 * - Falls back to reactive scaling if ML service unavailable
 * - Caches predictions to reduce API calls
 * 
 * Design:
 * - Java-based simulation (no real cloud APIs)
 * - Integrates with LoadBalancer, HostManager, and ML service
 * - Periodic monitoring thread
 * - Graceful scaling transitions
 * 
 * @author SOFT40051 Submission (Enhanced with ML)
 * @version 2.0
 */
public class ScalingService {
    
    // Scaling configuration
    private static final int MIN_CONTAINERS = 1;
    private static final int MAX_CONTAINERS = 5;
    private static final double SCALE_UP_THRESHOLD = 0.75;   // 75% load
    private static final double SCALE_DOWN_THRESHOLD = 0.30; // 30% load
    private static final long MONITORING_INTERVAL_MS = 15000; // 15 seconds
    
    // Current state
    private static int currentContainerCount = 1;
    private static final Map<String, Boolean> activeContainers = new ConcurrentHashMap<>();
    private static final Queue<Double> loadHistory = new ConcurrentLinkedQueue<>();
    private static final int LOAD_HISTORY_SIZE = 10;
    
    // Monitoring thread
    private static ScheduledExecutorService monitoringService;
    private static LoadBalancer loadBalancer;
    
    // ML Integration
    private static LoadPredictionService mlService;
    private static boolean mlEnabled = true;
    private static LoadPrediction lastPrediction;
    private static final List<Double> historicalLoads = new ArrayList<>();
    
    // Metrics
    private static volatile int totalFileOperations = 0;
    private static volatile long lastMetricTime = System.currentTimeMillis();
    private static volatile int scaleUpEvents = 0;
    private static volatile int scaleDownEvents = 0;
    private static volatile int mlScaleUpEvents = 0;
    private static volatile int mlScaleDownEvents = 0;
    private static volatile int reactiveScaleUpEvents = 0;
    private static volatile int reactiveScaleDownEvents = 0;
    
    /**
     * Initialize scaling service
     */
    public static void initialize(LoadBalancer lb) {
        loadBalancer = lb;
        
        // Register primary container
        activeContainers.put("soft40051-file-server", true);
        currentContainerCount = 1;
        
        // Initialize ML service
        try {
            mlService = new LoadPredictionService();
            
            // Check if ML service is available
            if (mlService.isPythonServiceHealthy()) {
                mlEnabled = true;
                System.out.println("[ScalingService] ✓ ML prediction service connected");
                System.out.println("[ScalingService] Mode: PROACTIVE (ML-based)");
            } else {
                mlEnabled = false;
                System.out.println("[ScalingService] ⚠ ML service unavailable - using REACTIVE mode");
            }
        } catch (Exception e) {
            mlEnabled = false;
            System.out.println("[ScalingService] ⚠ ML service initialization failed - using REACTIVE mode");
        }
        
        System.out.println("[ScalingService] Initialized with " + currentContainerCount + " container(s)");
    }
    
    /**
     * Start monitoring and auto-scaling
     */
    public static void startAutoScaling() {
        if (monitoringService != null && !monitoringService.isShutdown()) {
            System.out.println("[ScalingService] Auto-scaling already running");
            return;
        }
        
        monitoringService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScalingMonitorThread");
            t.setDaemon(true);
            return t;
        });
        
        monitoringService.scheduleAtFixedRate(() -> {
            try {
                evaluateScaling();
            } catch (Exception e) {
                System.err.println("[ScalingService] Monitoring error: " + e.getMessage());
            }
        }, MONITORING_INTERVAL_MS, MONITORING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        System.out.println("[ScalingService] Auto-scaling started");
    }
    
    /**
     * Stop auto-scaling
     */
    public static void stopAutoScaling() {
        if (monitoringService != null) {
            monitoringService.shutdown();
            try {
                monitoringService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                monitoringService.shutdownNow();
            }
            System.out.println("[ScalingService] Auto-scaling stopped");
        }
    }
    
    /**
     * Evaluate current load and make scaling decision
     * Uses ML prediction if available, falls back to reactive scaling
     */
    private static void evaluateScaling() throws Exception {
        double currentLoad = calculateLoad();
        
        // Add to history
        loadHistory.offer(currentLoad);
        if (loadHistory.size() > LOAD_HISTORY_SIZE) {
            loadHistory.poll();
        }
        
        // Calculate average load
        double avgLoad = loadHistory.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Convert to requests per hour for ML service
        double currentLoadReqPerHour = avgLoad * currentContainerCount * 10 * 3600; // ops/sec to req/hr
        
        // Try ML-based proactive scaling first
        if (mlEnabled && mlService != null) {
            try {
                evaluateProactiveScaling(currentLoadReqPerHour, avgLoad);
                return; // ML scaling succeeded
            } catch (Exception e) {
                System.err.println("[ScalingService] ML prediction failed: " + e.getMessage());
                System.out.println("[ScalingService] Falling back to REACTIVE mode");
                mlEnabled = false; // Disable ML for this session
            }
        }
        
        // Fallback to reactive scaling
        evaluateReactiveScaling(avgLoad);
    }
    
    /**
     * ML-based proactive scaling
     * Predicts load 30 minutes ahead and scales preemptively
     */
    private static void evaluateProactiveScaling(double currentLoadReqPerHour, double currentLoadPercent) {
        // Build load context
        LoadContext context = new LoadContext(
            LocalDateTime.now(),
            currentLoadReqPerHour,
            new ArrayList<>(historicalLoads)
        );
        
        // Update historical loads
        historicalLoads.add(currentLoadReqPerHour);
        if (historicalLoads.size() > 24) {
            historicalLoads.remove(0);
        }
        
        // Get prediction
        LoadPrediction prediction = mlService.getPrediction(context);
        
        if (prediction == null) {
            throw new RuntimeException("Failed to get prediction from ML service");
        }
        
        lastPrediction = prediction;
        
        // Calculate current capacity
        double currentCapacity = currentContainerCount * 10 * 3600; // req/hr
        
        // Log prediction
        System.out.println(String.format(
            "[ML-Predictor] Current: %.0f req/hr | Predicted (30min): %.0f req/hr | Confidence: [%.0f, %.0f]",
            currentLoadReqPerHour,
            prediction.getPredictedLoad(),
            prediction.getConfidenceLower(),
            prediction.getConfidenceUpper()
        ));
        
        System.out.println(String.format(
            "[ScalingService] Load: %.2f%% (current), Predicted: %.0f req/hr, Containers: %d",
            currentLoadPercent * 100,
            prediction.getPredictedLoad(),
            currentContainerCount
        ));
        
        // Proactive scaling decisions based on prediction
        if (mlService.shouldScaleUp(prediction, currentCapacity) && 
            currentContainerCount < MAX_CONTAINERS) {
            
            System.out.println(String.format(
                "[ML-Scaling] ⚡ PROACTIVE SCALE UP: Predicted load (%.0f) > 75%% capacity (%.0f)",
                prediction.getPredictedLoad(),
                currentCapacity * 0.75
            ));
            
            scaleUp(true); // true = ML-based
            mlScaleUpEvents++;
            
        } else if (mlService.shouldScaleDown(prediction, currentCapacity) && 
                   currentContainerCount > MIN_CONTAINERS) {
            
            System.out.println(String.format(
                "[ML-Scaling] ⚡ PROACTIVE SCALE DOWN: Predicted load (%.0f) < 30%% capacity (%.0f)",
                prediction.getPredictedLoad(),
                currentCapacity * 0.30
            ));
            
            scaleDown(true); // true = ML-based
            mlScaleDownEvents++;
        }
        
        // Record actual load for accuracy tracking
        if (lastPrediction != null && !lastPrediction.isStale()) {
            mlService.recordActualLoad(lastPrediction, currentLoadReqPerHour);
        }
    }
    
    /**
     * Traditional reactive scaling (fallback)
     */
    private static void evaluateReactiveScaling(double avgLoad) throws Exception {
        System.out.println(String.format(
            "[ScalingService] REACTIVE Mode - Load: %.2f%%, Containers: %d",
            avgLoad * 100,
            currentContainerCount
        ));
        
        // Scaling decisions based on current load
        if (avgLoad > SCALE_UP_THRESHOLD && currentContainerCount < MAX_CONTAINERS) {
            scaleUp(false); // false = reactive
            reactiveScaleUpEvents++;
        } else if (avgLoad < SCALE_DOWN_THRESHOLD && currentContainerCount > MIN_CONTAINERS) {
            scaleDown(false); // false = reactive
            reactiveScaleDownEvents++;
        }
    }
    
    /**
     * Calculate current system load (simulated)
     * In real system, this would use CPU, memory, request rate, etc.
     */
    private static double calculateLoad() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastMetricTime;
        
        // Calculate operations per second
        double opsPerSecond = 0;
        if (timeDiff > 0) {
            opsPerSecond = (totalFileOperations * 1000.0) / timeDiff;
        }
        
        // Simulate load based on operations (arbitrary scaling)
        // Assume max capacity is 10 ops/sec per container
        double capacity = currentContainerCount * 10.0;
        double load = Math.min(1.0, opsPerSecond / capacity);
        
        // Add some random variation (simulate real-world fluctuation)
        load += (Math.random() * 0.1 - 0.05); // ±5% variation
        load = Math.max(0.0, Math.min(1.0, load));
        
        // Reset metrics
        totalFileOperations = 0;
        lastMetricTime = currentTime;
        
        return load;
    }
    
    /**
     * Scale up: Add container
     * @param mlBased true if triggered by ML prediction, false if reactive
     */
    private static void scaleUp(boolean mlBased) {
        if (currentContainerCount >= MAX_CONTAINERS) {
            return;
        }
        
        String newContainerName = "soft40051-file-server-" + (currentContainerCount + 1);
        String mode = mlBased ? "ML-PROACTIVE" : "REACTIVE";
        
        System.out.println(String.format(
            "[ScalingService] ⬆ SCALING UP (%s): Adding container %s",
            mode,
            newContainerName
        ));
        
        try {
            // In real scenario, we would:
            // 1. Create new Docker container
            // 2. Wait for it to be healthy
            // 3. Add to load balancer
            
            // Simulated: Just mark as active
            activeContainers.put(newContainerName, true);
            currentContainerCount++;
            
            // Notify load balancer
            if (loadBalancer != null) {
                loadBalancer.setServerHealth(currentContainerCount - 1, true);
            }
            
            scaleUpEvents++;
            
            System.out.println(String.format(
                "[ScalingService] ✓ Scale up completed (%s). Total containers: %d",
                mode,
                currentContainerCount
            ));
            
        } catch (Exception e) {
            System.err.println("[ScalingService] Scale up failed: " + e.getMessage());
        }
    }
    
    /**
     * Scale up: Add container (backward compatibility)
     */
    private static void scaleUp() {
        scaleUp(false);
    }
    
    /**
     * Scale down: Remove container
     * @param mlBased true if triggered by ML prediction, false if reactive
     */
    private static void scaleDown(boolean mlBased) {
        if (currentContainerCount <= MIN_CONTAINERS) {
            return;
        }
        
        String containerToRemove = "soft40051-file-server-" + currentContainerCount;
        String mode = mlBased ? "ML-PROACTIVE" : "REACTIVE";
        
        System.out.println(String.format(
            "[ScalingService] ⬇ SCALING DOWN (%s): Removing container %s",
            mode,
            containerToRemove
        ));
        
        try {
            // In real scenario, we would:
            // 1. Remove from load balancer
            // 2. Drain connections
            // 3. Stop and remove container
            
            // Simulated: Mark as inactive
            activeContainers.put(containerToRemove, false);
            
            // Notify load balancer
            if (loadBalancer != null) {
                loadBalancer.setServerHealth(currentContainerCount - 1, false);
            }
            
            currentContainerCount--;
            scaleDownEvents++;
            
            System.out.println(String.format(
                "[ScalingService] ✓ Scale down completed (%s). Total containers: %d",
                mode,
                currentContainerCount
            ));
            
        } catch (Exception e) {
            System.err.println("[ScalingService] Scale down failed: " + e.getMessage());
        }
    }
    
    /**
     * Scale down: Remove container (backward compatibility)
     */
    private static void scaleDown() {
        scaleDown(false);
    }
    
    /**
     * Record file operation for load calculation
     */
    public static void recordFileOperation() {
        totalFileOperations++;
    }
    
    /**
     * Manually trigger scale up (for testing)
     */
    public static void manualScaleUp() throws Exception {
        scaleUp();
    }
    
    /**
     * Manually trigger scale down (for testing)
     */
    public static void manualScaleDown() throws Exception {
        scaleDown();
    }
    
    /**
     * Get current container count
     */
    public static int getContainerCount() {
        return currentContainerCount;
    }
    
    /**
     * Get active containers
     */
    public static Map<String, Boolean> getActiveContainers() {
        return new HashMap<>(activeContainers);
    }
    
    /**
     * Get scaling statistics
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("currentContainers", currentContainerCount);
        stats.put("minContainers", MIN_CONTAINERS);
        stats.put("maxContainers", MAX_CONTAINERS);
        stats.put("scaleUpEvents", scaleUpEvents);
        stats.put("scaleDownEvents", scaleDownEvents);
        stats.put("mlScaleUpEvents", mlScaleUpEvents);
        stats.put("mlScaleDownEvents", mlScaleDownEvents);
        stats.put("reactiveScaleUpEvents", reactiveScaleUpEvents);
        stats.put("reactiveScaleDownEvents", reactiveScaleDownEvents);
        stats.put("mlEnabled", mlEnabled);
        stats.put("activeContainers", new HashMap<>(activeContainers));
        stats.put("currentLoad", loadHistory.isEmpty() ? 0.0 : 
                  loadHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        
        // Add ML metrics if available
        if (mlService != null) {
            PredictionMetrics metrics = mlService.getMetrics();
            stats.put("mlPredictions", metrics.getTotalPredictions());
            stats.put("mlRMSE", metrics.getRMSE());
            stats.put("mlMAE", metrics.getMAE());
            stats.put("mlMAPE", metrics.getMAPE());
        }
        
        // Add last prediction if available
        if (lastPrediction != null) {
            stats.put("lastPrediction", lastPrediction.getPredictedLoad());
            stats.put("predictionConfidenceLower", lastPrediction.getConfidenceLower());
            stats.put("predictionConfidenceUpper", lastPrediction.getConfidenceUpper());
        }
        
        return stats;
    }
    
    /**
     * Get ML prediction service (for dashboard access)
     */
    public static LoadPredictionService getMLService() {
        return mlService;
    }
    
    /**
     * Get last prediction (for dashboard access)
     */
    public static LoadPrediction getLastPrediction() {
        return lastPrediction;
    }
    
    /**
     * Check if ML mode is enabled
     */
    public static boolean isMLEnabled() {
        return mlEnabled;
    }
    
    /**
     * Toggle ML mode (for testing)
     */
    public static void setMLEnabled(boolean enabled) {
        mlEnabled = enabled;
        System.out.println("[ScalingService] ML mode " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Reset statistics (for testing)
     */
    public static void resetStatistics() {
        scaleUpEvents = 0;
        scaleDownEvents = 0;
        totalFileOperations = 0;
        loadHistory.clear();
        System.out.println("[ScalingService] Statistics reset");
    }
}