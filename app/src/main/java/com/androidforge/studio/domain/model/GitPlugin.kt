package com.androidforge.studio.domain.model

/** Git status of the working tree. */
data class GitStatus(
    val isRepo: Boolean,
    val branch: String = "",
    val clean: Boolean = true,
    val staged: List<String> = emptyList(),
    val unstaged: List<String> = emptyList(),
    val untracked: List<String> = emptyList(),
)

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val author: String,
    val email: String,
    val timeEpochMs: Long,
    val message: String,
)

data class GitDiffHunk(
    val header: String,
    val lines: List<DiffLine>,
) {
    data class DiffLine(val type: Type, val text: String) {
        enum class Type { ADD, DEL, CONTEXT }
    }
}

data class GitDiff(val filePath: String, val hunks: List<GitDiffHunk>)

/** A plugin manifest (loaded from assets or filesDir/plugins). */
data class Plugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val type: PluginType,
    val enabled: Boolean = false,
    val entry: String = "",           // relative path of task script / theme json
    val sourcePath: String = "",      // where the manifest was found
) {
    enum class PluginType { TEMPLATE, BUILD_TASK, THEME, MARKETPLACE }
}

/** User-defined build task contributed by a plugin. */
data class BuildTask(
    val id: String,
    val name: String,
    val command: String,              // executed through the shell session
    val workingDir: String = ".",
    val pluginId: String = "",
)
