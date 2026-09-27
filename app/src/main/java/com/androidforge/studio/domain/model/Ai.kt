package com.androidforge.studio.domain.model

enum class ChatRole { USER, ASSISTANT, SYSTEM }

/** A single chat exchange with the AI assistant. */
data class ChatMessage(
    val id: Long = 0,
    val projectId: Long? = null,
    val role: ChatRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class AiProvider { OPENAI, GEMINI, LOCAL }

data class AiSettings(
    val provider: AiProvider = AiProvider.LOCAL,
    val model: String = "gpt-4o-mini",
    val apiKey: String = "",   // stored encrypted at rest
    val baseUrl: String = "",  // optional OpenAI-compatible override
)

/** Which extra context to attach when sending a prompt. */
data class AiContext(
    val includeProjectTree: Boolean = true,
    val includeOpenFile: Boolean = true,
    val includeBuildErrors: Boolean = false,
    val openFilePath: String? = null,
    val openFileContent: String? = null,
)
