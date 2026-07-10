package com.exemplo.mkbot.brain;

import com.exemplo.mkbot.vision.GameDetector.GameState;

import java.util.Random;

/**
 * Q-Learning agent com aprendizado melhorado.
 *
 * Estado: (faixa HP P1, faixa HP opp, faixa movimento, ultima acao)
 *   - HP P1: 5 faixas
 *   - HP opp: 5 faixas
 *   - Movimento: 3 faixas
 *   - Ultima acao: 12 valores
 *   - Total: 5 x 5 x 3 x 12 = 900 estados
 *
 * Melhorias:
 *   - Epsilon decay: comeca em 0.3 e decai pra 0.05 conforme treina
 *   - Penalidade por repetir acao (evita spam de um botao)
 *   - Estado inclui ultima acao (aprende sequencias/combos)
 */
public class QLearningAgent {

    private static final int N_HP_BINS = 5;
    private static final int N_MOTION_BINS = 3;
    private static final int N_ACTIONS = 12;
    private static final int N_STATES = N_HP_BINS * N_HP_BINS * N_MOTION_BINS * N_ACTIONS; // 900

    private static final double ALPHA = 0.15;
    private static final double GAMMA = 0.95;
    private static final double EPSILON_START = 0.3;
    private static final double EPSILON_MIN = 0.05;
    private static final double EPSILON_DECAY = 0.995;

    private final float[][] qTable;
    private final Random rng = new Random();
    private double currentEpsilon = EPSILON_START;
    private int fightsTrained = 0;
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

    public void onFightEnd() {
        fightsTrained++;
        // Epsilon decay: a cada luta, reduz exploracao
        currentEpsilon *= EPSILON_DECAY;
        if (currentEpsilon < EPSILON_MIN) currentEpsilon = EPSILON_MIN;
        System.out.println("Epsilon atual: " + currentEpsilon + " apos " + fightsTrained + " lutas");
    }

    public int getFightsTrained() {
        return fightsTrained;
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

    public void setFightsTrained(int count) {
        fightsTrained = count;
        // Recalcula epsilon baseado no numero de lutas
        currentEpsilon = EPSILON_START;
        for (int i = 0; i < count; i++) {
            currentEpsilon *= EPSILON_DECAY;
        }
        if (currentEpsilon < EPSILON_MIN) currentEpsilon = EPSILON_MIN;
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
