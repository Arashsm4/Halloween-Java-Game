
package lostinbabuland;

public enum Tile {
    FLOOR(false),
    WALL(true);

    public final boolean solid;

    Tile(boolean solid) {
        this.solid = solid;
    }
}
