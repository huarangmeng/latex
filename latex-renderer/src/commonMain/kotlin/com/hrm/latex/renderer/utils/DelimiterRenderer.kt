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

package com.hrm.latex.renderer.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.hrm.latex.base.log.HLog
import com.hrm.latex.renderer.font.GlyphPart
import com.hrm.latex.renderer.font.MathFontProvider
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.opentype.GlyphPathData
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 定界符渲染共享基础设施
 *
 * 所有需要渲染定界符（括号、竖线等）的 Measurer 统一调用此对象，
 * 消除 DelimiterMeasurer / MatrixMeasurer 等多处重复的
 * `measureDelimiterScaled()` 实现。
 *
 * 支持两种渲染路径：
 * 1. OTF 路径：
 *    a. 预设变体：通过 MATH 表 verticalVariants 选择高度足够的字形
 *    b. 字形组装：通过 MATH 表 verticalAssembly 用部件拼接任意高度
 *    c. fontSize 缩放兜底：对最大预设变体缩放 fontSize
 * 2. TTF 路径（KaTeX 字体方案）：Main → Size1 → Size2 → Size3 → Size4 逐级尝试
 */
internal object DelimiterRenderer {

    private const val TAG = "DelimiterRenderer"

    /**
     * 逐级尝试的字体级别列表（TTF 路径）
     */
    private data class SizeLevel(
        val name: String,
        val getFont: (com.hrm.latex.renderer.model.LatexFontFamilies) -> FontFamily
    )

    private val sizeLevels = listOf(
        SizeLevel("main") { it.main },
        SizeLevel("size1") { it.size1 },
        SizeLevel("size2") { it.size2 },
        SizeLevel("size3") { it.size3 },
        SizeLevel("size4") { it.size4 },
    )

    /**
     * 测量并缩放定界符至目标高度
     *
     * 优先使用 OTF 路径（预设变体 → 字形组装 → fontSize 缩放），
     * 无 MathFontProvider 或无变体时回退到 TTF 路径（KaTeX Size1~4）。
     *
     * @param delimiter 定界符字符串 (如 "(", "[", "{", "|", "‖")
     * @param context 当前渲染上下文
     * @param measurer 文本测量器
     * @param targetHeight 目标高度（像素）
     * @param density 密度（用于 sp → px 转换），可选；
     *        提供时使用精确像素值，不提供时用 fontSize.value 近似
     * @return 包含定界符的 NodeLayout
     */
    fun measureScaled(
        delimiter: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float,
        density: Density? = null
    ): NodeLayout {
        val glyph = FontResolver.resolveDelimiterGlyph(delimiter, context.fontFamilies)

        if (targetHeight <= 0f) {
            return measureText(glyph, FontResolver.delimiterContext(context, delimiter), measurer)
        }

        // 优先尝试 OTF 路径：预设变体 → 字形组装 → fontSize 缩放
        val provider = context.mathFontProvider
        if (provider != null) {
            val result = measureScaledWithProvider(
                glyph, context, measurer, targetHeight, provider, density
            )
            if (result != null) return result
        }

        // 回退到 TTF 路径：KaTeX Size1~4 逐级尝试
        return measureScaledWithSizeLevels(glyph, context, measurer, targetHeight)
    }

