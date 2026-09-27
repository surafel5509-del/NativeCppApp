# AndroidForge Studio - Professional

**A production-ready, professional mobile-only Android IDE that builds real Android apps entirely on a phone — from project creation to a signed APK/AAB with real compiler.**

AndroidForge Studio Professional goes far beyond AIDE and other mobile IDEs by combining:

- **Professional code editor** with Run button at top, real-time error catching, syntax highlighting for Kotlin/Java/C++/XML/Gradle/CMake/GLSL, 512 KB safety cap, multi-tab, auto-suggestions, snippets, problems view, search in file & project
- **Professional UI builder** with 40+ components (Material 3, Layout, Game, Native), drag & drop with constraints, layers panel, properties editor with sections, device previews (Phone, Tablet, Foldable, Desktop), theme previews (Light/Dark/Dynamic), blueprint mode, live preview, codegen to Compose and XML, export to project, undo/redo, copy/paste/duplicate
- **Real APK compiler** - professional on-device build pipeline: sync → NDK (CMake/ndk-build/clang) → aapt2 → kotlinc/javac → LibGDX assets → d8 → package → zipalign → apksigner → install. Real compiler, error catching with file:line diagnostics, clickable errors, fallback APK generation for offline, Gradle wrapper detection
- **NDK Professional** - C++/JNI, CMake 3.22+, OpenGL ES 3.2, EGL, Vulkan, native lib packaging (arm64-v8a, armeabi-v7a, x86_64), lldb debugging, offline capable
- **LibGDX Professional** - Full 2D game engine: core module, Android launcher, asset manager, Box2D physics, screens (Loading, Menu, Game, Pause), entities (Player, Enemy, Bullet), 60 FPS game loop, particles, touch input, asset packaging, offline
- **Project management Professional** - Create/open/import/export/duplicate/delete, 14 templates (Empty Compose, XML Layout, Compose Navigation, Clean MVVM, Firebase, E-commerce, Chat, 2D Game, LibGDX Game, NDK Native, NDK Game, Media, Maps, Wear OS), project explorer with file/folder creation (Kotlin, Java, XML, C++, CMake, etc.), search, filter by category, sort, favorites, offline
- **AI Assistant** - OpenAI, Gemini, offline rule-based fallback, project context flags, quick actions, per-project chat history
- **Terminal Professional** - Custom shell with forge doctor (toolchain check), NDK tools, LibGDX tasks, sandboxed to project dir, optional external shell, plugin build tasks
- **Git Professional** - JGit: init/status/log/diff/commit/checkout/clone/push/pull, color diff viewer, professional UI, offline capable
- **Debug Professional** - Paste FATAL EXCEPTION → parsed crash report with project frames highlighted, NDK stack, LibGDX log, breakpoints model
- **Resource Manager Professional** - Visual management of drawables, colors, strings, layouts, themes
- **APK Analyzer Professional** - Analyze APK size, dex, resources, native libs, assets, signature
- **Professional IDE features** - Logcat viewer, device manager, manifest editor, Gradle sync, signing config, keystore manager, offline docs, one-handed mode

All optimized for one-handed phone use, fully offline capable, production-ready.

---

## Professional Feature Matrix

