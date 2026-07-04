package com.example.ui.components.markdown

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.theme.LocalCortexSpacing

@Composable
fun MarkdownRenderer(
    content: String,
    onChecklistToggle: (originalLine: String, isChecked: Boolean, lineIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalCortexSpacing.current
    val lines = content.lines()
    var inCodeBlock = false
    var codeBlockContent = StringBuilder()

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    // end of code block
                    CodeBlock(codeBlockContent.toString())
                    codeBlockContent = StringBuilder()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
            } else if (inCodeBlock) {
                codeBlockContent.append(line).append("\n")
            } else {
                RenderLine(line = line, lineIndex = index, onChecklistToggle = onChecklistToggle)
            }
        }
    }
}

@Composable
private fun RenderLine(
    line: String,
    lineIndex: Int,
    onChecklistToggle: (String, Boolean, Int) -> Unit
) {
    val spacing = LocalCortexSpacing.current
    val trimmed = line.trim()
    
    when {
        trimmed.isEmpty() -> {
            Spacer(modifier = Modifier.height(spacing.small))
        }
        trimmed.startsWith("---") || trimmed.startsWith("***") -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.medium))
        }
        trimmed.startsWith("#") -> {
            val level = trimmed.takeWhile { it == '#' }.length
            val text = trimmed.drop(level).trim()
            val style = when (level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            MarkdownText(text, style = style.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = spacing.medium, bottom = spacing.small))
        }
        trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]") -> {
            val isChecked = trimmed.startsWith("- [x]", ignoreCase = true)
            val text = trimmed.drop(5).trim()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChecklistToggle(line, !isChecked, lineIndex) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(checked = isChecked, onCheckedChange = { onChecklistToggle(line, it, lineIndex) })
                Spacer(modifier = Modifier.width(spacing.small))
                MarkdownText(
                    text = text, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
            val text = trimmed.drop(2).trim()
            Row(modifier = Modifier.padding(start = spacing.medium, top = 2.dp, bottom = 2.dp)) {
                Text("•", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = spacing.small))
                MarkdownText(text = text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
            val match = Regex("^(\\d+\\.)\\s+(.*)").find(trimmed)
            if (match != null) {
                val num = match.groupValues[1]
                val text = match.groupValues[2]
                Row(modifier = Modifier.padding(start = spacing.medium, top = 2.dp, bottom = 2.dp)) {
                    Text(num, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = spacing.small))
                    MarkdownText(text = text, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                MarkdownText(text = trimmed, style = MaterialTheme.typography.bodyLarge)
            }
        }
        trimmed.startsWith(">") -> {
            val text = trimmed.drop(1).trim()
            val primaryColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .padding(vertical = spacing.small)
                    .drawBehind {
                        drawLine(
                            color = primaryColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .padding(start = spacing.medium)
            ) {
                MarkdownText(text = text, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        trimmed.contains("|") && trimmed.startsWith("|") -> {
            // Simplified table row
            val cells = trimmed.split("|").filter { it.isNotBlank() }.map { it.trim() }
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                cells.forEach { cell ->
                    Box(modifier = Modifier.weight(1f).padding(spacing.small)) {
                        MarkdownText(text = cell, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        else -> {
            MarkdownText(text = trimmed, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

@Composable
private fun CodeBlock(content: String) {
    val spacing = LocalCortexSpacing.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.small)
    ) {
        Text(
            text = content.trimEnd(),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(spacing.medium)
        )
    }
}

@Composable
private fun MarkdownText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|_.*?_|~~.*?~~|`.*?`|\\[.*?\\]\\(.*?\\))")
        val matches = regex.findAll(text)

        for (match in matches) {
            val beforeText = text.substring(currentIndex, match.range.first)
            append(beforeText)

            val matchText = match.value
            when {
                matchText.startsWith("**") && matchText.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchText.drop(2).dropLast(2))
                    }
                }
                matchText.startsWith("*") && matchText.endsWith("*") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(matchText.drop(1).dropLast(1))
                    }
                }
                matchText.startsWith("_") && matchText.endsWith("_") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(matchText.drop(1).dropLast(1))
                    }
                }
                matchText.startsWith("~~") && matchText.endsWith("~~") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(matchText.drop(2).dropLast(2))
                    }
                }
                matchText.startsWith("`") && matchText.endsWith("`") -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Gray.copy(alpha = 0.2f)
                    )) {
                        append(matchText.drop(1).dropLast(1))
                    }
                }
                matchText.startsWith("[") && matchText.contains("](") && matchText.endsWith(")") -> {
                    val labelEnd = matchText.indexOf("](")
                    val label = matchText.substring(1, labelEnd)
                    val url = matchText.substring(labelEnd + 2, matchText.length - 1)
                    
                    pushStringAnnotation(tag = "URL", annotation = url)
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(label)
                    }
                    pop()
                }
                else -> append(matchText)
            }
            currentIndex = match.range.last + 1
        }
        append(text.substring(currentIndex))
    }

    ClickableText(
        text = annotatedString,
        style = style.copy(color = if (style.color != Color.Unspecified) style.color else MaterialTheme.colorScheme.onBackground),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle invalid URL
                    }
                }
        }
    )
}
