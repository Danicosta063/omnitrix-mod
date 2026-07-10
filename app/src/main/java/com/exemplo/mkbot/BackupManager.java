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
    private static final String KEY_QTABLE = "qtable";
    private static final String KEY_FIGHTS = "fights";
    private static final String KEY_TOTAL_PLAY = "totalPlay";

    // Verifica se a pasta tem o arquivo de progresso
    // Se tem, carrega. Se nao tem, cria vazio.
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
            // Arquivo existe - carregar progresso
            String json = readFile(ctx, file);
            if (json != null) {
                parseAndApply(json, prefs);
                Log.i(TAG, "Progresso carregado do arquivo");
                return "Progresso carregado do arquivo";
            }
            return "Arquivo encontrado mas vazio";
        } else {
            // Arquivo nao existe - criar
            boolean created = createFile(ctx, tree);
            if (created) {
                Log.i(TAG, "Arquivo de progresso criado");
                return "Arquivo de progresso criado";
            }
            return "Erro ao criar arquivo";
        }
    }

    // Salva o progresso atual no arquivo
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
        Log.i(TAG, "Progresso salvo no arquivo");
    }

    // Le o JSON do arquivo
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

    // Escreve JSON no arquivo
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

    // Cria o arquivo vazio
    private static boolean createFile(Context ctx, DocumentFile tree) {
        try {
            DocumentFile file = tree.createFile("application/json", BACKUP_FILE);
            if (file != null) {
                // Escreve JSON inicial vazio
                String initial = "{\"qtable\":\"\",\"fights\":0,\"totalPlay\":0}";
                writeFile(ctx, file, initial);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar arquivo", e);
            return false;
        }
    }

    // Monta o JSON a partir dos SharedPreferences
    private static String buildJson(SharedPreferences prefs) {
        Gson gson = new Gson();
        BackupData data = new BackupData();
        data.qtable = prefs.getString(KEY_QTABLE, "");
        data.fights = prefs.getInt(KEY_FIGHTS, 0);
        data.totalPlay = prefs.getLong(KEY_TOTAL_PLAY, 0);
        return gson.toJson(data);
    }

    // Aplica o JSON carregado nos SharedPreferences
    private static void parseAndApply(String json, SharedPreferences prefs) {
        try {
            Gson gson = new Gson();
            BackupData data = gson.fromJson(json, BackupData.class);
            if (data == null) return;
            SharedPreferences.Editor ed = prefs.edit();
            if (data.qtable != null && !data.qtable.isEmpty()) {
                ed.putString(KEY_QTABLE, data.qtable);
            }
            ed.putInt(KEY_FIGHTS, data.fights);
            ed.putLong(KEY_TOTAL_PLAY, data.totalPlay);
            ed.apply();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear backup", e);
        }
    }

    // Pega o nome da pasta pra mostrar na UI
    public static String getFolderName(Context ctx, Uri treeUri) {
        DocumentFile tree = DocumentFile.fromTreeUri(ctx, treeUri);
        if (tree != null && tree.getName() != null) {
            return tree.getName();
        }
        return "pasta selecionada";
    }

    private static class BackupData {
        String qtable;
        int fights;
        long totalPlay;
    }
}
