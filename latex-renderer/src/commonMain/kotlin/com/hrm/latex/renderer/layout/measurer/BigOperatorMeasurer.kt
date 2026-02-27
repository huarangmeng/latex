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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.model.toLimitStyle
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.InkBoundsEstimator
import com.hrm.latex.renderer.utils.InkFontCategory
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.mapBigOp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 大型运算符测量器 — 处理 \sum, \int, \prod 等
 *
 * 支持两种模式：
 * 1. 侧边模式 (Side): 上下标在符号右侧（积分符号始终使用）
 * 2. 上下模式 (Display): 上下标在符号正上方/正下方（求和等在 DISPLAY 模式使用）
 *
 * 使用 MathConstants 集中管理所有排版参数。
 * 使用 FontResolver.compensatedFontWeight() 补偿字号放大导致的笔画变粗。
 */
internal class BigOperatorMeasurer : NodeMeasurer<LatexNode.BigOperator> {

    override fun measure(
        node: LatexNode.BigOperator,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val symbol = mapBigOp(node.operator)
        val isIntegral = node.operator.contains("int")
        val isNamedOperator = symbol == node.operator && symbol.all { it.isLetter() }

        // KaTeX 字体使用标准 Unicode 编码，直接使用 Unicode 字符渲染
        val renderSymbol = symbol

        val useSideMode = resolveLimitsMode(node, isIntegral, isNamedOperator, context)
        val scaleFactor = resolveScaleFactor(context, useSideMode, isIntegral)

        val opStyle = buildOperatorStyle(context, isNamedOperator, scaleFactor)
        val limitStyle = context.toLimitStyle()

        // ── 积分混合拉伸策略 ──
        // 目标：将总高度拉伸分解为 fontSize 均匀放大 + 剩余垂直 Canvas scale
        // fontSize 放大保持自然宽高比和笔画粗细，Canvas scale 仅处理剩余部分
        // 这样非均匀分量很小，避免水平压缩感；同时笔画不会过粗

        // 1. 先用基础字号测量，得到基础墨水高度
        val baseOpResult = measurer.measure(AnnotatedString(renderSymbol), opStyle.textStyle())
        val baseFontCategory = when {
            isNamedOperator -> InkFontCategory.TEXT
            else -> InkFontCategory.EXTENSION
        }
        val baseFontBytes = when {
            isNamedOperator -> null
            else -> context.fontBytesCache?.mainBytes  // KaTeX_Main 包含大型运算符
        }
        val baseFontSizePx = with(density) { opStyle.fontSize.toPx() }
        val baseFontWeightVal = opStyle.fontWeight?.weight ?: 400

        val baseInkBounds = if (baseFontBytes != null) {
            InkBoundsEstimator.measurePrecise(
                text = renderSymbol,
                fontSizePx = baseFontSizePx,
                fontBytes = baseFontBytes,
                baseline = baseOpResult.firstBaseline,
                fontWeightValue = baseFontWeightVal
            ) ?: InkBoundsEstimator.estimate(baseOpResult, baseFontCategory)
        } else {
            InkBoundsEstimator.estimate(baseOpResult, baseFontCategory)
        }

        // 2. 计算总需要的垂直拉伸倍数（相对于基础墨水高度）
        var totalVerticalScale = 1.0f
        if (isIntegral && context.mathStyle == MathStyle.DISPLAY) {
            if (context.bigOpHeightHint != null) {
                val targetHeight =
                    context.bigOpHeightHint * MathConstants.INTEGRAL_HEIGHT_HINT_OVERSHOOT
                val currentInkHeight = baseInkBounds.inkHeight
                if (currentInkHeight > 0f && targetHeight > currentInkHeight) {
                    totalVerticalScale = targetHeight / currentInkHeight
                }
            }
            totalVerticalScale =
                totalVerticalScale.coerceAtLeast(MathConstants.INTEGRAL_MIN_VERTICAL_SCALE)
        }

        // 3. 混合分解：fontSize 均匀放大 + 剩余垂直 scale
        //    fontScaleUp = totalVerticalScale ^ ratio  （均匀放大部分）
        //    remainingVerticalScale = totalVerticalScale / fontScaleUp （剩余垂直拉伸）
        //    ratio=0.5 时，两者均为 sqrt(totalVerticalScale)
        val fontScaleUp: Float
        val verticalScale: Float
        if (totalVerticalScale > 1.0f && isIntegral) {
            val ratio = MathConstants.INTEGRAL_FONT_SCALE_RATIO
            fontScaleUp = totalVerticalScale.toDouble().pow(ratio.toDouble()).toFloat()
            verticalScale = totalVerticalScale / fontScaleUp
        } else {
            fontScaleUp = 1.0f
            verticalScale = 1.0f
        }

        // 4. 用放大后的 fontSize 重新测量
        val finalOpStyle = if (fontScaleUp > 1.0f) {
            val weight = FontResolver.compensatedFontWeight(
                MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT,
                scaleFactor * fontScaleUp
            )
            opStyle.grow(fontScaleUp).copy(fontWeight = weight)
        } else {
            opStyle
        }
        val opResult = measurer.measure(AnnotatedString(renderSymbol), finalOpStyle.textStyle())

        // 5. 重新测量放大后的墨水边界
        val finalFontSizePx = with(density) { finalOpStyle.fontSize.toPx() }
        val finalFontWeightVal = finalOpStyle.fontWeight?.weight ?: 400

        val finalInkBounds = if (baseFontBytes != null) {
            InkBoundsEstimator.measurePrecise(
                text = renderSymbol,
                fontSizePx = finalFontSizePx,
                fontBytes = baseFontBytes,
                baseline = opResult.firstBaseline,
                fontWeightValue = finalFontWeightVal
            ) ?: InkBoundsEstimator.estimate(opResult, baseFontCategory)
        } else {
            InkBoundsEstimator.estimate(opResult, baseFontCategory)
        }

        val opWidth = opResult.size.width.toFloat()

        // 应用剩余垂直拉伸到墨水尺寸
        // verticalScale 现在仅是 sqrt(totalScale) 级别，非均匀分量很小
        val opInkHeight = finalInkBounds.inkHeight * verticalScale
        val opInkTopOffset = finalInkBounds.inkTopOffset
        val opInkBaseline = finalInkBounds.inkBaseline * verticalScale

        val opLayout = NodeLayout(opWidth, opInkHeight, opInkBaseline) { x, y ->
            if (verticalScale > 1.0f) {
                // 剩余垂直拉伸：非均匀分量很小（~sqrt 级别），水平压缩感轻微
                val scaledInkCenter = y + opInkHeight / 2f
                withTransform({
                    scale(1.0f, verticalScale, pivot = Offset(x + opWidth / 2f, scaledInkCenter))
                }) {
                    val textY = scaledInkCenter - finalInkBounds.inkHeight / 2f - opInkTopOffset
                    drawText(opResult, topLeft = Offset(x, textY))
                }
            } else {
                drawText(opResult, topLeft = Offset(x, y - opInkTopOffset))
            }
        }

        val superLayout = node.superscript?.let { measureGroup(listOf(it), limitStyle) }
        val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle) }

        val fontSizePx = with(density) { finalOpStyle.fontSize.toPx() }
        // opLayout 已经是墨水高度了，直接使用
        val opVisualHeight = if (isNamedOperator) {
            min(fontSizePx * MathConstants.BIG_OP_NAMED_VISUAL_HEIGHT, opLayout.height)
        } else {
            opLayout.height
        }

        return if (useSideMode) {
            layoutSideMode(
                context, density, measurer, opLayout, superLayout, subLayout,
                isIntegral, isNamedOperator, opVisualHeight, symbol
            )
        } else {
            layoutDisplayMode(
                context, density, measurer, opLayout, superLayout, subLayout,
                isNamedOperator
            )
        }
    }

    private fun resolveLimitsMode(
        node: LatexNode.BigOperator,
        isIntegral: Boolean,
        isNamedOperator: Boolean,
        context: RenderContext
    ): Boolean = when (node.limitsMode) {
        LatexNode.BigOperator.LimitsMode.LIMITS -> false
        LatexNode.BigOperator.LimitsMode.NOLIMITS -> true
        LatexNode.BigOperator.LimitsMode.AUTO -> when {
            isIntegral -> true
            isNamedOperator -> false
            else -> context.mathStyle != MathStyle.DISPLAY
        }
    }

    private fun resolveScaleFactor(
        context: RenderContext,
        useSideMode: Boolean,
        isIntegral: Boolean
    ): Float = when {
        // 积分号使用较小的基础字号，高度通过 verticalScale 纯垂直拉伸实现
        // 这样水平方向笔画保持纤细，不会因字号放大而变粗
        isIntegral && context.mathStyle == MathStyle.DISPLAY -> MathConstants.BIG_OP_INTEGRAL_DISPLAY_SCALE
        context.mathStyle == MathStyle.DISPLAY -> MathConstants.BIG_OP_DISPLAY_SCALE
        useSideMode -> MathConstants.BIG_OP_INLINE_SCALE
        else -> MathConstants.BIG_OP_DEFAULT_SCALE
    }

    private fun buildOperatorStyle(
        context: RenderContext,
        isNamedOperator: Boolean,
        scaleFactor: Float
    ): RenderContext {
        return if (!isNamedOperator) {
            val weight = FontResolver.compensatedFontWeight(
                MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT,
                scaleFactor
            )
            // KaTeX 字体：大型运算符 (∑, ∫ 等) 均在 KaTeX_Main 中，使用标准 Unicode
            val fontFamily = context.fontFamilies?.main ?: context.fontFamily
            context.grow(scaleFactor).copy(
                fontFamily = fontFamily,
                fontStyle = FontStyle.Normal,
                fontWeight = weight
            )
        } else {
            val weight = FontResolver.compensatedFontWeight(
                MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT,
                scaleFactor
            )
            // 命名运算符 (\lim, \sin 等) 使用 main 字体正体渲染
            // context.fontFamily 默认已是 fontFamilies.main（在 toContext() 中设置）
            val fontFamily = context.fontFamilies?.main ?: context.fontFamily
            context.grow(scaleFactor).copy(
                fontFamily = fontFamily,
                fontStyle = FontStyle.Normal,
                fontWeight = weight
            )
        }
    }

    private fun layoutSideMode(
        context: RenderContext, density: Density, measurer: TextMeasurer,
        opLayout: NodeLayout, superLayout: NodeLayout?, subLayout: NodeLayout?,
        isIntegral: Boolean, isNamedOperator: Boolean, opVisualHeight: Float, symbol: String
    ): NodeLayout {
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val fontSizePx = with(density) { context.fontSize.toPx() }

        val opVisualWidth = when {
            isIntegral -> fontSizePx * MathConstants.INTEGRAL_VISUAL_WIDTH
            isNamedOperator -> fontSizePx * symbol.length * MathConstants.NAMED_OP_CHAR_WIDTH
            else -> opLayout.width
        }

        val opDrawX = if (isIntegral || isNamedOperator) max(
            0f,
            (opVisualWidth - opLayout.width) / 2f
        ) else 0f
        val opActualLeft = if (opDrawX == 0f) opLayout.width else opVisualWidth

        // opLayout 已经是墨水高度，不需要额外估算
        val glyphVisualPart = opLayout.height

        // 积分符号定位策略：
        // 用原始（未拉伸）高度以数学轴为中心确定基准位置
        val opCenter = -axisHeight  // 数学轴 y 坐标

        // opTop/opBottom 用于非积分运算符和边界计算
        val opTop = opCenter - opVisualHeight / 2f
        val opBottom = opCenter + opVisualHeight / 2f

        // 拉伸后的 glyph 绘制位置：以数学轴为中心上下扩展
        val opGlyphDrawY = opCenter - glyphVisualPart / 2f

        val limitSpacing = when {
            isIntegral -> 0f
            isNamedOperator -> fontSizePx * MathConstants.NAMED_OP_SIDE_LIMIT_GAP
            else -> with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }
        }

        // 积分上标需要额外右移，避免与积分号顶部弯钩重叠
        val integralSuperKern = if (isIntegral) fontSizePx * 0.15f else 0f
        val superX = opActualLeft + limitSpacing + integralSuperKern
        val subX = if (isIntegral) {
            opActualLeft + limitSpacing - fontSizePx * MathConstants.INTEGRAL_SUBSCRIPT_INSET
        } else opActualLeft + limitSpacing

        val limitGap = when {
            isIntegral -> 0f
            isNamedOperator -> fontSizePx * MathConstants.NAMED_OP_LIMIT_GAP * 2.5f
            else -> fontSizePx * MathConstants.SYMBOL_OP_LIMIT_GAP
        }

        // 积分上下标定位：基于拉伸后的 glyph 墨水区域
        val superTop = if (superLayout != null) {
            if (isIntegral) {
                // 上标顶部与积分号墨水顶部对齐
                opGlyphDrawY
            } else {
                opTop - superLayout.height - limitGap
            }
        } else opTop

        val subTop = if (subLayout != null) {
            if (isIntegral) {
                // 下标 baseline 与拉伸后积分符号墨水底部对齐
                val glyphInkBottom = opGlyphDrawY + glyphVisualPart
                glyphInkBottom - subLayout.baseline * MathConstants.INTEGRAL_SUBSCRIPT_OVERLAP
            } else {
                opBottom + limitGap
            }
        } else opBottom

        // 边界计算：取所有元素的最小顶部和最大底部
        val glyphTop = opGlyphDrawY
        val glyphBottom = opGlyphDrawY + glyphVisualPart
        val minTop = minOf(
            if (superLayout != null) superTop else opTop,
            glyphTop
        )
        val maxBottom = maxOf(
            if (subLayout != null) subTop + subLayout.height else opBottom,
            glyphBottom
        )
        val totalHeight = maxBottom - minTop
        val baseline = -minTop

        val superRightEdge = superX + (superLayout?.width ?: 0f)
        val subRightEdge = subX + (subLayout?.width ?: 0f)
        val width = max(
            opActualLeft * MathConstants.BIG_OP_WIDTH_OVERFLOW_FACTOR,
            max(superRightEdge, subRightEdge)
        )

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            opLayout.draw(this, x + opDrawX, y + baseline + opGlyphDrawY)
            superLayout?.draw(this, x + superX, y + baseline + superTop)
            subLayout?.draw(this, x + subX, y + baseline + subTop)
        }
    }

    private fun layoutDisplayMode(
        context: RenderContext, density: Density, measurer: TextMeasurer,
        opLayout: NodeLayout, superLayout: NodeLayout?, subLayout: NodeLayout?,
        isNamedOperator: Boolean
    ): NodeLayout {
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val fontSizePx = with(density) { context.fontSize.toPx() }

        val spacing = if (isNamedOperator) {
            fontSizePx * MathConstants.NAMED_OP_LIMIT_GAP
        } else {
            fontSizePx * MathConstants.SYMBOL_OP_LIMIT_GAP
        }

        val maxWidth = max(opLayout.width, max(superLayout?.width ?: 0f, subLayout?.width ?: 0f))

        // opLayout 已经是墨水高度，不需要额外估算
        val glyphVisualPart = opLayout.height

        // 上限实际底部：使用 height（完整绘制区域），避免 descent 部分与运算符重叠
        val superBottom = superLayout?.height ?: 0f

        // 布局坐标 (y=0 = NodeLayout 顶部):
        val superDrawY = 0f
        val opDrawY = superBottom + spacing
        val subDrawY = opDrawY + glyphVisualPart + spacing
        val totalHeight = subDrawY + (subLayout?.height ?: 0f)

        // baseline：运算符视觉中心 + 数学轴偏移
        val opVisualCenter = opDrawY + glyphVisualPart / 2f
        val baseline = opVisualCenter + axisHeight

        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            opLayout.draw(this, x + (maxWidth - opLayout.width) / 2, y + opDrawY)
            superLayout?.draw(this, x + (maxWidth - superLayout.width) / 2, y + superDrawY)
            subLayout?.draw(this, x + (maxWidth - subLayout.width) / 2, y + subDrawY)
        }
    }
}
