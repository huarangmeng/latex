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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.model.toFractionChildStyle
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max

/**
 * 分数测量器 — 处理 \frac{num}{den}
 *
 * 布局模型：
 * ```
 *   ┌─── numerator (居中) ───┐
 *   │         gap             │
 *   ├───── fraction line ─────┤  ← baseline = lineY + ruleThickness/2 + axisHeight
 *   │         gap             │
 *   └─── denominator (居中) ──┘
 * ```
 *
 * 使用 MathStyle 状态机决定子式字号：
 * - DISPLAY → TEXT (子式)
 * - TEXT → SCRIPT
 * - SCRIPT → SCRIPT_SCRIPT
 */
internal class FractionMeasurer : NodeMeasurer<LatexNode.Fraction> {

    override fun measure(
        node: LatexNode.Fraction,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val childStyle = context.toFractionChildStyle()
        val numeratorLayout = measureGroup(listOf(node.numerator), childStyle)
        val denominatorLayout = measureGroup(listOf(node.denominator), childStyle)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val ruleThickness = fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
        val gap = fontSizePx * MathConstants.FRACTION_GAP
        val inset = fontSizePx * MathConstants.FRACTION_RULE_INSET

        val width = max(numeratorLayout.width, denominatorLayout.width) + gap
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)

        val numeratorTop = 0f
        val lineY = numeratorTop + numeratorLayout.height + gap
        val denominatorTop = lineY + ruleThickness + gap
        val height = denominatorTop + denominatorLayout.height
        val baseline = (lineY + ruleThickness / 2f) + axisHeight

        return NodeLayout(width, height, baseline) { x, y ->
            val numeratorX = x + (width - numeratorLayout.width) / 2
            numeratorLayout.draw(this, numeratorX, y + numeratorTop)

            drawLine(
                color = context.color,
                start = Offset(x + inset, y + lineY + ruleThickness / 2),
                end = Offset(x + width - inset, y + lineY + ruleThickness / 2),
                strokeWidth = ruleThickness
            )

            val denominatorX = x + (width - denominatorLayout.width) / 2
            denominatorLayout.draw(this, denominatorX, y + denominatorTop)
        }
    }
}
