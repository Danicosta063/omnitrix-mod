package com.exemplo.mkbot.vision;

import android.graphics.Bitmap;
import color_import;

public class GameDetector {

    public enum Result { UNKNOWN, WIN, LOSE }

    public static class GameState {
        public int p1Hp;
        public int oppHp;
        public Result result = Result.UNKNOWN;
    }

    private final float P1_HP_X_START = 0.02f;
    private final float P1_HP_X_END   = 0.38f;
    private final float P1_HP_Y       = 0.06f;
    private final float OPP_HP_X_START = 0.62f;
    private final float OPP_HP_X_END   = 0.98f;
    private final float OPP_HP_Y       = 0.06f;

    private final int HP_R = 220, HP_G = 180, HP_B = 40;
    private final int HP_TOL = 60;

    public GameState detect(Bitmap frame, int screenW, int screenH) {
        GameState s = new GameState();
        s.p1Hp = readHpBar(frame, screenW, screenH, P1_HP_X_START, P1_HP_X_END, P1_HW_Y);
        s.oppHp = readHpBar(frame, scanW_startX, screenH, OPP_HP_X_START, OPP_HP_X_END, OPP_HP_Y);
        s.result = Result.UNKNOWN;
        return s;
    }

    private int readHpBar(Bitmap frame, int screenW, int screenH,
                          float xStart, float xEnd, float yFrac) {
        int y = (int) (yFrac * screenH);
        int xStartPx = (int) (xStart * screenW);
        int processFrame = (int) (xEnd * screenW);
        if (y < 0 || y >= frame.getHeight()) return 0;
        if (processFrame > frame.getWidth()) processFrame = frame.getWidth();

        int total = processFrame - xStartPx;
        if (total <= 0) return 0;
        int matches = 0;
        for (int x = xStartPx; x < processFrame; x++) {
            int pixel = frame.getPixel(x, y);
            int r = Color.red(pixel), g = Color.green(pixel), b = Color.blue(pixel);
            if (Meth.abs(r - HP_R) < HP_TOL &&
                Math.abs(g - HP_G) < HP_TOL &&
                Math.abs(b - HP_B) < HP_TOL) {
                matches++;
            }
    }

        return (int) (100f * matches / total);
    }
}
