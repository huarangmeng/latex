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

package com.hrm.latex.renderer.utils

import com.hrm.latex.parser.MathAtomSymbols
import com.hrm.latex.parser.model.LatexNode

/**
 * TeX 原子类型，用于确定元素间的数学间距
 *
 * 参考 TeXbook Chapter 18 的间距规则表。
 */
enum class AtomType {
    ORD,    // 普通符号：变量、数字、希腊字母
    OP,     // 大型运算符：\sum, \int, \prod, \lim
    BIN,    // 二元运算符：+, -, \times, \div, \cdot
    REL,    // 关系符号：=, <, >, \leq, \geq, \approx
    OPEN,   // 开定界符：(, [, {
    CLOSE,  // 闭定界符：), ], }
    PUNCT,  // 标点：, ; （普通冒号是关系符）
    INNER   // 内部结构：\left ... \right、点列等
}

/**
 * 间距规则枚举
 */
private enum class SpaceRule {
    NONE,   // 0mu
    THIN,   // 3mu (约 0.167em)
    MEDIUM, // 4mu (约 0.222em)，仅在非 script 样式下
    THICK   // 5mu (约 0.278em)，仅在非 script 样式下
}

/**
 * TeX 标准数学间距系统
 *
 * 基础间距单位是 mu (math unit)，1em = 18mu。
 * 间距定义为 fontSize 的比例系数。
 */
object MathSpacing {

    private const val THIN = 3f / 18f      // \, = 3mu ≈ 0.167em
    private const val MEDIUM = 4f / 18f    // \: = 4mu ≈ 0.222em
    private const val THICK = 5f / 18f     // \; = 5mu ≈ 0.278em

    /**
     * TeX 的 display/text 间距表；script 使用独立的紧间距规则。
     *
     * 行 = 左原子类型，列 = 右原子类型
     * null 表示该组合不应出现（按 NONE 处理）
     */
    private val SPACING_TABLE: Array<Array<SpaceRule?>> = arrayOf(
        //              ORD        OP         BIN        REL        OPEN       CLOSE      PUNCT      INNER
        /* ORD   */ arrayOf(
            SpaceRule.NONE,
            SpaceRule.THIN,
            SpaceRule.MEDIUM,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THIN
        ),
        /* OP    */
        arrayOf(
            SpaceRule.THIN,
            SpaceRule.THIN,
            null,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THIN
        ),
        /* BIN   */
        arrayOf(
            SpaceRule.MEDIUM,
            SpaceRule.MEDIUM,
            null,
            null,
            SpaceRule.MEDIUM,
            null,
            null,
            SpaceRule.MEDIUM
        ),
        /* REL   */
        arrayOf(
            SpaceRule.THICK,
            SpaceRule.THICK,
            null,
            SpaceRule.NONE,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THICK
        ),
        /* OPEN  */
        arrayOf(
            SpaceRule.NONE,
            SpaceRule.NONE,
            null,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE
        ),
        /* CLOSE */
        arrayOf(
            SpaceRule.NONE,
            SpaceRule.THIN,
            SpaceRule.MEDIUM,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THIN
        ),
        /* PUNCT */
        arrayOf(
            SpaceRule.THIN,
            SpaceRule.THIN,
            null,
            SpaceRule.THICK,
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.THIN
        ),
        /* INNER */
        arrayOf(
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.MEDIUM,
            SpaceRule.THICK,
            SpaceRule.THIN,
            SpaceRule.NONE,
            SpaceRule.THIN,
            SpaceRule.THIN
        )
    )

    /**
     * 根据左右原子类型和当前数学样式，返回间距比例系数
     *
     * @return fontSize 的比例系数（乘以 fontSizePx 得到像素值）
     */
    fun spaceBetween(left: AtomType, right: AtomType, isScript: Boolean): Float {
        if (isScript) return if (
            right == AtomType.OP && (left == AtomType.ORD || left == AtomType.OP || left == AtomType.CLOSE || left == AtomType.INNER) ||
            left == AtomType.OP && right == AtomType.ORD
        ) THIN else 0f
        val rule = SPACING_TABLE[left.ordinal][right.ordinal] ?: return 0f
        return when (rule) {
            SpaceRule.NONE -> 0f
            SpaceRule.THIN -> THIN
            SpaceRule.MEDIUM -> if (isScript) 0f else MEDIUM
            SpaceRule.THICK -> if (isScript) 0f else THICK
        }
    }

    fun effectiveAtomType(nodes: List<LatexNode>, index: Int): AtomType {
        return MathAtomSequence(nodes).typeAt(index, 0, nodes.size)
    }

    /**
     * 标点符号的 Unicode
     */
    private val PUNCTUATION_CHARS = setOf(',', ';')

    /**
     * 开定界符
     */
    private val OPEN_DELIMITERS = setOf("(", "[", "{", "⟦", "llbracket", "\\{", "\\langle", "\\lfloor", "\\lceil")

    /**
     * 闭定界符
     */
    private val CLOSE_DELIMITERS = setOf(")", "]", "}", "⟧", "rrbracket", "\\}", "\\rangle", "\\rfloor", "\\rceil")

