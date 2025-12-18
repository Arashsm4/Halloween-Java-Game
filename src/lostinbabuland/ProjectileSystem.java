
package lostinbabuland;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ProjectileSystem {
    public final List<Bullet> bullets = new ArrayList<>();

    public void spawnPlayerBullet(float x, float y, float dirX, float dirY) {
        float nx = MathUtil.normX(dirX, dirY);
        float ny = MathUtil.normY(dirX, dirY);
        bullets.add(new Bullet(x, y, nx * GameConfig.BULLET_SPEED, ny * GameConfig.BULLET_SPEED, true));
    }

    public void spawnEnemyBullet(float x, float y, float dirX, float dirY) {
        float nx = MathUtil.normX(dirX, dirY);
        float ny = MathUtil.normY(dirX, dirY);
        bullets.add(new Bullet(x, y, nx * GameConfig.BULLET_SPEED * 0.9f, ny * GameConfig.BULLET_SPEED * 0.9f, false));
    }

    public void update(World world, Player player, List<Enemy> enemies, float dt, StatsTracker stats) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            if (b.dead) { it.remove(); continue; }

            b.life -= dt;
            if (b.life <= 0f) { it.remove(); continue; }

            float nx = b.x + b.vx * dt;
            float ny = b.y + b.vy * dt;

            // wall collision
            int tx = world.toTileX(nx);
            int ty = world.toTileY(ny);
            if (!world.inBounds(tx, ty) || world.isSolid(tx, ty)) {
                it.remove();
                continue;
            }

            b.x = nx; b.y = ny;

            // hit tests
            if (b.fromPlayer) {
                for (Enemy e : enemies) {
                    if (!e.alive) continue;
                    float dx = e.x - b.x, dy = e.y - b.y;
                    float rr = e.r + GameConfig.BULLET_RADIUS;
                    if (dx*dx + dy*dy <= rr*rr) {
                        e.alive = false;
                        it.remove();
                        stats.playerHitEnemy();
                        stats.addScore(15);
                        break;
                    }
                }
            } else {
                float dx = player.x - b.x, dy = player.y - b.y;
                float rr = player.r + GameConfig.BULLET_RADIUS;
                if (dx*dx + dy*dy <= rr*rr) {
                    it.remove();
                    player.hp = Math.max(0, player.hp - 1);
                    stats.playerGotHit();
                }
            }
        }
    }
}