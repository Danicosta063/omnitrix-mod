package com.exemplo.mkbot.vision;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * Detector de estado do jogo baseado em visao computacional simples.
 * Detecta:
 * - Barras de vida (HP) do P1 e do oponente (canto superior esquerdo/direito)
 * - Movimento (frame difference)
 * - Flash de impacto (hit flash)
 * - Tela preta (menu/pause)
 * - Distancia aproximada entre lutadores
 */
public class GameDetector {

    private static final String TAG = "GameDetector";

    // Regioes das barras de HP (fracoes da tela, landscape)
    // P1: canto superior esquerdo, P2: canto superior direito
    private static final float P1_HP_X1 = 0.04f, P1_HP_Y1 = 0.04f;
    private static final float P1_HP_X2 = 0.30f, P1_HP_Y2 = 0.07f;
    private static final float P2_HP_X1 = 0.70f, P2_HP_Y1 = 0.04f;
    private static final float P2_HP_X2 = 0.96f, P2_HP_Y2 = 0.07f;

    // Regiao central para deteccao de movimento e lutadores
    private static final float PLAY_X1 = 0.15f, PLAY_Y1 = 0.20f;
    private static final float PLAY_X2 = 0.85f, PLAY_Y2 = 0.85f;

    // Limiares
    private static final int HP_COLOR_MIN_R = 180; // barras de HP sao brilhantes (amarelo/laranja/vermelho)
    private static final int HP_COLOR_MIN_G = 100;
    private static final int MOTION_THRESHOLD = 25;
    private static final int FLASH_THRESHOLD = 200;
    private static final int BLACK_SCREEN_THRESHOLD = 30;

    private Bitmap prevFrame = null;
    private int prevP1X = -1, prevP1Y = -1;
    private int prevP2X = -1, prevP2Y = -1;

    public static class GameState {
        public int p1Hp;       // 0-100
        public int oppHp;      // 0-100
        public int motion;     // quantidade de pixels que mudaram
        public boolean hitFlash;
        public boolean blackScreen;
        public int distance;   // 0=perto, 1=medio, 2=longe
        public boolean oppCrouching;
        public boolean oppJumping;
        public boolean oppAttacking;

        public GameState() {
            p1Hp = 100;
            oppHp = 100;
            motion = 0;
            hitFlash = false;
            blackScreen = false;
            distance = 1;
            oppCrouching = false;
            oppJumping = false;
            oppAttacking = false;
        }
    }

    public GameState detect(Bitmap frame, int screenW, int screenH) {
        if (frame == null) return null;

        GameState state = new GameState();

        // 1. Verifica tela preta
        state.blackScreen = isBlackScreen(frame, screenW, screenH);
        if (state.blackScreen) {
            state.p1Hp = 0;
            state.oppHp = 0;
            state.motion = 0;
            return state;
        }

        // 2. Detecta HP
        state.p1Hp = detectHp(frame, screenW, screenH, true);
        state.oppHp = detectHp(frame, screenW, screenH, false);

        // 3. Detecta movimento
        state.motion = detectMotion(frame, screenW, screenH);

        // 4. Detecta flash de impacto
        state.hitFlash = detectHitFlash(frame, screenW, screenH);

        // 5. Detecta posicao dos lutadores e distancia
        int[] p1Pos = detectFighterPosition(frame, screenW, screenH, true);
        int[] p2Pos = detectFighterPosition(frame, screenW, screenH, false);

        if (p1Pos != null) prevP1X = p1Pos[0];
        if (p2Pos != null) prevP2X = p2Pos[0];

        if (p1Pos != null && p2Pos != null) {
            int dx = Math.abs(p1Pos[0] - p2Pos[0]);
            int screenThird = screenW / 3;
            if (dx < screenThird) state.distance = 0;       // perto
            else if (dx < screenThird * 2) state.distance = 1; // medio
            else state.distance = 2;                          // longe

            // Detecta se oponente esta no ar (Y acima do normal)
            int groundY = (int) (screenH * 0.75);
            if (p2Pos[1] < groundY - screenH * 0.15) {
                state.oppJumping = true;
            }
            // Detecta se oponente esta agachado (Y abaixo do normal e largura menor)
            if (p2Pos[1] > groundY + screenH * 0.05) {
                state.oppCrouching = true;
            }
        }

        // Oponente atacando: motion alto na regiao do oponente
        if (state.motion > 80 && p2Pos != null) {
            state.oppAttacking = true;
        }

        // Salva frame atual para proxima comparacao
        if (prevFrame != null) {
            prevFrame.recycle();
        }
        prevFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

        return state;
    }

