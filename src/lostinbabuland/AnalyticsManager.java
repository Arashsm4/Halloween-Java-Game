package lostinbabuland;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

public class AnalyticsManager {

    private long sessionStartNs;
    private long lastFrameNs;

    private double elapsedSec;
    private double fps;
    private int frames;

    private double memMB;
    private double peakMemMB;

    private String currentView = "TOP";
    private double topTimeSec;
    private double sideTimeSec;

    private int switches;
    private int rotations;

    private int playerShots;
    private int playerHits;
    private int enemyShots;
    private int enemyHits;

    private int candies;
    private int gems;

    private int bumps;
    private double dist;

    private int actions;     // for APM
    private double idleSec;  // time without movement or shooting

    private boolean underThreat;
    private double threatSec;

    private double threatStartSec = -1;
    private double reactionSum = 0;
    private int reactionCount = 0;

    private int mapW, mapH;
    private boolean[] visited;
    private int visitedCount;

    private double lastX = Double.NaN, lastY = Double.NaN;

    public void resetSession(int mapW, int mapH) {
        this.mapW = mapW;
        this.mapH = mapH;
        this.visited = new boolean[mapW * mapH];
        this.visitedCount = 0;

        sessionStartNs = System.nanoTime();
        lastFrameNs = sessionStartNs;

        elapsedSec = 0;
        fps = 0;
        frames = 0;

        memMB = 0;
        peakMemMB = 0;

        currentView = "TOP";
        topTimeSec = 0;
        sideTimeSec = 0;

        switches = 0;
        rotations = 0;

        playerShots = 0;
        playerHits = 0;
        enemyShots = 0;
        enemyHits = 0;

        candies = 0;
        gems = 0;

        bumps = 0;
        dist = 0;

        actions = 0;
        idleSec = 0;

        underThreat = false;
        threatSec = 0;

        threatStartSec = -1;
        reactionSum = 0;
        reactionCount = 0;

        lastX = Double.NaN;
        lastY = Double.NaN;
    }

    public void onStart() {
        sessionStartNs = System.nanoTime();
        lastFrameNs = sessionStartNs;
    }

    public void onFrame(double dt) {
        frames++;
        elapsedSec += dt;

        // fps (smoothed)
        double instFps = (dt > 1e-9) ? (1.0 / dt) : 0;
        fps = fps == 0 ? instFps : (fps * 0.93 + instFps * 0.07);

        // memory
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        memMB = used / 1024.0 / 1024.0;
        if (memMB > peakMemMB) peakMemMB = memMB;
    }

    public void onTick(double dt, String view, double px, double py, boolean moving, boolean threatNow) {
        currentView = view;

        if ("TOP".equals(view)) topTimeSec += dt;
        else sideTimeSec += dt;

        if (!moving && playerShots == 0 && elapsedSec < 2) {
            // ignore first seconds
        }

        if (!moving && !threatNow) idleSec += dt;

        if (threatNow) {
            threatSec += dt;
            if (!underThreat) {
                // threat just started
                threatStartSec = elapsedSec;
            }
        }
        underThreat = threatNow;

        // mark visited tile (coarse)
        int tx = (int)Math.floor(px);
        int ty = (int)Math.floor(py);
        markVisited(tx, ty);

        // if threat active and player reacts by moving or shooting later, record reaction time
        // movement reaction is detected externally via recordMove; shooting via recordPlayerShot
        if (!threatNow) threatStartSec = -1;
    }

    public void recordMove(double dt, double px, double py, int tx, int ty) {
        markVisited(tx, ty);
        actions++; // movement counts as action in APM (coarse)
        if (threatStartSec >= 0) {
            double rt = elapsedSec - threatStartSec;
            if (rt >= 0.08 && rt <= 5.0) {
                reactionSum += rt;
                reactionCount++;
                threatStartSec = -1;
            }
        }
    }

    public void recordDistance(double d) {
        dist += d;
    }

    public void recordSwitch(String newView) {
        switches++;
        actions++;
    }

    public void recordRotate() {
        rotations++;
        actions++;
    }

    public void recordPlayerShot() {
        playerShots++;
        actions++;
        if (threatStartSec >= 0) {
            double rt = elapsedSec - threatStartSec;
            if (rt >= 0.08 && rt <= 5.0) {
                reactionSum += rt;
                reactionCount++;
                threatStartSec = -1;
            }
        }
    }

    public void recordEnemyShot() {
        enemyShots++;
    }

    public void recordPlayerHit() {
        playerHits++;
    }

    public void recordEnemyHit() {
        enemyHits++;
    }

    public void recordCandy() {
        candies++;
        actions++;
    }

    public void recordGem() {
        gems++;
        actions++;
    }

    public void recordBump() {
        bumps++;
    }

    public void finish(boolean won) {
        // no-op placeholder for future (could compute final aggregates)
    }

