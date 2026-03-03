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
import com.hrm.latex.renderer.utils.opentype.GlyphPathData

/**
 * 数学字体排版参数的统一抽象。
 *
 * 这是整个渲染引擎的"字体知识"入口。所有排版决策（间距、对齐、伸缩）
 * 都通过此接口获取字体特定的精确值，而非硬编码。
 *
 * 两种实现：
 * - [OtfMathFontProvider]: 从 OTF 文件的 MATH 表动态读取
 * - [TtfFontSetProvider]: 基于 KaTeX TTF 字体集，提供与当前等价的硬编码值
 */
internal interface MathFontProvider {

    // ─── 能力标识 ───

    /**
     * 是否具备真实的字形变体数据（来自 OTF MATH 表）。
     *
     * - `true`: [verticalVariants]/[horizontalVariants] 返回 MATH 表中的精确变体
     * - `false`: 变体数据是启发式估算（如 KaTeX Size1~4 阶梯），
     *   某些消费者（如 BigOperatorMeasurer）应优先使用自身的传统策略
     */
    val hasGlyphVariants: Boolean

    // ─── 全局排版常量（对应 MATH 表的 MathConstants） ───

    /** 数学轴高度（px），分数线、运算符的垂直对称中心 */
    fun axisHeight(fontSizePx: Float): Float

    /** 分数线粗细（px） */
    fun fractionRuleThickness(fontSizePx: Float): Float

    /** 分数分子与分数线的最小间距（px），Display 模式 */
    fun fractionNumeratorDisplayGap(fontSizePx: Float): Float

    /** 分数分子与分数线的最小间距（px），Text 模式 */
    fun fractionNumeratorGap(fontSizePx: Float): Float

    /** 分数分母与分数线的最小间距（px），Display 模式 */
    fun fractionDenominatorDisplayGap(fontSizePx: Float): Float

    /** 分数分母与分数线的最小间距（px），Text 模式 */
    fun fractionDenominatorGap(fontSizePx: Float): Float

    /** 上标上移量（px） */
    fun superscriptShiftUp(fontSizePx: Float): Float

    /** 下标下移量（px） */
    fun subscriptShiftDown(fontSizePx: Float): Float

    /** 上下标之间最小间距（px） */
    fun subSuperscriptGapMin(fontSizePx: Float): Float

    /** SCRIPT 样式缩放百分比 (如 70 表示 70%) */
    fun scriptPercentScaleDown(): Int

    /** SCRIPT_SCRIPT 样式缩放百分比 (如 50 表示 50%) */
    fun scriptScriptPercentScaleDown(): Int

    /** 根号内容顶部到横线的最小间距（px），Display 模式 */
    fun radicalDisplayVerticalGap(fontSizePx: Float): Float

    /** 根号内容顶部到横线的最小间距（px），非 Display 模式 */
    fun radicalVerticalGap(fontSizePx: Float): Float

    /** 根号横线粗细（px） */
    fun radicalRuleThickness(fontSizePx: Float): Float

    /** 上限与运算符之间的最小间距（px） */
    fun upperLimitGapMin(fontSizePx: Float): Float

    /** 下限与运算符之间的最小间距（px） */
    fun lowerLimitGapMin(fontSizePx: Float): Float

    /** 上划线/overbar 间距（px） */
    fun overbarVerticalGap(fontSizePx: Float): Float

    /** 下划线/underbar 间距（px） */
    fun underbarVerticalGap(fontSizePx: Float): Float

    /** 重音符号在此高度以上的基础字形不抬升（px） */
    fun accentBaseHeight(fontSizePx: Float): Float

    /** 堆叠元素最小间距（px），Display 模式 */
    fun stackDisplayGapMin(fontSizePx: Float): Float

    /** 堆叠元素最小间距（px），非 Display 模式 */
    fun stackGapMin(fontSizePx: Float): Float

    // ─── 逐字形信息（对应 MATH 表的 MathGlyphInfo） ───

    /**
     * 获取指定字形的斜体修正值（px）。
     *
     * 用于确定上标相对于基础字形的水平偏移，
     * 防止斜体字形的突出部分与上标碰撞。
     *
     * @param glyphChar Unicode 字符
     * @param fontSizePx 字号（px）
     * @return 斜体修正值，无数据时返回 0
     */
    fun italicCorrection(glyphChar: String, fontSizePx: Float): Float

