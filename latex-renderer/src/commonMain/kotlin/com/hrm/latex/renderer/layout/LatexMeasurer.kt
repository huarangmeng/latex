package com.hrm.latex.renderer.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.base.LatexConstants
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Accent.AccentType
import com.hrm.latex.renderer.model.RenderStyle
import com.hrm.latex.renderer.model.applyStyle
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.model.withColor
import com.hrm.latex.renderer.utils.Side
import com.hrm.latex.renderer.utils.drawBracket
import com.hrm.latex.renderer.utils.lineSpacingPx
import com.hrm.latex.renderer.utils.mapBigOp
import com.hrm.latex.renderer.utils.spaceWidthPx
import com.hrm.latex.renderer.utils.splitLines
import kotlin.math.max
import kotlin.math.min

/**
 * 测量节点尺寸与布局
 */
internal fun measureNode(
    node: LatexNode, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    return when (node) {
        is LatexNode.Text -> measureText(node.content, style, measurer)
        is LatexNode.TextMode -> measureTextMode(node.text, style, measurer, density)
        is LatexNode.Symbol -> measureText(
            node.unicode.ifEmpty { node.symbol }, style, measurer
        )

        is LatexNode.Operator -> measureText(
            node.op,
            style.copy(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif),
            measurer
        )

        is LatexNode.Command -> measureText(node.name, style, measurer)

        is LatexNode.Space -> measureSpace(node.type, style, density)
        is LatexNode.NewLine -> NodeLayout(
            0f, lineSpacingPx(style, density), 0f
        ) { _, _ -> } // 换行符本身不绘制内容，在 Group 中处理

        is LatexNode.Group -> measureGroup(node.children, style, measurer, density)
        is LatexNode.Document -> measureGroup(node.children, style, measurer, density)

        is LatexNode.Fraction -> measureFraction(node, style, measurer, density)
        is LatexNode.Root -> measureRoot(node, style, measurer, density)
        is LatexNode.Superscript -> measureScript(node, style, measurer, density, isSuper = true)
        is LatexNode.Subscript -> measureScript(node, style, measurer, density, isSuper = false)

        is LatexNode.Matrix -> measureMatrix(node, style, measurer, density)
        is LatexNode.Array -> measureMatrixLike(node.rows, style, measurer, density)
        is LatexNode.Delimited -> measureDelimited(node, style, measurer, density)
        is LatexNode.ManualSizedDelimiter -> measureManualSizedDelimiter(node, style, measurer, density)
        is LatexNode.Cases -> measureCases(node, style, measurer, density)
        is LatexNode.Aligned -> measureAligned(node, style, measurer, density)
        is LatexNode.BigOperator -> measureBigOperator(node, style, measurer, density)
        is LatexNode.Accent -> measureAccent(node, style, measurer, density)
        is LatexNode.Binomial -> measureBinomial(node, style, measurer, density)

        is LatexNode.Style -> measureGroup(
            node.content, style.applyStyle(node.styleType), measurer, density
        )

        is LatexNode.Color -> measureGroup(
            node.content, style.withColor(node.color), measurer, density
        )

        is LatexNode.Environment -> measureGroup(node.content, style, measurer, density)
    }
}

/**
 * 测量节点组（处理行内排列和多行）
 */
fun measureGroup(
    nodes: List<LatexNode>, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 简单处理多行逻辑：按 NewLine 分割，测量各行，垂直堆叠
    val lines = splitLines(nodes)
    if (lines.size > 1) {
        return measureVerticalLines(lines, style, measurer, density)
    }

    // 单行 (InlineRow)
    val measuredNodes = nodes.map { measureNode(it, style, measurer, density) }

    var totalWidth = 0f
    var maxAscent = 0f // 基线以上高度
    var maxDescent = 0f // 基线以下高度

    measuredNodes.forEach {
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
        measuredNodes.forEach { child ->
            val childY = y + (baseline - child.baseline)
            child.draw(this, currentX, childY)
            currentX += child.width
        }
    }
}

