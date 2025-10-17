// LeaderboardManager.java - handles leaderboard data (e.g. save to backend)
package lostinbabuland;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeadbordManager {
    // Score entry structure
    public static class ScoreEntry {
        public String name;
        public int score;
        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    // In-memory list to simulate persistent storage
    private static List<ScoreEntry> highScores = new ArrayList<>();

    // Submit a new score (could connect to external backend in real use)
    public static void submitScore(String name, int score) {
        // Only store public name and score (email not saved for privacy)
        highScores.add(new ScoreEntry(name, score));
        // Sort scores descending by score
        Collections.sort(highScores, (a, b) -> b.score - a.score);
        // Keep top 10 scores
        if (highScores.size() > 10) {
            highScores = new ArrayList<>(highScores.subList(0, 10));
        }
    }

    // Retrieve top scores list
    public static List<ScoreEntry> getTopScores() {
        return new ArrayList<>(highScores);
    }
}
