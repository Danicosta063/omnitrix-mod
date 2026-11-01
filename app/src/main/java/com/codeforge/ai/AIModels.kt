
package com.codeforge.ai

data class AIModel(
    val id: String,
    val name: String,
    val provider: String,
    val description: String,
    val isFree: Boolean = false
)

object AIModels {

    val defaultModels = listOf(
        AIModel(
            id = "openai/gpt-4o",
            name = "GPT-4o",
            provider = "OpenAI",
            description = "Programação, explicações e análise de código."
        ),
        AIModel(
            id = "openai/gpt-4o-mini",
            name = "GPT-4o Mini",
            provider = "OpenAI",
            description = "Modelo econômico para tarefas de código."
        ),
        AIModel(
            id = "anthropic/claude-sonnet-4",
            name = "Claude Sonnet 4",
            provider = "Anthropic",
            description = "Programação e análise de projetos complexos."
        ),
        AIModel(
            id = "google/gemini-2.5-pro",
            name = "Gemini 2.5 Pro",
            provider = "Google",
            description = "Análise de projetos e arquivos extensos."
        ),
        AIModel(
            id = "deepseek/deepseek-chat",
            name = "DeepSeek Chat",
            provider = "DeepSeek",
            description = "Geração e correção de código."
        ),
        AIModel(
            id = "qwen/qwen3-coder",
            name = "Qwen3 Coder",
            provider = "Qwen",
            description = "Modelo especializado em programação."
        )
    )

    val systemPrompt = """
        Você é o CodeForge AI, um assistente especializado
        em desenvolvimento de software.

        Regras:
        1. Escreva códigos completos e funcionais.
        2. Informe os caminhos exatos dos arquivos.
        3. Ao corrigir um arquivo, forneça o conteúdo
           completo atualizado.
        4. Preserve a estrutura existente do projeto.
        5. Explique erros de compilação claramente.
        6. Não invente resultados de testes.
        7. Priorize código seguro e organizado.
        8. Responda em português do Brasil.
        9. Ajude com Kotlin, Java, Python, JavaScript,
           Minecraft Fabric, Android e outras tecnologias.
        10. Nunca solicite senhas ou chaves secretas.
    """.trimIndent()

    fun findById(id: String): AIModel? {
        return defaultModels.find { it.id == id }
    }

    fun getByProvider(provider: String): List<AIModel> {
        return defaultModels.filter {
            it.provider.equals(provider, ignoreCase = true)
        }
    }

    fun getProviders(): List<String> {
        return defaultModels.map { it.provider }.distinct()
    }

    fun createCustomModel(
        id: String,
        name: String = id
    ): AIModel {
        require(id.isNotBlank()) {
            "O identificador do modelo não pode estar vazio."
        }

        return AIModel(
            id = id.trim(),
            name = name.trim().ifBlank { id.trim() },
            provider = "Personalizado",
            description = "Modelo configurado manualmente."
        )
    }
}
