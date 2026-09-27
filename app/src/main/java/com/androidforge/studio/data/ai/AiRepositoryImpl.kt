package com.androidforge.studio.data.ai

import com.androidforge.studio.data.local.dao.ChatMessageDao
import com.androidforge.studio.data.local.entity.ChatMessageEntity
import com.androidforge.studio.domain.model.AiContext
import com.androidforge.studio.domain.model.AiProvider
import com.androidforge.studio.domain.model.AiSettings
import com.androidforge.studio.domain.model.ChatMessage
import com.androidforge.studio.domain.model.ChatRole
import com.androidforge.studio.domain.repository.AiRepository
import com.androidforge.studio.domain.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI assistant repository.
 *
 * Remote: OpenAI chat completions OR Gemini generateContent (chosen in Settings).
 * Offline: [LocalAssistant] rule-based fallback — always available, no network.
 * Context (project tree, open file, build errors) is packed into the prompt.
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    private val chatDao: ChatMessageDao,
    private val settings: SettingsRepository,
) : AiRepository {

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun observeChat(projectId: Long?): Flow<List<ChatMessage>> =
        chatDao.observe(projectId).map { list ->
            list.map {
                ChatMessage(it.id, it.projectId, ChatRole.valueOf(it.role), it.content, it.createdAt)
            }
        }

    override suspend fun sendPrompt(
        projectId: Long?,
        prompt: String,
        context: AiContext,
    ): ChatMessage = withContext(Dispatchers.IO) {
        // persist user message
        chatDao.insert(
            ChatMessageEntity(
                projectId = projectId,
                role = ChatRole.USER.name,
                content = prompt,
                createdAt = System.currentTimeMillis(),
            )
        )

        val history = chatDao.observe(projectId).first().takeLast(10).map {
            it.role.lowercase() to it.content
        }

        val packedContext = buildContextBlock(context)
        val settingsSnapshot = currentSettings()
        val responseText = try {
            when (settingsSnapshot.provider) {
                AiProvider.OPENAI -> callOpenAi(settingsSnapshot, history, prompt, packedContext)
                AiProvider.GEMINI -> callGemini(settingsSnapshot, prompt, packedContext, history)
                AiProvider.LOCAL -> LocalAssistant.respond(prompt, packedContext)
            }
        } catch (e: Exception) {
            // Offline / quota / network → graceful local fallback
            LocalAssistant.respond(prompt, packedContext) + "\n\n_(offline fallback: ${e.message?.take(80) ?: "network error"})_"
        }

        chatDao.insert(
            ChatMessageEntity(
                projectId = projectId,
                role = ChatRole.ASSISTANT.name,
                content = responseText,
                createdAt = System.currentTimeMillis(),
            )
        )
        ChatMessage(projectId = projectId, role = ChatRole.ASSISTANT, content = responseText)
    }

    override suspend fun clearChat(projectId: Long?) = chatDao.clear(projectId)

    override fun settings(): Flow<AiSettings> = kotlinx.coroutines.flow.flow {
        emit(currentSettings())
    }

    override suspend fun updateSettings(settings: AiSettings) {
        this.settings.setAiProvider(settings.provider)
        if (settings.apiKey.isNotBlank()) {
            when (settings.provider) {
                AiProvider.OPENAI -> this.settings.setOpenAiApiKey(settings.apiKey)
                AiProvider.GEMINI -> this.settings.setGeminiApiKey(settings.apiKey)
                AiProvider.LOCAL -> Unit
            }
        }
    }

    // ------------------------------------------------------------- helpers

    private suspend fun currentSettings() = AiSettings(
        provider = settings.aiProvider().first(),
        model = "gpt-4o-mini",
        apiKey = when (settings.aiProvider().first()) {
            AiProvider.OPENAI -> settings.openAiApiKey().first()
            AiProvider.GEMINI -> settings.geminiApiKey().first()
            AiProvider.LOCAL -> ""
        },
    )

    private suspend fun setProvider(p: AiProvider) = settings.setAiProvider(p)

    private fun buildContextBlock(ctx: AiContext): String = buildString {
        if (ctx.includeProjectTree) appendLine("[project tree]\n(files: ${ctx.openFilePath ?: "no file open"})")
        if (ctx.includeOpenFile && ctx.openFilePath != null) {
            appendLine("[open file: ${ctx.openFilePath}]")
            appendLine("```")
            appendLine((ctx.openFileContent ?: "").take(6000))
            appendLine("```")
        }
        if (ctx.includeBuildErrors) appendLine("[build errors]\n(see recent build log)")
    }

    private fun callOpenAi(
        s: AiSettings,
        history: List<Pair<String, String>>,
        prompt: String,
        contextBlock: String,
    ): String {
        require(s.apiKey.isNotBlank()) { "OpenAI API key not set" }
        val base = if (s.baseUrl.isBlank()) "https://api.openai.com/v1" else s.baseUrl.trimEnd('/')
        val body = JsonObject().apply {
            addProperty("model", s.model.ifBlank { "gpt-4o-mini" })
            val msgs = JsonArray()
            msgs.add(JsonObject().apply {
                addProperty("role", "system")
                addProperty(
                    "content",
                    "You are AndroidForge Studio's assistant, an expert Android/Kotlin/Compose IDE agent. " +
                        "Answer with runnable code. Use markdown. Context:\n$contextBlock",
                )
            })
            for ((role, content) in history) {
                msgs.add(JsonObject().apply { addProperty("role", role); addProperty("content", content) })
            }
            msgs.add(JsonObject().apply { addProperty("role", "user"); addProperty("content", prompt) })
            add("messages", msgs)
        }
        val req = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer ${s.apiKey}")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("OpenAI HTTP ${resp.code}")
            val json = gson.fromJson(text, JsonObject::class.java)
            return json.getAsJsonArray("choices")[0].asJsonObject
                .getAsJsonObject("message").get("content").asString
        }
    }

    private fun callGemini(
        s: AiSettings,
        prompt: String,
        contextBlock: String,
        history: List<Pair<String, String>>,
    ): String {
        require(s.apiKey.isNotBlank()) { "Gemini API key not set" }
        val model = if (s.model.startsWith("gemini")) s.model else "gemini-2.0-flash"
        val body = JsonObject().apply {
            val contents = JsonArray()
            for ((role, content) in history.takeLast(6)) {
                contents.add(JsonObject().apply {
                    addProperty("role", if (role == "assistant") "model" else "user")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", content) })
                    })
                })
            }
            contents.add(JsonObject().apply {
                addProperty("role", "user")
                add("parts", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty(
                            "text",
                            "Context:\n$contextBlock\n\nUser request:\n$prompt",
                        )
                    })
                })
            })
            add("contents", contents)
        }
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${s.apiKey}")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Gemini HTTP ${resp.code}")
            val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
            return json.getAsJsonObject("candidates")[0].asJsonObject
                .getAsJsonObject("content").getAsJsonArray("parts")[0].asJsonObject
                .get("text").asString
        }
    }
}
