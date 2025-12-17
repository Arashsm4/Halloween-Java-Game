// src/lostinbabuland/PlatformerSprint.java
package lostinbabuland;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class PlatformerSprint implements MiniGame {

    private enum ViewMode { TOP, SIDE }
    private enum GameState { PLAYING, WON }

    private static final int TILE = 36;
    private static final int HUD_H = 120;

    private static final double PLAYER_SIZE = 24;
    private static final double ENEMY_SIZE  = 24;

    private static final double PLAYER_SPEED = 190;
    private static final double ENEMY_PATROL_SPEED = 75;
    private static final double ENEMY_CHASE_SPEED  = 120;

    private static final double BULLET_SPEED_PLAYER = 420;
    private static final double BULLET_SPEED_ENEMY  = 330;

    private static final double SHOOT_COOLDOWN_PLAYER = 0.16;
    private static final double SHOOT_COOLDOWN_ENEMY  = 0.85;

    private static final double CHASE_RANGE = TILE * 8.0;
    private static final double SHOOT_RANGE = TILE * 7.0;

    private static final Color BG_TOP  = Color.rgb(12, 10, 16);
    private static final Color BG_SIDE = Color.rgb(8, 8, 12);

    private static final Color WALL_DARK  = Color.rgb(70, 70, 76);
    private static final Color WALL_LIGHT = Color.rgb(105, 105, 112);

    private Stage stage;
    private Scene scene;
    private Pane root;
    private Canvas canvas;
    private GraphicsContext g;

    private final Set<KeyCode> keysDown = new HashSet<>();

    private final AnalyticsManager analytics = new AnalyticsManager();
    private final ScoreManager score = new ScoreManager();

    private ViewMode viewMode = ViewMode.TOP;
    private GameState state = GameState.PLAYING;

    private Player player;
    private Enemy enemy;
    private Gem gem;

    private static final class Candy {
        final Rectangle2D b;
        boolean taken;
        Candy(double x, double y) { this.b = new Rectangle2D(x, y, 14, 14); }
    }
    private final List<Candy> candies = new ArrayList<>();

    private final List<Hole> holes = new ArrayList<>();
    private final List<Rectangle2D> walls = new ArrayList<>();
    private Rectangle2D exitDoor;
    private boolean exitOpen = false;

    private int worldW;
    private int worldH;

    private long lastFrameNs = 0;
    private double memSampleAcc = 0;
    private double fpsAcc = 0;
    private int fpsFrames = 0;
    private double fps = 0;

    private long holeCooldownUntilMs = 0;
    private boolean showStats = true;
    private boolean showAnalyticsPanel = false;

    private double gemPulse = 0;
    private double enemyBob = 0;

    // side view camera "turn"
    private double sideYawDeg = 0; // rotate projection slightly
    private static final double SIDE_YAW_MIN = -18;
    private static final double SIDE_YAW_MAX = 18;

    // aiming/shooting
    private double aimDx = 1, aimDy = 0;
    private double shootCdPlayer = 0;
    private double shootCdEnemy = 0;

    private int enemyPatrolDir = 1;

    private final Random rng = new Random(1337);
    private final List<double[]> stars = new ArrayList<>();

    // analytics / exploration
    private boolean[][] visited;
    private int uniqueVisited = 0;
    private int walkableTotal = 0;

    // bullets
    private static final class Bullet {
        double x, y, vx, vy;
        boolean fromEnemy;
        double life;
        Bullet(double x, double y, double vx, double vy, boolean fromEnemy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.fromEnemy = fromEnemy;
            this.life = 1.8;
        }
        Rectangle2D bounds() { return new Rectangle2D(x - 3, y - 3, 6, 6); }
    }
    private final List<Bullet> bullets = new ArrayList<>();

    private AnimationTimer timer;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        buildLevel();

        root = new Pane();
        root.setPrefSize(worldW, worldH + HUD_H);

        canvas = new Canvas(worldW, worldH + HUD_H);
        g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);
        root.getChildren().add(canvas);

        scene = new Scene(root, worldW, worldH + HUD_H, Color.BLACK);
        wireInput();

        analytics.startSession(viewMode.name());
        score.reset();
        state = GameState.PLAYING;

        stage.setTitle("Halloween Puzzle (TOP/SIDE) - 8-bit Canvas");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        stage.setOnCloseRequest(e -> stopGame("window_closed"));

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNs == 0) lastFrameNs = now;
                double dt = (now - lastFrameNs) / 1_000_000_000.0;
                lastFrameNs = now;
                if (dt > 0.05) dt = 0.05;

                update(dt);
                render(dt);
            }
        };
        timer.start();
    }

    private void stopGame(String reason) {
        if (timer != null) timer.stop();
        analytics.endSession(reason);
        Platform.exit();
    }

    private void wireInput() {
        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            keysDown.add(k);

            if (k == KeyCode.ESCAPE) stopGame("user_exit");
            if (k == KeyCode.TAB) showStats = !showStats;
            if (k == KeyCode.F1) showAnalyticsPanel = !showAnalyticsPanel;

            if (k == KeyCode.R) {
                analytics.endSession("restart");
                lastFrameNs = 0;
                start(stage);
            }

            // camera turn only in SIDE view
            if (viewMode == ViewMode.SIDE) {
                if (k == KeyCode.Q) sideYawDeg = clamp(sideYawDeg - 3, SIDE_YAW_MIN, SIDE_YAW_MAX);
                if (k == KeyCode.E) sideYawDeg = clamp(sideYawDeg + 3, SIDE_YAW_MIN, SIDE_YAW_MAX);
                if (k == KeyCode.DIGIT0) sideYawDeg = 0;
            }

            // shoot
            if (k == KeyCode.SPACE && state == GameState.PLAYING) {
                analytics.recordAttack();
                firePlayerBullet();
            }

            if (k == KeyCode.ENTER && state == GameState.WON) {
                analytics.endSession("win_confirmed");
                lastFrameNs = 0;
                start(stage);
            }
        });

        scene.setOnKeyReleased(e -> keysDown.remove(e.getCode()));
    }

    private void buildLevel() {
        walls.clear();
        holes.clear();
        candies.clear();
        bullets.clear();

        exitOpen = false;
        sideYawDeg = 0;

        // 22 cols each
        String[] map = {
                "######################",
                "#S....C....#....O....#",
                "#.####.##..#..###....#",
                "#.#..#.....#....#..C.#",
                "#.#..#.###.#.##.#....#",
                "#.#....#...#....#.#..#",
                "#.####.#.###.##.#..E.#",
                "#.#....#.....#..#....#",
                "#.#.##.#####..#..#...#",
                "#.#..#....O...#..#...#",
                "#.#G.#..C.....#..O...#",
                "#..............#...X.#",
                "######################"
        };

        int rows = map.length;
        int cols = map[0].length();
        worldW = cols * TILE;
        worldH = rows * TILE;

        visited = new boolean[rows][cols];
        uniqueVisited = 0;
        walkableTotal = 0;

        double startX = TILE + 6;
        double startY = TILE + 6;

        enemy = null;
        gem = null;
        exitDoor = null;

        for (int r = 0; r < rows; r++) {
            String line = map[r];
            for (int c = 0; c < cols; c++) {
                char ch = line.charAt(c);
                double x = c * TILE;
                double y = r * TILE;

                if (ch != '#') walkableTotal++;

                if (ch == '#') {
                    walls.add(new Rectangle2D(x, y, TILE, TILE));
                } else if (ch == 'S') {
                    startX = x + (TILE - PLAYER_SIZE) * 0.5;
                    startY = y + (TILE - PLAYER_SIZE) * 0.5;
                } else if (ch == 'G') {
                    gem = new Gem(x + (TILE - 16) * 0.5, y + (TILE - 16) * 0.5, 16);
                } else if (ch == 'O') {
                    holes.add(new Hole(x + 7, y + 7, TILE - 14));
                } else if (ch == 'E') {
                    double minX = x - 5 * TILE;
                    double maxX = x + 5 * TILE;
                    enemy = new Enemy(x + (TILE - ENEMY_SIZE) * 0.5, y + (TILE - ENEMY_SIZE) * 0.5,
                            ENEMY_SIZE, ENEMY_SIZE, minX, maxX, 110);
                } else if (ch == 'X') {
                    exitDoor = new Rectangle2D(x + 7, y + 7, TILE - 14, TILE - 14);
                } else if (ch == 'C') {
                    candies.add(new Candy(x + (TILE - 14) * 0.5, y + (TILE - 14) * 0.5));
                }
            }
        }

        player = new Player(startX, startY, PLAYER_SIZE, PLAYER_SIZE);

        if (enemy == null) {
            enemy = new Enemy(10 * TILE, 6 * TILE, ENEMY_SIZE, ENEMY_SIZE, 7 * TILE, 15 * TILE, 110);
        }
        if (gem == null) {
            gem = new Gem(3 * TILE + 10, 10 * TILE + 10, 16);
        }
        if (exitDoor == null) {
            exitDoor = new Rectangle2D((cols - 2) * TILE + 7, 1 * TILE + 7, TILE - 14, TILE - 14);
        }

        stars.clear();
        for (int i = 0; i < 90; i++) {
            stars.add(new double[] {
                    rng.nextDouble() * worldW,
                    rng.nextDouble() * worldH,
                    1 + rng.nextDouble() * 2
            });
        }

        // initialize aim toward right
        aimDx = 1; aimDy = 0;
        shootCdPlayer = 0;
        shootCdEnemy = 0;
        enemyPatrolDir = 1;

        analytics.setTileStats(0, walkableTotal);
    }

    private void update(double dt) {
        memSampleAcc += dt;
        if (memSampleAcc >= 0.35) {
            memSampleAcc = 0;
            analytics.sampleMemory();
        }

        fpsAcc += dt;
        fpsFrames++;
        if (fpsAcc >= 0.5) {
            fps = fpsFrames / fpsAcc;
            fpsAcc = 0;
            fpsFrames = 0;
        }

        if (state != GameState.PLAYING) {
            // still animate background pulses
            gemPulse += dt * 4.0;
            enemyBob += dt * 3.0;
            return;
        }

        if (player.isDead()) {
            analytics.recordDeath();
            stopGame("player_dead");
            return;
        }

        shootCdPlayer = Math.max(0, shootCdPlayer - dt);
        shootCdEnemy  = Math.max(0, shootCdEnemy  - dt);

        // movement input
        double inx = 0, iny = 0;
        if (keysDown.contains(KeyCode.LEFT) || keysDown.contains(KeyCode.A)) inx -= 1;
        if (keysDown.contains(KeyCode.RIGHT) || keysDown.contains(KeyCode.D)) inx += 1;
        if (keysDown.contains(KeyCode.UP) || keysDown.contains(KeyCode.W)) iny -= 1;
        if (keysDown.contains(KeyCode.DOWN) || keysDown.contains(KeyCode.S)) iny += 1;

        double distMoved = 0;

        double len = Math.hypot(inx, iny);
        if (len > 0) {
            double dx = (inx / len) * PLAYER_SPEED * dt;
            double dy = (iny / len) * PLAYER_SPEED * dt;

            // aim follows last movement direction
            aimDx = inx / len;
            aimDy = iny / len;

            distMoved = Math.hypot(dx, dy);

            movePlayerWithCollision(dx, dy);
        } else {
            analytics.addStationaryMs((long) (dt * 1000));
        }

        if (distMoved > 0) analytics.addDistance(distMoved);

        // mark visited tile
        markVisited();

        // update enemy AI and bullets
        updateEnemyAI(dt);
        updateBullets(dt);

        gemPulse += dt * 4.0;
        enemyBob += dt * 3.0;

        // pickups
        if (gem != null && !gem.isCollected() && player.bounds().intersects(gem.bounds())) {
            gem.collect();
            player.giveGem();
            exitOpen = true;
            analytics.recordGemCollected();
            score.add(150);
        }

        for (Candy c : candies) {
            if (!c.taken && player.bounds().intersects(c.b)) {
                c.taken = true;
                analytics.recordCandyCollected();
                score.add(25);
            }
        }

        // enemy body collision
        if (enemy != null && enemy.isAlive() && player.bounds().intersects(enemy.bounds())) {
            analytics.recordHitTaken();
            player.takeHit();
            player.respawn();
            score.add(-12);
        }

        // hole view switch
        long nowMs = System.currentTimeMillis();
        if (nowMs >= holeCooldownUntilMs) {
            for (Hole hole : holes) {
                if (player.bounds().intersects(hole.bounds())) {
                    analytics.recordHoleTrigger();
                    toggleView();
                    holeCooldownUntilMs = nowMs + 450;
                    break;
                }
            }
        }

        // win condition
        if (exitOpen && exitDoor != null && player.hasGem() && player.bounds().intersects(exitDoor)) {
            state = GameState.WON;
            analytics.endSession("win");
        }

        // keep analytics tile stats updated
        analytics.setTileStats(uniqueVisited, walkableTotal);
    }

    private void markVisited() {
        int r = (int) ((player.cy()) / TILE);
        int c = (int) ((player.cx()) / TILE);

        if (r >= 0 && r < visited.length && c >= 0 && c < visited[0].length) {
            if (!visited[r][c]) {
                visited[r][c] = true;
                uniqueVisited++;
            }
        }
    }

    private void updateEnemyAI(double dt) {
        if (enemy == null || !enemy.isAlive()) return;

        double ex = enemy.cx();
        double ey = enemy.cy();
        double px = player.cx();
        double py = player.cy();

        double dist = Math.hypot(px - ex, py - ey);

        boolean los = hasLineOfSight(ex, ey, px, py);

        boolean threatened = los && dist <= (SHOOT_RANGE * 1.15);
        if (threatened) analytics.addExposureMs((long) (dt * 1000));

        // chase if in line-of-sight and within chase range (or if player has gem = more aggressive)
        boolean aggressive = player.hasGem();
        double chaseRange = aggressive ? CHASE_RANGE * 1.25 : CHASE_RANGE;

        if (los && dist <= chaseRange) {
            double vx = (px - ex);
            double vy = (py - ey);
            double l = Math.hypot(vx, vy);
            if (l > 1e-6) {
                vx /= l; vy /= l;
            }

            // small "strafe" wobble for arcade feel
            double wob = 0.25 * Math.sin((System.currentTimeMillis() % 100000) / 160.0);
            double sx = -vy * wob;
            double sy =  vx * wob;

            double speed = aggressive ? ENEMY_CHASE_SPEED * 1.08 : ENEMY_CHASE_SPEED;
            double dx = (vx + sx) * speed * dt;
            double dy = (vy + sy) * speed * dt;

            moveEnemyWithCollision(dx, dy);
        } else {
            // patrol horizontally between minX/maxX (using the constructor range)
            double dx = enemyPatrolDir * ENEMY_PATROL_SPEED * dt;
            Rectangle2D nb = new Rectangle2D(enemy.x + dx, enemy.y, enemy.w, enemy.h);
            if (!hitsAnyWall(nb)) {
                enemy.x += dx;
            } else {
                enemyPatrolDir *= -1;
            }
            // bounce at implicit boundaries
            if (enemy.x < 0 || enemy.x > worldW - enemy.w) enemyPatrolDir *= -1;
        }

        // shoot if LoS and within range
        ex = enemy.cx();
        ey = enemy.cy();
        dist = Math.hypot(player.cx() - ex, player.cy() - ey);

        if (los && dist <= SHOOT_RANGE && shootCdEnemy <= 0) {
            shootCdEnemy = SHOOT_COOLDOWN_ENEMY;

            double tx = player.cx();
            double ty = player.cy();

            // tiny lead to reduce "dumbness"
            tx += aimDx * 12;
            ty += aimDy * 12;

            double vx = tx - ex;
            double vy = ty - ey;
            double l = Math.hypot(vx, vy);
            if (l < 1e-6) { vx = 1; vy = 0; l = 1; }
            vx /= l; vy /= l;

            bullets.add(new Bullet(ex, ey, vx * BULLET_SPEED_ENEMY, vy * BULLET_SPEED_ENEMY, true));
            analytics.recordEnemyShotFired();
        }
    }

    private void updateBullets(double dt) {
        if (bullets.isEmpty()) return;

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.life -= dt;
            b.x += b.vx * dt;
            b.y += b.vy * dt;

            if (b.life <= 0 || b.x < -20 || b.y < -20 || b.x > worldW + 20 || b.y > worldH + 20) {
                bullets.remove(i);
                continue;
            }

            Rectangle2D bb = b.bounds();

            // wall hit
            if (hitsAnyWall(bb)) {
                bullets.remove(i);
                continue;
            }

            // hit tests
            if (b.fromEnemy) {
                if (player.bounds().intersects(bb)) {
                    analytics.recordEnemyShotHit();
                    analytics.recordHitTaken();
                    player.takeHit();
                    player.respawn();
                    score.add(-15);
                    bullets.remove(i);
                }
            } else {
                if (enemy != null && enemy.isAlive() && enemy.bounds().intersects(bb)) {
                    analytics.recordPlayerShotHit();
                    enemy.kill();
                    analytics.recordEnemyDefeated();
                    score.add(95);
                    bullets.remove(i);
                }
            }
        }
    }

    private void firePlayerBullet() {
        if (shootCdPlayer > 0) return;
        shootCdPlayer = SHOOT_COOLDOWN_PLAYER;

        double px = player.cx();
        double py = player.cy();

        double vx = aimDx;
        double vy = aimDy;
        double l = Math.hypot(vx, vy);
        if (l < 1e-6) { vx = 1; vy = 0; l = 1; }
        vx /= l; vy /= l;

        // spawn slightly in front of player
        double sx = px + vx * 14;
        double sy = py + vy * 14;

        bullets.add(new Bullet(sx, sy, vx * BULLET_SPEED_PLAYER, vy * BULLET_SPEED_PLAYER, false));
        analytics.recordPlayerShotFired();
        score.add(-1);
    }

    private boolean hasLineOfSight(double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double dist = Math.hypot(dx, dy);
        if (dist < 1e-6) return true;

        int steps = Math.max(6, (int) (dist / (TILE / 3.0)));
        double stepx = dx / steps;
        double stepy = dy / steps;

        double x = ax;
        double y = ay;

        for (int i = 0; i <= steps; i++) {
            Rectangle2D p = new Rectangle2D(x - 2, y - 2, 4, 4);
            if (hitsAnyWall(p)) return false;
            x += stepx;
            y += stepy;
        }
        return true;
    }

    private void toggleView() {
        viewMode = (viewMode == ViewMode.TOP) ? ViewMode.SIDE : ViewMode.TOP;
        analytics.recordViewSwitch(viewMode.name());
        flash();
    }

    private void flash() {
        FadeTransition ft = new FadeTransition(Duration.millis(140), canvas);
        ft.setFromValue(1.0);
        ft.setToValue(0.35);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.play();
    }

    private void movePlayerWithCollision(double dx, double dy) {
        if (dx != 0) {
            double nx = player.x + dx;
            Rectangle2D nb = new Rectangle2D(nx, player.y, player.w, player.h);
            if (!hitsAnyWall(nb)) player.x = nx;
            else analytics.recordWallBump();
        }
        if (dy != 0) {
            double ny = player.y + dy;
            Rectangle2D nb = new Rectangle2D(player.x, ny, player.w, player.h);
            if (!hitsAnyWall(nb)) player.y = ny;
            else analytics.recordWallBump();
        }

        player.x = clamp(player.x, 0, worldW - player.w);
        player.y = clamp(player.y, 0, worldH - player.h);
    }

    private void moveEnemyWithCollision(double dx, double dy) {
        if (dx != 0) {
            double nx = enemy.x + dx;
            Rectangle2D nb = new Rectangle2D(nx, enemy.y, enemy.w, enemy.h);
            if (!hitsAnyWall(nb)) enemy.x = nx;
            else enemyPatrolDir *= -1;
        }
        if (dy != 0) {
            double ny = enemy.y + dy;
            Rectangle2D nb = new Rectangle2D(enemy.x, ny, enemy.w, enemy.h);
            if (!hitsAnyWall(nb)) enemy.y = ny;
        }

        enemy.x = clamp(enemy.x, 0, worldW - enemy.w);
        enemy.y = clamp(enemy.y, 0, worldH - enemy.h);
    }

    private boolean hitsAnyWall(Rectangle2D b) {
        for (Rectangle2D wall : walls) {
            if (wall.intersects(b)) return true;
        }
        return false;
    }

    private void render(double dt) {
        g.setFill(viewMode == ViewMode.TOP ? BG_TOP : BG_SIDE);
        g.fillRect(0, 0, worldW, worldH + HUD_H);

        drawStarfield();

        drawWorld();

        drawVignette();
        drawScanlines();

        drawHud();

        if (showAnalyticsPanel) drawAnalyticsPanel();
        if (state == GameState.WON) drawWinOverlay();
    }

    private void drawStarfield() {
        g.setFill(Color.rgb(160, 140, 210, 0.18));
        for (double[] s : stars) {
            double x = s[0];
            double y = s[1] + HUD_H;
            double r = s[2];
            g.fillRect(x, y, r, r);
        }
    }

    private void drawWorld() {
        // floor
        for (int y = 0; y < worldH; y += TILE) {
            for (int x = 0; x < worldW; x += TILE) {
                drawFloorTile(x, y);
            }
        }

        // walls
        for (Rectangle2D wall : walls) {
            drawWallTile((int) wall.getMinX(), (int) wall.getMinY());
        }

        // holes
        for (Hole hole : holes) {
            drawHole(hole.x(), hole.y(), hole.size());
        }

        // exit
        if (exitDoor != null) {
            drawExit(exitDoor, exitOpen);
        }

        // candies
        for (Candy c : candies) {
            if (!c.taken) drawCandy(c.b.getMinX(), c.b.getMinY());
        }

        // gem
        if (gem != null && !gem.isCollected()) {
            drawGem(gem.x(), gem.y(), gem.size());
        }

        // enemy
        if (enemy != null && enemy.isAlive()) {
            drawGhost(enemy.x, enemy.y, ENEMY_SIZE);
        }

        // bullets
        for (Bullet b : bullets) {
            drawBullet(b);
        }

        // player
        drawPlayer(player.x, player.y, PLAYER_SIZE);

        // tiny aim indicator
        drawAimVector();
    }

    private void drawAimVector() {
        double x = player.cx();
        double y = player.cy();
        double[] p0 = project(x, y);
        double[] p1 = project(x + aimDx * 18, y + aimDy * 18);

        g.setStroke(Color.rgb(255, 255, 255, 0.35));
        g.setLineWidth(2);
        g.strokeLine(p0[0], p0[1] + HUD_H, p1[0], p1[1] + HUD_H);
        g.setLineWidth(1);
    }

    private void drawVignette() {
        RadialGradient rg = new RadialGradient(
                0, 0,
                worldW * 0.5, (worldH + HUD_H) * 0.55,
                Math.max(worldW, worldH) * 0.75,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0,0,0,0.0)),
                new Stop(0.7, Color.rgb(0,0,0,0.12)),
                new Stop(1.0, Color.rgb(0,0,0,0.42))
        );
        g.setFill(rg);
        g.fillRect(0, 0, worldW, worldH + HUD_H);
    }

    private void drawScanlines() {
        g.setFill(Color.rgb(0, 0, 0, 0.08));
        for (int y = 0; y < worldH + HUD_H; y += 4) {
            g.fillRect(0, y, worldW, 1);
        }
    }

    private void drawFloorTile(int x, int y) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        Color a = Color.rgb(20, 18, 28);
        Color b = Color.rgb(26, 24, 34);

        g.setFill(((x / TILE + y / TILE) & 1) == 0 ? a : b);
        g.fillRect(sx, sy, TILE, TILE);

        g.setFill(Color.rgb(70, 60, 90, 0.10));
        g.fillRect(sx + 2, sy + 2, 2, 2);

        if (viewMode == ViewMode.SIDE) {
            g.setFill(Color.rgb(255, 255, 255, 0.04));
            g.fillRect(sx + 2, sy + 2, TILE - 4, 1);
        }
    }

    private void drawWallTile(int x, int y) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(WALL_DARK);
        g.fillRect(sx, sy, TILE, TILE);

        g.setFill(WALL_LIGHT);
        for (int i = 4; i < TILE; i += 8) {
            g.fillRect(sx + 2, sy + i, TILE - 4, 2);
        }

        g.setFill(Color.rgb(0, 0, 0, 0.25));
        g.fillRect(sx, sy, TILE, 3);
        g.fillRect(sx, sy, 3, TILE);

        // outline
        g.setStroke(Color.rgb(0,0,0,0.35));
        g.strokeRect(sx + 0.5, sy + 0.5, TILE - 1, TILE - 1);
    }

    private void drawHole(double x, double y, double size) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(Color.rgb(0, 0, 0, 0.78));
        g.fillOval(sx, sy, size, size);

        g.setStroke(Color.rgb(180, 180, 220, 0.22));
        g.strokeOval(sx + 2, sy + 2, size - 4, size - 4);

        g.setStroke(Color.rgb(140, 60, 180, 0.18));
        g.strokeOval(sx + 6, sy + 6, size - 12, size - 12);
    }

    private void drawExit(Rectangle2D door, boolean open) {
        double[] p = project(door.getMinX(), door.getMinY());
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(open ? Color.rgb(70, 220, 120) : Color.rgb(180, 60, 70));
        g.fillRect(sx, sy, door.getWidth(), door.getHeight());

        g.setFill(Color.rgb(0, 0, 0, 0.25));
        g.fillRect(sx + 4, sy + 4, door.getWidth() - 8, door.getHeight() - 8);

        g.setStroke(Color.rgb(255, 255, 255, 0.12));
        g.strokeRect(sx + 1, sy + 1, door.getWidth() - 2, door.getHeight() - 2);
    }

    private void drawCandy(double x, double y) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(Color.rgb(255, 140, 200));
        g.fillRect(sx + 4, sy + 4, 6, 6);
        g.setFill(Color.rgb(255, 220, 120));
        g.fillRect(sx + 6, sy + 10, 2, 6);

        g.setFill(Color.rgb(255, 255, 255, 0.22));
        g.fillRect(sx + 4, sy + 4, 2, 2);
    }

    private void drawGem(double x, double y, double size) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        double pulse = 1.0 + 0.12 * Math.sin(gemPulse);
        double cx = sx + size * 0.5;
        double cy = sy + size * 0.5;

        double s = size * pulse * 0.5;

        double[] xs = { cx, cx + s, cx, cx - s };
        double[] ys = { cy - s, cy, cy + s, cy };

        g.setFill(Color.rgb(255, 210, 80));
        g.fillPolygon(xs, ys, 4);
        g.setStroke(Color.rgb(255, 245, 180));
        g.strokePolygon(xs, ys, 4);

        g.setFill(Color.rgb(255, 255, 255, 0.18));
        g.fillOval(cx - 2, cy - 6, 4, 4);
    }

    private void drawGhost(double x, double y, double size) {
        double[] p = project(x, y + 3 * Math.sin(enemyBob));
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(Color.rgb(165, 125, 255));
        g.fillOval(sx, sy, size, size);

        g.setFill(Color.rgb(20, 20, 30));
        g.fillRect(sx + 7, sy + 8, 3, 3);
        g.fillRect(sx + 14, sy + 8, 3, 3);

        g.setFill(Color.rgb(230, 210, 255, 0.35));
        g.fillRect(sx + 5, sy + 5, 4, 3);

        g.setStroke(Color.rgb(0,0,0,0.35));
        g.strokeOval(sx + 0.5, sy + 0.5, size - 1, size - 1);
    }

    private void drawPlayer(double x, double y, double size) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(Color.rgb(255, 140, 50));
        g.fillOval(sx, sy, size, size);

        g.setFill(Color.rgb(20, 20, 25));
        g.fillRect(sx + 7, sy + 9, 3, 3);
        g.fillRect(sx + 14, sy + 9, 3, 3);
        g.fillRect(sx + 10, sy + 15, 4, 2);

        g.setFill(Color.rgb(80, 200, 120));
        g.fillRect(sx + 11, sy - 2, 2, 4);

        g.setStroke(Color.rgb(0,0,0,0.35));
        g.strokeOval(sx + 0.5, sy + 0.5, size - 1, size - 1);
    }

    private void drawBullet(Bullet b) {
        double[] p = project(b.x - 3, b.y - 3);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        if (b.fromEnemy) {
            g.setFill(Color.rgb(255, 80, 80));
        } else {
            g.setFill(Color.rgb(120, 255, 180));
        }
        g.fillRect(sx, sy, 6, 6);

        g.setFill(Color.rgb(255, 255, 255, 0.25));
        g.fillRect(sx + 1, sy + 1, 2, 2);
    }

    private void drawHud() {
        g.setFill(Color.rgb(0, 0, 0, 0.58));
        g.fillRect(0, 0, worldW, HUD_H);

        g.setStroke(Color.rgb(255, 255, 255, 0.12));
        g.strokeLine(0, HUD_H - 1, worldW, HUD_H - 1);

        AnalyticsManager.Snapshot s = analytics.snapshot();

        g.setFill(Color.rgb(245, 245, 255));
        g.setFont(Font.font("Monospaced", 16));

        String line1 = "View: " + viewMode +
                " | Score: " + score.get() +
                " | HP: " + player.hp() +
                " | Gem: " + (player.hasGem() ? "YES" : "NO") +
                " | Exit: " + (exitOpen ? "OPEN" : "LOCKED") +
                (viewMode == ViewMode.SIDE ? " | Turn(Q/E): " + (int) sideYawDeg + "deg" : "");

        g.fillText(line1, 14, 22);

        g.setFill(Color.rgb(200, 200, 225));
        g.setFont(Font.font("Monospaced", 13));
        g.fillText("Move: WASD/Arrows  Shoot: SPACE  SwitchView: step on HOLE  Stats: TAB  Panel: F1  Restart: R  Quit: ESC",
                14, 44);

        if (!showStats) return;

        double usedMB = s.currentUsedBytes / (1024.0 * 1024.0);
        double peakMB = s.peakUsedBytes / (1024.0 * 1024.0);

        double topPct = (s.totalMs <= 0) ? 0 : (100.0 * s.topViewMs / s.totalMs);
        double sidePct = 100.0 - topPct;

        double skill = computeSkillIndex(s);

        String l3 =
                "t=" + (s.totalMs / 1000) + "s  fps=" + String.format("%.1f", fps) +
                        "  mem=" + String.format("%.1f", usedMB) + "MB (peak " + String.format("%.1f", peakMB) + "MB)" +
                        "  APM=" + String.format("%.1f", s.actionsPerMinute) +
                        "  skill=" + String.format("%.0f", skill);

        String l4 =
                "P shots " + s.playerShotsHit + "/" + s.playerShotsFired + " (" + String.format("%.0f", 100 * s.playerAccuracy) + "%) " +
                        "| E shots " + s.enemyShotsHit + "/" + s.enemyShotsFired + " (" + String.format("%.0f", 100 * s.enemyAccuracy) + "%) " +
                        "| explore " + String.format("%.0f", 100 * s.explorationRatio) + "% " +
                        "| idle " + String.format("%.0f", 100 * s.hesitationRatio) + "% " +
                        "| threat " + String.format("%.0f", 100 * s.exposureRatio) + "%";

        String l5 =
                "dist=" + String.format("%.1f", s.distanceTraveled) +
                        "  switches=" + s.viewSwitches +
                        "  candies=" + s.candiesCollected +
                        "  hits=" + s.hitsTaken +
                        "  bumps=" + s.wallBumps +
                        "  TOP/SIDE=" + String.format("%.0f", topPct) + "/" + String.format("%.0f", sidePct) + "% " +
                        (s.timeToGemMs >= 0 ? ("  gem@" + (s.timeToGemMs / 1000) + "s") : "") +
                        (s.timeToFirstSwitchMs >= 0 ? ("  firstSwitch@" + (s.timeToFirstSwitchMs / 1000) + "s") : "");

        g.setFill(Color.rgb(240, 240, 250, 0.92));
        g.setFont(Font.font("Monospaced", 12));
        g.fillText(l3, 14, 64);
        g.fillText(l4, 14, 82);
        g.fillText(l5, 14, 100);
    }

    private double computeSkillIndex(AnalyticsManager.Snapshot s) {
        // stable, bounded, readable: 0..100
        double acc = s.playerAccuracy;                 // 0..1
        double exp = s.explorationRatio;              // 0..1
        double calm = 1.0 - s.hesitationRatio;        // 0..1
        double danger = 1.0 - Math.min(1.0, s.hitsTaken / 8.0); // 1 is good
        double pace = Math.min(1.0, s.actionsPerMinute / 60.0); // 60 APM cap

        double score =
                0.34 * acc +
                        0.18 * exp +
                        0.16 * calm +
                        0.18 * danger +
                        0.14 * pace;

        return clamp(score * 100.0, 0, 100);
    }

    private void drawAnalyticsPanel() {
        AnalyticsManager.Snapshot s = analytics.snapshot();

        double w = Math.min(worldW - 40, 720);
        double h = 210;

        double x = 20;
        double y = HUD_H + 20;

        g.setFill(Color.rgb(0, 0, 0, 0.78));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setStroke(Color.rgb(255, 255, 255, 0.12));
        g.strokeRoundRect(x + 0.5, y + 0.5, w - 1, h - 1, 12, 12);

        g.setFill(Color.rgb(245, 245, 255));
        g.setFont(Font.font("Monospaced", 16));
        g.fillText("ANALYTICS PANEL (F1 to hide)", x + 16, y + 26);

        g.setFont(Font.font("Monospaced", 12));
        g.setFill(Color.rgb(220, 220, 240));

        double usedMB = s.currentUsedBytes / (1024.0 * 1024.0);
        double peakMB = s.peakUsedBytes / (1024.0 * 1024.0);

        String a1 = "Performance: fps=" + String.format("%.1f", fps) +
                "  memUsed=" + String.format("%.1f", usedMB) + "MB  memPeak=" + String.format("%.1f", peakMB) + "MB";
        String a2 = "Behavior: distance=" + String.format("%.1f", s.distanceTraveled) +
                "  idle=" + String.format("%.0f", 100 * s.hesitationRatio) + "%  threat=" + String.format("%.0f", 100 * s.exposureRatio) + "%";
        String a3 = "Combat: playerAcc=" + String.format("%.0f", 100 * s.playerAccuracy) + "% (" + s.playerShotsHit + "/" + s.playerShotsFired + ")" +
                "  enemyAcc=" + String.format("%.0f", 100 * s.enemyAccuracy) + "% (" + s.enemyShotsHit + "/" + s.enemyShotsFired + ")";
        String a4 = "Learning signals: explore=" + String.format("%.0f", 100 * s.explorationRatio) + "%  APM=" + String.format("%.1f", s.actionsPerMinute) +
                "  wallBumps=" + s.wallBumps + "  switches=" + s.viewSwitches;
        String a5 = "Milestones: firstSwitch=" + (s.timeToFirstSwitchMs < 0 ? "N/A" : (s.timeToFirstSwitchMs / 1000) + "s") +
                "  gem=" + (s.timeToGemMs < 0 ? "N/A" : (s.timeToGemMs / 1000) + "s") +
                "  hits=" + s.hitsTaken + "  kills=" + s.enemiesDefeated;

        g.fillText(a1, x + 16, y + 56);
        g.fillText(a2, x + 16, y + 76);
        g.fillText(a3, x + 16, y + 96);
        g.fillText(a4, x + 16, y + 116);
        g.fillText(a5, x + 16, y + 136);

        g.setFill(Color.rgb(255, 255, 255, 0.10));
        g.fillRect(x + 16, y + 152, w - 32, 1);

        g.setFill(Color.rgb(200, 200, 220));
        g.fillText("Notes: threat% accumulates when enemy has LoS & you are within shoot range.", x + 16, y + 174);
        g.fillText("      explore% uses unique tiles visited / total walkable tiles.", x + 16, y + 192);
    }

    private void drawWinOverlay() {
        g.setFill(Color.rgb(0, 0, 0, 0.70));
        g.fillRect(0, HUD_H, worldW, worldH);

        g.setFill(Color.rgb(255, 255, 255));
        g.setFont(Font.font("Monospaced", 36));
        g.fillText("YOU WIN!", worldW * 0.35, HUD_H + worldH * 0.45);

        g.setFont(Font.font("Monospaced", 14));
        g.setFill(Color.rgb(220, 220, 240));
        g.fillText("Stats saved to: " + System.getProperty("user.home") + "\\halloween_game_stats.log",
                worldW * 0.18, HUD_H + worldH * 0.52);

        g.fillText("Press ENTER to play again, or ESC to quit.", worldW * 0.24, HUD_H + worldH * 0.58);
    }

    private double[] project(double worldX, double worldY) {
        if (viewMode == ViewMode.TOP) return new double[]{worldX, worldY};

        // base isometric
        double wx = worldX / TILE;
        double wy = worldY / TILE;

        double originX = worldW * 0.52;
        double originY = 18;

        double isoX = originX + (wx - wy) * TILE * 0.72;
        double isoY = originY + (wx + wy) * TILE * 0.38;

        // camera "turn" = small rotation in screen space around map center (stable, low risk)
        double pivotX = worldW * 0.50;
        double pivotY = worldH * 0.55;

        double ang = Math.toRadians(sideYawDeg);

        double px = isoX - pivotX;
        double py = isoY - pivotY;

        double rx = px * Math.cos(ang) - py * Math.sin(ang);
        double ry = px * Math.sin(ang) + py * Math.cos(ang);

        isoX = pivotX + rx;
        isoY = pivotY + ry;

        return new double[]{isoX, isoY};
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
