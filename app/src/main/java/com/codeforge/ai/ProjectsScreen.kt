
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

    val zipExporter = remember(context) {
        ZipExportManager(context)
    }

    val zipImporter = remember(context) {
        ZipImportManager(context)
    }

    var projects by remember {
        mutableStateOf(emptyList<CodeProject>())
    }

    var projectName by remember {
        mutableStateOf("")
    }

    var dialogAction by remember {
        mutableStateOf("")
    }

    var selectedProject by remember {
        mutableStateOf<CodeProject?>(null)
    }

    var projectToExport by remember {
        mutableStateOf<CodeProject?>(null)
    }

    var zipToImport by remember {
        mutableStateOf<Uri?>(null)
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun refreshProjects() {
        try {
            projects = projectManager.listProjects()
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Erro ao carregar projetos."
        }
    }

    LaunchedEffect(projectManager) {
        refreshProjects()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/zip"
        )
    ) { uri ->
        val project = projectToExport
        projectToExport = null

        if (uri != null && project != null) {
            scope.launch {
                busy = true

                try {
                    val result = withContext(Dispatchers.IO) {
                        zipExporter.exportProject(
                            project,
                            uri
                        )
                    }

                    result.fold(
                        onSuccess = {
                            successMessage =
                                "Projeto exportado com sucesso!"
                        },
                        onFailure = {
                            errorMessage = it.message
                                ?: "Erro ao exportar ZIP."
                        }
                    )
                } catch (e: Exception) {
                    errorMessage = e.message
                        ?: "Erro ao exportar ZIP."
                } finally {
                    busy = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            zipToImport = uri

            val fileName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                .orEmpty()

            projectName = fileName
                .take(80)
                .ifBlank { "Projeto importado" }

            dialogAction = "import"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Meus projetos")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream"
                                )
                            )
                        },
                        enabled = !busy
                    ) {
                        Text("Importar ZIP")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    projectName = ""
                    selectedProject = null
                    dialogAction = "create"
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
            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("Processando projeto...")

                Spacer(Modifier.height(12.dp))
            }

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum projeto criado. " +
                            "Toque em + ou Importar ZIP."
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

                        var menuExpanded by remember(project.id) {
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
                                        .clickable(
                                            enabled = !busy
                                        ) {
                                            onOpenProject(project)
                                        }
                                ) {
                                    Text(
                                        project.name,
                                        style = MaterialTheme
                                            .typography.titleMedium
                                    )

                                    Text(
                                        "Toque para abrir",
                                        style = MaterialTheme
                                            .typography.bodySmall
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = {
                                            menuExpanded = true
                                        },
                                        enabled = !busy
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
                                            text = { Text("Abrir") },
                                            onClick = {
                                                menuExpanded = false
                                                onOpenProject(project)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text("Exportar ZIP")
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                projectToExport = project

                                                exportLauncher.launch(
                                                    zipExporter
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
                                            onClick = {
                                                menuExpanded = false
                                                selectedProject = project
                                                projectName = project.name
                                                dialogAction = "rename"
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text("Excluir")
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                selectedProject = project
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
    }

    if (
        dialogAction == "create" ||
        dialogAction == "rename" ||
        dialogAction == "import"
    ) {
        val action = dialogAction

        AlertDialog(
            onDismissRequest = {
                dialogAction = ""
                zipToImport = null
            },
            title = {
                Text(
                    when (action) {
                        "create" -> "Novo projeto"
                        "rename" -> "Renomear projeto"
                        else -> "Importar projeto ZIP"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    if (action == "import") {
                        Text(
                            "Escolha um nome para o projeto importado."
                        )
                    }

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = {
                            projectName = it
                        },
                        label = {
                            Text("Nome do projeto")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = projectName.isNotBlank() && !busy,
                    onClick = {
                        when (action) {
                            "create" -> {
                                try {
                                    val project =
                                        projectManager.createProject(
                                            projectName
                                        )

                                    refreshProjects()
                                    dialogAction = ""
                                    onOpenProject(project)
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                        ?: "Erro ao criar projeto."
                                }
                            }

                            "rename" -> {
                                try {
                                    val project = selectedProject
                                        ?: error(
                                            "Projeto não selecionado."
                                        )

                                    projectManager.renameProject(
                                        project.id,
                                        projectName
                                    )

                                    refreshProjects()
                                    dialogAction = ""
                                    selectedProject = null
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                        ?: "Erro ao renomear."
                                }
                            }

                            "import" -> {
                                val uri = zipToImport

                                if (uri == null) {
                                    errorMessage =
                                        "Selecione um arquivo ZIP."
                                } else {
                                    val name = projectName

                                    dialogAction = ""
                                    zipToImport = null

                                    scope.launch {
                                        busy = true

                                        try {
                                            val result =
                                                zipImporter.importProject(
                                                    uri,
                                                    name
                                                )

                                            result.fold(
                                                onSuccess = {
                                                    refreshProjects()
                                                    successMessage =
                                                        "Projeto importado!"
                                                },
                                                onFailure = {
                                                    errorMessage =
                                                        it.message
                                                            ?: "Erro na importação."
                                                }
                                            )
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                                ?: "Erro na importação."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (action == "import") {
                            "Importar"
                        } else {
                            "Confirmar"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogAction = ""
                        zipToImport = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (dialogAction == "delete") {
        val project = selectedProject

        AlertDialog(
            onDismissRequest = {
                dialogAction = ""
                selectedProject = null
            },
            title = {
                Text("Excluir projeto?")
            },
            text = {
                Text(
                    "Todos os arquivos de " +
                        "'${project?.name.orEmpty()}' " +
                        "serão excluídos permanentemente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val target = project
                                ?: error(
                                    "Projeto não selecionado."
                                )

                            projectManager.deleteProject(
                                target.id
                            )

                            refreshProjects()
                            dialogAction = ""
                            selectedProject = null
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Erro ao excluir projeto."
                        }
                    }
                ) {
                    Text("Excluir")
             
