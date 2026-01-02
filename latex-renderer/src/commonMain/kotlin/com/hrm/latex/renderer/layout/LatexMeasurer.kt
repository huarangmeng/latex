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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
    node: LatexNode,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    return when (node) {
        is LatexNode.Text -> measureText(node.content, style, measurer, density)
        is LatexNode.TextMode -> measureText(node.text, style, measurer, density)
        is LatexNode.Symbol -> measureText(
            node.unicode.ifEmpty { node.symbol },
            style,
            measurer,
            density
        )

        is LatexNode.Operator -> measureText(
            node.op,
            style.copy(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif),
            measurer,
            density
        )

        is LatexNode.Command -> measureText(node.name, style, measurer, density)

        is LatexNode.Space -> measureSpace(node.type, style, density)
        is LatexNode.NewLine -> NodeLayout(
            0f,
            lineSpacingPx(style, density),
            0f
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
        is LatexNode.Cases -> measureCases(node, style, measurer, density)
        is LatexNode.Aligned -> measureAligned(node, style, measurer, density)
        is LatexNode.BigOperator -> measureBigOperator(node, style, measurer, density)
        is LatexNode.Accent -> measureAccent(node, style, measurer, density)
        is LatexNode.Binomial -> measureBinomial(node, style, measurer, density)

        is LatexNode.Style -> measureGroup(
            node.content,
            style.applyStyle(node.styleType),
            measurer,
            density
        )

        is LatexNode.Color -> measureGroup(
            node.content,
            style.withColor(node.color),
            measurer,
            density
        )

        is LatexNode.Environment -> measureGroup(node.content, style, measurer, density)
    }
}

/**
 * 测量节点组（处理行内排列和多行）
 */
fun measureGroup(
    nodes: List<LatexNode>,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
    lines: List<List<LatexNode>>,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
    text: String,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    val textStyle = style.textStyle()
    val result: TextLayoutResult = measurer.measure(
        text = AnnotatedString(text),
        style = textStyle
    )

    val width = result.size.width.toFloat()
    val height = result.size.height.toFloat()
    val baseline = result.firstBaseline

    return NodeLayout(width, height, baseline) { x, y ->
        drawText(result, topLeft = Offset(x, y))
    }
}

private fun measureSpace(type: LatexNode.Space.SpaceType, style: RenderStyle, density: Density): NodeLayout {
    val width = spaceWidthPx(style, type, density)
    return NodeLayout(width, 0f, 0f) { _, _ -> }
}

private fun measureFraction(
    node: LatexNode.Fraction,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    val childStyle = style.shrink(0.9f)
    val numLayout = measureGroup(listOf(node.numerator), childStyle, measurer, density)
    val denLayout = measureGroup(listOf(node.denominator), childStyle, measurer, density)

    val ruleThickness = with(density) { (style.fontSize * 0.05f).toPx() }
    val gap = with(density) { (style.fontSize * 0.2f).toPx() }

    val width = max(numLayout.width, denLayout.width) + gap // 水平方向增加一些填充
    val padding = gap / 2

    // 计算垂直位置
    // 轴高度通常约为 0.25em (数学符号的中心)
    val axisHeight = with(density) { (style.fontSize * 0.25f).toPx() }

    val numTop = 0f
    val numBottom = numTop + numLayout.height
    val lineY = numBottom + gap
    val denTop = lineY + ruleThickness + gap

    val height = denTop + denLayout.height
    // 分数线位于 `lineY + ruleThickness/2`
    // 我们希望分数线位于 `baseline - axisHeight`
    // 所以 `baseline = barY + axisHeight`
    val baseline = (lineY + ruleThickness / 2f) + axisHeight

    return NodeLayout(width, height, baseline) { x, y ->
        // 绘制分子 (居中)
        val numX = x + (width - numLayout.width) / 2
        numLayout.draw(this, numX, y + numTop)

        // 绘制分数线
        drawLine(
            color = style.color,
            start = Offset(x + padding, y + lineY + ruleThickness / 2),
            end = Offset(x + width - padding, y + lineY + ruleThickness / 2),
            strokeWidth = ruleThickness
        )

        // 绘制分母 (居中)
        val denX = x + (width - denLayout.width) / 2
        denLayout.draw(this, denX, y + denTop)
    }
}

