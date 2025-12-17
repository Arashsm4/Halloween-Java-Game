// src/lostinbabuland/Player.java
package lostinbabuland;

public class Player extends Character {
    private int hp = 3;
    private boolean hasGem;

    public final double spawnX;
    public final double spawnY;

    public Player(double x, double y, double w, double h) {
        super(x, y, w, h);
        this.spawnX = x;
        this.spawnY = y;
    }

    public int hp() { return hp; }
    public boolean hasGem() { return hasGem; }
    public void giveGem() { hasGem = true; }

    public void takeHit() {
        hp = Math.max(0, hp - 1);
    }

    public void respawn() {
        x = spawnX;
        y = spawnY;
        vx = 0;
        vy = 0;
    }

    public boolean isDead() { return hp <= 0; }
}
