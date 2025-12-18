package lostinbabuland;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

//to be honest making this was also so much pain but it worked somehow

public final class HudRenderer {
    private final Font monoBig = Font.font("Consolas", 22);
    private final Font monoSmall = Font.font("Consolas", 16);

    public void draw(GraphicsContext g, double w, double h, ViewMode view, int rotDeg, Player player,
                     boolean hasGemKey, boolean exitOpen, StatsTracker stats, int approxWalkable) {
        g.setFill(Color.rgb(0,0,0,0.75));
        g.fillRect(0, 0, w, GameConfig.HUD_H);

        g.setFill(Color.rgb(240,240,240));
        g.setFont(monoBig);

        String topLine = "View: " + view +
                " | Score: " + stats.score() +
                " | HP: " + player.hp +
                " | Gem: " + (hasGemKey ? "YES" : "NO") +
                " | Exit: " + (exitOpen ? "OPEN" : "LOCKED") +
                (view == ViewMode.SIDE ? (" | Turn(Q/E): " + rotDeg + "deg") : " | Turn: -");

        g.fillText(topLine, 18, 34);

        g.setFont(monoSmall);
        g.setFill(Color.rgb(210,210,210));
        String help = "Move: WASD/Arrows   Shoot: SPACE   SwitchView: step on HOLE   Stats: TAB   Panel: F1   Restart: R   Quit: ESC   Help: H";
        g.fillText(help, 18, 62);

        int skill = stats.skillScore(approxWalkable, player.hp);
        String l3 = String.format("t=%ds   fps=%.1f   mem=%.1fMB (peak %.1fMB)   APM=%.1f   skill=%d",
                (int)stats.t(), stats.fps(), stats.memMB(), stats.memPeakMB(), stats.apm(), skill);
        g.fillText(l3, 18, 86);

        String l4 = String.format("shots=%d hits=%d   Eshots=%d Ehits=%d   dist=%.0f   switches=%d   rot=%d   candies=%d   gems=%d   bumps=%d",
                stats.playerShots(), stats.playerHits(),
                stats.enemyShots(), stats.enemyHits(),
                stats.distance(), stats.switches(), stats.rotations(), stats.candies(), stats.gems(), stats.bumps());
        g.fillText(l4, 18, 110);
    }
}