private fun measureVerticalLines(
    lines: List<List<LatexNode>>, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    val measuredLines = lines.map { measureGroup(it, style, measurer, density) }
    val maxWidth = measuredLines.maxOfOrNull { it.width } ?: 0f
    val spacing = lineSpacingPx(style, density)

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

private fun measureText(
    text: String, style: RenderStyle, measurer: TextMeasurer
): NodeLayout {
    val textStyle = style.textStyle()
    val result: TextLayoutResult = measurer.measure(
        text = AnnotatedString(text), style = textStyle
    )

    val width = result.size.width.toFloat()
    val height = result.size.height.toFloat()
    val baseline = result.firstBaseline

    return NodeLayout(width, height, baseline) { x, y ->
        drawText(result, topLeft = Offset(x, y))
    }
}

/**
 * 测量文本模式内容（\text{...}）
 * 使用 Serif 字体和 Normal 样式，更适合在数学公式中显示普通文本
 */
private fun measureTextMode(
    text: String, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 为文本模式使用更合适的字体样式
    val textModeStyle = style.copy(
        fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif,
        // 如果当前是粗体，保持粗体；否则使用正常字重
        fontWeight = style.fontWeight ?: FontWeight.Normal
    )

    val textStyle = textModeStyle.textStyle()
    val result: TextLayoutResult = measurer.measure(
        text = AnnotatedString(text), style = textStyle
    )

    val width = result.size.width.toFloat()
    val height = result.size.height.toFloat()
    val baseline = result.firstBaseline

    return NodeLayout(width, height, baseline) { x, y ->
        drawText(result, topLeft = Offset(x, y))
    }
}

private fun measureSpace(
    type: LatexNode.Space.SpaceType, style: RenderStyle, density: Density
): NodeLayout {
    val width = spaceWidthPx(style, type, density)
    return NodeLayout(width, 0f, 0f) { _, _ -> }
}

/**
 * 测量分数节点
 *
 * 布局结构：
 * ```
 *   numerator (分子)
 *  ───────────────    ← 分数线
 *  denominator (分母)
 * ```
 */
private fun measureFraction(
    node: LatexNode.Fraction, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 分数的分子分母使用较小的字体
    val childStyle = style.shrink(LatexConstants.FRACTION_SCALE_FACTOR)
    val numeratorLayout = measureGroup(listOf(node.numerator), childStyle, measurer, density)
    val denominatorLayout = measureGroup(listOf(node.denominator), childStyle, measurer, density)

    // 分数线粗细
    val ruleThickness = with(density) {
        (style.fontSize * LatexConstants.FRACTION_RULE_THICKNESS_RATIO).toPx()
    }

    // 分数线与分子/分母之间的间距
    val gap = with(density) {
        (style.fontSize * LatexConstants.FRACTION_TOP_PADDING_RATIO).toPx()
    }

    val width = max(numeratorLayout.width, denominatorLayout.width) + gap
    val padding = gap / 2

    // 数学轴高度（数学符号的中心线位置）
    val axisHeight = with(density) {
        (style.fontSize * LatexConstants.MATH_AXIS_HEIGHT_RATIO).toPx()
    }

    // 计算垂直布局位置
    val numeratorTop = 0f
    val numeratorBottom = numeratorTop + numeratorLayout.height
    val lineY = numeratorBottom + gap
    val denominatorTop = lineY + ruleThickness + gap

    val height = denominatorTop + denominatorLayout.height

    // 分数线应该位于数学轴上：baseline - axisHeight = lineY + ruleThickness/2
    val baseline = (lineY + ruleThickness / 2f) + axisHeight

    return NodeLayout(width, height, baseline) { x, y ->
        // 绘制分子 (居中对齐)
        val numeratorX = x + (width - numeratorLayout.width) / 2
        numeratorLayout.draw(this, numeratorX, y + numeratorTop)

        // 绘制分数线
        drawLine(
            color = style.color,
            start = Offset(x + padding, y + lineY + ruleThickness / 2),
            end = Offset(x + width - padding, y + lineY + ruleThickness / 2),
            strokeWidth = ruleThickness
        )

        // 绘制分母 (居中对齐)
        val denominatorX = x + (width - denominatorLayout.width) / 2
        denominatorLayout.draw(this, denominatorX, y + denominatorTop)
    }
}

/**
 * 测量根号节点
 *
 * 布局结构：
 * ```
 *  n  ___
 *   \/  x    ← n 是指数(可选), x 是内容
 * ```
 */
private fun measureRoot(
    node: LatexNode.Root, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    val contentStyle = style
    // 根号指数使用较小字体
    val indexStyle = style.shrink(LatexConstants.ROOT_INDEX_SCALE_FACTOR)

    val contentLayout = measureGroup(listOf(node.content), contentStyle, measurer, density)
    val indexLayout = node.index?.let { measureGroup(listOf(it), indexStyle, measurer, density) }

    val ruleThickness = with(density) {
        (style.fontSize * LatexConstants.FRACTION_RULE_THICKNESS_RATIO).toPx()
    }
    val gap = ruleThickness * 2
    val extraTop = gap + ruleThickness

    // 根号符号宽度（勾号部分）
    val hookWidth = with(density) {
        (style.fontSize * LatexConstants.ROOT_HOOK_WIDTH_RATIO).toPx()
    }

    val indexWidth = indexLayout?.width ?: 0f
    val indexShiftX = if (indexLayout != null) {
        indexWidth - hookWidth * LatexConstants.ROOT_INDEX_OFFSET_RATIO
    } else {
        0f
    }

    val contentX = max(hookWidth, indexWidth) + ruleThickness

    val totalHeight = contentLayout.height + extraTop
    val baseline = contentLayout.baseline + extraTop

    val width = contentX + contentLayout.width + ruleThickness

    return NodeLayout(width, totalHeight, baseline) { x, y ->
        // 绘制指数（位于根号左上角）
        if (indexLayout != null) {
            val indexY =
                y + totalHeight * LatexConstants.ROOT_INDEX_OFFSET_RATIO - indexLayout.height
            indexLayout.draw(this, x, indexY)
        }

        // 绘制内容
        contentLayout.draw(this, x + contentX, y + extraTop)

        // 绘制根号符号
        val topY = y + ruleThickness / 2
        val bottomY = y + totalHeight - ruleThickness
        val midY = y + totalHeight * 0.5f

        // 水平横线
        drawLine(
            style.color, Offset(x + contentX, topY), Offset(x + width, topY), ruleThickness
        )

        // V 形符号（根号的勾）
        val p = Path()
        p.moveTo(x + contentX, topY)
        p.lineTo(x + contentX - hookWidth * 0.4f, bottomY) // 向下
        p.lineTo(x + contentX - hookWidth * 0.8f, midY + ruleThickness) // 向左上
        p.lineTo(x + contentX - hookWidth, midY + ruleThickness * 2) // 小尾巴

        drawPath(p, style.color, style = Stroke(ruleThickness))
    }
}

/**
 * 测量上标/下标节点
 *
 * @param isSuper true 表示上标，false 表示下标
 */
private fun measureScript(
    node: LatexNode, style: RenderStyle, measurer: TextMeasurer, density: Density, isSuper: Boolean
): NodeLayout {
    val baseNode =
        if (isSuper) (node as LatexNode.Superscript).base else (node as LatexNode.Subscript).base
    val scriptNode =
        if (isSuper) (node as LatexNode.Superscript).exponent else (node as LatexNode.Subscript).index

    // 上下标使用较小字体
    val scriptStyle = style.shrink(LatexConstants.SCRIPT_SCALE_FACTOR)

    val baseLayout = measureNode(baseNode, style, measurer, density)
    val scriptLayout = measureNode(scriptNode, scriptStyle, measurer, density)

    // 上标向上偏移，下标向下偏移
    val superscriptShift = with(density) {
        (style.fontSize * LatexConstants.SUPERSCRIPT_SHIFT_RATIO).toPx()
    }
    val subscriptShift = with(density) {
        (style.fontSize * LatexConstants.SUBSCRIPT_SHIFT_RATIO).toPx()
    }

    val scriptX = baseLayout.width + with(density) { 1.dp.toPx() }

    // 计算脚本相对于基线的偏移
    val scriptRelY = if (isSuper) -superscriptShift else subscriptShift

    // 计算总高度和基线位置
    val scriptTopRel = scriptRelY - scriptLayout.baseline
    val scriptBottomRel = scriptRelY + (scriptLayout.height - scriptLayout.baseline)

    val baseTopRel = -baseLayout.baseline
    val baseBottomRel = baseLayout.height - baseLayout.baseline

    val maxTopRel = min(scriptTopRel, baseTopRel)
    val maxBottomRel = max(scriptBottomRel, baseBottomRel)

    val totalHeight = maxBottomRel - maxTopRel
    val baseline = -maxTopRel

    val width = baseLayout.width + scriptLayout.width

    return NodeLayout(width, totalHeight, baseline) { x, y ->
        // 绘制基础内容
        baseLayout.draw(this, x, y + baseline - baseLayout.baseline)

        // 绘制上标/下标
        scriptLayout.draw(this, x + scriptX, y + baseline + scriptRelY - scriptLayout.baseline)
    }
}

private fun measureBigOperator(
    node: LatexNode.BigOperator, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 大型运算符，带上下限（显示样式）或侧面（行内样式）
    // 目前假设如果有上下标则采用显示样式行为
    val symbol = mapBigOp(node.operator)
    val isIntegral = node.operator.contains("int")

    val opStyle = style.grow(1.5f) // 放大运算符
    val limitStyle = style.shrink(0.8f)

    val opLayout = measureText(symbol, opStyle, measurer)
    val superLayout =
        node.superscript?.let { measureGroup(listOf(it), limitStyle, measurer, density) }
    val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle, measurer, density) }

    if (isIntegral) {
        // 积分符号：限制在右侧（脚本样式）
        val supShift = with(density) { (style.fontSize * 0.4f).toPx() }
        val subShift = with(density) { (style.fontSize * 0.2f).toPx() }

        // 确定间距
        val gap = with(density) { (style.fontSize * 0.1f).toPx() } // 运算符和脚本之间的微小间隙

        // 简单脚本逻辑：
        // 积分限制通常：下标底部与运算符底部对齐，上标顶部大致与运算符顶部对齐。

        // 运算符中心
        val opCenterY = opLayout.baseline - opLayout.height / 2

        // 简单位移逻辑
        val sUp = opLayout.height * 0.3f
        val sDown = opLayout.height * 0.2f

        val superRelBase = -sUp
        val subRelBase = sDown

        val opTop = -opLayout.baseline
        val opBottom = opLayout.height - opLayout.baseline

        val superTop = if (superLayout != null) superRelBase - superLayout.baseline else 0f
        val subBottom =
            if (subLayout != null) subRelBase + (subLayout.height - subLayout.baseline) else 0f

        val maxTop = min(opTop, if (superLayout != null) superTop else opTop)
        val maxBottom = max(opBottom, if (subLayout != null) subBottom else opBottom)

        val totalHeight = maxBottom - maxTop
        val baseline = -maxTop

        val scriptWidth = max(superLayout?.width ?: 0f, subLayout?.width ?: 0f)
        val width = opLayout.width + (if (scriptWidth > 0) gap + scriptWidth else 0f)

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            // 绘制运算符
            opLayout.draw(this, x, y + baseline - opLayout.baseline)

            val scriptX = x + opLayout.width + gap

            // 绘制上标
            superLayout?.draw(this, scriptX, y + baseline + superRelBase - superLayout.baseline)

            // 绘制下标
            subLayout?.draw(this, scriptX, y + baseline + subRelBase - subLayout.baseline)
        }
    } else {
        // 求和样式：限制在上方/下方
        val spacing = with(density) { (style.fontSize * 0.1f).toPx() }
        val maxWidth = max(opLayout.width, max(superLayout?.width ?: 0f, subLayout?.width ?: 0f))

        val opTop = (superLayout?.height ?: 0f) + (if (superLayout != null) spacing else 0f)
        val subTop = opTop + opLayout.height + (if (subLayout != null) spacing else 0f)

        val totalHeight = subTop + (subLayout?.height ?: 0f)
        val baseline = opTop + opLayout.baseline

        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            // 居中绘制运算符
            opLayout.draw(this, x + (maxWidth - opLayout.width) / 2, y + opTop)

            // 绘制上标
            superLayout?.draw(this, x + (maxWidth - superLayout.width) / 2, y)

            // 绘制下标
            subLayout?.draw(this, x + (maxWidth - subLayout.width) / 2, y + subTop)
        }
    }
}

