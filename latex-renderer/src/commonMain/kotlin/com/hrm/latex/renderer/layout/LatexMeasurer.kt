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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.hrm.latex.renderer.layout.measurer.TextContentMeasurer
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.TextDirection
import com.hrm.latex.renderer.model.applyFontSize
import com.hrm.latex.renderer.model.applyMathStyle
import com.hrm.latex.renderer.model.applyStyle
import com.hrm.latex.renderer.model.withColor
import com.hrm.latex.renderer.utils.lineSpacingPx
import com.hrm.latex.renderer.utils.parseDimension
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
 *
 * @param cache 可选的布局缓存。复用同一 AST 实例在相同样式和测量环境下的结果，
 *   编号、引用及其祖先除外。由 [LatexRenderer.measure] 绑定环境后传入。
 */
internal fun measureNode(
    node: LatexNode, context: RenderContext, measurer: TextMeasurer, density: Density,
    cache: LayoutCache? = null
): NodeLayout {
    // 查询缓存
    cache?.getNode(node, context)?.let { return it }

    // 复合原子的内部按自然宽度测量；外层行宽不能传进分子、根号或矩阵单元。
    // Subequations 是透明的环境容器，其内容仍可自动换行。
    fun childContext(s: RenderContext): RenderContext =
        if (node is LatexNode.Subequations) s else s.withoutAutomaticLineBreaking()
    val measureNodeRef = { n: LatexNode, s: RenderContext ->
        measureNode(n, childContext(s), measurer, density, cache)
    }
    val measureGroupRef = { nodes: List<LatexNode>, s: RenderContext ->
        measureGroup(nodes, childContext(s), measurer, density, cache = cache)
    }

    // 1. 优先通过注册表分发（覆盖绝大多数节点类型）
    val registeredMeasurer = MeasurerRegistry.find(node)
    if (registeredMeasurer != null) {
        val layout = registeredMeasurer.measure(
            node, context, measurer, density, measureNodeRef, measureGroupRef
        )
        // 自动编号：为需要编号的环境追加编号标签
        val result = maybeAttachEquationNumber(node, layout, context, measurer, density)
        cache?.putNode(node, context, result)
        return result
    }

    // 2. 轻量级节点：直接内联处理（无需独立 Measurer）
    val result = when (node) {
        is LatexNode.Label -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
        ) { _, _ -> /* Label 不渲染 */ }

        is LatexNode.HLine, is LatexNode.CLine -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
        ) { _, _ -> /* HLine/CLine 由 MatrixMeasurer 处理 */ }

        is LatexNode.Multicolumn -> measureGroup(node.content, context, measurer, density, cache = cache)

        is LatexNode.NewCommand -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
        ) { _, _ -> /* NewCommand 不渲染 */ }

        is LatexNode.NewEnvironment -> NodeLayout(
            width = 0f, height = 0f, baseline = 0f
        ) { _, _ -> /* NewEnvironment 不渲染 */ }

        is LatexNode.SectionHeading -> {
            measureSectionHeading(node, context, measurer, density, cache)
        }

        is LatexNode.NewLine -> NodeLayout(
            0f, lineSpacingPx(context, density), 0f
        ) { _, _ -> }

        is LatexNode.Group -> measureGroup(node.children, context, measurer, density, cache = cache)
        is LatexNode.Document -> measureGroup(node.children, context, measurer, density, cache = cache)

        is LatexNode.Style -> {
            val legacyTextStyle = node.styleType.legacyTextStyle?.takeIf { context.inTextMode }
            val styledContext = if (legacyTextStyle != null) {
                context.applyStyle(LatexNode.Style.StyleType.TEXT_NORMAL).applyStyle(legacyTextStyle)
            } else {
                context.applyStyle(node.styleType)
            }
            val contentContext = if (node.styleType.isTextStyle && !styledContext.inTextMode) {
                styledContext.copy(inTextMode = true)
            } else {
                styledContext
            }
            measureGroup(node.content, contentContext, measurer, density, cache = cache)
        }

        is LatexNode.MathClass -> measureGroup(
            node.content, context, measurer, density, cache = cache
        )

        is LatexNode.Color -> measureGroup(
            node.content, context.withColor(node.color), measurer, density, cache = cache
        )

        is LatexNode.MathChoice -> measureNode(
            node.branch(when (context.mathStyle) {
                MathStyle.DISPLAY -> 0
                MathStyle.TEXT -> 1
                MathStyle.SCRIPT -> 2
                MathStyle.SCRIPT_SCRIPT -> 3
            }), context, measurer, density, cache
        )

        is LatexNode.MathStyle -> measureGroup(
            node.content, context.applyMathStyle(node.mathStyleType), measurer, density, cache = cache
        )

        is LatexNode.FontSize -> measureGroup(
            node.content, context.applyFontSize(node.sizeType), measurer, density, cache = cache
        )

        is LatexNode.Environment -> {
            val envLayout = measureGroup(node.content, context, measurer, density, cache = cache)
            maybeAttachEquationNumber(node, envLayout, context, measurer, density)
        }

        is LatexNode.InlineMath -> measureGroup(
            node.children, context.copy(mathStyle = MathStyle.TEXT, isCramped = false, fontStyle = null, smallCaps = false, inTextMode = false),
            measurer, density, cache = cache
        )

        is LatexNode.DisplayMath -> measureGroup(
            node.children, context.copy(mathStyle = MathStyle.DISPLAY, isCramped = false, smallCaps = false, inTextMode = false),
            measurer, density, cache = cache
        )

        // MathLap: 绘制内容但宽度为零（或部分为零）
        is LatexNode.MathLap -> {
            val contentLayout = measureGroup(node.content, context.withoutAutomaticLineBreaking(), measurer, density, cache = cache)
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

        is LatexNode.Layout -> when (node.layoutType) {
            LatexNode.Layout.LayoutType.RAISE_BOX -> {
                val content = measureGroup(node.content, context.withoutAutomaticLineBreaking(), measurer, density, cache = cache)
                val shift = parseDimension(node.shift.orEmpty(), context, density)
                val rawBaseline = content.baseline + shift
                val childTop = maxOf(-rawBaseline, 0f)
                val baseline = rawBaseline + childTop
                val height = maxOf(childTop + content.height, baseline)
                NodeLayout(content.width, height, baseline) { x, y ->
                    content.draw(this, x, y + childTop)
                }
            }
            LatexNode.Layout.LayoutType.RULE -> {
                val width = parseDimension(node.width.orEmpty(), context, density).coerceAtLeast(0f)
                val ruleHeight = parseDimension(node.height.orEmpty(), context, density).coerceAtLeast(0f)
                val shift = parseDimension(node.shift.orEmpty(), context, density)
                val rawBaseline = ruleHeight + shift
                val ruleTop = maxOf(-rawBaseline, 0f)
                val baseline = rawBaseline + ruleTop
                val height = maxOf(ruleTop + ruleHeight, baseline)
                NodeLayout(width, height, baseline) { x, y ->
                    drawRect(context.color, topLeft = Offset(x, y + ruleTop), size = Size(width, ruleHeight))
                }
            }
        }

        // TextDirection: RTL/LTR 文本方向切换
        is LatexNode.TextDirection -> {
            val dir = when (node.direction) {
                LatexNode.TextDirection.Direction.RTL -> TextDirection.RTL
                LatexNode.TextDirection.Direction.LTR -> TextDirection.LTR
            }
            val dirContext = context.copy(textDirection = dir)
            measureGroup(node.content, dirContext, measurer, density, cache = cache)
        }

        else -> NodeLayout(0f, 0f, 0f) { _, _ -> }
    }
    cache?.putNode(node, context, result)
    return result
}

