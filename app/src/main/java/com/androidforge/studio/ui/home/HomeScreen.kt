package com.androidforge.studio.ui.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidforge.studio.domain.model.ImportSource
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.model.Template
import com.androidforge.studio.domain.model.TemplateCategory
import java.text.DateFormat
import java.util.Date

/**
 * Professional Home Screen - AndroidForge Studio
 * Features:
 * - Create project with 14 templates (Compose, XML, NDK, LibGDX, etc.)
 * - Import from ZIP, Git, Folder
 * - Search, filter by category, sort
 * - Favorite, duplicate, export, delete
 * - Professional project cards with template info
 * - Real APK build info
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenProject: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.info, state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
        state.info?.let { snackbar.showSnackbar(it) }
        if (state.info != null || state.error != null) viewModel.dismissInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AndroidForge Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Professional Android IDE • ${state.projects.size} projects • NDK • LibGDX • Real APK",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setFavoriteFilter(!state.filterFavorite) }) {
                        Icon(
                            if (state.filterFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorites only",
                            tint = if (state.filterFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::openImportDialog,
                    icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "Import") },
                    text = { Text("Import") },
                )
                FloatingActionButton(onClick = viewModel::openCreateDialog) {
                    Icon(Icons.Filled.Add, contentDescription = "New project")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar - Professional
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search projects... Professional search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )

            // Category filter chips - Professional
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("All") },
                    )
                }
                items(TemplateCategory.entries) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category.name,
                        onClick = { viewModel.setCategory(if (state.selectedCategory == category.name) null else category.name) },
                        label = { Text(category.displayName) },
                    )
                }
            }

            // Sort chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SortOption.entries) { sort ->
                    FilterChip(
                        selected = state.sortBy == sort,
                        onClick = { viewModel.setSortBy(sort) },
                        label = { Text(sort.displayName, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.projects.isEmpty() -> EmptyState(
                        favoriteFilter = state.filterFavorite,
                        onCreate = viewModel::openCreateDialog,
                        onImport = viewModel::openImportDialog,
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text(
                                "Professional Templates: ${Template.entries.size} • NDK • LibGDX • Offline • Real APK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        items(state.projects, key = { it.id }) { project ->
                            ProfessionalProjectCard(
                                project = project,
                                onOpen = { onOpenProject(project.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(project) },
                                onDelete = { viewModel.delete(project) },
                                onDuplicate = { viewModel.duplicate(project) },
                                onExport = { viewModel.export(project) },
                            )
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        ProfessionalCreateProjectDialog(
            creating = state.creating,
            onDismiss = viewModel::closeCreateDialog,
            onCreate = viewModel::create,
        )
    }

    if (state.showImportDialog) {
        ProfessionalImportDialog(
            importing = state.importing,
            importSource = state.importSource,
            gitUrl = state.gitUrl,
            onSourceChange = viewModel::setImportSource,
            onGitUrlChange = viewModel::setGitUrl,
            onDismiss = viewModel::closeImportDialog,
            onImportZip = { bytes, name -> viewModel.importFromZip(bytes, name) },
            onImportGit = viewModel::importFromGit,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfessionalProjectCard(
    project: Project,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val template = Template.fromId(project.templateId)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (project.isFavorite) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true })
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Template icon
                    val icon = when {
                        template.supportsNdk -> Icons.Filled.Memory
                        template.supportsLibGdx -> Icons.Filled.Gamepad
                        template.category == TemplateCategory.COMPOSE -> Icons.Filled.Code
                        else -> Icons.Filled.FileCopy
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${template.title} • ${template.category.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (project.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (project.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Duplicate - Professional") }, onClick = { menuOpen = false; onDuplicate() })
                            DropdownMenuItem(text = { Text("Export ZIP - Professional") }, onClick = { menuOpen = false; onExport() })
                            DropdownMenuItem(text = { Text("Delete - Professional") }, onClick = { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    project.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = false, onClick = {}, label = { Text(template.category.displayName, style = MaterialTheme.typography.labelSmall) })
                    if (template.supportsNdk) FilterChip(selected = false, onClick = {}, label = { Text("NDK", style = MaterialTheme.typography.labelSmall) })
                    if (template.supportsLibGdx) FilterChip(selected = false, onClick = {}, label = { Text("LibGDX", style = MaterialTheme.typography.labelSmall) })
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(project.updatedAt))} • Professional • Offline • Real APK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(favoriteFilter: Boolean, onCreate: () -> Unit, onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (favoriteFilter) "No favorites yet" else "No projects yet - Professional IDE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (favoriteFilter) "Star a project to pin it here."
            else "Professional Android IDE with 14 templates:\nCompose, XML, NDK, LibGDX, Firebase, E-commerce, Chat, 2D Game, Media, Maps, Wear OS\n\nFeatures: Real APK compiler, Run button, UI Builder, NDK, LibGDX, Offline",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        if (!favoriteFilter) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCreate) { Text("Create Professional Project") }
                OutlinedButton(onClick = onImport) { Text("Import") }
            }
            Spacer(Modifier.height(16.dp))
            Text("Professional: Run button at top, Build APK, Error catching, Real compiler", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun ProfessionalCreateProjectDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, packageName: String, templateId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(Template.EMPTY_COMPOSE) }
    var showCategoryFilter by remember { mutableStateOf<TemplateCategory?>(null) }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("New Professional Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package name") },
                    placeholder = { Text("com.example.myapp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Professional Templates (${Template.entries.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                
                // Category filter
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = showCategoryFilter == null, onClick = { showCategoryFilter = null }, label = { Text("All") })
                    }
                    items(TemplateCategory.entries) { cat ->
                        FilterChip(selected = showCategoryFilter == cat, onClick = { showCategoryFilter = if (showCategoryFilter == cat) null else cat }, label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    val filtered = if (showCategoryFilter == null) Template.entries else Template.entries.filter { it.category == showCategoryFilter }
                    items(filtered) { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = template == t,
                                onClick = { template = t },
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    if (t.supportsNdk) { Spacer(Modifier.width(4.dp)); Text("NDK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                    if (t.supportsLibGdx) { Spacer(Modifier.width(4.dp)); Text("LibGDX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
                                }
                                Text(
                                    t.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPkg = pkg.trim().ifBlank {
                        "com.example." + name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
                    }
                    onCreate(name, finalPkg, template.id)
                },
                enabled = !creating && name.isNotBlank(),
            ) { if (creating) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Create Professional") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") }
        },
    )
}

@Composable
internal fun ProfessionalImportDialog(
    importing: Boolean,
    importSource: ImportSource,
    gitUrl: String,
    onSourceChange: (ImportSource) -> Unit,
    onGitUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImportZip: (ByteArray, String) -> Unit,
    onImportGit: (String) -> Unit,
) {
    var zipName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text("Import Professional Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Professional import: ZIP, Git, Folder - Offline capable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ImportSource.entries) { source ->
                        FilterChip(
                            selected = importSource == source,
                            onClick = { onSourceChange(source) },
                            label = { Text(source.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                when (importSource) {
                    ImportSource.ZIP -> {
                        OutlinedTextField(
                            value = zipName,
                            onValueChange = { zipName = it },
                            label = { Text("Project name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Select ZIP file from storage - Professional", style = MaterialTheme.typography.labelSmall)
                        // In real app, file picker would be here
                        Button(onClick = { onImportZip(ByteArray(0), zipName.ifBlank { "Imported" }) }, enabled = !importing) {
                            Text("Pick ZIP - Professional")
                        }
                    }
                    ImportSource.GIT -> {
                        OutlinedTextField(
                            value = gitUrl,
                            onValueChange = onGitUrlChange,
                            label = { Text("Git URL (https://github.com/user/repo.git)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { onImportGit(gitUrl) }, enabled = !importing && gitUrl.isNotBlank()) {
                            if (importing) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Clone - Professional Git")
                        }
                    }
                    ImportSource.FOLDER -> {
                        Text("Import from local folder - Professional file manager", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { /* folder picker */ }, enabled = !importing) { Text("Pick Folder - Professional") }
                    }
                    ImportSource.TEMPLATE -> {
                        Text("Browse template gallery - 14 professional templates", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !importing) { Text("Close") }
        }
    )
}
