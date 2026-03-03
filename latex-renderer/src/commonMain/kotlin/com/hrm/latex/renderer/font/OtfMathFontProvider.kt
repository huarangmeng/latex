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

package com.hrm.latex.renderer.font

import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.renderer.utils.opentype.BinaryReader
import com.hrm.latex.renderer.utils.opentype.CmapParser
import com.hrm.latex.renderer.utils.opentype.CmapTable
import com.hrm.latex.renderer.utils.opentype.GlyphOutlineProvider
import com.hrm.latex.renderer.utils.opentype.GlyphPathData
import com.hrm.latex.renderer.utils.opentype.MathConstantsData
import com.hrm.latex.renderer.utils.opentype.MathGlyphInfoData
import com.hrm.latex.renderer.utils.opentype.MathKernTable
import com.hrm.latex.renderer.utils.opentype.MathTableData
import com.hrm.latex.renderer.utils.opentype.MathTableParser
import com.hrm.latex.renderer.utils.opentype.MathVariantsData
import com.hrm.latex.renderer.utils.opentype.OpenTypeFontParser

/**
 * OTF MATH 表驱动的 MathFontProvider 实现。
 *
 * 从 OTF 字体文件的 MATH 表中读取数学排版参数，提供字体设计师精确调校的排版值。
 * 相比 [TtfFontSetProvider] 的硬编码近似值，OTF 方案在以下方面精度更高：
 * - 逐字形精确斜体修正（而非类别估算）
 * - 逐字形重音附着点（而非居中近似）
 * - 高度敏感的数学字距
 * - 连续字形变体 + 字形组装（而非 5 级离散跳跃）
 *
 * @param fontBytes OTF 字体文件的完整字节数据
 * @param fontFamily 从 OTF 文件创建的 Compose FontFamily
 */
