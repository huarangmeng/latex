package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.ProofTree
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import kotlin.reflect.KClass

internal class ProofTreeMeasurer : NodeMeasurer {
    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(ProofTree::class)

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout,
    ): NodeLayout {
        node as ProofTree
        val conclusion = measureNode(node.conclusion, context)
        if (node.premises.isEmpty()) return conclusion

        // 按前提各自的结论基线对齐，保留它们在该基线两侧所占的高度。
        val em = with(density) { context.fontSize.toPx() }
        val premises = node.premises.map { measureNode(it, context) }
        val premiseGap = em * 0.75f
        val premisesWidth = premises.sumOf { it.width.toDouble() }.toFloat() + (premises.size - 1) * premiseGap
        val premisesBaseline = premises.maxOf { it.baseline }
        val premisesHeight = premisesBaseline + premises.maxOf { it.height - it.baseline }
        val labels = context.copy(fontSize = context.fontSize * 0.78f)
        val left = node.leftLabel?.let { measureNode(it, labels) }
        val right = node.rightLabel?.let { measureNode(it, labels) }
        val labelGap = em * 0.28f
        val leftExtra = left?.let { it.width + labelGap } ?: 0f
        val rightExtra = right?.let { it.width + labelGap } ?: 0f

        val stroke = (context.mathFontProvider?.fractionRuleThickness(em) ?: (em * 0.04f)).coerceAtLeast(0.5f)
        val ruleWidth = maxOf(premisesWidth, conclusion.width) + em * 0.4f
        val lineHeight = if (node.rule == ProofTree.Rule.DOUBLE) stroke * 3f else stroke
        val gap = em * 0.16f
        val bandHeight = maxOf(lineHeight, left?.height ?: 0f, right?.height ?: 0f) + 2f * gap
        val upperHeight = if (node.rootAtTop) conclusion.height else premisesHeight
        val premiseY = if (node.rootAtTop) upperHeight + bandHeight else 0f
        val conclusionY = if (node.rootAtTop) 0f else upperHeight + bandHeight
        val dash = when (node.rule) {
            ProofTree.Rule.DASHED -> PathEffect.dashPathEffect(floatArrayOf(em * 0.24f, em * 0.16f))
            ProofTree.Rule.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(stroke, stroke * 2f))
            else -> null
        }

        return NodeLayout(leftExtra + ruleWidth + rightExtra, premisesHeight + conclusion.height + bandHeight,
            conclusionY + conclusion.baseline) { x, y ->
            val ruleX = x + leftExtra
            var premiseX = ruleX + (ruleWidth - premisesWidth) / 2f
            for (premise in premises) {
                premise.draw(this, premiseX, y + premiseY + premisesBaseline - premise.baseline)
                premiseX += premise.width + premiseGap
            }
            conclusion.draw(this, ruleX + (ruleWidth - conclusion.width) / 2f, y + conclusionY)
            val ruleY = y + upperHeight + bandHeight / 2f
            left?.draw?.invoke(this, x, ruleY - left.height / 2f)
            right?.draw?.invoke(this, ruleX + ruleWidth + labelGap, ruleY - right.height / 2f)
            when (node.rule) {
                ProofTree.Rule.NONE -> Unit
                ProofTree.Rule.DOUBLE -> {
                    for (offset in listOf(-stroke, stroke)) {
                        drawLine(context.color, Offset(ruleX, ruleY + offset), Offset(ruleX + ruleWidth, ruleY + offset), stroke)
                    }
                }
                else -> drawLine(context.color, Offset(ruleX, ruleY), Offset(ruleX + ruleWidth, ruleY), stroke,
                    pathEffect = dash)
            }
        }
    }
}
