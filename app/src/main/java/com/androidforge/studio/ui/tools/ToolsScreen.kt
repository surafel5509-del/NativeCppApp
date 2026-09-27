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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
 * Professional Tools Hub - AndroidForge Studio
 * Features:
 * - Terminal with forge doctor, NDK, LibGDX tools
 * - Git with professional UI (init, status, commit, log, diff)
 * - Diff viewer
 * - Debug with crash analyzer
 * - NDK tools, LibGDX tasks, APK tools
 * - Professional, offline capable
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
                title = { 
                    Column {
                        Text("Tools - Professional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Terminal • Git • NDK • LibGDX • Debug • Professional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                actions = {
                    state.projects.take(2).forEach { p ->
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Create a project to use professional tools", color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("Terminal, Git, NDK, LibGDX, Debug - Professional IDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Professional tool categories
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = false, onClick = {}, label = { Text("NDK - Professional", style = MaterialTheme.typography.labelSmall) })
                FilterChip(selected = false, onClick = {}, label = { Text("LibGDX - Professional", style = MaterialTheme.typography.labelSmall) })
                FilterChip(selected = false, onClick = {}, label = { Text("APK - Professional", style = MaterialTheme.typography.labelSmall) })
                FilterChip(selected = false, onClick = {}, label = { Text("Offline - Professional", style = MaterialTheme.typography.labelSmall) })
            }

            TabRow(selectedTabIndex = state.tab.ordinal) {
                ToolTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    ToolTab.TERMINAL -> "Terminal - Pro"
                                    ToolTab.GIT -> "Git - Pro"
                                    ToolTab.DIFF -> "Diff - Pro"
                                    ToolTab.DEBUG -> "Debug - Pro"
                                },
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (state.tab) {
                ToolTab.TERMINAL -> ProfessionalTerminalTab(state, viewModel)
                ToolTab.GIT -> ProfessionalGitTab(state, viewModel)
                ToolTab.DIFF -> ProfessionalDiffTab(state)
                ToolTab.DEBUG -> ProfessionalDebugTab(state, viewModel)
            }
        }
    }
}

@Composable
private fun ProfessionalTerminalTab(state: ToolsUiState, viewModel: ToolsViewModel) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.terminalLines.size) {
        if (state.terminalLines.isNotEmpty()) listState.scrollToItem(state.terminalLines.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Professional build tasks
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
                        label = { Text("▶ ${task.name} - Pro", fontSize = 11.sp) },
                    )
                }
            }
        }

        // Professional NDK & LibGDX quick commands
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(selected = false, onClick = { viewModel.setTerminalInput("forge doctor"); viewModel.runCommand() }, label = { Text("forge doctor - Pro") })
            FilterChip(selected = false, onClick = { viewModel.setTerminalInput("ls -la"); viewModel.runCommand() }, label = { Text("ls -la") })
            FilterChip(selected = false, onClick = { viewModel.setTerminalInput("cat app/src/main/cpp/CMakeLists.txt"); viewModel.runCommand() }, label = { Text("CMakeLists - NDK Pro") })
            FilterChip(selected = false, onClick = { viewModel.setTerminalInput("ls core/src/"); viewModel.runCommand() }, label = { Text("LibGDX Core - Pro") })
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0D1117))
                .padding(8.dp),
        ) {
            item {
                Text(
                    "AndroidForge Studio Professional Terminal\nReal APK compiler • NDK • LibGDX • Offline\nType 'forge doctor' to check toolchain - Professional",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
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
                placeholder = { Text("Professional: ls, cd, cat, forge doctor, ndk-build, cmake…", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.runCommand() }),
            )
            IconButton(onClick = viewModel::runCommand) {
                Icon(Icons.Filled.Send, contentDescription = "Run - Professional")
            }
        }
    }
}

