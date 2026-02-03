package com.soft40051.app.gui;

import com.soft40051.app.scaling.ScalingService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import java.util.Map;

/**
 * ML Dashboard View - Clean, professional visualization of ML metrics
 * 
 * @author CloudFileSystem Team
 * @version 2.1
 */
public class MLDashboardView {
    
    // Refined Color Palette
    private static final String PRIMARY = "#2563EB";
    private static final String PRIMARY_LIGHT = "#3B82F6";
    private static final String SUCCESS = "#10B981";
    private static final String WARNING = "#F59E0B";
    private static final String DANGER = "#EF4444";
    private static final String NEUTRAL_50 = "#FAFAFA";
    private static final String NEUTRAL_100 = "#F5F5F5";
    private static final String NEUTRAL_200 = "#E5E5E5";
    private static final String NEUTRAL_400 = "#A3A3A3";
    private static final String NEUTRAL_600 = "#525252";
    private static final String NEUTRAL_800 = "#262626";
    
    private ScrollPane view;
    private Timeline autoRefreshTimeline;
    
    // UI Components
    private Label modeValueLabel;
    private Circle statusIndicator;
    private Label statusTextLabel;
    private Label predictionsValueLabel;
    private Label accuracyValueLabel;
    private Label containersValueLabel;
    private Label currentLoadValueLabel;
    private Label predictedLoadValueLabel;
    private Label confidenceValueLabel;
    private ProgressBar loadProgressBar;
    private BarChart<String, Number> scalingChart;
    
    // Scaling event labels for when chart has no data
    private Label mlScaleUpLabel;
    private Label mlScaleDownLabel;
    private Label reactiveScaleUpLabel;
    private Label reactiveScaleDownLabel;
    
    public MLDashboardView() {
        this.view = createView();
        startAutoRefresh();
        refreshData();
    }
    
