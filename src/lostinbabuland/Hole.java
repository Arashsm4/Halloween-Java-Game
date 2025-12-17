// src/lostinbabuland/Hole.java
package lostinbabuland;

import javafx.geometry.Rectangle2D;

public class Hole {
    private final double x, y, size;

    public Hole(double x, double y, double size) {
        this.x = x; this.y = y; this.size = size;
    }

    public Rectangle2D bounds() {
        return new Rectangle2D(x, y, size, size);
    }

    public double x() { return x; }
    public double y() { return y; }
    public double size() { return size; }
}
