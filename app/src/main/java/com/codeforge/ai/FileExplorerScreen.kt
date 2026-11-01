
package com.codeforge.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    project: CodeProject,
    projectManager: ProjectManager,
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    var files by remember(project.id) {
        mutableStateOf(emptyList<ProjectFile>())
    }

    var showNewMenu by remember {
        mutableStateOf(false)
    }

    var dialogAction by remember {
        mutableStateOf("")
    }

    var selectedFile by remember {
        mutableStateOf<ProjectFile?>(null)
    }

    var pathInput by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun refreshFiles() {
        try {
            files = projectManager.listFiles(project.id)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro ao carregar arquivos."
        }
    }

    LaunchedEffect(project.id) {
        refreshFiles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showNewMenu = true }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Adicionar"
                            )
                        }

                        DropdownMenu(
                            expanded = showNewMenu,
                            onDismissRequest = {
                                showNewMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Novo arquivo") },
                                onClick = {
                                    showNewMenu = false
                                    pathInput = ""
                                    dialogAction = "create_file"
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Nova pasta") },
                                onClick = {
                                    showNewMenu = false
                                    pathInput = ""
                                    dialogAction = "create_folder"
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Projeto vazio. Toque em + para criar arquivos."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = files,
                    key = { it.path }
                ) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!file.isDirectory) {
                                        onOpenFile(file.path)
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) {
                                    Icons.Default.Folder
                                } else {
                                    Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = file.path,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (!file.isDirectory) {
                                    Text(
                                        text = "${file.size} bytes",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    selectedFile = file
                                    pathInput = file.path
                                    dialogAction = "rename"
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Renomear"
                                )
                            }

                            IconButton(
                                onClick = {
                                    selectedFile = file
                                    dialogAction = "delete"
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (
        dialogAction == "create_file" ||
        dialogAction == "create_folder" ||
        dialogAction == "rename"
    ) {
        val action = dialogAction

        AlertDialog(
            onDismissRequest = {
                dialogAction = ""
                selectedFile = null
            },
            title = {
                Text(
                    when (action) {
                        "create_file" -> "Criar arquivo"
                        "create_folder" -> "Criar pasta"
                        else -> "Renomear item"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Use caminhos como src/Main.kt ou assets/imagens."
                    )

                    OutlinedTextField(
                        value = pathInput,
                        onValueChange = { pathInput = it },
                        label = { Text("Caminho") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pathInput.isNotBlank(),
                    onClick = {
                        try {
                            when (action) {
                                "create_file" -> {
                                    require(
                                        projectManager.listFiles(project.id)
                                            .none { it.path == pathInput }
                                    ) {
                                        "Já existe um item nesse caminho."
                                    }

                                    projectManager.saveFile(
                                        project.id,
                                        pathInput,
                                        ""
                                    )
                                }

                                "create_folder" -> {
                                    projectManager.createFolder(
                                        project.id,
                                        pathInput
                                    )
                                }

                                "rename" -> {
                                    val original = selectedFile
                                        ?: error("Item não selecionado.")

                                    projectManager.renameFile(
                                        project.id,
                                        original.path,
                                        pathInput
                                    )
                                }
                            }

                            refreshFiles()
                            dialogAction = ""
                            selectedFile = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Não foi possível concluir a operação."
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogAction = ""
                        selectedFile = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (dialogAction == "delete") {
        val item = selectedFile

        AlertDialog(
            onDismissRequest = {
                dialogAction = ""
                selectedFile = null
            },
            title = { Text("Excluir item?") },
            text = {
                Text(
                    "Excluir '${item?.path.orEmpty()}'? " +
                        "Se for uma pasta, todo o conteúdo será apagado."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val target = item
                                ?: error("Item não selecionado.")

                            projectManager.deleteFile(
                                project.id,
                                target.path
                            )

                            refreshFiles()
                            dialogAction = ""
                            selectedFile = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao excluir item."
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
                        selectedFile = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
            },
            title = { Text("Aviso") },
            text = { Text(error) },
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
