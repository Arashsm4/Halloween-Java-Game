// PlatformerSprint.java - mini-game: running platformer challenge
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class PlatformerSprint extends BorderPane implements MiniGame {
    private int gameIndex;

    public PlatformerSprint(int index) {
        this.gameIndex = index;
        this.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Platformer Sprint");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Instructions placeholder
        Label instruction = new Label("Dash through the platform course and avoid obstacles! (Placeholder)");
        instruction.setWrapText(true);
        instruction.setPadding(new Insets(10));
        this.setCenter(instruction);
        BorderPane.setAlignment(instruction, Pos.CENTER);
        // TODO: Implement platformer movement and obstacle collision

        // Finish button
        Button finishButton = new Button("Finish Line");
        finishButton.setOnAction(e -> {
            int points = 5 + (int)(Math.random() * 6);
            ScoreManager.addPoints(points);
            AudioManager.playSoundEffect("success.wav");
            // Last game leads to ending
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
        // Start platformer game (e.g., begin character movement)
    }

    @Override
    public void endGame() {
        // End platformer game (e.g., stop movement timers)
    }
}
