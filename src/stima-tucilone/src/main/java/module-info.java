module tucilone.stime {
    requires transitive javafx.graphics;

    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;

    opens tucilone.stime to javafx.fxml, java.desktop;

    exports tucilone.stime;
    exports tucilone.stime.utils;
}
