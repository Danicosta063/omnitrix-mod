package com.exemplo.mkbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Gerencia o backup do progresso do bot em uma pasta escolhida pelo usuario
 * usando o Storage Access Framework (sem precisar de root).
 *
 * Salva: Q-table, tempo total, tempo por personagem, personagem atual,
 * botaoes calibrados, estatisticas de luta, epsilon.
 */
public class BackupManager {

    private static final String TAG = "MKBot";
    private static final String BACKUP_FILE = "mkbot_progress.json";

    public static String checkAndLoad(Context ctx, SharedPreferences prefs) {
        String uriStr = prefs.getString("folderUri", null);
        if (uriStr == null) return "Nenhuma pasta selecionada";

        Uri treeUri = Uri.parse(uriStr);
        DocumentFile tree = DocumentFile.fromTreeUri(ctx, treeUri);
        if (tree == null || !tree.canWrite()) {
            return "Sem permissao na pasta";
        }

        DocumentFile file = tree.findFile(BACKUP_FILE);
        if (file != null && file.exists()) {
            String json = readFile(ctx, file);
            if (json != null) {
                parseAndApply(json, prefs);
                Log.i(TAG, "Progresso carregado do arquivo");
                return "Progresso carregado do arquivo";
            }
            return "Arquivo encontrado mas vazio";
        } else {
            boolean created = createFile(ctx, tree);
            if (created) {
                Log.i(TAG, "Arquivo de progresso criado");
                return "Arquivo de progresso criado";
            }
            return "Erro ao criar arquivo";
        }
    }

    public static void save(Context ctx, SharedPreferences prefs) {
        String uriStr = prefs.getString("folderUri", null);
        if (uriStr == null) return;

        Uri treeUri = Uri.parse(uriStr);
        DocumentFile tree = DocumentFile.fromTreeUri(ctx, treeUri);
        if (tree == null || !tree.canWrite()) return;

        DocumentFile file = tree.findFile(BACKUP_FILE);
        if (file == null || !file.exists()) {
            file = tree.createFile("application/json", BACKUP_FILE);
            if (file == null) return;
        }

        String json = buildJson(prefs);
        writeFile(ctx, file, json);
    }

    public static String getFolderName(Context ctx, Uri treeUri) {
        DocumentFile tree = DocumentFile.fromTreeUri(ctx, treeUri);
        if (tree != null && tree.getName() != null) {
            return tree.getName();
        }
        return "pasta selecionada";
    }

    // ============================================================
    //  Leitura / Escrita
    // ============================================================

    private static String readFile(Context ctx, DocumentFile file) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(file.getUri());
            if (is == null) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler arquivo", e);
            return null;
        }
    }

    private static void writeFile(Context ctx, DocumentFile file, String json) {
        try {
            OutputStream os = ctx.getContentResolver().openOutputStream(file.getUri());
            if (os == null) return;
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write(json);
            writer.flush();
            writer.close();
            os.close();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao escrever arquivo", e);
        }
    }

    private static boolean createFile(Context ctx, DocumentFile tree) {
        try {
            DocumentFile file = tree.createFile("application/json", BACKUP_FILE);
            if (file != null) {
                String initial = buildEmptyJson();
                writeFile(ctx, file, initial);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar arquivo", e);
            return false;
        }
    }

    // ============================================================
    //  Serializacao
    // ============================================================

    private static String buildEmptyJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("qtable", "");
        obj.addProperty("totalPlay", 0L);
        obj.addProperty("currentChar", "Ashrah");
        obj.addProperty("fights", 0);
        obj.addProperty("wins", 0);
        obj.addProperty("losses", 0);
        obj.addProperty("points", 0);
        obj.addProperty("buttonsJson", "");
        return new Gson().toJson(obj);
    }

    private static String buildJson(SharedPreferences prefs) {
        JsonObject obj = new JsonObject();
        obj.addProperty("qtable", prefs.getString("qtable", ""));
        obj.addProperty("totalPlay", prefs.getLong("totalPlay", 0));
        obj.addProperty("currentChar", prefs.getString("currentChar", "Ashrah"));
        obj.addProperty("fights", prefs.getInt("fights", 0));
        obj.addProperty("wins", prefs.getInt("wins", 0));
        obj.addProperty("losses", prefs.getInt("losses", 0));
        obj.addProperty("points", prefs.getInt("points", 0));
        obj.addProperty("buttonsJson", prefs.getString("buttonsJson", ""));

        // Tempos por personagem
        JsonObject charTimes = new JsonObject();
        String[] chars = {"Ashrah", "Scorpion", "Sub-Zero", "Generic"};
        for (String c : chars) {
            charTimes.addProperty(c, prefs.getLong("charTime_" + c, 0));
        }
        obj.add("charTimes", charTimes);

        return new Gson().toJson(obj);
    }

    private static void parseAndApply(String json, SharedPreferences prefs) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            SharedPreferences.Editor ed = prefs.edit();

            if (obj.has("qtable") && !obj.get("qtable").isJsonNull()) {
                String qt = obj.get("qtable").getAsString();
                if (qt != null && !qt.isEmpty()) {
                    ed.putString("qtable", qt);
                }
            }
            if (obj.has("totalPlay")) {
                ed.putLong("totalPlay", obj.get("totalPlay").getAsLong());
            }
            if (obj.has("currentChar") && !obj.get("currentChar").isJsonNull()) {
                ed.putString("currentChar", obj.get("currentChar").getAsString());
            }
            if (obj.has("fights")) {
                ed.putInt("fights", obj.get("fights").getAsInt());
            }
            if (obj.has("wins")) {
                ed.putInt("wins", obj.get("wins").getAsInt());
            }
            if (obj.has("losses")) {
                ed.putInt("losses", obj.get("losses").getAsInt());
            }
            if (obj.has("points")) {
                ed.putInt("points", obj.get("points").getAsInt());
            }
            if (obj.has("buttonsJson") && !obj.get("buttonsJson").isJsonNull()) {
                String bj = obj.get("buttonsJson").getAsString();
                if (bj != null && !bj.isEmpty()) {
                    ed.putString("buttonsJson", bj);
                }
            }
            if (obj.has("charTimes") && obj.get("charTimes").isJsonObject()) {
                JsonObject charTimes = obj.getAsJsonObject("charTimes");
                for (String c : charTimes.keySet()) {
                    ed.putLong("charTime_" + c, charTimes.get(c).getAsLong());
                }
            }

            ed.apply();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear backup", e);
        }
    }
}
