package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.hardware.HardwareBuffer;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

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

    // Overlay bloqueante (trava a tela)
    private WindowManager windowManager;
    private View lockOverlay;
    private boolean lockShown = false;

    private boolean volUpHolding = false;
    private boolean volDownHolding = false;
    private Runnable volUpHoldRunnable;
    private Runnable volDownHoldRunnable;

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
        long totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0);
        brain.restoreEpsilonByTime(totalPlay);

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        Log.i(TAG, "BotService conectado.");
    }

    // ============ OVERLAY BLOQUEANTE ============

    // Mostra overlay que cobre a tela e bloqueia toques
    private void showLockOverlay() {
        if (lockShown) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        // Layout com fundo preto semi-transparente + texto
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(0xCC000000); // preto 80% opaco

        TextView text = new TextView(this);
        text.setText("🔒 BOT JOGANDO\n\nSegure Volume− por 3s\npara parar");
        text.setTextColor(Color.WHITE);
        text.setTextSize(20);
        text.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        textParams.gravity = Gravity.CENTER;
        layout.addView(text, textParams);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        // SEM FLAG_NOT_FOCUSABLE = bloqueia toques (nao deixa passar pra baixo)
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        try {
            windowManager.addView(layout, params);
            lockOverlay = layout;
            lockShown = true;
            Log.i(TAG, "Overlay bloqueante ativado");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mostrar overlay", e);
        }
    }

    // Esconde overlay (libera a tela)
    private void hideLockOverlay() {
        if (!lockShown || windowManager == null) return;
        try {
            windowManager.removeView(lockOverlay);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao esconder overlay", e);
        }
        lockOverlay = null;
        lockShown = false;
        Log.i(TAG, "Overlay bloqueante desativado");
    }

    // ============ BOTOES DE VOLUME ============

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                volUpHolding = false;
                volUpHoldRunnable = () -> {
                    volUpHolding = true;
                    setPlayMode(true);
                    showLockOverlay();
                    Log.i(TAG, "Volume+ segurado 3s - BOT JOGANDO + TELA BLOQUEADA");
                };
                mainHandler.postDelayed(volUpHoldRunnable, 3000);
                return true;
            } else if (action == KeyEvent.ACTION_UP) {
                if (volUpHoldRunnable != null) {
                    mainHandler.removeCallbacks(volUpHoldRunnable);
                    volUpHoldRunnable = null;
                }
                if (volUpHolding) {
                    volUpHolding = false;
                    return true;
                } else {
                    adjustVolume(true);
                    return true;
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                volDownHolding = false;
                volDownHoldRunnable = () -> {
                    volDownHolding = true;
                    setPlayMode(false);
                    hideLockOverlay();
                    Log.i(TAG, "Volume- segurado 3s - BOT PARADO + TELA LIBERADA");
                };
                mainHandler.postDelayed(volDownHoldRunnable, 3000);
                return true;
            } else if (action == KeyEvent.ACTION_UP) {
                if (volDownHoldRunnable != null) {
                    mainHandler.removeCallbacks(volDownHoldRunnable);
                    volDownHoldRunnable = null;
                }
                if (volDownHolding) {
                    volDownHolding = false;
                    return true;
                } else {
                    adjustVolume(false);
                    return true;
                }
            }
        }
        return false;
    }

    private void adjustVolume(boolean up) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return;
        int dir = up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir,
                AudioManager.FLAG_SHOW_UI);
    }

    private void setPlayMode(boolean play) {
        SharedPreferences.Editor ed = prefs.edit();
        if (play) {
            ed.putLong(KEY_PLAY_START, System.currentTimeMillis());
        } else {
            long start = prefs.getLong(KEY_PLAY_START, 0);
            if (start > 0) {
                long elapsed = System.currentTimeMillis() - start;
                long total = prefs.getLong(KEY_TOTAL_PLAY, 0) + elapsed;
                ed.putLong(KEY_TOTAL_PLAY, total);
                ed.putLong(KEY_PLAY_START, 0);
            }
        }
        ed.putBoolean(KEY_PLAY_MODE, play).apply();
        BackupManager.save(this, prefs);
    }

    // ============ EVENTOS DE ACESSIBILIDADE ============

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (EMULATOR_PKG.equals(pkg)) {
            // CORRECAO: so inicia o loop se o usuario ativou o bot (KEY_RUNNING=true)
            if (prefs.getBoolean(KEY_RUNNING, false)) {
                startLoop();
            }
        } else if (pkg != null && !pkg.equals("android")) {
            stopLoop();
        }
    }

    @Override
    public void onInterrupt() {
        stopLoop();
    }

    // ============ LOOP ============

    private void startLoop() {
        if (loopRunning) return;
        loopRunning = true;
        mainHandler.post(captureRunnable);
    }

    private void stopLoop() {
        loopRunning = false;
        mainHandler.removeCallbacks(captureRunnable);
        lastState = null;
        lastActionIndex = -1;
        repeatActionCount = 0;
    }

    private final Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
            // Se o usuario desativou o bot, para o loop
            if (!prefs.getBoolean(KEY_RUNNING, false)) {
                stopLoop();
                return;
            }
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

    // ============ PROCESSAMENTO ============

    private void processFrame(Bitmap frame) {
        if (frame == null) return;
        if (!prefs.getBoolean(KEY_PLAY_MODE, false)) {
            frame.recycle();
            return;
        }

        screenWidth = frame.getWidth();
        screenHeight = frame.getHeight();
        try {
            GameState state = detector.detect(frame, screenWidth, screenHeight);
            if (state == null) return;

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
                if (lastActionIndex == 9 && dmgDealt > 0) reward += 5.0;
                if (lastActionIndex == 8 && lastActionIndex == 4 && dmgTaken == 0) reward += 1.0;
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

            saveCounter++;
            if (saveCounter % 100 == 0) {
                saveQTable();
                BackupManager.save(this, prefs);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro em processFrame", e);
        } finally {
            frame.recycle();
        }
    }

    private int actionIdxAnterior() {
        return lastActionIndex;
    }

    // ============ TOQUES - ARSENAL DO SCORPION ============

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
            case 8: holdButton(BTN_R2_X, BTN_R2_Y, 150); break;
            case 9: tap(BTN_R2_X, BTN_R2_Y, 70); break;
            case 10: holdButton(BTN_L1_X, BTN_L1_Y, 250); break;
            case 11: break;
            case 12: scorpionBloodySpear(); break;
            case 13: scorpionHellfire(); break;
            case 14: scorpionBackflipKick(); break;
            case 15: scorpionHellfirePunch(); break;
            case 16: scorpionTripleCombo(); break;
            default: break;
        }
    }

    private void scorpionBloodySpear() {
        tap(BTN_BACK_X, BTN_BACK_Y, 60);
        mainHandler.postDelayed(() -> tap(BTN_FWD_X, BTN_FWD_Y, 60), 80);
        mainHandler.postDelayed(() -> tap(BTN_A1_X, BTN_A1_Y, 70), 160);
    }

    private void scorpionHellfire() {
        tap(BTN_DOWN_X, BTN_DOWN_Y, 60);
        mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
        mainHandler.postDelayed(() -> tap(BTN_A2_X, BTN_A2_Y, 70), 160);
    }

    private void scorpionBackflipKick() {
        tap(BTN_FWD_X, BTN_FWD_Y, 60);
        mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
        mainHandler.postDelayed(() -> tap(BTN_A3_X, BTN_A3_Y, 70), 160);
    }

    private void scorpionHellfirePunch() {
        tap(BTN_FWD_X, BTN_FWD_Y, 60);
        mainHandler.postDelayed(() -> tap(BTN_BACK_X, BTN_BACK_Y, 60), 80);
        mainHandler.postDelayed(() -> tap(BTN_A4_X, BTN_A4_Y, 70), 160);
    }

    private void scorpionTripleCombo() {
        tap(BTN_A2_X, BTN_A2_Y, 70);
        mainHandler.postDelayed(() -> tap(BTN_A2_X, BTN_A2_Y, 70), 150);
        mainHandler.postDelayed(() -> tap(BTN_A3_X, BTN_A3_Y, 70), 300);
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

    // ============ PERSISTENCIA ============

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
