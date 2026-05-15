package com.example.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;


public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("views/dashboard-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        // Load fonts if available; fallback via CSS otherwise
        scene.getStylesheets().add(MainApplication.class.getResource("styles/dashboard.css").toExternalForm());
        stage.getIcons()
                .add(new javafx.scene.image.Image(MainApplication.class.getResourceAsStream("images/app_icon.png")));
        stage.setTitle("Atelier");

        // Remove standard OS application borders and title bar
        // stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Reverting back to
        // native OS bar

        stage.setScene(scene);
        // Safe layout minimum constraints
        stage.setMinWidth(700);
        stage.setMinHeight(300);

        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        nu.pattern.OpenCV.loadLocally();
        launch();
    }
}
