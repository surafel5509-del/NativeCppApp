package com.androidforge.studio.data.terminal

import android.content.Context
import com.androidforge.studio.domain.model.FileNode
import com.androidforge.studio.domain.model.Project
import com.androidforge.studio.domain.model.TerminalLine
import com.androidforge.studio.domain.repository.SettingsRepository
import com.androidforge.studio.domain.repository.TerminalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers as CoroutinesDispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom shell emulator.
 *
 * Built-ins (safe, sandboxed, parsed in Kotlin): ls, cd, pwd, cat, echo, mkdir,
 * rm, touch, clear, help, tree, forge (meta-commands).
 *
 * Anything else is executed through `/system/bin/sh -c` with the working
 * directory pinned to the project — unless sandbox mode is on, in which case
 * only built-ins run. Output streams back through a StateFlow.
 */
@Singleton
class ShellSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : TerminalRepository {

    private val _lines = MutableStateFlow<Map<Long, List<TerminalLine>>>(emptyMap())
    private val cwds = mutableMapOf<Long, String>()

    private fun projectsRoot(): File = File(context.filesDir, "projects")
    private fun projectRoot(project: Project): File = File(projectsRoot(), project.rootPath)
    private fun cwdOf(project: Project): File {
        val rel = cwds[project.id] ?: "."
        val base = projectRoot(project)
        val target = if (rel == ".") base else File(base, rel)
        return if (target.isDirectory && target.canonicalPath.startsWith(base.canonicalPath)) target else base
    }

    override fun observeLines(projectId: Long): Flow<List<TerminalLine>> =
        MutableStateFlow(_lines.value[projectId] ?: defaultBanner())

    override fun currentDir(project: Project): String = cwds[project.id] ?: "."

    override suspend fun cancel() { /* commands are short-lived; kept for interface parity */ }

    override suspend fun listDir(project: Project, relativePath: String): List<FileNode> =
        withContext(Dispatchers.IO) {
            val dir = File(projectRoot(project), relativePath)
            if (!dir.isDirectory) return@withContext emptyList()
            dir.listFiles()
                ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                ?.map { f ->
                    FileNode(
                        name = f.name,
                        path = f.path,
                        relativePath = f.relativeTo(projectRoot(project)).path,
                        isDirectory = f.isDirectory,
                        sizeBytes = if (f.isFile) f.length() else 0,
                    )
                } ?: emptyList()
        }

    override suspend fun execute(project: Project, command: String) = withContext(Dispatchers.IO) {
        append(project.id, TerminalLine(command, TerminalLine.Kind.INPUT))
        val cwd = cwdOf(project)
        val sandbox = settings.sandboxScopes().first()

        val parts = command.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return@withContext
        val cmd = parts[0]
        val args = parts.drop(1)

        when (cmd) {
            "help" -> append(project.id, TerminalLine(BANNER_HELP, TerminalLine.Kind.SYSTEM))
            "clear" -> _lines.update { it - project.id }
            "pwd" -> append(project.id, TerminalLine(cwd.path))
            "cd" -> handleCd(project, args)
            "ls" -> handleLs(project, cwd, args)
            "cat" -> handleCat(project, cwd, args)
            "echo" -> append(project.id, TerminalLine(args.joinToString(" ")))
            "mkdir" -> {
                if (args.isEmpty()) append(project.id, TerminalLine("usage: mkdir <dir>", TerminalLine.Kind.STDERR))
                else if (!File(cwd, args[0]).mkdirs() && !File(cwd, args[0]).isDirectory) {
                    append(project.id, TerminalLine("mkdir: cannot create ${args[0]}", TerminalLine.Kind.STDERR))
                }
            }
            "touch" -> {
                if (args.isEmpty()) append(project.id, TerminalLine("usage: touch <file>", TerminalLine.Kind.STDERR))
                else {
                    val f = File(cwd, args[0])
                    f.parentFile?.mkdirs()
                    if (!f.exists()) f.createNewFile()
                }
            }
            "rm" -> handleRm(project, cwd, args)
            "tree" -> handleTree(project, cwd)
            "forge" -> handleForge(project, cwd, args)
            else -> handleExternal(project, cwd, cmd, command, sandbox)
        }
    }

    private suspend fun handleCd(project: Project, args: List<String>) {
        val base = projectRoot(project)
        val rel = args.firstOrNull() ?: "."
        val target = if (rel == ".") base else if (rel == "..") {
            val cur = cwdOf(project)
            if (cur.canonicalPath == base.canonicalPath) base else cur.parentFile
        } else File(cwdOf(project), rel)
        when {
            !target.exists() -> append(project.id, TerminalLine("cd: no such directory: $rel", TerminalLine.Kind.STDERR))
            !target.canonicalPath.startsWith(base.canonicalPath) ->
                append(project.id, TerminalLine("cd: blocked outside project sandbox", TerminalLine.Kind.STDERR))
            else -> {
                val newRel = if (target.canonicalPath == base.canonicalPath) "."
                else target.canonicalFile.relativeTo(base.canonicalFile).path
                cwds[project.id] = newRel
                append(project.id, TerminalLine("→ $newRel", TerminalLine.Kind.SYSTEM))
            }
        }
    }

    private fun handleLs(project: Project, cwd: File, args: List<String>) {
        val target = if (args.isEmpty()) cwd else File(cwd, args[0])
        if (!target.isDirectory) {
            append(project.id, TerminalLine("ls: cannot access '${args.joinToString(" ")}'", TerminalLine.Kind.STDERR))
            return
        }
        val entries = target.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
        if (entries.isEmpty()) append(project.id, TerminalLine("(empty)"))
        else for (e in entries) {
            val suffix = if (e.isDirectory) "/" else "  ${e.length()}B"
            append(project.id, TerminalLine(e.name + suffix))
        }
    }

