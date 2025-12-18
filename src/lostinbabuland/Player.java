
package lostinbabuland;

public final class Player extends Entity {
    public int hp = GameConfig.INITIAL_HP;

    public float aimX = 1f, aimY = 0f; // last movement direction
    public float fireCooldown = 0f;

    public Player(float x, float y) {
        super(x, y, GameConfig.PLAYER_RADIUS);
    }

    @Override
    public void update(World world, float dt) {
        fireCooldown = Math.max(0f, fireCooldown - dt);
    }

    public void moveWithCollision(World world, float dx, float dy, float dt, StatsTracker stats) {
        // normalize to avoid diagonal speed boosts
        float nx = MathUtil.normX(dx, dy);
        float ny = MathUtil.normY(dx, dy);

        if (Math.abs(nx) > 1e-4f || Math.abs(ny) > 1e-4f) {
            aimX = nx; aimY = ny;
        }

        float speed = GameConfig.PLAYER_SPEED;
        float stepX = nx * speed * dt;
        float stepY = ny * speed * dt;

        float oldX = x, oldY = y;

        // X axis
        x += stepX;
        if (collides(world)) {
            // resolve against nearby tiles
            resolveX(world, stepX);
            stats.bump();
        }

        // Y axis
        y += stepY;
        if (collides(world)) {
            resolveY(world, stepY);
            stats.bump();
        }

        stats.addDistance(MathUtil.len(x-oldX, y-oldY));
    }

    private boolean collides(World world) {
        for (int[] t : Collision.nearbyTiles(world, x, y, r)) {
            if (Collision.circleIntersectsTile(world, x, y, r, t[0], t[1])) return true;
        }
        return false;
    }

    private void resolveX(World world, float stepX) {
        for (int[] t : Collision.nearbyTiles(world, x, y, r)) {
            int tx = t[0], ty = t[1];
            if (!world.isSolid(tx, ty)) continue;
            float left = tx * GameConfig.TILE;
            float right = left + GameConfig.TILE;
            float top = ty * GameConfig.TILE;
            float bottom = top + GameConfig.TILE;

            // circle vs rect: push on X by clamping Y and checking overlap
            float closestY = MathUtil.clamp(y, top, bottom);
            float dy = y - closestY;
            if (dy*dy > r*r) continue;

            if (stepX > 0) {
                if (x + r > left && x < left) x = left - r;
            } else if (stepX < 0) {
                if (x - r < right && x > right) x = right + r;
            }
        }
    }

    private void resolveY(World world, float stepY) {
        for (int[] t : Collision.nearbyTiles(world, x, y, r)) {
            int tx = t[0], ty = t[1];
            if (!world.isSolid(tx, ty)) continue;
            float left = tx * GameConfig.TILE;
            float right = left + GameConfig.TILE;
            float top = ty * GameConfig.TILE;
            float bottom = top + GameConfig.TILE;

            float closestX = MathUtil.clamp(x, left, right);
            float dx = x - closestX;
            if (dx*dx > r*r) continue;

            if (stepY > 0) {
                if (y + r > top && y < top) y = top - r;
            } else if (stepY < 0) {
                if (y - r < bottom && y > bottom) y = bottom + r;
            }
        }
    }
}