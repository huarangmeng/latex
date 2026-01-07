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
    val baseFontFamily: FontFamily? = FontFamily.Serif
)

/**
 * 内部渲染上下文（渲染树遍历过程中的状态）
 */
internal data class RenderContext(
    val fontSize: TextUnit,
    val color: Color,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = FontFamily.Serif,
    val fontVariant: FontVariant = FontVariant.NORMAL,
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
        fontFamily = baseFontFamily
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

internal fun RenderContext.applyStyle(styleType: LatexNode.Style.StyleType): RenderContext =
    when (styleType) {
        LatexNode.Style.StyleType.BOLD, LatexNode.Style.StyleType.BOLD_SYMBOL -> copy(fontWeight = FontWeight.Bold)
        LatexNode.Style.StyleType.ITALIC -> copy(fontStyle = FontStyle.Italic)
        LatexNode.Style.StyleType.ROMAN -> copy(
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.Serif
        )

        LatexNode.Style.StyleType.SANS_SERIF -> copy(fontFamily = FontFamily.SansSerif)
        LatexNode.Style.StyleType.MONOSPACE -> copy(fontFamily = FontFamily.Monospace)
        LatexNode.Style.StyleType.BLACKBOARD_BOLD -> copy(fontVariant = RenderContext.FontVariant.BLACKBOARD_BOLD)
        LatexNode.Style.StyleType.CALLIGRAPHIC -> copy(fontVariant = RenderContext.FontVariant.CALLIGRAPHIC)
        LatexNode.Style.StyleType.FRAKTUR -> copy(fontVariant = RenderContext.FontVariant.FRAKTUR)
        LatexNode.Style.StyleType.SCRIPT -> copy(fontVariant = RenderContext.FontVariant.SCRIPT)
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
