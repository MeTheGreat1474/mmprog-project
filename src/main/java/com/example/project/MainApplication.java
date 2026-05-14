package com.example.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApplication.class.getResource(
                        "views/dashboard-view.fxml"
                )
        );

        Scene scene = new Scene(
                fxmlLoader.load(),
                1280,
                800
        );

        scene.getStylesheets().add(
                MainApplication.class.getResource(
                        "styles/dashboard.css"
                ).toExternalForm()
        );

        stage.setTitle("Mosaic Module");

        stage.setScene(scene);

        stage.setMinWidth(700);

        stage.setMinHeight(300);

        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {

        // Load OpenCV before launching JavaFX
        OpenCV.loadLocally();

        launch();
    }
}