private fun measureRoot(
    node: LatexNode.Root,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    val contentStyle = style
    val indexStyle = style.shrink(0.6f)

    val contentLayout = measureGroup(listOf(node.content), contentStyle, measurer, density)
    val indexLayout = node.index?.let { measureGroup(listOf(it), indexStyle, measurer, density) }

    val ruleThickness = with(density) { (style.fontSize * 0.05f).toPx() }
    val gap = ruleThickness * 2
    val extraTop = gap + ruleThickness // 线条和内容上方间隙的空间

    // 根号符号几何结构
    // 包含一个勾号部分和一个垂直部分
    // 近似值：勾号宽度 ~ 0.5em，垂直部分取决于高度
    val checkWidth = with(density) { (style.fontSize * 0.6f).toPx() }

    val indexWidth = indexLayout?.width ?: 0f
    val indexShiftX = if (indexLayout != null) indexWidth - checkWidth * 0.5f else 0f

    val contentX = max(checkWidth, indexWidth) + ruleThickness

    val totalHeight = contentLayout.height + extraTop
    val baseline = contentLayout.baseline + extraTop

    val width = contentX + contentLayout.width + ruleThickness // 右侧少量填充

    return NodeLayout(width, totalHeight, baseline) { x, y ->
        // 绘制指数
        if (indexLayout != null) {
            // 指数位于根号勾号的"架子"上
            // 近似位置：左上区域
            indexLayout.draw(this, x, y + totalHeight * 0.6f - indexLayout.height)
        }

        // 绘制内容
        contentLayout.draw(this, x + contentX, y + extraTop)

        // 绘制根号符号
        val topY = y + ruleThickness / 2
        val bottomY = y + totalHeight - ruleThickness
        val midY = y + totalHeight * 0.5f

        // 水平横线
        drawLine(
            style.color,
            Offset(x + contentX, topY),
            Offset(x + width, topY),
            ruleThickness
        )

        // V 形符号
        val p = Path()
        p.moveTo(x + contentX, topY)
        p.lineTo(x + contentX - checkWidth * 0.4f, bottomY) // 向下
        p.lineTo(x + contentX - checkWidth * 0.8f, midY + ruleThickness) // 向左上
        p.lineTo(x + contentX - checkWidth, midY + ruleThickness * 2) // 小尾巴

        drawPath(p, style.color, style = Stroke(ruleThickness))
    }
}

private fun measureScript(
    node: LatexNode,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density,
    isSuper: Boolean
): NodeLayout {
    val baseNode =
        if (isSuper) (node as LatexNode.Superscript).base else (node as LatexNode.Subscript).base
    val scriptNode =
        if (isSuper) (node as LatexNode.Superscript).exponent else (node as LatexNode.Subscript).index

    val scriptStyle = style.shrink(0.75f)

    val baseLayout = measureNode(baseNode, style, measurer, density)
    val scriptLayout = measureNode(scriptNode, scriptStyle, measurer, density)

    // 计算偏移量
    // 上标：上移约 0.45em
    // 下标：下移约 0.25em
    val supShift = with(density) { (style.fontSize * 0.45f).toPx() }
    val subShift = with(density) { (style.fontSize * 0.25f).toPx() }

    val scriptX = baseLayout.width + with(density) { 1.dp.toPx() } // 增加微小水平间距

    // 相对于基准基线
    val scriptRelY = if (isSuper) -supShift else subShift

    // 需要确定总高度和新基线
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
        // 绘制基准
        baseLayout.draw(this, x, y + baseline - baseLayout.baseline)

        // 绘制脚本
        scriptLayout.draw(this, x + scriptX, y + baseline + scriptRelY - scriptLayout.baseline)
    }
}

private fun measureBigOperator(
    node: LatexNode.BigOperator,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    // 大型运算符，带上下限（显示样式）或侧面（行内样式）
    // 目前假设如果有上下标则采用显示样式行为
    val symbol = mapBigOp(node.operator)
    val isIntegral = node.operator.contains("int")

    val opStyle = style.grow(1.5f) // 放大运算符
    val limitStyle = style.shrink(0.8f)

    val opLayout = measureText(symbol, opStyle, measurer, density)
    val superLayout =
        node.superscript?.let { measureGroup(listOf(it), limitStyle, measurer, density) }
    val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle, measurer, density) }

    if (isIntegral) {
        // 积分符号：限制在右侧（脚本样式）
        val supShift = with(density) { (style.fontSize * 0.4f).toPx() }
        val subShift = with(density) { (style.fontSize * 0.2f).toPx() }

        // 确定间距
        val gap =
            with(density) { (style.fontSize * 0.1f).toPx() } // 运算符和脚本之间的微小间隙

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
    node: LatexNode.Matrix,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
    rows: List<List<LatexNode>>,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
    node: LatexNode.Delimited,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
        leftStr,
        style,
        measurer,
        density
    ) else null
    val rightLayout = if (rightType == null && rightStr != ".") measureText(
        rightStr,
        style,
        measurer,
        density
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

private fun measureCases(
    node: LatexNode.Cases,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
    node: LatexNode.Aligned,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    return measureMatrixLike(node.rows, style, measurer, density)
}

private fun measureAccent(
    node: LatexNode.Accent,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
): NodeLayout {
    val contentLayout = measureGroup(listOf(node.content), style, measurer, density)
    val accentChar = when (node.accentType) {
        AccentType.HAT -> "^"
        AccentType.TILDE -> "~"
        AccentType.BAR, AccentType.OVERLINE -> "¯"
        AccentType.UNDERLINE -> "_"
        AccentType.VEC -> "→"
        AccentType.DOT -> "˙"
        AccentType.DDOT -> "¨"
        AccentType.OVERBRACE -> "⏞"
        AccentType.UNDERBRACE -> "⏟"
    }

    // 如果是下划线/下括号，画在下方
    val isUnder =
        node.accentType == AccentType.UNDERLINE || node.accentType == AccentType.UNDERBRACE
    val accentLayout = measureText(accentChar, style.shrink(0.8f), measurer, density)

    val width = max(contentLayout.width, accentLayout.width)
    val totalHeight = contentLayout.height + accentLayout.height

    return NodeLayout(
        width,
        totalHeight,
        contentLayout.baseline + (if (isUnder) 0f else accentLayout.height)
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

private fun measureBinomial(
    node: LatexNode.Binomial,
    style: RenderStyle,
    measurer: TextMeasurer,
    density: Density
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
