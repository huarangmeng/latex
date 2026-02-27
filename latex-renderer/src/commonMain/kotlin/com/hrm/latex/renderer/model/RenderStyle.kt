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


package com.hrm.latex.renderer.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.utils.FontBytesCache
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.parseColor

/**
 * 子表达式高亮配置
 *
 * 支持按 LaTeX 子串模式或 AST 节点位置索引 来指定高亮区域。
 * 高亮在 Draw 阶段以半透明背景矩形叠加渲染。
 *
 * @param ranges 高亮范围列表
 */
data class HighlightConfig(
    val ranges: List<HighlightRange> = emptyList()
)

/**
 * 单个高亮范围
 *
 * @param pattern LaTeX 子串模式（精确匹配渲染时的文本内容）。
 *        为 null 时通过 [nodeIndices] 指定。
 * @param nodeIndices 文档根节点 children 的索引范围（从 0 开始，含首尾）。
 *        为 null 时通过 [pattern] 指定。
 * @param color 高亮背景色
 * @param borderColor 高亮边框色（null 则不绘制边框）
 */
data class HighlightRange(
    val pattern: String? = null,
    val nodeIndices: IntRange? = null,
    val color: Color = Color(0x3300AAFF),
    val borderColor: Color? = null
)

/**
 * LaTeX 渲染配置（用户外部设置）
 */
data class LatexConfig(
    val fontSize: TextUnit = 20.sp,
    val color: Color = Color.Black,
    val darkColor: Color = Color.White,
    val backgroundColor: Color = Color.Transparent,
    val darkBackgroundColor: Color = Color.Transparent,
    val baseFontFamily: FontFamily? = null,
    val fontFamilies: LatexFontFamilies? = null,
    val lineBreaking: LineBreakingConfig = LineBreakingConfig(),
    val highlight: HighlightConfig = HighlightConfig(),
    val accessibilityEnabled: Boolean = false
)

/**
 * line breaking configuration
 *
 * @property enabled whether automatic line breaking is enabled
 * @property maxWidth maximum line width in pixels, null means no limit
 */
data class LineBreakingConfig(
    val enabled: Boolean = false,
    val maxWidth: Float? = null
)

/**
 * 数学样式模式 (TeXbook §702)
 *
 * 影响字号缩放和间距行为：
 * - DISPLAY/TEXT: 全尺寸，MEDIUM/THICK 间距生效
 * - SCRIPT: 0.7x 缩放，MEDIUM/THICK 间距降为 0
 * - SCRIPT_SCRIPT: 0.5x 缩放，MEDIUM/THICK 间距降为 0
 */
enum class MathStyle(val scriptLevel: Int) {
    DISPLAY(0),
    TEXT(0),
    SCRIPT(1),
    SCRIPT_SCRIPT(2);

    val isScript get() = scriptLevel > 0

    fun scaleFactor(): Float = when (this) {
        DISPLAY, TEXT -> 1.0f
        SCRIPT -> MathConstants.SCRIPT_SCALE
        SCRIPT_SCRIPT -> MathConstants.SCRIPT_SCRIPT_SCALE
    }

    /** 进入上下标时的样式转换 */
    fun toScript(): MathStyle = when (this) {
        DISPLAY, TEXT -> SCRIPT
        SCRIPT, SCRIPT_SCRIPT -> SCRIPT_SCRIPT
    }

    /** 分数子式的样式转换 (TeXbook §694) */
    fun toFractionChild(): MathStyle = when (this) {
        DISPLAY -> TEXT
        TEXT -> SCRIPT
        SCRIPT -> SCRIPT_SCRIPT
        SCRIPT_SCRIPT -> SCRIPT_SCRIPT
    }

    /** 大型运算符上下限的样式转换 */
    fun toLimit(): MathStyle = toScript()
}

/**
 * 字体变体类型
 */
enum class FontVariant {
    NORMAL,
    BLACKBOARD_BOLD,
    CALLIGRAPHIC,
    FRAKTUR,
    SCRIPT
}

/**
 * 内部渲染上下文（渲染树遍历过程中的状态）
 *
 * 使用 data class 以便利的 copy() 操作。
 * 注意：不应将 RenderContext 作为 remember 的 key —— 应使用
 * 产生它的输入（LatexConfig + isDark 等）作为 key。
 */
internal data class RenderContext(
    val fontSize: TextUnit,
    val color: Color,
    val errorColor: Color = Color(0xFFCC0000),
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val fontVariant: FontVariant = FontVariant.NORMAL,
    val fontFamilies: LatexFontFamilies? = null,
    val isVariantFontFamily: Boolean = false,
    val mathStyle: MathStyle = MathStyle.DISPLAY,
    val bigOpHeightHint: Float? = null,
    val maxLineWidth: Float? = null,
    val lineBreakingEnabled: Boolean = false,
    val fontBytesCache: FontBytesCache? = null,
    val highlightRanges: List<HighlightRange> = emptyList()
)

