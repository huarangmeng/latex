package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.Side
import com.hrm.latex.renderer.utils.drawBracket
import kotlin.math.max

/**
 * 矩阵与数组测量器
 */
internal class MatrixMeasurer : NodeMeasurer<LatexNode> {

    enum class ColumnAlignment { LEFT, CENTER, RIGHT }

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

        val bracketWidth = with(density) { (context.fontSize * 0.5f).toPx() }
        val strokeWidth = with(density) { (context.fontSize * 0.05f).toPx() }

        val width = contentLayout.width + bracketWidth * 2
        val height = contentLayout.height
        val baseline = contentLayout.baseline

        return NodeLayout(width, height, baseline) { x, y ->
            drawBracket(bracketType, Side.LEFT, x, y, bracketWidth, height, strokeWidth, context.color)
            contentLayout.draw(this, x + bracketWidth, y)
            drawBracket(bracketType, Side.RIGHT, x + width - bracketWidth, y, bracketWidth, height, strokeWidth, context.color)
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
        colSpacingRatio: Float = 0.5f,
        rowSpacingRatio: Float = 0.2f,
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
        
        val bracketWidth = with(density) { (context.fontSize * 0.5f).toPx() }
        val strokeWidth = with(density) { (context.fontSize * 0.05f).toPx() }

        return NodeLayout(bracketWidth + matrixLayout.width, matrixLayout.height, matrixLayout.baseline) { x, y ->
            drawBracket(LatexNode.Matrix.MatrixType.BRACE, Side.LEFT, x, y, bracketWidth, matrixLayout.height, strokeWidth, context.color)
            matrixLayout.draw(this, x + bracketWidth, y)
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
        val rowSpacing = with(density) { (context.fontSize * 0.3f).toPx() }
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
