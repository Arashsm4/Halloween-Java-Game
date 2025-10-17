// DanceDanceGame.java - mini-game: rhythm dance challenge
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class DanceDanceGame extends BorderPane implements MiniGame {
    private int gameIndex;

    public DanceDanceGame(int index) {
        this.gameIndex = index;
        this.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Dance Dance Game");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Instructions placeholder
        Label instruction = new Label("Press the correct keys to dance with the ghosts! (Placeholder)");
        instruction.setWrapText(true);
        instruction.setPadding(new Insets(10));
        this.setCenter(instruction);
        BorderPane.setAlignment(instruction, Pos.CENTER);
        // TODO: Implement rhythm game visuals and key event handling

        // Finish button
        Button finishButton = new Button("Finish Dance");
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
        // Start dance game (e.g., begin music and prompt user input)
    }

    @Override
    public void endGame() {
        // End dance game (e.g., stop music)
    }
}
