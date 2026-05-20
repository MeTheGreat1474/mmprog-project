package com.example;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

public class MainApp extends Application 
{
    private Image originalImage; // The untouched file
    private Image currentImage;  // The image after transformations are applied
    private ImageView imageView = new ImageView();
    private Label heartIcon = new Label("♥");
    private Label statusLabel = new Label("System Status: Ready");
    private ProgressBar progressBar = new ProgressBar(0);

    private Slider brightSlider;
    private Slider rotateSlider;
    private Slider contrastSlider;
    private TextField textAngle;

    private boolean isGrayscalePreviewActive = false;
    private boolean isBorderPreviewActive = false;

    // Style Constants matching your screenshot
    private final String BG_DARK = "#0D0D0D";
    private final String PANEL_DARK = "#1A1A1A";
    private final String ACCENT_BLUE = "#00A3FF";
    private final String TEXT_LIGHT = "#FFFFFF";
    private final String TEXT_MUTED = "#888888";
    private final String BORDER_COLOR = "#222222";

    @Override
    public void start(Stage primaryStage) 
    {
        primaryStage.setTitle("Image Editor");

        // Left Sidebar 
        VBox sideMenu = new VBox(15);
        sideMenu.setPadding(new Insets(25, 15, 25, 15));
        sideMenu.setPrefWidth(260);
        sideMenu.setStyle("-fx-background-color: " + BG_DARK + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 1 0 0;");

        // App/Section Title 
        Label menuTitle = new Label("Image Processing");
        menuTitle.setStyle("-fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-font-size: 18px;");

        // Load Button
        Button loadButton = new Button("Open Image");
        loadButton.setMaxWidth(Double.MAX_VALUE);
        loadButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        loadButton.setOnAction(e -> loadFile(primaryStage));

        // 2. Geometric Section (Collapsible)
        Button geoToolsButton = new Button("Geometric Tools ▼");
        geoToolsButton.setMaxWidth(Double.MAX_VALUE);
        geoToolsButton.setAlignment(Pos.CENTER_LEFT);
        geoToolsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;");
        
        // Radiometric tools button
        Button radiometricToolsButton = new Button("Radiometric Tools ▼");
        radiometricToolsButton.setMaxWidth(Double.MAX_VALUE);
        radiometricToolsButton.setAlignment(Pos.CENTER_LEFT);
        radiometricToolsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;");
        
        VBox geometricBox = new VBox(12);
        geometricBox.setVisible(false);
        geometricBox.setManaged(false);
        geometricBox.setPadding(new Insets(0, 0, 10, 15));

        VBox radiometricBox = new VBox(12);
        radiometricBox.setVisible(false);
        radiometricBox.setManaged(false);
        radiometricBox.setPadding(new Insets(0, 0, 10, 15));

        Button grayscaleButton = new Button("Grayscale (Preview)");
        grayscaleButton.setMaxWidth(Double.MAX_VALUE);
        grayscaleButton.setStyle("-fx-background-color: #1F3D52; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-border-color: #00A3FF; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        grayscaleButton.setOnAction(e -> {
            if (currentImage != null) {
                brightSlider.setValue(0); 
                isBorderPreviewActive = false;
                isGrayscalePreviewActive = true;
                Image grayPreview = ImageProcessor.convertToGrayscale(currentImage);
                imageView.setImage(grayPreview);
                statusLabel.setText("Status: Previewing Grayscale.");
            }
        });

        Button commitGrayscaleButton = new Button("Apply Grayscale");
        commitGrayscaleButton.setMaxWidth(Double.MAX_VALUE);
        commitGrayscaleButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        commitGrayscaleButton.setOnAction(e -> {
            if (currentImage != null && isGrayscalePreviewActive) {
                currentImage = ImageProcessor.convertToGrayscale(currentImage);
                isGrayscalePreviewActive = false;
                imageView.setImage(currentImage);
                statusLabel.setText("Status: Grayscale applied to stack.");
            }
        });

        // --- Border Block ---
        Button borderButton = new Button("Add Border (Preview)");
        borderButton.setMaxWidth(Double.MAX_VALUE);
        borderButton.setStyle("-fx-background-color: #1F3D52; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-border-color: #00A3FF; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        borderButton.setOnAction(e -> {
            if (currentImage != null) {
                brightSlider.setValue(0);
                isGrayscalePreviewActive = false;
                isBorderPreviewActive = true;
                Image borderedPreview = ImageProcessor.addBorder(currentImage);
                imageView.setImage(borderedPreview);
                statusLabel.setText("Status: Previewing Border.");
            }
        });

