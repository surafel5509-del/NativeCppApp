package com.androidforge.studio

import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.ui.builder.ComponentType
import com.androidforge.studio.ui.builder.ComposeCodeGenerator
import com.androidforge.studio.ui.builder.LayoutParser
import com.androidforge.studio.ui.builder.UiComponent
import com.androidforge.studio.ui.builder.XmlCodeGenerator
import com.androidforge.studio.ui.builder.newComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiBuilderCodegenTest {

    private fun sampleTree(): UiComponent {
        val text = newComponent(ComponentType.TEXT, "t1").copy(props = mapOf("text" to "Hello Forge", "size" to "18"))
        val button = newComponent(ComponentType.BUTTON, "b1").copy(props = mapOf("text" to "Go"))
        val column = UiComponent(
            id = "root",
            type = ComponentType.COLUMN,
            children = listOf(text, button),
        )
        return column
    }

    @Test
    fun `compose generator emits valid-looking composable`() {
        val code = ComposeCodeGenerator.generate(sampleTree())
        assertTrue(code.contains("@Composable"))
        assertTrue(code.contains("fun GeneratedScreen()"))
        assertTrue(code.contains("Hello Forge"))
        assertTrue(code.contains("Button(onClick"))
        assertTrue(code.contains("import androidx.compose.foundation.layout.*"))
        // no duplicated child emission
        assertEquals(1, Regex("""Text\(text = "Hello Forge"""").findAll(code).count())
    }

    @Test
    fun `xml generator maps column to vertical linear layout`() {
        val xml = XmlCodeGenerator.generate(sampleTree())
        assertTrue(xml.startsWith("<?xml"))
        assertTrue(xml.contains("android:orientation=\"vertical\""))
        assertTrue(xml.contains("android:text=\"Hello Forge\""))
        assertTrue(xml.contains("<Button"))
    }

    @Test
    fun `parser round-trips basic xml`() {
        val xml = XmlCodeGenerator.generate(sampleTree())
        val parsed = LayoutParser.parse(xml)
        assertNotNull(parsed)
        assertTrue(parsed!!.type == ComponentType.COLUMN || parsed.type == ComponentType.ROW)
    }

    @Test
    fun `nested containers nest in output`() {
        val inner = UiComponent(
            id = "row1",
            type = ComponentType.ROW,
            children = listOf(newComponent(ComponentType.TEXT, "a")),
        )
        val root = UiComponent("root", ComponentType.COLUMN, children = listOf(inner))
        val code = ComposeCodeGenerator.generate(root)
        assertTrue(code.contains("Row("))
        assertTrue(code.contains("Column("))
        val colIdx = code.indexOf("Column(")
        val rowIdx = code.indexOf("Row(")
        assertTrue(colIdx < rowIdx)
    }

    @Test
    fun `new component ids are distinct`() {
        val a = newComponent(ComponentType.TEXT, "x1")
        val b = newComponent(ComponentType.TEXT, "x2")
        assertTrue(a.id != b.id)
    }

    @Test
    fun `code language detection for common files`() {
        assertEquals(CodeLanguage.KOTLIN, CodeLanguage.forFile("Main.kt"))
        assertEquals(CodeLanguage.GRADLE_KTS, CodeLanguage.forFile("build.gradle.kts"))
        assertEquals(CodeLanguage.XML, CodeLanguage.forFile("AndroidManifest.xml"))
        assertEquals(CodeLanguage.JSON, CodeLanguage.forFile("data.json"))
        assertEquals(CodeLanguage.JAVASCRIPT, CodeLanguage.forFile("app.js"))
        assertEquals(CodeLanguage.JAVA, CodeLanguage.forFile("Main.java"))
        assertEquals(CodeLanguage.TEXT, CodeLanguage.forFile("README"))
    }
}
