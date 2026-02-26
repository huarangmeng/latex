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
 * LaTeX AST → MathML 转换访问者
 *
 * 将 LaTeX 语法树转换为 Presentation MathML 标记语言，
 * 用于提升 Web 可访问性和跨平台数学公式交换。
 *
 * 用法示例：
 * ```kotlin
 * val mathml = MathMLVisitor.convert(document)
 * // 生成完整的 <math> 元素
 * ```
 *
 * 支持的 MathML 元素：
 * - `<mi>` 标识符（变量）
 * - `<mn>` 数字
 * - `<mo>` 运算符
 * - `<mfrac>` 分数
 * - `<msqrt>` / `<mroot>` 根号
 * - `<msup>` / `<msub>` / `<msubsup>` 上下标
 * - `<mover>` / `<munder>` / `<munderover>` 上下装饰
 * - `<mtable>` / `<mtr>` / `<mtd>` 矩阵
 * - `<mrow>` 行分组
 * - `<mtext>` 文本
 * - `<mstyle>` 样式
 * - `<menclose>` 围框/取消线
 * - `<mphantom>` 幻影
 */
class MathMLVisitor : BaseLatexVisitor<String>() {

    companion object {
        /**
         * 将 LaTeX AST 转换为完整的 MathML 字符串
         * @param node AST 根节点
         * @param displayMode 是否为 display 模式（block 或 inline）
         * @return 完整的 <math> 元素字符串
         */
        fun convert(node: LatexNode, displayMode: Boolean = true): String {
            val visitor = MathMLVisitor()
            val body = visitor.visit(node)
            val display = if (displayMode) "block" else "inline"
            return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"$display\">$body</math>"
        }

        /**
         * 将 LaTeX Document 转换为 MathML
         */
        fun convert(document: LatexNode.Document, displayMode: Boolean = true): String {
            return convert(document as LatexNode, displayMode)
        }
    }

    override fun defaultVisit(node: LatexNode): String = ""

    override fun visitDocument(node: LatexNode.Document): String {
        return mrow(node.children.joinToString("") { visit(it) })
    }

    override fun visitText(node: LatexNode.Text): String {
        return node.content.map { ch ->
            when {
                ch.isDigit() -> "<mn>$ch</mn>"
                ch.isLetter() -> "<mi>${escapeXml(ch.toString())}</mi>"
                ch == '+' || ch == '-' || ch == '=' || ch == '<' || ch == '>' ||
                    ch == ',' || ch == '.' || ch == '!' || ch == ':' || ch == ';' ->
                    "<mo>${escapeXml(ch.toString())}</mo>"
                ch == ' ' -> ""
                else -> "<mo>${escapeXml(ch.toString())}</mo>"
            }
        }.joinToString("")
    }

    override fun visitGroup(node: LatexNode.Group): String {
        return mrow(node.children.joinToString("") { visit(it) })
    }

    override fun visitSuperscript(node: LatexNode.Superscript): String {
        return "<msup>${visit(node.base)}${visit(node.exponent)}</msup>"
    }

    override fun visitSubscript(node: LatexNode.Subscript): String {
        return "<msub>${visit(node.base)}${visit(node.index)}</msub>"
    }

    override fun visitFraction(node: LatexNode.Fraction): String {
        return "<mfrac>${visit(node.numerator)}${visit(node.denominator)}</mfrac>"
    }

    override fun visitRoot(node: LatexNode.Root): String {
        return if (node.index != null) {
            "<mroot>${visit(node.content)}${visit(node.index)}</mroot>"
        } else {
            "<msqrt>${visit(node.content)}</msqrt>"
        }
    }

    override fun visitMatrix(node: LatexNode.Matrix): String {
        val (open, close) = when (node.type) {
            LatexNode.Matrix.MatrixType.PAREN -> "(" to ")"
            LatexNode.Matrix.MatrixType.BRACKET -> "[" to "]"
            LatexNode.Matrix.MatrixType.BRACE -> "{" to "}"
            LatexNode.Matrix.MatrixType.VBAR -> "|" to "|"
            LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> "‖" to "‖"
            LatexNode.Matrix.MatrixType.PLAIN -> "" to ""
        }
        val table = buildTable(node.rows)
        return if (open.isNotEmpty()) {
            mrow("<mo>$open</mo>$table<mo>$close</mo>")
        } else {
            table
        }
    }

