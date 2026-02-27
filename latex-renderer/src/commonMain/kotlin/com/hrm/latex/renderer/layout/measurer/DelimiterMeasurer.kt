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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants

/**
 * 定界符测量器
 *
 * 负责测量：
 * 1. 自动伸缩的括号 (\left( ... \right))
 * 2. 手动控制大小的括号 (\big, \Big, \bigg, \Bigg)
 */
internal class DelimiterMeasurer : NodeMeasurer<LatexNode> {

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Delimited -> measureDelimited(
                node,
                context,
                measurer,
                density,
                measureGroup
            )

            is LatexNode.ManualSizedDelimiter -> measureManualSizedDelimiter(
                node,
                context,
                measurer,
                density
            )

            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    /**
     * 测量自动伸缩的定界符 (\left ... \right)
     *
     * 逻辑：
     * 1. 测量内部内容。
     * 2. 内容的高度决定了括号的高度。
     * 3. 如果括号类型支持绘制（如 () [] {}），则绘制矢量图形。
     * 4. 否则（如文本），回退到普通文本测量。
     */
    private fun measureDelimited(
        node: LatexNode.Delimited,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)

        val leftStr = node.left
        val rightStr = node.right

        // 括号高度应该略高于内容,形成包裹感
        val delimiterPadding =
            with(density) { (context.fontSize * MathConstants.DELIMITER_PADDING).toPx() }
        val delimiterHeight = contentLayout.height + delimiterPadding * 2

        val leftLayout = if (leftStr != ".") {
            DelimiterRenderer.measureScaled(leftStr, context, measurer, delimiterHeight)
        } else null

        val rightLayout = if (rightStr != ".") {
            DelimiterRenderer.measureScaled(rightStr, context, measurer, delimiterHeight)
        } else null

        val leftW = leftLayout?.width ?: 0f
        val rightW = rightLayout?.width ?: 0f

        val width = leftW + contentLayout.width + rightW
        val baseline = contentLayout.baseline + delimiterPadding

        return NodeLayout(width, delimiterHeight, baseline) { x, y ->
            var curX = x

            // 括号与内容都从 y 开始绘制
            // 括号的高度是 delimiterHeight
            // 内容的高度是 contentLayout.height
            // 内容应该在括号内垂直居中,所以内容顶部 = y + (delimiterHeight - contentLayout.height) / 2
            val contentY = y + (delimiterHeight - contentLayout.height) / 2f

            // 绘制左侧括号
            if (leftLayout != null) {
                leftLayout.draw(this, curX, y)
                curX += leftLayout.width
            }

            // 绘制内容:垂直居中
            contentLayout.draw(this, curX, contentY)
            curX += contentLayout.width

            // 绘制右侧括号
            if (rightLayout != null) {
                rightLayout.draw(this, curX, y)
            }
        }
    }

    /**
     * 测量手动大小的定界符 (\big, \Big, \bigg, \Bigg)
     *
     * KaTeX 策略：直接使用对应的 Size 字体字形，无需放大 fontSize。
     * - \big  (1.2) → Size1
     * - \Big  (1.8) → Size2
     * - \bigg (2.4) → Size3
     * - \Bigg (3.0) → Size4
     */
    private fun measureManualSizedDelimiter(
        node: LatexNode.ManualSizedDelimiter,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        val delimiter = node.delimiter
        val scaleFactor = node.size

        val glyph = FontResolver.resolveDelimiterGlyph(delimiter, context.fontFamilies)

        // 直接用对应的 Size 字体，不放大 fontSize
        val fontFamily = FontResolver.manualDelimiterFont(context.fontFamilies, scaleFactor)
            ?: context.fontFamily
        val delimiterStyle = context.copy(
            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
            fontFamily = fontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
        )

        val result = measurer.measure(AnnotatedString(glyph), delimiterStyle.textStyle())

        // 括号应该相对于数学轴居中
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val height = result.size.height.toFloat()
        val baseline = height / 2f + axisHeight

        return NodeLayout(
            result.size.width.toFloat(),
            height,
            baseline
        ) { x, y ->
            drawText(result, topLeft = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}
