// EndingScene.java - concluding scene with final score and options
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class EndingScene extends BorderPane {
    public EndingScene() {
        this.setPadding(new Insets(20));

        // Determine final score and message
        int finalScore = ScoreManager.getScore();
        String message;
        if (finalScore >= 70) {
            message = "Amazing! You escaped Babuland with a great score!";
        } else if (finalScore >= 50) {
            message = "Good job! You made it through Babuland.";
        } else {
            message = "You barely survived Babuland. Better luck next time!";
        }

        // Title/score display
        Label scoreLabel = new Label("Final Score: " + finalScore);
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(10));

        // Center layout for score and message
        VBox centerBox = new VBox(10, scoreLabel, messageLabel);
        centerBox.setAlignment(Pos.CENTER);
        this.setCenter(centerBox);
        BorderPane.setAlignment(centerBox, Pos.CENTER);

        // Buttons for replay and leaderboard
        Button playAgainButton = new Button("Play Again");
        Button leaderboardButton = new Button("View Leaderboard");
        HBox buttonBox = new HBox(20, playAgainButton, leaderboardButton);
        buttonBox.setAlignment(Pos.CENTER);
        this.setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);

        // Play Again -> back to main menu
        playAgainButton.setOnAction(e -> {
            // Reset score and profile for a new game
            ScoreManager.reset();
            PlayerProfile.currentProfile = null;
            SceneManager.showMainMenu();
        });

        // Leaderboard -> submit score and show leaderboard
        leaderboardButton.setOnAction(e -> {
            // Submit score to leaderboard (only name and score)
            if (PlayerProfile.currentProfile != null) {
                LeaderboardManager.submitScore(PlayerProfile.currentProfile.getName(), finalScore);
            } else {
                LeaderboardManager.submitScore("Player", finalScore);
            }
            SceneManager.showLeaderboard();
        });
    }
}