private fun measureMatrix(
    node: LatexNode.Matrix, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    val contentLayout = measureMatrixLike(node.rows, style, measurer, density)

    // 定界符
    val bracketType = node.type
    if (bracketType == LatexNode.Matrix.MatrixType.PLAIN) return contentLayout

    // 测量括号
    val bracketWidth = with(density) { (style.fontSize * 0.5f).toPx() }
    val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }

    val width = contentLayout.width + bracketWidth * 2
    val height = contentLayout.height
    val baseline = contentLayout.baseline

    return NodeLayout(width, height, baseline) { x, y ->
        // 绘制左括号
        drawBracket(bracketType, Side.LEFT, x, y, bracketWidth, height, strokeWidth, style.color)

        // 绘制内容
        contentLayout.draw(this, x + bracketWidth, y)

        // 绘制右括号
        drawBracket(
            bracketType,
            Side.RIGHT,
            x + width - bracketWidth,
            y,
            bracketWidth,
            height,
            strokeWidth,
            style.color
        )
    }
}

private fun measureMatrixLike(
    rows: List<List<LatexNode>>, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 1. 测量所有单元格
    val measuredRows = rows.map { row ->
        row.map { node -> measureNode(node, style, measurer, density) }
    }

    // 2. 计算列宽和行高
    val colCount = measuredRows.maxOfOrNull { it.size } ?: 0
    val rowCount = measuredRows.size

    val colWidths = FloatArray(colCount)
    val rowHeights = FloatArray(rowCount)
    val rowBaselines = FloatArray(rowCount) // 每行最大 ascent

    // 列宽
    for (c in 0 until colCount) {
        var maxW = 0f
        for (r in 0 until rowCount) {
            if (c < measuredRows[r].size) {
                maxW = max(maxW, measuredRows[r][c].width)
            }
        }
        colWidths[c] = maxW
    }

    // 行高和基线
    for (r in 0 until rowCount) {
        var maxAscent = 0f
        var maxDescent = 0f
        for (c in 0 until measuredRows[r].size) {
            val cell = measuredRows[r][c]
            maxAscent = max(maxAscent, cell.baseline)
            maxDescent = max(maxDescent, cell.height - cell.baseline)
        }
        rowHeights[r] = maxAscent + maxDescent
        rowBaselines[r] = maxAscent
    }

    val colSpacing = with(density) { (style.fontSize * 0.5f).toPx() }
    val rowSpacing = with(density) { (style.fontSize * 0.2f).toPx() }

    val totalWidth = colWidths.sum() + colSpacing * max(0, colCount - 1)
    val totalHeight = rowHeights.sum() + rowSpacing * max(0, rowCount - 1)

    // 对齐块基线
    // 矩阵通常在数学轴上居中。
    // 为简单起见，与总高度的一半对齐，或第一行。
    // 标准做法：垂直居中于轴。
    val axisHeight = with(density) { (style.fontSize * 0.25f).toPx() }
    val baseline = totalHeight / 2 + axisHeight

    return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
        var currentY = y
        for (r in 0 until rowCount) {
            var currentX = x
            val rowBaseY = currentY + rowBaselines[r]

            for (c in 0 until measuredRows[r].size) {
                val cell = measuredRows[r][c]
                // 单元格在网格槽中居中还是左对齐？矩阵默认居中。
                val cellX = currentX + (colWidths[c] - cell.width) / 2
                val cellY = rowBaseY - cell.baseline

                cell.draw(this, cellX, cellY)

                currentX += colWidths[c] + colSpacing
            }
            currentY += rowHeights[r] + rowSpacing
        }
    }
}

