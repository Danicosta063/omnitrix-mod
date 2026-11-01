
package com.codeforge.ai

import android.content.Context
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExportManager(
    context: Context
) {
    private val appContext = context.applicationContext

    private val projectManager = ProjectManager(appContext)

    fun suggestFileName(project: CodeProject): String {
        val safeName = project.name
            .trim()
            .replace(
                Regex("[^a-zA-Z0-9._-]"),
                "_"
            )
            .trim('.', '_')
            .take(80)
            .ifBlank { "CodeForge_Project" }

        return "$safeName.zip"
    }

    fun exportProject(
        project: CodeProject,
        destinationUri: Uri
    ): Result<Unit> {
        return runCatching {
            val currentProject = projectManager
                .listProjects()
                .firstOrNull { it.id == project.id }
                ?: throw IOException(
                    "Projeto não encontrado."
                )

            val root = currentProject.directory.canonicalFile

            require(root.isDirectory) {
                "A pasta do projeto não existe."
            }

            val files = projectManager.listFiles(
                currentProject.id
            )

            val resolver = appContext.contentResolver

            val outputStream = resolver.openOutputStream(
                destinationUri,
                "w"
            ) ?: throw IOException(
                "Não foi possível abrir o destino do ZIP."
            )

            outputStream.use { output ->
                ZipOutputStream(
                    BufferedOutputStream(output)
                ).use { zip ->

                    val buffer = ByteArray(8192)

                    files.forEach { entry ->
                        val relativePath = entry.path
                            .replace('\\', '/')

                        if (
                            relativePath == ".codeforge-name" ||
                            relativePath.startsWith(
                                ".codeforge-name/"
                            )
                        ) {
                            return@forEach
                        }

                        val source = File(
                            root,
                            relativePath
                        ).canonicalFile

                        require(
                            source.toPath()
                                .startsWith(root.toPath()) &&
                                source != root
                        ) {
                            "Caminho inválido no projeto."
                        }

                        val zipName = if (entry.isDirectory) {
                            relativePath.trimEnd('/') + "/"
                        } else {
                            relativePath
                        }

                        val zipEntry = ZipEntry(zipName)

                        zip.putNextEntry(zipEntry)

                        if (!entry.isDirectory) {
                            require(source.isFile) {
                                "Arquivo não encontrado: $relativePath"
                            }

                            source.inputStream().use { input ->
                                while (true) {
                                    val count = input.read(buffer)

                                    if (count == -1) break

                                    zip.write(
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
}
