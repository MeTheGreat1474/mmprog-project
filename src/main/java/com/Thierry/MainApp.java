package com.Thierry;

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

import com.example.project.models.GeometricTransformations;
import com.example.project.models.ImageProcessorTest;
import com.example.project.models.ObjectSelector;

public class MainApp extends Application {

    private Image originalImage;
    private ImageView imageView = new ImageView();
    private Label heartIcon = new Label("♥");
    private Label statusLabel = new Label("Status: Ready");
    private ProgressBar progressBar = new ProgressBar(0);

    @Override
    public void start(Stage primaryStage) 
    {
        primaryStage.setTitle("Image Editor");

        // Left Sidebar 
        VBox sideMenu = new VBox(10);
        sideMenu.setPadding(new Insets(15));
        sideMenu.setPrefWidth(250);
        sideMenu.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 1 0 0;");

        // Load Button
        Button loadButton = new Button("Open Image");
        loadButton.setMaxWidth(Double.MAX_VALUE);
        loadButton.setOnAction(e -> loadFile(primaryStage));

        // 2. Geometric Section (Collapsible)
        Button geoToolsButton = new Button("Geometric Tools ▼");
        geoToolsButton.setMaxWidth(Double.MAX_VALUE);
        VBox geometricBox = new VBox(5);
        geometricBox.setVisible(false);
        geometricBox.setManaged(false);
        geometricBox.setPadding(new Insets(5, 0, 10, 20));

        Button grayscaleBtn = new Button("Grayscale");
        grayscaleBtn.setOnAction(e -> {
            if (originalImage != null) {
                Image grayImage = ImageProcessorTest.convertToGrayscale(originalImage);
                imageView.setImage(grayImage);
            }
        });

        Button borderBtn = new Button("Add Border");
        borderBtn.setOnAction(e -> {

            if (originalImage != null) {

                Image borderedImage =
                        ImageProcessorTest.addBorder(originalImage);

                imageView.setImage(borderedImage);
            }
        });

        Button scaleButton = new Button("Scale 2.0x");
        scaleButton.setOnAction(e -> {
            if (originalImage != null) {
                Image scaled = GeometricTransformations.scale(originalImage, 2.0);
                imageView.setImage(scaled);
            }
        });

        // Brightness control
         VBox radBox = new VBox(5);
        radBox.setVisible(false); 
        radBox.setManaged(false);
        radBox.setPadding(new Insets(5, 0, 10, 20));
        Slider brightSlider = new Slider(-0.5, 0.5, 0);
        brightSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (originalImage != null) {
                imageView.setImage(ImageProcessorTest.adjustBrightness(originalImage, newV.doubleValue()));
            }
        });
        radBox.getChildren().addAll(new Label("Brightness:"), brightSlider);

        Label contrastLabel = new Label("Contrast");

        Slider contrastSlider = new Slider(0.5, 2.0, 1.0);
        contrastSlider.setShowTickLabels(true);
        contrastSlider.setShowTickMarks(true);

        contrastSlider.valueProperty().addListener((obs, oldVal, newVal) -> {

            if (originalImage != null) {

                Image contrastImage =
                        ImageProcessorTest.adjustContrast(
                                originalImage,
                                newVal.doubleValue()
                        );

                imageView.setImage(contrastImage);
            }
        });

        geometricBox.getChildren().addAll(scaleButton, new Separator(), new Label("Brightness"), brightSlider);
        geoToolsButton.setOnAction(e -> 
        {
                toggle(geometricBox);
    }   );

        // 3. Extraction Section (Collapsible)
        Button extractionToolsButton = new Button("Object Extraction ▼");
        extractionToolsButton.setMaxWidth(Double.MAX_VALUE);
        VBox extractionBox = new VBox(10);
        extractionBox.setVisible(false); 
        extractionBox.setManaged(false);
        extractionBox.setPadding(new Insets(5, 0, 10, 20));

        ColorPicker colorPicker = new ColorPicker(Color.BLUE);
        Slider threshold = new Slider(0, 1, 0.4);
        Button extractButton = new Button("Extract & Save");
        extractButton.setOnAction(e -> {
            if (originalImage != null) {
                Image result = ObjectSelector.extractByColor(originalImage, colorPicker.getValue(), threshold.getValue());
                imageView.setImage(result);
                // ObjectSelector.saveExtractedObject(result, "extracted_result.png");
                statusLabel.setText("Status: Saved extracted_result.png");
            }
        });
        extractionBox.getChildren().addAll(new Label("Target Color:"), colorPicker, new Label("Threshold:"), threshold, extractButton);
        extractionToolsButton.setOnAction(e -> toggle(extractionBox));

        // // 4. Radiometric Section (Collapsible)
        // Button btnRadHeader = new Button("Radiometric Adjustment ▼");
        // btnRadHeader.setMaxWidth(Double.MAX_VALUE);
        // VBox radBox = new VBox(5);
        // radBox.setVisible(false); 
        // radBox.setManaged(false);
        // radBox.setPadding(new Insets(5, 0, 10, 20));

        // Slider brightSlider = new Slider(-0.5, 0.5, 0);
        // brightSlider.valueProperty().addListener((obs, oldV, newV) -> {
        //     if (originalImage != null) {
        //         imageView.setImage(ImageProcessorTest.adjustBrightness(originalImage, newV.doubleValue()));
        //     }
        // });
        // radBox.getChildren().addAll(new Label("Brightness:"), brightSlider);
        // btnRadHeader.setOnAction(e -> toggle(radBox));

        sideMenu.getChildren().addAll(loadButton, new Separator(), geoToolsButton, geometricBox, extractionToolsButton, extractionBox);

        sideMenu.getChildren().add(grayscaleBtn);

        sideMenu.getChildren().add(borderBtn);

        // --- Center Display (Requirement 2.1) ---
        heartIcon.setTextFill(Color.RED);
        heartIcon.setStyle("-fx-font-size: 40px;");
        heartIcon.setVisible(false);
        
        StackPane centerStack = new StackPane(imageView, heartIcon);
        centerStack.setStyle("-fx-background-color: #333;");
        StackPane.setAlignment(heartIcon, Pos.TOP_RIGHT);
        StackPane.setMargin(heartIcon, new Insets(20));
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(centerStack.widthProperty().multiply(0.8));

        // --- Bottom Status Bar ---
        HBox bottom = new HBox(15, statusLabel, progressBar);
        bottom.setPadding(new Insets(10));
        bottom.setStyle("-fx-background-color: #ddd;");

        BorderPane root = new BorderPane();
        root.setLeft(sideMenu);
        root.setCenter(centerStack);
        root.setBottom(bottom);

        primaryStage.setScene(new Scene(root, 1100, 750));
        primaryStage.show();
    }

    private void loadFile(Stage stage) {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            originalImage = new Image(file.toURI().toString());
            imageView.setImage(originalImage);
            statusLabel.setText("Loaded: " + file.getName());
            // Show Heart as an example of Metadata indicator
            heartIcon.setVisible(true); 
        }
    }

    public void loadExternalImage(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            originalImage = new Image(file.toURI().toString());
            imageView.setImage(originalImage);
            statusLabel.setText("Loaded: " + file.getName());
            heartIcon.setVisible(true); 
        }
    }

    private void toggle(VBox box) {
        boolean v = box.isVisible();
        box.setVisible(!v);
        box.setManaged(!v);
    }

    public static void main(String[] args) { launch(args); }
}

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
