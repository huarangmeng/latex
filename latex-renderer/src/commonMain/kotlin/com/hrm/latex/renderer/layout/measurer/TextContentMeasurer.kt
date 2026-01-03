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
import com.hrm.latex.renderer.model.RenderStyle
import com.hrm.latex.renderer.model.textStyle
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
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Text -> measureText(node.content, style, measurer)
            is LatexNode.TextMode -> measureTextMode(node.text, style, measurer)
            is LatexNode.Symbol -> measureText(
                node.unicode.ifEmpty { node.symbol }, style, measurer
            )
            is LatexNode.Operator -> measureText(
                node.op,
                style.copy(fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif),
                measurer
            )
            is LatexNode.Command -> measureText(node.name, style, measurer)
            is LatexNode.Space -> measureSpace(node.type, style, density)
            is LatexNode.HSpace -> measureHSpace(node, style, density)
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
        text: String, style: RenderStyle, measurer: TextMeasurer
    ): NodeLayout {
        val textStyle = style.textStyle()
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
     * 测量 \text{...} 模式的文本
     *
     * 文本模式下默认使用衬线字体 (Serif) 和正常字重，以区别于数学斜体。
     */
    private fun measureTextMode(
        text: String, style: RenderStyle, measurer: TextMeasurer
    ): NodeLayout {
        val textModeStyle = style.copy(
            fontStyle = FontStyle.Normal, fontFamily = FontFamily.Serif,
            fontWeight = style.fontWeight ?: FontWeight.Normal
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
        type: LatexNode.Space.SpaceType, style: RenderStyle, density: Density
    ): NodeLayout {
        val width = spaceWidthPx(style, type, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }

    /**
     * 测量自定义水平空格 (\hspace{...})
     */
    private fun measureHSpace(
        node: LatexNode.HSpace, style: RenderStyle, density: Density
    ): NodeLayout {
        val width = parseDimension(node.dimension, style, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }
}
