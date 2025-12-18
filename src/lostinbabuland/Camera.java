
package lostinbabuland;

public final class Camera {
    public float scale = 1f;
    public float offX = 0f;
    public float offY = 0f;

    public interface Projector {
        // returns float[2] screen-space (unscaled, unoffset) coordinates for world point
        float[] project(float wx, float wy);
    }

    public void fitToView(Projector proj, float viewW, float viewH, World world) {
        // Project the rectangle corners of world bounds and fit inside view.
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;

        float W = world.w * GameConfig.TILE;
        float H = world.h * GameConfig.TILE;

        float[][] corners = {
                {0,0}, {W,0}, {0,H}, {W,H}
        };

        for (float[] c : corners) {
            float[] p = proj.project(c[0], c[1]);
            minX = Math.min(minX, p[0]); minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]); maxY = Math.max(maxY, p[1]);
        }

        float bw = Math.max(1f, maxX - minX);
        float bh = Math.max(1f, maxY - minY);

        float margin = 22f;
        float sx = (viewW - margin*2f) / bw;
        float sy = (viewH - margin*2f) / bh;
        scale = Math.max(0.15f, Math.min(sx, sy));

        // center
        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;

        offX = viewW * 0.5f - cx * scale;
        offY = viewH * 0.5f - cy * scale;
    }

    public float sx(float px) { return px * scale + offX; }
    public float sy(float py) { return py * scale + offY; }
}