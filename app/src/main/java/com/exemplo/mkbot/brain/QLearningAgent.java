package com.exemplo.mkbot.brain;

import com.exemplo.mkbot.vision.GameDetector.GameState;

import java.util.Random;

/**
 * Q-Learning agent simples.
 *
 * Estado: combinacao de (faixa de HP do P1, faixa de HP do oponente).
 *   - HP do P1 dividido em 5 faixas: 0-20, 20-40, 40-60, 60-80, 80-100
 *   - HP do opp dividido em 5 faixas: 0-20, 20-40, 40-60, 60-80, 80-100
 *   - Total de estados: 5 x 5 = 25
 *
 * Acoes (12):
 *   0  Attack 1 (soco fraco)
 *   1  Attack 2 (soco forte)
 *   2  Attack 3 (chute)
 *   3  Attack 4 (chute forte)
 *   4  Recuar (keep-away)
 *   5  Avancar
 *   6  Pular
 *   7  Agachar
 *   8  Bloquear (hold)
 *   9  Fireball (back+fwd+A1)
 *   10 Throw
 *   11 Idle
 *
 * Q-table: matriz [25][12] de floats.
 * Politica: epsilon-greedy.
 */
public class QLearningAgent {

    private static final int N_HP_BINS = 5;
    private static final int N_STATES = N_HP_BINS * N_HP_BINS; // 25
    private static final int N_ACTIONS = 12;

    // Hiperparametros do Q-learning
    private static final double ALPHA = 0.2;      // taxa de aprendizado
    private static final double GAMMA = 0.9;      // fator de desconto
    private static final double EPSILON = 0.15;   // exploracao
    private static final double EPSILON_MIN = 0.02;

    private final float[][] qTable;
    private final Random rng = new Random();
    private int fightsTrained = 0;

    public QLearningAgent() {
        qTable = new float[N_STATES][N_ACTIONS];
        // Inicializa com zeros — o bot comeca sem saber nada
    }

    /**
     * Converte um GameState num indice discreto de estado.
     * Combina faixa de HP do P1 e faixa de HP do oponente.
     */
    public int discretizeState(GameState s) {
        int p1Bin = hpToBin(s.p1Hp);
        int oppBin = hpToBin(s.oppHp);
        return p1Bin * N_HP_BINS + oppBin;
    }

    private int hpToBin(int hp) {
        if (hp < 0) hp = 0;
        if (hp > 100) hp = 100;
        return hp / 20;          // 0..4
    }

    /**
     * Escolhe uma acao usando epsilon-greedy.
     * Com probabilidade epsilon, escolhe aleatoriamente (exploracao).
     * Caso contrario, escolhe a acao com maior Q-value (exploracao).
     */
    public int chooseAction(int stateIdx) {
        if (stateIdx < 0 || stateIdx >= N_STATES) return 0;

        double eps = Math.max(EPSILON_MIN, EPSILON);
        if (rng.nextDouble() < eps) {
            return rng.nextInt(N_ACTIONS);
        }
        return argMax(qTable[stateIdx]);
    }

    /**
     * Atualizacao do Q-learning:
     *   Q(s,a) <- Q(s,a) + alpha * [r + gamma * max_a' Q(s',a') - Q(s,a)]
     */
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
    }

    public int getFightsTrained() {
        return fightsTrained;
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

    // --- helpers ---

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
