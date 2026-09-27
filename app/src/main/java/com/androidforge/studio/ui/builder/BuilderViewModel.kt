package com.androidforge.studio.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class BuilderMode { 
    DESIGN, 
    CODE_COMPOSE, 
    CODE_XML, 
    PREVIEW, 
    LAYERS, 
    BLUEPRINT 
}

enum class DevicePreview(val displayName: String, val widthDp: Int, val heightDp: Int) {
    PHONE("Phone", 360, 800),
    PHONE_LANDSCAPE("Phone Landscape", 800, 360),
    TABLET("Tablet", 800, 1280),
    TABLET_LANDSCAPE("Tablet Landscape", 1280, 800),
    FOLDABLE("Foldable", 673, 841),
    DESKTOP("Desktop", 1280, 800),
}

enum class ThemePreview(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    DYNAMIC("Dynamic"),
}

data class BuilderUiState(
    val project: Project? = null,
    val root: UiComponent = UiComponent("root", ComponentType.COLUMN),
    val selectedId: String? = null,
    val mode: BuilderMode = BuilderMode.DESIGN,
    val paletteOpen: Boolean = true,
    val propertiesOpen: Boolean = false,
    val layersOpen: Boolean = false,
    val message: String? = null,
    val exportTarget: String = "app/src/main/java/generated/GeneratedScreen.kt",
    val exporting: Boolean = false,
    val devicePreview: DevicePreview = DevicePreview.PHONE,
    val themePreview: ThemePreview = ThemePreview.DARK,
    val showGrid: Boolean = true,
    val showConstraints: Boolean = false,
    val zoom: Float = 1f,
    val clipboard: UiComponent? = null,
    val history: List<UiComponent> = emptyList(),
    val historyIndex: Int = -1,
    val searchQuery: String = "",
) {
    val selected: UiComponent?
        get() = selectedId?.let { id -> find(root, id) }

    val canUndo: Boolean get() = historyIndex > 0
    val canRedo: Boolean get() = historyIndex < history.size - 1

    companion object {
        fun find(node: UiComponent, id: String): UiComponent? {
            if (node.id == id) return node
            for (c in node.children) find(c, id)?.let { return it }
            return null
        }

        fun replace(node: UiComponent, id: String, transform: (UiComponent) -> UiComponent): UiComponent {
            if (node.id == id) return transform(node)
            return node.copy(children = node.children.map { replace(it, id, transform) })
        }

        fun remove(node: UiComponent, id: String): UiComponent =
            node.copy(children = node.children.filterNot { it.id == id }
                .map { remove(it, id) })

        fun insert(node: UiComponent, parentId: String, child: UiComponent): UiComponent {
            if (node.id == parentId && node.type.isContainer) {
                return node.copy(children = node.children + child)
            }
            return node.copy(children = node.children.map { insert(it, parentId, child) })
        }

        fun insertAt(node: UiComponent, parentId: String, child: UiComponent, index: Int): UiComponent {
            if (node.id == parentId && node.type.isContainer) {
                val list = node.children.toMutableList()
                list.add(index.coerceIn(0, list.size), child)
                return node.copy(children = list)
            }
            return node.copy(children = node.children.map { insertAt(it, parentId, child, index) })
        }

        fun move(node: UiComponent, id: String, direction: Int): UiComponent {
            val idx = node.children.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val target = idx + direction
                if (target in node.children.indices) {
                    val list = node.children.toMutableList()
                    val item = list.removeAt(idx)
                    list.add(target, item)
                    return node.copy(children = list)
                }
                return node
            }
            return node.copy(children = node.children.map { move(it, id, direction) })
        }

        fun depth(node: UiComponent, id: String, d: Int = 0): Int {
            if (node.id == id) return d
            for (c in node.children) {
                val r = depth(c, id, d + 1)
                if (r >= 0) return r
            }
            return -1
        }

        fun parentId(node: UiComponent, id: String): String? {
            if (node.children.any { it.id == id }) return node.id
            for (c in node.children) parentId(c, id)?.let { return it }
            return null
        }

        fun flatten(node: UiComponent): List<UiComponent> {
            val list = mutableListOf<UiComponent>()
            fun visit(n: UiComponent) {
                list.add(n)
                n.children.forEach { visit(it) }
            }
            visit(node)
            return list
        }

        fun countComponents(node: UiComponent): Int = 1 + node.children.sumOf { countComponents(it) }
    }
}

