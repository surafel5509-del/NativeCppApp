package com.androidforge.studio.domain.repository

import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.model.TerminalLine
import kotlinx.coroutines.flow.Flow

/** Shell terminal bound to a project directory. */
interface TerminalRepository {
    fun observeLines(projectId: Long): Flow<List<TerminalLine>>
    suspend fun execute(project: Project, command: String)
    suspend fun cancel()
    fun currentDir(project: Project): String
    suspend fun listDir(project: Project, relativePath: String): List<FileNode>
}