/**
 * 从外部配置创建初始上下文
 */
internal fun LatexConfig.toContext(
    isDark: Boolean,
    fontFamilies: LatexFontFamilies
): RenderContext {
    val resolvedColor = if (isDark) {
        if (darkColor != Color.Unspecified) darkColor else Color.White
    } else {
        if (color != Color.Unspecified) color else Color.Black
    }

    val resolvedErrorColor = if (isDark) Color(0xFFFF6666) else Color(0xFFCC0000)

    return RenderContext(
        fontSize = fontSize,
        color = resolvedColor,
        errorColor = resolvedErrorColor,
        fontFamily = baseFontFamily ?: fontFamilies.main,
        fontFamilies = fontFamilies,
        isVariantFontFamily = false,
        maxLineWidth = if (lineBreaking.enabled) lineBreaking.maxWidth else null,
        lineBreakingEnabled = lineBreaking.enabled,
        highlightRanges = highlight.ranges
    )
}

internal fun RenderContext.textStyle(): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontFamily = fontFamily
)

/**
 * 进入上下标时的样式转换：使用 MathStyle 状态机自动决定字号
 */
internal fun RenderContext.toScriptStyle(): RenderContext {
    val newStyle = mathStyle.toScript()
    return copy(
        fontSize = fontSize * (newStyle.scaleFactor() / mathStyle.scaleFactor()),
        mathStyle = newStyle
    )
}

/**
 * 进入分数子式时的样式转换
 */
internal fun RenderContext.toFractionChildStyle(): RenderContext {
    val newStyle = mathStyle.toFractionChild()
    val scale = MathConstants.FRACTION_CHILD_SCALE * (newStyle.scaleFactor() / mathStyle.scaleFactor())
    return copy(
        fontSize = fontSize * scale,
        mathStyle = newStyle
    )
}

/**
 * 进入大型运算符上下限时的样式转换
 */
internal fun RenderContext.toLimitStyle(): RenderContext {
    val newStyle = mathStyle.toLimit()
    return copy(
        fontSize = fontSize * (newStyle.scaleFactor() / mathStyle.scaleFactor()),
        mathStyle = newStyle
    )
}

internal fun RenderContext.shrink(factor: Float): RenderContext = copy(fontSize = fontSize * factor)
internal fun RenderContext.grow(factor: Float): RenderContext = copy(fontSize = fontSize * factor)

internal fun RenderContext.withColor(colorString: String): RenderContext =
    parseColor(colorString)?.let {
        copy(color = it)
    } ?: this

internal fun RenderContext.applyStyle(styleType: LatexNode.Style.StyleType): RenderContext {
    val families = fontFamilies

    return when (styleType) {
        LatexNode.Style.StyleType.BOLD, LatexNode.Style.StyleType.BOLD_SYMBOL -> this
        LatexNode.Style.StyleType.ITALIC -> copy(fontStyle = FontStyle.Italic)
        LatexNode.Style.StyleType.ROMAN -> copy(
            fontStyle = FontStyle.Normal,
            fontFamily = families?.main ?: fontFamily,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.SANS_SERIF -> copy(
            fontFamily = families?.sansSerif ?: fontFamily,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.MONOSPACE -> copy(
            fontFamily = families?.monospace ?: fontFamily,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.BLACKBOARD_BOLD -> {
            val variantFamily = families?.ams
            copy(
                fontVariant = FontVariant.BLACKBOARD_BOLD,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.CALLIGRAPHIC -> {
            val variantFamily = families?.caligraphic
            copy(
                fontVariant = FontVariant.CALLIGRAPHIC,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.FRAKTUR -> {
            val variantFamily = families?.fraktur
            copy(
                fontVariant = FontVariant.FRAKTUR,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.SCRIPT -> {
            val variantFamily = families?.script
            copy(
                fontVariant = FontVariant.SCRIPT,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }
    }
}

/**
 * 应用数学模式（内部命令触发）
 */
internal fun RenderContext.applyMathStyle(mathStyleType: LatexNode.MathStyle.MathStyleType): RenderContext {
    val newMode = when (mathStyleType) {
        LatexNode.MathStyle.MathStyleType.DISPLAY -> MathStyle.DISPLAY
        LatexNode.MathStyle.MathStyleType.TEXT -> MathStyle.TEXT
        LatexNode.MathStyle.MathStyleType.SCRIPT -> MathStyle.SCRIPT
        LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> MathStyle.SCRIPT_SCRIPT
    }

    val scaleFactor = newMode.scaleFactor() / mathStyle.scaleFactor()

    return copy(
        fontSize = fontSize * scaleFactor,
        mathStyle = newMode
    )
}
