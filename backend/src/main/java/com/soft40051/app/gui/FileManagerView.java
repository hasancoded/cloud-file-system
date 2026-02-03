package com.soft40051.app.gui;

import com.soft40051.app.files.FileMetadata;
import com.soft40051.app.files.FilePermissions;
import com.soft40051.app.files.FileService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.Optional;

public class FileManagerView {
    
    // Modern Color Palette (matching MainApp)
    private static final String PRIMARY_BLUE = "#1976D2";
    private static final String PRIMARY_LIGHT = "#42A5F5";
    private static final String SUCCESS_GREEN = "#4CAF50";
    private static final String DANGER_RED = "#F44336";
    private static final String SURFACE_WHITE = "#FFFFFF";
    private static final String CARD_BG = "#FAFAFA";
    private static final String TEXT_PRIMARY = "#212121";
    private static final String TEXT_SECONDARY = "#757575";
    private static final String BORDER_LIGHT = "#E0E0E0";
    private static final String HOVER_HIGHLIGHT = "#E3F2FD";
    
    private String currentUser;
    private VBox view;
    private ListView<String> fileListView;
    private ObservableList<String> fileList;

    public FileManagerView(String username) {
        this.currentUser = username;
        this.fileList = FXCollections.observableArrayList();
        this.view = createView();
        refreshFileList();
    }