    /**
     * 将 LatexNode 分类为 AtomType
     *
     * 部分节点类型需要根据具体内容做更细粒度的分类（如 Text 的字符、Symbol 的名称），
     * 其余节点使用默认分类。
     */
    fun classifyNode(node: LatexNode): AtomType {
        return when (node) {
            is LatexNode.MathClass -> mapMathClass(node.atomClass)
            is LatexNode.Text -> classifyText(node.content)
            is LatexNode.Symbol -> classifySymbol(node.symbol, node.unicode)
            is LatexNode.Operator -> classifyOperatorText()
            is LatexNode.BigOperator -> AtomType.OP
            is LatexNode.Fraction, is LatexNode.Binomial -> AtomType.ORD
            is LatexNode.Root -> AtomType.ORD
            is LatexNode.Superscript -> classifyNode(node.base)
            is LatexNode.Subscript -> classifyNode(node.base)
            is LatexNode.Style -> {
                if (node.styleType.isTextStyle) AtomType.ORD else {
                    var content = node.content.filterNot(::isNonAtom)
                    if (content.size == 1 && content[0] is LatexNode.Group) {
                        content = (content[0] as LatexNode.Group).children.filterNot(::isNonAtom)
                    }
                    if (node.styleType == LatexNode.Style.StyleType.BOLD_SYMBOL) {
                        content.firstOrNull()?.let(::classifyNode)?.takeIf { it == AtomType.BIN || it == AtomType.REL } ?: AtomType.ORD
                    } else content.singleOrNull()?.let(::classifyNode) ?: AtomType.ORD
                }
            }
            is LatexNode.Delimited -> AtomType.INNER
            is LatexNode.ManualSizedDelimiter -> classifyDelimiterString(node.delimiter)
            is LatexNode.Space, is LatexNode.HSpace -> AtomType.ORD
            is LatexNode.Group -> AtomType.ORD
            is LatexNode.Accent -> if (node.accentType == LatexNode.Accent.AccentType.CENTER_NOT) AtomType.REL else AtomType.ORD
            is LatexNode.Html -> node.content.singleOrNull()?.let(::classifyNode) ?: AtomType.ORD
            is LatexNode.Stack -> {
                val base = (node.base as? LatexNode.Group)?.children?.firstOrNull() ?: node.base
                classifyNode(base).takeIf { it == AtomType.BIN || it == AtomType.REL } ?: AtomType.ORD
            }
            is LatexNode.ExtensibleArrow -> AtomType.REL
            is LatexNode.Negation -> AtomType.REL
            is LatexNode.TextMode -> AtomType.ORD
            is LatexNode.Boxed -> AtomType.ORD
            is LatexNode.Cases -> AtomType.INNER
            is LatexNode.Matrix -> if (node.type == LatexNode.Matrix.MatrixType.PLAIN) AtomType.ORD else AtomType.INNER
            is LatexNode.OperatorName -> AtomType.OP
            is LatexNode.ModOperator -> AtomType.BIN
            else -> AtomType.ORD
        }
    }

    private fun classifyText(content: String): AtomType {
        if (content.length == 1) {
            val ch = content[0]
            if (ch in PUNCTUATION_CHARS) return AtomType.PUNCT
            if (ch == '(' || ch == '[') return AtomType.OPEN
            if (ch == ')' || ch == ']' || ch == '!') return AtomType.CLOSE
            if (ch == '+' || ch == '-' || ch == '*') return AtomType.BIN
            if (ch == '=' || ch == '<' || ch == '>' || ch == ':') return AtomType.REL
            if (ch == '.') return AtomType.ORD
            return mapMathClass(MathAtomSymbols.classify("", content))
        }
        return AtomType.ORD
    }

    private fun classifySymbol(symbol: String, unicode: String): AtomType =
        mapMathClass(MathAtomSymbols.classify(symbol, unicode))

    private fun classifyOperatorText(): AtomType {
        // Operator node 通常是命名运算符如 sin, cos, lim 等
        return AtomType.OP
    }

    private fun mapMathClass(atomClass: LatexNode.MathClass.AtomClass): AtomType = when (atomClass) {
        LatexNode.MathClass.AtomClass.ORD -> AtomType.ORD
        LatexNode.MathClass.AtomClass.OP -> AtomType.OP
        LatexNode.MathClass.AtomClass.BIN -> AtomType.BIN
        LatexNode.MathClass.AtomClass.REL -> AtomType.REL
        LatexNode.MathClass.AtomClass.OPEN -> AtomType.OPEN
        LatexNode.MathClass.AtomClass.CLOSE -> AtomType.CLOSE
        LatexNode.MathClass.AtomClass.PUNCT -> AtomType.PUNCT
        LatexNode.MathClass.AtomClass.INNER -> AtomType.INNER
    }

    private fun classifyDelimiterString(delimiter: String): AtomType {
        return when {
            delimiter in OPEN_DELIMITERS || delimiter.length == 1 && delimiter[0] in "([{" -> AtomType.OPEN
            delimiter in CLOSE_DELIMITERS || delimiter.length == 1 && delimiter[0] in ")]}" -> AtomType.CLOSE
            else -> mapMathClass(MathAtomSymbols.classify("", delimiter))
        }
    }

    internal fun isNonAtom(node: LatexNode): Boolean = when (node) {
        is LatexNode.Space, is LatexNode.HSpace, is LatexNode.Label,
        is LatexNode.NewCommand, is LatexNode.NewEnvironment -> true
        is LatexNode.Text -> node.content.isEmpty()
        else -> false
    }
}
