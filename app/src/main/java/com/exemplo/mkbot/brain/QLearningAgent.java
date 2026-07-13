package com.exemplo.mkbot.brain;

import com.exemplo.mkbot.vision.GameDetector.GameState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Agente de aprendizado por reforco com:
 * 1. SARSA(lambda) com Eligibility Traces (substituicao)
 * 2. Prioritized Experience Replay (PER)
 * 3. Boltzmann Exploration
 * 4. Opponent Modeling (cadeia de Markov ordem 2)
 * 5. Reward shaping estilo agressivo
 *
 * Estado: HP_P1(5) x HP_OPP(5) x DIST(3) x CROUCH(2) x JUMP(2) x ATTACK(2) x LAST_ACT(16) = 9600 estados
 * Acoes: 16
 */
public class QLearningAgent {

    // =================== Dimensoes do estado ===================
    private static final int N_HP_BINS = 5;
    private static final int N_DIST_BINS = 3;
    private static final int N_BINARY = 2;
    private static final int N_ACTIONS = 16;
    private static final int N_STATES =
            N_HP_BINS * N_HP_BINS * N_DIST_BINS * N_BINARY * N_BINARY * N_BINARY * N_ACTIONS;

    // =================== Hiperparametros ===================
    private static final float ALPHA = 0.3f;
    private static final float GAMMA = 0.85f;
    private static final float LAMBDA = 0.9f;
    private static final float EPSILON_START = 0.6f;
    private static final float EPSILON_MIN = 0.05f;
    private static final float TEMP_START = 1.0f;
    private static final float TEMP_MIN = 0.1f;

    // =================== Reward shaping (agressivo) ===================
    public static final float R_HIT = 20.0f;
    public static final float R_COMBO = 50.0f;
    public static final float R_PUNISH = 30.0f;
    public static final float R_BLOCK_OK = 8.0f;
    public static final float R_DMG_TAKEN = -30.0f;
    public static final float R_WIN = 500.0f;
    public static final float R_LOSS = -800.0f;
    public static final float R_TIME = -0.5f;
    public static final float R_DIST = -0.2f;
    public static final float R_SPAM = -10.0f;
    public static final float R_NOOP = -0.1f;

    // =================== Q-table e traces ===================
    private float[][] qTable;
    private List<TraceEntry> traceEntries;
    private float epsilon;
    private float temperature;
    private int lastAction;
    private int repeatCount;
    private final Random random;

    // =================== Experience Replay ===================
    private final List<Transition> replayBuffer;
    private static final int REPLAY_MAX = 5000;
    private static final int REPLAY_BATCH = 32;

    // =================== Opponent Model ===================
    private final OpponentModel oppModel;

    // ============================================================
    //  Classes internas
    // ============================================================

    private static class TraceEntry {
        int state;
        int action;
        float trace;

        TraceEntry(int s, int a, float t) {
            state = s;
            action = a;
            trace = t;
        }
    }

    private static class Transition {
        int state;
        int action;
        float reward;
        int nextState;
        int nextAction;
        float priority;

        Transition(int s, int a, float r, int ns, int na) {
            state = s;
            action = a;
            reward = r;
            nextState = ns;
            nextAction = na;
            priority = Math.abs(r) + 0.01f;
        }
    }

    public static class OpponentModel {
        private static final int N_OPP = 8;
        private final int[][][] counts;
        private int prevPrev;
        private int prev;

        public OpponentModel() {
            counts = new int[N_OPP][N_OPP][N_OPP];
            prevPrev = -1;
            prev = -1;
        }

        public void observe(int act) {
            if (act < 0 || act >= N_OPP) return;
            if (prevPrev >= 0 && prev >= 0) {
                counts[prevPrev][prev][act]++;
            }
            prevPrev = prev;
            prev = act;
        }

        public int predict() {
            if (prevPrev < 0 || prev < 0) return -1;
            int[] c = counts[prevPrev][prev];
            int best = -1, bestC = 0;
            for (int i = 0; i < N_OPP; i++) {
                if (c[i] > bestC) {
                    bestC = c[i];
                    best = i;
                }
            }
            return best;
        }

