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
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.Side
import com.hrm.latex.renderer.utils.drawBracket

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
            is LatexNode.Delimited -> measureDelimited(node, context, measurer, density, measureGroup)
            is LatexNode.ManualSizedDelimiter -> measureManualSizedDelimiter(node, context, measurer, density)
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

        fun getBracketType(str: String): LatexNode.Matrix.MatrixType? = when (str) {
            "(", ")" -> LatexNode.Matrix.MatrixType.PAREN
            "[", "]" -> LatexNode.Matrix.MatrixType.BRACKET
            "{", "}" -> LatexNode.Matrix.MatrixType.BRACE
            "|", "|" -> LatexNode.Matrix.MatrixType.VBAR
            "||" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
            else -> null
        }

        val leftType = getBracketType(leftStr)
        val rightType = getBracketType(rightStr)

        // 测量左侧文本（如果不是绘制类型且不是忽略符 .）
        val leftLayout = if (leftType == null && leftStr != ".") {
            val textStyle = context.textStyle()
            val result = measurer.measure(AnnotatedString(leftStr), textStyle)
            NodeLayout(result.size.width.toFloat(), result.size.height.toFloat(), result.firstBaseline) { x, y ->
                drawText(result, topLeft = androidx.compose.ui.geometry.Offset(x, y))
            }
        } else null

        // 测量右侧文本
        val rightLayout = if (rightType == null && rightStr != ".") {
             val textStyle = context.textStyle()
            val result = measurer.measure(AnnotatedString(rightStr), textStyle)
            NodeLayout(result.size.width.toFloat(), result.size.height.toFloat(), result.firstBaseline) { x, y ->
                drawText(result, topLeft = androidx.compose.ui.geometry.Offset(x, y))
            }
        } else null

        val bracketWidth = with(density) { (context.fontSize * 0.4f).toPx() }
        val strokeWidth = with(density) { (context.fontSize * 0.05f).toPx() }

        val leftW = leftLayout?.width ?: if (leftStr != ".") bracketWidth else 0f
        val rightW = rightLayout?.width ?: if (rightStr != ".") bracketWidth else 0f

        val width = leftW + contentLayout.width + rightW
        val height = contentLayout.height
        val baseline = contentLayout.baseline

        return NodeLayout(width, height, baseline) { x, y ->
            var curX = x

            // 绘制左侧
            if (leftLayout != null) {
                leftLayout.draw(this, curX, y + baseline - leftLayout.baseline)
                curX += leftLayout.width
            } else if (leftType != null) {
                drawBracket(leftType, Side.LEFT, curX, y, leftW, height, strokeWidth, context.color)
                curX += leftW
            }

            // 绘制内容
            contentLayout.draw(this, curX, y)
            curX += contentLayout.width

            // 绘制右侧
            if (rightLayout != null) {
                rightLayout.draw(this, curX, y + baseline - rightLayout.baseline)
            } else if (rightType != null) {
                drawBracket(rightType, Side.RIGHT, curX, y, rightW, height, strokeWidth, context.color)
            }
        }
    }

    /**
     * 测量手动大小的定界符 (\big, \Big 等)
     *
     * 这些定界符作为独立的符号存在，不包裹内容。
     */
    private fun measureManualSizedDelimiter(
        node: LatexNode.ManualSizedDelimiter,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        val delimiter = node.delimiter
        val scaleFactor = node.size

        val bracketType = when (delimiter) {
            "(", ")" -> LatexNode.Matrix.MatrixType.PAREN
            "[", "]" -> LatexNode.Matrix.MatrixType.BRACKET
            "{", "}" -> LatexNode.Matrix.MatrixType.BRACE
            "|" -> LatexNode.Matrix.MatrixType.VBAR
            "||" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
            else -> null
        }

        val baseHeight = with(density) { (context.fontSize * 1.2f).toPx() }
        val height = baseHeight * scaleFactor
        val bracketWidth = with(density) { (context.fontSize * 0.4f).toPx() }
        val strokeWidth = with(density) { (context.fontSize * 0.05f).toPx() }

        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val baseline = height / 2 + axisHeight

        return if (bracketType != null) {
            val side = if (delimiter in listOf("(", "[", "{")) Side.LEFT else Side.RIGHT
            NodeLayout(bracketWidth, height, baseline) { x, y ->
                drawBracket(bracketType, side, x, y, bracketWidth, height, strokeWidth, context.color)
            }
        } else {
            // 如果不是标准括号，按放大后的文本绘制
            val delimiterStyle = context.grow(scaleFactor)
            val textStyle = delimiterStyle.textStyle()
            val result = measurer.measure(AnnotatedString(delimiter), textStyle)
            NodeLayout(result.size.width.toFloat(), result.size.height.toFloat(), result.firstBaseline) { x, y ->
                drawText(result, topLeft = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
    }
}
