package com.androidforge.studio.domain.usecase

import com.androidforge.studio.domain.model.AiContext
import com.androidforge.studio.domain.model.ChatMessage
import com.androidforge.studio.domain.repository.AiRepository
import javax.inject.Inject

/** Sends a prompt to the configured AI provider with project context attached. */
class SendChatMessageUseCase @Inject constructor(
    private val ai: AiRepository,
) {
    suspend operator fun invoke(projectId: Long?, prompt: String, context: AiContext): ChatMessage {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
        return ai.sendPrompt(projectId, prompt.trim(), context)
    }
}
