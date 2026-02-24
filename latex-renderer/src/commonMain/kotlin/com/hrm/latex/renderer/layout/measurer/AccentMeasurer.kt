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
import androidx.compose.ui.graphics.drawscope.withTransform
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Accent.AccentType
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.MathConstants
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
        val accentStyle = context.shrink(MathConstants.ACCENT_SCALE)
        val textStyle = accentStyle.textStyle()
        val result = measurer.measure(AnnotatedString(accentChar), textStyle)
        
        val (accentHeightScale, accentOffsetScale) = when (node.accentType) {
            AccentType.HAT -> MathConstants.ACCENT_HAT_HEIGHT to MathConstants.ACCENT_HAT_OFFSET
            AccentType.TILDE -> MathConstants.ACCENT_TILDE_HEIGHT to MathConstants.ACCENT_TILDE_OFFSET
            AccentType.BAR -> MathConstants.ACCENT_BAR_HEIGHT to MathConstants.ACCENT_BAR_OFFSET
            AccentType.VEC -> MathConstants.ACCENT_VEC_HEIGHT to MathConstants.ACCENT_VEC_OFFSET
            AccentType.DOT -> MathConstants.ACCENT_DOT_HEIGHT to MathConstants.ACCENT_DOT_OFFSET
            AccentType.DDOT -> MathConstants.ACCENT_DDOT_HEIGHT to MathConstants.ACCENT_DDOT_OFFSET
            else -> MathConstants.ACCENT_DEFAULT_HEIGHT to MathConstants.ACCENT_DEFAULT_OFFSET
        }

        val lineTop = result.getLineTop(0)
        val accentHeight = with(density) { (context.fontSize * accentHeightScale).toPx() }
        val accentBaseline = min(result.firstBaseline - lineTop, accentHeight)

        val accentLayout = NodeLayout(
            result.size.width.toFloat(),
            accentHeight,
            accentBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y - lineTop))
        }

        val width = max(contentLayout.width, accentLayout.width)
        val accentDownOffset = min(
            accentHeight * 0.9f,
            with(density) { (context.fontSize * accentOffsetScale).toPx() }
        )
        val totalHeight = contentLayout.height + accentHeight - accentDownOffset

        return NodeLayout(
            width,
            totalHeight,
            contentLayout.baseline + (if (isUnder) 0f else accentLayout.height - accentDownOffset)
        ) { x, y ->
            val centerX = x + width / 2
            val contentX = centerX - contentLayout.width / 2
            
            // 上方装饰符号向右偏移以补偿斜体效果
            val italicCorrection = if (!isUnder) {
                with(density) { (context.fontSize * MathConstants.ACCENT_ITALIC_CORRECTION).toPx() }
            } else 0f
            val accentX = centerX - accentLayout.width / 2 + italicCorrection

            if (isUnder) {
                contentLayout.draw(this, contentX, y)
                accentLayout.draw(this, accentX, y + contentLayout.height)
            } else {
                accentLayout.draw(this, accentX, y + accentDownOffset)
                contentLayout.draw(this, contentX, y + accentLayout.height - accentDownOffset)
            }
        }
    }

    /**
     * 测量宽装饰（自绘图形）
     *
     * 根据内容宽度，动态绘制横线、大括号、箭头或宽帽子。
     * Path 以 (0,0) 为原点构建，draw 时通过 translate 偏移。
     * NodeLayout 尺寸包含 Stroke 半宽。
     */
    private fun measureWideAccent(
        node: LatexNode.Accent,
        contentLayout: NodeLayout,
        context: RenderContext,
        density: Density
    ): NodeLayout {
        val isUnder = node.accentType == AccentType.UNDERLINE ||
                node.accentType == AccentType.UNDERBRACE

        val isArrowAccent = node.accentType == AccentType.OVERRIGHTARROW ||
                node.accentType == AccentType.OVERLEFTARROW

        val strokeWidth = when (node.accentType) {
            AccentType.OVERBRACE, AccentType.UNDERBRACE -> with(density) { 1.2f.dp.toPx() }
            AccentType.WIDEHAT -> with(density) { 1.5f.dp.toPx() }
            AccentType.OVERLINE, AccentType.UNDERLINE,
            AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW -> with(density) { 1.5f.dp.toPx() }
            AccentType.CANCEL -> with(density) { 1.5f.dp.toPx() }
            else -> with(density) { 1.5f.dp.toPx() }
        }
        val strokeHalf = strokeWidth / 2f

        val accentHeight = when (node.accentType) {
            AccentType.OVERLINE, AccentType.UNDERLINE -> with(density) { 2f.dp.toPx() }
            AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW ->
                with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_ARROW_HEIGHT).toPx() }
            else -> with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_DEFAULT_HEIGHT).toPx() }
        }
        val gap = when {
            isArrowAccent -> with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_ARROW_GAP).toPx() }
            else -> with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_DEFAULT_GAP).toPx() }
        }

        val width = contentLayout.width
        // 总高度包含 Stroke 半宽
        val totalHeight = contentLayout.height + accentHeight + gap + strokeHalf

        // Measure 阶段：以 (0,0) 为原点预构建 Path
        val accentPath: Path? = when (node.accentType) {
            AccentType.OVERBRACE -> {
                val leftX = 0f
                val rightX = width
                val centerX = width / 2
                val topY = 0f
                val bottomY = accentHeight
                val tipHeight = accentHeight * 0.25f
                val shoulderY = topY + tipHeight
                val curveWidth = min(width / 2, accentHeight)

                Path().apply {
                    moveTo(leftX, bottomY)
                    cubicTo(leftX, bottomY - (bottomY - shoulderY) * 0.6f, leftX + curveWidth * 0.4f, shoulderY, leftX + curveWidth, shoulderY)
                    lineTo(centerX - curveWidth, shoulderY)
                    cubicTo(centerX - curveWidth * 0.4f, shoulderY, centerX, shoulderY - tipHeight * 0.6f, centerX, topY)
                    cubicTo(centerX, shoulderY - tipHeight * 0.6f, centerX + curveWidth * 0.4f, shoulderY, centerX + curveWidth, shoulderY)
                    lineTo(rightX - curveWidth, shoulderY)
                    cubicTo(rightX - curveWidth * 0.4f, shoulderY, rightX, bottomY - (bottomY - shoulderY) * 0.6f, rightX, bottomY)
                }
            }

            AccentType.UNDERBRACE -> {
                val leftX = 0f
                val rightX = width
                val centerX = width / 2
                val topY = 0f
                val bottomY = accentHeight
                val tipHeight = accentHeight * 0.25f
                val shoulderY = bottomY - tipHeight
                val curveWidth = min(width / 2, accentHeight)

                Path().apply {
                    moveTo(leftX, topY)
                    cubicTo(leftX, topY + (shoulderY - topY) * 0.6f, leftX + curveWidth * 0.4f, shoulderY, leftX + curveWidth, shoulderY)
                    lineTo(centerX - curveWidth, shoulderY)
                    cubicTo(centerX - curveWidth * 0.4f, shoulderY, centerX, shoulderY + tipHeight * 0.6f, centerX, bottomY)
                    cubicTo(centerX, shoulderY + tipHeight * 0.6f, centerX + curveWidth * 0.4f, shoulderY, centerX + curveWidth, shoulderY)
                    lineTo(rightX - curveWidth, shoulderY)
                    cubicTo(rightX - curveWidth * 0.4f, shoulderY, rightX, topY + (shoulderY - topY) * 0.6f, rightX, topY)
                }
            }

            AccentType.WIDEHAT -> {
                val bottomY = accentHeight
                Path().apply {
                    moveTo(0f, bottomY)
                    lineTo(width / 2, 0f)
                    lineTo(width, bottomY)
                }
            }

            AccentType.OVERRIGHTARROW -> {
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                Path().apply {
                    moveTo(width, accentHeight / 2)
                    lineTo(width - arrowHeadSize, accentHeight / 2 - arrowHeadSize / 2)
                    lineTo(width - arrowHeadSize, accentHeight / 2 + arrowHeadSize / 2)
                    close()
                }
            }

            AccentType.OVERLEFTARROW -> {
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                Path().apply {
                    moveTo(0f, accentHeight / 2)
                    lineTo(arrowHeadSize, accentHeight / 2 - arrowHeadSize / 2)
                    lineTo(arrowHeadSize, accentHeight / 2 + arrowHeadSize / 2)
                    close()
                }
            }

            else -> null
        }

        return NodeLayout(
            width,
            totalHeight,
            contentLayout.baseline + (if (isUnder) 0f else accentHeight + gap)
        ) { x, y ->
            val accentY = if (isUnder) y + contentLayout.height + gap else y
            val contentY = if (isUnder) y else y + accentHeight + gap

            when (node.accentType) {
                AccentType.OVERLINE, AccentType.UNDERLINE -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY + strokeHalf),
                        end = Offset(x + width, accentY + strokeHalf),
                        strokeWidth = strokeWidth
                    )
                }

                AccentType.OVERBRACE, AccentType.UNDERBRACE, AccentType.WIDEHAT -> {
                    accentPath?.let { path ->
                        withTransform({ translate(left = x, top = accentY) }) {
                            drawPath(path = path, color = context.color, style = Stroke(width = strokeWidth))
                        }
                    }
                }

                AccentType.OVERRIGHTARROW -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY + accentHeight / 2),
                        end = Offset(x + width, accentY + accentHeight / 2),
                        strokeWidth = strokeWidth
                    )
                    accentPath?.let { path ->
                        withTransform({ translate(left = x, top = accentY) }) {
                            drawPath(path = path, color = context.color)
                        }
                    }
                }

                AccentType.OVERLEFTARROW -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY + accentHeight / 2),
                        end = Offset(x + width, accentY + accentHeight / 2),
                        strokeWidth = strokeWidth
                    )
                    accentPath?.let { path ->
                        withTransform({ translate(left = x, top = accentY) }) {
                            drawPath(path = path, color = context.color)
                        }
                    }
                }

                AccentType.CANCEL -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, contentY + contentLayout.height),
                        end = Offset(x + width, contentY),
                        strokeWidth = strokeWidth
                    )
                }

                else -> {}
            }

            contentLayout.draw(this, x, contentY)
        }
    }
}
