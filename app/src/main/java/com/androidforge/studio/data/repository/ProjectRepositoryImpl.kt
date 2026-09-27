package com.androidforge.studio.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.androidforge.studio.data.local.dao.ProjectDao
import com.androidforge.studio.data.local.entity.ProjectEntity
import com.androidforge.studio.data.template.TemplateEngine
import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.domain.model.FileContent
import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sandboxed filesystem + Room-backed project repository.
 * All paths resolve under filesDir/projects/<id> and are canonicalized to
 * prevent traversal outside the sandbox.
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ProjectDao,
    private val prefs: SharedPreferences,
) : ProjectRepository {

    private val projectsRoot: File get() = File(context.filesDir, "projects").apply { mkdirs() }

    override fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProject(id: Long): Project? = dao.getById(id)?.toDomain()

    override suspend fun createProject(
        name: String,
        packageName: String,
        templateId: String,
    ): Project = withContext(Dispatchers.IO) {
        var finalName = name
        var counter = 1
        while (dao.getByName(finalName) != null) {
            counter++
            finalName = "$name ($counter)"
        }
        val now = System.currentTimeMillis()
        val rootPath = java.util.UUID.randomUUID().toString().take(12)
        val entity = ProjectEntity(
            name = finalName,
            packageName = packageName,
            templateId = templateId,
            rootPath = rootPath,
            createdAt = now,
            updatedAt = now,
        )
        val id = dao.insert(entity)

        val projectDir = File(projectsRoot, rootPath)
        projectDir.deleteRecursively()
        projectDir.mkdirs()

        val files = TemplateEngine.generate(templateId, finalName, packageName)
        for ((rel, content) in files) {
            val target = safeResolve(projectDir, rel)
            target.parentFile?.mkdirs()
            target.writeText(content)
        }
        dao.touch(id, System.currentTimeMillis())
        entity.copy(id = id).toDomain()
    }

    override suspend fun renameProject(id: Long, newName: String) {
        dao.rename(id, newName.trim(), System.currentTimeMillis())
    }

    override suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) {
        dao.delete(id)
        File(projectsRoot, id.toString()).deleteRecursively()
        prefs.edit().remove("last_open_project").apply()
    }

    override suspend fun toggleFavorite(id: Long) {
        val current = dao.getById(id) ?: return
        dao.setFavorite(id, !current.isFavorite, System.currentTimeMillis())
    }

    override suspend fun importProjectFromZip(displayName: String, bytes: ByteArray): Project =
        withContext(Dispatchers.IO) {
            val rootPath = java.util.UUID.randomUUID().toString().take(12)
            val entity = ProjectEntity(
                name = displayName.removeSuffix(".zip").removeSuffix(".afx"),
                packageName = "com.example.imported",
                templateId = "imported",
                rootPath = rootPath,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val id = dao.insert(entity)
            val projectDir = File(projectsRoot, rootPath)
            projectDir.deleteRecursively()
            projectDir.mkdirs()
            unzip(bytes, projectDir)
            dao.touch(id, System.currentTimeMillis())
            entity.copy(id = id).toDomain()
        }

    override suspend fun exportProjectToZip(id: Long): File? = withContext(Dispatchers.IO) {
        val project = dao.getById(id) ?: return@withContext null
        val dir = File(projectsRoot, project.rootPath)
        if (!dir.isDirectory) return@withContext null
        val outDir = File(context.filesDir, "artifacts").apply { mkdirs() }
        val outFile = File(outDir, "${project.name}.zip")
        ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
            zipDirectory(dir, dir.name, zip)
        }
        outFile
    }

    override fun projectDir(project: Project): File = File(projectsRoot, project.rootPath)

    override suspend fun getFileTree(project: Project): FileNode = withContext(Dispatchers.IO) {
        val root = projectDir(project)
        buildTree(root, root, project.name)
    }

    override suspend fun readFile(project: Project, relativePath: String, maxBytes: Long): FileContent =
        withContext(Dispatchers.IO) {
            val file = safeResolve(projectDir(project), relativePath)
            if (!file.isFile) throw IllegalArgumentException("File not found: $relativePath")
            val total = file.length()
            if (total <= maxBytes) {
                FileContent(relativePath, file.readText(), false, total)
            } else {
                val bytes = file.inputStream().use { input ->
                    val buf = ByteArray(maxBytes.toInt())
                    var read = 0
                    while (read < buf.size) {
                        val n = input.read(buf, read, buf.size - read)
                        if (n < 0) break
                        read += n
                    }
                    buf.copyOf(read)
                }
                FileContent(relativePath, String(bytes, Charsets.UTF_8), true, total)
            }
        }

    override suspend fun writeFile(project: Project, relativePath: String, content: String): Boolean =
        withContext(Dispatchers.IO) {
            val target = safeResolve(projectDir(project), relativePath)
            target.parentFile?.mkdirs()
            target.writeText(content)
            dao.touch(project.id, System.currentTimeMillis())
            true
        }

    override suspend fun createFile(project: Project, relativePath: String, content: String): Boolean =
        withContext(Dispatchers.IO) {
            val target = safeResolve(projectDir(project), relativePath)
            if (target.exists()) return@withContext false
            target.parentFile?.mkdirs()
            target.writeText(content)
            dao.touch(project.id, System.currentTimeMillis())
            true
        }

    override suspend fun createDirectory(project: Project, relativePath: String): Boolean =
        withContext(Dispatchers.IO) {
            val target = safeResolve(projectDir(project), relativePath)
            if (target.exists()) return@withContext false
            target.mkdirs()
            true
        }

    override suspend fun deletePath(project: Project, relativePath: String): Boolean =
        withContext(Dispatchers.IO) {
            val target = safeResolve(projectDir(project), relativePath)
            if (!target.exists()) return@withContext false
            if (target == projectDir(project)) return@withContext false // never delete project root
            target.deleteRecursively().also {
                if (it) dao.touch(project.id, System.currentTimeMillis())
            }
        }

    override suspend fun renamePath(project: Project, fromRelative: String, toRelative: String): Boolean =
        withContext(Dispatchers.IO) {
            val from = safeResolve(projectDir(project), fromRelative)
            val to = safeResolve(projectDir(project), toRelative)
            if (!from.exists() || to.exists()) return@withContext false
            to.parentFile?.mkdirs()
            from.renameTo(to).also { if (it) dao.touch(project.id, System.currentTimeMillis()) }
        }

    override fun languageFor(fileName: String): CodeLanguage = CodeLanguage.forFile(fileName)

    // ------------------------------------------------------------------ util

    private fun safeResolve(root: File, relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, relativePath).canonicalFile
        if (!target.path.startsWith(canonicalRoot.path + File.separator) && target != canonicalRoot) {
            throw SecurityException("Path escapes project sandbox: $relativePath")
        }
        return target
    }

    private fun buildTree(current: File, root: File, name: String): FileNode {
        val children = (current.listFiles() ?: emptyArray())
            .filter { !it.name.startsWith(".") || it.name == ".gitignore" }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { buildTree(it, root, it.name) }
        return FileNode(
            name = name,
            path = current.path,
            relativePath = current.path.removePrefix(root.path).removePrefix("/"),
            isDirectory = current.isDirectory,
            sizeBytes = if (current.isFile) current.length() else 0,
            children = children,
        )
    }

    private fun zipDirectory(dir: File, prefix: String, zip: ZipOutputStream) {
        dir.walkTopDown().forEach { file ->
            val rel = file.relativeTo(dir).path
            val entryName = if (rel.isEmpty()) "$prefix/" else "$prefix/$rel"
            if (file.isDirectory) {
                if (entryName.endsWith("/").not()) zip.putNextEntry(ZipEntry("$entryName/"))
                else zip.putNextEntry(ZipEntry(entryName))
                zip.closeEntry()
            } else {
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun unzip(bytes: ByteArray, targetDir: File) {
        ZipInputStream(bytes.inputStream().buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val out = safeResolve(targetDir, entry.name)
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    ByteArrayOutputStream().use { bos -> zip.copyTo(bos); out.writeBytes(bos.toByteArray()) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id,
        name = name,
        packageName = packageName,
        templateId = templateId,
        rootPath = rootPath,
        isFavorite = isFavorite,
        isEncrypted = isEncrypted,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
