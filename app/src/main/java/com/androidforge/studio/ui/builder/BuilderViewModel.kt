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

enum class BuilderMode { DESIGN, CODE_COMPOSE, CODE_XML, PREVIEW }

data class BuilderUiState(
    val project: Project? = null,
    val root: UiComponent = UiComponent("root", ComponentType.COLUMN),
    val selectedId: String? = null,
    val mode: BuilderMode = BuilderMode.DESIGN,
    val paletteOpen: Boolean = true,
    val propertiesOpen: Boolean = false,
    val message: String? = null,
    val exportTarget: String = "app/src/main/java/generated/GeneratedScreen.kt",
    val exporting: Boolean = false,
) {
    val selected: UiComponent?
        get() = selectedId?.let { id -> find(root, id) }

    companion object {
        fun find(node: UiComponent, id: String): UiComponent? {
            if (node.id == id) return node
            for (c in node.children) find(c, id)?.let { return it }
            return null
        }

        /** Replaces the node with [id] using [transform]. */
        fun replace(node: UiComponent, id: String, transform: (UiComponent) -> UiComponent): UiComponent {
            if (node.id == id) return transform(node)
            return node.copy(children = node.children.map { replace(it, id, transform) })
        }

        fun remove(node: UiComponent, id: String): UiComponent =
            node.copy(children = node.children.filterNot { it.id == id }
                .map { remove(it, id) })

        /** Inserts [child] as last child of [parentId] (or as root sibling when parentId == root). */
        fun insert(node: UiComponent, parentId: String, child: UiComponent): UiComponent {
            if (node.id == parentId && node.type.isContainer) {
                return node.copy(children = node.children + child)
            }
            return node.copy(children = node.children.map { insert(it, parentId, child) })
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

    fun openProject(projectId: Long) {
        if (_state.value.project?.id == projectId) return
        viewModelScope.launch {
            val project = projects.getProject(projectId)
            _state.update { it.copy(project = project) }
        }
    }

    fun addToParent(type: ComponentType) {
        _state.update { st ->
            val parentId = st.selectedId?.takeIf { id ->
                BuilderUiState.find(st.root, id)?.type?.isContainer == true
            } ?: st.root.id
            val child = newComponent(type, nextId())
            st.copy(
                root = BuilderUiState.insert(st.root, parentId, child),
                selectedId = child.id,
                message = "Added ${type.displayName}",
            )
        }
    }

    fun select(id: String?) = _state.update { it.copy(selectedId = id, propertiesOpen = id != null) }

    fun deleteSelected() {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            if (id == st.root.id) return@update st.copy(message = "Cannot delete root")
            st.copy(
                root = BuilderUiState.remove(st.root, id),
                selectedId = null,
                propertiesOpen = false,
                message = "Deleted",
            )
        }
    }

    fun moveSelected(direction: Int) {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            st.copy(root = BuilderUiState.move(st.root, id, direction))
        }
    }

    fun setProp(key: String, value: String) {
        _state.update { st ->
            val id = st.selectedId ?: return@update st
            st.copy(
                root = BuilderUiState.replace(st.root, id) { c ->
                    c.copy(props = if (value.isEmpty()) c.props - key else c.props + (key to value))
                },
            )
        }
    }

    fun setMode(mode: BuilderMode) = _state.update { it.copy(mode = mode) }
    fun togglePalette() = _state.update { it.copy(paletteOpen = !it.paletteOpen) }
    fun toggleProperties() = _state.update { it.copy(propertiesOpen = !it.propertiesOpen) }
    fun setExportTarget(path: String) = _state.update { it.copy(exportTarget = path) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun composeCode(): String = ComposeCodeGenerator.generate(_state.value.root)
    fun xmlCode(): String = XmlCodeGenerator.generate(_state.value.root)

    fun loadXml(xml: String) {
        val parsed = LayoutParser.parse(xml)
        if (parsed != null) {
            _state.update { it.copy(root = parsed.copy(id = "root"), selectedId = null) }
        } else {
            _state.update { it.copy(message = "Could not parse layout") }
        }
    }

    fun exportToProject() {
        val st = _state.value
        val project = st.project ?: run {
            _state.update { it.copy(message = "No project open") }
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
                    message = if (ok) "Exported to ${st.exportTarget}" else "Export failed",
                )
            }
        }
    }
}