private fun measureDelimited(
    node: LatexNode.Delimited, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 类似于 Matrix 但只有一个单元格，且使用自定义定界符
    val contentLayout = measureGroup(node.content, style, measurer, density)

    val leftStr = node.left
    val rightStr = node.right

    fun getBracketType(str: String): LatexNode.Matrix.MatrixType? = when (str) {
        "(", ")" -> LatexNode.Matrix.MatrixType.PAREN
        "[", "]" -> LatexNode.Matrix.MatrixType.BRACKET
        "{", "}" -> LatexNode.Matrix.MatrixType.BRACE
        "|", "|" -> LatexNode.Matrix.MatrixType.VBAR
        "||" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
        else -> null // 回退到文本
    }

    val leftType = getBracketType(leftStr)
    val rightType = getBracketType(rightStr)

    // 如果为 null，则作为文本测量
    val leftLayout = if (leftType == null && leftStr != ".") measureText(
        leftStr, style, measurer
    ) else null
    val rightLayout = if (rightType == null && rightStr != ".") measureText(
        rightStr, style, measurer
    ) else null

    val bracketWidth = with(density) { (style.fontSize * 0.4f).toPx() }
    val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }

    val leftW = leftLayout?.width ?: if (leftStr != ".") bracketWidth else 0f
    val rightW = rightLayout?.width ?: if (rightStr != ".") bracketWidth else 0f

    val width = leftW + contentLayout.width + rightW
    val height = contentLayout.height
    val baseline = contentLayout.baseline

    return NodeLayout(width, height, baseline) { x, y ->
        var curX = x

        // 绘制左侧
        if (leftLayout != null) {
            leftLayout.draw(this, curX, y + baseline - leftLayout.baseline)
            curX += leftLayout.width
        } else if (leftType != null) {
            drawBracket(leftType, Side.LEFT, curX, y, leftW, height, strokeWidth, style.color)
            curX += leftW
        }

        // 绘制内容
        contentLayout.draw(this, curX, y)
        curX += contentLayout.width

        // 绘制右侧
        if (rightLayout != null) {
            rightLayout.draw(this, curX, y + baseline - rightLayout.baseline)
        } else if (rightType != null) {
            drawBracket(rightType, Side.RIGHT, curX, y, rightW, height, strokeWidth, style.color)
        }
    }
}

