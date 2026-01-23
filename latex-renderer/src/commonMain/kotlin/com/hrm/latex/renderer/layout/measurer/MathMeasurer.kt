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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.base.LatexConstants
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.LayoutUtils
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

    /**
     * 获取定界符的渲染上下文（使用 symbol 字体）
     */
    private fun delimiterContext(
        context: RenderContext,
        scale: Float = 1.0f
    ): RenderContext {
        // 根据缩放比例动态调整 fontWeight (100-400)
        // 更激进的调整策略,从 1.2x 就开始大幅减轻
        val weight = when {
            scale <= 1.0f -> 400  // 正常大小
            scale >= 2.0f -> 100  // 较高的括号就使用最细
            else -> {
                // 线性插值: scale 从 1.0 到 2.0,weight 从 400 到 100
                val t = (scale - 1.0f) / 1.0f
                (400 - t * 300).toInt().coerceIn(100, 400)
            }
        }

        val fontWeight = FontWeight(weight)

        return context.copy(
            fontStyle = FontStyle.Normal,
            fontFamily = context.fontFamilies?.symbol ?: context.fontFamily,
            fontWeight = fontWeight
        )
    }

    /**
     * 测量并缩放定界符
     */
    private fun measureDelimiterScaled(
        delimiter: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float
    ): NodeLayout {
        // 先用默认 weight 测量获取基础高度
        val baseContext = delimiterContext(context)
        val baseStyle = baseContext.textStyle()
        val baseResult = measurer.measure(AnnotatedString(delimiter), baseStyle)

        if (baseResult.size.height <= 0f || targetHeight <= 0f) {
            return NodeLayout(
                baseResult.size.width.toFloat(),
                baseResult.size.height.toFloat(),
                baseResult.firstBaseline
            ) { x, y ->
                drawText(baseResult, topLeft = Offset(x, y))
            }
        }

        val scale = targetHeight / baseResult.size.height

        // 根据缩放比例调整 fontSize 和 fontWeight
        val adjustedContext = delimiterContext(context, scale).copy(
            fontSize = context.fontSize * scale
        )
        val adjustedStyle = adjustedContext.textStyle()
        val adjustedResult = measurer.measure(AnnotatedString(delimiter), adjustedStyle)

        // 直接使用调整后的字体大小,不使用 Canvas scale
        return NodeLayout(
            width = adjustedResult.size.width.toFloat(),
            height = adjustedResult.size.height.toFloat(),
            baseline = adjustedResult.firstBaseline
        ) { x, y ->
            drawText(adjustedResult, topLeft = Offset(x, y))
        }
    }

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
            is LatexNode.Superscript -> measureScript(
                node,
                context,
                measurer,
                density,
                measureGlobal,
                isSuper = true
            )

            is LatexNode.Subscript -> measureScript(
                node,
                context,
                measurer,
                density,
                measureGlobal,
                isSuper = false
            )

            is LatexNode.BigOperator -> measureBigOperator(
                node,
                context,
                measurer,
                density,
                measureGroup
            )

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
        val baseNode =
            if (isSuper) (node as LatexNode.Superscript).base else (node as LatexNode.Subscript).base
        val scriptNode =
            if (isSuper) (node as LatexNode.Superscript).exponent else (node as LatexNode.Subscript).index

        // 检测同时存在上下标的情况（化学式等）
        // 例如: Ba^{2+}_{3} 会被解析为 Subscript(Superscript(Ba, 2+), 3)
        // 在这种情况下，上下标应该对齐在原始 base 的右侧
        val hasBothScripts = when {
            isSuper && baseNode is LatexNode.Subscript -> true  // 当前是上标，base 是下标
            !isSuper && baseNode is LatexNode.Superscript -> true  // 当前是下标，base 是上标
            else -> false
        }

        if (hasBothScripts) {
            // 提取真正的原始 base 和另一个脚本
            val (realBase, otherScriptNode) = if (isSuper) {
                // 当前是上标，base 是下标节点
                val subNode = baseNode as LatexNode.Subscript
                Pair(subNode.base, subNode.index)
            } else {
                // 当前是下标，base 是上标节点
                val superNode = baseNode as LatexNode.Superscript
                Pair(superNode.base, superNode.exponent)
            }

            // 测量布局
            val scriptStyle = context.shrink(LatexConstants.SCRIPT_SCALE_FACTOR)
            val realBaseLayout = measureGlobal(realBase, context)
            val currentScriptLayout = measureGlobal(scriptNode, scriptStyle)
            val otherScriptLayout = measureGlobal(otherScriptNode, scriptStyle)

            val superscriptShift = with(density) {
                (context.fontSize * LatexConstants.SUPERSCRIPT_SHIFT_RATIO).toPx()
            }
            val subscriptShift = with(density) {
                (context.fontSize * LatexConstants.SUBSCRIPT_SHIFT_RATIO).toPx()
            }

            // 上下标都从原始 base 右侧开始，水平对齐
            val scriptX = realBaseLayout.width + with(density) { 1.dp.toPx() }

            // 上标和下标的垂直位置
            val superLayout = if (isSuper) currentScriptLayout else otherScriptLayout
            val subLayout = if (isSuper) otherScriptLayout else currentScriptLayout
            val superRelY = -superscriptShift
            val subRelY = subscriptShift

            // 计算总体尺寸
            val superTopRel = superRelY - superLayout.baseline
            val superBottomRel = superRelY + (superLayout.height - superLayout.baseline)
            val subTopRel = subRelY - subLayout.baseline
            val subBottomRel = subRelY + (subLayout.height - subLayout.baseline)
            val baseTopRel = -realBaseLayout.baseline
            val baseBottomRel = realBaseLayout.height - realBaseLayout.baseline

            val maxTopRel = minOf(superTopRel, subTopRel, baseTopRel)
            val maxBottomRel = maxOf(superBottomRel, subBottomRel, baseBottomRel)

            val totalHeight = maxBottomRel - maxTopRel
            val baseline = -maxTopRel
            val width = scriptX + maxOf(superLayout.width, subLayout.width)

            return NodeLayout(width, totalHeight, baseline) { x, y ->
                realBaseLayout.draw(this, x, y + baseline - realBaseLayout.baseline)
                superLayout.draw(this, x + scriptX, y + baseline + superRelY - superLayout.baseline)
                subLayout.draw(this, x + scriptX, y + baseline + subRelY - subLayout.baseline)
            }
        }

        // 普通情况：只有单个上标或下标
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
        // 1. 强制设置了 NOLIMITS：使用侧边模式
        // 2. 强制设置了 LIMITS：使用上下模式
        // 3. AUTO 模式（默认）：
        //    - 积分符号 (\int)：始终使用侧边模式
        //    - 命名运算符 (lim, max, min 等)：始终使用上下模式（limits）
        //    - 其他符号运算符 (\sum, \prod 等)：仅在 DISPLAY 模式下使用上下模式
        val useSideMode = when (node.limitsMode) {
            LatexNode.BigOperator.LimitsMode.LIMITS -> false
            LatexNode.BigOperator.LimitsMode.NOLIMITS -> true
            LatexNode.BigOperator.LimitsMode.AUTO -> {
                when {
                    isIntegral -> true  // 积分始终侧边
                    isNamedOperator -> false  // 命名运算符始终上下
                    else -> context.mathStyle != RenderContext.MathStyleMode.DISPLAY  // 其他符号看模式
                }
            }
        }

        // 根据模式调整运算符缩放
        // 统一缩放因子以确保符号笔画粗细一致
        var scaleFactor = when {
            context.mathStyle == RenderContext.MathStyleMode.DISPLAY -> {
                1.5f  // Display 模式：所有大型运算符统一使用 1.5x
            }

            useSideMode -> {
                1.2f  // Side 模式（行内）：所有大型运算符统一使用 1.2x
            }

            else -> 1.3f
        }

        // 根据缩放因子动态调整 fontWeight,防止笔画变粗
        // 对所有大型运算符应用此调整
        val baseOpStyle = if (!isNamedOperator) {
            // 符号运算符(∑∫∏等): 较激进的 fontWeight 调整
            val weight = when {
                scaleFactor <= 1.0f -> 400  // 正常或更小
                scaleFactor >= 2.0f -> 100  // 很大,用最细
                else -> {
                    // 线性插值: scaleFactor 从 1.0 到 2.0,weight 从 400 到 100
                    val t = (scaleFactor - 1.0f) / 1.0f
                    (400 - t * 300).toInt().coerceIn(100, 400)
                }
            }
            context.grow(scaleFactor).copy(
                fontFamily = context.fontFamilies?.extension ?: context.fontFamily,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight(weight)
            )
        } else {
            // 命名运算符(lim, max, det等): 温和的 fontWeight 调整
            val weight = when {
                scaleFactor <= 1.0f -> 400  // 正常或更小
                scaleFactor >= 1.5f -> 300  // 较大时略微减轻
                else -> {
                    // 线性插值: scaleFactor 从 1.0 到 1.5,weight 从 400 到 300
                    val t = (scaleFactor - 1.0f) / 0.5f
                    (400 - t * 100).toInt().coerceIn(300, 400)
                }
            }
            context.grow(scaleFactor).copy(
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight(weight)
            )
        }

        val opStyle = baseOpStyle
        val limitStyle = context.shrink(LatexConstants.OPERATOR_LIMIT_SCALE_FACTOR)

        // 测量运算符符号
        val textStyle = opStyle.textStyle()
        val opResult = measurer.measure(AnnotatedString(symbol), textStyle)

        // 如果存在高度暗示且是积分符号,通过垂直拉伸来匹配高度
        var verticalScale = 1.0f
        if (isIntegral && context.bigOpHeightHint != null && context.mathStyle == RenderContext.MathStyleMode.DISPLAY) {
            val targetHeight = context.bigOpHeightHint * 1.05f
            val currentHeight = opResult.size.height.toFloat()
            if (targetHeight > currentHeight) {
                verticalScale = targetHeight / currentHeight
            }
        }

        // 对于额外的垂直拉伸,进一步调整 fontWeight
        val adjustedOpStyle = if (verticalScale > 1.0f) {
            // 获取当前的 fontWeight (已经根据 scaleFactor 调整过)
            val currentWeight = opStyle.fontWeight?.weight ?: 400
            // 根据 verticalScale 进一步减少 weight
            val additionalReduction = when {
                verticalScale >= 2.0f -> 200  // 额外减少200
                else -> {
                    val t = (verticalScale - 1.0f) / 1.0f
                    (t * 200).toInt()
                }
            }
            val finalWeight = (currentWeight - additionalReduction).coerceIn(100, 400)
            opStyle.copy(fontWeight = FontWeight(finalWeight))
        } else {
            opStyle
        }

        // 如果 fontWeight 改变了,重新测量
        val finalOpResult = if (adjustedOpStyle != opStyle) {
            measurer.measure(AnnotatedString(symbol), adjustedOpStyle.textStyle())
        } else {
            opResult
        }

        // 积分符号保持原本比例，高度通过垂直拉伸实现
        val opWidth = finalOpResult.size.width.toFloat()
        val opHeight = finalOpResult.size.height.toFloat() * verticalScale

        val opLayout = NodeLayout(
            opWidth,
            opHeight,
            finalOpResult.firstBaseline * verticalScale
        ) { x, y ->
            if (verticalScale != 1.0f) {
                withTransform({
                    // 仅在垂直方向拉伸
                    scale(1.0f, verticalScale, pivot = Offset(x + opWidth / 2f, y + opHeight / 2f))
                }) {
                    drawText(
                        finalOpResult,
                        topLeft = Offset(x, y + (opHeight - finalOpResult.size.height) / 2f)
                    )
                }
            } else {
                drawText(finalOpResult, topLeft = Offset(x, y))
            }
        }

        val superLayout = node.superscript?.let { measureGroup(listOf(it), limitStyle) }
        val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle) }

        if (useSideMode) {
            // ============ 步骤1: 获取数学轴高度（用于对齐） ============
            val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)

            // ============ 步骤2: 计算运算符的水平位置和视觉宽度 ============

            // 对于某些符号（如积分、命名运算符），glyph 包含大量右侧空白，我们定义一个较窄的"视觉核心区"
            val opVisualWidth = when {
                isIntegral -> with(density) { (context.fontSize * 0.22f).toPx() }  // 积分符号视觉核心区（缩小以让上下标更贴近）
                isNamedOperator -> with(density) { (context.fontSize * symbol.length * 0.45f).toPx() }  // 命名运算符按字符数估算
                else -> opLayout.width
            }

            // 运算符绘制时的左偏移（居中于视觉核心区）
            val opDrawX = if (isIntegral || isNamedOperator) {
                max(0f, (opVisualWidth - opLayout.width) / 2f)
            } else {
                0f
            }

            // 运算符实际占用的左侧宽度
            val opActualLeft = if (opDrawX == 0f) opLayout.width else opVisualWidth

            // ============ 步骤3: 计算积分符号应该垂直居中的目标高度 ============

            // 如果是积分符号且有高度暗示（来自括号内容），使用该高度
            // 否则使用运算符本身的高度
            val targetCenterHeight = if (isIntegral && context.bigOpHeightHint != null) {
                context.bigOpHeightHint
            } else {
                opLayout.height
            }

            // 积分符号应该垂直居中于目标高度（相对于数学轴）
            // 运算符的几何中心应该位于数学轴上
            val opCenterRelAxis = 0f  // 运算符中心对齐数学轴
            val opTop = -axisHeight + opCenterRelAxis - opLayout.height / 2f
            val opBottom = opTop + opLayout.height

            // ============ 步骤4: 计算上下限的水平位置 ============

            // 运算符的视觉右边缘位置
            val opVisualRight = opActualLeft

            // 上下限与积分符号之间的水平间距
            val limitSpacing = if (isIntegral) {
                with(density) { (context.fontSize * 0.0f).toPx() }  // 紧贴积分符号,无额外间距
            } else if (isNamedOperator) {
                with(density) { (context.fontSize * 0.03f).toPx() }  // 命名运算符（如 lim）使用更小的间距
            } else {
                with(density) { 1.dp.toPx() }
            }

            // 计算上下限的水平位置
            // 上标紧贴积分符号视觉右边缘
            val superX = opVisualRight + limitSpacing

            // 下标需要向左偏移,因为积分符号是倾斜的(从左上到右下)
            // 让下标更贴近积分符号底部
            val subX = if (isIntegral) {
                opVisualRight + limitSpacing - with(density) { (context.fontSize * 0.12f).toPx() }
            } else {
                opVisualRight + limitSpacing
            }

            // ============ 步骤5: 计算上下限的垂直位置 ============

            val limitGap = if (isIntegral) {
                with(density) { (context.fontSize * 0.0f).toPx() }  // 积分符号与上下限之间的垂直间距
            } else if (isNamedOperator) {
                with(density) { (context.fontSize * 0.05f).toPx() }  // 命名运算符使用更小的间距
            } else {
                with(density) { (context.fontSize * 0.1f).toPx() }
            }

            val superTop = if (superLayout != null) {
                // 积分符号:上标的顶部与积分符号顶部对齐
                if (isIntegral) {
                    opTop
                } else {
                    // 其他运算符:上限紧贴运算符顶部
                    opTop - superLayout.height - limitGap
                }
            } else {
                opTop
            }

            val subTop = if (subLayout != null) {
                // 积分符号:下标的底部与积分符号底部对齐
                if (isIntegral) {
                    opBottom - subLayout.height
                } else {
                    // 其他运算符:下限紧贴运算符底部
                    opBottom + limitGap
                }
            } else {
                opBottom
            }

            // ============ 步骤6: 计算总体布局尺寸 ============

            val minTop = if (superLayout != null) superTop else opTop
            val maxBottom = if (subLayout != null) subTop + subLayout.height else opBottom

            val totalHeight = maxBottom - minTop
            val baseline = -minTop  // 基线位于顶部

            // 总宽度：积分符号实际宽度 + 上下限超出部分
            val superRightEdge = superX + (superLayout?.width ?: 0f)
            val subRightEdge = subX + (subLayout?.width ?: 0f)
            val width = max(opActualLeft * 1.1f, max(superRightEdge, subRightEdge))

            return NodeLayout(width, totalHeight, baseline) { x, y ->
                // 绘制运算符（相对于数学轴居中）
                opLayout.draw(this, x + opDrawX, y + baseline + opTop)

                // 绘制上下限
                superLayout?.draw(this, x + superX, y + baseline + superTop)
                subLayout?.draw(this, x + subX, y + baseline + subTop)
            }
        } else {
            // 显示模式：上下标在正上方和正下方
            // 命名运算符（如 lim）使用更小的间距
            val spacing = if (isNamedOperator) {
                with(density) { (context.fontSize * 0.02f).toPx() }  // 命名运算符使用极小的间距（0.02em）
            } else {
                with(density) { (context.fontSize * LatexConstants.OPERATOR_LIMIT_GAP_RATIO).toPx() }
            }

            val visualOpHeight = opLayout.height * 0.85f
            val opPadding = (opLayout.height - visualOpHeight) / 2

            val maxWidth =
                max(opLayout.width, max(superLayout?.width ?: 0f, subLayout?.width ?: 0f))

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
        val contentHeight = numLayout.height + denLayout.height + gap

        // 括号高度应该略高于内容,形成包裹感
        val delimiterPadding = with(density) { (context.fontSize * 0.15f).toPx() }
        val delimiterHeight = contentHeight + delimiterPadding * 2

        // 修正：二项式系数的基准线应与数学轴对齐，而非简单的几何中心
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val center = numLayout.height + gap / 2f + delimiterPadding
        val baseline = center + axisHeight

        // 使用字体渲染括号（而不是 Path）
        val leftLayout = measureDelimiterScaled("(", context, measurer, delimiterHeight)
        val rightLayout = measureDelimiterScaled(")", context, measurer, delimiterHeight)

        val width = leftLayout.width + contentWidth + rightLayout.width

        return NodeLayout(width, delimiterHeight, baseline) { x, y ->
            // 内容在括号内垂直居中
            val contentY = y + (delimiterHeight - contentHeight) / 2f

            // 绘制左侧括号
            leftLayout.draw(this, x, y)

            // 绘制内容:垂直居中
            val numX = x + leftLayout.width + (contentWidth - numLayout.width) / 2
            val denX = x + leftLayout.width + (contentWidth - denLayout.width) / 2
            numLayout.draw(this, numX, contentY)
            denLayout.draw(this, denX, contentY + numLayout.height + gap)

            // 绘制右侧括号
            rightLayout.draw(this, x + leftLayout.width + contentWidth, y)
        }
    }
}
