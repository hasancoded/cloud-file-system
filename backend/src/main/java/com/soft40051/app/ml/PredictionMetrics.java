package com.soft40051.app.ml;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks prediction accuracy metrics over time
 * 
 * Calculates RMSE, MAE, and maintains prediction history
 * 
 * @author CloudFileSystem ML Team
 * @version 1.0
 */
public class PredictionMetrics {
    
    private static class PredictionRecord {
        LoadPrediction prediction;
        double actualLoad;
        LocalDateTime timestamp;
        
        PredictionRecord(LoadPrediction prediction, double actualLoad) {
            this.prediction = prediction;
            this.actualLoad = actualLoad;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    private final List<PredictionRecord> records;
    private static final int MAX_RECORDS = 1000;
    
    /**
     * Constructor
     */
    public PredictionMetrics() {
        this.records = new ArrayList<>();
    }
    
    /**
     * Record a prediction and its actual outcome
     * 
     * @param prediction Original prediction
     * @param actualLoad Actual observed load
     */
    public synchronized void recordPrediction(LoadPrediction prediction, double actualLoad) {
        records.add(new PredictionRecord(prediction, actualLoad));
        
        // Keep only last MAX_RECORDS
        if (records.size() > MAX_RECORDS) {
            records.remove(0);
        }
    }
    
    /**
     * Calculate Root Mean Squared Error (RMSE)
     * 
     * @return RMSE value
     */
    public double getRMSE() {
        if (records.isEmpty()) {
            return 0.0;
        }
        
        double sumSquaredErrors = records.stream()
            .mapToDouble(r -> {
                double error = r.prediction.getPredictedLoad() - r.actualLoad;
                return error * error;
            })
            .sum();
        
        return Math.sqrt(sumSquaredErrors / records.size());
    }
    
    /**
     * Calculate Mean Absolute Error (MAE)
     * 
     * @return MAE value
     */
    public double getMAE() {
        if (records.isEmpty()) {
            return 0.0;
        }
        
        double sumAbsErrors = records.stream()
            .mapToDouble(r -> Math.abs(r.prediction.getPredictedLoad() - r.actualLoad))
            .sum();
        
        return sumAbsErrors / records.size();
    }
    
    /**
     * Calculate Mean Absolute Percentage Error (MAPE)
     * 
     * @return MAPE value as percentage
     */
    public double getMAPE() {
        if (records.isEmpty()) {
            return 0.0;
        }
        
        double sumPercentErrors = records.stream()
            .filter(r -> r.actualLoad != 0)  // Avoid division by zero
            .mapToDouble(r -> {
                double error = Math.abs(r.prediction.getPredictedLoad() - r.actualLoad);
                return (error / r.actualLoad) * 100;
            })
            .sum();
        
        long validRecords = records.stream()
            .filter(r -> r.actualLoad != 0)
            .count();
        
        return validRecords > 0 ? sumPercentErrors / validRecords : 0.0;
    }
    
    /**
     * Get percentage of predictions within confidence interval
     * 
     * @return Percentage (0-100)
     */
    public double getConfidenceIntervalAccuracy() {
        if (records.isEmpty()) {
            return 0.0;
        }
        
        long withinInterval = records.stream()
            .filter(r -> r.prediction.isWithinConfidenceInterval(r.actualLoad))
            .count();
        
        return (withinInterval * 100.0) / records.size();
    }
    
    /**
     * Get total number of predictions recorded
     * 
     * @return Number of predictions
     */
    public int getTotalPredictions() {
        return records.size();
    }
    
    /**
     * Get average prediction error
     * 
     * @return Average absolute error
     */
    public double getAverageError() {
        return getMAE();
    }
    
    /**
     * Get recent predictions (last N)
     * 
     * @param count Number of recent predictions to retrieve
     * @return List of recent prediction records
     */
    public List<PredictionRecord> getRecentPredictions(int count) {
        int size = records.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(records.subList(fromIndex, size));
    }
    
    /**
     * Clear all recorded metrics
     */
    public synchronized void clear() {
        records.clear();
    }
    
    /**
     * Get metrics summary as string
     * 
     * @return Formatted metrics summary
     */
    public String getSummary() {
        return String.format(
            "Predictions: %d | RMSE: %.2f | MAE: %.2f | MAPE: %.2f%% | CI Accuracy: %.1f%%",
            getTotalPredictions(),
            getRMSE(),
            getMAE(),
            getMAPE(),
            getConfidenceIntervalAccuracy()
        );
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}
