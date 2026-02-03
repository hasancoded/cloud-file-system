package com.soft40051.app.ml;

import java.time.LocalDateTime;

/**
 * POJO representing a load prediction from the ML service
 * 
 * Contains predicted load value, confidence intervals, and metadata
 * 
 * @author CloudFileSystem ML Team
 * @version 1.0
 */
public class LoadPrediction {
    
    private double predictedLoad;
    private double confidenceLower;
    private double confidenceUpper;
    private String predictionHorizon;
    private double modelAccuracy;
    private LocalDateTime timestamp;
    private LocalDateTime predictionTime;
    
    /**
     * Default constructor
     */
    public LoadPrediction() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Full constructor
     * 
     * @param predictedLoad Predicted load in requests/hour
     * @param confidenceLower Lower bound of 95% confidence interval
     * @param confidenceUpper Upper bound of 95% confidence interval
     * @param predictionHorizon Time horizon for prediction (e.g., "30_minutes")
     * @param modelAccuracy Model R² score
     */
    public LoadPrediction(double predictedLoad, double confidenceLower, 
                         double confidenceUpper, String predictionHorizon, 
                         double modelAccuracy) {
        this.predictedLoad = predictedLoad;
        this.confidenceLower = confidenceLower;
        this.confidenceUpper = confidenceUpper;
        this.predictionHorizon = predictionHorizon;
        this.modelAccuracy = modelAccuracy;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public double getPredictedLoad() {
        return predictedLoad;
    }
    
    public void setPredictedLoad(double predictedLoad) {
        this.predictedLoad = predictedLoad;
    }
    
    public double getConfidenceLower() {
        return confidenceLower;
    }
    
    public void setConfidenceLower(double confidenceLower) {
        this.confidenceLower = confidenceLower;
    }
    
    public double getConfidenceUpper() {
        return confidenceUpper;
    }
    
    public void setConfidenceUpper(double confidenceUpper) {
        this.confidenceUpper = confidenceUpper;
    }
    
    public String getPredictionHorizon() {
        return predictionHorizon;
    }
    
    public void setPredictionHorizon(String predictionHorizon) {
        this.predictionHorizon = predictionHorizon;
    }
    
    public double getModelAccuracy() {
        return modelAccuracy;
    }
    
    public void setModelAccuracy(double modelAccuracy) {
        this.modelAccuracy = modelAccuracy;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public LocalDateTime getPredictionTime() {
        return predictionTime;
    }
    
    public void setPredictionTime(LocalDateTime predictionTime) {
        this.predictionTime = predictionTime;
    }
    
    /**
     * Get confidence interval width
     * 
     * @return Width of confidence interval
     */
    public double getConfidenceWidth() {
        return confidenceUpper - confidenceLower;
    }
    
    /**
     * Check if actual load falls within confidence interval
     * 
     * @param actualLoad Actual observed load
     * @return true if within confidence interval
     */
    public boolean isWithinConfidenceInterval(double actualLoad) {
        return actualLoad >= confidenceLower && actualLoad <= confidenceUpper;
    }
    
    /**
     * Calculate prediction error
     * 
     * @param actualLoad Actual observed load
     * @return Absolute error
     */
    public double calculateError(double actualLoad) {
        return Math.abs(predictedLoad - actualLoad);
    }
    
    /**
     * Calculate percentage error
     * 
     * @param actualLoad Actual observed load
     * @return Percentage error
     */
    public double calculatePercentageError(double actualLoad) {
        if (actualLoad == 0) return 0;
        return (Math.abs(predictedLoad - actualLoad) / actualLoad) * 100;
    }
    
    /**
     * Check if prediction is stale (older than 5 minutes)
     * 
     * @return true if prediction is stale
     */
    public boolean isStale() {
        return timestamp.plusMinutes(5).isBefore(LocalDateTime.now());
    }
    
    @Override
    public String toString() {
        return String.format(
            "LoadPrediction{predicted=%.0f, confidence=[%.0f, %.0f], accuracy=%.2f, horizon=%s, time=%s}",
            predictedLoad, confidenceLower, confidenceUpper, 
            modelAccuracy, predictionHorizon, timestamp
        );
    }
}