/**
 * 测量节点组（处理行内排列和多行）
 *
 * @param layoutMap 可选的布局映射表。当非 null 时，测量过程中会将每个子节点的
 *   相对位置记录到 layoutMap 中，为编辑器 hit-testing 和光标定位提供数据。
 *   当为 null 时（默认），无额外开销。
 * @param cache 可选的布局缓存。传播至子节点测量调用。
 */
internal fun measureGroup(
    inputNodes: List<LatexNode>, context: RenderContext, measurer: TextMeasurer, density: Density,
    layoutMap: LayoutMap? = null, cache: LayoutCache? = null
): NodeLayout {
    val styleIndex = when (context.mathStyle) {
        MathStyle.DISPLAY -> 0
        MathStyle.TEXT -> 1
        MathStyle.SCRIPT -> 2
        MathStyle.SCRIPT_SCRIPT -> 3
    }
    fun resolveChoices(node: LatexNode): List<LatexNode> {
        if (node !is LatexNode.MathChoice) return listOf(node)
        val branch = node.branch(styleIndex)
        return (if (branch is LatexNode.Group) branch.children else listOf(branch)).flatMap(::resolveChoices)
    }
    val nodes = if (inputNodes.any { it is LatexNode.MathChoice }) inputNodes.flatMap(::resolveChoices) else inputNodes
    // 显式换行分成逻辑行，每行仍可继续自动换行。
    val lines = splitLines(nodes)
    if (lines.size > 1) {
        return stackLines(lines, context, density, layoutMap) { line, lineMap ->
            measureGroup(line, context, measurer, density, lineMap, cache)
        }
    }

    // 节点只测量一次。分行阶段只读取尺寸，不能再次测量或推进公式编号。
    val hasIntegrals = context.mathStyle == MathStyle.DISPLAY &&
            nodes.any { it is LatexNode.BigOperator && it.operator.contains("int") }
    val initialLayouts = nodes.map { node ->
        if (hasIntegrals && node is LatexNode.BigOperator && node.operator.contains("int")) {
            null
        } else {
            measureNode(node, context, measurer, density, cache)
        }
    }
    val layouts = if (hasIntegrals) {
        GroupLayoutPostProcessor.fillDeferredIntegrals(
            nodes, initialLayouts, context, measurer, density, cache
        )
    } else {
        @Suppress("UNCHECKED_CAST")
        initialLayouts as List<NodeLayout>
    }
    val metrics = InlineRowMetrics(nodes, layouts, context, density)
    val maxWidth = context.layoutHints.maxLineWidth
    if (context.layoutHints.lineBreakingEnabled && maxWidth != null && nodes.isNotEmpty() &&
        metrics.width(0, nodes.size) > maxWidth
    ) {
        val ranges = LineBreaker(maxWidth).breakIntoRanges(
            nodes, FloatArray(layouts.size) { layouts[it].width }, metrics::width
        )
        if (ranges.size > 1) {
            return stackLines(ranges, context, density, layoutMap) { range, lineMap ->
                assembleInlineRow(nodes, layouts, metrics, range.first, range.last + 1, context, lineMap)
            }
        }
    }
    return assembleInlineRow(nodes, layouts, metrics, 0, nodes.size, context, layoutMap)
}

