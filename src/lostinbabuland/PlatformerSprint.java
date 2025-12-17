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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlatformerSprint implements MiniGame {

    private enum ViewMode { TOP, SIDE }
    private enum GameState { PLAYING, WON }

    private static final int TILE = 36;
    private static final int HUD_H = 60;

    private static final double PLAYER_SIZE = 24;
    private static final double ENEMY_SIZE  = 24;

    private static final double PLAYER_SPEED = 190;

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

    private double gemPulse = 0;
    private double enemyBob = 0;

    private final Random rng = new Random(1337);
    private final List<double[]> stars = new ArrayList<>();

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

            if (k == KeyCode.R) {
                analytics.endSession("restart");
                lastFrameNs = 0;
                start(stage);
            }

            if (k == KeyCode.SPACE && state == GameState.PLAYING) {
                analytics.recordAttack();
                tryAttackEnemy();
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

        exitOpen = false;

        String[] map = {
                "######################",
                "#S....C....#.........#",
                "#.####.##..#..###....#",
                "#.#..#.....#....#..C.#",
                "#.#..#.###.#.##.#....#",
                "#.#....#...#....#..#.#",
                "#.####.#.###.##.#..E.#",
                "#.#....#.....#..#....#",
                "#.#.##.#####..#..#..X#",
                "#.#..#....O..........#",
                "#.#G.#..C.....#..O...#",
                "######################"
        };

        int rows = map.length;
        int cols = map[0].length();
        worldW = cols * TILE;
        worldH = rows * TILE;

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
        for (int i = 0; i < 70; i++) {
            stars.add(new double[] {
                    rng.nextDouble() * worldW,
                    rng.nextDouble() * worldH,
                    1 + rng.nextDouble() * 2
            });
        }
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

        if (state != GameState.PLAYING) return;

        if (player.isDead()) {
            analytics.recordDeath();
            stopGame("player_dead");
            return;
        }

        double dx = 0, dy = 0;
        if (keysDown.contains(KeyCode.LEFT) || keysDown.contains(KeyCode.A)) dx -= 1;
        if (keysDown.contains(KeyCode.RIGHT) || keysDown.contains(KeyCode.D)) dx += 1;
        if (keysDown.contains(KeyCode.UP) || keysDown.contains(KeyCode.W)) dy -= 1;
        if (keysDown.contains(KeyCode.DOWN) || keysDown.contains(KeyCode.S)) dy += 1;

        double len = Math.hypot(dx, dy);
        if (len > 0) {
            dx = (dx / len) * PLAYER_SPEED * dt;
            dy = (dy / len) * PLAYER_SPEED * dt;
            movePlayerWithCollision(dx, dy);
        }

        if (enemy != null) enemy.update(dt, walls);

        gemPulse += dt * 4.0;
        enemyBob += dt * 3.0;

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

        if (enemy != null && enemy.isAlive() && player.bounds().intersects(enemy.bounds())) {
            analytics.recordHitTaken();
            player.takeHit();
            player.respawn();
            score.add(-10);
        }

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

        if (exitOpen && exitDoor != null && player.hasGem() && player.bounds().intersects(exitDoor)) {
            state = GameState.WON;
            analytics.endSession("win");
        }
    }

    private void tryAttackEnemy() {
        if (enemy == null || !enemy.isAlive()) return;

        double dist = Math.hypot(player.cx() - enemy.cx(), player.cy() - enemy.cy());
        if (dist <= TILE * 1.15) {
            enemy.kill();
            analytics.recordEnemyDefeated();
            score.add(80);
        } else {
            score.add(-1);
        }
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

        drawHud();
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
        for (int y = 0; y < worldH; y += TILE) {
            for (int x = 0; x < worldW; x += TILE) {
                drawFloorTile(x, y);
            }
        }

        for (Rectangle2D wall : walls) {
            drawWallTile((int) wall.getMinX(), (int) wall.getMinY());
        }

        for (Hole hole : holes) {
            drawHole(hole.x(), hole.y(), hole.size());
        }

        if (exitDoor != null) {
            drawExit(exitDoor, exitOpen);
        }

        for (Candy c : candies) {
            if (!c.taken) drawCandy(c.b.getMinX(), c.b.getMinY());
        }

        if (gem != null && !gem.isCollected()) {
            drawGem(gem.x(), gem.y(), gem.size());
        }

        if (enemy != null && enemy.isAlive()) {
            drawGhost(enemy.x, enemy.y, ENEMY_SIZE);
        }

        drawPlayer(player.x, player.y, PLAYER_SIZE);
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
    }

    private void drawHole(double x, double y, double size) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(Color.rgb(0, 0, 0, 0.75));
        g.fillOval(sx, sy, size, size);

        g.setStroke(Color.rgb(180, 180, 220, 0.22));
        g.strokeOval(sx + 2, sy + 2, size - 4, size - 4);
    }

    private void drawExit(Rectangle2D door, boolean open) {
        double[] p = project(door.getMinX(), door.getMinY());
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(open ? Color.rgb(70, 220, 120) : Color.rgb(180, 60, 70));
        g.fillRect(sx, sy, door.getWidth(), door.getHeight());

        g.setFill(Color.rgb(0, 0, 0, 0.25));
        g.fillRect(sx + 4, sy + 4, door.getWidth() - 8, door.getHeight() - 8);
    }

    private void drawCandy(double x, double y) {
        double[] p = project(x, y);
        double sx = p[0];
        double sy = p[1] + HUD_H;

        g.setFill(Color.rgb(255, 140, 200));
        g.fillRect(sx + 4, sy + 4, 6, 6);
        g.setFill(Color.rgb(255, 220, 120));
        g.fillRect(sx + 6, sy + 10, 2, 6);
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
    }

    private void drawHud() {
        g.setFill(Color.rgb(0, 0, 0, 0.55));
        g.fillRect(0, 0, worldW, HUD_H);

        g.setStroke(Color.rgb(255, 255, 255, 0.10));
        g.strokeLine(0, HUD_H - 1, worldW, HUD_H - 1);

        g.setFill(Color.rgb(240, 240, 250));
        g.setFont(Font.font("Monospaced", 16));

        AnalyticsManager.Snapshot s = analytics.snapshot();

        String line1 = "View: " + viewMode +
                " | Score: " + score.get() +
                " | HP: " + player.hp() +
                " | Gem: " + (player.hasGem() ? "YES" : "NO") +
                " | Exit: " + (exitOpen ? "OPEN" : "LOCKED");

        g.fillText(line1, 14, 22);

        String line2 = "Move: WASD/Arrows  Attack: SPACE  Switch: step on HOLE  Stats: TAB  Restart: R  Quit: ESC";
        g.setFill(Color.rgb(200, 200, 220));
        g.setFont(Font.font("Monospaced", 13));
        g.fillText(line2, 14, 44);

        if (!showStats) return;

        g.setFill(Color.rgb(255, 255, 255, 0.88));
        g.setFont(Font.font("Monospaced", 12));

        double usedMB = s.currentUsedBytes / (1024.0 * 1024.0);
        double peakMB = s.peakUsedBytes / (1024.0 * 1024.0);

        String stats = "t=" + (s.totalMs / 1000) + "s  fps=" + String.format("%.1f", fps) +
                "  mem=" + String.format("%.1f", usedMB) + "MB (peak " + String.format("%.1f", peakMB) + "MB)" +
                "  switches=" + s.viewSwitches +
                "  candies=" + s.candiesCollected +
                "  hits=" + s.hitsTaken +
                "  kills=" + s.enemiesDefeated;

        g.fillText(stats, 14, 58);
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

        double wx = worldX / TILE;
        double wy = worldY / TILE;

        double originX = worldW * 0.52;
        double originY = 20;

        double isoX = originX + (wx - wy) * TILE * 0.72;
        double isoY = originY + (wx + wy) * TILE * 0.38;

        return new double[]{isoX, isoY};
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
