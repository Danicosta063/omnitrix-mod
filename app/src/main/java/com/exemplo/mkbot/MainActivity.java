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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PLAY_MODE = "playMode";
    private static final String KEY_PLAY_START = "playStart";
    private static final String KEY_TOTAL_PLAY = "totalPlay";
    private static final String KEY_CURRENT_CHAR = "currentChar";
    private static final String KEY_FOLDER_URI = "folderUri";
    private static final String SERVICE_NAME = "com.exemplo.mkbot/.BotService";
    private static final int FOLDER_REQUEST_CODE = 1001;

    private SharedPreferences prefs;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private Spinner spinnerCharacter;
    private List<String> characters = Arrays.asList("Ashrah", "Scorpion");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        showMain();
    }

    private void showMain() {
        setContentView(R.layout.activity_main);

        Button btnFolder = findViewById(R.id.btnFolder);
        Button btnA11y = findViewById(R.id.btnA11y);
        Button btnActivate = findViewById(R.id.btnActivate);
        spinnerCharacter = findViewById(R.id.spinnerCharacter);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, characters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCharacter.setAdapter(adapter);

        String savedChar = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        int charIndex = characters.indexOf(savedChar);
        if (charIndex >= 0) spinnerCharacter.setSelection(charIndex);

        spinnerCharacter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = characters.get(position);
                prefs.edit().putString(KEY_CURRENT_CHAR, selected).apply();
                updateTimes();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnFolder.setOnClickListener(v -> selectFolder());

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
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean(KEY_RUNNING, !running).apply();
            if (!running) {
                // Ativando
                Toast.makeText(this, "Bot ativado", Toast.LENGTH_SHORT).show();
            } else {
                // Desativando - tambem para de jogar
                ed.putBoolean(KEY_PLAY_MODE, false).apply();
                Toast.makeText(this, "Bot desativado", Toast.LENGTH_SHORT).show();
            }
            updateActivateButton();
            updateStatus();
        });

        updateActivateButton();
        updateStatus();
        updateFolderStatus();
        updateTimes();
        startTimer();
    }

    private void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, FOLDER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FOLDER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri treeUri = data.getData();
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                prefs.edit().putString(KEY_FOLDER_URI, treeUri.toString()).apply();
                String status = BackupManager.checkAndLoad(this, prefs);
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                updateFolderStatus();
            }
        }
    }

    private void updateFolderStatus() {
        TextView txtFolder = findViewById(R.id.txtHint);
        // usa o hint fixo, nao mexe
    }

    private void updateActivateButton() {
        Button btnActivate = findViewById(R.id.btnActivate);
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        btnActivate.setText(running ? R.string.deactivate_bot : R.string.activate_bot);
    }

    private void updateStatus() {
        TextView txtStatus = findViewById(R.id.txtStatus);
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        boolean playing = prefs.getBoolean(KEY_PLAY_MODE, false);
        if (!running) {
            txtStatus.setText("Bot Desativado");
            txtStatus.setTextColor(0xFF888888);
        } else if (playing) {
            txtStatus.setText("Bot jogando");
            txtStatus.setTextColor(0xFF4CAF50);
        } else {
            txtStatus.setText("Bot parado");
            txtStatus.setTextColor(0xFFF44336);
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                updateTimes();
                updateActivateButton();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimes() {
        TextView txtCharTime = findViewById(R.id.txtCharTime);
        TextView txtScorpionTime = findViewById(R.id.txtScorpionTime);
        TextView txtTotalTime = findViewById(R.id.txtTotalTime);

        String currentChar = prefs.getString(KEY_CURRENT_CHAR, "Ashrah");
        boolean playing = prefs.getBoolean(KEY_PLAY_MODE, false);

        long ashrahTime = prefs.getLong("charTime_Ashrah", 0);
        long scorpionTime = prefs.getLong("charTime_Scorpion", 0);

        if (playing) {
            long charStart = prefs.getLong("charStart_" + currentChar, 0);
            if (charStart > 0) {
                long elapsed = System.currentTimeMillis() - charStart;
                if (currentChar.equals("Ashrah")) ashrahTime += elapsed;
                else if (currentChar.equals("Scorpion")) scorpionTime += elapsed;
            }
        }

        long totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0);
        if (playing) {
            long start = prefs.getLong(KEY_PLAY_START, 0);
            if (start > 0) totalPlay += System.currentTimeMillis() - start;
        }

        txtCharTime.setText("Ashrah: " + formatTime(ashrahTime));
        txtScorpionTime.setText("Scorpion: " + formatTime(scorpionTime));
        txtTotalTime.setText("Tempo Total: " + formatTime(totalPlay));
    }

    private String formatTime(long ms) {
        long sec = ms / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
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
        updateActivateButton();
        updateStatus();
        updateTimes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
