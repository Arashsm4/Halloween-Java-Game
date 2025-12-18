package lostinbabuland;

//I seprated into different parts so it should now both function better and easier to fix in case there was a problem

public final class GameConfig {
    private GameConfig() {}

    public static final int WINDOW_W = 1280;
    public static final int WINDOW_H = 720;

    public static final int HUD_H = 120;

    public static final int TILE = 28; // world tile size in pixels

    public static final int WORLD_W = 72; // tiles
    public static final int WORLD_H = 46; // tiles

    public static final float PLAYER_RADIUS = 11f;
    public static final float ENEMY_RADIUS = 11f;
    public static final float BULLET_RADIUS = 3.5f;

    public static final float PLAYER_SPEED = 190f;
    public static final float ENEMY_SPEED = 130f;
    public static final float BULLET_SPEED = 420f;

    public static final float PLAYER_FIRE_COOLDOWN = 0.16f;
    public static final float ENEMY_FIRE_COOLDOWN = 0.9f;

    // 60 Hz fixed
    public static final float FIXED_DT = 1f / 60f;

    // only clamp huge stalls (alt-tab etc.), not normal slow frames
    public static final float MAX_FRAME_DT = 0.25f;

    // max sim steps per rendered frame
    public static final int MAX_STEPS_PER_FRAME = 8;

    public static final int INITIAL_HP = 3;

    public static final int ENEMY_COUNT_MIN = 7;
    public static final int ENEMY_COUNT_MAX = 11;

    public static final int CANDY_COUNT = 18;
    public static final int GEM_COUNT = 8;
    public static final int HOLE_COUNT = 4;

    public static final float THREAT_RADIUS = 210f;

    // Rotation step for SIDE view it is genius hehehe
    public static final int ROT_STEP_DEG = 15;
    public static final int ROT_MIN = 0;
    public static final int ROT_MAX = 345;
}