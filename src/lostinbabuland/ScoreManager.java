
package lostinbabuland;

public class ScoreManager {
    private int score;

    public void reset() { score = 0; }
    public void add(int points) { score += points; }
    public int get() { return score; }
}
