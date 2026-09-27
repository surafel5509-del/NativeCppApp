package com.androidforge.studio.domain.repository

import com.androidforge.studio.domain.model.AiProvider
import com.androidforge.studio.domain.model.Breakpoint
import kotlinx.coroutines.flow.Flow

/** User preferences persisted with DataStore (plus encrypted secrets). */
interface SettingsRepository {
    fun themeMode(): Flow<String>            // "system" | "light" | "dark"
    suspend fun setThemeMode(mode: String)

    fun oneHandMode(): Flow<Boolean>
    suspend fun setOneHandMode(enabled: Boolean)

    fun editorFontSize(): Flow<Int>
    suspend fun setEditorFontSize(sp: Int)

    fun aiProvider(): Flow<AiProvider>
    suspend fun setAiProvider(provider: AiProvider)

    fun openAiApiKey(): Flow<String>
    suspend fun setOpenAiApiKey(key: String)

    fun geminiApiKey(): Flow<String>
    suspend fun setGeminiApiKey(key: String)

    fun cloudToken(): Flow<String>
    suspend fun setCloudToken(token: String)

    fun sandboxScopes(): Flow<Boolean>       // restrict shell to project dir
    suspend fun setSandboxScopes(enabled: Boolean)

    // ---- breakpoints ----
    fun observeBreakpoints(projectId: Long): Flow<List<Breakpoint>>
    suspend fun toggleBreakpoint(projectId: Long, filePath: String, line: Int)
    suspend fun clearBreakpoints(projectId: Long)
}
