package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.MathFontUtils
import com.hrm.latex.renderer.utils.parseDimension
import com.hrm.latex.renderer.utils.spaceWidthPx

/**
 * 文本内容测量器
 *
 * 负责测量基础文本、符号、命令名、空格等“叶子”节点。
 * 这些节点通常是渲染的基础单元，不再包含其他 LaTeX 结构。
 */
internal class TextContentMeasurer : NodeMeasurer<LatexNode> {
    
    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Text -> measureText(node.content, context, measurer)
            is LatexNode.TextMode -> measureTextMode(node.text, context, measurer)
            is LatexNode.Symbol -> measureSymbol(node, context, measurer)
            is LatexNode.Operator -> measureText(
                node.op,
                context.copy(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif),
                measurer
            )
            is LatexNode.Command -> measureText(node.name, context, measurer)
            is LatexNode.Space -> measureSpace(node.type, context, density)
            is LatexNode.HSpace -> measureHSpace(node, context, density)
            is LatexNode.NewLine -> NodeLayout(0f, 0f, 0f) { _, _ -> } // 换行符本身不占用空间，由 measureGroup 处理
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    /**
     * 测量普通文本
     *
     * 使用 Compose 的 TextMeasurer 计算文本的宽高和基线。
     */
    private fun measureText(
        text: String, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
        // 1. 处理数学特殊字体变体 (如 \mathbb, \mathcal)
        val transformedText = applyFontVariant(text, context.fontVariant)

        // 2. 数学模式下的变量处理规范：
        // - 字母 (A-Z, a-z) 默认使用斜体 (Italic)
        // - 数字 (0-9) 和符号默认使用正体 (Normal)
        // - 如果用户显式指定了样式（如 \mathrm, \mathbf），则遵循用户指定
        val resolvedStyle = if (context.fontStyle == null && context.fontVariant == RenderContext.FontVariant.NORMAL) {
            when {
                transformedText.any { it.isLetter() } -> context.copy(fontStyle = FontStyle.Italic)
                transformedText.any { it.isDigit() } -> context.copy(fontStyle = FontStyle.Normal)
                else -> context
            }
        } else {
            context
        }

        val textStyle = resolvedStyle.textStyle()
        val result: TextLayoutResult = measurer.measure(
            text = AnnotatedString(transformedText), style = textStyle
        )

        val baseWidth = result.size.width.toFloat()
        val height = result.size.height.toFloat()
        val baseline = result.firstBaseline
        
        // 斜体修正：为斜体文本添加右侧内边距，防止字符被截断
        // 特别是最后一个字符（如大写 X, Y, Z）会向右倾斜溢出
        val isItalic = resolvedStyle.fontStyle == FontStyle.Italic
        val italicCorrection = if (isItalic && transformedText.isNotEmpty()) {
            // 斜体修正值：
            // - 大写字母：字体大小的 15%（更倾斜）
            // - 小写字母：字体大小的 12%（中等倾斜，提高修正值以防止下标场景被截断）
            // - 其他字符：字体大小的 8%（较小倾斜）
            val lastChar = transformedText.last()
            when {
                lastChar.isUpperCase() -> context.fontSize.value * 0.15f  // 大写字母
                lastChar.isLowerCase() -> context.fontSize.value * 0.12f  // 小写字母（提高到12%）
                else -> context.fontSize.value * 0.08f  // 其他字符
            }
        } else {
            0f
        }
        
        val width = baseWidth + italicCorrection

        return NodeLayout(width, height, baseline) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    /**
     * 测量符号（Symbol）
     * 
     * 对于希腊字母等符号，遵循数学排版规范：
     * - 小写希腊字母默认斜体
     * - 大写希腊字母默认正体 (Standard LaTeX behavior)
     */
    private fun measureSymbol(
        node: LatexNode.Symbol, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
        val text = if (node.unicode.isEmpty()) node.symbol else node.unicode
        
        // 遵循数学排版规范应用样式
        val resolvedStyle = if (context.fontStyle == null) {
            when {
                isLowercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Italic)
                isUppercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Normal)
                else -> context
            }
        } else {
            context
        }
        
