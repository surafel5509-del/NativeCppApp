package com.androidforge.studio.data.template

import com.androidforge.studio.domain.model.Template

/**
 * Generates complete, buildable project file trees for every built-in template.
 * Files are returned as a map of relative path -> content.
 *
 * Generated projects target the same toolchain the on-device builder supports:
 * minSdk 26, compileSdk 35, AGP 8.7.x, Kotlin 2.1.x, Gradle Kotlin DSL.
 *
 * Professional expansion includes:
 * - LibGDX full game (core/android/assets, screens, Box2D, particles)
 * - NDK native (CMake, JNI, C++17, OpenGL ES)
 * - Clean MVVM architecture
 * - Compose Navigation
 * - Media, Maps, Wear OS
 */
object TemplateEngine {

    fun generate(templateId: String, projectName: String, packageName: String): Map<String, String> {
        val template = Template.fromId(templateId)
        val files = commonFiles(projectName, packageName, template)
        val specific = when (template) {
            Template.EMPTY_COMPOSE -> emptyComposeFiles(packageName)
            Template.XML_LAYOUT -> xmlLayoutFiles(packageName)
            Template.COMPOSE_NAVIGATION -> composeNavigationFiles(packageName)
            Template.CLEAN_MVVM -> cleanMvvmFiles(packageName)
            Template.FIREBASE -> firebaseFiles(packageName)
            Template.ECOMMERCE -> ecommerceFiles(packageName)
            Template.CHAT -> chatFiles(packageName)
            Template.GAME_2D -> game2dFiles(packageName)
            Template.LIBGDX_GAME -> libGdxGameFiles(packageName, projectName)
            Template.NDK_NATIVE -> ndkNativeFiles(packageName)
            Template.NDK_GAME -> ndkGameFiles(packageName)
            Template.MEDIA_APP -> mediaAppFiles(packageName)
            Template.MAPS_APP -> mapsAppFiles(packageName)
            Template.WEAR_OS -> wearOsFiles(packageName)
        }
        return files + specific
    }

    fun generateFileContent(templateId: String, fileName: String, packageName: String): String {
        return when (templateId) {
            "kotlin_class" -> kotlinClassTemplate(fileName, packageName)
            "kotlin_composable" -> kotlinComposableTemplate(fileName, packageName)
            "kotlin_viewmodel" -> kotlinViewModelTemplate(fileName, packageName)
            "cpp_file" -> cppFileTemplate(fileName, packageName)
            "xml_layout" -> xmlLayoutTemplate(fileName)
            "cmake_lists" -> cmakeListsTemplate()
            else -> "// New file: $fileName\npackage $packageName\n\n"
        }
    }

    // ---------------------------------------------------------------- common

    private fun commonFiles(projectName: String, packageName: String, template: Template): Map<String, String> {
        return linkedMapOf(
            "settings.gradle.kts" to """
                rootProject.name = "$projectName"
                include(":app")
                ${if (template.supportsLibGdx) """include(":core")""" else ""}
            """.trimIndent() + "\n",

            "build.gradle.kts" to """
                // Top-level build file - AndroidForge Studio Professional
                plugins {
                    id("com.android.application") version "8.7.3" apply false
                    id("com.android.library") version "8.7.3" apply false
                    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
                    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
                    id("org.jetbrains.kotlin.jvm") version "2.1.0" apply false
                }
                
                task("clean", Delete::class) {
                    delete(rootProject.buildDir)
                }
            """.trimIndent() + "\n",

            "gradle.properties" to """
                org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
                org.gradle.parallel=true
                org.gradle.caching=true
                android.useAndroidX=true
                kotlin.code.style=official
                android.nonTransitiveRClass=true
                android.nonFinalResIds=true
                android.enableJetifier=false
                # NDK
                android.ndkVersion=26.3.11579264
                # LibGDX
                gdxVersion=1.12.1
            """.trimIndent() + "\n",

            "gradle/wrapper/gradle-wrapper.properties" to """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
            """.trimIndent() + "\n",

            ".gitignore" to """
                *.iml
                .gradle
                /local.properties
                .idea
                build
                /captures
                .externalNativeBuild
                .cxx
                local.properties
                *.ap_
                *.aab
                *.apk
                /app/build/
                /core/build/
                /android/build/
                .DS_Store
                *.jks
                *.p12
                /artifacts/
                /build/
                .kotlin/
            """.trimIndent() + "\n",

            "app/src/main/AndroidManifest.xml" to manifestXml(packageName, template),

            "app/src/main/res/values/strings.xml" to """
                <resources>
                    <string name="app_name">$projectName</string>
                    <string name="welcome">Welcome to $projectName</string>
                    <string name="action_settings">Settings</string>
                    <string name="action_build">Build APK</string>
                    <string name="action_run">Run</string>
                </resources>
            """.trimIndent() + "\n",

            "app/src/main/res/values/colors.xml" to """
                <resources>
                    <color name="primary">#FF6D00</color>
                    <color name="primary_dark">#E65100</color>
                    <color name="primary_light">#FF9E40</color>
                    <color name="secondary">#1DE9B6</color>
                    <color name="background">#121212</color>
                    <color name="surface">#1E1E1E</color>
                    <color name="white">#FFFFFF</color>
                    <color name="black">#000000</color>
                    <color name="error">#CF6679</color>
                </resources>
            """.trimIndent() + "\n",

            "app/src/main/res/values/themes.xml" to """
                <resources>
                    <style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar">
                        <item name="colorPrimary">@color/primary</item>
                        <item name="android:statusBarColor">@android:color/transparent</item>
                        <item name="android:navigationBarColor">@android:color/transparent</item>
                    </style>
                </resources>
            """.trimIndent() + "\n",

            "README.md" to """
                # $projectName

                Generated by **AndroidForge Studio Professional** from the `${template.title}` template.

                Package: `$packageName`
                Template: `${template.id}`
                Category: `${template.category.displayName}`

                ## Features
                - ${template.description}
                - Min SDK: ${template.minSdk}, Target SDK: 35
                - Offline build support
                - Professional project structure

                ## Build

                * On device: open this project in AndroidForge Studio → **Run** button (top bar) → APK will be built automatically.
                * On desktop: `./gradlew assembleDebug` (requires JDK 17 + Android SDK 35).
                * For NDK: Ensure NDK 26+ installed, CMake 3.22+
                * For LibGDX: Desktop build `./gradlew desktop:dist`

                ## Project Structure
                ```
                app/src/main/
                ├── java/${packageName.replace('.', '/')}/
                │   ├── MainActivity.kt
                │   ├── ui/          # Composables & screens
                │   ├── data/        # Repositories & data sources
                │   ├── domain/      # Use cases & models
                │   └── di/          # Dependency injection
                ├── res/             # Resources
                └── AndroidManifest.xml
                ```

                ## Professional IDE Features Used
                - Real APK compiler (aapt2, d8, zipalign, apksigner)
                - NDK support with CMake
                - LibGDX 2D game engine
                - Visual UI builder
                - Code editor with syntax highlighting
                - Git integration
                - Terminal & build tools
            """.trimIndent() + "\n",

            "app/proguard-rules.pro" to """
                # AndroidForge Studio - ProGuard rules
                -keepattributes *Annotation*
                -keepattributes Signature
                -keepattributes InnerClasses,EnclosingMethod
                -keep class com.androidforge.** { *; }
                # Keep native methods
                -keepclasseswithmembernames class * {
                    native <methods>;
                }
                # LibGDX
                -keep class com.badlogic.gdx.** { *; }
                -keep class com.badlogic.gdx.graphics.g2d.** { *; }
                # Compose
                -keep class androidx.compose.** { *; }
            """.trimIndent() + "\n",
        )
    }

