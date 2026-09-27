package com.androidforge.studio.domain.model

/** A project stored in the app sandbox. [rootPath] is relative to the sandbox projects dir. */
data class Project(
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val templateId: String,
    val rootPath: String,
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Built-in project templates shipped with the studio. */
enum class Template(
    val id: String,
    val title: String,
    val description: String,
    val packageNameHint: String,
    val category: TemplateCategory,
    val supportsNdk: Boolean = false,
    val supportsLibGdx: Boolean = false,
    val minSdk: Int = 26,
) {
    EMPTY_COMPOSE(
        id = "empty_compose",
        title = "Empty Compose",
        description = "Single-activity Jetpack Compose app with Material 3 theme, navigation, ViewModel.",
        packageNameHint = "com.example.emptycompose",
        category = TemplateCategory.COMPOSE,
    ),
    XML_LAYOUT(
        id = "xml_layout",
        title = "XML Layout",
        description = "Classic View system with XML layout, AppCompat, ViewBinding, RecyclerView.",
        packageNameHint = "com.example.xmlapp",
        category = TemplateCategory.VIEW,
    ),
    COMPOSE_NAVIGATION(
        id = "compose_navigation",
        title = "Compose Navigation",
        description = "Multi-screen Compose app with Navigation Compose, bottom bar, ViewModel, Hilt.",
        packageNameHint = "com.example.navapp",
        category = TemplateCategory.COMPOSE,
    ),
    CLEAN_MVVM(
        id = "clean_mvvm",
        title = "Clean MVVM",
        description = "Production MVVM + Clean Architecture: data/domain/ui layers, Room, DataStore, Hilt.",
        packageNameHint = "com.example.cleanapp",
        category = TemplateCategory.ARCHITECTURE,
    ),
    FIREBASE(
        id = "firebase",
        title = "Firebase App",
        description = "Compose app wired for Firebase Auth & Firestore, Storage, Crashlytics, Analytics.",
        packageNameHint = "com.example.firebaseapp",
        category = TemplateCategory.BACKEND,
    ),
    ECOMMERCE(
        id = "ecommerce",
        title = "E-commerce",
        description = "Product list, detail, cart, checkout, payment UI with Room caching.",
        packageNameHint = "com.example.shop",
        category = TemplateCategory.BUSINESS,
    ),
    CHAT(
        id = "chat",
        title = "Chat App",
        description = "Message list, composer, thread UI, WebSocket ready, media sharing.",
        packageNameHint = "com.example.chat",
        category = TemplateCategory.COMMUNICATION,
    ),
    GAME_2D(
        id = "game_2d",
        title = "2D Game (Compose Canvas)",
        description = "Compose Canvas game loop with touch input, score HUD, particle system, sound.",
        packageNameHint = "com.example.game2d",
        category = TemplateCategory.GAME,
    ),
    LIBGDX_GAME(
        id = "libgdx_game",
        title = "LibGDX Game",
        description = "Professional LibGDX 2D game: core module, Android launcher, asset manager, screens, physics, Box2D, particles.",
        packageNameHint = "com.example.libgdxgame",
        category = TemplateCategory.GAME,
        supportsLibGdx = true,
    ),
    NDK_NATIVE(
        id = "ndk_native",
        title = "NDK Native (C++/JNI)",
        description = "NDK project with CMake, JNI bridge, C++17, OpenGL ES, native lib, Java wrapper.",
        packageNameHint = "com.example.ndkapp",
        category = TemplateCategory.NATIVE,
        supportsNdk = true,
    ),
    NDK_GAME(
        id = "ndk_game",
        title = "NDK Game (OpenGL)",
        description = "High-performance native game with OpenGL ES 3.2, EGL, input handling, audio via Oboe.",
        packageNameHint = "com.example.ndkgame",
        category = TemplateCategory.GAME,
        supportsNdk = true,
    ),
    MEDIA_APP(
        id = "media_app",
        title = "Media Player",
        description = "ExoPlayer, audio/video playback, playlist, background service, notifications.",
        packageNameHint = "com.example.media",
        category = TemplateCategory.MEDIA,
    ),
    MAPS_APP(
        id = "maps_app",
        title = "Maps & Location",
        description = "Google Maps, location tracking, geofencing, places autocomplete.",
        packageNameHint = "com.example.maps",
        category = TemplateCategory.LOCATION,
    ),
    WEAR_OS(
        id = "wear_os",
        title = "Wear OS",
        description = "Wear OS app with Tiles, Complications, Health Services.",
        packageNameHint = "com.example.wear",
        category = TemplateCategory.WEARABLE,
    );

    companion object {
        fun fromId(id: String): Template = entries.firstOrNull { it.id == id } ?: EMPTY_COMPOSE
    }
}

enum class TemplateCategory(val displayName: String) {
    COMPOSE("Compose"),
    VIEW("Views"),
    ARCHITECTURE("Architecture"),
    BACKEND("Backend"),
    BUSINESS("Business"),
    COMMUNICATION("Communication"),
    GAME("Games"),
    NATIVE("Native"),
    MEDIA("Media"),
    LOCATION("Location"),
    WEARABLE("Wearable"),
}

/** A node in the project file tree. */
data class FileNode(
    val name: String,
    val path: String,           // absolute path inside sandbox
    val relativePath: String,   // path relative to project root
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val children: List<FileNode> = emptyList(),
) {
    val extension: String
        get() = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    val isSourceFile: Boolean
        get() = extension in setOf("kt", "java", "cpp", "c", "h", "hpp", "xml", "gradle", "kts", "json", "cmake")

    val isImage: Boolean
        get() = extension in setOf("png", "jpg", "jpeg", "webp", "svg", "gif")

    val isGameAsset: Boolean
        get() = extension in setOf("png", "jpg", "atlas", "fnt", "tmx", "json", "ogg", "mp3", "wav")
}

/** Languages supported by the editor. */
enum class CodeLanguage(val id: String, val displayName: String, val mimeType: String) {
    KOTLIN("kotlin", "Kotlin", "text/x-kotlin"),
    JAVA("java", "Java", "text/x-java"),
    CPP("cpp", "C++", "text/x-c++src"),
    C("c", "C", "text/x-csrc"),
    CMAKE("cmake", "CMake", "text/x-cmake"),
    GLSL("glsl", "GLSL", "text/x-glsl"),
    XML("xml", "XML", "application/xml"),
    JSON("json", "JSON", "application/json"),
    GRADLE_KTS("gradle_kts", "Gradle Kotlin DSL", "text/x-kotlin"),
    GROOVY("groovy", "Groovy", "text/x-groovy"),
    HTML("html", "HTML", "text/html"),
    CSS("css", "CSS", "text/css"),
    JAVASCRIPT("javascript", "JavaScript", "application/javascript"),
    YAML("yaml", "YAML", "application/yaml"),
    PROPERTIES("properties", "Properties", "text/x-properties"),
    SHADER("shader", "Shader", "text/x-glsl"),
    TEXT("text", "Plain Text", "text/plain");

    companion object {
        fun forFile(name: String): CodeLanguage = when (name.substringAfterLast('.', "").lowercase()) {
            "kt", "kts" -> if (name.endsWith(".kts") && (name.startsWith("build") || name.startsWith("settings"))) GRADLE_KTS else KOTLIN
            "java" -> JAVA
            "cpp", "cc", "cxx", "hpp", "hxx" -> CPP
            "c", "h" -> C
            "cmake", "cmakeLists.txt" -> CMAKE
            "glsl", "vert", "frag", "geom", "comp" -> GLSL
            "xml", "xsd", "svg", "webmanifest" -> XML
            "json", "lock" -> JSON
            "gradle" -> GROOVY
            "groovy", "gvy" -> GROOVY
            "html", "htm" -> HTML
            "css" -> CSS
            "js", "mjs", "cjs", "jsx", "ts", "tsx" -> JAVASCRIPT
            "yml", "yaml" -> YAML
            "properties" -> PROPERTIES
            "glsl" -> SHADER
            else -> TEXT
        }
    }
}

/** An open editor tab. */
data class EditorTab(
    val filePath: String,
    val title: String,
    val language: CodeLanguage,
    val isDirty: Boolean = false,
    val cursorLine: Int = 0,
    val cursorColumn: Int = 0,
)

/** Result of a file read, sized to keep large files out of memory. */
data class FileContent(
    val path: String,
    val text: String,
    val truncated: Boolean,
    val totalBytes: Long,
)

/** Project import source */
enum class ImportSource(val displayName: String) {
    ZIP("ZIP Archive"),
    FOLDER("Local Folder"),
    GIT("Git Repository"),
    TEMPLATE("Template Gallery"),
}

/** File creation templates */
enum class FileTemplate(
    val id: String,
    val displayName: String,
    val extension: String,
    val category: String,
) {
    KOTLIN_CLASS("kotlin_class", "Kotlin Class", "kt", "Kotlin"),
    KOTLIN_OBJECT("kotlin_object", "Kotlin Object", "kt", "Kotlin"),
    KOTLIN_INTERFACE("kotlin_interface", "Kotlin Interface", "kt", "Kotlin"),
    KOTLIN_COMPOSABLE("kotlin_composable", "Compose Screen", "kt", "Compose"),
    KOTLIN_VIEWMODEL("kotlin_viewmodel", "ViewModel", "kt", "Architecture"),
    KOTLIN_REPOSITORY("kotlin_repository", "Repository", "kt", "Architecture"),
    JAVA_CLASS("java_class", "Java Class", "java", "Java"),
    XML_LAYOUT("xml_layout", "XML Layout", "xml", "Layout"),
    XML_DRAWABLE("xml_drawable", "Drawable", "xml", "Drawable"),
    XML_VALUES("xml_values", "Values XML", "xml", "Values"),
    CPP_FILE("cpp_file", "C++ Source", "cpp", "Native"),
    CPP_HEADER("cpp_header", "C++ Header", "h", "Native"),
    CMAKE_LISTS("cmake_lists", "CMakeLists.txt", "txt", "Native"),
    GLSL_SHADER("glsl_shader", "GLSL Shader", "glsl", "Graphics"),
    JSON_FILE("json_file", "JSON File", "json", "Data"),
    PROPERTIES_FILE("properties_file", "Properties", "properties", "Config"),
}