        public void reset() {
            prevPrev = -1;
            prev = -1;
        }
    }

    // ============================================================
    //  Construtor
    // ============================================================

    public QLearningAgent() {
        qTable = new float[N_STATES][N_ACTIONS];
        traceEntries = new ArrayList<>();
        replayBuffer = new ArrayList<>();
        oppModel = new OpponentModel();
        random = new Random();
        epsilon = EPSILON_START;
        temperature = TEMP_START;
        lastAction = -1;
        repeatCount = 0;
        initQTable();
    }

    private void initQTable() {
        for (int s = 0; s < N_STATES; s++) {
            for (int a = 0; a < N_ACTIONS; a++) {
                qTable[s][a] = (random.nextFloat() - 0.5f) * 0.1f;
            }
        }
    }

    // ============================================================
    //  Discretizacao de estado
    // ============================================================

    public int discretizeState(GameState s) {
        return discretizeState(s, lastAction);
    }

    public int discretizeState(GameState s, int lastAct) {
        int p1 = hpToBin(s.p1Hp);
        int opp = hpToBin(s.oppHp);
        int dist = clamp(s.distance, 0, N_DIST_BINS - 1);
        int cr = s.oppCrouching ? 1 : 0;
        int jp = s.oppJumping ? 1 : 0;
        int atk = s.oppAttacking ? 1 : 0;
        int la = (lastAct >= 0 && lastAct < N_ACTIONS) ? lastAct : 0;

        int idx = la;
        idx = idx * N_BINARY + atk;
        idx = idx * N_BINARY + jp;
        idx = idx * N_BINARY + cr;
        idx = idx * N_DIST_BINS + dist;
        idx = idx * N_HP_BINS + opp;
        idx = idx * N_HP_BINS + p1;
        return idx;
    }

