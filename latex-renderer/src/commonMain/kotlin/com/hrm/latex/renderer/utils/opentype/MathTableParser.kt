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

package com.hrm.latex.renderer.utils.opentype

// ─── MATH 表数据模型 ─────────────────────────────────────────────

/**
 * OpenType MATH 表的完整解析结果。
 *
 * 参考：https://learn.microsoft.com/en-us/typography/opentype/spec/math
 */
internal class MathTableData(
    val constants: MathConstantsData,
    val glyphInfo: MathGlyphInfoData,
    val variants: MathVariantsData
)

/**
 * MathConstants 子表：~60 个全局排版常量。
 *
 * 所有值以设计空间单位 (design units) 表示，需通过
 * `value * fontSizePx / unitsPerEm` 转换为像素。
 *
 * 百分比值 (scriptPercentScaleDown 等) 直接是百分数整数。
 */
internal data class MathConstantsData(
    // ── 脚本缩放 (百分比) ──
    val scriptPercentScaleDown: Int,           // 建议 80%
    val scriptScriptPercentScaleDown: Int,     // 建议 60%

    // ── 间距与间隙 ──
    val delimitedSubFormulaMinHeight: Int,
    val displayOperatorMinHeight: Int,

    // ── MathValueRecord 值 (设计空间单位) ──
    val mathLeading: Int,
    val axisHeight: Int,
    val accentBaseHeight: Int,
    val flattenedAccentBaseHeight: Int,
    val subscriptShiftDown: Int,
    val subscriptTopMax: Int,
    val subscriptBaselineDropMin: Int,
    val superscriptShiftUp: Int,
    val superscriptShiftUpCramped: Int,
    val superscriptBottomMin: Int,
    val superscriptBaselineDropMax: Int,
    val subSuperscriptGapMin: Int,
    val superscriptBottomMaxWithSubscript: Int,
    val spaceAfterScript: Int,
    val upperLimitGapMin: Int,
    val upperLimitBaselineRiseMin: Int,
    val lowerLimitGapMin: Int,
    val lowerLimitBaselineDropMin: Int,
    val stackTopShiftUp: Int,
    val stackTopDisplayStyleShiftUp: Int,
    val stackBottomShiftDown: Int,
    val stackBottomDisplayStyleShiftDown: Int,
    val stackGapMin: Int,
    val stackDisplayStyleGapMin: Int,
    val stretchStackTopShiftUp: Int,
    val stretchStackBottomShiftDown: Int,
    val stretchStackGapAboveMin: Int,
    val stretchStackGapBelowMin: Int,
    val fractionNumeratorShiftUp: Int,
    val fractionNumeratorDisplayStyleShiftUp: Int,
    val fractionDenominatorShiftDown: Int,
    val fractionDenominatorDisplayStyleShiftDown: Int,
    val fractionNumeratorGapMin: Int,
    val fractionNumeratorDisplayStyleGapMin: Int,
    val fractionRuleThickness: Int,
    val fractionDenominatorGapMin: Int,
    val fractionDenominatorDisplayStyleGapMin: Int,
    val skewedFractionHorizontalGap: Int,
    val skewedFractionVerticalGap: Int,
    val overbarVerticalGap: Int,
    val overbarRuleThickness: Int,
    val overbarExtraAscender: Int,
    val underbarVerticalGap: Int,
    val underbarRuleThickness: Int,
    val underbarExtraDescender: Int,
    val radicalVerticalGap: Int,
    val radicalDisplayStyleVerticalGap: Int,
    val radicalRuleThickness: Int,
    val radicalExtraAscender: Int,
    val radicalKernBeforeDegree: Int,
    val radicalKernAfterDegree: Int,
    val radicalDegreeBottomRaisePercent: Int
)

/**
 * MathGlyphInfo 子表：逐字形的数学排版信息。
 *
 * 包含斜体修正、重音附着点、扩展形状标记、数学字距。
 */
internal class MathGlyphInfoData(
    /** Glyph ID → 斜体修正值 (设计空间单位) */
    val italicsCorrections: Map<Int, Int>,
    /** Glyph ID → 重音附着点 x 坐标 (设计空间单位) */
    val topAccentAttachments: Map<Int, Int>,
    /** 扩展形状的 Glyph ID 集合 */
    val extendedShapes: Set<Int>,
    /** Glyph ID → 四方向数学字距表 */
    val mathKerns: Map<Int, MathKernInfoRecord>
)

/**
 * 单个 glyph 的数学字距信息（四个方向）。
 *
 * 每个方向是一个高度→字距的分段函数。
 */
