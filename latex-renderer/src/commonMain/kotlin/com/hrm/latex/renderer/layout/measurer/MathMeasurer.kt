package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.base.LatexConstants
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderStyle
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.Side
import com.hrm.latex.renderer.utils.drawBracket
import com.hrm.latex.renderer.utils.mapBigOp
import kotlin.math.max
import kotlin.math.min

/**
 * 数学结构测量器
 *
 * 负责测量复杂的数学结构，如分数、根号、上下标、大型运算符（积分、求和）等。
 * 这些结构通常涉及子节点的相对定位和缩放。
 */
internal class MathMeasurer : NodeMeasurer<LatexNode> {

    override fun measure(
        node: LatexNode,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Fraction -> measureFraction(node, style, measurer, density, measureGroup)
            is LatexNode.Root -> measureRoot(node, style, measurer, density, measureGroup)
            is LatexNode.Superscript -> measureScript(node, style, measurer, density, measureGlobal, isSuper = true)
            is LatexNode.Subscript -> measureScript(node, style, measurer, density, measureGlobal, isSuper = false)
            is LatexNode.BigOperator -> measureBigOperator(node, style, measurer, density, measureGroup)
            is LatexNode.Binomial -> measureBinomial(node, style, measurer, density, measureGroup)
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    /**
     * 测量分数 (\frac{num}{den})
     *
     * 布局逻辑：
     * 1. 分子和分母字体缩小。
     * 2. 计算分数线厚度和间距。
     * 3. 垂直对齐分子、分数线、分母，使其中心对齐。
     * 4. 确定基线，通常位于数学轴上。
     */
    private fun measureFraction(
        node: LatexNode.Fraction,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        val childStyle = style.shrink(LatexConstants.FRACTION_SCALE_FACTOR)
        val numeratorLayout = measureGroup(listOf(node.numerator), childStyle)
        val denominatorLayout = measureGroup(listOf(node.denominator), childStyle)

        val ruleThickness = with(density) {
            (style.fontSize * LatexConstants.FRACTION_RULE_THICKNESS_RATIO).toPx()
        }

        val gap = with(density) {
            (style.fontSize * LatexConstants.FRACTION_TOP_PADDING_RATIO).toPx()
        }

        val width = max(numeratorLayout.width, denominatorLayout.width) + gap
        val padding = gap / 2

        val axisHeight = with(density) {
            (style.fontSize * LatexConstants.MATH_AXIS_HEIGHT_RATIO).toPx()
        }

        val numeratorTop = 0f
        val numeratorBottom = numeratorTop + numeratorLayout.height
        val lineY = numeratorBottom + gap
        val denominatorTop = lineY + ruleThickness + gap

        val height = denominatorTop + denominatorLayout.height
        val baseline = (lineY + ruleThickness / 2f) + axisHeight

        return NodeLayout(width, height, baseline) { x, y ->
            val numeratorX = x + (width - numeratorLayout.width) / 2
            numeratorLayout.draw(this, numeratorX, y + numeratorTop)

            drawLine(
                color = style.color,
                start = Offset(x + padding, y + lineY + ruleThickness / 2),
                end = Offset(x + width - padding, y + lineY + ruleThickness / 2),
                strokeWidth = ruleThickness
            )

            val denominatorX = x + (width - denominatorLayout.width) / 2
            denominatorLayout.draw(this, denominatorX, y + denominatorTop)
        }
    }

    /**
     * 测量根号 (\sqrt[index]{content})
     *
     * 布局逻辑：
     * 1. 测量内容和指数（可选）。
     * 2. 绘制根号符号（V形 + 横线）。
     * 3. 将指数放置在根号左上角的合适位置。
     */
    private fun measureRoot(
        node: LatexNode.Root,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        val indexStyle = style.shrink(LatexConstants.ROOT_INDEX_SCALE_FACTOR)

        val contentLayout = measureGroup(listOf(node.content), style)
        val indexLayout = node.index?.let { measureGroup(listOf(it), indexStyle) }

        val ruleThickness = with(density) {
            (style.fontSize * LatexConstants.FRACTION_RULE_THICKNESS_RATIO).toPx()
        }
        val gap = ruleThickness * 2
        val extraTop = gap + ruleThickness

        val hookWidth = with(density) {
            (style.fontSize * LatexConstants.ROOT_HOOK_WIDTH_RATIO).toPx()
        }

        val indexWidth = indexLayout?.width ?: 0f
        // 调整指数的水平位置，使其稍微嵌入根号钩子中
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
            if (indexLayout != null) {
                val indexY =
                    y + totalHeight * LatexConstants.ROOT_INDEX_OFFSET_RATIO - indexLayout.height
                indexLayout.draw(this, x, indexY)
            }

            contentLayout.draw(this, x + contentX, y + extraTop)

            val topY = y + ruleThickness / 2
            val bottomY = y + totalHeight - ruleThickness
            val midY = y + totalHeight * 0.5f

            // 绘制顶部横线
            drawLine(
                style.color, Offset(x + contentX, topY), Offset(x + width, topY), ruleThickness
            )

            // 绘制 V 形钩子
            val p = Path()
            p.moveTo(x + contentX, topY)
            p.lineTo(x + contentX - hookWidth * 0.4f, bottomY)
            p.lineTo(x + contentX - hookWidth * 0.8f, midY + ruleThickness)
            p.lineTo(x + contentX - hookWidth, midY + ruleThickness * 2)

            drawPath(p, style.color, style = Stroke(ruleThickness))
        }
    }

