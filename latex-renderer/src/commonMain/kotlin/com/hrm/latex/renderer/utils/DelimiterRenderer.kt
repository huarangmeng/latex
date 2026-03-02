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

package com.hrm.latex.renderer.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.renderer.font.MathFontProvider
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle

/**
 * 定界符渲染共享基础设施
 *
 * 所有需要渲染定界符（括号、竖线等）的 Measurer 统一调用此对象，
 * 消除 DelimiterMeasurer / MatrixMeasurer 等多处重复的
 * `measureDelimiterScaled()` 实现。
 *
 * 支持两种渲染路径：
 * 1. OTF 路径：通过 MathFontProvider.verticalVariants() 获取 MATH 表中的字形变体，
 *    选择高度足够的最小变体；所有变体都不够时，对最大变体做 fontSize 缩放兜底。
 * 2. TTF 路径（KaTeX 字体方案）：Main → Size1 → Size2 → Size3 → Size4 逐级尝试。
 *    此处不做 Path 手绘，所有定界符均使用字体字形渲染。
 */
internal object DelimiterRenderer {

    /**
     * 逐级尝试的字体级别列表（TTF 路径）
     *
     * 按 KaTeX 规范：Main → Size1 → Size2 → Size3 → Size4
     */
    private data class SizeLevel(
        val name: String,
        val getFont: (com.hrm.latex.renderer.model.LatexFontFamilies) -> FontFamily
    )

    private val sizeLevels = listOf(
        SizeLevel("main") { it.main },
        SizeLevel("size1") { it.size1 },
        SizeLevel("size2") { it.size2 },
        SizeLevel("size3") { it.size3 },
        SizeLevel("size4") { it.size4 },
    )

    /**
     * 测量并缩放定界符至目标高度
     *
     * 优先使用 OTF 路径（MathFontProvider.verticalVariants()），
     * 无 MathFontProvider 或无变体时回退到 TTF 路径（KaTeX Size1~4）。
     *
     * @param delimiter 定界符字符串 (如 "(", "[", "{", "|", "‖")
     * @param context 当前渲染上下文
     * @param measurer 文本测量器
     * @param targetHeight 目标高度（像素）
     * @return 包含定界符的 NodeLayout
     */
    fun measureScaled(
        delimiter: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float
    ): NodeLayout {
        val glyph = FontResolver.resolveDelimiterGlyph(delimiter, context.fontFamilies)

        if (targetHeight <= 0f) {
            return measureText(glyph, FontResolver.delimiterContext(context, delimiter), measurer)
        }

        // 优先尝试 OTF 路径：通过 MATH 表的 verticalVariants 选择最佳字形
        val provider = context.mathFontProvider
        if (provider != null) {
            val result = measureScaledWithProvider(glyph, context, measurer, targetHeight, provider)
            if (result != null) return result
        }

        // 回退到 TTF 路径：KaTeX Size1~4 逐级尝试
        return measureScaledWithSizeLevels(glyph, context, measurer, targetHeight)
    }

    /**
     * OTF 路径：通过 MathFontProvider.verticalVariants() 选择字形变体
     *
     * @return 成功时返回 NodeLayout；无变体数据时返回 null（由调用方回退到 TTF 路径）
     */
    private fun measureScaledWithProvider(
        glyph: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float,
        provider: MathFontProvider
    ): NodeLayout? {
        val fontSizePx = context.fontSize.value  // sp value, 近似为 px
        val variants = provider.verticalVariants(glyph, fontSizePx)
        if (variants.isEmpty()) return null

        // 逐级尝试从小到大的变体，选择高度 >= targetHeight 的最小变体
        for (variant in variants) {
            val variantContext = context.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                fontFamily = variant.fontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )
            val layout = measureText(variant.glyphChar, variantContext, measurer)
            if (layout.height >= targetHeight) {
                return layout
            }
        }

        // 所有预设变体都不够高 → 对最大变体做 fontSize 缩放兜底
        val largest = variants.last()
        val largestContext = context.copy(
            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
            fontFamily = largest.fontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
        )
        val largestLayout = measureText(largest.glyphChar, largestContext, measurer)
        if (largestLayout.height <= 0f) return largestLayout

        val scale = targetHeight / largestLayout.height
        val scaledContext = largestContext.copy(
            fontSize = context.fontSize * scale
        )
        return measureText(largest.glyphChar, scaledContext, measurer)
    }

    /**
     * TTF 路径：KaTeX Size1~4 逐级尝试
     */
    private fun measureScaledWithSizeLevels(
        glyph: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float
    ): NodeLayout {
        val fontFamilies = context.fontFamilies

        if (fontFamilies == null) {
            return measureText(glyph, FontResolver.delimiterContext(context, glyph), measurer)
        }

        // 逐级尝试 Main → Size1 → Size2 → Size3 → Size4
        var bestLayout: NodeLayout? = null
        var bestContext: RenderContext? = null

        for (level in sizeLevels) {
            val font = level.getFont(fontFamilies)
            val levelContext = context.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                fontFamily = font,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )
            val layout = measureText(glyph, levelContext, measurer)

            bestLayout = layout
            bestContext = levelContext

            if (layout.height >= targetHeight) {
                return layout
            }
        }

        // 所有 Size 字体都不够高 → 对 Size4 做 fontSize 缩放
        val size4Layout = bestLayout!!
        val size4Context = bestContext!!

        if (size4Layout.height <= 0f) return size4Layout

        val scale = targetHeight / size4Layout.height
        val scaledContext = size4Context.copy(
            fontSize = context.fontSize * scale
        )
        return measureText(glyph, scaledContext, measurer)
    }

    /**
     * 以指定样式测量定界符文本
     */
    fun measureText(
        delimiter: String,
        delimiterStyle: RenderContext,
        measurer: TextMeasurer
    ): NodeLayout {
        val textStyle = delimiterStyle.textStyle()
        val result = measurer.measure(AnnotatedString(delimiter), textStyle)
        return NodeLayout(
            result.size.width.toFloat(),
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }
}
