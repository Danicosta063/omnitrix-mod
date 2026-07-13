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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.exemplo.mkbot.brain.QLearningAgent;
import com.exemplo.mkbot.vision.GameDetector;
import com.exemplo.mkbot.vision.GameDetector.GameState;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";
    private static final String KEY_POINTS = "points";
    private static final String KEY_BUTTONS = "buttonsJson";

    private static final long LOOP_INTERVAL_MS = 100;

    // IDs das acoes (16 total)
    private static final int ACT_A1 = 0;
    private static final int ACT_A2 = 1;
    private static final int ACT_A3 = 2;
    private static final int ACT_A4 = 3;
    private static final int ACT_BACK = 4;
    private static final int ACT_FWD = 5;
    private static final int ACT_UP = 6;
    private static final int ACT_DOWN = 7;
    private static final int ACT_BLOCK = 8;
    private static final int ACT_THROW = 9;
    private static final int ACT_STYLE = 10;
    private static final int ACT_SPECIAL1 = 11;
    private static final int ACT_SPECIAL2 = 12;
    private static final int ACT_SPECIAL3 = 13;
    private static final int ACT_SPECIAL4 = 14;
    private static final int ACT_NOOP = 15;

    private static final int MOVE_HOLD_FRAMES = 10;

    // ============================================================
    //  Calibracao por voz
    // ============================================================

    private static final int CALIB_OFF = 0;
    private static final int CALIB_ACTIVE = 1;

    private static final String[] CALIB_KEYS = {
            ButtonMap.A1, ButtonMap.A2, ButtonMap.A3, ButtonMap.A4,
            ButtonMap.UP, ButtonMap.DOWN, ButtonMap.BACK, ButtonMap.FWD,
            ButtonMap.R1, ButtonMap.R2, ButtonMap.L1, ButtonMap.L2
    };

    private static final String[] CALIB_NAMES_PT = {
            "Toque no botao Quadrado",
            "Toque no botao Triangulo",
            "Toque no botao Equis",
            "Toque no botao Circulo",
            "Toque no botao Cima",
            "Toque no botao Baixo",
            "Toque no botao Esquerda",
            "Toque no botao Direita",
            "Toque no botao Erre Um, Especial",
            "Toque no botao Erre Dois, Bloqueio",
            "Toque no botao Ele Um, Trocar Estilo",
            "Toque no botao Ele Dois, Pegar Arma"
    };

    // ============================================================
    //  Campos principais
    // ============================================================

    private Handler mainHandler;
    private Executor bgExecutor;
    private SharedPreferences prefs;
    private Gson gson;
    private QLearningAgent brain;
    private GameDetector detector;
    private ButtonMap buttonMap;
    private String currentCharacter = "Ashrah";

    private boolean loopRunning = false;
    private int lastActionIndex = -1;
    private int moveHoldCount = 0;
    private int moveHoldAction = -1;
    private GameState lastState = null;
    private int screenWidth = 1, screenHeight = 1;
    private int consecutiveHits = 0;
    private int saveCounter = 0;
    private int frameCount = 0;

    private boolean inFight = false;
    private int fightConfirmFrames = 0;
    private int pausedFrames = 0;
    private int blackScreenFrames = 0;
    private int p1HpAtEnd = 0;
    private int oppHpAtEnd = 0;
    private boolean fightEnded = false;

    // Calibracao
    private int calibState = CALIB_OFF;
    private int calibIndex = 0;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean calibPendingStart = false;
    private WindowManager windowManager;
    private FrameLayout overlayLayout;
    private TextView overlayText;
    private Map<String, float[]> calibButtons;
    private int calibScreenW = 1, calibScreenH = 1;

    // ============================================================
    //  Conexao do servico
    // ============================================================

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mainHandler = new Handler(Looper.getMainLooper());
        bgExecutor = Executors.newSingleThreadExecutor();
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        gson = new Gson();
        brain = new QLearningAgent();
        detector = new GameDetector();
        buttonMap = new ButtonMap(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        calibButtons = new HashMap<>();
        loadQTable();
        long totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0);
        brain.restoreEpsilonByTime(totalPlay);
        currentCharacter = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");

        // Inicializa TTS em portugues
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(new Locale("pt", "BR"));
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault());
                    }
                    ttsReady = true;
                    Log.i(TAG, "TTS pronto.");
                    if (calibPendingStart) {
                        calibPendingStart = false;
                        startCalibration();
                    }
                } else {
                    Log.w(TAG, "TTS falhou ao iniciar.");
                    ttsReady = false;
                }
            }
        });

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        Log.i(TAG, "BotService conectado.");
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        removeOverlay();
        super.onDestroy();
    }

    // ============================================================
    //  Botoes de Volume
    // ============================================================

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if (!prefs.getBoolean(KEY_RUNNING, false)) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (calibState == CALIB_ACTIVE) {
                    // Se ja esta calibrando, Volume+ cancela e recomeca
                    cancelCalibration();
                    return true;
                }
                if (!buttonMap.isFullyCalibrated()) {
                    // Precisa calibrar primeiro
                    forceStartCalibration();
                    return true;
                }
                // Ja calibrado, joga
                forceStartPlaying();
                return true;
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (calibState == CALIB_ACTIVE) {
                    cancelCalibration();
                    return true;
                }
                setPlayMode(false);
                Log.i(TAG, "Volume- clicado - BOT PARADO");
                return true;
            }
            return true;
        }
        return false;
    }

    // ============================================================
    //  Calibracao por voz
    // ============================================================

    private void forceStartCalibration() {
        Log.i(TAG, "Volume+ clicado - INICIANDO CALIBRACAO POR VOZ");
        calibIndex = 0;
        calibButtons = new HashMap<>();

        // Pegar dimensoes reais da tela
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        calibScreenW = metrics.widthPixels;
        calibScreenH = metrics.heightPixels;
        Log.i(TAG, "Tela calibracao: " + calibScreenW + "x" + calibScreenH);

        if (ttsReady) {
            startCalibration();
        } else {
            calibPendingStart = true;
            Log.i(TAG, "Aguardando TTS ficar pronto...");
        }
    }

    private void startCalibration() {
        calibState = CALIB_ACTIVE;
        showOverlay();
        speak("Calibracao iniciada. " + CALIB_NAMES_PT[0]);
        updateOverlayText(CALIB_NAMES_PT[0]);
        Log.i(TAG, "Calibracao ativa. Botao 1: " + CALIB_KEYS[0]);
    }

    private void onCalibTap(float x, float y) {
        if (calibState != CALIB_ACTIVE) return;
        if (calibIndex >= CALIB_KEYS.length) return;

        float fx = x / calibScreenW;
        float fy = y / calibScreenH;
        String key = CALIB_KEYS[calibIndex];
        calibButtons.put(key, new float[]{fx, fy});

        Log.i(TAG, "Calibrado " + key + " = (" + fx + ", " + fy + ")");

        calibIndex++;

        if (calibIndex >= CALIB_KEYS.length) {
            // Fim da calibracao
            finishCalibration();
        } else {
            String nextMsg = CALIB_NAMES_PT[calibIndex];
            speak(nextMsg);
            updateOverlayText(nextMsg);
            Log.i(TAG, "Proximo botao: " + CALIB_KEYS[calibIndex]);
        }
    }

    private void finishCalibration() {
        calibState = CALIB_OFF;
        // Salvar no SharedPreferences
        String json = gson.toJson(calibButtons);
        prefs.edit().putString(KEY_BUTTONS, json).apply();
        BackupManager.save(this, prefs);
        // Recarregar ButtonMap
        buttonMap = new ButtonMap(this);
        removeOverlay();
        speak("Calibracao concluida com sucesso. O bot vai comecar a jogar agora.");
        Log.i(TAG, "Calibracao concluida. " + calibButtons.size() + " botoes salvos.");
        // Iniciar jogo
        mainHandler.postDelayed(() -> forceStartPlaying(), 2000);
    }

    private void cancelCalibration() {
        calibState = CALIB_OFF;
        calibIndex = 0;
        removeOverlay();
        speak("Calibracao cancelada.");
        Log.i(TAG, "Calibracao cancelada.");
    }

    private void speak(String text) {
        if (tts != null && ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "calib_msg");
        }
    }

    // ============================================================
    //  Overlay para capturar toques durante calibracao
    // ============================================================

    private void showOverlay() {
        if (overlayLayout != null) {
            removeOverlay();
        }

        overlayLayout = new FrameLayout(this);
        overlayLayout.setBackgroundColor(Color.TRANSPARENT);

        overlayText = new TextView(this);
        overlayText.setText(CALIB_NAMES_PT[0]);
        overlayText.setTextSize(18f);
        overlayText.setTextColor(Color.WHITE);
        overlayText.setBackgroundColor(Color.argb(200, 0, 0, 0));
        overlayText.setPadding(30, 40, 30, 40);
        overlayText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        textParams.topMargin = 20;
        overlayLayout.addView(overlayText, textParams);

        overlayLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (calibState != CALIB_ACTIVE) return false;
                    float x = event.getRawX();
                    float y = event.getRawY();
                    // Ignora toques na area do texto (topo da tela)
                    if (y < 120) return true;
                    onCalibTap(x, y);
                    return true;
                }
                return false;
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        try {
            windowManager.addView(overlayLayout, params);
            Log.i(TAG, "Overlay de calibracao mostrado.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao mostrar overlay", e);
        }
    }

    private void updateOverlayText(String text) {
        if (overlayText != null) {
            mainHandler.post(() -> overlayText.setText(text));
        }
    }

    private void removeOverlay() {
        if (overlayLayout != null) {
            try {
                windowManager.removeView(overlayLayout);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao remover overlay", e);
            }
            overlayLayout = null;
            overlayText = null;
        }
    }

    // ============================================================
    //  Iniciar jogo
    // ============================================================

    private void forceStartPlaying() {
        Log.i(TAG, "Volume+ clicado - REINICIANDO TUDO");
        loopRunning = false;
        mainHandler.removeCallbacks(captureRunnable);
        lastState = null;
        lastActionIndex = -1;
        moveHoldCount = 0;
        moveHoldAction = -1;
        consecutiveHits = 0;
        saveCounter = 0;
        frameCount = 0;
        inFight = false;
        fightConfirmFrames = 0;
        pausedFrames = 0;
        blackScreenFrames = 0;
        fightEnded = false;
        brain.resetLastAction();
        detector.reset();
        buttonMap = new ButtonMap(this);
        currentCharacter = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");

        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(KEY_PLAY_START, System.currentTimeMillis());
        ed.putLong("charStart_" + currentCharacter, System.currentTimeMillis());
        ed.putBoolean(KEY_PLAY_MODE, true);
        ed.apply();

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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    // ============================================================
    //  Loop de captura
    // ============================================================

    private final Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning) return;
            if (!prefs.getBoolean(KEY_RUNNING, false)) {
                loopRunning = false;
                return;
            }
            if (calibState == CALIB_ACTIVE) {
                mainHandler.postDelayed(this, LOOP_INTERVAL_MS);
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

    // ============================================================
    //  Processamento de frame
    // ============================================================

    private void processFrame(Bitmap frame) {
        if (frame == null) return;
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

            if (state.blackScreen) {
                blackScreenFrames++;
                if (blackScreenFrames > 5) {
                    if (inFight && !fightEnded) {
                        onFightEnd(false);
                    }
                    inFight = false;
                    fightConfirmFrames = 0;
                    frame.recycle();
                    return;
                }
            } else {
                blackScreenFrames = 0;
            }

            if (!inFight && !fightEnded && !state.blackScreen
                    && state.p1Hp > 0 && state.oppHp > 0
                    && state.motion > 15) {
                fightConfirmFrames++;
                if (fightConfirmFrames >= 3) {
                    inFight = true;
                    fightEnded = false;
                    pausedFrames = 0;
                    consecutiveHits = 0;
                    brain.resetLastAction();
                    detector.reset();
                    Log.i(TAG, "Luta iniciada. P1=" + state.p1Hp +
                              " opp=" + state.oppHp + " motion=" + state.motion);
                }
            } else if (!inFight) {
                fightConfirmFrames = 0;
            }

            if (!inFight) {
                frame.recycle();
                return;
            }

            p1HpAtEnd = state.p1Hp;
            oppHpAtEnd = state.oppHp;

            if (state.oppHp <= 0 && state.p1Hp > 0) {
                onFightEnd(true);
                frame.recycle();
                return;
            }
            if (state.p1Hp <= 0 && state.oppHp > 0) {
                onFightEnd(false);
                frame.recycle();
                return;
            }

            if (state.motion < 5) {
                pausedFrames++;
                if (pausedFrames > 60 && !fightEnded) {
                    boolean win = decideWinner();
                    onFightEnd(win);
                    frame.recycle();
                    return;
                }
                if (pausedFrames > 10) {
                    frame.recycle();
                    return;
                }
            } else {
                pausedFrames = 0;
            }

            if (fightEnded) {
                frame.recycle();
                return;
            }

            boolean comboHit = false;
            if (state.hitFlash) {
                consecutiveHits++;
                if (consecutiveHits >= 2) {
                    comboHit = true;
                    addPoints(10);
                }
                addPoints(5);
            } else {
                consecutiveHits = 0;
            }

            if (moveHoldCount > 0 && moveHoldAction >= 0) {
                moveHoldCount--;
                final int act = moveHoldAction;
                mainHandler.post(() -> executeAction(act));
                frame.recycle();
                return;
            }

            float reward = brain.computeReward(lastState, state, lastActionIndex, comboHit);

            int stateIdx = brain.discretizeState(state);
            int actionIdx = brain.chooseAction(stateIdx);

            if (lastState != null && lastActionIndex >= 0) {
                int lastIdx = brain.discretizeState(lastState, lastActionIndex);
                int nextIdx = brain.discretizeState(state, actionIdx);
                brain.update(lastIdx, lastActionIndex, reward, nextIdx, actionIdx);
            }

            if (actionIdx == ACT_BACK || actionIdx == ACT_FWD
                    || actionIdx == ACT_UP || actionIdx == ACT_DOWN) {
                moveHoldCount = MOVE_HOLD_FRAMES - 1;
                moveHoldAction = actionIdx;
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
            Log.e(TAG, "Erro processFrame", e);
        } finally {
            frame.recycle();
        }
    }

    // ============================================================
    //  Fim de luta
    // ============================================================

    private boolean decideWinner() {
        if (oppHpAtEnd <= 0 && p1HpAtEnd > 0) return true;
        if (p1HpAtEnd <= 0 && oppHpAtEnd > 0) return false;
        if (p1HpAtEnd > oppHpAtEnd) return true;
        return false;
    }

    private void onFightEnd(boolean win) {
        if (fightEnded) return;
        fightEnded = true;
        inFight = false;

        int fights = prefs.getInt(KEY_FIGHTS, 0) + 1;
        int wins = prefs.getInt(KEY_WINS, 0);
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

        float terminal = win ? QLearningAgent.R_WIN : QLearningAgent.R_LOSS;
        if (lastState != null && lastActionIndex >= 0) {
            int idx = brain.discretizeState(lastState, lastActionIndex);
            brain.update(idx, lastActionIndex, terminal, idx, 0);
        }
        saveQTable();
        Log.i(TAG, "Luta fim win=" + win + " total=" + fights +
              " V=" + wins + " D=" + losses);

        mainHandler.postDelayed(() -> {
            fightEnded = false;
            inFight = false;
            lastState = null;
            lastActionIndex = -1;
            moveHoldCount = 0;
            moveHoldAction = -1;
            fightConfirmFrames = 0;
            pausedFrames = 0;
            consecutiveHits = 0;
            brain.resetLastAction();
            detector.reset();
            Log.i(TAG, "Pronto para nova luta.");
        }, 3000);
    }

    private void addPoints(int amount) {
        int points = prefs.getInt(KEY_POINTS, 0) + amount;
        prefs.edit().putInt(KEY_POINTS, points).apply();
    }

    // ============================================================
    //  Execucao de acoes
    // ============================================================

    private void executeAction(int actionIdx) {
        try {
            switch (actionIdx) {
                case ACT_A1: tapBtn(ButtonMap.A1, 70); break;
                case ACT_A2: tapBtn(ButtonMap.A2, 70); break;
                case ACT_A3: tapBtn(ButtonMap.A3, 70); break;
                case ACT_A4: tapBtn(ButtonMap.A4, 70); break;
                case ACT_BACK: tapBtn(ButtonMap.BACK, 80); break;
                case ACT_FWD: tapBtn(ButtonMap.FWD, 80); break;
                case ACT_UP: tapBtn(ButtonMap.UP, 80); break;
                case ACT_DOWN: tapBtn(ButtonMap.DOWN, 80); break;
                case ACT_BLOCK: holdBtn(ButtonMap.R2, 200); break;
                case ACT_THROW: tapBtn(ButtonMap.R2, 70); break;
                case ACT_STYLE: tapBtn(ButtonMap.L1, 70); break;
                case ACT_SPECIAL1: special1(); break;
                case ACT_SPECIAL2: special2(); break;
                case ACT_SPECIAL3: special3(); break;
                case ACT_SPECIAL4: special4(); break;
                case ACT_NOOP: break;
                default: break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro executeAction", e);
        }
    }

    // ---- Especiais da Ashrah ----
    // Heavenly Light: D, B + A1
    private void special1() {
        if (currentCharacter.equals("Ashrah")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.BACK, ButtonMap.A1}, 50);
        } else if (currentCharacter.equals("Scorpion")) {
            tapBtnSeq(new String[]{ButtonMap.BACK, ButtonMap.FWD, ButtonMap.A1}, 50);
        } else if (currentCharacter.equals("Sub-Zero")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.FWD, ButtonMap.A1}, 50);
        } else {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.BACK, ButtonMap.A1}, 50);
        }
    }

    // Lightning Blast: D, F + A1
    private void special2() {
        if (currentCharacter.equals("Ashrah")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.FWD, ButtonMap.A1}, 50);
        } else if (currentCharacter.equals("Scorpion")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.BACK, ButtonMap.A2}, 50);
        } else if (currentCharacter.equals("Sub-Zero")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.BACK, ButtonMap.A1}, 50);
        } else {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.FWD, ButtonMap.A1}, 50);
        }
    }

    // Spin Cycle: D, U + A3
    private void special3() {
        if (currentCharacter.equals("Ashrah")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.UP, ButtonMap.A3}, 50);
        } else if (currentCharacter.equals("Scorpion")) {
            tapBtnSeq(new String[]{ButtonMap.FWD, ButtonMap.BACK, ButtonMap.A3}, 50);
        } else if (currentCharacter.equals("Sub-Zero")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.FWD, ButtonMap.A2}, 50);
        } else {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.UP, ButtonMap.A3}, 50);
        }
    }

    // Nature's Torpedo: F, F + A4
    private void special4() {
        if (currentCharacter.equals("Ashrah")) {
            tapBtnSeq(new String[]{ButtonMap.FWD, ButtonMap.FWD, ButtonMap.A4}, 50);
        } else if (currentCharacter.equals("Scorpion")) {
            tapBtnSeq(new String[]{ButtonMap.FWD, ButtonMap.BACK, ButtonMap.A4}, 50);
        } else if (currentCharacter.equals("Sub-Zero")) {
            tapBtnSeq(new String[]{ButtonMap.DOWN, ButtonMap.BACK, ButtonMap.A3}, 50);
        } else {
            tapBtnSeq(new String[]{ButtonMap.FWD, ButtonMap.FWD, ButtonMap.A4}, 50);
        }
    }

    // ============================================================
    //  Toques e gestos
    // ============================================================

    private void tapBtn(String key, long durationMs) {
        float[] coord = buttonMap.get(key, screenWidth, screenHeight);
        if (coord == null) return;
        try {
            Path p = new Path();
            p.moveTo(coord[0], coord[1]);
            GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, durationMs);
            dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Erro tapBtn " + key, e);
        }
    }

    private void holdBtn(String key, long durationMs) {
        float[] coord = buttonMap.get(key, screenWidth, screenHeight);
        if (coord == null) return;
        try {
            Path p = new Path();
            p.moveTo(coord[0], coord[1]);
            GestureDescription.StrokeDescription s =
                new GestureDescription.StrokeDescription(p, 0, durationMs);
            dispatchGesture(new GestureDescription.Builder().addStroke(s).build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Erro holdBtn " + key, e);
        }
    }

    private void tapBtnSeq(String[] keys, long delayMs) {
        if (keys == null || keys.length == 0) return;
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final long dur = 60;
            mainHandler.postDelayed(() -> tapBtn(key, dur), i * delayMs);
        }
    }

    // ============================================================
    //  Save / Load Q-table
    // ============================================================

    private void saveQTable() {
        try {
            prefs.edit().putString(KEY_QTABLE, gson.toJson(brain.getQTable())).apply();
        } catch (Exception e) {
            Log.e(TAG, "Erro saveQTable", e);
        }
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
            Log.w(TAG, "Q-table invalida, recomecando do zero.");
        }
    }
}
