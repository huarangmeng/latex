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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max

/**
 * 测量特殊效果节点（boxed, phantom, smash, vphantom, hphantom, negation, tag, substack）
 */
internal class SpecialEffectMeasurer : NodeMeasurer<LatexNode> {

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Boxed -> measureBoxed(node, context, density, measureGroup)
            is LatexNode.Phantom -> measurePhantom(node, context, density, measureGroup)
            is LatexNode.Smash -> measureSmash(node, context, density, measureGroup)
            is LatexNode.VPhantom -> measureVPhantom(node, context, density, measureGroup)
            is LatexNode.HPhantom -> measureHPhantom(node, context, density, measureGroup)
            is LatexNode.Negation -> measureNegation(node, context, measurer, density, measureGlobal)
            is LatexNode.Tag -> measureTag(node, context, measurer, density, measureGlobal)
            is LatexNode.Substack -> measureSubstack(node, context, density, measureGroup)
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    /**
     * 测量 \boxed{...} 方框效果
     */
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
                size = androidx.compose.ui.geometry.Size(totalWidth, totalHeight),
                style = Stroke(width = borderWidth)
            )
        }
    }

    /**
     * 测量 \phantom{...} 幻影效果
     * 占据空间但不绘制内容
     */
    private fun measurePhantom(
        node: LatexNode.Phantom,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        return NodeLayout(
            contentLayout.width,
            contentLayout.height,
            contentLayout.baseline
        ) { _, _ -> }
    }

    /**
     * 测量 \smash{...}
     * 绘制内容但高度视为零（基线保持在顶部）
     */
    private fun measureSmash(
        node: LatexNode.Smash,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        // smash 将高度和深度都设为 0，但仍然绘制内容
        // 使用一个极小的高度避免除零，baseline = 0
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val minHeight = fontSizePx * 0.01f
        return NodeLayout(
            contentLayout.width,
            minHeight,
            0f
        ) { x, y ->
            // 绘制内容在垂直居中位置（虽然 layout 高度为 0，内容仍可见）
            val contentY = y - contentLayout.baseline
            contentLayout.draw(this, x, contentY)
        }
    }

    /**
     * 测量 \vphantom{...}
     * 只保留高度/基线，宽度为零
     */
    private fun measureVPhantom(
        node: LatexNode.VPhantom,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGroup(node.content, context)
        return NodeLayout(
            0f,
            contentLayout.height,
            contentLayout.baseline
        ) { _, _ -> }
    }

    /**
     * 测量 \hphantom{...}
     * 只保留宽度，高度为最小值
     */
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

    /**
     * 测量 \not 否定修饰
     * 在关系符号上叠加一条斜线
     */
    private fun measureNegation(
        node: LatexNode.Negation,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val contentLayout = measureGlobal(node.content, context)
        val strokeWidth = with(density) { 1.5f.dp.toPx() }
        
        // 斜线从左下方到右上方，覆盖内容
        val slashPadding = contentLayout.width * 0.1f
        
        return NodeLayout(
            contentLayout.width,
            contentLayout.height,
            contentLayout.baseline
        ) { x, y ->
            contentLayout.draw(this, x, y)
            // 绘制否定斜线（从左下到右上）
            drawLine(
                color = context.color,
                start = Offset(x + slashPadding, y + contentLayout.height * 0.85f),
                end = Offset(x + contentLayout.width - slashPadding, y + contentLayout.height * 0.15f),
                strokeWidth = strokeWidth
            )
        }
    }

    /**
     * 测量 \tag{label} 或 \tag*{label}
     * 在公式右侧添加编号标签
     */
    private fun measureTag(
        node: LatexNode.Tag,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val labelLayout = measureGlobal(node.label, context)
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val gap = fontSizePx * 1.5f  // 标签与公式之间的间距
        
        if (node.starred) {
            // \tag* 不加括号
            val totalWidth = gap + labelLayout.width
            return NodeLayout(totalWidth, labelLayout.height, labelLayout.baseline) { x, y ->
                labelLayout.draw(this, x + gap, y)
            }
        } else {
            // \tag 加括号: (label)
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

    /**
     * 测量 \substack{line1 \\\\ line2}
     * 垂直堆叠多行，居中对齐
     */
    private fun measureSubstack(
        node: LatexNode.Substack,
        context: RenderContext,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        if (node.rows.isEmpty()) {
            return NodeLayout(0f, 0f, 0f) { _, _ -> }
        }
        
        // substack 内容使用较小字号
        val substackContext = context.shrink(MathConstants.SCRIPT_SCALE)
        val fontSizePx = with(density) { substackContext.fontSize.toPx() }
        val rowSpacing = fontSizePx * 0.15f
        
        val rowLayouts = node.rows.map { measureGroup(it, substackContext) }
        val maxWidth = rowLayouts.maxOf { it.width }
        
        var totalHeight = 0f
        val positions = rowLayouts.map { layout ->
            val y = totalHeight
            totalHeight += layout.height + rowSpacing
            y
        }
        if (positions.isNotEmpty()) totalHeight -= rowSpacing
        
        val baseline = totalHeight / 2f
        
        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            rowLayouts.forEachIndexed { i, layout ->
                val rowX = x + (maxWidth - layout.width) / 2f
                layout.draw(this, rowX, y + positions[i])
            }
        }
    }
}
