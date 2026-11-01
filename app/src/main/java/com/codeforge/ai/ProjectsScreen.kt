
package com.codeforge.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectManager: ProjectManager,
    onBack: () -> Unit,
    onOpenProject: (CodeProject) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val zipManager = remember(context) {
        ZipExportManager(context)
    }

    var projects by remember {
        mutableStateOf(emptyList<CodeProject>())
    }

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var projectToRename by remember {
        mutableStateOf<CodeProject?>(null)
    }

    var projectToDelete by remember {
        mutableStateOf<CodeProject?>(null)
    }

    var projectToExport by remember {
        mutableStateOf<CodeProject?>(null)
    }

    var nameInput by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    var exporting by remember {
        mutableStateOf(false)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/zip"
        )
    ) { uri: Uri? ->
        val project = projectToExport
        projectToExport = null

        if (uri != null && project != null) {
            scope.launch {
                exporting = true

                try {
                    val result = withContext(Dispatchers.IO) {
                        zipManager.exportProject(
                            project,
                            uri
                        )
                    }

                    result.fold(
                        onSuccess = {
                            successMessage =
                                "Projeto exportado com sucesso!"
                        },
                        onFailure = { error ->
                            errorMessage = error.message
                                ?: "Não foi possível exportar o ZIP."
                        }
                    )
                } catch (e: Exception) {
                    errorMessage = e.message
                        ?: "Erro ao exportar projeto."
                } finally {
                    exporting = false
                }
            }
        }
    }

    fun refreshProjects() {
        try {
            projects = projectManager.listProjects()
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Erro ao listar projetos."
        }
    }

    LaunchedEffect(projectManager) {
        refreshProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Meus projetos")
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    nameInput = ""
                    showCreateDialog = true
                },
                modifier = Modifier
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Novo projeto"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {

            if (exporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("Exportando projeto...")

                Spacer(Modifier.height(12.dp))
            }

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum projeto criado. " +
                            "Toque em + para começar."
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = projects,
                        key = { it.id }
                    ) { project ->

                        var menuExpanded by remember {
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
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme
                                        .colorScheme.primary
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onOpenProject(project)
                                        }
                                ) {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme
                                            .typography.titleMedium
                                    )

                                    Text(
                                        text = "Toque para abrir",
                                        style = MaterialTheme
                                            .typography.bodySmall
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
                                                "Opções do projeto"
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
                                                Text("Abrir")
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onOpenProject(project)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text("Exportar ZIP")
                                            },
                                            enabled = !exporting,
                                            onClick = {
                                                menuExpanded = false
                                                projectToExport = project

                                                exportLauncher.launch(
                                                    zipManager
                                                        .suggestFileName(
                                                            project
                                                        )
                                                )
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
                                                nameInput = project.name
                                                projectToRename = project
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
                                                projectToDelete = project
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
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
            },
            title = {
                Text("Novo projeto")
            },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                    },
                    label = {
                        Text("Nome do projeto")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        try {
                            val project =
                                projectManager.createProject(
                                    nameInput
                                )

                            refreshProjects()
                            showCreateDialog = false
                            onOpenProject(project)
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao criar projeto."
                        }
                    }
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    projectToRename?.let { project ->
        AlertDialog(
            onDismissRequest = {
                projectToRename = null
            },
            title = {
                Text("Renomear projeto")
            },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                    },
                    label = {
                        Text("Novo nome")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        try {
                            projectManager.renameProject(
                                project.id,
                                nameInput
                            )

                            refreshProjects()
                            projectToRename = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao renomear projeto."
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        projectToRename = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = {
                projectToDelete = null
            },
            title = {
                Text("Excluir projeto?")
            },
            text = {
                Text(
                    "Todos os arquivos de '${project.name}' " +
                        "serão excluídos permanentemente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            projectManager.deleteProject(
                                project.id
                            )

                            refreshProjects()
                            projectToDelete = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao excluir projeto."
                        }
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        projectToDelete = null
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