        Button commitBorderButton = new Button("Apply Border");
        commitBorderButton.setMaxWidth(Double.MAX_VALUE);
        commitBorderButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        commitBorderButton.setOnAction(e -> {
            if (currentImage != null && isBorderPreviewActive) 
            {
                currentImage = ImageProcessor.addBorder(currentImage);
                isBorderPreviewActive = false;
                imageView.setImage(currentImage);
                statusLabel.setText("Status: Border applied to stack.");
            }
        });

        Button emailButton = new Button("Share via Email");
        emailButton.setMaxWidth(Double.MAX_VALUE);
        emailButton.setAlignment(Pos.CENTER_LEFT);
        emailButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        emailButton.setOnAction(e -> {

            try {

                Desktop.getDesktop().browse(new URI("mailto:?subject=Shared Image&body=Check out this edited image!"));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button whatsappButton = new Button("Share via WhatsApp");
        whatsappButton.setMaxWidth(Double.MAX_VALUE);
        whatsappButton.setAlignment(Pos.CENTER_LEFT);
        whatsappButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        whatsappButton.setOnAction(e -> {

            try 
            {
                String message = "Check out this edited image!";
                String whatsappURL = "https://wa.me/?text=" + URLEncoder.encode(message, "UTF-8");

                Desktop.getDesktop().browse(new URI(whatsappURL));

            } 
            catch (Exception ex) 
            {
                ex.printStackTrace();
            }
        });


        Label lblScale = new Label("Choose Scale Factor:");
        lblScale.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");
        
        ComboBox<Double> scaleOptions = new ComboBox<>();
        scaleOptions.getItems().addAll(0.5, 1.0, 1.5, 2.0, 3.0, 5.0);
        scaleOptions.setValue(1.0); // Default value
        scaleOptions.setMaxWidth(Double.MAX_VALUE);
        scaleOptions.setStyle("-fx-background-color: #262626; -fx-text-fill: white; -fx-mark-color: white;");

        Button btnApplyScale = new Button("Apply Scaling");
        btnApplyScale.setMaxWidth(Double.MAX_VALUE);
        btnApplyScale.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");

        btnApplyScale.setOnAction(e -> {
            if (originalImage != null) 
            {
                double factor = scaleOptions.getValue();
                
                // 1. Calculate the new scale transformation directly from the source file
                currentImage = GeometricTransformations.scale(currentImage, factor); 
                
                // 2. Refresh the display canvas
                imageView.setImage(currentImage);
                
                // 3. Clear slider leftovers so your UI doesn't visually drift out of sync
                if (brightSlider != null) brightSlider.setValue(0);
                if (rotateSlider != null) 
                {
                    rotateSlider.setValue(0);
                    textAngle.setText("0");
                }
                
                statusLabel.setText("Status: Image scaled to " + factor + "x baseline. Stack layers refreshed.");
            }
        });

        // Rotating Image
        Label rotateLabel = new Label("Rotation Angle");
        rotateLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");

        // Layout container to place the slider and text box side-by-side
        HBox rotateInputContainer = new HBox(10);
        rotateInputContainer.setAlignment(Pos.CENTER_LEFT);

        rotateSlider = new Slider(0, 360, 0);
        rotateSlider.setStyle("-fx-control-inner-background: #262626;");
        HBox.setHgrow(rotateSlider, Priority.ALWAYS); // Let slider expand to fill space

        textAngle = new TextField("0");
        textAngle.setPrefWidth(55);
        textAngle.setStyle("-fx-background-color: #262626; -fx-text-fill: white; -fx-border-color: #444; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-alignment: center;");

        // Add elements to the horizontal row container
        rotateInputContainer.getChildren().addAll(rotateSlider, textAngle);

        // 1. Slider Listener: Updates the text field and live preview during dragging
        rotateSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (!textAngle.isFocused()) 
            {
                // Display angle as a clean integer string while sliding
                textAngle.setText(String.valueOf(Math.round(newV.doubleValue())));
            }
            if (currentImage != null) 
            {
                Image preview = GeometricTransformations.rotate(currentImage, newV.doubleValue());
                imageView.setImage(preview);
            }
        });

        // 2. TextField Listener: Triggers when the user hits Enter or types an exact value
        textAngle.setOnAction(e -> {
            try 
            {
                double typedAngle = Double.parseDouble(textAngle.getText());
                
                // Clamp the typed value between a valid 0-360 degree circle frame boundary
                if (typedAngle < 0) typedAngle = 0;
                if (typedAngle > 360) typedAngle = 360;
                
                textAngle.setText(String.valueOf(Math.round(typedAngle)));
                rotateSlider.setValue(typedAngle); // This automatically kicks off a live preview update
                
            } 
            catch (NumberFormatException ex) 
            {
                // Safety net: Reset back to current slider position if letters are typed
                textAngle.setText(String.valueOf(Math.round(rotateSlider.getValue())));
            }
        });

        // Optional safety: also update when user clicks away from the text box
        textAngle.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) 
            { // Focus lost
                textAngle.fireEvent(new javafx.event.ActionEvent(textAngle, null));
            }
        });

        Button commitRotateButton = new Button("Apply Rotation");
        commitRotateButton.setMaxWidth(Double.MAX_VALUE);
        commitRotateButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        commitRotateButton.setOnAction(e -> {
            if (currentImage != null) {
                // Bake the final angle value into the image processing stack pipeline
                currentImage = GeometricTransformations.rotate(currentImage, rotateSlider.getValue());
                rotateSlider.setValue(0); // Reset slider back to base 0
                textAngle.setText("0");    // Reset textbox text display back to base 0
                imageView.setImage(currentImage);
                statusLabel.setText("Rotation applied to stack.");
            }
        });

        // Brightness control
        Label lblBrightness = new Label("Brightness");
        lblBrightness.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");
        
        brightSlider = new Slider(-0.5, 0.5, 0);
        brightSlider.setStyle("-fx-control-inner-background: #262626;");
        brightSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (currentImage != null) 
            {
                Image preview = ImageProcessor.adjustBrightness(currentImage, newV.doubleValue());
                imageView.setImage(preview);
            }
        });

        Button btnCommitBrightness = new Button("Apply Brightness");
        btnCommitBrightness.setMaxWidth(Double.MAX_VALUE);
        btnCommitBrightness.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");

        btnCommitBrightness.setOnAction(e -> {
            if (currentImage != null) 
            {
                // This "bakes" the current brightness into the image stack
                currentImage = ImageProcessor.adjustBrightness(currentImage, brightSlider.getValue());
                brightSlider.setValue(0); // Reset slider to neutral after committing
                imageView.setImage(currentImage);
                statusLabel.setText("Brightness applied to stack.");
            }
        });

        // --- Contrast Block ---
        Label contrastLabel = new Label("Contrast");
        contrastLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");

        contrastSlider = new Slider(0.5, 2.0, 1.0);
        contrastSlider.setShowTickLabels(true);
        contrastSlider.setShowTickMarks(true);
        contrastSlider.setStyle("-fx-control-inner-background: #262626;");
        
        contrastSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (currentImage != null) {
                // Reset active button previews to avoid layer blending issues
                isGrayscalePreviewActive = false;
                isBorderPreviewActive = false;
                
                // Live preview generated off the active working image state
                Image contrastPreview = ImageProcessor.adjustContrast(currentImage, newVal.doubleValue());
                imageView.setImage(contrastPreview);
            }
        });

        Button commitContrastButton = new Button("Commit Contrast");
        commitContrastButton.setMaxWidth(Double.MAX_VALUE);
        commitContrastButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        commitContrastButton.setOnAction(e -> {
            if (currentImage != null) {
                // Permanently bake the contrast transformation into the image stack
                currentImage = ImageProcessor.adjustContrast(currentImage, contrastSlider.getValue());
                contrastSlider.setValue(1.0); // Reset slider back to the neutral 1.0 center point
                imageView.setImage(currentImage);
                statusLabel.setText("Status: Contrast changes applied to stack.");
            }
        });


        Label lblFlip = new Label("Mirror Adjustments");
        lblFlip.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");

        HBox flipButtonContainer = new HBox(10);
        flipButtonContainer.setAlignment(Pos.CENTER);

        Button btnFlipHorizontal = new Button("Flip Horizontal ⇄");
        btnFlipHorizontal.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnFlipHorizontal, Priority.ALWAYS);
        btnFlipHorizontal.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnFlipHorizontal.setOnAction(e -> {
            if (currentImage != null) {
                // Instantly flip and bake into current baseline stack
                currentImage = GeometricTransformations.flipHorizontal(currentImage);
                imageView.setImage(currentImage);
                statusLabel.setText("Status: Flipped image horizontally.");
            }
        });

        Button btnFlipVertical = new Button("Flip Vertical ⇅");
        btnFlipVertical.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnFlipVertical, Priority.ALWAYS);
        btnFlipVertical.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnFlipVertical.setOnAction(e -> {
            if (currentImage != null) {
                // Instantly flip and bake into current baseline stack
                currentImage = GeometricTransformations.flipVertical(currentImage);
                imageView.setImage(currentImage);
                statusLabel.setText("Status: Flipped image vertically.");
            }
        });

        flipButtonContainer.getChildren().addAll(btnFlipHorizontal, btnFlipVertical);

        geometricBox.getChildren().addAll(lblScale, scaleOptions, btnApplyScale, new Separator(), rotateLabel, rotateInputContainer, flipButtonContainer, commitRotateButton);
        geoToolsButton.setOnAction(e -> 
        {
            toggle(geometricBox);
        });

        radiometricBox.getChildren().addAll(lblBrightness, brightSlider, btnCommitBrightness, new Separator(), contrastLabel, contrastSlider, commitContrastButton, new Separator(), grayscaleButton, commitGrayscaleButton, new Separator(), borderButton, commitBorderButton);
        radiometricToolsButton.setOnAction(e -> {
            toggle(radiometricBox);
        });

        // 3. Extraction Section (Collapsible)
        Button extractionToolsButton = new Button("Object Extraction ▼");
        extractionToolsButton.setMaxWidth(Double.MAX_VALUE);
        extractionToolsButton.setAlignment(Pos.CENTER_LEFT);
        extractionToolsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;");
        
        VBox extractionBox = new VBox(12);
        extractionBox.setVisible(false); 
        extractionBox.setManaged(false);
        extractionBox.setPadding(new Insets(0, 0, 10, 15));

        ColorPicker colorPicker = new ColorPicker(Color.BLUE);
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setStyle("-fx-background-color: #262626;");
        
        Slider threshold = new Slider(0, 1, 0.4);
        threshold.setStyle("-fx-control-inner-background: #262626;");
        
        Button extractButton = new Button("Extract (Preview)");
        extractButton.setMaxWidth(Double.MAX_VALUE);
        extractButton.setStyle("-fx-background-color: #1F3D52; -fx-text-fill: #00A3FF; -fx-font-weight: bold; -fx-border-color: #00A3FF; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        
        extractButton.setOnAction(e -> {
            if (currentImage != null) 
            {
                // Live preview only - doesn't change currentImage baseline yet
                Image preview = ObjectSelector.extractByColor(currentImage, colorPicker.getValue(), threshold.getValue());
                imageView.setImage(preview);
                statusLabel.setText("Status: Previewing Color Extraction.");
            }
        });

        Button btnCommitExtract = new Button("Commit Extract");
        btnCommitExtract.setMaxWidth(Double.MAX_VALUE);
        btnCommitExtract.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        
        btnCommitExtract.setOnAction(e -> {
            if (currentImage != null) 
            {
                // Bakes the extraction results into the image state pipeline
                currentImage = ObjectSelector.extractByColor(currentImage, colorPicker.getValue(), threshold.getValue());
                imageView.setImage(currentImage);
                statusLabel.setText("Status: Extraction applied to stack.");
            }
        });

        Label lblColor = new Label("Target Color:");
        lblColor.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");
        Label lblThreshold = new Label("Threshold:");
        lblThreshold.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");

        extractionBox.getChildren().addAll(lblColor, colorPicker, lblThreshold, threshold, extractButton, btnCommitExtract);
        extractionToolsButton.setOnAction(e -> toggle(extractionBox));

        // Bottom action buttons inside the Sidebar
        Button saveButton = new Button("Save Image");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        saveButton.setOnAction(e -> {
            if (currentImage != null)
            {
                // Modification logic from original request to match database save behavior if required:
                ObjectSelector.saveExtractedObject(currentImage, "extracted_" + System.currentTimeMillis() + ".png");
                statusLabel.setText("Status: Saved to DarkroomLibrary");
            }
        });

        Button resetButton = new Button("Reset to Original");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setStyle("-fx-background-color: #331A1A; -fx-text-fill: #FF5555; -fx-font-weight: bold; -fx-border-color: #552222; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        resetButton.setOnAction(e -> {
            if (originalImage != null) 
            {
                currentImage = originalImage; // Overwrite the stack with the original
                imageView.setImage(currentImage);
                if (brightSlider != null) 
                {
                    brightSlider.setValue(0);
                }
                if (rotateSlider != null) 
                {
                    rotateSlider.setValue(0);
                    textAngle.setText("0");
                }
                // --- Add this line for the contrast slider ---
                if (contrastSlider != null) {
                    contrastSlider.setValue(1.0);
                }

                statusLabel.setText("Status: All effects cleared.");
            }
        });

        sideMenu.getChildren().addAll(
            menuTitle, loadButton, new Separator(), geoToolsButton, geometricBox, 
            radiometricToolsButton, radiometricBox, 
            extractionToolsButton, extractionBox, new Separator(), 
            saveButton, emailButton, whatsappButton, resetButton);

            // Wrap the sidebar in a ScrollPane to allow scrolling when expanded
        ScrollPane sidebarScrollPane = new ScrollPane(sideMenu);
        sidebarScrollPane.setFitToWidth(true); // Forces sidebar contents to match width
        sidebarScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scrollbar
        sidebarScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Only show vertical scrollbar if needed
        
        // Remove background borders and matching colors for a seamless dark-theme look
        sidebarScrollPane.setStyle("-fx-background: " + BG_DARK + "; -fx-background-color: transparent; -fx-viewport-background-to-transparent: true; -fx-border-width: 0;");

        // Center Display (Matches Custom Video Output Container Panel)
        heartIcon.setTextFill(Color.RED);
        heartIcon.setStyle("-fx-font-size: 40px;");
        heartIcon.setVisible(false);
        
        StackPane centerStack = new StackPane(imageView, heartIcon);
        centerStack.setPadding(new Insets(25));
        centerStack.setStyle("-fx-background-color: " + BG_DARK + ";");
        
        // Window Output Frame Container Card Block
        VBox displayWindowFrame = new VBox(15);
        displayWindowFrame.setAlignment(Pos.CENTER);
        displayWindowFrame.setPadding(new Insets(20));
        displayWindowFrame.setStyle("-fx-background-color: " + PANEL_DARK + "; -fx-background-radius: 10px; -fx-border-color: #2b2b2b; -fx-border-radius: 10px;");
        
        Label frameHeader = new Label("Generated Image Output View");
        frameHeader.setStyle("-fx-text-fill: " + TEXT_LIGHT + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        
        imageView.setPreserveRatio(true);
        // imageView.fitWidthProperty().bind(primaryStage.widthProperty().multiply(0.55));
        imageView.fitHeightProperty().bind(primaryStage.heightProperty().subtract(180));
        imageView.fitWidthProperty().bind(primaryStage.widthProperty().subtract(360));
        
        displayWindowFrame.getChildren().addAll(frameHeader, centerStack);
        
        // Wrapper box to give clean margin contrast in Center
        StackPane centerWrapper = new StackPane(displayWindowFrame);
        centerWrapper.setPadding(new Insets(30));
        centerWrapper.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Bottom Media Style Status Bar -
        HBox bottom = new HBox(15, statusLabel);
        bottom.setPadding(new Insets(12, 20, 12, 20));
        bottom.setStyle("-fx-background-color: " + PANEL_DARK + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1 0 0 0;");
        statusLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-family: 'monospace';");

        BorderPane root = new BorderPane();
        root.setLeft(sidebarScrollPane);
        root.setCenter(centerWrapper);
        root.setBottom(bottom);

        primaryStage.setScene(new Scene(root, 1150, 760));
        primaryStage.show();
    }

    private void loadFile(Stage stage) 
    {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(stage);
        if (file != null) 
        {
            originalImage = new Image(file.toURI().toString());
            currentImage = originalImage;
            imageView.setImage(currentImage);
            // RESET BOTH SLIDERS FOR THE NEW IMAGE
            if (brightSlider != null) 
            {
                brightSlider.setValue(0);
            }
            if (rotateSlider != null) 
            {
                rotateSlider.setValue(0);
                textAngle.setText("0");
            }

            // --- Add this line for the contrast slider ---
            if (contrastSlider != null) {
                contrastSlider.setValue(1.0);
            }

            statusLabel.setText("Status: Loaded " + file.getName());
            heartIcon.setVisible(false); 
        }
    }

    public void loadExternalTargetImage(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            originalImage = new Image(file.toURI().toString());
            currentImage = originalImage;
            imageView.setImage(currentImage);
            // RESET BOTH SLIDERS FOR THE NEW IMAGE
            if (brightSlider != null) {
                brightSlider.setValue(0);
            }
            if (rotateSlider != null) {
                rotateSlider.setValue(0);
                textAngle.setText("0");
            }
            statusLabel.setText("Status: Loaded " + file.getName());
            heartIcon.setVisible(false);
        }
    }

    private void toggle(VBox box) 
    {
        boolean v = box.isVisible();
        box.setVisible(!v);
        box.setManaged(!v);
    }

    public static void main(String[] args) 
    { 
        launch(args); 
    }
}

