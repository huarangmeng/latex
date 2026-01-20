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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Accent.AccentType
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import kotlin.math.max
import kotlin.math.min

/**
 * 装饰符号测量器
 *
 * 负责测量重音符号（如 \hat, \vec）和可伸缩的宽装饰（如 \widehat, \overline, \underbrace）。
 */
internal class AccentMeasurer : NodeMeasurer<LatexNode.Accent> {

    override fun measure(
        node: LatexNode.Accent,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(listOf(node.content), context)

        // 判断是否是宽装饰（需要横向拉伸）
        val isWideAccent = when (node.accentType) {
            AccentType.WIDEHAT, AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW,
            AccentType.OVERLINE, AccentType.UNDERLINE,
            AccentType.OVERBRACE, AccentType.UNDERBRACE, AccentType.CANCEL -> true
            else -> false
        }

        if (isWideAccent) {
            return measureWideAccent(node, contentLayout, context, density)
        }

        // 普通字符装饰
        val accentChar = when (node.accentType) {
            AccentType.HAT -> "^"
            AccentType.TILDE -> "~"
            AccentType.BAR -> "¯"
            AccentType.VEC -> "→"
            AccentType.DOT -> "˙"
            AccentType.DDOT -> "¨"
            else -> ""
        }

        val isUnder = node.accentType == AccentType.UNDERLINE || node.accentType == AccentType.UNDERBRACE
        val accentStyle = context.shrink(0.8f)
        val textStyle = accentStyle.textStyle()
        val result = measurer.measure(AnnotatedString(accentChar), textStyle)
        
        val accentLayout = NodeLayout(
            result.size.width.toFloat(),
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }

        val width = max(contentLayout.width, accentLayout.width)
        val totalHeight = contentLayout.height + accentLayout.height

        return NodeLayout(
            width, totalHeight, contentLayout.baseline + (if (isUnder) 0f else accentLayout.height)
        ) { x, y ->
            val centerX = x + width / 2
            val contentX = centerX - contentLayout.width / 2
            val accentX = centerX - accentLayout.width / 2

            if (isUnder) {
                contentLayout.draw(this, contentX, y)
                accentLayout.draw(this, accentX, y + contentLayout.height)
            } else {
                accentLayout.draw(this, accentX, y)
                contentLayout.draw(this, contentX, y + accentLayout.height)
            }
        }
    }

    /**
     * 测量宽装饰（自绘图形）
     *
     * 根据内容宽度，动态绘制横线、大括号、箭头或宽帽子。
     */
    private fun measureWideAccent(
        node: LatexNode.Accent,
        contentLayout: NodeLayout,
        context: RenderContext,
        density: Density
    ): NodeLayout {
        val isUnder = node.accentType == AccentType.UNDERLINE ||
                node.accentType == AccentType.UNDERBRACE

        val accentHeight = when (node.accentType) {
            AccentType.OVERLINE, AccentType.UNDERLINE -> with(density) { 2f.dp.toPx() }
            else -> with(density) { (context.fontSize * 0.3f).toPx() }
        }
        val gap = with(density) { (context.fontSize * 0.08f).toPx() }

        val width = contentLayout.width
        val totalHeight = contentLayout.height + accentHeight + gap

        return NodeLayout(
            width,
            totalHeight,
            contentLayout.baseline + (if (isUnder) 0f else accentHeight + gap)
        ) { x, y ->
            val accentY = if (isUnder) y + contentLayout.height + gap else y
            val contentY = if (isUnder) y else y + accentHeight + gap

            when (node.accentType) {
                AccentType.OVERLINE -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY),
                        end = Offset(x + width, accentY),
                        strokeWidth = with(density) { 1.5f.dp.toPx() }
                    )
                }

