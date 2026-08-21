package com.example.nativeapp

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun project_configuration_isValid() {
        val appName = "NativeCppApp"
        assertTrue(appName.isNotBlank())
    }
}