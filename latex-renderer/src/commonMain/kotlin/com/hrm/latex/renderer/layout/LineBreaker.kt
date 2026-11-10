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

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode

/**
 * 基于断点优先级的公式换行器：空格、关系符、加减、乘除依次优先，运算符放在新行。
 * 分数、矩阵等原子不拆开；只有单个原子本身超宽时允许该行超过宽度限制。
 * 使用区间输出和单调候选队列，每个节点、候选点至多入队和出队一次。
 */
internal class LineBreaker(private val maxWidth: Float) {

    companion object {
        private const val NOBREAK = Int.MAX_VALUE / 2
        private const val PENALTY_SPACE = -100
        private const val PENALTY_RELATION = 0
        private const val PENALTY_ADDITIVE = 100
        private const val PENALTY_MULTIPLICATIVE = 200
        private const val PENALTY_OTHER_OP = 300
        private const val PENALTY_DEPTH_FACTOR = 400
    }

    fun breakIntoLines(
        nodes: List<LatexNode>,
        widths: FloatArray
    ): List<List<Int>> = breakIntoRanges(nodes, widths).map { it.toList() }

    /**
     * 返回连续、不重叠且覆盖全部节点的行区间。
     *
     * [lineWidth] 查询 [start, endExclusive) 的最终宽度，必须为 O(1)：调用方可用
     * 原子宽度、间距的前缀和，再修正行首、行尾的 TeX 原子分类；不要在回调中扫描整行。
     * 未提供回调时只计算节点宽度的前缀和。
     */
    fun breakIntoRanges(
        nodes: List<LatexNode>,
        widths: FloatArray,
        lineWidth: ((start: Int, endExclusive: Int) -> Float)? = null
    ): List<IntRange> {
        require(widths.size == nodes.size) { "节点与宽度数量必须一致" }
        if (nodes.isEmpty()) return listOf(IntRange.EMPTY)

        val prefixWidths = if (lineWidth == null) {
            DoubleArray(widths.size + 1).also { prefix ->
                for (index in widths.indices) prefix[index + 1] = prefix[index] + widths[index]
            }
        } else null
        fun width(start: Int, end: Int): Float = lineWidth?.invoke(start, end)
            ?: (prefixWidths!![end] - prefixWidths[start]).toFloat()

        if (width(0, nodes.size) <= maxWidth) return listOf(nodes.indices)

        // 按索引递增、罚分递增保存候选；同罚分保留更靠右的断点，避免过短行。
        val candidateIndices = IntArray(nodes.size)
        val candidatePenalties = IntArray(nodes.size)
        var candidateHead = 0
        var candidateEnd = 0
        val forbidden = forbiddenBreaks(nodes)
        val ranges = ArrayList<IntRange>()
        var lineStart = 0
        var lastAllowedBoundary = 0
        var depth = 0

        for (i in nodes.indices) {
            val node = nodes[i]
            depth += getDepthDelta(node, entering = true)
            val penalty = calculatePenalty(node, depth)
            val allowed = forbidden?.get(i) != true
            if (allowed) lastAllowedBoundary = i
            if (i > lineStart && penalty < NOBREAK && allowed) {
                while (candidateEnd > candidateHead && candidatePenalties[candidateEnd - 1] >= penalty) {
                    candidateEnd--
                }
                candidateIndices[candidateEnd] = i
                candidatePenalties[candidateEnd] = penalty
                candidateEnd++
            }
            depth += getDepthDelta(node, entering = false)

            // 提交旧候选后，剩余部分加当前节点仍可能超宽，必须继续检查。
            while (i > lineStart && width(lineStart, i + 1) > maxWidth) {
                var breakAt = if (candidateHead < candidateEnd) candidateIndices[candidateHead] else lastAllowedBoundary
                if (breakAt <= lineStart) break
                // 负间距或边界修正可能使去掉前缀后的候选行反而变宽，不能提交超宽组合。
                if (breakAt > lineStart + 1 && width(lineStart, breakAt) > maxWidth) {
                    breakAt = lineStart + 1
                    while (breakAt < i && forbidden?.get(breakAt) == true) breakAt++
                    if (forbidden?.get(breakAt) == true) break
                }
                ranges.add(lineStart until breakAt)
                lineStart = breakAt
                // 保留新行中尚未过期的候选，避免重新扫描后缀或丢失更靠后的运算符。
                while (candidateHead < candidateEnd && candidateIndices[candidateHead] <= lineStart) {
                    candidateHead++
                }
            }
        }
        ranges.add(lineStart until nodes.size)
        return ranges
    }

