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
import com.hrm.latex.renderer.utils.parseColor

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
    val fontFamilies: LatexFontFamilies? = null
)

/**
 * 内部渲染上下文（渲染树遍历过程中的状态）
 */
internal data class RenderContext(
    val fontSize: TextUnit,
    val color: Color,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val fontVariant: FontVariant = FontVariant.NORMAL,
    val fontFamilies: LatexFontFamilies? = null,
    val isVariantFontFamily: Boolean = false,
    val mathStyle: MathStyleMode = MathStyleMode.DISPLAY,
    val bigOpHeightHint: Float? = null // 大型运算符（如积分）的高度暗示
) {
    enum class MathStyleMode {
        DISPLAY,         // \displaystyle
        TEXT,            // \textstyle
        SCRIPT,          // \scriptstyle
        SCRIPT_SCRIPT    // \scriptscriptstyle
    }

    enum class FontVariant {
        NORMAL,
        BLACKBOARD_BOLD, // \mathbb
        CALLIGRAPHIC,    // \mathcal
        FRAKTUR,         // \mathfrak
        SCRIPT           // \mathscr
    }
}

/**
 * 从外部配置创建初始上下文
 */
internal fun LatexConfig.toContext(isDark: Boolean): RenderContext {
    val resolvedColor = if (isDark) {
        if (darkColor != Color.Unspecified) darkColor else Color.White
    } else {
        if (color != Color.Unspecified) color else Color.Black
    }

    return RenderContext(
        fontSize = fontSize,
        color = resolvedColor,
        fontFamily = baseFontFamily,
        fontFamilies = fontFamilies,
        isVariantFontFamily = false
    )
}

internal fun RenderContext.textStyle(): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontFamily = fontFamily
)

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
            fontFamily = families?.roman ?: fontFamily,
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
            val variantFamily = families?.blackboardBold
            copy(
                fontVariant = RenderContext.FontVariant.BLACKBOARD_BOLD,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.CALLIGRAPHIC -> {
            val variantFamily = families?.calligraphic
            copy(
                fontVariant = RenderContext.FontVariant.CALLIGRAPHIC,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.FRAKTUR -> {
            val variantFamily = families?.fraktur
            copy(
                fontVariant = RenderContext.FontVariant.FRAKTUR,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.SCRIPT -> {
            val variantFamily = families?.script
            copy(
                fontVariant = RenderContext.FontVariant.SCRIPT,
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
        LatexNode.MathStyle.MathStyleType.DISPLAY -> RenderContext.MathStyleMode.DISPLAY
        LatexNode.MathStyle.MathStyleType.TEXT -> RenderContext.MathStyleMode.TEXT
        LatexNode.MathStyle.MathStyleType.SCRIPT -> RenderContext.MathStyleMode.SCRIPT
        LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> RenderContext.MathStyleMode.SCRIPT_SCRIPT
    }

    val scaleFactor = when (newMode) {
        RenderContext.MathStyleMode.DISPLAY, RenderContext.MathStyleMode.TEXT -> 1.0f
        RenderContext.MathStyleMode.SCRIPT -> 0.7f
        RenderContext.MathStyleMode.SCRIPT_SCRIPT -> 0.5f
    }

    return copy(
        fontSize = fontSize * scaleFactor,
        mathStyle = newMode
    )
}
