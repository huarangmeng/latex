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
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max

/**
 * 可扩展箭头测量器
 *
 * 支持 \xrightarrow{文字}、\xleftarrow{文字} 等命令
 */
internal class ExtensibleArrowMeasurer : NodeMeasurer<LatexNode.ExtensibleArrow> {

    override fun measure(
        node: LatexNode.ExtensibleArrow,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val aboveStyle = context.shrink(MathConstants.STACK_SCRIPT_SCALE)
        val aboveLayout = measureGroup(listOf(node.content), aboveStyle)
        val belowLayout = node.below?.let { measureGroup(listOf(it), aboveStyle) }

        val minArrowLength = with(density) { MathConstants.EXTENSIBLE_ARROW_MIN_LENGTH_DP.dp.toPx() }
        val contentWidth = max(
            max(aboveLayout.width, belowLayout?.width ?: 0f),
            minArrowLength
        )
        val padding = with(density) { MathConstants.EXTENSIBLE_ARROW_PADDING_DP.dp.toPx() }
        val strokeWidth = with(density) { MathConstants.EXTENSIBLE_ARROW_STROKE_DP.dp.toPx() }
        val strokeHalf = strokeWidth / 2f
        val arrowHeadSize = with(density) { MathConstants.EXTENSIBLE_ARROW_HEAD_SIZE_DP.dp.toPx() }

        val totalWidth = contentWidth + padding * 2
        val arrowStrokeHeight = with(density) { MathConstants.EXTENSIBLE_ARROW_STROKE_HEIGHT_DP.dp.toPx() }
        val topGap = with(density) { MathConstants.EXTENSIBLE_ARROW_TEXT_GAP_DP.dp.toPx() }
        val bottomGap = with(density) { MathConstants.EXTENSIBLE_ARROW_TEXT_GAP_DP.dp.toPx() }

        // 相对 Y 坐标
        val aboveRelY = strokeHalf // 顶部留出 Stroke 半宽
        val arrowRelY = aboveRelY + aboveLayout.height + topGap
        val belowRelY = arrowRelY + arrowStrokeHeight + bottomGap

        val totalHeight = if (belowLayout != null) {
            belowRelY + belowLayout.height + strokeHalf
        } else {
            arrowRelY + arrowStrokeHeight + strokeHalf
        }

        val baseline = arrowRelY + arrowStrokeHeight / 2

        // 相对 X 坐标
        val aboveRelX = (totalWidth - aboveLayout.width) / 2
        val belowRelX = if (belowLayout != null) (totalWidth - belowLayout.width) / 2 else 0f

        // 箭头的相对坐标 (以 (0,0) 为原点构建)
        val arrowStartRelX = padding
        val arrowEndRelX = totalWidth - padding
        val arrowCenterRelY = arrowRelY + arrowStrokeHeight / 2
        val color = context.color

        // 预构建箭头头部 Path (以 (0,0) 为原点)
        val arrowResult = buildArrowPaths(node.direction, arrowStartRelX, arrowEndRelX, arrowCenterRelY, arrowHeadSize)

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            // 绘制上方文字
            aboveLayout.draw(this, x + aboveRelX, y + aboveRelY)

            // 绘制箭头线条 (drawLine 使用绝对坐标，这是允许的简单算术)
            drawLine(
                color = color,
                start = Offset(x + arrowStartRelX, y + arrowCenterRelY),
                end = Offset(x + arrowEndRelX, y + arrowCenterRelY),
                strokeWidth = strokeWidth
            )

            // 绘制预构建的箭头头部 Path
            if (arrowResult.filledPaths.isNotEmpty() || arrowResult.strokePaths.isNotEmpty()) {
                withTransform({ translate(left = x, top = y) }) {
                    arrowResult.filledPaths.forEach { path ->
                        drawPath(path = path, color = color)
                    }
                    arrowResult.strokePaths.forEach { path ->
                        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
                    }
                }
            }

            // 绘制下方文字
            belowLayout?.let { layout ->
                layout.draw(this, x + belowRelX, y + belowRelY)
            }
        }
    }

    /**
     * 箭头路径构建结果
     */
    private data class ArrowPaths(
        val filledPaths: List<Path>,  // 实心三角箭头
        val strokePaths: List<Path>   // 描边路径（钩子）
    )

    /**
     * 在 Measure 阶段预构建箭头头部 Path (以 (0,0) 为原点)
     */
    private fun buildArrowPaths(
        direction: LatexNode.ExtensibleArrow.Direction,
        startX: Float,
        endX: Float,
        centerY: Float,
        headSize: Float
    ): ArrowPaths {
        val filledPaths = mutableListOf<Path>()
        val strokePaths = mutableListOf<Path>()

        when (direction) {
            LatexNode.ExtensibleArrow.Direction.RIGHT -> {
                filledPaths.add(Path().apply {
                    moveTo(endX, centerY)
                    lineTo(endX - headSize, centerY - headSize / 2)
                    lineTo(endX - headSize, centerY + headSize / 2)
                    close()
                })
            }
            LatexNode.ExtensibleArrow.Direction.LEFT -> {
                filledPaths.add(Path().apply {
                    moveTo(startX, centerY)
                    lineTo(startX + headSize, centerY - headSize / 2)
                    lineTo(startX + headSize, centerY + headSize / 2)
                    close()
                })
            }
            LatexNode.ExtensibleArrow.Direction.BOTH -> {
                filledPaths.add(Path().apply {
                    moveTo(endX, centerY)
                    lineTo(endX - headSize, centerY - headSize / 2)
                    lineTo(endX - headSize, centerY + headSize / 2)
                    close()
                })
                filledPaths.add(Path().apply {
                    moveTo(startX, centerY)
                    lineTo(startX + headSize, centerY - headSize / 2)
                    lineTo(startX + headSize, centerY + headSize / 2)
                    close()
                })
            }
            LatexNode.ExtensibleArrow.Direction.HOOK_RIGHT -> {
                filledPaths.add(Path().apply {
                    moveTo(endX, centerY)
                    lineTo(endX - headSize, centerY - headSize / 2)
                    lineTo(endX - headSize, centerY + headSize / 2)
                    close()
                })
                val hookRadius = headSize * 0.6f
                strokePaths.add(Path().apply {
                    moveTo(startX, centerY)
                    cubicTo(
                        startX, centerY + hookRadius,
                        startX + hookRadius * 0.5f, centerY + hookRadius,
                        startX + hookRadius, centerY
                    )
                })
            }
            LatexNode.ExtensibleArrow.Direction.HOOK_LEFT -> {
                filledPaths.add(Path().apply {
                    moveTo(startX, centerY)
                    lineTo(startX + headSize, centerY - headSize / 2)
                    lineTo(startX + headSize, centerY + headSize / 2)
                    close()
                })
                val hookRadius = headSize * 0.6f
                strokePaths.add(Path().apply {
                    moveTo(endX, centerY)
                    cubicTo(
                        endX, centerY + hookRadius,
                        endX - hookRadius * 0.5f, centerY + hookRadius,
                        endX - hookRadius, centerY
                    )
                })
            }
        }

        return ArrowPaths(filledPaths, strokePaths)
    }
}
