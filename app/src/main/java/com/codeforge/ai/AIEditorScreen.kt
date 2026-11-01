
package com.codeforge.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEditorScreen(
    project: CodeProject,
    filePath: String,
    projectManager: ProjectManager,
    apiKeyManager: ApiKeyManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val aiManager = remember(projectManager) {
        ProjectAIManager(projectManager)
    }

    var instruction by remember {
        mutableStateOf("")
    }

    var selectedModel by remember {
        mutableStateOf(AIModels.defaultModels.first())
    }

    var modelMenuExpanded by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var aiResponse by remember {
        mutableStateOf("")
    }

    var preparedChange by remember {
        mutableStateOf<ProjectAIResult?>(null)
    }

    var showConfirmDialog by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun requestAnalysis() {
        if (loading) return

        val key = apiKeyManager.getApiKey()

        if (key.isNullOrBlank()) {
            errorMessage =
                "Configure sua chave OpenRouter nas configurações."
            return
        }

        if (instruction.isBlank()) {
            errorMessage =
                "Escreva o que deseja que a IA faça."
            return
        }

        scope.launch {
            loading = true
            aiResponse = ""
            preparedChange = null

            try {
                val result = aiManager.analyzeFile(
                    projectId = project.id,
                    filePath = filePath,
                    instruction = instruction,
                    apiKey = key,
                    modelId = selectedModel.id
                )

                result.fold(
                    onSuccess = {
                        aiResponse = it.aiResponse

                        try {
                            preparedChange =
                                aiManager.prepareFileChange(
                                    projectId = project.id,
                                    filePath = filePath,
                                    aiResponse = it.aiResponse
                                )
                        } catch (_: Exception) {
                            preparedChange = null
                        }
                    },
                    onFailure = {
                        errorMessage = it.message
                            ?: "Não foi possível analisar o arquivo."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message
                    ?: "Erro ao consultar a IA."
            } finally {
                loading = false
            }
        }
    }

    BackHandler(enabled = !loading) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Assistente de código")

                        Text(
                            text = filePath,
                            style = MaterialTheme
                                .typography.bodySmall,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !loading
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Projeto: ${project.name}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Arquivo: $filePath",
                style = MaterialTheme.typography.bodyMedium
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedCard(
                    onClick = {
                        if (!loading) {
                            modelMenuExpanded = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

                            Spacer(
                                modifier = Modifier.height(4.dp)
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
                                selectedModel = model
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = instruction,
                onValueChange = {
                    instruction = it
                },
                label = {
                    Text("O que a IA deve fazer?")
                },
                placeholder = {
                    Text(
                        "Exemplo: corrija os erros deste " +
                            "arquivo e devolva o código completo."
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp),
                minLines = 4,
                maxLines = 8
            )

            Button(
                onClick = {
                    requestAnalysis()
                },
                enabled = !loading &&
                    instruction.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    if (loading) {
                        "Analisando..."
                    } else {
                        "Analisar com IA"
                    }
                )
            }

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (aiResponse.isNotBlank()) {
                HorizontalDivider()

                Text(
                    "Resposta da IA",
                    style = MaterialTheme.typography.titleLarge
                )

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiResponse,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                val change = preparedChange

                if (
                    change != null &&
                    !change.suggestedCode.isNullOrBlank()
                ) {
                    HorizontalDivider()

                    Text(
                        "Código sugerido",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Confira o conteúdo completo antes " +
                            "de substituir o arquivo original.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(
                                    rememberScrollState()
                                )
                                .horizontalScroll(
                                    rememberScrollState()
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = change.suggestedCode.orEmpty(),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme
                                    .typography.bodySmall,
                                softWrap = false
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            showConfirmDialog = true
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Aplicar código sugerido")
                    }

                    OutlinedButton(
                        onClick = {
                            preparedChange = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Descartar sugestão")
                    }
                } else {
                    Text(
                        "Nenhuma substituição automática foi " +
                            "preparada. Você pode consultar a " +
                            "resposta acima.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
            },
            title = {
                Text("Substituir arquivo?")
            },
            text = {
                Text(
                    "O conteúdo atual de '$filePath' será " +
                        "substituído pelo código sugerido. " +
                        "Confira a sugestão antes de continuar. " +
                        "Recomendamos exportar um ZIP de backup."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false

                        val change = preparedChange

                        if (change != null) {
                            try {
                                val result =
                                    aiManager.applyFileChange(change)

                                result.fold(
                                    onSuccess = {
                                        preparedChange = null
                                        successMessage =
                                            "Arquivo atualizado com sucesso!"
                                    },
                                    onFailure = {
                                        errorMessage = it.message
                                            ?: "Erro ao salvar alteração."
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message
                                    ?: "Erro ao aplicar alteração."
                            }
                        }
                    }
                ) {
                    Text("Substituir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
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

    successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                successMessage = null
            },
            title = {
                Text("Sucesso")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        successMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
