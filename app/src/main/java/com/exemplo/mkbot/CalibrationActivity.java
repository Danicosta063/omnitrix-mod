package com.exemplo.mkbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class CalibrationActivity extends AppCompatActivity {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_BUTTONS = "buttonsJson";

    // Ordem dos botoes para calibrar (12 botoes)
    private static final String[] BUTTON_KEYS = {
            "A1", "A2", "A3", "A4",
            "UP", "DOWN", "BACK", "FWD",
            "R1", "R2", "L1", "L2"
    };

    private static final int[] BUTTON_NAME_IDS = {
            R.string.btn_a1_name, R.string.btn_a2_name, R.string.btn_a3_name, R.string.btn_a4_name,
            R.string.btn_up_name, R.string.btn_down_name, R.string.btn_back_name, R.string.btn_fwd_name,
            R.string.btn_r1_name, R.string.btn_r2_name, R.string.btn_l1_name, R.string.btn_l2_name
    };

    private SharedPreferences prefs;
    private Gson gson;
    private Map<String, float[]> buttons;
    private int currentIndex = 0;

    private TextView txtProgress;
    private TextView txtBtnName;
    private View touchOverlay;
    private Button btnSkip;
    private Button btnRedo;
    private Button btnCancel;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        gson = new Gson();
        loadButtons();

        txtProgress = findViewById(R.id.txtCalibProgress);
        txtBtnName = findViewById(R.id.txtCalibBtnName);
        touchOverlay = findViewById(R.id.touchOverlay);
        btnSkip = findViewById(R.id.btnCalibSkip);
        btnRedo = findViewById(R.id.btnCalibRedo);
        btnCancel = findViewById(R.id.btnCalibCancel);
        btnFinish = findViewById(R.id.btnCalibFinish);

        touchOverlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();
                float w = v.getWidth();
                float h = v.getHeight();
                if (w > 0 && h > 0) {
                    float fx = x / w;
                    float fy = y / h;
                    String key = BUTTON_KEYS[currentIndex];
                    buttons.put(key, new float[]{fx, fy});
                    Toast.makeText(this, key + " salvo: " +
                            String.format("%.3f, %.3f", fx, fy), Toast.LENGTH_SHORT).show();
                    saveButtons();
                    nextButton();
                }
                return true;
            }
            return false;
        });

        btnSkip.setOnClickListener(v -> nextButton());
        btnRedo.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateUI();
            }
        });
        btnCancel.setOnClickListener(v -> finish());
        btnFinish.setOnClickListener(v -> {
            saveButtons();
            Toast.makeText(this, R.string.calibration_done, Toast.LENGTH_LONG).show();
            finish();
        });

        updateUI();
    }

    private void loadButtons() {
        String json = prefs.getString(KEY_BUTTONS, null);
        if (json != null) {
            try {
                buttons = gson.fromJson(json, HashMap.class);
                if (buttons == null) buttons = new HashMap<>();
            } catch (Exception e) {
                buttons = new HashMap<>();
            }
        } else {
            buttons = new HashMap<>();
        }
    }

    private void saveButtons() {
        prefs.edit().putString(KEY_BUTTONS, gson.toJson(buttons)).apply();
        BackupManager.save(this, prefs);
    }

    private void nextButton() {
        currentIndex++;
        if (currentIndex >= BUTTON_KEYS.length) {
            // Fim
            touchOverlay.setVisibility(View.GONE);
            txtProgress.setText("Concluido!");
            txtBtnName.setText("Calibracao finalizada");
            btnSkip.setVisibility(View.GONE);
            btnRedo.setVisibility(View.GONE);
            btnFinish.setVisibility(View.VISIBLE);
            saveButtons();
        } else {
            updateUI();
        }
    }

    private void updateUI() {
        if (currentIndex >= BUTTON_KEYS.length) return;
        txtProgress.setText(String.format("Botao %d de %d", currentIndex + 1, BUTTON_KEYS.length));
        txtBtnName.setText(getString(BUTTON_NAME_IDS[currentIndex]));
        touchOverlay.setVisibility(View.VISIBLE);
        btnFinish.setVisibility(View.GONE);
        btnSkip.setVisibility(View.VISIBLE);
        btnRedo.setVisibility(View.VISIBLE);
    }
}