        val textStyle = resolvedStyle.textStyle()
        val result: TextLayoutResult = measurer.measure(
            text = AnnotatedString(text), style = textStyle
        )

        val baseWidth = result.size.width.toFloat()
        val height = result.size.height.toFloat()
        val originalBaseline = result.firstBaseline
        
        val baseline = if (isArrowOrCenteredSymbol(node.symbol)) {
            height * 0.85f
        } else {
            originalBaseline
        }
        
        // 斜体修正：小写希腊字母默认使用斜体，需要添加右侧内边距
        val isItalic = resolvedStyle.fontStyle == FontStyle.Italic
        val italicCorrection = if (isItalic && text.isNotEmpty()) {
            // 希腊字母的斜体修正（提高到14%以防止被截断）
            context.fontSize.value * 0.14f
        } else {
            0f
        }
        
        val width = baseWidth + italicCorrection

        return NodeLayout(width, height, baseline) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    private fun applyFontVariant(text: String, variant: RenderContext.FontVariant): String {
        return when (variant) {
            RenderContext.FontVariant.BLACKBOARD_BOLD -> MathFontUtils.toBlackboardBold(text)
            RenderContext.FontVariant.CALLIGRAPHIC -> MathFontUtils.toCalligraphic(text)
            // 目前暂不支持 Fraktur 和 Script 的 Unicode 映射，保留原样
            else -> text
        }
    }

    /**
     * 判断是否为小写希腊字母
     */
    private fun isLowercaseGreek(symbol: String): Boolean {
        return symbol in setOf(
            "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa",
            "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi",
            "chi", "psi", "omega",
            "varpi", "varrho", "varsigma", "vartheta", "varphi", "varepsilon"
        )
    }

    /**
     * 判断是否为大写希腊字母
     */
    private fun isUppercaseGreek(symbol: String): Boolean {
        return symbol in setOf(
            "Gamma", "Delta", "Theta", "Lambda", "Xi", "Pi", "Sigma", "Upsilon", "Phi", "Psi", "Omega"
        )
    }

    /**
     * 判断符号是否应该垂直居中
     * 
     * 箭头、等号、加减号等二元运算符应该居中显示
     */
    private fun isArrowOrCenteredSymbol(symbol: String): Boolean {
        return symbol in setOf(
            // 箭头
            "rightarrow", "leftarrow", "leftrightarrow",
            "Rightarrow", "Leftarrow", "Leftrightarrow",
            "longrightarrow", "longleftarrow", "longleftrightarrow",
            "uparrow", "downarrow", "updownarrow",
            "Uparrow", "Downarrow", "Updownarrow",
            "mapsto", "to",
            // 等号和关系符号
            "equals", "neq", "approx", "equiv", "sim",
            "leq", "geq", "ll", "gg",
            "subset", "supset", "subseteq", "supseteq",
            // 二元运算符
            "plus", "minus", "times", "div", "cdot",
            "pm", "mp", "ast", "star", "circ",
            "oplus", "ominus", "otimes", "oslash"
        )
    }

    /**
     * 测量 \text{...} 模式的文本
     *
     * 文本模式下默认使用衬线字体 (Serif) 和正常字重，以区别于数学斜体。
     */
    private fun measureTextMode(
        text: String, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
        val textModeStyle = context.copy(
            fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif,
            fontWeight = context.fontWeight ?: FontWeight.Normal
        )

        val textStyle = textModeStyle.textStyle()
        val result: TextLayoutResult = measurer.measure(
            text = AnnotatedString(text), style = textStyle
        )

        val width = result.size.width.toFloat()
        val height = result.size.height.toFloat()
        val baseline = result.firstBaseline

        return NodeLayout(width, height, baseline) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    /**
     * 测量标准空格 (quad, qquad, thin, etc.)
     */
    private fun measureSpace(
        type: LatexNode.Space.SpaceType, context: RenderContext, density: Density
    ): NodeLayout {
        val width = spaceWidthPx(context, type, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }

    /**
     * 测量自定义水平空格 (\hspace{...})
     */
    private fun measureHSpace(
        node: LatexNode.HSpace, context: RenderContext, density: Density
    ): NodeLayout {
        val width = parseDimension(node.dimension, context, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }
}
