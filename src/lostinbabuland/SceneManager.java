package lostinbabuland; // SceneManager.java - Manages scene transitions for the "Lost in Babuland" game.
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

public class SceneManager {
    // Holds the primary stage for scene switching
    private static Stage primaryStage;
    // Total number of mini-game scenes (for dynamic management)
    private static final int TOTAL_MINI_GAMES = 8;

    // Private constructor to prevent instantiation (utility class pattern)
    private SceneManager() { }

    /**
     * Initialize the SceneManager with the main Stage.
     * This must be called once from the Application start method.
     * @param stage the primary Stage of the application
     */
    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Ensure the primary stage is set before switching scenes.
     */
    private static void checkStageInitialized() {
        if (primaryStage == null) {
            throw new IllegalStateException("SceneManager has not been initialized with a Stage.");
        }
    }

    /**
     * Apply a fade-in transition to the given scene root node.
     * This provides a smooth transition effect between scenes.
     * @param root the root node of the new scene
     */
    private static void applyFadeTransition(Parent root) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Switch to the Main Menu scene where the player enters name/email.
     */
    public static void goToMainMenu() {
        checkStageInitialized();
        // Create content for Main Menu (placeholder pane)
        StackPane mainMenuRoot = new StackPane();
        mainMenuRoot.getChildren().add(new Label("Main Menu - Enter Name/Email (placeholder)"));
        Scene mainMenuScene = new Scene(mainMenuRoot, 800, 600);
        // Set the new scene on the primary stage
        primaryStage.setScene(mainMenuScene);
        // Optionally, set stage title: primaryStage.setTitle("Lost in Babuland - Main Menu");
        // Show the stage (if not already visible)
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        // Apply fade transition for a smooth appearance
        applyFadeTransition(mainMenuRoot);
    }

    /**
     * Switch to the Cave scene (story environment).
     */
    public static void goToCaveScene() {
        checkStageInitialized();
        StackPane caveRoot = new StackPane();
        caveRoot.getChildren().add(new Label("Cave Scene (placeholder)"));
        Scene caveScene = new Scene(caveRoot, 800, 600);
        primaryStage.setScene(caveScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(caveRoot);
    }

    /**
     * Switch to the Jungle scene (story environment).
     */
    public static void goToJungleScene() {
        checkStageInitialized();
        StackPane jungleRoot = new StackPane();
        jungleRoot.getChildren().add(new Label("Jungle Scene (placeholder)"));
        Scene jungleScene = new Scene(jungleRoot, 800, 600);
        primaryStage.setScene(jungleScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(jungleRoot);
    }

    /**
     * Switch to the River scene (story environment).
     */
    public static void goToRiverScene() {
        checkStageInitialized();
        StackPane riverRoot = new StackPane();
        riverRoot.getChildren().add(new Label("River Scene (placeholder)"));
        Scene riverScene = new Scene(riverRoot, 800, 600);
        primaryStage.setScene(riverScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(riverRoot);
    }

    /**
     * Switch to the Village scene (story environment).
     */
    public static void goToVillageScene() {
        checkStageInitialized();
        StackPane villageRoot = new StackPane();
        villageRoot.getChildren().add(new Label("Village Scene (placeholder)"));
        Scene villageScene = new Scene(villageRoot, 800, 600);
        primaryStage.setScene(villageScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(villageRoot);
    }

    /**
     * Switch to the Final House scene (story environment).
     */
    public static void goToFinalHouseScene() {
        checkStageInitialized();
        StackPane houseRoot = new StackPane();
        houseRoot.getChildren().add(new Label("Final House Scene (placeholder)"));
        Scene houseScene = new Scene(houseRoot, 800, 600);
        primaryStage.setScene(houseScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(houseRoot);
    }

    /**
     * Switch to a Mini-Game scene by number. Allows dynamic addition or removal of mini-games.
     * @param gameNumber the mini-game number (1 to TOTAL_MINI_GAMES)
     */
    public static void goToMiniGame(int gameNumber) {
        checkStageInitialized();
        // Validate game number
        if (gameNumber < 1 || gameNumber > TOTAL_MINI_GAMES) {
            throw new IllegalArgumentException("Invalid mini game number: " + gameNumber);
        }
        // Create content for the mini-game scene
        StackPane gameRoot = new StackPane();
        gameRoot.getChildren().add(new Label("Mini-Game " + gameNumber + " Scene (placeholder)"));
        Scene gameScene = new Scene(gameRoot, 800, 600);
        primaryStage.setScene(gameScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(gameRoot);
    }

    /**
     * Switch to Mini-Game 1 scene.
     */
    public static void goToMiniGame1() {
        goToMiniGame(1);
    }

    /**
     * Switch to Mini-Game 2 scene.
     */
    public static void goToMiniGame2() {
        goToMiniGame(2);
    }

    /**
     * Switch to Mini-Game 3 scene.
     */
    public static void goToMiniGame3() {
        goToMiniGame(3);
    }

    /**
     * Switch to Mini-Game 4 scene.
     */
    public static void goToMiniGame4() {
        goToMiniGame(4);
    }

    /**
     * Switch to Mini-Game 5 scene.
     */
    public static void goToMiniGame5() {
        goToMiniGame(5);
    }

    /**
     * Switch to Mini-Game 6 scene.
     */
    public static void goToMiniGame6() {
        goToMiniGame(6);
    }

    /**
     * Switch to Mini-Game 7 scene.
     */
    public static void goToMiniGame7() {
        goToMiniGame(7);
    }

    /**
     * Switch to Mini-Game 8 scene.
     */
    public static void goToMiniGame8() {
        goToMiniGame(8);
    }

    /**
     * Switch to the final Ending scene.
     * @param scorePassed true for party ending (player passed), false for rejection ending.
     */
    public static void goToEndingScene(boolean scorePassed) {
        checkStageInitialized();
        StackPane endingRoot = new StackPane();
        Label endingLabel;
        if (scorePassed) {
            endingLabel = new Label("Ending: Party! Congratulations on passing!");
        } else {
            endingLabel = new Label("Ending: Rejection. Better luck next time!");
        }
        endingRoot.getChildren().add(endingLabel);
        Scene endingScene = new Scene(endingRoot, 800, 600);
        primaryStage.setScene(endingScene);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        applyFadeTransition(endingRoot);
    }
}
