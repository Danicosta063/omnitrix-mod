package com.exemplo.mkbot;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia as coordenadas calibradas dos botoes do emulador.
 * As coordenadas sao armazenadas como fracoes (0.0 a 1.0) relativas
 * ao tamanho da tela, para funcionar em qualquer celular.
 */
public class ButtonMap {

    private static final String PREFS = "mkbot_prefs";
    private static final String KEY_BUTTONS = "buttonsJson";

    // Chaves dos 12 botoes
    public static final String A1 = "A1";       // Square
    public static final String A2 = "A2";       // Triangle
    public static final String A3 = "A3";       // X / Cross
    public static final String A4 = "A4";       // Circle
    public static final String UP = "UP";       // D-pad Cima
    public static final String DOWN = "DOWN";   // D-pad Baixo
    public static final String BACK = "BACK";   // D-pad Esquerda
    public static final String FWD = "FWD";     // D-pad Direita
    public static final String R1 = "R1";       // Special
    public static final String R2 = "R2";       // Block / Throw
    public static final String L1 = "L1";       // Style Change
    public static final String L2 = "L2";       // Grab Weapon

    private final Map<String, float[]> buttons;

    public ButtonMap(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_BUTTONS, null);
        if (json != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<HashMap<String, float[]>>() {}.getType();
                Map<String, float[]> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    buttons = loaded;
                } else {
                    buttons = new HashMap<>();
                }
            } catch (Exception e) {
                buttons = new HashMap<>();
            }
        } else {
            buttons = new HashMap<>();
        }
    }

    /**
     * Retorna as coordenadas absolutas (em pixels) do botao, dado o tamanho da tela.
     * @param key Chave do botao (ex: A1, A2, UP, etc.)
     * @param screenW Largura da tela em pixels
     * @param screenH Altura da tela em pixels
     * @return float[]{x, y} em pixels, ou null se o botao nao foi calibrado
     */
    public float[] get(String key, int screenW, int screenH) {
        float[] frac = buttons.get(key);
        if (frac == null || frac.length < 2) return null;
        return new float[]{frac[0] * screenW, frac[1] * screenH};
    }

    /**
     * Retorna as fracoes (0.0 a 1.0) do botao.
     */
    public float[] getFraction(String key) {
        float[] frac = buttons.get(key);
        if (frac == null || frac.length < 2) return null;
        return new float[]{frac[0], frac[1]};
    }

    public boolean isCalibrated(String key) {
        float[] frac = buttons.get(key);
        return frac != null && frac.length >= 2;
    }

    /**
     * Verifica se todos os botoes essenciais estao calibrados.
     * R1 e L2 sao opcionais (R1 nem sempre e usado, L2 so para pegar armas).
     */
    public boolean isFullyCalibrated() {
        String[] essential = {A1, A2, A3, A4, UP, DOWN, BACK, FWD, R2, L1};
        for (String key : essential) {
            if (!isCalibrated(key)) return false;
        }
        return true;
    }

    /**
     * Retorna o numero de botoes calibrados.
     */
    public int getCalibratedCount() {
        return buttons.size();
    }

    /**
     * Lista de todos os botoes que devem ser calibrados.
     */
    public static String[] getAllButtonKeys() {
        return new String[]{A1, A2, A3, A4, UP, DOWN, BACK, FWD, R1, R2, L1, L2};
    }
}
