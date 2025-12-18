
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
