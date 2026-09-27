package com.androidforge.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidforge.studio.domain.model.CodeLanguage
import com.androidforge.studio.ui.theme.CodeStyle

/**
 * In-memory multi-line code editor.
 *
 * - Syntax highlighting via [SyntaxTokenizer] + VisualTransformation
 * - Line-number gutter synced to the scroll state
 * - Error underlines for lines listed in [errorLines]
 * - Optional search highlight (matches get a subtle background)
 *
 * The transformation never changes text length (OffsetMapping.Identity), so
 * cursor/selection math is unaffected by highlighting.
 */
@Composable
fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    language: CodeLanguage,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 13,
    readOnly: Boolean = false,
    errorLines: Set<Int> = emptySet(),
    searchQuery: String = "",
    highlightStyle: HighlightStyle = remember { HighlightStyle() },
) {
    val scrollState = rememberScrollState()
    val verticalScroll = rememberScrollState()
    var cursorLine by remember { mutableStateOf(1) }

    val textStyle = CodeStyle.copy(
        fontSize = fontSizeSp.sp,
        color = Color(0xFFE6EDF3),
    )

    val transformation = remember(language, highlightStyle, errorLines, searchQuery, fontSizeSp) {
        HighlightTransformation(language, highlightStyle, errorLines, searchQuery, textStyle)
    }

    val lines = remember(value) { value.count { it == '\n' } + 1 }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1117)),
    ) {
        // ---- line number gutter ----
        LineNumbersGutter(
            lineCount = lines,
            currentLine = cursorLine,
            errorLines = errorLines,
            style = textStyle,
            modifier = Modifier
                .verticalScroll(verticalScroll)
                .width(44.dp)
                .padding(top = 8.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp, end = 12.dp),
                enabled = !readOnly,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color(0xFFFF6D00)),
                visualTransformation = transformation,
                onTextLayout = { _ ->
                    // Cursor line tracking would require selection state; the
                    // gutter highlights line 1 by default. Error lines still
                    // show red regardless of cursor position.
                    cursorLine = 1
                },
            )
        }
    }
}

@Composable
private fun LineNumbersGutter(
    lineCount: Int,
    currentLine: Int,
    errorLines: Set<Int>,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val lineHeight = (style.fontSize.value * 1.5f).sp
    Box(modifier = modifier.background(Color(0xFF010409))) {
        androidx.compose.foundation.layout.Column {
            for (i in 1..lineCount) {
                val isError = i in errorLines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(44.dp)
                        .padding(horizontal = 6.dp)
                        .background(
                            if (i == currentLine) Color(0xFF1F6FEB).copy(alpha = 0.25f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    androidx.compose.material3.Text(
                        text = i.toString(),
                        style = style,
                        color = when {
                            isError -> Color(0xFFFF5252)
                            i == currentLine -> Color(0xFF8B949E)
                            else -> Color(0xFF484F58)
                        },
                        fontSize = (style.fontSize.value - 1).sp,
                    )
                }
            }
        }
    }
}

/**
 * VisualTransformation producing highlighted text.
 * Text length is preserved exactly (identity offset mapping).
 */
class HighlightTransformation(
    private val language: CodeLanguage,
    private val style: HighlightStyle,
    private val errorLines: Set<Int>,
    private val searchQuery: String,
    private val textStyle: TextStyle,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val out = buildAnnotatedString {
            var blockComment = false
            var lineStart = 0
            val lines = raw.split('\n')
            for ((idx, line) in lines.withIndex()) {
                val (tokens, stillInBlock) = SyntaxTokenizer.tokenizeLine(line, language, blockComment)
                blockComment = stillInBlock

                val isErrorLine = (idx + 1) in errorLines

                // Base style for error lines: red-ish tint + underline
                pushStyle(
                    if (isErrorLine) SpanStyle(
                        color = style.error,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    ) else SpanStyle()
                )
                var pos = 0
                for (tok in tokens) {
                    if (tok.start > pos) append(line.substring(pos, tok.start))
                    val color = SyntaxTokenizer.colorOf(tok.type, style)
                    val seg = line.substring(tok.start, tok.end.coerceAtMost(line.length))
                    if (color != Color.Unspecified) {
                        withStyle(SpanStyle(color = color)) { append(seg) }
                    } else {
                        append(seg)
                    }
                    pos = tok.end.coerceAtMost(line.length)
                }
                if (pos < line.length) append(line.substring(pos))
                pop()

                // search match background (simple case-insensitive contains per line)
                // handled via additional styling pass below
                if (idx < lines.size - 1) append('\n')
                lineStart += line.length + 1
            }

            // Search highlighting pass
            if (searchQuery.isNotBlank()) {
                val q = searchQuery
                var searchFrom = 0
                while (true) {
                    val at = raw.indexOf(q, searchFrom, ignoreCase = true)
                    if (at < 0) break
                    addStyle(
                        androidx.compose.ui.text.SpanStyle(
                            background = Color(0x66FFB74D),
                            color = Color.Unspecified,
                        ),
                        at,
                        at + q.length,
                    )
                    searchFrom = at + q.length.coerceAtLeast(1)
                }
            }
        }

        return TransformedText(out, OffsetMapping.Identity)
    }
}
