
package lostinbabuland;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class World {
    public final int w, h;
    private final Tile[][] tiles;
    public final Random rng;

    public float startX, startY;
    public float exitX, exitY;

    public final List<Pickup> pickups = new ArrayList<>();
    public final List<Hole> holes = new ArrayList<>();

    public World(long seed) {
        this.w = GameConfig.WORLD_W;
        this.h = GameConfig.WORLD_H;
        this.tiles = new Tile[w][h];
        this.rng = new Random(seed);
        generate();
    }

    public boolean inBounds(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < w && ty < h;
    }

    public Tile tile(int tx, int ty) {
        if (!inBounds(tx, ty)) return Tile.WALL;
        return tiles[tx][ty];
    }

    public boolean isSolid(int tx, int ty) {
        return tile(tx, ty).solid;
    }

    public float tileCenterX(int tx) { return tx * GameConfig.TILE + GameConfig.TILE * 0.5f; }
    public float tileCenterY(int ty) { return ty * GameConfig.TILE + GameConfig.TILE * 0.5f; }

    public int toTileX(float x) { return (int)(x / GameConfig.TILE); }
    public int toTileY(float y) { return (int)(y / GameConfig.TILE); }

    public boolean isWalkablePx(float x, float y) {
        int tx = toTileX(x), ty = toTileY(y);
        return inBounds(tx, ty) && !isSolid(tx, ty);
    }

    private void generate() {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) tiles[x][y] = Tile.FLOOR;

        // border walls
        for (int x = 0; x < w; x++) { tiles[x][0] = Tile.WALL; tiles[x][h-1] = Tile.WALL; }
        for (int y = 0; y < h; y++) { tiles[0][y] = Tile.WALL; tiles[w-1][y] = Tile.WALL; }

        // random wall clusters
        int clusters = 58;
        for (int i = 0; i < clusters; i++) {
            int cx = 2 + rng.nextInt(w-4);
            int cy = 2 + rng.nextInt(h-4);
            int rw = 1 + rng.nextInt(4);
            int rh = 1 + rng.nextInt(4);
            for (int y = cy; y < cy+rh && y < h-1; y++) {
                for (int x = cx; x < cx+rw && x < w-1; x++) {
                    if (rng.nextFloat() < 0.85f) tiles[x][y] = Tile.WALL;
                }
            }
        }

        // carve a few corridors so it doesn't lock up
        for (int i = 0; i < 6; i++) {
            int y = 3 + rng.nextInt(h-6);
            for (int x = 2; x < w-2; x++) if (rng.nextFloat() < 0.55f) tiles[x][y] = Tile.FLOOR;
        }
        for (int i = 0; i < 4; i++) {
            int x = 3 + rng.nextInt(w-6);
            for (int y = 2; y < h-2; y++) if (rng.nextFloat() < 0.55f) tiles[x][y] = Tile.FLOOR;
        }

        // choose start/exit far apart (retry a few times)
        int sx=2, sy=2, ex=w-3, ey=h-3;
        for (int tries = 0; tries < 200; tries++) {
            int ax = 2 + rng.nextInt(w-4);
            int ay = 2 + rng.nextInt(h-4);
            int bx = 2 + rng.nextInt(w-4);
            int by = 2 + rng.nextInt(h-4);
            if (isSolid(ax, ay) || isSolid(bx, by)) continue;
            int man = Math.abs(ax-bx) + Math.abs(ay-by);
            if (man < (w+h)/2) continue;
            // ensure path exists
            var path = Pathfinder.bfsNextSteps(this, ax, ay, bx, by, 20000);
            if (!path.isEmpty()) { sx=ax; sy=ay; ex=bx; ey=by; break; }
        }

        startX = tileCenterX(sx);
        startY = tileCenterY(sy);
        exitX = tileCenterX(ex);
        exitY = tileCenterY(ey);

        pickups.clear();
        holes.clear();

        placePickups(PickupType.CANDY, GameConfig.CANDY_COUNT, sx, sy, ex, ey);
        placePickups(PickupType.GEM, GameConfig.GEM_COUNT, sx, sy, ex, ey);
        placeHoles(GameConfig.HOLE_COUNT, sx, sy, ex, ey);
    }

    //aaaaaaa so much pain
    private void placePickups(PickupType type, int count, int sx, int sy, int ex, int ey) {
        int placed = 0;
        int safety = 0;
        while (placed < count && safety++ < 20000) {
            int tx = 2 + rng.nextInt(w-4);
            int ty = 2 + rng.nextInt(h-4);
            if (isSolid(tx, ty)) continue;
            if (Math.abs(tx-sx)+Math.abs(ty-sy) < 6) continue;
            if (Math.abs(tx-ex)+Math.abs(ty-ey) < 6) continue;

            float px = tileCenterX(tx), py = tileCenterY(ty);
            boolean tooClose = false;
            for (Pickup p : pickups) {
                float dx = p.x - px, dy = p.y - py;
                if (dx*dx + dy*dy < (GameConfig.TILE*0.7f)*(GameConfig.TILE*0.7f)) { tooClose = true; break; }
            }
            if (tooClose) continue;

            pickups.add(new Pickup(type, px, py));
            placed++;
        }
    }

    private void placeHoles(int count, int sx, int sy, int ex, int ey) {
        int placed = 0;
        int safety = 0;
        while (placed < count && safety++ < 20000) {
            int tx = 2 + rng.nextInt(w-4);
            int ty = 2 + rng.nextInt(h-4);
            if (isSolid(tx, ty)) continue;
            if (Math.abs(tx-sx)+Math.abs(ty-sy) < 8) continue;
            if (Math.abs(tx-ex)+Math.abs(ty-ey) < 8) continue;

            float px = tileCenterX(tx), py = tileCenterY(ty);
            boolean tooClose = false;
            for (Hole h : holes) {
                float dx = h.x - px, dy = h.y - py;
                if (dx*dx + dy*dy < (GameConfig.TILE*2f)*(GameConfig.TILE*2f)) { tooClose = true; break; }
            }
            if (tooClose) continue;

            holes.add(new Hole(px, py));
            placed++;
        }
    }
}