package com.soft40051.app.gui;

import com.soft40051.app.auth.AuthService;
import com.soft40051.app.database.*;
import com.soft40051.app.files.*;
import com.soft40051.app.loadbalancer.LoadBalancer;
import com.soft40051.app.scaling.ScalingService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.sql.*;

/**
 * Main Application - Enhanced with Modern Professional UI
 * @version 2.0 (Enhanced GUI)
 */
public class MainApp extends Application {

    // Modern Color Palette
    private static final String PRIMARY_BLUE = "#1976D2";
    private static final String PRIMARY_DARK = "#1565C0";
    private static final String PRIMARY_LIGHT = "#42A5F5";
    private static final String SECONDARY_GRAY = "#424242";
    private static final String SUCCESS_GREEN = "#4CAF50";
    private static final String WARNING_ORANGE = "#FF9800";
    private static final String DANGER_RED = "#F44336";
    private static final String BACKGROUND = "#F5F5F5";
    private static final String SURFACE_WHITE = "#FFFFFF";
    private static final String CARD_BG = "#FAFAFA";
    private static final String TEXT_PRIMARY = "#212121";
    private static final String TEXT_SECONDARY = "#757575";
    private static final String BORDER_LIGHT = "#E0E0E0";
    private static final String HOVER_HIGHLIGHT = "#E3F2FD";

    private Stage primaryStage;
    private static LoadBalancer loadBalancer;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize all backend services
        try {
            System.out.println("========================================");
            System.out.println("  CloudFileSystem v2.0 - Initializing");
            System.out.println("========================================\n");
            
            // Stage 1: Database initialization
            System.out.println("[1/8] Initializing databases...");
            DB.initializeSchema(); // Schema must be initialized first
            SQLiteCache.initCache();
            AuthService.createDefaultAdmin();
            
            // Stage 2: Sync service
            System.out.println("[2/8] Starting database sync service...");
            DatabaseSyncService.startSyncService();
            
            // Stage 3: Conflict resolution
            System.out.println("[3/8] Initializing conflict resolver...");
            ConflictResolver.initializeVersioning();
            
            // Stage 4: File partitioning
            System.out.println("[4/8] Initializing file partitioning...");
            FilePartitioner.initializePartitioning();
            
            // Stage 5: Load balancer
            System.out.println("[5/8] Initializing load balancer...");
            loadBalancer = new LoadBalancer();
            loadBalancer.setLatencySimulation(true, 100, 500);
            
            // Stage 6: Scaling service
            System.out.println("[6/8] Initializing auto-scaling...");
            ScalingService.initialize(loadBalancer);
            ScalingService.startAutoScaling();
            
            // Stage 7: Concurrency control
            System.out.println("[7/8] Enhanced concurrency control active...");
            
            // Stage 8: Complete
            System.out.println("[8/8] All systems initialized\n");
            System.out.println("========================================");
            System.out.println("  CloudFileSystem Ready");
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            showError("Initialization Error", "Failed to initialize system: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Show login screen
        showLoginScreen();
        
        primaryStage.setTitle("CloudFileSystem v2.0 - Login");
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
    }
    
    /**
     * Graceful shutdown
     */
    private void shutdown() {
        System.out.println("\n[Shutdown] Stopping services...");
        
        try {
            DatabaseSyncService.stopSyncService();
            ScalingService.stopAutoScaling();
            System.out.println("[Shutdown] All services stopped gracefully");
        } catch (Exception e) {
            System.err.println("[Shutdown] Error during shutdown: " + e.getMessage());
        }
    }

    private void showLoginScreen() {
        // Root container
        HBox root = new HBox(0);
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Left branding panel
        VBox brandingPanel = new VBox(30);
        brandingPanel.setPrefWidth(400);
        brandingPanel.setAlignment(Pos.CENTER);
        brandingPanel.setPadding(new Insets(60));
        brandingPanel.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " + PRIMARY_DARK + ", " + PRIMARY_BLUE + ");" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 5, 0);"
        );

