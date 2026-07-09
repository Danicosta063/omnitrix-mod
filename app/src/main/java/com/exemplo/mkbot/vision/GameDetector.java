package com.exemplo.mkbot.vision;

import android.graphics.Bitmap;
import android.graphics.Color;

public class GameDetector {

    public enum Result { UNKNOWN, WIN, LOSE }

    public static class GameState {
        public int p1Hp;
        public int oppHp;
        public int motion;
        public boolean hitFlash;
        public Result result = Result.UNKNOWN;
    }

    private final float P1_HP_X_START = 0.02f;
    private final float P1_HP_X_END   = 0.38f;
    private final float P1_HP_Y       = 0.06f;
    private final float OPP_HP_X_START = 0.62f;
    private final float OPP_HP_X_END   = 0.98f;
    private final float OPP_HP_Y       = 0.06f;

    // Tolerancia aumentada pra pegar mais variacoes de cor
    private final int HP_TOL = 120;

    private final float RESULT_X_START = 0.30f;
    private final float RESULT_X_END   = 0.70f;
    private final float RESULT_Y_START = 0.35f;
    private final float RESULT_Y_END   = 0.65f;

    private final float HIT_X_START = 0.20f;
    private final float HIT_X_END   = 0.80f;
    private final float HIT_Y_START = 0.30f;
    private final float HIT_Y_END   = 0.80f;

    private Bitmap previousFrame = null;

    public GameState detect(Bitmap frame, int screenW, int screenH) {
        GameState s = new GameState();

        s.p1Hp = readHpBar(frame, screenW, screenH,
                P1_HP_X_START, P1_HP_X_END, P1_HP_Y);
        s.oppHp = readHpBar(frame, screenW, screenH,
                OPP_HP_X_START, OPP_HP_X_END, OPP_HP_Y);

        s.motion = detectMotion(frame, screenW, screenH);
        s.hitFlash = detectHitFlash(frame, screenW, screenH);
        s.result = detectResult(frame, screenW, screenH);

        if (previousFrame != null && !previousFrame.equals(frame)) {
            previousFrame.recycle();
        }
        previousFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

        return s;
    }

    // Detecta barra de vida: conta pixels "brilhantes e coloridos"
    // (nao precisa de cor exata - qualquer pixel vivo na barra conta)
    private int readHpBar(Bitmap frame, int screenW, int screenH,
                          float xStart, float xEnd, float yFrac) {
        int y = (int) (yFrac * screenH);
        int xStartPx = (int) (xStart * screenW);
        int xEndPx = (int) (xEnd * screenW);
        if (y < 0 || y >= frame.getHeight()) return 0;
        if (xEndPx > frame.getWidth()) xEndPx = frame.getWidth();
        if (xStartPx < 0) xStartPx = 0;

        int total = xEndPx - xStartPx;
        if (total <= 0) return 0;
        int matches = 0;
        for (int x = xStartPx; x < xEndPx; x++) {
            int pixel = frame.getPixel(x, y);
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            // Pixel "vivo" = brilhante E colorido (nao preto, nao cinza)
            int brightness = (r + g + b) / 3;
            int maxColor = Math.max(r, Math.max(g, b));
            int minColor = Math.min(r, Math.min(g, b));
            boolean isBright = brightness > 80;
            boolean isColored = (maxColor - minColor) > 30;
            if (isBright && isColored) {
                matches++;
            }
        }
        return (int) (100f * matches / total);
    }

    private int detectMotion(Bitmap frame, int screenW, int screenH) {
        if (previousFrame == null) return 0;
        if (previousFrame.getWidth() != frame.getWidth() ||
            previousFrame.getHeight() != frame.getHeight()) return 0;

        int xStart = (int) (0.30f * screenW);
        int xEnd   = (int) (0.70f * screenW);
        int yStart = (int) (0.30f * screenH);
        int yEnd   = (int) (0.85f * screenH);

        int changed = 0;
        int sampled = 0;
        int step = 8;

        for (int y = yStart; y < yEnd; y += step) {
            for (int x = xStart; x < xEnd; x += step) {
                int c1 = frame.getPixel(x, y);
                int c2 = previousFrame.getPixel(x, y);
                int dr = Math.abs(Color.red(c1) - Color.red(c2));
                int dg = Math.abs(Color.green(c1) - Color.green(c2));
                int db = Math.abs(Color.blue(c1) - Color.blue(c2));
                if (dr + dg + db > 60) changed++;
                sampled++;
            }
        }
        if (sampled == 0) return 0;
        int motion = (int) (100f * changed / sampled);
        if (motion > 100) motion = 100;
        return motion;
    }

    private boolean detectHitFlash(Bitmap frame, int screenW, int screenH) {
        int xStart = (int) (HIT_X_START * screenW);
        int xEnd   = (int) (HIT_X_END * screenW);
        int yStart = (int) (HIT_Y_START * screenH);
        int yEnd   = (int) (HIT_Y_END * screenH);
        if (xStart < 0 || xEnd > frame.getWidth() ||
            yStart < 0 || yEnd > frame.getHeight()) return false;

        int brightCount = 0;
        int threshold = 240;
        int step = 6;

        for (int y = yStart; y < yEnd; y += step) {
            for (int x = xStart; x < xEnd; x += step) {
                int pixel = frame.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                if (r > threshold && g > threshold && b > threshold) {
                    brightCount++;
                }
            }
        }
        int total = ((xEnd - xStart) / step) * ((yEnd - yStart) / step);
        if (total == 0) return false;
        return brightCount > (total / 30);
    }

    private Result detectResult(Bitmap frame, int screenW, int screenH) {
        int xStart = (int) (RESULT_X_START * screenW);
        int xEnd   = (int) (RESULT_X_END * screenW);
        int yStart = (int) (RESULT_Y_START * screenH);
        int yEnd   = (int) (RESULT_Y_END * screenH);
        if (xStart < 0 || xEnd > frame.getWidth() ||
            yStart < 0 || yEnd > frame.getHeight()) return Result.UNKNOWN;

        int brightCount = 0;
        int threshold = 200;
        int step = 4;

        for (int y = yStart; y < yEnd; y += step) {
            for (int x = xStart; x < xEnd; x += step) {
                int pixel = frame.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                if (r > threshold && g > threshold && b > threshold) {
                    brightCount++;
                }
            }
        }
        int total = ((xEnd - xStart) / step) * ((yEnd - yStart) / step);
        if (total == 0) return Result.UNKNOWN;
        if (brightCount > (total / 7)) {
            return Result.UNKNOWN;
        }
        return Result.UNKNOWN;
    }
}
