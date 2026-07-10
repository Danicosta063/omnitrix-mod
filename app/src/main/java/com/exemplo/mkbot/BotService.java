package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Path;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

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
    private static final String KEY_PLAY_MODE = "playMode";
    private static final String KEY_QTABLE = "qtable";
    private static final String KEY_FIGHTS = "fights";

    private static final String EMULATOR_PKG = "xyz.aethersx2.android";
    private static final long LOOP_INTERVAL_MS = 120;

    private static final float BTN_A1_X = 0.807f, BTN_A1_Y = 0.727f;
    private static final float BTN_A2_X = 0.875f, BTN_A2_Y = 0.583f;
    private static final float BTN_A3_X = 0.858f, BTN_A3_Y = 0.861f;
    private static final float BTN_A4_X = 0.925f, BTN_A4_Y = 0.741f;
    private static final float BTN_BACK_X = 0.083f, BTN_BACK_Y = 0.731f;
    private static final float BTN_FWD_X = 0.215f, BTN_FWD_Y = 0.741f;
    private static final float BTN_UP_X = 0.139f, BTN_UP_Y = 0.593f;
    private static final float BTN_DOWN_X = 0.150f, BTN_DOWN_Y = 0.880f;
    private static final float BTN_R2_X = 0.858f, BTN_R2_Y = 0.218f;
    private static final float BTN_R1_X = 0.858f, BTN_R1_Y = 0.331f;
    private static final float BTN_L1_X = 0.146f, BTN_L1_Y = 0.331f;

    private Handler mainHandler;
    private Executor bgExecutor;
    private SharedPreferences prefs;
    private Gson gson;
    private QLearningAgent brain;
    private GameDetector detector;

    private boolean loopRunning = false;
    private int lastActionIndex = -1;
    private int repeatActionCount = 0;
    private GameState lastState = null;
    private int screenWidth = 1, screenHeight = 1;
    private int consecutiveHits = 0;
    private int saveCounter = 0;

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
        int fights = prefs.getInt(KEY_FIGHTS, 0);
        brain.setFightsTrained(fights);
        Log.i(TAG, "BotService conectado.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (EMULATOR_PKG.equals(pkg)) {
            startLoop();
        } else if (pkg != null && !pkg.equals("android")) {
            stopLoop();
        }
    }

    @Override
    public void onInterrupt() {
        stopLoop();
    }

    private void startLoop() {
        if (loopRunning) return;
        loopRunning = true;
        Log.i(TAG, "Loop iniciado.");
        mainHandler.post(captureRunnable);
    }

    private void stopLoop() {
        loopRunning = false;
        mainHandler.removeCallbacks(captureRunnable);
        lastState = null;
        lastActionIndex = -1;
        repeatActionCount = 0;
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
                        final Bitmap frame = screenshotToBitmap(result);
                        if (frame != null) {
                            bgExecutor.execute(() -> processFrame(frame));
                        }
                        result.getHardwareBuffer().close();
                    }
                    @Override
                    public void onFailure(int code) {
                        Log.w(TAG, "takeScreenshot falhou: code=" + code);
                    }
                });
            mainHandler.postDelayed(this, LOOP_INTERVAL_MS);
        }
    };

    private Bitmap screenshotToBitmap(ScreenshotResult result) {
        if (result == null) return null;
        HardwareBuffer hb = result.getHardwareBuffer();
        ColorSpace cs = result.getColorSpace();
        if (hb == null) return null;
        try {
            Bitmap hw = Bitmap.wrapHardwareBuffer(hb, cs);
            if (hw == null) return null;
            Bitmap soft = hw.copy(Bitmap.Config.ARGB_8888, false);
            hw.recycle();
            return soft;
        } catch (Exception e) {
            Log.e(TAG, "screenshotToBitmap failed", e);
            return null;
        }
    }

    private void processFrame(Bitmap frame) {
        if (frame == null) return;

        // Só toca se o usuario ativou o playMode (segurou + por 3s)
        if (!prefs.getBoolean(KEY_PLAY_MODE, false)) {
            frame.recycle();
            return;
        }

        screenWidth = frame.getWidth();
        screenHeight = frame.getHeight();
        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) return;

            // Conta acertos
            if (state.hitFlash) {
                consecutiveHits++;
            } else {
                consecutiveHits = 0;
            }

            // Recompensa densa
            double reward = 0;
            if (lastState != null && lastActionIndex >= 0) {
                int dmgDealt = lastState.oppHp - state.oppHp;
                int dmgTaken = lastState.p1Hp - state.p1Hp;
                reward = dmgDealt * 1.0 - dmgTaken * 1.0;
                if (state.hitFlash) reward += 2.0;

                // Penalidade por repetir a mesma acao 3+ vezes
                if (lastActionIndex == actionIdxAnterior()) {
                    repeatActionCount++;
                    if (repeatActionCount >= 3) reward -= 1.0;
                }

                if (dmgDealt == 0 && dmgTaken == 0 && !state.hitFlash) reward = -0.1;
            }

            int stateIdx = brain.discretizeState(state);
            int actionIdx = brain.chooseAction(stateIdx);

            if (lastState != null && lastActionIndex >= 0) {
                int lastIdx = brain.discretizeState(lastState, lastActionIndex);
                int nextIdx = brain.discretizeState(state, actionIdx);
                brain.update(lastIdx, lastActionIndex, reward, nextIdx);
            }

            if (actionIdx != lastActionIndex) {
                repeatActionCount = 0;
            }

            final int act = actionIdx;
            mainHandler.post(() -> executeAction(act));

            lastState = state;
            lastActionIndex = actionIdx;

            // Salvar Q-table a cada 100 frames
            saveCounter++;
            if (saveCounter % 100 == 0) saveQTable();

        } catch (Exception e) {
            Log.e(TAG, "Erro em processFrame", e);
        } finally {
            frame.recycle();
        }
    }

    private int actionIdxAnterior() {
        return lastActionIndex;
    }

    private void executeAction(int actionIdx) {
        switch (actionIdx) {
            case 0: tap(BTN_A1_X, BTN_A1_Y, 70); break;
            case 1: tap(BTN_A2_X, BTN_A2_Y, 70); break;
            case 2: tap(BTN_A3_X, BTN_A3_Y, 70); break;
            case 3: tap(BTN_A4_X, BTN_A4_Y, 70); break;
            case 4: tap(BTN_BACK_X, BTN_BACK_Y, 70); break;
            case 5: tap(BTN_FWD_X, BTN_FWD_Y, 70); break;
            case 6: tap(BTN_UP_X, BTN_UP_Y, 70); break;
            case 7: tap(BTN_DOWN_X, BTN_DOWN_Y, 70); break;
            case 8: holdButton(BTN_R2_X, BTN_R2_Y, 400); break;
            case 9: holdButton(BTN_R1_X, BTN_R1_Y, 250); break;
            case 10: holdButton(BTN_L1_X, BTN_L1_Y, 250); break;
            case 11: break;
            default: break;
        }
    }

    private void tap(float fx, float fy, long durationMs) {
        Path p = new Path();
        p.moveTo(fx * screenWidth, fy * screenHeight);
        GestureDescription.StrokeDescription s =
            new GestureDescription.StrokeDescription(p, 0, durationMs);
        dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
    }

    private void holdButton(float fx, float fy, long durationMs) {
        Path p = new Path();
        p.moveTo(fx * screenWidth, fy * screenHeight);
        GestureDescription.StrokeDescription s =
            new GestureDescription.StrokeDescription(p, 0, durationMs);
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
            if (loaded != null) brain.setQTable(loaded);
        } catch (Exception e) {
            Log.w(TAG, "Q-table invalida.");
        }
    }
}