    private fun handleCat(project: Project, cwd: File, args: List<String>) {
        if (args.isEmpty()) {
            append(project.id, TerminalLine("usage: cat <file>", TerminalLine.Kind.STDERR))
            return
        }
        val f = File(cwd, args[0])
        if (!f.isFile) append(project.id, TerminalLine("cat: ${args[0]}: No such file", TerminalLine.Kind.STDERR))
        else f.readText().take(100_000).lineSequence().forEach { append(project.id, TerminalLine(it)) }
    }

    private fun handleRm(project: Project, cwd: File, args: List<String>) {
        val recursive = args.any { it.contains('r') && it.startsWith("-") }
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) {
            append(project.id, TerminalLine("usage: rm [-r] <path>", TerminalLine.Kind.STDERR))
            return
        }
        val root = projectRoot(project)
        for (t in targets) {
            val f = File(cwd, t)
            if (!f.exists()) {
                append(project.id, TerminalLine("rm: $t: No such file", TerminalLine.Kind.STDERR))
                continue
            }
            if (f.canonicalPath == root.canonicalPath) {
                append(project.id, TerminalLine("rm: refused to delete project root", TerminalLine.Kind.STDERR))
                continue
            }
            if (!f.canonicalPath.startsWith(root.canonicalPath)) {
                append(project.id, TerminalLine("rm: blocked outside sandbox", TerminalLine.Kind.STDERR))
                continue
            }
            if (f.isDirectory && !recursive) {
                append(project.id, TerminalLine("rm: $t: is a directory (use -r)", TerminalLine.Kind.STDERR))
                continue
            }
            f.deleteRecursively()
        }
    }

    private fun handleTree(project: Project, cwd: File) {
        fun walk(dir: File, prefix: String, depth: Int) {
            if (depth > 4) return
            val children = dir.listFiles()
                ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                ?: return
            for ((i, c) in children.withIndex()) {
                val last = i == children.size - 1
                append(project.id, TerminalLine(prefix + (if (last) "└── " else "├── ") + c.name))
                if (c.isDirectory) walk(c, prefix + (if (last) "    " else "│   "), depth + 1)
            }
        }
        append(project.id, TerminalLine(cwd.name))
        walk(cwd, "", 0)
    }

    private suspend fun handleForge(project: Project, cwd: File, args: List<String>) {
        when (args.firstOrNull() ?: "help") {
            "doctor" -> {
                append(project.id, TerminalLine("AndroidForge doctor", TerminalLine.Kind.SYSTEM))
                append(project.id, TerminalLine("  project dir : ${cwd.path}"))
                append(project.id, TerminalLine("  toolchain   : ${File(context.filesDir, "toolchain/bin").path}"))
                for (c in listOf("aapt2", "d8", "apksigner", "zipalign", "kotlinc", "java")) {
                    val found = File(context.filesDir, "toolchain/bin/$c").canExecute()
                    append(project.id, TerminalLine("  ${if (found) "✓" else "✗"} $c"))
                }
            }
            "help" -> append(project.id, TerminalLine(FORGE_HELP, TerminalLine.Kind.SYSTEM))
            else -> append(project.id, TerminalLine("forge: unknown subcommand '${args.firstOrNull()}'", TerminalLine.Kind.STDERR))
        }
    }

    private suspend fun handleExternal(
        project: Project,
        cwd: File,
        cmd: String,
        command: String,
        sandbox: Boolean,
    ) {
        if (sandbox) {
            append(
                project.id,
                TerminalLine(
                    "sandbox: external command '$cmd' blocked — built-ins only " +
                        "(disable Sandbox in Settings ▸ Terminal to allow /system/bin/sh).",
                    TerminalLine.Kind.STDERR,
                ),
            )
            return
        }
        try {
            val process = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(cwd)
                .redirectErrorStream(false)
                .start()
            val out = withContext(Dispatchers.IO) { process.inputStream.bufferedReader().readText() }
            val err = withContext(Dispatchers.IO) { process.errorStream.bufferedReader().readText() }
            process.waitFor()
            out.lineSequence().filter { it.isNotEmpty() }.forEach { append(project.id, TerminalLine(it)) }
            err.lineSequence().filter { it.isNotEmpty() }.forEach {
                append(project.id, TerminalLine(it, TerminalLine.Kind.STDERR))
            }
        } catch (e: Exception) {
            append(project.id, TerminalLine("$cmd: ${e.message}", TerminalLine.Kind.STDERR))
        }
    }

    private fun defaultBanner(): List<TerminalLine> = listOf(
        TerminalLine("AndroidForge Shell — type `help` for built-ins.", TerminalLine.Kind.SYSTEM),
    )

    private fun append(projectId: Long, line: TerminalLine) {
        _lines.update { current ->
            val existing = current[projectId] ?: emptyList()
            val next = (existing + line).takeLast(2000) // bound memory
            current + (projectId to next)
        }
    }

    private companion object {
        val BANNER_HELP = """
            Built-ins: ls, cd, pwd, cat, echo, mkdir, touch, rm, tree, clear, help, forge
            Example:   forge doctor
        """.trimIndent()
        val FORGE_HELP = """
            forge doctor      — check toolchain availability
            forge build       — run a build (Build tab)
            forge git status  — git status (Tools ▸ Git)
        """.trimIndent()
    }
}
