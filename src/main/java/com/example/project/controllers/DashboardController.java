package com.example.project.controllers;

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

import java.net.URL;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load dummy images
        String[] imageFiles = { "photo_1.png", "photo_2.png", "photo_3.png", "photo_4.png" };

        // 1. First column: The Upload Zone prompt
        createUploadPromptCell();

        // 2. Populate remaining 15 spaces
        for (int i = 0; i < 15; i++) {
            String file = imageFiles[i % 4];
            createAndAddImageCell(file, i + 1); // pass adjusted index for badge alignments
        }

        imageCountLabel.setText("Showing 16 items from recent import");

        // Select first by default
        setSelectedPreview(imageFiles[0]);

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
            javafx.scene.Node baseNode = cell.getChildren().get(0);

            if (baseNode instanceof ImageView) {
                ImageView iv = (ImageView) baseNode;
                // Sever old bindings to prevent cyclical geometry crashing
                iv.fitWidthProperty().unbind();
                iv.fitHeightProperty().unbind();

                // Tie dimensions safely to outermost viewport rather than unstable grid
                // internals
                iv.fitWidthProperty().bind(gridScrollPane.widthProperty()
                        .subtract(imageGrid.getHgap() * (cols - 1) + 40)
                        .divide(cols));

                iv.fitHeightProperty().bind(iv.fitWidthProperty().multiply(0.75));
            }

            imageGrid.add(cell, i % cols, i / cols);
        }
    }

    private void createUploadPromptCell() {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("upload-cell");

        VBox content = new VBox(10);
        content.setAlignment(javafx.geometry.Pos.CENTER);

        Label icon = new Label("+");
        icon.getStyleClass().add("upload-icon");

        Label text = new Label("Drag & Drop Image\nor Click to Browse");
        text.getStyleClass().add("upload-text");

        content.getChildren().addAll(icon, text);
        cell.getChildren().add(content);

        cell.setOnMouseClicked(e -> {
            System.out.println("Trigger OS FileChooser intent here");
        });

        allCells.add(cell);
    }

    private void createAndAddImageCell(String file, int index) {
        try {
            URL resource = getClass().getResource("/com/example/project/images/" + file);
            if (resource == null)
                return;

            Image img = new Image(resource.toExternalForm());
            ImageView imageView = new ImageView(img);
            imageView.setPreserveRatio(false); // Make them obey forced bounds

            // Bind image scaling natively to scroll pane container width to prevent layout
            // overflow loops.
            // Provide a strict initial fallback size. This prevents the geometry engine
            // from
            // defaulting to the raw image size (1024px) for a split-second before runLater
            // fires.
            imageView.setFitWidth(250);
            imageView.setFitHeight(187.5);

            // Perfectly round the physical image view so it doesn't bleed out of the cell
            // container
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(imageView.fitWidthProperty());
            clip.heightProperty().bind(imageView.fitHeightProperty());
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            imageView.setClip(clip);

            StackPane cell = new StackPane(imageView);
            cell.getStyleClass().add("image-cell");

            // Randomly assign selection or badges for visual variety
            if (index == 1) { // Let's mark the second one as selected like in the mockup
                cell.getStyleClass().add("selected");

                // Add heart badge
                Label heartBadge = createHeartBadge();
                StackPane.setAlignment(heartBadge, javafx.geometry.Pos.TOP_LEFT);
                StackPane.setMargin(heartBadge, new javafx.geometry.Insets(0, 0, 0, 10));

                // Filename label
                Label fileLabel = new Label("DSC_0492.NEF");
                fileLabel.getStyleClass().add("grid-filename");
                StackPane.setAlignment(fileLabel, javafx.geometry.Pos.BOTTOM_CENTER);
                StackPane.setMargin(fileLabel, new javafx.geometry.Insets(0, 0, 15, 0));

                cell.getChildren().addAll(heartBadge, fileLabel);
            } else if (index == 2) {
                // Add heart badge
                Label heartBadge = createHeartBadge();
                StackPane.setAlignment(heartBadge, javafx.geometry.Pos.TOP_LEFT);
                StackPane.setMargin(heartBadge, new javafx.geometry.Insets(0, 0, 0, 10));
                cell.getChildren().add(heartBadge);
            }

            cell.setOnMouseClicked(event -> {
                setSelectedPreview(file);
                // In real app, toggle "selected" style class across grid cells
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
            URL resource = getClass().getResource("/com/example/project/images/" + file);
            if (resource != null) {
                selectedImagePreview.setImage(new Image(resource.toExternalForm()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
