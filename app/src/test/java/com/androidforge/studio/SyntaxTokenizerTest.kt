package com.androidforge.studio

import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.ui.editor.SyntaxTokenizer
import com.androidforge.studio.ui.editor.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxTokenizerTest {

    @Test
    fun `kotlin keywords are tokenized as KEYWORD`() {
        val (tokens, inBlock) = SyntaxTokenizer.tokenizeLine("fun main() {", CodeLanguage.KOTLIN)
        assertFalse(inBlock)
        val funTok = tokens.first { it.start == 0 }
        assertEquals("fun", "fun")
        assertEquals(TokenType.KEYWORD, funTok.type)
        assertEquals(3, funTok.end)
    }

    @Test
    fun `strings are captured whole`() {
        val line = """val greeting = "hello world""""
        val (tokens, _) = SyntaxTokenizer.tokenizeLine(line, CodeLanguage.KOTLIN)
        val stringTok = tokens.firstOrNull { it.type == TokenType.STRING }
        assertTrue("expected a STRING token", stringTok != null)
        assertEquals("hello world".length + 2, stringTok!!.end - stringTok.start)
    }

    @Test
    fun `line comments swallow the rest of the line`() {
        val line = "val x = 1 // trailing comment"
        val (tokens, _) = SyntaxTokenizer.tokenizeLine(line, CodeLanguage.KOTLIN)
        val comment = tokens.first { it.type == TokenType.COMMENT }
        assertEquals(line.indexOf("//"), comment.start)
        assertEquals(line.length, comment.end)
    }

    @Test
    fun `block comment opens and reports state`() {
        val (tokens, stillIn) = SyntaxTokenizer.tokenizeLine("/* unclosed", CodeLanguage.KOTLIN)
        assertTrue(stillIn)
        assertEquals(TokenType.COMMENT, tokens.first().type)
    }

    @Test
    fun `block comment closes and reports state false`() {
        val (tokens, stillIn) = SyntaxTokenizer.tokenizeLine("still comment */ code", CodeLanguage.KOTLIN, true)
        assertFalse(stillIn)
        val comment = tokens.first { it.type == TokenType.COMMENT }
        val codeStart = comment.end
        assertTrue(codeStart < "still comment */ code".length)
    }

    @Test
    fun `xml tags are highlighted`() {
        val (tokens, _) = SyntaxTokenizer.tokenizeLine("""<Button android:text="Hi"/>""", CodeLanguage.XML)
        assertTrue(tokens.any { it.type == TokenType.TAG })
        assertTrue(tokens.any { it.type == TokenType.STRING })
    }

    @Test
    fun `empty line produces no tokens`() {
        val (tokens, inBlock) = SyntaxTokenizer.tokenizeLine("", CodeLanguage.KOTLIN)
        assertTrue(tokens.isEmpty())
        assertFalse(inBlock)
    }

    @Test
    fun `tokens cover non-overlapping ranges in order`() {
        val line = """class Foo(val bar: String = "x") { }"""
        val (tokens, _) = SyntaxTokenizer.tokenizeLine(line, CodeLanguage.KOTLIN)
        var last = 0
        for (t in tokens) {
            assertTrue("token out of order at ${t.start}", t.start >= last)
            last = t.end
        }
    }

    @Test
    fun `java language uses java keywords`() {
        val (tokens, _) = SyntaxTokenizer.tokenizeLine("public static void main", CodeLanguage.JAVA)
        assertEquals(TokenType.KEYWORD, tokens[0].type)
    }

    @Test
    fun `numbers are tokenized`() {
        val (tokens, _) = SyntaxTokenizer.tokenizeLine("val n = 42", CodeLanguage.KOTLIN)
        assertTrue(tokens.any { it.type == TokenType.NUMBER && it.end - it.start == 2 })
    }
}
