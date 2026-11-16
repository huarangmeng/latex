package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.applyFontSize
import com.hrm.latex.renderer.model.applyMathStyle
import com.hrm.latex.renderer.model.withColor
import com.hrm.latex.renderer.utils.MathSpacing

/** Color and size/style declarations do not introduce ordinary math atoms. */
internal class InlineMathList private constructor(
    val nodes: List<LatexNode>,
    val contexts: List<RenderContext>?,
    val owners: List<LatexNode>?,
    val glueContext: RenderContext,
) {
    fun lines(): List<InlineMathList> {
        if (nodes.none { it is LatexNode.NewLine }) return listOf(this)
        val result = mutableListOf<InlineMathList>()
        fun slice(start: Int, end: Int) = InlineMathList(
            nodes.subList(start, end), contexts?.subList(start, end), owners?.subList(start, end), glueContext
        )
        var start = 0
        for (i in nodes.indices) if (nodes[i] is LatexNode.NewLine) {
            result += slice(start, i)
            start = i + 1
        }
        if (start < nodes.size || result.isEmpty()) result += slice(start, nodes.size)
        return result
    }

    companion object {
        fun prepare(input: List<LatexNode>, context: RenderContext): InlineMathList {
            val single = input.filterNot(MathSpacing::isNonAtom).singleOrNull()
            val glueContext = when (single) {
                is LatexNode.MathStyle -> context.applyMathStyle(single.mathStyleType)
                is LatexNode.FontSize -> context.applyFontSize(single.sizeType)
                else -> context
            }
            if (input.none { it is LatexNode.Color || it is LatexNode.MathStyle || it is LatexNode.FontSize || it is LatexNode.MathChoice }) {
                return InlineMathList(input, null, null, glueContext)
            } else {
                val atoms = mutableListOf<LatexNode>()
                val styles = mutableListOf<RenderContext>()
                val origins = mutableListOf<LatexNode>()
                fun append(node: LatexNode, style: RenderContext, owner: LatexNode) {
                    when (node) {
                        is LatexNode.Color -> node.content.forEach { append(it, style.withColor(node.color), owner) }
                        is LatexNode.MathStyle -> node.content.forEach { append(it, style.applyMathStyle(node.mathStyleType), owner) }
                        is LatexNode.FontSize -> node.content.forEach { append(it, style.applyFontSize(node.sizeType), owner) }
                        is LatexNode.MathChoice -> {
                            val branch = node.branch(when (style.mathStyle) {
                                MathStyle.DISPLAY -> 0
                                MathStyle.TEXT -> 1
                                MathStyle.SCRIPT -> 2
                                MathStyle.SCRIPT_SCRIPT -> 3
                            })
                            if (branch is LatexNode.Group) branch.children.forEach { append(it, style, owner) }
                            else append(branch, style, owner)
                        }
                        else -> {
                            atoms += node
                            styles += style
                            origins += owner
                        }
                    }
                }
                input.forEach { append(it, context, it) }
                return InlineMathList(atoms, styles, origins, glueContext)
            }
        }
    }
}
