
package com.codeforge.ai

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.UUID

data class CodeProject(
    val id: String,
    val name: String,
    val directory: File
)

data class ProjectFile(
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)

class ProjectManager(context: Context) {

    private val root = File(
        context.applicationContext.filesDir,
        "codeforge_projects"
    ).canonicalFile

    init {
        if (!root.exists() && !root.mkdirs()) {
            throw IOException("Não foi possível criar a pasta de projetos.")
        }
    }

    private fun validateName(name: String): String {
        val clean = name.trim()

        require(
            clean.isNotEmpty() &&
                clean != "." &&
                clean != ".." &&
                clean.length <= 100 &&
                clean.none {
                    it == '/' ||
                        it == '\\' ||
                        it.code < 32
                }
        ) {
            "Nome inválido."
        }

        return clean
    }

    private fun getProjectDirectory(id: String): File {
        require(
            Regex("[a-f0-9-]{36}").matches(id)
        ) {
            "Identificador de projeto inválido."
        }

        val directory = File(root, id).canonicalFile

        require(directory.parentFile == root) {
            "Caminho de projeto inválido."
        }

        return directory
    }

    private fun resolveProjectPath(
        projectId: String,
        relativePath: String
    ): File {
        val directory = getProjectDirectory(projectId)

        require(directory.isDirectory) {
            "Projeto não encontrado."
        }

        require(relativePath.isNotBlank()) {
            "O caminho não pode estar vazio."
        }

        val target = File(
            directory,
            relativePath
        ).canonicalFile

        require(
            target != directory &&
                target.toPath().startsWith(directory.toPath())
        ) {
            "Acesso fora do projeto não permitido."
        }

        return target
    }

    fun createProject(name: String): CodeProject {
        val cleanName = validateName(name)
        val id = UUID.randomUUID().toString()
        val directory = getProjectDirectory(id)

        if (!directory.mkdirs()) {
            throw IOException("Não foi possível criar o projeto.")
        }

        try {
            File(directory, ".codeforge-name")
                .writeText(cleanName, Charsets.UTF_8)
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }

        return CodeProject(id, cleanName, directory)
    }

    fun listProjects(): List<CodeProject> {
        return root.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { directory ->
                try {
                    val safeDirectory = getProjectDirectory(
                        directory.name
                    )

                    if (safeDirectory != directory.canonicalFile) {
                        return@mapNotNull null
                    }

                    val nameFile = File(
                        safeDirectory,
                        ".codeforge-name"
                    )

                    if (!nameFile.isFile) {
                        return@mapNotNull null
                    }

                    CodeProject(
                        id = directory.name,
                        name = nameFile.readText(Charsets.UTF_8),
                        directory = safeDirectory
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.name.lowercase() }
    }

    fun renameProject(
        projectId: String,
        newName: String
    ) {
        val directory = getProjectDirectory(projectId)

        require(directory.isDirectory) {
            "Projeto não encontrado."
        }

        File(directory, ".codeforge-name").writeText(
            validateName(newName),
            Charsets.UTF_8
        )
    }

    fun deleteProject(projectId: String) {
        val directory = getProjectDirectory(projectId)

        require(directory.isDirectory) {
            "Projeto não encontrado."
        }

        if (!directory.deleteRecursively()) {
            throw IOException("Não foi possível excluir o projeto.")
        }
    }

    fun createFolder(
        projectId: String,
        relativePath: String
    ) {
        val folder = resolveProjectPath(
            projectId,
            relativePath
        )

        if (!folder.isDirectory && !folder.mkdirs()) {
            throw IOException("Não foi possível criar a pasta.")
        }
    }

    fun saveFile(
        projectId: String,
        relativePath: String,
        content: String
    ) {
        val file = resolveProjectPath(
            projectId,
            relativePath
        )

        require(file.name != ".codeforge-name") {
            "Nome reservado pelo aplicativo."
        }

        val parent = file.parentFile
            ?: throw IOException("Pasta inválida.")

        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Não foi possível criar a pasta.")
        }

        file.writeText(content, Charsets.UTF_8)
    }

    fun readFile(
        projectId: String,
        relativePath: String
    ): String {
        val file = resolveProjectPath(
            projectId,
            relativePath
        )

        require(file.isFile) {
            "Arquivo não encontrado."
        }

        return file.readText(Charsets.UTF_8)
    }

    fun deleteFile(
        projectId: String,
        relativePath: String
    ) {
        val file = resolveProjectPath(
            projectId,
            relativePath
        )

        require(file.name != ".codeforge-name") {
            "Arquivo reservado."
        }

        require(file.exists()) {
            "Arquivo não encontrado."
        }

        val deleted = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }

        if (!deleted) {
            throw IOException("Não foi possível excluir.")
        }
    }

    fun renameFile(
        projectId: String,
        oldPath: String,
        newPath: String
    ) {
        val source = resolveProjectPath(
            projectId,
            oldPath
        )

        val destination = resolveProjectPath(
            projectId,
            newPath
        )

        require(source.exists()) {
            "Arquivo original não encontrado."
        }

        require(
            source.name != ".codeforge-name" &&
                destination.name != ".codeforge-name"
        ) {
            "Nome reservado."
        }

        require(!destination.exists()) {
            "Já existe um arquivo com esse nome."
        }

        require(
            !destination.toPath().startsWith(source.toPath())
        ) {
            "Não é possível mover uma pasta para dentro dela mesma."
        }

        val parent = destination.parentFile
            ?: throw IOException("Destino inválido.")

        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Não foi possível criar a pasta.")
        }

        if (!source.renameTo(destination)) {
            throw IOException("Não foi possível renomear.")
        }
    }

    fun listFiles(projectId: String): List<ProjectFile> {
        val directory = getProjectDirectory(projectId)

        require(directory.isDirectory) {
            "Projeto não encontrado."
        }

        return directory.walkTopDown()
            .filter {
                it != directory &&
                    it.name != ".codeforge-name"
            }
            .map {
                ProjectFile(
                    path = it.relativeTo(directory)
                        .invariantSeparatorsPath,
                    isDirectory = it.isDirectory,
                    size = if (it.isFile) it.length() else 0L
                )
            }
            .sortedWith(
                compareBy<ProjectFile> { !it.isDirectory }
                    .thenBy { it.path.lowercase() }
            )
            .toList()
    }
}
