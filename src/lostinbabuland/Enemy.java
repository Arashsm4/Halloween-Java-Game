
package lostinbabuland;

import java.util.List;

public final class Enemy extends Entity {
    public boolean alive = true;

    private float pathTimer = 0f;
    private float shootTimer = 0f;

    private int targetTx, targetTy;
    private List<int[]> path = List.of();
    private int pathIndex = 0;

    public Enemy(float x, float y) {
        super(x, y, GameConfig.ENEMY_RADIUS);
        // random initial target
        this.targetTx = 0;
        this.targetTy = 0;
        this.pathTimer = (float)Math.random() * 0.4f;
        this.shootTimer = (float)Math.random() * 0.6f;
    }

    @Override
    public void update(World world, float dt) {
        pathTimer = Math.max(0f, pathTimer - dt);
        shootTimer = Math.max(0f, shootTimer - dt);
    }

    public void think(World world, Player player, float dt, java.util.List<Enemy> allEnemies, ProjectileSystem projectiles, StatsTracker stats) {
        if (!alive) return;

        float dxp = player.x - x, dyp = player.y - y;
        float dist = MathUtil.len(dxp, dyp);

        boolean chasing = dist < 330f;

        // plan path periodically
        if (pathTimer <= 0f) {
            pathTimer = chasing ? 0.35f : 0.7f;

            int sx = world.toTileX(x), sy = world.toTileY(y);

            if (chasing) {
                int gx = world.toTileX(player.x), gy = world.toTileY(player.y);
                path = Pathfinder.bfsNextSteps(world, sx, sy, gx, gy, 16000);
                pathIndex = 0;
            } else {
                // pick random reachable floor target
                for (int tries = 0; tries < 40; tries++) {
                    int tx = 2 + world.rng.nextInt(world.w-4);
                    int ty = 2 + world.rng.nextInt(world.h-4);
                    if (world.isSolid(tx, ty)) continue;
                    if (Math.abs(tx - sx) + Math.abs(ty - sy) < 8) continue;
                    targetTx = tx; targetTy = ty;
                    path = Pathfinder.bfsNextSteps(world, sx, sy, targetTx, targetTy, 16000);
                    pathIndex = 0;
                    if (!path.isEmpty()) break;
                }
            }
        }

        // desired direction
        float ddx = 0f, ddy = 0f;
        if (!path.isEmpty() && pathIndex < path.size()) {
            int[] node = path.get(pathIndex);
            float tx = world.tileCenterX(node[0]);
            float ty = world.tileCenterY(node[1]);
            float dd = MathUtil.len(tx - x, ty - y);
            if (dd < 8f) pathIndex++;
            ddx = tx - x;
            ddy = ty - y;
        } else if (chasing) {
            ddx = dxp; ddy = dyp;
        }

        float ndx = MathUtil.normX(ddx, ddy);
        float ndy = MathUtil.normY(ddx, ddy);

        // separation
        float sepX = 0f, sepY = 0f;
        for (Enemy e : allEnemies) {
            if (e == this || !e.alive) continue;
            float ex = x - e.x, ey = y - e.y;
            float d = MathUtil.len(ex, ey);
            if (d < 1e-3f) continue;
            float range = 30f;
            if (d < range) {
                float s = (range - d) / range;
                sepX += (ex / d) * s;
                sepY += (ey / d) * s;
            }
        }

        float dirX = ndx + sepX * 0.9f;
        float dirY = ndy + sepY * 0.9f;

        dirX = MathUtil.normX(dirX, dirY);
        dirY = MathUtil.normY(dirX, dirY);

        float stepX = dirX * GameConfig.ENEMY_SPEED * dt;
        float stepY = dirY * GameConfig.ENEMY_SPEED * dt;

        // collision move
        float oldX = x, oldY = y;

        x += stepX;
        if (collides(world)) resolveAxis(world, stepX, true);

        y += stepY;
        if (collides(world)) resolveAxis(world, stepY, false);

        // shoot if line-of-sight and in range
        if (chasing && dist < 290f && shootTimer <= 0f && Collision.lineOfSight(world, x, y, player.x, player.y)) {
            shootTimer = GameConfig.ENEMY_FIRE_COOLDOWN;
            float bx = MathUtil.normX(dxp, dyp);
            float by = MathUtil.normY(dxp, dyp);
            projectiles.spawnEnemyBullet(x + bx*(r+6f), y + by*(r+6f), bx, by);
            stats.enemyShot();
        }

        stats.enemyMoveDistance(MathUtil.len(x-oldX, y-oldY));
    }

    private boolean collides(World world) {
        for (int[] t : Collision.nearbyTiles(world, x, y, r)) {
            if (Collision.circleIntersectsTile(world, x, y, r, t[0], t[1])) return true;
        }
        return false;
    }

    private void resolveAxis(World world, float step, boolean axisX) {
        for (int[] t : Collision.nearbyTiles(world, x, y, r)) {
            int tx = t[0], ty = t[1];
            if (!world.isSolid(tx, ty)) continue;
            float left = tx * GameConfig.TILE;
            float right = left + GameConfig.TILE;
            float top = ty * GameConfig.TILE;
            float bottom = top + GameConfig.TILE;

            if (axisX) {
                float closestY = MathUtil.clamp(y, top, bottom);
                float dy = y - closestY;
                if (dy*dy > r*r) continue;

                if (step > 0) {
                    if (x + r > left && x < left) x = left - r;
                } else if (step < 0) {
                    if (x - r < right && x > right) x = right + r;
                }
            } else {
                float closestX = MathUtil.clamp(x, left, right);
                float dx = x - closestX;
                if (dx*dx > r*r) continue;

                if (step > 0) {
                    if (y + r > top && y < top) y = top - r;
                } else if (step < 0) {
                    if (y - r < bottom && y > bottom) y = bottom + r;
                }
            }
        }
    }
}