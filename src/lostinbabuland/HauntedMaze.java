// src/lostinbabuland/HauntedMaze.java
package lostinbabuland;

import javafx.stage.Stage;

public class HauntedMaze implements MiniGame {
    @Override public void start(Stage stage) { new PlatformerSprint().start(stage); }
}
