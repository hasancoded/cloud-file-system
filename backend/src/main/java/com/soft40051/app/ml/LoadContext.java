package com.soft40051.app.ml;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Context object containing current load state for prediction requests
 * 
 * Encapsulates current time, load, and historical load data
 * 
 * @author CloudFileSystem ML Team
 * @version 1.0
 */
public class LoadContext {
    
    private LocalDateTime currentTime;
    private double currentLoad;
    private List<Double> historicalLoads;
    
    /**
     * Default constructor
     */
    public LoadContext() {
        this.currentTime = LocalDateTime.now();
        this.historicalLoads = new ArrayList<>();
    }
    
    /**
     * Constructor with current load
     * 
     * @param currentLoad Current load in requests/hour
     */
    public LoadContext(double currentLoad) {
        this.currentTime = LocalDateTime.now();
        this.currentLoad = currentLoad;
        this.historicalLoads = new ArrayList<>();
    }
    
    /**
     * Full constructor
     * 
     * @param currentTime Current timestamp
     * @param currentLoad Current load in requests/hour
     * @param historicalLoads List of historical load values (last 4 hours)
     */
    public LoadContext(LocalDateTime currentTime, double currentLoad, 
                      List<Double> historicalLoads) {
        this.currentTime = currentTime;
        this.currentLoad = currentLoad;
        this.historicalLoads = historicalLoads != null ? 
            new ArrayList<>(historicalLoads) : new ArrayList<>();
    }
    
    // Getters and Setters
    
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }
    
    public void setCurrentTime(LocalDateTime currentTime) {
        this.currentTime = currentTime;
    }
    
    public double getCurrentLoad() {
        return currentLoad;
    }
    
    public void setCurrentLoad(double currentLoad) {
        this.currentLoad = currentLoad;
    }
    
    public List<Double> getHistoricalLoads() {
        return new ArrayList<>(historicalLoads);
    }
    
    public void setHistoricalLoads(List<Double> historicalLoads) {
        this.historicalLoads = historicalLoads != null ? 
            new ArrayList<>(historicalLoads) : new ArrayList<>();
    }
    
    /**
     * Add a historical load value
     * 
     * @param load Load value to add
     */
    public void addHistoricalLoad(double load) {
        historicalLoads.add(load);
        
        // Keep only last 24 values (24 hours)
        if (historicalLoads.size() > 24) {
            historicalLoads.remove(0);
        }
    }
    
    /**
     * Get average of historical loads
     * 
     * @return Average historical load
     */
    public double getAverageHistoricalLoad() {
        if (historicalLoads.isEmpty()) {
            return currentLoad;
        }
        return historicalLoads.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(currentLoad);
    }
    
    /**
     * Get most recent historical load (1 hour ago)
     * 
     * @return Most recent historical load, or current load if none available
     */
    public double getLoad1HourAgo() {
        if (historicalLoads.isEmpty()) {
            return currentLoad;
        }
        return historicalLoads.get(historicalLoads.size() - 1);
    }
    
    @Override
    public String toString() {
        return String.format(
            "LoadContext{time=%s, current=%.0f, historical=%d values, avg=%.0f}",
            currentTime, currentLoad, historicalLoads.size(), 
            getAverageHistoricalLoad()
        );
    }
}
