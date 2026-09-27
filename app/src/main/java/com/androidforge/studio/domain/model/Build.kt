package com.androidforge.studio.domain.model

/** Phases of a build, in execution order. */
enum class BuildStage(val displayName: String) {
    PENDING("Pending"),
    SYNC("Gradle sync"),
    COMPILE_RES("Compile resources"),
    COMPILE_KOTLIN("Compile Kotlin"),
    DEX("Dex (d8)"),
    PACKAGE("Package APK"),
    ZIP_ALIGN("Zipalign"),
    SIGN("Sign"),
    INSTALL("Install"),
    DONE("Done"),
    FAILED("Failed");
}

enum class BuildTarget { APK, AAB }

enum class BuildStatus { IDLE, RUNNING, SUCCESS, FAILED, CANCELLED }

/** One line of build output produced by a stage. */
data class BuildLogLine(
    val stage: BuildStage,
    val message: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

/** A parsed compiler/tool error that can be tapped to jump to source. */
data class BuildDiagnostic(
    val filePath: String?,
    val line: Int?,
    val message: String,
    val severity: Severity,
) {
    enum class Severity { ERROR, WARNING, INFO }
}

/** Persisted record of a build run. */
data class BuildSession(
    val id: Long = 0,
    val projectId: Long,
    val target: BuildTarget,
    val status: BuildStatus = BuildStatus.IDLE,
    val stage: BuildStage = BuildStage.PENDING,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val artifactPath: String? = null,
    val log: String = "",
    val diagnostics: String = "", // JSON-encoded list of BuildDiagnostic
)

/** Signing configuration for release/debug builds. */
data class SigningConfig(
    val keystorePath: String,
    val alias: String,
    val storePassword: String,
    val keyPassword: String,
) {
    companion object {
        fun debugDefault(keystorePath: String) = SigningConfig(
            keystorePath = keystorePath,
            alias = "androidforge-debug",
            storePassword = "android",
            keyPassword = "android",
        )
    }
}