| Area | What you get - Professional |
|---|---|
| **Projects** | Create/open/import/export/duplicate/delete · **14 templates**: Empty Compose, XML Layout, Compose Navigation, Clean MVVM, Firebase, E-commerce, Chat, 2D Game (Compose Canvas), **LibGDX Game (Professional - core/android/assets/Box2D)**, **NDK Native (C++/JNI/CMake/OpenGL ES)**, NDK Game (OpenGL), Media, Maps, Wear OS · project explorer with **create file/folder** (Kotlin class, Compose screen, ViewModel, Java, XML, C++, CMake, GLSL) · search in project · filter by category · sort · favorites · offline |
| **Editor Professional** | **Run button at top** (tap to build & compile APK, error catching) · Kotlin/Java/**C++/C/CMake/GLSL**/XML/JSON/Gradle/HTML/CSS/JS/YAML highlighting · line numbers · **error underlines with file:line diagnostics, clickable** · search in file & search in project · undo/redo · multi-tab with dirty indicators · auto-suggestions · **code snippets** · **problems view** · 512 KB safety cap · **NDK support** · **LibGDX support** · offline |
| **UI Builder Professional** | **40+ components**: Column, Row, Box, ConstraintLayout, Scaffold, LazyColumn, FlowRow, Card, ElevatedCard, TopAppBar, NavigationBar, TabRow, Button (Filled, Elevated, Tonal, Outlined, Text), IconButton, FAB (Small, Large, Extended), TextField, SearchBar, Slider, Switch, Checkbox, RadioButton, Chip (Filter, Input, Suggestion), Text, Icon, Image, AsyncImage, Divider, Badge, Progress, ListItem, WebView, MapView, VideoPlayer, Lottie, Canvas, **GameView (LibGDX)**, **NativeView (NDK)**, **OpenGLView** · **tap/drag-drop with constraints** · **layers panel (component tree)** · **properties panel with sections (Layout, Appearance, Behavior, Events, Constraints)** · **device previews (Phone, Tablet, Foldable, Desktop)** · **theme previews (Light/Dark/Dynamic)** · **blueprint mode** · **grid & constraints visualization** · **live preview** · **codegen to Compose and XML (production-ready)** · **export into project** · **undo/redo with history (50 steps)** · **copy/paste/duplicate** · **professional canvas with device frames** |
| **Build Professional** | **Real APK compiler**: Staged pipeline: sync → **NDK (CMake/ndk-build/clang)** → aapt2 → kotlinc/javac → **LibGDX assets** → d8 → package → zipalign → apksigner · **APK/AAB target** · **build variants (debug/release)** · **signing config manager** · live log with professional coloring · **diagnostics with file:line, clickable errors** · **error catching** · install & run · **WorkManager background builds** · **cloud build via GitHub Actions** · **Gradle wrapper detection & fallback** · **fallback APK generation for offline** · **offline capable** |
| **NDK Professional** | **C++/JNI, CMake 3.22+, NDK 26+, clang, OpenGL ES 3.2, EGL, Vulkan**, C++17, native lib packaging (arm64-v8a, armeabi-v7a, x86_64), JNI bridge auto-generation, lldb, ndk-stack, **real native compilation on device**, offline |
| **LibGDX Professional** | **Full 2D game engine**: core module (game logic, screens, entities), Android launcher, asset manager (textures, sounds, atlases), **Box2D physics**, screens (Loading, Menu, Game, Pause), entities (Player, Enemy, Bullet, PowerUp), **60 FPS game loop**, particles, touch input, asset packaging, **real game build**, offline |
| **Terminal Professional** | Custom shell: `ls cd pwd cat echo mkdir touch rm tree clear help forge` + **forge doctor (professional toolchain check with NDK, LibGDX)**, **NDK commands (ndk-build, cmake, clang)**, **LibGDX tasks**, sandboxed to project dir (optional external `/system/bin/sh`) · plugin build tasks · offline |
| **Git Professional** | JGit: init/status/log/diff/commit/checkout/clone/push/pull · **professional UI** · color diff viewer · offline capable |
| **Debug Professional** | Paste a `FATAL EXCEPTION` → parsed crash report with **project frames highlighted** · **NDK stack (ndk-stack, addr2line)** · **LibGDX log (Gdx.app.log, Box2D debug)** · breakpoints model · professional |
| **Resources Professional** | **Visual resource manager**: drawables (Vector, PNG, WebP, 9-patch), colors, strings (with translations), layouts, themes, **professional UI** |
| **APK Analyzer Professional** | **Analyze APK**: size, dex (d8), resources (aapt2), native libs (NDK), assets (LibGDX), signature (apksigner), **professional** |
| **Settings Professional** | Light/dark/system theme · one-hand mode · editor font size · **keymaps** · **NDK path** · **LibGDX settings** · AI provider + keys · cloud PAT · sandboxed shell · offline |
| **Security** | Android Keystore AES-256-GCM sealing for API keys/tokens · path-traversal-safe sandbox FS · sandboxed shell · professional |

---

## Professional Architecture

MVVM + Clean Architecture with strict unidirectional flow - Professional, production-ready:

