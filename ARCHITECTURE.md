# AndroidForge Studio — Architecture

This document explains how the app is structured, how data flows, and the design decisions behind each subsystem.

---

## 1. Layering

AndroidForge Studio follows **MVVM + Clean Architecture** with a strict dependency direction:

```
┌────────────────────────────────────────────────────────────┐
│ UI            Compose screens · ViewModels · UiState       │
│               (ui/…  — observes StateFlow, fires intents)  │
├────────────────────────────────────────────────────────────┤
│ DOMAIN        models (pure Kotlin data classes)            │
│               repository interfaces                        │
│               use cases (CreateProject, ReadFile, …)       │
├────────────────────────────────────────────────────────────┤
│ DATA          Room DB · DataStore · SecureVault            │
│               TemplateEngine · BuildPipeline · Shell       │
│               JGit · AiRepositoryImpl · PluginRepository   │
├────────────────────────────────────────────────────────────┤
│ PLATFORM      Android SDK (Keystore, WorkManager,          │
│               FileProvider, ProcessBuilder, Notification)  │
└────────────────────────────────────────────────────────────┘
```

- **Domain has zero Android imports** (except `java.io.File` for repo APIs) → unit-testable on the JVM.
- **UI never talks to Data directly** — only through use cases / repository interfaces from the `domain` package.
- **DI (Hilt)** binds interfaces to implementations in `di/RepositoryModule`.

## 2. Module & package map

Single Gradle module (`app`), packages:

| Package | Responsibility |
|---|---|
| `domain.model` | `Project`, `Build`(+Stages/LogLine/History), `Ai`, `Git`, `Plugin`, `Terminal`, `Debug` |
| `domain.repository` | interfaces: `ProjectRepository`, `BuildRepository`, `AiRepository`, `GitRepository`, `SettingsRepository`, `PluginRepository`, `ShellSessionRepository` |
| `domain.usecase` | `CreateProjectUseCase`, `ReadFileUseCase`, `SaveFileUseCase`, `StartBuildUseCase`… |
| `data.local` | Room `AppDatabase`, `ProjectDao`/`BuildDao`, entities, `SettingsStore` (DataStore) |
| `data.security` | `SecureVault` — Keystore AES-256-GCM sealed values |
| `data.template` | `TemplateEngine` — 6 sample projects as in-memory `Map<String,String>` |
| `data.build` | `ToolchainManager`, `BuildPipeline` (`BuildRepositoryImpl`), `ProcessRunner`, `BuildErrorParser`, `CloudBuildClient` |
| `data.terminal` | `ShellSessionRepository` — custom shell + optional `/system/bin/sh` bridge |
| `data.git` | `JGitRepository` — thin adapter over JGit porcelain |
| `data.ai` | `AiRepositoryImpl` (OpenAI/Gemini/Anthropic via Retrofit) + `LocalAssistant` (offline) |
| `data.plugin` | `PluginRepositoryImpl` — JSON manifests in `assets/plugins` + installed dirs |
| `ui.*` | feature packages (nav, home, editor, builder, ai, build, tools, plugins, settings, theme) |
| `worker` | `BuildWorker` — HiltWorker running builds in background with notifications |

## 3. State management

Each screen has exactly **one ViewModel** exposing `StateFlow<UiState>`:

```kotlin
data class HomeUiState(val projects: List<Project> = emptyList(), val busy: Boolean = false)
```

- ViewModels take injected repositories/use cases (constructor `@Inject`), never Android `Context` except `@ApplicationContext`.
- Screens are plain `@Composable`s: `val state by vm.uiState.collectAsStateWithLifecycle()` + one-shot events via `Channel`/`SharedFlow` where needed (snackbars).
- Long-running work (builds, clones, terminal) lives in the repositories with `CoroutineScope(SupervisorJob + Dispatchers.IO)`; ViewModels only observe Flows.

## 4. Navigation

