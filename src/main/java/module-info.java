module com.example.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires metadata.extractor;
    requires javafx.media;
    requires javafx.swing;
    requires java.desktop;
    requires opencv;
    requires javafx.graphics;

    opens com.example.project to javafx.fxml;
    exports com.example.project;

    opens com.example.project.controllers to javafx.fxml;
    exports com.example.project.controllers;
    
    opens com.Thierry to javafx.fxml, javafx.graphics;
    exports com.Thierry;
}
