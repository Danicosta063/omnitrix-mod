
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
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
    var currentPage by remember {
        mutableStateOf<AppPage>(AppPage.Chat)
    }

    val messages = remember {
        mutableStateListOf<ChatMessage>()
    }

    var nextMessageId by remember {
        mutableLongStateOf(0L)
    }

    var currentChatId by remember {
        mutableStateOf<String?>(null)
    }

    var selectedModel by remember {
        mutableStateOf(AIModels.defaultModels.first())
    }

    var chatInput by remember {
        mutableStateOf("")
    }

    var chatError by remember {
        mutableStateOf<String?>(null)
    }

    var chatBusy by remember {
        mutableStateOf(false)
    }

    fun loadChat(chat: SavedChat) {
        currentChatId = chat.id
        messages.clear()

        chat.messages.forEach { saved ->
            if (
                saved.role == "user" ||
                saved.role == "assistant"
            ) {
                nextMessageId += 1L

                messages.add(
                    ChatMessage(
                        id = nextMessageId,
                        text = saved.content,
                        isUser = saved.role == "user"
                    )
                )
            }
        }

        selectedModel = AIModels.defaultModels
            .firstOrNull { it.id == chat.modelId }
            ?: AIModels.createCustomModel(chat.modelId)

        chatInput = ""
        currentPage = AppPage.Chat
    }

    fun newChat() {
        currentChatId = null
        messages.clear()
        chatInput = ""
        currentPage = AppPage.Chat
    }

    fun returnToEditor(
        project: CodeProject,
        filePath: String
    ) {
        currentPage = AppPage.Editor(
            project = project,
            filePath = filePath,
            revision = System.nanoTime().toInt()
        )
    }

    when (val page = currentPage) {

        AppPage.Chat -> {
            CodeForgeChatScreen(
                apiKeyManager = apiKeyManager,
                historyManager = historyManager,
                messages = messages,
                nextMessageId = {
                    nextMessageId += 1L
                    nextMessageId
                },
                currentChatId = currentChatId,
                onChatIdChange = {
                    currentChatId = it
                },
                selectedModel = selectedModel,
                onModelChange = {
                    selectedModel = it
                },
                input = chatInput,
                onInputChange = {
                    chatInput = it
                },
                busy = chatBusy,
                onBusyChange = {
                    chatBusy = it
                },
                onOpenSettings = {
                    currentPage = AppPage.Settings
                },
                onOpenProjects = {
                    currentPage = AppPage.Projects
                },
                onOpenHistory = {
                    currentPage = AppPage.History
                },
                onNewChat = {
                    newChat()
                },
                onError = {
                    chatError = it
                }
            )
        }

        AppPage.Settings -> {
            BackHandler {
                currentPage = AppPage.Chat
            }

            SettingsScreen(
                apiKeyManager = apiKeyManager,
                onBack = {
                    currentPage = AppPage.Chat
                }
            )
        }

        AppPage.Projects -> {
            BackHandler {
                currentPage = AppPage.Chat
            }

            ProjectsScreen(
                projectManager = projectManager,
                onBack = {
                    currentPage = AppPage.Chat
                },
                onOpenProject = { project ->
                    currentPage = AppPage.Explorer(project)
                }
            )
        }

        AppPage.History -> {
            BackHandler {
                currentPage = AppPage.Chat
            }

            ChatHistoryScreen(
                historyManager = historyManager,
                currentModelId = selectedModel.id,
                onBack = {
                    currentPage = AppPage.Chat
                },
                onOpenChat = { chat ->
                    loadChat(chat)
                }
            )
        }

        is AppPage.Explorer -> {
            BackHandler {
                currentPage = AppPage.Projects
            }

            FileExplorerScreen(
                project = page.project,
                projectManager = projectManager,
                onBack = {
                    currentPage = AppPage.Projects
                },
                onOpenFile = { filePath ->
                    returnToEditor(
                        page.project,
                        filePath
                    )
                }
            )
        }

        is AppPage.Editor -> {
            key(
                page.project.id,
                page.filePath,
                page.revision
            ) {
                CodeEditorScreen(
                    project = page.project,
                    filePath = page.filePath,
                    projectManager = projectManager,
                    onBack = {
                        currentPage = AppPage.Explorer(
                            page.project
                        )
                    },
                    onOpenAI = {
                        currentPage = AppPage.AIEditor(
                            project = page.project,
                            filePath = page.filePath
                        )
                    }
                )
            }
        }

        is AppPage.AIEditor -> {
            AIEditorScreen(
                project = page.project,
                filePath = page.filePath,
                projectManager = projectManager,
                apiKeyManager = apiKeyManager,
                onBack = {
                    returnToEditor(
                        page.project,
                        page.filePath
                    )
                }
            )
        }
    }

    chatError?.let { error ->
        AlertDialog(
            onDismissRequest = {
                chatError = null
            },
            title = {
                Text("Erro")
            },
            text = {
                Text(error)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatError = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeForgeChatScreen(
    apiKeyManager: ApiKeyManager,
    historyManager: ChatHistoryManager,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<ChatMessage>,
    nextMessageId: () -> Long,
    currentChatId: String?,
    onChatIdChange: (String?) -> Unit,
    selectedModel: AIModel,
    onModelChange: (AIModel) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val api = remember {
        OpenRouterApi()
    }

    var modelMenuExpanded by remember {
        mutableStateOf(false)
    }

    fun sendMessage() {
        val userText = input.trim()

        if (userText.isBlank() || busy) {
            return
        }

        if (userText.length > 100_000) {
            onError("A mensagem excede o limite de 100 mil caracteres.")
            return
        }

        val apiKey = apiKeyManager.getApiKey()

        if (apiKey.isNullOrBlank()) {
            onError(
                "Configure sua chave OpenRouter nas configurações."
            )
            return
        }

        val chatId = try {
            val existingId = currentChatId

            if (existingId != null) {
                historyManager.addMessage(
                    existingId,
                    "user",
                    userText
                )

                existingId
            } else {
                val chat = historyManager.createChat(
                    modelId = selectedModel.id
                )

                historyManager.addMessage(
                    chat.id,
                    "user",
                    userText
                )

                onChatIdChange(chat.id)
                chat.id
            }
        } catch (e: Exception) {
            onError(
                e.message ?: "Erro ao salvar mensagem."
            )
            return
        }

        messages.add(
            ChatMessage(
                id = nextMessageId(),
                text = userText,
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

        val modelId = selectedModel.id

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    api.sendMessage(
                        apiKey = apiKey,
                        model = modelId,
                        messages = requestMessages
                    )
                }

                result.fold(
                    onSuccess = { response ->
                        try {
                            historyManager.addMessage(
                                chatId,
                                "assistant",
                                response
                            )

                            messages.add(
                                ChatMessage(
                                    id = nextMessageId(),
                                    text = response,
                                    isUser = false
                                )
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
                                ?: "Erro ao consultar a IA."
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
                        onClick = onOpenHistory,
                        enabled = !busy
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Histórico"
                        )
                    }

                    IconButton(
                        onClick = onOpenProjects
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Projetos"
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings
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
                    verticalAlignment =
                        Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Peça ajuda com seu código...")
                        },
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                sendMessage()
                            }
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            sendMessage()
                        },
                        enabled = input.isNotBlank() &&
                            !busy
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
            ExposedDropdownMenuBox(
                expanded = modelMenuExpanded,
                onExpandedChange = {
                    if (!busy) {
                        modelMenuExpanded = !modelMenuExpanded
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                OutlinedTextField(
                    value = selectedModel.name,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Modelo de IA")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = modelMenuExpanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        )
                )

                ExposedDropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = {
                        modelMenuExpanded = false
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
                                    currentChatId?.let { chatId ->
                                        historyManager.changeModel(
                 
