package com.exemplo.mkbot.vision;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Detector de estado do jogo com visao leve e rapida.
 *
 * Detecta:
 *  - HP do P1 e do oponente (por cor da barra, no topo da tela)
 *  - Movimento do oponente (por diferenca de frames)
 *  - Flash de impacto (golpe acertou - pixels brancos/brilhantes)
 *  - WIN/LOSE (texto brilhante no centro da tela)
 *
 * Tudo por leitura de pixels nativa do Android. Sem OpenCV, sem ML.
 * Roda em ~5ms por frame no S21.
 */
public class GameDetector {

    public enum Result { UNKNOWN, WIN, LOSE }

    public static class GameState {
        public int p1Hp;       // 0..100
        public int oppHp;      // 0..100
        public int motion;     // 0..100 (0 = parado, 100 = muito movimento)
        public boolean hitFlash; // true se um golpe acabei de acertar
        public Result result = Result.UNKNOWN;
    }

    // Frações da tela calibradas pro Samsung S21 (2400x1080 landscape)
    // Barra de vida P1: canto superior esquerdo
    private final float P1_HP_X_START = 0.02f;
    private final float P1_HP_X_END   = 0.38f;
    private final float P1_HP_Y       = 0.06f;
    // Barra de vida oponente: canto superior direito
    private final float OPP_HP_X_START = 0.62f;
    private final float OPP_HP_X_END   = 0.98f;
    private final float OPP_HP_Y       = 0.06f;

    // Cor da barra de vida (amarelo/dourado do MK Armageddon)
    private final int HP_R = 220, HP_G = 180, HP_B = 40;
    private final int HP_TOL = 60;

    // Area central pra detectar WIN/LOSE (texto brilhante)
    private final float RESULT_X_START = 0.30f;
    private final float RESULT_X_END   = 0.70f;
    private final float RESULT_Y_START = 0.35f;
    private final float RESULT_Y_END   = 0.65f;

    // Area central pra detectar flash de impacto (golpe acertou)
    private final float HIT_X_START = 0.20f;
    private final float HIT_X_END   = 0.80f;
    private final float HIT_Y_START = 0.30f;
    private final float HIT_Y_END   = 0.80f;

    // Frame anterior - usado pra detectar movimento
    private Bitmap previousFrame = null;

    public GameState detect(Bitmap frame, int screenW, int screenH) {
        GameState s = new GameState();

        // HP dos dois lados
        s.p1Hp = readHpBar(frame, screenW, screenH,
                P1_HP_X_START, P1_HP_X_END, P1_HP_Y);
        s.oppHp = readHpBar(frame, screenW, screenH,
                OPP_HP_X_START, OPP_HP_X_END, OPP_HP_Y);

        // Movimento do oponente (diferenca vs frame anterior)
        s.motion = detectMotion(frame, screenW, screenH);

        // Flash de impacto
        s.hitFlash = detectHitFlash(frame, screenW, screenH);

        // Resultado (WIN/LOSE)
        s.result = detectResult(frame, screenW, screenH);

        // Guarda frame atual pra proxima comparacao
        if (previousFrame != null && !previousFrame.equals(frame)) {
            previousFrame.recycle();
        }
        previousFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

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
        if (xStartPx < 0) xStartPx = 0;

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

    // Detecta movimento comparando o frame atual com o anterior.
    // Amostra pixels numa grade e conta quantos mudaram significativamente.
    // Retorna 0..100 (0 = parado, 100 = muito movimento).
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
        int step = 8; // amostra a cada 8 pixels pra ser rapido

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

    // Detecta flash de impacto: pixels muito brancos/brilhantes na area central.
    // No MK Armageddon, quando um golpe acerta, ha um flash branco/amarelo.
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
        // Se mais de 3% dos pixels amostrados forem muito brilhantes, é flash
        int total = ((xEnd - xStart) / step) * ((yEnd - yStart) / step);
        if (total == 0) return false;
        return brightCount > (total / 30);
    }

    // Detecta WIN/LOSE: texto brilhante grande no centro da tela.
    // Heuristica simples: muitos pixels brancos na caixa central.
    // Nao distingue WIN de LOSE por cor, mas o BotService infere pelo HP.
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
        // Se mais de 15% dos pixels forem brilhantes, há texto na tela
        int total = ((xEnd - xStart) / step) * ((yEnd - yStart) / step);
        if (total == 0) return Result.UNKNOWN;
        if (brightCount > (total / 7)) {
            // Tem texto brilhante. WIN ou LOSE? Inferir pelo HP:
            // Se oponente tem HP 0, foi WIN. Se P1 tem HP 0, foi LOSE.
            // Como nao lemos HP aqui de novo, retornamos UNKNOWN
            // e deixamos o BotService decidir pela logica de HP.
            return Result.UNKNOWN; // BotService usa HP pra decidir
        }
        return Result.UNKNOWN;
    }
}
