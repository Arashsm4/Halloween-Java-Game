// CandyCatch.java - mini-game: catch falling candies
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class CandyCatch extends BorderPane implements MiniGame {
    private int gameIndex;

    public CandyCatch(int index) {
        this.gameIndex = index;
        this.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Candy Catch");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Instructions placeholder
        Label instruction = new Label("Catch the falling candies before they hit the ground! (Placeholder)");
        instruction.setWrapText(true);
        instruction.setPadding(new Insets(10));
        this.setCenter(instruction);
        BorderPane.setAlignment(instruction, Pos.CENTER);
        // TODO: Add falling candy animation using Timeline or Canvas

        // Finish button
        Button finishButton = new Button("Catch Complete");
        finishButton.setOnAction(e -> {
            int points = 5 + (int)(Math.random() * 6);
            ScoreManager.addPoints(points);
            AudioManager.playSoundEffect("success.wav");
            if (gameIndex < SceneManager.TOTAL_GAMES - 1) {
                SceneManager.showEnvironment(gameIndex + 1);
            } else {
                SceneManager.showEnding();
            }
        });
        this.setBottom(finishButton);
        BorderPane.setAlignment(finishButton, Pos.CENTER);
    }

    @Override
    public void startGame() {
        // Start candy falling logic or animations
    }

    @Override
    public void endGame() {
        // End candy catch game, stop animations
    }
}
