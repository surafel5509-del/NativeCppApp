package com.androidforge.studio.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class GameAsset(
    val name: String,
    val type: String,
    val size: String,
    val path: String,
)

data class GameManagerState(
    val engine: String = "LibGDX 1.12.1 - Professional",
    val assets: List<GameAsset> = emptyList(),
    val screens: List<String> = listOf("LoadingScreen", "GameScreen", "MenuScreen", "PauseScreen"),
    val entities: List<String> = listOf("Player", "Enemy", "Bullet", "PowerUp"),
    val fps: Int = 60,
)

@HiltViewModel
class GameManagerViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(GameManagerState())
    val state: StateFlow<GameManagerState> = _state.asStateFlow()

    init {
        _state.value = GameManagerState(
            assets = listOf(
                GameAsset("player.png", "Texture", "256 KB", "assets/player.png"),
                GameAsset("enemy.png", "Texture", "128 KB", "assets/enemy.png"),
                GameAsset("background.png", "Texture", "512 KB", "assets/background.png"),
                GameAsset("jump.wav", "Sound", "64 KB", "assets/jump.wav"),
                GameAsset("bgm.mp3", "Music", "2.4 MB", "assets/bgm.mp3"),
                GameAsset("game.atlas", "Atlas", "32 KB", "assets/game.atlas"),
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameManagerScreen(viewModel: GameManagerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Game Manager - LibGDX Professional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("2D Game Engine • Box2D • Assets • 60 FPS • Offline", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {}, icon = { Icon(Icons.Filled.PlayArrow, null) }, text = { Text("Run Game - Professional") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.Gamepad, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("LibGDX Professional Game Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("Professional 2D game development with LibGDX 1.12.1, Box2D physics, asset manager, screens, particles, 60 FPS - Offline capable", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Engine: ${state.engine}", style = MaterialTheme.typography.labelMedium)
                        Text("FPS: ${state.fps} - Professional game loop", style = MaterialTheme.typography.labelMedium)
                        Text("Offline: Fully offline, no internet required - Professional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Professional LibGDX Features", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("• Core module (game logic, screens, entities) - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Android launcher (AndroidApplication, config) - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Asset manager (textures, sounds, atlases) - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Box2D physics (bodies, fixtures, world) - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Screens: Loading, Menu, Game, Pause - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Entities: Player, Enemy, Bullet, PowerUp - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• 60 FPS game loop with delta time - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Input: touch, gestures, accelerometer - Professional", style = MaterialTheme.typography.labelSmall)
                        Text("• Build: APK with assets, NDK optional - Professional", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Game Assets - Professional Manager", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        state.assets.forEach { asset ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(asset.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(asset.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text(asset.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(asset.size, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Screens - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            state.screens.forEach { Text("• $it", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Entities - Professional", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            state.entities.forEach { Text("• $it", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sample GameScreen.kt - Professional LibGDX", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            """
                            class GameScreen : Screen {
                                private lateinit var world: World
                                private lateinit var player: Player
                                override fun render(delta: Float) {
                                    world.step(delta, 6, 2)
                                    // 60 FPS professional game loop
                                }
                            }
                            """.trimIndent(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        )
                    }
                }
            }
        }
    }
}


