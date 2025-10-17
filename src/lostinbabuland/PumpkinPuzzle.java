// PumpkinPuzzle.java - mini-game: a pumpkin puzzle challenge
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class PumpkinPuzzle extends BorderPane implements MiniGame {
    private int gameIndex;

    public PumpkinPuzzle(int index) {
        this.gameIndex = index;
        this.setPadding(new Insets(20));

        // Title at top
        Label titleLabel = new Label("Pumpkin Puzzle");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Instructions/placeholder in center
        Label instruction = new Label("Arrange the pumpkin pieces to solve the puzzle! (Placeholder)");
        instruction.setWrapText(true);
        instruction.setPadding(new Insets(10));
        this.setCenter(instruction);
        BorderPane.setAlignment(instruction, Pos.CENTER);
        // TODO: Add puzzle grid and pumpkin piece visuals

        // Finish button at bottom
        Button finishButton = new Button("Solve Puzzle");
        finishButton.setOnAction(e -> {
            // Award points for completing the puzzle
            int points = 5 + (int)(Math.random() * 6); // random 5-10 points
            ScoreManager.addPoints(points);
            // Play success sound effect
            AudioManager.playSoundEffect("success.wav");
            // Move to next scene (next environment or ending)
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
        // Initialize or start game mechanics (if any)
    }

    @Override
    public void endGame() {
        // Cleanup game resources or timers (if any)
    }
}
