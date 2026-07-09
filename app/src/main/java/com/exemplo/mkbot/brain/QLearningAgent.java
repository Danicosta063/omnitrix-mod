package com.exemplo.mkbot.brain;

import com.exemplo.mkbot.vision.GameDetector.GameState;

import java.util.Random;

/**
 * Q-Learning agent com estado rico.
 *
 * Estado: (faixa HP P1, faixa HP opp, faixa movimento)
 *   - HP P1: 5 faixas (0-20, 20-40, 40-60, 60-80, 80-100)
 *   - HP opp: 5 faixas
 *   - Movimento: 3 faixas (parado, pouco, muito)
 *   - Total: 5 x 5 x 3 = 75 estados
 *
 * Acoes (12):
 *   0  Attack 1 (Quadrado)
 *   1  Attack 2 (Triangulo)
 *   2  Attack 3 (X/Cross)
 *   3  Attack 4 (Circulo)
 *   4  Recuar (Esquerda)
 *   5  Avancar (Direita)
 *   6  Pular (Cima)
 *   7  Agachar (Baixo)
 *   8  Block (R2)
 *   9  Throw (R1)
 *   10 Idle
 *   11 Idle2
 *
 * Q-table: matriz [75][12] de floats.
 * Inicializada com valores aleatorios pequenos (nao zeros) pra
 * o bot comecar testando botoes diferentes.
 * Politica: epsilon-greedy.
 */
public class QLearningAgent {

    private static final int N_HP_BINS = 5;
    private static final int N_MOTION_BINS = 3;
    private static final int N_STATES = N_HP_BINS * N_HP_BINS * N_MOTION_BINS; // 75
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
        // Inicializa com valores aleatorios pequenos em vez de zeros.
        // Isso faz o bot comecar testando botoes diferentes em vez de
        // sempre clicar no mesmo (argMax de zeros = sempre indice 0).
        Random initRng = new Random();
        for (int i = 0; i < N_STATES; i++) {
            for (int j = 0; j < N_ACTIONS; j++) {
                qTable[i][j] = initRng.nextFloat() * 0.1f;
            }
        }
    }

    /**
     * Converte um GameState num indice discreto de estado.
     * Combina faixa de HP do P1, faixa de HP do oponente, e faixa de movimento.
     */
    public int discretizeState(GameState s) {
        int p1Bin = hpToBin(s.p1Hp);
        int oppBin = hpToBin(s.oppHp);
        int motionBin = motionToBin(s.motion);
        return p1Bin * (N_HP_BINS * N_MOTION_BINS) + oppBin * N_MOTION_BINS + motionBin;
    }

    private int hpToBin(int hp) {
        if (hp < 0) hp = 0;
        if (hp > 100) hp = 100;
        return hp / 20;          // 0..4
    }

    private int motionToBin(int motion) {
        if (motion < 10) return 0;      // parado / pausado
        if (motion < 30) return 1;      // pouco movimento
        return 2;                        // muito movimento
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
