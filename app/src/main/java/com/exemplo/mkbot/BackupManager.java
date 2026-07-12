package com.exemplo.mkbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

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

    private static String buildEmptyJson() {
        Gson gson = new Gson();
        BackupData data = new BackupData();
        data.qtable = "";
        data.totalPlay = 0;
        data.currentChar = "Ashrah";
        data.charTime_Ashrah = 0;
        data.charTime_Scorpion = 0;
        return gson.toJson(data);
    }

    private static String buildJson(SharedPreferences prefs) {
        Gson gson = new Gson();
        BackupData data = new BackupData();
        data.qtable = prefs.getString("qtable", "");
        data.totalPlay = prefs.getLong("totalPlay", 0);
        data.currentChar = prefs.getString("currentChar", "Ashrah");
        data.charTime_Ashrah = prefs.getLong("charTime_Ashrah", 0);
        data.charTime_Scorpion = prefs.getLong("charTime_Scorpion", 0);
        return gson.toJson(data);
    }

    private static void parseAndApply(String json, SharedPreferences prefs) {
        try {
            Gson gson = new Gson();
            BackupData data = gson.fromJson(json, BackupData.class);
            if (data == null) return;
            SharedPreferences.Editor ed = prefs.edit();
            if (data.qtable != null && !data.qtable.isEmpty()) {
                ed.putString("qtable", data.qtable);
            }
            ed.putLong("totalPlay", data.totalPlay);
            if (data.currentChar != null) {
                ed.putString("currentChar", data.currentChar);
            }
            ed.putLong("charTime_Ashrah", data.charTime_Ashrah);
            ed.putLong("charTime_Scorpion", data.charTime_Scorpion);
            ed.apply();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear backup", e);
        }
    }

    public static String getFolderName(Context ctx, Uri treeUri) {
        DocumentFile tree = DocumentFile.fromTreeUri(ctx, treeUri);
        if (tree != null && tree.getName() != null) {
            return tree.getName();
        }
        return "pasta selecionada";
    }

    private static class BackupData {
        String qtable;
        long totalPlay;
        String currentChar;
        long charTime_Ashrah;
        long charTime_Scorpion;
    }
}
