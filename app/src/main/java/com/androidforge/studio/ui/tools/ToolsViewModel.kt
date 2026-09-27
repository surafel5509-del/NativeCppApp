package com.androidforge.studio.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.BuildTask
import com.androidforge.studio.domain.model.CrashReport
import com.androidforge.studio.domain.model.GitCommit
import com.androidforge.studio.domain.model.GitDiff
import com.androidforge.studio.domain.model.GitStatus
import com.androidforge.studio.domain.model.Plugin
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.model.TerminalLine
import com.androidforge.studio.domain.repository.GitRepository
import com.androidforge.studio.domain.repository.PluginRepository
import com.androidforge.studio.domain.repository.ProjectRepository
import com.androidforge.studio.domain.repository.TerminalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ToolTab { TERMINAL, GIT, DIFF, DEBUG }

data class ToolsUiState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val tab: ToolTab = ToolTab.TERMINAL,
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalInput: String = "",
    val gitStatus: GitStatus? = null,
    val gitLog: List<GitCommit> = emptyList(),
    val gitDiffs: List<GitDiff> = emptyList(),
    val commitMessage: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val crashInput: String = "",
    val crashReport: CrashReport? = null,
    val buildTasks: List<BuildTask> = emptyList(),
    val plugins: List<Plugin> = emptyList(),
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val projects: ProjectRepository,
    private val terminal: TerminalRepository,
    private val git: GitRepository,
    private val pluginRepo: PluginRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ToolsUiState())
    val state: StateFlow<ToolsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            projects.observeProjects().collect { list ->
                _state.update { st ->
                    val sel = st.selectedProjectId?.takeIf { id -> list.any { it.id == id } }
                        ?: list.firstOrNull()?.id
                    st.copy(projects = list, selectedProjectId = sel)
                }
                refreshProjectData()
            }
        }
        viewModelScope.launch {
            pluginRepo.observePlugins().collect { list ->
                _state.update { it.copy(plugins = list) }
            }
        }
    }

    fun setTab(tab: ToolTab) {
        _state.update { it.copy(tab = tab) }
        if (tab == ToolTab.GIT) refreshGit()
    }

    fun selectProject(id: Long) {
        _state.update { it.copy(selectedProjectId = id) }
        refreshProjectData()
    }

    private fun refreshProjectData() {
        val id = _state.value.selectedProjectId ?: return
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            runCatching { pluginRepo.buildTasks(project) }
                .onSuccess { tasks -> _state.update { it.copy(buildTasks = tasks) } }
            observeTerminal(id)
            if (_state.value.tab == ToolTab.GIT) refreshGit()
        }
    }

    private var terminalObserving = false
    private fun observeTerminal(projectId: Long) {
        if (terminalObserving) return
        terminalObserving = true
        viewModelScope.launch {
            terminal.observeLines(projectId).collect { lines ->
                _state.update { it.copy(terminalLines = lines) }
            }
        }
    }

    fun setTerminalInput(v: String) = _state.update { it.copy(terminalInput = v) }

    fun runCommand() {
        val cmd = _state.value.terminalInput.trim()
        val id = _state.value.selectedProjectId ?: return
        if (cmd.isEmpty()) return
        _state.update { it.copy(terminalInput = "") }
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            runCatching { terminal.execute(project, cmd) }
                .onFailure { e ->
                    _state.update { it.copy(message = e.message) }
                }
        }
    }

    fun refreshGit() {
        val id = _state.value.selectedProjectId ?: return
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            _state.update { it.copy(busy = true) }
            val status = runCatching { git.status(project) }.getOrNull()
            val log = runCatching { git.log(project) }.getOrElse { emptyList() }
            _state.update { it.copy(gitStatus = status, gitLog = log, busy = false) }
        }
    }

    fun gitInit() = gitAction("Init") { p -> git.init(p) }
    fun gitStageAll() = gitAction("Staged all") { p -> git.addAll(p) }

    fun setCommitMessage(v: String) = _state.update { it.copy(commitMessage = v) }

    fun gitCommit() {
        val msg = _state.value.commitMessage.trim()
        if (msg.isEmpty()) {
            _state.update { it.copy(message = "Commit message required") }
            return
        }
        val id = _state.value.selectedProjectId ?: return
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            val hash = git.commit(project, msg, "AndroidForge User", "user@androidforge.local")
            _state.update {
                it.copy(
                    commitMessage = "",
                    message = if (hash != null) "Committed ${hash.take(7)}" else "Commit failed",
                )
            }
            refreshGit()
        }
    }

    fun gitDiff() {
        val id = _state.value.selectedProjectId ?: return
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            val diffs = runCatching { git.diff(project) }.getOrElse { emptyList() }
            _state.update { it.copy(gitDiffs = diffs, tab = ToolTab.DIFF) }
        }
    }

    private fun gitAction(label: String, action: suspend (Project) -> Boolean) {
        val id = _state.value.selectedProjectId ?: return
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            val ok = runCatching { action(project) }.getOrElse { false }
            _state.update { it.copy(message = if (ok) "$label OK" else "$label failed") }
            refreshGit()
        }
    }

    fun setCrashInput(v: String) = _state.update { it.copy(crashInput = v) }

    fun analyzeCrash() {
        val text = _state.value.crashInput
        if (text.isBlank()) {
            _state.update { it.copy(message = "Paste a crash log first") }
            return
        }
        val pkg = _state.value.projects.firstOrNull { it.id == _state.value.selectedProjectId }?.packageName ?: ""
        val report = CrashLogAnalyzer.parse(text, pkg)
        _state.update { it.copy(crashReport = report, message = "Parsed ${report.stackFrames.size} frames") }
    }

    fun runBuildTask(task: BuildTask) {
        val id = _state.value.selectedProjectId ?: return
        viewModelScope.launch {
            val project = projects.getProject(id) ?: return@launch
            _state.update { it.copy(tab = ToolTab.TERMINAL, terminalInput = task.command) }
            terminal.execute(project, task.command)
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}

/**
 * Parses FATAL EXCEPTION blocks (and generic Java stack traces) from logcat
 * output, marking frames that belong to the current project package.
 */
object CrashLogAnalyzer {

    fun parse(log: String, projectPackage: String): CrashReport {
        val lines = log.lines()
        val fatalIdx = lines.indexOfFirst { it.contains("FATAL EXCEPTION") }
        val start = if (fatalIdx >= 0) fatalIdx else 0

        var process = "unknown"
        var exceptionType = "UnknownException"
        var message = ""
        val frames = mutableListOf<CrashReport.StackFrame>()
        val causes = mutableListOf<String>()

        val frameRegex = Regex("""^\s*(?:at\s+)?([\w.$]+)\.([\w$<>]+)\(([\w.$]*?)(?::(\d+))?\)""")
        val causedBy = Regex("""^(?:Caused by|Suppressed):\s*(.+)$""")
        // Strip "MM-DD HH:MM:SS.mmm PID TID L Tag: " logcat prefixes so the
        // ^-anchored matchers above see the actual message.
        val logcatPrefix = Regex("""^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+\s+\d+\s+\d+\s+[VDIWEF]\s+[^:]+:\s?""")

        var inTrace = false
        for (i in start until lines.size) {
            val line = logcatPrefix.replaceFirst(lines[i], "")
            if (line.contains("FATAL EXCEPTION")) {
                inTrace = true
                continue
            }
            if (line.startsWith("Process:") || line.contains("Process: ")) {
                process = line.substringAfter("Process:").trim().take(80)
                continue
            }
            if (line.startsWith("pid:") || line.contains("Build fingerprint")) continue

            val causeMatch = causedBy.find(line.trim())
            if (causeMatch != null) {
                causes += causeMatch.groupValues[1].trim()
                if (exceptionType == "UnknownException") {
                    exceptionType = causeMatch.groupValues[1].substringBefore(':').trim()
                    message = causeMatch.groupValues[1].substringAfter(':', "").trim()
                }
                inTrace = true
                continue
            }

            if (inTrace) {
                // Exception header line: "java.lang.NullPointerException: msg"
                if (Regex("""^[a-zA-Z][\w.]*Exception(:.*)?$""").matches(line.trim()) ||
                    Regex("""^[a-zA-Z][\w.]*Error(:.*)?$""").matches(line.trim())
                ) {
                    exceptionType = line.trim().substringBefore(':').trim()
                    message = line.trim().substringAfter(':', "").trim()
                    continue
                }
                val m = frameRegex.find(line)
                if (m != null) {
                    val cls = m.groupValues[1]
                    val method = m.groupValues[2]
                    val file = m.groupValues[3].ifBlank { null }
                    val lineNo = m.groupValues[4].toIntOrNull()
                    frames += CrashReport.StackFrame(
                        className = cls,
                        methodName = method,
                        file = file,
                        line = lineNo,
                        inProject = projectPackage.isNotBlank() && cls.startsWith(projectPackage),
                    )
                    continue
                }
                if (line.isBlank() && frames.isNotEmpty()) break
            }
        }

        if (message.isBlank()) message = log.lineSequence().firstOrNull { it.isNotBlank() }?.take(200) ?: ""
        return CrashReport(
            process = process,
            exceptionType = exceptionType,
            message = message,
            stackFrames = frames,
            causeChain = causes,
        )
    }
}