`ui/nav/Routes.kt` defines a `NavHost` with **bottom navigation** (Home · Editor · Build · AI · Tools) plus secondary routes (`builder`, `plugins`, `settings`). Editor/Build take `?projectId=` optional args. One-hand mode moves the bottom bar up when enabled.

## 5. Persistence

| Store | Tech | Contents |
|---|---|---|
| Projects metadata | **Room** (`ProjectEntity`) | id, name, package, template, path, favorite, timestamps |
| Build history | **Room** (`BuildEntity`) | target, mode, stages, log tail, timestamps |
| Preferences | **DataStore** (`SettingsStore`) | theme, font size, one-hand, provider, sandbox, last opened project |
| Secrets | **Keystore-sealed strings inside DataStore** | OpenAI/Gemini/Anthropic keys, GitHub PAT (seal via `SecureVault`, unseal on read in `SettingsRepositoryImpl`) |
| Chat history | Room or per-project file (AI screen persists with project id) |
| Files | `filesDir/projects/<root>/` canonicalized sandbox |

Migrations: keep `fallbackToDestructiveMigration()` off; add `Migration` objects in `data/local/Migrations.kt` (schema exported via Room `exportSchema`).

## 6. Editor pipeline

```
raw text ──► SyntaxTokenizer (line-based) ──► VisualTransformation (AnnotatedString)
                 │ per-language rules (Kotlin/Java/XML/JSON/Gradle/…)
                 ▼
        CodeEditorState (selection, undo/redo stacks, cursor)
                 │
        Error underlines ← BuildErrorParser/Build diagnostics
```

- `CodeEditor` is a `BasicTextField` + `VisualTransformation` → highlighting costs only on text change, layout stays cheap.
- Tab bar keeps per-file dirty state; `SaveFileUseCase` writes through `ProjectRepository.writeFile`.
- Autocomplete pops from token prefix match against language keyword tables + project symbols.

## 7. UI builder

Component tree (`UiComponent`: type, id, props, children) is the single source of truth:

- **Canvas**: renders the tree with real Compose (tap to select, drag to reorder/reparent, handles for resize where applicable).
- **Live preview**: same renderer in read-only mode.
- **Codegen**: `ComposeCodeGenerator` and `XmlCodeGenerator` walk the tree deterministically (imports collected, then emitted).
- **Export**: writes the generated file into the open project (e.g. `ui/GeneratedScreen.kt` or `res/layout/generated.xml`).

## 8. Build pipeline (on-device)

`BuildRepositoryImpl.runLocal()` executes staged processes via `ProcessRunner` (line-streamed into `Flow<BuildLogLine>`):

```
0 SYNCHRONIZE  gradle-less sync: merge manifest, R.java generation (aapt2 compile+link --proto-format)
1 COMPILE_KT   kotlinc: kotlin+java sources → classes.jar
2 COMPILE_JAVA javac (desktop JDK) for java sources
3 DEX          d8 → classes.dex
4 PACKAGE      zip: AndroidManifest.xml(proto), dex, res, assets, lib, META-INF
5 ALIGN        zipalign -p 4
6 SIGN         apksigner (debug.p12) → app-debug.apk
7 DONE         FileProvider URI → ACTION_VIEW install intent
```

- Each stage reports `BuildStageState(RUNNING/OK/FAILED/SKIPPED)`; first failure stops the pipeline and `BuildErrorParser` extracts `file:line` diagnostics the editor can jump to.
- **ToolchainManager** locates tools in `filesDir/toolchain`, Termux prefixes, or `$PATH`; `ToolStatus` powers `forge doctor`.
- **AAB target** runs bundle steps (zip of proto manifest + resources + dex) instead of APK packaging.
- **Cloud mode**: `CloudBuildClient` (PAT + repo + workflow id from settings) dispatches `workflow_dispatch`, polls runs, downloads the artifact zip.
- **Backgrounding**: `BuildWorker` (HiltWorker, expedited) observes `builds.observeBuild()` until terminal state and posts progress notifications; UI re-attaches by build id.

## 9. AI assistant