// import javafx.application.Application;
// import javafx.geometry.*;
// import javafx.scene.Scene;
// import javafx.scene.control.*;
// import javafx.scene.image.*;
// import javafx.scene.layout.*;
// import javafx.scene.paint.Color;
// import javafx.stage.FileChooser;
// import javafx.stage.Stage;
// import java.io.File;

// public class MainApp extends Application 
// {
//     private Image originalImage; // The untouched file
//     private Image currentImage;  // The image after transformations are applied
//     private ImageView imageView = new ImageView();
//     private Label heartIcon = new Label("♥");
//     private Label statusLabel = new Label("Status: Ready");
//     private ProgressBar progressBar = new ProgressBar(0);
//     private Slider brightSlider;

//     @Override
//     public void start(Stage primaryStage) 
//     {
//         primaryStage.setTitle("Image Editor");

//         // Left Sidebar 
//         VBox sideMenu = new VBox(10);
//         sideMenu.setPadding(new Insets(15));
//         sideMenu.setPrefWidth(250);
//         sideMenu.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 1 0 0;");

//         // Load Button
//         Button loadButton = new Button("Open Image");
//         loadButton.setMaxWidth(Double.MAX_VALUE);
//         loadButton.setOnAction(e -> loadFile(primaryStage));

//         // 2. Geometric Section (Collapsible)
//         Button geoToolsButton = new Button("Geometric Tools ▼");
//         geoToolsButton.setMaxWidth(Double.MAX_VALUE);
//         VBox geometricBox = new VBox(10);
//         geometricBox.setVisible(false);
//         geometricBox.setManaged(false);
//         geometricBox.setPadding(new Insets(10, 0, 10, 20));

