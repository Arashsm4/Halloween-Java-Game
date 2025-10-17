// GameApp.java - main application entry point
package lostinbabuland;

import javafx.application.Application;
import javafx.stage.Stage;

public class GameApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lost in Babuland - Halloween Adventure");
        // Initialize scene manager with main stage
        SceneManager.initialize(primaryStage);
        // Launch the main menu scene
        SceneManager.showMainMenu();
        // Start background music (if asset available)
        AudioManager.playBackgroundMusic("background.mp3"); // placeholder filename
        // Show the window
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
