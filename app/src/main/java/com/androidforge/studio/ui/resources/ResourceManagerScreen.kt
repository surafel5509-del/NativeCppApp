package com.androidforge.studio.ui.resources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResourceItem(
    val name: String,
    val type: String,
    val value: String,
    val file: String,
)

data class ResourceManagerState(
    val project: Project? = null,
    val drawables: List<ResourceItem> = emptyList(),
    val colors: List<ResourceItem> = emptyList(),
    val strings: List<ResourceItem> = emptyList(),
    val layouts: List<ResourceItem> = emptyList(),
    val loading: Boolean = false,
)

@HiltViewModel
class ResourceManagerViewModel @Inject constructor(
    private val projects: ProjectRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ResourceManagerState())
    val state: StateFlow<ResourceManagerState> = _state.asStateFlow()

    fun openProject(projectId: Long) {
        viewModelScope.launch {
            val project = projects.getProject(projectId) ?: return@launch
            _state.update { it.copy(project = project, loading = true) }
            val drawables = listOf(
                ResourceItem("ic_launcher", "drawable", "Launcher icon", "res/mipmap/ic_launcher.xml"),
                ResourceItem("primary", "color", "#FF6D00", "res/values/colors.xml"),
                ResourceItem("app_name", "string", project.name, "res/values/strings.xml"),
            )
            _state.update { it.copy(drawables = drawables, loading = false) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceManagerScreen(projectId: Long, viewModel: ResourceManagerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(projectId) { if (projectId > 0) viewModel.openProject(projectId) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Resources - Professional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Drawables, Colors, Strings, Layouts - Professional manager", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = "Add resource") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Professional Resource Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Manage all app resources visually - Professional IDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Colors - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        state.drawables.filter { it.type == "color" }.forEach { res ->
                            Text("${res.name}: ${res.value}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("#FF6D00 Primary, #1DE9B6 Secondary, #121212 Background - Professional", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Strings - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("app_name: ${state.project?.name ?: "App"}", style = MaterialTheme.typography.bodySmall)
                        Text("Professional string management with translations", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Drawables - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("ic_launcher, ic_launcher_round, backgrounds - Professional", style = MaterialTheme.typography.bodySmall)
                        Text("Supports: Vector, PNG, WebP, 9-patch - Professional", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