/**
 * 测量手动大小分隔符节点
 * 
 * 将分隔符渲染为指定大小的独立符号，不包裹内容
 * 
 * @param node 手动大小分隔符节点
 * @param style 渲染样式
 * @param measurer 文本测量器
 * @param density 密度信息
 */
private fun measureManualSizedDelimiter(
    node: LatexNode.ManualSizedDelimiter,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    val delimiter = node.delimiter
    val scaleFactor = node.size
    
    // 根据分隔符类型选择渲染方式
    val bracketType = when (delimiter) {
        "(", ")" -> LatexNode.Matrix.MatrixType.PAREN
        "[", "]" -> LatexNode.Matrix.MatrixType.BRACKET
        "{", "}" -> LatexNode.Matrix.MatrixType.BRACE
        "|" -> LatexNode.Matrix.MatrixType.VBAR
        "||" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
        else -> null
    }
    
    // 计算分隔符的尺寸
    val baseHeight = with(density) { (style.fontSize * 1.2f).toPx() }
    val height = baseHeight * scaleFactor
    val bracketWidth = with(density) { (style.fontSize * 0.4f).toPx() }
    val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }
    
    // 基线位置：基于数学轴对齐（与周围内容在同一水平线上）
    // 数学轴通常位于 x-height 的中心，约为字体大小的 0.25
    val axisHeight = with(density) { (style.fontSize * 0.25f).toPx() }
    val baseline = height / 2 + axisHeight
    
    return if (bracketType != null) {
        // 使用绘图渲染括号
        val side = if (delimiter in listOf("(", "[", "{")) Side.LEFT else Side.RIGHT
        
        NodeLayout(bracketWidth, height, baseline) { x, y ->
            drawBracket(bracketType, side, x, y, bracketWidth, height, strokeWidth, style.color)
        }
    } else {
        // 使用文本渲染其他分隔符（如角括号、特殊符号）
        val delimiterStyle = style.grow(scaleFactor)
        measureText(delimiter, delimiterStyle, measurer)
    }
}