    /**
     * OTF 路径：三级回退策略
     *
     * 统一使用 verticalVariants() 获取变体列表，每个变体同时携带 glyphId 和 glyphChar：
     * 1. 预设变体 — 优先用 glyphId + glyphPath() 渲染（Path 路径），
     *    失败时降级到 glyphChar + TextMeasurer 渲染
     * 2. 字形组装 — 用 verticalAssembly() 的部件 glyphId + glyphPath() 拼接
     * 3. fontSize 缩放兜底 — 对最大预设变体放大 fontSize
     *
     * @return 成功时返回 NodeLayout；无变体数据时返回 null（回退到 TTF 路径）
     */
    private fun measureScaledWithProvider(
        glyph: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float,
        provider: MathFontProvider,
        density: Density?
    ): NodeLayout? {
        val fontSizePx = if (density != null) {
            with(density) { context.fontSize.toPx() }
        } else {
            context.fontSize.value
        }

        val variants = provider.verticalVariants(glyph, fontSizePx)
        if (variants.isEmpty()) return null

        val drawColor = context.color

        // ── 第 1 级：预设变体 ──
        // 双路渲染：优先 glyphPath（Path 路径），降级到 TextMeasurer
        var bestPathData: GlyphPathData? = null
        var bestPathGlyphId = 0
        var bestTextLayout: NodeLayout? = null
        var bestTextVariant: com.hrm.latex.renderer.font.GlyphVariant? = null

        for (variant in variants) {
            // 优先尝试 Path 渲染（绕过 Unicode 映射限制）
            if (variant.glyphId != 0) {
                val pathData = provider.glyphPath(variant.glyphId, fontSizePx)
                if (pathData != null) {
                    bestPathData = pathData
                    bestPathGlyphId = variant.glyphId
                    if (pathData.height >= targetHeight) {
                        return createPathNodeLayout(pathData, drawColor)
                    }
                }
            }

            // TextMeasurer 降级（适用于 TTF 字体或 Path 提取失败的场景）
            if (variant.glyphChar.isNotEmpty()) {
                val variantContext = context.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                    fontFamily = variant.fontFamily,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                )
                val layout = measureText(variant.glyphChar, variantContext, measurer)
                bestTextLayout = layout
                bestTextVariant = variant
                if (bestPathData == null && layout.height >= targetHeight) {
                    return layout
                }
            }
        }

        // 所有预设变体都不够高 → 进入第 2 级（组装）

        // ── 第 2 级：字形组装 ──
        val assemblyResult = measureWithAssembly(
            glyph, fontSizePx, targetHeight, provider, drawColor
        )
        if (assemblyResult != null) {
            HLog.d(
                TAG,
                "Assembly succeeded for '$glyph': ${assemblyResult.width}x${assemblyResult.height}"
            )
            return assemblyResult
        }

        // ── 第 3 级：fontSize 缩放兜底 ──
        // 优先使用 Path 缩放（精确），降级到 TextMeasurer 缩放
        if (bestPathData != null && bestPathData.height > 0f) {
            val scale = targetHeight / bestPathData.height
            val scaledFontSizePx = fontSizePx * scale
            val lastGlyphId = variants.last().glyphId.takeIf { it != 0 } ?: bestPathGlyphId
            if (lastGlyphId != 0) {
                val scaledPathData = provider.glyphPath(lastGlyphId, scaledFontSizePx)
                if (scaledPathData != null) {
                    return createPathNodeLayout(scaledPathData, drawColor)
                }
            }
            return createPathNodeLayout(bestPathData, drawColor)
        }

