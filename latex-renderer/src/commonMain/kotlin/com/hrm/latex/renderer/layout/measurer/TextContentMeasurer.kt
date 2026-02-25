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


package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.FontVariant
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.MathFontUtils
import com.hrm.latex.renderer.utils.isCenteredSymbol
import com.hrm.latex.renderer.utils.parseDimension
import com.hrm.latex.renderer.utils.spaceWidthPx

/**
 * 文本内容测量器
 */
internal class TextContentMeasurer : NodeMeasurer<LatexNode> {

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Text -> measureText(node.content, context, measurer, density)
            is LatexNode.TextMode -> measureTextMode(node.text, context, measurer)
            is LatexNode.Symbol -> measureSymbol(node, context, measurer, density)
            is LatexNode.Operator -> {
                val operatorGap = with(density) { (context.fontSize * MathConstants.OPERATOR_RIGHT_GAP).toPx() }
                val layout = measureText(
                    node.op,
                    context.copy(
                        fontStyle = FontStyle.Normal,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    ),
                    measurer,
                    density
                )
                NodeLayout(layout.width + operatorGap, layout.height, layout.baseline, layout.draw)
            }

            is LatexNode.Command -> measureText(node.name, context, measurer, density)
            is LatexNode.Space -> measureSpace(node.type, context, density)
            is LatexNode.HSpace -> measureHSpace(node, context, density)
            is LatexNode.NewLine -> NodeLayout(0f, 0f, 0f) { _, _ -> }
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    private fun measureText(
        text: String, context: RenderContext, measurer: TextMeasurer, density: Density
    ): NodeLayout {
        // 字体变体处理逻辑：
        // - 如果设置了变体字体家族(如 blackboardBold, calligraphic 等)，优先使用字体文件自身的字形
        // - 否则，使用 Unicode 数学字母作为降级方案
        val transformedText = if (context.isVariantFontFamily) {
            // 直接使用原始字符，让字体文件提供正确的字形
            text
        } else {
            // 使用 Unicode 数学字母映射作为降级方案
            applyFontVariant(text, context.fontVariant)
        }

        val resolvedStyle =
            if (context.fontStyle == null && context.fontVariant == FontVariant.NORMAL) {
                when {
                    transformedText.any { it.isLetter() } -> context.copy(fontStyle = FontStyle.Italic)
                    transformedText.any { it.isDigit() } -> context.copy(fontStyle = FontStyle.Normal)
                    else -> context
                }
            } else {
                context
            }

        return measureAnnotatedText(transformedText, resolvedStyle, measurer, density)
    }

