
package com.codeforge.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    project: CodeProject,
    filePath: String,
    projectManager: ProjectManager,
    onBack: () -> Unit
) {
    var code by remember(project.id, filePath) {
        mutableStateOf("")
    }

    var savedCode by remember(project.id, filePath) {
        mutableStateOf("")
    }

    var loaded by remember(project.id, filePath) {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }

    var statusMessage by remember {
        mutableStateOf("")
    }

    val hasUnsavedChanges = loaded && code != savedCode

    LaunchedEffect(project.id, filePath) {
        try {
            val content = projectManager.readFile(
                project.id,
                filePath
            )

            code = content
            savedCode = content
            loaded = true
            statusMessage = ""
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Não foi possível abrir o arquivo."
        }
    }

    fun saveCode() {
        try {
            projectManager.saveFile(
                project.id,
                filePath,
                code
            )

            savedCode = code
            statusMessage = "Arquivo salvo com sucesso."
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Não foi possível salvar o arquivo."
        }
    }

    fun requestBack() {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
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
                            filePath.substringAfterLast("/"),
                            maxLines = 1
                        )

                        Text(
                            if (hasUnsavedChanges) {
                                "Alterações não salvas"
                            } else {
                                filePath
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { requestBack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { saveCode() },
                        enabled = loaded && hasUnsavedChanges
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Salvar"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (hasUnsavedChanges) {
                            "● Modificado"
                        } else {
                            "✓ Salvo"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "${code.count { it == '\n' } + 1} linhas",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (statusMessage.isNotBlank()) {
                    Text(
                        statusMessage,
                        modifier = Modifier.padding(
                            start = 10.dp,
                            bottom = 8.dp
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { padding ->

        if (!loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Carregando arquivo...")
            }
        } else {
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(12.dp)
            ) {
                val lineCount = code.count {
                    it == '\n'
                } + 1

                Column(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (line in 1..lineCount) {
                        Text(
                            text = line.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.Gray
                        )
                    }
                }

                BasicTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        statusMessage = ""
                    },
                    modifier = Modifier.widthIn(min = 320.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (code.isEmpty()) {
                                Text(
                                    "// Escreva seu código aqui",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            innerTextField()
                        }
                    }
                )
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
                    "Você modificou este arquivo. " +
                        "Deseja salvar antes de sair?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveCode()

                        if (code == savedCode) {
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
                        }
                    ) {
                        Text("Cancelar")
                    }

                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onBack()
                        }
                    ) {
                        Text("Descartar")
                    }
                }
            }
        )
    }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
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
                        errorMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
