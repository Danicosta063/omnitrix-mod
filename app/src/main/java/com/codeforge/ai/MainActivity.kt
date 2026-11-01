
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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

        val apiKeyManager = ApiKeyManager(
            applicationContext
        )

        val projectManager = ProjectManager(
            applicationContext
        )

        setContent {
            val colors = darkColorScheme(
                primary = AppAccent,
                background = AppBackground,
                surface = AppSurface
            )

            MaterialTheme(
                colorScheme = colors
            ) {
                CodeForgeApp(
                    apiKeyManager = apiKeyManager,
                    projectManager = projectManager
                )
            }
        }
    }
}

@Composable
private fun CodeForgeApp(
    apiKeyManager: ApiKeyManager,
    projectManager: ProjectManager
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

    var selectedModel by remember {
        mutableStateOf(AIModels.defaultModels.first())
    }

    var chatInput by remember {
        mutableStateOf("")
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
                messages = messages,
                nextMessageId = {
                    nextMessageId += 1L
                    nextMessageId
                },
                selectedModel = selectedModel,
                onModelChange = {
                    selectedModel = it
                },
                input = chatInput,
                onInputChange = {
                    chatInput = it
                },
                onOpenSettings = {
                    currentPage = AppPage.Settings
                },
                onOpenProjects = {
                    currentPage = AppPage.Projects
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeForgeChatScreen(
    apiKeyManager: ApiKeyManager,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<ChatMessage>,
    nextMessageId: () -> Long,
    selectedModel: AIModel,
    onModelChange: (AIModel) -> Unit,
    input: String,
    onInputChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProjects: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val api = remember {
        OpenRouterApi()
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var modelMenuExpanded by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun sendMessage() {
        val userText = input.trim()

        if (userText.isBlank() || loading) {
            return
        }

        val apiKey = apiKeyManager.getApiKey()

        if (apiKey.isNullOrBlank()) {
            errorMessage =
                "Configure sua chave OpenRouter nas configurações."
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
        loading = true

        scope.launch {
            try {
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

                val result = api.sendMessage(
                    apiKey = apiKey,
                    model = selectedModel.id,
                    messages = requestMessages
                )

                result.fold(
                    onSuccess = { response ->
                        messages.add(
                            ChatMessage(
                                id = nextMessageId(),
                                text = response,
                                isUser = false
                            )
                        )
                    },
                    onFailure = { error ->
                        errorMessage = error.message
                            ?: "Erro ao consultar a IA."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message
                    ?: "Erro ao enviar mensagem."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(messages.size, loading) {
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
                            !loading
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
                    modelMenuExpanded = !modelMenuExpanded
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
                                onModelChange(model)
                                modelMenuExpanded = false
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
                    Text(
                        "Seu assistente de programação está pronto.",
                        style = MaterialTheme
                            .typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageBubble(message)
                    }

                    if (loading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
            },
            title = {
                Text("Erro")
            },
            text = {
                Text(message)
            },
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
            shape = RoundedCornerShape(16.dp),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .widthIn(max = 340.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
