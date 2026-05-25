package com.fioiu8.linkbot.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import androidx.compose.foundation.BorderStroke

/**
 * Markdown 文本渲染组件
 * 
 * 支持的 Markdown 特性：
 * - 粗体 **text**
 * - 斜体 *text*
 * - 删除线 ~~text~~
 * - 行内代码 `code`
 * - 代码块 ```language ... ```
 * - 引用块 > text
 * - 公式块 $$ formula $$
 * - 表格 | header | header |
 * - 链接 [text](url)
 * - 脚注 [^ref] 和 [^ref]: definition
 * - 列表（有序和无序）
 * - 标题 #, ##, ###, etc.
 *
 * @param content Markdown 格式的文本内容
 * @param contentAlignment 内容水平对齐方式，默认为左对齐
 */
@Composable
fun MarkdownText(
    content: String,
    contentAlignment: Alignment.Horizontal = Alignment.Start
) {
    /**
     * 根据主题模式设置颜色
     * 
     * 使用 MiuixTheme.colorSchemeMode 获取当前主题模式，
     * 当模式为 System/MonetSystem 时使用 MiuixTheme.colorScheme.isDark 获取系统实际深色状态
     */
    val isDark = when (MiuixTheme.colorSchemeMode) {
        ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> true
        ColorSchemeMode.System, ColorSchemeMode.MonetSystem -> MiuixTheme.colorScheme.isDark
        else -> false
    }
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val codeBg = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5F5F5)
    val quoteColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFFF0F0F0)
    
    // 获取 URI 处理器和上下文
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // 按行分割内容
    val lines = content.split("\n")
    
    // 状态变量：追踪代码块、表格、引用等状态
    var inCodeBlock = false
    var codeLanguage = ""
    val codeContent = StringBuilder()
    var inTable = false
    val tableHeaders = mutableListOf<String>()
    val tableAlignments = mutableListOf<String>()
    val tableRows = mutableListOf<List<String>>()
    var quoteLines = mutableListOf<String>()

    // 解析脚注定义 [^ref]: definition
    val footnotes = remember { mutableMapOf<String, String>() }
    lines.forEach { line ->
        val fnMatch = Regex("""^\[\^(\S+)\]:\s*(.+)$""").find(line)
        if (fnMatch != null) {
            footnotes[fnMatch.groupValues[1]] = fnMatch.groupValues[2]
        }
    }
    Column(
        Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = contentAlignment
    ) {
        lines.forEachIndexed { lineIndex, line ->
            if (line.trimStart().startsWith(">") && !inCodeBlock) {
                quoteLines.add(line.trimStart().removePrefix(">").trimStart())
                val nextLine = lines.getOrNull(lineIndex + 1)
                if (nextLine == null || !nextLine.trimStart().startsWith(">")) {
                    QuoteBlock(quoteLines.joinToString("\n"), quoteColor, isDark, uriHandler)
                    quoteLines.clear()
                }
                return@forEachIndexed
            }

            when {
                line.trimStart().startsWith("$$") && line.trim().endsWith("$$") && line.trim().length > 4 -> {
                    FormulaBlock(line.trim().removePrefix("$$").removeSuffix("$$").trim(), isDark)
                }
                line.trimStart().startsWith("```") -> {
                    if (inCodeBlock) {
                        CodeBlockView(codeContent.toString(), codeLanguage, codeBg, isDark, context)
                        codeContent.clear()
                        codeLanguage = ""
                    } else {
                        codeLanguage = line.trimStart().removePrefix("```").trim()
                    }
                    inCodeBlock = !inCodeBlock
                    return@forEachIndexed
                }
                inCodeBlock -> {
                    if (codeContent.isNotEmpty()) codeContent.append("\n")
                    codeContent.append(line)
                    return@forEachIndexed
                }
                line.trimStart().startsWith("|") && line.trimEnd().endsWith("|") -> {
                    handleTableLine(line, lines, lineIndex, tableHeaders, tableAlignments, tableRows, isDark, uriHandler)
                }
                else -> {
                    if (inTable && tableHeaders.isNotEmpty()) {
                        TableView(tableHeaders, tableAlignments, tableRows, isDark, uriHandler)
                        tableHeaders.clear()
                        tableAlignments.clear()
                        tableRows.clear()
                        inTable = false
                    }
                }
            }

            if (!inCodeBlock && !inTable && quoteLines.isEmpty()) {
                RenderLine(line, textColor, uriHandler, footnotes, isDark)
            }
        }

        if (inCodeBlock && codeContent.isNotEmpty()) {
            CodeBlockView(codeContent.toString(), codeLanguage, codeBg, isDark, context)
            codeContent.clear()
            codeLanguage = ""
            inCodeBlock = false
        }

        if (inTable && tableHeaders.isNotEmpty()) {
            TableView(tableHeaders, tableAlignments, tableRows, isDark, uriHandler)
            tableHeaders.clear()
            tableAlignments.clear()
            tableRows.clear()
        }
    }
}

