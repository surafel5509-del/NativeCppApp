package com.androidforge.studio

import com.androidforge.studio.data.ai.LocalAssistant
import com.androidforge.studio.ui.tools.CrashLogAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAssistantAndCrashTest {

    @Test
    fun `local assistant returns compose template for screen request`() {
        val reply = LocalAssistant.respond("create a settings screen", "")
        assertTrue(reply.contains("GeneratedScreen"))
        assertTrue(reply.contains("@Composable"))
    }

    @Test
    fun `local assistant triages known errors`() {
        val reply = LocalAssistant.respond("error: Unresolved reference: foo", "")
        assertTrue(reply.contains("classpath"))
    }

    @Test
    fun `local assistant never returns blank`() {
        for (prompt in listOf("hello", "explain state", "gradle dependency", "git log", "write a test")) {
            assertTrue(prompt, LocalAssistant.respond(prompt, "").isNotBlank())
        }
    }

    @Test
    fun `crash analyzer parses fatal exception block`() {
        val log = """
            09-27 10:00:00.000  1234  1234 E AndroidRuntime: FATAL EXCEPTION: main
            09-27 10:00:00.000  1234  1234 E AndroidRuntime: Process: com.example.app, PID: 1234
            09-27 10:00:00.000  1234  1234 E AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method
            09-27 10:00:00.000  1234  1234 E AndroidRuntime: 	at com.example.app.ui.MainScreenKt.MainScreen(MainScreen.kt:42)
            09-27 10:00:00.000  1234  1234 E AndroidRuntime: 	at androidx.activity.ComponentActivity.onCreate(ComponentActivity.java:1)
        """.trimIndent()

        val report = CrashLogAnalyzer.parse(log, "com.example.app")
        assertEquals("java.lang.NullPointerException", report.exceptionType)
        assertEquals(2, report.stackFrames.size)
        assertTrue(report.stackFrames[0].inProject)
        assertEquals(42, report.stackFrames[0].line)
        assertFalse(report.stackFrames[1].inProject)
        assertTrue(report.process.contains("com.example.app"))
    }

    @Test
    fun `crash analyzer handles caused-by chains`() {
        val log = """
            FATAL EXCEPTION: main
            java.lang.RuntimeException: boom
            Caused by: java.io.IOException: disk full
            	at com.example.app.DataRepo.load(DataRepo.kt:10)
        """.trimIndent()
        val report = CrashLogAnalyzer.parse(log, "com.example.app")
        assertEquals("java.lang.RuntimeException", report.exceptionType)
        assertTrue(report.causeChain.any { it.contains("disk full") })
        assertEquals("com.example.app.DataRepo", report.stackFrames[0].className)
    }

    @Test
    fun `crash analyzer survives garbage input`() {
        val report = CrashLogAnalyzer.parse("just some random text", "com.app")
        assertNotNull(report)
        assertTrue(report.stackFrames.isEmpty())
    }

    @Test
    fun `frame regex captures kotlin file and line`() {
        val log = """
            FATAL EXCEPTION: main
            java.lang.IllegalStateException: nope
            	at com.acme.app.HomeKt.Home(Home.kt:99)
        """.trimIndent()
        val report = CrashLogAnalyzer.parse(log, "com.acme.app")
        assertEquals(1, report.stackFrames.size)
        val frame = report.stackFrames[0]
        assertEquals("Home", frame.methodName)
        assertEquals("Home.kt", frame.file)
        assertEquals(99, frame.line)
        assertTrue(frame.inProject)
    }
}
