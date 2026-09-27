package com.androidforge.studio.domain.repository

import com.androidforge.studio.domain.model.BuildTask
import com.androidforge.studio.domain.model.GitCommit
import com.androidforge.studio.domain.model.GitDiff
import com.androidforge.studio.domain.model.GitStatus
import com.androidforge.studio.domain.model.Plugin
import com.androidforge.studio.domain.model.Project
import kotlinx.coroutines.flow.Flow

/** Git operations backed by JGit, scoped to a project directory. */
interface GitRepository {
    suspend fun status(project: Project): GitStatus
    suspend fun log(project: Project, limit: Int = 50): List<GitCommit>
    suspend fun diff(project: Project, filePath: String? = null): List<GitDiff>
    suspend fun init(project: Project): Boolean
    suspend fun addAll(project: Project): Boolean
    suspend fun commit(project: Project, message: String, author: String, email: String): String?
    suspend fun checkout(project: Project, ref: String): Boolean
    suspend fun clone(url: String, project: Project, username: String?, token: String?): Boolean
    suspend fun push(project: Project, remote: String, username: String?, token: String?): Boolean
    suspend fun pull(project: Project, remote: String, username: String?, token: String?): Boolean
}

/** Plugin registry: bundled assets + user-installed plugins in filesDir/plugins. */
interface PluginRepository {
    fun observePlugins(): Flow<List<Plugin>>
    suspend fun refresh()
    suspend fun setEnabled(pluginId: String, enabled: Boolean): Boolean
    suspend fun installFromBytes(fileName: String, bytes: ByteArray): Plugin?
    suspend fun uninstall(pluginId: String): Boolean
    suspend fun buildTasks(project: Project): List<BuildTask>
    suspend fun installedTemplates(): List<Plugin>
}
