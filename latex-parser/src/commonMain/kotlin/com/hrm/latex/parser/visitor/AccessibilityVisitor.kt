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

package com.hrm.latex.parser.visitor

import com.hrm.latex.parser.model.LatexNode

/**
 * 可访问性访问者：将 LaTeX AST 转换为屏幕阅读器友好的文本描述
 *
 * 遵循 MathSpeak 风格的读法规则，为视觉障碍用户提供数学公式的自然语言描述。
 *
 * 用法示例：
 * ```kotlin
 * val description = AccessibilityVisitor.describe(document)
 * // "fraction: 1 over 2" for \frac{1}{2}
 * ```
 */
class AccessibilityVisitor : BaseLatexVisitor<String>() {

    companion object {
        /**
         * 将 LaTeX AST 转换为可访问性文本描述
         */
        fun describe(node: LatexNode): String {
            return AccessibilityVisitor().visit(node).trim()
        }

        /**
         * 将 LaTeX 文档转换为可访问性文本描述
         */
        fun describe(document: LatexNode.Document): String {
            return AccessibilityVisitor().visitDocument(document).trim()
        }

        private val SYMBOL_NAMES = mapOf(
            "alpha" to "alpha", "beta" to "beta", "gamma" to "gamma",
            "delta" to "delta", "epsilon" to "epsilon", "zeta" to "zeta",
            "eta" to "eta", "theta" to "theta", "iota" to "iota",
            "kappa" to "kappa", "lambda" to "lambda", "mu" to "mu",
            "nu" to "nu", "xi" to "xi", "pi" to "pi",
            "rho" to "rho", "sigma" to "sigma", "tau" to "tau",
            "phi" to "phi", "chi" to "chi", "psi" to "psi", "omega" to "omega",
            "infty" to "infinity", "partial" to "partial",
            "nabla" to "nabla", "hbar" to "h bar",
            "times" to "times", "div" to "divided by",
            "pm" to "plus or minus", "mp" to "minus or plus",
            "cdot" to "dot", "cdots" to "dots", "ldots" to "dots",
            "leq" to "less than or equal to", "geq" to "greater than or equal to",
            "neq" to "not equal to", "approx" to "approximately",
            "equiv" to "equivalent to", "sim" to "similar to",
            "rightarrow" to "right arrow", "leftarrow" to "left arrow",
            "Rightarrow" to "implies", "Leftarrow" to "is implied by",
            "leftrightarrow" to "if and only if",
            "in" to "in", "notin" to "not in",
            "subset" to "subset of", "supset" to "superset of",
            "cup" to "union", "cap" to "intersection",
            "forall" to "for all", "exists" to "there exists",
            "to" to "to",
        )

        private val UNICODE_NAMES = mapOf(
            "α" to "alpha", "β" to "beta", "γ" to "gamma",
            "δ" to "delta", "ε" to "epsilon", "ζ" to "zeta",
            "η" to "eta", "θ" to "theta", "ι" to "iota",
            "κ" to "kappa", "λ" to "lambda", "μ" to "mu",
            "ν" to "nu", "ξ" to "xi", "π" to "pi",
            "ρ" to "rho", "σ" to "sigma", "τ" to "tau",
            "φ" to "phi", "χ" to "chi", "ψ" to "psi", "ω" to "omega",
            "∞" to "infinity", "∂" to "partial",
            "∇" to "nabla", "ℏ" to "h bar",
            "×" to "times", "÷" to "divided by",
            "±" to "plus or minus", "∓" to "minus or plus",
            "·" to "dot", "⋯" to "dots", "…" to "dots",
            "≤" to "less than or equal to", "≥" to "greater than or equal to",
            "≠" to "not equal to", "≈" to "approximately",
            "≡" to "equivalent to", "∼" to "similar to",
            "→" to "right arrow", "←" to "left arrow",
            "⇒" to "implies", "⇐" to "is implied by",
            "↔" to "if and only if",
            "∈" to "in", "∉" to "not in",
            "⊂" to "subset of", "⊃" to "superset of",
            "∪" to "union", "∩" to "intersection",
            "∀" to "for all", "∃" to "there exists",
        )
    }

