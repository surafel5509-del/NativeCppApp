package com.androidforge.studio.ui.build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.BuildSession
import com.androidforge.studio.domain.model.BuildStage
import com.androidforge.studio.domain.model.BuildStatus
import com.androidforge.studio.domain.model.BuildTarget
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.BuildRepository
import com.androidforge.studio.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuildUiState(
    val project: Project? = null,
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long = -1L,
    val target: BuildTarget = BuildTarget.APK,
    val useCloud: Boolean = false,
    val session: BuildSession = BuildSession(projectId = -1, target = BuildTarget.APK),
    val history: List<BuildSession> = emptyList(),
    val logLines: List<String> = emptyList(),
    val lastStage: BuildStage = BuildStage.PENDING,
    val syncing: Boolean = false,
    val syncResult: String? = null,
    val message: String? = null,
)

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val builds: BuildRepository,
    private val projects: ProjectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BuildUiState())
    val state: StateFlow<BuildUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            projects.observeProjects().collect { list ->
                _state.update { st ->
                    val selected = st.selectedProjectId
                        .takeIf { id -> list.any { it.id == id } }
                        ?: list.firstOrNull()?.id
                        ?: -1L
                    st.copy(
                        projects = list,
                        selectedProjectId = selected,
                        project = list.firstOrNull { it.id == selected },
                    )
                }
                val id = _state.value.selectedProjectId
                if (id > 0) loadHistory(id)
            }
        }
        // Single live observer: refresh log + history whenever the session changes.
        viewModelScope.launch {
            builds.observeBuild().collect { session ->
                _state.update { st ->
                    // Keep the last real pipeline stage so the strip can show
                    // where a FAILED run actually stopped.
                    val shown = if (session.stage == BuildStage.FAILED) st.lastStage else session.stage
                    st.copy(
                        session = session,
                        lastStage = shown,
                        logLines = session.log.lines().let { lines ->
                            if (lines.size == 1 && lines[0].isEmpty()) emptyList()
                            else lines.dropLastWhile { it.isEmpty() }
                        },
                    )
                }
                if (session.status == BuildStatus.SUCCESS ||
                    session.status == BuildStatus.FAILED ||
                    session.status == BuildStatus.CANCELLED
                ) {
                    if (session.projectId > 0) loadHistory(session.projectId)
                }
            }
        }
    }

    fun selectProject(id: Long) {
        _state.update { st ->
            st.copy(
                selectedProjectId = id,
                project = st.projects.firstOrNull { it.id == id },
            )
        }
        loadHistory(id)
        // Reset the visible log when switching away from the active run.
        val st = _state.value
        if (st.session.projectId != id || st.session.status == BuildStatus.IDLE) {
            _state.update { it.copy(logLines = emptyList()) }
        }
    }

    private fun loadHistory(projectId: Long) {
        viewModelScope.launch {
            val history = builds.recentSessions(projectId)
            _state.update { it.copy(history = history) }
        }
    }

    fun setTarget(target: BuildTarget) = _state.update { it.copy(target = target) }

    fun setCloud(useCloud: Boolean) = _state.update { it.copy(useCloud = useCloud) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun dismissSyncResult() = _state.update { it.copy(syncResult = null) }

    fun clearLog() = _state.update { it.copy(logLines = emptyList()) }

    fun startBuild() {
        val st = _state.value
        if (st.selectedProjectId <= 0) {
            _state.update { it.copy(message = "Create a project first") }
            return
        }
        if (st.session.status == BuildStatus.RUNNING) return
        _state.update { it.copy(logLines = emptyList(), message = null) }
        viewModelScope.launch {
            builds.startBuild(st.selectedProjectId, st.target, st.useCloud)
            // history refreshed by the init observer on terminal states
        }
    }

    fun cancelBuild() = builds.cancelBuild()

    fun sync() {
        val st = _state.value
        if (st.selectedProjectId <= 0 || st.syncing) return
        _state.update { it.copy(syncing = true, syncResult = null) }
        viewModelScope.launch {
            val ok = builds.syncProject(st.selectedProjectId)
            _state.update {
                it.copy(
                    syncing = false,
                    syncResult = if (ok) "Sync OK" else "Sync failed — check the log",
                )
            }
        }
    }

    fun install(path: String) {
        if (path.isBlank()) return
        viewModelScope.launch { builds.installApk(path) }
    }

    fun clearHistory() {
        val id = _state.value.selectedProjectId
        if (id <= 0) return
        viewModelScope.launch {
            builds.clearHistory(id)
            _state.update { it.copy(history = emptyList()) }
        }
    }
}
