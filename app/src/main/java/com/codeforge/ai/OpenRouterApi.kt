
package com.codeforge.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApiMessage(
    val role: String,
    val content: String
)

class OpenRouterApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiUrl =
        "https://openrouter.ai/api/v1/chat/completions"

    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ApiMessage>
    ): Result<String> = withContext(Dispatchers.IO) {

        try {
            require(apiKey.isNotBlank()) {
                "Configure sua chave do OpenRouter."
            }

            require(model.isNotBlank()) {
                "Selecione um modelo de IA."
            }

            require(messages.isNotEmpty()) {
                "Digite uma mensagem."
            }

            val jsonMessages = JSONArray()

            messages.forEach { message ->
                val item = JSONObject()
                item.put("role", message.role)
                item.put("content", message.content)
                jsonMessages.put(item)
            }

            val requestJson = JSONObject().apply {
                put("model", model)
                put("messages", jsonMessages)
                put("stream", false)
            }

            val requestBody = requestJson
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )

            val request = Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("X-Title", "CodeForge AI")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->

                val responseText =
                    response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(responseText)
                            .optJSONObject("error")
                            ?.optString("message")
                            ?.takeIf { it.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    }

                    throw IOException(
                        errorMessage
                            ?: "Erro HTTP ${response.code} no OpenRouter."
                    )
                }

                val responseJson = JSONObject(responseText)

                val choices = responseJson.optJSONArray("choices")
                    ?: throw IOException(
                        "Resposta sem escolhas do modelo."
                    )

                if (choices.length() == 0) {
                    throw IOException(
                        "A IA não retornou nenhuma resposta."
                    )
                }

                val message = choices
                    .getJSONObject(0)
                    .getJSONObject("message")

                val content = message.optString("content")

                if (content.isBlank()) {
                    throw IOException(
                        "A IA retornou uma resposta vazia."
                    )
                }

                Result.success(content)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(
        apiKey: String,
        model: String
    ): Result<String> {
        return sendMessage(
            apiKey = apiKey,
            model = model,
            messages = listOf(
                ApiMessage(
                    role = "user",
                    content = "Responda apenas: Conexão funcionando!"
                )
            )
        )
    }
}
