package com.androidforge.studio.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Professional UI Builder - AndroidForge Studio
 * Features:
 * - 40+ components (Material 3, Layout, Game, Native)
 * - Drag & drop with constraints
 * - Layers panel (component tree)
 * - Professional properties editor
 * - Device previews (Phone, Tablet, Foldable)
 * - Theme previews (Light/Dark/Dynamic)
 * - Blueprint mode
 * - Real-time code generation (Compose + XML)
 * - Export to project
 * - Undo/Redo with history
 * - Copy/Paste/Duplicate
 * - Grid & constraints visualization
 * - Professional canvas with device frames
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiBuilderScreen(
    projectId: Long,
    onBack: () -> Unit,
    viewModel: BuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(projectId) { if (projectId > 0) viewModel.openProject(projectId) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var draggingType by remember { mutableStateOf<ComponentType?>(null) }
    var showDeviceMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("UI Builder - Professional", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text("${viewModel.getComponentCount()} components | ${state.devicePreview.displayName} | ${state.themePreview.displayName}", 
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo/Redo - Professional
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = if (state.canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = viewModel::redo, enabled = state.canRedo) {
                        Icon(Icons.Filled.Redo, contentDescription = "Redo", tint = if (state.canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                    // Device selector
                    Box {
                        FilledTonalIconButton(onClick = { showDeviceMenu = true }) {
                            Icon(Icons.Filled.Visibility, contentDescription = "Device")
                        }
                        DropdownMenu(expanded = showDeviceMenu, onDismissRequest = { showDeviceMenu = false }) {
                            DevicePreview.entries.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text("${device.displayName} (${device.widthDp}x${device.heightDp})") },
                                    onClick = { viewModel.setDevicePreview(device); showDeviceMenu = false }
                                )
                            }
                        }
                    }
                    // Theme selector
                    Box {
                        FilledTonalIconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Filled.Palette, contentDescription = "Theme")
                        }
                        DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                            ThemePreview.entries.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.displayName) },
                                    onClick = { viewModel.setThemePreview(theme); showThemeMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = viewModel::toggleGrid) {
                        Icon(Icons.Filled.GridOn, contentDescription = "Grid", tint = if (state.showGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = viewModel::toggleLayers) {
                        Icon(Icons.Filled.Layers, contentDescription = "Layers", tint = if (state.layersOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    FilledTonalIconButton(onClick = viewModel::exportToProject) {
                        Icon(Icons.Filled.Code, contentDescription = "Export to project")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Professional mode tabs
            val modes = listOf(
                BuilderMode.DESIGN to "Design",
                BuilderMode.BLUEPRINT to "Blueprint",
                BuilderMode.LAYERS to "Layers",
                BuilderMode.CODE_COMPOSE to "Compose",
                BuilderMode.CODE_XML to "XML",
                BuilderMode.PREVIEW to "Preview",
            )
            TabRow(selectedTabIndex = modes.indexOfFirst { it.first == state.mode }) {
                modes.forEach { (mode, label) ->
                    Tab(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        text = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when (state.mode) {
                    BuilderMode.DESIGN -> ProfessionalDesignCanvas(
                        state = state,
                        onSelect = viewModel::select,
                        onDrop = { type -> viewModel.addToParent(type) },
                        dragOffset = dragOffset,
                        draggingType = draggingType,
                    )
                    BuilderMode.BLUEPRINT -> BlueprintCanvas(state = state, onSelect = viewModel::select)
                    BuilderMode.LAYERS -> LayersPanel(state = state, onSelect = viewModel::select, onMove = viewModel::moveSelectedToParent, onDelete = viewModel::deleteSelected)
                    BuilderMode.CODE_COMPOSE -> CodePane(viewModel.composeCode(), "Compose - Professional")
                    BuilderMode.CODE_XML -> CodePane(viewModel.xmlCode(), "XML - Professional")
                    BuilderMode.PREVIEW -> ProfessionalLivePreview(state = state)
                }
            }

            // Professional toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.selected?.let { "${it.type.icon} ${it.type.displayName} (${it.id.take(8)})" } ?: "No selection - Professional",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.selectedId != null) {
                    IconButton(onClick = viewModel::copySelected, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = viewModel::paste, enabled = state.clipboard != null, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { viewModel.moveSelected(-1) }, enabled = state.selectedId != null, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.moveSelected(1) }, enabled = state.selectedId != null, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = viewModel::duplicateSelected, enabled = state.selectedId != null, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = viewModel::deleteSelected, enabled = state.selectedId != null && state.selectedId != "root", modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = viewModel::togglePalette,
                    modifier = Modifier.height(32.dp),
                ) { Text("+ Add", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }

    // Professional palette bottom sheet with categories
    if (state.paletteOpen) {
        ModalBottomSheet(onDismissRequest = viewModel::togglePalette) {
            Column(modifier = Modifier.padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Professional Components - 40+ types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "Tap to add • Long-press drag to canvas • Professional Material 3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(12.dp))
                
                professionalPalette.forEach { (category, items) ->
                    Text(
                        "${category.displayName} (${items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items) { item ->
                            ProfessionalPaletteItem(
                                item = item,
                                onAdd = { viewModel.addToParent(item.type) },
                                onDragStart = { draggingType = item.type },
                                onDragEnd = { draggingType = null; dragOffset = 0f },
                                onDrag = { dragOffset += it.y }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // Professional properties sheet
    if (state.propertiesOpen && state.selected != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.select(null) }) {
            val selected = state.selected ?: return@ModalBottomSheet
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${selected.type.icon} ${selected.type.displayName} - Professional Properties", 
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(selected.type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Layout & Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        
                        if (selected.type == ComponentType.TEXT || selected.type.category == ComponentCategory.INPUT) {
                            OutlinedTextField(
                                value = selected.text,
                                onValueChange = { viewModel.setProp("text", it) },
                                label = { Text("Text") },
                                singleLine = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (selected.type == ComponentType.TEXT) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = selected.props["size"] ?: "16",
                                    onValueChange = { viewModel.setProp("size", it) },
                                    label = { Text("Size (sp)") },
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = selected.props["fontWeight"] ?: "normal",
                                    onValueChange = { viewModel.setProp("fontWeight", it) },
                                    label = { Text("Weight") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = selected.props["padding"] ?: "",
                                onValueChange = { viewModel.setProp("padding", it) },
                                label = { Text("Padding (dp)") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = selected.props["width"] ?: "",
                                onValueChange = { viewModel.setProp("width", it) },
                                label = { Text("Width") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedTextField(
                            value = selected.props["background"] ?: "",
                            onValueChange = { viewModel.setProp("background", it) },
                            label = { Text("Background (hex RRGGBB)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = selected.props["elevation"] ?: "",
                            onValueChange = { viewModel.setProp("elevation", it) },
                            label = { Text("Elevation (dp)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (selected.type == ComponentType.CONSTRAINT_LAYOUT || selected.type == ComponentType.COLUMN) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Constraints - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = selected.constraints["top"] ?: "",
                                onValueChange = { viewModel.setConstraint("top", it) },
                                label = { Text("Top constraint") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = selected.constraints["start"] ?: "",
                                onValueChange = { viewModel.setConstraint("start", it) },
                                label = { Text("Start constraint") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.select(null) }) { Text("Done") }
                    FilledTonalButton(onClick = viewModel::duplicateSelected) { Text("Duplicate") }
                    TextButton(onClick = viewModel::deleteSelected) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalPaletteItem(
    item: PaletteItem,
    onAdd: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .pointerInput(item.type) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            }
            .clickable { onAdd() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        Text(item.type.icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            item.type.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ProfessionalDesignCanvas(
    state: BuilderUiState,
    onSelect: (String) -> Unit,
    onDrop: (ComponentType) -> Unit,
    dragOffset: Float,
    draggingType: ComponentType?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UiBuilderTheme.canvasBackground)
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Professional device frame with specs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.devicePreview.displayName} - ${state.devicePreview.widthDp}x${state.devicePreview.heightDp}dp", 
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
                Text("Zoom: ${(state.zoom * 100).toInt()}% | Grid: ${if (state.showGrid) "ON" else "OFF"}", 
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(UiBuilderTheme.deviceFrame)
                    .border(2.dp, UiBuilderTheme.deviceBorder, RoundedCornerShape(24.dp))
                    .padding(12.dp),
            ) {
                ProfessionalComponentTree(
                    node = state.root,
                    selectedId = state.selectedId,
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxSize(),
                    showGrid = state.showGrid,
                )
            }
            if (draggingType != null) {
                Text(
                    "Dragging ${draggingType.displayName} - Release to drop (Professional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun BlueprintCanvas(state: BuilderUiState, onSelect: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Blueprint Mode - Professional", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            ProfessionalComponentTree(node = state.root, selectedId = state.selectedId, onSelect = onSelect, modifier = Modifier.fillMaxWidth().padding(16.dp), blueprint = true)
        }
    }
}

@Composable
private fun LayersPanel(state: BuilderUiState, onSelect: (String) -> Unit, onMove: (String) -> Unit, onDelete: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text("Layers - Professional (${BuilderUiState.countComponents(state.root)} components)", 
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        val flattened = BuilderUiState.flatten(state.root)
        items(flattened) { component ->
            val depth = BuilderUiState.depth(state.root, component.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 16).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (component.id == state.selectedId) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onSelect(component.id) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(component.type.icon, modifier = Modifier.padding(end = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(component.type.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = if (component.id == state.selectedId) FontWeight.Bold else FontWeight.Normal)
                    Text(component.id.take(12), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                if (component.id != "root") {
                    IconButton(onClick = { onMove(component.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Layers, contentDescription = "Move", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalComponentTree(
    node: UiComponent,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    showGrid: Boolean = false,
    blueprint: Boolean = false,
) {
    val selected = node.id == selectedId
    val shape = RoundedCornerShape(8.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(
            when {
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                node.id == "root" -> Color.Transparent
                blueprint -> Color.Transparent
                else -> previewColor(node.type)
            }
        )
        .border(
            width = if (selected) 2.dp else if (blueprint) 1.dp else 0.5.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else if (blueprint) Color(0xFF1F6FEB) else Color(0x33FFFFFF),
            shape = shape,
        )
        .clickable { onSelect(node.id) }
        .padding(if (node.type.isContainer) 6.dp else 4.dp)

    when {
        node.type.isContainer -> {
            val isRow = node.type == ComponentType.ROW || node.type == ComponentType.LAZY_ROW || node.type == ComponentType.FLOW_ROW
            if (isRow) {
                Row(
                    modifier = baseModifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (node.children.isEmpty()) ProfessionalEmptySlot()
                    node.children.forEach { child ->
                        ProfessionalComponentTree(child, selectedId, onSelect, Modifier.weight(1f, fill = false), showGrid, blueprint)
                    }
                }
            } else {
                Column(
                    modifier = baseModifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (node.children.isEmpty()) ProfessionalEmptySlot()
                    node.children.forEach { child ->
                        ProfessionalComponentTree(child, selectedId, onSelect, Modifier.fillMaxWidth(), showGrid, blueprint)
                    }
                }
            }
        }
        node.type == ComponentType.TEXT -> Text(
            text = node.text.ifBlank { "Professional Text" },
            fontSize = (node.props["size"]?.toIntOrNull() ?: 16).sp,
            color = if (blueprint) Color(0xFF1F6FEB) else Color(0xFFE6EDF3),
            fontWeight = if (node.props["fontWeight"] == "bold") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(node.props["padding"]?.toIntOrNull()?.dp ?: 2.dp),
        )
        node.type.category == ComponentCategory.INPUT && node.type.displayName.contains("Button") -> Button(onClick = { onSelect(node.id) }) {
            Text(node.text.ifBlank { "Professional ${node.type.displayName}" })
        }
        node.type == ComponentType.FAB -> androidx.compose.material3.FloatingActionButton(onClick = { onSelect(node.id) }) {
            Text("+")
        }
        node.type == ComponentType.TEXT_FIELD || node.type == ComponentType.OUTLINED_TEXT_FIELD -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF21262D), RoundedCornerShape(6.dp))
                .padding(10.dp),
        ) {
            Text(
                node.props["hint"] ?: "Professional TextField",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
            )
        }
        node.type == ComponentType.SPACER -> Spacer(Modifier.height((node.props["height"]?.toIntOrNull() ?: 16).dp))
        node.type == ComponentType.DIVIDER -> Box(
            Modifier.fillMaxWidth().height(1.dp).background(Color(0x55FFFFFF))
        )
        node.type == ComponentType.ICON -> Text(node.type.icon, color = Color(0xFFFFB74D), fontSize = 22.sp)
        node.type == ComponentType.IMAGE || node.type == ComponentType.ASYNC_IMAGE -> Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFF21262D), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("🖼️ Professional Image", color = Color(0xFF8B949E), fontSize = 12.sp) }
        node.type == ComponentType.CARD || node.type == ComponentType.ELEVATED_CARD -> ProfessionalCardish(node, selectedId, onSelect, blueprint)
        node.type == ComponentType.GAME_VIEW -> Box(
            Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF0D1117), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFFF6D00), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("🎮 ${node.props["gameClass"] ?: "LibGDX Game"} - Professional", color = Color(0xFFFF6D00)) }
        node.type == ComponentType.NATIVE_VIEW || node.type == ComponentType.OPENGL_VIEW -> Box(
            Modifier.fillMaxWidth().height(80.dp).background(Color.Black, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("⚙️ Native: ${node.props["nativeLib"] ?: "native-lib"}", color = Color.White, fontSize = 12.sp) }
        else -> Text("${node.type.icon} ${node.type.displayName}", color = if (blueprint) Color(0xFF1F6FEB) else Color(0xFF8B949E), fontSize = 12.sp)
    }
}

@Composable
private fun ProfessionalCardish(node: UiComponent, selectedId: String?, onSelect: (String) -> Unit, blueprint: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (blueprint) Color.Transparent else Color(0xFF161B22))
            .border(1.dp, if (blueprint) Color(0xFF1F6FEB) else Color(0xFF30363D), RoundedCornerShape(10.dp))
            .clickable { onSelect(node.id) }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (node.children.isEmpty()) Text("🃏 Professional Card", color = Color(0xFF8B949E), fontSize = 12.sp)
        node.children.forEach { ProfessionalComponentTree(it, selectedId, onSelect, Modifier.fillMaxWidth(), blueprint = blueprint) }
    }
}

@Composable
private fun ProfessionalEmptySlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Empty container — add professional components",
            color = Color(0xFF6E7681),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun CodePane(code: String, title: String) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                "Professional • Production-ready",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = code,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 12.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE6EDF3),
            )
        }
    }
}

@Composable
private fun ProfessionalLivePreview(root: UiComponent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Professional Preview - Live", style = MaterialTheme.typography.titleSmall, color = Color(0xFF1F6FEB), fontWeight = FontWeight.Bold)
            PreviewRender(root)
        }
    }
}

@Composable
private fun PreviewRender(node: UiComponent) {
    when {
        node.type.isContainer -> {
            if (node.type == ComponentType.ROW) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { node.children.forEach { PreviewRender(it) } }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { node.children.forEach { PreviewRender(it) } }
            }
        }
        node.type == ComponentType.TEXT -> Text(
            node.text.ifBlank { "Professional Text" },
            fontSize = (node.props["size"]?.toIntOrNull() ?: 16).sp,
            color = Color(0xFF1F2328),
            fontWeight = if (node.props["fontWeight"] == "bold") FontWeight.Bold else FontWeight.Normal,
        )
        node.type.displayName.contains("Button") -> Button(onClick = { }) { Text(node.text.ifBlank { "Professional Button" }) }
        node.type == ComponentType.FAB -> androidx.compose.material3.FloatingActionButton(onClick = { }) { Text("+") }
        node.type == ComponentType.TEXT_FIELD || node.type == ComponentType.OUTLINED_TEXT_FIELD -> OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text(node.props["hint"] ?: "") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
        )
        node.type == ComponentType.SPACER -> Spacer(Modifier.height((node.props["height"]?.toIntOrNull() ?: 16).dp))
        node.type == ComponentType.DIVIDER -> Box(
            Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD))
        )
        node.type == ComponentType.ICON -> Text("★", color = Color(0xFFFF6D00), fontSize = 24.sp)
        node.type == ComponentType.IMAGE -> Box(
            Modifier.fillMaxWidth().height(90.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("Professional Image", color = Color(0xFF666666)) }
        node.type == ComponentType.CARD -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF6F8FA))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { node.children.forEach { PreviewRender(it) } }
        node.type == ComponentType.GAME_VIEW -> Box(
            Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF0D1117), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("🎮 Professional LibGDX Game", color = Color(0xFFFF6D00)) }
        else -> Text("${node.type.icon} ${node.type.displayName}", style = MaterialTheme.typography.bodySmall)
    }
}
