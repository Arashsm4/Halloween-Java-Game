package lostinbabuland;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public final class PlatformerSprint {
    private final HudRenderer hud = new HudRenderer();

    private World world;
    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final ProjectileSystem projectiles = new ProjectileSystem();

    private ViewMode view = ViewMode.SIDE;
    private int rotDeg = 0;

    private boolean showStats = true;
    private boolean showPanel = false;
    private boolean showHelp = false;

    private boolean hasGemKey = false;
    private boolean exitOpen = false;

    private final StatsTracker stats = new StatsTracker();

    private boolean up, down, left, right;
    private boolean shootHeld;
    private boolean shootPressedEdge;
    private boolean tabHeldEdge;
    private boolean f1HeldEdge;
    private boolean hHeldEdge;
    private boolean qHeldEdge;
    private boolean eHeldEdge;
    private boolean rHeldEdge;

    private double accumulator = 0;
    private int approxWalkable = 1;

    public void start(Stage stage) {
        Canvas canvas = new Canvas(GameConfig.WINDOW_W, GameConfig.WINDOW_H);
        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(root, GameConfig.WINDOW_W, GameConfig.WINDOW_H, Color.BLACK);

        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        setupInput(scene);

        stage.setTitle("Halloween Puzzle (TOP/SIDE) - 8bit + Analytics");
        stage.setScene(scene);
        stage.show();

        resetGame();

        GraphicsContext g = canvas.getGraphicsContext2D();

        AnimationTimer timer = new AnimationTimer() {
            long last = 0;

            @Override public void handle(long now) {
                if (last == 0) last = now;
                double rawFrameDt = (now - last) / 1e9;
                last = now;

                if (rawFrameDt < 0) rawFrameDt = 0;
                stats.renderFrame((float)rawFrameDt);

                double frameDt = Math.min(rawFrameDt, GameConfig.MAX_FRAME_DT);
                accumulator += frameDt;

                if (tabHeldEdge) { showStats = !showStats; tabHeldEdge = false; }
                if (f1HeldEdge) { showPanel = !showPanel; f1HeldEdge = false; }
                if (hHeldEdge) { showHelp = !showHelp; hHeldEdge = false; }
                if (rHeldEdge) { resetGame(); rHeldEdge = false; }

                if (view == ViewMode.SIDE) {
                    if (qHeldEdge) { rotDeg = MathUtil.wrapDeg(rotDeg - GameConfig.ROT_STEP_DEG); stats.rotated(); qHeldEdge = false; }
                    if (eHeldEdge) { rotDeg = MathUtil.wrapDeg(rotDeg + GameConfig.ROT_STEP_DEG); stats.rotated(); eHeldEdge = false; }
                } else {
                    qHeldEdge = false; eHeldEdge = false;
                }

                int steps = 0;
                while (accumulator >= GameConfig.FIXED_DT && steps < GameConfig.MAX_STEPS_PER_FRAME) {
                    step(GameConfig.FIXED_DT);
                    accumulator -= GameConfig.FIXED_DT;
                    steps++;
                }
                if (steps >= GameConfig.MAX_STEPS_PER_FRAME) {
                    // drop excess time instead of slowing forever
                    accumulator = 0;
                }

                render(g, canvas.getWidth(), canvas.getHeight());
            }
        };
        timer.start();
    }

    private void resetGame() {
        long seed = System.nanoTime();
        world = new World(seed);

        player = new Player(world.startX, world.startY);

        enemies.clear();
        projectiles.bullets.clear();

        spawnEnemies();

        hasGemKey = false;
        exitOpen = false;
        view = ViewMode.SIDE;
        rotDeg = 0;

        approxWalkable = estimateWalkableTiles();
        accumulator = 0;
    }

    private int estimateWalkableTiles() {
        int walk = 0;
        for (int y = 0; y < world.h; y++) for (int x = 0; x < world.w; x++) if (!world.isSolid(x, y)) walk++;
        return Math.max(1, walk);
    }

    private void spawnEnemies() {
        int count = GameConfig.ENEMY_COUNT_MIN + world.rng.nextInt(GameConfig.ENEMY_COUNT_MAX - GameConfig.ENEMY_COUNT_MIN + 1);
        int placed = 0;
        int safety = 0;

        while (placed < count && safety++ < 20000) {
            int tx = 2 + world.rng.nextInt(world.w-4);
            int ty = 2 + world.rng.nextInt(world.h-4);
            if (world.isSolid(tx, ty)) continue;

            float x = world.tileCenterX(tx);
            float y = world.tileCenterY(ty);

            float dx = x - player.x, dy = y - player.y;
            if (dx*dx + dy*dy < (GameConfig.TILE*10f)*(GameConfig.TILE*10f)) continue;

            boolean close = false;
            for (Enemy e : enemies) {
                float ex = e.x - x, ey = e.y - y;
                if (ex*ex + ey*ey < (GameConfig.TILE*3f)*(GameConfig.TILE*3f)) { close = true; break; }
            }
            if (close) continue;

            enemies.add(new Enemy(x, y));
            placed++;
        }
    }

    private void setupInput(Scene scene) {
        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.W || k == KeyCode.UP) up = true;
            if (k == KeyCode.S || k == KeyCode.DOWN) down = true;
            if (k == KeyCode.A || k == KeyCode.LEFT) left = true;
            if (k == KeyCode.D || k == KeyCode.RIGHT) right = true;

            if (k == KeyCode.SPACE) {
                if (!shootHeld) shootPressedEdge = true;
                shootHeld = true;
            }

            if (k == KeyCode.TAB) if (!tabHeldEdge) tabHeldEdge = true;
            if (k == KeyCode.F1) if (!f1HeldEdge) f1HeldEdge = true;
            if (k == KeyCode.H) if (!hHeldEdge) hHeldEdge = true;
            if (k == KeyCode.Q) if (!qHeldEdge) qHeldEdge = true;
            if (k == KeyCode.E) if (!eHeldEdge) eHeldEdge = true;
            if (k == KeyCode.R) if (!rHeldEdge) rHeldEdge = true;

            if (k == KeyCode.ESCAPE) Platform.exit();
        });

        scene.setOnKeyReleased(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.W || k == KeyCode.UP) up = false;
            if (k == KeyCode.S || k == KeyCode.DOWN) down = false;
            if (k == KeyCode.A || k == KeyCode.LEFT) left = false;
            if (k == KeyCode.D || k == KeyCode.RIGHT) right = false;
            if (k == KeyCode.SPACE) shootHeld = false;
        });
    }

    private void step(float dt) {
        float mx = 0f, my = 0f;
        if (left) mx -= 1f;
        if (right) mx += 1f;
        if (up) my -= 1f;
        if (down) my += 1f;

        float speedNow = MathUtil.len(mx, my) * GameConfig.PLAYER_SPEED;

        boolean inThreat = false;
        for (Enemy e : enemies) {
            if (!e.alive) continue;
            float dx = e.x - player.x, dy = e.y - player.y;
            if (dx*dx + dy*dy < GameConfig.THREAT_RADIUS * GameConfig.THREAT_RADIUS) { inThreat = true; break; }
        }

        int tileKey = world.toTileY(player.y) * world.w + world.toTileX(player.x);
        stats.simStep(dt, speedNow, inThreat, tileKey, approxWalkable);

        player.moveWithCollision(world, mx, my, dt, stats);
        player.update(world, dt);

        if (shootPressedEdge) {
            shootPressedEdge = false;
            if (player.fireCooldown <= 0f && player.hp > 0) {
                player.fireCooldown = GameConfig.PLAYER_FIRE_COOLDOWN;
                float ax = player.aimX, ay = player.aimY;
                if (Math.abs(ax) < 1e-4f && Math.abs(ay) < 1e-4f) { ax = 1f; ay = 0f; }
                projectiles.spawnPlayerBullet(player.x + ax*(player.r+7f), player.y + ay*(player.r+7f), ax, ay);
                stats.playerShot();
            }
        }

        for (Pickup p : world.pickups) {
            if (p.collected) continue;
            float dx = p.x - player.x, dy = p.y - player.y;
            float rr = player.r + 10f;
            if (dx*dx + dy*dy <= rr*rr) {
                p.collected = true;
                if (p.type == PickupType.CANDY) stats.collectedCandy();
                if (p.type == PickupType.GEM) { stats.collectedGem(); hasGemKey = true; exitOpen = true; }
            }
        }

        for (Hole h : world.holes) {
            float dx = h.x - player.x, dy = h.y - player.y;
            if (dx*dx + dy*dy <= (GameConfig.TILE*0.35f)*(GameConfig.TILE*0.35f)) {
                view = (view == ViewMode.TOP) ? ViewMode.SIDE : ViewMode.TOP;
                stats.switchedView();
                player.x += player.aimX * 18f;
                player.y += player.aimY * 18f;
                break;
            }
        }

        for (Enemy e : enemies) e.update(world, dt);
        for (Enemy e : enemies) e.think(world, player, dt, enemies, projectiles, stats);

        projectiles.update(world, player, enemies, dt, stats);

        float ex = world.exitX - player.x, ey = world.exitY - player.y;
        if (exitOpen && ex*ex + ey*ey <= (GameConfig.TILE*0.45f)*(GameConfig.TILE*0.45f)) resetGame();
        if (player.hp <= 0) resetGame();
    }

    private void render(GraphicsContext g, double w, double h) {
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, h);

        double viewW = w;
        double viewH = h - GameConfig.HUD_H;

        double vx0 = 0;
        double vy0 = GameConfig.HUD_H;

        g.setFill(Color.rgb(255,255,255,0.06));
        for (int i = 0; i < 70; i++) {
            double sx = (i * 97) % viewW;
            double sy = (i * 53) % viewH;
            g.fillRect(sx, vy0 + sy, 2, 2);
        }

        Camera cam = new Camera();
        Camera.Projector proj = (view == ViewMode.TOP) ? Projection.top(world) : Projection.sideIso(world, rotDeg);
        cam.fitToView(proj, (float)viewW, (float)viewH, world);

        if (view == ViewMode.SIDE) {
            g.setStroke(Color.rgb(255,255,255,0.05));
            for (int ty = 0; ty <= world.h; ty++) {
                float wy = ty * GameConfig.TILE;
                float[] a = proj.project(0, wy);
                float[] b = proj.project(world.w * GameConfig.TILE, wy);
                g.strokeLine(vx0 + cam.sx(a[0]), vy0 + cam.sy(a[1]), vx0 + cam.sx(b[0]), vy0 + cam.sy(b[1]));
            }
            for (int tx = 0; tx <= world.w; tx++) {
                float wx = tx * GameConfig.TILE;
                float[] a = proj.project(wx, 0);
                float[] b = proj.project(wx, world.h * GameConfig.TILE);
                g.strokeLine(vx0 + cam.sx(a[0]), vy0 + cam.sy(a[1]), vx0 + cam.sx(b[0]), vy0 + cam.sy(b[1]));
            }
        }

        for (int y = 0; y < world.h; y++) {
            for (int x = 0; x < world.w; x++) {
                if (!world.isSolid(x, y)) continue;

                float wx = x * GameConfig.TILE;
                float wy = y * GameConfig.TILE;
                float[] p = proj.project(wx + GameConfig.TILE*0.5f, wy + GameConfig.TILE*0.5f);

                double sx = vx0 + cam.sx(p[0]);
                double sy = vy0 + cam.sy(p[1]);
                double sz = cam.scale * 0.95;

                if (view == ViewMode.TOP) {
                    g.setFill(Color.rgb(130,130,140,0.95));
                    g.fillRect(sx - GameConfig.TILE*0.5*sz, sy - GameConfig.TILE*0.5*sz, GameConfig.TILE*sz, GameConfig.TILE*sz);
                } else {
                    double bw = GameConfig.TILE * 0.55 * sz;
                    double bh = GameConfig.TILE * 0.38 * sz;
                    double hgt = GameConfig.TILE * 0.95 * sz;

                    g.setFill(Color.rgb(120,120,130,0.85));
                    g.fillRoundRect(sx - bw/2, sy - bh/2 - hgt, bw, bh, 6, 6);

                    g.setFill(Color.rgb(150,150,160,0.85));
                    g.fillRoundRect(sx - bw/2, sy - bh/2, bw, bh, 6, 6);
                }
            }
        }

        for (Hole hole : world.holes) {
            float[] p = proj.project(hole.x, hole.y);
            double sx = vx0 + cam.sx(p[0]);
            double sy = vy0 + cam.sy(p[1]);
            double rr = 10 * cam.scale;

            g.setStroke(Color.rgb(160,90,255,0.55));
            g.strokeOval(sx - rr, sy - rr, rr*2, rr*2);
            g.setStroke(Color.rgb(160,90,255,0.25));
            g.strokeOval(sx - rr*1.5, sy - rr*1.5, rr*3, rr*3);
        }

        for (Pickup pck : world.pickups) {
            if (pck.collected) continue;
            float[] p = proj.project(pck.x, pck.y);
            double sx = vx0 + cam.sx(p[0]);
            double sy = vy0 + cam.sy(p[1]);
            double rr = (pck.type == PickupType.CANDY ? 6 : 7) * cam.scale;

            if (pck.type == PickupType.CANDY) g.setFill(Color.rgb(255,160,40,0.95));
            else g.setFill(Color.rgb(120,210,255,0.95));
            g.fillOval(sx-rr, sy-rr, rr*2, rr*2);
        }

        float[] exitP = proj.project(world.exitX, world.exitY);
        double ex = vx0 + cam.sx(exitP[0]);
        double ey = vy0 + cam.sy(exitP[1]);
        double es = 18 * cam.scale;
        g.setFill(exitOpen ? Color.rgb(80,255,120,0.95) : Color.rgb(80,255,120,0.25));
        g.fillRect(ex - es/2, ey - es/2, es, es);

        for (Enemy e : enemies) {
            if (!e.alive) continue;
            float[] p = proj.project(e.x, e.y);
            double sx = vx0 + cam.sx(p[0]);
            double sy = vy0 + cam.sy(p[1]);
            double rr = 12 * cam.scale;

            g.setFill(Color.rgb(255,80,105,0.85));
            g.fillOval(sx-rr, sy-rr, rr*2, rr*2);

            g.setFill(Color.rgb(255,255,255,0.70));
            g.fillOval(sx-rr*0.35, sy-rr*0.15, rr*0.35, rr*0.35);
            g.fillOval(sx+rr*0.05, sy-rr*0.15, rr*0.35, rr*0.35);
        }

        for (Bullet b : projectiles.bullets) {
            float[] p = proj.project(b.x, b.y);
            double sx = vx0 + cam.sx(p[0]);
            double sy = vy0 + cam.sy(p[1]);
            double rr = 3.6 * cam.scale;
            g.setFill(b.fromPlayer ? Color.rgb(240,240,240,0.95) : Color.rgb(255,120,120,0.95));
            g.fillOval(sx-rr, sy-rr, rr*2, rr*2);
        }

        float[] pp = proj.project(player.x, player.y);
        double px = vx0 + cam.sx(pp[0]);
        double py = vy0 + cam.sy(pp[1]);
        double pr = 13 * cam.scale;

        g.setFill(Color.rgb(255,165,60,0.95));
        g.fillOval(px-pr, py-pr, pr*2, pr*2);
        g.setFill(Color.rgb(30,30,30,0.85));
        g.fillOval(px-pr*0.35, py-pr*0.15, pr*0.25, pr*0.25);
        g.fillOval(px+pr*0.10, py-pr*0.15, pr*0.25, pr*0.25);

        if (showStats) hud.draw(g, w, h, view, rotDeg, player, hasGemKey, exitOpen, stats, approxWalkable);

        if (showHelp || showPanel) {
            g.setFill(Color.rgb(0,0,0,0.65));
            g.fillRect(18, GameConfig.HUD_H + 18, 520, 140);
            g.setFill(Color.rgb(240,240,240,0.9));
            g.fillText(showHelp
                            ? "Help: step on purple HOLE to switch TOP/SIDE. Q/E rotates SIDE view. Collect GEM to open EXIT."
                            : "Panel: placeholder (stable).",
                    30, GameConfig.HUD_H + 54);
        }
    }
}