package com.androidforge.studio.domain.repository

import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.domain.model.FileContent
import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.Project
import kotlinx.coroutines.flow.Flow
import java.io.File

/** CRUD + filesystem access for projects stored in the app sandbox. */
interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    suspend fun getProject(id: Long): Project?
    suspend fun createProject(name: String, packageName: String, templateId: String): Project
    suspend fun renameProject(id: Long, newName: String)
    suspend fun deleteProject(id: Long)
    suspend fun toggleFavorite(id: Long)
    suspend fun importProjectFromZip(displayName: String, bytes: ByteArray): Project
    suspend fun exportProjectToZip(id: Long): File?

    /** Directory (absolute path) of a project. */
    fun projectDir(project: Project): File

    // ---- file tree ----
    suspend fun getFileTree(project: Project): FileNode
    suspend fun readFile(project: Project, relativePath: String, maxBytes: Long = 512L * 1024): FileContent
    suspend fun writeFile(project: Project, relativePath: String, content: String): Boolean
    suspend fun createFile(project: Project, relativePath: String, content: String = ""): Boolean
    suspend fun createDirectory(project: Project, relativePath: String): Boolean
    suspend fun deletePath(project: Project, relativePath: String): Boolean
    suspend fun renamePath(project: Project, fromRelative: String, toRelative: String): Boolean

    /** Well-known language for a file name. */
    fun languageFor(fileName: String): CodeLanguage
}
