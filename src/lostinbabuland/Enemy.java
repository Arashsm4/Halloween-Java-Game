// src/lostinbabuland/Enemy.java
package lostinbabuland;

import java.util.List;
import javafx.geometry.Rectangle2D;

public class Enemy extends Character {
    private final double minX;
    private final double maxX;
    private final double speed;

    public Enemy(double x, double y, double w, double h, double minX, double maxX, double speed) {
        super(x, y, w, h);
        this.minX = minX;
        this.maxX = maxX;
        this.speed = speed;
        this.vx = speed;
    }

    public void update(double dt, List<Rectangle2D> walls) {
        if (!alive) return;

        double nextX = x + vx * dt;
        Rectangle2D next = new Rectangle2D(nextX, y, w, h);

        boolean hitWall = false;
        for (Rectangle2D wall : walls) {
            if (wall.intersects(next)) { hitWall = true; break; }
        }

        if (nextX < minX || nextX > maxX || hitWall) {
            vx = -vx;
        } else {
            x = nextX;
        }
    }
}