    override fun visitArray(node: LatexNode.Array): String {
        return buildTable(node.rows)
    }

    override fun visitSymbol(node: LatexNode.Symbol): String {
        val unicode = node.unicode
        return if (unicode.length == 1 && unicode[0].isLetter()) {
            "<mi>${escapeXml(unicode)}</mi>"
        } else {
            "<mo>${escapeXml(unicode)}</mo>"
        }
    }

    override fun visitOperator(node: LatexNode.Operator): String {
        return "<mo>${escapeXml(node.op)}</mo>"
    }

    override fun visitSpace(node: LatexNode.Space): String {
        val width = when (node.type) {
            LatexNode.Space.SpaceType.THIN -> "thinmathspace"
            LatexNode.Space.SpaceType.MEDIUM -> "mediummathspace"
            LatexNode.Space.SpaceType.THICK -> "thickmathspace"
            LatexNode.Space.SpaceType.QUAD -> "1em"
            LatexNode.Space.SpaceType.QQUAD -> "2em"
            LatexNode.Space.SpaceType.NEGATIVE_THIN -> "negativethinmathspace"
            LatexNode.Space.SpaceType.NORMAL -> "0.25em"
        }
        return "<mspace width=\"$width\"/>"
    }

    override fun visitHSpace(node: LatexNode.HSpace): String {
        return "<mspace width=\"${escapeXml(node.dimension)}\"/>"
    }

    override fun visitNewLine(node: LatexNode.NewLine): String = ""

