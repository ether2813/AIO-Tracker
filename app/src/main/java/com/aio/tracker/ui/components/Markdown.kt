package com.aio.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String, val indent: Int, val marker: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object Rule : MdBlock
    data object Space : MdBlock
}

private fun parse(md: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val numbered = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
    val bullet = Regex("^(\\s*)[-*•+]\\s+(.*)$")
    for (raw in md.replace("\r", "").lines()) {
        val line = raw.trimEnd()
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> if (out.lastOrNull() != MdBlock.Space) out += MdBlock.Space
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 4)
                out += MdBlock.Heading(level, trimmed.drop(level).trim().trim('#').trim())
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> out += MdBlock.Rule
            bullet.matches(line) -> {
                val m = bullet.find(line)!!
                out += MdBlock.Bullet(m.groupValues[2], m.groupValues[1].length / 2, "•")
            }
            numbered.matches(line) -> {
                val m = numbered.find(line)!!
                out += MdBlock.Bullet(m.groupValues[3], m.groupValues[1].length / 2, m.groupValues[2] + ".")
            }
            else -> {
                val last = out.lastOrNull()
                if (last is MdBlock.Paragraph) out[out.lastIndex] = MdBlock.Paragraph(last.text + " " + trimmed)
                else out += MdBlock.Paragraph(trimmed)
            }
        }
    }
    return out
}

/** Handles **bold**, *italic*, _italic_ and `code`. */
private fun inline(text: String, codeColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(text.substring(i + 2, end)); pop()
                    i = end + 2
                } else { append("**"); i += 2 }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeColor)); append(text.substring(i + 1, end)); pop()
                    i = end + 1
                } else { append('`'); i++ }
            }
            (text[i] == '*' || text[i] == '_') && i + 1 < text.length && text[i + 1] != ' ' &&
                (i == 0 || !text[i - 1].isLetterOrDigit()) -> {
                val c = text[i]
                val end = text.indexOf(c, i + 1)
                if (end > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(text.substring(i + 1, end)); pop()
                    i = end + 1
                } else { append(c); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parse(markdown) }
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val typo = MaterialTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEachIndexed { index, b ->
            when (b) {
                is MdBlock.Heading -> {
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Text(
                        inline(b.text, codeBg),
                        style = when (b.level) {
                            1 -> typo.titleLarge
                            2 -> typo.titleMedium
                            else -> typo.titleSmall
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is MdBlock.Bullet -> Row(Modifier.padding(start = (b.indent * 16).dp, top = 2.dp)) {
                    Text(b.marker, style = typo.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(inline(b.text, codeBg), style = typo.bodyMedium)
                }
                is MdBlock.Paragraph -> Text(inline(b.text, codeBg), style = typo.bodyMedium)
                MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 8.dp))
                MdBlock.Space -> Spacer(Modifier.height(6.dp))
            }
        }
    }
}
