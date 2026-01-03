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
 * 渲染样式配置
 *
 * @property fontSize 字体大小
 * @property color 文本颜色（浅色模式/默认）
 * @property darkColor 文本颜色（深色模式）
 * @property backgroundColor 背景颜色（浅色模式/默认）
 * @property darkBackgroundColor 背景颜色（深色模式）
 * @property fontWeight 字体粗细
 * @property fontStyle 字体样式
 * @property fontFamily 字体族
 * @property mathStyle 数学模式（控制公式大小）
 */
data class RenderStyle(
    val fontSize: TextUnit = 20.sp,
    val color: Color = Color.Black,
    val darkColor: Color = Color.White,
    val backgroundColor: Color = Color.White,
    val darkBackgroundColor: Color = Color.Black,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val mathStyle: MathStyleMode = MathStyleMode.TEXT
) {
    enum class MathStyleMode {
        DISPLAY,         // \displaystyle - 显示模式（最大，用于独立公式）
        TEXT,            // \textstyle - 文本模式（正常大小）
        SCRIPT,          // \scriptstyle - 脚本模式（上下标大小）
        SCRIPT_SCRIPT    // \scriptscriptstyle - 小脚本模式（二级上下标）
    }
}

internal fun RenderStyle.textStyle(): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontFamily = fontFamily
)

fun RenderStyle.shrink(factor: Float): RenderStyle = copy(fontSize = fontSize * factor)
fun RenderStyle.grow(factor: Float): RenderStyle = copy(fontSize = fontSize * factor)

fun RenderStyle.withColor(colorString: String): RenderStyle = parseColor(colorString)?.let {
    copy(color = it)
} ?: this

fun RenderStyle.applyStyle(styleType: LatexNode.Style.StyleType): RenderStyle =
    when (styleType) {
        LatexNode.Style.StyleType.BOLD, LatexNode.Style.StyleType.BOLD_SYMBOL -> copy(fontWeight = FontWeight.Bold)
        LatexNode.Style.StyleType.ITALIC -> copy(fontStyle = FontStyle.Italic)
        LatexNode.Style.StyleType.ROMAN -> copy(
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.Serif
        )

        LatexNode.Style.StyleType.SANS_SERIF -> copy(fontFamily = FontFamily.SansSerif)
        LatexNode.Style.StyleType.MONOSPACE -> copy(fontFamily = FontFamily.Monospace)
        LatexNode.Style.StyleType.BLACKBOARD_BOLD -> copy(fontWeight = FontWeight.Bold)
        LatexNode.Style.StyleType.FRAKTUR, LatexNode.Style.StyleType.SCRIPT, LatexNode.Style.StyleType.CALLIGRAPHIC -> this
    }

/**
 * 应用数学模式
 * 
 * 根据数学模式调整字体大小：
 * - DISPLAY: 不变（默认大小）
 * - TEXT: 不变（默认大小）
 * - SCRIPT: 0.7倍（上下标大小）
 * - SCRIPT_SCRIPT: 0.5倍（二级上下标大小）
 */
fun RenderStyle.applyMathStyle(mathStyleType: LatexNode.MathStyle.MathStyleType): RenderStyle {
    val newMode = when (mathStyleType) {
        LatexNode.MathStyle.MathStyleType.DISPLAY -> RenderStyle.MathStyleMode.DISPLAY
        LatexNode.MathStyle.MathStyleType.TEXT -> RenderStyle.MathStyleMode.TEXT
        LatexNode.MathStyle.MathStyleType.SCRIPT -> RenderStyle.MathStyleMode.SCRIPT
        LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> RenderStyle.MathStyleMode.SCRIPT_SCRIPT
    }
    
    // 根据当前模式和目标模式计算缩放因子
    val scaleFactor = when (newMode) {
        RenderStyle.MathStyleMode.DISPLAY, RenderStyle.MathStyleMode.TEXT -> 1.0f
        RenderStyle.MathStyleMode.SCRIPT -> 0.7f
        RenderStyle.MathStyleMode.SCRIPT_SCRIPT -> 0.5f
    }
    
    return copy(
        fontSize = fontSize * scaleFactor,
        mathStyle = newMode
    )
}
