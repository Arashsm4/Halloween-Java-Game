// PlayerProfile.java - stores player information and score
package lostinbabuland;

public class PlayerProfile {
    // Current player's profile (for global access)
    public static PlayerProfile currentProfile;
    private String name;
    private String email;
    private int score;

    public PlayerProfile(String name, String email) {
        this.name = name;
        this.email = email;
        this.score = 0;
    }

    // Getters
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public int getScore() {
        return score;
    }

    // Update score
    public void setScore(int score) {
        this.score = score;
    }
}
