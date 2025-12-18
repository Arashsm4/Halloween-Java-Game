
package lostinbabuland;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenuScene {
    public void show(Stage stage) {
        Button start = new Button("Start Game");
        start.setOnAction(e -> new PlatformerSprint().start(stage));
        VBox root = new VBox(12, start);
        root.setStyle("-fx-padding: 24;");
        stage.setScene(new Scene(root, 900, 650));
        stage.setTitle("Menu");
        stage.show();
    }
}
