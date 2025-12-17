// src/lostinbabuland/CandyCatch.java
package lostinbabuland;

import javafx.stage.Stage;

public class CandyCatch implements MiniGame {
    @Override public void start(Stage stage) { new PlatformerSprint().start(stage); }
}