    /**
     * 测量上标/下标 (a^b, a_b)
     *
     * @param isSuper true 为上标，false 为下标
     */
    private fun measureScript(
        node: LatexNode,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout,
        isSuper: Boolean
    ): NodeLayout {
        val baseNode = if (isSuper) (node as LatexNode.Superscript).base else (node as LatexNode.Subscript).base
        val scriptNode = if (isSuper) (node as LatexNode.Superscript).exponent else (node as LatexNode.Subscript).index

        val scriptStyle = style.shrink(LatexConstants.SCRIPT_SCALE_FACTOR)

        val baseLayout = measureGlobal(baseNode, style)
        val scriptLayout = measureGlobal(scriptNode, scriptStyle)

        val superscriptShift = with(density) {
            (style.fontSize * LatexConstants.SUPERSCRIPT_SHIFT_RATIO).toPx()
        }
        val subscriptShift = with(density) {
            (style.fontSize * LatexConstants.SUBSCRIPT_SHIFT_RATIO).toPx()
        }

        val scriptX = baseLayout.width + with(density) { 1.dp.toPx() }
        val scriptRelY = if (isSuper) -superscriptShift else subscriptShift

        // 计算合成后的总高度和基线
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
            baseLayout.draw(this, x, y + baseline - baseLayout.baseline)
            scriptLayout.draw(this, x + scriptX, y + baseline + scriptRelY - scriptLayout.baseline)
        }
    }

    /**
     * 测量大型运算符 (\sum, \int, \prod)
     *
     * 支持两种模式：
     * 1. **行内模式 (Inline)** / 积分模式：上下标显示在符号右侧。
     * 2. **显示模式 (Display)**：上下标显示在符号正上方和正下方（求和、乘积等）。
     *
     * 当前实现逻辑：
     * - 积分符号 (\int) 始终将极限放在右侧。
     * - 其他符号 (\sum, \prod) 默认将极限放在上下方（模拟 Display 模式）。
     */
    private fun measureBigOperator(
        node: LatexNode.BigOperator,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        val symbol = mapBigOp(node.operator)
        val isIntegral = node.operator.contains("int")

        val opStyle = style.grow(1.5f)
        val limitStyle = style.shrink(0.8f)

        // 测量运算符符号
        val textStyle = opStyle.textStyle()
        val opResult = measurer.measure(AnnotatedString(symbol), textStyle)
        val opLayout = NodeLayout(
            opResult.size.width.toFloat(),
            opResult.size.height.toFloat(),
            opResult.firstBaseline
        ) { x, y ->
            drawText(opResult, topLeft = Offset(x, y))
        }

        val superLayout = node.superscript?.let { measureGroup(listOf(it), limitStyle) }
        val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle) }

        if (isIntegral) {
            // 积分模式：脚本在右侧
            val gap = with(density) { (style.fontSize * 0.1f).toPx() }
            val sUp = opLayout.height * 0.3f
            val sDown = opLayout.height * 0.2f
            val superRelBase = -sUp
            val subRelBase = sDown

            val opTop = -opLayout.baseline
            val opBottom = opLayout.height - opLayout.baseline

            val superTop = if (superLayout != null) superRelBase - superLayout.baseline else 0f
            val subBottom = if (subLayout != null) subRelBase + (subLayout.height - subLayout.baseline) else 0f

            val maxTop = min(opTop, if (superLayout != null) superTop else opTop)
            val maxBottom = max(opBottom, if (subLayout != null) subBottom else opBottom)

            val totalHeight = maxBottom - maxTop
            val baseline = -maxTop
            val scriptWidth = max(superLayout?.width ?: 0f, subLayout?.width ?: 0f)
            val width = opLayout.width + (if (scriptWidth > 0) gap + scriptWidth else 0f)

            return NodeLayout(width, totalHeight, baseline) { x, y ->
                opLayout.draw(this, x, y + baseline - opLayout.baseline)
                val scriptX = x + opLayout.width + gap
                superLayout?.draw(this, scriptX, y + baseline + superRelBase - superLayout.baseline)
                subLayout?.draw(this, scriptX, y + baseline + subRelBase - subLayout.baseline)
            }
        } else {
            // 求和模式：脚本在上下方
            val spacing = with(density) { (style.fontSize * 0.1f).toPx() }
            val maxWidth = max(opLayout.width, max(superLayout?.width ?: 0f, subLayout?.width ?: 0f))
            val opTop = (superLayout?.height ?: 0f) + (if (superLayout != null) spacing else 0f)
            val subTop = opTop + opLayout.height + (if (subLayout != null) spacing else 0f)

            val totalHeight = subTop + (subLayout?.height ?: 0f)
            val baseline = opTop + opLayout.baseline

            return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
                opLayout.draw(this, x + (maxWidth - opLayout.width) / 2, y + opTop)
                superLayout?.draw(this, x + (maxWidth - superLayout.width) / 2, y)
                subLayout?.draw(this, x + (maxWidth - subLayout.width) / 2, y + subTop)
            }
        }
    }

    /**
     * 测量二项式系数 (\binom{n}{k})
     *
     * 类似于分数布局，但没有横线，并且左右包裹圆括号。
     */
    private fun measureBinomial(
        node: LatexNode.Binomial,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        val childStyle = style.shrink(0.9f)
        val numLayout = measureGroup(listOf(node.top), childStyle)
        val denLayout = measureGroup(listOf(node.bottom), childStyle)

        val gap = with(density) { (style.fontSize * 0.2f).toPx() }
        val contentWidth = max(numLayout.width, denLayout.width)
        val height = numLayout.height + denLayout.height + gap
        val baseline = numLayout.height + gap / 2

        val bracketWidth = with(density) { (style.fontSize * 0.4f).toPx() }
        val strokeWidth = with(density) { (style.fontSize * 0.05f).toPx() }
        val width = contentWidth + bracketWidth * 2

        return NodeLayout(width, height, baseline) { x, y ->
            // 绘制左右括号
            drawBracket(LatexNode.Matrix.MatrixType.PAREN, Side.LEFT, x, y, bracketWidth, height, strokeWidth, style.color)
            drawBracket(LatexNode.Matrix.MatrixType.PAREN, Side.RIGHT, x + width - bracketWidth, y, bracketWidth, height, strokeWidth, style.color)
            
            // 绘制内容
            val numX = x + bracketWidth + (contentWidth - numLayout.width) / 2
            val denX = x + bracketWidth + (contentWidth - denLayout.width) / 2
            numLayout.draw(this, numX, y)
            denLayout.draw(this, denX, y + numLayout.height + gap)
        }
    }
}
