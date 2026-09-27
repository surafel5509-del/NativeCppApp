# AndroidForge Studio

**A mobile-only Android IDE that builds Android apps entirely on a phone — from project creation to a signed APK/AAB.**

AndroidForge Studio goes beyond AIDE by combining a Compose-native code editor, a drag & drop UI builder with live preview, an AI assistant (cloud + offline), a staged on-device build pipeline, a sandboxed terminal, real Git (JGit), a plugin system, a crash analyzer, and optional GitHub Actions cloud builds — all optimized for one-handed phone use.

---

## Feature matrix

| Area | What you get |
|---|---|
| **Projects** | Create/open/import/export/delete · 6 templates (Empty Compose, XML Layout, Firebase, E-commerce, Chat, 2D Game) · project explorer · favorites |
| **Editor** | Kotlin/Java/XML/JSON/Gradle/HTML/CSS/JS highlighting · line numbers · error underlines · search · undo/redo · multi-tab · auto-suggestions · 512 KB safety cap |
| **UI Builder** | Component palette · tap/drag-drop canvas · properties panel · **live preview** · codegen to **Compose and XML** · export into the project |
| **AI Assistant** | OpenAI · Gemini · **offline rule-based fallback** · project context flags (tree/file/errors) · quick actions · per-project chat history |
| **Build** | Staged pipeline: sync → aapt2 → kotlinc/javac → d8 → package → zipalign → apksigner · APK/AAB target · live log · diagnostics · install & run · **WorkManager background builds** · **cloud build via GitHub Actions** |
| **Terminal** | Custom shell: `ls cd pwd cat echo mkdir touch rm tree clear help forge` · sandboxed to project dir (optional external `/system/bin/sh`) · plugin build tasks |
| **Git** | JGit: init/status/log/diff/commit/checkout/clone/push/pull · color diff viewer |
| **Debug** | Paste a `FATAL EXCEPTION` → parsed crash report with **project frames highlighted** · breakpoints model |
| **Plugins** | JSON manifests (THEME/TEMPLATE/BUILD_TASK/MARKETPLACE) · bundled marketplace assets · enable/disable · uninstall |
| **Settings** | Light/dark/system theme · one-hand mode · editor font size · AI provider + keys · cloud PAT · sandboxed shell |
| **Security** | Android Keystore AES-256-GCM sealing for API keys/tokens · path-traversal-safe sandbox FS · sandboxed shell |

---

## Architecture (short version)

MVVM + Clean Architecture with a strict unidirectional flow:

```
UI (Compose screens + ViewModels, StateFlow<UiState>)
        │  calls
Domain (models · repository interfaces · use cases)
        │  implemented by
Data   (Room · DataStore · SecureVault · TemplateEngine · BuildPipeline ·
        ShellSession · JGit · AiRepositoryImpl · PluginRepository)
        │
Platform (WorkManager · FileProvider · ProcessBuilder · Android Keystore)
```

- **100% Jetpack Compose + Material 3**, no XML app layouts.
- **Hilt** for DI (`di/`), **Room** for persistence (`data/local/`), **DataStore** for preferences.
- Every screen has a `StateFlow<UiState>` ViewModel; screens are stateless-composable friendly (tested).
- The build pipeline emits `BuildSession` state + log lines the UI collects.

Full details: **[ARCHITECTURE.md](ARCHITECTURE.md)**.

---

## Project layout