@HiltViewModel
class BuilderViewModel @Inject constructor(
    private val projects: ProjectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state.asStateFlow()

    private var counter = 0
    private fun nextId(): String = "n${counter++}-${UUID.randomUUID().toString().take(6)}"

    init {
        // Initialize with professional sample
        val sample = UiComponent(
            id = "root",
            type = ComponentType.COLUMN,
            props = mapOf("padding" to "16"),
            children = listOf(
                UiComponent("sample-1", ComponentType.TOP_APP_BAR, mapOf("title" to "Professional App")),
                UiComponent("sample-2", ComponentType.TEXT, mapOf("text" to "Welcome to Professional UI Builder", "size" to "24", "fontWeight" to "bold")),
                UiComponent("sample-3", ComponentType.SPACER, mapOf("height" to "16")),
                UiComponent("sample-4", ComponentType.CARD, mapOf("elevation" to "4"), listOf(
                    UiComponent("sample-4-1", ComponentType.TEXT, mapOf("text" to "Professional Card", "size" to "18")),
                    UiComponent("sample-4-2", ComponentType.TEXT, mapOf("text" to "Drag & drop components, edit properties, preview instantly", "size" to "14")),
                )),
                UiComponent("sample-5", ComponentType.SPACER, mapOf("height" to "16")),
                UiComponent("sample-6", ComponentType.ROW, mapOf(), listOf(
                    UiComponent("sample-6-1", ComponentType.BUTTON, mapOf("text" to "Primary")),
                    UiComponent("sample-6-2", ComponentType.OUTLINED_BUTTON, mapOf("text" to "Secondary")),
                )),
            )
        )
        _state.update { it.copy(root = sample, history = listOf(sample), historyIndex = 0) }
    }

    fun openProject(projectId: Long) {
        if (_state.value.project?.id == projectId) return
        viewModelScope.launch {
            val project = projects.getProject(projectId)
            _state.update { it.copy(project = project) }
        }
    }

    private fun pushHistory(newRoot: UiComponent) {
        _state.update { st ->
            val newHistory = st.history.take(st.historyIndex + 1) + newRoot
            val trimmed = if (newHistory.size > 50) newHistory.takeLast(50) else newHistory
            st.copy(
                root = newRoot,
                history = trimmed,
                historyIndex = trimmed.size - 1,
            )
        }
    }

    fun addToParent(type: ComponentType) {
        val st = _state.value
        val parentId = st.selectedId?.takeIf { id ->
            BuilderUiState.find(st.root, id)?.type?.isContainer == true
        } ?: st.root.id
        val child = newComponent(type, nextId())
        val newRoot = BuilderUiState.insert(st.root, parentId, child)
        pushHistory(newRoot)
        _state.update { it.copy(selectedId = child.id, message = "Added ${type.displayName} - Professional", propertiesOpen = true) }
    }

    fun addToParentAt(type: ComponentType, parentId: String, index: Int) {
        val child = newComponent(type, nextId())
        val newRoot = BuilderUiState.insertAt(_state.value.root, parentId, child, index)
        pushHistory(newRoot)
        _state.update { it.copy(selectedId = child.id, message = "Added ${type.displayName}") }
    }

    fun select(id: String?) = _state.update { it.copy(selectedId = id, propertiesOpen = id != null) }

    fun deleteSelected() {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            if (id == st.root.id) return@update st.copy(message = "Cannot delete root - Professional protection")
            val newRoot = BuilderUiState.remove(st.root, id)
            st.copy(
                root = newRoot,
                history = st.history.take(st.historyIndex + 1) + newRoot,
                historyIndex = st.historyIndex + 1,
                selectedId = null,
                propertiesOpen = false,
                message = "Deleted - Professional",
            )
        }
    }

    fun duplicateSelected() {
        val st = _state.value
        val id = st.selectedId ?: return
        val selected = BuilderUiState.find(st.root, id) ?: return
        val parentId = BuilderUiState.parentId(st.root, id) ?: st.root.id
        val duplicate = selected.copy(id = nextId(), children = selected.children.map { it.copy(id = nextId()) })
        val newRoot = BuilderUiState.insert(st.root, parentId, duplicate)
        pushHistory(newRoot)
        _state.update { it.copy(selectedId = duplicate.id, message = "Duplicated - Professional") }
    }

    fun copySelected() {
        val st = _state.value
        val id = st.selectedId ?: return
        val selected = BuilderUiState.find(st.root, id) ?: return
        _state.update { it.copy(clipboard = selected, message = "Copied ${selected.type.displayName}") }
    }

    fun paste() {
        val st = _state.value
        val clipboard = st.clipboard ?: return
        val parentId = st.selectedId?.takeIf { id -> BuilderUiState.find(st.root, id)?.type?.isContainer == true } ?: st.root.id
        val paste = clipboard.copy(id = nextId(), children = clipboard.children.map { it.copy(id = nextId()) })
        val newRoot = BuilderUiState.insert(st.root, parentId, paste)
        pushHistory(newRoot)
        _state.update { it.copy(selectedId = paste.id, message = "Pasted - Professional") }
    }

    fun moveSelected(direction: Int) {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            val newRoot = BuilderUiState.move(st.root, id, direction)
            st.copy(
                root = newRoot,
                history = st.history.take(st.historyIndex + 1) + newRoot,
                historyIndex = st.historyIndex + 1,
            )
        }
    }

    fun moveSelectedToParent(parentId: String) {
        val st = _state.value
        val id = st.selectedId ?: return
        if (id == parentId) return
        val selected = BuilderUiState.find(st.root, id) ?: return
        // Check not moving into own descendant
        if (BuilderUiState.find(selected, parentId) != null) return
        var newRoot = BuilderUiState.remove(st.root, id)
        newRoot = BuilderUiState.insert(newRoot, parentId, selected)
        pushHistory(newRoot)
    }

    fun setProp(key: String, value: String) {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            val newRoot = BuilderUiState.replace(st.root, id) { c ->
                c.copy(props = if (value.isEmpty()) c.props - key else c.props + (key to value))
            }
            st.copy(root = newRoot)
        }
    }

    fun setConstraint(key: String, value: String) {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            val newRoot = BuilderUiState.replace(st.root, id) { c ->
                c.copy(constraints = if (value.isEmpty()) c.constraints - key else c.constraints + (key to value))
            }
            st.copy(root = newRoot)
        }
    }

    fun setMode(mode: BuilderMode) = _state.update { it.copy(mode = mode) }
    fun togglePalette() = _state.update { it.copy(paletteOpen = !it.paletteOpen) }
    fun toggleProperties() = _state.update { it.copy(propertiesOpen = !it.propertiesOpen) }
    fun toggleLayers() = _state.update { it.copy(layersOpen = !it.layersOpen) }
    fun toggleGrid() = _state.update { it.copy(showGrid = !it.showGrid) }
    fun toggleConstraints() = _state.update { it.copy(showConstraints = !it.showConstraints) }
    fun setExportTarget(path: String) = _state.update { it.copy(exportTarget = path) }
    fun setDevicePreview(device: DevicePreview) = _state.update { it.copy(devicePreview = device) }
    fun setThemePreview(theme: ThemePreview) = _state.update { it.copy(themePreview = theme) }
    fun setZoom(zoom: Float) = _state.update { it.copy(zoom = zoom.coerceIn(0.25f, 3f)) }
    fun setSearchQuery(query: String) = _state.update { it.copy(searchQuery = query) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun undo() {
        _state.update { st ->
            if (!st.canUndo) return@update st
            val newIndex = st.historyIndex - 1
            st.copy(root = st.history[newIndex], historyIndex = newIndex, selectedId = null)
        }
    }

    fun redo() {
        _state.update { st ->
            if (!st.canRedo) return@update st
            val newIndex = st.historyIndex + 1
            st.copy(root = st.history[newIndex], historyIndex = newIndex, selectedId = null)
        }
    }

    fun clearCanvas() {
        val newRoot = UiComponent("root", ComponentType.COLUMN)
        pushHistory(newRoot)
        _state.update { it.copy(selectedId = null, message = "Canvas cleared - Professional") }
    }

    fun composeCode(): String = ComposeCodeGenerator.generate(_state.value.root)
    fun xmlCode(): String = XmlCodeGenerator.generate(_state.value.root)

    fun loadXml(xml: String) {
        val parsed = LayoutParser.parse(xml)
        if (parsed != null) {
            val newRoot = parsed.copy(id = "root")
            pushHistory(newRoot)
            _state.update { it.copy(selectedId = null) }
        } else {
            _state.update { it.copy(message = "Could not parse layout - Professional parser") }
        }
    }

    fun exportToProject() {
        val st = _state.value
        val project = st.project ?: run {
            _state.update { it.copy(message = "No project open - Open a project first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(exporting = true) }
            val isXml = st.exportTarget.endsWith(".xml")
            val content = if (isXml) xmlCode() else composeCode()
            val ok = projects.createFile(project, st.exportTarget, content) ||
                projects.writeFile(project, st.exportTarget, content)
            _state.update {
                it.copy(
                    exporting = false,
                    message = if (ok) "Exported to ${st.exportTarget} - Professional" else "Export failed",
                )
            }
        }
    }

    fun generateProfessionalScreen(): String {
        return composeCode()
    }

    fun getComponentCount(): Int = BuilderUiState.countComponents(_state.value.root)
    fun getFlattened(): List<UiComponent> = BuilderUiState.flatten(_state.value.root)
}
