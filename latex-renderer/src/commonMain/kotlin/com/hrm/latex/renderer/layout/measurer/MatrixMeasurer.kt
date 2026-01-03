package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderStyle
import com.hrm.latex.renderer.utils.Side
import com.hrm.latex.renderer.utils.drawBracket
import kotlin.math.max

/**
 * 矩阵与数组测量器
 *
 * 负责测量表格类结构，包括：
 * - 矩阵 (matrix, pmatrix, bmatrix 等)
 * - 数组 (array)
 * - 对齐环境 (aligned)
 * - 分段函数 (cases)
 */
internal class MatrixMeasurer : NodeMeasurer<LatexNode> {

    override fun measure(
        node: LatexNode,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Matrix -> measureMatrix(node, style, measurer, density, measureGlobal)
            is LatexNode.Array -> measureMatrixLike(node.rows, style, measurer, density, measureGlobal)
            is LatexNode.Cases -> measureCases(node, style, measurer, density, measureGlobal)
            is LatexNode.Aligned -> measureMatrixLike(node.rows, style, measurer, density, measureGlobal)
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    /**
     * 测量矩阵
     *
     * 1. 测量内部网格内容。
     * 2. 根据矩阵类型添加对应的定界符（圆括号、方括号等）。
     */
    private fun measureMatrix(
        node: LatexNode.Matrix,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureMatrixLike(node.rows, style, measurer, density, measureGlobal)
        val bracketType = node.type
        if (bracketType == LatexNode.Matrix.MatrixType.PLAIN) return contentLayout

        val bracketWidth = with(density) { (style.fontSize * 0.5f).toPx() }
        val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }

        val width = contentLayout.width + bracketWidth * 2
        val height = contentLayout.height
        val baseline = contentLayout.baseline

        return NodeLayout(width, height, baseline) { x, y ->
            drawBracket(bracketType, Side.LEFT, x, y, bracketWidth, height, strokeWidth, style.color)
            contentLayout.draw(this, x + bracketWidth, y)
            drawBracket(bracketType, Side.RIGHT, x + width - bracketWidth, y, bracketWidth, height, strokeWidth, style.color)
        }
    }

    /**
     * 通用网格测量逻辑 (Array/Matrix 核心)
     *
     * 算法：
     * 1. 测量所有单元格。
     * 2. 计算每列的最大宽度。
     * 3. 计算每行的最大高度（基线上和基线下）。
     * 4. 计算总宽高达，并进行网格布局。
     */
    private fun measureMatrixLike(
        rows: List<List<LatexNode>>,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout
    ): NodeLayout {
        val measuredRows = rows.map { row ->
            row.map { node -> measureGlobal(node, style) }
        }

        val colCount = measuredRows.maxOfOrNull { it.size } ?: 0
        val rowCount = measuredRows.size

        val colWidths = FloatArray(colCount)
        val rowHeights = FloatArray(rowCount)
        val rowBaselines = FloatArray(rowCount)

        // 计算列宽
        for (c in 0 until colCount) {
            var maxW = 0f
            for (r in 0 until rowCount) {
                if (c < measuredRows[r].size) {
                    maxW = max(maxW, measuredRows[r][c].width)
                }
            }
            colWidths[c] = maxW
        }

        // 计算行高和基线
        for (r in 0 until rowCount) {
            var maxAscent = 0f
            var maxDescent = 0f
            for (c in 0 until measuredRows[r].size) {
                val cell = measuredRows[r][c]
                maxAscent = max(maxAscent, cell.baseline)
                maxDescent = max(maxDescent, cell.height - cell.baseline)
            }
            rowHeights[r] = maxAscent + maxDescent
            rowBaselines[r] = maxAscent
        }

        val colSpacing = with(density) { (style.fontSize * 0.5f).toPx() }
        val rowSpacing = with(density) { (style.fontSize * 0.2f).toPx() }

        val totalWidth = colWidths.sum() + colSpacing * max(0, colCount - 1)
        val totalHeight = rowHeights.sum() + rowSpacing * max(0, rowCount - 1)

        val axisHeight = with(density) { (style.fontSize * 0.25f).toPx() }
        val baseline = totalHeight / 2 + axisHeight

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            var currentY = y
            for (r in 0 until rowCount) {
                var currentX = x
                val rowBaseY = currentY + rowBaselines[r]

                for (c in 0 until measuredRows[r].size) {
                    val cell = measuredRows[r][c]
                    // 单元格居中对齐
                    val cellX = currentX + (colWidths[c] - cell.width) / 2
                    val cellY = rowBaseY - cell.baseline

                    cell.draw(this, cellX, cellY)
                    currentX += colWidths[c] + colSpacing
                }
                currentY += rowHeights[r] + rowSpacing
            }
        }
    }

    /**
     * 测量分段函数 (cases)
     *
     * 类似于一个两列的表格，但左侧有大括号，且默认左对齐。
     */
    private fun measureCases(
        node: LatexNode.Cases,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout
    ): NodeLayout {
        // 将 cases 转换为 "表达式 if 条件" 的三列结构或两列结构
        val rows = node.cases.map { (cond, expr) ->
            listOf(expr, LatexNode.Text(" if "), cond)
        }

        val matrixLayout = measureMatrixLike(rows, style, measurer, density, measureGlobal)
        val bracketWidth = with(density) { (style.fontSize * 0.5f).toPx() }
        val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }

        val width = bracketWidth + matrixLayout.width
        val height = matrixLayout.height
        val baseline = matrixLayout.baseline

        return NodeLayout(width, height, baseline) { x, y ->
            // 绘制左大括号
            drawBracket(
                LatexNode.Matrix.MatrixType.BRACE,
                Side.LEFT,
                x,
                y,
                bracketWidth,
                height,
                strokeWidth,
                style.color
            )
            matrixLayout.draw(this, x + bracketWidth, y)
        }
    }
}
