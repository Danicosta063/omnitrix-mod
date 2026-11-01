package com.codeforge.ai

data class AIModel(
    val id: String,
    val name: String,
    val provider: String,
    val description: String,
    val isFree: Boolean = false,
    val family: String = "",
    val releaseDate: String? = null,
    val isAvailable: Boolean = true,
    val isHistorical: Boolean = false,
    val contextLength: Int? = null,
    val inputPricePerMillion: Double? = null,
    val outputPricePerMillion: Double? = null
)

data class AIProvider(
    val id: String,
    val name: String,
    val family: String,
    val firstReleaseDate: String? = null,
    val description: String = ""
)

object AIModels {

    val providers = listOf(
        AIProvider(
            "openai",
            "OpenAI",
            "GPT",
            "2018-06-11",
            "Família GPT e modelos de raciocínio."
        ),
        AIProvider(
            "google",
            "Google",
            "Gemini",
            "2023-12-06",
            "Família Gemini."
        ),
        AIProvider(
            "meta-llama",
            "Meta",
            "Llama",
            "2023-02-24",
            "Família Llama de modelos abertos."
        ),
        AIProvider(
            "anthropic",
            "Anthropic",
            "Claude",
            "2023-03-14",
            "Família Claude."
        ),
        AIProvider(
            "mistralai",
            "Mistral AI",
            "Mistral",
            "2023-09-27",
            "Família Mistral e Mixtral."
        ),
        AIProvider(
            "deepseek",
            "DeepSeek",
            "DeepSeek",
            "2023-11-29",
            "Modelos gerais e de raciocínio."
        ),
        AIProvider(
            "x-ai",
            "xAI",
            "Grok",
            "2023-11-04",
            "Família Grok."
        ),
        AIProvider(
            "qwen",
            "Qwen",
            "Qwen",
            "2023-08-03",
            "Família Qwen."
        )
    )

    val sortedProviders: List<AIProvider>
        get() = providers.sortedWith(
            compareBy<AIProvider> {
                it.firstReleaseDate ?: "9999-12-31"
            }.thenBy { it.name }
        )    val defaultModels = listOf(
        AIModel(
            id = "openai/gpt-4o",
            name = "GPT-4o",
            provider = "OpenAI",
            description = "Programação e análise de código.",
            family = "GPT",
            releaseDate = "2024-05-13"
        ),
        AIModel(
            id = "openai/gpt-4o-mini",
            name = "GPT-4o Mini",
            provider = "OpenAI",
            description = "Modelo econômico para tarefas de código.",
            family = "GPT",
            releaseDate = "2024-07-18"
        ),
        AIModel(
            id = "anthropic/claude-sonnet-4",
            name = "Claude Sonnet 4",
            provider = "Anthropic",
            description = "Programação e projetos complexos.",
            family = "Claude",
            releaseDate = "2025-05-22"
        ),
        AIModel(
            id = "google/gemini-2.5-pro",
            name = "Gemini 2.5 Pro",
            provider = "Google",
            description = "Análise de projetos extensos.",
            family = "Gemini",
            releaseDate = "2025-03-25"
        ),
        AIModel(
            id = "deepseek/deepseek-chat",
            name = "DeepSeek Chat",
            provider = "DeepSeek",
            description = "Geração e correção de código.",
            family = "DeepSeek"
        ),
        AIModel(
            id = "qwen/qwen3-coder",
            name = "Qwen3 Coder",
            provider = "Qwen",
            description = "Especializado em programação.",
            family = "Qwen",
            releaseDate = "2025-07-22"
        )
    )

    val historicalModels = listOf(
        AIModel(
            id = "historical/openai/gpt-1",
            name = "GPT-1",
            provider = "OpenAI",
            description = "Primeira geração da família GPT.",
            family = "GPT",
            releaseDate = "2018-06-11",
            isAvailable = false,
            isHistorical = true
        ),
        AIModel(
            id = "historical/openai/gpt-2",
            name = "GPT-2",
            provider = "OpenAI",
            description = "Segunda geração da família GPT.",
            family = "GPT",
            releaseDate = "2019-02-14",
            isAvailable = false,
            isHistorical = true
        ),
        AIModel(
            id = "historical/openai/gpt-3",
            name = "GPT-3",
            provider = "OpenAI",
            description = "Terceira geração da família GPT.",
            family = "GPT",
            releaseDate = "2020-05-28",
            isAvailable = false,
            isHistorical = true
        ),
        AIModel(
            id = "historical/anthropic/claude-1",
            name = "Claude 1",
            provider = "Anthropic",
            description = "Primeira geração pública do Claude.",
            family = "Claude",
            releaseDate = "2023-03-14",
            isAvailable = false,
            isHistorical = true
        )
    )    val allModels: List<AIModel>
        get() = (historicalModels + defaultModels)
            .distinctBy { it.id }
            .sortedWith(
                compareBy<AIModel> {
                    it.releaseDate ?: "9999-12-31"
                }.thenBy { it.name }
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
        return allModels.find { it.id == id }
    }

    fun getByProvider(provider: String): List<AIModel> {
        return allModels.filter {
            it.provider.equals(
                provider,
                ignoreCase = true
            )
        }
    }

    fun getByFamily(family: String): List<AIModel> {
        return allModels.filter {
            it.family.equals(
                family,
                ignoreCase = true
            )
        }
    }

    fun getProviders(): List<String> {
        return defaultModels
            .map { it.provider }
            .distinct()
    }

    fun getAvailableModels(): List<AIModel> {
        return allModels.filter { it.isAvailable }
    }

    fun getHistoricalModels(): List<AIModel> {
        return allModels.filter { it.isHistorical }
    }

    fun createCustomModel(
        id: String,
        name: String = id
    ): AIModel {
        require(id.isNotBlank()) {
            "O identificador do modelo não pode estar vazio."
        }

        val cleanId = id.trim()

        val providerId = cleanId
            .substringBefore("/", "")
            .lowercase()

        val knownProvider = providers.find {
            it.id.equals(
                providerId,
                ignoreCase = true
            )
        }

        return AIModel(
            id = cleanId,
            name = name.trim().ifBlank { cleanId },
            provider = knownProvider?.name ?: "Personalizado",
            description = "Modelo configurado manualmente.",
            family = knownProvider?.family ?: "Outros"
        )
    }
}
