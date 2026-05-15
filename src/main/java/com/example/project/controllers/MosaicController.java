package com.example.project.controllers;

import com.example.project.models.MosaicService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class MosaicController {

    @FXML
    private TextField targetImageField;

    @FXML
    private TextField tileFolderField;

    @FXML
    private Slider tileSizeSlider;

    @FXML
    private Label tileSizeLabel;

    @FXML
    private Button generateButton;

    @FXML
    private ImageView previewImageView;

    @FXML
    private ProgressIndicator progressIndicator;

    private final MosaicService mosaicService = new MosaicService();

    private File targetImageFile;
    private File tileFolder;

    @FXML
    public void initialize() {

        progressIndicator.setVisible(false);

        tileSizeSlider.setMin(10);
        tileSizeSlider.setMax(50);
        tileSizeSlider.setValue(25);

        tileSizeLabel.setText("25");

        tileSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            tileSizeLabel.setText(String.valueOf(newVal.intValue()));
        });
    }

    @FXML
    private void handleChooseTargetImage() {

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Choose Target Image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png",
                        "*.jpg",
                        "*.jpeg"
                )
        );

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {

            targetImageFile = selectedFile;

            targetImageField.setText(selectedFile.getAbsolutePath());

            Image image = new Image(selectedFile.toURI().toString());

            previewImageView.setImage(image);
        }
    }

    public void loadExternalTargetImage(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            targetImageFile = file;
            targetImageField.setText(file.getAbsolutePath());
            Image image = new Image(file.toURI().toString());
            previewImageView.setImage(image);

            // Automatically set tile folder to the image's parent directory
            File parentDir = file.getParentFile();
            if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
                tileFolder = parentDir;
                tileFolderField.setText(parentDir.getAbsolutePath());
            }
        }
    }

    @FXML
    private void handleChooseTileFolder() {

        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle("Choose Tile Images Folder");

        File selectedFolder = directoryChooser.showDialog(null);

        if (selectedFolder != null) {

            tileFolder = selectedFolder;

            tileFolderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    @FXML
    private void handleGenerateMosaic() {

        if (targetImageFile == null || tileFolder == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Input",
                    "Please select a target image and tile folder."
            );

            return;
        }

        generateButton.setDisable(true);
        progressIndicator.setVisible(true);

        int tileSize = (int) tileSizeSlider.getValue();

        File outputFile = new File("mosaic_output.jpg");

        Task<Void> mosaicTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                mosaicService.createMosaic(
                        targetImageFile.getAbsolutePath(),
                        tileFolder.getAbsolutePath(),
                        outputFile.getAbsolutePath(),
                        tileSize
                );

                return null;
            }
        };

        mosaicTask.setOnSucceeded(event -> {

            generateButton.setDisable(false);
            progressIndicator.setVisible(false);

            Image mosaicImage = new Image(outputFile.toURI().toString());

            previewImageView.setImage(mosaicImage);

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Mosaic generated successfully!"
            );
        });

        mosaicTask.setOnFailed(event -> {

            generateButton.setDisable(false);
            progressIndicator.setVisible(false);

            mosaicTask.getException().printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to generate mosaic."
            );
        });

        Thread thread = new Thread(mosaicTask);

        thread.setDaemon(true);

        thread.start();
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message
    ) {

        Alert alert = new Alert(type);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
}
