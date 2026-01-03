package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderStyle
import com.hrm.latex.renderer.model.shrink
import kotlin.math.max

/**
 * 可扩展箭头测量器
 *
 * 支持 \xrightarrow{文字}、\xleftarrow{文字} 等命令
 */
internal class ExtensibleArrowMeasurer {

    fun measure(
        node: LatexNode.ExtensibleArrow,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        // 测量上方文字（必选）- 包装成List确保作为整体渲染
        val aboveStyle = style.shrink(0.7f)
        val aboveLayout = measureGroup(listOf(node.content), aboveStyle)

        // 测量下方文字（可选）- 包装成List确保作为整体渲染
        val belowLayout = node.below?.let { measureGroup(listOf(it), aboveStyle) }

        // 箭头的最小长度
        val minArrowLength = with(density) { 30.dp.toPx() }

        // 总宽度 = max(上方文字宽度, 下方文字宽度, 最小长度) + 两侧留白
        val contentWidth = max(
            max(aboveLayout.width, belowLayout?.width ?: 0f),
            minArrowLength
        )
        val padding = with(density) { 4.dp.toPx() }
        val totalWidth = contentWidth + padding * 2

        // 箭头线条的粗细和间距
        val arrowStrokeHeight = with(density) { 2.dp.toPx() }
        val topGap = with(density) { 2.dp.toPx() }
        val bottomGap = with(density) { 2.dp.toPx() }

        // 计算各部分的 Y 坐标
        val aboveY = 0f
        val arrowY = aboveLayout.height + topGap
        val belowY = arrowY + arrowStrokeHeight + bottomGap

        // 总高度
        val totalHeight = if (belowLayout != null) {
            aboveLayout.height + topGap + arrowStrokeHeight + bottomGap + belowLayout.height
        } else {
            aboveLayout.height + topGap + arrowStrokeHeight
        }

        // 基线位置（以箭头为基准，向上偏移一点使其与周围文本对齐）
        val baseline = arrowY + arrowStrokeHeight / 2

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            // 绘制上方文字（居中）
            val aboveX = x + (totalWidth - aboveLayout.width) / 2
            aboveLayout.draw(this, aboveX, y + aboveY)

            // 绘制箭头
            val arrowStartX = x + padding
            val arrowEndX = x + totalWidth - padding
            drawArrow(
                direction = node.direction,
                startX = arrowStartX,
                endX = arrowEndX,
                centerY = y + arrowY + arrowStrokeHeight / 2,
                color = style.color,
                density = density
            )

            // 绘制下方文字（如果有，居中）
            belowLayout?.let { layout ->
                val belowX = x + (totalWidth - layout.width) / 2
                layout.draw(this, belowX, y + belowY)
            }
        }
    }

    /**
     * 绘制箭头
     */
    private fun DrawScope.drawArrow(
        direction: LatexNode.ExtensibleArrow.Direction,
        startX: Float,
        endX: Float,
        centerY: Float,
        color: Color,
        density: Density
    ) {
        val strokeWidth = with(density) { 1.5f.dp.toPx() }
        val arrowHeadSize = with(density) { 5.dp.toPx() }

        when (direction) {
            LatexNode.ExtensibleArrow.Direction.RIGHT -> {
                // 绘制线条
                drawLine(
                    color = color,
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = strokeWidth
                )

                // 绘制右箭头头部
                val path = Path().apply {
                    moveTo(endX, centerY)
                    lineTo(endX - arrowHeadSize, centerY - arrowHeadSize / 2)
                    lineTo(endX - arrowHeadSize, centerY + arrowHeadSize / 2)
                    close()
                }
                drawPath(path = path, color = color)
            }

            LatexNode.ExtensibleArrow.Direction.LEFT -> {
                // 绘制线条
                drawLine(
                    color = color,
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = strokeWidth
                )

                // 绘制左箭头头部
                val path = Path().apply {
                    moveTo(startX, centerY)
                    lineTo(startX + arrowHeadSize, centerY - arrowHeadSize / 2)
                    lineTo(startX + arrowHeadSize, centerY + arrowHeadSize / 2)
                    close()
                }
                drawPath(path = path, color = color)
            }

            LatexNode.ExtensibleArrow.Direction.BOTH -> {
                // 绘制线条
                drawLine(
                    color = color,
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = strokeWidth
                )

                // 绘制左箭头头部
                val leftPath = Path().apply {
                    moveTo(startX, centerY)
                    lineTo(startX + arrowHeadSize, centerY - arrowHeadSize / 2)
                    lineTo(startX + arrowHeadSize, centerY + arrowHeadSize / 2)
                    close()
                }
                drawPath(path = leftPath, color = color)

                // 绘制右箭头头部
                val rightPath = Path().apply {
                    moveTo(endX, centerY)
                    lineTo(endX - arrowHeadSize, centerY - arrowHeadSize / 2)
                    lineTo(endX - arrowHeadSize, centerY + arrowHeadSize / 2)
                    close()
                }
                drawPath(path = rightPath, color = color)
            }
        }
    }
}
