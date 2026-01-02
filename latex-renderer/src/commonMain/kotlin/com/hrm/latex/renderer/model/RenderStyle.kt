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
 */
data class RenderStyle(
    val fontSize: TextUnit = 20.sp,
    val color: Color = Color.Unspecified,
    val darkColor: Color = Color.Unspecified,
    val backgroundColor: Color = Color.Unspecified,
    val darkBackgroundColor: Color = Color.Unspecified,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null
)

// 扩展函数

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
