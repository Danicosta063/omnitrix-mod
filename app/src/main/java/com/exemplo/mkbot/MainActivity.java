package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_PIN = "pin";
    private static final String KEY_PLAY_MODE = "playMode";
    private static final String KEY_PLAY_START = "playStart";
    private static final String KEY_TOTAL_PLAY = "totalPlay";
    private static final String KEY_RUNNING = "running";
    private static final String SERVICE_NAME = "com.exemplo.mkbot/.BotService";

    private SharedPreferences prefs;
    private int appState = 0;
    private Handler handler = new Handler();
    private Handler timerHandler = new Handler();
    private Runnable plusHoldRunnable;
    private Runnable minusHoldRunnable;
    private Runnable plusCountdownRunnable;
    private Runnable minusCountdownRunnable;
    private Runnable timerRunnable;
    private int plusCountdown = 3;
    private int minusCountdown = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getString(KEY_PIN, null) == null) {
            showRegister();
        } else {
            showLogin();
        }
    }

    // ============ TELA DE LOGIN / CADASTRO ============

    private void showLogin() {
        appState = 0;
        setContentView(R.layout.activity_login);
        TextView title = findViewById(R.id.txtLoginTitle);
        title.setText(R.string.enter_pin);
        Button btn = findViewById(R.id.btnEnter);
        btn.setText(R.string.enter);
        EditText edt = findViewById(R.id.edtPin);
        edt.setText("");
        findViewById(R.id.txtLoginError).setVisibility(View.GONE);
        btn.setOnClickListener(v -> {
            String input = edt.getText().toString();
            String pin = prefs.getString(KEY_PIN, "");
            if (input.equals(pin)) {
                showMain();
            } else {
                TextView err = findViewById(R.id.txtLoginError);
                err.setText(R.string.wrong_pin);
                err.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showRegister() {
        appState = 1;
        setContentView(R.layout.activity_login);
        TextView title = findViewById(R.id.txtLoginTitle);
        title.setText(R.string.define_pin);
        Button btn = findViewById(R.id.btnEnter);
        btn.setText(R.string.define);
        EditText edt = findViewById(R.id.edtPin);
        edt.setText("");
        findViewById(R.id.txtLoginError).setVisibility(View.GONE);
        btn.setOnClickListener(v -> {
            String input = edt.getText().toString();
            if (input.length() == 4) {
                prefs.edit().putString(KEY_PIN, input).apply();
                showMain();
            } else {
                TextView err = findViewById(R.id.txtLoginError);
                err.setText(R.string.invalid_pin);
                err.setVisibility(View.VISIBLE);
            }
        });
    }

    // ============ TELA PRINCIPAL ============

    private void showMain() {
        appState = 2;
        setContentView(R.layout.activity_main);

        Button btnA11y = findViewById(R.id.btnA11y);
        Button btnActivate = findViewById(R.id.btnActivate);
        Button btnPlus = findViewById(R.id.btnPlus);
        Button btnMinus = findViewById(R.id.btnMinus);

        btnA11y.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnActivate.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Ative a acessibilidade primeiro",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
            boolean running = prefs.getBoolean(KEY_RUNNING, false);
            prefs.edit().putBoolean(KEY_RUNNING, !running).apply();
            updateActivateButton();
        });

        // Botão +: segurar 3s pra JOGAR (com countdown visual)
        btnPlus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Muda cor imediatamente pra indicar que registrou
                        btnPlus.setBackgroundColor(0xFF4CAF50);
                        btnPlus.setTextColor(0xFFFFFFFF);
                        plusCountdown = 3;
                        btnPlus.setText(String.valueOf(plusCountdown));
                        // Countdown a cada 1 segundo
                        plusCountdownRunnable = new Runnable() {
                            @Override
                            public void run() {
                                plusCountdown--;
                                if (plusCountdown > 0) {
                                    btnPlus.setText(String.valueOf(plusCountdown));
                                    handler.postDelayed(this, 1000);
                                }
                            }
                        };
                        handler.postDelayed(plusCountdownRunnable, 1000);
                        // Dispara apos 3 segundos
                        plusHoldRunnable = () -> {
                            setPlayMode(true);
                            updatePlayUI(true);
                            startTimer();
                            btnPlus.setText("OK");
                            Toast.makeText(MainActivity.this, "Bot jogando!",
                                    Toast.LENGTH_SHORT).show();
                        };
                        handler.postDelayed(plusHoldRunnable, 3000);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Soltou antes de 3s - cancela tudo
                        if (plusHoldRunnable != null) {
                            handler.removeCallbacks(plusHoldRunnable);
                            plusHoldRunnable = null;
                        }
                        if (plusCountdownRunnable != null) {
                            handler.removeCallbacks(plusCountdownRunnable);
                            plusCountdownRunnable = null;
                        }
                        // Volta o botao ao estado normal
                        if (!prefs.getBoolean(KEY_PLAY_MODE, false)) {
                            btnPlus.setBackgroundColor(0xFFCCCCCC);
                            btnPlus.setTextColor(0xFF000000);
                        }
                        btnPlus.setText(R.string.plus);
                        return true;
                }
                return false;
            }
        });

        // Botão -: segurar 3s pra PARAR (com countdown visual)
        btnMinus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btnMinus.setBackgroundColor(0xFFF44336);
                        btnMinus.setTextColor(0xFFFFFFFF);
                        minusCountdown = 3;
                        btnMinus.setText(String.valueOf(minusCountdown));
                        minusCountdownRunnable = new Runnable() {
                            @Override
                            public void run() {
                                minusCountdown--;
                                if (minusCountdown > 0) {
                                    btnMinus.setText(String.valueOf(minusCountdown));
                                    handler.postDelayed(this, 1000);
                                }
                            }
                        };
                        handler.postDelayed(minusCountdownRunnable, 1000);
                        minusHoldRunnable = () -> {
                            setPlayMode(false);
                            updatePlayUI(false);
                            stopTimer();
                            btnMinus.setText("OK");
                            Toast.makeText(MainActivity.this, "Bot parado!",
                                    Toast.LENGTH_SHORT).show();
                        };
                        handler.postDelayed(minusHoldRunnable, 3000);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (minusHoldRunnable != null) {
                            handler.removeCallbacks(minusHoldRunnable);
                            minusHoldRunnable = null;
                        }
                        if (minusCountdownRunnable != null) {
                            handler.removeCallbacks(minusCountdownRunnable);
                            minusCountdownRunnable = null;
                        }
                        if (prefs.getBoolean(KEY_PLAY_MODE, false)) {
                            btnMinus.setBackgroundColor(0xFFCCCCCC);
                            btnMinus.setTextColor(0xFF000000);
                        }
                        btnMinus.setText(R.string.minus);
                        return true;
                }
                return false;
            }
        });

        updateActivateButton();
        updatePlayUI(prefs.getBoolean(KEY_PLAY_MODE, false));
        updateHours();
        if (prefs.getBoolean(KEY_PLAY_MODE, false)) startTimer();
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
    }

    private void updatePlayUI(boolean playing) {
        if (appState != 2) return;
        Button btnPlus = findViewById(R.id.btnPlus);
        Button btnMinus = findViewById(R.id.btnMinus);
        TextView txtStatus = findViewById(R.id.txtStatus);
        if (btnPlus == null) return;
        if (playing) {
            btnPlus.setBackgroundColor(0xFF4CAF50);
            btnPlus.setTextColor(0xFFFFFFFF);
            btnPlus.setText(R.string.plus);
            btnMinus.setBackgroundColor(0xFFCCCCCC);
            btnMinus.setTextColor(0xFF000000);
            btnMinus.setText(R.string.minus);
            txtStatus.setText("Bot jogando");
            txtStatus.setTextColor(0xFF4CAF50);
        } else {
            btnPlus.setBackgroundColor(0xFFCCCCCC);
            btnPlus.setTextColor(0xFF000000);
            btnPlus.setText(R.string.plus);
            btnMinus.setBackgroundColor(0xFFF44336);
            btnMinus.setTextColor(0xFFFFFFFF);
            btnMinus.setText(R.string.minus);
            txtStatus.setText("Bot parado");
            txtStatus.setTextColor(0xFF888888);
        }
    }

    private void updateActivateButton() {
        if (appState != 2) return;
        Button btnActivate = findViewById(R.id.btnActivate);
        if (btnActivate == null) return;
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        btnActivate.setText(running ? R.string.deactivate_bot : R.string.activate_bot);
    }

    // ============ CONTADOR DE HORAS ============

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (prefs.getBoolean(KEY_PLAY_MODE, false)) {
                    updateHours();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        updateHours();
    }

    private void updateHours() {
        if (appState != 2) return;
        TextView txtHours = findViewById(R.id.txtHours);
        if (txtHours == null) return;
        long total = prefs.getLong(KEY_TOTAL_PLAY, 0);
        long current = 0;
        if (prefs.getBoolean(KEY_PLAY_MODE, false)) {
            long start = prefs.getLong(KEY_PLAY_START, 0);
            if (start > 0) current = System.currentTimeMillis() - start;
        }
        long ms = total + current;
        long sec = ms / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        txtHours.setText("Horas jogadas: " +
                String.format("%02d:%02d:%02d", h, m, s));
    }

    // ============ ACESSIBILIDADE ============

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabled =
                am.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : enabled) {
            if (TextUtils.equals(info.getId(), SERVICE_NAME)) return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appState == 2) {
            updateActivateButton();
            updateHours();
        }
    }
}
