
package com.codeforge.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

data class SavedChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedChat(
    val id: String,
    val title: String,
    val modelId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<SavedChatMessage>
)

class ChatHistoryManager(context: Context) {

    private val historyDirectory = File(
        context.applicationContext.filesDir,
        "codeforge_chats"
    )

    companion object {
        private const val MAX_MESSAGES = 500
        private const val MAX_MESSAGE_LENGTH = 100_000
        private const val MAX_TITLE_LENGTH = 100
        private const val MAX_MODEL_LENGTH = 200
    }

    init {
        if (
            !historyDirectory.isDirectory &&
            !historyDirectory.mkdirs()
        ) {
            throw IOException(
                "Não foi possível criar a pasta de conversas."
            )
        }
    }

    @Synchronized
    fun createChat(
        modelId: String,
        title: String = "Nova conversa"
    ): SavedChat {
        require(modelId.isNotBlank()) {
            "Selecione um modelo de IA."
        }

        require(modelId.length <= MAX_MODEL_LENGTH) {
            "Identificador do modelo muito longo."
        }

        val now = System.currentTimeMillis()

        val chat = SavedChat(
            id = UUID.randomUUID().toString(),
            title = normalizeTitle(title),
            modelId = modelId,
            createdAt = now,
            updatedAt = now,
            messages = emptyList()
        )

        saveChat(chat)
        return chat
    }

    @Synchronized
    fun listChats(): List<SavedChat> {
        return historyDirectory
            .listFiles()
            .orEmpty()
            .filter {
                it.isFile &&
                    it.extension == "json"
            }
            .mapNotNull { file ->
                try {
                    readChatFile(file)
                } catch (_: Exception) {
                    null
                }
            }
            .sortedByDescending {
                it.updatedAt
            }
    }

    @Synchronized
    fun getChat(chatId: String): SavedChat? {
        val file = getChatFile(chatId)

        if (!file.isFile) {
            return null
        }

        return readChatFile(file)
    }

    @Synchronized
    fun saveChat(chat: SavedChat) {
        require(chat.messages.size <= MAX_MESSAGES) {
            "A conversa atingiu o limite de mensagens."
        }

        require(chat.modelId.isNotBlank()) {
            "O modelo não pode estar vazio."
        }

        require(chat.modelId.length <= MAX_MODEL_LENGTH) {
            "Identificador do modelo muito longo."
        }

        chat.messages.forEach { message ->
            require(
                message.role == "user" ||
                    message.role == "assistant" ||
                    message.role == "system"
            ) {
                "Tipo de mensagem inválido."
            }

            require(
                message.content.length <= MAX_MESSAGE_LENGTH
            ) {
                "Uma mensagem excede o limite permitido."
            }
        }

        val json = JSONObject().apply {
            put("id", chat.id)
            put("title", normalizeTitle(chat.title))
            put("modelId", chat.modelId)
            put("createdAt", chat.createdAt)
            put("updatedAt", chat.updatedAt)

            val messagesArray = JSONArray()

            chat.messages.forEach { message ->
                messagesArray.put(
                    JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                        put("timestamp", message.timestamp)
                    }
                )
            }

            put("messages", messagesArray)
        }

        val targetFile = getChatFile(chat.id)

        val temporaryFile = File(
            historyDirectory,
            "${chat.id}.tmp"
        )

        try {
            temporaryFile.writeText(
                json.toString(),
                Charsets.UTF_8
            )

            if (!temporaryFile.renameTo(targetFile)) {
                if (
                    targetFile.exists() &&
                    !targetFile.delete()
                ) {
                    throw IOException(
                        "Não foi possível atualizar a conversa."
                    )
                }

                if (!temporaryFile.renameTo(targetFile)) {
                    throw IOException(
                        "Não foi possível salvar a conversa."
                    )
                }
            }
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    @Synchronized
    fun addMessage(
        chatId: String,
        role: String,
        content: String
    ): SavedChat {
        require(
            role == "user" ||
                role == "assistant"
        ) {
            "Tipo de mensagem inválido."
        }

        require(content.length <= MAX_MESSAGE_LENGTH) {
            "Mensagem muito longa."
        }

        val current = getChat(chatId)
            ?: throw IOException(
                "Conversa não encontrada."
            )

        require(current.messages.size < MAX_MESSAGES) {
            "A conversa atingiu o limite de mensagens."
        }

        val updatedTitle = if (
            current.messages.isEmpty() &&
            role == "user" &&
            current.title == "Nova conversa"
        ) {
            normalizeTitle(content)
        } else {
            current.title
        }

        val updated = current.copy(
            title = updatedTitle,
            updatedAt = System.currentTimeMillis(),
            messages = current.messages +
                SavedChatMessage(
                    role = role,
                    content = content
                )
        )

        saveChat(updated)
        return updated
    }

    @Synchronized
    fun renameChat(
        chatId: String,
        newTitle: String
    ): SavedChat {
        val current = getChat(chatId)
            ?: throw IOException(
                "Conversa não encontrada."
            )

        val updated = current.copy(
            title = normalizeTitle(newTitle),
            updatedAt = System.currentTimeMillis()
        )

        saveChat(updated)
        return updated
    }

    @Synchronized
    fun changeModel(
        chatId: String,
        modelId: String
    ): SavedChat {
        val current = getChat(chatId)
            ?: throw IOException(
                "Conversa não encontrada."
            )

        val updated = current.copy(
            modelId = modelId,
            updatedAt = System.currentTimeMillis()
        )

        saveChat(updated)
        return updated
    }

    @Synchronized
    fun deleteChat(chatId: String): Boolean {
        val file = getChatFile(chatId)

        return !file.exists() || file.delete()
    }

    @Synchronized
    fun clearHistory(): Boolean {
        return historyDirectory
            .listFiles()
            .orEmpty()
            .filter {
                it.isFile &&
                    it.extension == "json"
            }
            .all {
                it.delete()
            }
    }

    private fun readChatFile(file: File): SavedChat {
        val json = JSONObject(
            file.readText(Charsets.UTF_8)
        )

        val messagesJson = json.getJSONArray(
            "messages"
        )

        val messages = buildList {
            for (index in 0 until messagesJson.length()) {
                val item = messagesJson.getJSONObject(index)

                add(
                    SavedChatMessage(
                        role = item.getString("role"),
                        content = item.getString("content"),
                        timestamp = item.optLong(
                            "timestamp",
                            0L
                        )
                    )
                )
            }
        }

        return SavedChat(
            id = json.getString("id"),
            title = json.getString("title"),
            modelId = json.getString("modelId"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            messages = messages
        )
    }

    private fun getChatFile(chatId: String): File {
        require(
            Regex(
                "[0-9a-fA-F-]{36}"
            ).matches(chatId)
        ) {
            "Identificador de conversa inválido."
        }

        val file = File(
            historyDirectory,
            "$chatId.json"
        ).canonicalFile

        require(
            file.parentFile == historyDirectory.canonicalFile
        ) {
            "Caminho de conversa inválido."
        }

        return file
    }

    private fun normalizeTitle(title: String): String {
        return title
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
            .take(MAX_TITLE_LENGTH)
            .ifBlank {
                "Nova conversa"
            }
    }
}