        if (bestTextLayout != null && bestTextVariant != null && bestTextLayout.height > 0f) {
            val scale = targetHeight / bestTextLayout.height
            val scaledContext = context.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                fontFamily = bestTextVariant.fontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = context.fontSize * scale
            )
            return measureText(bestTextVariant.glyphChar, scaledContext, measurer)
        }

        return bestTextLayout ?: bestPathData?.let { createPathNodeLayout(it, drawColor) }
    }

    // ─── 字形组装算法 ────────────────────────────────────────────

    /**
     * 通过 MATH 表 verticalAssembly 用部件拼接定界符。
     *
     * 算法 (OpenType MATH 规范)：
     * 1. 分离 non-extender（固定部件）和 extender（扩展部件）
     * 2. 计算固定部件的总高度（减去 minConnectorOverlap 重叠）
     * 3. 如果固定高度已足够 → 不需要扩展件重复
     * 4. 否则，计算扩展件需要重复的次数 N
     * 5. 将扩展件重复 N 次插入部件列表
     * 6. 均匀分配重叠量使总高度 = targetHeight
     * 7. 用 glyphPath 渲染每个部件，按计算好的偏移拼接
     */
    private fun measureWithAssembly(
        glyph: String,
        fontSizePx: Float,
        targetHeight: Float,
        provider: MathFontProvider,
        drawColor: Color
    ): NodeLayout? {
        val assembly = provider.verticalAssembly(glyph, fontSizePx) ?: return null
        if (assembly.parts.isEmpty()) return null

        val partPaths = mutableListOf<Pair<GlyphPart, GlyphPathData>>()
        for (part in assembly.parts) {
            if (part.glyphId == 0) {
                HLog.d(TAG, "Assembly part has no glyphId, skipping assembly")
                return null
            }
            val pathData = provider.glyphPath(part.glyphId, fontSizePx)
            if (pathData == null) {
                HLog.d(TAG, "Assembly part glyph ${part.glyphId} path extraction failed")
                return null  // 任一部件失败则放弃组装
            }
            partPaths.add(part to pathData)
        }

        // OpenType MATH 规范中垂直组装部件按 bottom-to-top 排列（字体坐标系 y 轴向上），
        // 但 Canvas 渲染是 top-to-bottom（y 轴向下），需要反转部件顺序。
        // 同时交换每个部件的 startConnectorLength 和 endConnectorLength，
        // 因为反转后原来的"起始端"变成了"结束端"。
        val reversedPartPaths = partPaths.reversed().map { (part, pathData) ->
            part.copy(
                startConnectorLength = part.endConnectorLength,
                endConnectorLength = part.startConnectorLength
            ) to pathData
        }

        val minOverlap = assembly.minConnectorOverlap

        // 2. 构建展开的部件列表（扩展件重复 N 次）
        val expandedParts = buildExpandedPartList(reversedPartPaths, minOverlap, targetHeight)
        if (expandedParts.isEmpty()) return null

        // 3. 计算每个部件的实际绘制偏移和总尺寸
        val placement = computeAssemblyPlacement(expandedParts, minOverlap, targetHeight)

        // 4. 构建 NodeLayout
        return createAssemblyNodeLayout(placement, drawColor)
    }

    /**
     * 构建展开的部件列表：非扩展件保持 1 个，扩展件重复 N 次。
     *
     * 使用 OpenType MATH 规范定义的 fullAdvance（而非 pathData.height 墨水边界）
     * 计算总高度和扩展件重复次数，确保部件间距正确无间隙。
     *
     * @return 展开后的 (GlyphPart, GlyphPathData) 列表
     */
    private fun buildExpandedPartList(
        partPaths: List<Pair<GlyphPart, GlyphPathData>>,
        minOverlap: Float,
        targetHeight: Float
    ): List<Pair<GlyphPart, GlyphPathData>> {
        // 计算不含扩展件重复时的总高度（使用 fullAdvance）
        var nonExtenderAdvance = 0f
        var extenderAdvance = 0f
        var connectionCount = 0
        val hasExtender = partPaths.any { it.first.isExtender }

        for (i in partPaths.indices) {
            val (part, _) = partPaths[i]
            if (part.isExtender) {
                extenderAdvance = part.fullAdvance
            }
            nonExtenderAdvance += part.fullAdvance
            if (i > 0) connectionCount++
        }

        // 基础高度（所有部件各出现 1 次，扣除重叠）
        val baseHeight = nonExtenderAdvance - connectionCount * minOverlap

        if (!hasExtender || baseHeight >= targetHeight) {
            // 不需要扩展件重复
            return partPaths.toList()
        }

        // 计算扩展件需要额外重复的次数
        val extenderNet = extenderAdvance - minOverlap  // 每增加一个扩展件的净增高度
        if (extenderNet <= 0f) return partPaths.toList()

        val needed = targetHeight - baseHeight
        // 扩展件总数 = 已有的1次 + 额外重复次数
        // 但注意可能有多个扩展件位置（如大括号有上下两段扩展件），需要同步重复
        val extenderCount = partPaths.count { it.first.isExtender }
        val neededPerPosition = needed / extenderCount
        val extraRepeats = ceil(neededPerPosition / extenderNet).toInt().coerceAtLeast(0)

        // 构建展开的列表
        val expanded = mutableListOf<Pair<GlyphPart, GlyphPathData>>()
        for ((part, pathData) in partPaths) {
            expanded.add(part to pathData)
            if (part.isExtender && extraRepeats > 0) {
                repeat(extraRepeats) {
                    expanded.add(part to pathData)
                }
            }
        }
        return expanded
    }

    /**
     * 已放置的组装部件，包含绘制偏移。
     */
    private data class PlacedPart(
        val pathData: GlyphPathData,
        val yOffset: Float,     // 该部件 fullAdvance 区域的 y 偏移（从整体顶部开始）
        val pathInkOffsetY: Float  // Path 墨水区域在 fullAdvance 内的 y 偏移
    )

    /**
     * 组装放置结果。
     */
    private data class AssemblyPlacement(
        val parts: List<PlacedPart>,
        val totalWidth: Float,
        val totalHeight: Float,
        val baseline: Float,
        val globalMinX: Float  // 所有部件的最小 minX（用于统一水平对齐）
    )

    /**
     * 计算组装部件的放置位置。
     *
     * 策略：从上到下依次放置部件，相邻部件之间有重叠（overlap）。
     * 使用 fullAdvance（OpenType MATH 规范定义的完整前进高度）计算间距，
     * 而非 pathData.height（墨水边界高度），确保部件无间隙拼接。
     *
     * 每个部件的 Path 墨水区域在 fullAdvance 内的偏移通过
     * (fullAdvance - inkHeight) / 2 近似计算（假设 sidebearing 上下对称）。
     */
    private fun computeAssemblyPlacement(
        parts: List<Pair<GlyphPart, GlyphPathData>>,
        minOverlap: Float,
        targetHeight: Float
    ): AssemblyPlacement {
        if (parts.isEmpty()) {
            return AssemblyPlacement(emptyList(), 0f, 0f, 0f, 0f)
        }

        val n = parts.size
        val connectionCount = n - 1

        // 用最小重叠计算总高度（基于 fullAdvance）
        var heightWithMinOverlap = 0f
        for ((part, _) in parts) {
            heightWithMinOverlap += part.fullAdvance
        }
        heightWithMinOverlap -= connectionCount * minOverlap

        // 计算实际需要的重叠量
        // 如果 heightWithMinOverlap > targetHeight，增加重叠；否则使用最小重叠
        val actualOverlap = if (connectionCount > 0 && heightWithMinOverlap > targetHeight) {
            val excess = heightWithMinOverlap - targetHeight
            val additionalOverlap = excess / connectionCount
            // 重叠不能超过每个连接器允许的最大值
            val maxAllowed = (0 until connectionCount).minOf { i ->
                val endConnector = parts[i].first.endConnectorLength
                val startConnector = parts[i + 1].first.startConnectorLength
                min(endConnector, startConnector)
            }
            min(minOverlap + additionalOverlap, maxAllowed)
        } else {
            minOverlap
        }

        // 放置部件（使用 fullAdvance 推进 currentY）
        val placed = mutableListOf<PlacedPart>()
        var currentY = 0f
        // 水平对齐：所有部件共享字体坐标系的 x=0 参考点。
        // Path 中的 x 坐标是 fontX * scale（未减去 minX），
        // 不同部件的 minX/maxX 不同，需要找到统一的包围盒。
        var globalMinX = Float.MAX_VALUE
        var globalMaxX = Float.MIN_VALUE
        var maxAdvanceWidth = 0f

        for (i in parts.indices) {
            val (part, pathData) = parts[i]
            val yOffset = if (i == 0) 0f else currentY - actualOverlap

            // Path 墨水区域在 fullAdvance 内的偏移
            val pathInkOffsetY = (part.fullAdvance - pathData.height) / 2f

            placed.add(PlacedPart(pathData, yOffset, pathInkOffsetY.coerceAtLeast(0f)))
            globalMinX = min(globalMinX, pathData.minX)
            globalMaxX = max(globalMaxX, pathData.maxX)
            maxAdvanceWidth = max(maxAdvanceWidth, pathData.advanceWidth)
            currentY = yOffset + part.fullAdvance
        }

        val totalHeight = currentY
        // 总宽度使用 advanceWidth（包含右侧 sidebearing，确保与后续内容有正确间距）
        // 同时确保宽度足以包含所有墨水
        val inkWidth = globalMaxX - globalMinX.coerceAtMost(0f)
        val totalWidth = max(inkWidth, maxAdvanceWidth)
        // baseline 取整体高度的中点附近（组装定界符通常以数学轴为中心）
        val baseline = totalHeight / 2f

        return AssemblyPlacement(placed, totalWidth, totalHeight, baseline, globalMinX)
    }

    /**
     * 从组装放置结果创建 NodeLayout。
     *
     * 每个部件的 Path 以 (0,0) 为原点预构建，draw 时通过 translate 偏移到实际位置。
     * 部件水平居中对齐。yOffset 基于 fullAdvance 坐标系，pathInkOffsetY 补偿
     * Path 墨水区域在 fullAdvance 内的偏移。
     */
    private fun createAssemblyNodeLayout(
        placement: AssemblyPlacement,
        drawColor: Color
    ): NodeLayout {
        if (placement.parts.isEmpty()) return NodeLayout.EMPTY

        val width = placement.totalWidth
        val height = placement.totalHeight
        val baseline = placement.baseline

        // 预计算每个部件的水平偏移（统一对齐）和垂直偏移（fullAdvance 偏移 + 墨水偏移）
        // 所有部件共享字体坐标系的 x=0 参考点，用 -globalMinX 将所有 Path 偏移到正坐标区域。
        // 这样不同宽度的部件（top/middle/bottom/extender）都以字体设计的基准对齐。
        val globalMinX = placement.globalMinX

        data class DrawInfo(
            val path: androidx.compose.ui.graphics.Path,
            val xOff: Float,
            val yOff: Float
        )

        val drawInfos = placement.parts.map { placed ->
            val xOff = -globalMinX.coerceAtMost(0f)
            // yOffset 是 fullAdvance 区域的起始位置，pathInkOffsetY 是墨水在 fullAdvance 内的偏移
            val yOff = placed.yOffset + placed.pathInkOffsetY
            DrawInfo(placed.pathData.path, xOff, yOff)
        }

        return NodeLayout(width, height, baseline) { x, y ->
            for (info in drawInfos) {
                withTransform({ translate(left = x + info.xOff, top = y + info.yOff) }) {
                    drawPath(info.path, color = drawColor, style = Fill)
                }
            }
        }
    }

    // ─── 变体 Path NodeLayout 创建 ──────────────────────────────

    /**
     * 从 GlyphPathData 创建 NodeLayout（单个变体字形）。
     *
     * 宽度使用 advanceWidth（包含右侧 sidebearing），确保与后续内容有正确间距。
     * 此方法同时被 DelimiterRenderer 和 BigOperatorMeasurer 共享使用。
     */
    internal fun createPathNodeLayout(
        pathData: GlyphPathData,
        drawColor: Color
    ): NodeLayout {
        val path = pathData.path
        val inkWidth = pathData.width.coerceAtLeast(1f)
        val height = pathData.height.coerceAtLeast(1f)
        val baseline = pathData.baselineY
        val xOffset = -pathData.minX.coerceAtMost(0f)
        // 使用 advanceWidth 确保右侧有 sidebearing 间距，但至少要包含墨水
        val width = max(inkWidth + xOffset, pathData.advanceWidth)

        return NodeLayout(
            width = width,
            height = height,
            baseline = baseline
        ) { x, y ->
            withTransform({ translate(left = x + xOffset, top = y) }) {
                drawPath(path, color = drawColor, style = Fill)
            }
        }
    }

    // ─── TTF 路径 ────────────────────────────────────────────────

    /**
     * TTF 路径：KaTeX Size1~4 逐级尝试
     */
    private fun measureScaledWithSizeLevels(
        glyph: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float
    ): NodeLayout {
        val fontFamilies = context.fontFamilies

        if (fontFamilies == null) {
            return measureText(glyph, FontResolver.delimiterContext(context, glyph), measurer)
        }

        // 逐级尝试 Main → Size1 → Size2 → Size3 → Size4
        var bestLayout: NodeLayout? = null
        var bestContext: RenderContext? = null

        for (level in sizeLevels) {
            val font = level.getFont(fontFamilies)
            val levelContext = context.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                fontFamily = font,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )
            val layout = measureText(glyph, levelContext, measurer)

            bestLayout = layout
            bestContext = levelContext

            if (layout.height >= targetHeight) {
                return layout
            }
        }

        // 所有 Size 字体都不够高 → 对 Size4 做 fontSize 缩放
        val size4Layout = bestLayout!!
        val size4Context = bestContext!!

        if (size4Layout.height <= 0f) return size4Layout

        val scale = targetHeight / size4Layout.height
        val scaledContext = size4Context.copy(
            fontSize = context.fontSize * scale
        )
        return measureText(glyph, scaledContext, measurer)
    }

    // ─── 文本测量 ────────────────────────────────────────────────

    /**
     * 以指定样式测量定界符文本
     */
    fun measureText(
        delimiter: String,
        delimiterStyle: RenderContext,
        measurer: TextMeasurer
    ): NodeLayout {
        val textStyle = delimiterStyle.textStyle()
        val result = measurer.measure(AnnotatedString(delimiter), textStyle)
        return NodeLayout(
            result.size.width.toFloat(),
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }
}
