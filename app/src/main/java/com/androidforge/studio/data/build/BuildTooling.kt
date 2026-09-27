package com.androidforge.studio.data.build

import com.androidforge.studio.domain.model.BuildDiagnostic
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.min

/**
 * Runs a build tool as a subprocess, streaming output line-by-line so the UI
 * can render the log live. Never throws on non-zero exits — callers inspect
 * [Result.success] / [Result.exitCode].
 */
object ProcessRunner {

    data class Result(
        val success: Boolean,
        val exitCode: Int,
        val output: String = "",
    )

    fun run(
        command: List<String>,
        workDir: File? = null,
        timeoutSeconds: Long = 180,
        onOutput: ((String, isError: Boolean) -> Unit)? = null,
    ): Result {
        return try {
            val builder = ProcessBuilder(command)
            if (workDir != null) builder.directory(workDir)
            val process = builder.start()

            val collected = StringBuilder()
            fun pump(stream: java.io.InputStream, isError: Boolean) {
                Thread {
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val l = line ?: break
                            synchronized(collected) { collected.append(l).append('\n') }
                            try {
                                onOutput?.invoke(l, isError)
                            } catch (_: Exception) {
                                // A failing log consumer must never kill the build.
                            }
                        }
                    }
                }.apply { isDaemon = true; start() }
            }
            pump(process.inputStream, isError = false)
            pump(process.errorStream, isError = true)

            val finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                onOutput?.invoke("Process timed out after ${timeoutSeconds}s: ${command.first()}", true)
                return Result(success = false, exitCode = -1, output = synchronized(collected) { collected.toString() })
            }
            val text = synchronized(collected) { collected.toString() }
            Result(success = process.exitValue() == 0, exitCode = process.exitValue(), output = text)
        } catch (e: Exception) {
            onOutput?.invoke("Failed to start ${command.firstOrNull()}: ${e.message}", true)
            Result(success = false, exitCode = -1, output = e.message ?: "")
        }
    }
}

/**
 * Parses compiler/tool output into tappable diagnostics.
 *
 * Supported shapes:
 *  - kotlinc: `e: file:///path/Main.kt:12:5 Unresolved reference: foo`
 *  - javac:   `/path/Main.java:12: error: cannot find symbol`
 *  - aapt2:   `error: file res/layout/x.xml:5: resource ... not found` (loose)
 *  - d8:      `Error: ...`
 */
object BuildErrorParser {

    private val KOTLIN = Regex("""^[ew]:\s+file://(.+?):(\d+)(?::\d+)?\s+(.+)$""")
    private val JAVAC = Regex("""^(.+?):(\d+):\s+error:\s+(.+)$""")
    private val LOOSE_FILE_LINE = Regex("""(?:file\s+)?((?:/|\w:)[^\s:]+?\.(?:kt|java|xml|gradle|kts)):(\d+)[:\s]+(.+)""")
    private val GENERIC_ERROR = Regex("""^(?:error|Error|FAILURE):\s*(.+)$""")

    fun parse(output: String): List<BuildDiagnostic> {
        val result = ArrayList<BuildDiagnostic>()
        for (raw in output.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            KOTLIN.matchEntire(line)?.let { m ->
                result += BuildDiagnostic(
                    filePath = stripFileUri(m.groupValues[1]),
                    line = m.groupValues[2].toIntOrNull(),
                    message = m.groupValues[3],
                    severity = if (line.startsWith("w:")) BuildDiagnostic.Severity.WARNING else BuildDiagnostic.Severity.ERROR,
                )
                return@let
            } ?: JAVAC.matchEntire(line)?.let { m ->
                result += BuildDiagnostic(
                    filePath = stripFileUri(m.groupValues[1]),
                    line = m.groupValues[2].toIntOrNull(),
                    message = m.groupValues[3],
                    severity = BuildDiagnostic.Severity.ERROR,
                )
                return@let
            } ?: run {
                if (line.startsWith("e:") || line.startsWith("error") || line.contains("FAILURE")) {
                    LOOSE_FILE_LINE.find(line)?.let { m ->
                        result += BuildDiagnostic(
                            filePath = stripFileUri(m.groupValues[1]),
                            line = m.groupValues[2].toIntOrNull(),
                            message = m.groupValues[3].trim(),
                            severity = BuildDiagnostic.Severity.ERROR,
                        )
                    } ?: GENERIC_ERROR.find(line)?.let { m ->
                        result += BuildDiagnostic(
                            filePath = null,
                            line = null,
                            message = m.groupValues[1].trim(),
                            severity = BuildDiagnostic.Severity.ERROR,
                        )
                    }
                }
            }
        }
        return result
    }

    /** Extracts the "What went wrong:" section from a Gradle failure report. */
    fun gradleFailure(output: String): String? {
        val idx = output.indexOf("What went wrong:")
        if (idx < 0) return null
        val section = output.substring(idx + "What went wrong:".length)
        val end = section.indexOf("* Try:")
        val body = if (end >= 0) section.substring(0, end) else section
        return body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
            .take(6).joinToString(" ").take(600)
    }

    private fun stripFileUri(path: String): String = path.removePrefix("file://")

    /** Safe truncation helper for log persistence. */
    fun capLog(text: String, maxChars: Int = 200_000): String =
        if (text.length <= maxChars) text
        else "…(truncated)…\n" + text.substring(text.length - min(maxChars, text.length))
}
