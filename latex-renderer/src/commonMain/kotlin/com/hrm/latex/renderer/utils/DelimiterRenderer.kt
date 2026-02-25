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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle

/**
 * 定界符渲染共享基础设施
 *
 * 所有需要渲染定界符（括号、竖线等）的 Measurer 统一调用此对象，
 * 消除 MathMeasurer / DelimiterMeasurer / MatrixMeasurer 三处重复的
 * `measureDelimiterScaled()` 实现。
 *
 * 设计理由：
 * - 定界符渲染策略统一为"fontSize 缩放 + FontWeight 补偿"，由 FontResolver 管理字体回退。
 * - 此处不做 Path 手绘，所有定界符均使用字体字形渲染。
 */
internal object DelimiterRenderer {

    /**
     * 测量并缩放定界符至目标高度
     *
     * 策略：放大 fontSize 让字体引擎以目标字号光栅化（保证高度），
     * 然后通过水平缩放补偿笔画变粗的问题。
     *
     * CM 字体是单 weight 字体，FontWeight 调整无效，因此使用 scaleX 补偿：
     * - scale <= 1.5 → 不补偿（笔画增粗不明显）
     * - scale > 1.5  → scaleX = 1/sqrt(scale)，让笔画宽度随放大率开方衰减
     *
     * @param delimiter 定界符字符串 (如 "(", "[", "{", "|", "‖")
     * @param context 当前渲染上下文
     * @param measurer 文本测量器
     * @param targetHeight 目标高度（像素）
     * @return 包含缩放后定界符的 NodeLayout
     */
    fun measureScaled(
        delimiter: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float
    ): NodeLayout {
        // 将 Unicode 定界符转换为 CM 字体中的正确 TeX char code
        val glyph = FontResolver.resolveDelimiterGlyph(delimiter, context.fontFamilies)

        val baseLayout = measureText(glyph, FontResolver.delimiterContext(context, delimiter), measurer)
        if (baseLayout.height <= 0f || targetHeight <= 0f) {
            return baseLayout
        }

        val scale = targetHeight / baseLayout.height
        val adjustedContext = FontResolver.delimiterContext(context, delimiter, scale).copy(
            fontSize = context.fontSize * scale
        )
        val scaledLayout = measureText(glyph, adjustedContext, measurer)

        // 笔画补偿：scale 较大时水平方向压缩，让笔画看起来更细
        // CM 字体是单 weight，FontWeight 调整无效，因此用 scaleX 补偿
        if (scale > 1.5f) {
            val scaleX = 1f / kotlin.math.sqrt(scale).coerceIn(1f, 1.6f)
            val compensatedWidth = scaledLayout.width * scaleX
            return NodeLayout(
                compensatedWidth,
                scaledLayout.height,
                scaledLayout.baseline
            ) { x, y ->
                // 以原始文本中心为 pivot 做水平缩放，这样文本自动居中
                val textCenterX = x + compensatedWidth / 2f
                withTransform({
                    scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(textCenterX, y))
                }) {
                    // scaleX 以 textCenterX 为中心缩放，文本绘制起点需要
                    // 从 textCenterX 向左偏移原始宽度的一半
                    scaledLayout.draw(this, textCenterX - scaledLayout.width / 2f, y)
                }
            }
        }

        return scaledLayout
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
