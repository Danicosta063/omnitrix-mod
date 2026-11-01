
package com.codeforge.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKeyManager: ApiKeyManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val api = remember { OpenRouterApi() }

    var apiKey by remember { mutableStateOf("") }
    var hasSavedKey by remember {
        mutableStateOf(apiKeyManager.hasApiKey())
    }
    var showKey by remember { mutableStateOf(false) }
    var selectedModel by remember {
        mutableStateOf(AIModels.defaultModels.first())
    }
    var expanded by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Voltar")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "OpenRouter API",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                if (hasSavedKey)
                    "Chave de API armazenada neste dispositivo."
                else
                    "Nenhuma chave de API configurada."
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Chave de API") },
                placeholder = { Text("sk-or-v1-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (showKey)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation()
            )

            Row {
                Checkbox(
                    checked = showKey,
                    onCheckedChange = { showKey = it }
                )
                Text(
                    "Mostrar chave digitada",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Button(
                onClick = {
                    try {
                        apiKeyManager.saveApiKey(apiKey)
                        apiKey = ""
                        hasSavedKey = true
                        status = "Chave salva com sucesso."
                    } catch (e: Exception) {
                        status = e.message ?: "Erro ao salvar chave."
                    }
                },
                enabled = apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar chave")
            }

            HorizontalDivider()

            Text(
                "Modelo de inteligência artificial",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedModel.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecionar modelo") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AIModels.defaultModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Text(model.name + " - " + model.provider)
                            },
                            onClick = {
                                selectedModel = model
                                expanded = false
                            }
                        )
                    }
                }
            }

            Text(
                "Modelo: " + selectedModel.id,
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    testing = true
                    status = "Testando conexão..."

                    scope.launch {
                        try {
                            val key = apiKeyManager.getApiKey()

                            if (key.isNullOrBlank()) {
                                status = "Configure uma chave primeiro."
                            } else {
                                val result = api.testConnection(
                                    key,
                                    selectedModel.id
                                )

                                status = result.fold(
                                    onSuccess = {
                                        "Conexão funcionando! Resposta: " + it
                                    },
                                    onFailure = {
                                        "Erro: " + (it.message ?: "Desconhecido")
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            status = "Erro: " + (e.message ?: "Desconhecido")
                        } finally {
                            testing = false
                        }
                    }
                },
                enabled = hasSavedKey && !testing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (testing)
                        "Testando..."
                    else
                        "Testar conexão"
                )
            }

            if (status.isNotBlank()) {
                Text(status)
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = {
                    try {
                        apiKeyManager.deleteApiKey()
                        hasSavedKey = false
                        apiKey = ""
                        status = "Chave excluída."
                    } catch (e: Exception) {
                        status = e.message ?: "Erro ao excluir chave."
                    }
                },
                enabled = hasSavedKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Excluir chave de API")
            }

            Text(
                "Aviso: os modelos podem consumir créditos do OpenRouter. " +
                    "A chave fica armazenada criptografada no dispositivo.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