//         Label lblScale = new Label("Choose Scale Factor:");
//         ComboBox<Double> scaleOptions = new ComboBox<>();
//         scaleOptions.getItems().addAll(0.5, 1.0, 1.5, 2.0, 3.0, 5.0);
//         scaleOptions.setValue(1.0); // Default value
//         scaleOptions.setMaxWidth(Double.MAX_VALUE);

//         Button btnApplyScale = new Button("Apply Scaling");
//         btnApplyScale.setMaxWidth(Double.MAX_VALUE);

//         btnApplyScale.setOnAction(e -> {
//             if (currentImage != null) 
//             {
//                 System.out.println("HOHOH");
//                 double factor = scaleOptions.getValue();
//                 // Use currentImage as the source
//                 currentImage = GeometricTransformations.scale(currentImage, factor); 
//                 imageView.setImage(currentImage);
//                 statusLabel.setText("Status: Scaled. You can now apply other effects.");
//             }
//         });
        
//         // Button scaleButton = new Button("Scale 2.0x");
//         // scaleButton.setOnAction(e -> {
//         //     if (originalImage != null) {
//         //         Image scaled = GeometricTransformations.scale(originalImage, 2.0);
//         //         imageView.setImage(scaled);
//         //     }
//         // });

//         // Brightness control
//         // VBox radBox = new VBox(5);
//         // radBox.setVisible(false); 
//         // radBox.setManaged(false);
//         // radBox.setPadding(new Insets(5, 0, 10, 20));
//         brightSlider = new Slider(-0.5, 0.5, 0);
//         brightSlider.valueProperty().addListener((obs, oldV, newV) -> {
//             if (currentImage != null) 
//             {
//                 Image preview = ImageProcessorTest.adjustBrightness(currentImage, newV.doubleValue());
//                 imageView.setImage(preview);
//             }
//         });

