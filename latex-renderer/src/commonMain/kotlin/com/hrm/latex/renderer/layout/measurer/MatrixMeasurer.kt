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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max

/**
 * 矩阵与数组测量器
 */
internal class MatrixMeasurer : NodeMeasurer<LatexNode> {

    enum class ColumnAlignment { LEFT, CENTER, RIGHT }

    /**
     * 将 MatrixType 映射到对应的 Unicode 括号字符
     */
    private fun getDelimiterChar(type: LatexNode.Matrix.MatrixType, isLeft: Boolean): String {
        return when (type) {
            LatexNode.Matrix.MatrixType.PAREN -> if (isLeft) "(" else ")"
            LatexNode.Matrix.MatrixType.BRACKET -> if (isLeft) "[" else "]"
            LatexNode.Matrix.MatrixType.BRACE -> if (isLeft) "{" else "}"
            LatexNode.Matrix.MatrixType.VBAR -> "|"
            LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> "‖"
            LatexNode.Matrix.MatrixType.PLAIN -> ""
        }
    }

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Matrix -> measureMatrix(node, context, measurer, density, measureGlobal)
            is LatexNode.Array -> measureMatrixLike(node.rows, context, measurer, density, measureGlobal)
            is LatexNode.Cases -> measureCases(node, context, measurer, density, measureGlobal)
            is LatexNode.Aligned, is LatexNode.Split -> {
                val rows = if (node is LatexNode.Aligned) node.rows else (node as LatexNode.Split).rows
                val alignments = List(10) { if (it % 2 == 0) ColumnAlignment.RIGHT else ColumnAlignment.LEFT }
                measureMatrixLike(rows, context, measurer, density, measureGlobal, alignments = alignments)
            }
            is LatexNode.Eqnarray -> {
                val alignments = listOf(ColumnAlignment.RIGHT, ColumnAlignment.CENTER, ColumnAlignment.LEFT)
                measureMatrixLike(node.rows, context, measurer, density, measureGlobal, alignments = alignments)
            }
            is LatexNode.Multline -> measureMultline(node, context, density, measureGlobal)
            is LatexNode.Subequations -> measureGroup(node.content, context)
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    private fun measureMatrix(
        node: LatexNode.Matrix,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureMatrixLike(node.rows, context, measurer, density, measureGlobal)
        val bracketType = node.type
        if (bracketType == LatexNode.Matrix.MatrixType.PLAIN) return contentLayout

        // 括号高度应该略高于内容,形成包裹感
        val delimiterPadding = with(density) { (context.fontSize * MathConstants.DELIMITER_PADDING).toPx() }
        val delimiterHeight = contentLayout.height + delimiterPadding * 2

        // 使用字体渲染括号（而不是 Path）
        val leftChar = getDelimiterChar(bracketType, isLeft = true)
        val rightChar = getDelimiterChar(bracketType, isLeft = false)

        val leftLayout = if (leftChar.isNotEmpty()) {
            DelimiterRenderer.measureScaled(leftChar, context, measurer, delimiterHeight)
        } else null

        val rightLayout = if (rightChar.isNotEmpty()) {
            DelimiterRenderer.measureScaled(rightChar, context, measurer, delimiterHeight)
        } else null

        val leftW = leftLayout?.width ?: 0f
        val rightW = rightLayout?.width ?: 0f

        val width = leftW + contentLayout.width + rightW
        val height = delimiterHeight
        val baseline = contentLayout.baseline + delimiterPadding

        return NodeLayout(width, height, baseline) { x, y ->
            var curX = x

            // 内容在括号内垂直居中
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
     * 通用网格测量逻辑
     */
    private fun measureMatrixLike(
        rows: List<List<LatexNode>>,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        alignments: List<ColumnAlignment>? = null,
        colSpacingRatio: Float = MathConstants.MATRIX_COLUMN_SPACING,
        rowSpacingRatio: Float = MathConstants.MATRIX_ROW_SPACING,
        isBaselineFirstRow: Boolean = false
    ): NodeLayout {
        val measuredRows = rows.map { row -> row.map { measureGlobal(it, context) } }
        val colCount = measuredRows.maxOfOrNull { it.size } ?: 0
        val rowCount = measuredRows.size
        if (rowCount == 0) return NodeLayout(0f, 0f, 0f) { _, _ -> }

        val colWidths = FloatArray(colCount)
        val rowHeights = FloatArray(rowCount)
        val rowBaselines = FloatArray(rowCount)

        for (c in 0 until colCount) {
            colWidths[c] = measuredRows.maxOfOrNull { if (c < it.size) it[c].width else 0f } ?: 0f
        }

        for (r in 0 until rowCount) {
            var maxAscent = 0f
            var maxDescent = 0f
            measuredRows[r].forEach { cell ->
                maxAscent = max(maxAscent, cell.baseline)
                maxDescent = max(maxDescent, cell.height - cell.baseline)
            }
            rowHeights[r] = maxAscent + maxDescent
            rowBaselines[r] = maxAscent
        }

        val colSpacing = with(density) { (context.fontSize * colSpacingRatio).toPx() }
        val rowSpacing = with(density) { (context.fontSize * rowSpacingRatio).toPx() }

        val totalWidth = colWidths.sum() + colSpacing * max(0, colCount - 1)
        val totalHeight = rowHeights.sum() + rowSpacing * max(0, rowCount - 1)

        val baseline = if (isBaselineFirstRow) {
            rowBaselines[0]
        } else {
            val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
            totalHeight / 2 + axisHeight
        }

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            var currentY = y
            for (r in 0 until rowCount) {
                var currentX = x
                val rowBaseY = currentY + rowBaselines[r]
                for (c in 0 until measuredRows[r].size) {
                    val cell = measuredRows[r][c]
                    val alignment = alignments?.getOrNull(c) ?: ColumnAlignment.CENTER
                    val cellX = when (alignment) {
                        ColumnAlignment.LEFT -> currentX
                        ColumnAlignment.CENTER -> currentX + (colWidths[c] - cell.width) / 2
                        ColumnAlignment.RIGHT -> currentX + colWidths[c] - cell.width
                    }
                    cell.draw(this, cellX, rowBaseY - cell.baseline)
                    currentX += colWidths[c] + colSpacing
                }
                currentY += rowHeights[r] + rowSpacing
            }
        }
    }

    private fun measureCases(
        node: LatexNode.Cases,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val rows = node.cases.map { listOf(it.second, LatexNode.Text(" if "), it.first) }
        val matrixLayout = measureMatrixLike(rows, context, measurer, density, measureGlobal, 
            alignments = listOf(ColumnAlignment.LEFT, ColumnAlignment.CENTER, ColumnAlignment.LEFT))
        
        // 括号高度应该略高于内容,形成包裹感
        val delimiterPadding = with(density) { (context.fontSize * MathConstants.DELIMITER_PADDING).toPx() }
        val delimiterHeight = matrixLayout.height + delimiterPadding * 2
        
        // 使用字体渲染大括号（而不是 Path）
        val leftChar = getDelimiterChar(LatexNode.Matrix.MatrixType.BRACE, isLeft = true)
        val leftLayout = DelimiterRenderer.measureScaled(leftChar, context, measurer, delimiterHeight)

        val width = leftLayout.width + matrixLayout.width
        val height = delimiterHeight
        val baseline = matrixLayout.baseline + delimiterPadding

        return NodeLayout(width, height, baseline) { x, y ->
            // 内容在括号内垂直居中
            val contentY = y + (delimiterHeight - matrixLayout.height) / 2f

            // 绘制左侧大括号
            leftLayout.draw(this, x, y)
            
            // 绘制内容:垂直居中
            matrixLayout.draw(this, x + leftLayout.width, contentY)
        }
    }

    private fun measureMultline(
        node: LatexNode.Multline,
        context: RenderContext,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val lineLayouts = node.lines.map { measureGlobal(it, context) }
        if (lineLayouts.isEmpty()) return NodeLayout(0f, 0f, 0f) { _, _ -> }

        val maxWidth = lineLayouts.maxOf { it.width }
        val rowSpacing = with(density) { (context.fontSize * MathConstants.MULTLINE_ROW_SPACING).toPx() }
        val totalHeight = lineLayouts.sumOf { it.height.toDouble() }.toFloat() + rowSpacing * (lineLayouts.size - 1)
        val baseline = lineLayouts.first().baseline

        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            var currentY = y
            lineLayouts.forEachIndexed { i, layout ->
                val offsetX = when {
                    i == 0 -> 0f
                    i == lineLayouts.lastIndex -> maxWidth - layout.width
                    else -> (maxWidth - layout.width) / 2
                }
                layout.draw(this, x + offsetX, currentY)
                currentY += layout.height + rowSpacing
            }
        }
    }
}
