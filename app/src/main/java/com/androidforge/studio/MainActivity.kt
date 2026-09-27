package com.androidforge.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.androidforge.studio.ui.ai.AiScreen
import com.androidforge.studio.ui.builder.UiBuilderScreen
import com.androidforge.studio.ui.build.BuildScreen
import com.androidforge.studio.ui.editor.EditorScreen
import com.androidforge.studio.ui.home.HomeScreen
import com.androidforge.studio.ui.nav.BottomBar
import com.androidforge.studio.ui.nav.Routes
import com.androidforge.studio.ui.plugins.PluginsScreen
import com.androidforge.studio.ui.settings.SettingsScreen
import com.androidforge.studio.ui.theme.AndroidForgeTheme
import com.androidforge.studio.ui.tools.ToolsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidForgeTheme {
                AndroidForgeRoot()
            }
        }
    }
}

@Composable
fun AndroidForgeRoot() {
    val navController: NavHostController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenProject = { id ->
                        navController.navigate("${Routes.EDITOR}/$id")
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.EDITOR) {
                EditorScreen(
                    projectId = -1L,
                    onOpenBuilder = { },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("${Routes.EDITOR}/{projectId}") { entry ->
                val pid = entry.arguments?.getString("projectId")?.toLongOrNull() ?: -1L
                EditorScreen(
                    projectId = pid,
                    onOpenBuilder = { navController.navigate("${Routes.BUILDER}/$pid") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.BUILD) {
                BuildScreen(projectId = -1L)
            }
            composable("${Routes.BUILDER}/{projectId}") { entry ->
                val pid = entry.arguments?.getString("projectId")?.toLongOrNull() ?: -1L
                UiBuilderScreen(projectId = pid, onBack = { navController.popBackStack() })
            }
            composable("${Routes.BUILD}/{projectId}") { entry ->
                val pid = entry.arguments?.getString("projectId")?.toLongOrNull() ?: -1L
                BuildScreen(projectId = pid)
            }
            composable(Routes.AI) { AiScreen() }
            composable(Routes.TOOLS) { ToolsScreen() }
            composable(Routes.PLUGINS) { PluginsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
