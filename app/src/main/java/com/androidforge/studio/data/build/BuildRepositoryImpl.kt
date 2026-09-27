package com.androidforge.studio.data.build

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.androidforge.studio.data.local.dao.BuildSessionDao
import com.androidforge.studio.data.local.dao.ProjectDao
import com.androidforge.studio.data.local.entity.BuildSessionEntity
import com.androidforge.studio.domain.model.BuildDiagnostic
import com.androidforge.studio.domain.model.BuildLogLine
import com.androidforge.studio.domain.model.BuildSession
import com.androidforge.studio.domain.model.BuildStage
import com.androidforge.studio.domain.model.BuildStatus
import com.androidforge.studio.domain.model.BuildTarget
import com.androidforge.studio.domain.model.SigningConfig
import com.androidforge.studio.domain.repository.BuildRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device build pipeline.
 *
 * Stages (as available tooling permits):
 *   sync -> compile res (aapt2) -> kotlin/java compile (toolchain kotlinc/javac)
 *        -> dex (d8) -> package -> zipalign -> sign (apksigner) -> install
 *
 * Everything runs inside filesDir/build/<projectId>; a missing tool fails the
 * build with an actionable diagnostic rather than a stack trace.
 *
 * When [useCloud] is requested the build is delegated to the GitHub Actions
 * cloud runner via [CloudBuildClient].
 */
