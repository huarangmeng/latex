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


package com.hrm.latex.renderer.layout

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.measurer.AccentMeasurer
import com.hrm.latex.renderer.layout.measurer.BigOperatorMeasurer
import com.hrm.latex.renderer.layout.measurer.BinomialMeasurer
import com.hrm.latex.renderer.layout.measurer.DelimiterMeasurer
import com.hrm.latex.renderer.layout.measurer.ExtensibleArrowMeasurer
import com.hrm.latex.renderer.layout.measurer.FractionMeasurer
import com.hrm.latex.renderer.layout.measurer.MatrixMeasurer
import com.hrm.latex.renderer.layout.measurer.RootMeasurer
import com.hrm.latex.renderer.layout.measurer.ScriptMeasurer
import com.hrm.latex.renderer.layout.measurer.SpecialEffectMeasurer
import com.hrm.latex.renderer.layout.measurer.StackMeasurer
import com.hrm.latex.renderer.layout.measurer.TextContentMeasurer
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.applyMathStyle
import com.hrm.latex.renderer.model.applyStyle
import com.hrm.latex.renderer.model.withColor
import com.hrm.latex.renderer.utils.MathSpacing
import com.hrm.latex.renderer.utils.lineSpacingPx
import com.hrm.latex.renderer.utils.splitLines

/**
 * 测量器注册表，避免重复创建实例
 */
private object MeasurerRegistry {
    val text = TextContentMeasurer()
    val fraction = FractionMeasurer()
    val root = RootMeasurer()
    val script = ScriptMeasurer()
    val bigOperator = BigOperatorMeasurer()
    val binomial = BinomialMeasurer()
    val matrix = MatrixMeasurer()
    val accent = AccentMeasurer()
    val delimiter = DelimiterMeasurer()
    val extensibleArrow = ExtensibleArrowMeasurer()
    val stack = StackMeasurer()
    val specialEffect = SpecialEffectMeasurer()
}

/**
 * 测量节点尺寸与布局
 */
internal fun measureNode(
    node: LatexNode, context: RenderContext, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 递归函数引用
    val measureGlobal = { n: LatexNode, s: RenderContext ->
        measureNode(n, s, measurer, density)
    }
    val measureGroupRef = { nodes: List<LatexNode>, s: RenderContext ->
        measureGroup(nodes, s, measurer, density)
    }

    return when (node) {
        is LatexNode.Text, is LatexNode.TextMode, is LatexNode.Symbol,
        is LatexNode.Operator, is LatexNode.Command, is LatexNode.Space,
        is LatexNode.HSpace ->
            MeasurerRegistry.text.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Fraction ->
            MeasurerRegistry.fraction.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Root ->
            MeasurerRegistry.root.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Superscript, is LatexNode.Subscript ->
            MeasurerRegistry.script.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.BigOperator ->
            MeasurerRegistry.bigOperator.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Binomial ->
            MeasurerRegistry.binomial.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Matrix, is LatexNode.Array, is LatexNode.Cases, is LatexNode.Aligned,
        is LatexNode.Split, is LatexNode.Multline, is LatexNode.Eqnarray, is LatexNode.Subequations ->
            MeasurerRegistry.matrix.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Delimited, is LatexNode.ManualSizedDelimiter ->
            MeasurerRegistry.delimiter.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Accent ->
            MeasurerRegistry.accent.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.ExtensibleArrow ->
            MeasurerRegistry.extensibleArrow.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Stack ->
            MeasurerRegistry.stack.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.Boxed, is LatexNode.Phantom ->
            MeasurerRegistry.specialEffect.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

        is LatexNode.NewCommand -> NodeLayout(
            width = 0f,
            height = 0f,
            baseline = 0f
        ) { _, _ -> /* NewCommand 不渲染 */ }

        is LatexNode.NewLine -> NodeLayout(
            0f, lineSpacingPx(context, density), 0f
        ) { _, _ -> }

        is LatexNode.Group -> measureGroup(node.children, context, measurer, density)
        is LatexNode.Document -> measureGroup(node.children, context, measurer, density)

        is LatexNode.Style -> measureGroup(
            node.content, context.applyStyle(node.styleType), measurer, density
        )

        is LatexNode.Color -> measureGroup(
            node.content, context.withColor(node.color), measurer, density
        )

        is LatexNode.MathStyle -> measureGroup(
            node.content, context.applyMathStyle(node.mathStyleType), measurer, density
        )

        is LatexNode.Environment -> measureGroup(node.content, context, measurer, density)
    }
}

/**
 * 测量节点组（处理行内排列和多行）
 */
