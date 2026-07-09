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
    private static final String KEY_QTABLE = "qtable";
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";
    private static final String KEY_POINTS = "points";
    private static final String KEY_HITS = "hits";
    private static final String KEY_COMBOS = "combos";
    private static final String KEY_BLOCKS = "blocks";
    private static final String KEY_FATALITIES = "fatalities";

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
    private boolean inFight = false;
    private boolean fightEnded = false;
    private boolean doingFatality = false;
    private boolean waitingNewFight = false;
    private int lastActionIndex = -1;
    private GameState lastState = null;
    private int pausedFrames = 0;
    private int motionStartFrames = 0;
    private int screenWidth = 1, screenHeight = 1;
    private int consecutiveHits = 0;
    private int p1HpAtEnd = 0;
    private int oppHpAtEnd = 0;

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
        Log.i(TAG, "BotService conectado.");
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
        fightEnded = false;
        doingFatality = false;
        waitingNewFight = false;
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
        screenWidth = frame.getWidth();
        screenHeight = frame.getHeight();
        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) return;

            Log.i(TAG, "HP P1=" + state.p1Hp + " opp=" + state.oppHp +
                       " motion=" + state.motion + " flash=" + state.hitFlash);

            // ENTRAR em luta: motion ALTO (threshold 20) por 3 frames
            // Sem exigir HP, porque a deteccao de cor pode falhar
            if (!inFight && !waitingNewFight && state.motion > 20) {
                motionStartFrames++;
                if (motionStartFrames >= 3) {
                    inFight = true;
                    fightEnded = false;
                    doingFatality = false;
                    waitingNewFight = false;
                    pausedFrames = 0;
                    consecutiveHits = 0;
                    Log.i(TAG, "Luta iniciada. motion=" + state.motion);
                }
            } else if (!inFight) {
                motionStartFrames = 0;
            }

            if (!inFight) {
                return;
            }

            p1HpAtEnd = state.p1Hp;
            oppHpAtEnd = state.oppHp;

            if (state.motion < 5) {
                pausedFrames++;
                if (pausedFrames > 50 && !fightEnded) {
                    boolean win = decideWinner();
                    onFightEnd(win);
                    return;
                }
                if (pausedFrames > 15) {
                    return;
                }
            } else {
                pausedFrames = 0;
            }

            if (fightEnded) {
                return;
            }

            if (state.hitFlash) {
                int hits = prefs.getInt(KEY_HITS, 0) + 1;
                prefs.edit().putInt(KEY_HITS, hits).apply();
                consecutiveHits++;
                if (consecutiveHits >= 2) {
                    int combos = prefs.getInt(KEY_COMBOS, 0) + 1;
                    prefs.edit().putInt(KEY_COMBOS, combos).apply();
                    addPoints(10);
                }
                addPoints(5);
            } else {
                consecutiveHits = 0;
            }

            double reward = 0;
            if (lastState != null && lastActionIndex >= 0) {
                int dmgDealt = lastState.oppHp - state.oppHp;
                int dmgTaken = lastState.p1Hp - state.p1Hp;
                reward = dmgDealt * 1.0 - dmgTaken * 1.0;
                if (state.hitFlash) reward += 2.0;

                if (dmgDealt > 0) addPoints(dmgDealt);
                if (dmgTaken > 0) addPoints(-dmgTaken);

                if (lastActionIndex == 8 && dmgTaken == 0 && state.motion > 5) {
                    int blocks = prefs.getInt(KEY_BLOCKS, 0) + 1;
                    prefs.edit().putInt(KEY_BLOCKS, blocks).apply();
                    addPoints(3);
                }

                if (dmgDealt == 0 && dmgTaken == 0 && !state.hitFlash) reward = -0.1;
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

    private boolean decideWinner() {
        Log.i(TAG, "Decidindo vencedor: P1=" + p1HpAtEnd + " opp=" + oppHpAtEnd);
        if (oppHpAtEnd <= 0 && p1HpAtEnd > 0) return true;
        if (p1HpAtEnd <= 0 && oppHpAtEnd > 0) return false;
        if (p1HpAtEnd > oppHpAtEnd) return true;
        if (oppHpAtEnd > p1HpAtEnd) return false;
        return false;
    }

    private void addPoints(int amount) {
        int points = prefs.getInt(KEY_POINTS, 0) + amount;
        prefs.edit().putInt(KEY_POINTS, points).apply();
    }

    private void onFightEnd(boolean win) {
        if (fightEnded) return;
        fightEnded = true;
        inFight = false;
        waitingNewFight = true;

        int fights = prefs.getInt(KEY_FIGHTS, 0) + 1;
        int wins   = prefs.getInt(KEY_WINS,    0);
        int losses = prefs.getInt(KEY_LOSSES, 0);
        if (win) {
            wins++;
            addPoints(100);
        } else {
            losses++;
            addPoints(-50);
        }
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
        Log.i(TAG, "Luta fim win=" + win + " total=" + fights +
              " V=" + wins + " D=" + losses + " pontos=" + prefs.getInt(KEY_POINTS, 0));

        if (win) {
            doingFatality = true;
            mainHandler.postDelayed(() -> doFatality(), 1500);
            int fatalities = prefs.getInt(KEY_FATALITIES, 0) + 1;
            prefs.edit().putInt(KEY_FATALITIES, fatalities).apply();
            addPoints(30);
        }

        mainHandler.postDelayed(() -> {
            fightEnded = false;
            doingFatality = false;
            waitingNewFight = false;
            lastState = null;
            lastActionIndex = -1;
            Log.i(TAG, "Pronto para nova luta.");
        }, 10000);
    }

    private void doFatality() {
        Log.i(TAG, "Tentando fatality: Head Smash (Fwd, Fwd, Circle)");
        tap(BTN_FWD_X, BTN_FWD_Y, 70);
        mainHandler.postDelayed(() -> tap(BTN_FWD_X, BTN_FWD_Y, 70), 300);
        mainHandler.postDelayed(() -> tap(BTN_A4_X, BTN_A4_Y, 70), 600);
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
            if (loaded != null) {
                brain.setQTable(loaded);
            }
        } catch (Exception e) {
            Log.w(TAG, "Q-table invalida, recomecando do zero.");
        }
    }
}
