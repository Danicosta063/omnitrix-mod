package com.exemplo.mkbot.vision;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Detecta o estado do jogo a partir de um screenshot.
 * MK Armageddon (PS2 emulado) tem HUD fixo:
 *   - Barra de vida P1: canto superior esquerdo, amarela/dourada
 *   - Barra de vida oponente: canto superior direito
 *   - "YOU WIN" / "YOU LOSE" no centro ao final do round
 *
 * Deteccao por cor + posicao. Simples mas funcional.
 */
public class GameDetector {

    public enum Result { UNKNOWN, WIN, LOSE }

    public static class GameState {
        public int p1Hp;       // 0..100
        public int oppHp;      // 0..100
        public Result result = Result.UNKNOWN;
    }

    // Frações da tela — AJUSTE conforme seu aparelho
    private final float P1_HP_X_START = 0.02f;
    private final float P1_HP_X_END   = 0.38f;
    private final float P1_HP_Y       = 0.06f;
    private final float OPP_HP_X_START = 0.62f;
    private final float OPP_HP_X_END   = 0.98f;
    private final float OPP_HP_Y       = 0.06f;

    // Cor da barra de vida (amarelo/dourado MK Armageddon)
    private final int HP_R = 220, HP_G = 180, HP_B = 40;
    private final int HP_TOL = 60;

    public GameState detect(Bitmap frame, int screenW, int screenH) {
        GameState s = new GameState();
        s.p1Hp = readHpBar(frame, screenW, screenH,
                P1_HP_X_START, P1_HP_X_END, P1_HP_Y);
        s.oppHp = readHpBar(frame, screenW, screenH,
                OPP_HP_X_START, OPP_HP_X_END, OPP_HP_Y);
        s.result = Result.UNKNOWN;
        return s;
    }

    // Conta pixels da cor da barra de vida numa faixa horizontal.
    // Retorna porcentagem preenchida (0..100).
    private int readHpBar(Bitmap frame, int screenW, int screenH,
                          float xStart, float xEnd, float yFrac) {
        int y = (int) (yFrac * screenH);
        int xStartPx = (int) (xStart * screenW);
        int xEndPx = (int) (xEnd * screenW);
        if (y < 0 || y >= frame.getHeight()) return 0;
        if (xEndPx > frame.getWidth()) xEndPx = frame.getWidth();

        int total = xEndPx - xStartPx;
        if (total <= 0) return 0;
        int matches = 0;
        for (int x = xStartPx; x < xEndPx; x++) {
            int pixel = frame.getPixel(x, y);
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            if (Math.abs(r - HP_R) < HP_TOL &&
                Math.abs(g - HP_G) < HP_TOL &&
                Math.abs(b - HP_B) < HP_TOL) {
                matches++;
            }
        }
        return (int) (100f * matches / total);
    }
}
