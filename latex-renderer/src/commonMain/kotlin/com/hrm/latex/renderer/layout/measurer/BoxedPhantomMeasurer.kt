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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.reflect.KClass

/**
 * 布局修饰测量器 — 处理 \boxed, \phantom, \smash, \vphantom, \hphantom
 */
internal class BoxedPhantomMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Boxed::class,
        LatexNode.Phantom::class,
        LatexNode.Smash::class,
        LatexNode.VPhantom::class,
        LatexNode.HPhantom::class,
        LatexNode.ColorBox::class,
        LatexNode.Hyperlink::class,
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout = when (node) {
        is LatexNode.Boxed -> measureBoxed(node, context, density, measureGroup)
        is LatexNode.Phantom -> measurePhantom(node, context, measureGroup)
        is LatexNode.Smash -> measureSmash(node, context, density, measureGroup)
        is LatexNode.VPhantom -> measureVPhantom(node, context, measureGroup)
        is LatexNode.HPhantom -> measureHPhantom(node, context, density, measureGroup)
        is LatexNode.ColorBox -> measureColorBox(node, context, density, measureGroup)
        is LatexNode.Hyperlink -> measureHyperlink(node, context, density, measurer, measureGroup)
        else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
    }

    private fun measureBoxed(
        node: LatexNode.Boxed,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)

        val padding = with(density) { (context.fontSize * MathConstants.BOXED_PADDING).toPx() }
        val borderWidth = with(density) { MathConstants.BOXED_BORDER_WIDTH_DP.dp.toPx() }

        val totalWidth = contentLayout.width + 2 * padding
        val totalHeight = contentLayout.height + 2 * padding
        val baseline = contentLayout.baseline + padding

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            contentLayout.draw(this, x + padding, y + padding)
            drawRect(
                color = context.color,
                topLeft = Offset(x, y),
                size = Size(totalWidth, totalHeight),
                style = Stroke(width = borderWidth)
            )
        }
    }

    private fun measurePhantom(
        node: LatexNode.Phantom,
        context: RenderContext,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        return NodeLayout(
            contentLayout.width,
            contentLayout.height,
            contentLayout.baseline
        ) { _, _ -> }
    }

    private fun measureSmash(
        node: LatexNode.Smash,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val minDim = fontSizePx * 0.01f

        val ascent = contentLayout.baseline
        val descent = contentLayout.height - contentLayout.baseline

        return when (node.smashType) {
            LatexNode.Smash.SmashType.BOTH -> {
                NodeLayout(contentLayout.width, minDim, 0f) { x, y ->
                    val contentY = y - contentLayout.baseline
                    contentLayout.draw(this, x, contentY)
                }
            }
            LatexNode.Smash.SmashType.TOP -> {
                val height = descent.coerceAtLeast(minDim)
                NodeLayout(contentLayout.width, height, 0f) { x, y ->
                    val contentY = y - ascent
                    contentLayout.draw(this, x, contentY)
                }
            }
            LatexNode.Smash.SmashType.BOTTOM -> {
                val height = ascent.coerceAtLeast(minDim)
                NodeLayout(contentLayout.width, height, ascent) { x, y ->
                    contentLayout.draw(this, x, y)
                }
            }
        }
    }

    private fun measureVPhantom(
        node: LatexNode.VPhantom,
        context: RenderContext,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        return NodeLayout(
            0f,
            contentLayout.height,
            contentLayout.baseline
        ) { _, _ -> }
    }

    private fun measureHPhantom(
        node: LatexNode.HPhantom,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val minHeight = fontSizePx * 0.01f
        return NodeLayout(
            contentLayout.width,
            minHeight,
            0f
        ) { _, _ -> }
    }

    private fun measureColorBox(
        node: LatexNode.ColorBox,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)

        val padding = with(density) { (context.fontSize * MathConstants.BOXED_PADDING).toPx() }
        val borderWidth = with(density) { MathConstants.BOXED_BORDER_WIDTH_DP.dp.toPx() }

        val totalWidth = contentLayout.width + 2 * padding
        val totalHeight = contentLayout.height + 2 * padding
        val baseline = contentLayout.baseline + padding

        val bgColor = parseColor(node.backgroundColor)
        val borderColor = node.borderColor?.let { parseColor(it) }

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            // 绘制背景色
            drawRect(
                color = bgColor,
                topLeft = Offset(x, y),
                size = Size(totalWidth, totalHeight)
            )
            // 绘制边框（仅 fcolorbox）
            if (borderColor != null) {
                drawRect(
                    color = borderColor,
                    topLeft = Offset(x, y),
                    size = Size(totalWidth, totalHeight),
                    style = Stroke(width = borderWidth)
                )
            }
            contentLayout.draw(this, x + padding, y + padding)
        }
    }

    private fun measureHyperlink(
        node: LatexNode.Hyperlink,
        context: RenderContext,
        density: Density,
        textMeasurer: TextMeasurer,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        // 超链接以蓝色 + 下划线渲染
        val linkColor = Color(0xFF0066CC)
        val linkContext = context.copy(color = linkColor)

        val contentLayout = if (node.content.isNotEmpty()) {
            measureGroup(node.content, linkContext)
        } else {
            // \url{...} 没有显示内容，用 URL 本身作为文本
            val textStyle = linkContext.textStyle()
            val result = textMeasurer.measure(AnnotatedString(node.url), textStyle)
            val w = result.size.width.toFloat()
            val h = result.size.height.toFloat()
            val bl = result.firstBaseline
            NodeLayout(w, h, bl) { x, y ->
                drawText(result, topLeft = Offset(x, y))
            }
        }

        val underlineStroke = with(density) { 1f.dp.toPx() }
        val underlineGap = with(density) { 1f.dp.toPx() }
        val totalHeight = contentLayout.height + underlineGap + underlineStroke

        return NodeLayout(contentLayout.width, totalHeight, contentLayout.baseline) { x, y ->
            contentLayout.draw(this, x, y)
            // 绘制下划线
            val underlineY = y + contentLayout.height + underlineGap
            drawLine(
                color = linkColor,
                start = Offset(x, underlineY),
                end = Offset(x + contentLayout.width, underlineY),
                strokeWidth = underlineStroke
            )
        }
    }

    /**
     * 解析颜色名或十六进制颜色
     */
    private fun parseColor(colorString: String): Color {
        return com.hrm.latex.renderer.utils.parseColor(colorString) ?: Color.Black
    }
}
