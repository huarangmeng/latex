package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

private val dataKey = Regex("[A-Za-z0-9_-]+")
private val simpleColor = Regex("[A-Za-z]+|#[0-9a-fA-F]{3}|#[0-9a-fA-F]{6}")

internal fun CommandRegistry.installMediaHandlers() {
    register("htmlClass", "htmlId", "htmlData", "htmlStyle") { command, ctx, stream ->
        val range = stream.peek(-1)?.range ?: SourceRange.EMPTY
        val value = stream.readArgumentTokens().rawText().trim()
        val body = ctx.parseArgument()
            ?: return@register invalidMetadata(ctx, range, "Missing \\$command content")
        val content = (body as? LatexNode.Group)?.children ?: listOf(body)
        var color: String? = null
        val attributes = when (command) {
            "htmlClass" -> mapOf("class" to value)
            "htmlId" -> mapOf("id" to value)
            "htmlData" -> parsePairs(value)?.mapKeys { (key, _) -> "data-$key" }
            else -> {
                // Compose 直接使用声明的颜色，导出只写入同样经过校验的属性。
                val declaration = value.trimEnd(';').split(':', limit = 2)
                color = declaration.getOrNull(1)?.trim()?.takeIf {
                    declaration.first().trim().equals("color", ignoreCase = true) && simpleColor.matches(it)
                }
                color?.let { mapOf("style" to "color: $it;") }
            }
        } ?: return@register invalidMetadata(ctx, range, "Invalid \\$command attributes", content)
        LatexNode.Html(content, attributes, color)
    }

    register("includegraphics") { _, ctx, stream ->
        val range = stream.peek(-1)?.range ?: SourceRange.EMPTY
        val raw = stream.readOptionalTokens()?.rawText().orEmpty().trim()
        val options = if (raw.isEmpty()) emptyMap() else parsePairs(raw)
        val source = ctx.expandTokens(stream.readArgumentTokens()).rawText().trim()
        if (options == null || options.keys.any { it !in setOf("alt", "width", "height", "totalheight") } ||
            options.any { (key, value) -> key != "alt" && (!ParseUtils.isDimension(value) || value.startsWith('-')) } ||
            !isImageSource(source)) {
            return@register invalidMetadata(ctx, range, "Invalid \\includegraphics source or dimensions")
        }
        val file = source.substringBefore('?').substringBefore('#').substringAfterLast('/')
        LatexNode.InlineImage(source, options["alt"] ?: file.substringBeforeLast('.'), options["height"] ?: "0.9em",
            options["width"], options["totalheight"])
    }
}

internal fun isImageSource(source: String): Boolean {
    if (source.isBlank() || source.any { it.isISOControl() }) return false
    val firstPart = source.substringBefore('/').substringBefore('?').substringBefore('#')
    return ':' !in firstPart || source.startsWith("https://", true) || source.startsWith("http://", true)
}

/** 只有最外层逗号分隔键值，花括号内的替代文本允许包含逗号。 */
private fun parsePairs(input: String): Map<String, String>? {
    val result = mutableMapOf<String, String>()
    var from = 0
    var depth = 0
    for (i in 0..input.length) {
        when (input.getOrNull(i)) {
            '{' -> depth++
            '}' -> if (--depth < 0) return null
        }
        if (i == input.length || (input[i] == ',' && depth == 0)) {
            val pair = input.substring(from, i).split('=', limit = 2)
            val key = pair.first().trim()
            if (pair.size != 2 || !dataKey.matches(key) || depth != 0) return null
            result[key] = pair.last().trim().removeSurrounding("{", "}")
            from = i + 1
        }
    }
    return result
}

private fun List<LatexToken>.rawText(): String = joinToString("") { token ->
    when (token) {
        is LatexToken.Ampersand -> "&"
        is LatexToken.Subscript -> "_"
        is LatexToken.Superscript -> "^"
        is LatexToken.Prime -> "'"
        is LatexToken.MathShift -> "$".repeat(token.count)
        else -> listOf(token).text()
    }
}

private fun invalidMetadata(
    ctx: LatexParserContext, range: SourceRange, message: String, content: List<LatexNode> = emptyList(),
): LatexNode.Error {
    ctx.diagnostics.add(ParseDiagnostic(range, message, ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.INVALID_ARGUMENT))
    return LatexNode.Error(message, content, range)
}