    override fun defaultVisit(node: LatexNode): String = ""

    override fun visitDocument(node: LatexNode.Document): String {
        return node.children.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitText(node: LatexNode.Text): String = node.content

    override fun visitGroup(node: LatexNode.Group): String {
        return node.children.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitSuperscript(node: LatexNode.Superscript): String {
        val base = visit(node.base)
        val exp = visit(node.exponent)
        return if (exp == "2") "$base squared"
        else if (exp == "3") "$base cubed"
        else "$base to the power of $exp"
    }

    override fun visitSubscript(node: LatexNode.Subscript): String {
        val base = visit(node.base)
        val idx = visit(node.index)
        return "$base sub $idx"
    }

    override fun visitFraction(node: LatexNode.Fraction): String {
        val num = visit(node.numerator)
        val den = visit(node.denominator)
        return "fraction: $num over $den"
    }

    override fun visitRoot(node: LatexNode.Root): String {
        val content = visit(node.content)
        val index = node.index?.let { visit(it) }
        return if (index != null) "$index-th root of $content"
        else "square root of $content"
    }

    override fun visitMatrix(node: LatexNode.Matrix): String {
        val rows = node.rows.size
        val cols = node.rows.firstOrNull()?.size ?: 0
        val bracket = when (node.type) {
            LatexNode.Matrix.MatrixType.PAREN -> "parenthesized "
            LatexNode.Matrix.MatrixType.BRACKET -> "bracketed "
            LatexNode.Matrix.MatrixType.BRACE -> "braced "
            LatexNode.Matrix.MatrixType.VBAR -> "determinant "
            LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> "norm "
            else -> ""
        }
        val body = node.rows.mapIndexed { i, row ->
            "row ${i + 1}: " + row.joinToString(", ") { visit(it) }
        }.joinToString("; ")
        return "${bracket}$rows by $cols matrix: $body"
    }

    override fun visitArray(node: LatexNode.Array): String {
        val body = node.rows.mapIndexed { i, row ->
            "row ${i + 1}: " + row.joinToString(", ") { visit(it) }
        }.joinToString("; ")
        return "array: $body"
    }

    override fun visitSymbol(node: LatexNode.Symbol): String {
        return symbolName(node.symbol, node.unicode)
    }

    override fun visitOperator(node: LatexNode.Operator): String = node.op

    override fun visitSpace(node: LatexNode.Space): String = " "

    override fun visitHSpace(node: LatexNode.HSpace): String = " "

    override fun visitNewLine(node: LatexNode.NewLine): String = "; "

    override fun visitDelimited(node: LatexNode.Delimited): String {
        val left = delimiterName(node.left, isLeft = true)
        val right = delimiterName(node.right, isLeft = false)
        val content = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        return "$left $content $right"
    }

    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): String {
        return delimiterName(node.delimiter, isLeft = true)
    }

    override fun visitAccent(node: LatexNode.Accent): String {
        val content = visit(node.content)
        val accent = when (node.accentType) {
            LatexNode.Accent.AccentType.HAT -> "hat"
            LatexNode.Accent.AccentType.TILDE -> "tilde"
            LatexNode.Accent.AccentType.BAR -> "bar"
            LatexNode.Accent.AccentType.DOT -> "dot"
            LatexNode.Accent.AccentType.DDOT -> "double dot"
            LatexNode.Accent.AccentType.VEC -> "vector"
            LatexNode.Accent.AccentType.OVERLINE -> "overline"
            LatexNode.Accent.AccentType.UNDERLINE -> "underline"
            LatexNode.Accent.AccentType.OVERBRACE -> "overbrace"
            LatexNode.Accent.AccentType.UNDERBRACE -> "underbrace"
            LatexNode.Accent.AccentType.WIDEHAT -> "wide hat"
            LatexNode.Accent.AccentType.OVERRIGHTARROW -> "right arrow over"
            LatexNode.Accent.AccentType.OVERLEFTARROW -> "left arrow over"
            LatexNode.Accent.AccentType.CANCEL -> "canceled"
            LatexNode.Accent.AccentType.BCANCEL -> "back-canceled"
            LatexNode.Accent.AccentType.XCANCEL -> "cross-canceled"
        }
        return "$content $accent"
    }

    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): String {
        val above = visit(node.content)
        val below = node.below?.let { visit(it) }
        val dir = when (node.direction) {
            LatexNode.ExtensibleArrow.Direction.RIGHT -> "right arrow"
            LatexNode.ExtensibleArrow.Direction.LEFT -> "left arrow"
            LatexNode.ExtensibleArrow.Direction.BOTH -> "bidirectional arrow"
            LatexNode.ExtensibleArrow.Direction.HOOK_RIGHT -> "hook right arrow"
            LatexNode.ExtensibleArrow.Direction.HOOK_LEFT -> "hook left arrow"
        }
        return buildString {
            append(dir)
            if (above.isNotBlank()) append(" labeled $above")
            if (below != null && below.isNotBlank()) append(" with $below below")
        }
    }

