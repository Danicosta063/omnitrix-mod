package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";
    private static final String KEY_POINTS = "points";
    private static final String KEY_HITS = "hits";
    private static final String KEY_COMBOS = "combos";
    private static final String KEY_BLOCKS = "blocks";
    private static final String KEY_QTABLE = "qtable";

    private static final String SERVICE_NAME = "com.exemplo.mkbot/.BotService";

    private Button btnToggle, btnEnableA11y, btnReset;
    private TextView txtStatus, txtTraining, txtWins, txtLosses, txtWinRate;
    private TextView txtPoints, txtHits, txtCombos, txtBlocks;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        btnToggle = findViewById(R.id.btnToggle);
        btnEnableA11y = findViewById(R.id.btnEnableA11y);
        btnReset = findViewById(R.id.btnReset);
        txtStatus = findViewById(R.id.txtStatus);
        txtTraining = findViewById(R.id.txtTraining);
        txtWins = findViewById(R.id.txtWins);
        txtLosses = findViewById(R.id.txtLosses);
        txtWinRate = findViewById(R.id.txtWinRate);
        txtPoints = findViewById(R.id.txtPoints);
        txtHits = findViewById(R.id.txtHits);
        txtCombos = findViewById(R.id.txtCombos);
        txtBlocks = findViewById(R.id.txtBlocks);

        btnToggle.setOnClickListener(v -> onToggle());
        btnEnableA11y.setOnClickListener(v -> openAccessibilitySettings());
        btnReset.setOnClickListener(v -> onReset());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
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

    private void onToggle() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "Ative a acessibilidade primeiro.",
                    Toast.LENGTH_LONG).show();
            openAccessibilitySettings();
            return;
        }
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        prefs.edit().putBoolean(KEY_RUNNING, !running).apply();
        updateUI();
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void onReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_qtable)
                .setMessage(R.string.reset_confirm)
                .setPositiveButton("Sim", (d, w) -> {
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.remove(KEY_QTABLE);
                    ed.putInt(KEY_FIGHTS, 0);
                    ed.putInt(KEY_WINS, 0);
                    ed.putInt(KEY_LOSSES, 0);
                    ed.putInt(KEY_POINTS, 0);
                    ed.putInt(KEY_HITS, 0);
                    ed.putInt(KEY_COMBOS, 0);
                    ed.putInt(KEY_BLOCKS, 0);
                    ed.apply();
                    updateUI();
                    Toast.makeText(this, "Aprendizado resetado.",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void updateUI() {
        boolean a11y = isAccessibilityEnabled();
        boolean running = prefs.getBoolean(KEY_RUNNING, false);

        if (!a11y) {
            btnToggle.setText(R.string.bot_off);
            txtStatus.setText("Acessibilidade desativada");
        } else if (running) {
            btnToggle.setText(R.string.bot_on);
            txtStatus.setText("Bot rodando");
        } else {
            btnToggle.setText(R.string.bot_off);
            txtStatus.setText("Bot parado (a11y ativa)");
        }

        int fights = prefs.getInt(KEY_FIGHTS, 0);
        int wins = prefs.getInt(KEY_WINS, 0);
        int losses = prefs.getInt(KEY_LOSSES, 0);
        int points = prefs.getInt(KEY_POINTS, 0);
        int hits = prefs.getInt(KEY_HITS, 0);
        int combos = prefs.getInt(KEY_COMBOS, 0);
        int blocks = prefs.getInt(KEY_BLOCKS, 0);
        float winrate = (fights > 0) ? (100f * wins / fights) : 0f;

        txtTraining.setText(getString(R.string.training_label, fights));
        txtWins.setText(getString(R.string.wins_label, wins));
        txtLosses.setText(getString(R.string.losses_label, losses));
        txtWinRate.setText(getString(R.string.winrate_label, winrate));
        txtPoints.setText(getString(R.string.points_label, points));
        txtHits.setText(getString(R.string.hits_label, hits));
        txtCombos.setText(getString(R.string.combos_label, combos));
        txtBlocks.setText(getString(R.string.blocks_label, blocks));
    }
}
