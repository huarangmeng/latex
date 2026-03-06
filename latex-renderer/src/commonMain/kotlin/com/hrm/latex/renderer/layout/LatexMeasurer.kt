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
import com.hrm.latex.renderer.layout.measurer.NodeMeasurer
import com.hrm.latex.renderer.layout.measurer.RootMeasurer
import com.hrm.latex.renderer.layout.measurer.ScriptMeasurer
import com.hrm.latex.renderer.layout.measurer.BoxedPhantomMeasurer
import com.hrm.latex.renderer.layout.measurer.NegationMeasurer
import com.hrm.latex.renderer.layout.measurer.RefMeasurer
import com.hrm.latex.renderer.layout.measurer.SideSetTensorMeasurer
import com.hrm.latex.renderer.layout.measurer.StackMeasurer
import com.hrm.latex.renderer.layout.measurer.SubstackMeasurer
import com.hrm.latex.renderer.layout.measurer.TagMeasurer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import com.hrm.latex.renderer.layout.measurer.TextContentMeasurer
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.applyMathStyle
import com.hrm.latex.renderer.model.applyStyle
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.model.withColor
import com.hrm.latex.renderer.utils.MathSpacing
import com.hrm.latex.renderer.utils.lineSpacingPx
import com.hrm.latex.renderer.utils.splitLines
import kotlin.reflect.KClass

/**
 * 测量器注册表
 *
 * 自动根据各 [NodeMeasurer.handledNodeTypes] 建立节点类型 → 测量器的映射。
 * 新增节点类型时只需编写新的测量器并加入 [measurers] 列表，无需修改分发逻辑。
 */
private object MeasurerRegistry {
    private val measurers: List<NodeMeasurer> = listOf(
        TextContentMeasurer(),
        FractionMeasurer(),
        RootMeasurer(),
        ScriptMeasurer(),
        BigOperatorMeasurer(),
        BinomialMeasurer(),
        MatrixMeasurer(),
        AccentMeasurer(),
        DelimiterMeasurer(),
        ExtensibleArrowMeasurer(),
        StackMeasurer(),
        BoxedPhantomMeasurer(),
        NegationMeasurer(),
        TagMeasurer(),
        SubstackMeasurer(),
        RefMeasurer(),
        SideSetTensorMeasurer(),
    )

    /** 节点类型 → 测量器 的查找表，由 [measurers] 自动构建 */
    private val lookupTable: Map<KClass<out LatexNode>, NodeMeasurer> = buildMap {
        for (m in measurers) {
            for (type in m.handledNodeTypes) {
                require(!containsKey(type)) {
                    "Duplicate handler for ${type.simpleName}: " +
                            "both ${get(type)!!::class.simpleName} and ${m::class.simpleName}"
                }
                put(type, m)
            }
        }
    }

    /** 查找节点对应的测量器，未注册时返回 null */
    fun find(node: LatexNode): NodeMeasurer? = lookupTable[node::class]
}

/**
 * 测量节点尺寸与布局
 */
