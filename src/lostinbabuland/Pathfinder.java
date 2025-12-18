
package lostinbabuland;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Pathfinder {
    private Pathfinder(){}

    public static List<int[]> bfsNextSteps(World world, int sx, int sy, int gx, int gy, int maxNodes) {
        if (!world.inBounds(sx, sy) || !world.inBounds(gx, gy)) return List.of();
        if (world.isSolid(gx, gy) || world.isSolid(sx, sy)) return List.of();
        if (sx == gx && sy == gy) return List.of();

        int w = world.w, h = world.h;
        int[][] prev = new int[w*h][2];
        boolean[] vis = new boolean[w*h];
        for (int i = 0; i < prev.length; i++) { prev[i][0] = -1; prev[i][1] = -1; }

        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        vis[sy*w + sx] = true;

        int visited = 0;
        int[] dirs = {1,0,-1,0, 0,1,0,-1};

        while (!q.isEmpty() && visited < maxNodes) {
            int[] n = q.removeFirst();
            int x = n[0], y = n[1];
            visited++;

            if (x == gx && y == gy) break;

            for (int i = 0; i < 8; i += 2) {
                int nx = x + dirs[i];
                int ny = y + dirs[i+1];
                if (!world.inBounds(nx, ny) || world.isSolid(nx, ny)) continue;
                int idx = ny*w + nx;
                if (vis[idx]) continue;
                vis[idx] = true;
                prev[idx][0] = x;
                prev[idx][1] = y;
                q.addLast(new int[]{nx, ny});
            }
        }

        if (!vis[gy*w + gx]) return List.of();

        // reconstruct path from goal back to start
        List<int[]> rev = new ArrayList<>();
        int cx = gx, cy = gy;
        while (!(cx == sx && cy == sy)) {
            rev.add(new int[]{cx, cy});
            int[] p = prev[cy*w + cx];
            cx = p[0]; cy = p[1];
            if (cx < 0) break;
        }

        // reverse into forward list
        List<int[]> fwd = new ArrayList<>();
        for (int i = rev.size() - 1; i >= 0; i--) fwd.add(rev.get(i));
        return fwd;
    }
}