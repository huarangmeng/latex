/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.model.SourceRange
import kotlin.math.roundToInt

/**
 * 颜色命令：\color, \textcolor
 */
internal fun CommandRegistry.installColorHandlers() {
    register("definecolor") { _, ctx, _ ->
        val name = ParseUtils.extractText(listOfNotNull(ctx.parseArgument())).trim()
        val model = ParseUtils.extractText(listOfNotNull(ctx.parseArgument())).trim()
        val value = ParseUtils.extractText(listOfNotNull(ctx.parseArgument())).trim()
        val resolved = resolveColor(ctx, model, value)
        if (resolved != null) ctx.defineColor(name, resolved)
        LatexNode.Text("")
    }
    register("colorlet") { _, ctx, _ ->
        val name = ParseUtils.extractText(listOfNotNull(ctx.parseArgument())).trim()
        val value = ParseUtils.extractText(listOfNotNull(ctx.parseArgument())).trim()
        ctx.defineColor(name, ctx.colors[value] ?: value)
        LatexNode.Text("")
    }
    register("color") { _, ctx, _ ->
        val color = readColor(ctx) ?: return@register LatexNode.Text("")
        LatexNode.Color(emptyList(), color, isDeclaration = true)
    }
    val colorHandler = CommandHandler { _, ctx, _ ->
        val colorName = readColor(ctx)

        val contentArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        if (colorName != null) LatexNode.Color(content, colorName) else LatexNode.Group(content)
    }

    register("textcolor", handler = colorHandler)

    // \colorbox{color}{text}
    register("colorbox") { _, ctx, _ ->
        val colorArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val colorName = ParseUtils.extractColorName(colorArg).let { ctx.colors[it] ?: it }

        val contentArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.ColorBox(content, colorName)
    }

    // \fcolorbox{borderColor}{bgColor}{text}
    register("fcolorbox") { _, ctx, _ ->
        val borderColorArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val borderColor = ParseUtils.extractColorName(borderColorArg).let { ctx.colors[it] ?: it }

        val bgColorArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val bgColor = ParseUtils.extractColorName(bgColorArg).let { ctx.colors[it] ?: it }

        val contentArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.ColorBox(content, bgColor, borderColor)
    }
}

private fun readColor(ctx: LatexParserContext): String? {
    val model = ctx.tokenStream.readOptionalTokens()?.text()?.trim()
    val value = ParseUtils.extractText(listOfNotNull(ctx.parseArgument())).trim()
    return resolveColor(ctx, model, value)
}

private fun resolveColor(ctx: LatexParserContext, model: String?, value: String): String? {
    if (model == null || model.isEmpty() || model == "named") return ctx.colors[value] ?: value
    if (model == "HTML" && value.matches(Regex("[0-9a-fA-F]{6}"))) return "#$value"
    val components = value.split(',').map { it.trim().toDoubleOrNull() }
    val rgb = when {
        model == "RGB" && components.size == 3 && components.all { it != null && it in 0.0..255.0 } -> components.map { it!! / 255.0 }
        model == "rgb" && components.size == 3 && components.all { it != null && it in 0.0..1.0 } -> components.map { it!! }
        model == "gray" && components.size == 1 && components[0]?.let { it in 0.0..1.0 } == true -> List(3) { components[0]!! }
        else -> null
    }
    if (rgb != null) return "#" + rgb.joinToString("") { (it * 255).roundToInt().toString(16).padStart(2, '0') }
    ctx.diagnostics.add(ParseDiagnostic(ctx.tokenStream.peek(-1)?.range ?: SourceRange.EMPTY,
        "Invalid color specification: [$model]{$value}", ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.INVALID_ARGUMENT))
    return null
}
