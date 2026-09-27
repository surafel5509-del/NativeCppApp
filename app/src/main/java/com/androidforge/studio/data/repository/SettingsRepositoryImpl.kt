package com.androidforge.studio.data.repository

import com.androidforge.studio.data.local.SettingsStore
import com.androidforge.studio.data.local.dao.BreakpointDao
import com.androidforge.studio.data.local.entity.BreakpointEntity
import com.androidforge.studio.data.security.SecureVault
import com.androidforge.studio.domain.model.AiProvider
import com.androidforge.studio.domain.model.Breakpoint
import com.androidforge.studio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences backed by DataStore. Secret values (API keys, tokens) are sealed
 * with [SecureVault] before hitting disk and transparently unsealed on read,
 * so callers always work with plaintext.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsStore,
    private val vault: SecureVault,
    private val breakpointDao: BreakpointDao,
) : SettingsRepository {

    override fun themeMode(): Flow<String> = store.themeMode
    override suspend fun setThemeMode(mode: String) = store.setThemeMode(mode)

    override fun oneHandMode(): Flow<Boolean> = store.oneHandMode
    override suspend fun setOneHandMode(enabled: Boolean) = store.setOneHandMode(enabled)

    override fun editorFontSize(): Flow<Int> = store.editorFontSize
    override suspend fun setEditorFontSize(sp: Int) = store.setEditorFontSize(sp)

    override fun aiProvider(): Flow<AiProvider> = store.aiProvider.map {
        runCatching { AiProvider.valueOf(it) }.getOrDefault(AiProvider.LOCAL)
    }

    override suspend fun setAiProvider(provider: AiProvider) = store.setAiProvider(provider.name)

    override fun openAiApiKey(): Flow<String> = store.openAiKeyEnc.map { vault.unseal(it) }
    override suspend fun setOpenAiApiKey(key: String) = store.setOpenAiKeyEnc(vault.seal(key))

    override fun geminiApiKey(): Flow<String> = store.geminiKeyEnc.map { vault.unseal(it) }
    override suspend fun setGeminiApiKey(key: String) = store.setGeminiKeyEnc(vault.seal(key))

    override fun cloudToken(): Flow<String> = store.cloudTokenEnc.map { vault.unseal(it) }
    override suspend fun setCloudToken(token: String) = store.setCloudTokenEnc(vault.seal(token))

    override fun sandboxScopes(): Flow<Boolean> = store.sandboxScopes
    override suspend fun setSandboxScopes(enabled: Boolean) = store.setSandboxScopes(enabled)

    override fun observeBreakpoints(projectId: Long): Flow<List<Breakpoint>> =
        breakpointDao.observeForProject(projectId).map { list ->
            list.map { Breakpoint(it.id, it.projectId, it.filePath, it.line, it.enabled, it.condition) }
        }

    override suspend fun toggleBreakpoint(projectId: Long, filePath: String, line: Int) {
        val existing = breakpointDao.get(projectId, filePath, line)
        if (existing != null) breakpointDao.delete(projectId, filePath, line)
        else breakpointDao.insert(
            BreakpointEntity(
                projectId = projectId,
                filePath = filePath,
                line = line,
                enabled = true,
                condition = "",
            )
        )
    }

    override suspend fun clearBreakpoints(projectId: Long) = breakpointDao.clearForProject(projectId)
}
