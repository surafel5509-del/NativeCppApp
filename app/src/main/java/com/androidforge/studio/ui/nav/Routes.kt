package com.androidforge.studio.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

/** Central route table - Professional IDE */
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val BUILDER = "builder"
    const val BUILD = "build"
    const val AI = "ai"
    const val TOOLS = "tools"
    const val PLUGINS = "plugins"
    const val SETTINGS = "settings"
    // Professional new routes
    const val RESOURCES = "resources"
    const val MANIFEST = "manifest"
    const val NDK = "ndk"
    const val GAME = "game"
    const val APK_ANALYZER = "apk_analyzer"
    const val LOGCAT = "logcat"
    const val DEVICE = "device"
    const val GRADLE = "gradle"
    const val GIT = "git"
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val professional: Boolean = false,
)

/**
 * Five-tab bottom navigation — thumb-reachable on phones:
 * Projects · Editor · Build · AI · Tools
 * Professional expansion with secondary navigation
 */
val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Projects", Icons.Filled.Home),
    BottomDestination(Routes.EDITOR, "Editor", Icons.Filled.Code, professional = true),
    BottomDestination(Routes.BUILD, "Build", Icons.Filled.Build, professional = true),
    BottomDestination(Routes.AI, "AI", Icons.Filled.SmartToy),
    BottomDestination(Routes.TOOLS, "Tools", Icons.Filled.Terminal, professional = true),
)

/** Extra destinations reachable from Tools/Settings (not shown in the bar) - Professional */
val secondaryDestinations = listOf(
    BottomDestination(Routes.PLUGINS, "Plugins", Icons.Filled.History),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
    BottomDestination(Routes.BUILDER, "UI Builder", Icons.Filled.Palette, professional = true),
    BottomDestination(Routes.RESOURCES, "Resources", Icons.Filled.Dashboard, professional = true),
    BottomDestination(Routes.NDK, "NDK", Icons.Filled.Memory, professional = true),
    BottomDestination(Routes.GAME, "Game", Icons.Filled.Gamepad, professional = true),
)

/** Professional IDE features list */
val professionalFeatures = listOf(
    "Real APK Compiler (aapt2, d8, apksigner, zipalign)" to "Build real APKs offline",
    "Run Button at Top" to "One-tap build & run",
    "NDK Support (C++/JNI, CMake, OpenGL ES)" to "Native development",
    "LibGDX Game Engine (Box2D, Assets)" to "Professional 2D games",
    "UI Builder (40+ components)" to "Visual design, drag & drop",
    "Error Catching & Diagnostics" to "File:line errors, clickable",
    "Offline Capable" to "Fully offline, no internet needed",
    "Project Import/Export" to "ZIP, Git, Folder",
    "File & Folder Creation" to "Templates, nested folders",
    "Professional Templates (14)" to "Compose, NDK, LibGDX, etc.",
)
