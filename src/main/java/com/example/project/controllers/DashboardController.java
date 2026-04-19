package com.example.project.controllers;

import com.example.project.database.DatabaseManager;
import com.example.project.models.ImageRecord;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private GridPane imageGrid;

    // Internal state cache for dynamic restructure
    private java.util.List<StackPane> allCells = new java.util.ArrayList<>();
    private int currentCols = -1;

    @FXML
    private ImageView selectedImagePreview;

    @FXML
    private Label imageCountLabel;

    @FXML
    private javafx.scene.control.ScrollPane gridScrollPane;

    @FXML
    private javafx.scene.layout.BorderPane mainSplitContainer;

    @FXML
    private javafx.scene.control.ScrollPane metadataSidebar;

    @FXML
    private javafx.scene.layout.HBox topRightActions;

    @FXML
    private javafx.scene.layout.HBox topBar;

    @FXML
    private javafx.scene.control.TextArea annotationArea;

    @FXML
    private javafx.scene.control.Button syncMetadataBtn;

    @FXML
    private javafx.scene.control.Button removeMetadataBtn;

    @FXML
    private javafx.scene.control.Button openFullscreenBtn;

    private java.util.Map<String, String> annotations;
    private StackPane currentActiveCell = null;
    private String currentActiveFile = null;
    private DatabaseManager db = DatabaseManager.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Init state
        annotations = db.loadAllAnnotations();

        // 1. The Upload Zone cell
        createUploadPromptCell();

        // 2. Populate from DB
        java.util.List<ImageRecord> library = db.loadLibrary();
        for (int i = 0; i < library.size(); i++) {
            createAndAddImageCell(library.get(i), i + 1);
        }

        imageCountLabel.setText("Showing " + library.size() + " items from recent import");

        // Select first by default
        if (!library.isEmpty()) {
            setSelectedPreview(library.get(0).getFilePath());
        }

        // --- Sidebar Shrink Binding ---
        // Sacrifices right-panel width (max 450) if main area drastically decreases,
        // prioritizing 450px minimum width for the strictly critical grid library.
        javafx.beans.binding.DoubleBinding dynamicRightPanelWidth = new javafx.beans.binding.DoubleBinding() {
            {
                super.bind(mainSplitContainer.widthProperty());
            }

            @Override
            protected double computeValue() {
                double totalWidth = mainSplitContainer.getWidth();
                return Math.max(0, Math.min(450, totalWidth - 450));
            }
        };

        metadataSidebar.prefWidthProperty().bind(dynamicRightPanelWidth);
        metadataSidebar.minWidthProperty().bind(dynamicRightPanelWidth);
        metadataSidebar.maxWidthProperty().bind(dynamicRightPanelWidth);

        topRightActions.prefWidthProperty().bind(dynamicRightPanelWidth);
        topRightActions.minWidthProperty().bind(dynamicRightPanelWidth);
        topRightActions.maxWidthProperty().bind(dynamicRightPanelWidth);

        // --- Fluid Restructure Layout Engine ---
        javafx.application.Platform.runLater(() -> {
            gridScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                double width = newBounds.getWidth();
                if (width <= 20)
                    return;

                double minThumbWidth = 200; // Optimal card width to trigger column collapses
                double hgap = imageGrid.getHgap();

                // Determine raw integer column count mapping
                int calcCols = (int) ((width - 40 + hgap) / (minThumbWidth + hgap));
                int cols = Math.max(1, Math.min(4, calcCols)); // Constrain to 4

                // If the column layout shifted natively, reconstruct the grid safely
                if (cols != currentCols) {
                    currentCols = cols;
                    rebuildGridPane(cols);
                }
            });
        });

        // Instantly force 4 columns on startup pipeline to murder the 5-column flash
        // bug
        currentCols = 4;
        rebuildGridPane(4);

        // Map Sync Action
        syncMetadataBtn.setOnAction(e -> {
            if (currentActiveCell != null && currentActiveFile != null) {
                String text = annotationArea.getText();
                boolean isEmpty = text == null || text.trim().isEmpty();

                if (!isEmpty) {
                    annotations.put(currentActiveFile, text);
                    db.saveAnnotation(currentActiveFile, text);
                    if (!hasHeartBadge(currentActiveCell)) {
                        Label heartBadge = createHeartBadge();
                        StackPane.setAlignment(heartBadge, javafx.geometry.Pos.TOP_LEFT);
                        StackPane.setMargin(heartBadge, new javafx.geometry.Insets(0, 0, 0, 10)); // Adjusted inset

                        if (!currentActiveCell.getChildren().isEmpty()
                                && currentActiveCell.getChildren().get(0) instanceof StackPane) {
                            ((StackPane) currentActiveCell.getChildren().get(0)).getChildren().add(heartBadge);
                        } else {
                            // Fallback
                            currentActiveCell.getChildren().add(heartBadge);
                        }
                    }
                } else {
                    annotations.remove(currentActiveFile);
                    db.deleteAnnotation(currentActiveFile);
                    removeHeartBadge(currentActiveCell);
                }
            }
        });

        if (removeMetadataBtn != null) {
            removeMetadataBtn.setOnAction(e -> {
                System.out.println("Clear Annotation Button triggered");
                if (currentActiveCell != null && currentActiveFile != null) {
                    annotationArea.setText("");
                    annotations.remove(currentActiveFile);
                    db.deleteAnnotation(currentActiveFile);
                    removeHeartBadge(currentActiveCell);
                    System.out.println("Purged text, removed from memory, deleted from DB, and detached badge for: " + currentActiveFile);
                } else {
                    System.out.println("Ignored clear because active cell or active file state is null.");
                }
            });
        }

        if (openFullscreenBtn != null) {
            openFullscreenBtn.setOnAction(e -> {
                if (currentActiveFile != null) {
                    fetchAndOpenImageWindow(currentActiveFile);
                }
            });
        }
    }

    private boolean hasHeartBadge(StackPane cell) {
        if (cell == null || cell.getChildren().isEmpty())
            return false;
        if (cell.getChildren().get(0) instanceof StackPane) {
            StackPane wrapper = (StackPane) cell.getChildren().get(0);
            for (javafx.scene.Node n : wrapper.getChildren()) {
                if (n instanceof Label && n.getStyleClass().contains("heart-badge")) {
                    return true;
                }
            }
        } else {
            for (javafx.scene.Node n : cell.getChildren()) {
                if (n instanceof Label && n.getStyleClass().contains("heart-badge"))
                    return true;
            }
        }
        return false;
    }

    private void removeHeartBadge(StackPane cell) {
        if (cell == null || cell.getChildren().isEmpty()) return;
        
        if (cell.getChildren().get(0) instanceof StackPane) {
            StackPane wrapper = (StackPane) cell.getChildren().get(0);
            javafx.scene.Node toRemove = null;
            for (javafx.scene.Node n : wrapper.getChildren()) {
                if (n instanceof Label && n.getStyleClass().contains("heart-badge")) {
                    toRemove = n;
                    break;
                }
            }
            if (toRemove != null) wrapper.getChildren().remove(toRemove);
        } else {
            javafx.scene.Node toRemove = null;
            for (javafx.scene.Node n : cell.getChildren()) {
                if (n instanceof Label && n.getStyleClass().contains("heart-badge")) {
                    toRemove = n;
                    break;
                }
            }
            if (toRemove != null) cell.getChildren().remove(toRemove);
        }
    }

    private void rebuildGridPane(int cols) {
        imageGrid.getChildren().clear();
        imageGrid.getColumnConstraints().clear();

        // Generate flexible evenly distributed columns
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            imageGrid.getColumnConstraints().add(cc);
        }

        // Re-inject the cached cells into the fresh grid bounds
        for (int i = 0; i < allCells.size(); i++) {
            StackPane cell = allCells.get(i);

            // Fixed height logic
            cell.setPrefHeight(190);
            cell.setMinHeight(190);
            cell.setMaxHeight(190);

            javafx.scene.Node baseNode = cell.getChildren().get(0);

            if (baseNode instanceof StackPane) {
                StackPane wrapper = (StackPane) baseNode;
                if (!wrapper.getChildren().isEmpty() && wrapper.getChildren().get(0) instanceof ImageView) {
                    ImageView iv = (ImageView) wrapper.getChildren().get(0);
                    // Sever old bindings to prevent cyclical geometry crashing
                    iv.fitWidthProperty().unbind();
                    iv.fitHeightProperty().unbind();

                    // Tie dimensions safely to outermost viewport rather than unstable grid
                    // internals
                    iv.fitWidthProperty().bind(gridScrollPane.widthProperty()
                            .subtract(imageGrid.getHgap() * (cols - 1) + 60)
                            .divide(cols));

                    iv.setFitHeight(180);
                }
            } else if (baseNode instanceof ImageView) {
                ImageView iv = (ImageView) baseNode;
                iv.fitWidthProperty().unbind();
                iv.fitHeightProperty().unbind();
                iv.fitWidthProperty().bind(gridScrollPane.widthProperty()
                        .subtract(imageGrid.getHgap() * (cols - 1) + 60)
                        .divide(cols));
                iv.setFitHeight(180);
            }

            imageGrid.add(cell, i % cols, i / cols);
        }
    }

    private void createUploadPromptCell() {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("upload-cell");
        cell.setFocusTraversable(true);

        VBox content = new VBox(10);
        content.setAlignment(javafx.geometry.Pos.CENTER);

        Label icon = new Label("+");
        icon.getStyleClass().add("upload-icon");

        Label text = new Label("Drag & Drop Image\nor Click to Browse");
        text.getStyleClass().add("upload-text");

        content.getChildren().addAll(icon, text);
        cell.getChildren().add(content);

        cell.setOnMouseClicked(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.nef"));
            File selectedFile = chooser.showOpenDialog(cell.getScene().getWindow());
            if (selectedFile != null) {
                importImageToLibrary(selectedFile);
            }
        });

        allCells.add(cell);
    }

    private void importImageToLibrary(File sourceFile) {
        try {
            File destFolder = new File(DatabaseManager.IMAGES_FOLDER);
            File destFile = new File(destFolder, sourceFile.getName());

            if (destFile.exists()) {
                destFile = new File(destFolder, System.currentTimeMillis() + "_" + sourceFile.getName());
            }

            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            ImageRecord record = new ImageRecord(0, destFile.getAbsolutePath(), destFile.getName(),
                    System.currentTimeMillis());
            db.insertImageRecord(record);

            createAndAddImageCell(record, allCells.size());

            int count = allCells.size() - 1;
            imageCountLabel.setText("Showing " + count + " items from recent import");

            rebuildGridPane(currentCols);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createAndAddImageCell(ImageRecord record, int index) {
        String file = record.getFilePath();
        try {
            File imgFile = new File(file);
            if (!imgFile.exists())
                return;

            Image img = new Image(imgFile.toURI().toString());
            ImageView imageView = new ImageView(img);
            imageView.setPreserveRatio(true); // Maintain original image ratio

            // Bind image scaling natively to scroll pane container width to prevent layout
            // overflow loops.
            // Provide a strict initial fallback size. This prevents the geometry engine
            // from
            // defaulting to the raw image size (1024px) for a split-second before runLater
            // fires.
            imageView.setFitWidth(250);
            imageView.setFitHeight(187.5);

            StackPane imageWrapper = new StackPane(imageView);
            imageWrapper.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);

            StackPane cell = new StackPane(imageWrapper);
            cell.getStyleClass().add("image-cell");
            cell.setFocusTraversable(true);

            // Filename hover label for all cells
            Label fileLabel = new Label(record.getFilename());
            fileLabel.getStyleClass().add("grid-filename");
            StackPane.setAlignment(fileLabel, javafx.geometry.Pos.BOTTOM_CENTER);
            StackPane.setMargin(fileLabel, new javafx.geometry.Insets(0, 0, 15, 0));
            fileLabel.setMouseTransparent(true);
            fileLabel.visibleProperty().bind(cell.hoverProperty());
            cell.getChildren().add(fileLabel);

            // Initial visual state
            if (index == 1) {
                cell.getStyleClass().add("selected");
                currentActiveCell = cell;
                currentActiveFile = file;
            }

            // Sync visual annotation heart presence on boot
            String existingNotes = annotations.get(file);
            if (existingNotes != null && !existingNotes.trim().isEmpty()) {
                Label heartBadge = createHeartBadge();
                StackPane.setAlignment(heartBadge, javafx.geometry.Pos.TOP_LEFT);
                StackPane.setMargin(heartBadge, new javafx.geometry.Insets(0, 0, 0, 10)); // Adjusted inset
                imageWrapper.getChildren().add(heartBadge);
            }

            cell.setOnMouseClicked(event -> {
                setSelectedPreview(file);
                // Remove 'selected' class from all cells
                for (StackPane c : allCells) {
                    c.getStyleClass().remove("selected");
                }
                // Apply 'selected' class to the clicked cell
                cell.getStyleClass().add("selected");
                cell.requestFocus();

                // Track state and reload annotation map
                currentActiveCell = cell;
                currentActiveFile = file;
                if (annotationArea != null) {
                    annotationArea.setText(annotations.getOrDefault(file, ""));
                }

                if (event.getClickCount() == 2) {
                    fetchAndOpenImageWindow(file);
                }
            });

            // Push into memory rather than directly injecting
            allCells.add(cell);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Label createHeartBadge() {
        Label l = new Label("♥");
        l.getStyleClass().add("heart-badge");
        return l;
    }

    private void setSelectedPreview(String file) {
        try {
            File imgFile = new File(file);
            if (imgFile.exists()) {
                selectedImagePreview.setImage(new Image(imgFile.toURI().toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchAndOpenImageWindow(String filePath) {
        try {
            File imgFile = new File(filePath);
            if (!imgFile.exists())
                return;

            Image fullImg = new Image(imgFile.toURI().toString());
            ImageView fullView = new ImageView(fullImg);
            fullView.setPreserveRatio(true);
            fullView.setSmooth(true);

            StackPane root = new StackPane(fullView);
            root.setStyle("-fx-background-color: #0D0D0D;");

            Scene scene = new Scene(root, 1024, 768);

            fullView.fitWidthProperty().bind(scene.widthProperty());
            fullView.fitHeightProperty().bind(scene.heightProperty());

            Stage stage = new Stage();
            stage.setTitle("Darkroom Atelier Viewer - " + imgFile.getName());
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
