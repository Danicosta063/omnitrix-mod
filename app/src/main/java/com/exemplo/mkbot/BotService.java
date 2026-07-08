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
import android.util.DisplayMetrics;
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
    private static final String KEY_QTABLE = "qtable";
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";

    private static final String EMULATOR_PKG = "xyz.aethersx2.cturnip";
    private static final long LOOP_INTERVAL_MS = 120;

    private static final float BTN_A1_X = 0.82f, BTN_A1_Y = 0.82f;
    private static final float BTN_A2_X = 0.88f, BTN_A2_Y = 0.76f;
    private static final float BTN_A3_X = 0.94f, BTN_A3_Y = 0.82f;
    private static final float BTN_A4_X = 0.88f, BTN_A4_Y = 0.88f;
    private static final float BTN_BACK_X = 0.12f, BTN_BACK_Y = 0.80f;
    private static final float BTN_FWD_X = 0.22f, BTN_FWD_Y = 0.80f;
    private static final float BTN_UP_X = 0.16f, BTN_UP_Y = 0.72f;
    private static final float BTN_DOWN_X = 0.16f, BTN_DOWN_Y = 0.88f;
    private static final float BTN_R1_X = 0.96f, BTN_R1_Y = 0.65f;
    private static final float BTN_R2_X = 0.76f, BTN_R2_Y = 0.65f;

    private Handler mainHandler;
    private Executor bgExecutor;
    private SharedPreferences prefs;
    private Gson gson;
    private QLearningAgent brain;
    private GameDetector detector;

    private boolean loopRunning = false;
    private boolean inFight = false;
    private int lastActionIndex = -1;
    private GameState lastState = null;
    private int screenWidth, screenHeight;

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
        mainHandler.post(captureRunnable);
    }

    private void stopLoop() {
        loopRunning = false;
        mainHandler.removeCallbacks(captureRunnable);
        inFight = false;
        lastState = null;
        lastActionIndex = -1;
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
        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) return;

            if (!inFight && state.p1Hp > 0 && state.oppHp > 0) {
                inFight = true;
            }

            if (inFight) {
                if (state.oppHp <= 0) { onFightEnd(true);  return; }
                if (state.p1Hp  <= 0) { onFightEnd(false); return; }
            }

            double reward = 0;
            if (lastState != null && lastActionIndex >= 0 && inFight) {
                int dmgDealt = lastState.oppHp - state.oppHp;
                int dmgTaken = lastState.p1Hp - state.p1Hp;
                reward = dmgDealt * 1.0 - dmgTaken * 1.0;
                if (dmgDealt == 0 && dmgTaken == 0) reward = -0.1;
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
        int wins   = prefs.getInt(KEY_WINS,    0);
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
        lastState = null;
        lastActionIndex = -1;
    }

    private void executeAction(int actionIdx) {
        switch (actionIdx) {
            case 0: tap(BTN_A1_X, BTN_A1_Y); break;
            case 1: tap(BTN_A2_X, BTN_A2_Y); break;
            case 2: tap(BTN_A3_X, BTN_A3_Y); break;
            case 3: tap(BTN_A4_X, BTN_A4_Y); break;
            case 4: tap(BTN_BACK_X, BTN_BACK_Y); break;
            case 5: tap(BTN_FWD_X, BTN_FWD_Y); break;
            case 6: tap(BTN_UP_X, BTN_UP_Y); break;
            case 7: tap(BTN_DOWN_X, BTN_DOWN_Y); break;
            case 8: tap(BTN_R2_X, BTN_R2_Y); break;
            case 9: tap(BTN_R1_X, BTN_R1_Y); break;
            case 10: break;
            default: break;
        }
    }

    private void tap(float fx, float fy) {
        Path p = new Path();
        p.moveTo(fx * screenWidth, fy * screenHeight);
        GestureDescription.StrokeDescription s =
            new GestureDescription.StrokeDescription(p, 0, 70);
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
            }
        } catch (Exception e) {
            Log.w(TAG, "Q-table invalida, recomecando do zero.");
        }
    }
}
