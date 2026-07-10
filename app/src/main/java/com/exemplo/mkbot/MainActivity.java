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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_PLAY_MODE = "playMode";
    private static final String KEY_PLAY_START = "playStart";
    private static final String KEY_TOTAL_PLAY = "totalPlay";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_FOLDER_URI = "folderUri";
    private static final String SERVICE_NAME = "com.exemplo.mkbot/.BotService";

    private static final int FOLDER_REQUEST_CODE = 1001;

    private SharedPreferences prefs;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

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
            prefs.edit().putBoolean(KEY_RUNNING, !running).apply();
            updateActivateButton();
        });

        updateActivateButton();
        updatePlayUI();
        updateHours();
        updateFolderStatus();
        startTimer();
    }

    // ============ SELECAO DE PASTA ============

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
                // Persiste a permissao pra funcionar depois de reiniciar
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                prefs.edit().putString(KEY_FOLDER_URI, treeUri.toString()).apply();

                // Verifica se ja tem arquivo de progresso la
                String status = BackupManager.checkAndLoad(this, prefs);
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();

                updateFolderStatus();
            }
        }
    }

    private void updateFolderStatus() {
        TextView txtFolderStatus = findViewById(R.id.txtFolderStatus);
        String uriStr = prefs.getString(KEY_FOLDER_URI, null);
        if (uriStr == null) {
            txtFolderStatus.setText(R.string.folder_not_selected);
        } else {
            Uri uri = Uri.parse(uriStr);
            String name = BackupManager.getFolderName(this, uri);
            txtFolderStatus.setText(getString(R.string.folder_selected, name));
        }
    }

    // ============ UI ============

    private void updateActivateButton() {
        Button btnActivate = findViewById(R.id.btnActivate);
        boolean running = prefs.getBoolean(KEY_RUNNING, false);
        btnActivate.setText(running ? R.string.deactivate_bot : R.string.activate_bot);
    }

    private void updatePlayUI() {
        TextView txtStatus = findViewById(R.id.txtStatus);
        boolean playing = prefs.getBoolean(KEY_PLAY_MODE, false);
        if (playing) {
            txtStatus.setText("Bot jogando");
            txtStatus.setTextColor(0xFF4CAF50);
        } else {
            txtStatus.setText("Bot parado");
            txtStatus.setTextColor(0xFF888888);
        }
    }

    // ============ CONTADOR DE HORAS ============

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updatePlayUI();
                updateHours();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateHours() {
        TextView txtHours = findViewById(R.id.txtHours);
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
        updateActivateButton();
        updatePlayUI();
        updateHours();
        updateFolderStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
