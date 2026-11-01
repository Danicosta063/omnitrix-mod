
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
        val filePath: String,
        val revision: Int = 0
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
                    apiKeyManager = apiKeyManager,
                    projectManager = projectManager,
                    historyManager = historyManager
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

    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
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
                nextId += 1L
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

    fun returnToEditor(
        project: CodeProject,
        filePath: String
    ) {
        page = AppPage.Editor(
            project = project,
            filePath = filePath,
            revision = System.nanoTime().toInt()
        )
    }

    when (val current = page) {
        AppPage.Chat -> {
            CodeForgeChatScreen(
                apiKeyManager = apiKeyManager,
                historyManager = historyManager,
                messages = messages,
                nextId = {
                    nextId += 1L
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
                onInputChange = { input = it },
                busy = busy,
                onBusyChange = { busy = it },
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
                onBack = { page = AppPage.Chat }
            )
        }

        AppPage.Projects -> {
            BackHandler {
                page = AppPage.Chat
            }
            ProjectsScreen(
                projectManager = projectManager,
                onBack = { page = AppPage.Chat },
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
                onBack = { page = AppPage.Chat },
                onOpenChat = { openChat(it) }
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
                    returnToEditor(current.project, it)
                }
            )
        }

        is AppPage.Editor -> {
            key(
                current.project.id,
                current.filePath,
                current.revision
            ) {
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
        }

        is AppPage.AIEditor -> {
            AIEditorScreen(
                project = current.project,
                filePath = current.filePath,
                projectManager = projectManager,
                apiKeyManager = apiKeyManager,
                onBack = {
                    returnToEditor(
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

@Composable
private fun CodeForgeChatScreen(
    apiKeyManager: ApiKeyManager,
    historyManager: ChatHistoryManager,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<ChatMessage>,
    nextId: () -> Long,
    chatId: String?,
    onChatIdChange: (String?) -> Unit,
    selectedModel: AIModel,
    onModelChange: (AIModel) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onNewChat: () -> Unit,
    onHistory: () -> Unit,
    onProjects: () -> Unit,
    onSettings: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val api = remember { OpenRouterApi() }

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    fun sendMessage() {
        val text = input.trim()

        if (text.isBlank() || busy) return

        if (text.length > 100_000) {
            onError("Mensagem muito grande.")
            return
        }

        val apiKey = apiKeyManager.getApiKey()

        if (apiKey.isNullOrBlank()) {
            onError(
                "Configure sua chave OpenRouter nas configurações."
            )
            return
        }

        val activeChatId = try {
            if (chatId != null) {
                historyManager.addMessage(
                    chatId,
                    "user",
                    text
                )
                chatId
            } else {
                val newChat = historyManager.createChat(
                    modelId = selectedModel.id
                )

                historyManager.addMessage(
                    newChat.id,
                    "user",
                    text
                )

                onChatIdChange(newChat.id)
                newChat.id
            }
        } catch (e: Exception) {
            onError(
                e.message ?: "Erro ao salvar conversa."
            )
            return
        }

        messages.add(
            ChatMessage(
                id = nextId(),
                text = text,
                isUser = true
            )
        )

        onInputChange("")
        onBusyChange(true)

        val requestMessages = buildList {
            add(
                ApiMessage(
                    role = "system",
                    content = AIModels.systemPrompt
                )
            )

            messages.forEach { message ->
                add(
                    ApiMessage(
                        role = if (message.isUser) {
                            "user"
                        } else {
                            "assistant"
                        },
                        content = message.text
                    )
                )
            }
        }

        val requestModelId = selectedModel.id

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    api.sendMessage(
                        apiKey = apiKey,
                        model = requestModelId,
                        messages = requestMessages
                    )
                }

                result.fold(
                    onSuccess = { response ->
                        messages.add(
                            ChatMessage(
                                id = nextId(),
                                text = response,
                                isUser = false
                            )
                        )

                        try {
                            historyManager.addMessage(
                                activeChatId,
                                "assistant",
                                response
                            )
                        } catch (e: Exception) {
                            onError(
                                e.message
                                    ?: "Erro ao salvar resposta."
                            )
                        }
                    },
                    onFailure = { error ->
                        onError(
                            error.message
                                ?: "Erro ao consultar IA."
                        )
                    }
                )
            } catch (e: Exception) {
                onError(
                    e.message ?: "Erro ao enviar mensagem."
                )
            } finally {
                onBusyChange(false)
            }
        }
    }

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("CodeForge AI")
                },
                actions = {
                    IconButton(
                        onClick = onNewChat,
                        enabled = !busy
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nova conversa"
                        )
                    }

                    IconButton(
                        onClick = onHistory,
                        enabled = !busy
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Histórico"
                        )
                    }

                    IconButton(
                        onClick = onProjects
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Projetos"
                        )
                    }

                    IconButton(
                        onClick = onSettings
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurações"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Peça ajuda com seu código...")
                        },
                        maxLines = 5
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    IconButton(
                        onClick = { sendMessage() },
                        enabled = input.isNotBlank() && !busy
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = AppAccent
                        )
                    }
                }
            }
        }
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                OutlinedCard(
                    onClick = {
                        if (!busy) {
                            menuExpanded = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Modelo de IA",
                                style = MaterialTheme
                                    .typography.labelMedium
                            )

                            Text(
                                selectedModel.name,
                                style = MaterialTheme
                                    .typography.bodyLarge
                            )
                        }

                        Text("▼")
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    }
                ) {
                    AIModels.defaultModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(model.name)
                                    Text(
                                        model.provider,
                                        style = MaterialTheme
                                            .typography.bodySmall
                                    )
                                }
                            },
                            onClick = {
                                try {
                                    if (chatId != null) {
                                        historyManager.changeModel(
                                            chatId,
                                            model.id
                                        )
                                    }

                                    onModelChange(model)
                                    menuExpanded = false
                                } catch (e: Exception) {
                                    onError(
                                        e.message
                                            ?: "Erro ao alterar modelo."
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "CodeForge AI",
                            style = MaterialTheme
                                .typography.headlineMedium
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            "Seu assistente de programação com IA.",
                            style = MaterialTheme
                                .typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageBubble(message)
                    }

                    if (busy) {
                        item {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    modifier = Modifier.width(12.dp)
                                )

                                Text("A IA está respondendo...")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            color = if (message.isUser) {
                AppAccent.copy(alpha = 0.18f)
            } else {
                AppSurface
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = if (message.isUser) {
                        "Você"
                    } else {
                        "IA"
                    },
                    color = AppAccent,
                    style = MaterialTheme
                        .typography.labelMedium
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = message.text,
                    style = MaterialTheme
                        .typography.bodyMedium
                )
            }
        }
    }
}
