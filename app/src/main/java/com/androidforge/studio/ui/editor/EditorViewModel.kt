package com.androidforge.studio.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.domain.model.EditorTab
import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import com.androidforge.studio.domain.usecase.ReadFileUseCase
import com.androidforge.studio.domain.usecase.SaveFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val project: Project? = null,
    val tree: FileNode? = null,
    val tabs: List<EditorTab> = emptyList(),
    val activePath: String? = null,
    val content: String = "",
    val loading: Boolean = false,
    val dirty: Boolean = false,
    val truncated: Boolean = false,
    val errorLines: Set<Int> = emptySet(),
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val showTree: Boolean = true,
    val suggestions: List<String> = emptyList(),
    val message: String? = null,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projects: ProjectRepository,
    private val readFile: ReadFileUseCase,
    private val saveFile: SaveFileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    // buffer snapshots for undo/redo (bounded)
    private val undoBuffer = ArrayDeque<String>()
    private val redoBuffer = ArrayDeque<String>()
    private var lastSnapshotAt = 0L

    fun openProject(projectId: Long) {
        if (_state.value.project?.id == projectId) return
        viewModelScope.launch {
            val project = projects.getProject(projectId)
            if (project == null) {
                _state.update { it.copy(message = "Project $projectId not found") }
                return@launch
            }
            _state.update { it.copy(project = project, loading = true) }
            val tree = projects.getFileTree(project)
            _state.update { it.copy(tree = tree, loading = false) }
            // open README if present, else first file
            val first = findFirstFile(tree)
            if (first != null) openFile(first)
        }
    }

    fun refreshTree() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            val tree = projects.getFileTree(project)
            _state.update { it.copy(tree = tree) }
        }
    }

    fun openFile(relativePath: String) {
        val project = _state.value.project ?: return
        if (_state.value.dirty) saveCurrent(silent = true)
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val file = readFile(project, relativePath)
                val lang = CodeLanguage.forFile(file.path.substringAfterLast('/'))
                // close duplicate tab
                val tabs = _state.value.tabs.filterNot { it.filePath == relativePath } +
                    EditorTab(relativePath, file.path.substringAfterLast('/'), lang)
                undoBuffer.clear()
                redoBuffer.clear()
                undoBuffer.addLast(file.text)
                _state.update {
                    it.copy(
                        tabs = tabs,
                        activePath = relativePath,
                        content = file.text,
                        dirty = false,
                        truncated = file.truncated,
                        loading = false,
                        searchQuery = "",
                        suggestions = emptyList(),
                        errorLines = validate(file.text, lang),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, message = e.message) }
            }
        }
    }

    fun closeTab(relativePath: String) {
        val st = _state.value
        val tabs = st.tabs.filterNot { it.filePath == relativePath }
        val newActive = when {
            st.activePath != relativePath -> st.activePath
            tabs.isNotEmpty() -> tabs.last().filePath
            else -> null
        }
        _state.update {
            it.copy(tabs = tabs, activePath = newActive, content = "", dirty = false)
        }
        if (newActive != null && newActive != st.activePath) {
            openFile(newActive)
        }
    }

    fun onContentChange(newContent: String) {
        val current = _state.value
        if (current.activePath == null) return
        // bounded undo snapshots (throttled to 400ms)
        val now = System.currentTimeMillis()
        if (now - lastSnapshotAt > 400) {
            undoBuffer.addLast(current.content)
            if (undoBuffer.size > 60) undoBuffer.removeFirst()
            redoBuffer.clear()
            lastSnapshotAt = now
        }
        val lang = current.tabs.firstOrNull { it.filePath == current.activePath }?.language
            ?: CodeLanguage.TEXT
        _state.update {
            it.copy(
                content = newContent,
                dirty = true,
                errorLines = validate(newContent, lang),
                suggestions = computeSuggestions(newContent, it.activePath?.substringAfterLast('/') ?: ""),
            )
        }
    }

    fun undo() {
        if (undoBuffer.size <= 1) return
        val cur = undoBuffer.removeLast()
        redoBuffer.addLast(cur)
        val prev = undoBuffer.last()
        _state.update { it.copy(content = prev, dirty = true) }
    }

    fun redo() {
        if (redoBuffer.isEmpty()) return
        val next = redoBuffer.removeLast()
        undoBuffer.addLast(next)
        _state.update { it.copy(content = next, dirty = true) }
    }

    fun save() = saveCurrent(silent = false)

    private fun saveCurrent(silent: Boolean) {
        val st = _state.value
        val project = st.project ?: return
        val path = st.activePath ?: return
        viewModelScope.launch {
            val ok = saveFile(project, path, st.content)
            _state.update {
                it.copy(
                    dirty = if (ok) false else it.dirty,
                    message = if (!ok) "Save failed" else if (!silent) "Saved" else null,
                )
            }
        }
    }

    fun toggleSearch() = _state.update { it.copy(searchOpen = !it.searchOpen, searchQuery = "") }
    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }
    fun toggleTree() = _state.update { it.copy(showTree = !it.showTree) }

    fun createFile(relativePath: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            val ok = projects.createFile(project, relativePath, "")
            refreshTree()
            if (ok) openFile(relativePath)
            else _state.update { it.copy(message = "File already exists") }
        }
    }

    fun createDirectory(relativePath: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projects.createDirectory(project, relativePath)
            refreshTree()
        }
    }

    fun deletePath(relativePath: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projects.deletePath(project, relativePath)
            if (_state.value.activePath == relativePath) {
                _state.update { it.copy(activePath = null, content = "", tabs = it.tabs.filterNot { t -> t.filePath == relativePath }) }
            }
            refreshTree()
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun applySuggestion(word: String) {
        val content = _state.value.content
        // replace the trailing partial word heuristically: caller supplies full word
        _state.update { it.copy(content = content, suggestions = emptyList()) }
        // Simple append strategy: insert at end (real implementation would use cursor offset)
        onContentChange(content + word)
    }

    fun clearSuggestions() = _state.update { it.copy(suggestions = emptyList()) }

    // ------------------------------------------------------------ helpers

    private fun findFirstFile(node: FileNode): String? {
        if (!node.isDirectory && node.name.endsWith(".kt")) return node.relativePath
        for (c in node.children) {
            if (!c.isDirectory && c.name.endsWith(".kt")) return c.relativePath
            if (c.isDirectory) findFirstFile(c)?.let { return it }
        }
        return null
    }

    /**
     * Lightweight static checks — bracket balance + TODO/fixme underlines.
     * (The full LSP/Tree-sitter path plugs in here.)
     */
    private fun validate(text: String, language: CodeLanguage): Set<Int> {
        if (language == CodeLanguage.TEXT) return emptySet()
        val errors = mutableSetOf<Int>()
        var brace = 0
        var paren = 0
        var bracket = 0
        var inString = false
        var inLineComment = false
        var line = 1
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val next = text.getOrNull(i + 1)
            if (c == '\n') {
                line++
                inLineComment = false
                i++
                continue
            }
            if (inLineComment) { i++; continue }
            if (inString) {
                if (c == '\\') { i += 2; continue }
                if (c == '"') inString = false
                i++; continue
            }
            when {
                c == '/' && next == '/' -> { inLineComment = true; i += 2; continue }
                c == '"' -> inString = true
                c == '{' -> brace++
                c == '}' -> { brace--; if (brace < 0) { errors += line; brace = 0 } }
                c == '(' -> paren++
                c == ')' -> { paren--; if (paren < 0) { errors += line; paren = 0 } }
                c == '[' -> bracket++
                c == ']' -> { bracket--; if (bracket < 0) { errors += line; bracket = 0 } }
            }
            i++
        }
        if (brace > 0 || paren > 0 || bracket > 0) {
            // mark the last line as suspect
            errors += text.count { it == '\n' } + 1
        }
        // highlight unresolved markers
        text.lineSequence().forEachIndexed { idx, l ->
            if (l.contains("// ERROR") || l.contains("FIXME")) errors += idx + 1
        }
        return errors
    }

    /** Keyword + file-symbol suggestions for the word being typed. */
    private fun computeSuggestions(content: String, fileName: String): List<String> {
        val words = content
            .split(Regex("[^A-Za-z0-9_]+"))
            .filter { it.length >= 3 }
            .toSet()
        val lang = CodeLanguage.forFile(fileName)
        val keywords = when (lang) {
            CodeLanguage.KOTLIN, CodeLanguage.GRADLE_KTS ->
                listOf("composable", "viewmodel", "launchedeffect", "mutablestateof", "column", "row", "box", "text", "button", "scaffold")
            CodeLanguage.JAVA -> listOf("class", "public", "void", "static", "string")
            CodeLanguage.XML -> listOf("layout_width", "layout_height", "match_parent", "wrap_content", "text")
            else -> emptyList()
        }
        return (words.take(12) + keywords).distinct().take(16)
    }
}