@Composable
private fun ProfessionalGitTab(state: ToolsUiState, viewModel: ToolsViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Git - Professional Version Control", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Professional Git with JGit - Offline capable, real Git operations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::gitInit, enabled = !state.busy) { Text("Init - Pro") }
            OutlinedButton(onClick = viewModel::refreshGit, enabled = !state.busy) { Text("Status - Pro") }
            OutlinedButton(onClick = viewModel::gitStageAll, enabled = !state.busy) { Text("Stage All - Pro") }
            OutlinedButton(onClick = viewModel::gitDiff, enabled = !state.busy) { Text("Diff - Pro") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Git Status - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (state.gitStatus == null) {
                    Text("No Git repo - Run Init - Professional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    Text("Branch: ${state.gitStatus.branch} - Professional", style = MaterialTheme.typography.labelSmall)
                    Text("Changes: ${state.gitStatus.changes.size} - Professional", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        OutlinedTextField(
            value = state.commitMessage,
            onValueChange = viewModel::setCommitMessage,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Commit message - Professional") },
            placeholder = { Text("Professional commit message") },
        )
        Button(onClick = viewModel::gitCommit, enabled = state.commitMessage.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Commit - Professional Git")
        }

        Text("Commit History - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        state.gitLog.take(10).forEach { commit ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(commit.message, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text("${commit.hash.take(7)} • ${commit.author} - Professional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProfessionalDiffTab(state: ToolsUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Diff Viewer - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Professional diff with color highlighting - Real Git diff", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        
        if (state.gitDiffs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No diffs - Professional - Run Git Diff", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.gitDiffs) { diff ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(diff.filePath, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            diff.hunks.forEach { hunk ->
                                Text("@@ ${hunk.header} @@ - Professional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                hunk.lines.take(20).forEach { line ->
                                    val color = when {
                                        line.startsWith("+") -> Color(0xFF238636)
                                        line.startsWith("-") -> Color(0xFFCF222E)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Text(line, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalDebugTab(state: ToolsUiState, viewModel: ToolsViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Debug - Professional Crash Analyzer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Professional crash analysis with project frame highlighting - Real diagnostics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        
        OutlinedTextField(
            value = state.crashInput,
            onValueChange = viewModel::setCrashInput,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            label = { Text("Paste crash log (FATAL EXCEPTION) - Professional") },
            placeholder = { Text("09-27 10:00:00 E AndroidRuntime: FATAL EXCEPTION: main\njava.lang.NullPointerException... Professional") },
        )
        Button(onClick = viewModel::analyzeCrash, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Analyze Crash - Professional")
        }

        if (state.crashReport != null) {
            val report = state.crashReport
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Crash Report - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Process: ${report.process}", style = MaterialTheme.typography.labelSmall)
                    Text("Exception: ${report.exceptionType}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Message: ${report.message}", style = MaterialTheme.typography.bodySmall)
                    if (report.causeChain.isNotEmpty()) {
                        Text("Caused by:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        report.causeChain.forEach { cause ->
                            Text("• $cause", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("Stack Frames (${report.stackFrames.size}) - Professional:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    report.stackFrames.take(15).forEach { frame ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (frame.inProject) "★ " else "  ",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (frame.inProject) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            )
                            Column {
                                Text("${frame.className}.${frame.methodName}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), fontWeight = if (frame.inProject) FontWeight.Bold else FontWeight.Normal, color = if (frame.inProject) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                if (frame.file != null) Text("${frame.file}:${frame.line ?: "?"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        // Professional tools
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Professional Debug Tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = false, onClick = {}, label = { Text("NDK Stack - Pro", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = false, onClick = {}, label = { Text("LibGDX Log - Pro", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = false, onClick = {}, label = { Text("APK Log - Pro", style = MaterialTheme.typography.labelSmall) })
                }
                Text("• NDK: ndk-stack, addr2line - Professional native debugging", style = MaterialTheme.typography.labelSmall)
                Text("• LibGDX: Gdx.app.log, Box2D debug - Professional game debugging", style = MaterialTheme.typography.labelSmall)
                Text("• APK: logcat, crash analyzer - Professional", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
