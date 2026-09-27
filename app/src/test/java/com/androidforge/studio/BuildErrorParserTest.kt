package com.androidforge.studio

import com.androidforge.studio.data.build.BuildErrorParser
import com.androidforge.studio.domain.model.BuildDiagnostic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildErrorParserTest {

    @Test
    fun `parses kotlinc error lines`() {
        val output = """
            e: file:///data/projects/1/app/src/main/java/App.kt:12:5 Unresolved reference: foo
            w: file:///data/projects/1/app/src/main/java/App.kt:20:1 unused variable
        """.trimIndent()
        val diagnostics = BuildErrorParser.parse(output)
        assertEquals(2, diagnostics.size)

        val err = diagnostics[0]
        assertEquals("/data/projects/1/app/src/main/java/App.kt", err.filePath)
        assertEquals(12, err.line)
        assertTrue(err.message.contains("Unresolved reference"))
        assertEquals(BuildDiagnostic.Severity.ERROR, err.severity)

        val warn = diagnostics[1]
        assertEquals(BuildDiagnostic.Severity.WARNING, warn.severity)
        assertEquals(20, warn.line)
    }

    @Test
    fun `parses javac error lines`() {
        val output = "/src/Main.java:7: error: cannot find symbol"
        val diagnostics = BuildErrorParser.parse(output)
        assertEquals(1, diagnostics.size)
        assertEquals("/src/Main.java", diagnostics[0].filePath)
        assertEquals(7, diagnostics[0].line)
    }

    @Test
    fun `parses generic failure without file`() {
        val output = "error: unable to build apk"
        val diagnostics = BuildErrorParser.parse(output)
        assertTrue(diagnostics.isNotEmpty())
        assertTrue(diagnostics.first().message.contains("unable to build"))
    }

    @Test
    fun `clean output produces no diagnostics`() {
        val output = "BUILD SUCCESSFUL in 12s\n12 actionable tasks executed"
        val diagnostics = BuildErrorParser.parse(output)
        assertTrue(diagnostics.isEmpty())
    }

    @Test
    fun `gradleFailure extracts what went wrong`() {
        val output = """
            > Task :app:compileKotlin FAILED

            What went wrong:
            Execution failed for task ':app:compileKotlin'.
            > Compilation error: Unresolved reference

            * Try:
            Run with --stacktrace
        """.trimIndent()
        val failure = BuildErrorParser.gradleFailure(output)
        assertNotNull(failure)
        assertTrue(failure!!.contains("Execution failed"))
    }

    @Test
    fun `gradleFailure returns null without section`() {
        assertEquals(null, BuildErrorParser.gradleFailure("all good"))
    }

    @Test
    fun `capLog truncates huge logs`() {
        val big = "x".repeat(300_000)
        val capped = BuildErrorParser.capLog(big, 1000)
        assertTrue(capped.length <= 1000 + 20)
        assertTrue(capped.contains("truncated"))
    }
}
