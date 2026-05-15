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
    @FXML private javafx.scene.control.Button openEditorBtn;
    @FXML private javafx.scene.control.Button deleteImageBtn;

    @FXML private javafx.scene.control.MenuButton filterBtn;
    @FXML private javafx.scene.control.CheckBox filterHasAnnotation;
    @FXML private javafx.scene.control.CheckBox filterTypePNG;
    @FXML private javafx.scene.control.CheckBox filterTypeJPEG;
    @FXML private javafx.scene.control.CheckBox filterTypeRAW;

    @FXML private javafx.scene.control.SplitMenuButton sortBtn;
    @FXML private javafx.scene.control.MenuItem menuSortCreated;
    @FXML private javafx.scene.control.MenuItem menuSortModified;

    @FXML private javafx.scene.control.TextField metaName;
    @FXML private Label metaFormat;
    @FXML private Label metaSize;
    @FXML private Label metaDimensions;
    @FXML private Label metaDateCreated;
    @FXML private Label metaDateModified;
    @FXML private Label metaIso;
    @FXML private Label metaAperture;
    @FXML private Label metaShutter;
    @FXML private Label metaFocalLength;
    @FXML private Label metaLens;

    // =========================================================================
    // INTERNAL STATE VARIABLES
    // =========================================================================
    
    private java.util.List<StackPane> cachedImageCells = new java.util.ArrayList<>();
    private int currentCols = -1;
    private boolean sortAscending = true;
    private String sortCriteria = "CREATED";
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
        updateSortButtonText();
        renderGrid();
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
                    renderGrid();
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

        if (deleteImageBtn != null) {
            deleteImageBtn.setOnAction(e -> handleDeleteImage());
        }

        if (openFullscreenBtn != null) {
            openFullscreenBtn.setOnAction(e -> {
                if (currentActiveFile != null) {
                    fetchAndOpenImageWindow(currentActiveFile);
                }
            });
        }

        if (openEditorBtn != null) {
            openEditorBtn.setOnAction(e -> {
                if (currentActiveFile != null) {
                    try {
                        Stage editorStage = new Stage();
                        com.Thierry.MainApp editorApp = new com.Thierry.MainApp();
                        editorApp.start(editorStage);
                        editorApp.loadExternalImage(currentActiveFile);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        if (metaName != null) {
            metaName.setOnAction(e -> handleRenameImage());
            metaName.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    handleRenameImage();
                }
            });
        }

        if (sortBtn != null) {
            sortBtn.setOnAction(e -> {
                sortAscending = !sortAscending;
                updateSortButtonText();
                renderGrid();
            });
            if (menuSortCreated != null) {
                menuSortCreated.setOnAction(e -> {
                    sortCriteria = "CREATED";
                    updateSortButtonText();
                    renderGrid();
                });
            }
            if (menuSortModified != null) {
                menuSortModified.setOnAction(e -> {
                    sortCriteria = "MODIFIED";
                    updateSortButtonText();
                    renderGrid();
                });
            }
        }
        
        if (filterHasAnnotation != null) filterHasAnnotation.setOnAction(e -> renderGrid());
        if (filterTypePNG != null) filterTypePNG.setOnAction(e -> renderGrid());
        if (filterTypeJPEG != null) filterTypeJPEG.setOnAction(e -> renderGrid());
        if (filterTypeRAW != null) filterTypeRAW.setOnAction(e -> renderGrid());
    }

    // =========================================================================
    // UI UPDATERS & RENDERERS
    // =========================================================================
    
    private void updateSortButtonText() {
        if (sortBtn == null) return;
        String dir = sortAscending ? "↑" : "↓";
        String criteriaStr = sortCriteria.equals("CREATED") ? "Date Created" : "Date Modified";
        sortBtn.setText(criteriaStr + " " + dir);
    }

    private void renderGrid() {
        if (cachedImageCells.isEmpty()) return;
        if (cachedImageCells.size() == 1) {
            if (currentCols > 0) rebuildGridPane(currentCols, cachedImageCells);
            return;
        }
        
        StackPane uploadCell = cachedImageCells.get(0);
        java.util.List<StackPane> cellsToProcess = new java.util.ArrayList<>(cachedImageCells.subList(1, cachedImageCells.size()));
        
        // 1. Filter
        boolean checkAnn = filterHasAnnotation != null && filterHasAnnotation.isSelected();
        boolean checkPng = filterTypePNG != null && filterTypePNG.isSelected();
        boolean checkJpeg = filterTypeJPEG != null && filterTypeJPEG.isSelected();
        boolean checkRaw = filterTypeRAW != null && filterTypeRAW.isSelected();
        boolean anyTypeFilter = checkPng || checkJpeg || checkRaw;
        
        java.util.List<StackPane> filteredCells = new java.util.ArrayList<>();
        for (StackPane cell : cellsToProcess) {
            String file = (String) cell.getUserData();
            if (file == null) continue;
            
            // Annotation Filter
            if (checkAnn) {
                boolean hasAnn = annotations != null && annotations.containsKey(file) && !annotations.get(file).trim().isEmpty();
                if (!hasAnn) continue;
            }
            
            // Type Filter
            if (anyTypeFilter) {
                String lower = file.toLowerCase();
                boolean isPng = lower.endsWith(".png");
                boolean isJpeg = lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                boolean isRaw = lower.endsWith(".nef") || lower.endsWith(".cr2") || lower.endsWith(".arw");
                if (!( (checkPng && isPng) || (checkJpeg && isJpeg) || (checkRaw && isRaw) )) {
                    continue;
                }
            }
            
            filteredCells.add(cell);
        }
        
        // 2. Sort
        filteredCells.sort((cellA, cellB) -> {
            String fileA = (String) cellA.getUserData();
            String fileB = (String) cellB.getUserData();
            if (fileA == null || fileB == null) return 0;
            try {
                java.nio.file.attribute.BasicFileAttributes attrA = Files.readAttributes(new File(fileA).toPath(), java.nio.file.attribute.BasicFileAttributes.class);
                java.nio.file.attribute.BasicFileAttributes attrB = Files.readAttributes(new File(fileB).toPath(), java.nio.file.attribute.BasicFileAttributes.class);
                
                long timeA = sortCriteria.equals("CREATED") ? attrA.creationTime().toMillis() : attrA.lastModifiedTime().toMillis();
                long timeB = sortCriteria.equals("CREATED") ? attrB.creationTime().toMillis() : attrB.lastModifiedTime().toMillis();
                
                return sortAscending ? Long.compare(timeA, timeB) : Long.compare(timeB, timeA);
            } catch (Exception e) {
                return 0;
            }
        });
        
        // 3. Layout
        java.util.List<StackPane> finalCells = new java.util.ArrayList<>();
        finalCells.add(uploadCell);
        finalCells.addAll(filteredCells);
        
        if (currentCols > 0) {
            rebuildGridPane(currentCols, finalCells);
        }
    }

    private void rebuildGridPane(int cols, java.util.List<StackPane> cellsToRender) {
        imageGrid.getChildren().clear();
        imageGrid.getColumnConstraints().clear();

        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            imageGrid.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < cellsToRender.size(); i++) {
            StackPane cell = cellsToRender.get(i);
            
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
                updateMetadataPanel(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMetadataPanel(String file) {
        try {
            File imgFile = new File(file);
            if (!imgFile.exists()) return;

            if (metaName != null) metaName.setText(imgFile.getName());
            
            if (metaFormat != null) {
                String nameLower = imgFile.getName().toLowerCase();
                String format = "Unknown";
                if (nameLower.endsWith(".png")) format = "PNG";
                else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) format = "JPEG";
                else if (nameLower.endsWith(".nef")) format = "RAW (NEF)";
                metaFormat.setText(format);
            }

            if (metaSize != null) {
                long bytes = imgFile.length();
                double mb = bytes / (1024.0 * 1024.0);
                metaSize.setText(String.format("%.1f MB", mb));
            }

            if (metaDimensions != null) metaDimensions.setText("-");
            if (metaDateCreated != null) metaDateCreated.setText("-");
            if (metaDateModified != null) metaDateModified.setText("-");
            if (metaIso != null) metaIso.setText("-");
            if (metaAperture != null) metaAperture.setText("-");
            if (metaShutter != null) metaShutter.setText("-");
            if (metaFocalLength != null) metaFocalLength.setText("-");
            if (metaLens != null) metaLens.setText("-");

            try {
                java.nio.file.attribute.BasicFileAttributes attr = Files.readAttributes(imgFile.toPath(), java.nio.file.attribute.BasicFileAttributes.class);
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                if (metaDateCreated != null) {
                    metaDateCreated.setText(dtf.format(java.time.LocalDateTime.ofInstant(attr.creationTime().toInstant(), java.time.ZoneId.systemDefault())));
                }
                if (metaDateModified != null) {
                    metaDateModified.setText(dtf.format(java.time.LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault())));
                }
            } catch (Exception e) {
                System.out.println("Could not read file dates: " + e.getMessage());
            }

            try {
                com.drew.metadata.Metadata metadata = com.drew.imaging.ImageMetadataReader.readMetadata(imgFile);
                
                com.drew.metadata.exif.ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifSubIFDDirectory.class);
                if (directory != null) {
                    if (metaIso != null && directory.containsTag(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                        metaIso.setText(directory.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                    }
                    if (metaAperture != null && directory.containsTag(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FNUMBER)) {
                        metaAperture.setText("f/" + directory.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FNUMBER));
                    }
                    if (metaShutter != null && directory.containsTag(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                        String shutter = directory.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
                        if (shutter != null) {
                            if (shutter.contains(".")) {
                                double s = Double.parseDouble(shutter);
                                if (s < 1) shutter = "1/" + Math.round(1/s) + "s";
                                else shutter = s + "s";
                            } else {
                                shutter += "s";
                            }
                        }
                        metaShutter.setText(shutter);
                    }
                    if (metaFocalLength != null && directory.containsTag(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                        metaFocalLength.setText(directory.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FOCAL_LENGTH) + "mm");
                    }
                    if (metaLens != null && directory.containsTag(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_LENS_MODEL)) {
                        metaLens.setText(directory.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_LENS_MODEL));
                    }
                }
                
                int width = 0;
                int height = 0;
                
                com.drew.metadata.exif.ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifIFD0Directory.class);
                if (ifd0Dir != null && ifd0Dir.containsTag(com.drew.metadata.exif.ExifIFD0Directory.TAG_IMAGE_WIDTH)) {
                    width = ifd0Dir.getInt(com.drew.metadata.exif.ExifIFD0Directory.TAG_IMAGE_WIDTH);
                    height = ifd0Dir.getInt(com.drew.metadata.exif.ExifIFD0Directory.TAG_IMAGE_HEIGHT);
                } else {
                    com.drew.metadata.jpeg.JpegDirectory jpegDir = metadata.getFirstDirectoryOfType(com.drew.metadata.jpeg.JpegDirectory.class);
                    if (jpegDir != null) {
                        width = jpegDir.getImageWidth();
                        height = jpegDir.getImageHeight();
                    } else {
                        com.drew.metadata.png.PngDirectory pngDir = metadata.getFirstDirectoryOfType(com.drew.metadata.png.PngDirectory.class);
                        if (pngDir != null) {
                            if (pngDir.containsTag(com.drew.metadata.png.PngDirectory.TAG_IMAGE_WIDTH)) {
                                width = pngDir.getInt(com.drew.metadata.png.PngDirectory.TAG_IMAGE_WIDTH);
                                height = pngDir.getInt(com.drew.metadata.png.PngDirectory.TAG_IMAGE_HEIGHT);
                            }
                        }
                    }
                }
                if (width > 0 && height > 0 && metaDimensions != null) {
                    metaDimensions.setText(width + " × " + height);
                }

            } catch (Exception ex) {
                System.out.println("Could not read metadata for " + file + ": " + ex.getMessage());
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

        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasFiles()) {
                boolean hasImage = false;
                for (File file : event.getDragboard().getFiles()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".nef")) {
                        hasImage = true;
                        break;
                    }
                }
                if (hasImage) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.COPY_OR_MOVE);
                }
            }
            event.consume();
        });

        cell.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                for (File file : dragboard.getFiles()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".nef")) {
                        importImageToLibrary(file);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
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

            cell.setUserData(file);
            cell.setOnMouseClicked(event -> {
                String currentFile = (String) cell.getUserData();
                setSelectedPreview(currentFile);
                
                for (StackPane c : cachedImageCells) {
                    c.getStyleClass().remove("selected");
                }
                
                cell.getStyleClass().add("selected");
                cell.requestFocus();

                currentActiveCell = cell;
                currentActiveFile = currentFile;
                if (annotationArea != null) {
                    annotationArea.setText(annotations.getOrDefault(currentFile, ""));
                }

                if (event.getClickCount() == 2) {
                    fetchAndOpenImageWindow(currentFile);
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

            int count = cachedImageCells.size() - 1; // account for upload cell
            imageCountLabel.setText("Showing " + count + " items from recent import");

            renderGrid();
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

    private void handleRenameImage() {
        if (currentActiveFile == null || currentActiveCell == null) return;
        String newName = metaName.getText().trim();
        File oldFile = new File(currentActiveFile);
        
        if (newName.isEmpty() || newName.equals(oldFile.getName())) {
            metaName.setText(oldFile.getName());
            return;
        }
        
        // ensure extension is maintained
        String oldName = oldFile.getName();
        int dotIdx = oldName.lastIndexOf('.');
        String ext = dotIdx != -1 ? oldName.substring(dotIdx) : "";
        
        if (!newName.toLowerCase().endsWith(ext.toLowerCase())) {
            newName += ext;
        }

        File newFile = new File(oldFile.getParent(), newName);
        if (newFile.exists()) {
            System.out.println("File already exists");
            metaName.setText(oldName);
            return;
        }

        if (oldFile.renameTo(newFile)) {
            db.updateImageFilePath(currentActiveFile, newFile.getAbsolutePath(), newName);
            
            // update annotations map
            if (annotations.containsKey(currentActiveFile)) {
                annotations.put(newFile.getAbsolutePath(), annotations.remove(currentActiveFile));
            }
            
            // update UI label
            if (!currentActiveCell.getChildren().isEmpty()) {
                javafx.scene.Node baseNode = currentActiveCell.getChildren().get(currentActiveCell.getChildren().size() - 1);
                if (baseNode instanceof Label && baseNode.getStyleClass().contains("grid-filename")) {
                    ((Label) baseNode).setText(newName);
                } else {
                    for (javafx.scene.Node n : currentActiveCell.getChildren()) {
                        if (n instanceof Label && n.getStyleClass().contains("grid-filename")) {
                            ((Label) n).setText(newName);
                            break;
                        }
                    }
                }
            }
            
            currentActiveFile = newFile.getAbsolutePath();
            currentActiveCell.setUserData(currentActiveFile);
            metaName.setText(newName);
            
        } else {
            System.out.println("Failed to rename file");
            metaName.setText(oldName);
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

    private void handleDeleteImage() {
        if (currentActiveFile == null || currentActiveCell == null) return;
        
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Image");
        alert.setHeaderText("Remove Image Completely");
        alert.setContentText("Are you sure you want to permanently delete this image from your library and disk?");
        
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            File file = new File(currentActiveFile);
            if (file.exists()) file.delete();
            
            db.deleteImageRecord(currentActiveFile);
            db.deleteAnnotation(currentActiveFile);
            annotations.remove(currentActiveFile);
            
            cachedImageCells.remove(currentActiveCell);
            
            int count = cachedImageCells.size() - 1;
            imageCountLabel.setText("Showing " + count + " items from recent import");
            
            if (cachedImageCells.size() > 1) {
                StackPane nextCell = cachedImageCells.get(1); // Index 0 is the upload prompt
                String nextFile = (String) nextCell.getUserData();
                setSelectedPreview(nextFile);
                
                for (StackPane c : cachedImageCells) {
                    c.getStyleClass().remove("selected");
                }
                nextCell.getStyleClass().add("selected");
                
                currentActiveCell = nextCell;
                currentActiveFile = nextFile;
                if (annotationArea != null) {
                    annotationArea.setText(annotations.getOrDefault(currentActiveFile, ""));
                }
            } else {
                currentActiveCell = null;
                currentActiveFile = null;
                selectedImagePreview.setImage(null);
                if (annotationArea != null) annotationArea.setText("");
                if (metaName != null) metaName.setText("-");
                if (metaFormat != null) metaFormat.setText("-");
                if (metaSize != null) metaSize.setText("-");
                if (metaDimensions != null) metaDimensions.setText("-");
                if (metaDateCreated != null) metaDateCreated.setText("-");
                if (metaDateModified != null) metaDateModified.setText("-");
                if (metaIso != null) metaIso.setText("-");
                if (metaAperture != null) metaAperture.setText("-");
                if (metaShutter != null) metaShutter.setText("-");
                if (metaFocalLength != null) metaFocalLength.setText("-");
                if (metaLens != null) metaLens.setText("-");
            }
            
            renderGrid();
        }
    }
}
