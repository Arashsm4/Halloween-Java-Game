// src/lostinbabuland/BatShooter.java
package lostinbabuland;

import javafx.stage.Stage;

public class BatShooter implements MiniGame {
    @Override public void start(Stage stage) { new PlatformerSprint().start(stage); }
}
