package com.androidforge.studio.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

/** Central route table. */
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val BUILDER = "builder"
    const val BUILD = "build"
    const val AI = "ai"
    const val TOOLS = "tools"
    const val PLUGINS = "plugins"
    const val SETTINGS = "settings"
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Five-tab bottom navigation — thumb-reachable on phones:
 * Projects · Editor · Build · AI · Tools.
 */
val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Projects", Icons.Filled.Home),
    BottomDestination(Routes.EDITOR, "Editor", Icons.Filled.Code),
    BottomDestination(Routes.BUILD, "Build", Icons.Filled.Build),
    BottomDestination(Routes.AI, "AI", Icons.Filled.SmartToy),
    BottomDestination(Routes.TOOLS, "Tools", Icons.Filled.Terminal),
)

/** Extra destinations reachable from Tools/Settings (not shown in the bar). */
val secondaryDestinations = listOf(
    BottomDestination(Routes.PLUGINS, "Plugins", Icons.Filled.History),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)
