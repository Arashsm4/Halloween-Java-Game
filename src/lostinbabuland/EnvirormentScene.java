// EnvironmentScene.java - story scene between mini-games
package lostinbabuland;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class EnvirormentScene extends BorderPane {
    private int gameIndex;

    public EnvirormentScene(String title, String description, int gameIndex) {
        this.gameIndex = gameIndex;
        this.setPadding(new Insets(20));

        // Title label at top
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        this.setTop(titleLabel);

        // Description text in center
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setPadding(new Insets(10));
        this.setCenter(descLabel);
        BorderPane.setAlignment(descLabel, Pos.CENTER);
        // TODO: Add background image or illustration for the environment

        // Continue button at bottom to proceed to next mini-game
        Button continueButton = new Button("Continue");
        continueButton.setOnAction(e -> {
            // Transition to the associated mini-game
            SceneManager.showMiniGame(gameIndex);
        });
        this.setBottom(continueButton);
        BorderPane.setAlignment(continueButton, Pos.CENTER);
    }
}