//         Button btnCommitBrightness = new Button("Commit Brightness");

//         btnCommitBrightness.setOnAction(e -> {
//             if (currentImage != null) 
//             {
//                 // This "bakes" the current brightness into the image stack
//                 currentImage = ImageProcessorTest.adjustBrightness(currentImage, brightSlider.getValue());
//                 brightSlider.setValue(0); // Reset slider to neutral after committing
//                 imageView.setImage(currentImage);
//                 statusLabel.setText("Brightness applied to stack.");
//             }
//         });


//         geometricBox.getChildren().addAll(lblScale, scaleOptions, btnApplyScale, new Separator(), new Label("Brightness"), brightSlider, btnCommitBrightness);
//         geoToolsButton.setOnAction(e -> 
//         {
//                 toggle(geometricBox);
//     }   );

//         // 3. Extraction Section (Collapsible)
//         Button extractionToolsButton = new Button("Object Extraction ▼");
//         extractionToolsButton.setMaxWidth(Double.MAX_VALUE);
//         VBox extractionBox = new VBox(10);
//         extractionBox.setVisible(false); 
//         extractionBox.setManaged(false);
//         extractionBox.setPadding(new Insets(5, 0, 10, 20));

//         ColorPicker colorPicker = new ColorPicker(Color.BLUE);
//         Slider threshold = new Slider(0, 1, 0.4);
//         Button extractButton = new Button("Extract");
//         extractButton.setOnAction(e -> {
//             if (currentImage != null) 
//             {
//                 currentImage = ObjectSelector.extractByColor(currentImage, colorPicker.getValue(), threshold.getValue());
//                 imageView.setImage(currentImage);
//                 statusLabel.setText("Status: Extracted Image");
//             }
//         });

