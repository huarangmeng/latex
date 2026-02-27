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
 * 设计理由（KaTeX 字体方案）：
 * - KaTeX 提供 Size1~4 四套独立设计的定界符字体，每套字形大小不同但笔画粗细一致。
 * - 自动伸缩策略：Main → Size1 → Size2 → Size3 → Size4 逐级尝试，选能包含内容的最小字形。
 * - 若 Size4 仍不够高，则对 Size4 字形进行 fontSize 微调缩放。
 * - 此处不做 Path 手绘，所有定界符均使用字体字形渲染。
 */
internal object DelimiterRenderer {

    /**
     * 逐级尝试的字体级别列表
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
     * KaTeX 策略：
     * 1. 从 Main → Size1 → Size2 → Size3 → Size4 逐级测量
     * 2. 选择高度 >= targetHeight 的最小尺寸字形
     * 3. 如果 Size4 仍不够高，对 Size4 做 fontSize 等比缩放
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
        val fontFamilies = context.fontFamilies

        if (fontFamilies == null || targetHeight <= 0f) {
            return measureText(glyph, FontResolver.delimiterContext(context, delimiter), measurer)
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

            // 记录每一级的结果，最后选择合适的
            bestLayout = layout
            bestContext = levelContext

            // 如果该级字形已经足够高，直接使用
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