private fun measureCases(
    node: LatexNode.Cases, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 类似于两列矩阵，左对齐。
    // 左侧有一个大花括号。
    val rows = node.cases.map { (cond, expr) ->
        listOf(expr, LatexNode.Text(" if "), cond)
    }

    val matrixLayout = measureMatrixLike(rows, style, measurer, density)

    val bracketWidth = with(density) { (style.fontSize * 0.5f).toPx() }
    val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }

    val width = bracketWidth + matrixLayout.width
    val height = matrixLayout.height
    val baseline = matrixLayout.baseline

    return NodeLayout(width, height, baseline) { x, y ->
        drawBracket(
            LatexNode.Matrix.MatrixType.BRACE,
            Side.LEFT,
            x,
            y,
            bracketWidth,
            height,
            strokeWidth,
            style.color
        )
        matrixLayout.draw(this, x + bracketWidth, y)
    }
}

private fun measureAligned(
    node: LatexNode.Aligned, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    return measureMatrixLike(node.rows, style, measurer, density)
}

private fun measureAccent(
    node: LatexNode.Accent, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    val contentLayout = measureGroup(listOf(node.content), style, measurer, density)
    
    // 对于宽装饰（需要根据内容宽度拉伸的装饰），使用特殊绘制逻辑
    val isWideAccent = when (node.accentType) {
        AccentType.WIDEHAT, AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW,
        AccentType.OVERLINE, AccentType.UNDERLINE,
        AccentType.OVERBRACE, AccentType.UNDERBRACE -> true
        else -> false
    }
    
    if (isWideAccent) {
        return measureWideAccent(node, contentLayout, style, measurer, density)
    }
    
    val accentChar = when (node.accentType) {
        AccentType.HAT -> "^"
        AccentType.TILDE -> "~"
        AccentType.BAR -> "¯"
        AccentType.VEC -> "→"
        AccentType.DOT -> "˙"
        AccentType.DDOT -> "¨"
        AccentType.OVERLINE, AccentType.UNDERLINE,
        AccentType.OVERBRACE, AccentType.UNDERBRACE,
        AccentType.WIDEHAT, AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW -> ""
    }

    // 如果是下划线/下括号，画在下方
    val isUnder =
        node.accentType == AccentType.UNDERLINE || node.accentType == AccentType.UNDERBRACE
    val accentLayout = measureText(accentChar, style.shrink(0.8f), measurer)

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
 * 测量宽装饰（widehat, overrightarrow, overleftarrow, overline, underline, overbrace, underbrace）
 * 这些装饰需要根据内容宽度进行拉伸绘制
 */
private fun measureWideAccent(
    node: LatexNode.Accent,
    contentLayout: NodeLayout,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    // 判断是否是下方装饰
    val isUnder = node.accentType == AccentType.UNDERLINE || 
                  node.accentType == AccentType.UNDERBRACE
    
    // 装饰高度和间距
    val accentHeight = when (node.accentType) {
        AccentType.OVERLINE, AccentType.UNDERLINE -> with(density) { 2f.dp.toPx() } // 线条只需要很薄
        else -> with(density) { (style.fontSize * 0.3f).toPx() }
    }
    val gap = with(density) { (style.fontSize * 0.08f).toPx() }
    
    val width = contentLayout.width
    val totalHeight = contentLayout.height + accentHeight + gap
    
    return NodeLayout(
        width, 
        totalHeight, 
        contentLayout.baseline + (if (isUnder) 0f else accentHeight + gap)
    ) { x, y ->
        // 根据装饰类型确定位置
        val accentY = if (isUnder) y + contentLayout.height + gap else y
        val contentY = if (isUnder) y else y + accentHeight + gap
        
        // 绘制装饰
        when (node.accentType) {
            AccentType.OVERLINE -> {
                // 绘制上划线：横线
                drawLine(
                    color = style.color,
                    start = Offset(x, accentY),
                    end = Offset(x + width, accentY),
                    strokeWidth = with(density) { 1.5f.dp.toPx() }
                )
            }
            
            AccentType.UNDERLINE -> {
                // 绘制下划线：横线
                drawLine(
                    color = style.color,
                    start = Offset(x, accentY),
                    end = Offset(x + width, accentY),
                    strokeWidth = with(density) { 1.5f.dp.toPx() }
                )
            }
            
            AccentType.OVERBRACE -> {
                // 绘制上大括号：标准的 LaTeX 样式（中间有尖角）
                val path = Path().apply {
                    val leftX = x
                    val rightX = x + width
                    val centerX = x + width / 2
                    val topY = accentY
                    val bottomY = accentY + accentHeight
                    
                    // 计算关键尺寸
                    // 尖角的高度占比
                    val tipHeight = accentHeight * 0.25f
                    // 肩膀（横线部分）的Y坐标
                    val shoulderY = topY + tipHeight
                    // 弯曲部分的宽度，限制为高度的一倍，且不超过总宽度的一半
                    val curveWidth = min(width / 2, accentHeight)
                    
                    // 1. 左脚起笔
                    moveTo(leftX, bottomY)
                    // 2. 左侧上升曲线：从垂直变为水平
                    // CP1 在起点的上方，CP2 在终点的左侧
                    cubicTo(
                        leftX, bottomY - (bottomY - shoulderY) * 0.6f,
                        leftX + curveWidth * 0.4f, shoulderY,
                        leftX + curveWidth, shoulderY
                    )
                    // 3. 左侧横线
                    lineTo(centerX - curveWidth, shoulderY)
                    // 4. 中间尖角左半边：从水平变为向上
                    cubicTo(
                        centerX - curveWidth * 0.4f, shoulderY,
                        centerX, shoulderY - tipHeight * 0.6f,
                        centerX, topY
                    )
                    // 5. 中间尖角右半边：从向上变为水平
                    cubicTo(
                        centerX, shoulderY - tipHeight * 0.6f,
                        centerX + curveWidth * 0.4f, shoulderY,
                        centerX + curveWidth, shoulderY
                    )
                    // 6. 右侧横线
                    lineTo(rightX - curveWidth, shoulderY)
                    // 7. 右侧下降曲线：从水平变为垂直
                    cubicTo(
                        rightX - curveWidth * 0.4f, shoulderY,
                        rightX, bottomY - (bottomY - shoulderY) * 0.6f,
                        rightX, bottomY
                    )
                }
                drawPath(
                    path = path,
                    color = style.color,
                    style = Stroke(width = with(density) { 1.2f.dp.toPx() }) // 稍微细一点，显得更精致
                )
            }
            
            AccentType.UNDERBRACE -> {
                // 绘制下大括号：标准的 LaTeX 样式
                val path = Path().apply {
                    val leftX = x
                    val rightX = x + width
                    val centerX = x + width / 2
                    val topY = accentY
                    val bottomY = accentY + accentHeight
                    
                    // 计算关键尺寸
                    val tipHeight = accentHeight * 0.25f
                    // 肩膀（横线部分）的Y坐标
                    val shoulderY = bottomY - tipHeight
                    val curveWidth = min(width / 2, accentHeight)
                    
                    // 1. 左脚起笔
                    moveTo(leftX, topY)
                    // 2. 左侧下降曲线
                    cubicTo(
                        leftX, topY + (shoulderY - topY) * 0.6f,
                        leftX + curveWidth * 0.4f, shoulderY,
                        leftX + curveWidth, shoulderY
                    )
                    // 3. 左侧横线
                    lineTo(centerX - curveWidth, shoulderY)
                    // 4. 中间尖角左半边
                    cubicTo(
                        centerX - curveWidth * 0.4f, shoulderY,
                        centerX, shoulderY + tipHeight * 0.6f,
                        centerX, bottomY
                    )
                    // 5. 中间尖角右半边
                    cubicTo(
                        centerX, shoulderY + tipHeight * 0.6f,
                        centerX + curveWidth * 0.4f, shoulderY,
                        centerX + curveWidth, shoulderY
                    )
                    // 6. 右侧横线
                    lineTo(rightX - curveWidth, shoulderY)
                    // 7. 右侧上升曲线
                    cubicTo(
                        rightX - curveWidth * 0.4f, shoulderY,
                        rightX, topY + (shoulderY - topY) * 0.6f,
                        rightX, topY
                    )
                }
                drawPath(
                    path = path,
                    color = style.color,
                    style = Stroke(width = with(density) { 1.2f.dp.toPx() })
                )
            }
            
            AccentType.WIDEHAT -> {
                // 绘制宽帽子：倒V形状
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
                    color = style.color,
                    style = Stroke(width = with(density) { 1.5f.dp.toPx() })
                )
            }
            
            AccentType.OVERRIGHTARROW -> {
                // 绘制右箭头
                val arrowY = accentY + accentHeight / 2
                val arrowEndX = x + width
                val arrowStartX = x
                
                // 箭头线
                drawLine(
                    color = style.color,
                    start = Offset(arrowStartX, arrowY),
                    end = Offset(arrowEndX, arrowY),
                    strokeWidth = with(density) { 1.5f.dp.toPx() }
                )
                
                // 箭头头部
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                val path = Path().apply {
                    moveTo(arrowEndX, arrowY)
                    lineTo(arrowEndX - arrowHeadSize, arrowY - arrowHeadSize / 2)
                    lineTo(arrowEndX - arrowHeadSize, arrowY + arrowHeadSize / 2)
                    close()
                }
                drawPath(path = path, color = style.color)
            }
            
            AccentType.OVERLEFTARROW -> {
                // 绘制左箭头
                val arrowY = accentY + accentHeight / 2
                val arrowStartX = x
                val arrowEndX = x + width
                
                // 箭头线
                drawLine(
                    color = style.color,
                    start = Offset(arrowStartX, arrowY),
                    end = Offset(arrowEndX, arrowY),
                    strokeWidth = with(density) { 1.5f.dp.toPx() }
                )
                
                // 箭头头部
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                val path = Path().apply {
                    moveTo(arrowStartX, arrowY)
                    lineTo(arrowStartX + arrowHeadSize, arrowY - arrowHeadSize / 2)
                    lineTo(arrowStartX + arrowHeadSize, arrowY + arrowHeadSize / 2)
                    close()
                }
                drawPath(path = path, color = style.color)
            }
            
            else -> {}
        }
        
        // 绘制内容
        contentLayout.draw(this, x, contentY)
    }
}