    private ScrollPane createView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(0));
        
        // Status overview section
        HBox statusSection = createStatusSection();
        
        // Metrics grid
        GridPane metricsGrid = createMetricsGrid();
        
        // Two-column layout for scaling info and prediction
        HBox contentRow = new HBox(16);
        VBox scalingSection = createScalingSection();
        VBox predictionSection = createPredictionSection();
        
        HBox.setHgrow(scalingSection, Priority.ALWAYS);
        scalingSection.setMaxWidth(Double.MAX_VALUE);
        predictionSection.setMinWidth(280);
        predictionSection.setMaxWidth(300);
        
        contentRow.getChildren().addAll(scalingSection, predictionSection);
        
        container.getChildren().addAll(statusSection, metricsGrid, contentRow);
        
        // Wrap in ScrollPane for overflow
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        return scrollPane;
    }
    
    private HBox createStatusSection() {
        HBox section = new HBox(16);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(14, 18, 14, 18));
        section.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);"
        );
        
        // Status section with dot next to Connected text
        VBox statusBox = new VBox(1);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        
        Label statusTitle = new Label("Status");
        statusTitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 10px;");
        
        HBox statusValueRow = new HBox(5);
        statusValueRow.setAlignment(Pos.CENTER_LEFT);
        
        statusIndicator = new Circle(4);
        statusIndicator.setStyle("-fx-fill: " + SUCCESS + ";");
        
        statusTextLabel = new Label("Connected");
        statusTextLabel.setStyle("-fx-text-fill: " + SUCCESS + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        statusValueRow.getChildren().addAll(statusIndicator, statusTextLabel);
        statusBox.getChildren().addAll(statusTitle, statusValueRow);
        
        // Mode display
        VBox modeBox = new VBox(1);
        Label modeTitle = new Label("Mode");
        modeTitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 10px;");
        
        modeValueLabel = new Label("PROACTIVE");
        modeValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        modeBox.getChildren().addAll(modeTitle, modeValueLabel);
        
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // Auto-refresh info
        VBox updateBox = new VBox(1);
        Label updateTitle = new Label("Refresh");
        updateTitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 10px;");
        
        Label updateValue = new Label("Every 15s");
        updateValue.setStyle("-fx-text-fill: " + NEUTRAL_600 + "; -fx-font-size: 12px;");
        
        updateBox.getChildren().addAll(updateTitle, updateValue);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Refresh button
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle(
            "-fx-background-color: " + NEUTRAL_100 + ";" +
            "-fx-text-fill: " + NEUTRAL_600 + ";" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 6 14;" +
            "-fx-background-radius: 5px;" +
            "-fx-cursor: hand;"
        );
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle(
            "-fx-background-color: " + PRIMARY + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 6 14;" +
            "-fx-background-radius: 5px;" +
            "-fx-cursor: hand;"
        ));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle(
            "-fx-background-color: " + NEUTRAL_100 + ";" +
            "-fx-text-fill: " + NEUTRAL_600 + ";" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 6 14;" +
            "-fx-background-radius: 5px;" +
            "-fx-cursor: hand;"
        ));
        refreshBtn.setOnAction(e -> refreshData());
        
        section.getChildren().addAll(statusBox, sep1, modeBox, sep2, updateBox, spacer, refreshBtn);
        
        return section;
    }
    
    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(0);
        
        VBox predictionsCard = createMetricCard("Predictions", "0", PRIMARY);
        predictionsValueLabel = (Label) ((VBox) predictionsCard.getChildren().get(0)).getChildren().get(1);
        
        VBox accuracyCard = createMetricCard("RMSE", "0.00", SUCCESS);
        accuracyValueLabel = (Label) ((VBox) accuracyCard.getChildren().get(0)).getChildren().get(1);
        
        VBox containersCard = createMetricCard("Containers", "1", PRIMARY_LIGHT);
        containersValueLabel = (Label) ((VBox) containersCard.getChildren().get(0)).getChildren().get(1);
        
        VBox loadCard = createMetricCard("Load", "0%", WARNING);
        currentLoadValueLabel = (Label) ((VBox) loadCard.getChildren().get(0)).getChildren().get(1);
        
        GridPane.setHgrow(predictionsCard, Priority.ALWAYS);
        GridPane.setHgrow(accuracyCard, Priority.ALWAYS);
        GridPane.setHgrow(containersCard, Priority.ALWAYS);
        GridPane.setHgrow(loadCard, Priority.ALWAYS);
        
        grid.add(predictionsCard, 0, 0);
        grid.add(accuracyCard, 1, 0);
        grid.add(containersCard, 2, 0);
        grid.add(loadCard, 3, 0);
        
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            grid.getColumnConstraints().add(col);
        }
        
        return grid;
    }
    
    private VBox createMetricCard(String title, String value, String accentColor) {
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);"
        );
        card.setPadding(new Insets(14));
        
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 11px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");
        
        content.getChildren().addAll(titleLabel, valueLabel);
        card.getChildren().add(content);
        
        return card;
    }
    
    private VBox createScalingSection() {
        VBox section = new VBox(14);
        section.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);"
        );
        section.setPadding(new Insets(16));
        
        Label title = new Label("Scaling Events");
        title.setStyle("-fx-text-fill: " + NEUTRAL_800 + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label subtitle = new Label("ML-based vs reactive scaling decisions");
        subtitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 11px;");
        
        VBox header = new VBox(2);
        header.getChildren().addAll(title, subtitle);
        
        // Simple stat cards instead of chart for cleaner look
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(12);
        statsGrid.setVgap(12);
        statsGrid.setPadding(new Insets(10, 0, 0, 0));
        
        VBox mlUpCard = createScalingStatCard("ML Scale Up", "0", SUCCESS);
        mlScaleUpLabel = (Label) mlUpCard.lookup(".value-label");
        if (mlScaleUpLabel == null) {
            mlScaleUpLabel = (Label) ((VBox) mlUpCard.getChildren().get(0)).getChildren().get(1);
        }
        
        VBox mlDownCard = createScalingStatCard("ML Scale Down", "0", PRIMARY);
        mlScaleDownLabel = (Label) ((VBox) mlDownCard.getChildren().get(0)).getChildren().get(1);
        
        VBox reactiveUpCard = createScalingStatCard("Reactive Up", "0", WARNING);
        reactiveScaleUpLabel = (Label) ((VBox) reactiveUpCard.getChildren().get(0)).getChildren().get(1);
        
        VBox reactiveDownCard = createScalingStatCard("Reactive Down", "0", DANGER);
        reactiveScaleDownLabel = (Label) ((VBox) reactiveDownCard.getChildren().get(0)).getChildren().get(1);
        
        statsGrid.add(mlUpCard, 0, 0);
        statsGrid.add(mlDownCard, 1, 0);
        statsGrid.add(reactiveUpCard, 0, 1);
        statsGrid.add(reactiveDownCard, 1, 1);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        statsGrid.getColumnConstraints().addAll(col1, col2);
        
        section.getChildren().addAll(header, statsGrid);
        
        return section;
    }
    
    private VBox createScalingStatCard(String label, String value, String color) {
        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color: " + NEUTRAL_50 + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: " + NEUTRAL_200 + ";" +
            "-fx-border-radius: 8px;" +
            "-fx-border-width: 1px;"
        );
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        
        VBox content = new VBox(2);
        
        Label titleLabel = new Label(label);
        titleLabel.setStyle("-fx-text-fill: " + NEUTRAL_600 + "; -fx-font-size: 11px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        valueLabel.getStyleClass().add("value-label");
        
        content.getChildren().addAll(titleLabel, valueLabel);
        card.getChildren().add(content);
        
        return card;
    }
    
    private VBox createPredictionSection() {
        VBox section = new VBox(0);
        section.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);"
        );
        
        // Header
        VBox header = new VBox(2);
        header.setPadding(new Insets(16, 16, 12, 16));
        
        Label title = new Label("Load Forecast");
        title.setStyle("-fx-text-fill: " + NEUTRAL_800 + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label subtitle = new Label("30-minute prediction");
        subtitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 11px;");
        
        header.getChildren().addAll(title, subtitle);
        
        Separator sep = new Separator();
        
        // Content
        VBox content = new VBox(14);
        content.setPadding(new Insets(14, 16, 14, 16));
        
        // Predicted load
        VBox predictedBox = new VBox(4);
        Label predictedTitle = new Label("Predicted Load");
        predictedTitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 11px;");
        
        predictedLoadValueLabel = new Label("-- req/hr");
        predictedLoadValueLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        predictedLoadValueLabel.setStyle("-fx-text-fill: " + PRIMARY + ";");
        
        confidenceValueLabel = new Label("Confidence: --");
        confidenceValueLabel.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 10px;");
        
        predictedBox.getChildren().addAll(predictedTitle, predictedLoadValueLabel, confidenceValueLabel);
        
        // Progress
        VBox progressBox = new VBox(6);
        Label progressTitle = new Label("System Utilization");
        progressTitle.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 11px;");
        
        loadProgressBar = new ProgressBar(0);
        loadProgressBar.setPrefWidth(Double.MAX_VALUE);
        loadProgressBar.setPrefHeight(6);
        loadProgressBar.setStyle(
            "-fx-accent: " + PRIMARY + ";" +
            "-fx-background-color: " + NEUTRAL_100 + ";" +
            "-fx-background-radius: 3px;"
        );
        
        progressBox.getChildren().addAll(progressTitle, loadProgressBar);
        
        content.getChildren().addAll(predictedBox, progressBox);
        
        // Footer
        VBox footer = new VBox(4);
        footer.setPadding(new Insets(12, 16, 14, 16));
        footer.setStyle("-fx-background-color: " + NEUTRAL_50 + "; -fx-background-radius: 0 0 10px 10px;");
        
        Label infoText = new Label("ML predictions based on historical load patterns");
        infoText.setStyle("-fx-text-fill: " + NEUTRAL_400 + "; -fx-font-size: 10px;");
        infoText.setWrapText(true);
        
        footer.getChildren().add(infoText);
        
        section.getChildren().addAll(header, sep, content, footer);
        
        return section;
    }
    
    private void refreshData() {
        try {
            Map<String, Object> stats = ScalingService.getStatistics();
            
            // Update status
            boolean mlEnabled = (Boolean) stats.get("mlEnabled");
            statusIndicator.setStyle("-fx-fill: " + (mlEnabled ? SUCCESS : DANGER) + ";");
            statusTextLabel.setText(mlEnabled ? "Connected" : "Disconnected");
            statusTextLabel.setStyle("-fx-text-fill: " + (mlEnabled ? SUCCESS : DANGER) + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            modeValueLabel.setText(mlEnabled ? "PROACTIVE" : "REACTIVE");
            modeValueLabel.setStyle("-fx-text-fill: " + (mlEnabled ? PRIMARY : WARNING) + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            
            // Update metrics
            if (stats.containsKey("mlPredictions")) {
                predictionsValueLabel.setText(String.valueOf(stats.get("mlPredictions")));
                double rmse = (Double) stats.get("mlRMSE");
                accuracyValueLabel.setText(String.format("%.1f", rmse));
            }
            
            containersValueLabel.setText(String.valueOf(stats.get("currentContainers")));
            
            double currentLoad = (Double) stats.get("currentLoad");
            currentLoadValueLabel.setText(String.format("%.0f%%", currentLoad * 100));
            loadProgressBar.setProgress(Math.min(1.0, currentLoad));
            
            // Update scaling events
            int mlUp = (Integer) stats.get("mlScaleUpEvents");
            int mlDown = (Integer) stats.get("mlScaleDownEvents");
            int reactiveUp = (Integer) stats.get("reactiveScaleUpEvents");
            int reactiveDown = (Integer) stats.get("reactiveScaleDownEvents");
            
            mlScaleUpLabel.setText(String.valueOf(mlUp));
            mlScaleDownLabel.setText(String.valueOf(mlDown));
            reactiveScaleUpLabel.setText(String.valueOf(reactiveUp));
            reactiveScaleDownLabel.setText(String.valueOf(reactiveDown));
            
            // Update prediction
            if (stats.containsKey("lastPrediction")) {
                double predicted = (Double) stats.get("lastPrediction");
                double lower = (Double) stats.get("predictionConfidenceLower");
                double upper = (Double) stats.get("predictionConfidenceUpper");
                
                predictedLoadValueLabel.setText(String.format("%.0f req/hr", predicted));
                confidenceValueLabel.setText(String.format("Range: %.0f - %.0f", lower, upper));
            }
            
        } catch (Exception e) {
            System.err.println("[MLDashboard] Refresh error: " + e.getMessage());
        }
    }
    
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> refreshData()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }
    
    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }
    
    public ScrollPane getView() {
        return view;
    }
}