//         extractionBox.getChildren().addAll(new Label("Target Color:"), colorPicker, new Label("Threshold:"), threshold, extractButton);
//         extractionToolsButton.setOnAction(e -> toggle(extractionBox));

//         Button saveButton = new Button("Save Image");
//         saveButton.setOnAction(e -> {
//             if (currentImage != null)
//             {
//                 ObjectSelector.extractByColor(currentImage, colorPicker.getValue(), threshold.getValue());
//                 ObjectSelector.saveExtractedObject(currentImage, "extracted_result.png");
//                 statusLabel.setText("Status: Saved extracted_result.png");
//             }
//         });

//         Button resetButton = new Button("Reset to Original");
//         resetButton.setStyle("-fx-base: #ff9999;"); // Light red color

//         resetButton.setOnAction(e -> {
//             if (originalImage != null) {
//                 currentImage = originalImage; // Overwrite the stack with the original
//                 imageView.setImage(currentImage);
//                 brightSlider.setValue(0);
//                 statusLabel.setText("Status: All effects cleared.");
//             }
//         });

//         // // 4. Radiometric Section (Collapsible)
//         // Button btnRadHeader = new Button("Radiometric Adjustment ▼");
//         // btnRadHeader.setMaxWidth(Double.MAX_VALUE);
//         // VBox radBox = new VBox(5);
//         // radBox.setVisible(false); 
//         // radBox.setManaged(false);
//         // radBox.setPadding(new Insets(5, 0, 10, 20));

