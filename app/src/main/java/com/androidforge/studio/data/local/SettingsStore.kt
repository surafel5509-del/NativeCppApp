package com.androidforge.studio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Typed DataStore wrapper for user preferences (non-secret settings only). */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONE_HAND = booleanPreferencesKey("one_hand_mode")
        val EDITOR_FONT = intPreferencesKey("editor_font_size")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val OPENAI_KEY = stringPreferencesKey("openai_key")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val CLOUD_TOKEN = stringPreferencesKey("cloud_token")
        val SANDBOX = booleanPreferencesKey("sandbox_scopes")
        val LAST_PROJECT = intPreferencesKey("last_project_id")
    }

    val themeMode: Flow<String> = context.settingsStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    suspend fun setThemeMode(v: String) = context.settingsStore.edit { it[Keys.THEME_MODE] = v }

    val oneHandMode: Flow<Boolean> = context.settingsStore.data.map { it[Keys.ONE_HAND] ?: false }
    suspend fun setOneHandMode(v: Boolean) = context.settingsStore.edit { it[Keys.ONE_HAND] = v }

    val editorFontSize: Flow<Int> = context.settingsStore.data.map { it[Keys.EDITOR_FONT] ?: 14 }
    suspend fun setEditorFontSize(v: Int) = context.settingsStore.edit { it[Keys.EDITOR_FONT] = v }

    val aiProvider: Flow<String> = context.settingsStore.data.map { it[Keys.AI_PROVIDER] ?: "LOCAL" }
    suspend fun setAiProvider(v: String) = context.settingsStore.edit { it[Keys.AI_PROVIDER] = v }

    /** API keys live here but are sealed with AES-GCM before hitting disk. */
    val openAiKeyEnc: Flow<String> = context.settingsStore.data.map { it[Keys.OPENAI_KEY] ?: "" }
    suspend fun setOpenAiKeyEnc(v: String) = context.settingsStore.edit { it[Keys.OPENAI_KEY] = v }

    val geminiKeyEnc: Flow<String> = context.settingsStore.data.map { it[Keys.GEMINI_KEY] ?: "" }
    suspend fun setGeminiKeyEnc(v: String) = context.settingsStore.edit { it[Keys.GEMINI_KEY] = v }

    val cloudTokenEnc: Flow<String> = context.settingsStore.data.map { it[Keys.CLOUD_TOKEN] ?: "" }
    suspend fun setCloudTokenEnc(v: String) = context.settingsStore.edit { it[Keys.CLOUD_TOKEN] = v }

    val sandboxScopes: Flow<Boolean> = context.settingsStore.data.map { it[Keys.SANDBOX] ?: true }
    suspend fun setSandboxScopes(v: Boolean) = context.settingsStore.edit { it[Keys.SANDBOX] = v }

    val lastProjectId: Flow<Int> = context.settingsStore.data.map { it[Keys.LAST_PROJECT] ?: -1 }
    suspend fun setLastProjectId(v: Int) = context.settingsStore.edit { it[Keys.LAST_PROJECT] = v }
}
