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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.toLimitStyle
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.InkBoundsEstimator
import com.hrm.latex.renderer.utils.InkFontCategory
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.mapBigOp
import kotlin.math.max
import kotlin.math.min

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

        val useSideMode = resolveLimitsMode(node, isIntegral, isNamedOperator, context)
        val scaleFactor = resolveScaleFactor(context, useSideMode)

        val opStyle = buildOperatorStyle(context, isNamedOperator, scaleFactor)
        val limitStyle = context.toLimitStyle()

        val opResult = measurer.measure(AnnotatedString(symbol), opStyle.textStyle())

        var verticalScale = 1.0f
        if (isIntegral && context.bigOpHeightHint != null && context.mathStyle == MathStyle.DISPLAY) {
            val targetHeight = context.bigOpHeightHint * MathConstants.INTEGRAL_HEIGHT_HINT_OVERSHOOT
            val currentHeight = opResult.size.height.toFloat()
            if (targetHeight > currentHeight) {
                verticalScale = targetHeight / currentHeight
            }
        }

        val adjustedOpStyle = applyVerticalScaleWeightCompensation(opStyle, verticalScale)
        val finalOpResult = if (adjustedOpStyle != opStyle) {
            measurer.measure(AnnotatedString(symbol), adjustedOpStyle.textStyle())
        } else opResult

        val opWidth = finalOpResult.size.width.toFloat()

        // 使用精确 glyph bounds 测量墨水边界（优先）
        // 通过平台原生 API (Android: Paint.getTextBounds, 其他: Skia Font.getBounds)
        // 获取字形的精确 bounding box，消除行框的 ascent/descent 空白
        val fontCategory = if (isNamedOperator) InkFontCategory.TEXT else InkFontCategory.EXTENSION
        val fontBytes = if (!isNamedOperator) context.fontBytesCache?.extensionBytes else null
        val fontSizePxForMeasure = with(density) { adjustedOpStyle.fontSize.toPx() }
        val fontWeightVal = adjustedOpStyle.fontWeight?.weight ?: 400

        val inkBounds = if (fontBytes != null) {
            // 精确测量：使用平台原生 API
            InkBoundsEstimator.measurePrecise(
                text = symbol,
                fontSizePx = fontSizePxForMeasure,
                fontBytes = fontBytes,
                baseline = finalOpResult.firstBaseline,
                fontWeightValue = fontWeightVal
            ) ?: InkBoundsEstimator.estimate(finalOpResult, fontCategory)
        } else {
            // 回退：字体字节未加载，使用启发式估算
            InkBoundsEstimator.estimate(finalOpResult, fontCategory)
        }

        // 应用垂直拉伸到墨水高度
        val opInkHeight = inkBounds.inkHeight * verticalScale
        val opInkTopOffset = inkBounds.inkTopOffset * verticalScale
        val opInkBaseline = inkBounds.inkBaseline * verticalScale

        val opLayout = NodeLayout(opWidth, opInkHeight, opInkBaseline) { x, y ->
            if (verticalScale != 1.0f) {
                val rawHeight = finalOpResult.size.height.toFloat()
                withTransform({
                    scale(1.0f, verticalScale, pivot = Offset(x + opWidth / 2f, y + opInkHeight / 2f))
                }) {
                    drawText(finalOpResult, topLeft = Offset(x, y - opInkTopOffset / verticalScale))
                }
            } else {
                // 向上偏移 inkTopOffset，将墨水区域对齐到 NodeLayout 的 y=0
                drawText(finalOpResult, topLeft = Offset(x, y - opInkTopOffset))
            }
        }

        val superLayout = node.superscript?.let { measureGroup(listOf(it), limitStyle) }
        val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle) }

        val fontSizePx = with(density) { opStyle.fontSize.toPx() }
        // opLayout 已经是墨水高度了，直接使用
        val opVisualHeight = if (isNamedOperator) {
            min(fontSizePx * MathConstants.BIG_OP_NAMED_VISUAL_HEIGHT, opLayout.height)
        } else {
            opLayout.height
        }

        return if (useSideMode) {
            layoutSideMode(context, density, measurer, opLayout, superLayout, subLayout,
                isIntegral, isNamedOperator, opVisualHeight, symbol)
        } else {
            layoutDisplayMode(context, density, measurer, opLayout, superLayout, subLayout,
                isNamedOperator, opVisualHeight)
        }
    }

    private fun resolveLimitsMode(
        node: LatexNode.BigOperator, isIntegral: Boolean, isNamedOperator: Boolean, context: RenderContext
    ): Boolean = when (node.limitsMode) {
        LatexNode.BigOperator.LimitsMode.LIMITS -> false
        LatexNode.BigOperator.LimitsMode.NOLIMITS -> true
        LatexNode.BigOperator.LimitsMode.AUTO -> when {
            isIntegral -> true
            isNamedOperator -> false
            else -> context.mathStyle != MathStyle.DISPLAY
        }
    }

    private fun resolveScaleFactor(context: RenderContext, useSideMode: Boolean): Float = when {
        context.mathStyle == MathStyle.DISPLAY -> MathConstants.BIG_OP_DISPLAY_SCALE
        useSideMode -> MathConstants.BIG_OP_INLINE_SCALE
        else -> MathConstants.BIG_OP_DEFAULT_SCALE
    }

    private fun buildOperatorStyle(context: RenderContext, isNamedOperator: Boolean, scaleFactor: Float): RenderContext {
        return if (!isNamedOperator) {
            val weight = FontResolver.compensatedFontWeight(MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT, scaleFactor)
            context.grow(scaleFactor).copy(
                fontFamily = context.fontFamilies?.extension ?: context.fontFamily,
                fontStyle = FontStyle.Normal,
                fontWeight = weight
            )
        } else {
            val weight = FontResolver.compensatedFontWeight(MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT, scaleFactor)
            context.grow(scaleFactor).copy(
                fontStyle = FontStyle.Normal,
                fontWeight = weight
            )
        }
    }

    private fun applyVerticalScaleWeightCompensation(opStyle: RenderContext, verticalScale: Float): RenderContext {
        if (verticalScale <= 1.0f) return opStyle
        val currentWeight = opStyle.fontWeight?.weight ?: MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT
        val additionalReduction = when {
            verticalScale >= 2.0f -> MathConstants.BIG_OP_VERTICAL_SCALE_WEIGHT_REDUCTION
            else -> {
                val t = (verticalScale - 1.0f) / 1.0f
                (t * MathConstants.BIG_OP_VERTICAL_SCALE_WEIGHT_REDUCTION).toInt()
            }
        }
        val finalWeight = (currentWeight - additionalReduction).coerceIn(
            MathConstants.BIG_OP_SYMBOL_MIN_WEIGHT, MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT
        )
        return opStyle.copy(fontWeight = FontWeight(finalWeight))
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

        val opDrawX = if (isIntegral || isNamedOperator) max(0f, (opVisualWidth - opLayout.width) / 2f) else 0f
        val opActualLeft = if (opDrawX == 0f) opLayout.width else opVisualWidth

        // opLayout 已经是墨水高度，不需要额外估算
        val glyphVisualPart = opLayout.height

        val opTop = -axisHeight - opVisualHeight / 2f
        val opBottom = opTop + opVisualHeight
        val opGlyphDrawY = opTop + (opVisualHeight - glyphVisualPart) / 2f
        val opVisualRight = opActualLeft

        val limitSpacing = when {
            isIntegral -> 0f
            isNamedOperator -> fontSizePx * MathConstants.NAMED_OP_SIDE_LIMIT_GAP
            else -> with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }
        }

        val superX = opVisualRight + limitSpacing
        val subX = if (isIntegral) {
            opVisualRight + limitSpacing - fontSizePx * MathConstants.INTEGRAL_SUBSCRIPT_INSET
        } else opVisualRight + limitSpacing

        val limitGap = when {
            isIntegral -> 0f
            isNamedOperator -> fontSizePx * MathConstants.NAMED_OP_LIMIT_GAP * 2.5f
            else -> fontSizePx * MathConstants.SYMBOL_OP_LIMIT_GAP
        }

        // 积分上下标定位：基于 glyph 墨水区域
        // opGlyphDrawY 是 glyph 墨水顶部相对于 baseline 的 y 坐标
        // glyph 墨水底部 = opGlyphDrawY + glyphVisualPart
        val superTop = if (superLayout != null) {
            if (isIntegral) {
                // 上标顶部与积分符号墨水顶部对齐
                opGlyphDrawY
            } else {
                opTop - superLayout.height - limitGap
            }
        } else opTop

        val subTop = if (subLayout != null) {
            if (isIntegral) {
                // 下标顶部紧贴积分符号墨水底部（baseline 对齐到底部附近）
                val glyphInkBottom = opGlyphDrawY + glyphVisualPart
                glyphInkBottom - subLayout.baseline * MathConstants.INTEGRAL_SUBSCRIPT_OVERLAP
            } else {
                opBottom + limitGap
            }
        } else opBottom

        val minTop = if (superLayout != null) superTop else opTop
        val maxBottom = if (subLayout != null) subTop + subLayout.height else opBottom
        val totalHeight = maxBottom - minTop
        val baseline = -minTop

        val superRightEdge = superX + (superLayout?.width ?: 0f)
        val subRightEdge = subX + (subLayout?.width ?: 0f)
        val width = max(opActualLeft * MathConstants.BIG_OP_WIDTH_OVERFLOW_FACTOR, max(superRightEdge, subRightEdge))

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            opLayout.draw(this, x + opDrawX, y + baseline + opGlyphDrawY)
            superLayout?.draw(this, x + superX, y + baseline + superTop)
            subLayout?.draw(this, x + subX, y + baseline + subTop)
        }
    }

    private fun layoutDisplayMode(
        context: RenderContext, density: Density, measurer: TextMeasurer,
        opLayout: NodeLayout, superLayout: NodeLayout?, subLayout: NodeLayout?,
        isNamedOperator: Boolean, opVisualHeight: Float
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

        // 上限墨水底部：使用 baseline 而非 height，因为 height 包含 descent 空白
        val superInkBottom = superLayout?.baseline ?: 0f

        // 布局坐标 (y=0 = NodeLayout 顶部):
        val superDrawY = 0f
        val opDrawY = superInkBottom + spacing
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