```
app/
├── build.gradle.kts                 # Compose BOM, Hilt, Room/KSP, WorkManager…
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── assets/
    │   │   ├── signing/debug.p12    # debug keystore used by apksigner
    │   │   └── plugins/*.json       # bundled plugin marketplace
    │   ├── res/                     # theme, strings, adaptive launcher icon
    │   └── java/com/androidforge/studio/
    │       ├── AndroidForgeApp.kt   # @HiltAndroidApp + WorkManager config
    │       ├── MainActivity.kt      # edge-to-edge + NavHost + bottom bar
    │       ├── worker/BuildWorker.kt
    │       ├── di/                  # DatabaseModule, RepositoryModule, AppModule
    │       ├── domain/
    │       │   ├── model/           # Project, Build, Ai, Git/Plugin, Terminal/Debug
    │       │   ├── repository/      # interfaces
    │       │   └── usecase/         # CreateProject, ReadFile, SaveFile, Build…
    │       ├── data/
    │       │   ├── local/           # Room DB, DAOs, entities, DataStore
    │       │   ├── security/        # SecureVault (Keystore AES-GCM)
    │       │   ├── repository/      # ProjectRepositoryImpl, SettingsRepositoryImpl
    │       │   ├── template/        # TemplateEngine (6 sample projects)
    │       │   ├── build/           # ToolchainManager, BuildRepositoryImpl,
    │       │   │                    # ProcessRunner, BuildErrorParser, CloudBuildClient
    │       │   ├── terminal/        # ShellSessionRepository (custom shell)
    │       │   ├── git/             # JGitRepository
    │       │   ├── ai/              # AiRepositoryImpl + LocalAssistant (offline)
    │       │   └── plugin/          # PluginRepositoryImpl
    │       └── ui/
    │           ├── theme/           # Material3 theme (Forge palette)
    │           ├── nav/             # routes + bottom bar
    │           ├── home/            # project list + create dialog
    │           ├── editor/          # CodeEditor, SyntaxTokenizer, tree, tabs
    │           ├── builder/         # UI builder (palette/canvas/codegen)
    │           ├── ai/              # chat screen
    │           ├── build/           # build screen (stages, log, history)
    │           ├── tools/           # terminal/git/diff/debug tabs
    │           ├── plugins/         # marketplace
    │           └── settings/        # preferences
    ├── test/                        # JUnit4 unit tests
    └── androidTest/                 # Compose UI tests
```

---

## Requirements & build

| | |
|---|---|
| JDK | 17 |
| Gradle | 8.11.1 (wrapper included) |
| AGP | 8.7.3 · Kotlin 2.1.0 · KSP 2.1.0-1.0.29 · Hilt 2.51.1 |
| SDK | compileSdk/targetSdk **35**, minSdk **26** |

```bash
# unit tests + lint-free debug build
./gradlew test assembleDebug

# install on a connected device
./gradlew installDebug
```

CI (`.github/workflows/android-build.yml`) runs tests and uploads the debug APK on every push/PR.

---

## Building apps *on the phone* (on-device toolchain)

The on-device pipeline needs native tools. They are **not** bundled (licensing/size); the Build tab tells you exactly what's missing. Provision them once:

1. `Files ▸ toolchain/bin/` — copy `aapt2`, `d8`, `apksigner`, `zipalign` (from any Android SDK *build-tools* 35.x)
2. `sdk/platforms/android-35/android.jar` — platform jar
3. `kotlinc/` + a JDK 17 (`java`, `javac`) — or Termux: `pkg install openjdk-17 kotlin`
4. Press **Build ▸ Assemble** — stages light up as they complete.

Alternatively:

- **Cloud mode** (Settings ▸ Cloud Build): put `ghp_TOKEN|owner/repo|android-build.yml` and hit Build with the **Cloud** chip enabled. The bundled workflow builds and packages artifacts with GitHub Actions; the studio downloads the artifact.
- **Gradle projects** created from templates also build on desktop with `./gradlew assembleDebug`.

Check your setup any time in **Terminal ▸ `forge doctor`**.

---

## Sample projects (templates)

Create → pick a template:

| Template | Highlights |
|---|---|
| **Empty Compose** | Single-activity Compose + Material 3, light/dark |
| **XML Layout** | AppCompat + ConstraintLayout/LinearLayout, View system |
| **Firebase App** | Firebase BOM (Auth/Firestore) + `google-services.json.example` |
| **E-commerce** | Product list/detail, sample catalog, price formatting |
| **Chat App** | Message bubbles, composer, in-memory repository |
| **2D Game** | Compose `Canvas` game loop, touch input, score HUD |

Every template ships `settings.gradle.kts`, `build.gradle.kts` (root + app), manifest, ProGuard rules, `.gitignore`, and a README — a complete, desktop-buildable project.

---

## Tests

```bash
./gradlew test              # unit tests (JVM)
./gradlew connectedAndroidTest   # Compose UI tests (device required)
```

Unit coverage focuses on the engine layer: tokenizer, error parser, template engine, code generators, crash analyzer, offline AI, and `CreateProjectUseCase` (with fakes).

---

## Security notes

- API keys/tokens are sealed with **Android Keystore AES-256-GCM** before touching DataStore.
- The file repository **canonicalizes** every path; traversal outside `filesDir/projects` throws `SecurityException`.
- Terminal defaults to **sandboxed built-ins**; external commands require explicitly disabling the sandbox.
- The debug keystore (`assets/signing/debug.p12`, password `android`) is for **local debug signing only** — create your own keystore for releases.

## License

Apache-2.0 (see repository license). Project templates generated by the studio belong to the user who generates them.
