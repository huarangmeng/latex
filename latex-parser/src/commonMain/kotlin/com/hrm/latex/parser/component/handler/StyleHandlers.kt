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

/**
 * 字体样式 & 数学模式切换命令
 */
internal fun CommandRegistry.installStyleHandlers() {
    // 字体样式
    val styleMapping = mapOf(
        "mathbf" to LatexNode.Style.StyleType.BOLD,
        "textbf" to LatexNode.Style.StyleType.BOLD,
        "bf" to LatexNode.Style.StyleType.BOLD,
        "boldsymbol" to LatexNode.Style.StyleType.BOLD_SYMBOL,
        "bm" to LatexNode.Style.StyleType.BOLD_SYMBOL,
        "mathit" to LatexNode.Style.StyleType.ITALIC,
        "textit" to LatexNode.Style.StyleType.ITALIC,
        "it" to LatexNode.Style.StyleType.ITALIC,
        "mathrm" to LatexNode.Style.StyleType.ROMAN,
        "textrm" to LatexNode.Style.StyleType.ROMAN,
        "rm" to LatexNode.Style.StyleType.ROMAN,
        "mathsf" to LatexNode.Style.StyleType.SANS_SERIF,
        "textsf" to LatexNode.Style.StyleType.SANS_SERIF,
        "sf" to LatexNode.Style.StyleType.SANS_SERIF,
        "mathtt" to LatexNode.Style.StyleType.MONOSPACE,
        "texttt" to LatexNode.Style.StyleType.MONOSPACE,
        "tt" to LatexNode.Style.StyleType.MONOSPACE,
        "mathbb" to LatexNode.Style.StyleType.BLACKBOARD_BOLD,
        "mathfrak" to LatexNode.Style.StyleType.FRAKTUR,
        "mathscr" to LatexNode.Style.StyleType.SCRIPT,
        "mathcal" to LatexNode.Style.StyleType.CALLIGRAPHIC,
        // unicode-math
        "symbf" to LatexNode.Style.StyleType.BOLD_SYMBOL,
        "symit" to LatexNode.Style.StyleType.ITALIC,
        "symsf" to LatexNode.Style.StyleType.SANS_SERIF,
        "symrm" to LatexNode.Style.StyleType.ROMAN,
    )

    for ((cmd, styleType) in styleMapping) {
        register(cmd) { _, ctx, _ ->
            val content = ctx.parseArgument()
            LatexNode.Style(
                if (content != null) listOf(content) else emptyList(),
                styleType
            )
        }
    }

    // 数学模式切换
    val mathStyleMapping = mapOf(
        "displaystyle" to LatexNode.MathStyle.MathStyleType.DISPLAY,
        "textstyle" to LatexNode.MathStyle.MathStyleType.TEXT,
        "scriptstyle" to LatexNode.MathStyle.MathStyleType.SCRIPT,
        "scriptscriptstyle" to LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT,
    )

    for ((cmd, mathStyleType) in mathStyleMapping) {
        register(cmd) { _, ctx, _ ->
            val content = ctx.parseArgument()
            LatexNode.MathStyle(
                if (content != null) listOf(content) else emptyList(),
                mathStyleType
            )
        }
    }

    // 文本模式
    register("text", "mbox") { _, ctx, _ ->
        val content = ctx.parseArgument()
        val text = when (content) {
            is LatexNode.Text -> content.content
            is LatexNode.Group -> ParseUtils.extractText(content.children)
            else -> ""
        }
        LatexNode.TextMode(text)
    }
}
