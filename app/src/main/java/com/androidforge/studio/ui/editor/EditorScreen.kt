package com.androidforge.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidforge.studio.domain.model.FileNode

/**
 * Editor screen: file tree (left/drawer), tab strip, code area, search bar,
 * undo/redo/save toolbar, and inline autocomplete suggestions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Long,
    onOpenBuilder: () -> Unit,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(projectId) { if (projectId > 0) viewModel.openProject(projectId) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.activePath?.substringAfterLast('/') ?: state.project?.name ?: "Editor",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleTree) {
                        Icon(
                            if (state.showTree) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                            contentDescription = "Toggle file tree",
                        )
                    }
                    IconButton(onClick = viewModel::undo, enabled = state.undoStack.isNotEmpty() || true) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = viewModel::redo) {
                        Icon(Icons.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = viewModel::save, enabled = state.dirty) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = onOpenBuilder, enabled = state.project != null) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Open UI Builder")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.project == null && !state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    if (projectId <= 0) "Open a project from the Projects tab to start editing."
                    else "Loading project…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ---- search bar ----
            if (state.searchOpen) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    placeholder = { Text("Search in file…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::toggleSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        }
                    },
                )
            }

            // ---- tab strip ----
            if (state.tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (tab in state.tabs) {
                        val active = tab.filePath == state.activePath
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.openFile(tab.filePath) }
                                .background(
                                    if (active) MaterialTheme.colorScheme.surface
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(
                                (if (tab.isDirty || (active && state.dirty)) "● " else "") + tab.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close tab",
                                modifier = Modifier
                                    .width(16.dp)
                                    .clickable { viewModel.closeTab(tab.filePath) },
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // ---- file tree ----
                if (state.showTree) {
                    Column(
                        modifier = Modifier
                            .width(210.dp)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Files", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = {
                                val dir = "app/src/main/java/new/"
                                viewModel.createDirectory(dir)
                            }, modifier = Modifier.height(28.dp)) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder",
                                    modifier = Modifier.width(18.dp))
                            }
                        }
                        val tree = state.tree
                        if (tree == null) {
                            CircularProgressIndicator(Modifier.padding(16.dp))
                        } else {
                            TreeView(
                                node = tree,
                                depth = 0,
                                activePath = state.activePath,
                                onOpen = viewModel::openFile,
                                onDelete = viewModel::deletePath,
                            )
                        }
                    }
                }

                // ---- editor column ----
                Column(modifier = Modifier.weight(1f)) {
                    if (state.truncated) {
                        Text(
                            "File truncated to 512 KB for performance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    if (state.activePath == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Select a file to edit",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            CodeEditor(
                                value = state.content,
                                onValueChange = viewModel::onContentChange,
                                language = state.tabs.firstOrNull { it.filePath == state.activePath }?.language
                                    ?: com.androidforge.studio.domain.model.CodeLanguage.TEXT,
                                fontSizeSp = 14,
                                errorLines = state.errorLines,
                                searchQuery = state.searchQuery,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // ---- autocomplete suggestion chips ----
                        if (state.suggestions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                state.suggestions.forEach { s ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.applySuggestion(s) },
                                        label = { Text(s, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                        }

                        // ---- status bar ----
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                state.activePath ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                buildString {
                                    append(state.tabs.firstOrNull { it.filePath == state.activePath }?.language?.displayName ?: "")
                                    if (state.dirty) append(" · unsaved")
                                    if (state.errorLines.isNotEmpty()) append(" · ${state.errorLines.size} issues")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.errorLines.isNotEmpty()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeView(
    node: FileNode,
    depth: Int,
    activePath: String?,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val expanded = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

    fun isExpanded(path: String): Boolean =
        expanded[path] ?: (path.count { it == '/' } < 3)

    val rows = remember(node, expanded.toMap()) {
        flattenTree(node, 0) { path -> expanded[path] ?: (path.count { it == '/' } < 3) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.node.path + ":" + it.depth }) { row ->
            if (row.node.isDirectory) {
                DirectoryRow(
                    row = row,
                    open = isExpanded(row.node.path),
                    onToggle = { expanded[row.node.path] = !isExpanded(row.node.path) },
                )
            } else {
                FileRow(row.node, row.depth, activePath, onOpen, onDelete)
            }
        }
    }
}

private data class TreeRow(val node: FileNode, val depth: Int)

/** Flattens the visible tree into rows (respects expansion state). */
private fun flattenTree(
    node: FileNode,
    depth: Int,
    isExpanded: (String) -> Boolean,
): List<TreeRow> {
    val out = mutableListOf<TreeRow>()
    fun visit(n: FileNode, d: Int) {
        if (d > 0) out += TreeRow(n, d)
        if (n.isDirectory) {
            val open = d == 0 || isExpanded(n.path)
            if (open) {
                for (c in n.children) visit(c, d + 1)
            }
        }
    }
    if (node.isDirectory && node.relativePath.isEmpty()) {
        for (c in node.children) visit(c, 0)
    } else {
        visit(node, depth)
    }
    return out
}

@Composable
private fun DirectoryRow(row: TreeRow, open: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = (8 + row.depth * 14).dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
    ) {
        Icon(
            if (open) Icons.Filled.ArrowDropDown else Icons.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.width(18.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            row.node.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FileRow(
    node: FileNode,
    depth: Int,
    activePath: String?,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val active = node.relativePath == activePath
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable { onOpen(node.relativePath) }
            .padding(start = (8 + depth * 14).dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
    ) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier.width(18.dp),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            node.name,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onDelete(node.relativePath) },
            modifier = Modifier.height(24.dp),
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                modifier = Modifier.width(14.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
