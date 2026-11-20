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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.MathAtomSymbols

/**
 * 数学类代码命令：\mathord, \mathbin, \mathrel, \mathopen, \mathclose,
 * \mathpunct, \mathinner
 *
 * 这些控制序列把参数子公式强制归入指定的 TeX 原子类，从而改变它与相邻
 * 原子之间的数学间距（参见 TeXbook 第 18 章）。
 *
 * 注意：`\mathop` 由 [installOperatorHandlers] 处理为 [LatexNode.BigOperator]，
 * 以保留大型运算符的上下标（limits）渲染行为。
 */
internal fun CommandRegistry.installMathClassHandlers() {
    register("@binrel", "pmb") { name, ctx, _ ->
        val sample = ctx.parseArgument() ?: LatexNode.Text("")
        val type = binOrRelClass(sample)
        val content = if (name == "pmb") {
            LatexNode.Layout(LatexNode.Layout.LayoutType.POOR_MANS_BOLD, listOf(sample))
        } else ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.MathClass(listOf(content), type)
    }

    register("ordinarycolon", "vcentcolon", "ratio") { name, _, _ ->
        LatexNode.MathClass(listOf(LatexNode.Text(if (name == "ordinarycolon") ":" else "∶")),
            LatexNode.MathClass.AtomClass.REL)
    }

    register("thickapprox", "thicksim") { name, _, _ ->
        val symbol = if (name == "thickapprox") LatexNode.Symbol("approx", "≈") else LatexNode.Symbol("sim", "∼")
        LatexNode.MathClass(listOf(LatexNode.Style(listOf(symbol), LatexNode.Style.StyleType.BOLD_SYMBOL)),
            LatexNode.MathClass.AtomClass.REL)
    }

    val colonRelations = mapOf(
        "Coloneqq" to "∷=", "coloncolonequals" to "∷=", "Coloneq" to "∷−", "coloncolonminus" to "∷−",
        "Eqqcolon" to "=∷", "equalscoloncolon" to "=∷", "Eqcolon" to "−∷", "minuscoloncolon" to "−∷",
        "colonapprox" to "∶≈", "Colonapprox" to "∷≈", "coloncolonapprox" to "∷≈",
        "colonsim" to "∶∼", "Colonsim" to "∷∼", "coloncolonsim" to "∷∼",
        "approxcolon" to "≈∶", "approxcoloncolon" to "≈∷", "simcolon" to "∼∶", "simcoloncolon" to "∼∷",
    )
    register(*colonRelations.keys.toTypedArray()) { name, _, _ ->
        val glyphs = colonRelations.getValue(name)
        // 运算符内部按字形间距拼接，整个组合在外层只占一个关系原子。
        val parts = glyphs.map { LatexNode.MathClass(listOf(LatexNode.Text(it.toString())), LatexNode.MathClass.AtomClass.ORD) }
        LatexNode.MathClass(listOf(parts.first(), LatexNode.HSpace("-1.2mu"), parts.last()), LatexNode.MathClass.AtomClass.REL)
    }
    val classCommandMapping = mapOf(
        "mathord" to LatexNode.MathClass.AtomClass.ORD,
        "mathbin" to LatexNode.MathClass.AtomClass.BIN,
        "mathrel" to LatexNode.MathClass.AtomClass.REL,
        "mathopen" to LatexNode.MathClass.AtomClass.OPEN,
        "mathclose" to LatexNode.MathClass.AtomClass.CLOSE,
        "mathpunct" to LatexNode.MathClass.AtomClass.PUNCT,
        "mathinner" to LatexNode.MathClass.AtomClass.INNER,
    )

    for ((cmd, atomClass) in classCommandMapping) {
        register(cmd) { _, ctx, _ ->
            val content = ctx.parseArgument()
            LatexNode.MathClass(
                if (content != null) listOf(content) else emptyList(),
                atomClass
            )
        }
    }
}

private fun binOrRelClass(node: LatexNode): LatexNode.MathClass.AtomClass {
    val first = if (node is LatexNode.Group) node.children.firstOrNull() else node
    val type = when (first) {
        is LatexNode.Symbol -> MathAtomSymbols.classify(first.symbol, first.unicode)
        is LatexNode.MathClass -> first.atomClass
        is LatexNode.Text -> when (first.content) {
            "+", "-", "*" -> LatexNode.MathClass.AtomClass.BIN
            "=", "<", ">", ":" -> LatexNode.MathClass.AtomClass.REL
            else -> MathAtomSymbols.classify("", first.content)
        }
        else -> LatexNode.MathClass.AtomClass.ORD
    }
    return if (type == LatexNode.MathClass.AtomClass.BIN || type == LatexNode.MathClass.AtomClass.REL) type
        else LatexNode.MathClass.AtomClass.ORD
}
