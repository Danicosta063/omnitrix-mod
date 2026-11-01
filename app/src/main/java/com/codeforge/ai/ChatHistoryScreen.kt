
package com.codeforge.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    historyManager: ChatHistoryManager,
    currentModelId: String,
    onBack: () -> Unit,
    onOpenChat: (SavedChat) -> Unit
) {
    var chats by remember {
        mutableStateOf(emptyList<SavedChat>())
    }

    var selectedChat by remember {
        mutableStateOf<SavedChat?>(null)
    }

    var dialogAction by remember {
        mutableStateOf("")
    }

    var titleInput by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun refreshChats() {
        try {
            chats = historyManager.listChats()
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Erro ao carregar conversas."
        }
    }

    fun createNewChat() {
        try {
            val chat = historyManager.createChat(
                modelId = currentModelId
            )

            refreshChats()
            onOpenChat(chat)
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Erro ao criar conversa."
        }
    }

    LaunchedEffect(historyManager) {
        refreshChats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Histórico de conversas")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            createNewChat()
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nova conversa"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    createNewChat()
                },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text("Novo chat")
                }
            )
        }
    ) { padding ->

        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme
                            .colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Nenhuma conversa salva",
                        style = MaterialTheme
                            .typography.titleMedium
                    )

                    Text(
                        "Crie uma conversa para começar.",
                        style = MaterialTheme
                            .typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 100.dp
                ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = chats,
                    key = { it.id }
                ) { chat ->

                    var menuExpanded by remember(chat.id) {
                        mutableStateOf(false)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme
                                    .colorScheme.primary
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onOpenChat(chat)
                                    },
                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    chat.title,
                                    style = MaterialTheme
                                        .typography.titleMedium,
                                    maxLines = 2
                                )

                                val modelName =
                                    AIModels.defaultModels
                                        .firstOrNull {
                                            it.id == chat.modelId
                                        }
                                        ?.name
                                        ?: chat.modelId

                                Text(
                                    modelName,
                                    style = MaterialTheme
                                        .typography.bodySmall
                                )

                                Text(
                                    DateFormat
                                        .getDateTimeInstance(
                                            DateFormat.SHORT,
                                            DateFormat.SHORT
                                        )
                                        .format(
                                            Date(chat.updatedAt)
                                        ),
                                    style = MaterialTheme
                                        .typography.labelSmall,
                                    color = MaterialTheme
                                        .colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = {
                                        menuExpanded = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription =
                                            "Opções da conversa"
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = {
                                        menuExpanded = false
                                    }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text("Abrir conversa")
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onOpenChat(chat)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text("Renomear")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            selectedChat = chat
                                            titleInput = chat.title
                                            dialogAction = "rename"
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text("Excluir")
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            selectedChat = chat
                                            dialogAction = "delete"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogAction == "rename") {
        val chat = selectedChat

        AlertDialog(
            onDismissRequest = {
                dialogAction = ""
                selectedChat = null
            },
            title = {
                Text("Renomear conversa")
            },
            text = {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                    },
                    label = {
                        Text("Nome da conversa")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = titleInput.isNotBlank(),
                    onClick = {
                        try {
                            val target = chat
                                ?: error(
                                    "Conversa não selecionada."
                                )

                            historyManager.renameChat(
                                target.id,
                                titleInput
                            )

                            refreshChats()
                            dialogAction = ""
                            selectedChat = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao renomear conversa."
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogAction = ""
                        selectedChat = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (dialogAction == "delete") {
        val chat = selectedChat

        AlertDialog(
            onDismissRequest = {
                dialogAction = ""
                selectedChat = null
            },
            title = {
                Text("Excluir conversa?")
            },
            text = {
                Text(
                    "A conversa '${chat?.title.orEmpty()}' " +
                        "será excluída permanentemente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val target = chat
                                ?: error(
                                    "Conversa não selecionada."
                                )

                            val deleted =
                                historyManager.deleteChat(
                                    target.id
                                )

                            if (!deleted) {
                                error(
                                    "Não foi possível excluir a conversa."
                                )
                            }

                            refreshChats()
                            dialogAction = ""
                            selectedChat = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao excluir conversa."
                        }
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogAction = ""
                        selectedChat = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
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
