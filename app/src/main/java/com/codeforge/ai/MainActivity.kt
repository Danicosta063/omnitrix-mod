
package com.codeforge.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val Background = Color(0xFF101218)
private val Surface = Color(0xFF1C2029)
private val Accent = Color(0xFF65D6A5)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyManager = ApiKeyManager(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Background,
                    surface = Surface
                )
            ) {
                var showSettings by remember {
                    mutableStateOf(false)
                }

                if (showSettings) {
                    SettingsScreen(
                        apiKeyManager = keyManager,
                        onBack = { showSettings = false }
                    )
                } else {
                    CodeForgeScreen(
                        apiKeyManager = keyManager,
                        onOpenSettings = {
                            showSettings = true
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeForgeScreen(
    apiKeyManager: ApiKeyManager,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val api = remember { OpenRouterApi() }
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    var selectedModel by remember {
        mutableStateOf(AIModels.defaultModels.first())
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Olá! Sou o CodeForge AI. " +
                    "Configure sua chave do OpenRouter " +
                    "e escolha um modelo para começar.",
                false
            )
        )
    }

    fun sendMessage() {
        val userText = input.trim()

        if (userText.isBlank() || loading) return

        val apiKey = try {
            apiKeyManager.getApiKey()
        } catch (_: Exception) {
            null
        }

        if (apiKey.isNullOrBlank()) {
            messages.add(
                ChatMessage(
                    "Configure sua chave de API " +
                        "nas configurações antes de enviar.",
                    false
                )
            )
            return
        }

        val history = mutableListOf(
            ApiMessage(
                role = "system",
                content = AIModels.systemPrompt
            )
        )

        messages.forEach { message ->
            if (message.isUser) {
                history.add(
                    ApiMessage("user", message.text)
                )
            } else if (!message.text.startsWith("Olá!") &&
                !message.text.startsWith("Configure sua chave") &&
                !message.text.startsWith("Erro:")) {
                history.add(
                    ApiMessage("assistant", message.text)
                )
            }
        }

        history.add(ApiMessage("user", userText))

        messages.add(ChatMessage(userText, true))
        input = ""
        loading = true

        scope.launch {
            try {
                val result = api.sendMessage(
                    apiKey = apiKey,
                    model = selectedModel.id,
                    messages = history
                )

                result.fold(
                    onSuccess = {
                        messages.add(ChatMessage(it, false))
                    },
                    onFailure = {
                        messages.add(
                            ChatMessage(
                                "Erro: " +
                                    (it.message ?: "Falha desconhecida"),
                                false
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        "Erro: " +
                            (e.message ?: "Falha desconhecida"),
                        false
                    )
                )
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
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier.background(Surface)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = Accent
                            )
                            Text(
                                " CodeForge AI",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Configurações"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        if (!loading) expanded = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    OutlinedTextField(
                        value = selectedModel.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modelo de IA") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                ExposedDropdownMenuAnchorType
                                    .PrimaryNotEditable
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        AIModels.defaultModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        model.name +
                                            " — " +
                                            model.provider
                                    )
                                },
                                onClick = {
                                    selectedModel = model
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Peça um código...")
                    },
                    maxLines = 6,
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )

                IconButton(
                    onClick = { sendMessage() },
                    enabled = input.isNotBlank() && !loading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Accent
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(messages) { _, message ->
                MessageBubble(message)
            }

            if (loading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("A IA está respondendo...")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    Color(0xFF214E40)
                } else {
                    Surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                if (!message.isUser) {
                    Text(
                        "CodeForge AI",
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    message.text,
                    color = Color.White
                )
            }
        }
    }
}
