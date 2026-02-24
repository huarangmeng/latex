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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max

/**
 * 根号测量器 — 处理 \sqrt[index]{content}
 *
 * 布局模型：
 * ```
 *   [index]  ┌──── horizontal bar ────┐
 *         \  │      gap               │
 *          V │   ┌─ content ─┐        │
 *            │   └───────────┘        │
 * ```
 *
 * Path 以 (0,0) 为原点构建，draw 时通过 translate 偏移。
 * NodeLayout 尺寸包含 Stroke 半宽。
 */
internal class RootMeasurer : NodeMeasurer<LatexNode.Root> {

    override fun measure(
        node: LatexNode.Root,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val indexStyle = context.shrink(MathConstants.RADICAL_INDEX_SCALE)

        val contentLayout = measureGroup(listOf(node.content), context)
        val indexLayout = node.index?.let { measureGroup(listOf(it), indexStyle) }

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val ruleThickness = fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
        val strokeHalf = ruleThickness / 2f
        val gap = ruleThickness * MathConstants.RADICAL_TOP_GAP_MULTIPLIER
        val extraTop = gap + ruleThickness

        val hookWidth = fontSizePx * MathConstants.RADICAL_HOOK_WIDTH

        val indexWidth = indexLayout?.width ?: 0f
        val contentX = max(hookWidth, indexWidth) + ruleThickness
        val totalHeight = contentLayout.height + extraTop + strokeHalf
        val baseline = contentLayout.baseline + extraTop
        val width = contentX + contentLayout.width + ruleThickness + strokeHalf

        val topY0 = ruleThickness / 2f
        val bottomY0 = totalHeight - ruleThickness - strokeHalf
        val midY0 = totalHeight * 0.5f
        val sqrtPath = Path().apply {
            moveTo(contentX, topY0)
            lineTo(contentX - hookWidth * 0.4f, bottomY0)
            lineTo(contentX - hookWidth * 0.8f, midY0 + ruleThickness)
            lineTo(contentX - hookWidth, midY0 + ruleThickness * 2)
        }

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            if (indexLayout != null) {
                val indexY = y + totalHeight * MathConstants.RADICAL_INDEX_OFFSET - indexLayout.height
                indexLayout.draw(this, x, indexY)
            }

            contentLayout.draw(this, x + contentX, y + extraTop)

            drawLine(
                context.color,
                Offset(x + contentX, y + topY0),
                Offset(x + width - strokeHalf, y + topY0),
                ruleThickness
            )

            withTransform({ translate(left = x, top = y) }) {
                drawPath(sqrtPath, context.color, style = Stroke(ruleThickness))
            }
        }
    }
}