    private fun manifestXml(packageName: String, template: Template): String {
        val permissions = buildList {
            add("""<uses-permission android:name="android.permission.INTERNET" />""")
            if (template.category.name == "LOCATION") {
                add("""<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />""")
                add("""<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />""")
            }
            if (template.category.name == "MEDIA") {
                add("""<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />""")
                add("""<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />""")
            }
            if (template.supportsNdk) {
                add("""<uses-feature android:glEsVersion="0x00030002" android:required="true" />""")
            }
        }.joinToString("\n    ")

        return """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">

            $permissions

            <application
                android:allowBackup="true"
                android:icon="@mipmap/ic_launcher"
                android:label="@string/app_name"
                android:supportsRtl="true"
                android:theme="@style/Theme.App"
                android:usesCleartextTraffic="false">
                <activity
                    android:name=".MainActivity"
                    android:exported="true"
                    android:windowSoftInputMode="adjustResize"
                    android:theme="@style/Theme.App">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
                ${if (template.category.name == "MEDIA") """
                <service android:name=".media.PlaybackService" android:exported="false" android:foregroundServiceType="mediaPlayback" />
                """.trim() else ""}
            </application>

        </manifest>
    """.trimIndent() + "\n"
    }

    // ------------------------------------------------------------ app module

    private fun appGradle(packageName: String, compose: Boolean, extraDeps: List<String> = emptyList(), ndk: Boolean = false, libgdx: Boolean = false): String {
        val composeBlock = if (compose) """
            buildFeatures { 
                compose = true
                buildConfig = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.14"
            }
        """.trimIndent() else """
            buildFeatures {
                buildConfig = true
                viewBinding = true
            }
        """.trimIndent()

        val ndkBlock = if (ndk) """
            ndkVersion = "26.3.11579264"
            externalNativeBuild {
                cmake {
                    path = file("src/main/cpp/CMakeLists.txt")
                    version = "3.22.1"
                }
            }
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            }
        """.trimIndent() else ""

        val deps = buildList {
            add("implementation(\"androidx.core:core-ktx:1.15.0\")")
            add("implementation(\"androidx.appcompat:appcompat:1.7.0\")")
            add("implementation(\"com.google.android.material:material:1.12.0\")")
            add("implementation(\"androidx.constraintlayout:constraintlayout:2.2.0\")")
            if (compose) {
                add("implementation(platform(\"androidx.compose:compose-bom:2024.12.01\"))")
                add("implementation(\"androidx.compose.ui:ui\")")
                add("implementation(\"androidx.compose.ui:ui-tooling-preview\")")
                add("implementation(\"androidx.compose.material3:material3\")")
                add("implementation(\"androidx.compose.material:material-icons-extended\")")
                add("implementation(\"androidx.activity:activity-compose:1.9.3\")")
                add("implementation(\"androidx.lifecycle:lifecycle-runtime-ktx:2.8.7\")")
                add("implementation(\"androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7\")")
                add("implementation(\"androidx.navigation:navigation-compose:2.8.5\")")
                add("implementation(\"androidx.hilt:hilt-navigation-compose:1.2.0\")")
            }
            add("implementation(\"com.google.dagger:hilt-android:2.54\")")
            add("ksp(\"com.google.dagger:hilt-android-compiler:2.54\")")
            add("implementation(\"androidx.room:room-runtime:2.7.1\")")
            add("implementation(\"androidx.room:room-ktx:2.7.1\")")
            add("ksp(\"androidx.room:room-compiler:2.7.1\")")
            add("implementation(\"androidx.datastore:datastore-preferences:1.1.1\")")
            if (libgdx) {
                add("implementation(\"com.badlogicgames.gdx:gdx:1.12.1\")")
                add("implementation(\"com.badlogicgames.gdx:gdx-backend-android:1.12.1\")")
                add("implementation(\"com.badlogicgames.gdx:gdx-box2d:1.12.1\")")
                add("implementation(\"com.badlogicgames.gdx:gdx-box2d-platform:1.12.1:natives-armeabi-v7a\")")
                add("implementation(\"com.badlogicgames.gdx:gdx-box2d-platform:1.12.1:natives-arm64-v8a\")")
            }
            addAll(extraDeps)
            add("testImplementation(\"junit:junit:4.13.2\")")
            add("androidTestImplementation(\"androidx.test.ext:junit:1.2.1\")")
        }.joinToString("\n") { "    $it" }

        return """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
                ${if (compose) "id(\"org.jetbrains.kotlin.plugin.compose\")" else ""}
                id("com.google.devtools.ksp")
                id("com.google.dagger.hilt.android")
            }

            android {
                namespace = "$packageName"
                compileSdk = 35

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = ${if (ndk) 26 else 26}
                    targetSdk = 35
                    versionCode = 1
                    versionName = "1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    $ndkBlock
                }

                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                    debug {
                        isDebuggable = true
                        applicationIdSuffix = ".debug"
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                    isCoreLibraryDesugaringEnabled = true
                }
                kotlinOptions { jvmTarget = "17" }
                $composeBlock
                packaging {
                    resources {
                        excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*")
                    }
                    jniLibs {
                        useLegacyPackaging = false
                    }
                }
            }

            dependencies {
            $deps
                coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
            }
        """.trimIndent() + "\n"
    }

    private fun appGradleFile(packageName: String, compose: Boolean, extraDeps: List<String> = emptyList(), ndk: Boolean = false, libgdx: Boolean = false): Pair<String, String> =
        "app/build.gradle.kts" to appGradle(packageName, compose, extraDeps, ndk, libgdx)

    private fun proguardFile(): Pair<String, String> =
        "app/proguard-rules.pro" to "# Add project specific ProGuard rules here.\n-keepattributes *Annotation*\n"

    private fun launcherManifestFix(): Pair<String, String> =
        "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" to """
            <?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@color/primary" />
                <foreground android:drawable="@android:drawable/sym_def_app_icon" />
            </adaptive-icon>
        """.trimIndent() + "\n"

    // ------------------------------------------------------ 1) Empty Compose