    /**
     * 获取指定字形的重音附着点 x 坐标（px，相对于字形左边缘）。
     *
     * 用于 \hat, \tilde 等重音符号在基础字形上方的精确水平定位。
     *
     * @param glyphChar Unicode 字符
     * @param fontSizePx 字号（px）
     * @return 附着点 x 坐标，无数据时返回 -1（表示应使用字形宽度一半居中）
     */
    fun topAccentAttachment(glyphChar: String, fontSizePx: Float): Float

    /**
     * 获取数学字距调整（px）。
     *
     * 数学字距与普通字距不同：它根据相邻字形的**高度**动态调整，
     * 例如上标较矮时可以更靠近基础字形。
     *
     * @param glyphChar 基础字形
     * @param height 邻接元素在该高度处的位置（px）
     * @param fontSizePx 字号（px）
     * @param isRight true=右侧字距, false=左侧字距
     * @return 字距值（正值=推开，负值=拉近）
     */
    fun mathKern(glyphChar: String, height: Float, fontSizePx: Float, isRight: Boolean): Float

    // ─── 字形变体与组装（对应 MATH 表的 MathVariants） ───

    /**
     * 获取指定字形的垂直尺寸变体列表。
     *
     * 返回从小到大排列的预设变体。每个变体同时携带：
     * - `glyphId`: 字体内部 Glyph ID，配合 [glyphPath] 直接提取 Path 渲染（OTF 路径）
     * - `glyphChar` + `fontFamily`: Unicode 字符 + 字体，配合 TextMeasurer 渲染（TTF 降级路径）
     *
     * 消费方优先使用 `glyphId` + [glyphPath]（精确，绕过 Unicode 映射限制），
     * 失败时降级到 `glyphChar` + TextMeasurer。
     *
     * @param glyphChar 基础字形的 Unicode 字符（如 "("）
     * @param fontSizePx 字号（px）
     * @return 从小到大的变体列表，空列表表示无变体
     */
    fun verticalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant>

    /**
     * 获取指定字形的水平尺寸变体列表。
     *
     * 用于 \widehat、\widetilde、水平花括号等需要水平伸缩的装饰。
     * 数据格式与 [verticalVariants] 相同。
     */
    fun horizontalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant>

    /**
     * 获取指定字形的垂直组装部件。
     *
     * 当所有预设变体都不够大时，使用组装部件拼出任意高度的字形。
     * 组装由顶部件、底部件、中间件（可选）和扩展件（可重复）组成。
     *
     * 每个部件同时携带 `glyphId`（用于 Path 渲染）和 `glyphChar`（用于 TextMeasurer 降级）。
     *
     * @param glyphChar 基础字形
     * @param fontSizePx 字号（px）
     * @return 组装描述，null 表示该字形不支持组装
     */
    fun verticalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly?

    /**
     * 获取指定字形的水平组装部件。
     */
    fun horizontalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly?

    // ─── 字体访问 ───

    /**
     * 获取用于渲染指定数学角色的 FontFamily。
     *
     * OTF 实现：始终返回同一个 FontFamily（单字体文件包含一切）
     * TTF 实现：根据角色路由到不同的 FontFamily（main/math/ams/size1~4 等）
     */
    fun fontFamilyFor(role: MathFontRole): FontFamily

    /**
     * 获取渲染指定变体字形所需的 FontFamily。
     *
     * OTF 实现：返回主 FontFamily（变体字形在同一字体中）
     * TTF 实现：根据变体级别返回对应 Size 字体
     */
    fun fontFamilyForVariant(glyphChar: String, variantIndex: Int): FontFamily

    /**
     * 获取字体字节数据（用于 GlyphBoundsProvider 精确测量）。
     */
    fun fontBytes(role: MathFontRole): ByteArray?

    // ─── Glyph Outline（直接从字体提取 Path 数据） ───

