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

/**
 * Controller for the Main Dashboard Layout.
 * Handles the dynamic image grid sizing, database metadata synchronization,
 * and user interactions seamlessly inside the primary UI instance.
 */
public class DashboardController implements Initializable {

    // =========================================================================
    // STATIC LAYOUT CONSTANTS
    // =========================================================================
    
    private static final int SIDEBAR_WIDTH_MAX = 450;
    
    private static final double GRID_CELL_MIN_WIDTH = 200.0;
    private static final int GRID_CELL_HEIGHT = 190;
    private static final int GRID_IMAGE_HEIGHT = 180;
    
    private static final int DEFAULT_COLUMNS = 4;
    private static final int THUMBNAIL_RESAMPLE_SIZE = 300;
    private static final int PREVIEW_RESAMPLE_SIZE = 500;
    private static final double FALLBACK_THUMBNAIL_WIDTH = 250.0;
    private static final double FALLBACK_THUMBNAIL_HEIGHT = 187.5;
    
    private static final int FULL_VIEWER_WIDTH = 1024;
    private static final int FULL_VIEWER_HEIGHT = 768;

    // =========================================================================
    // FXML UI INJECTIONS
    // =========================================================================
    
    @FXML private GridPane imageGrid;
    @FXML private ImageView selectedImagePreview;
    @FXML private Label imageCountLabel;
    @FXML private javafx.scene.control.ScrollPane gridScrollPane;
    @FXML private javafx.scene.layout.BorderPane mainSplitContainer;
    @FXML private javafx.scene.control.ScrollPane metadataSidebar;
    @FXML private javafx.scene.layout.HBox topRightActions;
    @FXML private javafx.scene.layout.HBox topBar;
    @FXML private javafx.scene.control.TextArea annotationArea;
    @FXML private javafx.scene.control.Button syncMetadataBtn;
    @FXML private javafx.scene.control.Button removeMetadataBtn;
    @FXML private javafx.scene.control.Button openFullscreenBtn;

    // =========================================================================
    // INTERNAL STATE VARIABLES
    // =========================================================================
    
    private java.util.List<StackPane> cachedImageCells = new java.util.ArrayList<>();
    private int currentCols = -1;
    private java.util.Map<String, String> annotations;
    private StackPane currentActiveCell = null;
    private String currentActiveFile = null;
    private DatabaseManager db = DatabaseManager.getInstance();

    // =========================================================================
    // INITIALIZATION LIFECYCLE
    // =========================================================================
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        
        // 1. Load active annotations from SQLite database
        annotations = db.loadAllAnnotations();

        // 2. Initialize the dynamic grid components
        createUploadPromptCell();
        java.util.List<ImageRecord> library = db.loadLibrary();
        for (int i = 0; i < library.size(); i++) {
            createAndAddImageCell(library.get(i), i + 1);
        }

        imageCountLabel.setText("Showing " + library.size() + " items from recent import");

        // Select the first image natively to populate the sidebar text/preview
        if (!library.isEmpty()) {
            setSelectedPreview(library.get(0).getFilePath());
        }

        // 3. Register geometric layout rules and action hooks
        bindSidebarGeometry();
        bindGridResponsiveEngine();
        registerButtonActions();

