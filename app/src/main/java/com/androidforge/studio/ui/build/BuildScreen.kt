package com.androidforge.studio.ui.build

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidforge.studio.domain.model.BuildSession
import com.androidforge.studio.domain.model.BuildStage
import com.androidforge.studio.domain.model.BuildStatus
import com.androidforge.studio.domain.model.BuildTarget
import com.androidforge.studio.ui.theme.ForgeOrange
import com.androidforge.studio.ui.theme.ForgeRed
import com.androidforge.studio.ui.theme.ForgeTeal
import java.text.DateFormat
import java.util.Date

private val PendingGray = Color(0xFF8B949E)
private val LogBackground = Color(0xFF0D1117)

private val PIPELINE: List<BuildStage> = listOf(
    BuildStage.SYNC,
    BuildStage.COMPILE_RES,
    BuildStage.COMPILE_KOTLIN,
    BuildStage.DEX,
    BuildStage.PACKAGE,
    BuildStage.ZIP_ALIGN,
    BuildStage.SIGN,
    BuildStage.INSTALL,
    BuildStage.DONE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(
    projectId: Long,
    viewModel: BuildViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tabIndex by remember { mutableIntStateOf(0) }
    var projectMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (projectId > 0) viewModel.selectProject(projectId)
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }
    LaunchedEffect(state.syncResult) {
        state.syncResult?.let { snackbar.showSnackbar(it); viewModel.dismissSyncResult() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Build") },
                actions = {
                    if (state.history.isNotEmpty() && tabIndex == 1) {
                        IconButton(onClick = viewModel::clearHistory) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear history")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ---- project + target controls --------------------------------
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    FilterChip(
                        selected = state.selectedProjectId > 0,
                        onClick = { projectMenuOpen = true },
                        label = {
                            Text(
                                state.project?.name ?: "No project",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = projectMenuOpen,
                        onDismissRequest = { projectMenuOpen = false },
                    ) {
                        state.projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    projectMenuOpen = false
                                    viewModel.selectProject(p.id)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.target == BuildTarget.APK,
                    onClick = { viewModel.setTarget(BuildTarget.APK) },
                    label = { Text("APK") },
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = state.target == BuildTarget.AAB,
                    onClick = { viewModel.setTarget(BuildTarget.AAB) },
                    label = { Text("AAB") },
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = state.useCloud,
                    onClick = { viewModel.setCloud(!state.useCloud) },
                    label = { Text(if (state.useCloud) "Cloud" else "On-device") },
                )
            }

            // ---- tabs ------------------------------------------------------
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Pipeline") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("History") })
            }

            when (tabIndex) {
                0 -> PipelineTab(state, viewModel)
                1 -> HistoryPane(
                    sessions = state.history,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PipelineTab(state: BuildUiState, viewModel: BuildViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        StageStrip(state = state)

        // ---- actions -----------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = viewModel::sync,
                enabled = !state.syncing && state.selectedProjectId > 0,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (state.syncing) "Syncing…" else "Sync")
            }

            val running = state.session.status == BuildStatus.RUNNING
            if (running) {
                Button(
                    onClick = viewModel::cancelBuild,
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeRed),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = viewModel::startBuild,
                    enabled = state.selectedProjectId > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeOrange),
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("${if (state.target == BuildTarget.AAB) "Bundle" else "Build"}${if (state.useCloud) " (cloud)" else ""}")
                }
            }

            val artifact = state.session.artifactPath
            if (!artifact.isNullOrBlank() && state.session.status == BuildStatus.SUCCESS) {
                Button(onClick = { viewModel.install(artifact) }) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Install")
                }
            }

            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = viewModel::clearLog,
                enabled = state.logLines.isNotEmpty(),
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear log")
            }
        }

        if (state.syncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        OutputPane(
            logLines = state.logLines,
            artifactPath = state.session.artifactPath,
            status = state.session.status,
            onInstall = { state.session.artifactPath?.let(viewModel::install) },
            onClear = viewModel::clearLog,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Horizontal pipeline visualization: each stage lights up as the build moves
 * through it (teal = done, orange = running, red = failed, gray = pending).
 */
@Composable
private fun StageStrip(state: BuildUiState) {
    val status = state.session.status
    val last = state.lastStage
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PIPELINE.forEach { stage ->
            val color = when {
                status == BuildStatus.SUCCESS -> ForgeTeal
                status == BuildStatus.FAILED || status == BuildStatus.CANCELLED ->
                    when {
                        stage.ordinal < last.ordinal -> ForgeTeal
                        stage.ordinal == last.ordinal -> ForgeRed
                        else -> PendingGray
                    }
                status == BuildStatus.RUNNING ->
                    when {
                        stage.ordinal < last.ordinal -> ForgeTeal
                        stage.ordinal == last.ordinal -> ForgeOrange
                        else -> PendingGray
                    }
                else -> PendingGray
            }
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(color.copy(alpha = if (color == PendingGray) 0.4f else 1f), RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stageShortLabel(stage),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (color == PendingGray) PendingGray else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (status == BuildStatus.RUNNING) {
            CircularProgressIndicator(
                modifier = Modifier.padding(start = 8.dp).height(16.dp).width(16.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun stageShortLabel(stage: BuildStage): String = when (stage) {
    BuildStage.PENDING -> "pending"
    BuildStage.SYNC -> "sync"
    BuildStage.COMPILE_RES -> "res"
    BuildStage.COMPILE_KOTLIN -> "kotlin"
    BuildStage.DEX -> "dex"
    BuildStage.PACKAGE -> "pkg"
    BuildStage.ZIP_ALIGN -> "align"
    BuildStage.SIGN -> "sign"
    BuildStage.INSTALL -> "install"
    BuildStage.DONE -> "done"
    BuildStage.FAILED -> "failed"
}

/**
 * Streaming build log with auto-scroll plus the install/clear action bar.
 * The scrolling pane is `weight(1f)` inside this composable's Column so the
 * action bar is never clipped.
 */
@Composable
private fun OutputPane(
    logLines: List<String>,
    artifactPath: String?,
    status: BuildStatus,
    onInstall: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) listState.animateScrollToItem(logLines.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LogBackground, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(top = 8.dp),
    ) {
        if (logLines.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Build output will stream here.\nRun `forge doctor` in Terminal to check your toolchain.",
                    color = PendingGray,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
            ) {
                items(logLines) { line ->
                    val isError = line.contains("error", ignoreCase = true) ||
                        line.contains("failed", ignoreCase = true) ||
                        line.startsWith("e:")
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (isError) ForgeRed else ForgeTeal,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = when (status) {
                    BuildStatus.SUCCESS -> ForgeTeal
                    BuildStatus.FAILED, BuildStatus.CANCELLED -> ForgeRed
                    BuildStatus.RUNNING -> ForgeOrange
                    BuildStatus.IDLE -> PendingGray
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear, enabled = logLines.isNotEmpty()) {
                Text("Clear")
            }
            if (!artifactPath.isNullOrBlank() && status == BuildStatus.SUCCESS) {
                Button(onClick = onInstall) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Install")
                }
            }
        }
    }
}

@Composable
private fun HistoryPane(
    sessions: List<BuildSession>,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No builds yet — runs appear here.", color = PendingGray)
        }
        return
    }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(sessions) { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${s.target} · ${s.stage}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        dateFormat.format(Date(s.startedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = PendingGray,
                    )
                    if (!s.artifactPath.isNullOrBlank()) {
                        Text(
                            s.artifactPath.substringAfterLast('/'),
                            style = MaterialTheme.typography.bodySmall,
                            color = ForgeTeal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = s.status.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = when (s.status) {
                        BuildStatus.SUCCESS -> ForgeTeal
                        BuildStatus.FAILED, BuildStatus.CANCELLED -> ForgeRed
                        BuildStatus.RUNNING -> ForgeOrange
                        BuildStatus.IDLE -> PendingGray
                    },
                )
            }
        }
    }
}
