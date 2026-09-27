package com.androidforge.studio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val creating: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val filterFavorite: Boolean = false,
)

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
                        projects = if (it.filterFavorite) list.filter { p -> p.isFavorite } else list,
                        loading = false,
                    )
                }
            }
        }
    }

    fun openCreateDialog() = _state.update { it.copy(showCreateDialog = true, error = null) }
    fun closeCreateDialog() = _state.update { it.copy(showCreateDialog = false) }

    fun setFavoriteFilter(enabled: Boolean) {
        _state.update {
            it.copy(
                filterFavorite = enabled,
                projects = if (enabled) allProjects.filter { p -> p.isFavorite } else allProjects,
            )
        }
    }

    fun create(name: String, packageName: String, templateId: String) {
        viewModelScope.launch {
            _state.update { it.copy(creating = true, error = null) }
            val result = createProject(name, packageName, templateId)
            _state.update {
                it.copy(
                    creating = false,
                    showCreateDialog = result.project == null,
                    error = result.error,
                    info = result.project?.let { p -> "Created ${p.name}" },
                )
            }
        }
    }

    fun toggleFavorite(project: Project) {
        viewModelScope.launch { projects.toggleFavorite(project.id) }
    }

    fun delete(project: Project) {
        viewModelScope.launch { projects.deleteProject(project.id) }
    }

    fun dismissInfo() = _state.update { it.copy(info = null, error = null) }
}
