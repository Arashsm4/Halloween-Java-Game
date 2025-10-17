// MainMenuScene.java - initial menu for player to enter details
package lostinbabuland;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class MainMenuScene extends VBox {
    private TextField nameField;
    private TextField emailField;

    public MainMenuScene() {
        super(15); // vertical spacing
        // Align content to center
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));

        // Title label
        Label titleLabel = new Label("Lost in Babuland");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        // TODO: Add a logo image above title if available
        this.getChildren().add(titleLabel);

        // Name input
        Label nameLabel = new Label("Name:");
        nameField = new TextField();
        nameField.setPrefWidth(200);
        // Email input
        Label emailLabel = new Label("Email:");
        emailField = new TextField();
        emailField.setPrefWidth(200);

        // Container for input fields
        VBox formBox = new VBox(10, nameLabel, nameField, emailLabel, emailField);
        formBox.setAlignment(Pos.CENTER);
        this.getChildren().add(formBox);

        // Start game and Leaderboard buttons
        Button startButton = new Button("Start Adventure");
        Button leaderboardButton = new Button("View Leaderboard");
        HBox buttonBox = new HBox(20, startButton, leaderboardButton);
        buttonBox.setAlignment(Pos.CENTER);
        this.getChildren().add(buttonBox);

        // Handle Start button click
        startButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                // Warn if fields are empty
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter name and email to start.");
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }
            // Create player profile and reset score
            PlayerProfile.currentProfile = new PlayerProfile(name, email);
            ScoreManager.reset();
            // Proceed to the first environment scene
            SceneManager.showEnvironment(0);
        });

        // Handle Leaderboard button click
        leaderboardButton.setOnAction(e -> {
            // Go to leaderboard scene (no profile needed just to view)
            SceneManager.showLeaderboard();
        });
    }
}