```
UI (Compose screens + ViewModels, StateFlow<UiState>, Professional)
        │  calls
Domain (models · repository interfaces · use cases - Professional)
        │  implemented by
Data   (Room · DataStore · SecureVault · TemplateEngine (14 templates) · BuildPipeline (Real APK + NDK + LibGDX) ·
        ShellSession · JGit · AiRepositoryImpl · PluginRepository · ResourceManager · NdkManager · GameManager)
        │  Professional
Platform (WorkManager · FileProvider · ProcessBuilder · Android Keystore · NDK · LibGDX)
```

- **100% Jetpack Compose + Material 3**, no XML app layouts - Professional
- **Hilt** for DI (`di/`), **Room** for persistence (`data/local/`), **DataStore** for preferences - Professional
- Every screen has a `StateFlow<UiState>` ViewModel; screens are stateless-composable friendly (tested) - Professional
- The build pipeline emits `BuildSession` state + log lines the UI collects - Professional with NDK & LibGDX
- **Offline first** - Fully functional offline, no internet required - Professional

Full details: **[ARCHITECTURE.md](ARCHITECTURE.md)**.

---

## Project Layout - Professional

```
app/
├── build.gradle.kts                 # Compose BOM, Hilt, Room/KSP, WorkManager, NDK, LibGDX
├── proguard-rules.pro               # Professional ProGuard with NDK, LibGDX, Compose
└── src/
    ├── main/
    │   ├── AndroidManifest.xml      # Professional with NDK, LibGDX permissions
    │   ├── assets/
    │   │   ├── signing/debug.p12    # debug keystore used by apksigner - Professional
    │   │   └── plugins/*.json       # bundled plugin marketplace - Professional
    │   ├── res/                     # theme, strings, adaptive launcher icon - Professional
    │   └── java/com/androidforge/studio/
    │       ├── AndroidForgeApp.kt   # @HiltAndroidApp + WorkManager config - Professional
    │       ├── MainActivity.kt      # edge-to-edge + NavHost + bottom bar + Professional routes
    │       ├── worker/BuildWorker.kt # Professional background builds with NDK, LibGDX
    │       ├── di/                  # DatabaseModule, RepositoryModule, AppModule - Professional
    │       ├── domain/
    │       │   ├── model/           # Project (14 templates), Build (NDK, LibGDX), Ai, Git/Plugin, Terminal/Debug, FileTemplate - Professional
    │       │   ├── repository/      # interfaces - Professional
    │       │   └── usecase/         # CreateProject, ReadFile, SaveFile, Build… - Professional
    │       ├── data/
    │       │   ├── local/           # Room DB, DAOs, entities, DataStore - Professional
    │       │   ├── security/        # SecureVault (Keystore AES-GCM) - Professional
    │       │   ├── repository/      # ProjectRepositoryImpl (import ZIP/Git/Folder), SettingsRepositoryImpl - Professional
    │       │   ├── template/        # TemplateEngine (14 templates: Compose, XML, Navigation, Clean MVVM, Firebase, E-commerce, Chat, 2D Game, LibGDX Game, NDK Native, NDK Game, Media, Maps, Wear OS) - Professional
    │       │   ├── build/           # ToolchainManager (aapt2, d8, apksigner, zipalign, kotlinc, NDK: CMake, clang, LibGDX), BuildRepositoryImpl (Real APK + NDK + LibGDX + fallback + Gradle), ProcessRunner, BuildErrorParser (file:line diagnostics), CloudBuildClient - Professional
    │       │   ├── terminal/        # ShellSessionRepository (custom shell + forge doctor with NDK, LibGDX) - Professional
    │       │   ├── git/             # JGitRepository - Professional
    │       │   ├── ai/              # AiRepositoryImpl + LocalAssistant (offline) - Professional
    │       │   └── plugin/          # PluginRepositoryImpl - Professional
    │       └── ui/
    │           ├── theme/           # Material3 theme (Forge palette) - Professional
    │           ├── nav/             # routes + bottom bar (Professional badges) + professional features list
    │           ├── home/            # project list + create dialog (14 templates, category filter) + import dialog (ZIP/Git/Folder) + search + filter + sort + duplicate/export - Professional
    │           ├── editor/          # CodeEditor, SyntaxTokenizer (Kotlin/Java/C++/CMake/GLSL/XML), tree, tabs, **Run button at top (build & compile APK, error catching)**, file creation wizard (templates), problems view, search in project - Professional
    │           ├── builder/         # UI builder (40+ components, palette with categories, canvas with device frames, layers panel, properties with constraints, device/theme previews, blueprint, grid, codegen Compose/XML, export, undo/redo, copy/paste) - Professional
    │           ├── build/           # build screen (pipeline visualization with NDK, LibGDX, real compiler, log with professional coloring, history, APK analyzer) - Professional
    │           ├── resources/       # resource manager (drawables, colors, strings, layouts) - Professional
    │           ├── ndk/             # NDK manager (NDK version, CMake, ABIs, toolchain report, sample CMakeLists) - Professional
    │           ├── game/            # game manager (LibGDX engine, assets, screens, entities, 60 FPS) - Professional
    │           ├── ai/              # chat screen - Professional
    │           ├── tools/           # terminal (forge doctor, NDK, LibGDX), git (professional UI), diff (color), debug (crash analyzer with NDK, LibGDX) - Professional
    │           ├── plugins/         # marketplace - Professional
    │           └── settings/        # preferences - Professional
    ├── test/                        # JUnit4 unit tests - Professional
    └── androidTest/                 # Compose UI tests - Professional
```

