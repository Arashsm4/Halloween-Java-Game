
package lostinbabuland;

public abstract class Entity {
    public float x, y;
    public float vx, vy;
    public final float r;

    protected Entity(float x, float y, float r) {
        this.x = x; this.y = y; this.r = r;
    }

    public abstract void update(World world, float dt);
}

//noooice