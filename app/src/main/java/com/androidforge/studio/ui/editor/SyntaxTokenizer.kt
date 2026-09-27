package com.androidforge.studio.ui.editor

import androidx.compose.ui.graphics.Color
import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.ui.theme.ForgeAmber
import com.androidforge.studio.ui.theme.ForgeBlue
import com.androidforge.studio.ui.theme.ForgeOrange
import com.androidforge.studio.ui.theme.ForgeRed
import com.androidforge.studio.ui.theme.ForgeTeal

/**
 * Lightweight regex/scan-based syntax tokenizer.
 *
 * Produces a flat list of (start, end, tokenType) spans for a line-oriented
 * highlighter. Good enough for mobile-sized files; a Tree-sitter/LSP backend
 * can replace it behind the same interface later.
 */
enum class TokenType { TEXT, KEYWORD, STRING, COMMENT, NUMBER, ANNOTATION, TAG, ATTR, PUNCT, FUNC, TYPE }

data class Token(val start: Int, val end: Int, val type: TokenType) {
    init { require(end >= start) }
}

data class HighlightStyle(
    val keyword: Color = ForgeOrange,
    val string: Color = ForgeTeal,
    val comment: Color = Color(0xFF6A737D),
    val number: Color = ForgeAmber,
    val annotation: Color = ForgeBlue,
    val tag: Color = ForgeOrange,
    val attr: Color = ForgeAmber,
    val func: Color = ForgeBlue,
    val type: Color = Color(0xFFC792EA),
    val error: Color = ForgeRed,
)