    private VBox createView() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(0));

        // Action buttons with modern styling
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(0, 0, 10, 0));

        Button createButton = createActionButton("Create File", SUCCESS_GREEN, true);
        Button readButton = createActionButton("Read", PRIMARY_BLUE, false);
        Button updateButton = createActionButton("Update", PRIMARY_LIGHT, false);
        Button deleteButton = createActionButton("Delete", DANGER_RED, true);
        Button shareButton = createActionButton("Share", PRIMARY_BLUE, false);
        Button refreshButton = createActionButton("Refresh", TEXT_SECONDARY, false);

        buttonBar.getChildren().addAll(createButton, readButton, updateButton, deleteButton, shareButton, refreshButton);

        // File list header
        HBox listHeader = new HBox();
        listHeader.setPadding(new Insets(10, 15, 10, 15));
        listHeader.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-width: 0 0 2 0;"
        );
        
        Label headerLabel = new Label("Your Files");
        headerLabel.setStyle(
            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;"
        );
        
        listHeader.getChildren().add(headerLabel);

        // Styled file list
        fileListView = new ListView<>(fileList);
        fileListView.setPrefHeight(400);
        fileListView.setStyle(
            "-fx-background-color: " + SURFACE_WHITE + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-width: 1px;"
        );
        
        // Custom cell factory for better styling
        fileListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(
                        "-fx-padding: 12 15;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";"
                    );
                    
                    // Hover effect
                    setOnMouseEntered(e -> {
                        if (!isEmpty()) {
                            setStyle(
                                "-fx-background-color: " + HOVER_HIGHLIGHT + ";" +
                                "-fx-padding: 12 15;" +
                                "-fx-font-size: 13px;" +
                                "-fx-text-fill: " + PRIMARY_BLUE + ";" +
                                "-fx-font-weight: bold;"
                            );
                        }
                    });
                    
                    setOnMouseExited(e -> {
                        if (!isEmpty() && !isSelected()) {
                            setStyle(
                                "-fx-padding: 12 15;" +
                                "-fx-font-size: 13px;" +
                                "-fx-text-fill: " + TEXT_PRIMARY + ";"
                            );
                        }
                    });
                }
            }
        });

        // Button actions
        createButton.setOnAction(e -> createFile());
        readButton.setOnAction(e -> readFile());
        updateButton.setOnAction(e -> updateFile());
        deleteButton.setOnAction(e -> deleteFile());
        shareButton.setOnAction(e -> shareFile());
        refreshButton.setOnAction(e -> refreshFileList());

        VBox listContainer = new VBox(0);
        listContainer.getChildren().addAll(listHeader, fileListView);

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

    private void createFile() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New File");
        dialog.setHeaderText("📄 Create a new file in your cloud storage");
        
        // Style the dialog
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField filenameField = new TextField();
        filenameField.setPromptText("Enter filename (e.g., document.txt)");
        filenameField.setPrefWidth(350);
        styleInputField(filenameField);

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Enter file content here...");
        contentArea.setPrefRowCount(8);
        contentArea.setPrefWidth(350);
        contentArea.setWrapText(true);
        styleTextArea(contentArea);

        Label filenameLabel = new Label("Filename:");
        filenameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        
        Label contentLabel = new Label("Content:");
        contentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        grid.add(filenameLabel, 0, 0);
        grid.add(filenameField, 0, 1);
        grid.add(contentLabel, 0, 2);
        grid.add(contentArea, 0, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String filename = filenameField.getText().trim();
            String content = contentArea.getText();

            if (filename.isEmpty()) {
                showWarning("Invalid Input", "Please enter a filename");
                return;
            }

            try {
                FileService.create(filename, content, currentUser);
                showInfo("Success", "File created: " + filename);
                refreshFileList();
            } catch (Exception e) {
                showError("Error", "Failed to create file: " + e.getMessage());
            }
        }
    }

    private void readFile() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a file to read");
            return;
        }

        String filename = extractFilename(selected);

        try {
            String content = FileService.read(filename, currentUser);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("File Content");
            alert.setHeaderText("" + filename);
            
            TextArea textArea = new TextArea(content);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(15);
            textArea.setPrefWidth(500);
            styleTextArea(textArea);
            
            alert.getDialogPane().setContent(textArea);
            styleDialog(alert);
            alert.showAndWait();
            
        } catch (Exception e) {
            showError("Error", "Failed to read file: " + e.getMessage());
        }
    }

    private void updateFile() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a file to update");
            return;
        }

        String filename = extractFilename(selected);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update File");
        dialog.setHeaderText("Update content for: " + filename);
        styleDialog(dialog);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label label = new Label("New Content:");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Enter new file content...");
        contentArea.setPrefRowCount(10);
        contentArea.setPrefWidth(450);
        contentArea.setWrapText(true);
        styleTextArea(contentArea);

        content.getChildren().addAll(label, contentArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newContent = contentArea.getText();

            try {
                FileService.update(filename, newContent, currentUser);
                showInfo("Success", "File updated: " + filename);
            } catch (Exception e) {
                showError("Error", "Failed to update file: " + e.getMessage());
            }
        }
    }

    private void deleteFile() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a file to delete");
            return;
        }

        String filename = extractFilename(selected);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete file: " + filename + "?");
        confirm.setContentText("This action cannot be undone. The file will be permanently deleted.");
        styleDialog(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                FileService.delete(filename, currentUser);
                showInfo("Success", "File deleted: " + filename);
                refreshFileList();
            } catch (Exception e) {
                showError("Error", "Failed to delete file: " + e.getMessage());
            }
        }
    }

    private void shareFile() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select a file to share");
            return;
        }

        String filename = extractFilename(selected);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Share File");
        dialog.setHeaderText("Share: " + filename);
        styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        Label userLabel = new Label("Share with user:");
        userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_PRIMARY + ";");
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setPrefWidth(300);
        styleInputField(usernameField);

        Label permLabel = new Label("Permission level:");
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

        grid.add(userLabel, 0, 0);
        grid.add(usernameField, 0, 1);
        grid.add(permLabel, 0, 2);
        grid.add(permissionBox, 0, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String shareWith = usernameField.getText().trim();
            String permission = permissionBox.getValue();

            if (shareWith.isEmpty()) {
                showWarning("Invalid Input", "Please enter a username");
                return;
            }

            try {
                FilePermissions.grant(filename, shareWith, permission);
                showInfo("Success", "File shared with " + shareWith + " (" + permission + ")");
            } catch (Exception e) {
                showError("Error", "Failed to share file: " + e.getMessage());
            }
        }
    }

    private void refreshFileList() {
        try {
            List<String> files = FileMetadata.listByOwner(currentUser);
            fileList.clear();
            fileList.addAll(files);
        } catch (Exception e) {
            showError("Error", "Failed to load files: " + e.getMessage());
        }
    }

    private String extractFilename(String listItem) {
        // If listItem is just filename, return it
        return listItem.split(" ")[0];
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

    private void styleTextArea(TextArea area) {
        area.setStyle(
            "-fx-background-color: " + CARD_BG + ";" +
            "-fx-border-color: " + BORDER_LIGHT + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 10;" +
            "-fx-font-size: 13px;" +
            "-fx-font-family: 'Consolas', 'Monaco', monospace;"
        );
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