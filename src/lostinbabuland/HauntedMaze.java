// HauntedMaze.java - mini-game: navigate a haunted maze
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class HauntedMaze extends BorderPane implements MiniGame {
    private int gameIndex;

    public HauntedMaze(int index) {
        this.gameIndex = index;
        this.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Haunted Maze");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Instructions placeholder
        Label instruction = new Label("Find your way out of the haunted maze! (Placeholder)");
        instruction.setWrapText(true);
        instruction.setPadding(new Insets(10));
        this.setCenter(instruction);
        BorderPane.setAlignment(instruction, Pos.CENTER);
        // TODO: Implement maze generation and navigation visuals

        // Finish button
        Button finishButton = new Button("Exit Maze");
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
        // Start maze game logic (e.g., display maze)
    }

    @Override
    public void endGame() {
        // End maze game logic (if needed)
    }
}
