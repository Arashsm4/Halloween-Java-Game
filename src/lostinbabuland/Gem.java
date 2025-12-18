
package lostinbabuland;

import javafx.geometry.Rectangle2D;

public class Gem {
    private final double x, y;
    private final double size;
    private boolean collected;

    public Gem(double x, double y, double size) {
        this.x = x; this.y = y; this.size = size;
    }

    public Rectangle2D bounds() {
        return new Rectangle2D(x, y, size, size);
    }

    public boolean isCollected() { return collected; }
    public void collect() { collected = true; }

    public double x() { return x; }
    public double y() { return y; }
    public double size() { return size; }
}

//yum yum gems to eat