    public String[] hudLines(int score, int hp, int enemiesAlive, int collectiblesLeft, int camRot) {
        double apm = elapsedSec > 1 ? (actions / (elapsedSec / 60.0)) : 0;

        double explore = (mapW * mapH > 0) ? (visitedCount * 100.0 / (double)(mapW * mapH)) : 0;

        double pAcc = (playerShots > 0) ? (playerHits * 100.0 / playerShots) : 0;
        double eAcc = (enemyShots > 0) ? (enemyHits * 100.0 / enemyShots) : 0;

        double topPct = (elapsedSec > 1e-6) ? (topTimeSec * 100.0 / elapsedSec) : 0;
        double sidePct = (elapsedSec > 1e-6) ? (sideTimeSec * 100.0 / elapsedSec) : 0;

        double threatPct = (elapsedSec > 1e-6) ? (threatSec * 100.0 / elapsedSec) : 0;
        double idlePct = (elapsedSec > 1e-6) ? (idleSec * 100.0 / elapsedSec) : 0;

        double avgRt = (reactionCount > 0) ? (reactionSum / reactionCount) : -1;

        // Simple “skill” heuristic (bounded-ish)
        double skill = 0;
        skill += clamp01(pAcc / 100.0) * 40;
        skill += clamp01(explore / 100.0) * 25;
        skill += clamp01((apm / 60.0)) * 20;
        skill += clamp01((1.0 - idlePct / 100.0)) * 15;
        int skillInt = (int)Math.round(skill);

        String l1 = String.format(Locale.US,
                "t=%.0fs  fps=%.1f  mem=%.1fMB (peak %.1fMB)  APM=%.1f  skill=%d",
                elapsedSec, fps, memMB, peakMemMB, apm, skillInt);

        String l2 = String.format(Locale.US,
                "P shots %d/%d (%.0f%%) | E shots %d/%d (%.0f%%) | explore %.0f%% | idle %.0f%% | threat %.0f%%",
                playerHits, playerShots, pAcc,
                enemyHits, enemyShots, eAcc,
                explore, idlePct, threatPct);

        String l3 = String.format(Locale.US,
                "dist=%.0f  switches=%d  rot=%d  candies=%d  gems=%d  bumps=%d  TOP/SIDE=%.0f/%.0f%%  rt=%s",
                dist, switches, rotations, candies, gems, bumps,
                topPct, sidePct,
                (avgRt < 0 ? "-" : String.format(Locale.US, "%.2fs", avgRt))
        );

        return new String[]{l1, l2, l3};
    }

    public String[] panelLines() {
        double pAcc = (playerShots > 0) ? (playerHits * 100.0 / playerShots) : 0;
        double eAcc = (enemyShots > 0) ? (enemyHits * 100.0 / enemyShots) : 0;
        double avgRt = (reactionCount > 0) ? (reactionSum / reactionCount) : -1;

        return new String[] {
                String.format(Locale.US, "Shots: P=%d (hit %d, %.0f%%)  E=%d (hit %d, %.0f%%)", playerShots, playerHits, pAcc, enemyShots, enemyHits, eAcc),
                String.format(Locale.US, "Switches=%d  Rotations=%d  Bumps=%d", switches, rotations, bumps),
                String.format(Locale.US, "Candy=%d  Gems=%d  Distance=%.0f", candies, gems, dist),
                String.format(Locale.US, "Threat=%.0fs  Idle=%.0fs  AvgRT=%s", threatSec, idleSec, (avgRt < 0 ? "-" : String.format(Locale.US, "%.2fs", avgRt))),
                String.format(Locale.US, "ExploredTiles=%d/%d", visitedCount, mapW * mapH),
        };
    }

    public void saveSession(Path file, boolean won) {
        String header = "timestamp,won,elapsed_sec,fps_avg,mem_peak_mb,view_top_sec,view_side_sec,switches,rotations," +
                "player_shots,player_hits,enemy_shots,enemy_hits,candies,gems,bumps,distance,actions,apm,threat_sec,idle_sec,explore_pct,avg_reaction_sec\n";

        double apm = elapsedSec > 1 ? (actions / (elapsedSec / 60.0)) : 0;
        double explorePct = (mapW * mapH > 0) ? (visitedCount * 100.0 / (double)(mapW * mapH)) : 0;
        double avgRt = (reactionCount > 0) ? (reactionSum / reactionCount) : -1;

        String row = String.format(Locale.US,
                "%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f,%d,%.3f,%.3f,%.3f,%.2f,%s\n",
                System.currentTimeMillis(),
                won ? "true" : "false",
                elapsedSec,
                fps,
                peakMemMB,
                topTimeSec,
                sideTimeSec,
                switches,
                rotations,
                playerShots,
                playerHits,
                enemyShots,
                enemyHits,
                candies,
                gems,
                bumps,
                dist,
                actions,
                apm,
                threatSec,
                idleSec,
                explorePct,
                (avgRt < 0 ? "" : String.format(Locale.US, "%.3f", avgRt))
        );

        try {
            boolean exists = Files.exists(file);
            if (!exists) Files.writeString(file, header, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            Files.writeString(file, row, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private void markVisited(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= mapW || ty >= mapH) return;
        int idx = ty * mapW + tx;
        if (!visited[idx]) {
            visited[idx] = true;
            visitedCount++;
        }
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
