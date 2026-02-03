package com.soft40051.app.ml;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST client for Python ML prediction service
 * 
 * Provides methods to:
 * - Get load predictions
 * - Check API health
 * - Retrieve prediction metrics
 * - Cache predictions to avoid excessive API calls
 * 
 * @author CloudFileSystem ML Team
 * @version 1.0
 */
public class LoadPredictionService {
    
    private static final String DEFAULT_API_URL = "http://localhost:5000";
    private static final int CACHE_TTL_MINUTES = 5;
    private static final int REQUEST_TIMEOUT_SECONDS = 10;
    
    private final String apiUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private final PredictionMetrics metrics;
    
    // Prediction cache
    private LoadPrediction cachedPrediction;
    private LocalDateTime cacheTimestamp;
    
    /**
     * Constructor with default API URL
     */
    public LoadPredictionService() {
        this(DEFAULT_API_URL);
    }
    
    /**
     * Constructor with custom API URL
     * 
     * @param apiUrl Python API base URL
     */
    public LoadPredictionService(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build();
        this.gson = new Gson();
        this.metrics = new PredictionMetrics();
    }
    
    /**
     * Get load prediction from ML service
     * 
     * Returns cached prediction if available and not stale (< 5 minutes old)
     * 
     * @param context Load context with current and historical data
     * @return LoadPrediction object, or null if API call fails
     */
    public LoadPrediction getPrediction(LoadContext context) {
        // Check cache first
        if (isCacheValid()) {
            System.out.println("[ML-Predictor] Using cached prediction");
            return cachedPrediction;
        }
        
        try {
            // Build request JSON
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("current_time", 
                context.getCurrentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            requestBody.put("current_load", context.getCurrentLoad());
            requestBody.put("historical_loads", context.getHistoricalLoads());
            
            String jsonBody = gson.toJson(requestBody);
            
            // Make HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/predict"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                // Parse response
                LoadPrediction prediction = parsePredictionResponse(response.body());
                
                // Update cache
                cachedPrediction = prediction;
                cacheTimestamp = LocalDateTime.now();
                
                // Log prediction
                System.out.printf(
                    "[ML-Predictor] Prediction: %.0f req/hr (current: %.0f, confidence: [%.0f, %.0f])%n",
                    prediction.getPredictedLoad(),
                    context.getCurrentLoad(),
                    prediction.getConfidenceLower(),
                    prediction.getConfidenceUpper()
                );
                
                return prediction;
            } else {
                System.err.printf(
                    "[ML-Predictor] API error: HTTP %d - %s%n",
                    response.statusCode(),
                    response.body()
                );
                return null;
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("[ML-Predictor] Failed to get prediction: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get prediction asynchronously
     * 
     * @param context Load context
     * @return CompletableFuture with prediction
     */
    public CompletableFuture<LoadPrediction> getPredictionAsync(LoadContext context) {
        return CompletableFuture.supplyAsync(() -> getPrediction(context));
    }
    
    /**
     * Check if Python API is healthy
     * 
     * @return true if API is reachable and model is loaded
     */
    public boolean isPythonServiceHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                boolean healthy = json.get("status").getAsString().equals("healthy");
                boolean modelLoaded = json.get("model_loaded").getAsBoolean();
                
                return healthy && modelLoaded;
            }
            
            return false;
            
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Determine if system should scale up based on prediction
     * 
     * @param prediction Load prediction
     * @param currentCapacity Current system capacity (requests/hour)
     * @return true if predicted load exceeds 75% of capacity
     */
    public boolean shouldScaleUp(LoadPrediction prediction, double currentCapacity) {
        if (prediction == null) {
            return false;
        }
        
        double threshold = currentCapacity * 0.75;
        return prediction.getPredictedLoad() > threshold;
    }
    
    /**
     * Determine if system should scale down based on prediction
     * 
     * @param prediction Load prediction
     * @param currentCapacity Current system capacity (requests/hour)
     * @return true if predicted load is below 30% of capacity
     */
    public boolean shouldScaleDown(LoadPrediction prediction, double currentCapacity) {
        if (prediction == null) {
            return false;
        }
        
        double threshold = currentCapacity * 0.30;
        return prediction.getPredictedLoad() < threshold;
    }
    
    /**
     * Record actual load for accuracy tracking
     * 
     * @param prediction Original prediction
     * @param actualLoad Actual observed load
     */
    public void recordActualLoad(LoadPrediction prediction, double actualLoad) {
        if (prediction == null) {
            return;
        }
        
        // Track locally
        metrics.recordPrediction(prediction, actualLoad);
        
        // Send to Python API for tracking
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("predicted_load", prediction.getPredictedLoad());
            requestBody.put("actual_load", actualLoad);
            
            String jsonBody = gson.toJson(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/record_actual"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            
        } catch (Exception e) {
            // Silent fail - not critical
        }
    }
    
    /**
     * Get prediction metrics
     * 
     * @return PredictionMetrics object
     */
    public PredictionMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Clear prediction cache
     */
    public void clearCache() {
        cachedPrediction = null;
        cacheTimestamp = null;
    }
    
    /**
     * Check if cached prediction is still valid
     * 
     * @return true if cache is valid
     */
    private boolean isCacheValid() {
        if (cachedPrediction == null || cacheTimestamp == null) {
            return false;
        }
        
        LocalDateTime expiryTime = cacheTimestamp.plusMinutes(CACHE_TTL_MINUTES);
        return LocalDateTime.now().isBefore(expiryTime);
    }
    
    /**
     * Parse prediction response JSON
     * 
     * @param jsonResponse JSON response string
     * @return LoadPrediction object
     */
    private LoadPrediction parsePredictionResponse(String jsonResponse) {
        JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
        
        LoadPrediction prediction = new LoadPrediction();
        prediction.setPredictedLoad(json.get("predicted_load").getAsDouble());
        prediction.setConfidenceLower(json.get("confidence_lower").getAsDouble());
        prediction.setConfidenceUpper(json.get("confidence_upper").getAsDouble());
        prediction.setPredictionHorizon(json.get("prediction_horizon").getAsString());
        prediction.setModelAccuracy(json.get("model_accuracy").getAsDouble());
        
        return prediction;
    }
    
    /**
     * Get API URL
     * 
     * @return API base URL
     */
    public String getApiUrl() {
        return apiUrl;
    }
}
