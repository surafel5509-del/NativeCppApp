package com.androidforge.studio.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.AiContext
import com.androidforge.studio.domain.model.AiProvider
import com.androidforge.studio.domain.model.ChatMessage
import com.androidforge.studio.domain.model.ChatRole
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.AiRepository
import com.androidforge.studio.domain.repository.ProjectRepository
import com.androidforge.studio.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val provider: AiProvider = AiProvider.LOCAL,
    val projectId: Long? = null,
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val includeTree: Boolean = true,
    val includeFile: Boolean = true,
    val includeErrors: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val ai: AiRepository,
    private val sendChat: SendChatMessageUseCase,
    private val projects: ProjectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    private var chatJob: Job? = null

    init {
        chatJob = viewModelScope.launch {
            ai.observeChat(null).collect { list ->
                _state.update { it.copy(messages = list) }
            }
        }
        viewModelScope.launch {
            ai.settings().collect { s ->
                _state.update { it.copy(provider = s.provider) }
            }
        }
        viewModelScope.launch {
            projects.observeProjects().collect { list ->
                _state.update { st ->
                    val sel = st.selectedProjectId ?: list.firstOrNull()?.id
                    st.copy(projects = list, selectedProjectId = sel, projectId = sel)
                }
            }
        }
    }

    fun setInput(v: String) = _state.update { it.copy(input = v) }

    fun selectProject(id: Long?) {
        _state.update { it.copy(selectedProjectId = id, projectId = id) }
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            ai.observeChat(id).collect { list -> _state.update { it.copy(messages = list) } }
        }
    }

    fun toggleContextFlag(which: Int) {
        _state.update {
            when (which) {
                0 -> it.copy(includeTree = !it.includeTree)
                1 -> it.copy(includeFile = !it.includeFile)
                else -> it.copy(includeErrors = !it.includeErrors)
            }
        }
    }

    fun send() {
        val prompt = _state.value.input.trim()
        if (prompt.isEmpty() || _state.value.sending) return
        _state.update { it.copy(input = "", sending = true, message = null) }
        viewModelScope.launch {
            try {
                val ctx = AiContext(
                    includeProjectTree = _state.value.includeTree,
                    includeOpenFile = _state.value.includeFile,
                    includeBuildErrors = _state.value.includeErrors,
                )
                sendChat(_state.value.projectId, prompt, ctx)
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "AI request failed") }
            } finally {
                _state.update { it.copy(sending = false) }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch { ai.clearChat(_state.value.projectId) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** Quick actions shown as chips above the input. */
    fun applyQuickAction(action: String) {
        val prompt = when (action) {
            "Create screen" -> "Create a new screen for my app with a top bar, list and FAB"
            "Fix error" -> "Analyze this build error and propose a fix"
            "Explain code" -> "Explain what the open file does, step by step"
            "Improve" -> "Suggest improvements for the current project architecture"
            else -> action
        }
        _state.update { it.copy(input = prompt) }
    }
}
