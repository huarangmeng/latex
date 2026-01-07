package com.hrm.latex.renderer.layout

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.measurer.AccentMeasurer
import com.hrm.latex.renderer.layout.measurer.DelimiterMeasurer
import com.hrm.latex.renderer.layout.measurer.ExtensibleArrowMeasurer
import com.hrm.latex.renderer.layout.measurer.MathMeasurer
import com.hrm.latex.renderer.layout.measurer.MatrixMeasurer
import com.hrm.latex.renderer.layout.measurer.SpecialEffectMeasurer
import com.hrm.latex.renderer.layout.measurer.StackMeasurer
import com.hrm.latex.renderer.layout.measurer.TextContentMeasurer
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.applyMathStyle
import com.hrm.latex.renderer.model.applyStyle
import com.hrm.latex.renderer.model.withColor
import com.hrm.latex.renderer.utils.lineSpacingPx
import com.hrm.latex.renderer.utils.splitLines

/**
 * 测量器注册表，避免重复创建实例
 */
private object MeasurerRegistry {
    val text = TextContentMeasurer()
    val math = MathMeasurer()
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

        is LatexNode.Fraction, is LatexNode.Root, is LatexNode.Superscript,
        is LatexNode.Subscript, is LatexNode.BigOperator, is LatexNode.Binomial ->
            MeasurerRegistry.math.measure(node, context, measurer, density, measureGlobal, measureGroupRef)

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

    // 单行 (InlineRow)
    // 第一遍测量：获取所有节点的初步尺寸
    val initialLayouts = nodes.map { measureNode(it, context, measurer, density) }

    // 检查是否存在需要根据内容调整高度的大型运算符（如积分）
    val hasIntegrals = nodes.any { it is LatexNode.BigOperator && it.operator.contains("int") }
    
    val finalMeasuredNodes = if (hasIntegrals && context.mathStyle == RenderContext.MathStyleMode.DISPLAY) {
        // 计算除了积分以外的其他内容的最大高度
        var maxContentAscent = 0f
        var maxContentDescent = 0f
        
        nodes.forEachIndexed { index, node ->
            val isIntegral = node is LatexNode.BigOperator && node.operator.contains("int")
            if (!isIntegral) {
                val layout = initialLayouts[index]
                if (layout.baseline > maxContentAscent) maxContentAscent = layout.baseline
                if (layout.height - layout.baseline > maxContentDescent) maxContentDescent = layout.height - layout.baseline
            }
        }
        
        val contentHeight = maxContentAscent + maxContentDescent
        
        // 如果有明显的高度（例如分式），则重新测量积分以匹配高度
        if (contentHeight > 0) {
            nodes.mapIndexed { index, node ->
                val isIntegral = node is LatexNode.BigOperator && node.operator.contains("int")
                if (isIntegral) {
                    measureNode(node, context.copy(bigOpHeightHint = contentHeight), measurer, density)
                } else {
                    initialLayouts[index]
                }
            }
        } else {
            initialLayouts
        }
    } else {
        initialLayouts
    }

    var totalWidth = 0f
    var maxAscent = 0f // 基线以上高度
    var maxDescent = 0f // 基线以下高度

    finalMeasuredNodes.forEach {
        val ascent = it.baseline
        val descent = it.height - it.baseline
        if (ascent > maxAscent) maxAscent = ascent
        if (descent > maxDescent) maxDescent = descent
        totalWidth += it.width
    }

    val height = maxAscent + maxDescent
    val baseline = maxAscent

    return NodeLayout(totalWidth, height, baseline) { x, y ->
        var currentX = x
        finalMeasuredNodes.forEach { child ->
            val childY = y + (baseline - child.baseline)
            child.draw(this, currentX, childY)
            currentX += child.width
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
