package com.androidforge.studio.data.plugin

import android.content.Context
import com.androidforge.studio.data.local.dao.PluginDao
import com.androidforge.studio.data.local.entity.PluginEntity
import com.androidforge.studio.domain.model.BuildTask
import com.androidforge.studio.domain.model.Plugin
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.PluginRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plugin registry.
 *
 * Sources, merged on [refresh]:
 *  1. bundled manifests in assets/plugins/*.json
 *  2. user-installed manifests in filesDir/plugins/*.json
 *  3. plugin zips installed through [installFromBytes]
 *
 * Manifest schema (plugin.json):
 * ```json
 * { "id": "...", "name": "...", "version": "1.0.0", "description": "...",
 *   "author": "...", "type": "THEME|TEMPLATE|BUILD_TASK|MARKETPLACE",
 *   "entry": "theme-dark.json" }
 * ```
 * Enabled flags are persisted in Room so state survives restarts.
 */
@Singleton
class PluginRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PluginDao,
) : PluginRepository {

    private val pluginsDir: File get() = File(context.filesDir, "plugins").apply { mkdirs() }

    override fun observePlugins(): Flow<List<Plugin>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        // 1) bundled
        val bundled = context.assets.list("plugins").orEmpty()
            .filter { it.endsWith(".json") }
            .mapNotNull { name ->
                context.assets.open("plugins/$name").use { parse(it.reader().readText(), "asset:plugins/$name") }
            }

        // 2) installed
        val installed = pluginsDir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { f -> f.readText().let { parse(it, f.absolutePath) } }

        // 3) upsert all into Room, preserving enabled flags for known ids
        val existing = dao.all().associateBy { it.pluginId }
        for (p in bundled + installed) {
            dao.upsert(
                PluginEntity(
                    pluginId = p.id,
                    name = p.name,
                    version = p.version,
                    description = p.description,
                    author = p.author,
                    type = p.type.name,
                    enabled = existing[p.id]?.enabled ?: false,
                    entry = p.entry,
                    sourcePath = p.sourcePath,
                )
            )
        }
        // drop rows whose files disappeared
        val knownIds = (bundled + installed).map { it.id }.toSet()
        for (row in dao.all()) {
            if (row.pluginId !in knownIds) dao.delete(row.pluginId)
        }
    }

    override suspend fun setEnabled(pluginId: String, enabled: Boolean): Boolean {
        val all = dao.all()
        if (all.none { it.pluginId == pluginId }) return false
        dao.setEnabled(pluginId, enabled)
        return true
    }

    override suspend fun installFromBytes(fileName: String, bytes: ByteArray): Plugin? =
        withContext(Dispatchers.IO) {
            try {
                if (fileName.endsWith(".json")) {
                    val text = String(bytes, Charsets.UTF_8)
                    val plugin = parse(text, fileName) ?: return@withContext null
                    File(pluginsDir, "${plugin.id}.json").writeText(text)
                    refresh()
                    plugin
                } else if (fileName.endsWith(".zip")) {
                    var manifest: Plugin? = null
                    ZipInputStream(bytes.inputStream()).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val out = File(pluginsDir, entry.name.substringAfterLast('/'))
                                out.outputStream().use { zip.copyTo(it) }
                                if (entry.name.endsWith("plugin.json")) {
                                    manifest = parse(out.readText(), out.absolutePath)
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                    refresh()
                    manifest
                } else null
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun uninstall(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        dao.delete(pluginId)
        File(pluginsDir, "$pluginId.json").delete()
        true
    }

    override suspend fun buildTasks(project: Project): List<BuildTask> {
        val all = dao.all().filter { it.enabled }
        val tasks = mutableListOf<BuildTask>()
        for (row in all) {
            if (row.type != Plugin.PluginType.BUILD_TASK.name) continue
            if (row.entry.isBlank()) continue
            val taskFile = resolveEntry(row.sourcePath, row.entry) ?: continue
            if (!taskFile.isFile) continue
            // Entry file contains lines: name=command
            for (line in taskFile.readText().lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val name = trimmed.substringBefore('=').trim()
                val cmd = trimmed.substringAfter('=').trim()
                if (name.isNotEmpty() && cmd.isNotEmpty()) {
                    tasks += BuildTask(
                        id = "${row.pluginId}:$name",
                        name = name,
                        command = cmd,
                        pluginId = row.pluginId,
                    )
                }
            }
        }
        return tasks
    }

    override suspend fun installedTemplates(): List<Plugin> =
        dao.all().filter { it.type == Plugin.PluginType.TEMPLATE.name }.map { it.toDomain() }

    // ------------------------------------------------------------- helpers

    private fun resolveEntry(sourcePath: String, entry: String): File? = when {
        sourcePath.startsWith("asset:") -> null // asset entries handled separately
        else -> {
            val base = File(sourcePath).parentFile
            base?.let { File(it, entry) }
        }
    }

    private fun parse(json: String, source: String): Plugin? = try {
        val o = JSONObject(json)
        Plugin(
            id = o.getString("id"),
            name = o.getString("name"),
            version = o.optString("version", "1.0.0"),
            description = o.optString("description", ""),
            author = o.optString("author", ""),
            type = runCatching { Plugin.PluginType.valueOf(o.getString("type")) }
                .getOrDefault(Plugin.PluginType.MARKETPLACE),
            entry = o.optString("entry", ""),
            sourcePath = source,
        )
    } catch (e: Exception) {
        null
    }

    private fun PluginEntity.toDomain() = Plugin(
        id = pluginId,
        name = name,
        version = version,
        description = description,
        author = author,
        type = runCatching { Plugin.PluginType.valueOf(type) }.getOrDefault(Plugin.PluginType.MARKETPLACE),
        enabled = enabled,
        entry = entry,
        sourcePath = sourcePath,
    )
}