/**
 * 渲染单行文本（处理标题、列表等特殊行）
 * @param line 单行文本内容
 * @param textColor 文本颜色
 * @param uriHandler URI 处理器（用于打开链接）
 * @param footnotes 脚注映射表
 * @param isDark 是否为深色模式
 */
@Composable
private fun RenderLine(line: String, textColor: Color, uriHandler: UriHandler, footnotes: Map<String, String>, isDark: Boolean) {
    val codeBg = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF5F5F5)
    val linkColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)

    // 处理标题
    when {
        line.startsWith("### ") -> Text(line.removePrefix("### "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        line.startsWith("## ") -> Text(line.removePrefix("## "), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        line.startsWith("# ") -> Text(line.removePrefix("# "), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
        // 处理有序列表
        line.matches(Regex("""^\d+\.\s+.*""")) -> RichText(line, uriHandler, footnotes)
        // 处理无序列表
        line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> RichText(line, uriHandler, footnotes)
        // 处理普通文本
        else -> RichText(line, uriHandler, footnotes)
    }
}

/**
 * 渲染富文本（支持链接、粗体、斜体、代码、删除线、脚注）
 * @param text 文本内容
 * @param uriHandler URI 处理器（用于打开链接）
 * @param footnotes 脚注映射表
 */
@Composable
private fun RichText(
    text: String,
    uriHandler: UriHandler,
    footnotes: Map<String, String> = emptyMap()
) {
    // 根据主题模式设置颜色
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val linkColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)

    // 构建带注解的字符串，支持多种格式
    val annotated = buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            // 定义各种 Markdown 元素的正则表达式
            val linkRegex = Regex("""\[(.*?)\]\((\S+?)\)""")           // [text](url)
            val linkMatch = linkRegex.find(remaining)
            val urlRegex = Regex("""(https?://\S+)""")                  // http://...
            val urlMatch = if (linkMatch == null) urlRegex.find(remaining) else null
            val boldRegex = Regex("\\*\\*(.+?)\\*\\*")                  // **text**
            val boldMatch = boldRegex.find(remaining)
            val italicRegex = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")  // *text* (排除 **)
            val italicMatch = if (boldMatch == null) italicRegex.find(remaining) else null
            val codeRegex = Regex("`(.+?)`")                            // `code`
            val codeMatch = codeRegex.find(remaining)
            val strikeRegex = Regex("~~(.+?)~~")                        // ~~text~~
            val strikeMatch = strikeRegex.find(remaining)
            val fnRegex = Regex("""\[\^(\S+)\]""")                      // [^ref]
            val fnMatch = fnRegex.find(remaining)

            // 将所有匹配项按位置排序
            val matches = listOfNotNull(
                linkMatch?.let { "link" to it },
                urlMatch?.let { "url" to it },
                boldMatch?.let { "bold" to it },
                italicMatch?.let { "italic" to it },
                codeMatch?.let { "code" to it },
                strikeMatch?.let { "strike" to it },
                fnMatch?.let { "footnote" to it }
            ).sortedBy { it.second.range.first }

            // 如果没有匹配项，直接追加剩余文本
            if (matches.isEmpty()) {
                append(remaining)
                break
            }

            // 处理第一个匹配项
            val (type, match) = matches.first()
            append(remaining.substring(0, match.range.first))

            when (type) {
                "link" -> {
                    // 处理链接 [text](url)
                    val linkText = match.groupValues[1]
                    val url = match.groupValues[2]
                    val start = length
                    withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { 
                        append(linkText) 
                    }
                    addStringAnnotation("URL", url, start, length)
                }
                "url" -> {
                    // 处理纯 URL
                    val url = match.value
                    val start = length
                    withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { 
                        append(url) 
                    }
                    addStringAnnotation("URL", url, start, length)
                }
                "footnote" -> {
                    // 处理脚注引用 [^ref]
                    val fnId = match.groupValues[1]
                    withStyle(SpanStyle(
                        color = linkColor,
                        fontSize = 12.sp,
                        baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript
                    )) {
                        append("[$fnId]")
                    }
                }
                "bold" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
                "italic" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(match.groupValues[1]) }
                "code" -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = if (isDark) Color(0xFF444444) else Color(0xFFEEEEEE), fontSize = 14.sp)) { append(match.groupValues[1]) }
                "strike" -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(match.groupValues[1]) }
            }
            remaining = remaining.substring(match.range.last + 1)
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = annotated,
        modifier = Modifier
            .wrapContentWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    layoutResult.value?.let { layout ->
                        annotated.getStringAnnotations(
                            "URL",
                            layout.getOffsetForPosition(offset),
                            layout.getOffsetForPosition(offset)
                        )
                            .firstOrNull()
                            ?.let { try { uriHandler.openUri(it.item) } catch (_: Exception) {} }
                    }
                }
            },
        onTextLayout = { layoutResult.value = it }
    )
}

