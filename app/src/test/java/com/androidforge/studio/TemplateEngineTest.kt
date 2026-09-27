package com.androidforge.studio

import com.androidforge.studio.data.template.TemplateEngine
import com.androidforge.studio.domain.model.Template
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateEngineTest {

    @Test
    fun `all built-in templates generate required files`() {
        for (template in Template.entries) {
            val files = TemplateEngine.generate(template.id, "MyApp", "com.example.myapp")
            assertTrue("${template.id}: missing settings.gradle.kts", files.containsKey("settings.gradle.kts"))
            assertTrue("${template.id}: missing root build.gradle.kts", files.containsKey("build.gradle.kts"))
            assertTrue("${template.id}: missing app build.gradle.kts", files.containsKey("app/build.gradle.kts"))
            assertTrue("${template.id}: missing manifest", files.keys.any { it.endsWith("AndroidManifest.xml") })
            assertTrue("${template.id}: missing MainActivity", files.keys.any { it.endsWith("MainActivity.kt") })
            assertTrue("${template.id}: missing proguard", files.containsKey("app/proguard-rules.pro"))
        }
    }

    @Test
    fun `package name is substituted into sources`() {
        val files = TemplateEngine.generate(Template.EMPTY_COMPOSE.id, "MyApp", "com.acme.rocks")
        val mainActivity = files.entries.first { it.key.endsWith("MainActivity.kt") }.value
        assertTrue(mainActivity.contains("package com.acme.rocks"))
        val appGradle = files["app/build.gradle.kts"]!!
        assertTrue(appGradle.contains("namespace = \"com.acme.rocks\""))
        assertTrue(appGradle.contains("applicationId = \"com.acme.rocks\""))
    }

    @Test
    fun `project name appears in settings and strings`() {
        val files = TemplateEngine.generate(Template.XML_LAYOUT.id, "Cool App", "com.cool.app")
        assertTrue(files["settings.gradle.kts"]!!.contains("rootProject.name = \"Cool App\""))
        val strings = files.entries.first { it.key.endsWith("strings.xml") }.value
        assertTrue(strings.contains("Cool App"))
    }

    @Test
    fun `compose template declares compose plugin`() {
        val files = TemplateEngine.generate(Template.EMPTY_COMPOSE.id, "X", "com.x.y")
        val gradle = files["app/build.gradle.kts"]!!
        assertTrue(gradle.contains("org.jetbrains.kotlin.plugin.compose"))
        assertTrue(gradle.contains("material3"))
    }

    @Test
    fun `xml template does not use compose plugin`() {
        val files = TemplateEngine.generate(Template.XML_LAYOUT.id, "X", "com.x.y")
        val gradle = files["app/build.gradle.kts"]!!
        assertTrue(!gradle.contains("org.jetbrains.kotlin.plugin.compose"))
        assertTrue(gradle.contains("constraintlayout") || gradle.contains("appcompat"))
    }

    @Test
    fun `ecommerce template contains catalog model`() {
        val files = TemplateEngine.generate(Template.ECOMMERCE.id, "Shop", "com.shop.app")
        assertTrue(files.keys.any { it.endsWith("Product.kt") })
        assertTrue(files.keys.any { it.endsWith("ShopScreen.kt") })
    }

    @Test
    fun `chat template contains message model`() {
        val files = TemplateEngine.generate(Template.CHAT.id, "Chat", "com.chat.app")
        assertTrue(files.keys.any { it.endsWith("Message.kt") })
        assertTrue(files.keys.any { it.endsWith("ChatScreen.kt") })
    }

    @Test
    fun `game template contains game screen`() {
        val files = TemplateEngine.generate(Template.GAME_2D.id, "Game", "com.game.app")
        assertTrue(files.keys.any { it.endsWith("GameScreen.kt") })
    }

    @Test
    fun `firebase template includes google services example`() {
        val files = TemplateEngine.generate(Template.FIREBASE.id, "Fire", "com.fire.app")
        assertTrue(files.keys.any { it.contains("google-services.json") })
        assertTrue(files["app/build.gradle.kts"]!!.contains("firebase-bom"))
    }

    @Test
    fun `every generated file has non-blank content`() {
        val files = TemplateEngine.generate(Template.EMPTY_COMPOSE.id, "X", "com.x.y")
        for ((path, content) in files) {
            assertTrue("$path is blank", content.isNotBlank())
        }
    }
}