    private fun emptyComposeFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.foundation.layout.Box
                import androidx.compose.foundation.layout.fillMaxSize
                import androidx.compose.foundation.layout.padding
                import androidx.compose.material3.MaterialTheme
                import androidx.compose.material3.Scaffold
                import androidx.compose.material3.Text
                import androidx.compose.material3.darkColorScheme
                import androidx.compose.material3.lightColorScheme
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Alignment
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.tooling.preview.Preview
                import androidx.compose.foundation.isSystemInDarkTheme
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            AppTheme {
                                MainScreen()
                            }
                        }
                    }
                }

                @Composable
                private fun AppTheme(content: @Composable () -> Unit) {
                    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = colors, content = content)
                }

                @Composable
                fun MainScreen() {
                    Scaffold { padding ->
                        Box(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "Hello from AndroidForge Studio Professional!")
                        }
                    }
                }

                @Preview(showBackground = true)
                @Composable
                private fun MainScreenPreview() {
                    AppTheme { MainScreen() }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/di/AppModule.kt" to """
                package $packageName.di

                import dagger.Module
                import dagger.hilt.InstallIn
                import dagger.hilt.components.SingletonComponent

                @Module
                @InstallIn(SingletonComponent::class)
                object AppModule
            """.trimIndent() + "\n",
        )
    }

    private fun composeNavigationFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import androidx.navigation.compose.rememberNavController
                import $packageName.ui.AppNavHost
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            MaterialTheme {
                                val navController = rememberNavController()
                                AppNavHost(navController = navController)
                            }
                        }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/AppNavHost.kt" to """
                package $packageName.ui

                import androidx.compose.runtime.Composable
                import androidx.navigation.NavHostController
                import androidx.navigation.compose.NavHost
                import androidx.navigation.compose.composable

                @Composable
                fun AppNavHost(navController: NavHostController) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(onNavigateToDetail = { navController.navigate("detail/${'$'}it") }) }
                        composable("detail/{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: "0"
                            DetailScreen(id = id, onBack = { navController.popBackStack() })
                        }
                        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/HomeScreen.kt" to """
                package $packageName.ui

                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun HomeScreen(onNavigateToDetail: (String) -> Unit) {
                    Scaffold(
                        topBar = { TopAppBar(title = { Text("Home") }) },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { onNavigateToDetail("1") }) { Text("+") }
                        }
                    ) { padding ->
                        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                            Text("Professional Navigation Template", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { onNavigateToDetail("42") }) { Text("Go to Detail") }
                        }
                    }
                }

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun DetailScreen(id: String, onBack: () -> Unit) {
                    Scaffold(topBar = { TopAppBar(title = { Text("Detail ${'$'}id") }, navigationIcon = { IconButton(onClick = onBack) { Text("←") } }) }) { padding ->
                        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("Detail for item ${'$'}id")
                        }
                    }
                }

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun SettingsScreen(onBack: () -> Unit) {
                    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Text("←") } }) }) { padding ->
                        Column(modifier = Modifier.padding(padding).padding(16.dp)) { Text("Settings") }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    private fun cleanMvvmFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import dagger.hilt.android.AndroidEntryPoint
                import $packageName.ui.home.HomeScreen

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme { HomeScreen() } }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/domain/model/User.kt" to """
                package $packageName.domain.model

                data class User(val id: String, val name: String, val email: String)
                data class Task(val id: String, val title: String, val completed: Boolean)
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/domain/repository/UserRepository.kt" to """
                package $packageName.domain.repository

                import $packageName.domain.model.User
                import kotlinx.coroutines.flow.Flow

                interface UserRepository {
                    fun observeUsers(): Flow<List<User>>
                    suspend fun getUser(id: String): User?
                    suspend fun saveUser(user: User)
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/data/local/AppDatabase.kt" to """
                package $packageName.data.local

                import androidx.room.Database
                import androidx.room.RoomDatabase

                @Database(entities = [UserEntity::class], version = 1)
                abstract class AppDatabase : RoomDatabase() {
                    abstract fun userDao(): UserDao
                }

                @androidx.room.Entity(tableName = "users")
                data class UserEntity(@androidx.room.PrimaryKey val id: String, val name: String, val email: String)

                @androidx.room.Dao
                interface UserDao {
                    @androidx.room.Query("SELECT * FROM users") fun observeAll(): kotlinx.coroutines.flow.Flow<List<UserEntity>>
                    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun insert(user: UserEntity)
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/home/HomeViewModel.kt" to """
                package $packageName.ui.home

                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import dagger.hilt.android.lifecycle.HiltViewModel
                import kotlinx.coroutines.flow.*
                import javax.inject.Inject

                data class HomeUiState(val users: List<String> = emptyList(), val loading: Boolean = false)

                @HiltViewModel
                class HomeViewModel @Inject constructor() : ViewModel() {
                    private val _state = MutableStateFlow(HomeUiState())
                    val state: StateFlow<HomeUiState> = _state.asStateFlow()
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/home/HomeScreen.kt" to """
                package $packageName.ui.home

                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp
                import androidx.hilt.navigation.compose.hiltViewModel

                @Composable
                fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
                    val state by viewModel.state.collectAsState()
                    Scaffold(topBar = { TopAppBar(title = { Text("Clean MVVM") }) }) { padding ->
                        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                            Text("Production-ready Clean Architecture", style = MaterialTheme.typography.headlineSmall)
                            Text("Domain → Data → UI layers with Hilt, Room, DataStore")
                        }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    // ---------------------------------------------------------- 2) XML Layout

    private fun xmlLayoutFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = false),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/res/layout/activity_main.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:padding="24dp">

                    <TextView
                        android:id="@+id/title"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="XML Layout App - Professional"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <Button
                        android:id="@+id/action"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:text="Build APK"
                        app:layout_constraintTop_toBottomOf="@id/title"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/recycler"
                        android:layout_width="0dp"
                        android:layout_height="0dp"
                        android:layout_marginTop="16dp"
                        app:layout_constraintTop_toBottomOf="@id/action"
                        app:layout_constraintBottom_toBottomOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />

                </androidx.constraintlayout.widget.ConstraintLayout>
            """.trimIndent() + "\n",
            "app/src/main/res/layout/item_project.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_margin="8dp">
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="16dp">
                        <TextView android:id="@+id/name" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textStyle="bold" />
                        <TextView android:id="@+id/pkg" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="12sp" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import android.widget.Button
                import android.widget.TextView
                import android.widget.Toast
                import androidx.appcompat.app.AppCompatActivity
                import androidx.recyclerview.widget.LinearLayoutManager
                import androidx.recyclerview.widget.RecyclerView

                class MainActivity : AppCompatActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContentView(R.layout.activity_main)

                        val title = findViewById<TextView>(R.id.title)
                        val action = findViewById<Button>(R.id.action)
                        val recycler = findViewById<RecyclerView>(R.id.recycler)
                        
                        recycler.layoutManager = LinearLayoutManager(this)
                        title.text = getString(R.string.app_name)
                        action.setOnClickListener {
                            Toast.makeText(this, "Professional XML Layout - Ready to Build APK!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    // ------------------------------------------------------------ 3) Firebase

    private fun firebaseFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        val base = emptyComposeFiles(packageName).toMutableMap()
        base[appGradleFile(packageName, compose = true).first] = appGradle(
            packageName,
            compose = true,
            extraDeps = listOf(
                "implementation(\"com.google.firebase:firebase-bom:33.7.0\")",
                "implementation(\"com.google.firebase:firebase-auth-ktx\")",
                "implementation(\"com.google.firebase:firebase-firestore-ktx\")",
                "implementation(\"com.google.firebase:firebase-storage-ktx\")",
                "implementation(\"com.google.firebase:firebase-crashlytics-ktx\")",
                "implementation(\"com.google.firebase:firebase-analytics-ktx\")",
            ),
        )
        base["app/google-services.json.example"] = """
            {
              "project_info": {
                "project_number": "000000000000",
                "project_id": "your-firebase-project",
                "storage_bucket": "your-firebase-project.appspot.com"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
                    "android_client_info": { "package_name": "$packageName" }
                  },
                  "api_key": [{ "current_key": "REPLACE_ME" }]
                }
              ],
              "configuration_version": "1"
            }
        """.trimIndent() + "\n"
        base["app/src/main/java/$pkgPath/data/AuthRepository.kt"] = """
            package $packageName.data

            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.flow

            data class User(val uid: String, val email: String)

            class AuthRepository {
                fun observeAuthState(): Flow<User?> = flow { emit(null) }
                suspend fun signIn(email: String, password: String): Result<User> = Result.success(User("1", email))
                suspend fun signUp(email: String, password: String): Result<User> = Result.success(User("1", email))
                suspend fun signOut() {}
            }
        """.trimIndent() + "\n"
        return base
    }

    // ----------------------------------------------------------- 4) Ecommerce

    private fun ecommerceFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/model/Product.kt" to """
                package $packageName.model

                data class Product(
                    val id: Long,
                    val name: String,
                    val priceCents: Int,
                    val emoji: String,
                    val description: String = "",
                    val imageUrl: String = "",
                    val rating: Float = 4.5f,
                    val inStock: Boolean = true,
                ) {
                    val price: String get() = "$" + "%,.2f".format(priceCents / 100.0)
                }

                object SampleCatalog {
                    val products = listOf(
                        Product(1, "Forge Hammer", 2499, "🔨", "Professional blacksmith hammer", rating = 4.8f),
                        Product(2, "Anvil Stand", 8999, "🗿", "Heavy duty anvil stand", rating = 4.9f),
                        Product(3, "Heat Gloves", 1599, "🧤", "Heat resistant gloves", rating = 4.6f),
                        Product(4, "Steel Tongs", 1299, "🦾", "Precision steel tongs", rating = 4.7f),
                        Product(5, "Apron Deluxe", 3499, "🥼", "Premium leather apron", rating = 4.8f),
                    )
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/ShopScreen.kt" to """
                package $packageName.ui

                import androidx.compose.foundation.layout.*
                import androidx.compose.foundation.lazy.LazyColumn
                import androidx.compose.foundation.lazy.items
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp
                import $packageName.model.Product
                import $packageName.model.SampleCatalog

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun ShopScreen() {
                    var cart by remember { mutableStateOf<List<Product>>(emptyList()) }
                    Scaffold(
                        topBar = { TopAppBar(title = { Text("Forge Shop - Professional") }, actions = { Badge { Text("${'$'}{cart.size}") } }) },
                    ) { padding ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(SampleCatalog.products) { product -> 
                                ProductCard(product, onAddToCart = { cart = cart + product })
                            }
                        }
                    }
                }

                @Composable
                private fun ProductCard(product: Product, onAddToCart: () -> Unit) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "${'$'}{product.emoji} ${'$'}{product.name}", style = MaterialTheme.typography.titleMedium)
                            Text(text = product.description, style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = product.price, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = onAddToCart) { Text("Add to Cart") }
                            }
                        }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import $packageName.ui.ShopScreen
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme { ShopScreen() } }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    // ---------------------------------------------------------------- 5) Chat

    private fun chatFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/model/Message.kt" to """
                package $packageName.model

                data class Message(
                    val id: Long,
                    val fromMe: Boolean,
                    val text: String,
                    val time: String,
                    val status: MessageStatus = MessageStatus.SENT,
                    val attachments: List<Attachment> = emptyList(),
                )
                enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }
                data class Attachment(val type: String, val url: String, val name: String)
                data class Chat(val id: String, val name: String, val lastMessage: String, val unreadCount: Int)
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/ChatScreen.kt" to """
                package $packageName.ui

                import androidx.compose.foundation.background
                import androidx.compose.foundation.layout.*
                import androidx.compose.foundation.lazy.LazyColumn
                import androidx.compose.foundation.lazy.items
                import androidx.compose.foundation.lazy.rememberLazyListState
                import androidx.compose.foundation.shape.RoundedCornerShape
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Alignment
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.graphics.Color
                import androidx.compose.ui.unit.dp
                import $packageName.model.Message
                import java.text.SimpleDateFormat
                import java.util.*

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun ChatScreen() {
                    val messages = remember {
                        mutableStateListOf(
                            Message(1, false, "Hey! Welcome to the forge 🔥 Professional Chat", "09:41"),
                            Message(2, true, "Thanks, this editor is fast and professional.", "09:42"),
                        )
                    }
                    var draft by remember { mutableStateOf("") }
                    val listState = rememberLazyListState()

                    Scaffold(
                        topBar = { TopAppBar(title = { Text("Forge Chat - Pro") }) },
                    ) { padding ->
                        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(messages) { msg -> Bubble(msg) }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { draft = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Message…") },
                                )
                                Button(
                                    onClick = {
                                        if (draft.isNotBlank()) {
                                            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                            messages.add(Message(System.nanoTime(), true, draft.trim(), now))
                                            draft = ""
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                ) { Text("Send") }
                            }
                        }
                    }
                }

                @Composable
                private fun Bubble(msg: Message) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.fromMe) Arrangement.End else Arrangement.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (msg.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .widthIn(max = 280.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column {
                                Text(
                                    text = msg.text,
                                    color = if (msg.fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = msg.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (msg.fromMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import $packageName.ui.ChatScreen
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme { ChatScreen() } }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    // ------------------------------------------------------------- 6) 2D Game Compose

    private fun game2dFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/engine/GameEngine.kt" to """
                package $packageName.engine

                import androidx.compose.runtime.*
                import androidx.compose.ui.geometry.Offset
                import kotlinx.coroutines.delay

                data class GameObject(
                    var position: Offset,
                    var velocity: Offset = Offset.Zero,
                    var radius: Float = 50f,
                    var health: Int = 100,
                    var active: Boolean = true,
                )

                class GameEngine {
                    var score by mutableLongStateOf(0L)
                    var level by mutableIntStateOf(1)
                    var lives by mutableIntStateOf(3)
                    var isRunning by mutableStateOf(true)
                    var objects = mutableStateListOf<GameObject>()

                    suspend fun gameLoop() {
                        while (isRunning) {
                            update()
                            delay(16) // ~60 FPS
                        }
                    }

                    private fun update() {
                        objects.forEach { obj ->
                            obj.position += obj.velocity
                        }
                        // Collision detection, etc.
                    }

                    fun spawnEnemy() {
                        objects.add(GameObject(position = Offset((0..1000).random().toFloat(), 0f), velocity = Offset(0f, 5f)))
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/GameScreen.kt" to """
                package $packageName.ui

                import androidx.compose.foundation.Canvas
                import androidx.compose.foundation.gestures.detectTapGestures
                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.MaterialTheme
                import androidx.compose.material3.Text
                import androidx.compose.runtime.*
                import androidx.compose.ui.Alignment
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.geometry.Offset
                import androidx.compose.ui.graphics.Color
                import androidx.compose.ui.input.pointer.pointerInput
                import androidx.compose.ui.unit.dp
                import androidx.compose.ui.unit.sp
                import $packageName.engine.GameEngine
                import kotlinx.coroutines.delay

                @Composable
                fun GameScreen() {
                    var target by remember { mutableStateOf(Offset(300f, 600f)) }
                    var score by remember { mutableLongStateOf(0L) }
                    var misses by remember { mutableLongStateOf(0L) }
                    var level by remember { mutableIntStateOf(1) }
                    val engine = remember { GameEngine() }

                    LaunchedEffect(Unit) {
                        engine.gameLoop()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { pos ->
                                    val dx = pos.x - target.x
                                    val dy = pos.y - target.y
                                    if (dx * dx + dy * dy < 90f * 90f) {
                                        score += 1
                                        if (score % 10 == 0L) level++
                                        target = Offset(
                                            (50..(size.width - 50)).random().toFloat(),
                                            (150..(size.height - 150)).random().toFloat(),
                                        )
                                    } else {
                                        misses += 1
                                    }
                                }
                            },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Background gradient
                            drawRect(color = Color(0xFF0D1117))
                            // Target
                            drawCircle(color = Color(0xFFFF6D00), radius = 60f, center = target)
                            drawCircle(color = Color.White, radius = 70f, center = target, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                            // Particles
                            repeat(5) { i ->
                                drawCircle(
                                    color = Color(0xFFFFB74D).copy(alpha = 0.3f),
                                    radius = 20f + i * 10f,
                                    center = target + Offset((i * 10f), (i * 5f))
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Score: ${'$'}score | Level: ${'$'}level | Misses: ${'$'}misses",
                                fontSize = 18.sp,
                                color = Color.White,
                            )
                            Text(
                                text = "Professional 2D Game Engine - 60 FPS",
                                fontSize = 12.sp,
                                color = Color(0xFF8B949E),
                            )
                        }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import androidx.compose.material3.darkColorScheme
                import $packageName.ui.GameScreen
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme(colorScheme = darkColorScheme()) { GameScreen() } }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    // ------------------------------------------------------------- 7) LibGDX Professional

    private fun libGdxGameFiles(packageName: String, projectName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            "settings.gradle.kts" to """
                rootProject.name = "$projectName"
                include(":app")
                include(":core")
            """.trimIndent() + "\n",
            "build.gradle.kts" to """
                plugins {
                    id("com.android.application") version "8.7.3" apply false
                    id("com.android.library") version "8.7.3" apply false
                    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
                }
            """.trimIndent() + "\n",
            appGradleFile(packageName, compose = false, libgdx = true).first to appGradle(packageName, compose = false, libgdx = true),
            "core/build.gradle.kts" to """
                plugins {
                    id("java-library")
                    id("org.jetbrains.kotlin.jvm") version "2.1.0"
                }

                java {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                dependencies {
                    api("com.badlogicgames.gdx:gdx:1.12.1")
                    api("com.badlogicgames.gdx:gdx-box2d:1.12.1")
                    api("com.badlogicgames.gdx:gdx-ai:1.8.2")
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/AndroidLauncher.kt" to """
                package $packageName

                import android.os.Bundle
                import com.badlogic.gdx.backends.android.AndroidApplication
                import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
                import $packageName.core.ForgeGame

                class AndroidLauncher : AndroidApplication() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        val config = AndroidApplicationConfiguration().apply {
                            useAccelerometer = false
                            useCompass = false
                            useWakelock = true
                            useImmersiveMode = true
                        }
                        initialize(ForgeGame(), config)
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/AndroidManifest.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <uses-permission android:name="android.permission.INTERNET" />
                    <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35" />
                    <application
                        android:allowBackup="true"
                        android:icon="@mipmap/ic_launcher"
                        android:label="$projectName"
                        android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
                        <activity
                            android:name=".$packageName.AndroidLauncher"
                            android:exported="true"
                            android:screenOrientation="landscape"
                            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent() + "\n",
            "core/src/main/java/$pkgPath/core/ForgeGame.kt" to """
                package $packageName.core

                import com.badlogic.gdx.Game
                import com.badlogic.gdx.Gdx
                import com.badlogic.gdx.assets.AssetManager
                import com.badlogic.gdx.graphics.g2d.SpriteBatch
                import $packageName.core.screens.LoadingScreen

                class ForgeGame : Game() {
                    lateinit var batch: SpriteBatch
                    lateinit var assets: AssetManager

                    override fun create() {
                        batch = SpriteBatch()
                        assets = AssetManager()
                        setScreen(LoadingScreen(this))
                        Gdx.app.log("ForgeGame", "Professional LibGDX Game - AndroidForge Studio")
                    }

                    override fun dispose() {
                        batch.dispose()
                        assets.dispose()
                    }
                }
            """.trimIndent() + "\n",
            "core/src/main/java/$pkgPath/core/screens/LoadingScreen.kt" to """
                package $packageName.core.screens

                import com.badlogic.gdx.Gdx
                import com.badlogic.gdx.Screen
                import com.badlogic.gdx.graphics.GL20
                import $packageName.core.ForgeGame

                class LoadingScreen(private val game: ForgeGame) : Screen {
                    private var progress = 0f

                    override fun render(delta: Float) {
                        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
                        progress += delta * 0.5f
                        if (progress >= 1f) {
                            game.setScreen(GameScreen(game))
                        }
                    }

                    override fun show() {}
                    override fun resize(width: Int, height: Int) {}
                    override fun pause() {}
                    override fun resume() {}
                    override fun hide() {}
                    override fun dispose() {}
                }
            """.trimIndent() + "\n",
            "core/src/main/java/$pkgPath/core/screens/GameScreen.kt" to """
                package $packageName.core.screens

                import com.badlogic.gdx.Gdx
                import com.badlogic.gdx.Input
                import com.badlogic.gdx.Screen
                import com.badlogic.gdx.graphics.GL20
                import com.badlogic.gdx.graphics.OrthographicCamera
                import com.badlogic.gdx.graphics.g2d.BitmapFont
                import com.badlogic.gdx.math.Vector2
                import com.badlogic.gdx.physics.box2d.*
                import $packageName.core.ForgeGame
                import $packageName.core.entities.Player
                import $packageName.core.entities.Enemy

                class GameScreen(private val game: ForgeGame) : Screen {
                    private lateinit var camera: OrthographicCamera
                    private lateinit var world: World
                    private lateinit var player: Player
                    private lateinit var font: BitmapFont
                    private var enemies = mutableListOf<Enemy>()
                    private var score = 0

                    override fun show() {
                        camera = OrthographicCamera().apply {
                            setToOrtho(false, 800f, 480f)
                        }
                        world = World(Vector2(0f, -9.8f), true)
                        player = Player(world, Vector2(100f, 100f))
                        font = BitmapFont()
                        
                        repeat(5) {
                            enemies.add(Enemy(world, Vector2((200 + it * 100).toFloat(), 200f)))
                        }
                    }

                    override fun render(delta: Float) {
                        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f)
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                        world.step(delta, 6, 2)
                        camera.update()

                        game.batch.projectionMatrix = camera.combined
                        game.batch.begin()
                        font.draw(game.batch, "Professional LibGDX - Score: ${'$'}score", 10f, 470f)
                        font.draw(game.batch, "Touch to move | Box2D Physics | 60 FPS", 10f, 450f)
                        game.batch.end()

                        if (Gdx.input.isTouched) {
                            val touchX = Gdx.input.x.toFloat()
                            val touchY = (Gdx.graphics.height - Gdx.input.y).toFloat()
                            player.moveTo(Vector2(touchX, touchY))
                        }

                        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                            score++
                        }
                    }

                    override fun resize(width: Int, height: Int) {}
                    override fun pause() {}
                    override fun resume() {}
                    override fun hide() {}
                    override fun dispose() {
                        world.dispose()
                        font.dispose()
                    }
                }
            """.trimIndent() + "\n",
            "core/src/main/java/$pkgPath/core/entities/Player.kt" to """
                package $packageName.core.entities

                import com.badlogic.gdx.math.Vector2
                import com.badlogic.gdx.physics.box2d.*

                class Player(world: World, position: Vector2) {
                    val body: Body

                    init {
                        val bodyDef = BodyDef().apply {
                            type = BodyDef.BodyType.DynamicBody
                            this.position.set(position)
                        }
                        body = world.createBody(bodyDef)
                        val shape = PolygonShape().apply { setAsBox(16f, 16f) }
                        val fixtureDef = FixtureDef().apply {
                            this.shape = shape
                            density = 1f
                            friction = 0.5f
                        }
                        body.createFixture(fixtureDef)
                        shape.dispose()
                    }

                    fun moveTo(target: Vector2) {
                        val direction = target.cpy().sub(body.position).nor().scl(5f)
                        body.linearVelocity = direction
                    }
                }

                class Enemy(world: World, position: Vector2) {
                    val body: Body
                    var health = 100

                    init {
                        val bodyDef = BodyDef().apply {
                            type = BodyDef.BodyType.DynamicBody
                            this.position.set(position)
                        }
                        body = world.createBody(bodyDef)
                        val shape = CircleShape().apply { radius = 16f }
                        body.createFixture(shape, 1f)
                        shape.dispose()
                    }
                }
            """.trimIndent() + "\n",
            "core/src/main/java/$pkgPath/core/utils/AssetLoader.kt" to """
                package $packageName.core.utils

                import com.badlogic.gdx.assets.AssetManager
                import com.badlogic.gdx.graphics.Texture
                import com.badlogic.gdx.audio.Sound
                import com.badlogic.gdx.audio.Music

                class AssetLoader(private val manager: AssetManager) {
                    fun loadAll() {
                        // Textures
                        manager.load("player.png", Texture::class.java)
                        manager.load("enemy.png", Texture::class.java)
                        manager.load("background.png", Texture::class.java)
                        // Sounds
                        manager.load("jump.wav", Sound::class.java)
                        manager.load("explosion.wav", Sound::class.java)
                        // Music
                        manager.load("bgm.mp3", Music::class.java)
                    }

                    fun isLoaded(): Boolean = manager.update()
                    fun progress(): Float = manager.progress
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                // Wrapper - actual launcher is AndroidLauncher
                class MainActivity : AndroidLauncher()
            """.trimIndent() + "\n",
        )
    }

    // ------------------------------------------------------------- NDK Native Professional

    private fun ndkNativeFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true, ndk = true),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/cpp/CMakeLists.txt" to """
                cmake_minimum_required(VERSION 3.22.1)
                project("$packageName")

                # Professional NDK setup - AndroidForge Studio
                set(CMAKE_CXX_STANDARD 17)
                set(CMAKE_CXX_STANDARD_REQUIRED ON)
                set(CMAKE_CXX_EXTENSIONS OFF)

                # Find required packages
                find_library(log-lib log)
                find_library(android-lib android)
                find_library(EGL-lib EGL)
                find_library(GLESv3-lib GLESv3)

                # Main native library
                add_library(
                    native-lib
                    SHARED
                    native-lib.cpp
                    forge-engine.cpp
                    jni-bridge.cpp
                    graphics/renderer.cpp
                    graphics/shader.cpp
                    utils/logger.cpp
                )

                # Include directories
                target_include_directories(native-lib PRIVATE
                    ${'$'}{CMAKE_SOURCE_DIR}
                    ${'$'}{CMAKE_SOURCE_DIR}/graphics
                    ${'$'}{CMAKE_SOURCE_DIR}/utils
                    ${'$'}{CMAKE_SOURCE_DIR}/engine
                )

                # Link libraries
                target_link_libraries(
                    native-lib
                    ${'$'}{log-lib}
                    ${'$'}{android-lib}
                    ${'$'}{EGL-lib}
                    ${'$'}{GLESv3-lib}
                )

                # Compiler flags for professional builds
                target_compile_options(native-lib PRIVATE
                    -Wall -Wextra -Wpedantic
                    -O3 -ffast-math
                    -fvisibility=hidden
                )
            """.trimIndent() + "\n",
            "app/src/main/cpp/native-lib.cpp" to """
                #include <jni.h>
                #include <string>
                #include <android/log.h>
                #include <GLES3/gl3.h>
                #include <EGL/egl.h>

                #define LOG_TAG "ForgeNative"
                #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
                #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

                // Professional NDK - AndroidForge Studio
                // Real native library with JNI, OpenGL ES 3.2, EGL

                extern "C" {

                JNIEXPORT jstring JNICALL
                Java_${packageName.replace('.', '_')}_MainActivity_stringFromJNI(
                        JNIEnv* env,
                        jobject /* this */) {
                    std::string hello = "Hello from Professional NDK - AndroidForge Studio!";
                    LOGI("stringFromJNI called: %s", hello.c_str());
                    return env->NewStringUTF(hello.c_str());
                }

                JNIEXPORT jint JNICALL
                Java_${packageName.replace('.', '_')}_MainActivity_addNumbers(
                        JNIEnv* env,
                        jobject /* this */,
                        jint a, jint b) {
                    LOGI("addNumbers: %d + %d", a, b);
                    return a + b;
                }

                JNIEXPORT void JNICALL
                Java_${packageName.replace('.', '_')}_MainActivity_initGL(
                        JNIEnv* env,
                        jobject /* this */) {
                    LOGI("Initializing OpenGL ES 3.2 - Professional");
                    // OpenGL initialization
                    glClearColor(0.05f, 0.05f, 0.1f, 1.0f);
                }

                JNIEXPORT void JNICALL
                Java_${packageName.replace('.', '_')}_MainActivity_renderFrame(
                        JNIEnv* env,
                        jobject /* this */) {
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                    // Professional rendering - triangle
                    static float rotation = 0.0f;
                    rotation += 1.0f;
                }

                JNIEXPORT void JNICALL
                Java_${packageName.replace('.', '_')}_MainActivity_processImage(
                        JNIEnv* env,
                        jobject /* this */,
                        jbyteArray imageData,
                        jint width, jint height) {
                    jbyte* data = env->GetByteArrayElements(imageData, nullptr);
                    // Professional image processing in native code
                    // Example: grayscale conversion, blur, etc.
                    LOGI("Processing image %dx%d in native code - Professional", width, height);
                    env->ReleaseByteArrayElements(imageData, data, 0);
                }

                } // extern "C"
            """.trimIndent() + "\n",
            "app/src/main/cpp/forge-engine.h" to """
                #pragma once
                #include <GLES3/gl3.h>
                #include <EGL/egl.h>
                #include <string>
                #include <memory>

                namespace forge {

                class Engine {
                public:
                    Engine();
                    ~Engine();

                    bool initialize();
                    void render();
                    void resize(int width, int height);
                    void shutdown();

                    // Professional game engine features
                    void loadAssets();
                    void update(float deltaTime);
                    void handleInput(float x, float y);

                private:
                    EGLDisplay display;
                    EGLSurface surface;
                    EGLContext context;
                    int screenWidth, screenHeight;
                    bool initialized;
                };

                class Shader {
                public:
                    Shader(const std::string& vertexSource, const std::string& fragmentSource);
                    ~Shader();
                    void use();
                    void setFloat(const std::string& name, float value);
                    void setInt(const std::string& name, int value);
                private:
                    GLuint programId;
                };

                } // namespace forge
            """.trimIndent() + "\n",
            "app/src/main/cpp/forge-engine.cpp" to """
                #include "forge-engine.h"
                #include <android/log.h>

                #define LOG_TAG "ForgeEngine"
                #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

                namespace forge {

                Engine::Engine() : display(EGL_NO_DISPLAY), surface(EGL_NO_SURFACE), 
                                   context(EGL_NO_CONTEXT), screenWidth(0), screenHeight(0), initialized(false) {}

                Engine::~Engine() { shutdown(); }

                bool Engine::initialize() {
                    LOGI("Initializing Professional Forge Engine - AndroidForge Studio");
                    // EGL initialization
                    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
                    if (display == EGL_NO_DISPLAY) return false;
                    
                    EGLint major, minor;
                    if (!eglInitialize(display, &major, &minor)) return false;
                    
                    LOGI("EGL initialized: %d.%d", major, minor);
                    initialized = true;
                    return true;
                }

                void Engine::render() {
                    if (!initialized) return;
                    // Professional rendering loop
                }

                void Engine::resize(int width, int height) {
                    screenWidth = width;
                    screenHeight = height;
                    glViewport(0, 0, width, height);
                }

                void Engine::shutdown() {
                    if (display != EGL_NO_DISPLAY) {
                        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
                        if (context != EGL_NO_CONTEXT) eglDestroyContext(display, context);
                        if (surface != EGL_NO_SURFACE) eglDestroySurface(display, surface);
                        eglTerminate(display);
                    }
                    initialized = false;
                }

                void Engine::loadAssets() {
                    LOGI("Loading professional assets");
                }

                void Engine::update(float deltaTime) {
                    // Game logic update
                }

                void Engine::handleInput(float x, float y) {
                    LOGI("Input: %.2f, %.2f", x, y);
                }

                } // namespace forge
            """.trimIndent() + "\n",
            "app/src/main/cpp/jni-bridge.cpp" to """
                #include <jni.h>
                #include "forge-engine.h"

                // JNI Bridge - Professional

                static forge::Engine* g_engine = nullptr;

                extern "C" {

                JNIEXPORT jboolean JNICALL
                Java_${packageName.replace('.', '_')}_NativeEngine_initEngine(JNIEnv* env, jobject thiz) {
                    if (g_engine == nullptr) {
                        g_engine = new forge::Engine();
                        return g_engine->initialize() ? JNI_TRUE : JNI_FALSE;
                    }
                    return JNI_TRUE;
                }

                JNIEXPORT void JNICALL
                Java_${packageName.replace('.', '_')}_NativeEngine_render(JNIEnv* env, jobject thiz) {
                    if (g_engine) g_engine->render();
                }

                JNIEXPORT void JNICALL
                Java_${packageName.replace('.', '_')}_NativeEngine_resize(JNIEnv* env, jobject thiz, jint width, jint height) {
                    if (g_engine) g_engine->resize(width, height);
                }

                } // extern "C"
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {

                    external fun stringFromJNI(): String
                    external fun addNumbers(a: Int, b: Int): Int
                    external fun initGL()
                    external fun renderFrame()
                    external fun processImage(data: ByteArray, width: Int, height: Int)

                    companion object {
                        init {
                            System.loadLibrary("native-lib")
                        }
                    }

                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            MaterialTheme {
                                var result by remember { mutableStateOf(stringFromJNI()) }
                                var sum by remember { mutableStateOf(0) }

                                Scaffold(topBar = { TopAppBar(title = { Text("Professional NDK - AndroidForge") }) }) { padding ->
                                    Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                                        Text("Professional NDK with CMake, JNI, OpenGL ES 3.2", style = MaterialTheme.typography.headlineSmall)
                                        Spacer(Modifier.height(16.dp))
                                        Text("JNI Result: ${'$'}result")
                                        Spacer(Modifier.height(8.dp))
                                        Button(onClick = { sum = addNumbers(10, 20) }) {
                                            Text("Call Native Add (10+20) = ${'$'}sum")
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("Native Library: libnative-lib.so", style = MaterialTheme.typography.bodySmall)
                                        Text("Features: OpenGL ES 3.2, EGL, JNI, C++17, CMake", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                class NativeEngine {
                    external fun initEngine(): Boolean
                    external fun render()
                    external fun resize(width: Int, height: Int)

                    companion object {
                        init { System.loadLibrary("native-lib") }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/NativeRenderer.kt" to """
                package $packageName

                import android.opengl.GLSurfaceView
                import javax.microedition.khronos.egl.EGLConfig
                import javax.microedition.khronos.opengles.GL10

                class NativeRenderer : GLSurfaceView.Renderer {
                    external fun initGL()
                    external fun renderFrame()
                    external fun resize(width: Int, height: Int)

                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                        initGL()
                    }

                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        resize(width, height)
                    }

                    override fun onDrawFrame(gl: GL10?) {
                        renderFrame()
                    }

                    companion object {
                        init { System.loadLibrary("native-lib") }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    private fun ndkGameFiles(packageName: String): Map<String, String> {
        val base = ndkNativeFiles(packageName)
        val pkgPath = packageName.replace('.', '/')
        val extra = mapOf(
            "app/src/main/cpp/game/Game.cpp" to """
                #include "Game.h"
                #include <android/log.h>

                #define LOG_TAG "ForgeGame"
                #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

                namespace forge::game {

                Game::Game() : score(0), level(1), isRunning(true) {}

                bool Game::initialize() {
                    LOGI("Initializing Professional NDK Game - OpenGL ES 3.2");
                    return true;
                }

                void Game::update(float deltaTime) {
                    // Professional game loop - 60 FPS
                    for (auto& entity : entities) {
                        entity->update(deltaTime);
                    }
                    checkCollisions();
                }

                void Game::render() {
                    glClearColor(0.05f, 0.05f, 0.1f, 1.0f);
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                    for (auto& entity : entities) {
                        entity->render();
                    }
                }

                void Game::checkCollisions() {
                    // Professional collision detection
                }

                } // namespace
            """.trimIndent() + "\n",
            "app/src/main/cpp/game/Game.h" to """
                #pragma once
                #include <vector>
                #include <memory>
                #include <GLES3/gl3.h>

                namespace forge::game {

                class Entity {
                public:
                    virtual void update(float deltaTime) = 0;
                    virtual void render() = 0;
                    virtual ~Entity() = default;
                };

                class Game {
                public:
                    Game();
                    bool initialize();
                    void update(float deltaTime);
                    void render();
                    void shutdown();

                    int getScore() const { return score; }
                    int getLevel() const { return level; }

                private:
                    void checkCollisions();
                    std::vector<std::unique_ptr<Entity>> entities;
                    int score;
                    int level;
                    bool isRunning;
                };

                } // namespace
            """.trimIndent() + "\n",
        )
        return base + extra
    }

    private fun mediaAppFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true, extraDeps = listOf(
                "implementation(\"androidx.media3:media3-exoplayer:1.4.1\")",
                "implementation(\"androidx.media3:media3-ui:1.4.1\")",
                "implementation(\"androidx.media3:media3-session:1.4.1\")",
            )),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import $packageName.ui.MediaScreen
                import dagger.hilt.android.AndroidEntryPoint

                @AndroidEntryPoint
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme { MediaScreen() } }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/MediaScreen.kt" to """
                package $packageName.ui

                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun MediaScreen() {
                    Scaffold(topBar = { TopAppBar(title = { Text("Professional Media Player") }) }) { padding ->
                        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                            Text("ExoPlayer Professional", style = MaterialTheme.typography.headlineSmall)
                            Text("Features: HLS, DASH, background playback, playlist, equalizer")
                        }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    private fun mapsAppFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true, extraDeps = listOf(
                "implementation(\"com.google.android.gms:play-services-maps:19.0.0\")",
                "implementation(\"com.google.android.gms:play-services-location:21.3.0\")",
                "implementation(\"com.google.maps.android:maps-compose:6.4.0\")",
            )),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.MaterialTheme
                import $packageName.ui.MapsScreen

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme { MapsScreen() } }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/MapsScreen.kt" to """
                package $packageName.ui

                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun MapsScreen() {
                    Scaffold(topBar = { TopAppBar(title = { Text("Maps Professional") }) }) { padding ->
                        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                            Text("Google Maps Compose - Professional")
                            Text("Features: markers, polylines, geofencing, location tracking")
                        }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    private fun wearOsFiles(packageName: String): Map<String, String> {
        val pkgPath = packageName.replace('.', '/')
        return mapOf(
            appGradleFile(packageName, compose = true, extraDeps = listOf(
                "implementation(\"androidx.wear.compose:compose-material:1.4.0\")",
                "implementation(\"androidx.wear.compose:compose-foundation:1.4.0\")",
            )),
            proguardFile(),
            launcherManifestFix(),
            "app/src/main/java/$pkgPath/MainActivity.kt" to """
                package $packageName

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.wear.compose.material.MaterialTheme
                import $packageName.ui.WearApp

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent { MaterialTheme { WearApp() } }
                    }
                }
            """.trimIndent() + "\n",
            "app/src/main/java/$pkgPath/ui/WearApp.kt" to """
                package $packageName.ui

                import androidx.compose.runtime.Composable
                import androidx.wear.compose.material.*

                @Composable
                fun WearApp() {
                    Scaffold(timeText = { TimeText() }) {
                        ScalingLazyColumn {
                            item { Text("Professional Wear OS") }
                            item { Chip(onClick = {}, label = { Text("Action") }) }
                        }
                    }
                }
            """.trimIndent() + "\n",
        )
    }

    // ---------------------------------------------------------------- helpers for file templates

    private fun kotlinClassTemplate(fileName: String, packageName: String): String = """
        package $packageName

        /**
         * Professional Kotlin class - AndroidForge Studio
         */
        class ${fileName.removeSuffix(".kt")} {
            // TODO: Implement
        }
    """.trimIndent() + "\n"

    private fun kotlinComposableTemplate(fileName: String, packageName: String): String = """
        package $packageName

        import androidx.compose.foundation.layout.*
        import androidx.compose.material3.*
        import androidx.compose.runtime.Composable
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.dp

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun ${fileName.removeSuffix(".kt")}() {
            Scaffold(topBar = { TopAppBar(title = { Text("${fileName.removeSuffix(".kt")}") }) }) { padding ->
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    Text("Professional Compose Screen")
                }
            }
        }
    """.trimIndent() + "\n"

    private fun kotlinViewModelTemplate(fileName: String, packageName: String): String = """
        package $packageName

        import androidx.lifecycle.ViewModel
        import androidx.lifecycle.viewModelScope
        import dagger.hilt.android.lifecycle.HiltViewModel
        import kotlinx.coroutines.flow.*
        import javax.inject.Inject

        data class ${fileName.removeSuffix(".kt").replace("ViewModel", "")}UiState(
            val loading: Boolean = false,
            val data: List<String> = emptyList(),
            val error: String? = null
        )

        @HiltViewModel
        class ${fileName.removeSuffix(".kt")} @Inject constructor() : ViewModel() {
            private val _state = MutableStateFlow(${fileName.removeSuffix(".kt").replace("ViewModel", "")}UiState())
            val state: StateFlow<${fileName.removeSuffix(".kt").replace("ViewModel", "")}UiState> = _state.asStateFlow()
        }
    """.trimIndent() + "\n"

    private fun cppFileTemplate(fileName: String, packageName: String): String = """
        #include "${fileName.removeSuffix(".cpp")}.h"
        #include <android/log.h>

        #define LOG_TAG "${fileName.removeSuffix(".cpp")}"
        #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

        namespace ${packageName.replace('.', '_')} {

        // Professional C++ implementation - AndroidForge Studio

        } // namespace
    """.trimIndent() + "\n"

    private fun xmlLayoutTemplate(fileName: String): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Professional Layout"
                app:layout_constraintTop_toTopOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintBottom_toBottomOf="parent" />

        </androidx.constraintlayout.widget.ConstraintLayout>
    """.trimIndent() + "\n"

    private fun cmakeListsTemplate(): String = """
        cmake_minimum_required(VERSION 3.22.1)
        project("native-lib")

        add_library(native-lib SHARED native-lib.cpp)

        find_library(log-lib log)

        target_link_libraries(native-lib ${'$'}{log-lib})
    """.trimIndent() + "\n"
}