//         // Slider brightSlider = new Slider(-0.5, 0.5, 0);
//         // brightSlider.valueProperty().addListener((obs, oldV, newV) -> {
//         //     if (originalImage != null) {
//         //         imageView.setImage(ImageProcessorTest.adjustBrightness(originalImage, newV.doubleValue()));
//         //     }
//         // });
//         // radBox.getChildren().addAll(new Label("Brightness:"), brightSlider);
//         // btnRadHeader.setOnAction(e -> toggle(radBox));

//         sideMenu.getChildren().addAll(loadButton, new Separator(), geoToolsButton, geometricBox, extractionToolsButton, extractionBox, new Separator(), saveButton, resetButton);

//         // --- Center Display (Requirement 2.1) ---
//         heartIcon.setTextFill(Color.RED);
//         heartIcon.setStyle("-fx-font-size: 40px;");
//         heartIcon.setVisible(false);
        
//         StackPane centerStack = new StackPane(imageView, heartIcon);
//         centerStack.setStyle("-fx-background-color: #333;");
//         StackPane.setAlignment(heartIcon, Pos.TOP_RIGHT);
//         StackPane.setMargin(heartIcon, new Insets(20));
//         imageView.setPreserveRatio(true);
//         imageView.fitWidthProperty().bind(centerStack.widthProperty().multiply(0.8));

//         // --- Bottom Status Bar ---
//         HBox bottom = new HBox(15, statusLabel);
//         bottom.setPadding(new Insets(10));
//         bottom.setStyle("-fx-background-color: #ddd;");

//         BorderPane root = new BorderPane();
//         root.setLeft(sideMenu);
//         root.setCenter(centerStack);
//         root.setBottom(bottom);

//         primaryStage.setScene(new Scene(root, 1100, 750));
//         primaryStage.show();
//     }

//     private void loadFile(Stage stage) 
//     {
//         FileChooser fc = new FileChooser();
//         File file = fc.showOpenDialog(stage);
//         if (file != null) 
//         {
//             originalImage = new Image(file.toURI().toString());
//             currentImage = originalImage;
//             imageView.setImage(currentImage);
//             brightSlider.setValue(0);
//             statusLabel.setText("Loaded: " + file.getName());
//             // Show Heart as an example of Metadata indicator

//             heartIcon.setVisible(false); 
//         }
//     }

//     private void toggle(VBox box) {
//         boolean v = box.isVisible();
//         box.setVisible(!v);
//         box.setManaged(!v);
//     }

//     public static void main(String[] args) { launch(args); }
// }

// import javafx.application.Application;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.Scene;
// import javafx.scene.control.*;
// import javafx.scene.image.Image;
// import javafx.scene.image.ImageView;
// import javafx.scene.layout.*;
// import javafx.scene.paint.Color;
// import javafx.scene.text.Font;
// import javafx.scene.text.Text;
// import javafx.stage.FileChooser;
// import javafx.stage.Stage;
// import java.io.File;

// public class MainApp extends Application 
// {
//     private ImageView imageView = new ImageView();
//     private StackPane imageContainer = new StackPane();
//     private Text heartIndicator = new Text("♥");
//     private Image originalImage;

//     // Main layout components
//     private BorderPane root = new BorderPane();
//     private VBox sideMenu = new VBox(15); // Left sidebar for functions
//     private HBox statusBar = new HBox(10); // Bottom bar for status/volume
//     private StackPane displayArea = new StackPane(); // Center area for image

//     // Shared UI elements
//     private Label statusLabel = new Label("System Ready");
//     private Slider volumeSlider = new Slider(0, 100, 50);

//     @Override
//     public void start(Stage primaryStage) 
//     {
//         primaryStage.setTitle("WIG3003 Multimedia System 2026");

//         // --- 1. LEFT SIDEBAR (The Navigation Module) ---
//         // Requirement 2.1: Navigation and Selection [cite: 10]
//         sideMenu.setPadding(new Insets(20));
//         sideMenu.setPrefWidth(200);
//         sideMenu.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0;");
        
