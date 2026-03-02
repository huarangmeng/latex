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
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.utils.MathConstants

/**
 * TTF 字体集的 MathFontProvider 实现。
 *
 * 将现有的 KaTeX TTF 字体集和 [MathConstants] 硬编码常量适配到
 * [MathFontProvider] 统一接口，保证零回归。
 *
 * 所有常量值来自 [MathConstants]，通过 `fontSizePx * ratio` 转换为像素。
 * 斜体修正、重音附着使用启发式估算（与迁移前行为一致）。
 * 字形变体通过 KaTeX Size1~4 字体的 5 级阶梯实现。
 *
 * @param fontFamilies KaTeX 12 槽位字体家族（含字节数据）
 */
class TtfFontSetProvider(
    private val fontFamilies: LatexFontFamilies
) : MathFontProvider {

    override val hasGlyphVariants: Boolean = false

    // ─── 全局排版常量 ────────────────────────────────────────────

    override fun axisHeight(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.MATH_AXIS_HEIGHT_RATIO
    }

    override fun fractionRuleThickness(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
    }

    override fun fractionNumeratorDisplayGap(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_GAP
    }

    override fun fractionNumeratorGap(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_GAP
    }

    override fun fractionDenominatorDisplayGap(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_GAP
    }

    override fun fractionDenominatorGap(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_GAP
    }

    override fun superscriptShiftUp(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.SUPERSCRIPT_SHIFT
    }

    override fun subscriptShiftDown(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.SUBSCRIPT_SHIFT
    }

    override fun subSuperscriptGapMin(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.SCRIPT_MIN_GAP
    }

    override fun scriptPercentScaleDown(): Int {
        return (MathConstants.SCRIPT_SCALE * 100).toInt()  // 0.7 → 70
    }

    override fun scriptScriptPercentScaleDown(): Int {
        return (MathConstants.SCRIPT_SCRIPT_SCALE * 100).toInt()  // 0.5 → 50
    }

    override fun radicalDisplayVerticalGap(fontSizePx: Float): Float {
        // Display 模式：ruleThickness * RADICAL_TOP_GAP_MULTIPLIER
        return fractionRuleThickness(fontSizePx) * MathConstants.RADICAL_TOP_GAP_MULTIPLIER
    }

    override fun radicalVerticalGap(fontSizePx: Float): Float {
        // 非 Display 模式：ruleThickness * (RADICAL_TOP_GAP_MULTIPLIER * 0.7)
        return fractionRuleThickness(fontSizePx) * MathConstants.RADICAL_TOP_GAP_MULTIPLIER * 0.7f
    }

    override fun radicalRuleThickness(fontSizePx: Float): Float {
        return fractionRuleThickness(fontSizePx)
    }

    override fun upperLimitGapMin(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.SYMBOL_OP_LIMIT_GAP
    }

    override fun lowerLimitGapMin(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.SYMBOL_OP_LIMIT_GAP
    }

    override fun overbarVerticalGap(fontSizePx: Float): Float {
        return fractionRuleThickness(fontSizePx) * 3f
    }

    override fun underbarVerticalGap(fontSizePx: Float): Float {
        return fractionRuleThickness(fontSizePx) * 3f
    }

    override fun accentBaseHeight(fontSizePx: Float): Float {
        // KaTeX x-height 近似值
        return fontSizePx * 0.45f
    }

    override fun stackDisplayGapMin(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_GAP * 2f
    }

    override fun stackGapMin(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_GAP
    }

    // ─── 逐字形信息（启发式估算） ──────────────────────────────

    override fun italicCorrection(glyphChar: String, fontSizePx: Float): Float {
        if (glyphChar.isEmpty()) return 0f
        val firstChar = glyphChar.first()
        return when {
            firstChar.isUpperCase() -> fontSizePx * MathConstants.ITALIC_RIGHT_OVERHANG_UPPER
            firstChar.isLowerCase() -> fontSizePx * MathConstants.ITALIC_RIGHT_OVERHANG_LOWER
            else -> fontSizePx * MathConstants.ITALIC_RIGHT_OVERHANG_OTHER
        }
    }

    override fun topAccentAttachment(glyphChar: String, fontSizePx: Float): Float {
        // TTF 无逐字形数据，返回 -1 表示应使用居中
        return -1f
    }

    override fun mathKern(
        glyphChar: String,
        height: Float,
        fontSizePx: Float,
        isRight: Boolean
    ): Float {
        // TTF 无数学字距数据
        return 0f
    }

    // ─── 字形变体（KaTeX Size1~4 阶梯） ────────────────────────

    override fun verticalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant> {
        // TTF 方案：返回 5 级字体的逻辑变体（Main + Size1~4）
        // advanceMeasurement 设为递增的估算值，实际渲染时由 TextMeasurer 决定
        return listOf(
            GlyphVariant(glyphChar, fontSizePx * 1.0f, fontFamilies.main),
            GlyphVariant(glyphChar, fontSizePx * 1.2f, fontFamilies.size1),
            GlyphVariant(glyphChar, fontSizePx * 1.8f, fontFamilies.size2),
            GlyphVariant(glyphChar, fontSizePx * 2.4f, fontFamilies.size3),
            GlyphVariant(glyphChar, fontSizePx * 3.0f, fontFamilies.size4),
        )
    }

    override fun horizontalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant> {
        // TTF 不支持水平变体
        return emptyList()
    }

    override fun verticalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly? {
        // TTF 不支持字形组装
        return null
    }

    override fun horizontalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly? {
        // TTF 不支持字形组装
        return null
    }

    // ─── 字体访问 ────────────────────────────────────────────────

    override fun fontFamilyFor(role: MathFontRole): FontFamily {
        return when (role) {
            MathFontRole.ROMAN -> fontFamilies.main
            MathFontRole.MATH_ITALIC -> fontFamilies.math
            MathFontRole.BLACKBOARD_BOLD -> fontFamilies.ams
            MathFontRole.CALLIGRAPHIC -> fontFamilies.caligraphic
            MathFontRole.FRAKTUR -> fontFamilies.fraktur
            MathFontRole.SCRIPT -> fontFamilies.script
            MathFontRole.SANS_SERIF -> fontFamilies.sansSerif
            MathFontRole.MONOSPACE -> fontFamilies.monospace
            MathFontRole.LARGE_OPERATOR -> fontFamilies.main
            MathFontRole.DELIMITER -> fontFamilies.main
        }
    }

    override fun fontFamilyForVariant(glyphChar: String, variantIndex: Int): FontFamily {
        return when (variantIndex) {
            0 -> fontFamilies.main
            1 -> fontFamilies.size1
            2 -> fontFamilies.size2
            3 -> fontFamilies.size3
            4 -> fontFamilies.size4
            else -> fontFamilies.size4
        }
    }

    override fun fontBytes(role: MathFontRole): ByteArray? {
        return when (role) {
            MathFontRole.ROMAN, MathFontRole.LARGE_OPERATOR, MathFontRole.DELIMITER ->
                fontFamilies.mainBytes
            MathFontRole.MATH_ITALIC -> fontFamilies.mathBytes
            else -> fontFamilies.mainBytes
        }
    }
}