    /** A no-break marker binds the atoms on both sides, including intervening spaces. */
    private fun forbiddenBreaks(nodes: List<LatexNode>): BooleanArray? {
        var forbidden: BooleanArray? = null
        var previousAtom = -1
        var binding = false
        for ((index, node) in nodes.withIndex()) {
            if ((node as? LatexNode.HSpace)?.breakHint == LatexNode.HSpace.BreakHint.FORBID) binding = true
            if (node is LatexNode.Space || node is LatexNode.HSpace) continue
            if (binding && previousAtom >= 0) {
                if (forbidden == null) forbidden = BooleanArray(nodes.size)
                for (boundary in previousAtom + 1..index) forbidden[boundary] = true
                // This breaker places operators at the start of the next line.
                if (calculatePenalty(nodes[previousAtom], 0) < NOBREAK) forbidden[previousAtom] = true
            }
            previousAtom = index
            binding = false
        }
        return forbidden
    }

    private fun getDepthDelta(node: LatexNode, entering: Boolean): Int {
        val delta = when (node) {
            is LatexNode.Group,
            is LatexNode.Delimited,
            is LatexNode.Style,
            is LatexNode.MathClass,
            is LatexNode.Color,
            is LatexNode.MathStyle,
            is LatexNode.FontSize -> 1

            else -> 0
        }
        return if (entering) delta else -delta
    }

    private fun calculatePenalty(node: LatexNode, depth: Int): Int {
        val basePenalty = when (node) {
            is LatexNode.Operator -> getOperatorPenalty(node.op)
            is LatexNode.Symbol -> getSymbolPenalty(node.unicode)
            is LatexNode.Text -> getTextPenalty(node.content)
            is LatexNode.Space -> PENALTY_SPACE
            is LatexNode.HSpace -> if (node.breakHint == LatexNode.HSpace.BreakHint.ALLOW) PENALTY_SPACE else NOBREAK

            is LatexNode.Fraction,
            is LatexNode.Root,
            is LatexNode.Matrix,
            is LatexNode.Array,
            is LatexNode.Delimited,
            is LatexNode.BigOperator,
            is LatexNode.Accent,
            is LatexNode.Binomial,
            is LatexNode.Cases,
            is LatexNode.Superscript,
            is LatexNode.Subscript,
            is LatexNode.Stack,
            is LatexNode.ExtensibleArrow,
            is LatexNode.ManualSizedDelimiter -> NOBREAK

            is LatexNode.Group -> {
                if (node.children.size == 1) {
                    calculatePenalty(node.children[0], depth)
                } else {
                    NOBREAK
                }
            }

            else -> NOBREAK
        }

        return if (basePenalty >= NOBREAK) {
            NOBREAK
        } else {
            basePenalty + depth * PENALTY_DEPTH_FACTOR
        }
    }

    private fun getOperatorPenalty(op: String): Int = when (op) {
        "=", "≠", "≈", "≡", "∼", "≃", "≅", "<", ">", "≤", "≥", "≪", "≫",
        "⊂", "⊃", "⊆", "⊇", "∈", "∋", "≺", "≻", "∝", "≔", "≕", "∷",
        "eq", "ne", "neq", "approx", "equiv", "sim", "le", "leq", "ge", "geq",
        "ll", "gg", "subset", "supset", "subseteq", "supseteq", "in", "ni",
        "prec", "succ", "propto", "varpropto",
        "coloneqq", "coloneq", "eqqcolon", "eqcolon", "dblcolon" -> PENALTY_RELATION

        "+", "-", "−", "±", "∓",
        "pm", "mp", "plus", "minus" -> PENALTY_ADDITIVE

        "×", "÷", "·", "∗", "⋅", "∘",
        "times", "div", "cdot", "ast", "star", "circ" -> PENALTY_MULTIPLICATIVE

        else -> PENALTY_OTHER_OP
    }

    private fun getSymbolPenalty(unicode: String): Int = when (unicode) {
        "=", "≠", "≈", "≡", "∼", "≃", "≅", "<", ">", "≤", "≥", "≪", "≫",
        "⊂", "⊃", "⊆", "⊇", "∈", "∋", "≺", "≻", "∝", "≔", "≕", "∷" -> PENALTY_RELATION

        "+", "-", "−", "±", "∓" -> PENALTY_ADDITIVE

        "×", "÷", "·", "∗", "⋅", "∘" -> PENALTY_MULTIPLICATIVE

        else -> NOBREAK
    }

    private fun getTextPenalty(content: String): Int {
        val c = content.trim()
        return when {
            c.length != 1 -> NOBREAK
            c[0] in "=<>" -> PENALTY_RELATION
            c[0] in "+-" -> PENALTY_ADDITIVE
            else -> NOBREAK
        }
    }
}
