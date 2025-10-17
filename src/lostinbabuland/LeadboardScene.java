// LeaderboardScene.java - displays top player scores
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import java.util.List;

public class LeadboardScene extends BorderPane {
    public LeadboardScene() {
        this.setPadding(new Insets(20));

        // Title label
        Label titleLabel = new Label("Leadboard");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        this.setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        // Grid for scores
        GridPane grid = new GridPane();
        grid.setHgap(50);
        grid.setVgap(5);
        grid.setAlignment(Pos.CENTER);
        // Header row
        Label nameHeader = new Label("Name");
        Label scoreHeader = new Label("Score");
        nameHeader.setStyle("-fx-font-weight: bold;");
        scoreHeader.setStyle("-fx-font-weight: bold;");
        grid.add(nameHeader, 0, 0);
        grid.add(scoreHeader, 1, 0);

        // Get top scores and populate grid
        List<LeadboardManager.ScoreEntry> topScores = LeadboardManager.getTopScores();
        for (int i = 0; i < topScores.size(); i++) {
            LeadboardManager.ScoreEntry entry = topScores.get(i);
            Label nameLabel = new Label(entry.name);
            Label scoreLabel = new Label(Integer.toString(entry.score));
            grid.add(nameLabel, 0, i + 1);
            grid.add(scoreLabel, 1, i + 1);
        }
        this.setCenter(grid);

        // Back to main menu button
        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            SceneManager.showMainMenu();
        });
        this.setBottom(backButton);
        BorderPane.setAlignment(backButton, Pos.CENTER);
    }
}
