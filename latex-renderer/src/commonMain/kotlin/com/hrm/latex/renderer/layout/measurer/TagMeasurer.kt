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
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import kotlin.reflect.KClass

/**
 * 标签测量器 — 处理 \tag{label}, \tag*{label}
 */
internal class TagMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Tag::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Tag
        val labelLayout = measureNode(node.label, context)
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val gap = fontSizePx * 1.5f

        if (node.starred) {
            val totalWidth = gap + labelLayout.width
            return NodeLayout(totalWidth, labelLayout.height, labelLayout.baseline) { x, y ->
                labelLayout.draw(this, x + gap, y)
            }
        } else {
            val parenStyle = context.textStyle()
            val leftParen = measurer.measure(AnnotatedString("("), parenStyle)
            val rightParen = measurer.measure(AnnotatedString(")"), parenStyle)

            val leftW = leftParen.size.width.toFloat()
            val rightW = rightParen.size.width.toFloat()
            val totalWidth = gap + leftW + labelLayout.width + rightW
            val height = labelLayout.height
            val baseline = labelLayout.baseline

            return NodeLayout(totalWidth, height, baseline) { x, y ->
                drawText(leftParen, topLeft = Offset(x + gap, y))
                labelLayout.draw(this, x + gap + leftW, y)
                drawText(rightParen, topLeft = Offset(x + gap + leftW + labelLayout.width, y))
            }
        }
    }
}