private fun measureBinomial(
    node: LatexNode.Binomial, style: RenderStyle, measurer: TextMeasurer, density: Density
): NodeLayout {
    // 类似于分数，但带括号且没有线
    val childStyle = style.shrink(0.9f)
    val numLayout = measureGroup(listOf(node.top), childStyle, measurer, density)
    val denLayout = measureGroup(listOf(node.bottom), childStyle, measurer, density)

    val gap = with(density) { (style.fontSize * 0.2f).toPx() }
    val contentWidth = max(numLayout.width, denLayout.width)
    val height = numLayout.height + denLayout.height + gap
    val baseline = numLayout.height + gap / 2

    val bracketWidth = with(density) { (style.fontSize * 0.4f).toPx() }
    val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }

    val width = contentWidth + bracketWidth * 2

    return NodeLayout(width, height, baseline) { x, y ->
        // 括号
        drawBracket(
            LatexNode.Matrix.MatrixType.PAREN,
            Side.LEFT,
            x,
            y,
            bracketWidth,
            height,
            strokeWidth,
            style.color
        )
        drawBracket(
            LatexNode.Matrix.MatrixType.PAREN,
            Side.RIGHT,
            x + width - bracketWidth,
            y,
            bracketWidth,
            height,
            strokeWidth,
            style.color
        )

        // 内容
        val numX = x + bracketWidth + (contentWidth - numLayout.width) / 2
        val denX = x + bracketWidth + (contentWidth - denLayout.width) / 2

        numLayout.draw(this, numX, y)
        denLayout.draw(this, denX, y + numLayout.height + gap)
    }
}
