module tucilone.stime {
    requires transitive javafx.graphics;

    requires javafx.controls;
    requires javafx.fxml;

    opens tucilone.stime to javafx.fxml;
    exports tucilone.stime;
}
