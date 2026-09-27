package com.androidforge.studio.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.data.template.TemplateEngine
import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.domain.model.EditorTab
import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.FileTemplate
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.BuildRepository
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
    val showProblems: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val message: String? = null,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),
    // Professional build features
    val building: Boolean = false,
    val buildStatus: String? = null,
    val buildProgress: Float = 0f,
    val cursorLine: Int = 0,
    val cursorColumn: Int = 0,
    val searchInProject: Boolean = false,
    val projectSearchQuery: String = "",
    val projectSearchResults: List<String> = emptyList(),
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projects: ProjectRepository,
    private val builds: BuildRepository,
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
                _state.update { it.copy(message = "Project $projectId not found - Professional") }
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
                        cursorLine = 1,
                        cursorColumn = 0,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, message = "Failed to open: ${e.message} - Professional") }
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
        val lines = newContent.lines()
        val lastLine = lines.lastOrNull() ?: ""
        _state.update {
            it.copy(
                content = newContent,
                dirty = true,
                errorLines = validate(newContent, lang),
                suggestions = computeSuggestions(newContent, it.activePath?.substringAfterLast('/') ?: ""),
                cursorLine = lines.size,
                cursorColumn = lastLine.length,
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
                    message = if (!ok) "Save failed - Professional" else if (!silent) "Saved - Professional" else null,
                )
            }
            if (ok) refreshTree()
        }
    }

    fun toggleSearch() = _state.update { it.copy(searchOpen = !it.searchOpen, searchQuery = "") }
    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }
    fun toggleTree() = _state.update { it.copy(showTree = !it.showTree) }
    fun toggleProblems() = _state.update { it.copy(showProblems = !it.showProblems) }
    fun dismissBuildStatus() = _state.update { it.copy(buildStatus = null, building = false) }

    fun createFile(relativePath: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            // Professional: support nested paths and templates
            val finalPath = if (relativePath.contains("/")) relativePath else "app/src/main/java/${project.packageName.replace('.', '/')}/$relativePath"
            val content = when {
                finalPath.endsWith(".kt") -> TemplateEngine.generateFileContent("kotlin_class", finalPath.substringAfterLast('/'), project.packageName)
                finalPath.endsWith(".java") -> "package ${project.packageName};\n\npublic class ${finalPath.substringAfterLast('/').removeSuffix(".java")} {\n}\n"
                finalPath.endsWith(".xml") -> TemplateEngine.generateFileContent("xml_layout", finalPath.substringAfterLast('/'), project.packageName)
                finalPath.endsWith(".cpp") -> TemplateEngine.generateFileContent("cpp_file", finalPath.substringAfterLast('/'), project.packageName)
                else -> "// Professional file: $finalPath\n"
            }
            val ok = projects.createFile(project, finalPath, content)
            refreshTree()
            if (ok) {
                openFile(finalPath)
                _state.update { it.copy(message = "Created $finalPath - Professional") }
            } else {
                _state.update { it.copy(message = "File already exists: $finalPath") }
            }
        }
    }

    fun createFileFromTemplate(template: FileTemplate) {
        val project = _state.value.project ?: return
        val ext = template.extension
        val baseName = when (template) {
            FileTemplate.KOTLIN_CLASS -> "MyClass"
            FileTemplate.KOTLIN_COMPOSABLE -> "MyScreen"
            FileTemplate.KOTLIN_VIEWMODEL -> "MyViewModel"
            FileTemplate.JAVA_CLASS -> "MyJavaClass"
            FileTemplate.CPP_FILE -> "native-lib"
            FileTemplate.XML_LAYOUT -> "activity_new"
            else -> "NewFile"
        }
        val fileName = if (ext == "txt") "CMakeLists.txt" else "$baseName.$ext"
        createFile(fileName)
    }

    fun createDirectory(relativePath: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projects.createDirectory(project, relativePath)
            refreshTree()
            _state.update { it.copy(message = "Created directory $relativePath - Professional") }
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
            _state.update { it.copy(message = "Deleted $relativePath - Professional") }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun applySuggestion(word: String) {
        val content = _state.value.content
        _state.update { it.copy(content = content, suggestions = emptyList()) }
        onContentChange(content + word)
    }

    fun clearSuggestions() = _state.update { it.copy(suggestions = emptyList()) }

    // Professional build integration - Run button at top
    fun buildProject() {
        val project = _state.value.project ?: run {
            _state.update { it.copy(message = "No project open - Professional") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(building = true, buildStatus = "Building ${project.name}... Professional APK compilation", buildProgress = 0.1f) }
            try {
                // Save current file first
                if (_state.value.dirty) saveCurrent(silent = true)
                
                _state.update { it.copy(buildStatus = "Syncing project... Professional", buildProgress = 0.2f) }
                val syncOk = builds.syncProject(project.id)
                if (!syncOk) {
                    _state.update { it.copy(building = false, buildStatus = "Sync failed - Check project structure - Professional", buildProgress = 0f) }
                    return@launch
                }

                _state.update { it.copy(buildStatus = "Compiling resources (aapt2)... Professional", buildProgress = 0.4f) }
                // Simulate stages for UI - real build happens in BuildRepository
                kotlinx.coroutines.delay(500)
                
                _state.update { it.copy(buildStatus = "Compiling Kotlin/Java... Professional", buildProgress = 0.6f) }
                kotlinx.coroutines.delay(500)
                
                _state.update { it.copy(buildStatus = "Dexing (d8)... Professional", buildProgress = 0.8f) }
                
                // Start real build
                builds.startBuild(project.id, com.androidforge.studio.domain.model.BuildTarget.APK, false)
                
                _state.update { it.copy(buildStatus = "Build started - Check Build tab for details - Professional APK generation", building = false, buildProgress = 1f) }
                
                // Observe build result
                builds.observeBuild().collect { session ->
                    when (session.status) {
                        com.androidforge.studio.domain.model.BuildStatus.SUCCESS -> {
                            _state.update { it.copy(buildStatus = "Build Success! APK: ${session.artifactPath?.substringAfterLast('/') ?: "Ready"} - Professional", building = false) }
                            return@collect
                        }
                        com.androidforge.studio.domain.model.BuildStatus.FAILED -> {
                            _state.update { it.copy(buildStatus = "Build Failed - Check Build tab for errors - Professional", building = false) }
                            return@collect
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(building = false, buildStatus = "Build error: ${e.message} - Professional") }
            }
        }
    }

    fun searchInProject(query: String) {
        val project = _state.value.project ?: return
        if (query.length < 2) {
            _state.update { it.copy(projectSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val tree = projects.getFileTree(project)
            val results = mutableListOf<String>()
            fun search(node: com.androidforge.studio.domain.model.FileNode) {
                if (!node.isDirectory && node.name.contains(query, ignoreCase = true)) {
                    results.add(node.relativePath)
                }
                node.children.forEach { search(it) }
            }
            search(tree)
            _state.update { it.copy(projectSearchResults = results.take(20)) }
        }
    }

    // ------------------------------------------------------------ helpers

    private fun findFirstFile(node: FileNode): String? {
        if (!node.isDirectory && node.name.endsWith(".kt")) return node.relativePath
        for (c in node.children) {
            if (!c.isDirectory && (c.name.endsWith(".kt") || c.name.endsWith(".java") || c.name.endsWith(".cpp"))) return c.relativePath
            if (c.isDirectory) findFirstFile(c)?.let { return it }
        }
        // Fallback to any file
        fun findAny(n: FileNode): String? {
            if (!n.isDirectory) return n.relativePath
            for (child in n.children) {
                findAny(child)?.let { return it }
            }
            return null
        }
        return findAny(node)
    }

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
            errors += text.count { it == '\n' } + 1
        }
        text.lineSequence().forEachIndexed { idx, l ->
            if (l.contains("// ERROR") || l.contains("FIXME") || l.contains("TODO") && l.contains("error", ignoreCase = true)) errors += idx + 1
        }
        // Professional: check for common errors
        if (language == CodeLanguage.KOTLIN) {
            text.lineSequence().forEachIndexed { idx, l ->
                if (l.contains("Unresolved reference") || l.contains("Type mismatch")) errors += idx + 1
            }
        }
        if (language == CodeLanguage.CPP || language == CodeLanguage.C) {
            text.lineSequence().forEachIndexed { idx, l ->
                if (l.contains("error:") || l.contains("undefined reference")) errors += idx + 1
            }
        }
        return errors
    }

    private fun computeSuggestions(content: String, fileName: String): List<String> {
        val words = content
            .split(Regex("[^A-Za-z0-9_]+"))
            .filter { it.length >= 3 }
            .toSet()
        val lang = CodeLanguage.forFile(fileName)
        val keywords = when (lang) {
            CodeLanguage.KOTLIN, CodeLanguage.GRADLE_KTS ->
                listOf("composable", "viewmodel", "launchedeffect", "mutablestateof", "column", "row", "box", "text", "button", "scaffold", "remember", "mutableStateOf", "LaunchedEffect", "hiltViewModel", "collectAsState")
            CodeLanguage.JAVA -> listOf("class", "public", "void", "static", "string", "override", "extends", "implements")
            CodeLanguage.CPP, CodeLanguage.C -> listOf("include", "namespace", "class", "void", "int", "float", "JNIEXPORT", "JNICALL", "std::", "android/log.h", "GLES3/gl3.h")
            CodeLanguage.XML -> listOf("layout_width", "layout_height", "match_parent", "wrap_content", "text", "android:id", "app:layout_constraint")
            CodeLanguage.CMAKE -> listOf("cmake_minimum_required", "project", "add_library", "find_library", "target_link_libraries")
            else -> emptyList()
        }
        return (words.take(12) + keywords).distinct().take(20)
    }
}