                AccentType.UNDERLINE -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY),
                        end = Offset(x + width, accentY),
                        strokeWidth = with(density) { 1.5f.dp.toPx() }
                    )
                }

                AccentType.OVERBRACE -> {
                    val path = Path().apply {
                        val leftX = x
                        val rightX = x + width
                        val centerX = x + width / 2
                        val topY = accentY
                        val bottomY = accentY + accentHeight

                        val tipHeight = accentHeight * 0.25f
                        val shoulderY = topY + tipHeight
                        val curveWidth = min(width / 2, accentHeight)

                        moveTo(leftX, bottomY)
                        cubicTo(
                            leftX, bottomY - (bottomY - shoulderY) * 0.6f,
                            leftX + curveWidth * 0.4f, shoulderY,
                            leftX + curveWidth, shoulderY
                        )
                        lineTo(centerX - curveWidth, shoulderY)
                        cubicTo(
                            centerX - curveWidth * 0.4f, shoulderY,
                            centerX, shoulderY - tipHeight * 0.6f,
                            centerX, topY
                        )
                        cubicTo(
                            centerX, shoulderY - tipHeight * 0.6f,
                            centerX + curveWidth * 0.4f, shoulderY,
                            centerX + curveWidth, shoulderY
                        )
                        lineTo(rightX - curveWidth, shoulderY)
                        cubicTo(
                            rightX - curveWidth * 0.4f, shoulderY,
                            rightX, bottomY - (bottomY - shoulderY) * 0.6f,
                            rightX, bottomY
                        )
                    }
                    drawPath(
                        path = path,
                        color = context.color,
                        style = Stroke(width = with(density) { 1.2f.dp.toPx() })
                    )
                }

                AccentType.UNDERBRACE -> {
                    val path = Path().apply {
                        val leftX = x
                        val rightX = x + width
                        val centerX = x + width / 2
                        val topY = accentY
                        val bottomY = accentY + accentHeight

                        val tipHeight = accentHeight * 0.25f
                        val shoulderY = bottomY - tipHeight
                        val curveWidth = min(width / 2, accentHeight)

                        moveTo(leftX, topY)
                        cubicTo(
                            leftX, topY + (shoulderY - topY) * 0.6f,
                            leftX + curveWidth * 0.4f, shoulderY,
                            leftX + curveWidth, shoulderY
                        )
                        lineTo(centerX - curveWidth, shoulderY)
                        cubicTo(
                            centerX - curveWidth * 0.4f, shoulderY,
                            centerX, shoulderY + tipHeight * 0.6f,
                            centerX, bottomY
                        )
                        cubicTo(
                            centerX, shoulderY + tipHeight * 0.6f,
                            centerX + curveWidth * 0.4f, shoulderY,
                            centerX + curveWidth, shoulderY
                        )
                        lineTo(rightX - curveWidth, shoulderY)
                        cubicTo(
                            rightX - curveWidth * 0.4f, shoulderY,
                            rightX, topY + (shoulderY - topY) * 0.6f,
                            rightX, topY
                        )
                    }
                    drawPath(
                        path = path,
                        color = context.color,
                        style = Stroke(width = with(density) { 1.2f.dp.toPx() })
                    )
                }

                AccentType.WIDEHAT -> {
                    val path = Path().apply {
                        val leftX = x
                        val rightX = x + width
                        val bottomY = accentY + accentHeight
                        val topY = accentY

                        moveTo(leftX, bottomY)
                        lineTo(x + width / 2, topY)
                        lineTo(rightX, bottomY)
                    }
                    drawPath(
                        path = path,
                        color = context.color,
                        style = Stroke(width = with(density) { 1.5f.dp.toPx() })
                    )
                }

                AccentType.OVERRIGHTARROW -> {
                    val arrowY = accentY + accentHeight / 2
                    val arrowEndX = x + width
                    val arrowStartX = x

                    drawLine(
                        color = context.color,
                        start = Offset(arrowStartX, arrowY),
                        end = Offset(arrowEndX, arrowY),
                        strokeWidth = with(density) { 1.5f.dp.toPx() }
                    )

                    val arrowHeadSize = with(density) { 4f.dp.toPx() }
                    val path = Path().apply {
                        moveTo(arrowEndX, arrowY)
                        lineTo(arrowEndX - arrowHeadSize, arrowY - arrowHeadSize / 2)
                        lineTo(arrowEndX - arrowHeadSize, arrowY + arrowHeadSize / 2)
                        close()
                    }
                    drawPath(path = path, color = context.color)
                }

                AccentType.OVERLEFTARROW -> {
                    val arrowY = accentY + accentHeight / 2
                    val arrowStartX = x
                    val arrowEndX = x + width

                    drawLine(
                        color = context.color,
                        start = Offset(arrowStartX, arrowY),
                        end = Offset(arrowEndX, arrowY),
                        strokeWidth = with(density) { 1.5f.dp.toPx() }
                    )

                    val arrowHeadSize = with(density) { 4f.dp.toPx() }
                    val path = Path().apply {
                        moveTo(arrowStartX, arrowY)
                        lineTo(arrowStartX + arrowHeadSize, arrowY - arrowHeadSize / 2)
                        lineTo(arrowStartX + arrowHeadSize, arrowY + arrowHeadSize / 2)
                        close()
                    }
                    drawPath(path = path, color = context.color)
                }

                AccentType.CANCEL -> {
                    // 从左下角到右上角画斜线
                    drawLine(
                        color = context.color,
                        start = Offset(x, contentY + contentLayout.height),
                        end = Offset(x + width, contentY),
                        strokeWidth = with(density) { 1.5f.dp.toPx() }
                    )
                }
                
                else -> {}
            }

            contentLayout.draw(this, x, contentY)
        }
    }
}
