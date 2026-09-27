package com.androidforge.studio.data.ai

/**
 * Fully offline assistant. Deterministic pattern-based responses so the AI tab
 * is useful without any network or API key: code templates, explanations and
 * error triage for common Android/Kotlin issues.
 */
object LocalAssistant {

    fun respond(prompt: String, contextBlock: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("screen") || p.contains("composable") || p.contains("ui") -> screenTemplate(prompt)
            p.contains("error") || p.contains("exception") || p.contains("crash") -> errorTriage(prompt)
            p.contains("explain") || p.contains("what is") || p.contains("how does") -> explain(prompt)
            p.contains("gradle") || p.contains("dependency") || p.contains("build") -> gradleHelp(prompt)
            p.contains("test") || p.contains("junit") -> testTemplate()
            p.contains("viewmodel") || p.contains("mvvm") || p.contains("architecture") -> viewModelTemplate()
            p.contains("room") || p.contains("database") -> roomTemplate()
            p.contains("git") -> gitHelp()
            else -> generic(prompt, contextBlock)
        }
    }

    private fun screenTemplate(request: String) = """
## Generated Compose screen (offline template)

Here's a production-shaped screen for: *$request*

```kotlin
@Composable
fun GeneratedScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generated") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Hello from AndroidForge", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { /* TODO: wire action */ }) {
                Text("Continue")
            }
        }
    }
}
```

> Offline mode: connect an OpenAI/Gemini key in **Settings ▸ AI** for project-aware generation.
    """.trimIndent()

    private fun errorTriage(prompt: String): String {
        val rules = listOf(
            "Unresolved reference" to "The symbol isn't on the classpath. Check imports, add the dependency in app/build.gradle.kts, then Sync.",
            "ClassNotFoundException" to "Missing runtime dependency or ProGuard stripped it. Add a -keep rule or verify the dependency is `implementation` not `compileOnly`.",
            "NullPointerException" to "Use `?.` and `?:` defaults, or make the type nullable and handle both branches.",
            "IndexOutOfBounds" to "Guard with `indices.contains(i)` or use `getOrNull(i)`.",
            "NetworkOnMainThread" to "Move the call into a coroutine: `viewModelScope.launch { withContext(Dispatchers.IO) { … } }`.",
            "OutOfMemory" to "Large bitmap? Use Coil with size overrides; large file? Stream instead of readText().",
            "SecurityException" to "A required permission is missing — check the manifest and runtime permission flow.",
        )
        val matched = rules.filter { (k, _) -> prompt.contains(k, ignoreCase = true) }
        val advice = matched.joinToString("\n\n") { (k, v) -> "- **$k** → $v" }
        return buildString {
            appendLine("## Crash triage (offline rules)")
            appendLine()
            if (advice.isNotEmpty()) appendLine(advice) else {
                appendLine("No exact rule matched. General checklist:")
                appendLine("1. Read the **first** frame of the stack trace that's in your package.")
                appendLine("2. Reproduce in the Build tab output / logcat.")
                appendLine("3. Add a breakpoint in that frame (tap the gutter in the editor).")
            }
            appendLine()
            appendLine("Paste the exact `e:` line from the build log for line-accurate help.")
        }
    }

    private fun explain(prompt: String) = """
## Explanation (offline)

**Prompt:** $prompt

In short: Jetpack Compose describes UI as a tree of *composables* that re-run
whenever their state changes. State hoisting (keeping state above the composable
that uses it) makes UI testable and predictable:

- `remember { mutableStateOf(x) }` → local UI state
- `ViewModel + StateFlow` → survives rotation, holds business state
- `collectAsStateWithLifecycle()` → safe collection in Compose

Connect a cloud key in **Settings ▸ AI** for a deep, project-specific answer.
    """.trimIndent()

    private fun gradleHelp(prompt: String) = """
## Gradle help (offline)

Common fixes:
- **Sync fails**: `settings.gradle.kts` repositories must include `google()` + `mavenCentral()`.
- **Add a dependency** in `app/build.gradle.kts`:
  ```kotlin
  implementation("androidx.room:room-runtime:2.7.1")
  ksp("androidx.room:room-compiler:2.7.1")
  ```
- **Version conflict**: run `./gradlew app:dependencies --configuration releaseRuntimeClasspath`.
- **On device**: Sync lives in the Build tab; imported tooling lives in `toolchain/bin`.
    """.trimIndent()

    private fun testTemplate() = """
## Unit test template

```kotlin
class MyViewModelTest {
    @get:Rule val rule = MainDispatcherRule()

    @Test
    fun `initial state is idle`() = runTest {
        val vm = MyViewModel(FakeRepo())
        assertEquals(State.Idle, vm.uiState.first())
    }
}
```
Put it under `app/src/test/java/…`. Run in the Build tab ▸ **Run tests**.
    """.trimIndent()

    private fun viewModelTemplate() = """
## MVVM ViewModel template

```kotlin
@HiltViewModel
class ItemViewModel @Inject constructor(
    private val repo: ItemRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ItemUiState())
    val state: StateFlow<ItemUiState> = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        repo.items().collect { items ->
            _state.update { it.copy(loading = false, items = items) }
        }
    }
}
```
    """.trimIndent()

    private fun roomTemplate() = """
## Room template

```kotlin
@Entity data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao interface NoteDao {
    @Query("SELECT * FROM note ORDER BY updatedAt DESC") fun observeAll(): Flow<List<Note>>
    @Insert suspend fun upsert(note: Note)
}
```
    """.trimIndent()

    private fun gitHelp() = """
## Git (Tools ▸ Git tab)

- **Init** creates `.git` in the project root
- **Stage all** = `git add -A`
- **Commit** writes a commit with your name/email from Settings
- **Diff** shows hunks with color-coded add/del lines
- **Clone/Push/Pull** need a remote URL + PAT (Settings ▸ Cloud)
    """.trimIndent()

    private fun generic(prompt: String, context: String) = """
## Offline assistant

You asked: **$prompt**

I'm running in *offline mode* (no API key configured). I can still:

| Ask me… | Example |
|---|---|
| Generate UI | *"create a settings screen"* |
| Triage crashes | *"error: Unresolved reference: foo"* |
| Explain concepts | *"explain state hoisting"* |
| Gradle help | *"add room dependency"* |
| Templates | *"show a ViewModel template"* |

For full project-aware answers, add an OpenAI or Gemini key in **Settings ▸ AI**.

---
_context attached:_ ${context.ifBlank { "(none)" }}
    """.trimIndent()
}