object SyntaxTokenizer {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "object", "interface", "fun", "val", "var",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "try", "catch", "finally", "throw", "is", "in", "as", "this", "super",
        "null", "true", "false", "sealed", "data", "enum", "open", "abstract",
        "override", "private", "public", "internal", "protected", "companion",
        "suspend", "operator", "inline", "reified", "lateinit", "const", "by",
        "get", "set", "init", "constructor", "vararg", "out", "typealias", "annotation",
    )

    private val JAVA_KEYWORDS = setOf(
        "package", "import", "class", "interface", "enum", "extends", "implements",
        "public", "private", "protected", "static", "final", "abstract", "new",
        "return", "if", "else", "for", "while", "do", "switch", "case", "break",
        "continue", "try", "catch", "finally", "throw", "throws", "this", "super",
        "null", "true", "false", "void", "int", "long", "double", "float", "boolean",
        "char", "byte", "short", "instanceof", "default",
    )

    private val GRADLE_KEYWORDS = setOf(
        "plugins", "dependencies", "android", "implementation", "api", "testImplementation",
        "androidTestImplementation", "ksp", "kapt", "apply", "false", "true", "val", "def",
        "apply false", "alias", "id", "version", "buildscript", "allprojects", "repositories",
        "task", "ext", "group", "doLast", "doFirst",
    )

    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while",
        "class", "extends", "new", "this", "super", "import", "export", "from",
        "async", "await", "try", "catch", "finally", "throw", "typeof", "instanceof",
        "null", "undefined", "true", "false", "switch", "case", "break", "continue",
        "do", "delete", "in", "of", "yield",
    )

    /** Tokenizes a single line (multi-line states handled by caller for block comments). */
    fun tokenizeLine(
        line: String,
        language: CodeLanguage,
        inBlockComment: Boolean = false,
    ): Pair<List<Token>, Boolean> {
        val tokens = mutableListOf<Token>()
        var blockState = inBlockComment
        var i = 0
        val n = line.length

        fun add(start: Int, end: Int, type: TokenType) {
            if (end > start) tokens += Token(start, end, type)
        }

        val keywords = when (language) {
            CodeLanguage.KOTLIN, CodeLanguage.GRADLE_KTS -> KOTLIN_KEYWORDS
            CodeLanguage.JAVA, CodeLanguage.CPP, CodeLanguage.C -> JAVA_KEYWORDS
            CodeLanguage.GROOVY, CodeLanguage.CMAKE -> GRADLE_KEYWORDS
            CodeLanguage.JAVASCRIPT -> JS_KEYWORDS
            CodeLanguage.XML, CodeLanguage.HTML -> emptySet()
            CodeLanguage.JSON, CodeLanguage.CSS, CodeLanguage.TEXT,
            CodeLanguage.YAML, CodeLanguage.PROPERTIES, CodeLanguage.GLSL, CodeLanguage.SHADER -> emptySet()
        }
        val lineComment = when (language) {
            CodeLanguage.KOTLIN, CodeLanguage.JAVA, CodeLanguage.GRADLE_KTS,
            CodeLanguage.GROOVY, CodeLanguage.JAVASCRIPT, CodeLanguage.CSS,
            CodeLanguage.CPP, CodeLanguage.C, CodeLanguage.CMAKE, CodeLanguage.GLSL, CodeLanguage.SHADER
            -> "//"
            CodeLanguage.XML, CodeLanguage.HTML -> "<!--"
            CodeLanguage.JSON, CodeLanguage.TEXT, CodeLanguage.YAML, CodeLanguage.PROPERTIES -> "\u0000" // none
        }

        // XML / HTML: tag-aware scan
        if (language == CodeLanguage.XML || language == CodeLanguage.HTML) {
            return xmlTokens(line)
        }

        while (i < n) {
            if (blockState) {
                // Continuation of a /* … block comment: look for the */ closer.
                val close = line.indexOf("*/", i)
                val end = if (close >= 0) close + 2 else n
                if (close >= 0) blockState = false
                add(i, end, TokenType.COMMENT)
                i = end
                continue
            }
            val ch = line[i]

            // block comment start
            if (language != CodeLanguage.JSON && line.startsWith("/*", i)) {
                val close = line.indexOf("*/", i + 2)
                if (close >= 0) {
                    add(i, close + 2, TokenType.COMMENT)
                    i = close + 2
                } else {
                    add(i, n, TokenType.COMMENT)
                    blockState = true
                    i = n
                }
                continue
            }
            // line comment
            if (lineComment != "\u0000" && line.startsWith(lineComment, i)) {
                add(i, n, TokenType.COMMENT)
                i = n
                continue
            }
            // strings
            if (ch == '"' || ch == '\'') {
                val quote = ch
                // triple-quoted strings (Kotlin)
                if (line.startsWith("\"\"\"", i)) {
                    val close = line.indexOf("\"\"\"", i + 3)
                    if (close >= 0) { add(i, close + 3, TokenType.STRING); i = close + 3 }
                    else { add(i, n, TokenType.STRING); i = n }
                    continue
                }
                var j = i + 1
                while (j < n) {
                    if (line[j] == '\\') { j += 2; continue }
                    if (line[j] == quote) break
                    j++
                }
                add(i, (j + 1).coerceAtMost(n), TokenType.STRING)
                i = (j + 1).coerceAtMost(n)
                continue
            }
            // annotations (@Foo) — Kotlin/Java
            if (ch == '@' && (language == CodeLanguage.KOTLIN || language == CodeLanguage.JAVA)) {
                var j = i + 1
                while (j < n && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                add(i, j, TokenType.ANNOTATION)
                i = j
                continue
            }
            // numbers
            if (ch.isDigit()) {
                var j = i
                while (j < n && (line[j].isLetterOrDigit() || line[j] == '.' || line[j] == '_')) j++
                add(i, j, TokenType.NUMBER)
                i = j
                continue
            }
            // identifiers / keywords
            if (ch.isLetter() || ch == '_') {
                var j = i
                while (j < n && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                val word = line.substring(i, j)
                val next = line.substring(j).trimStart()
                when {
                    word in keywords -> add(i, j, TokenType.KEYWORD)
                    word == "fun" || (next.startsWith("(") && word.isNotEmpty() && word[0].isLowerCase()) ->
                        add(i, j, TokenType.FUNC)
                    word.isNotEmpty() && word[0].isUpperCase() -> add(i, j, TokenType.TYPE)
                    else -> add(i, j, TokenType.TEXT)
                }
                i = j
                continue
            }
            // punctuation (only mark operators, skip whitespace)
            if (!ch.isWhitespace()) {
                add(i, i + 1, TokenType.PUNCT)
            }
            i++
        }
        return tokens to blockState
    }

    private fun xmlTokens(line: String): Pair<List<Token>, Boolean> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = line.length
        while (i < n) {
            if (line.startsWith("<!--", i)) {
                val close = line.indexOf("-->", i)
                val end = if (close >= 0) close + 3 else n
                tokens += Token(i, end, TokenType.COMMENT)
                i = end
                continue
            }
            if (line[i] == '<') {
                val close = line.indexOf('>', i)
                val end = if (close >= 0) close + 1 else n
                val seg = line.substring(i, end)
                // color tag name
                val tagNameEnd = seg.indexOfFirst { it.isWhitespace() || it == '>' || it == '/' }
                if (tagNameEnd > 1) tokens += Token(i, i + tagNameEnd, TokenType.TAG)
                // attributes: name="value"
                val attrRegex = Regex("""([\w:.-]+)\s*=\s*("[^"]*"|'[^']*')""")
                for (m in attrRegex.findAll(seg)) {
                    val s = i + m.range.first
                    tokens += Token(s, s + m.groupValues[1].length, TokenType.ATTR)
                    val vs = s + m.groupValues[1].length + (m.value.length - m.groupValues[1].length - m.groupValues[2].length)
                    tokens += Token(vs, vs + m.groupValues[2].length, TokenType.STRING)
                }
                tokens += Token(i, end, TokenType.TAG)
                i = end
                continue
            }
            // text node
            val next = line.indexOf('<', i).let { if (it < 0) n else it }
            i = next
            if (i < n && line[i] != '<') i++ // safety
        }
        // Deduplicate overlapping tokens by keeping the more specific ones
        return dedupe(tokens) to false
    }

    private fun dedupe(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        // Sort by start asc, then shortest first: specific tokens (tag name,
        // attributes) survive, and the whole-tag token spanning them is dropped.
        val sorted = tokens.sortedWith(compareBy({ it.start }, { it.end - it.start }))
        val result = mutableListOf<Token>()
        var lastEnd = -1
        for (t in sorted) {
            if (t.start >= lastEnd) {
                result += t
                lastEnd = t.end
            }
        }
        return result
    }

    /** Maps token types to span colors for the current style. */
    fun colorOf(type: TokenType, style: HighlightStyle): Color = when (type) {
        TokenType.KEYWORD -> style.keyword
        TokenType.STRING -> style.string
        TokenType.COMMENT -> style.comment
        TokenType.NUMBER -> style.number
        TokenType.ANNOTATION -> style.annotation
        TokenType.TAG -> style.tag
        TokenType.ATTR -> style.attr
        TokenType.FUNC -> style.func
        TokenType.TYPE -> style.type
        TokenType.PUNCT -> Color.Unspecified
        TokenType.TEXT -> Color.Unspecified
    }
}
