
package lostinbabuland;

import javafx.application.Application;
import javafx.stage.Stage;

public class GameApp extends Application {
    @Override
    public void start(Stage stage) {
        new PlatformerSprint().start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// big thanks to the creator of the JavaFX but my man your engine got some problems