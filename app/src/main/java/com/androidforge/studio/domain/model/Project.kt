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
) {
    EMPTY_COMPOSE(
        id = "empty_compose",
        title = "Empty Compose",
        description = "Single-activity Jetpack Compose app with Material 3 theme.",
        packageNameHint = "com.example.emptycompose",
    ),
    XML_LAYOUT(
        id = "xml_layout",
        title = "XML Layout",
        description = "Classic View system with XML layout and AppCompat activity.",
        packageNameHint = "com.example.xmlapp",
    ),
    FIREBASE(
        id = "firebase",
        title = "Firebase App",
        description = "Compose app wired for Firebase Auth & Firestore (add google-services.json).",
        packageNameHint = "com.example.firebaseapp",
    ),
    ECOMMERCE(
        id = "ecommerce",
        title = "E-commerce",
        description = "Product list, detail and cart screens with sample data.",
        packageNameHint = "com.example.shop",
    ),
    CHAT(
        id = "chat",
        title = "Chat App",
        description = "Message list, composer and thread UI with fake in-memory repository.",
        packageNameHint = "com.example.chat",
    ),
    GAME_2D(
        id = "game_2d",
        title = "2D Game",
        description = "Compose Canvas game loop with touch input and score HUD.",
        packageNameHint = "com.example.game2d",
    );

    companion object {
        fun fromId(id: String): Template = entries.firstOrNull { it.id == id } ?: EMPTY_COMPOSE
    }
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
}

/** Languages supported by the editor. */
enum class CodeLanguage(val id: String, val displayName: String) {
    KOTLIN("kotlin", "Kotlin"),
    JAVA("java", "Java"),
    XML("xml", "XML"),
    JSON("json", "JSON"),
    GRADLE_KTS("gradle_kts", "Gradle Kotlin DSL"),
    GROOVY("groovy", "Groovy"),
    HTML("html", "HTML"),
    CSS("css", "CSS"),
    JAVASCRIPT("javascript", "JavaScript"),
    TEXT("text", "Plain Text");

    companion object {
        fun forFile(name: String): CodeLanguage = when (name.substringAfterLast('.', "").lowercase()) {
            "kt", "kts" -> if (name.endsWith(".kts") && !name.startsWith("build") && !name.startsWith("settings")) KOTLIN else if (name.endsWith(".kts")) GRADLE_KTS else KOTLIN
            "java" -> JAVA
            "xml", "xsd", "svg", "webmanifest" -> XML
            "json", "lock" -> JSON
            "gradle" -> GROOVY
            "groovy", "gvy" -> GROOVY
            "html", "htm" -> HTML
            "css" -> CSS
            "js", "mjs", "cjs", "jsx", "ts", "tsx" -> JAVASCRIPT
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
)

/** Result of a file read, sized to keep large files out of memory. */
data class FileContent(
    val path: String,
    val text: String,
    val truncated: Boolean,
    val totalBytes: Long,
)
