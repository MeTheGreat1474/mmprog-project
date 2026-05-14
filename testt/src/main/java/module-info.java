module com.example
{
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.swing;
    requires java.desktop;
    requires opencv;
    requires javafx.graphics;

    opens com.example to javafx.fxml;
    exports com.example;
}
