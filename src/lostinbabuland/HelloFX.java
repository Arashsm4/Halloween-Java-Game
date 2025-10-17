package lostinbabuland;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloFX extends Application {
    @Override public void start(Stage s){ s.setScene(new Scene(new Label("JavaFX OK"), 400, 200)); s.show(); }
    public static void main(String[] args){ launch(args); }
}