    override fun visitStack(node: LatexNode.Stack): String {
        val base = visit(node.base)
        val above = node.above?.let { visit(it) }
        val below = node.below?.let { visit(it) }
        return buildString {
            append(base)
            if (above != null) append(" with $above above")
            if (below != null) append(" with $below below")
        }
    }

    override fun visitStyle(node: LatexNode.Style): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitColor(node: LatexNode.Color): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitMathStyle(node: LatexNode.MathStyle): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitBigOperator(node: LatexNode.BigOperator): String {
        val op = bigOperatorName(node.operator)
        val sub = node.subscript?.let { "from ${visit(it)}" }
        val sup = node.superscript?.let { "to ${visit(it)}" }
        return listOfNotNull(op, sub, sup).joinToString(" ")
    }

    override fun visitAligned(node: LatexNode.Aligned): String {
        return node.rows.joinToString("; ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitCases(node: LatexNode.Cases): String {
        val body = node.cases.joinToString("; ") { (expr, cond) ->
            "${visit(expr)} if ${visit(cond)}"
        }
        return "cases: $body"
    }

    override fun visitSplit(node: LatexNode.Split): String {
        return node.rows.joinToString("; ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitMultline(node: LatexNode.Multline): String {
        return node.lines.joinToString("; ") { visit(it) }
    }

    override fun visitEqnarray(node: LatexNode.Eqnarray): String {
        return node.rows.joinToString("; ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitSubequations(node: LatexNode.Subequations): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitBinomial(node: LatexNode.Binomial): String {
        val top = visit(node.top)
        val bottom = visit(node.bottom)
        return "$top choose $bottom"
    }

    override fun visitTextMode(node: LatexNode.TextMode): String = node.text

    override fun visitNegation(node: LatexNode.Negation): String {
        return "not ${visit(node.content)}"
    }

    override fun visitTag(node: LatexNode.Tag): String {
        return "(${visit(node.label)})"
    }

    override fun visitSubstack(node: LatexNode.Substack): String {
        return node.rows.joinToString(", ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitSmash(node: LatexNode.Smash): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitVPhantom(node: LatexNode.VPhantom): String = ""
    override fun visitHPhantom(node: LatexNode.HPhantom): String = ""

    override fun visitLabel(node: LatexNode.Label): String = ""

    override fun visitRef(node: LatexNode.Ref): String = "reference ${node.key}"

    override fun visitEqRef(node: LatexNode.EqRef): String = "(${node.key})"

    override fun visitSideSet(node: LatexNode.SideSet): String {
        val base = visit(node.base)
        val parts = mutableListOf<String>()
        node.leftSub?.let { parts.add("left sub ${visit(it)}") }
        node.leftSup?.let { parts.add("left super ${visit(it)}") }
        node.rightSub?.let { parts.add("right sub ${visit(it)}") }
        node.rightSup?.let { parts.add("right super ${visit(it)}") }
        return "$base with ${parts.joinToString(", ")}"
    }

    override fun visitTensor(node: LatexNode.Tensor): String {
        val base = visit(node.base)
        val indices = node.indices.joinToString(" ") { (isUpper, indexNode) ->
            val prefix = if (isUpper) "upper" else "lower"
            "$prefix ${visit(indexNode)}"
        }
        return "$base $indices"
    }

    override fun visitTabular(node: LatexNode.Tabular): String {
        val rows = node.rows.size
        val cols = node.rows.firstOrNull()?.size ?: 0
        val body = node.rows.mapIndexed { i, row ->
            "row ${i + 1}: " + row.joinToString(", ") { visit(it) }
        }.joinToString("; ")
        return "$rows by $cols table: $body"
    }

    override fun visitBoxed(node: LatexNode.Boxed): String {
        return "boxed: " + node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitPhantom(node: LatexNode.Phantom): String = ""

    override fun visitNewCommand(node: LatexNode.NewCommand): String = ""

    // ========== 辅助方法 ==========

    private fun symbolName(symbol: String, unicode: String): String {
        return SYMBOL_NAMES[symbol] ?: UNICODE_NAMES[unicode] ?: unicode
    }

    private fun bigOperatorName(operator: String): String {
        return when (operator) {
            "∑", "\\sum", "sum" -> "sum"
            "∏", "\\prod", "prod" -> "product"
            "∫", "\\int", "int" -> "integral"
            "∮", "\\oint", "oint" -> "contour integral"
            "∬", "\\iint", "iint" -> "double integral"
            "∭", "\\iiint", "iiint" -> "triple integral"
            "⋃", "\\bigcup", "bigcup" -> "union"
            "⋂", "\\bigcap", "bigcap" -> "intersection"
            "⊕", "\\bigoplus", "bigoplus" -> "direct sum"
            "⊗", "\\bigotimes", "bigotimes" -> "tensor product"
            "⋁", "\\bigvee", "bigvee" -> "disjunction"
            "⋀", "\\bigwedge", "bigwedge" -> "conjunction"
            "∐", "\\coprod", "coprod" -> "coproduct"
            "lim" -> "limit"
            "max" -> "maximum"
            "min" -> "minimum"
            "sup" -> "supremum"
            "inf" -> "infimum"
            "limsup" -> "limit superior"
            "liminf" -> "limit inferior"
            "det" -> "determinant"
            "gcd" -> "greatest common divisor"
            else -> operator
        }
    }

    private fun delimiterName(delim: String, isLeft: Boolean): String {
        if (delim.isEmpty() || delim == ".") return ""
        return when (delim) {
            "(", ")" -> if (isLeft) "open paren" else "close paren"
            "[", "]" -> if (isLeft) "open bracket" else "close bracket"
            "{", "}", "\\{", "\\}" -> if (isLeft) "open brace" else "close brace"
            "|" -> "vertical bar"
            "‖", "\\|" -> "double vertical bar"
            "⟨", "\\langle" -> "left angle bracket"
            "⟩", "\\rangle" -> "right angle bracket"
            "⌊", "\\lfloor" -> "floor"
            "⌋", "\\rfloor" -> "end floor"
            "⌈", "\\lceil" -> "ceiling"
            "⌉", "\\rceil" -> "end ceiling"
            else -> delim
        }
    }

    private fun String.collapseSpaces(): String {
        return replace(Regex("\\s+"), " ").trim()
    }
}
