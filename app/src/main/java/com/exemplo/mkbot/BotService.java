package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.exemplo.mkbot.brain.QLearningAgent;
import com.exemplo.mkbot.vision.GameDetector;
import com.exemplo.mkbot.vision.GameDetector.GameState;
import com.google.gson.Gson;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BotService extends AccessibilityService {

    private static final String TAG = "MKBot";
    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_QTABLE = "qtable";
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";

    // Ajuste para o pacote do seu emulador:
    // AetherSX2:  "xyz.aethersx2.android"
    // NetherSX2:  "xyz.aethersx2.android"
    // DamonPS2 free: "com.damonps2.free"
    // DamonPS2 pro:  "com.damonps2.pro"
    private static final String EMULATOR_PKG = "xyz.aethersx2.android";

    // Intervalo entre capturas (ms) — ~8 FPS
    private static final long LOOP_INTERVAL_MS = 120;

    // Coordenadas dos botões on-screen do emulador (fração da tela).
    // Valores padrão para layout típico do AetherSX2.
    // AJUSTE conforme o layout que aparece no seu celular.
    private static final float BTN_BACK_X = 0.12f,  BTN_BACK_Y = 0.80f;
    private static final float BTN_FWD_X  = 0.22f,  BTN_FWD_Y  = 0.80f;
    private static final float BTN_UP_X   = 0.17f,  BTN_UP_Y   = 0.72f;
    private static final float BTN_DOWN_X = 0.17f,  BTN_DOWN_Y = 0.88f;
    private static final float BTN_A1_X   = 0.82f,  BTN_A1_Y   = 0.82f; // Square
    private static final float BTN_A2_X   = 0.88f,  BTN_A2_Y   = 0.76f; // Triangle
    private static final float BTN_A3_X   = 0.94f,  BTN_A3_Y   = 0.82f; // Circle
    private static final float BTN_A4_X   = 0.88f,  BTN_A4_Y   = 0.88f; // Cross
    private static final float BTN_R1_X   = 0.96f,  BTN_R1_Y   = 0.65f; // Throw
    private static final float BTN_R2_X   = 0.96f,  BTN_R2_Y   = 0.70f; // Block

    private Handler mainHandler;
    private Executor bgExecutor;
    private SharedPreferences prefs;
    private Gson gson;

    private QLearningAgent brain;
    private GameDetector detector;

    private boolean loopRunning = false;
    private GameState lastState = null;
    private int lastActionIndex = -1;
    private int screenWidth, screenHeight;

    private boolean inFight = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mainHandler = new Handler(Looper.getMainLooper());
        bgExecutor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        gson = new Gson();
        brain = new QLearningAgent();
        detector = new GameDetector();
        loadQTable();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        Log.i(TAG, "BotService conectado. Tela: " + screenWidth + "x" + screenHeight);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (EMULATOR_PKG.equals(pkg)) {
            checkAndStartLoop();
        } else if (pkg != null && !pkg.equals("android")) {
            stopLoop();
        }
    }

    @Override
    public void onInterrupt() {
        stopLoop();
    }

    private void checkAndStartLoop() {
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        if (running && !loopRunning) startLoop();
        else if (!running && loopRunning) stopLoop();
    }

    private void startLoop() {
        loopRunning = true;
        Log.i(TAG, "Loop iniciado.");
        mainHandler.post(captureRunnable);
    }

    private void stopLoop() {
        loopRunning = false;
        mainHandler.removeCallbacks(captureRunnable);
        inFight = false;
        lastState = null;
        lastActionIndex = -1;
        Log.i(TAG, "Loop parado.");
    }

    private final Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        Bitmap hw = Bitmap.wrapHardwareBuffer(
                            result.getHardwareBuffer(), result.getColorSpace());
                        if (hw != null) {
                            Bitmap soft = hw.copy(Bitmap.Config.ARGB_8888, false);
                            hw.recycle();
                            final Bitmap frame = soft;
                            bgExecutor.execute(() -> processFrame(frame));
                        }
                        result.getHardwareBuffer().close();
                    }
                    @Override
                    public void onFailure(int code) {
                        Log.w(TAG, "takeScreenshot falhou: " + code);
                    }
                });
            mainHandler.postDelayed(this, LOOP_INTERVAL_MS);
        }
    };

    private void processFrame(Bitmap frame) {
        if (frame == null) return;
        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) return; // tela não reconhecida

            if (state.result == GameDetector.Result.WIN) {
                onFightEnd(true);
                return;
            }
            if (state.result == GameDetector.Result.LOSE) {
                onFightEnd(false);
                return;
            }

            if (!inFight && state.p1Hp > 0 && state.oppHp > 0) {
                inFight = true;
                Log.i(TAG, "Luta iniciada. P1=" + state.p1Hp + " opp=" + state.oppHp);
            }

            // Recompensa densa: dano causado - dano sofrido
            double reward = 0;
            if (lastState != null && lastActionIndex >= 0 && inFight) {
                int dmgDealt = lastState.oppHp - state.oppHp;
                int dmgTaken = lastState.p1Hp - state.p1Hp;
                reward = dmgDealt * 1.0 - dmgTaken * 1.0;
                if (dmgDealt == 0 && dmgTaken == 0) reward = -0.1; // inação
            }

            int stateIdx = brain.discretizeState(state);
            int actionIdx = brain.chooseAction(stateIdx);

            if (lastState != null && lastActionIndex >= 0) {
                int lastIdx = brain.discretizeState(lastState);
                brain.update(lastIdx, lastActionIndex, reward, stateIdx);
            }

            final int act = actionIdx;
            mainHandler.post(() -> executeAction(act));

            lastState = state;
            lastActionIndex = actionIdx;

        } catch (Exception e) {
            Log.e(TAG, "Erro em processFrame", e);
        } finally {
            frame.recycle();
        }
    }

    private void onFightEnd(boolean win) {
        if (!inFight) return;
        inFight = false;
        int fights = prefs.getInt(KEY_FIGHTS, 0) + 1;
        int wins = prefs.getInt(KEY_WINS, 0);
        int losses = prefs.getInt(KEY_LOSSES, 0);
        if (win) wins++; else losses++;
        prefs.edit()
            .putInt(KEY_FIGHTS, fights)
            .putInt(KEY_WINS, wins)
            .putInt(KEY_LOSSES, losses)
            .apply();

        double terminal = win ? 50.0 : -100.0;
        if (lastState != null && lastActionIndex >= 0) {
            int idx = brain.discretizeState(lastState);
            brain.update(idx, lastActionIndex, terminal, idx);
        }
        saveQTable();
        Log.i(TAG, "Luta fim. win=" + win + " total=" + fights +
              " V=" + wins + " D=" + losses);
        lastState = null;
        lastActionIndex = -1;
    }

    private void executeAction(int actionIdx) {
        switch (actionIdx) {
            case 0: tap(BTN_A1_X, BTN_A1_Y); break;             // Attack 1
            case 1: tap(BTN_A2_X, BTN_A2_Y); break;             // Attack 2
            case 2: tap(BTN_A3_X, BTN_A3_Y); break;             // Attack 3 (kick)
            case 3: tap(BTN_A4_X, BTN_A4_Y); break;             // Attack 4
            case 4: tap(BTN_BACK_X, BTN_BACK_Y); break;         // Recuar (keep-away)
            case 5: tap(BTN_FWD_X, BTN_FWD_Y); break;           // Avançar
            case 6: tap(BTN_UP_X, BTN_UP_Y); break;             // Pular
            case 7: tap(BTN_DOWN_X, BTN_DOWN_Y); break;         // Agachar
            case 8: holdButton(BTN_R2_X, BTN_R2_Y, 400); break; // Bloquear
            case 9: specialFireball(); break;                   // Projétil (back+fwd+A1)
            case 10: tap(BTN_R1_X, BTN_R1_Y); break;            // Throw
            case 11: break; // idle
        }
    }

    private void tap(float fx, float fy) {
        Path p = new Path();
        p.moveTo(fx * screenWidth, fy * screenHeight);
        GestureDescription.StrokeDescription s =
            new GestureDescription.StrokeDescription(p, 0, 50);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void holdButton(float fx, float fy, long durationMs) {
        Path p = new Path();
        p.moveTo(fx * screenWidth, fy * screenHeight);
        GestureDescription.StrokeDescription s =
            new GestureDescription.StrokeDescription(p, 0, durationMs);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void specialFireball() {
        // Back, depois Forward, depois Attack 1 — input de projétil clássico
        tapAt(BTN_BACK_X, BTN_BACK_Y, 0);
        mainHandler.postDelayed(() -> tapAt(BTN_FWD_X, BTN_FWD_Y, 0), 90);
        mainHandler.postDelayed(() -> tapAt(BTN_A1_X, BTN_A1_Y, 0), 180);
    }

    private void tapAt(float fx, float fy, long startOffset) {
        Path p = new Path();
        p.moveTo(fx * screenWidth, fy * screenHeight);
        GestureDescription.StrokeDescription s =
            new GestureDescription.StrokeDescription(p, startOffset, 60);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void saveQTable() {
        prefs.edit().putString(KEY_QTABLE, gson.toJson(brain.getQTable())).apply();
    }

    private void loadQTable() {
        String json = prefs.getString(KEY_QTABLE, null);
        if (json == null) return;
        try {
            float[][] loaded = gson.fromJson(json, float[][].class);
            if (loaded != null) {
                brain.setQTable(loaded);
                Log.i(TAG, "Q-table carregada.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Q-table inválida, recomeçando do zero.");
        }
    }
}