        // Boot the initial grid bounds safely
        currentCols = DEFAULT_COLUMNS;
        rebuildGridPane(currentCols);
    }
    
    /**
     * Binds the sidebar properties so it conditionally compresses to maintain maximum
     * usable width for the primary photo database grid across smaller window sizes.
     */
    private void bindSidebarGeometry() {
        javafx.beans.binding.DoubleBinding dynamicRightPanelWidth = new javafx.beans.binding.DoubleBinding() {
            {
                super.bind(mainSplitContainer.widthProperty());
            }

            @Override
            protected double computeValue() {
                double totalWidth = mainSplitContainer.getWidth();
                return Math.max(0, Math.min(SIDEBAR_WIDTH_MAX, totalWidth - SIDEBAR_WIDTH_MAX));
            }
        };

        metadataSidebar.prefWidthProperty().bind(dynamicRightPanelWidth);
        metadataSidebar.minWidthProperty().bind(dynamicRightPanelWidth);
        metadataSidebar.maxWidthProperty().bind(dynamicRightPanelWidth);
        topRightActions.prefWidthProperty().bind(dynamicRightPanelWidth);
        topRightActions.minWidthProperty().bind(dynamicRightPanelWidth);
        topRightActions.maxWidthProperty().bind(dynamicRightPanelWidth);
    }
    
    /**
     * Attaches an aggressive listener to the grid scrollpane. It calculates the necessary
     * column quantities dynamically by calculating internal window width against absolute margin padding,
     * ensuring columns shift natively exactly like modern web FlexBox grid designs.
     */
    private void bindGridResponsiveEngine() {
        javafx.application.Platform.runLater(() -> {
            gridScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                double width = newBounds.getWidth();
                if (width <= 20) return;

                double hgap = imageGrid.getHgap();
                int calcCols = (int) ((width - 40 + hgap) / (GRID_CELL_MIN_WIDTH + hgap));
                int cols = Math.max(1, Math.min(DEFAULT_COLUMNS, calcCols)); // Constrain grid columns

                if (cols != currentCols) {
                    currentCols = cols;
                    rebuildGridPane(cols);
                }
            });
        });
    }

    private void registerButtonActions() {
        if (syncMetadataBtn != null) {
            syncMetadataBtn.setOnAction(e -> handleSaveAnnotation());
        }
        
        if (removeMetadataBtn != null) {
            removeMetadataBtn.setOnAction(e -> handleClearAnnotation());
        }

        if (openFullscreenBtn != null) {
            openFullscreenBtn.setOnAction(e -> {
                if (currentActiveFile != null) {
                    fetchAndOpenImageWindow(currentActiveFile);
                }
            });
        }
    }

    // =========================================================================
    // UI UPDATERS & RENDERERS
    // =========================================================================
    
    private void rebuildGridPane(int cols) {
        imageGrid.getChildren().clear();
        imageGrid.getColumnConstraints().clear();

        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            imageGrid.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < cachedImageCells.size(); i++) {
            StackPane cell = cachedImageCells.get(i);
            
            cell.setPrefHeight(GRID_CELL_HEIGHT);
            cell.setMinHeight(GRID_CELL_HEIGHT);
            cell.setMaxHeight(GRID_CELL_HEIGHT);

            javafx.scene.Node baseNode = cell.getChildren().get(0);
            ImageView targetView = null;

            if (baseNode instanceof StackPane) {
                StackPane wrapper = (StackPane) baseNode;
                if (!wrapper.getChildren().isEmpty() && wrapper.getChildren().get(0) instanceof ImageView) {
                    targetView = (ImageView) wrapper.getChildren().get(0);
                }
            } else if (baseNode instanceof ImageView) {
                targetView = (ImageView) baseNode;
            }
            
            if (targetView != null) {
                targetView.fitWidthProperty().unbind();
                targetView.fitHeightProperty().unbind();

                targetView.fitWidthProperty().bind(gridScrollPane.widthProperty()
                        .subtract(imageGrid.getHgap() * (cols - 1) + 60)
                        .divide(cols));

                targetView.setFitHeight(GRID_IMAGE_HEIGHT);
            }

            imageGrid.add(cell, i % cols, i / cols);
        }
    }

    private void setSelectedPreview(String file) {
        try {
            File imgFile = new File(file);
            if (imgFile.exists()) {
                selectedImagePreview.setImage(new Image(imgFile.toURI().toString(), PREVIEW_RESAMPLE_SIZE, PREVIEW_RESAMPLE_SIZE, true, true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // COMPONENT GENERATORS
    // =========================================================================

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

        cachedImageCells.add(cell);
    }

    private void createAndAddImageCell(ImageRecord record, int index) {
        String file = record.getFilePath();
        try {
            File imgFile = new File(file);
            if (!imgFile.exists()) return;

            // Downsample memory load footprint & smooth pixel aliasing natively
            Image img = new Image(imgFile.toURI().toString(), THUMBNAIL_RESAMPLE_SIZE, THUMBNAIL_RESAMPLE_SIZE, true, true);
            ImageView imageView = new ImageView(img);
            imageView.setPreserveRatio(true);

            imageView.setFitWidth(FALLBACK_THUMBNAIL_WIDTH);
            imageView.setFitHeight(FALLBACK_THUMBNAIL_HEIGHT);

            StackPane imageWrapper = new StackPane(imageView);
            imageWrapper.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);

            StackPane cell = new StackPane(imageWrapper);
            cell.getStyleClass().add("image-cell");
            cell.setFocusTraversable(true);

            // Hover tooltip mapping
            Label fileLabel = new Label(record.getFilename());
            fileLabel.getStyleClass().add("grid-filename");
            StackPane.setAlignment(fileLabel, javafx.geometry.Pos.BOTTOM_CENTER);
            StackPane.setMargin(fileLabel, new javafx.geometry.Insets(0, 0, 15, 0));
            fileLabel.setMouseTransparent(true);
            fileLabel.visibleProperty().bind(cell.hoverProperty());
            cell.getChildren().add(fileLabel);

            // Check selection state bindings
            if (index == 1) {
                cell.getStyleClass().add("selected");
                currentActiveCell = cell;
                currentActiveFile = file;
            }

            // Bind current annotation database memory
            String existingNotes = annotations.get(file);
            if (existingNotes != null && !existingNotes.trim().isEmpty()) {
                Label heartBadge = createHeartBadge();
                StackPane.setAlignment(heartBadge, javafx.geometry.Pos.TOP_LEFT);
                StackPane.setMargin(heartBadge, new javafx.geometry.Insets(0, 0, 0, 10));
                imageWrapper.getChildren().add(heartBadge);
            }

            cell.setOnMouseClicked(event -> {
                setSelectedPreview(file);
                
                for (StackPane c : cachedImageCells) {
                    c.getStyleClass().remove("selected");
                }
                
                cell.getStyleClass().add("selected");
                cell.requestFocus();

                currentActiveCell = cell;
                currentActiveFile = file;
                if (annotationArea != null) {
                    annotationArea.setText(annotations.getOrDefault(file, ""));
                }

                if (event.getClickCount() == 2) {
                    fetchAndOpenImageWindow(file);
                }
            });

            cachedImageCells.add(cell);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void fetchAndOpenImageWindow(String filePath) {
        try {
            File imgFile = new File(filePath);
            if (!imgFile.exists()) return;

            Image fullImg = new Image(imgFile.toURI().toString());
            ImageView fullView = new ImageView(fullImg);
            fullView.setPreserveRatio(true);
            fullView.setSmooth(true);

            StackPane root = new StackPane(fullView);
            root.setStyle("-fx-background-color: #0D0D0D;");

            Scene scene = new Scene(root, FULL_VIEWER_WIDTH, FULL_VIEWER_HEIGHT);

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

    private Label createHeartBadge() {
        Label l = new Label("♥");
        l.getStyleClass().add("heart-badge");
        return l;
    }

    // =========================================================================
    // DATA & STATE OPERATIONS
    // =========================================================================

    private void importImageToLibrary(File sourceFile) {
        try {
            File destFolder = new File(DatabaseManager.IMAGES_FOLDER);
            File destFile = new File(destFolder, sourceFile.getName());

            if (destFile.exists()) {
                destFile = new File(destFolder, System.currentTimeMillis() + "_" + sourceFile.getName());
            }

            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            ImageRecord record = new ImageRecord(0, destFile.getAbsolutePath(), destFile.getName(), System.currentTimeMillis());
            db.insertImageRecord(record);

            createAndAddImageCell(record, cachedImageCells.size());

            int count = cachedImageCells.size() - 1;
            imageCountLabel.setText("Showing " + count + " items from recent import");

            rebuildGridPane(currentCols);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleSaveAnnotation() {
        if (currentActiveCell != null && currentActiveFile != null) {
            String text = annotationArea.getText();
            boolean isEmpty = text == null || text.trim().isEmpty();

            if (!isEmpty) {
                annotations.put(currentActiveFile, text);
                db.saveAnnotation(currentActiveFile, text);
                if (!hasHeartBadge(currentActiveCell)) {
                    Label heartBadge = createHeartBadge();
                    StackPane.setAlignment(heartBadge, javafx.geometry.Pos.TOP_LEFT);
                    StackPane.setMargin(heartBadge, new javafx.geometry.Insets(0, 0, 0, 10));

                    if (!currentActiveCell.getChildren().isEmpty() && currentActiveCell.getChildren().get(0) instanceof StackPane) {
                        ((StackPane) currentActiveCell.getChildren().get(0)).getChildren().add(heartBadge);
                    } else {
                        currentActiveCell.getChildren().add(heartBadge);
                    }
                }
            } else {
                handleClearAnnotation();
            }
        }
    }

    private void handleClearAnnotation() {
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
    }

    private boolean hasHeartBadge(StackPane cell) {
        if (cell == null || cell.getChildren().isEmpty()) return false;
        
        if (cell.getChildren().get(0) instanceof StackPane) {
            StackPane wrapper = (StackPane) cell.getChildren().get(0);
            for (javafx.scene.Node n : wrapper.getChildren()) {
                if (n instanceof Label && n.getStyleClass().contains("heart-badge")) return true;
            }
        } else {
            for (javafx.scene.Node n : cell.getChildren()) {
                if (n instanceof Label && n.getStyleClass().contains("heart-badge")) return true;
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
}
