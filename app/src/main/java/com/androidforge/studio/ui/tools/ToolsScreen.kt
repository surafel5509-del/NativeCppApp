package com.androidforge.studio.ui.tools

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidforge.studio.domain.model.GitDiffHunk
import com.androidforge.studio.domain.model.TerminalLine

/**
 * Tools hub: Terminal · Git · Diff · Debug (crash analyzer + build tasks).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(viewModel: ToolsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools") },
                actions = {
                    state.projects.forEach { p ->
                        FilterChip(
                            selected = p.id == state.selectedProjectId,
                            onClick = { viewModel.selectProject(p.id) },
                            label = { Text(p.name, maxLines = 1, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.projects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Create a project to use the tools", color = MaterialTheme.colorScheme.outline)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                ToolTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    ToolTab.TERMINAL -> "Terminal"
                                    ToolTab.GIT -> "Git"
                                    ToolTab.DIFF -> "Diff"
                                    ToolTab.DEBUG -> "Debug"
                                },
                                maxLines = 1,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (state.tab) {
                ToolTab.TERMINAL -> TerminalTab(state, viewModel)
                ToolTab.GIT -> GitTab(state, viewModel)
                ToolTab.DIFF -> DiffTab(state)
                ToolTab.DEBUG -> DebugTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun TerminalTab(state: ToolsUiState, viewModel: ToolsViewModel) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.terminalLines.size) {
        if (state.terminalLines.isNotEmpty()) listState.scrollToItem(state.terminalLines.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // build tasks contributed by plugins
        if (state.buildTasks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.buildTasks.forEach { task ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.runBuildTask(task) },
                        label = { Text("▶ ${task.name}", fontSize = 11.sp) },
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0D1117))
                .padding(8.dp),
        ) {
            items(state.terminalLines) { line ->
                Text(
                    line.text,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = when (line.kind) {
                        TerminalLine.Kind.INPUT -> Color(0xFFFF6D00)
                        TerminalLine.Kind.STDERR -> Color(0xFFFF5252)
                        TerminalLine.Kind.SYSTEM -> Color(0xFF1DE9B6)
                        TerminalLine.Kind.STDOUT -> Color(0xFFD4D4D4)
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$ ",
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp),
            )
            OutlinedTextField(
                value = state.terminalInput,
                onValueChange = viewModel::setTerminalInput,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("ls, cd, cat, forge doctor…", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.runCommand() }),
            )
            IconButton(onClick = viewModel::runCommand) {
                Icon(Icons.Filled.Send, contentDescription = "Run", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun GitTab(state: ToolsUiState, viewModel: ToolsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // status card
        val status = state.gitStatus
        when {
            status == null -> Text("Checking repository…", color = MaterialTheme.colorScheme.outline)
            !status.isRepo -> {
                Text("Not a git repository yet.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = viewModel::gitInit) {
                    Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("git init")
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⎇ ${status.branch}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (status.clean) "clean" else "modified",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.clean) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.error,
                    )
                }
                if (status.staged.isNotEmpty()) {
                    Text("Staged (${status.staged.size})", style = MaterialTheme.typography.labelSmall)
                    status.staged.take(8).forEach { Text("  + $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
                if (status.unstaged.isNotEmpty()) {
                    Text("Unstaged (${status.unstaged.size})", style = MaterialTheme.typography.labelSmall)
                    status.unstaged.take(8).forEach { Text("  ~ $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
                if (status.untracked.isNotEmpty()) {
                    Text("Untracked (${status.untracked.size})", style = MaterialTheme.typography.labelSmall)
                    status.untracked.take(8).forEach { Text("  ? $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::gitStageAll, enabled = status?.isRepo == true) { Text("Stage all") }
            OutlinedButton(onClick = viewModel::gitDiff, enabled = status?.isRepo == true) { Text("Diff") }
            OutlinedButton(onClick = viewModel::refreshGit) { Text("Refresh") }
        }

        OutlinedTextField(
            value = state.commitMessage,
            onValueChange = viewModel::setCommitMessage,
            label = { Text("Commit message") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = viewModel::gitCommit,
            enabled = status?.isRepo == true && state.commitMessage.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Commit") }

        if (state.gitLog.isNotEmpty()) {
            Text("History", style = MaterialTheme.typography.titleSmall)
            state.gitLog.forEach { commit ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        "${commit.shortHash}  ${commit.message.take(60)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    Text(
                        "${commit.author} · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(commit.timeEpochMs))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffTab(state: ToolsUiState) {
    if (state.gitDiffs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No changes", color = MaterialTheme.colorScheme.outline)
                Text(
                    "Tap Diff in the Git tab to compute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.gitDiffs) { diff ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.shapes.small)
                    .padding(8.dp),
            ) {
                Text(
                    diff.filePath,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                diff.hunks.forEach { hunk ->
                    Text(
                        hunk.header,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    hunk.lines.forEach { l ->
                        Text(
                            l.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = when (l.type) {
                                GitDiffHunk.DiffLine.Type.ADD -> Color(0xFF3FB950)
                                GitDiffHunk.DiffLine.Type.DEL -> Color(0xFFF85149)
                                GitDiffHunk.DiffLine.Type.CONTEXT -> Color(0xFF8B949E)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugTab(state: ToolsUiState, viewModel: ToolsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.BugReport,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.height(28.dp),
        )
        Text("Crash log analyzer", style = MaterialTheme.typography.titleMedium)
        Text(
            "Paste a FATAL EXCEPTION block from logcat or a stack trace. " +
                "Frames belonging to your project are highlighted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = state.crashInput,
            onValueChange = viewModel::setCrashInput,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            placeholder = { Text("Paste crash log here…", fontSize = 12.sp) },
        )
        Button(onClick = viewModel::analyzeCrash, modifier = Modifier.fillMaxWidth()) {
            Text("Analyze crash")
        }

        val report = state.crashReport
        if (report != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22), MaterialTheme.shapes.small)
                    .padding(10.dp),
            ) {
                Text(
                    report.exceptionType,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                if (report.message.isNotBlank()) {
                    Text(report.message, color = Color(0xFFD4D4D4), fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                report.causeChain.forEach { Text("Caused by: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary) }
                Spacer(Modifier.height(6.dp))
                report.stackFrames.take(25).forEach { frame ->
                    Text(
                        (if (frame.inProject) "▶ " else "  ") +
                            "${frame.className.substringAfterLast('.')}.${frame.methodName}" +
                            (frame.line?.let { ":$it" } ?: ""),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (frame.inProject) MaterialTheme.colorScheme.primary
                        else Color(0xFF8B949E),
                    )
                }
                if (report.stackFrames.none { it.inProject }) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No frames matched your package — check that the log is from your app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        if (state.plugins.any { it.enabled }) {
            Text("Enabled plugins", style = MaterialTheme.typography.titleSmall)
            state.plugins.filter { it.enabled }.forEach { p ->
                Text(
                    "• ${p.name} ${p.version} (${p.type})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
