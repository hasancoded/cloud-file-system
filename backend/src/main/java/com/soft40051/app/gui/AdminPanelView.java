package com.soft40051.app.gui;

import com.soft40051.app.auth.AuthService;
import com.soft40051.app.database.DB;
import com.soft40051.app.files.FilePermissions;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminPanelView {

    // Modern Color Palette (matching MainApp)
    private static final String PRIMARY_BLUE = "#1976D2";
    private static final String PRIMARY_LIGHT = "#42A5F5";
    private static final String WARNING_ORANGE = "#FF9800";
    private static final String DANGER_RED = "#F44336";
    private static final String SECONDARY_GRAY = "#424242";
    private static final String SURFACE_WHITE = "#FFFFFF";
    private static final String CARD_BG = "#FAFAFA";
    private static final String TEXT_PRIMARY = "#212121";
    private static final String TEXT_SECONDARY = "#757575";
    private static final String BORDER_LIGHT = "#E0E0E0";
    private static final String HOVER_HIGHLIGHT = "#E3F2FD";

    private VBox view;
    private ListView<String> userListView;
    private ObservableList<String> userList;

    public AdminPanelView() {
        this.userList = FXCollections.observableArrayList();
        this.view = createView();
        refreshUserList();
    }

    private VBox createView() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(0));

        // Action Buttons with modern styling
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(0, 0, 10, 0));

        Button promoteButton = createActionButton("Promote to Admin", WARNING_ORANGE, true);
        Button demoteButton = createActionButton("Demote to User", SECONDARY_GRAY, false);
        Button deleteButton = createActionButton("Delete User", DANGER_RED, true);
        Button permissionsButton = createActionButton("Manage Permissions", PRIMARY_BLUE, false);
        Button refreshButton = createActionButton("Refresh", TEXT_SECONDARY, false);

        buttonBar.getChildren().addAll(promoteButton, demoteButton, deleteButton, permissionsButton, refreshButton);

        // User list header
        HBox listHeader = new HBox();
        listHeader.setPadding(new Insets(10, 15, 10, 15));
        listHeader.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 0 0 2 0;"
        );
        
        Label headerLabel = new Label("All Users");
        headerLabel.setStyle(
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;"
        );
        
        listHeader.getChildren().add(headerLabel);

        // Styled user list
        userListView = new ListView<>(userList);
        userListView.setPrefHeight(400);
        userListView.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-width: 1px;"
        );

        // Custom cell factory for better styling with role badges
        userListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Parse user info
                    String username = item.split(" \\(")[0];
                    boolean isAdmin = item.contains("(ADMIN)");
                    
                    // Create HBox for user entry
                    HBox userBox = new HBox(10);
                    userBox.setAlignment(Pos.CENTER_LEFT);
                    
                    // Username label
                    Label nameLabel = new Label(username);
                    nameLabel.setStyle(
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-weight: bold;"
                    );
                    
                    // Role badge
                    Label roleBadge = new Label(isAdmin ? "ADMIN" : "USER");
                    roleBadge.setStyle(
                        "-fx-background-color: " + (isAdmin ? WARNING_ORANGE : PRIMARY_BLUE) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 8;" +
                        "-fx-background-radius: 10px;"
                    );
                    
                    userBox.getChildren().addAll(nameLabel, roleBadge);
                    
                    // Add shared files info if present
                    if (item.contains("—")) {
                        String filesInfo = item.substring(item.indexOf("—"));
                        Label filesLabel = new Label(filesInfo);
                        filesLabel.setStyle(
                            "-fx-font-size: 11px;" +
                            "-fx-text-fill: " + TEXT_SECONDARY + ";" +
                            "-fx-padding: 0 0 0 10;"
                        );
                        userBox.getChildren().add(filesLabel);
                    }
                    
                    setGraphic(userBox);
                    setText(null);
                    
                    setStyle("-fx-padding: 10 15;");
                    
                    // Hover effect
                    setOnMouseEntered(e -> {
                        if (!isEmpty()) {
                            setStyle(
                                "-fx-background-color: " + HOVER_HIGHLIGHT + ";" +
                                "-fx-padding: 10 15;"
                            );
                        }
                    });
                    
                    setOnMouseExited(e -> {
                        if (!isEmpty() && !isSelected()) {
                            setStyle("-fx-padding: 10 15;");
                        }
                    });
                }
            }
        });

        // Button Actions
        promoteButton.setOnAction(e -> promoteUser());
        demoteButton.setOnAction(e -> demoteUser());
        deleteButton.setOnAction(e -> deleteUser());
        permissionsButton.setOnAction(e -> managePermissions());
        refreshButton.setOnAction(e -> refreshUserList());

        VBox listContainer = new VBox(0);
        listContainer.getChildren().addAll(listHeader, userListView);

        container.getChildren().addAll(buttonBar, listContainer);

        return container;
    }

    // Helper method to create styled action buttons
    private Button createActionButton(String text, String color, boolean filled) {
        Button button = new Button(text);
        button.setPrefHeight(36);
        
        if (filled) {
            button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;"
            );
            
            button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: derive(" + color + ", -10%);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
            ));
            
            button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;"
            ));
        } else {
            button.setStyle(
                "-fx-background-color: " + SURFACE_WHITE + ";" +
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-border-color: " + BORDER_LIGHT + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;"
            );
            
            button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-border-color: " + color + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;"
            ));
            
            button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + SURFACE_WHITE + ";" +
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-border-color: " + BORDER_LIGHT + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-cursor: hand;"
            ));
        }
        
        return button;
    }

    // --- User Management ---
    private void promoteUser() {
        String username = getSelectedUsername();
        if (username == null) return;

        if (username.equals("admin")) {
            showWarning("Invalid Action", "Cannot modify default admin");
            return;
        }

        try {
            AuthService.promoteToAdmin(username);
            showInfo("Success", "" + username + " promoted to ADMIN");
            refreshUserList();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void demoteUser() {
        String username = getSelectedUsername();
        if (username == null) return;

        if (username.equals("admin")) {
            showWarning("Invalid Action", "Cannot modify default admin");
            return;
        }

        try {
            AuthService.demoteToUser(username);
            showInfo("Success", "" + username + " demoted to USER");
            refreshUserList();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void deleteUser() {
        String username = getSelectedUsername();
        if (username == null) return;

        if (username.equals("admin")) {
            showWarning("Invalid Action", "Cannot delete default admin");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete user: " + username + "?");
        confirm.setContentText("This will permanently delete the user and all their files. This action cannot be undone.");
        styleDialog(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                AuthService.deleteUser(username);
                showInfo("Success", "User deleted: " + username);
                refreshUserList();
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }

    // --- File Permissions ---
    private void managePermissions() {
        String username = getSelectedUsername();
        if (username == null) return;

        Dialog<ButtonType> actionDialog = new Dialog<>();
        actionDialog.setTitle("File Permission Action");
        actionDialog.setHeaderText("Manage permissions for: " + username);
        styleDialog(actionDialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label label = new Label("Select action:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        ToggleGroup group = new ToggleGroup();
        
        RadioButton grantRadio = new RadioButton("Grant file access");
        grantRadio.setToggleGroup(group);
        grantRadio.setSelected(true);
        grantRadio.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_PRIMARY + ";");
        
        RadioButton revokeRadio = new RadioButton("Revoke file access");
        revokeRadio.setToggleGroup(group);
        revokeRadio.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_PRIMARY + ";");

        content.getChildren().addAll(label, grantRadio, revokeRadio);

        actionDialog.getDialogPane().setContent(content);
        actionDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> actionResult = actionDialog.showAndWait();
        if (actionResult.isPresent() && actionResult.get() == ButtonType.OK) {
            if (grantRadio.isSelected()) {
                grantPermission(username);
            } else {
                revokePermission(username);
            }
        }
    }

    private void grantPermission(String username) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Grant File Access");
        dialog.setHeaderText("Grant access for: " + username);
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        Label fileLabel = new Label("Filename:");
        fileLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        
        TextField filenameField = new TextField();
        filenameField.setPromptText("Enter filename");
        filenameField.setPrefWidth(300);
        styleInputField(filenameField);

        Label permLabel = new Label("Permission:");
        permLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        
        ComboBox<String> permissionBox = new ComboBox<>();
        permissionBox.getItems().addAll("READ", "WRITE");
        permissionBox.setValue("READ");
        permissionBox.setPrefWidth(300);
        permissionBox.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-font-size: 13px;"
        );

        grid.add(fileLabel, 0, 0);
        grid.add(filenameField, 0, 1);
        grid.add(permLabel, 0, 2);
        grid.add(permissionBox, 0, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String filename = filenameField.getText().trim();
            String permission = permissionBox.getValue();

            if (filename.isEmpty()) {
                showError("Invalid Input", "Please enter a filename");
                return;
            }

            try {
                FilePermissions.grant(filename, username, permission);
                showInfo("Success", "Granted " + permission + " on " + filename + " to " + username);
                refreshUserList();
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }

    private void revokePermission(String username) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Revoke File Access");
        dialog.setHeaderText("Revoke access for: " + username);
        styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label label = new Label("Filename:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        TextField filenameField = new TextField();
        filenameField.setPromptText("Enter filename");
        filenameField.setPrefWidth(350);
        styleInputField(filenameField);

        content.getChildren().addAll(label, filenameField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String filename = filenameField.getText().trim();

            if (filename.isEmpty()) {
                showError("Invalid Input", "Please enter a filename");
                return;
            }

            try {
                FilePermissions.revoke(filename, username);
                showInfo("Success", "Revoked permission on " + filename + " for " + username);
                refreshUserList();
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }
    
    // --- Helper Methods ---
    private String getSelectedUsername() {
        String selected = userListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a user");
            return null;
        }
        return selected.split(" \\(")[0];
    }

    private void refreshUserList() {
        try {
            Connection con = DB.connect();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username, role FROM users");

            userList.clear();
            while (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                List<String> sharedFiles = getFilesSharedWith(username);
                String filesStr = sharedFiles.isEmpty() ? "" : " — Files: " + String.join(", ", sharedFiles);
                userList.add(username + " (" + role + ")" + filesStr);
            }

            con.close();
        } catch (Exception e) {
            showError("Error", "Failed to load users: " + e.getMessage());
        }
    }

    private List<String> getFilesSharedWith(String username) {
        List<String> files = new ArrayList<>();
        try {
            Connection con = DB.connect();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT f.filename FROM files f JOIN file_permissions p ON f.id = p.file_id WHERE p.shared_with = ?"
            );
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                files.add(rs.getString("filename"));
            }
            con.close();
        } catch (Exception e) {
            System.err.println("Failed to fetch shared files: " + e.getMessage());
        }
        return files;
    }

    public VBox getView() {
        return view;
    }

    // Styling helper methods
    private void styleInputField(TextField field) {
        field.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 13px;"
        );
        
        field.setOnMouseEntered(e -> field.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + PRIMARY_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 8 12;" +
            "-fx-font-size: 13px;"
        ));
        
        field.setOnMouseExited(e -> {
            if (!field.isFocused()) {
                field.setStyle(
                    "-fx-background-color: " + CARD_BG + ";" +
                    "-fx-border-color: " + BORDER_LIGHT + ";" +
                    "-fx-border-radius: 6px;" +
                    "-fx-background-radius: 6px;" +
                    "-fx-padding: 8 12;" +
                    "-fx-font-size: 13px;"
                );
            }
        });
    }

    private void styleDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 1px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);"
        );
    }

    private void styleDialog(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 1px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);"
        );
    }

    // --- Alerts ---
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
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
        alert.setHeaderText("Warning");
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }
}