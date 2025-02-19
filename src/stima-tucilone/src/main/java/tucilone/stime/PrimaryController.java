package tucilone.stime;

import java.io.IOException;

import javafx.fxml.FXML;

public class PrimaryController {

    @FXML
    @SuppressWarnings("unused")
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }
}