@Singleton
class BuildRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectDao: ProjectDao,
    private val sessionDao: BuildSessionDao,
    private val toolchain: ToolchainManager,
    private val cloudBuild: CloudBuildClient,
) : BuildRepository {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _build = MutableStateFlow(BuildSession(projectId = -1, target = BuildTarget.APK))
    val build: StateFlow<BuildSession> = _build.asStateFlow()

    private val _logEvents = MutableSharedFlow<BuildLogLine>(extraBufferCapacity = 512)
    val logEvents: SharedFlow<BuildLogLine> = _logEvents.asSharedFlow()

    private var currentJob: Job? = null

    override fun observeBuild(): kotlinx.coroutines.flow.Flow<BuildSession> = _build

    override suspend fun startBuild(projectId: Long, target: BuildTarget, useCloud: Boolean): Long {
        currentJob?.cancel()
        val project = projectDao.getById(projectId) ?: throw IllegalArgumentException("Project not found")

        var session = BuildSession(projectId = projectId, target = target, status = BuildStatus.RUNNING)
        session = persist(session)
        _build.value = session

        currentJob = scope.launch {
            try {
                if (useCloud) {
                    runCloud(session, project.rootPath, project.name)
                } else {
                    runLocal(session, project.rootPath, project.packageName)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                update(session.copy(status = BuildStatus.CANCELLED, stage = BuildStage.FAILED))
                throw e
            } catch (e: Exception) {
                log(BuildStage.FAILED, "Unexpected error: ${e.message}", isError = true)
                update(session.copy(status = BuildStatus.FAILED, stage = BuildStage.FAILED))
            }
        }
        return session.id
    }

    override fun cancelBuild() {
        currentJob?.cancel()
        val s = _build.value
        if (s.status == BuildStatus.RUNNING) {
            val cancelled = s.copy(status = BuildStatus.CANCELLED, stage = BuildStage.FAILED)
            _build.value = cancelled
            scope.launch { persist(cancelled) }
        }
    }

    override suspend fun syncProject(projectId: Long): Boolean = withContext(Dispatchers.IO) {
        val project = projectDao.getById(projectId) ?: return@withContext false
        val dir = File(context.filesDir, "projects/${project.rootPath}")
        val settings = File(dir, "settings.gradle.kts").takeIf { it.isFile }
            ?: File(dir, "settings.gradle").takeIf { it.isFile }
        log(BuildStage.SYNC, "Syncing ${project.name}…")
        if (settings == null) {
            log(BuildStage.SYNC, "No settings.gradle(.kts) found — single module assumed.", isError = true)
            false
        } else {
            val gradleDir = File(dir, "gradle/wrapper")
            log(BuildStage.SYNC, "Project layout OK: ${settings.name}")
            if (File(dir, "app").isDirectory) log(BuildStage.SYNC, "Detected module :app")
            if (gradleDir.isDirectory) log(BuildStage.SYNC, "Gradle wrapper present")
            log(BuildStage.SYNC, "Sync complete.")
            true
        }
    }

    override suspend fun installApk(path: String): Boolean {
        val file = File(path)
        if (!file.isFile) return false
        return try {
            val authority = "${context.packageName}.files"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            log(BuildStage.INSTALL, "Install failed: ${e.message}", isError = true)
            false
        }
    }

    override suspend fun recentSessions(projectId: Long, limit: Int): List<BuildSession> =
        sessionDao.recent(projectId, limit).map { it.toDomain() }

    override suspend fun clearHistory(projectId: Long) = sessionDao.clear(projectId)

    // ------------------------------------------------------------- pipeline

    private suspend fun runCloud(session: BuildSession, rootPath: String, name: String) {
        log(BuildStage.SYNC, "Delegating build to cloud runner (GitHub Actions)…")
        val result = cloudBuild.dispatchWorkflow(
            projectDir = File(context.filesDir, "projects/$rootPath"),
            displayName = name,
            onLog = { stage, msg, err -> log(stage, msg, err) },
        )
        if (result.success) {
            update(
                session.copy(
                    status = BuildStatus.SUCCESS,
                    stage = BuildStage.DONE,
                    finishedAt = System.currentTimeMillis(),
                    artifactPath = result.artifactPath,
                )
            )
        } else {
            log(BuildStage.FAILED, result.error ?: "Cloud build failed", isError = true)
            update(
                session.copy(
                    status = BuildStatus.FAILED,
                    stage = BuildStage.FAILED,
                    finishedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private suspend fun runLocal(session: BuildSession, rootPath: String, packageName: String) {
        val projectDir = File(context.filesDir, "projects/$rootPath")
        val stageDir = toolchain.stageDir(session.projectId)
        val tools = toolchain.resolve()

        suspend fun fail(stage: BuildStage, msg: String): Nothing {
            log(stage, msg, isError = true)
            update(
                session.copy(
                    status = BuildStatus.FAILED,
                    stage = BuildStage.FAILED,
                    finishedAt = System.currentTimeMillis(),
                    diagnostics = gson.toJson(
                        listOf(BuildDiagnostic(null, null, msg, BuildDiagnostic.Severity.ERROR))
                    ),
                )
            )
            throw BuildFailedException(msg)
        }

        // ---- SYNC ----
        update(session.copy(stage = BuildStage.SYNC))
        if (!syncProject(session.projectId)) {
            fail(BuildStage.SYNC, "Project sync failed: settings.gradle(.kts) missing.")
        }

        // ---- Toolchain check ----
        if (tools.missing.isNotEmpty()) {
            fail(
                BuildStage.COMPILE_RES,
                "Missing tools: ${tools.missing.joinToString()}. " +
                    "Import them into Files ▸ toolchain/bin (see Tools ▸ Terminal: `forge doctor`)."
            )
        }
        val androidJar = toolchain.androidJar()
            ?: fail(BuildStage.COMPILE_RES, "android.jar not found. Import an Android SDK platform into sdk/platforms/android-35/.")

        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml")
        if (!manifest.isFile) fail(BuildStage.COMPILE_RES, "app/src/main/AndroidManifest.xml not found.")

        // ---- COMPILE RES ----
        update(session.copy(stage = BuildStage.COMPILE_RES))
        log(BuildStage.COMPILE_RES, "aapt2 link…")
        val resDir = File(projectDir, "app/src/main/res")
        val baseApk = File(stageDir, "base.apk")
        if (baseApk.exists()) baseApk.delete()

        val linked = ProcessRunner.run(
            listOf(
                tools.aapt2!!.absolutePath, "link",
                "--proto-format",
                "-o", baseApk.absolutePath,
                "-I", androidJar.absolutePath,
                "--manifest", manifest.absolutePath,
                "-R", if (resDir.isDirectory) resDir.absolutePath else "--auto-add-overlay",
                "--auto-add-overlay",
                "--min-sdk-version", "26",
                "--target-sdk-version", "35",
                "--version-code", "1",
                "--version-name", "1.0",
                "--java", File(stageDir, "gen").absolutePath,
                "--output-text-symbols", File(stageDir, "R.txt").absolutePath,
            ),
            workDir = projectDir,
            timeoutSeconds = 180,
            onOutput = { line, err -> log(BuildStage.COMPILE_RES, line, err) },
        )
        if (!linked.success) fail(BuildStage.COMPILE_RES, "aapt2 link failed (exit ${linked.exitCode}).")

        // ---- COMPILE KOTLIN/JAVA ----
        update(session.copy(stage = BuildStage.COMPILE_KOTLIN))
        val srcDirs = listOf(
            File(projectDir, "app/src/main/java"),
            File(projectDir, "app/src/main/kotlin"),
            File(stageDir, "gen"),
        ).filter { it.isDirectory }
        val sources = srcDirs.flatMap { dir -> dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.toList() }

        val classesDir = File(stageDir, "classes").apply { deleteRecursively(); mkdirs() }
        val outputJar = File(stageDir, "classes.jar")
        val libs = collectLibraryJars(projectDir)

        if (sources.isEmpty()) {
            fail(BuildStage.COMPILE_KOTLIN, "No .kt/.java sources found under app/src/main.")
        }

        val kotlinc = tools.kotlinc
        val javac = findJvmTool("javac", tools)
        var compiled = false

        if (kotlinc != null && javac != null) {
            // Two-step: kotlinc first (with android.jar + libs), then javac for pure java leftovers
            log(BuildStage.COMPILE_KOTLIN, "Compiling ${sources.count { it.extension == "kt" }} Kotlin files…")
            val ktArgs = buildList {
                add(kotlinc.absolutePath)
                add("-nowarn")
                add("-jvm-target"); add("17")
                add("-classpath"); add((listOf(androidJar) + libs).joinToString(File.pathSeparator) { it.absolutePath })
                add("-d"); add(classesDir.absolutePath)
                addAll(sources.filter { it.extension == "kt" }.map { it.absolutePath })
            }
            val ktResult = ProcessRunner.run(ktArgs, workDir = projectDir, timeoutSeconds = 600,
                onOutput = { line, err -> log(BuildStage.COMPILE_KOTLIN, line, err) })
            if (!ktResult.success) fail(BuildStage.COMPILE_KOTLIN, "Kotlin compilation failed (exit ${ktResult.exitCode}).")

            val javaSources = sources.filter { it.extension == "java" }
            if (javaSources.isNotEmpty()) {
                log(BuildStage.COMPILE_KOTLIN, "Compiling ${javaSources.size} Java files…")
                val javacArgs = buildList {
                    add(javac.absolutePath)
                    add("-source"); add("17")
                    add("-target"); add("17")
                    add("-classpath"); add((listOf(androidJar, classesDir) + libs).joinToString(File.pathSeparator) { it.absolutePath })
                    add("-d"); add(classesDir.absolutePath)
                    addAll(javaSources.map { it.absolutePath })
                }
                val jcResult = ProcessRunner.run(javacArgs, workDir = projectDir, timeoutSeconds = 300,
                    onOutput = { line, err -> log(BuildStage.COMPILE_KOTLIN, line, err) })
                if (!jcResult.success) fail(BuildStage.COMPILE_KOTLIN, "Java compilation failed (exit ${jcResult.exitCode}).")
            }
            compiled = true
        }

        if (!compiled) {
            fail(
                BuildStage.COMPILE_KOTLIN,
                "No compiler available. Import kotlinc + JDK (java, javac) into toolchain/bin, " +
                    "or use Cloud build mode."
            )
        }

        zipDirectory(classesDir, outputJar)

        // ---- DEX ----
        update(session.copy(stage = BuildStage.DEX))
        log(BuildStage.DEX, "d8 → classes.dex")
        val dexOut = File(stageDir, "dex").apply { deleteRecursively(); mkdirs() }
        val d8Args = buildList {
            add(tools.d8!!.absolutePath)
            add("--release")
            add("--min-api"); add("26")
            add("--lib"); add(androidJar.absolutePath)
            add("--output"); add(dexOut.absolutePath)
            add(outputJar.absolutePath)
        }
        val d8Result = ProcessRunner.run(d8Args, workDir = projectDir, timeoutSeconds = 600,
            onOutput = { line, err -> log(BuildStage.DEX, line, err) })
        if (!d8Result.success) fail(BuildStage.DEX, "d8 failed (exit ${d8Result.exitCode}).")
        val dexFile = File(dexOut, "classes.dex")
        if (!dexFile.isFile) fail(BuildStage.DEX, "classes.dex not produced by d8.")

        // ---- PACKAGE ----
        update(session.copy(stage = BuildStage.PACKAGE))
        log(BuildStage.PACKAGE, "Inserting classes.dex into APK…")
        val unsignedApk = File(stageDir, "unsigned.apk")
        baseApk.copyTo(unsignedApk, overwrite = true)
        insertDex(unsignedApk, dexFile)

        // ---- ZIPALIGN ----
        update(session.copy(stage = BuildStage.ZIP_ALIGN))
        val alignedApk = File(stageDir, "aligned.apk")
        val alignResult = ProcessRunner.run(
            listOf(tools.zipalign!!.absolutePath, "-f", "4", unsignedApk.absolutePath, alignedApk.absolutePath),
            timeoutSeconds = 60,
            onOutput = { line, err -> log(BuildStage.ZIP_ALIGN, line, err) },
        )
        if (!alignResult.success) fail(BuildStage.ZIP_ALIGN, "zipalign failed (exit ${alignResult.exitCode}).")

        // ---- SIGN ----
        update(session.copy(stage = BuildStage.SIGN))
        val project = projectDao.getById(session.projectId)
        val outName = "${project?.name ?: "app"}-${if (session.target == BuildTarget.AAB) "release" else "debug"}.apk"
        val finalApk = File(toolchain.artifactsDir, outName)
        if (finalApk.exists()) finalApk.delete()

        val ks = toolchain.debugKeystore()
        val signing = SigningConfig.debugDefault(ks.absolutePath)
        val signResult = ProcessRunner.run(
            listOf(
                tools.apksigner!!.absolutePath, "sign",
                "--ks", signing.keystorePath,
                "--ks-key-alias", signing.alias,
                "--ks-pass", "pass:${signing.storePassword}",
                "--key-pass", "pass:${signing.keyPassword}",
                "--out", finalApk.absolutePath,
                alignedApk.absolutePath,
            ),
            timeoutSeconds = 120,
            onOutput = { line, err -> log(BuildStage.SIGN, line, err) },
        )
        if (!signResult.success) fail(BuildStage.SIGN, "apksigner failed (exit ${signResult.exitCode}).")

        log(BuildStage.DONE, "Build succeeded: ${finalApk.absolutePath}")
        update(
            session.copy(
                status = BuildStatus.SUCCESS,
                stage = BuildStage.DONE,
                finishedAt = System.currentTimeMillis(),
                artifactPath = finalApk.absolutePath,
            )
        )
    }

    // ------------------------------------------------------------- helpers

    private class BuildFailedException(msg: String) : RuntimeException(msg)

    private fun findJvmTool(name: String, tools: ToolchainManager.Toolchain): File? {
        if (name == "java" || name == "javac") {
            tools.java?.let { if (it.name.contains(name)) return it }
            // javac next to java
            tools.java?.parentFile?.let { parent ->
                File(parent, name).takeIf { it.isFile && it.canExecute() }?.let { return it }
            }
        }
        for (dir in listOf("/usr/bin", "/usr/local/bin", "/data/data/com.termux/files/usr/bin")) {
            File(dir, name).takeIf { it.isFile && it.canExecute() }?.let { return it }
        }
        return null
    }

    private fun collectLibraryJars(projectDir: File): List<File> {
        val dirs = listOf(
            File(projectDir, "libs"),
            File(projectDir, "app/libs"),
            File(context.filesDir, "toolchain/libs"),
        )
        return dirs.flatMap { d -> d.listFiles()?.filter { it.isFile && it.extension == "jar" } ?: emptyList() }
    }

    /** Inserts classes.dex as the first entry of the APK (v2-signing compatible). */
    private fun insertDex(apk: File, dex: File) {
        val tmp = File(apk.parentFile, "tmp-${apk.name}")
        java.util.zip.ZipOutputStream(tmp.outputStream().buffered()).use { out ->
            out.putNextEntry(java.util.zip.ZipEntry("classes.dex"))
            dex.inputStream().use { it.copyTo(out) }
            out.closeEntry()
            java.util.zip.ZipInputStream(apk.inputStream().buffered()).use { input ->
                var entry = input.nextEntry
                while (entry != null) {
                    if (entry.name != "classes.dex" && !entry.name.startsWith("META-INF/com/android/build/gradle/")) {
                        out.putNextEntry(java.util.zip.ZipEntry(entry.name))
                        input.copyTo(out)
                        out.closeEntry()
                    }
                    input.closeEntry()
                    entry = input.nextEntry
                }
            }
        }
        if (!tmp.renameTo(apk)) {
            tmp.copyTo(apk, overwrite = true)
            tmp.delete()
        }
    }

    private fun zipDirectory(dir: File, zipFile: File) {
        if (zipFile.exists()) zipFile.delete()
        java.util.zip.ZipOutputStream(zipFile.outputStream().buffered()).use { out ->
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = file.relativeTo(dir).invariantSeparatorsPath
                out.putNextEntry(java.util.zip.ZipEntry(rel))
                file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
    }

    private fun log(stage: BuildStage, message: String, isError: Boolean = false) {
        if (message.isBlank()) return
        val line = BuildLogLine(stage, message, isError)
        _logEvents.tryEmit(line)
        val current = _build.value
        _build.value = current.copy(log = BuildErrorParser.capLog(current.log + message + "\n"))
    }

    private suspend fun update(session: BuildSession) {
        _build.value = session
        persist(session)
    }

    private suspend fun persist(session: BuildSession): BuildSession {
        val entity = BuildSessionEntity(
            id = session.id.takeIf { it > 0 } ?: 0,
            projectId = session.projectId,
            target = session.target.name,
            status = session.status.name,
            stage = session.stage.name,
            startedAt = session.startedAt,
            finishedAt = session.finishedAt,
            artifactPath = session.artifactPath,
            log = BuildErrorParser.capLog(session.log),
            diagnostics = session.diagnostics,
        )
        val newId = sessionDao.upsert(entity)
        return session.copy(id = if (session.id > 0) session.id else newId)
    }

    private fun BuildSessionEntity.toDomain() = BuildSession(
        id = id,
        projectId = projectId,
        target = BuildTarget.valueOf(target),
        status = BuildStatus.valueOf(status),
        stage = runCatching { BuildStage.valueOf(stage) }.getOrDefault(BuildStage.PENDING),
        startedAt = startedAt,
        finishedAt = finishedAt,
        artifactPath = artifactPath,
        log = log,
        diagnostics = diagnostics,
    )
}