        // Branding content
        Label brandTitle = new Label("CloudFileSystem");
        brandTitle.setFont(Font.font("System", FontWeight.BOLD, 36));
        brandTitle.setStyle("-fx-text-fill: white;");

        Label brandSubtitle = new Label("Enterprise Cloud Storage");
        brandSubtitle.setFont(Font.font("System", FontWeight.NORMAL, 15));
        brandSubtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.85);");

        Separator separator = new Separator();
        separator.setMaxWidth(250);
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        VBox features = new VBox(10);
        features.setAlignment(Pos.CENTER);
        features.setMaxWidth(280);
        features.setPadding(new Insets(10, 0, 0, 0));

        String featureStyle = "-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;";
        
        Label feature1 = new Label("ML-Powered Load Prediction");
        feature1.setStyle(featureStyle);

        Label feature2 = new Label("Proactive Auto-Scaling");
        feature2.setStyle(featureStyle);

        Label feature3 = new Label("Real-time Synchronization");
        feature3.setStyle(featureStyle);

        Label feature4 = new Label("Distributed Load Balancing");
        feature4.setStyle(featureStyle);

        Label feature5 = new Label("Secure File Sharing");
        feature5.setStyle(featureStyle);
        
        Label feature6 = new Label("Concurrency Control");
        feature6.setStyle(featureStyle);

        features.getChildren().addAll(feature1, feature2, feature3, feature4, feature5, feature6);

        brandingPanel.getChildren().addAll(brandTitle, brandSubtitle, separator, features);

        // Right login panel
        StackPane loginPanel = new StackPane();
        loginPanel.setStyle("-fx-background-color: " + SURFACE_WHITE + ";");
        HBox.setHgrow(loginPanel, Priority.ALWAYS);

        // Login card container
        VBox loginCard = new VBox(25);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(40));
        loginCard.setMaxWidth(450);

        // Title section
        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        Label titleLabel = new Label("Welcome Back");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");

        Label subtitleLabel = new Label("Sign in to your account");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Login form
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(10, 0, 0, 0));

        // Username field
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        usernameLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefHeight(40);
        usernameField.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 14px;"
        );
        usernameField.setOnMouseEntered(e -> usernameField.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + PRIMARY_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 14px;"
        ));
        usernameField.setOnMouseExited(e -> {
            if (!usernameField.isFocused()) {
                usernameField.setStyle(
                    "-fx-background-color: " + CARD_BG + ";" +
                    "-fx-border-color: " + BORDER_LIGHT + ";" +
                    "-fx-border-radius: 6px;" +
                    "-fx-background-radius: 6px;" +
                    "-fx-padding: 8 12;" +
                    "-fx-font-size: 14px;"
                );
            }
        });

        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Password field
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("Password");
        passwordLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        passwordLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(40);
        passwordField.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 14px;"
        );
        passwordField.setOnMouseEntered(e -> passwordField.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + PRIMARY_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 14px;"
        ));
        passwordField.setOnMouseExited(e -> {
            if (!passwordField.isFocused()) {
                passwordField.setStyle(
                    "-fx-background-color: " + CARD_BG + ";" +
                    "-fx-border-color: " + BORDER_LIGHT + ";" +
                    "-fx-border-radius: 6px;" +
                    "-fx-background-radius: 6px;" +
                    "-fx-padding: 8 12;" +
                    "-fx-font-size: 14px;"
                );
            }
        });

        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        formBox.getChildren().addAll(usernameBox, passwordBox);

        // Buttons
        VBox buttonBox = new VBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button loginButton = createPrimaryButton("Login", PRIMARY_BLUE);
        loginButton.setPrefWidth(350);
        
        Button registerButton = createSecondaryButton("Create New Account", PRIMARY_BLUE);
        registerButton.setPrefWidth(350);

        buttonBox.getChildren().addAll(loginButton, registerButton);

        // Status label
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: " + DANGER_RED + "; -fx-font-size: 13px;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(350);
        statusLabel.setAlignment(Pos.CENTER);

        // Login action
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter both username and password");
                return;
            }

            try {
                boolean success = AuthService.login(username, password);
                if (success) {
                    // Trigger sync on login
                    DatabaseSyncService.forceSyncNow();
                    
                    // Get user role
                    String role = getUserRole(username);
                    SessionManager.login(username, role);
                    showDashboard();
                } else {
                    statusLabel.setText("Invalid username or password");
                }
            } catch (Exception ex) {
                showError("Login Error", ex.getMessage());
            }
        });

        // Register action
        registerButton.setOnAction(e -> showRegisterScreen());

        // Allow Enter key to login
        passwordField.setOnAction(e -> loginButton.fire());

        loginCard.getChildren().addAll(titleBox, formBox, buttonBox, statusLabel);
        loginPanel.getChildren().add(loginCard);
        root.getChildren().addAll(brandingPanel, loginPanel);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
    }

    private void showRegisterScreen() {
        // Root container with gradient background
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, " + HOVER_HIGHLIGHT + ", " + BACKGROUND + ");");

        // Registration card
        VBox registerCard = new VBox(25);
        registerCard.setAlignment(Pos.CENTER);
        registerCard.setPadding(new Insets(40));
        registerCard.setMaxWidth(450);
        registerCard.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-background-radius: 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 4);"
        );

        // Title
        Label titleLabel = new Label("Create New Account");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle("-fx-text-fill: " + PRIMARY_BLUE + ";");

        Label subtitleLabel = new Label("Join CloudFileSystem today");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");

        // Form
        VBox formBox = new VBox(15);
        formBox.setAlignment(Pos.CENTER);

        // Username field
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        usernameLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        usernameField.setPrefHeight(40);
        usernameField.setStyle(getInputFieldStyle());
        addInputFieldHoverEffect(usernameField);

        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Password field
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label("Password");
        passwordLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        passwordLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Choose a password");
        passwordField.setPrefHeight(40);
        passwordField.setStyle(getInputFieldStyle());
        addInputFieldHoverEffect(passwordField);

        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // Confirm password field
        VBox confirmBox = new VBox(8);
        Label confirmLabel = new Label("Confirm Password");
        confirmLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
        confirmLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter your password");
        confirmField.setPrefHeight(40);
        confirmField.setStyle(getInputFieldStyle());
        addInputFieldHoverEffect(confirmField);

        confirmBox.getChildren().addAll(confirmLabel, confirmField);

        formBox.getChildren().addAll(usernameBox, passwordBox, confirmBox);

        // Buttons
        VBox buttonBox = new VBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button registerButton = createPrimaryButton("Create Account", SUCCESS_GREEN);
        registerButton.setPrefWidth(350);
        
        Button backButton = createSecondaryButton("Back to Login", SECONDARY_GRAY);
        backButton.setPrefWidth(350);

        buttonBox.getChildren().addAll(registerButton, backButton);

        // Status label
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: " + DANGER_RED + "; -fx-font-size: 13px;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(350);
        statusLabel.setAlignment(Pos.CENTER);

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("All fields are required");
                return;
            }

            if (!password.equals(confirm)) {
                statusLabel.setText("Passwords do not match");
                return;
            }

            try {
                AuthService.register(username, password, "USER");
                showInfo("Success", "Account created successfully! You can now login.");
                showLoginScreen();
            } catch (Exception ex) {
                statusLabel.setText("" + ex.getMessage());
            }
        });

        backButton.setOnAction(e -> showLoginScreen());

        registerCard.getChildren().addAll(titleLabel, subtitleLabel, formBox, buttonBox, statusLabel);
        root.getChildren().add(registerCard);

        Scene scene = new Scene(root, 700, 700);
        primaryStage.setScene(scene);
    }

    private void showDashboard() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Top bar with modern gradient
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(16, 20, 16, 20));
        topBar.setStyle(
            "-fx-background-color: linear-gradient(to right, " + PRIMARY_DARK + ", " + PRIMARY_BLUE + ");" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);"
        );
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Welcome section
        VBox welcomeBox = new VBox(2);
        Label welcomeLabel = new Label("Welcome back, " + SessionManager.getUsername());
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label roleLabel = new Label(SessionManager.getRole() + " Account");
        roleLabel.setStyle("-fx-text-fill: " + HOVER_HIGHLIGHT + "; -fx-font-size: 12px;");
        
        welcomeBox.getChildren().addAll(welcomeLabel, roleLabel);

        // System status indicator
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(6, 12, 6, 12));
        statusBox.setStyle(
            "-fx-background-color: rgba(76, 175, 80, 0.2);" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + SUCCESS_GREEN + ";" +
            "-fx-border-radius: 20px;" +
            "-fx-border-width: 1.5px;"
        );
        
        Label statusDot = new Label("");
        statusDot.setStyle("-fx-text-fill: " + SUCCESS_GREEN + "; -fx-font-size: 16px;");
        
        Label statusText = new Label("All Systems Operational");
        statusText.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        statusBox.getChildren().addAll(statusDot, statusText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = createDangerButton("Logout");
        logoutButton.setOnAction(e -> {
            try {
                DatabaseSyncService.forceSyncNow(); // Sync on logout
                AuthService.logout(SessionManager.getUsername());
                SessionManager.logout();
                showLoginScreen();
                primaryStage.setTitle("CloudFileSystem - Login");
            } catch (Exception ex) {
                showError("Logout Error", ex.getMessage());
            }
        });

        topBar.getChildren().addAll(welcomeBox, statusBox, spacer, logoutButton);

        // Sidebar navigation with modern styling
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 0 1 0 0;"
        );
        sidebar.setPrefWidth(220);

        Label navTitle = new Label("NAVIGATION");
        navTitle.setStyle(
            "-fx-text-fill: " + TEXT_SECONDARY + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 0 0 10 8;"
        );

        Button filesButton = createNavButton(" My Files", false);
        Button sharedButton = createNavButton(" Shared Files", false);
        Button systemButton = createNavButton(" System Status", false);
        Button mlDashButton = createNavButton(" ML Dashboard", false);

        sidebar.getChildren().addAll(navTitle, filesButton, sharedButton, systemButton, mlDashButton);

        if (SessionManager.isAdmin()) {
            // Add separator
            Separator separator = new Separator();
            separator.setStyle("-fx-padding: 10 0;");
            
            Label adminTitle = new Label("ADMINISTRATION");
            adminTitle.setStyle(
                "-fx-text-fill: " + TEXT_SECONDARY + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 0 10 8;"
            );
            
            Button adminButton = createNavButton(" User Management", true);
            adminButton.setOnAction(e -> showAdminPanel(root));
            
            sidebar.getChildren().addAll(separator, adminTitle, adminButton);
        }

        filesButton.setOnAction(e -> showFileManagement(root));
        sharedButton.setOnAction(e -> showSharedFiles(root));
        systemButton.setOnAction(e -> showSystemStatus(root));
        mlDashButton.setOnAction(e -> showMLDashboard(root));

        root.setTop(topBar);
        root.setLeft(sidebar);

        // Show files by default
        showFileManagement(root);

        Scene scene = new Scene(root, 1100, 750);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CloudFileSystem v2.0 - Dashboard");
    }

    private void showFileManagement(BorderPane root) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Page header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("File Management");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        Label subtitleLabel = new Label("Manage your cloud files");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");
        
        VBox headerText = new VBox(4);
        headerText.getChildren().addAll(titleLabel, subtitleLabel);
        
        header.getChildren().add(headerText);

        // File manager view in a card
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );
        card.setPadding(new Insets(20));

        FileManagerView fileManager = new FileManagerView(SessionManager.getUsername());
        card.getChildren().add(fileManager.getView());

        content.getChildren().addAll(header, card);
        root.setCenter(content);
    }

    private void showSharedFiles(BorderPane root) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Page header
        Label titleLabel = new Label("Shared With Me");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        Label subtitleLabel = new Label("Files other users have shared with you");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");
        
        VBox headerBox = new VBox(4);
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Content card
        VBox card = new VBox(15);
        card.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );
        card.setPadding(new Insets(20));

        ListView<String> sharedFilesList = new ListView<>();
        sharedFilesList.setPrefHeight(450);
        sharedFilesList.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;"
        );

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button readButton = createSecondaryButton("Read", PRIMARY_BLUE);
        Button refreshButton = createSecondaryButton("Refresh", SECONDARY_GRAY);

        buttonBar.getChildren().addAll(readButton, refreshButton);

        refreshButton.setOnAction(e -> {
            try {
                Connection con = DB.connect();
                PreparedStatement ps = con.prepareStatement(
                    "SELECT f.filename, f.owner, fp.permission " +
                    "FROM file_permissions fp " +
                    "JOIN files f ON fp.file_id = f.id " +
                    "WHERE fp.shared_with = ?"
                );
                ps.setString(1, SessionManager.getUsername());
                ResultSet rs = ps.executeQuery();

                sharedFilesList.getItems().clear();
                while (rs.next()) {
                    String filename = rs.getString("filename");
                    String owner = rs.getString("owner");
                    String permission = rs.getString("permission");
                    sharedFilesList.getItems().add(filename + " (by " + owner + ") [" + permission + "]");
                }

                con.close();
            } catch (Exception ex) {
                showError("Error", "Failed to load shared files: " + ex.getMessage());
            }
        });

        readButton.setOnAction(e -> {
            String selected = sharedFilesList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showWarning("No Selection", "Please select a file to read");
                return;
            }

            String filename = selected.split(" \\(")[0];

            try {
                String fileContent = FileService.read(filename, SessionManager.getUsername());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("File Content");
                alert.setHeaderText("File: " + filename);

                TextArea textArea = new TextArea(fileContent);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(10);

                alert.getDialogPane().setContent(textArea);
                styleDialog(alert);
                alert.showAndWait();

            } catch (Exception ex) {
                showError("Error", "Failed to read file: " + ex.getMessage());
            }
        });

        refreshButton.fire();

        card.getChildren().addAll(buttonBar, sharedFilesList);
        content.getChildren().addAll(headerBox, card);
        root.setCenter(content);
    }

    private void showSystemStatus(BorderPane root) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Page header
        Label titleLabel = new Label("System Status & Metrics");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        Label subtitleLabel = new Label("Real-time system performance and health");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");
        
        VBox headerBox = new VBox(4);
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Content card
        VBox card = new VBox(15);
        card.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );
        card.setPadding(new Insets(20));

        TextArea statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setPrefRowCount(22);
        statusArea.setFont(Font.font("Consolas", 13));
        statusArea.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );

        Button refreshButton = createPrimaryButton("Refresh Status", PRIMARY_BLUE);

        refreshButton.setOnAction(e -> {
            StringBuilder status = new StringBuilder();
            status.append("═".repeat(70)).append("\n");
            status.append("          CLOUDFILESYSTEM v2.0 - SYSTEM STATUS\n");
            status.append("═".repeat(70)).append("\n\n");

            try {
                // Database sync status
                var syncStatus = DatabaseSyncService.getSyncStatus();
                status.append("DATABASE SYNCHRONIZATION\n");
                status.append("  ├─ Service Running: ").append(syncStatus.get("isServiceRunning")).append("\n");
                status.append("  ├─ Last Sync: ").append(syncStatus.get("lastSyncTime")).append("\n");
                status.append("  └─ Interval: ").append(syncStatus.get("syncIntervalSeconds")).append("s\n\n");

                // Load balancer status
                var lbStats = loadBalancer.getStatistics();
                status.append("LOAD BALANCER\n");
                status.append("  ├─ Total Requests: ").append(lbStats.get("totalRequests")).append("\n");
                status.append("  ├─ Healthy Servers: ").append(lbStats.get("healthyServers")).append("/").append(lbStats.get("totalServers")).append("\n");
                status.append("  └─ Avg Wait Time: ").append(lbStats.get("avgWaitTimeMs")).append("ms\n\n");

                // Scaling status
                var scalingStats = ScalingService.getStatistics();
                status.append("AUTO-SCALING\n");
                status.append("  ├─ Active Containers: ").append(scalingStats.get("currentContainers")).append("\n");
                status.append("  ├─ Scale Up Events: ").append(scalingStats.get("scaleUpEvents")).append("\n");
                status.append("  ├─ Scale Down Events: ").append(scalingStats.get("scaleDownEvents")).append("\n");
                status.append("  ├─ ML-Based Scale Ups: ").append(scalingStats.get("mlScaleUpEvents")).append("\n");
                status.append("  ├─ ML-Based Scale Downs: ").append(scalingStats.get("mlScaleDownEvents")).append("\n");
                status.append("  ├─ Reactive Scale Ups: ").append(scalingStats.get("reactiveScaleUpEvents")).append("\n");
                status.append("  ├─ Reactive Scale Downs: ").append(scalingStats.get("reactiveScaleDownEvents")).append("\n");
                status.append("  └─ Current Load: ").append(String.format("%.1f%%", 
                    ((Double)scalingStats.get("currentLoad")) * 100)).append("\n\n");

                // ML Prediction Service status
                status.append("ML PREDICTION SERVICE\n");
                boolean mlEnabled = (Boolean) scalingStats.get("mlEnabled");
                status.append("  ├─ Mode: ").append(mlEnabled ? "PROACTIVE (ML-based)" : "REACTIVE (fallback)").append("\n");
                status.append("  ├─ Service Status: ").append(mlEnabled ? "Connected" : "Disconnected").append("\n");
                
                if (scalingStats.containsKey("mlPredictions")) {
                    status.append("  ├─ Total Predictions: ").append(scalingStats.get("mlPredictions")).append("\n");
                    status.append("  ├─ RMSE: ").append(String.format("%.2f req/hr", scalingStats.get("mlRMSE"))).append("\n");
                    status.append("  ├─ MAE: ").append(String.format("%.2f req/hr", scalingStats.get("mlMAE"))).append("\n");
                    status.append("  └─ MAPE: ").append(String.format("%.2f%%", scalingStats.get("mlMAPE"))).append("\n");
                } else {
                    status.append("  └─ Predictions: Not available\n");
                }
                
                // Last prediction info
                if (scalingStats.containsKey("lastPrediction")) {
                    status.append("\nLAST PREDICTION\n");
                    status.append("  ├─ Predicted Load: ").append(String.format("%.0f req/hr", scalingStats.get("lastPrediction"))).append("\n");
                    status.append("  └─ Confidence: [").append(String.format("%.0f", scalingStats.get("predictionConfidenceLower")))
                          .append(", ").append(String.format("%.0f", scalingStats.get("predictionConfidenceUpper"))).append("] req/hr\n\n");
                } else {
                    status.append("\n");
                }

                // Concurrency status
                var lockStats = com.soft40051.app.concurrency.FileLock.getStatistics();
                status.append("CONCURRENCY CONTROL\n");
                status.append("  ├─ Total Acquisitions: ").append(lockStats.get("totalAcquisitions")).append("\n");
                status.append("  ├─ Avg Wait Time: ").append(lockStats.get("avgWaitTimeMs")).append("ms\n");
                status.append("  ├─ Max Wait Time: ").append(lockStats.get("maxWaitTimeMs")).append("ms\n");
                status.append("  └─ Fair Semaphore: ").append(lockStats.get("isFairSemaphore")).append("\n\n");

                status.append("═".repeat(70)).append("\n");
                status.append("✅ All systems operational\n");
                status.append("═".repeat(70));

            } catch (Exception ex) {
                status.append("ERROR: Failed to retrieve status\n");
                status.append(ex.getMessage());
            }

            statusArea.setText(status.toString());
        });

        // Auto-refresh on load
        refreshButton.fire();

        card.getChildren().addAll(refreshButton, statusArea);
        content.getChildren().addAll(headerBox, card);
        root.setCenter(content);
    }

    private void showMLDashboard(BorderPane root) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Page header
        Label titleLabel = new Label("ML Prediction Dashboard");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        Label subtitleLabel = new Label("Real-time ML-based load predictions and scaling metrics");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + ";");
        
        VBox headerBox = new VBox(4);
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // ML Dashboard view - already contains proper styling
        MLDashboardView mlDashboard = new MLDashboardView();
        javafx.scene.control.ScrollPane dashboardView = mlDashboard.getView();
        VBox.setVgrow(dashboardView, Priority.ALWAYS);

        content.getChildren().addAll(headerBox, dashboardView);
        root.setCenter(content);
    }

    private void showAdminPanel(BorderPane root) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: " + BACKGROUND + ";");

        // Page header
        Label titleLabel = new Label("User Management");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
        
        Label subtitleLabel = new Label("Administrator Panel");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setStyle("-fx-text-fill: " + WARNING_ORANGE + "; -fx-font-weight: bold;");
        
        VBox headerBox = new VBox(4);
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Content card
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );
        card.setPadding(new Insets(20));

        AdminPanelView adminPanel = new AdminPanelView();
        card.getChildren().add(adminPanel.getView());

        content.getChildren().addAll(headerBox, card);
        root.setCenter(content);
    }

    // Helper method to create styled primary buttons
    private Button createPrimaryButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefHeight(44);
        button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: derive(" + color + ", -10%);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        ));
        
        return button;
    }

    // Helper method to create styled secondary buttons
    private Button createSecondaryButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefHeight(44);
        button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 24;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        ));
        
        return button;
    }

    // Helper method to create danger buttons
    private Button createDangerButton(String text) {
        Button button = new Button(text);
        button.setStyle(
            "-fx-background-color: " + DANGER_RED + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: derive(" + DANGER_RED + ", -15%);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: " + DANGER_RED + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        ));
        
        return button;
    }

    // Helper method to create navigation buttons
    private Button createNavButton(String text, boolean isAdmin) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPrefHeight(42);
        
        String baseColor = isAdmin ? WARNING_ORANGE : PRIMARY_BLUE;
        
        button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: normal;" +
            "-fx-padding: 10 15;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: center-left;"
        );
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: " + HOVER_HIGHLIGHT + ";" +
            "-fx-text-fill: " + baseColor + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 15;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: center-left;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: normal;" +
            "-fx-padding: 10 15;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-alignment: center-left;"
        ));
        
        return button;
    }

    // Helper method for input field styling
    private String getInputFieldStyle() {
        return "-fx-background-color: " + CARD_BG + ";" +
               "-fx-border-color: " + BORDER_LIGHT + ";" +
               "-fx-border-radius: 6px;" +
               "-fx-background-radius: 6px;" +
               "-fx-padding: 8 12;" +
               "-fx-font-size: 14px;";
    }

    // Helper method to add hover effect to input fields
    private void addInputFieldHoverEffect(TextInputControl field) {
        field.setOnMouseEntered(e -> field.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + PRIMARY_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 14px;"
        ));
        
        field.setOnMouseExited(e -> {
            if (!field.isFocused()) {
                field.setStyle(getInputFieldStyle());
            }
        });
    }

    // Helper method to style dialogs
    private void styleDialog(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 1px;"
        );
    }

    private String getUserRole(String username) throws Exception {
        try (Connection con = DB.connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT role FROM users WHERE username = ?")) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                } else {
                    throw new Exception("User not found");
                }
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}