/** 从现成的节点尺寸装配一行，不执行任何测量。绘制和编辑器坐标共用定位规则。 */
private fun assembleInlineRow(
    nodes: List<LatexNode>, layouts: List<NodeLayout>, metrics: InlineRowMetrics,
    start: Int, end: Int, context: RenderContext, layoutMap: LayoutMap?
): NodeLayout {
    val width = metrics.width(start, end)
    var ascent = 0f
    var descent = 0f
    for (i in start until end) {
        ascent = maxOf(ascent, layouts[i].baseline)
        descent = maxOf(descent, layouts[i].height - layouts[i].baseline)
    }
    val rowAscent = ascent
    val isRtl = context.textDirection == TextDirection.RTL
    if (layoutMap != null) {
        forEachInlinePlacement(layouts, metrics, start, end, width, rowAscent, isRtl) { index, x, y ->
            val child = layouts[index]
            layoutMap.add(nodes[index], x, y, child.width, child.height, child.baseline)
        }
    }
    return NodeLayout(width, rowAscent + descent, rowAscent) { originX, originY ->
        forEachInlinePlacement(layouts, metrics, start, end, width, rowAscent, isRtl) { index, x, y ->
            layouts[index].draw(this, originX + x, originY + y)
        }
    }
}

/** 内联遍历使测量与绘制共用坐标计算，无需为每行保存两份位置数组。 */
private inline fun forEachInlinePlacement(
    layouts: List<NodeLayout>, metrics: InlineRowMetrics, start: Int, end: Int,
    width: Float, ascent: Float, isRtl: Boolean, action: (index: Int, x: Float, y: Float) -> Unit
) {
    var x = if (isRtl) width else 0f
    for (i in start until end) {
        val child = layouts[i]
        if (isRtl) x -= child.width
        val y = ascent - child.baseline
        action(i, x, y)
        val spacing = if (i + 1 < end) metrics.spacing(i, start, end) else 0f
        if (isRtl) x -= spacing else x += child.width + spacing
    }
}

