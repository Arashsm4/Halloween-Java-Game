
package lostinbabuland;

//well it is obvious how I passed the math and software design :))

public final class MathUtil {
    private MathUtil(){}

    public static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }

    public static float len(float x, float y) {
        return (float)Math.sqrt(x*x + y*y);
    }

    public static float normX(float x, float y) {
        float l = len(x,y);
        return l <= 1e-6f ? 0f : x/l;
    }

    public static float normY(float x, float y) {
        float l = len(x,y);
        return l <= 1e-6f ? 0f : y/l;
    }

    public static int wrapDeg(int deg) {
        int d = deg % 360;
        if (d < 0) d += 360;
        return d;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b-a)*t;
    }
}