    private boolean isBlackScreen(Bitmap frame, int w, int h) {
        long totalLum = 0;
        int count = 0;
        int stepX = Math.max(1, w / 30);
        int stepY = Math.max(1, h / 20);
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                int pixel = frame.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                totalLum += (r + g + b) / 3;
                count++;
            }
        }
        if (count == 0) return true;
        int avgLum = (int) (totalLum / count);
        return avgLum < BLACK_SCREEN_THRESHOLD;
    }

    private int detectHp(Bitmap frame, int w, int h, boolean isP1) {
        float x1f, y1f, x2f, y2f;
        if (isP1) {
            x1f = P1_HP_X1; y1f = P1_HP_Y1;
            x2f = P1_HP_X2; y2f = P1_HP_Y2;
        } else {
            x1f = P2_HP_X1; y1f = P2_HP_Y1;
            x2f = P2_HP_X2; y2f = P2_HP_Y2;
        }

        int x1 = (int) (x1f * w);
        int y1 = (int) (y1f * h);
        int x2 = (int) (x2f * w);
        int y2 = (int) (y2f * h);

        // Para P1, a barra enche da esquerda pra direita
        // Para P2, a barra enche da direita pra esquerda
        int brightPixels = 0;
        int totalPixels = 0;
        int lastBrightX = -1;

        for (int y = y1; y < y2 && y < h; y++) {
            for (int x = x1; x < x2 && x < w; x++) {
                int pixel = frame.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // Barras de HP sao brilhantes (amarelo/laranja/vermelho/verde)
                if (r > HP_COLOR_MIN_R && g > HP_COLOR_MIN_G) {
                    brightPixels++;
                    if (isP1) {
                        if (x > lastBrightX) lastBrightX = x;
                    } else {
                        if (lastBrightX < 0 || x < lastBrightX) lastBrightX = x;
                    }
                }
                totalPixels++;
            }
        }

        if (totalPixels == 0) return 100;

        // Calcula HP baseado na largura preenchida
        int barWidth = x2 - x1;
        if (barWidth <= 0) return 100;

        int filledWidth;
        if (isP1) {
            // P1: barra enche da esquerda
            filledWidth = lastBrightX > 0 ? (lastBrightX - x1) : 0;
        } else {
            // P2: barra enche da direita
            filledWidth = lastBrightX > 0 ? (x2 - lastBrightX) : 0;
        }

        int hp = (filledWidth * 100) / barWidth;
        if (hp < 0) hp = 0;
        if (hp > 100) hp = 100;
        return hp;
    }

    private int detectMotion(Bitmap frame, int w, int h) {
        if (prevFrame == null) return 0;
        if (prevFrame.getWidth() != frame.getWidth()
                || prevFrame.getHeight() != frame.getHeight()) {
            return 0;
        }

        int x1 = (int) (PLAY_X1 * w);
        int y1 = (int) (PLAY_Y1 * h);
        int x2 = (int) (PLAY_X2 * w);
        int y2 = (int) (PLAY_Y2 * h);

        int changedPixels = 0;
        int stepX = Math.max(1, (x2 - x1) / 60);
        int stepY = Math.max(1, (y2 - y1) / 40);

        for (int y = y1; y < y2 && y < h; y += stepY) {
            for (int x = x1; x < x2 && x < w; x += stepX) {
                int c1 = frame.getPixel(x, y);
                int c2 = prevFrame.getPixel(x, y);
                int dr = Math.abs(Color.red(c1) - Color.red(c2));
                int dg = Math.abs(Color.green(c1) - Color.green(c2));
                int db = Math.abs(Color.blue(c1) - Color.blue(c2));
                if (dr + dg + db > MOTION_THRESHOLD * 3) {
                    changedPixels++;
                }
            }
        }
        return changedPixels;
    }

    private boolean detectHitFlash(Bitmap frame, int w, int h) {
        // Flash de impacto: muitos pixels muito brancos/brilhantes aparecem subitamente
        int x1 = (int) (PLAY_X1 * w);
        int y1 = (int) (PLAY_Y1 * h);
        int x2 = (int) (PLAY_X2 * w);
        int y2 = (int) (PLAY_Y2 * h);

        int brightCount = 0;
        int stepX = Math.max(1, (x2 - x1) / 50);
        int stepY = Math.max(1, (y2 - y1) / 30);

        for (int y = y1; y < y2 && y < h; y += stepY) {
            for (int x = x1; x < x2 && x < w; x += stepX) {
                int pixel = frame.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                if (r > FLASH_THRESHOLD && g > FLASH_THRESHOLD && b > FLASH_THRESHOLD) {
                    brightCount++;
                }
            }
        }
        return brightCount > 30;
    }

    private int[] detectFighterPosition(Bitmap frame, int w, int h, boolean isP1) {
        // Deteccao simples: procura a regiao com maior concentracao de pixels "coloridos"
        // (nao背景, nao preto, nao branco puro) na metade correspondente da tela
        int halfStart = isP1 ? (int) (w * 0.15) : (int) (w * 0.50);
        int halfEnd = isP1 ? (int) (w * 0.50) : (int) (w * 0.85);
        int yStart = (int) (h * 0.20);
        int yEnd = (int) (h * 0.85);

        int bestX = -1, bestY = -1;
        int bestCount = 0;

        int stepX = Math.max(1, (halfEnd - halfStart) / 20);
        int stepY = Math.max(1, (yEnd - yStart) / 15);
        int blockSize = Math.max(8, w / 60);

        for (int y = yStart; y < yEnd; y += stepY) {
            for (int x = halfStart; x < halfEnd; x += stepX) {
                int colorCount = 0;
                for (int dy = 0; dy < blockSize && y + dy < h; dy++) {
                    for (int dx = 0; dx < blockSize && x + dx < w; dx++) {
                        int pixel = frame.getPixel(x + dx, y + dy);
                        int r = Color.red(pixel);
                        int g = Color.green(pixel);
                        int b = Color.blue(pixel);
                        int maxC = Math.max(r, Math.max(g, b));
                        int minC = Math.min(r, Math.min(g, b));
                        // Pixel "colorido" = saturacao alta e brilho medio
                        if (maxC - minC > 60 && maxC > 80 && maxC < 240) {
                            colorCount++;
                        }
                    }
                }
                if (colorCount > bestCount) {
                    bestCount = colorCount;
                    bestX = x + blockSize / 2;
                    bestY = y + blockSize / 2;
                }
            }
        }

        if (bestX < 0) return null;
        return new int[]{bestX, bestY};
    }

    public void reset() {
        if (prevFrame != null) {
            prevFrame.recycle();
            prevFrame = null;
        }
        prevP1X = -1;
        prevP2X = -1;
    }
}