/** 垂直组合显式行或自动行，并把行内坐标平移到整块内容坐标。 */
private fun <T> stackLines(
    lines: List<T>, context: RenderContext, density: Density, layoutMap: LayoutMap?,
    measureLine: (T, LayoutMap?) -> NodeLayout
): NodeLayout {
    val spacing = lineSpacingPx(context, density)
    val positions = FloatArray(lines.size)
    val layouts = ArrayList<NodeLayout>(lines.size)
    var height = 0f
    var width = 0f
    for ((i, line) in lines.withIndex()) {
        val lineMap = if (layoutMap != null) LayoutMap() else null
        val layout = measureLine(line, lineMap)
        positions[i] = height
        if (lineMap != null) layoutMap?.appendTranslated(lineMap, height)
        layouts.add(layout)
        width = maxOf(width, layout.width)
        height += layout.height
        if (i + 1 < lines.size) height += spacing
    }
    return NodeLayout(width, height, layouts.firstOrNull()?.baseline ?: 0f) { x, y ->
        for (i in layouts.indices) layouts[i].draw(this, x, y + positions[i])
    }
}

/** 自动换行属于外层流式容器约束，复合原子内部只保留显式换行。 */
private fun RenderContext.withoutAutomaticLineBreaking(): RenderContext =
    if (!layoutHints.lineBreakingEnabled && layoutHints.maxLineWidth == null) this else copy(
        layoutHints = layoutHints.copy(lineBreakingEnabled = false, maxLineWidth = null)
    )

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

    // 渲染编号标签 "(N)"。通过 TextContentMeasurer 复用 KaTeX Main
    // 字形的墨水边界，避免括号的完整文本行框与数字墨水框混用。
    val fontSizePx = with(density) { context.fontSize.toPx() }
    val gap = fontSizePx * 1.5f // 与 TagMeasurer 保持一致的间距
    val tagLayout = measureNode(LatexNode.Text("($number)"), context, textMeasurer, density)
    val tagWidth = gap + tagLayout.width

    val totalWidth = contentLayout.width + tagWidth
    val baseline = maxOf(contentLayout.baseline, tagLayout.baseline)
    val depth = maxOf(
        contentLayout.height - contentLayout.baseline,
        tagLayout.height - tagLayout.baseline
    )
    val totalHeight = baseline + depth

    return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
        // 绘制公式内容
        contentLayout.draw(this, x, y + baseline - contentLayout.baseline)

        // 编号与公式共享同一条数学基线。
        val tagY = y + baseline - tagLayout.baseline
        val tagX = x + contentLayout.width + gap
        tagLayout.draw(this, tagX, tagY)
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

/**
 * 测量章节标题节点
 *
 * 根据标题层级应用不同的字号缩放和粗体样式：
 * - section: 1.4x 粗体
 * - subsection: 1.2x 粗体
 * - subsubsection: 1.1x 粗体
 * - paragraph: 1.0x 粗体
 * - subparagraph: 1.0x 正常
 */
private fun measureSectionHeading(
    node: LatexNode.SectionHeading,
    context: RenderContext,
    measurer: TextMeasurer,
    density: Density,
    cache: LayoutCache? = null
): NodeLayout {
    val (scaleFactor, useBold) = when (node.level) {
        LatexNode.SectionHeading.HeadingLevel.SECTION -> 1.4f to true
        LatexNode.SectionHeading.HeadingLevel.SUBSECTION -> 1.2f to true
        LatexNode.SectionHeading.HeadingLevel.SUBSUBSECTION -> 1.1f to true
        LatexNode.SectionHeading.HeadingLevel.PARAGRAPH -> 1.0f to true
        LatexNode.SectionHeading.HeadingLevel.SUBPARAGRAPH -> 1.0f to false
    }

    val newFontSize = context.fontSize * scaleFactor
    val headingContext = if (useBold) {
        context.copy(
            fontSize = newFontSize,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal
        )
    } else {
        context.copy(fontSize = newFontSize)
    }

    return measureGroup(node.content, headingContext, measurer, density, cache = cache)
}
