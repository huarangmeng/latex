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
import kotlin.math.min

/**
 * 测量特殊效果节点（boxed, phantom, smash, vphantom, hphantom, negation, tag, substack, ref, eqref, sideset, tensor）
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
            is LatexNode.Phantom -> measurePhantom(node, context, measureGroup)
            is LatexNode.Smash -> measureSmash(node, context, density, measureGroup)
            is LatexNode.VPhantom -> measureVPhantom(node, context, measureGroup)
            is LatexNode.HPhantom -> measureHPhantom(node, context, density, measureGroup)
            is LatexNode.Negation -> measureNegation(node, context, density, measureGlobal)
            is LatexNode.Tag -> measureTag(node, context, measurer, density, measureGlobal)
            is LatexNode.Substack -> measureSubstack(node, context, density, measureGroup)
            is LatexNode.Ref -> measureRef(node, context, measurer)
            is LatexNode.EqRef -> measureEqRef(node, context, measurer)
            is LatexNode.SideSet -> measureSideSet(
                node,
                context,
                density,
                measureGlobal
            )

            is LatexNode.Tensor -> measureTensor(
                node,
                context,
                density,
                measureGlobal
            )

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
                end = Offset(
                    x + contentLayout.width - slashPadding,
                    y + contentLayout.height * 0.15f
                ),
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

    /**
     * 测量 \ref{key}
     * 渲染引用标签文本（这里简单显示 key）
     */
    private fun measureRef(
        node: LatexNode.Ref,
        context: RenderContext,
        measurer: TextMeasurer
    ): NodeLayout {
        val style = context.textStyle()
        val result = measurer.measure(AnnotatedString(node.key), style)
        val width = result.size.width.toFloat()
        val height = result.size.height.toFloat()
        val baseline = result.firstBaseline

        return NodeLayout(width, height, baseline) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    /**
     * 测量 \eqref{key}
     * 渲染带括号的引用标签文本
     */
    private fun measureEqRef(
        node: LatexNode.EqRef,
        context: RenderContext,
        measurer: TextMeasurer
    ): NodeLayout {
        val style = context.textStyle()
        val displayText = "(${node.key})"
        val result = measurer.measure(AnnotatedString(displayText), style)
        val width = result.size.width.toFloat()
        val height = result.size.height.toFloat()
        val baseline = result.firstBaseline

        return NodeLayout(width, height, baseline) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    /**
     * 测量 \sideset{_a^b}{_c^d}{\sum}
     * 四角标注大型运算符
     */
    private fun measureSideSet(
        node: LatexNode.SideSet,
        context: RenderContext,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val baseLayout = measureGlobal(node.base, context)
        val scriptContext = context.shrink(MathConstants.SCRIPT_SCALE)

        val leftSubLayout = node.leftSub?.let { measureGlobal(it, scriptContext) }
        val leftSupLayout = node.leftSup?.let { measureGlobal(it, scriptContext) }
        val rightSubLayout = node.rightSub?.let { measureGlobal(it, scriptContext) }
        val rightSupLayout = node.rightSup?.let { measureGlobal(it, scriptContext) }

        val leftWidth = max(leftSubLayout?.width ?: 0f, leftSupLayout?.width ?: 0f)
        val rightWidth = max(rightSubLayout?.width ?: 0f, rightSupLayout?.width ?: 0f)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val scriptGap = fontSizePx * 0.05f

        val totalWidth = leftWidth + scriptGap + baseLayout.width + scriptGap + rightWidth

        // 垂直布局：与 ScriptMeasurer 一致，用 fontSizePx 计算偏移
        val supShift = fontSizePx * MathConstants.SUPERSCRIPT_SHIFT
        val subShift = fontSizePx * MathConstants.SUBSCRIPT_SHIFT

        val topExtent = max(
            leftSupLayout?.let { supShift + it.height } ?: 0f,
            rightSupLayout?.let { supShift + it.height } ?: 0f
        )
        val aboveBase = max(baseLayout.baseline, topExtent)

        val belowBase = max(
            baseLayout.height - baseLayout.baseline,
            max(
                leftSubLayout?.let { subShift + it.height } ?: 0f,
                rightSubLayout?.let { subShift + it.height } ?: 0f
            )
        )

        val totalHeight = aboveBase + belowBase
        val baseline = aboveBase

        val baseRelX = leftWidth + scriptGap
        val baseRelY = aboveBase - baseLayout.baseline

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            // 绘制基础运算符
            baseLayout.draw(this, x + baseRelX, y + baseRelY)

            // 左上标
            leftSupLayout?.let {
                val lsX = x + leftWidth - it.width
                val lsY = y + aboveBase - supShift - it.height
                it.draw(this, lsX, lsY)
            }
            // 左下标
            leftSubLayout?.let {
                val lsX = x + leftWidth - it.width
                val lsY = y + aboveBase + subShift
                it.draw(this, lsX, lsY)
            }
            // 右上标
            rightSupLayout?.let {
                val rsX = x + baseRelX + baseLayout.width + scriptGap
                val rsY = y + aboveBase - supShift - it.height
                it.draw(this, rsX, rsY)
            }
            // 右下标
            rightSubLayout?.let {
                val rsX = x + baseRelX + baseLayout.width + scriptGap
                val rsY = y + aboveBase + subShift
                it.draw(this, rsX, rsY)
            }
        }
    }

    /**
     * 测量 \tensor{T}{^a_b^c_d}
     * 张量指标排列：相邻的上下标配对堆叠在同一列。
     * 坐标系与 ScriptMeasurer 保持一致（基线相对坐标）。
     */
    private fun measureTensor(
        node: LatexNode.Tensor,
        context: RenderContext,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val baseLayout = measureGlobal(node.base, context)
        val scriptStyle = context.shrink(MathConstants.SCRIPT_SCALE)

        if (node.indices.isEmpty()) {
            return baseLayout
        }

        val fontSizePx = with(density) { context.fontSize.toPx() }
        // 与 ScriptMeasurer 一致的偏移量计算
        val superscriptShift = fontSizePx * MathConstants.SUPERSCRIPT_SHIFT
        val subscriptShift = fontSizePx * MathConstants.SUBSCRIPT_SHIFT
        val scriptKern = with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }

        // 将指标分组为列：相邻的上标+下标合并为一列
        data class IndexColumn(
            var supLayout: NodeLayout? = null,
            var subLayout: NodeLayout? = null
        )

        val columns = mutableListOf<IndexColumn>()
        var i = 0
        val indexLayouts = node.indices.map { (isUpper, indexNode) ->
            Pair(isUpper, measureGlobal(indexNode, scriptStyle))
        }

        while (i < indexLayouts.size) {
            val (isUpper, layout) = indexLayouts[i]
            val col = IndexColumn()
            if (isUpper) {
                col.supLayout = layout
                if (i + 1 < indexLayouts.size && !indexLayouts[i + 1].first) {
                    col.subLayout = indexLayouts[i + 1].second
                    i += 2
                } else {
                    i++
                }
            } else {
                col.subLayout = layout
                if (i + 1 < indexLayouts.size && indexLayouts[i + 1].first) {
                    col.supLayout = indexLayouts[i + 1].second
                    i += 2
                } else {
                    i++
                }
            }
            columns.add(col)
        }

        // 使用基线相对坐标系（与 ScriptMeasurer 一致）
        // 上标基线相对于 base 基线向上偏移 superscriptShift
        // 下标基线相对于 base 基线向下偏移 subscriptShift
        val superRelY = -superscriptShift
        val subRelY = subscriptShift

        // 计算边界（相对于 base 基线）
        val baseTopRel = -baseLayout.baseline
        val baseBottomRel = baseLayout.height - baseLayout.baseline

        var minTopRel = baseTopRel
        var maxBottomRel = baseBottomRel

        for (col in columns) {
            col.supLayout?.let { sup ->
                minTopRel = min(minTopRel, superRelY - sup.baseline)
                maxBottomRel = max(maxBottomRel, superRelY + (sup.height - sup.baseline))
            }
            col.subLayout?.let { sub ->
                minTopRel = min(minTopRel, subRelY - sub.baseline)
                maxBottomRel = max(maxBottomRel, subRelY + (sub.height - sub.baseline))
            }
        }

        val totalHeight = maxBottomRel - minTopRel
        val baseline = -minTopRel

        // 计算每列宽度和 x 偏移
        val colWidths = columns.map { col ->
            max(col.supLayout?.width ?: 0f, col.subLayout?.width ?: 0f)
        }

        val scriptX = baseLayout.width + scriptKern
        val colRelXList = mutableListOf<Float>()
        var cx = scriptX
        for (idx in columns.indices) {
            colRelXList.add(cx)
            cx += colWidths[idx]
        }

        val totalWidth = cx

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            baseLayout.draw(this, x, y + baseline - baseLayout.baseline)

            for (idx in columns.indices) {
                val col = columns[idx]
                val colX = x + colRelXList[idx]

                col.supLayout?.let { sup ->
                    sup.draw(this, colX, y + baseline + superRelY - sup.baseline)
                }
                col.subLayout?.let { sub ->
                    sub.draw(this, colX, y + baseline + subRelY - sub.baseline)
                }
            }
        }
    }
}
