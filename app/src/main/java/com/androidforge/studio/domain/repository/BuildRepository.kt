package com.androidforge.studio.domain.repository

import com.androidforge.studio.domain.model.BuildSession
import com.androidforge.studio.domain.model.BuildTarget
import kotlinx.coroutines.flow.Flow

/** Executes builds (on-device pipeline / Gradle runner / cloud) and reports progress. */
interface BuildRepository {
    /** Live progress of the current build; emits log lines and stage changes. */
    fun observeBuild(): Flow<BuildSession>

    suspend fun startBuild(projectId: Long, target: BuildTarget, useCloud: Boolean = false): Long
    fun cancelBuild()
    suspend fun syncProject(projectId: Long): Boolean
    suspend fun installApk(path: String): Boolean

    suspend fun recentSessions(projectId: Long, limit: Int = 20): List<BuildSession>
    suspend fun clearHistory(projectId: Long)
}
