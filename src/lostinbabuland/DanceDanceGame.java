
package lostinbabuland;

import javafx.stage.Stage;

public class DanceDanceGame implements MiniGame {
    @Override public void start(Stage stage) { new PlatformerSprint().start(stage); }
}
