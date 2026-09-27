package com.androidforge.studio.data.build

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locates the on-device build toolchain.
 *
 * Users import binaries once (aapt2, d8, apksigner, zipalign from any
 * Android SDK build-tools; kotlinc + a JDK 17; android.jar from a platform)
 * into the app sandbox:
 *
 * ```
 * filesDir/toolchain/bin/<tools…>     executables (chmod applied on resolve)
 * filesDir/sdk/platforms/android-35/android.jar
 * ```
 *
 * Termux prefixes and `$PATH` are searched as fallbacks so a Termux install
 * works out of the box.
 */
@Singleton
class ToolchainManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Toolchain(
        val aapt2: File?,
        val d8: File?,
        val apksigner: File?,
        val zipalign: File?,
        val kotlinc: File?,   // may be a wrapper script
        val java: File?,
        val aidl: File?,
    ) {
        /** Only the packaging tools are mandatory here; compilers get a friendlier error later. */
        val missing: List<String> get() = buildList {
            if (aapt2 == null) add("aapt2")
            if (d8 == null) add("d8")
            if (apksigner == null) add("apksigner")
            if (zipalign == null) add("zipalign")
        }

        val complete: Boolean get() = missing.isEmpty()
    }

    /** Where users drop toolchain binaries: Files ▸ toolchain/bin. */
    val toolchainBinDir: File
        get() = File(context.filesDir, "toolchain/bin")

    /** Android platform jar home: Files ▸ sdk/platforms/android-35. */
    val platformDir: File
        get() = File(context.filesDir, "sdk/platforms/android-35")

    fun androidJar(): File? {
        val direct = File(platformDir, "android.jar")
        if (direct.isFile) return direct
        // Fall back to whatever platform directory exists.
        val platforms = File(context.filesDir, "sdk/platforms")
        val jars = platforms.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.name }          // android-35 > android-34 …
            ?.mapNotNull { dir -> File(dir, "android.jar").takeIf { f -> f.isFile } }
            .orEmpty()
        return jars.firstOrNull()
    }

    fun resolve(): Toolchain = Toolchain(
        aapt2 = findTool("aapt2"),
        d8 = findTool("d8"),
        apksigner = findTool("apksigner"),
        zipalign = findTool("zipalign"),
        kotlinc = findTool("kotlinc"),
        java = findTool("java"),
        aidl = findTool("aidl"),
    )

    private fun findTool(name: String): File? {
        val candidates = buildList {
            add(File(toolchainBinDir, name))
            add(File(toolchainBinDir, "$name.sh"))
            // Termux
            add(File("/data/data/com.termux/files/usr/bin", name))
            add(File("/data/data/com.termux/files/usr/lib", name))
            // Android system locations (read-only, but harmless to probe)
            add(File("/system/bin", name))
            // $PATH
            val path = System.getenv("PATH") ?: ""
            for (dir in path.split(File.pathSeparator)) {
                if (dir.isNotBlank()) add(File(dir, name))
            }
        }
        for (file in candidates) {
            if (file.isFile && file.canRead()) {
                if (!file.canExecute()) file.setExecutable(true) // app-owned copies need +x
                if (file.canExecute() || file.parent == toolchainBinDir.absolutePath) return file
            }
        }
        return null
    }

    /** Per-build scratch directory: filesDir/build/<projectId>. */
    fun stageDir(projectId: Long): File =
        File(context.filesDir, "build/$projectId").apply { mkdirs() }

    /** Finished artifacts exposed through the FileProvider (`artifacts/` path). */
    val artifactsDir: File
        get() = File(context.filesDir, "artifacts").apply { mkdirs() }

    /** Extracts the bundled debug keystore (assets/signing/debug.p12) once. */
    fun debugKeystore(): File {
        toolchainBinDir.mkdirs()
        val target = File(toolchainBinDir, "androidforge-debug.p12")
        if (!target.exists()) {
            context.assets.open("signing/debug.p12").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return target
    }
}
