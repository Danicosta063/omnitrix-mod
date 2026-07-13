package com.exemplo.mkbot;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PLAY_MODE = "playMode";
    private static final String KEY_PLAY_START = "playStart";
    private static final String KEY_TOTAL_PLAY = "totalPlay";
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";
    private static final String KEY_POINTS = "points";
    private static final String KEY_CURRENT_CHAR = "currentChar";
    private static final String KEY_FOLDER_URI = "folderUri";
    private static final String SERVICE_NAME = "com.exemplo.mkbot/.BotService";

    private static final int REQUEST_FOLDER = 1001;

    private SharedPreferences prefs;
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);

        Spinner spinnerCharacter = findViewById(R.id.spinnerCharacter);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.characters_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCharacter.setAdapter(adapter);

        String currentChar = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        int charPos = adapter.getPosition(currentChar);
        if (charPos >= 0) spinnerCharacter.setSelection(charPos);

        spinnerCharacter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                prefs.edit().putString(KEY_CURRENT_CHAR, selected).apply();
                updateStats();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        Button btnA11y = findViewById(R.id.btnA11y);
        btnA11y.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        Button btnFolder = findViewById(R.id.btnFolder);
        btnFolder.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_FOLDER);
        });

        Button btnCalibrate = findViewById(R.id.btnCalibrate);
        btnCalibrate.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CalibrationActivity.class));
        });

        Button btnToggle = findViewById(R.id.btnToggle);
        btnToggle.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Ative a acessibilidade primeiro",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
            boolean running = prefs.getBoolean(KEY_RUNNING, false);
            prefs.edit().putBoolean(KEY_RUNNING, !running).apply();
            if (!running) {
                Toast.makeText(this, "Bot ativado. Use Volume+ para jogar.",
                        Toast.LENGTH_LONG).show();
            } else {
                prefs.edit().putBoolean(KEY_PLAY_MODE, false).apply();
                Toast.makeText(this, "Bot desativado", Toast.LENGTH_SHORT).show();
            }
            updateToggleButton();
        });

        Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Resetar Aprendizado")
                    .setMessage(R.string.reset_confirm)
                    .setPositiveButton("Sim", (d, w) -> {
                        prefs.edit()
                                .putInt(KEY_FIGHTS, 0)
                                .putInt(KEY_WINS, 0)
                                .putInt(KEY_LOSSES, 0)
                                .putInt(KEY_POINTS, 0)
                                .putString("qtable", null)
                                .putLong(KEY_TOTAL_PLAY, 0)
                                .putLong(KEY_PLAY_START, 0)
                                .putBoolean(KEY_PLAY_MODE, false)
                                .apply();
                        BackupManager.save(this, prefs);
                        updateStats();
                        Toast.makeText(this, "Aprendizado resetado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Nao", null)
                    .show();
        });

        startStatsUpdate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Ignora
                }
                prefs.edit().putString(KEY_FOLDER_URI, treeUri.toString()).apply();
                String msg = BackupManager.checkAndLoad(this, prefs);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                updateStats();
            }
        }
    }

    private void updateToggleButton() {
        Button btnToggle = findViewById(R.id.btnToggle);
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        btnToggle.setText(running ? R.string.deactivate_bot : R.string.activate_bot);
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

    private void startStatsUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStats();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateStats() {
        TextView txtStatus = findViewById(R.id.txtStatus);
        TextView txtHours = findViewById(R.id.txtHours);
        TextView txtCharTime = findViewById(R.id.txtCharTime);
        TextView txtTraining = findViewById(R.id.txtTraining);
        TextView txtWins = findViewById(R.id.txtWins);
        TextView txtLosses = findViewById(R.id.txtLosses);
        TextView txtWinRate = findViewById(R.id.txtWinRate);
        TextView txtPoints = findViewById(R.id.txtPoints);
        TextView txtEpsilon = findViewById(R.id.txtEpsilon);

        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        boolean playing = prefs.getBoolean(KEY_PLAY_MODE, false);
        if (txtStatus != null) {
            if (playing) {
                txtStatus.setText(R.string.bot_jogando);
                txtStatus.setTextColor(0xFF4CAF50);
            } else if (running) {
                txtStatus.setText(R.string.bot_parado);
                txtStatus.setTextColor(0xFF888888);
            } else {
                txtStatus.setText(R.string.bot_desligado);
                txtStatus.setTextColor(0xFF888888);
            }
        }

        long total = prefs.getLong(KEY_TOTAL_PLAY, 0);
        long current = 0;
        if (prefs.getBoolean(KEY_PLAY_MODE, false)) {
            long start = prefs.getLong(KEY_PLAY_START, 0);
            if (start > 0) current = System.currentTimeMillis() - start;
        }
        long ms = total + current;
        if (txtHours != null) txtHours.setText("Tempo de jogo: " + formatTime(ms));

        String currentChar = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        long charTime = prefs.getLong("charTime_" + currentChar, 0);
        long charCurrent = 0;
        long charStart = prefs.getLong("charStart_" + currentChar, 0);
        if (charStart > 0) charCurrent = System.currentTimeMillis() - charStart;
        if (txtCharTime != null) {
            txtCharTime.setText("Tempo " + currentChar + ": " + formatTime(charTime + charCurrent));
        }

        int fights = prefs.getInt(KEY_FIGHTS, 0);
        int wins = prefs.getInt(KEY_WINS, 0);
        int losses = prefs.getInt(KEY_LOSSES, 0);
        int points = prefs.getInt(KEY_POINTS, 0);
        float winRate = fights > 0 ? (wins * 100.0f / fights) : 0.0f;

        if (txtTraining != null) txtTraining.setText("Lutas treinadas: " + fights);
        if (txtWins != null) txtWins.setText("Vitorias: " + wins);
        if (txtLosses != null) txtLosses.setText("Derrotas: " + losses);
        if (txtWinRate != null) txtWinRate.setText(String.format("Taxa de vitoria: %.1f%%", winRate));
        if (txtPoints != null) txtPoints.setText("Pontos: " + points);

        long totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0) + current;
        float epsilon = computeEpsilon(totalPlay);
        if (txtEpsilon != null) txtEpsilon.setText(String.format("Epsilon: %.3f", epsilon));

        updateToggleButton();
    }

    private float computeEpsilon(long totalPlayMs) {
        float epsilonStart = 0.6f;
        float epsilonMin = 0.05f;
        long twoHoursMs = 2 * 60 * 60 * 1000L;
        float t = Math.min(1.0f, (float) totalPlayMs / twoHoursMs);
        return epsilonStart - (epsilonStart - epsilonMin) * t;
    }

    private String formatTime(long ms) {
        long sec = ms / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}
