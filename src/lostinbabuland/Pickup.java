
package lostinbabuland;

public final class Pickup {
    public final PickupType type;
    public float x, y;
    public boolean collected;

    public Pickup(PickupType type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
}