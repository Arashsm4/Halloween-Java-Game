
package lostinbabuland;

public final class Projection {
    private Projection(){}

    public static Camera.Projector top(World world) {
        return (wx, wy) -> new float[]{ wx, wy };
    }

    public static Camera.Projector sideIso(World world, int rotDeg) {
        float cx = (world.w * GameConfig.TILE) * 0.5f;
        float cy = (world.h * GameConfig.TILE) * 0.5f;

        double a = Math.toRadians(rotDeg);
        float ca = (float)Math.cos(a);
        float sa = (float)Math.sin(a);

        return (wx, wy) -> {
            float x = wx - cx;
            float y = wy - cy;

            // rotate on plane
            float rx = x * ca - y * sa;
            float ry = x * sa + y * ca;

            // isometric projection
            float isoX = (rx - ry) * 0.85f;
            float isoY = (rx + ry) * 0.42f;

            // return around 0,0 space; Camera will fit+center
            return new float[]{ isoX, isoY };
        };
    }
}