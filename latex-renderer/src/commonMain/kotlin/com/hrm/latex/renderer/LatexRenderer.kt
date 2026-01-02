package com.hrm.latex.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.measureGroup
import com.hrm.latex.renderer.model.RenderStyle

/**
 * Latex 渲染组件
 *
 * @param latex LaTeX 字符串
 * @param modifier 修饰符
 * @param style 渲染样式（包含颜色、背景色、深浅色模式配置、字体大小等）
 * @param isDarkTheme 是否为深色模式（默认跟随系统）
 * @param parser Latex 解析器
 */
@Composable
fun Latex(
    latex: String,
    modifier: Modifier = Modifier,
    style: RenderStyle = RenderStyle(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    parser: LatexParser = remember { LatexParser() }
) {
    // 确定最终文本颜色
    val resolvedTextColor = if (isDarkTheme) {
        if (style.darkColor != Color.Unspecified) style.darkColor else Color.White
    } else {
        if (style.color != Color.Unspecified) style.color else Color.Black
    }

    // 确定最终背景颜色
    val resolvedBackgroundColor = if (isDarkTheme) {
        style.darkBackgroundColor
    } else {
        style.backgroundColor
    }

    // 构建最终样式
    val resolvedStyle = style.copy(
        color = resolvedTextColor,
        backgroundColor = resolvedBackgroundColor
    )

    val document = remember(latex) { runCatching { parser.parse(latex) }.getOrNull() }
    if (document != null) {
        LatexDocument(
            modifier = modifier,
            children = document.children,
            style = resolvedStyle
        )
    } else {
        Text(text = "Invalid LaTeX", color = Color.Red, modifier = modifier)
    }
}

/**
 * Latex 文档渲染组件
 *
 * @param modifier 修饰符
 * @param children 文档根节点
 * @param style 渲染样式
 */
@Composable
fun LatexDocument(
    modifier: Modifier = Modifier,
    children: List<LatexNode>,
    style: RenderStyle = RenderStyle()
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val layout = remember(children, style, density) {
        measureGroup(children, style, measurer, density)
    }

    val widthDp = with(density) { layout.width.toDp() }
    val heightDp = with(density) { layout.height.toDp() }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.size(widthDp, heightDp)) {
            // 绘制背景
            if (style.backgroundColor != Color.Unspecified) {
                drawRect(color = style.backgroundColor)
            }
            // 绘制内容
            layout.draw(this, 0f, 0f)
        }
    }
}
