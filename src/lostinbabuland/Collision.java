
package lostinbabuland;

import java.util.ArrayList;
import java.util.List;

//well some parts of this I still don't get but thanks to internet it helped me figure out

public final class Collision {
    private Collision(){}

    public static List<int[]> nearbyTiles(World world, float x, float y, float r) {
        int minTx = (int)Math.floor((x - r) / GameConfig.TILE);
        int maxTx = (int)Math.floor((x + r) / GameConfig.TILE);
        int minTy = (int)Math.floor((y - r) / GameConfig.TILE);
        int maxTy = (int)Math.floor((y + r) / GameConfig.TILE);

        minTx = Math.max(0, minTx); minTy = Math.max(0, minTy);
        maxTx = Math.min(world.w - 1, maxTx); maxTy = Math.min(world.h - 1, maxTy);

        List<int[]> out = new ArrayList<>();
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) out.add(new int[]{tx, ty});
        }
        return out;
    }

    public static boolean aabbOverlap(float ax1, float ay1, float ax2, float ay2,
                                      float bx1, float by1, float bx2, float by2) {
        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }

    public static boolean circleIntersectsTile(World world, float cx, float cy, float r, int tx, int ty) {
        if (!world.isSolid(tx, ty)) return false;

        float x1 = tx * GameConfig.TILE;
        float y1 = ty * GameConfig.TILE;
        float x2 = x1 + GameConfig.TILE;
        float y2 = y1 + GameConfig.TILE;

        float closestX = MathUtil.clamp(cx, x1, x2);
        float closestY = MathUtil.clamp(cy, y1, y2);
        float dx = cx - closestX;
        float dy = cy - closestY;
        return dx*dx + dy*dy <= r*r;
    }

    public static boolean lineOfSight(World world, float x0, float y0, float x1, float y1) {
        // grid DDA raycast
        float dx = x1 - x0, dy = y1 - y0;
        float dist = MathUtil.len(dx, dy);
        if (dist < 1e-4f) return true;

        int steps = Math.max(8, (int)(dist / 10f));
        float sx = dx / steps;
        float sy = dy / steps;

        float x = x0, y = y0;
        for (int i = 0; i <= steps; i++) {
            int tx = (int)(x / GameConfig.TILE);
            int ty = (int)(y / GameConfig.TILE);
            if (world.inBounds(tx, ty) && world.isSolid(tx, ty)) return false;
            x += sx; y += sy;
        }
        return true;
    }
}