```
prompt + context flags ─► AiRepositoryImpl
                            ├── provider == LOCAL → LocalAssistant (pattern rules)
                            ├── provider OPENAI/GEMINI/ANTHROPIC → Retrofit API
                            └── transport key → SecureVault unseal
                                   ▼
                            AiReply (text) → per-project history store → UI
```

`LocalAssistant` pattern-matches screen/error/gradle/test/mvvm questions to deterministic templates so the app is useful offline and in CI screenshots.

## 10. Terminal & sandboxing

- Commands are parsed by `ShellSessionRepository`; **built-ins** (`ls cd cat tree forge …`) resolve paths inside the project sandbox (`CanonicalPathSandbox`), rejecting `..` escapes with `SecurityException`.
- `forge tasks` lists plugin build tasks; `forge doctor` prints toolchain status.
- Optional external execution (`sh -c`) is behind `SettingsRepository.sandboxedShell == false` (default ON = blocked).

## 11. Git

`JGitRepository` wraps porcelain commands; operations run on `Dispatchers.IO`, results map into `GitStatusEntry/GitLogEntry/GitDiffEntry` models. Clone/push take credentials from settings (PAT sealed).

## 12. Plugins

Manifest JSON: `{id,name,version,description,author,type,entry}` where `type ∈ THEME | TEMPLATE | BUILD_TASK | MARKETPLACE`. Loaded from `assets/plugins/*.json` plus `filesDir/plugins/`. Enable/disable state is a preference map; `BUILD_TASK` entry file is `name = shell command` lines → surfaced in Terminal (`forge tasks`) and Tools.

## 13. Background work & notifications

- `BuildWorker` — OneTime, expedited, tag `androidforge-build`, channel `AndroidForgeApp.CHANNEL_BUILDS`.
- Foreground conversion guarded with `runCatching` (Android 12+ `setForeground` restrictions).

## 14. Security model

| Threat | Control |
|---|---|
| Path traversal by project file ops | `CanonicalPathSandbox` — every resolved path must stay under `filesDir/projects` |
| Secrets in DataStore plaintext | AES-256-GCM via Android Keystore (`SecureVault`), used transparently by `SettingsRepositoryImpl` |
| Shell escaping project dir | sandbox flag (default on) → only in-process built-ins |
| Signing key theft | debug keystore only in `assets/`, documented as debug-only; release signing left to user's own keystore |

## 15. Testing strategy

- **Unit (JVM)**: tokenizer, error parser, template engine (all 6 templates, manifest, gradle files), codegen (Compose/XML round-trip), crash analyzer, `LocalAssistant`, `CreateProjectUseCase` with fake repositories, settings store logic.
- **UI (instrumented)**: Compose rule tests for critical flows (project creation dialog, editor render).
- Domain purity makes most logic testable without Robolectric; Android-only pieces are isolated behind interfaces.

## 16. Build & toolchain versions

Pinned (keep in sync across `build.gradle.kts` / `settings.gradle.kts` / workflow):

```
Gradle 8.11.1 · AGP 8.7.3 · Kotlin 2.1.0 (compose compiler 2.1.0)
KSP 2.1.0-1.0.29 · Hilt 2.54 · Room 2.7.1 · Compose BOM 2024.12.01
compileSdk 35 · minSdk 26 · targetSdk 35 · JVM target 17 · JDK 17
```

CI: `.github/workflows/android-build.yml` → checkout, JDK 17, `./gradlew test assembleDebug`, artifact upload. **No NDK/CMake** — the app is pure Kotlin (the on-device *toolchain* is user-supplied, not compiled into the app).

## 17. Extension points

- **New template** → add `Template` enum entry + `when` branch in `TemplateEngine.generate`.
- **New editor language** → add rule set in `SyntaxTokenizer`.
- **New AI provider** → add `AiProvider` entry + Retrofit service + routing in `AiRepositoryImpl`.
- **New build stage** → extend `BuildStage` enum; `BuildRepositoryImpl.runLocal` pipeline + `BuildWorker` progress mapping pick it up automatically.
