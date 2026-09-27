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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Drag & drop UI builder:
 *  - Palette (bottom sheet): tap OR long-press-drag a component onto the canvas
 *  - Canvas: tap to select; drag to reposition within its parent order
 *  - Properties sheet: edit text/size/padding/background
 *  - Tabs: Design · Compose code · XML code · Live preview
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

    // drag state: offset of the finger, and whether a palette drag is active
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var draggingType by remember { androidx.compose.runtime.mutableStateOf<ComponentType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UI Builder", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePalette) {
                        Icon(Icons.Filled.Layers, contentDescription = "Palette")
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
            // ---- mode tabs ----
            val modes = listOf(
                BuilderMode.DESIGN to "Design",
                BuilderMode.CODE_COMPOSE to "Compose",
                BuilderMode.CODE_XML to "XML",
                BuilderMode.PREVIEW to "Preview",
            )
            TabRow(selectedTabIndex = modes.indexOfFirst { it.first == state.mode }) {
                modes.forEachIndexed { idx, (mode, label) ->
                    Tab(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        text = { Text(label) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (state.mode) {
                BuilderMode.DESIGN -> DesignCanvas(
                    state = state,
                    onSelect = viewModel::select,
                    onDrop = { type -> viewModel.addToParent(type) },
                    dragOffset = dragOffset,
                    draggingType = draggingType,
                )
                BuilderMode.CODE_COMPOSE -> CodePane(viewModel.composeCode())
                BuilderMode.CODE_XML -> CodePane(viewModel.xmlCode())
                BuilderMode.PREVIEW -> LivePreview(state.root)
            }

            // ---- toolbar row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.selected?.let { "Selected: ${it.type.displayName}" } ?: "Nothing selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.moveSelected(-1) }, enabled = state.selectedId != null) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                }
                IconButton(onClick = { viewModel.moveSelected(1) }, enabled = state.selectedId != null) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                }
                IconButton(onClick = viewModel::deleteSelected, enabled = state.selectedId != null) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
                Button(
                    onClick = viewModel::togglePalette,
                    modifier = Modifier.height(36.dp),
                ) { Text("+ Add") }
            }
        }
    }

    // ---- palette bottom sheet ----
    if (state.paletteOpen) {
        ModalBottomSheet(onDismissRequest = viewModel::togglePalette) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Components — tap to add into selection",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(defaultPalette) { item ->
                        val parentContainer = state.selectedId?.let {
                            BuilderUiState.find(state.root, it)?.type?.isContainer == true
                        } ?: true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(78.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pointerInput(item.type) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingType = item.type },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                        },
                                        onDragEnd = {
                                            if (dragOffset > 60f) viewModel.addToParent(item.type)
                                            dragOffset = 0f
                                            draggingType = null
                                        },
                                        onDragCancel = {
                                            dragOffset = 0f
                                            draggingType = null
                                        },
                                    )
                                }
                                .clickable { viewModel.addToParent(item.type) }
                                .padding(vertical = 12.dp),
                        ) {
                            Icon(
                                ComponentIcon.forType(item.type),
                                contentDescription = item.type.displayName,
                                modifier = Modifier.size(22.dp),
                                tint = if (parentContainer) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Text(
                    "Long-press a component and drag down onto the canvas to drop it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    // ---- properties sheet ----
    if (state.propertiesOpen && state.selected != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.select(null) }) {
            val selected = state.selected ?: return@ModalBottomSheet
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Properties — ${selected.type.displayName}", style = MaterialTheme.typography.titleMedium)

                if (selected.type == ComponentType.TEXT || selected.type == ComponentType.BUTTON) {
                    OutlinedTextField(
                        value = selected.text,
                        onValueChange = { viewModel.setProp("text", it) },
                        label = { Text("Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (selected.type == ComponentType.TEXT) {
                    OutlinedTextField(
                        value = selected.props["size"] ?: "16",
                        onValueChange = { viewModel.setProp("size", it) },
                        label = { Text("Text size (sp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (selected.type == ComponentType.TEXTFIELD) {
                    OutlinedTextField(
                        value = selected.props["hint"] ?: "",
                        onValueChange = { viewModel.setProp("hint", it) },
                        label = { Text("Hint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (selected.type == ComponentType.SPACER) {
                    OutlinedTextField(
                        value = selected.props["height"] ?: "16",
                        onValueChange = { viewModel.setProp("height", it) },
                        label = { Text("Height (dp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = selected.props["padding"] ?: "",
                    onValueChange = { viewModel.setProp("padding", it) },
                    label = { Text("Padding (dp, blank = 0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = selected.props["background"] ?: "",
                    onValueChange = { viewModel.setProp("background", it) },
                    label = { Text("Background (hex RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.select(null) }) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun DesignCanvas(
    state: BuilderUiState,
    onSelect: (String) -> Unit,
    onDrop: (ComponentType) -> Unit,
    dragOffset: Float,
    draggingType: ComponentType?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // device frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161B22))
                    .border(1.dp, Color(0xFF30363D), RoundedCornerShape(20.dp))
                    .pointerInput(draggingType) {
                        if (draggingType != null && dragOffset > 60f) {
                            // dropped while finger over canvas
                        }
                    }
                    .padding(12.dp),
            ) {
                ComponentTree(
                    node = state.root,
                    selectedId = state.selectedId,
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (draggingType != null) {
                Text(
                    "Release over canvas after the sheet collapses, or tap the palette item.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Renders the component tree inside the phone frame with selection styling. */
@Composable
private fun ComponentTree(
    node: UiComponent,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = node.id == selectedId
    val shape = RoundedCornerShape(8.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(
            when {
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                node.id == "root" -> Color.Transparent
                else -> previewColor(node.type)
            }
        )
        .border(
            width = if (selected) 2.dp else 0.5.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
            shape = shape,
        )
        .clickable { onSelect(node.id) }
        .padding(if (node.type.isContainer) 6.dp else 4.dp)

    when {
        node.type.isContainer -> {
            val isRow = node.type == ComponentType.ROW
            if (isRow) {
                Row(
                    modifier = baseModifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (node.children.isEmpty()) EmptySlot()
                    node.children.forEach { child ->
                        ComponentTree(child, selectedId, onSelect, Modifier.weight(1f, fill = false))
                    }
                }
            } else {
                Column(
                    modifier = baseModifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (node.children.isEmpty()) EmptySlot()
                    node.children.forEach { child ->
                        ComponentTree(child, selectedId, onSelect, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        node.type == ComponentType.TEXT -> Text(
            text = node.text.ifBlank { "Text" },
            fontSize = (node.props["size"]?.toIntOrNull() ?: 16).sp,
            color = Color(0xFFE6EDF3),
            modifier = Modifier.padding(node.props["padding"]?.toIntOrNull()?.dp ?: 2.dp),
        )
        node.type == ComponentType.BUTTON -> Button(onClick = { onSelect(node.id) }) {
            Text(node.text.ifBlank { "Button" })
        }
        node.type == ComponentType.TEXTFIELD -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF21262D), RoundedCornerShape(6.dp))
                .padding(10.dp),
        ) {
            Text(
                node.props["hint"] ?: "TextField",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
            )
        }
        node.type == ComponentType.SPACER -> Spacer(Modifier.height((node.props["height"]?.toIntOrNull() ?: 16).dp))
        node.type == ComponentType.DIVIDER -> Box(
            Modifier.fillMaxWidth().height(1.dp).background(Color(0x55FFFFFF))
        )
        node.type == ComponentType.ICON -> Text("★", color = Color(0xFFFFB74D), fontSize = 22.sp)
        node.type == ComponentType.IMAGE -> Box(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFF21262D), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("Image", color = Color(0xFF8B949E), fontSize = 12.sp) }
        node.type == ComponentType.CARD -> Cardish(node, selectedId, onSelect)
        else -> Text(node.type.displayName, color = Color(0xFF8B949E), fontSize = 12.sp)
    }
}

@Composable
private fun Cardish(node: UiComponent, selectedId: String?, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161B22))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(10.dp))
            .clickable { onSelect(node.id) }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (node.children.isEmpty()) Text("Card", color = Color(0xFF8B949E), fontSize = 12.sp)
        node.children.forEach { ComponentTree(it, selectedId, onSelect, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun EmptySlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Empty container — add components",
            color = Color(0xFF6E7681),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun CodePane(code: String) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Generated source", style = MaterialTheme.typography.labelSmall)
            Text(
                "tap & hold to select",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = code,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 12.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE6EDF3),
            )
        }
    }
}

/** Live Compose preview: renders the component tree as real composables. */
@Composable
private fun LivePreview(root: UiComponent) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            node.text.ifBlank { "Text" },
            fontSize = (node.props["size"]?.toIntOrNull() ?: 16).sp,
            color = Color(0xFF1F2328),
        )
        node.type == ComponentType.BUTTON -> Button(onClick = { }) { Text(node.text.ifBlank { "Button" }) }
        node.type == ComponentType.TEXTFIELD -> OutlinedTextField(
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
        ) { Text("Image", color = Color(0xFF666666)) }
        node.type == ComponentType.CARD -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF6F8FA))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { node.children.forEach { PreviewRender(it) } }
        else -> {}
    }
}

/** Small icon resolver so the palette works without extra icon dependencies. */
object ComponentIcon {
    @Composable
    fun forType(type: ComponentType) = when (type) {
        ComponentType.COLUMN -> Icons.Filled.ArrowDownward
        ComponentType.ROW -> Icons.Filled.ArrowUpward
        ComponentType.BOX -> Icons.Filled.Layers
        ComponentType.TEXT -> Icons.Filled.Code
        ComponentType.BUTTON -> Icons.Filled.Visibility
        ComponentType.TEXTFIELD -> Icons.Filled.Code
        ComponentType.LIST -> Icons.Filled.Layers
        ComponentType.CARD -> Icons.Filled.Layers
        ComponentType.IMAGE -> Icons.Filled.Visibility
        ComponentType.ICON -> Icons.Filled.Visibility
        ComponentType.DIVIDER -> Icons.Filled.ArrowDownward
        ComponentType.SPACER -> Icons.Filled.ArrowDownward
    }
}