internal data class MathKernInfoRecord(
    val topRight: MathKernTable?,
    val topLeft: MathKernTable?,
    val bottomRight: MathKernTable?,
    val bottomLeft: MathKernTable?
)

/**
 * 单方向的数学字距表：高度敏感的分段字距函数。
 *
 * 给定一个高度 h，返回对应的字距值：
 * - 如果 h < heights[0]，返回 kerns[0]
 * - 如果 heights[i-1] <= h < heights[i]，返回 kerns[i]
 * - 如果 h >= heights[last]，返回 kerns[last]
 */
internal data class MathKernTable(
    val heights: List<Int>,   // correctionHeight 数组 (设计空间单位)
    val kerns: List<Int>      // kern 值数组，长度 = heights.size + 1
)

/**
 * MathVariants 子表：字形尺寸变体与组装。
 */
internal class MathVariantsData(
    /** 部件之间最小连接器重叠量 (设计空间单位) */
    val minConnectorOverlap: Int,
    /** 垂直方向：Glyph ID → 变体构造 */
    val verticalConstructions: Map<Int, GlyphConstructionData>,
    /** 水平方向：Glyph ID → 变体构造 */
    val horizontalConstructions: Map<Int, GlyphConstructionData>
)

/**
 * 单个字形的变体构造：预设变体列表 + 可选的组装描述。
 */
internal data class GlyphConstructionData(
    /** 组装描述（用部件拼装任意大小），null 表示不支持组装 */
    val assembly: GlyphAssemblyData?,
    /** 预设的不同大小变体列表（从小到大） */
    val variants: List<GlyphVariantRecord>
)

/**
 * 预设字形变体记录。
 *
 * @property variantGlyph 变体的 Glyph ID
 * @property advanceMeasurement 该变体在增长方向上的尺寸 (设计空间单位)
 */
internal data class GlyphVariantRecord(
    val variantGlyph: Int,
    val advanceMeasurement: Int
)

/**
 * 字形组装描述：如何用部件拼装任意大小的字形。
 */
internal data class GlyphAssemblyData(
    val italicsCorrection: Int,
    val parts: List<GlyphPartRecord>
)

/**
 * 字形组装的单个部件。
 *
 * @property glyphId 部件的 Glyph ID
 * @property startConnectorLength 起始连接器长度 (设计空间单位)
 * @property endConnectorLength 结束连接器长度 (设计空间单位)
 * @property fullAdvance 部件完整尺寸 (设计空间单位)
 * @property isExtender 是否为可重复的扩展部件
 */
internal data class GlyphPartRecord(
    val glyphId: Int,
    val startConnectorLength: Int,
    val endConnectorLength: Int,
    val fullAdvance: Int,
    val isExtender: Boolean
)

// ─── MATH 表解析器 ───────────────────────────────────────────────

/**
 * OpenType MATH 表解析器。
 *
 * 从字体二进制数据中解析 MATH 表的三个子表：
 * - MathConstants: 全局排版常量
 * - MathGlyphInfo: 逐字形信息
 * - MathVariants: 字形变体与组装
 *
 * 所有返回值以设计空间单位 (design units) 表示。
 * 调用方负责通过 `value * fontSizePx / unitsPerEm` 转换为像素。
 */
