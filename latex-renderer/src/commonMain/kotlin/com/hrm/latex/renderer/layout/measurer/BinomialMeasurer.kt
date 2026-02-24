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

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max

/**
 * 二项式测量器 — 处理 \binom{n}{k}
 *
 * 布局类似分数，但无横线，左右包裹圆括号。
 * 使用 DelimiterRenderer 统一渲染括号。
 */
internal class BinomialMeasurer : NodeMeasurer<LatexNode.Binomial> {

    override fun measure(
        node: LatexNode.Binomial,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val childStyle = context.shrink(MathConstants.BINOMIAL_CHILD_SCALE)
        val numLayout = measureGroup(listOf(node.top), childStyle)
        val denLayout = measureGroup(listOf(node.bottom), childStyle)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val gap = fontSizePx * MathConstants.BINOMIAL_GAP
        val contentWidth = max(numLayout.width, denLayout.width)
        val contentHeight = numLayout.height + denLayout.height + gap

        val delimiterPadding = fontSizePx * MathConstants.DELIMITER_PADDING
        val delimiterHeight = contentHeight + delimiterPadding * 2

        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val center = numLayout.height + gap / 2f + delimiterPadding
        val baseline = center + axisHeight

        val leftLayout = DelimiterRenderer.measureScaled("(", context, measurer, delimiterHeight)
        val rightLayout = DelimiterRenderer.measureScaled(")", context, measurer, delimiterHeight)

        val width = leftLayout.width + contentWidth + rightLayout.width

        return NodeLayout(width, delimiterHeight, baseline) { x, y ->
            val contentY = y + (delimiterHeight - contentHeight) / 2f

            leftLayout.draw(this, x, y)

            val numX = x + leftLayout.width + (contentWidth - numLayout.width) / 2
            val denX = x + leftLayout.width + (contentWidth - denLayout.width) / 2
            numLayout.draw(this, numX, contentY)
            denLayout.draw(this, denX, contentY + numLayout.height + gap)

            rightLayout.draw(this, x + leftLayout.width + contentWidth, y)
        }
    }
}
