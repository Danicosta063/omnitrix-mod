
package com.codeforge.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AppBackground = Color(0xFF101218)
private val AppSurface = Color(0xFF1C2029)
private val AppAccent = Color(0xFF65D6A5)

private data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

private sealed interface AppPage {
    data object Chat : AppPage
    data object Settings : AppPage
    data object Projects : AppPage
    data object History : AppPage

    data class Explorer(
        val project: CodeProject
    ) : AppPage

    data class Editor(
        val project: CodeProject,
        val filePath: String
    ) : AppPage

    data class AIEditor(
        val project: CodeProject,
        val filePath: String
    ) : AppPage
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKeyManager = ApiKeyManager(applicationContext)
        val projectManager = ProjectManager(applicationContext)
        val historyManager = ChatHistoryManager(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AppAccent,
                    background = AppBackground,
                    surface = AppSurface
                )
            ) {
                CodeForgeApp(
                    apiKeyManager,
                    projectManager,
                    historyManager
                )
            }
        }
    }
}

@Composable
private fun CodeForgeApp(
    apiKeyManager: ApiKeyManager,
    projectManager: ProjectManager,
    historyManager: ChatHistoryManager
) {
    var page by remember {
        mutableStateOf<AppPage>(AppPage.Chat)
    }

    var currentChatId by remember {
        mutableStateOf<String?>(null)
    }

    var selectedModel by remember {
        mutableStateOf(AIModels.defaultModels.first())
    }

    val messages = remember {
        mutableStateListOf<ChatMessage>()
    }

    var nextId by remember {
        mutableLongStateOf(0L)
    }

    var input by remember {
        mutableStateOf("")
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun openChat(chat: SavedChat) {
        currentChatId = chat.id
        messages.clear()

        chat.messages.forEach { message ->
            if (
                message.role == "user" ||
                message.role == "assistant"
            ) {
                nextId++

                messages.add(
                    ChatMessage(
                        id = nextId,
                        text = message.content,
                        isUser = message.role == "user"
                    )
                )
            }
        }

        selectedModel = AIModels.defaultModels
            .firstOrNull { it.id == chat.modelId }
            ?: AIModel(
                id = chat.modelId,
                name = chat.modelId,
                provider = "Personalizado",
                description = ""
            )

        input = ""
        page = AppPage.Chat
    }

    fun newChat() {
        currentChatId = null
        messages.clear()
        input = ""
        page = AppPage.Chat
    }

    when (val current = page) {
        AppPage.Chat -> {
            CodeForgeChatScreen(
                apiKeyManager = apiKeyManager,
                historyManager = historyManager,
                messages = messages,
                nextId = {
                    nextId++
                    nextId
                },
                chatId = currentChatId,
                onChatIdChange = {
                    currentChatId = it
                },
                selectedModel = selectedModel,
                onModelChange = {
                    selectedModel = it
                },
                input = input,
                onInputChange = {
                    input = it
                },
                busy = busy,
                onBusyChange = {
                    busy = it
                },
                onNewChat = { newChat() },
                onHistory = {
                    page = AppPage.History
                },
                onProjects = {
                    page = AppPage.Projects
                },
                onSettings = {
                    page = AppPage.Settings
                },
                onError = {
                    errorMessage = it
                }
            )
        }

        AppPage.Settings -> {
            BackHandler {
                page = AppPage.Chat
            }

            SettingsScreen(
                apiKeyManager = apiKeyManager,
                onBack = {
                    page = AppPage.Chat
                }
            )
        }

        AppPage.Projects -> {
            BackHandler {
                page = AppPage.Chat
            }

            ProjectsScreen(
                projectManager = projectManager,
                onBack = {
                    page = AppPage.Chat
                },
                onOpenProject = {
                    page = AppPage.Explorer(it)
                }
            )
        }

        AppPage.History -> {
            BackHandler {
                page = AppPage.Chat
            }

            ChatHistoryScreen(
                historyManager = historyManager,
                currentModelId = selectedModel.id,
                onBack = {
                    page = AppPage.Chat
                },
                onOpenChat = {
                    openChat(it)
                }
            )
        }

        is AppPage.Explorer -> {
            BackHandler {
                page = AppPage.Projects
            }

            FileExplorerScreen(
                project = current.project,
                projectManager = projectManager,
                onBack = {
                    page = AppPage.Projects
                },
                onOpenFile = {
                    page = AppPage.Editor(
                        current.project,
                        it
                    )
                }
            )
        }

        is AppPage.Editor -> {
            CodeEditorScreen(
                project = current.project,
                filePath = current.filePath,
                projectManager = projectManager,
                onBack = {
                    page = AppPage.Explorer(
                        current.project
                    )
                },
                onOpenAI = {
                    page = AppPage.AIEditor(
                        current.project,
                        current.filePath
                    )
                }
            )
        }

        is AppPage.AIEditor -> {
            AIEditorScreen(
                project = current.project,
                filePath = current.filePath,
                projectManager = projectManager,
                apiKeyManager = apiKeyManager,
                onBack = {
                    page = AppPage.Editor(
                        current.project,
                        current.filePath
                    )
                }
            )
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
            },
            title = { Text("Erro") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
