package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.base.LatexConstants
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.LayoutUtils
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
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Fraction -> measureFraction(node, context, measurer, density, measureGroup)
            is LatexNode.Root -> measureRoot(node, context, measurer, density, measureGroup)
            is LatexNode.Superscript -> measureScript(node, context, measurer, density, measureGlobal, isSuper = true)
            is LatexNode.Subscript -> measureScript(node, context, measurer, density, measureGlobal, isSuper = false)
            is LatexNode.BigOperator -> measureBigOperator(node, context, measurer, density, measureGroup)
            is LatexNode.Binomial -> measureBinomial(node, context, measurer, density, measureGroup)
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
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val childStyle = context.shrink(LatexConstants.FRACTION_SCALE_FACTOR)
        val numeratorLayout = measureGroup(listOf(node.numerator), childStyle)
        val denominatorLayout = measureGroup(listOf(node.denominator), childStyle)

        val ruleThickness = with(density) {
            (context.fontSize * LatexConstants.FRACTION_RULE_THICKNESS_RATIO).toPx()
        }

        val gap = with(density) {
            (context.fontSize * LatexConstants.FRACTION_TOP_PADDING_RATIO).toPx()
        }

        val width = max(numeratorLayout.width, denominatorLayout.width) + gap
        val padding = gap / 2

        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)

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
                color = context.color,
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
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val indexStyle = context.shrink(LatexConstants.ROOT_INDEX_SCALE_FACTOR)

        val contentLayout = measureGroup(listOf(node.content), context)
        val indexLayout = node.index?.let { measureGroup(listOf(it), indexStyle) }

        val ruleThickness = with(density) {
            (context.fontSize * LatexConstants.FRACTION_RULE_THICKNESS_RATIO).toPx()
        }
        val gap = ruleThickness * 2
        val extraTop = gap + ruleThickness

        val hookWidth = with(density) {
            (context.fontSize * LatexConstants.ROOT_HOOK_WIDTH_RATIO).toPx()
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
                context.color, Offset(x + contentX, topY), Offset(x + width, topY), ruleThickness
            )

            // 绘制 V 形钩子
            val p = Path()
            p.moveTo(x + contentX, topY)
            p.lineTo(x + contentX - hookWidth * 0.4f, bottomY)
            p.lineTo(x + contentX - hookWidth * 0.8f, midY + ruleThickness)
            p.lineTo(x + contentX - hookWidth, midY + ruleThickness * 2)

            drawPath(p, context.color, style = Stroke(ruleThickness))
        }
    }

    /**
     * 测量上标/下标 (a^b, a_b)
     *
     * @param isSuper true 为上标，false 为下标
     */
    private fun measureScript(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        isSuper: Boolean
    ): NodeLayout {
        val baseNode = if (isSuper) (node as LatexNode.Superscript).base else (node as LatexNode.Subscript).base
        val scriptNode = if (isSuper) (node as LatexNode.Superscript).exponent else (node as LatexNode.Subscript).index

        val scriptStyle = context.shrink(LatexConstants.SCRIPT_SCALE_FACTOR)

        val baseLayout = measureGlobal(baseNode, context)
        val scriptLayout = measureGlobal(scriptNode, scriptStyle)

        val superscriptShift = with(density) {
            (context.fontSize * LatexConstants.SUPERSCRIPT_SHIFT_RATIO).toPx()
        }
        val subscriptShift = with(density) {
            (context.fontSize * LatexConstants.SUBSCRIPT_SHIFT_RATIO).toPx()
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
        // 修复：宽度应该包含 scriptX 的偏移量
        val width = scriptX + scriptLayout.width

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            baseLayout.draw(this, x, y + baseline - baseLayout.baseline)
            scriptLayout.draw(this, x + scriptX, y + baseline + scriptRelY - scriptLayout.baseline)
        }
    }

    /**
     * 测量大型运算符 (\sum, \int, \prod)
     *
     * 支持两种模式：
     * 1. **行内模式 (Inline/Side)** ：上下标显示在符号右侧（类似普通上下标）。
     * 2. **显示模式 (Display)**：上下标显示在符号正上方和正下方。
     *
     * 模式选择逻辑：
     * - 积分符号 (\int) 始终使用侧边模式。
     * - 求和/乘积符号 (\sum, \prod)：仅在 DISPLAY 模式下使用上下模式。
     */
    private fun measureBigOperator(
        node: LatexNode.BigOperator,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val symbol = mapBigOp(node.operator)
        val isIntegral = node.operator.contains("int")

        // 识别文本类型的算子（如 det, lim, max 等），这些算子应该使用正体（Roman）
        val isNamedOperator = symbol == node.operator && symbol.all { it.isLetter() }

        // 判断是否使用侧边模式（上下标在右侧）：
        // 1. 强制设置了 NOLIMITS
        // 2. 设置了 AUTO：
        //    - 积分符号 (\int) 默认使用侧边模式
        //    - 其他大型运算符 (\sum, \prod 等) 在非 DISPLAY 模式下使用侧边模式
        // 注意：强制设置了 LIMITS 则始终不使用侧边模式
        val useSideMode = when (node.limitsMode) {
            LatexNode.BigOperator.LimitsMode.LIMITS -> false
            LatexNode.BigOperator.LimitsMode.NOLIMITS -> true
            LatexNode.BigOperator.LimitsMode.AUTO -> {
                isIntegral || context.mathStyle != RenderContext.MathStyleMode.DISPLAY
            }
        }

        // 根据模式调整运算符缩放
        var scaleFactor = when {
            context.mathStyle == RenderContext.MathStyleMode.DISPLAY -> {
                if (isIntegral) 1.5f else 1.6f // 增大积分在 display 模式下的基础大小
            }

            useSideMode -> if (isIntegral) 1.2f else 1.0f // 即使是侧边模式，积分也应该稍微大一点
            else -> 1.3f
        }

        val opStyle = context.grow(scaleFactor).let {
            if (isNamedOperator) it.copy(fontStyle = FontStyle.Normal) else it
        }
        val limitStyle = context.shrink(LatexConstants.OPERATOR_LIMIT_SCALE_FACTOR)

        // 测量运算符符号
        val textStyle = opStyle.textStyle()
        val opResult = measurer.measure(AnnotatedString(symbol), textStyle)

        // 如果存在高度暗示且是积分符号，通过垂直拉伸（stretching）来匹配高度
        var verticalScale = 1.0f
        if (isIntegral && context.bigOpHeightHint != null && context.mathStyle == RenderContext.MathStyleMode.DISPLAY) {
            val targetHeight = context.bigOpHeightHint * 1.05f
            val currentHeight = opResult.size.height.toFloat()
            if (targetHeight > currentHeight) {
                verticalScale = targetHeight / currentHeight
            }
        }

        // 积分符号保持原本比例，高度通过垂直拉伸实现，笔画宽度由 scaleFactor (1.2x) 决定
        val opWidth = opResult.size.width.toFloat()
        val opHeight = opResult.size.height.toFloat() * verticalScale

        val opLayout = NodeLayout(
            opWidth,
            opHeight,
            opResult.firstBaseline * verticalScale
        ) { x, y ->
            if (verticalScale != 1.0f) {
                withTransform({
                    // 仅在垂直方向拉伸
                    scale(1.0f, verticalScale, pivot = Offset(x + opWidth / 2f, y + opHeight / 2f))
                }) {
                    drawText(opResult, topLeft = Offset(x, y + (opHeight - opResult.size.height) / 2f))
                }
            } else {
                drawText(opResult, topLeft = Offset(x, y))
            }
        }

        val superLayout = node.superscript?.let { measureGroup(listOf(it), limitStyle) }
        val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle) }

        if (useSideMode) {
            // ============ 步骤1: 计算积分符号的位置和尺寸 ============
            val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
            
            // 积分符号相对整体基线的位置（垂直居中于数学轴）
            val opRelBase = -axisHeight - opLayout.height / 2f
            val opTop = opRelBase  // 积分符号顶部相对基线
            val opBottom = opRelBase + opHeight  // 积分符号底部相对基线
            
            // 积分符号的实际绘制位置（x坐标）
            // 对于积分符号，由于 glyph 包含大量右侧空白，我们定义一个较窄的"视觉核心区"
            val opVisualWidth = if (isIntegral) {
                with(density) { (context.fontSize * 0.35f).toPx() }
            } else {
                opLayout.width
            }
            
            // 积分符号绘制时的左偏移（居中于视觉核心区）
            val opDrawX = if (isIntegral) {
                max(0f, (opVisualWidth - opLayout.width) / 2f)
            } else {
                0f
            }
            
            // 积分符号实际占用的左侧宽度
            val opActualLeft = if (opDrawX == 0f) opLayout.width else opVisualWidth
            
            // ============ 步骤2: 基于积分符号的实际位置，计算上下限的位置 ============
            
            // 积分符号在布局中的实际绘制区域（考虑 opDrawX 偏移后）
            // 积分符号的视觉右边缘位置
            val opVisualRight = opActualLeft
            
            // 上下限与积分符号之间的水平间距
            val limitSpacing = if (isIntegral) {
                with(density) { (context.fontSize * 0.08f).toPx() }
            } else {
                with(density) { 1.dp.toPx() }
            }
            
            // 计算上下限的水平位置
            // 关键：积分符号是倾斜的（从左上到右下），因此：
            // - 上限：在顶部位置，需要相对于积分视觉右边缘偏移
            // - 下限：在底部位置，由于倾斜，实际上已经更靠右，所以偏移量更小
            val superX = if (isIntegral) {
                opVisualRight + limitSpacing
            } else {
                opVisualRight + limitSpacing
            }
            
            val subX = if (isIntegral) {
                // 积分符号倾斜角度约为 15-20 度
                // 从顶部到底部的水平偏移约为 height * tan(angle) ≈ height * 0.3
                // 因此下限需要向左回退以贴近积分符号底部
                opVisualRight + limitSpacing - with(density) { (context.fontSize * 0.20f).toPx() }
            } else {
                opVisualRight + limitSpacing
            }
            
            // ============ 步骤3: 计算上下限的垂直位置（基线对齐） ============
            
            val superRelBase = if (isIntegral) {
                if (superLayout != null) {
                    // 上限文本顶部对齐积分符号顶部
                    // 文本顶部 = 基线 - baseline
                    // 因此：基线 = opTop + baseline
                    opTop + superLayout.baseline
                } else {
                    -opLayout.height * 0.45f - axisHeight
                }
            } else {
                -opLayout.height * 0.45f - axisHeight
            }
            
            val subRelBase = if (isIntegral) {
                if (subLayout != null) {
                    // 下限文本底部对齐积分符号底部
                    // 文本底部 = 基线 + (height - baseline)
                    // 因此：基线 = opBottom - (height - baseline)
                    opBottom - (subLayout.height - subLayout.baseline)
                } else {
                    opLayout.height * 0.35f - axisHeight
                }
            } else {
                opLayout.height * 0.35f - axisHeight
            }

            // ============ 步骤4: 计算总体布局尺寸 ============
            
            val superTop = if (superLayout != null) superRelBase - superLayout.baseline else opTop
            val subBottom = if (subLayout != null) subRelBase + (subLayout.height - subLayout.baseline) else opBottom

            val maxTop = min(opTop, superTop)
            val maxBottom = max(opBottom, subBottom)

            val totalHeight = maxBottom - maxTop
            val baseline = -maxTop
            
            // 总宽度：积分符号实际宽度 + 上下限超出部分
            val superRightEdge = superX + (superLayout?.width ?: 0f)
            val subRightEdge = subX + (subLayout?.width ?: 0f)
            val width = max(opActualLeft * 1.1f, max(superRightEdge, subRightEdge))

            return NodeLayout(width, totalHeight, baseline) { x, y ->
                opLayout.draw(this, x + opDrawX, y + baseline + opRelBase)
                superLayout?.draw(this, x + superX, y + baseline + superRelBase - superLayout.baseline)
                subLayout?.draw(this, x + subX, y + baseline + subRelBase - subLayout.baseline)
            }
        } else {
            // 显示模式：上下标在正上方和正下方
            val spacing = with(density) { (context.fontSize * LatexConstants.OPERATOR_LIMIT_GAP_RATIO).toPx() }
            val visualOpHeight = opLayout.height * 0.85f
            val opPadding = (opLayout.height - visualOpHeight) / 2

            val maxWidth = max(opLayout.width, max(superLayout?.width ?: 0f, subLayout?.width ?: 0f))

            val opTop = (superLayout?.height ?: 0f) + spacing - opPadding
            val subTop = opTop + visualOpHeight + spacing

            val totalHeight = subTop + (subLayout?.height ?: 0f)
            val baseline = opTop + opLayout.baseline - opPadding

            return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
                opLayout.draw(this, x + (maxWidth - opLayout.width) / 2, y + opTop - opPadding)
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
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val childStyle = context.shrink(0.9f)
        val numLayout = measureGroup(listOf(node.top), childStyle)
        val denLayout = measureGroup(listOf(node.bottom), childStyle)

        val gap = with(density) { (context.fontSize * 0.2f).toPx() }
        val contentWidth = max(numLayout.width, denLayout.width)
        val height = numLayout.height + denLayout.height + gap
        
        // 修正：二项式系数的基准线应与数学轴对齐，而非简单的几何中心
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val center = numLayout.height + gap / 2f
        val baseline = center + axisHeight

        val bracketWidth = with(density) { (context.fontSize * 0.4f).toPx() }
        val strokeWidth = with(density) { (context.fontSize * 0.05f).toPx() }
        val width = contentWidth + bracketWidth * 2

        return NodeLayout(width, height, baseline) { x, y ->
            // 绘制左右括号
            drawBracket(LatexNode.Matrix.MatrixType.PAREN, Side.LEFT, x, y, bracketWidth, height, strokeWidth, context.color)
            drawBracket(LatexNode.Matrix.MatrixType.PAREN, Side.RIGHT, x + width - bracketWidth, y, bracketWidth, height, strokeWidth, context.color)
            
            // 绘制内容
            val numX = x + bracketWidth + (contentWidth - numLayout.width) / 2
            val denX = x + bracketWidth + (contentWidth - denLayout.width) / 2
            numLayout.draw(this, numX, y)
            denLayout.draw(this, denX, y + numLayout.height + gap)
        }
    }
}
