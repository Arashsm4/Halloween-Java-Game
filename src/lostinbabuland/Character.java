
package lostinbabuland;

import javafx.geometry.Rectangle2D;

public abstract class Character {
    protected double x, y;
    protected double w, h;

    protected double vx, vy;
    protected boolean alive = true;

    protected Character(double x, double y, double w, double h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public Rectangle2D bounds() {
        return new Rectangle2D(x, y, w, h);
    }

    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }

    public double cx() { return x + w * 0.5; }
    public double cy() { return y + h * 0.5; }
}
