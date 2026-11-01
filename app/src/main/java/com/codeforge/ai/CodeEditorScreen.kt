
package com.codeforge.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    project: CodeProject,
    filePath: String,
    projectManager: ProjectManager,
    onBack: () -> Unit,
    onOpenAI: () -> Unit = {}
) {
    var code by remember(
        project.id,
        filePath
    ) {
        mutableStateOf("")
    }

    var savedCode by remember(
        project.id,
        filePath
    ) {
        mutableStateOf("")
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }

    var showAIDialog by remember {
        mutableStateOf(false)
    }

    var showErrorDialog by remember {
        mutableStateOf(false)
    }

    var showSaveMessage by remember {
        mutableStateOf(false)
    }

    val hasChanges = code != savedCode

    fun reloadFile() {
        try {
            val content = projectManager.readFile(
                project.id,
                filePath
            )

            code = content
            savedCode = content
            loadError = null
        } catch (e: Exception) {
            loadError = e.message
                ?: "Não foi possível abrir o arquivo."
        }
    }

    fun saveFile(): Boolean {
        return try {
            projectManager.saveFile(
                project.id,
                filePath,
                code
            )

            savedCode = code
            message = "Arquivo salvo com sucesso!"
            showSaveMessage = true
            true
        } catch (e: Exception) {
            message = e.message
                ?: "Erro ao salvar arquivo."
            showErrorDialog = true
            false
        }
    }

    fun requestBack() {
        if (hasChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(
        project.id,
        filePath
    ) {
        reloadFile()
    }

    BackHandler {
        requestBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            filePath.substringAfterLast('/')
                        )

                        Text(
                            project.name,
                            style = MaterialTheme
                                .typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            requestBack()
                        }
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
                            if (hasChanges) {
                                showAIDialog = true
                            } else {
                                onOpenAI()
                            }
                        },
                        enabled = loadError == null
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription =
                                "Analisar arquivo com IA"
                        )
                    }

                    IconButton(
                        onClick = {
                            saveFile()
                        },
                        enabled = loadError == null &&
                            hasChanges
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Salvar arquivo"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "Linhas: ${code.count { it == '\n' } + 1}",
                        style = MaterialTheme
                            .typography.bodySmall
                    )

                    Text(
                        if (hasChanges) {
                            "Não salvo"
                        } else {
                            "Salvo"
                        },
                        style = MaterialTheme
                            .typography.bodySmall,
                        color = if (hasChanges) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loadError != null) {
                Text(
                    text = loadError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                val verticalScroll = rememberScrollState()
                val horizontalScroll = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                        .horizontalScroll(horizontalScroll)
                        .padding(12.dp)
                ) {
                    val lineCount = code.count {
                        it == '\n'
                    } + 1

                    Text(
                        text = (1..lineCount)
                            .joinToString("\n"),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = MaterialTheme
                                .typography.bodyMedium.fontSize,
                            lineHeight = MaterialTheme
                                .typography.bodyMedium.lineHeight,
                            color = MaterialTheme
                                .colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(Modifier.width(12.dp))

                    BasicTextField(
                        value = code,
                        onValueChange = {
                            code = it
                        },
                        modifier = Modifier
                            .widthIn(min = 280.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = MaterialTheme
                                .typography.bodyMedium.fontSize,
                            lineHeight = MaterialTheme
                                .typography.bodyMedium.lineHeight,
                            color = MaterialTheme
                                .colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(
                            MaterialTheme.colorScheme.primary
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (code.isEmpty()) {
                                    Text(
                                        "Digite seu código aqui...",
                                        color = MaterialTheme
                                            .colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },
            title = {
                Text("Alterações não salvas")
            },
            text = {
                Text(
                    "Deseja salvar suas alterações " +
                        "antes de sair?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveFile()) {
                            showExitDialog = false
                            onBack()
                        }
                    }
                ) {
                    Text("Salvar e sair")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onBack()
                        }
                    ) {
                        Text("Descartar")
                    }

                    TextButton(
                        onClick = {
                            showExitDialog = false
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    if (showAIDialog) {
        AlertDialog(
            onDismissRequest = {
                showAIDialog = false
            },
            title = {
                Text("Salvar antes de usar IA?")
            },
            text = {
                Text(
                    "O assistente analisa a versão salva " +
                        "do arquivo. Deseja salvar suas " +
                        "alterações antes de continuar?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveFile()) {
                            showAIDialog = false
                            onOpenAI()
                        }
                    }
                ) {
                    Text("Salvar e abrir IA")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAIDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
            },
            title = {
                Text("Erro")
            },
            text = {
                Text(message.orEmpty())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showSaveMessage) {
        AlertDialog(
            onDismissRequest = {
                showSaveMessage = false
            },
            title = {
                Text("Sucesso")
            },
            text = {
                Text(message.orEmpty())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveMessage = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
