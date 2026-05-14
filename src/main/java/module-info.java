module com.example.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires metadata.extractor;
    requires opencv;

    opens com.example.project to javafx.fxml;

    exports com.example.project;

    opens com.example.project.controllers to javafx.fxml;

    exports com.example.project.controllers;
}