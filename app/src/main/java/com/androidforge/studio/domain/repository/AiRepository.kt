package com.androidforge.studio.domain.repository

import com.androidforge.studio.domain.model.AiContext
import com.androidforge.studio.domain.model.AiSettings
import com.androidforge.studio.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/** AI assistant: remote providers (OpenAI/Gemini) with an offline local fallback. */
interface AiRepository {
    fun observeChat(projectId: Long?): Flow<List<ChatMessage>>
    suspend fun sendPrompt(
        projectId: Long?,
        prompt: String,
        context: AiContext,
    ): ChatMessage
    suspend fun clearChat(projectId: Long?)
    fun settings(): Flow<AiSettings>
    suspend fun updateSettings(settings: AiSettings)
}