    /**
     * 通过 Glyph ID 直接从字体文件提取字形轮廓 Path。
     *
     * 这是绕过 TextMeasurer 的核心能力，用于渲染 MATH 表中没有 Unicode 映射的
     * 变体字形（如积分符号的 display-size 变体）。
     *
     * @param glyphId 字体内部的 Glyph ID（从 MATH 表的 MathVariants 获取）
     * @param fontSizePx 目标字号（像素）
     * @return 缩放到目标字号的 Path 和尺寸信息，不支持时返回 null
     */
    fun glyphPath(glyphId: Int, fontSizePx: Float): GlyphPathData? = null

    /**
     * 将 Unicode 字符映射到 Glyph ID。
     *
     * 用于将 Unicode 字符串转换为 Glyph ID，再通过 [glyphPath] 提取 Path。
     *
     * @param text Unicode 字符（取第一个字符的码位）
     * @return Glyph ID，无映射时返回 0
     */
    fun charToGlyphId(text: String): Int = 0
}

/**
 * 字形尺寸变体。
 *
 * 同时携带 Glyph ID（用于 Path 渲染）和 Unicode 字符（用于 TextMeasurer 降级），
 * 统一支持 OTF 和 TTF 两种渲染路径。
 *
 * @property glyphId 字体内部 Glyph ID；OTF 字体提供精确值，TTF 字体为 0（不支持 Path 渲染）
 * @property glyphChar 变体字形的 Unicode 字符（可能为空串，表示该变体无 Unicode 映射）
 * @property advanceMeasurement 该变体的高度/宽度（px，取决于方向）
 * @property fontFamily 渲染此变体所需的 FontFamily
 */
data class GlyphVariant(
    val glyphId: Int,
    val glyphChar: String,
    val advanceMeasurement: Float,
    val fontFamily: FontFamily
)

/**
 * 字形组装描述。
 *
 * 定义如何用多个部件拼装出任意大小的字形（如超高括号、超宽花括号）。
 * 部件同时携带 Glyph ID 和 Unicode 字符，统一支持 Path 渲染和 TextMeasurer 降级。
 *
 * @property parts 组装部件列表（按从上到下或从左到右的顺序）
 * @property minConnectorOverlap 部件之间的最小连接器重叠量（px）
 * @property italicsCorrection 组装整体的斜体修正值（px），默认 0
 */
data class GlyphAssembly(
    val parts: List<GlyphPart>,
    val minConnectorOverlap: Float,
    val italicsCorrection: Float = 0f
)

/**
 * 字形组装的单个部件。
 *
 * 同时携带 Glyph ID（用于 Path 渲染）和 Unicode 字符（用于 TextMeasurer 降级）。
 *
 * @property glyphId 字体内部 Glyph ID；OTF 提供精确值，TTF 为 0
 * @property glyphChar 部件字形的 Unicode 字符（可能为空串）
 * @property startConnectorLength 起始连接器长度（px）— 与前一个部件重叠的区域
 * @property endConnectorLength 结束连接器长度（px）— 与后一个部件重叠的区域
 * @property fullAdvance 部件的完整前进宽度/高度（px）
 * @property isExtender 是否为扩展部件（可重复以增加总长度）
 * @property fontFamily 渲染此部件所需的 FontFamily
 */
data class GlyphPart(
    val glyphId: Int,
    val glyphChar: String,
    val startConnectorLength: Float,
    val endConnectorLength: Float,
    val fullAdvance: Float,
    val isExtender: Boolean,
    val fontFamily: FontFamily
)

/**
 * 数学字体中的字形角色。
 *
 * 用于 [MathFontProvider.fontFamilyFor] 的路由。
 * OTF 字体中这些角色都在同一字体文件中；
 * TTF 字体集中不同角色对应不同的字体文件。
 */
enum class MathFontRole {
    /** 正体文本、运算符、标点 */
    ROMAN,
    /** 数学斜体变量 */
    MATH_ITALIC,
    /** 黑板粗体 (ℝℕℤ) */
    BLACKBOARD_BOLD,
    /** 花体 */
    CALLIGRAPHIC,
    /** 哥特体 */
    FRAKTUR,
    /** 手写体 */
    SCRIPT,
    /** 无衬线 */
    SANS_SERIF,
    /** 等宽 */
    MONOSPACE,
    /** 大型运算符 (∑∫∏) */
    LARGE_OPERATOR,
    /** 定界符 (默认尺寸) */
    DELIMITER,
}