internal class MathTableParser(
    private val reader: BinaryReader,
    private val tableOffset: Int
) {

    /**
     * 解析整个 MATH 表。
     *
     * @return 解析结果，如果 MATH 表为空或解析失败返回 null
     */
    fun parse(): MathTableData? {
        return try {
            // MATH Header: Version(4) + MathConstantsOffset(2) + MathGlyphInfoOffset(2) + MathVariantsOffset(2)
            val majorVersion = reader.readUInt16(tableOffset)
            val minorVersion = reader.readUInt16(tableOffset + 2)

            // 版本检查：当前仅支持 1.0
            if (majorVersion != 1) return null

            val constantsOffset = reader.readUInt16(tableOffset + 4)
            val glyphInfoOffset = reader.readUInt16(tableOffset + 6)
            val variantsOffset = reader.readUInt16(tableOffset + 8)

            val constants = parseMathConstants(tableOffset + constantsOffset)
            val glyphInfo = parseMathGlyphInfo(tableOffset + glyphInfoOffset)
            val variants = parseMathVariants(tableOffset + variantsOffset)

            MathTableData(constants, glyphInfo, variants)
        } catch (e: Exception) {
            null
        }
    }

    // ─── MathConstants 解析 ──────────────────────────────────────

    /**
     * 解析 MathConstants 子表。
     *
     * 前 4 个是简单整数值 (int16/uint16)。
     * 后续 51 个是 MathValueRecord (value: int16 + deviceOffset: uint16)。
     * 最后 1 个是百分比值 (int16)。
     */
    private fun parseMathConstants(offset: Int): MathConstantsData {
        var pos = offset

        // 前 4 个简单值
        val scriptPercentScaleDown = reader.readInt16(pos); pos += 2
        val scriptScriptPercentScaleDown = reader.readInt16(pos); pos += 2
        val delimitedSubFormulaMinHeight = reader.readUInt16(pos); pos += 2
        val displayOperatorMinHeight = reader.readUInt16(pos); pos += 2

        // 接下来 51 个 MathValueRecord（每个 4 字节：value(int16) + deviceTableOffset(uint16)）
        fun readMathValue(): Int {
            val value = reader.readInt16(pos); pos += 4  // 跳过 deviceTableOffset
            return value
        }

        return MathConstantsData(
            scriptPercentScaleDown = scriptPercentScaleDown,
            scriptScriptPercentScaleDown = scriptScriptPercentScaleDown,
            delimitedSubFormulaMinHeight = delimitedSubFormulaMinHeight,
            displayOperatorMinHeight = displayOperatorMinHeight,
            mathLeading = readMathValue(),
            axisHeight = readMathValue(),
            accentBaseHeight = readMathValue(),
            flattenedAccentBaseHeight = readMathValue(),
            subscriptShiftDown = readMathValue(),
            subscriptTopMax = readMathValue(),
            subscriptBaselineDropMin = readMathValue(),
            superscriptShiftUp = readMathValue(),
            superscriptShiftUpCramped = readMathValue(),
            superscriptBottomMin = readMathValue(),
            superscriptBaselineDropMax = readMathValue(),
            subSuperscriptGapMin = readMathValue(),
            superscriptBottomMaxWithSubscript = readMathValue(),
            spaceAfterScript = readMathValue(),
            upperLimitGapMin = readMathValue(),
            upperLimitBaselineRiseMin = readMathValue(),
            lowerLimitGapMin = readMathValue(),
            lowerLimitBaselineDropMin = readMathValue(),
            stackTopShiftUp = readMathValue(),
            stackTopDisplayStyleShiftUp = readMathValue(),
            stackBottomShiftDown = readMathValue(),
            stackBottomDisplayStyleShiftDown = readMathValue(),
            stackGapMin = readMathValue(),
            stackDisplayStyleGapMin = readMathValue(),
            stretchStackTopShiftUp = readMathValue(),
            stretchStackBottomShiftDown = readMathValue(),
            stretchStackGapAboveMin = readMathValue(),
            stretchStackGapBelowMin = readMathValue(),
            fractionNumeratorShiftUp = readMathValue(),
            fractionNumeratorDisplayStyleShiftUp = readMathValue(),
            fractionDenominatorShiftDown = readMathValue(),
            fractionDenominatorDisplayStyleShiftDown = readMathValue(),
            fractionNumeratorGapMin = readMathValue(),
            fractionNumeratorDisplayStyleGapMin = readMathValue(),
            fractionRuleThickness = readMathValue(),
            fractionDenominatorGapMin = readMathValue(),
            fractionDenominatorDisplayStyleGapMin = readMathValue(),
            skewedFractionHorizontalGap = readMathValue(),
            skewedFractionVerticalGap = readMathValue(),
            overbarVerticalGap = readMathValue(),
            overbarRuleThickness = readMathValue(),
            overbarExtraAscender = readMathValue(),
            underbarVerticalGap = readMathValue(),
            underbarRuleThickness = readMathValue(),
            underbarExtraDescender = readMathValue(),
            radicalVerticalGap = readMathValue(),
            radicalDisplayStyleVerticalGap = readMathValue(),
            radicalRuleThickness = readMathValue(),
            radicalExtraAscender = readMathValue(),
            radicalKernBeforeDegree = readMathValue(),
            radicalKernAfterDegree = readMathValue(),
            radicalDegreeBottomRaisePercent = reader.readInt16(pos) // 最后一个是简单 int16
        )
    }

    // ─── MathGlyphInfo 解析 ──────────────────────────────────────

    private fun parseMathGlyphInfo(offset: Int): MathGlyphInfoData {
        val italicsCorrectionOffset = reader.readUInt16(offset)
        val topAccentOffset = reader.readUInt16(offset + 2)
        val extendedShapeOffset = reader.readUInt16(offset + 4)
        val mathKernOffset = reader.readUInt16(offset + 6)

        val italicsCorrections = if (italicsCorrectionOffset != 0) {
            parseCoverageValueMap(offset + italicsCorrectionOffset)
        } else emptyMap()

        val topAccentAttachments = if (topAccentOffset != 0) {
            parseCoverageValueMap(offset + topAccentOffset)
        } else emptyMap()

        val extendedShapes = if (extendedShapeOffset != 0) {
            parseCoverage(offset + extendedShapeOffset).toSet()
        } else emptySet()

        val mathKerns = if (mathKernOffset != 0) {
            parseMathKernInfo(offset + mathKernOffset)
        } else emptyMap()

        return MathGlyphInfoData(italicsCorrections, topAccentAttachments, extendedShapes, mathKerns)
    }

    /**
     * 解析 Coverage + MathValueRecord 数组的组合结构。
     *
     * 用于 MathItalicsCorrectionInfo 和 MathTopAccentAttachment。
     * 结构：CoverageOffset(2) + count(2) + MathValueRecord[count]
     */
    private fun parseCoverageValueMap(offset: Int): Map<Int, Int> {
        val coverageOffset = reader.readUInt16(offset)
        val count = reader.readUInt16(offset + 2)
        val glyphIds = parseCoverage(offset + coverageOffset)
        val result = mutableMapOf<Int, Int>()

        for (i in 0 until minOf(count, glyphIds.size)) {
            // MathValueRecord: value(int16) + deviceTableOffset(uint16)
            val value = reader.readInt16(offset + 4 + i * 4)
            result[glyphIds[i]] = value
        }
        return result
    }

    /**
     * 解析 MathKernInfo 表。
     *
     * 结构：CoverageOffset(2) + count(2) + MathKernInfoRecord[count]
     * 每个 MathKernInfoRecord 包含 4 个偏移量（四个方向的 MathKernTable）
     */
    private fun parseMathKernInfo(offset: Int): Map<Int, MathKernInfoRecord> {
        val coverageOffset = reader.readUInt16(offset)
        val count = reader.readUInt16(offset + 2)
        val glyphIds = parseCoverage(offset + coverageOffset)
        val result = mutableMapOf<Int, MathKernInfoRecord>()

        for (i in 0 until minOf(count, glyphIds.size)) {
            val recordOffset = offset + 4 + i * 8
            val topRightOff = reader.readUInt16(recordOffset)
            val topLeftOff = reader.readUInt16(recordOffset + 2)
            val bottomRightOff = reader.readUInt16(recordOffset + 4)
            val bottomLeftOff = reader.readUInt16(recordOffset + 6)

            result[glyphIds[i]] = MathKernInfoRecord(
                topRight = if (topRightOff != 0) parseMathKernTable(offset + topRightOff) else null,
                topLeft = if (topLeftOff != 0) parseMathKernTable(offset + topLeftOff) else null,
                bottomRight = if (bottomRightOff != 0) parseMathKernTable(offset + bottomRightOff) else null,
                bottomLeft = if (bottomLeftOff != 0) parseMathKernTable(offset + bottomLeftOff) else null
            )
        }
        return result
    }

    /**
     * 解析 MathKernTable：高度敏感的分段字距函数。
     *
     * 结构：heightCount(2) + MathValueRecord[heightCount] (heights) + MathValueRecord[heightCount+1] (kerns)
     */
    private fun parseMathKernTable(offset: Int): MathKernTable {
        val heightCount = reader.readUInt16(offset)
        val heights = mutableListOf<Int>()
        val kerns = mutableListOf<Int>()

        var pos = offset + 2
        // 读取 heightCount 个 correctionHeight (MathValueRecord)
        for (i in 0 until heightCount) {
            heights.add(reader.readInt16(pos))
            pos += 4  // 跳过 deviceTableOffset
        }
        // 读取 heightCount + 1 个 kern 值 (MathValueRecord)
        for (i in 0..heightCount) {
            kerns.add(reader.readInt16(pos))
            pos += 4
        }

        return MathKernTable(heights, kerns)
    }

    // ─── MathVariants 解析 ──────────────────────────────────────

    private fun parseMathVariants(offset: Int): MathVariantsData {
        val minConnectorOverlap = reader.readUInt16(offset)
        val vertGlyphCoverageOffset = reader.readUInt16(offset + 2)
        val horizGlyphCoverageOffset = reader.readUInt16(offset + 4)
        val vertGlyphCount = reader.readUInt16(offset + 6)
        val horizGlyphCount = reader.readUInt16(offset + 8)

        // 垂直方向构造
        val vertGlyphIds = if (vertGlyphCoverageOffset != 0) {
            parseCoverage(offset + vertGlyphCoverageOffset)
        } else emptyList()

        val vertConstructions = mutableMapOf<Int, GlyphConstructionData>()
        val vertArrayStart = offset + 10
        for (i in 0 until minOf(vertGlyphCount, vertGlyphIds.size)) {
            val constructionOffset = reader.readUInt16(vertArrayStart + i * 2)
            if (constructionOffset != 0) {
                vertConstructions[vertGlyphIds[i]] =
                    parseGlyphConstruction(offset + constructionOffset)
            }
        }

        // 水平方向构造
        val horizGlyphIds = if (horizGlyphCoverageOffset != 0) {
            parseCoverage(offset + horizGlyphCoverageOffset)
        } else emptyList()

        val horizConstructions = mutableMapOf<Int, GlyphConstructionData>()
        val horizArrayStart = vertArrayStart + vertGlyphCount * 2
        for (i in 0 until minOf(horizGlyphCount, horizGlyphIds.size)) {
            val constructionOffset = reader.readUInt16(horizArrayStart + i * 2)
            if (constructionOffset != 0) {
                horizConstructions[horizGlyphIds[i]] =
                    parseGlyphConstruction(offset + constructionOffset)
            }
        }

        return MathVariantsData(minConnectorOverlap, vertConstructions, horizConstructions)
    }

    /**
     * 解析 MathGlyphConstruction。
     *
     * 结构：GlyphAssemblyOffset(2) + variantCount(2) + MathGlyphVariantRecord[variantCount]
     */
    private fun parseGlyphConstruction(offset: Int): GlyphConstructionData {
        val assemblyOffset = reader.readUInt16(offset)
        val variantCount = reader.readUInt16(offset + 2)

        val variants = mutableListOf<GlyphVariantRecord>()
        for (i in 0 until variantCount) {
            val recordOffset = offset + 4 + i * 4
            variants.add(
                GlyphVariantRecord(
                    variantGlyph = reader.readUInt16(recordOffset),
                    advanceMeasurement = reader.readUInt16(recordOffset + 2)
                )
            )
        }

        val assembly = if (assemblyOffset != 0) {
            parseGlyphAssembly(offset + assemblyOffset)
        } else null

        return GlyphConstructionData(assembly, variants)
    }

    /**
     * 解析 GlyphAssembly。
     *
     * 结构：ItalicsCorrection(MathValueRecord:4) + partCount(2) + GlyphPartRecord[partCount]
     */
    private fun parseGlyphAssembly(offset: Int): GlyphAssemblyData {
        val italicsCorrection = reader.readInt16(offset)
        // Skip deviceTableOffset (2 bytes)
        val partCount = reader.readUInt16(offset + 4)

        val parts = mutableListOf<GlyphPartRecord>()
        for (i in 0 until partCount) {
            val partOffset = offset + 6 + i * 10
            parts.add(
                GlyphPartRecord(
                    glyphId = reader.readUInt16(partOffset),
                    startConnectorLength = reader.readUInt16(partOffset + 2),
                    endConnectorLength = reader.readUInt16(partOffset + 4),
                    fullAdvance = reader.readUInt16(partOffset + 6),
                    isExtender = (reader.readUInt16(partOffset + 8) and 0x0001) != 0
                )
            )
        }

        return GlyphAssemblyData(italicsCorrection, parts)
    }

    // ─── Coverage 表解析 ─────────────────────────────────────────

    /**
     * 解析 Coverage 表，返回有序的 Glyph ID 列表。
     *
     * Format 1: 直接枚举 Glyph ID
     * Format 2: 范围列表 [startGlyph, endGlyph]
     */
    private fun parseCoverage(offset: Int): List<Int> {
        val format = reader.readUInt16(offset)
        return when (format) {
            1 -> parseCoverageFormat1(offset)
            2 -> parseCoverageFormat2(offset)
            else -> emptyList()
        }
    }

    private fun parseCoverageFormat1(offset: Int): List<Int> {
        val glyphCount = reader.readUInt16(offset + 2)
        return (0 until glyphCount).map { reader.readUInt16(offset + 4 + it * 2) }
    }

    private fun parseCoverageFormat2(offset: Int): List<Int> {
        val rangeCount = reader.readUInt16(offset + 2)
        val result = mutableListOf<Int>()
        for (i in 0 until rangeCount) {
            val rangeOffset = offset + 4 + i * 6
            val startGlyph = reader.readUInt16(rangeOffset)
            val endGlyph = reader.readUInt16(rangeOffset + 2)
            // startCoverageIndex at rangeOffset + 4 (not needed for our purpose)
            for (glyph in startGlyph..endGlyph) {
                result.add(glyph)
            }
        }
        return result
    }
}
