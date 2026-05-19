package com.example.project.controllers;

import com.example.project.models.VideoService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoController {

    @FXML private TextArea overlayTextArea;
    @FXML private TextField selectedFolderField;
    @FXML private Button generateVideoButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private MediaView mediaView;
    @FXML private Slider seekSlider;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Button playPauseButton;

    private final VideoService videoService = new VideoService();
    private File selectedFolder;
    private MediaPlayer mediaPlayer;
    private final File outputVideo =
            new File("generated_video.mp4");

    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        seekSlider.setMin(0);
        seekSlider.valueChangingProperty().addListener(
                (obs, wasChanging, isChanging) -> {

                    if (!isChanging && mediaPlayer != null) {

                        mediaPlayer.seek(
                                Duration.seconds(
                                        seekSlider.getValue()
                                )
                        );
                    }
                }
        );
    }

    @FXML
    private void handleChooseImageFolder() {
        DirectoryChooser directoryChooser =
                new DirectoryChooser();

        directoryChooser.setTitle(
                "Choose Favourite Images Folder"
        );

        File folder = directoryChooser.showDialog(null);
        if (folder != null) {
            selectedFolder = folder;
            selectedFolderField.setText(
                    folder.getAbsolutePath()
            );
        }
    }

    public void loadExternalImageFolder(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            selectedFolder = folder;
            selectedFolderField.setText(folder.getAbsolutePath());
        }
    }

    @FXML
    private void handleGenerateVideo() {
        if (selectedFolder == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Missing Folder",
                    "Please choose an image folder."
            );
            return;
        }

        List<String> imagePaths = loadImagePaths(selectedFolder);
        if (imagePaths.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "No Images",
                    "Selected folder contains no valid images."
            );
            return;
        }

        generateVideoButton.setDisable(true);
        progressIndicator.setVisible(true);

        String overlayText = overlayTextArea.getText().trim();

        Task<Void> videoTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                videoService.createVideo(
                        imagePaths,
                        outputVideo.getAbsolutePath(),
                        overlayText
                );

                return null;
            }
        };

        videoTask.setOnSucceeded(event -> {
            generateVideoButton.setDisable(false);
            progressIndicator.setVisible(false);
            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "Video generated successfully!"
            );
            loadVideoPlayer();
        });

        videoTask.setOnFailed(event -> {
            generateVideoButton.setDisable(false);
            progressIndicator.setVisible(false);
            videoTask.getException().printStackTrace();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Failed to generate video."
            );
        });

        Thread thread = new Thread(videoTask);
        thread.setDaemon(true);
        thread.start();
    }

    private List<String> loadImagePaths(File folder) {
        List<String> imagePaths = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) {
            return imagePaths;
        }

        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png")
                    || name.endsWith(".jpg")
                    || name.endsWith(".jpeg")) {

                imagePaths.add(
                        file.getAbsolutePath()
                );
            }
        }
        return imagePaths;
    }

    private void loadVideoPlayer() {
        Media media = new Media(
                outputVideo.toURI().toString()
        );

        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.setOnReady(() -> {

            Duration totalDuration =
                    mediaPlayer.getTotalDuration();

            seekSlider.setMax(
                    totalDuration.toSeconds()
            );

            totalTimeLabel.setText(
                    formatTime(totalDuration)
            );
        });

        mediaPlayer.currentTimeProperty().addListener(
                (obs, oldTime, newTime) -> {

                    if (!seekSlider.isValueChanging()) {

                        seekSlider.setValue(
                                newTime.toSeconds()
                        );
                    }

                    currentTimeLabel.setText(
                            formatTime(newTime)
                    );
                }
        );
    }

    @FXML
    private void handlePlayPause() {

        if (mediaPlayer == null) {
            return;
        }

        MediaPlayer.Status status =
                mediaPlayer.getStatus();

        if (status == MediaPlayer.Status.PLAYING) {

            mediaPlayer.pause();

            playPauseButton.setText("▶");

        } else {

            mediaPlayer.play();

            playPauseButton.setText("⏸");
        }
    }

    @FXML
    private void handleSeekForward() {

        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.seek(
                mediaPlayer.getCurrentTime()
                        .add(Duration.seconds(5))
        );
    }

    @FXML
    private void handleSeekBackward() {

        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.seek(
                mediaPlayer.getCurrentTime()
                        .subtract(Duration.seconds(5))
        );
    }

    @FXML
    private void handleStop() {

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            seekSlider.setValue(0);
            playPauseButton.setText("▶");
        }
    }

    private String formatTime(Duration duration) {
        int totalSeconds =
                (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
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