internal fun measureNode(
    node: LatexNode, context: RenderContext, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 递归函数引用
    val measureNodeRef = { n: LatexNode, s: RenderContext ->
        measureNode(n, s, measurer, density)
    }
    val measureGroupRef = { nodes: List<LatexNode>, s: RenderContext ->
        measureGroup(nodes, s, measurer, density)
    }

    // 1. 优先通过注册表分发（覆盖绝大多数节点类型）
    val registeredMeasurer = MeasurerRegistry.find(node)
    if (registeredMeasurer != null) {
        val layout = registeredMeasurer.measure(
            node, context, measurer, density, measureNodeRef, measureGroupRef
        )
        // 自动编号：为需要编号的环境追加编号标签
        return maybeAttachEquationNumber(node, layout, context, measurer, density)
    }

    // 2. 轻量级节点：直接内联处理（无需独立 Measurer）
    return when (node) {
        is LatexNode.Label -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
        ) { _, _ -> /* Label 不渲染 */ }

        is LatexNode.HLine, is LatexNode.CLine -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
        ) { _, _ -> /* HLine/CLine 由 MatrixMeasurer 处理 */ }

        is LatexNode.Multicolumn -> measureGroup(node.content, context, measurer, density)

        is LatexNode.NewCommand -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
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

        is LatexNode.Environment -> {
            val envLayout = measureGroup(node.content, context, measurer, density)
            maybeAttachEquationNumber(node, envLayout, context, measurer, density)
        }

        is LatexNode.InlineMath -> measureGroup(
            node.children, context.copy(mathStyle = MathStyle.TEXT), measurer, density
        )

        is LatexNode.DisplayMath -> measureGroup(
            node.children, context.copy(mathStyle = MathStyle.DISPLAY), measurer, density
        )

        // MathLap: 绘制内容但宽度为零（或部分为零）
        is LatexNode.MathLap -> {
            val contentLayout = measureGroup(node.content, context, measurer, density)
            when (node.lapType) {
                // clap: 居中对齐，宽度为零
                LatexNode.MathLap.LapType.CLAP -> NodeLayout(0f, contentLayout.height, contentLayout.baseline) { x, y ->
                    contentLayout.draw(this, x - contentLayout.width / 2f, y)
                }
                // llap: 内容向左扩展，宽度为零
                LatexNode.MathLap.LapType.LLAP -> NodeLayout(0f, contentLayout.height, contentLayout.baseline) { x, y ->
                    contentLayout.draw(this, x - contentLayout.width, y)
                }
                // rlap: 内容向右扩展，宽度为零
                LatexNode.MathLap.LapType.RLAP -> NodeLayout(0f, contentLayout.height, contentLayout.baseline) { x, y ->
                    contentLayout.draw(this, x, y)
                }
            }
        }

        else -> NodeLayout(0f, 0f, 0f) { _, _ -> }
    }
}

/**
 * 测量节点组（处理行内排列和多行）
 *
 * @param layoutMap 可选的布局映射表。当非 null 时，测量过程中会将每个子节点的
 *   相对位置记录到 layoutMap 中，为编辑器 hit-testing 和光标定位提供数据。
 *   当为 null 时（默认），无额外开销。
 */
