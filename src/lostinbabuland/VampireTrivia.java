// VampireTrivia.java - mini-game: answer vampire-themed trivia
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class VampireTrivia extends BorderPane implements MiniGame {
    private int gameIndex;

    public VampireTrivia(int index) {
        this.gameIndex = index;
        this.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Vampire Trivia");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Instructions placeholder (could be questions)
        Label instruction = new Label("Answer the vampire's trivia questions correctly to proceed! (Placeholder)");
        instruction.setWrapText(true);
        instruction.setPadding(new Insets(10));
        this.setCenter(instruction);
        BorderPane.setAlignment(instruction, Pos.CENTER);
        // TODO: Provide multiple-choice trivia questions and check answers

        // Finish button
        Button finishButton = new Button("Finish Quiz");
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
        // Start trivia (e.g., display first question)
    }

    @Override
    public void endGame() {
        // End trivia game (e.g., tally results)
    }
}
