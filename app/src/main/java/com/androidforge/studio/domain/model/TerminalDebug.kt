package com.androidforge.studio.domain.model

/** Terminal emulator: one emitted line. */
data class TerminalLine(
    val text: String,
    val kind: Kind = Kind.STDOUT,
    val timestamp: Long = System.currentTimeMillis(),
) {
    enum class Kind { STDOUT, STDERR, SYSTEM, INPUT }
}

/** Breakpoint set by the user on a line of a file. */
data class Breakpoint(
    val id: Long = 0,
    val projectId: Long,
    val filePath: String,
    val line: Int,
    val enabled: Boolean = true,
    val condition: String = "",
)

/** A parsed crash report shown by the crash analyzer. */
data class CrashReport(
    val process: String,
    val exceptionType: String,
    val message: String,
    val stackFrames: List<StackFrame>,
    val causeChain: List<String>,
) {
    data class StackFrame(
        val className: String,
        val methodName: String,
        val file: String?,
        val line: Int?,
        val inProject: Boolean,
    )
}
