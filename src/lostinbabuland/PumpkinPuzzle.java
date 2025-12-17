// src/lostinbabuland/PumpkinPuzzle.java
package lostinbabuland;

import javafx.stage.Stage;

public class PumpkinPuzzle implements MiniGame {
    @Override public void start(Stage stage) { new PlatformerSprint().start(stage); }
}
