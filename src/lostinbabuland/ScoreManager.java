// ScoreManager.java - manages and updates the player's score
package lostinbabuland;

public class ScoreManager {
    private static int totalScore = 0;

    // Add points to the current total score
    public static void addPoints(int points) {
        totalScore += points;
        // Update PlayerProfile score as well (if profile exists)
        if (PlayerProfile.currentProfile != null) {
            PlayerProfile.currentProfile.setScore(totalScore);
        }
    }

    // Get the current total score
    public static int getScore() {
        return totalScore;
    }

    // Reset score to zero
    public static void reset() {
        totalScore = 0;
        if (PlayerProfile.currentProfile != null) {
            PlayerProfile.currentProfile.setScore(0);
        }
    }
}
