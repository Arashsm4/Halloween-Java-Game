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

        public final long currentUsedBytes;
        public final long peakUsedBytes;

        private Snapshot(long totalMs, long topViewMs, long sideViewMs,
                         int viewSwitches, int gemsCollected, int candiesCollected,
                         int enemiesDefeated, int attacks, int hitsTaken, int deaths,
                         int wallBumps, int holeTriggers,
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
            this.currentUsedBytes = currentUsedBytes;
            this.peakUsedBytes = peakUsedBytes;
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

        currentUsedBytes = 0;
        peakUsedBytes = 0;
        sampleMemory();
    }

    public void recordAttack() { attacks++; }
    public void recordGemCollected() { gemsCollected++; }
    public void recordCandyCollected() { candiesCollected++; }
    public void recordEnemyDefeated() { enemiesDefeated++; }
    public void recordHitTaken() { hitsTaken++; }
    public void recordDeath() { deaths++; }
    public void recordWallBump() { wallBumps++; }
    public void recordHoleTrigger() { holeTriggers++; }

    public void recordViewSwitch(String newView) {
        if (ended) return;
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
                currentUsedBytes, peakUsedBytes
        );
    }

    public void endSession(String reason) {
        if (ended) return;
        ended = true;

        accumulateViewTime();
        long endMs = System.currentTimeMillis();
        long totalMs = endMs - sessionStartMs;

        StringBuilder sb = new StringBuilder();
        sb.append("\n==== Halloween JavaFX Session ====\n");
        sb.append("Time: ").append(LocalDateTime.now()).append("\n");
        sb.append("Reason: ").append(reason == null ? "unknown" : reason).append("\n");
        sb.append("TotalPlayMs: ").append(totalMs).append("\n");
        sb.append("TopViewMs: ").append(topViewMs).append("\n");
        sb.append("SideViewMs: ").append(sideViewMs).append("\n");
        sb.append("ViewSwitches: ").append(viewSwitches).append("\n");
        sb.append("GemsCollected: ").append(gemsCollected).append("\n");
        sb.append("CandiesCollected: ").append(candiesCollected).append("\n");
        sb.append("EnemiesDefeated: ").append(enemiesDefeated).append("\n");
        sb.append("Attacks: ").append(attacks).append("\n");
        sb.append("HitsTaken: ").append(hitsTaken).append("\n");
        sb.append("Deaths: ").append(deaths).append("\n");
        sb.append("WallBumps: ").append(wallBumps).append("\n");
        sb.append("HoleTriggers: ").append(holeTriggers).append("\n");
        sb.append("PeakUsedMemoryMB: ").append(String.format("%.2f", peakUsedBytes / (1024.0 * 1024.0))).append("\n");
        sb.append("=================================\n");

        writeToFile(sb.toString());
    }

    private void accumulateViewTime() {
        long now = System.currentTimeMillis();
        long delta = now - lastViewSwitchMs;
        if ("TOP".equals(currentView)) topViewMs += delta;
        else sideViewMs += delta;
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
