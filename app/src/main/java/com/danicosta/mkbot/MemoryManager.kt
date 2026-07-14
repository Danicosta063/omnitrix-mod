package com.danicosta.mkbot

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject

object MemoryManager {

    private const val FILE_NAME = "mkbot_memory.json"
    private const val SAVE_INTERVAL_MS = 20_000L

    private var appContext: Context? = null
    private var totalMillis: Long = 0L
    private val characterMillis: MutableMap<String, Long> = mutableMapOf()
    private var lastSaveAt = 0L
    private var dirty = false
    private var loadedOnce = false

    private val ioThread = HandlerThread("MKBotMemoryIO").apply { start() }
    private val ioHandler = Handler(ioThread.looper)

    fun init(context: Context) {
        appContext = context.applicationContext
        if (!loadedOnce) {
            loadedOnce = true
            load()
        }
    }

    fun addElapsedMillis(character: String, millis: Long) {
        totalMillis += millis
        characterMillis[character] = (characterMillis[character] ?: 0L) + millis
        dirty = true
        if (System.currentTimeMillis() - lastSaveAt > SAVE_INTERVAL_MS) save()
    }

    fun totalSeconds(): Long = totalMillis / 1000
    fun characterSeconds(character: String): Long = (characterMillis[character] ?: 0L) / 1000

    fun save() {
        val ctx = appContext ?: return
        val uri = folderUri() ?: return
        if (!dirty) return
        lastSaveAt = System.currentTimeMillis()
        dirty = false
        val json = buildJson(MainActivity.CURRENT_CHARACTER)
        ioHandler.post {
            try {
                val root = DocumentFile.fromTreeUri(ctx, uri) ?: return@post
                val file = root.findFile(FILE_NAME) ?: root.createFile("application/json", FILE_NAME)
                file?.let {
                    ctx.contentResolver.openOutputStream(it.uri, "wt")?.use { out ->
                        out.write(json.toString().toByteArray())
                    }
                }
            } catch (e: Exception) {
                dirty = true
            }
        }
    }

    fun load() {
        val ctx = appContext ?: return
        val uri = folderUri() ?: return
        ioHandler.post {
            try {
                val root = DocumentFile.fromTreeUri(ctx, uri) ?: return@post
                val file = root.findFile(FILE_NAME) ?: return@post
                val text = ctx.contentResolver.openInputStream(file.uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@post
                val json = JSONObject(text)
                totalMillis = json.optLong("totalSeconds", 0L) * 1000

                val character = MainActivity.CURRENT_CHARACTER
                val charObj = json.optJSONObject("characters")?.optJSONObject(character) ?: return@post
                characterMillis[character] = charObj.optLong("seconds", 0L) * 1000

                val movesObj = charObj.optJSONObject("moveScores") ?: return@post
                val keys = movesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val scoreObj = movesObj.getJSONObject(key)
                    BotBrain.moveScores[key] = ActionScore().apply {
                        attempts = scoreObj.optInt("attempts", 0)
                        successes = scoreObj.optInt("successes", 0)
                    }
                }
            } catch (e: Exception) {
                // Ainda não existe arquivo, ou deu erro lendo — começa do zero sem travar o app.
            }
        }
    }

    private fun folderUri(): Uri? {
        val ctx = appContext ?: return null
        val uriString = ctx.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(MainActivity.KEY_FOLDER_URI, null)
        return uriString?.let { Uri.parse(it) }
    }

    private fun buildJson(character: String): JSONObject {
        val root = JSONObject()
        root.put("totalSeconds", totalSeconds())
        val charactersObj = JSONObject()
        val charObj = JSONObject()
        charObj.put("seconds", characterSeconds(character))
        val movesObj = JSONObject()
        BotBrain.moveScores.forEach { (key, score) ->
            movesObj.put(key, JSONObject().apply {
                put("attempts", score.attempts)
                put("successes", score.successes)
            })
        }
        charObj.put("moveScores", movesObj)
        charactersObj.put(character, charObj)
        root.put("characters", charactersObj)
        return root
    }
}