internal fun measureGroup(
    nodes: List<LatexNode>, context: RenderContext, measurer: TextMeasurer, density: Density,
    layoutMap: LayoutMap? = null
): NodeLayout {
    // 简单处理多行逻辑：按 NewLine 分割，测量各行，垂直堆叠
    val lines = splitLines(nodes)
    if (lines.size > 1) {
        return measureVerticalLines(lines, context, measurer, density)
    }

    // automatic line breaking when enabled and maxWidth is set
    val maxWidth = context.layoutHints.maxLineWidth
    var precomputedLayouts: List<NodeLayout>? = null

    if (context.layoutHints.lineBreakingEnabled && maxWidth != null && nodes.isNotEmpty()) {
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
                return measureVerticalLines(
                    lineNodeLists,
                    context.copy(layoutHints = context.layoutHints.copy(lineBreakingEnabled = false)),
                    measurer,
                    density
                )
            }
        }

        // reuse layouts if no line break occurred
        precomputedLayouts = layouts
    }

    // 单行 (InlineRow)
    // 积分延迟测量优化：DISPLAY 模式下，积分节点先跳过，等非积分节点测量完毕后
    // 根据右侧邻居高度注入 heightHint 一次性测量，避免双重测量。
    val hasIntegrals = context.mathStyle == MathStyle.DISPLAY &&
            nodes.any { it is LatexNode.BigOperator && it.operator.contains("int") }

    val initialLayouts: List<NodeLayout?>
    if (hasIntegrals && precomputedLayouts == null) {
        // 延迟测量路径：积分节点暂置 null
        initialLayouts = nodes.map { node ->
            if (node is LatexNode.BigOperator && node.operator.contains("int")) {
                null // 延迟测量
            } else {
                measureNode(node, context, measurer, density)
            }
        }
    } else {
        initialLayouts = precomputedLayouts?.map { it }
            ?: nodes.map { measureNode(it, context, measurer, density) }
    }

    // 填充延迟的积分节点（使用右侧邻居高度作为 heightHint）
    val finalMeasuredNodes = if (initialLayouts.any { it == null }) {
        GroupLayoutPostProcessor.fillDeferredIntegrals(
            nodes, initialLayouts, context, measurer, density
        )
    } else {
        @Suppress("UNCHECKED_CAST")
        initialLayouts as List<NodeLayout>
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
            rightNode is LatexNode.Space || rightNode is LatexNode.HSpace
        ) {
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

    // 采集子节点布局信息到 LayoutMap（仅当 layoutMap 非 null 时）
    if (layoutMap != null) {
        var relX = 0f
        finalMeasuredNodes.forEachIndexed { index, child ->
            val relY = baseline - child.baseline
            layoutMap.add(nodes[index], relX, relY, child.width, child.height, child.baseline)
            relX += child.width
            if (index < spacings.size) relX += spacings[index]
        }
    }

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

/**
 * 检查节点是否为需要自动编号的环境，如果是则在布局右侧追加编号标签 (N)。
 *
 * 编号逻辑：
 * - 仅在 [RenderContext.equationNumbering] 非 null 时生效
 * - 判断节点是否为需要编号的环境（非星号变体、无手动 \tag）
 * - 从共享计数器获取下一个编号，渲染为 "(N)" 追加到右侧
 */
private fun maybeAttachEquationNumber(
    node: LatexNode,
    contentLayout: NodeLayout,
    context: RenderContext,
    textMeasurer: TextMeasurer,
    density: Density
): NodeLayout {
    val numbering = context.equationNumbering ?: return contentLayout
    val envName = getNumberableEnvName(node) ?: return contentLayout

    // 检查是否需要编号
    if (!EquationNumbering.isNumberedEnvName(envName)) return contentLayout
    if (hasManualTag(node)) return contentLayout

    // 获取下一个编号
    val number = numbering.nextNumber()

    // 渲染编号标签 "(N)"
    val style = context.textStyle()
    val fontSizePx = with(density) { context.fontSize.toPx() }
    val gap = fontSizePx * 1.5f // 与 TagMeasurer 保持一致的间距

    val leftParen = textMeasurer.measure(AnnotatedString("("), style)
    val numberResult = textMeasurer.measure(AnnotatedString(number), style)
    val rightParen = textMeasurer.measure(AnnotatedString(")"), style)

    val leftW = leftParen.size.width.toFloat()
    val numberW = numberResult.size.width.toFloat()
    val rightW = rightParen.size.width.toFloat()
    val tagWidth = gap + leftW + numberW + rightW
    val tagBaseline = numberResult.firstBaseline

    val totalWidth = contentLayout.width + tagWidth
    val totalHeight = contentLayout.height
    val baseline = contentLayout.baseline

    return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
        // 绘制公式内容
        contentLayout.draw(this, x, y)

        // 绘制编号标签 — 垂直居中对齐到内容基线
        val tagY = y + baseline - tagBaseline
        val tagX = x + contentLayout.width + gap
        drawText(leftParen, topLeft = Offset(tagX, tagY))
        drawText(numberResult, topLeft = Offset(tagX + leftW, tagY))
        drawText(rightParen, topLeft = Offset(tagX + leftW + numberW, tagY))
    }
}

/**
 * 获取节点的可编号环境名。
 * 返回 null 表示该节点不是可编号的环境类型。
 */
private fun getNumberableEnvName(node: LatexNode): String? {
    return when (node) {
        is LatexNode.Environment -> node.name
        is LatexNode.Aligned -> node.envName
        is LatexNode.Multline -> node.envName
        is LatexNode.Eqnarray -> node.envName
        else -> null
    }
}

/**
 * 检查环境节点中是否包含手动 \tag 命令
 */
private fun hasManualTag(node: LatexNode): Boolean {
    val children = when (node) {
        is LatexNode.Environment -> node.content
        is LatexNode.Aligned -> node.rows.flatten()
        is LatexNode.Multline -> node.lines
        is LatexNode.Eqnarray -> node.rows.flatten()
        else -> return false
    }
    return containsTag(children)
}

/**
 * 递归检查节点列表中是否包含 \tag 命令
 */
private fun containsTag(nodes: List<LatexNode>): Boolean {
    for (node in nodes) {
        if (node is LatexNode.Tag) return true
        val children = node.children()
        if (children.isNotEmpty() && containsTag(children)) return true
    }
    return false
}
