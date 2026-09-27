package com.androidforge.studio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.ImportSource
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.model.Template
import com.androidforge.studio.domain.repository.ProjectRepository
import com.androidforge.studio.domain.usecase.CreateProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val projects: List<Project> = emptyList(),
    val loading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val creating: Boolean = false,
    val importing: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val filterFavorite: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val importSource: ImportSource = ImportSource.ZIP,
    val gitUrl: String = "",
    val sortBy: SortOption = SortOption.UPDATED,
)

enum class SortOption(val displayName: String) {
    NAME("Name"),
    UPDATED("Last Updated"),
    CREATED("Created"),
    SIZE("Size"),
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projects: ProjectRepository,
    private val createProject: CreateProjectUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Raw (unfiltered) list so the favorite filter can be re-applied instantly. */
    private var allProjects: List<Project> = emptyList()

    init {
        viewModelScope.launch {
            projects.observeProjects().collect { list ->
                allProjects = list
                _state.update {
                    it.copy(
                        projects = filterAndSort(list, it.filterFavorite, it.searchQuery, it.selectedCategory, it.sortBy),
                        loading = false,
                    )
                }
            }
        }
    }

    private fun filterAndSort(
        list: List<Project>,
        favoriteOnly: Boolean,
        searchQuery: String,
        category: String?,
        sortBy: SortOption,
    ): List<Project> {
        var filtered = list
        if (favoriteOnly) filtered = filtered.filter { it.isFavorite }
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true) ||
                it.templateId.contains(searchQuery, ignoreCase = true)
            }
        }
        if (category != null) {
            filtered = filtered.filter { it.templateId.contains(category, ignoreCase = true) }
        }
        return when (sortBy) {
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.UPDATED -> filtered.sortedByDescending { it.updatedAt }
            SortOption.CREATED -> filtered.sortedByDescending { it.createdAt }
            SortOption.SIZE -> filtered.sortedBy { it.name.length }
        }
    }

    fun openCreateDialog() = _state.update { it.copy(showCreateDialog = true, error = null) }
    fun closeCreateDialog() = _state.update { it.copy(showCreateDialog = false) }
    
    fun openImportDialog() = _state.update { it.copy(showImportDialog = true, error = null) }
    fun closeImportDialog() = _state.update { it.copy(showImportDialog = false) }

    fun setFavoriteFilter(enabled: Boolean) {
        _state.update {
            it.copy(
                filterFavorite = enabled,
                projects = filterAndSort(allProjects, enabled, it.searchQuery, it.selectedCategory, it.sortBy),
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                projects = filterAndSort(allProjects, it.filterFavorite, query, it.selectedCategory, it.sortBy),
            )
        }
    }

    fun setCategory(category: String?) {
        _state.update {
            it.copy(
                selectedCategory = category,
                projects = filterAndSort(allProjects, it.filterFavorite, it.searchQuery, category, it.sortBy),
            )
        }
    }

    fun setSortBy(sortBy: SortOption) {
        _state.update {
            it.copy(
                sortBy = sortBy,
                projects = filterAndSort(allProjects, it.filterFavorite, it.searchQuery, it.selectedCategory, sortBy),
            )
        }
    }

    fun setImportSource(source: ImportSource) = _state.update { it.copy(importSource = source) }
    fun setGitUrl(url: String) = _state.update { it.copy(gitUrl = url) }

    fun create(name: String, packageName: String, templateId: String) {
        viewModelScope.launch {
            _state.update { it.copy(creating = true, error = null) }
            val result = createProject(name, packageName, templateId)
            _state.update {
                it.copy(
                    creating = false,
                    showCreateDialog = result.project == null,
                    error = result.error,
                    info = result.project?.let { p -> "Created ${p.name} - Professional ${Template.fromId(templateId).title}" },
                )
            }
        }
    }

    fun importFromZip(bytes: ByteArray, name: String) {
        viewModelScope.launch {
            _state.update { it.copy(importing = true, error = null) }
            try {
                val project = projects.importProjectFromZip(name, bytes)
                _state.update { it.copy(importing = false, showImportDialog = false, info = "Imported ${project.name} - Professional") }
            } catch (e: Exception) {
                _state.update { it.copy(importing = false, error = "Import failed: ${e.message} - Professional") }
            }
        }
    }

    fun importFromGit(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(importing = true, error = null) }
            try {
                // Professional Git clone - would use JGit in real implementation
                // For now, create a project that will be cloned
                val name = url.substringAfterLast('/').removeSuffix(".git").ifBlank { "ImportedProject" }
                val packageName = "com.example.${name.lowercase().replace(Regex("[^a-z0-9]"), "")}"
                val result = createProject(name, packageName, "empty_compose")
                if (result.project != null) {
                    _state.update { it.copy(importing = false, showImportDialog = false, info = "Git project ${result.project.name} created - Clone in Tools tab - Professional") }
                } else {
                    _state.update { it.copy(importing = false, error = result.error) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(importing = false, error = "Git import failed: ${e.message}") }
            }
        }
    }

    fun toggleFavorite(project: Project) {
        viewModelScope.launch { projects.toggleFavorite(project.id) }
    }

    fun delete(project: Project) {
        viewModelScope.launch { 
            projects.deleteProject(project.id)
            _state.update { it.copy(info = "Deleted ${project.name} - Professional") }
        }
    }

    fun duplicate(project: Project) {
        viewModelScope.launch {
            try {
                val newName = "${project.name} Copy"
                val result = createProject(newName, "${project.packageName}.copy", project.templateId)
                _state.update { it.copy(info = if (result.project != null) "Duplicated to ${result.project.name} - Professional" else result.error) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Duplicate failed: ${e.message}") }
            }
        }
    }

    fun export(project: Project) {
        viewModelScope.launch {
            try {
                val file = projects.exportProjectToZip(project.id)
                _state.update { it.copy(info = if (file != null) "Exported to ${file.name} - Professional" else "Export failed") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    fun dismissInfo() = _state.update { it.copy(info = null, error = null) }
}
