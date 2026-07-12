package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.accessibilityservice.AccessibilityServiceInfo;
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
import android.view.KeyEvent;
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
    private static final String KEY_PLAY_START = "playStart";
    private static final String KEY_TOTAL_PLAY = "totalPlay";
    private static final String KEY_QTABLE = "qtable";
    private static final String KEY_CURRENT_CHAR = "currentChar";

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
    private static final float BTN_JUMPLEFT_X = 0.082f, BTN_JUMPLEFT_Y = 0.639f;
    private static final float BTN_JUMPRIGHT_X = 0.200f, BTN_JUMPRIGHT_Y = 0.635f;

    private Handler mainHandler;
    private Executor bgExecutor;
    private SharedPreferences prefs;
    private Gson gson;
    private QLearningAgent brain;
    private GameDetector detector;
    private String currentCharacter = "Ashrah";

    private boolean loopRunning = false;
    private int lastActionIndex = -1;
    private int repeatActionCount = 0;
    private GameState lastState = null;
    private int screenWidth = 1, screenHeight = 1;
    private int consecutiveHits = 0;
    private int saveCounter = 0;
    private int frameCount = 0;

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
        long totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0);
        brain.restoreEpsilonByTime(totalPlay);
        currentCharacter = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        Log.i(TAG, "BotService conectado.");
    }

    // ============ BOTOES DE VOLUME ============

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        // Se o bot estiver DESATIVADO, nao consome - volume funciona normal
        if (!prefs.getBoolean(KEY_RUNNING, false)) {
            return false;
        }

        // Volume+ = JOGAR (sempre reinicia tudo)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                forceStartPlaying();
                return true;
            }
            return true;
        }

        // Volume- = PARAR
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                setPlayMode(false);
                Log.i(TAG, "Volume- clicado - BOT PARADO");
                return true;
            }
            return true;
        }
        return false;
    }

    // FORCA reinicio total - chamado toda vez que Volume+ e apertado
    private void forceStartPlaying() {
        Log.i(TAG, "Volume+ clicado - REINICIANDO TUDO");

        // 1. Para o loop atual completamente
        loopRunning = false;
        mainHandler.removeCallbacks(captureRunnable);

        // 2. Reseta TODOS os estados
        lastState = null;
        lastActionIndex = -1;
        repeatActionCount = 0;
        consecutiveHits = 0;
        saveCounter = 0;
        frameCount = 0;
        brain.resetLastAction();

        // 3. Atualiza SharedPreferences
        currentCharacter = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(KEY_PLAY_START, System.currentTimeMillis());
        ed.putLong("charStart_" + currentCharacter, System.currentTimeMillis());
        ed.putBoolean(KEY_PLAY_MODE, true);
        ed.apply();

        // 4. Inicia o loop fresco
        loopRunning = true;
        mainHandler.post(captureRunnable);

        Log.i(TAG, "Bot JOGANDO - loop reiniciado");
    }

    private void setPlayMode(boolean play) {
        currentCharacter = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        SharedPreferences.Editor ed = prefs.edit();
        if (play) {
            ed.putLong(KEY_PLAY_START, System.currentTimeMillis());
            ed.putLong("charStart_" + currentCharacter, System.currentTimeMillis());
        } else {
            long start = prefs.getLong(KEY_PLAY_START, 0);
            if (start > 0) {
                long elapsed = System.currentTimeMillis() - start;
                long total = prefs.getLong(KEY_TOTAL_PLAY, 0) + elapsed;
                ed.putLong(KEY_TOTAL_PLAY, total);
                ed.putLong(KEY_PLAY_START, 0);
            }
            long charStart = prefs.getLong("charStart_" + currentCharacter, 0);
            if (charStart > 0) {
                long charElapsed = System.currentTimeMillis() - charStart;
                long charTotal = prefs.getLong("charTime_" + currentCharacter, 0) + charElapsed;
                ed.putLong("charTime_" + currentCharacter, charTotal);
                ed.putLong("charStart_" + currentCharacter, 0);
            }
            saveQTable();
        }
        ed.putBoolean(KEY_PLAY_MODE, play).apply();
        BackupManager.save(this, prefs);
    }

    // ============ EVENTOS DE ACESSIBILIDADE ============

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // NAO faz nada aqui - so o Volume+ controla o bot
        // Isso previne que o bot pare no meio da luta
    }

    @Override
    public void onInterrupt() {
        // NAO para o loop aqui - so Volume- para
    }

    // ============ LOOP ============

    private final Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) return;
            if (!prefs.getBoolean(KEY_RUNNING, false)) {
                loopRunning = false;
                return;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                mainHandler.postDelayed(this, LOOP_INTERVAL_MS);
                return;
            }
            try {
                takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                    new TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(ScreenshotResult result) {
                            try {
                                final Bitmap frame = screenshotToBitmap(result);
                                if (frame != null) {
                                    bgExecutor.execute(() -> processFrame(frame));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erro onSuccess", e);
                            } finally {
                                if (result != null && result.getHardwareBuffer() != null) {
                                    result.getHardwareBuffer().close();
                                }
                            }
                        }
                        @Override
                        public void onFailure(int code) {
                            Log.w(TAG, "takeScreenshot falhou: " + code);
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Erro takeScreenshot", e);
            }
            // SEMPRE reagenda - o loop so para se loopRunning = false
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
            return null;
        }
    }

    // ============ PROCESSAMENTO ============

    private void processFrame(Bitmap frame) {
        if (frame == null) return;

        // Se Volume- foi apertado, para
        if (!prefs.getBoolean(KEY_PLAY_MODE, false)) {
            frame.recycle();
            return;
        }

        currentCharacter = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        screenWidth = frame.getWidth();
        screenHeight = frame.getHeight();
        frameCount++;

        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) {
                frame.recycle();
                return;
            }

            long currentSession = 0;
            long start = prefs.getLong(KEY_PLAY_START, 0);
            if (start > 0) currentSession = System.currentTimeMillis() - start;
            long totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0) + currentSession;
            brain.decayEpsilonByTime(totalPlay);

            if (state.hitFlash) {
                consecutiveHits++;
            } else {
                consecutiveHits = 0;
            }

            double reward = 0;
            if (lastState != null && lastActionIndex >= 0) {
                int dmgDealt = lastState.oppHp - state.oppHp;
                int dmgTaken = lastState.p1Hp - state.p1Hp;
                reward = dmgDealt * 1.0 - dmgTaken * 1.0;
                if (state.hitFlash) reward += 2.0;
                if (lastActionIndex == lastActionIndex) {
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

            if (frameCount % 50 == 0) {
                saveQTable();
                BackupManager.save(this, prefs);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro processFrame", e);
        } finally {
            frame.recycle();
        }
    }

    // ============ ARSENAL POR PERSONAGEM ============

    private void executeAction(int actionIdx) {
        try {
            switch (actionIdx) {
                case 0: tap(BTN_A1_X, BTN_A1_Y, 70); break;
                case 1: tap(BTN_A2_X, BTN_A2_Y, 70); break;
                case 2: tap(BTN_A3_X, BTN_A3_Y, 70); break;
                case 3: tap(BTN_A4_X, BTN_A4_Y, 70); break;
                case 4: tap(BTN_BACK_X, BTN_BACK_Y, 70); break;
                case 5: tap(BTN_FWD_X, BTN_FWD_Y, 70); break;
                case 6: tap(BTN_UP_X, BTN_UP_Y, 70); break;
                case 7: tap(BTN_DOWN_X, BTN_DOWN_Y, 70); break;
                case 8: holdButton(BTN_R2_X, BTN_R2_Y, 250); break;
                case 9: holdButton(BTN_R1_X, BTN_R1_Y, 250); break;
                case 10: holdButton(BTN_L1_X, BTN_L1_Y, 250); break;
                case 11: break;
                case 12: special1(); break;
                case 13: special2(); break;
                case 14: special3(); break;
                case 15: special4(); break;
                case 16: tripleCombo(); break;
                case 17: tap(BTN_JUMPLEFT_X, BTN_JUMPLEFT_Y, 70); break;
                case 18: tap(BTN_JUMPRIGHT_X, BTN_JUMPRIGHT_Y, 70); break;
                case 19: airComboSimple(); break;
                case 20: airComboAdvanced(); break;
                case 21: parryCounter(); break;
                case 22: sidestepUp(); break;
                case 23: sidestepDown(); break;
                default: break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro executeAction", e);
        }
    }

    private void special1() {
        if (currentCharacter.equals("Ashrah")) {
            tap(BTN_DOWN_X, BTN_DOWN_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A1_X, BTN_A1_Y, 70), 160);
        } else {
            tap(BTN_BACK_X, BTN_BACK_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_FWD_X, BTN_FWD_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A1_X, BTN_A1_Y, 70), 160);
        }
    }

    private void special2() {
        if (currentCharacter.equals("Ashrah")) {
            tap(BTN_DOWN_X, BTN_DOWN_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_FWD_X, BTN_FWD_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A1_X, BTN_A1_Y, 70), 160);
        } else {
            tap(BTN_DOWN_X, BTN_DOWN_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A2_X, BTN_A2_Y, 70), 160);
        }
    }

    private void special3() {
        if (currentCharacter.equals("Ashrah")) {
            tap(BTN_DOWN_X, BTN_DOWN_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_UP_X, BTN_UP_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A3_X, BTN_A3_Y, 70), 160);
        } else {
            tap(BTN_FWD_X, BTN_FWD_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A3_X, BTN_A3_Y, 70), 160);
        }
    }

    private void special4() {
        if (currentCharacter.equals("Ashrah")) {
            tap(BTN_FWD_X, BTN_FWD_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_FWD_X, BTN_FWD_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A4_X, BTN_A4_Y, 70), 160);
        } else {
            tap(BTN_FWD_X, BTN_FWD_Y, 60);
            mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
            mainHandler.postDelayed(() -> tap(BTN_A4_X, BTN_A4_Y, 70), 160);
        }
    }

    private void tripleCombo() {
        tap(BTN_A2_X, BTN_A2_Y, 70);
        mainHandler.postDelayed(() -> tap(BTN_A2_X, BTN_A2_Y, 70), 150);
        mainHandler.postDelayed(() -> tap(BTN_A3_X, BTN_A3_Y, 70), 300);
    }

    private void airComboSimple() {
        holdButton(BTN_R1_X, BTN_R1_Y, 250);
        mainHandler.postDelayed(() -> tap(BTN_UP_X, BTN_UP_Y, 70), 300);
        mainHandler.postDelayed(() -> tap(BTN_A1_X, BTN_A1_Y, 70), 500);
        mainHandler.postDelayed(() -> tap(BTN_A2_X, BTN_A2_Y, 70), 650);
    }

    private void airComboAdvanced() {
        holdButton(BTN_R1_X, BTN_R1_Y, 250);
        mainHandler.postDelayed(() -> tap(BTN_UP_X, BTN_UP_Y, 70), 300);
        mainHandler.postDelayed(() -> holdButton(BTN_L1_X, BTN_L1_Y, 100), 500);
        mainHandler.postDelayed(() -> tap(BTN_A1_X, BTN_A1_Y, 70), 650);
        mainHandler.postDelayed(() -> tap(BTN_A2_X, BTN_A2_Y, 70), 800);
    }

    private void parryCounter() {
        tap(BTN_BACK_X, BTN_BACK_Y, 60);
        mainHandler.postDelayed(() -> holdButton(BTN_R2_X, BTN_R2_Y, 200), 50);
    }

    private void sidestepUp() {
        tap(BTN_UP_X, BTN_UP_Y, 50);
        mainHandler.postDelayed(() -> tap(BTN_UP_X, BTN_UP_Y, 50), 60);
    }

    private void sidestepDown() {
        tap(BTN_DOWN_X, BTN_DOWN_Y, 50);
        mainHandler.postDelayed(() -> tap(BTN_DOWN_X, BTN_DOWN_Y, 50), 60);
    }

    private void tap(float fx, float fy, long durationMs) {
        try {
            Path p = new Path();
            p.moveTo(fx * screenWidth, fy * screenHeight);
            GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, durationMs);
            dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Erro tap", e);
        }
    }

    private void holdButton(float fx, float fy, long durationMs) {
        try {
            Path p = new Path();
            p.moveTo(fx * screenWidth, fy * screenHeight);
            GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, durationMs);
            dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Erro hold", e);
        }
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
        }
    }
}
