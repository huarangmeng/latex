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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.font.MathFontProvider
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.opentype.GlyphPathData
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * 根号测量器 — 处理 \sqrt[index]{content}
 *
 * 渲染策略（两级回退）：
 * 1. OTF 路径：从 MATH 表获取 √ 的垂直变体字形，通过 glyphPath() 提取 Path 渲染。
 *    字形自身已包含 surd（弯钩）+ overline（顶部横线），内容对齐到 overline 下方。
 *    仅需根据内容宽度延伸 overline。
 * 2. Path 回退：手工绘制根号折线 + drawLine 画 overline（无 math 表字体时的最终兜底）。
 *
 * 布局模型：
 * ```
 *   [index]  ┌──── overline ──────┐
 *         \  │      gap           │
 *    surd V  │   ┌─ content ─┐   │
 *            │   └───────────┘   │
 * ```
 */
internal class RootMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Root::class
    )

    companion object {
        /** √ 字符 (U+221A SQUARE ROOT) */
        private const val SQRT_CHAR = "\u221A"

        /** 根号内容左侧额外间距 / fontSize */
        private const val CONTENT_LEFT_KERN = 0.08f

        /** 根号内容右侧额外间距 / fontSize */
        private const val CONTENT_RIGHT_KERN = 0.05f
    }

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Root
        val indexStyle = context.shrink(MathConstants.RADICAL_INDEX_SCALE)

        val contentLayout = measureGroup(listOf(node.content), context)
        val indexLayout = node.index?.let { measureGroup(listOf(it), indexStyle) }

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val provider = context.mathFontProvider

        val ruleThickness = provider?.radicalRuleThickness(fontSizePx)
            ?: (fontSizePx * MathConstants.FRACTION_RULE_THICKNESS)
        val gap = provider?.radicalVerticalGap(fontSizePx)
            ?: (ruleThickness * MathConstants.RADICAL_TOP_GAP_MULTIPLIER)

        // OTF 路径：字形自身包含 overline，targetHeight = 内容高度 + gap（无需 ruleThickness）
        // Path 回退：需要自己画 overline，targetHeight = 内容高度 + gap + ruleThickness
        val extraTop = gap + ruleThickness
        val targetSurdHeight = contentLayout.height + extraTop

        // 尝试 OTF 字形路径
        val surdResult = measureSurdWithProvider(provider, fontSizePx, targetSurdHeight, context.color)

        return if (surdResult != null) {
            layoutRootWithGlyph(
                surdResult = surdResult,
                contentLayout = contentLayout,
                indexLayout = indexLayout,
                ruleThickness = ruleThickness,
                gap = gap,
                fontSizePx = fontSizePx,
                color = context.color,
                targetSurdHeight = targetSurdHeight
            )
        } else {
            val fallbackSurd = measureSurdFallbackPath(fontSizePx, targetSurdHeight, ruleThickness, context.color)
            layoutRootWithPath(
                surdResult = fallbackSurd,
                contentLayout = contentLayout,
                indexLayout = indexLayout,
                ruleThickness = ruleThickness,
                extraTop = extraTop,
                fontSizePx = fontSizePx,
                color = context.color,
                targetSurdHeight = targetSurdHeight
            )
        }
    }

    // ─── OTF 字形变体路径 ────────────────────────────────────────

    /**
     * 通过 MathFontProvider 获取 √ 字形。
     *
     * 策略：
     * 1. 遍历 verticalVariants，找到 **inkHeight** >= targetHeight 的最小变体。
     *    如果墨水高度超出目标过多（> 15%），缩放 fontSize 精确匹配。
     * 2. 若所有预设变体都不够高，尝试字形组装。
     * 3. 若组装也不支持，对最大变体做 fontSize 等比缩放。
     *
     * @return SurdResult 或 null（表示 provider 无法提供根号字形）
     */
    private fun measureSurdWithProvider(
        provider: MathFontProvider?,
        fontSizePx: Float,
        targetHeight: Float,
        color: Color
    ): SurdResult? {
        if (provider == null) return null

        val variants = provider.verticalVariants(SQRT_CHAR, fontSizePx)
        if (variants.isEmpty()) return null

        // 第 1 级：预设变体 — 基于实际墨水高度选择
        var bestPathData: GlyphPathData? = null
        var bestGlyphId = 0
        var bestInkHeight = 0f

        for (variant in variants) {
            if (variant.glyphId != 0) {
                val pathData = provider.glyphPath(variant.glyphId, fontSizePx)
                if (pathData != null) {
                    bestPathData = pathData
                    bestGlyphId = variant.glyphId
                    bestInkHeight = pathData.height

                    if (pathData.height >= targetHeight) {
                        // 用墨水高度判断是否过大
                        val ratio = pathData.height / targetHeight
                        if (ratio <= 1.15f) {
                            // 偏差在 15% 以内，直接使用
                            return createSurdFromPathData(pathData, targetHeight, color)
                        } else {
                            // 墨水高度偏差太大，缩放 fontSize 精确匹配
                            return scaleGlyphToTarget(
                                provider, variant.glyphId, fontSizePx,
                                pathData.height, targetHeight, color
                            ) ?: createSurdFromPathData(pathData, targetHeight, color)
                        }
                    }
                }
            }
        }

        // 第 2 级：字形组装
        val assemblyResult = measureSurdWithAssembly(provider, fontSizePx, targetHeight, color)
        if (assemblyResult != null) return assemblyResult

        // 第 3 级：对最大预设变体做 fontSize 缩放
        if (bestPathData != null && bestInkHeight > 0f) {
            val lastGlyphId = variants.last().glyphId.takeIf { it != 0 } ?: bestGlyphId
            if (lastGlyphId != 0) {
                return scaleGlyphToTarget(
                    provider, lastGlyphId, fontSizePx,
                    bestInkHeight, targetHeight, color
                ) ?: createSurdFromPathData(bestPathData, targetHeight, color)
            }
            return createSurdFromPathData(bestPathData, targetHeight, color)
        }

        return null
    }

    /**
     * 缩放 fontSize 使字形墨水高度精确匹配 targetHeight。
     */
    private fun scaleGlyphToTarget(
        provider: MathFontProvider,
        glyphId: Int,
        fontSizePx: Float,
        currentInkHeight: Float,
        targetHeight: Float,
        color: Color
    ): SurdResult? {
        val scale = targetHeight / currentInkHeight
        val scaledPathData = provider.glyphPath(glyphId, fontSizePx * scale) ?: return null
        return createSurdFromPathData(scaledPathData, targetHeight, color)
    }

    /**
     * 从 GlyphPathData 创建 SurdResult。
     *
     * 布局高度 = max(targetHeight, inkHeight)，确保字形完整不被裁切。
     * 字形底部对齐布局底部，overlineY = 字形顶部在布局坐标系中的 y。
     */
    private fun createSurdFromPathData(
        pathData: GlyphPathData,
        targetHeight: Float,
        color: Color
    ): SurdResult {
        val path = pathData.path
        val width = max(pathData.advanceWidth, pathData.width)
        val inkHeight = pathData.height
        val xOffset = -pathData.minX.coerceAtMost(0f)

        // 布局高度取两者较大值，确保字形完整不被裁切
        val layoutHeight = max(targetHeight, inkHeight)

        // 字形底部对齐布局底部，glyphOffsetY >= 0
        val glyphOffsetY = layoutHeight - inkHeight

        // overline y = 字形顶部在布局坐标系中的位置
        val overlineY = glyphOffsetY

        val drawFn: DrawScope.(Float, Float) -> Unit = { x, y ->
            withTransform({ translate(left = x + xOffset, top = y + glyphOffsetY) }) {
                drawPath(path, color = color, style = Fill)
            }
        }

        return SurdResult(
            width = width + xOffset,
            height = layoutHeight,
            overlineY = overlineY,
            isGlyph = true,
            drawSurd = drawFn
        )
    }

    /**
     * 字形组装：用 MATH 表 verticalAssembly 的部件拼接超高根号。
     */
    private fun measureSurdWithAssembly(
        provider: MathFontProvider,
        fontSizePx: Float,
        targetHeight: Float,
        color: Color
    ): SurdResult? {
        val assembly = provider.verticalAssembly(SQRT_CHAR, fontSizePx) ?: return null
        if (assembly.parts.isEmpty()) return null

        val partPaths = mutableListOf<Pair<com.hrm.latex.renderer.font.GlyphPart, GlyphPathData>>()
        for (part in assembly.parts) {
            if (part.glyphId == 0) return null
            val pathData = provider.glyphPath(part.glyphId, fontSizePx) ?: return null
            partPaths.add(part to pathData)
        }

        // 反转部件顺序（bottom-to-top → top-to-bottom）
        val reversedParts = partPaths.reversed().map { (part, pathData) ->
            part.copy(
                startConnectorLength = part.endConnectorLength,
                endConnectorLength = part.startConnectorLength
            ) to pathData
        }

        val minOverlap = assembly.minConnectorOverlap
        val expandedParts = buildExpandedPartList(reversedParts, minOverlap, targetHeight)
        if (expandedParts.isEmpty()) return null

        val n = expandedParts.size
        val connectionCount = n - 1

        var heightWithMinOverlap = 0f
        for ((part, _) in expandedParts) {
            heightWithMinOverlap += part.fullAdvance
        }
        heightWithMinOverlap -= connectionCount * minOverlap

        val actualOverlap = if (connectionCount > 0 && heightWithMinOverlap > targetHeight) {
            val excess = heightWithMinOverlap - targetHeight
            val additional = excess / connectionCount
            val maxAllowed = (0 until connectionCount).minOf { i ->
                min(expandedParts[i].first.endConnectorLength,
                    expandedParts[i + 1].first.startConnectorLength)
            }
            min(minOverlap + additional, maxAllowed)
        } else {
            minOverlap
        }

        data class PlacedPart(val path: Path, val xOff: Float, val yOff: Float)

        var currentY = 0f
        var maxWidth = 0f
        val placements = mutableListOf<PlacedPart>()

        for (i in expandedParts.indices) {
            val (part, pathData) = expandedParts[i]
            val yOffset = if (i == 0) 0f else currentY - actualOverlap
            val pathInkOffsetY = ((part.fullAdvance - pathData.height) / 2f).coerceAtLeast(0f)
            val xOff = -pathData.minX.coerceAtMost(0f)

            placements.add(PlacedPart(pathData.path, xOff, yOffset + pathInkOffsetY))
            maxWidth = max(maxWidth, max(pathData.width + xOff, pathData.advanceWidth))
            currentY = yOffset + part.fullAdvance
        }

        val totalHeight = currentY

        val drawFn: DrawScope.(Float, Float) -> Unit = { x, y ->
            for (placed in placements) {
                withTransform({ translate(left = x + placed.xOff, top = y + placed.yOff) }) {
                    drawPath(placed.path, color = color, style = Fill)
                }
            }
        }

        return SurdResult(
            width = maxWidth,
            height = totalHeight,
            overlineY = 0f,
            isGlyph = true,
            drawSurd = drawFn
        )
    }

    private fun buildExpandedPartList(
        partPaths: List<Pair<com.hrm.latex.renderer.font.GlyphPart, GlyphPathData>>,
        minOverlap: Float,
        targetHeight: Float
    ): List<Pair<com.hrm.latex.renderer.font.GlyphPart, GlyphPathData>> {
        var nonExtenderAdvance = 0f
        var extenderAdvance = 0f
        var connectionCount = 0
        val hasExtender = partPaths.any { it.first.isExtender }

        for (i in partPaths.indices) {
            val (part, _) = partPaths[i]
            if (part.isExtender) extenderAdvance = part.fullAdvance
            nonExtenderAdvance += part.fullAdvance
            if (i > 0) connectionCount++
        }

        val baseHeight = nonExtenderAdvance - connectionCount * minOverlap
        if (!hasExtender || baseHeight >= targetHeight) return partPaths.toList()

        val extenderNet = extenderAdvance - minOverlap
        if (extenderNet <= 0f) return partPaths.toList()

        val needed = targetHeight - baseHeight
        val extenderCount = partPaths.count { it.first.isExtender }
        val neededPerPosition = needed / extenderCount
        val extraRepeats = ceil(neededPerPosition / extenderNet).toInt().coerceAtLeast(0)

        val expanded = mutableListOf<Pair<com.hrm.latex.renderer.font.GlyphPart, GlyphPathData>>()
        for ((part, pathData) in partPaths) {
            expanded.add(part to pathData)
            if (part.isExtender && extraRepeats > 0) {
                repeat(extraRepeats) { expanded.add(part to pathData) }
            }
        }
        return expanded
    }

    // ─── Path 回退方案 ───────────────────────────────────────────

    /**
     * 手工 Path 绘制根号（无 math 表字体时的最终兜底）。
     */
    private fun measureSurdFallbackPath(
        fontSizePx: Float,
        targetHeight: Float,
        ruleThickness: Float,
        color: Color
    ): SurdResult {
        val strokeWidth = ruleThickness
        val strokeHalf = strokeWidth / 2f

        val tailWidth = fontSizePx * 0.08f
        val checkWidth = fontSizePx * 0.12f
        val hookWidth = fontSizePx * 0.16f

        val totalSurdWidth = tailWidth + checkWidth + hookWidth
        val surdHeight = targetHeight

        val vBottomY = surdHeight - strokeHalf
        val vMidY = surdHeight * 0.55f
        val tailY = vMidY - fontSizePx * 0.05f

        val sqrtPath = Path().apply {
            moveTo(strokeHalf, tailY)
            lineTo(tailWidth + strokeHalf, vMidY)
            lineTo(tailWidth + checkWidth, vBottomY)
            lineTo(totalSurdWidth, strokeHalf)
        }

        val drawFn: DrawScope.(Float, Float) -> Unit = { x, y ->
            withTransform({ translate(left = x, top = y) }) {
                drawPath(sqrtPath, color = color, style = Stroke(strokeWidth))
            }
        }

        return SurdResult(
            width = totalSurdWidth + strokeHalf,
            height = surdHeight,
            overlineY = strokeHalf,
            isGlyph = false,
            drawSurd = drawFn
        )
    }

    // ─── 布局组合（字体字形路径） ────────────────────────────────

    /**
     * OTF 字形路径的布局。
     *
     * 字形自身包含 surd + overline 的一小段。
     * 仅需延伸 overline 到内容右端。
     *
     * 内容底部对齐 surd 底部（targetSurdHeight 已包含 gap + ruleThickness，
     * 因此内容顶部自然位于 overline 下方 gap + ruleThickness 处）。
     */
    private fun layoutRootWithGlyph(
        surdResult: SurdResult,
        contentLayout: NodeLayout,
        indexLayout: NodeLayout?,
        ruleThickness: Float,
        gap: Float,
        fontSizePx: Float,
        color: Color,
        targetSurdHeight: Float
    ): NodeLayout {
        val contentLeftKern = fontSizePx * CONTENT_LEFT_KERN
        val contentRightKern = fontSizePx * CONTENT_RIGHT_KERN

        val contentX = surdResult.width + contentLeftKern
        val totalWidth = contentX + contentLayout.width + contentRightKern
        val totalHeight = surdResult.height

        // 内容底部对齐 surd 底部
        val contentTopY = totalHeight - contentLayout.height
        val baseline = contentTopY + contentLayout.baseline

        // overline y 位置（surd 坐标系中的顶部）
        val overlineAbsY = surdResult.overlineY

        // 根指数位置
        val indexWidth = indexLayout?.width ?: 0f
        val indexHeight = indexLayout?.height ?: 0f
        val indexX = max(0f, surdResult.width * 0.5f - indexWidth)
        val indexY = surdResult.height * 0.3f - indexHeight

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            indexLayout?.draw(this, x + indexX, y + indexY)

            surdResult.drawSurd(this, x, y)

            // 延伸 overline 到内容右端
            val overlineStartX = x + surdResult.width - fontSizePx * 0.02f
            val overlineEndX = x + totalWidth
            drawLine(
                color,
                Offset(overlineStartX, y + overlineAbsY),
                Offset(overlineEndX, y + overlineAbsY),
                ruleThickness
            )

            contentLayout.draw(this, x + contentX, y + contentTopY)
        }
    }

    // ─── 布局组合（Path 回退路径） ───────────────────────────────

    /**
     * Path 回退路径的布局。
     * surd 是手工折线，overline 完全由 drawLine 绘制。
     */
    private fun layoutRootWithPath(
        surdResult: SurdResult,
        contentLayout: NodeLayout,
        indexLayout: NodeLayout?,
        ruleThickness: Float,
        extraTop: Float,
        fontSizePx: Float,
        color: Color,
        targetSurdHeight: Float
    ): NodeLayout {
        val strokeHalf = ruleThickness / 2f
        val contentLeftKern = fontSizePx * CONTENT_LEFT_KERN
        val contentRightKern = fontSizePx * CONTENT_RIGHT_KERN

        val contentX = surdResult.width + contentLeftKern
        val totalWidth = contentX + contentLayout.width + contentRightKern + strokeHalf
        val totalHeight = surdResult.height + strokeHalf
        val contentTopY = extraTop
        val baseline = contentTopY + contentLayout.baseline

        val indexWidth = indexLayout?.width ?: 0f
        val indexHeight = indexLayout?.height ?: 0f
        val indexX = max(0f, surdResult.width * 0.5f - indexWidth)
        val indexY = surdResult.height * 0.3f - indexHeight

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            indexLayout?.draw(this, x + indexX, y + indexY)

            surdResult.drawSurd(this, x, y)

            // overline 从 surd 顶端到内容右侧
            val overlineY = y + surdResult.overlineY
            drawLine(
                color,
                Offset(x + surdResult.width, overlineY),
                Offset(x + totalWidth - strokeHalf, overlineY),
                ruleThickness
            )

            contentLayout.draw(this, x + contentX, y + contentTopY)
        }
    }

    // ─── 内部数据结构 ────────────────────────────────────────────

    /**
     * 根号 surd 部分的测量结果。
     *
     * @property width surd 宽度
     * @property height surd 高度
     * @property overlineY overline 在 surd 坐标系中的 y 位置
     * @property isGlyph 是否来自字体字形（true）或手工 Path（false）
     * @property drawSurd 绘制 surd 的 lambda
     */
    private data class SurdResult(
        val width: Float,
        val height: Float,
        val overlineY: Float,
        val isGlyph: Boolean,
        val drawSurd: DrawScope.(Float, Float) -> Unit
    )
}
