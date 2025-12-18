
package lostinbabuland;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class LeadboardScene {
    public void show(Stage stage) {
        VBox root = new VBox(10, new Text("Leaderboard (not used in this build)"));
        root.setStyle("-fx-padding: 24;");
        stage.setScene(new Scene(root, 900, 650));
        stage.show();
    }
}
