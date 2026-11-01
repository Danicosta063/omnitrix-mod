
package com.codeforge.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipImportManager(context: Context) {

    private val appContext = context.applicationContext
    private val projectManager = ProjectManager(appContext)

    companion object {
        private const val MAX_FILES = 5000
        private const val MAX_TOTAL_SIZE = 200L * 1024 * 1024
        private const val MAX_FILE_SIZE = 50L * 1024 * 1024
        private const val MAX_PATH_LENGTH = 500
    }

    suspend fun importProject(
        uri: Uri,
        projectName: String
    ): Result<CodeProject> = withContext(Dispatchers.IO) {
        runCatching {
            val name = projectName.trim()

            require(name.isNotEmpty()) {
                "Informe um nome para o projeto."
            }

            val project = projectManager.createProject(name)

            try {
                extractZip(uri, project)
                project
            } catch (error: Exception) {
                try {
                    projectManager.deleteProject(project.id)
                } catch (cleanupError: Exception) {
                    error.addSuppressed(cleanupError)
                }
                throw error
            }
        }
    }

    private fun extractZip(
        uri: Uri,
        project: CodeProject
    ) {
        val inputStream = appContext.contentResolver
            .openInputStream(uri)
            ?: throw IOException(
                "Não foi possível abrir o arquivo ZIP."
            )

        val root = project.directory.canonicalFile
        var fileCount = 0
        var totalBytes = 0L

        val seenPaths = mutableSetOf<String>()

        inputStream.use { source ->
            ZipInputStream(
                BufferedInputStream(source)
            ).use { zip ->

                val buffer = ByteArray(8192)

                while (true) {
                    val entry: ZipEntry =
                        zip.nextEntry ?: break

                    val rawPath = entry.name

                    require(rawPath.isNotBlank()) {
                        "O ZIP contém um caminho vazio."
                    }

                    require(
                        rawPath.length <= MAX_PATH_LENGTH
                    ) {
                        "O ZIP contém um caminho muito longo."
                    }

                    require(
                        !rawPath.startsWith("/") &&
                            !rawPath.startsWith("\\") &&
                            '\\' !in rawPath &&
                            ':' !in rawPath &&
                            rawPath.none { it.code < 32 }
                    ) {
                        "O ZIP contém um caminho inválido."
                    }

                    val parts = rawPath
                        .trimEnd('/')
                        .split('/')

                    require(
                        parts.all {
                            it.isNotEmpty() &&
                                it != "." &&
                                it != ".."
                        }
                    ) {
                        "O ZIP contém um caminho inseguro."
                    }

                    require(
                        parts.none {
                            it == ".codeforge-name"
                        }
                    ) {
                        "O ZIP contém um nome reservado."
                    }

                    val relativePath = parts.joinToString("/")

                    require(
                        seenPaths.add(relativePath)
                    ) {
                        "O ZIP contém entradas duplicadas."
                    }

                    val destination = java.io.File(
                        root,
                        relativePath
                    ).canonicalFile

                    require(
                        destination != root &&
                            destination.toPath()
                                .startsWith(root.toPath())
                    ) {
                        "O ZIP tenta acessar arquivos fora do projeto."
                    }

                    fileCount++

                    require(fileCount <= MAX_FILES) {
                        "O ZIP contém arquivos demais."
                    }

                    if (entry.isDirectory) {
                        if (
                            !destination.isDirectory &&
                            !destination.mkdirs()
                        ) {
                            throw IOException(
                                "Não foi possível criar a pasta."
                            )
                        }
                    } else {
                        require(!destination.isDirectory) {
                            "Um arquivo tem o mesmo nome de uma pasta."
                        }

                        val parent = destination.parentFile
                            ?: throw IOException(
                                "Pasta de destino inválida."
                            )

                        if (
                            !parent.isDirectory &&
                            !parent.mkdirs()
                        ) {
                            throw IOException(
                                "Não foi possível criar a pasta de destino."
                            )
                        }

                        var fileBytes = 0L

                        destination.outputStream().use { output ->
                            while (true) {
                                val count = zip.read(buffer)

                                if (count == -1) break
                                if (count == 0) continue

                                fileBytes += count
                                totalBytes += count

                                require(
                                    fileBytes <= MAX_FILE_SIZE
                                ) {
                                    "Um arquivo ultrapassa o limite de 50 MB."
                                }

                                require(
                                    totalBytes <= MAX_TOTAL_SIZE
                                ) {
                                    "O projeto ultrapassa o limite de 200 MB."
                                }

                                output.write(
                                    buffer,
                                    0,
                                    count
                                )
                            }
                        }
                    }

                    zip.closeEntry()
                }
            }
        }
    }
}