    private int hpToBin(int hp) {
        if (hp < 0) hp = 0;
        if (hp > 100) hp = 100;
        return hp / 20;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ============================================================
    //  Selecao de acao (Boltzmann + epsilon)
    // ============================================================

    public int chooseAction(int stateIdx) {
        if (random.nextFloat() < epsilon) {
            return random.nextInt(N_ACTIONS);
        }
        return boltzmannSelect(stateIdx);
    }

    private int boltzmannSelect(int stateIdx) {
        if (stateIdx < 0 || stateIdx >= N_STATES) return random.nextInt(N_ACTIONS);
        float[] q = qTable[stateIdx];
        float[] probs = new float[N_ACTIONS];
        float sum = 0;
        for (int a = 0; a < N_ACTIONS; a++) {
            float val = q[a] / temperature;
            if (val > 50) val = 50;
            if (val < -50) val = -50;
            probs[a] = (float) Math.exp(val);
            sum += probs[a];
        }
        if (sum <= 0) return random.nextInt(N_ACTIONS);
        float r = random.nextFloat() * sum;
        float cumul = 0;
        for (int a = 0; a < N_ACTIONS; a++) {
            cumul += probs[a];
            if (r <= cumul) return a;
        }
        return N_ACTIONS - 1;
    }

    // ============================================================
    //  Atualizacao SARSA(lambda) + PER
    // ============================================================

    public void update(int state, int action, float reward, int nextState, int nextAction) {
        if (state < 0 || state >= N_STATES) return;
        if (nextState < 0 || nextState >= N_STATES) return;

        float qSa = qTable[state][action];
        float qNext = qTable[nextState][nextAction];
        float tdError = reward + GAMMA * qNext - qSa;

        // 1. Dezeni traces existentes e atualiza Q
        Iterator<TraceEntry> it = traceEntries.iterator();
        while (it.hasNext()) {
            TraceEntry e = it.next();
            e.trace *= GAMMA * LAMBDA;
            if (e.trace < 0.01f) {
                it.remove();
            } else {
                qTable[e.state][e.action] += ALPHA * tdError * e.trace;
            }
        }

        // 2. Adiciona trace atual (replacing)
        boolean found = false;
        for (TraceEntry e : traceEntries) {
            if (e.state == state && e.action == action) {
                e.trace = 1.0f;
                found = true;
                break;
            }
        }
        if (!found) {
            traceEntries.add(new TraceEntry(state, action, 1.0f));
        }
        qTable[state][action] += ALPHA * tdError;

        // 3. Guarda no replay buffer
        if (replayBuffer.size() >= REPLAY_MAX) {
            replayBuffer.remove(0);
        }
        Transition t = new Transition(state, action, reward, nextState, nextAction);
        t.priority = Math.abs(tdError) + 0.01f;
        replayBuffer.add(t);

        // 4. Prioritized replay
        prioritizedReplay();

        // 5. Atualiza repeticao e ultima acao
        if (action == lastAction) {
            repeatCount++;
        } else {
            repeatCount = 0;
        }
        lastAction = action;
    }

    private void prioritizedReplay() {
        if (replayBuffer.size() < REPLAY_BATCH) return;

        float totalPri = 0;
        for (Transition t : replayBuffer) totalPri += t.priority;
        if (totalPri <= 0) return;

        for (int i = 0; i < REPLAY_BATCH; i++) {
            float r = random.nextFloat() * totalPri;
            float cumul = 0;
            Transition sel = null;
            for (Transition t : replayBuffer) {
                cumul += t.priority;
                if (r <= cumul) {
                    sel = t;
                    break;
                }
            }
            if (sel == null) continue;

            float tdErr = sel.reward + GAMMA * qTable[sel.nextState][sel.nextAction]
                    - qTable[sel.state][sel.action];
            qTable[sel.state][sel.action] += ALPHA * tdErr;
            sel.priority = Math.abs(tdErr) + 0.01f;
        }
    }

    // ============================================================
    //  Decaimento de epsilon por tempo
    // ============================================================

    public void decayEpsilonByTime(long totalPlayMs) {
        long twoHours = 2 * 60 * 60 * 1000L;
        float t = Math.min(1.0f, (float) totalPlayMs / twoHours);
        epsilon = EPSILON_START - (EPSILON_START - EPSILON_MIN) * t;
        temperature = TEMP_START - (TEMP_START - TEMP_MIN) * t;
    }

    public void restoreEpsilonByTime(long totalPlayMs) {
        decayEpsilonByTime(totalPlayMs);
    }

    // ============================================================
    //  Reward shaping
    // ============================================================

    public float computeReward(GameState prev, GameState curr, int action, boolean comboHit) {
        float r = 0;
        if (prev != null) {
            int dmgDealt = prev.oppHp - curr.oppHp;
            int dmgTaken = prev.p1Hp - curr.p1Hp;

            if (dmgDealt > 0) r += dmgDealt * (R_HIT / 10.0f);
            if (dmgTaken > 0) r += dmgTaken * (R_DMG_TAKEN / 10.0f);
            if (curr.hitFlash) r += R_HIT;
            if (comboHit) r += R_COMBO;
            if (action == 8 && dmgTaken == 0 && curr.motion > 5) r += R_BLOCK_OK;

            if (dmgDealt == 0 && dmgTaken == 0 && !curr.hitFlash) r += R_NOOP;

            r += R_DIST * (curr.distance / 2.0f);
            r += R_TIME;

            if (repeatCount >= 3) r += R_SPAM;
        }
        return r;
    }

    // ============================================================
    //  Getters / Setters
    // ============================================================

    public float[][] getQTable() {
        return qTable;
    }

    public void setQTable(float[][] loaded) {
        if (loaded != null && loaded.length == N_STATES) {
            qTable = loaded;
        }
    }

    public float getEpsilon() {
        return epsilon;
    }

    public int getLastAction() {
        return lastAction;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public OpponentModel getOpponentModel() {
        return oppModel;
    }

    public void resetLastAction() {
        lastAction = -1;
        repeatCount = 0;
        traceEntries.clear();
    }

    public int getNStates() {
        return N_STATES;
    }

    public int getNActions() {
        return N_ACTIONS;
    }
}
