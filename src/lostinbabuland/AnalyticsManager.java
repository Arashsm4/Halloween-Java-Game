// src/lostinbabuland/AnalyticsManager.java
package lostinbabuland;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public final class AnalyticsManager {

    public static final class Snapshot {
        public final long totalMs;
        public final long topViewMs;
        public final long sideViewMs;

        public final int viewSwitches;
        public final int gemsCollected;
        public final int candiesCollected;
        public final int enemiesDefeated;
        public final int attacks;
        public final int hitsTaken;
        public final int deaths;
        public final int wallBumps;
        public final int holeTriggers;

        public final int playerShotsFired;
        public final int playerShotsHit;
        public final int enemyShotsFired;
        public final int enemyShotsHit;

        public final double distanceTraveled;
        public final long stationaryMs;
        public final long exposureMs;

        public final int uniqueTilesVisited;
        public final int walkableTilesTotal;

        public final long timeToFirstSwitchMs; // -1 if never switched
        public final long timeToGemMs;         // -1 if never collected

        public final long currentUsedBytes;
        public final long peakUsedBytes;

        public final double playerAccuracy;    // 0..1
        public final double enemyAccuracy;     // 0..1
        public final double explorationRatio;  // 0..1
        public final double hesitationRatio;   // 0..1  (stationary / total)
        public final double exposureRatio;     // 0..1  (threat exposure / total)
        public final double actionsPerMinute;  // APM-ish

        private Snapshot(long totalMs, long topViewMs, long sideViewMs,
                         int viewSwitches, int gemsCollected, int candiesCollected,
                         int enemiesDefeated, int attacks, int hitsTaken, int deaths,
                         int wallBumps, int holeTriggers,
                         int playerShotsFired, int playerShotsHit,
                         int enemyShotsFired, int enemyShotsHit,
                         double distanceTraveled, long stationaryMs, long exposureMs,
                         int uniqueTilesVisited, int walkableTilesTotal,
                         long timeToFirstSwitchMs, long timeToGemMs,
                         long currentUsedBytes, long peakUsedBytes) {

            this.totalMs = totalMs;
            this.topViewMs = topViewMs;
            this.sideViewMs = sideViewMs;

            this.viewSwitches = viewSwitches;
            this.gemsCollected = gemsCollected;
            this.candiesCollected = candiesCollected;
            this.enemiesDefeated = enemiesDefeated;
            this.attacks = attacks;
            this.hitsTaken = hitsTaken;
            this.deaths = deaths;
            this.wallBumps = wallBumps;
            this.holeTriggers = holeTriggers;

            this.playerShotsFired = playerShotsFired;
            this.playerShotsHit = playerShotsHit;
            this.enemyShotsFired = enemyShotsFired;
            this.enemyShotsHit = enemyShotsHit;

            this.distanceTraveled = distanceTraveled;
            this.stationaryMs = stationaryMs;
            this.exposureMs = exposureMs;

            this.uniqueTilesVisited = uniqueTilesVisited;
            this.walkableTilesTotal = walkableTilesTotal;

            this.timeToFirstSwitchMs = timeToFirstSwitchMs;
            this.timeToGemMs = timeToGemMs;

            this.currentUsedBytes = currentUsedBytes;
            this.peakUsedBytes = peakUsedBytes;

            this.playerAccuracy = playerShotsFired <= 0 ? 0.0 : (playerShotsHit / (double) playerShotsFired);
            this.enemyAccuracy  = enemyShotsFired  <= 0 ? 0.0 : (enemyShotsHit  / (double) enemyShotsFired);

            this.explorationRatio = walkableTilesTotal <= 0 ? 0.0 : (uniqueTilesVisited / (double) walkableTilesTotal);

            double t = Math.max(1, totalMs);
            this.hesitationRatio = clamp01(stationaryMs / (double) t);
            this.exposureRatio   = clamp01(exposureMs / (double) t);

            double minutes = totalMs / 60000.0;
            if (minutes <= 0) minutes = 1e-9;
            int actions = attacks + viewSwitches + candiesCollected + gemsCollected + enemiesDefeated;
            this.actionsPerMinute = actions / minutes;
        }

        private static double clamp01(double v) {
            if (v < 0) return 0;
            if (v > 1) return 1;
            return v;
        }
    }

    private long sessionStartMs;
    private long lastViewSwitchMs;
    private String currentView = "TOP";
    private long topViewMs;
    private long sideViewMs;

    private int viewSwitches;
    private int gemsCollected;
    private int candiesCollected;
    private int enemiesDefeated;
    private int hitsTaken;
    private int deaths;
    private int attacks;
    private int wallBumps;
    private int holeTriggers;

    private int playerShotsFired;
    private int playerShotsHit;
    private int enemyShotsFired;
    private int enemyShotsHit;

    private double distanceTraveled;
    private long stationaryMs;
    private long exposureMs;

    private int uniqueTilesVisited;
    private int walkableTilesTotal;

    private long timeToFirstSwitchMs = -1;
    private long timeToGemMs = -1;

    private long currentUsedBytes;
    private long peakUsedBytes;

    private boolean ended;

    public void startSession(String initialView) {
        ended = false;
        sessionStartMs = System.currentTimeMillis();
        lastViewSwitchMs = sessionStartMs;
        currentView = initialView == null ? "TOP" : initialView;

        topViewMs = 0;
        sideViewMs = 0;

        viewSwitches = 0;
        gemsCollected = 0;
        candiesCollected = 0;
        enemiesDefeated = 0;
        hitsTaken = 0;
        deaths = 0;
        attacks = 0;
        wallBumps = 0;
        holeTriggers = 0;

        playerShotsFired = 0;
        playerShotsHit = 0;
        enemyShotsFired = 0;
        enemyShotsHit = 0;

        distanceTraveled = 0;
        stationaryMs = 0;
        exposureMs = 0;

        uniqueTilesVisited = 0;
        walkableTilesTotal = 0;

        timeToFirstSwitchMs = -1;
        timeToGemMs = -1;

        currentUsedBytes = 0;
        peakUsedBytes = 0;
        sampleMemory();
    }

    public void recordAttack() { attacks++; }
    public void recordGemCollected() {
        gemsCollected++;
        if (timeToGemMs < 0) timeToGemMs = System.currentTimeMillis() - sessionStartMs;
    }
    public void recordCandyCollected() { candiesCollected++; }
    public void recordEnemyDefeated() { enemiesDefeated++; }
    public void recordHitTaken() { hitsTaken++; }
    public void recordDeath() { deaths++; }
    public void recordWallBump() { wallBumps++; }
    public void recordHoleTrigger() { holeTriggers++; }

    public void recordPlayerShotFired() { playerShotsFired++; }
    public void recordPlayerShotHit() { playerShotsHit++; }
    public void recordEnemyShotFired() { enemyShotsFired++; }
    public void recordEnemyShotHit() { enemyShotsHit++; }

    public void addDistance(double d) { distanceTraveled += Math.max(0, d); }
    public void addStationaryMs(long ms) { stationaryMs += Math.max(0, ms); }
    public void addExposureMs(long ms) { exposureMs += Math.max(0, ms); }

    public void setTileStats(int uniqueVisited, int walkableTotal) {
        uniqueTilesVisited = Math.max(0, uniqueVisited);
        walkableTilesTotal = Math.max(0, walkableTotal);
    }

    public void recordViewSwitch(String newView) {
        if (ended) return;

        if (timeToFirstSwitchMs < 0) {
            timeToFirstSwitchMs = System.currentTimeMillis() - sessionStartMs;
        }

        accumulateViewTime();
        currentView = newView;
        lastViewSwitchMs = System.currentTimeMillis();
        viewSwitches++;
    }

    public void sampleMemory() {
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        currentUsedBytes = used;
        if (used > peakUsedBytes) peakUsedBytes = used;
    }

    public Snapshot snapshot() {
        long now = System.currentTimeMillis();
        long total = now - sessionStartMs;

        long tTop = topViewMs;
        long tSide = sideViewMs;

        if (!ended) {
            long delta = now - lastViewSwitchMs;
            if ("TOP".equals(currentView)) tTop += delta;
            else tSide += delta;
        }

        return new Snapshot(
                total, tTop, tSide,
                viewSwitches, gemsCollected, candiesCollected,
                enemiesDefeated, attacks, hitsTaken, deaths,
                wallBumps, holeTriggers,
                playerShotsFired, playerShotsHit,
                enemyShotsFired, enemyShotsHit,
                distanceTraveled, stationaryMs, exposureMs,
                uniqueTilesVisited, walkableTilesTotal,
                timeToFirstSwitchMs, timeToGemMs,
                currentUsedBytes, peakUsedBytes
        );
    }

    public void endSession(String reason) {
        if (ended) return;
        ended = true;

        accumulateViewTime();
        long endMs = System.currentTimeMillis();
        Snapshot s = snapshot();

        StringBuilder sb = new StringBuilder();
        sb.append("\n==== Halloween JavaFX Session ====\n");
        sb.append("Time: ").append(LocalDateTime.now()).append("\n");
        sb.append("Reason: ").append(reason == null ? "unknown" : reason).append("\n");

        sb.append("TotalPlayMs: ").append(s.totalMs).append("\n");
        sb.append("TopViewMs: ").append(s.topViewMs).append("\n");
        sb.append("SideViewMs: ").append(s.sideViewMs).append("\n");
        sb.append("ViewSwitches: ").append(s.viewSwitches).append("\n");

        sb.append("GemsCollected: ").append(s.gemsCollected).append("\n");
        sb.append("CandiesCollected: ").append(s.candiesCollected).append("\n");
        sb.append("EnemiesDefeated: ").append(s.enemiesDefeated).append("\n");

        sb.append("Attacks: ").append(s.attacks).append("\n");
        sb.append("HitsTaken: ").append(s.hitsTaken).append("\n");
        sb.append("Deaths: ").append(s.deaths).append("\n");

        sb.append("WallBumps: ").append(s.wallBumps).append("\n");
        sb.append("HoleTriggers: ").append(s.holeTriggers).append("\n");

        sb.append("PlayerShotsFired: ").append(s.playerShotsFired).append("\n");
        sb.append("PlayerShotsHit: ").append(s.playerShotsHit).append("\n");
        sb.append("EnemyShotsFired: ").append(s.enemyShotsFired).append("\n");
        sb.append("EnemyShotsHit: ").append(s.enemyShotsHit).append("\n");

        sb.append("PlayerAccuracy: ").append(String.format("%.3f", s.playerAccuracy)).append("\n");
        sb.append("EnemyAccuracy: ").append(String.format("%.3f", s.enemyAccuracy)).append("\n");

        sb.append("DistanceTraveled: ").append(String.format("%.2f", s.distanceTraveled)).append("\n");
        sb.append("StationaryMs: ").append(s.stationaryMs).append("\n");
        sb.append("ExposureMs: ").append(s.exposureMs).append("\n");

        sb.append("UniqueTilesVisited: ").append(s.uniqueTilesVisited).append("\n");
        sb.append("WalkableTilesTotal: ").append(s.walkableTilesTotal).append("\n");
        sb.append("ExplorationRatio: ").append(String.format("%.3f", s.explorationRatio)).append("\n");

        sb.append("TimeToFirstSwitchMs: ").append(s.timeToFirstSwitchMs).append("\n");
        sb.append("TimeToGemMs: ").append(s.timeToGemMs).append("\n");

        sb.append("ActionsPerMinute: ").append(String.format("%.2f", s.actionsPerMinute)).append("\n");

        sb.append("PeakUsedMemoryMB: ").append(String.format("%.2f", s.peakUsedBytes / (1024.0 * 1024.0))).append("\n");
        sb.append("=================================\n");

        writeToFile(sb.toString());
    }

    private void accumulateViewTime() {
        long now = System.currentTimeMillis();
        long delta = now - lastViewSwitchMs;
        if ("TOP".equals(currentView)) topViewMs += delta;
        else sideViewMs += delta;
        lastViewSwitchMs = now;
    }

    private void writeToFile(String text) {
        try {
            Path path = Path.of(System.getProperty("user.home"), "halloween_game_stats.log");
            try (BufferedWriter w = Files.newBufferedWriter(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            )) {
                w.write(text);
            }
        } catch (IOException ignored) {
        }
    }
}