    override fun visitDelimited(node: LatexNode.Delimited): String {
        val content = node.content.joinToString("") { visit(it) }
        val left = if (node.left.isNotEmpty() && node.left != ".")
            "<mo stretchy=\"true\">${escapeXml(node.left)}</mo>" else ""
        val right = if (node.right.isNotEmpty() && node.right != ".")
            "<mo stretchy=\"true\">${escapeXml(node.right)}</mo>" else ""
        return mrow("$left$content$right")
    }

    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): String {
        return "<mo stretchy=\"true\">${escapeXml(node.delimiter)}</mo>"
    }

    override fun visitAccent(node: LatexNode.Accent): String {
        val content = visit(node.content)
        return when (node.accentType) {
            LatexNode.Accent.AccentType.HAT -> "<mover>$content<mo>^</mo></mover>"
            LatexNode.Accent.AccentType.TILDE -> "<mover>$content<mo>~</mo></mover>"
            LatexNode.Accent.AccentType.BAR -> "<mover>$content<mo>¯</mo></mover>"
            LatexNode.Accent.AccentType.DOT -> "<mover>$content<mo>˙</mo></mover>"
            LatexNode.Accent.AccentType.DDOT -> "<mover>$content<mo>¨</mo></mover>"
            LatexNode.Accent.AccentType.VEC -> "<mover>$content<mo>→</mo></mover>"
            LatexNode.Accent.AccentType.OVERLINE -> "<mover>$content<mo>¯</mo></mover>"
            LatexNode.Accent.AccentType.UNDERLINE -> "<munder>$content<mo>_</mo></munder>"
            LatexNode.Accent.AccentType.OVERBRACE -> "<mover>$content<mo>⏞</mo></mover>"
            LatexNode.Accent.AccentType.UNDERBRACE -> "<munder>$content<mo>⏟</mo></munder>"
            LatexNode.Accent.AccentType.WIDEHAT -> "<mover>$content<mo>^</mo></mover>"
            LatexNode.Accent.AccentType.OVERRIGHTARROW -> "<mover>$content<mo>→</mo></mover>"
            LatexNode.Accent.AccentType.OVERLEFTARROW -> "<mover>$content<mo>←</mo></mover>"
            LatexNode.Accent.AccentType.CANCEL -> "<menclose notation=\"updiagonalstrike\">$content</menclose>"
            LatexNode.Accent.AccentType.BCANCEL -> "<menclose notation=\"downdiagonalstrike\">$content</menclose>"
            LatexNode.Accent.AccentType.XCANCEL -> "<menclose notation=\"updiagonalstrike downdiagonalstrike\">$content</menclose>"
        }
    }

    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): String {
        val arrowChar = when (node.direction) {
            LatexNode.ExtensibleArrow.Direction.RIGHT -> "→"
            LatexNode.ExtensibleArrow.Direction.LEFT -> "←"
            LatexNode.ExtensibleArrow.Direction.BOTH -> "↔"
            LatexNode.ExtensibleArrow.Direction.HOOK_RIGHT -> "↪"
            LatexNode.ExtensibleArrow.Direction.HOOK_LEFT -> "↩"
        }
        val arrow = "<mo stretchy=\"true\">$arrowChar</mo>"
        val above = visit(node.content)
        val below = node.below?.let { visit(it) }
        return if (below != null) {
            "<munderover>$arrow${mrow(below)}${mrow(above)}</munderover>"
        } else {
            "<mover>$arrow${mrow(above)}</mover>"
        }
    }

    override fun visitStack(node: LatexNode.Stack): String {
        val base = visit(node.base)
        val above = node.above?.let { visit(it) }
        val below = node.below?.let { visit(it) }
        return when {
            above != null && below != null -> "<munderover>$base${mrow(below)}${mrow(above)}</munderover>"
            above != null -> "<mover>$base${mrow(above)}</mover>"
            below != null -> "<munder>$base${mrow(below)}</munder>"
            else -> base
        }
    }

    override fun visitStyle(node: LatexNode.Style): String {
        val content = node.content.joinToString("") { visit(it) }
        val attr = when (node.styleType) {
            LatexNode.Style.StyleType.BOLD, LatexNode.Style.StyleType.BOLD_SYMBOL ->
                " mathvariant=\"bold\""
            LatexNode.Style.StyleType.ITALIC -> " mathvariant=\"italic\""
            LatexNode.Style.StyleType.ROMAN -> " mathvariant=\"normal\""
            LatexNode.Style.StyleType.SANS_SERIF -> " mathvariant=\"sans-serif\""
            LatexNode.Style.StyleType.MONOSPACE -> " mathvariant=\"monospace\""
            LatexNode.Style.StyleType.BLACKBOARD_BOLD -> " mathvariant=\"double-struck\""
            LatexNode.Style.StyleType.FRAKTUR -> " mathvariant=\"fraktur\""
            LatexNode.Style.StyleType.SCRIPT -> " mathvariant=\"script\""
            LatexNode.Style.StyleType.CALLIGRAPHIC -> " mathvariant=\"script\""
        }
        return "<mstyle$attr>$content</mstyle>"
    }

    override fun visitColor(node: LatexNode.Color): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mstyle mathcolor=\"${escapeXml(node.color)}\">$content</mstyle>"
    }

    override fun visitMathStyle(node: LatexNode.MathStyle): String {
        val content = node.content.joinToString("") { visit(it) }
        val size = when (node.mathStyleType) {
            LatexNode.MathStyle.MathStyleType.DISPLAY -> "normal"
            LatexNode.MathStyle.MathStyleType.TEXT -> "normal"
            LatexNode.MathStyle.MathStyleType.SCRIPT -> "scriptlevel=\"1\""
            LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> "scriptlevel=\"2\""
        }
        val display = when (node.mathStyleType) {
            LatexNode.MathStyle.MathStyleType.DISPLAY -> " displaystyle=\"true\""
            LatexNode.MathStyle.MathStyleType.TEXT -> " displaystyle=\"false\""
            else -> ""
        }
        return "<mstyle$display>$content</mstyle>"
    }

    override fun visitBigOperator(node: LatexNode.BigOperator): String {
        val op = "<mo>${escapeXml(node.operator)}</mo>"
        val sub = node.subscript?.let { visit(it) }
        val sup = node.superscript?.let { visit(it) }
        return when {
            sub != null && sup != null -> "<munderover>$op${mrow(sub)}${mrow(sup)}</munderover>"
            sub != null -> "<munder>$op${mrow(sub)}</munder>"
            sup != null -> "<mover>$op${mrow(sup)}</mover>"
            else -> op
        }
    }

    override fun visitAligned(node: LatexNode.Aligned): String {
        return buildTable(node.rows)
    }

    override fun visitCases(node: LatexNode.Cases): String {
        val rows = node.cases.map { (expr, cond) -> listOf(expr, cond) }
        return mrow("<mo>{</mo>${buildTable(rows)}")
    }

    override fun visitSplit(node: LatexNode.Split): String {
        return buildTable(node.rows)
    }

    override fun visitMultline(node: LatexNode.Multline): String {
        val rows = node.lines.map { listOf(it) }
        return buildTable(rows)
    }

    override fun visitEqnarray(node: LatexNode.Eqnarray): String {
        return buildTable(node.rows)
    }

    override fun visitSubequations(node: LatexNode.Subequations): String {
        return node.content.joinToString("") { visit(it) }
    }

    override fun visitBinomial(node: LatexNode.Binomial): String {
        return mrow("<mo>(</mo><mfrac linethickness=\"0\">${visit(node.top)}${visit(node.bottom)}</mfrac><mo>)</mo>")
    }

    override fun visitTextMode(node: LatexNode.TextMode): String {
        return "<mtext>${escapeXml(node.text)}</mtext>"
    }

    override fun visitNegation(node: LatexNode.Negation): String {
        return "<menclose notation=\"updiagonalstrike\">${visit(node.content)}</menclose>"
    }

    override fun visitTag(node: LatexNode.Tag): String {
        val label = visit(node.label)
        return if (node.starred) label else mrow("<mo>(</mo>$label<mo>)</mo>")
    }

    override fun visitSubstack(node: LatexNode.Substack): String {
        return buildTable(node.rows)
    }

    override fun visitSmash(node: LatexNode.Smash): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mpadded height=\"0\" depth=\"0\">$content</mpadded>"
    }

    override fun visitVPhantom(node: LatexNode.VPhantom): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mphantom>$content</mphantom>"
    }

    override fun visitHPhantom(node: LatexNode.HPhantom): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mphantom>$content</mphantom>"
    }

    override fun visitLabel(node: LatexNode.Label): String = ""

    override fun visitRef(node: LatexNode.Ref): String {
        return "<mtext>${escapeXml(node.key)}</mtext>"
    }

    override fun visitEqRef(node: LatexNode.EqRef): String {
        return mrow("<mo>(</mo><mtext>${escapeXml(node.key)}</mtext><mo>)</mo>")
    }

    override fun visitSideSet(node: LatexNode.SideSet): String {
        val base = visit(node.base)
        // 用 mmultiscripts 表示四角标注
        val leftSub = node.leftSub?.let { visit(it) } ?: "<none/>"
        val leftSup = node.leftSup?.let { visit(it) } ?: "<none/>"
        val rightSub = node.rightSub?.let { visit(it) } ?: "<none/>"
        val rightSup = node.rightSup?.let { visit(it) } ?: "<none/>"
        return "<mmultiscripts>$base$rightSub$rightSup<mprescripts/>$leftSub$leftSup</mmultiscripts>"
    }

    override fun visitTensor(node: LatexNode.Tensor): String {
        val base = visit(node.base)
        if (node.indices.isEmpty()) return base
        // 简化处理：配对为 sub/sup
        var result = base
        var i = 0
        while (i < node.indices.size) {
            val (isUpper, indexNode) = node.indices[i]
            val idx = visit(indexNode)
            if (isUpper) {
                // 检查下一个是否为下标
                if (i + 1 < node.indices.size && !node.indices[i + 1].first) {
                    val subIdx = visit(node.indices[i + 1].second)
                    result = "<msubsup>$result${mrow(subIdx)}${mrow(idx)}</msubsup>"
                    i += 2
                } else {
                    result = "<msup>$result${mrow(idx)}</msup>"
                    i++
                }
            } else {
                if (i + 1 < node.indices.size && node.indices[i + 1].first) {
                    val supIdx = visit(node.indices[i + 1].second)
                    result = "<msubsup>$result${mrow(idx)}${mrow(supIdx)}</msubsup>"
                    i += 2
                } else {
                    result = "<msub>$result${mrow(idx)}</msub>"
                    i++
                }
            }
        }
        return result
    }

    override fun visitTabular(node: LatexNode.Tabular): String {
        return buildTable(node.rows)
    }

    override fun visitBoxed(node: LatexNode.Boxed): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<menclose notation=\"box\">$content</menclose>"
    }

    override fun visitPhantom(node: LatexNode.Phantom): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mphantom>$content</mphantom>"
    }

    override fun visitNewCommand(node: LatexNode.NewCommand): String = ""

    // ========== 辅助方法 ==========

    private fun mrow(content: String): String = "<mrow>$content</mrow>"

    private fun buildTable(rows: List<List<LatexNode>>): String {
        val sb = StringBuilder("<mtable>")
        for (row in rows) {
            sb.append("<mtr>")
            for (cell in row) {
                sb.append("<mtd>")
                sb.append(visit(cell))
                sb.append("</mtd>")
            }
            sb.append("</mtr>")
        }
        sb.append("</mtable>")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