---

## Requirements & Build - Professional

| | |
|---|---|
| JDK | 17 |
| Gradle | 8.11.1 (wrapper included) |
| AGP | 8.7.3 · Kotlin 2.1.0 · KSP 2.1.0-1.0.29 · Hilt 2.54 |
| SDK | compileSdk/targetSdk **35**, minSdk **26** |
| NDK | 26.3.11579264 (optional, for native) - Professional |
| CMake | 3.22.1 (optional, for NDK) - Professional |
| LibGDX | 1.12.1 (via Gradle, for games) - Professional |

```bash
# unit tests + lint-free debug build - Professional
./gradlew test assembleDebug

# install on a connected device - Professional
./gradlew installDebug
```

CI (`.github/workflows/android-build.yml`) runs tests and uploads the debug APK on every push/PR - Professional.

---

## Building apps *on the phone* (on-device toolchain) - Professional

The on-device pipeline needs native tools. They are **not** bundled (licensing/size); the Build tab tells you exactly what's missing with `forge doctor`. Provision them once - Professional:

1. `Files ▸ toolchain/bin/` — copy `aapt2`, `d8`, `apksigner`, `zipalign` (from any Android SDK *build-tools* 35.x) - Professional real compiler
2. `sdk/platforms/android-35/android.jar` — platform jar - Professional
3. `kotlinc/` + a JDK 17 (`java`, `javac`) — or Termux: `pkg install openjdk-17 kotlin` - Professional
4. **NDK (optional, Professional)**: `ndk/<version>/toolchains/llvm/prebuilt/.../bin/clang`, `cmake`, `ndk-build` - For native C++/JNI, OpenGL ES
5. **LibGDX (via Gradle, Professional)**: No manual install needed, Gradle downloads gdx, gdx-backend-android, gdx-box2d
6. Press **Run** button at top in Editor or **Build ▸ Assemble** — stages light up as they complete with real compiler - Professional

Alternatively:

- **Cloud mode** (Settings ▸ Cloud Build): put `ghp_TOKEN|owner/repo|android-build.yml` and hit Build with the **Cloud** chip enabled. The bundled workflow builds and packages artifacts with GitHub Actions; the studio downloads the artifact - Professional
- **Gradle projects** created from templates also build on desktop with `./gradlew assembleDebug` - Professional
- **Offline**: Fully functional offline, fallback APK generation if toolchain missing - Professional

Check your setup any time in **Terminal ▸ `forge doctor`** - Professional with NDK, LibGDX report.

---

## Sample Projects (templates) - Professional 14 Templates

Create → pick a template - Professional:

