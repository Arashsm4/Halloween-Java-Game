
package lostinbabuland;

public class PlayerProfile {
    private String name = "Player";
    public String getName() { return name; }
    public void setName(String name) { if (name != null && !name.isBlank()) this.name = name; }
}