internal class OtfMathFontProvider(
    private val fontBytes: ByteArray,
    private val fontFamily: FontFamily
) : MathFontProvider {

    override val hasGlyphVariants: Boolean = true

    private val reader = BinaryReader(fontBytes)
    private val fontInfo = OpenTypeFontParser.parse(reader)
    private val unitsPerEm = fontInfo.unitsPerEm

    /** Glyph Outline 提取器（懒加载，仅在首次需要 Path 渲染时初始化 CFF 解析） */
    private val glyphOutlineProvider: GlyphOutlineProvider by lazy {
        GlyphOutlineProvider(fontBytes)
    }

    /** cmap 表解析结果（Unicode ↔ GlyphID 双向映射） */
    private val cmapTable: CmapTable = run {
        val cmapRecord = fontInfo.tables["cmap"]
        if (cmapRecord != null) {
            CmapParser(reader, cmapRecord.offset).parse()
        } else {
            // 无 cmap 表时返回空映射
            CmapParser(reader, 0).parse()
        }
    }

    /** MATH 表解析结果 */
    private val mathTable: MathTableData? = run {
        val mathRecord = fontInfo.tables["MATH"]
        if (mathRecord != null) {
            MathTableParser(reader, mathRecord.offset).parse()
        } else null
    }

    private val constants: MathConstantsData? get() = mathTable?.constants
    private val glyphInfo: MathGlyphInfoData? get() = mathTable?.glyphInfo
    private val variants: MathVariantsData? get() = mathTable?.variants

    // ─── 设计空间单位转像素 ──────────────────────────────────────

    private fun designToPx(designUnits: Int, fontSizePx: Float): Float {
        if (unitsPerEm <= 0) return 0f
        return designUnits.toFloat() * fontSizePx / unitsPerEm.toFloat()
    }

    // ─── 全局排版常量 ────────────────────────────────────────────

    override fun axisHeight(fontSizePx: Float): Float {
        val v = constants?.axisHeight ?: return fontSizePx * FALLBACK_AXIS_RATIO
        return designToPx(v, fontSizePx)
    }

    override fun fractionRuleThickness(fontSizePx: Float): Float {
        val v = constants?.fractionRuleThickness ?: return fontSizePx * FALLBACK_RULE_THICKNESS
        return designToPx(v, fontSizePx)
    }

    override fun fractionNumeratorDisplayGap(fontSizePx: Float): Float {
        val v = constants?.fractionNumeratorDisplayStyleGapMin
            ?: return fontSizePx * FALLBACK_FRACTION_GAP
        return designToPx(v, fontSizePx)
    }

    override fun fractionNumeratorGap(fontSizePx: Float): Float {
        val v = constants?.fractionNumeratorGapMin ?: return fontSizePx * FALLBACK_FRACTION_GAP
        return designToPx(v, fontSizePx)
    }

    override fun fractionDenominatorDisplayGap(fontSizePx: Float): Float {
        val v = constants?.fractionDenominatorDisplayStyleGapMin
            ?: return fontSizePx * FALLBACK_FRACTION_GAP
        return designToPx(v, fontSizePx)
    }

    override fun fractionDenominatorGap(fontSizePx: Float): Float {
        val v = constants?.fractionDenominatorGapMin ?: return fontSizePx * FALLBACK_FRACTION_GAP
        return designToPx(v, fontSizePx)
    }

    override fun superscriptShiftUp(fontSizePx: Float): Float {
        val v = constants?.superscriptShiftUp ?: return fontSizePx * FALLBACK_SUPERSCRIPT_SHIFT
        return designToPx(v, fontSizePx)
    }

    override fun subscriptShiftDown(fontSizePx: Float): Float {
        val v = constants?.subscriptShiftDown ?: return fontSizePx * FALLBACK_SUBSCRIPT_SHIFT
        return designToPx(v, fontSizePx)
    }

    override fun subSuperscriptGapMin(fontSizePx: Float): Float {
        val v = constants?.subSuperscriptGapMin ?: return fontSizePx * FALLBACK_SCRIPT_GAP
        return designToPx(v, fontSizePx)
    }

    override fun scriptPercentScaleDown(): Int {
        return constants?.scriptPercentScaleDown ?: 70
    }

    override fun scriptScriptPercentScaleDown(): Int {
        return constants?.scriptScriptPercentScaleDown ?: 50
    }

    override fun radicalDisplayVerticalGap(fontSizePx: Float): Float {
        val v = constants?.radicalDisplayStyleVerticalGap
            ?: return fractionRuleThickness(fontSizePx) * 2f
        return designToPx(v, fontSizePx)
    }

    override fun radicalVerticalGap(fontSizePx: Float): Float {
        val v = constants?.radicalVerticalGap
            ?: return fractionRuleThickness(fontSizePx) * 1.4f
        return designToPx(v, fontSizePx)
    }

    override fun radicalRuleThickness(fontSizePx: Float): Float {
        val v = constants?.radicalRuleThickness ?: return fractionRuleThickness(fontSizePx)
        return designToPx(v, fontSizePx)
    }

    override fun upperLimitGapMin(fontSizePx: Float): Float {
        val v = constants?.upperLimitGapMin ?: return fontSizePx * 0.2f
        return designToPx(v, fontSizePx)
    }

    override fun lowerLimitGapMin(fontSizePx: Float): Float {
        val v = constants?.lowerLimitGapMin ?: return fontSizePx * 0.2f
        return designToPx(v, fontSizePx)
    }

    override fun overbarVerticalGap(fontSizePx: Float): Float {
        val v = constants?.overbarVerticalGap ?: return fractionRuleThickness(fontSizePx) * 3f
        return designToPx(v, fontSizePx)
    }

    override fun underbarVerticalGap(fontSizePx: Float): Float {
        val v = constants?.underbarVerticalGap ?: return fractionRuleThickness(fontSizePx) * 3f
        return designToPx(v, fontSizePx)
    }

    override fun accentBaseHeight(fontSizePx: Float): Float {
        val v = constants?.accentBaseHeight ?: return fontSizePx * 0.45f
        return designToPx(v, fontSizePx)
    }

    override fun stackDisplayGapMin(fontSizePx: Float): Float {
        val v = constants?.stackDisplayStyleGapMin ?: return fontSizePx * 0.3f
        return designToPx(v, fontSizePx)
    }

    override fun stackGapMin(fontSizePx: Float): Float {
        val v = constants?.stackGapMin ?: return fontSizePx * 0.15f
        return designToPx(v, fontSizePx)
    }

    // ─── 逐字形信息 ──────────────────────────────────────────────

    override fun italicCorrection(glyphChar: String, fontSizePx: Float): Float {
        val info = glyphInfo ?: return 0f
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return 0f
        val correction = info.italicsCorrections[glyphId] ?: return 0f
        return designToPx(correction, fontSizePx)
    }

    override fun topAccentAttachment(glyphChar: String, fontSizePx: Float): Float {
        val info = glyphInfo ?: return -1f
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return -1f
        val attachment = info.topAccentAttachments[glyphId] ?: return -1f
        return designToPx(attachment, fontSizePx)
    }

    override fun mathKern(
        glyphChar: String,
        height: Float,
        fontSizePx: Float,
        isRight: Boolean
    ): Float {
        val info = glyphInfo ?: return 0f
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return 0f
        val kernInfo = info.mathKerns[glyphId] ?: return 0f

        // 选择方向
        val kernTable: MathKernTable = (if (isRight) {
            kernInfo.topRight
        } else {
            kernInfo.topLeft
        }) ?: return 0f

        // 将 height 从 px 转换回设计空间单位进行比较
        val heightDesign = if (unitsPerEm > 0 && fontSizePx > 0f) {
            (height * unitsPerEm / fontSizePx).toInt()
        } else 0

        // 分段函数查找
        val kernValue = lookupKernValue(kernTable, heightDesign)
        return designToPx(kernValue, fontSizePx)
    }

    /**
     * 在 MathKernTable 中查找给定高度的字距值。
     *
     * 分段函数：
     * - h < heights[0] → kerns[0]
     * - heights[i-1] <= h < heights[i] → kerns[i]
     * - h >= heights[last] → kerns[last]
     */
    private fun lookupKernValue(table: MathKernTable, height: Int): Int {
        if (table.heights.isEmpty()) {
            return table.kerns.firstOrNull() ?: 0
        }
        for (i in table.heights.indices) {
            if (height < table.heights[i]) {
                return table.kerns[i]
            }
        }
        return table.kerns.last()
    }

    // ─── 字形变体与组装 ──────────────────────────────────────────

    override fun verticalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant> {
        val variantsData = variants ?: return emptyList()
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return emptyList()
        val construction = variantsData.verticalConstructions[glyphId] ?: return emptyList()

        return construction.variants.map { record ->
            val variantChar = cmapTable.glyphIdToString(record.variantGlyph)
            GlyphVariant(
                glyphId = record.variantGlyph,
                glyphChar = variantChar.ifEmpty { glyphChar },
                advanceMeasurement = designToPx(record.advanceMeasurement, fontSizePx),
                fontFamily = fontFamily
            )
        }
    }

    override fun horizontalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant> {
        val variantsData = variants ?: return emptyList()
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return emptyList()
        val construction = variantsData.horizontalConstructions[glyphId] ?: return emptyList()

        return construction.variants.map { record ->
            val variantChar = cmapTable.glyphIdToString(record.variantGlyph)
            GlyphVariant(
                glyphId = record.variantGlyph,
                glyphChar = variantChar.ifEmpty { glyphChar },
                advanceMeasurement = designToPx(record.advanceMeasurement, fontSizePx),
                fontFamily = fontFamily
            )
        }
    }

    override fun verticalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly? {
        val variantsData = variants ?: return null
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return null
        val construction = variantsData.verticalConstructions[glyphId] ?: return null
        val assembly = construction.assembly ?: return null

        return GlyphAssembly(
            parts = assembly.parts.map { part ->
                val partChar = cmapTable.glyphIdToString(part.glyphId)
                GlyphPart(
                    glyphId = part.glyphId,
                    glyphChar = partChar,
                    startConnectorLength = designToPx(part.startConnectorLength, fontSizePx),
                    endConnectorLength = designToPx(part.endConnectorLength, fontSizePx),
                    fullAdvance = designToPx(part.fullAdvance, fontSizePx),
                    isExtender = part.isExtender,
                    fontFamily = fontFamily
                )
            },
            minConnectorOverlap = designToPx(variantsData.minConnectorOverlap, fontSizePx),
            italicsCorrection = designToPx(assembly.italicsCorrection, fontSizePx)
        )
    }

    override fun horizontalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly? {
        val variantsData = variants ?: return null
        val glyphId = cmapTable.stringToGlyphId(glyphChar)
        if (glyphId == 0) return null
        val construction = variantsData.horizontalConstructions[glyphId] ?: return null
        val assembly = construction.assembly ?: return null

        return GlyphAssembly(
            parts = assembly.parts.map { part ->
                val partChar = cmapTable.glyphIdToString(part.glyphId)
                GlyphPart(
                    glyphId = part.glyphId,
                    glyphChar = partChar,
                    startConnectorLength = designToPx(part.startConnectorLength, fontSizePx),
                    endConnectorLength = designToPx(part.endConnectorLength, fontSizePx),
                    fullAdvance = designToPx(part.fullAdvance, fontSizePx),
                    isExtender = part.isExtender,
                    fontFamily = fontFamily
                )
            },
            minConnectorOverlap = designToPx(variantsData.minConnectorOverlap, fontSizePx),
            italicsCorrection = designToPx(assembly.italicsCorrection, fontSizePx)
        )
    }

    // ─── 字体访问 ────────────────────────────────────────────────

    override fun fontFamilyFor(role: MathFontRole): FontFamily {
        // OTF 数学字体：所有角色都在同一字体中
        return fontFamily
    }

    override fun fontFamilyForVariant(glyphChar: String, variantIndex: Int): FontFamily {
        // OTF：所有变体在同一字体中
        return fontFamily
    }

    override fun fontBytes(role: MathFontRole): ByteArray {
        return fontBytes
    }

    // ─── Glyph Outline 提取 ──────────────────────────────────────

    override fun glyphPath(glyphId: Int, fontSizePx: Float): GlyphPathData? {
        return glyphOutlineProvider.getPath(glyphId, fontSizePx)
    }

    override fun charToGlyphId(text: String): Int {
        return cmapTable.stringToGlyphId(text)
    }

    // ─── 兜底常量 ────────────────────────────────────────────────

    companion object {
        private const val FALLBACK_AXIS_RATIO = 0.25f
        private const val FALLBACK_RULE_THICKNESS = 0.05f
        private const val FALLBACK_FRACTION_GAP = 0.15f
        private const val FALLBACK_SUPERSCRIPT_SHIFT = 0.45f
        private const val FALLBACK_SUBSCRIPT_SHIFT = 0.25f
        private const val FALLBACK_SCRIPT_GAP = 0.1f
    }
}
