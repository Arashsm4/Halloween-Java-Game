
package lostinbabuland;

//pew pew pew dead

public final class Bullet {
    public float x, y;
    public float vx, vy;
    public float life = 2.2f;
    public final boolean fromPlayer;
    public boolean dead;

    public Bullet(float x, float y, float vx, float vy, boolean fromPlayer) {
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
        this.fromPlayer = fromPlayer;
    }
}