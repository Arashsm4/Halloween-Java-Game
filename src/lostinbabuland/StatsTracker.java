package lostinbabuland;

import java.util.HashSet;
import java.util.Set;

public final class StatsTracker {
    private float timeSec = 0f;

    // REAL render fps FINALLLLLLY
    private float renderFpsSmooth = 60f;
    private float memPeakMB = 0f;

    private int score = 0;

    private int playerShots = 0;
    private int playerHits = 0;
    private int enemyShots = 0;
    private int enemyHits = 0;

    private int switches = 0;
    private int rotations = 0;

    private int candies = 0;
    private int gems = 0;

    private int bumps = 0;

    private float dist = 0f;
    private float enemyMoveDist = 0f;

    private float idleTime = 0f;
    private float threatTime = 0f;

    private float actionWindow = 0f;
    private int actionsInWindow = 0;

    private boolean threatened = false;
    private float threatStart = 0f;
    private float reactionSum = 0f;
    private int reactionN = 0;

    private final Set<Integer> visited = new HashSet<>();

    public void renderFrame(float frameDt) {
        if (frameDt <= 1e-6f) return;
        float inst = 1f / frameDt;
        renderFpsSmooth = MathUtil.lerp(renderFpsSmooth, inst, 0.08f);

        float mem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024f * 1024f);
        memPeakMB = Math.max(memPeakMB, mem);
    }

    public void simStep(float dt, float playerSpeedNow, boolean inThreat, int tileKey, int approxWalkable) {
        timeSec += dt;

        if (playerSpeedNow < 8f) idleTime += dt;
        if (inThreat) threatTime += dt;

        if (!threatened && inThreat) {
            threatened = true;
            threatStart = timeSec;
        }
        if (threatened && !inThreat) {
            threatened = false;
        }

        visited.add(tileKey);

        actionWindow += dt;
        if (actionWindow > 60f) {
            actionWindow -= 60f;
            actionsInWindow = 0;
        }
    }

    public float t() { return timeSec; }
    public float fps() { return renderFpsSmooth; }
    public float memMB() { return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024f*1024f); }
    public float memPeakMB() { return memPeakMB; }

    public void addScore(int v) { score += v; }
    public int score() { return score; }

    public void playerShot() { playerShots++; actionsInWindow++; }
    public void playerHitEnemy() { playerHits++; actionsInWindow++; if (threatened) react(); }
    public void enemyShot() { enemyShots++; }
    public void playerGotHit() { enemyHits++; actionsInWindow++; }

    public void collectedCandy() { candies++; score += 5; actionsInWindow++; if (threatened) react(); }
    public void collectedGem() { gems++; score += 20; actionsInWindow++; if (threatened) react(); }

    public int candies() { return candies; }
    public int gems() { return gems; }

    public void switchedView() { switches++; actionsInWindow++; if (threatened) react(); }
    public void rotated() { rotations++; actionsInWindow++; if (threatened) react(); }

    public int switches() { return switches; }
    public int rotations() { return rotations; }

    public void bump() { bumps++; }
    public int bumps() { return bumps; }

    public void addDistance(float d) { dist += d; }
    public float distance() { return dist; }

    public void enemyMoveDistance(float d) { enemyMoveDist += d; }

    public float apm() {
        float w = Math.max(1f, Math.min(60f, actionWindow));
        return actionsInWindow * (60f / w);
    }

    public float accuracy() { return playerShots == 0 ? 0f : (float)playerHits / (float)playerShots; }

    public float idlePct() { return timeSec <= 1e-6f ? 0f : (idleTime / timeSec); }
    public float threatPct() { return timeSec <= 1e-6f ? 0f : (threatTime / timeSec); }

    public float explorePct(int approxWalkable) {
        if (approxWalkable <= 0) return 0f;
        return Math.min(1f, visited.size() / (float)approxWalkable);
    }

    public float reactionAvg() { return reactionN == 0 ? 0f : reactionSum / reactionN; }

    public int skillScore(int approxWalkable, int hp) {
        float acc = accuracy();
        float exp = explorePct(approxWalkable);
        float t = threatPct();

        float skill = 0f;
        skill += acc * 55f;
        skill += exp * 35f;
        skill += (hp / 3f) * 20f;
        skill += (1f - Math.min(1f, t)) * 10f;
        return Math.max(0, Math.min(99, Math.round(skill)));
    }

    private void react() {
        float rt = Math.max(0f, timeSec - threatStart);
        reactionSum += rt;
        reactionN++;
        threatened = false;
    }

    // getters used by HUD
    public int playerShots() { return playerShots; }
    public int playerHits() { return playerHits; }
    public int enemyShots() { return enemyShots; }
    public int enemyHits() { return enemyHits; }
}