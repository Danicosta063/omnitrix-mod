package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
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
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

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

    private void showMain() {
        appState = 2;
        setContentView(R.layout.activity_main);

        Button btnA11y = findViewById(R.id.btnA11y);
        Button btnActivate = findViewById(R.id.btnActivate);

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

        updateActivateButton();
        updatePlayUI();
        updateHours();
        startTimer();
    }

    private void updateActivateButton() {
        if (appState != 2) return;
        Button btnActivate = findViewById(R.id.btnActivate);
        if (btnActivate == null) return;
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        btnActivate.setText(running ? R.string.deactivate_bot : R.string.activate_bot);
    }

    private void updatePlayUI() {
        if (appState != 2) return;
        TextView txtStatus = findViewById(R.id.txtStatus);
        if (txtStatus == null) return;
        boolean playing = prefs.getBoolean(KEY_PLAY_MODE, false);
        if (playing) {
            txtStatus.setText("Bot jogando");
            txtStatus.setTextColor(0xFF4CAF50);
        } else {
            txtStatus.setText("Bot parado");
            txtStatus.setTextColor(0xFF888888);
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (appState == 2) {
                    updatePlayUI();
                    updateHours();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
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
            updatePlayUI();
            updateHours();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
