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
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
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
            is LatexNode.Text -> measureText(node.content, context, measurer)
            is LatexNode.TextMode -> measureTextMode(node.text, context, measurer)
            is LatexNode.Symbol -> measureSymbol(node, context, measurer)
            is LatexNode.Operator -> {
                val operatorGap = with(density) { (context.fontSize * 0.166f).toPx() }
                val layout = measureText(
                    node.op,
                    context.copy(
                        fontStyle = FontStyle.Normal,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    ),
                    measurer
                )
                layout.copy(width = layout.width + operatorGap)
            }

            is LatexNode.Command -> measureText(node.name, context, measurer)
            is LatexNode.Space -> measureSpace(node.type, context, density)
            is LatexNode.HSpace -> measureHSpace(node, context, density)
            is LatexNode.NewLine -> NodeLayout(0f, 0f, 0f) { _, _ -> }
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    private fun measureText(
        text: String, context: RenderContext, measurer: TextMeasurer
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
            if (context.fontStyle == null && context.fontVariant == RenderContext.FontVariant.NORMAL) {
                when {
                    transformedText.any { it.isLetter() } -> context.copy(fontStyle = FontStyle.Italic)
                    transformedText.any { it.isDigit() } -> context.copy(fontStyle = FontStyle.Normal)
                    else -> context
                }
            } else {
                context
            }

        return measureAnnotatedText(transformedText, resolvedStyle, measurer)
    }

    private fun measureSymbol(
        node: LatexNode.Symbol, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
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

        // 对某些符号（如 ℏ, ∇, ∂）应用极细字重，避免笔画过粗
        if (needsLightWeight(node.symbol)) {
            resolvedStyle = resolvedStyle.copy(fontWeight = FontWeight.ExtraLight)
        }

        val layout = measureAnnotatedText(text, resolvedStyle, measurer)

        if (isCenteredSymbol(node.symbol)) {
            return layout.copy(baseline = layout.height * 0.85f)
        }

        return layout
    }

    private fun measureAnnotatedText(
        text: String, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
        val result = measurer.measure(AnnotatedString(text), context.textStyle())
        val baseWidth = result.size.width.toFloat()

        val italicCorrection = if (context.fontStyle == FontStyle.Italic && text.isNotEmpty()) {
            val lastChar = text.last()
            when {
                lastChar.isUpperCase() -> context.fontSize.value * 0.15f
                lastChar.isLowerCase() -> context.fontSize.value * 0.12f
                else -> context.fontSize.value * 0.08f
            }
        } else 0f

        return NodeLayout(
            baseWidth + italicCorrection,
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    /**
     * 应用字体变体的降级方案（Unicode 映射）
     *
     * 注意：这是降级方案，仅在无法加载字体时使用。
     * 正常情况下应该直接使用对应的字体系列（blackboardBold, calligraphic等）。
     */
    private fun applyFontVariant(text: String, variant: RenderContext.FontVariant): String {
        return when (variant) {
            RenderContext.FontVariant.BLACKBOARD_BOLD -> MathFontUtils.toBlackboardBold(text)
            RenderContext.FontVariant.CALLIGRAPHIC -> MathFontUtils.toCalligraphic(text)
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
