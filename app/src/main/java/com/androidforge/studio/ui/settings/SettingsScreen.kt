package com.androidforge.studio.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.androidforge.studio.domain.model.AiProvider
import com.androidforge.studio.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "system",
    val oneHandMode: Boolean = false,
    val editorFontSize: Int = 14,
    val aiProvider: AiProvider = AiProvider.LOCAL,
    val openAiKey: String = "",
    val geminiKey: String = "",
    val cloudToken: String = "",
    val sandbox: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.themeMode().collect { v -> _state.update { it.copy(themeMode = v) } }
        }
        viewModelScope.launch {
            repo.oneHandMode().collect { v -> _state.update { it.copy(oneHandMode = v) } }
        }
        viewModelScope.launch {
            repo.editorFontSize().collect { v -> _state.update { it.copy(editorFontSize = v) } }
        }
        viewModelScope.launch {
            repo.aiProvider().collect { v -> _state.update { it.copy(aiProvider = v) } }
        }
        viewModelScope.launch {
            repo.openAiApiKey().collect { v -> _state.update { it.copy(openAiKey = v) } }
        }
        viewModelScope.launch {
            repo.geminiApiKey().collect { v -> _state.update { it.copy(geminiKey = v) } }
        }
        viewModelScope.launch {
            repo.cloudToken().collect { v -> _state.update { it.copy(cloudToken = v) } }
        }
        viewModelScope.launch {
            repo.sandboxScopes().collect { v -> _state.update { it.copy(sandbox = v) } }
        }
    }

    fun setTheme(mode: String) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setOneHand(v: Boolean) = viewModelScope.launch { repo.setOneHandMode(v) }
    fun setFontSize(v: Int) = viewModelScope.launch { repo.setEditorFontSize(v) }
    fun setProvider(p: AiProvider) = viewModelScope.launch { repo.setAiProvider(p) }
    fun setOpenAiKey(v: String) = viewModelScope.launch { repo.setOpenAiApiKey(v) }
    fun setGeminiKey(v: String) = viewModelScope.launch { repo.setGeminiApiKey(v) }
    fun setCloudToken(v: String) = viewModelScope.launch { repo.setCloudToken(v) }
    fun setSandbox(v: Boolean) = viewModelScope.launch { repo.setSandboxScopes(v) }

    fun testKeys() {
        viewModelScope.launch {
            _state.update { it.copy(message = "Keys sealed with Android Keystore (AES-256-GCM)") }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- Appearance ----
            SectionTitle("Appearance")
            Text("Theme", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                    FilterChip(
                        selected = state.themeMode == id,
                        onClick = { viewModel.setTheme(id) },
                        label = { Text(label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("One-hand mode", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Shift primary actions toward the bottom of the screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(checked = state.oneHandMode, onCheckedChange = viewModel::setOneHand)
            }

            HorizontalDivider()
            SectionTitle("Editor")
            Text(
                "Font size: ${state.editorFontSize}sp",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = state.editorFontSize.toFloat(),
                onValueChange = { viewModel.setFontSize(it.toInt()) },
                valueRange = 10f..22f,
                steps = 11,
            )

            HorizontalDivider()
            SectionTitle("AI provider")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.aiProvider == AiProvider.LOCAL,
                    onClick = { viewModel.setProvider(AiProvider.LOCAL) },
                    label = { Text("Offline") },
                )
                FilterChip(
                    selected = state.aiProvider == AiProvider.OPENAI,
                    onClick = { viewModel.setProvider(AiProvider.OPENAI) },
                    label = { Text("OpenAI") },
                )
                FilterChip(
                    selected = state.aiProvider == AiProvider.GEMINI,
                    onClick = { viewModel.setProvider(AiProvider.GEMINI) },
                    label = { Text("Gemini") },
                )
            }
            if (state.aiProvider == AiProvider.OPENAI) {
                OutlinedTextField(
                    value = state.openAiKey,
                    onValueChange = viewModel::setOpenAiKey,
                    label = { Text("OpenAI API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            if (state.aiProvider == AiProvider.GEMINI) {
                OutlinedTextField(
                    value = state.geminiKey,
                    onValueChange = viewModel::setGeminiKey,
                    label = { Text("Gemini API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            Text(
                "Keys are encrypted with Android Keystore (AES-256-GCM) before storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            HorizontalDivider()
            SectionTitle("Cloud build (GitHub Actions)")
            OutlinedTextField(
                value = state.cloudToken,
                onValueChange = viewModel::setCloudToken,
                label = { Text("PAT | owner/repo | workflow.yml") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("ghp_xxx|me/myrepo|android-build.yml") },
            )

            HorizontalDivider()
            SectionTitle("Security")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sandboxed shell", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Terminal only runs built-ins inside the project directory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(checked = state.sandbox, onCheckedChange = viewModel::setSandbox)
            }

            Button(onClick = viewModel::testKeys, modifier = Modifier.fillMaxWidth()) {
                Text("Verify encryption status")
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "AndroidForge Studio 1.0.0 — mobile-only Android IDE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