//         Label menuLabel = new Label("FUNCTIONS");
//         menuLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

//         // --- UI Components ---
//         Button btnLoad = new Button("Load Image");
//         Button btnScale = new Button("Scale (2x)");
//         Button btnExtract = new Button("Extract Object");
//         ColorPicker colorPicker = new ColorPicker(Color.BLUE);
//         colorPicker.setTooltip(new Tooltip("Select target color for extraction"));

//         // Setting width to MAX so buttons are uniform
//         btnLoad.setMaxWidth(Double.MAX_VALUE);
//         btnScale.setMaxWidth(Double.MAX_VALUE);
//         btnExtract.setMaxWidth(Double.MAX_VALUE);

//         // Add to sidebar - Your friends can add their buttons here later!
//         sideMenu.getChildren().addAll(menuLabel, new Separator(), btnLoad, btnScale, btnExtract);

//         // // Requirement 2.2: Slider for continuous control [cite: 15]
//         // Label brightnessLabel = new Label("Brightness:");
//         // Slider brightnessSlider = new Slider(-0.5, 0.5, 0);
//         // brightnessSlider.setShowTickLabels(true);

//         // // Requirement 2.1: Visual Status Indicator (Red Heart) 
//         // heartIndicator.setFill(Color.RED);
//         // heartIndicator.setFont(Font.font(24));
//         // heartIndicator.setVisible(false); // Only show if annotated

//         // // --- Layout ---
//         // imageContainer.getChildren().addAll(imageView, heartIndicator);
//         // StackPane.setAlignment(heartIndicator, Pos.TOP_RIGHT); // Upper-right quadrant 

//         // VBox controls = new VBox(10, btnLoad, colorPicker, btnScale, btnExtract, brightnessLabel, brightnessSlider);
//         // controls.setPrefWidth(150);

//         // --- 2. CENTER DISPLAY (The Visualization Module) ---
//         imageView.setPreserveRatio(true);
//         imageView.setFitWidth(600);
//         displayArea.getChildren().add(imageView);
//         displayArea.setStyle("-fx-background-color: #2b2b2b;"); // Dark background for contrast

//         // --- 3. BOTTOM BAR (The Status & Playback Module) ---
//         // Requirement 2.3: Playback control and status indicators 
//         statusBar.setPadding(new Insets(10, 20, 10, 20));
//         statusBar.setAlignment(Pos.CENTER_LEFT);
//         statusBar.setStyle("-fx-background-color: #eeeeee; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

//         Label volLabel = new Label("Volume:");
//         volumeSlider.setPrefWidth(150);
        
//         // Progress bar can act as a status or seek bar 
//         ProgressBar progressBar = new ProgressBar(0);
//         progressBar.setPrefWidth(200);

//         // Region to push volume to the right
//         Region spacer = new Region();
//         HBox.setHgrow(spacer, Priority.ALWAYS);

//         statusBar.getChildren().addAll(statusLabel, progressBar, spacer, volLabel, volumeSlider);

//         // --- Event Handling ---
//         btnLoad.setOnAction(e -> {
//             FileChooser fileChooser = new FileChooser();
//             File file = fileChooser.showOpenDialog(primaryStage);
//             if (file != null) {
//             originalImage = new Image(file.toURI().toString()); // Save the clean copy
//             imageView.setImage(originalImage);
//             heartIndicator.setVisible(true); 
//         }
//         });

//         // Geometric Transformation: Resizing [cite: 17]
//         btnScale.setOnAction(e -> {
//             if (imageView.getImage() != null) {
//                 Image scaled = GeometricTransformations.scale(imageView.getImage(), 2.0);
//                 imageView.setImage(scaled);
//             }
//         });

//         // Object Selection and Extraction [cite: 18]
//         btnExtract.setOnAction(e -> {
//             if (imageView.getImage() != null) {
//                 // Example: Extract pixels similar to Blue
//                 Color seleColor = colorPicker.getValue();
//                 Image extracted = ObjectSelector.extractByColor(imageView.getImage(), seleColor, 0.5);
//                 imageView.setImage(extracted);
//             }
//         });

//         // Radiometric Adjustment via Slider [cite: 15]
//         // brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
//         // if (originalImage != null) {
//         //     // ALWAYS start from originalImage, not imageView.getImage()
//         //     Image bright = ImageProcessorTest.adjustBrightness(originalImage, newVal.doubleValue());
//         //     imageView.setImage(bright);
//         // }
//         // });

//         // --- 4. COMBINE INTO ROOT ---
//         root.setLeft(sideMenu);
//         root.setCenter(displayArea);
//         root.setBottom(statusBar);

//         primaryStage.setScene(new Scene(root, 1100, 750));
//         primaryStage.show();
//     }

//     public static void main(String[] args) 
//     {
//         launch(args);
//     }
// }