    private fun measureSymbol(
        node: LatexNode.Symbol, context: RenderContext, measurer: TextMeasurer, density: Density
    ): NodeLayout {
        // 1. 尝试通过 FontResolver 获取 CM 字体映射
        //    优先用命令名查找，失败时用 unicode 字符反向查找
        val symbolInfo = FontResolver.resolveSymbol(node.symbol, context.fontFamilies)
            ?: FontResolver.resolveSymbol(node.unicode, context.fontFamilies)

        if (symbolInfo != null) {
            // 使用 CM 字体 TTF char code 渲染
            val fontFamily = FontResolver.getFontForSymbol(symbolInfo, context.fontFamilies)
            var resolvedStyle = context.copy(
                fontStyle = symbolInfo.fontStyle,
                fontFamily = fontFamily ?: context.fontFamily
            )

            if (needsLightWeight(node.symbol)) {
                resolvedStyle = resolvedStyle.copy(fontWeight = FontWeight.ExtraLight)
            }

            val layout = measureAnnotatedText(symbolInfo.texGlyph, resolvedStyle, measurer, density)

            if (isCenteredSymbol(node.symbol) || isCenteredSymbol(node.unicode)) {
                return NodeLayout(layout.width, layout.height, layout.height * MathConstants.CENTERED_SYMBOL_BASELINE, layout.draw)
            }
            return layout
        }

        // 2. 回退：无 CM 字体映射时，使用 Unicode 字符渲染（原有逻辑）
        val text = if (node.unicode.isEmpty()) node.symbol else node.unicode

        var resolvedStyle = if (context.fontStyle == null) {
            when {
                isLowercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Italic)
                isUppercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Normal)
                else -> context
            }
        } else {
            context
        }

        if (needsLightWeight(node.symbol)) {
            resolvedStyle = resolvedStyle.copy(fontWeight = FontWeight.ExtraLight)
        }

        val layout = measureAnnotatedText(text, resolvedStyle, measurer, density)

        if (isCenteredSymbol(node.symbol) || isCenteredSymbol(node.unicode)) {
            return NodeLayout(layout.width, layout.height, layout.height * MathConstants.CENTERED_SYMBOL_BASELINE, layout.draw)
        }

        return layout
    }

    private fun measureAnnotatedText(
        text: String, context: RenderContext, measurer: TextMeasurer, density: Density
    ): NodeLayout {
        val result = measurer.measure(AnnotatedString(text), context.textStyle())
        val baseWidth = result.size.width.toFloat()

        // 斜体悬伸补偿：必须使用 px 单位，不能用 sp 的数值直接加到 px 宽度上
        val fontSizePx = with(density) { context.fontSize.toPx() }

        val rightOverhang = if (context.fontStyle == FontStyle.Italic && text.isNotEmpty()) {
            val lastChar = text.last()
            when {
                lastChar.isUpperCase() -> fontSizePx * MathConstants.ITALIC_RIGHT_OVERHANG_UPPER
                lastChar.isLowerCase() -> fontSizePx * MathConstants.ITALIC_RIGHT_OVERHANG_LOWER
                else -> fontSizePx * MathConstants.ITALIC_RIGHT_OVERHANG_OTHER
            }
        } else 0f

        val leftOverhang = if (context.fontStyle == FontStyle.Italic && text.isNotEmpty()) {
            when {
                text.first() in "FTVWYfv" -> fontSizePx * MathConstants.ITALIC_LEFT_OVERHANG
                else -> 0f
            }
        } else 0f

        val totalWidth = baseWidth + leftOverhang + rightOverhang

        return NodeLayout(
            totalWidth,
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x + leftOverhang, y))
        }
    }

    /**
     * 应用字体变体的降级方案（Unicode 映射）
     *
     * 注意：这是降级方案，仅在无法加载字体时使用。
     * 正常情况下应该直接使用对应的字体系列（blackboardBold, calligraphic等）。
     */
    private fun applyFontVariant(text: String, variant: FontVariant): String {
        return when (variant) {
            FontVariant.BLACKBOARD_BOLD -> MathFontUtils.toBlackboardBold(text)
            FontVariant.CALLIGRAPHIC -> MathFontUtils.toCalligraphic(text)
            else -> text
        }
    }

    private fun isLowercaseGreek(symbol: String): Boolean {
        return symbol in setOf(
            "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa",
            "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi",
            "chi", "psi", "omega",
            "varpi", "varrho", "varsigma", "vartheta", "varphi", "varepsilon"
        )
    }

    private fun isUppercaseGreek(symbol: String): Boolean {
        return symbol in setOf(
            "Gamma",
            "Delta",
            "Theta",
            "Lambda",
            "Xi",
            "Pi",
            "Sigma",
            "Upsilon",
            "Phi",
            "Psi",
            "Omega"
        )
    }

    /**
     * 判断符号是否需要使用极细字重（FontWeight.ExtraLight）
     * 某些符号（如 ℏ, ∇, ∂）在正常字重下笔画过粗，需要使用极细字重
     */
    private fun needsLightWeight(symbol: String): Boolean {
        return symbol in setOf(
            "hbar",      // ℏ (h-bar)
            "nabla",     // ∇ (nabla)
            "partial"    // ∂ (partial derivative)
        )
    }

    private fun measureTextMode(
        text: String, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
        val textStyle = context.copy(
            fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif,
            fontWeight = context.fontWeight ?: FontWeight.Normal
        ).textStyle()
        val result = measurer.measure(AnnotatedString(text), textStyle)

        return NodeLayout(
            result.size.width.toFloat(),
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    private fun measureSpace(
        type: LatexNode.Space.SpaceType, context: RenderContext, density: Density
    ): NodeLayout {
        val width = spaceWidthPx(context, type, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }

    private fun measureHSpace(
        node: LatexNode.HSpace, context: RenderContext, density: Density
    ): NodeLayout {
        val width = parseDimension(node.dimension, context, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }
}