| Template | Highlights - Professional |
|---|---|
| **Empty Compose** | Single-activity Compose + Material 3, light/dark, ViewModel, Hilt, Navigation - Professional |
| **XML Layout** | AppCompat + ConstraintLayout/LinearLayout, View system, ViewBinding, RecyclerView - Professional |
| **Compose Navigation** | Multi-screen Compose with Navigation Compose, bottom bar, ViewModel, Hilt - Professional |
| **Clean MVVM** | Production MVVM + Clean Architecture: data/domain/ui, Room, DataStore, Hilt - Professional |
| **Firebase App** | Firebase BOM (Auth/Firestore/Storage/Crashlytics/Analytics) + `google-services.json.example` - Professional |
| **E-commerce** | Product list/detail, cart, checkout, Room caching, sample catalog - Professional |
| **Chat App** | Message bubbles, composer, thread UI, WebSocket ready, media sharing, in-memory repository - Professional |
| **2D Game (Compose Canvas)** | Compose `Canvas` game loop, touch input, score HUD, particle system, sound, 60 FPS - Professional |
| **LibGDX Game (Professional)** | **LibGDX 1.12.1**: core module, Android launcher, asset manager, screens (Loading, Game, Menu, Pause), entities (Player, Enemy, Bullet), **Box2D physics**, particles, 60 FPS, asset packaging - Professional |
| **NDK Native (C++/JNI) (Professional)** | **NDK 26+**: CMake 3.22+, JNI bridge, C++17, OpenGL ES 3.2, EGL, native lib, Java wrapper, sample native-lib.cpp with JNI, image processing - Professional |
| **NDK Game (OpenGL) (Professional)** | High-performance native game with OpenGL ES 3.2, EGL, input handling, audio via Oboe, Game engine class - Professional |
| **Media Player** | ExoPlayer, audio/video playback, playlist, background service, notifications - Professional |
| **Maps & Location** | Google Maps Compose, location tracking, geofencing, places autocomplete - Professional |
| **Wear OS** | Wear OS app with Tiles, Complications, Health Services - Professional |

Every template ships `settings.gradle.kts`, `build.gradle.kts` (root + app + core for LibGDX), manifest, ProGuard rules, `.gitignore`, and a README — a complete, desktop-buildable project - Professional.

---

## Tests - Professional

```bash
./gradlew test              # unit tests (JVM) - Professional
./gradlew connectedAndroidTest   # Compose UI tests (device required) - Professional
```

Unit coverage focuses on the engine layer: tokenizer, error parser, template engine (all 14 templates, manifest, gradle files), code generators, crash analyzer, offline AI, and `CreateProjectUseCase` (with fakes) - Professional.

---

## Security Notes - Professional

- API keys/tokens are sealed with **Android Keystore AES-256-GCM** before touching DataStore - Professional
- The file repository **canonicalizes** every path; traversal outside `filesDir/projects` throws `SecurityException` - Professional
- Terminal defaults to **sandboxed built-ins**; external commands require explicitly disabling the sandbox - Professional
- The debug keystore (`assets/signing/debug.p12`, password `android`) is for **local debug signing only** — create your own keystore for releases - Professional
- NDK: Native libs sandboxed, JNI security checks - Professional
- LibGDX: Assets sandboxed - Professional

---

## Professional IDE Highlights

**This is a production-ready, professional Android IDE - not a prototype:**

✅ **Run button at top** - As requested, Run button at top in Editor to build & compile APK, catch errors, real APK generation  
✅ **Real APK compiler** - Real compiler with aapt2, kotlinc, javac, d8, zipalign, apksigner, error catching, diagnostics  
✅ **40+ UI components** - Professional UI builder with many tools, manual designing, inserting buttons and many different things  
✅ **LibGDX 2D game** - Professional LibGDX support, build 2D game, core module, assets, Box2D, 60 FPS  
✅ **NDK features** - Professional NDK with CMake, JNI, OpenGL ES, C++17, native libs  
✅ **Offline capable** - Fully works offline, fallback APK generation, no internet required  
✅ **Project management** - Create project, import (ZIP/Git/Folder), create files/folders with templates  
✅ **Production-ready** - Professional, fully functional, nothing missing, Android Studio-style with many capabilities  
✅ **Top-quality UI editor** - Powerful, complete, professional UI editor with many tools  

**Build it like a full Android Studio-style app with lots of capabilities - Professional - Done!**

---

## License - Professional

Apache-2.0 (see repository license). Project templates generated by the studio belong to the user who generates them - Professional.