internal fun measureGroup(
    nodes: List<LatexNode>, context: RenderContext, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 简单处理多行逻辑：按 NewLine 分割，测量各行，垂直堆叠
    val lines = splitLines(nodes)
    if (lines.size > 1) {
        return measureVerticalLines(lines, context, measurer, density)
    }

    // automatic line breaking when enabled and maxWidth is set
    val maxWidth = context.maxLineWidth
    var precomputedLayouts: List<NodeLayout>? = null

    if (context.lineBreakingEnabled && maxWidth != null && nodes.isNotEmpty()) {
        val layouts = nodes.map { measureNode(it, context, measurer, density) }
        val widths = FloatArray(layouts.size) { layouts[it].width }

        var totalWidth = 0f
        for (w in widths) totalWidth += w

        if (totalWidth > maxWidth) {
            val lineBreaker = LineBreaker(maxWidth)
            val brokenLines = lineBreaker.breakIntoLines(nodes, widths)

            if (brokenLines.size > 1) {
                val lineNodeLists = brokenLines.map { indices ->
                    indices.map { nodes[it] }
                }
                return measureVerticalLines(lineNodeLists, context.copy(lineBreakingEnabled = false), measurer, density)
            }
        }

        // reuse layouts if no line break occurred
        precomputedLayouts = layouts
    }

    // 单行 (InlineRow)
    // 第一遍测量：获取所有节点的初步尺寸
    val initialLayouts = precomputedLayouts ?: nodes.map { measureNode(it, context, measurer, density) }

    // 检查是否存在需要根据内容调整高度的大型运算符（如积分）
    val hasIntegrals = nodes.any { it is LatexNode.BigOperator && it.operator.contains("int") }
    
    val finalMeasuredNodes = if (hasIntegrals && context.mathStyle == MathStyle.DISPLAY) {
        // 为每个积分计算其右侧相邻内容的高度（而非整行最大高度）
        // 避免积分被拉伸到与同行其他大型结构（如求和+分数）一样高
        nodes.mapIndexed { index, node ->
            val isIntegral = node is LatexNode.BigOperator && node.operator.contains("int")
            if (isIntegral) {
                // 收集积分右侧相邻非运算符节点的高度
                var rightContentAscent = 0f
                var rightContentDescent = 0f
                for (j in (index + 1) until nodes.size) {
                    val rightNode = nodes[j]
                    // 遇到下一个大型运算符则停止
                    if (rightNode is LatexNode.BigOperator) break
                    // 跳过空白节点
                    if (rightNode is LatexNode.Space || rightNode is LatexNode.HSpace) continue
                    val layout = initialLayouts[j]
                    if (layout.baseline > rightContentAscent) rightContentAscent = layout.baseline
                    if (layout.height - layout.baseline > rightContentDescent) rightContentDescent = layout.height - layout.baseline
                }
                val rightContentHeight = rightContentAscent + rightContentDescent
                if (rightContentHeight > 0) {
                    measureNode(node, context.copy(bigOpHeightHint = rightContentHeight), measurer, density)
                } else {
                    initialLayouts[index]
                }
            } else {
                initialLayouts[index]
            }
        }
    } else {
        initialLayouts
    }

    // 计算 TeX 标准原子间距
    val isScript = context.mathStyle == MathStyle.SCRIPT ||
            context.mathStyle == MathStyle.SCRIPT_SCRIPT
    val fontSizePx = with(density) { context.fontSize.toPx() }

    // 对每对相邻节点计算间距（spacings[i] = nodes[i] 与 nodes[i+1] 之间的间距）
    val spacings = FloatArray(nodes.size) { 0f }
    for (i in 0 until nodes.size - 1) {
        val leftNode = nodes[i]
        val rightNode = nodes[i + 1]
        // Space/HSpace 节点自带间距，不额外添加
        if (leftNode is LatexNode.Space || leftNode is LatexNode.HSpace ||
            rightNode is LatexNode.Space || rightNode is LatexNode.HSpace) {
            continue
        }
        val leftType = MathSpacing.classifyNode(leftNode)
        val rightType = MathSpacing.classifyNode(rightNode)
        val spacingFactor = MathSpacing.spaceBetween(leftType, rightType, isScript)
        spacings[i] = spacingFactor * fontSizePx
    }

    var totalWidth = 0f
    var maxAscent = 0f // 基线以上高度
    var maxDescent = 0f // 基线以下高度

    finalMeasuredNodes.forEachIndexed { index, layout ->
        val ascent = layout.baseline
        val descent = layout.height - layout.baseline
        if (ascent > maxAscent) maxAscent = ascent
        if (descent > maxDescent) maxDescent = descent
        totalWidth += layout.width
        if (index < spacings.size) totalWidth += spacings[index]
    }

    val height = maxAscent + maxDescent
    val baseline = maxAscent

    return NodeLayout(totalWidth, height, baseline) { x, y ->
        var currentX = x
        finalMeasuredNodes.forEachIndexed { index, child ->
            val childY = y + (baseline - child.baseline)
            child.draw(this, currentX, childY)
            currentX += child.width
            if (index < spacings.size) currentX += spacings[index]
        }
    }
}

private fun measureVerticalLines(
    lines: List<List<LatexNode>>, context: RenderContext, measurer: TextMeasurer, density: Density
): NodeLayout {
    val measuredLines = lines.map { measureGroup(it, context, measurer, density) }
    val maxWidth = measuredLines.maxOfOrNull { it.width } ?: 0f
    val spacing = lineSpacingPx(context, density)

    var totalHeight = 0f
    val positions = measuredLines.map {
        val y = totalHeight
        totalHeight += it.height + spacing
        y
    }
    if (positions.isNotEmpty()) totalHeight -= spacing // 移除最后一个间隙

    // 整个块的基线通常与第一行的基线对齐
    val baseline = measuredLines.firstOrNull()?.baseline ?: 0f

    return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
        measuredLines.forEachIndexed { i, line ->
            // 默认左对齐
            line.draw(this, x, y + positions[i])
        }
    }
}
