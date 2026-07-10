package com.exemplo.mkbot.brain;

import com.exemplo.mkbot.vision.GameDetector.GameState;

import java.util.Random;

/**
 * Q-Learning agent com arsenal do Scorpion.
 *
 * Estado: (faixa HP P1, faixa HP opp, faixa movimento, ultima acao)
 *   5 x 5 x 3 x 17 = 1275 estados
 *
 * Acoes (17):
 *   0  Attack 1 (Quadrado)
 *   1  Attack 2 (Triangulo)
 *   2  Attack 3 (X/Cross)
 *   3  Attack 4 (Circulo)
 *   4  Recuar (Esquerda)
 *   5  Avancar (Direita)
 *   6  Pular (Cima)
 *   7  Agachar (Baixo)
 *   8  Block (R2 - segurar)
 *   9  Throw (R2 normal)
 *   10 Pegar arma (L1)
 *   11 Idle
 *   12 Bloody Spear (Back, Fwd + A1) - puxar
 *   13 Hellfire (Down, Back + A2) - fogo no chao
 *   14 Backflip Kick (Fwd, Back + A3) - anti-aereo
 *   15 Hellfire Punch (Fwd, Back + A4) - teleporte
 *   16 Triple Combo (A2, A2, A3) - combo 3 hits
 */
public class QLearningAgent {

    private static final int N_HP_BINS = 5;
    private static final int N_MOTION_BINS = 3;
    private static final int N_ACTIONS = 17;
    private static final int N_STATES = N_HP_BINS * N_HP_BINS * N_MOTION_BINS * N_ACTIONS;

    private static final double ALPHA = 0.15;
    private static final double GAMMA = 0.95;
    private static final double EPSILON_START = 0.4;     // mais exploracao
    private static final double EPSILON_MIN = 0.05;
    private static final long DECAY_INTERVAL_MS = 300000;
    private static final double DECAY_FACTOR = 0.95;

    private final float[][] qTable;
    private final Random rng = new Random();
    private double currentEpsilon = EPSILON_START;
    private long lastDecayTime = 0;
    private int lastActionChosen = -1;

    public QLearningAgent() {
        qTable = new float[N_STATES][N_ACTIONS];
        Random initRng = new Random();
        for (int i = 0; i < N_STATES; i++) {
            for (int j = 0; j < N_ACTIONS; j++) {
                qTable[i][j] = initRng.nextFloat() * 0.1f;
            }
        }
    }

    public int discretizeState(GameState s) {
        return discretizeState(s, lastActionChosen);
    }

    public int discretizeState(GameState s, int lastAction) {
        int p1Bin = hpToBin(s.p1Hp);
        int oppBin = hpToBin(s.oppHp);
        int motionBin = motionToBin(s.motion);
        int actBin = (lastAction >= 0 && lastAction < N_ACTIONS) ? lastAction : 0;
        return ((p1Bin * N_HP_BINS + oppBin) * N_MOTION_BINS + motionBin) * N_ACTIONS + actBin;
    }

    private int hpToBin(int hp) {
        if (hp < 0) hp = 0;
        if (hp > 100) hp = 100;
        return hp / 20;
    }

    private int motionToBin(int motion) {
        if (motion < 10) return 0;
        if (motion < 30) return 1;
        return 2;
    }

    public int chooseAction(int stateIdx) {
        if (stateIdx < 0 || stateIdx >= N_STATES) return 0;
        if (rng.nextDouble() < currentEpsilon) {
            lastActionChosen = rng.nextInt(N_ACTIONS);
            return lastActionChosen;
        }
        lastActionChosen = argMax(qTable[stateIdx]);
        return lastActionChosen;
    }

    public void update(int stateIdx, int actionIdx, double reward, int nextStateIdx) {
        if (stateIdx < 0 || stateIdx >= N_STATES) return;
        if (actionIdx < 0 || actionIdx >= N_ACTIONS) return;
        float[] row = qTable[stateIdx];
        float maxNext = (nextStateIdx >= 0 && nextStateIdx < N_STATES)
                ? maxOf(qTable[nextStateIdx]) : 0f;
        float current = row[actionIdx];
        float target = (float) (reward + GAMMA * maxNext);
        row[actionIdx] = (float) (current + ALPHA * (target - current));
    }

    public void decayEpsilonByTime(long totalPlayMs) {
        if (lastDecayTime == 0) {
            lastDecayTime = totalPlayMs;
            return;
        }
        long elapsed = totalPlayMs - lastDecayTime;
        if (elapsed >= DECAY_INTERVAL_MS) {
            int intervals = (int) (elapsed / DECAY_INTERVAL_MS);
            for (int i = 0; i < intervals; i++) {
                currentEpsilon *= DECAY_FACTOR;
            }
            if (currentEpsilon < EPSILON_MIN) currentEpsilon = EPSILON_MIN;
            lastDecayTime = totalPlayMs;
        }
    }

    public void restoreEpsilonByTime(long totalPlayMs) {
        currentEpsilon = EPSILON_START;
        long intervals = totalPlayMs / DECAY_INTERVAL_MS;
        for (long i = 0; i < intervals; i++) {
            currentEpsilon *= DECAY_FACTOR;
        }
        if (currentEpsilon < EPSILON_MIN) currentEpsilon = EPSILON_MIN;
        lastDecayTime = totalPlayMs;
    }

    public double getCurrentEpsilon() {
        return currentEpsilon;
    }

    public float[][] getQTable() {
        return qTable;
    }

    public void setQTable(float[][] loaded) {
        if (loaded == null) return;
        for (int i = 0; i < N_STATES && i < loaded.length; i++) {
            for (int j = 0; j < N_ACTIONS && j < loaded[i].length; j++) {
                qTable[i][j] = loaded[i][j];
            }
        }
    }

    public void resetLastAction() {
        lastActionChosen = -1;
    }

    private int argMax(float[] arr) {
        int best = 0;
        float bestVal = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > bestVal) {
                bestVal = arr[i];
                best = i;
            }
        }
        return best;
    }

    private float maxOf(float[] arr) {
        float m = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > m) m = arr[i];
        }
        return m;
    }
}
