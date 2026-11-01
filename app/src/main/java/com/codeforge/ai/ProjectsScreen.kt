
package com.codeforge.ai

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectManager: ProjectManager,
    onBack: () -> Unit,
    onOpenProject: (CodeProject) -> Unit
) {
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

    var nameInput by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun refreshProjects() {
        try {
            projects = projectManager.listProjects()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro ao listar projetos."
        }
    }

    LaunchedEffect(projectManager) {
        refreshProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus projetos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                }
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
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum projeto criado. Toque em + para começar."
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = projects,
                        key = { it.id }
                    ) { project ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenProject(project)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        project.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        "Abrir projeto",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        nameInput = project.name
                                        projectToRename = project
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Renomear"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        projectToDelete = project
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
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
            },
            title = { Text("Novo projeto") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nome do projeto") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        try {
                            val project = projectManager.createProject(
                                nameInput
                            )
                            refreshProjects()
                            showCreateDialog = false
                            onOpenProject(project)
                        } catch (e: Exception) {
                            errorMessage = e.message
                        }
                    }
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false }
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
            title = { Text("Renomear projeto") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Novo nome") },
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
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToRename = null }
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
            title = { Text("Excluir projeto?") },
            text = {
                Text(
                    "Todos os arquivos de '${project.name}' serão excluídos. " +
                        "Essa ação não poderá ser desfeita."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            projectManager.deleteProject(project.id)
                            refreshProjects()
                            projectToDelete = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                        }
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToDelete = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Aviso") },
            text = { Text(error) },
            confirmButton = {
                TextButton(
                    onClick = { errorMessage = null }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