@Composable
private fun QuoteBlock(content: String, bgColor: Color, isDark: Boolean, uriHandler: UriHandler) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (isDark) Color(0xFF888888) else Color(0xFF666666), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                RichText(content, uriHandler)
            }
        }
    }
}

@Composable
private fun CodeBlockView(code: String, language: String, bgColor: Color, isDark: Boolean, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    Surface(modifier = Modifier.fillMaxWidth(), color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(language.ifEmpty { "code" }, fontSize = 11.sp, color = if (isDark) Color(0xFF888888) else Color(0xFF999999), fontFamily = FontFamily.Monospace)
                IconButton(onClick = { clipboard.setPrimaryClip(ClipData.newPlainText("code", code)) }, minWidth = 28.dp, minHeight = 28.dp, cornerRadius = 14.dp) {
                    Icon(MiuixIcons.Copy, "复制", tint = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(code.trimEnd(), fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333))
        }
    }
}

@Composable
private fun FormulaBlock(formula: String, isDark: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF1A1A2E) else Color(0xFFF8F8FF)
    ) {
        Text(formula, modifier = Modifier.padding(16.dp).fillMaxWidth(), fontSize = 18.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
    }
}

@Composable
private fun TableView(headers: List<String>, alignments: List<String>, rows: List<List<String>>, isDark: Boolean, uriHandler: UriHandler) {
    val borderColor = if (isDark) Color(0xFF444444) else Color(0xFFDDDDDD)
    val headerBg = if (isDark) Color(0xFF333333) else Color(0xFFF0F0F0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Column {
            Surface(
                color = headerBg,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    headers.forEachIndexed { _, h ->
                        Box(modifier = Modifier.weight(1f)) {
                            RichText(h, uriHandler)
                        }
                    }
                }
            }
            HorizontalDivider(color = borderColor)

            rows.forEachIndexed { ri, row ->
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    row.forEachIndexed { _, c ->
                        Box(modifier = Modifier.weight(1f)) {
                            RichText(c, uriHandler)
                        }
                    }
                }
                if (ri < rows.size - 1) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun handleTableLine(line: String, lines: List<String>, lineIndex: Int, headers: MutableList<String>, alignments: MutableList<String>, rows: MutableList<List<String>>, isDark: Boolean, uriHandler: UriHandler) {
    val cells = line.trim().split("|").map { it.trim() }.filter { it.isNotEmpty() }
    if (cells.isEmpty()) return
    if (cells.all { it.matches(Regex("^:?-+:?$")) }) {
        alignments.clear()
        alignments.addAll(cells.map { when { it.startsWith(":") && it.endsWith(":") -> "center"; it.endsWith(":") -> "right"; else -> "left" } })
    } else if (headers.isEmpty()) {
        headers.clear(); headers.addAll(cells)
    } else {
        rows.add(cells)
    }
    val nextLine = lines.getOrNull(lineIndex + 1)
    if (nextLine == null || !nextLine.trimStart().startsWith("|")) {
        if (headers.isNotEmpty()) TableView(headers, alignments, rows, isDark, uriHandler)
        headers.clear(); alignments.clear(); rows.clear()
    }
}