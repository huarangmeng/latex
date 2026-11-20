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


package com.hrm.latex.parser.component

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.component.handler.text
import com.hrm.latex.parser.tokenizer.LatexToken

internal class EnvironmentParser(private val context: LatexParserContext) {
    private val tokenStream get() = context.tokenStream

    private enum class Kind(vararg val names: String) {
        MATRIX(
            "matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix",
            "matrix*", "pmatrix*", "bmatrix*", "Bmatrix*", "vmatrix*", "Vmatrix*",
        ),
        SMALL_MATRIX("smallmatrix", "smallmatrix*"),
        ARRAY("array"),
        SUBARRAY("subarray"),
        TABULAR("tabular"),
        ALIGN("align", "aligned", "align*", "gather", "gathered", "gather*", "flalign", "flalign*"),
        ALIGNAT("alignat", "alignat*", "alignedat"),
        SPLIT("split"),
        PROOF("prooftree"),
        MULTLINE("multline", "multline*", "multlined"),
        EQNARRAY("eqnarray", "eqnarray*"),
        SUBEQUATIONS("subequations"),
        CASES("cases", "cases*"),
        DISPLAY_CASES("dcases", "dcases*"),
        RIGHT_CASES("rcases", "rcases*"),
        EQUATION("equation", "equation*", "displaymath"),
        RTL("RTL"),
        LTR("LTR"),
    }

    companion object {
        // 环境分发与覆盖检查来自同一份入口表。
        private val kindsByName = buildMap {
            for (kind in Kind.entries) for (name in kind.names) put(name, kind)
        }

        internal fun supportedEnvironments(): Set<String> = kindsByName.keys

        private const val TAG = "EnvironmentParser"
        private val ROW_GAP_PATTERN =
            Regex("""([+-]?(?:\d+(?:\.\d*)?|\.\d+))\s*(em|ex|mu|pt|mm|cm|in|bp|pc|dd|cc|sp)""")
    }

    private data class RowColumnStructure(
        val rows: List<List<LatexNode>>,
        val rowGaps: List<LatexNode.RowGap?>
    )

    /**
     * 解析环境
     */
    fun parseEnvironment(): LatexNode? {
        val beginToken = tokenStream.advance() as? LatexToken.BeginEnvironment ?: return null
        val envName = beginToken.name
        val startOffset = beginToken.range.start

        HLog.d(TAG) { "解析环境: $envName" }

        context.pushScope()
        return try {
            val result = when (kindsByName[envName]) {
                Kind.MATRIX -> parseMatrix(envName, isSmall = false)
                Kind.SMALL_MATRIX -> parseMatrix(envName, isSmall = true)
                Kind.ARRAY -> parseArray()
                Kind.SUBARRAY -> parseSubarray()
                Kind.TABULAR -> parseTabular()
                Kind.ALIGN -> parseAligned(envName)
                Kind.ALIGNAT -> parseAlignat(envName)
                Kind.SPLIT -> parseSplit()
                Kind.PROOF -> parseProofTree(context)
                Kind.MULTLINE -> parseMultline(envName)
                Kind.EQNARRAY -> parseEqnarray(envName)
                Kind.SUBEQUATIONS -> parseSubequations()
                Kind.CASES -> parseCases(LatexNode.Cases.CasesStyle.NORMAL)
                Kind.DISPLAY_CASES -> parseCases(LatexNode.Cases.CasesStyle.DISPLAY)
                Kind.RIGHT_CASES -> parseCases(LatexNode.Cases.CasesStyle.RIGHT)
                Kind.RTL -> LatexNode.TextDirection(parseEnvironmentContent(), LatexNode.TextDirection.Direction.RTL)
                Kind.LTR -> LatexNode.TextDirection(parseEnvironmentContent(), LatexNode.TextDirection.Direction.LTR)
                Kind.EQUATION, null -> LatexNode.Environment(envName, parseEnvironmentContent())
            }
            // 环境内容停在结束标记前，由进入环境的这一层完成配对。
            val closing = tokenStream.peek() as? LatexToken.EndEnvironment
            if (!tokenStream.consumeClosing { it is LatexToken.EndEnvironment && it.name == envName }) {
                val message = if (closing == null) "Missing \\end{$envName}"
                    else "Expected \\end{$envName}, found \\end{${closing.name}}"
                context.diagnostics.add(ParseDiagnostic(
                    closing?.range ?: beginToken.range, message,
                    ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.MISMATCHED_ENVIRONMENT
                ))
            }
            result.withSourceRange(tokenStream.rangeFrom(startOffset))
        } finally { context.popScope() }
    }

    // ====================================================================
    // 通用行列结构解析器
    // ====================================================================

    /**
     * 通用行列结构解析器。
     *
     * 统一处理 matrix、aligned、split、eqnarray 等环境中的
     * EndEnvironment / Ampersand / NewLine token 分发逻辑。
     *
     * @param handleSpecialNodes 是否将 HLine/CLine 作为独立行标记处理（仅 array/tabular 需要）
     */
    private fun parseRowColumnStructure(handleSpecialNodes: Boolean = false): RowColumnStructure {
        val rows = mutableListOf<List<LatexNode>>()
        val rowGaps = mutableListOf<LatexNode.RowGap?>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        context.pushScope()
        try {
            while (!tokenStream.isEOF() && !tokenStream.peek().isScopeCloser()) {
                if ((tokenStream.peek() as? LatexToken.Command)?.name.let { it == "cr" || it == "newline" }) {
                    if (currentCell.isNotEmpty()) {
                        currentRow.add(LatexNode.Group(context.normalizeStyleDeclarations(currentCell)))
                    }
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentCell = mutableListOf()
                    tokenStream.advance()
                    context.popScope()
                    context.pushScope()
                    rowGaps.add(null)
                    continue
                }
                when (tokenStream.peek()) {
                    is LatexToken.Ampersand -> {
                        currentRow.add(LatexNode.Group(context.normalizeStyleDeclarations(currentCell)))
                        currentCell = mutableListOf()
                        tokenStream.advance()
                        context.popScope()
                        context.pushScope()
                    }

                    is LatexToken.NewLine -> {
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(context.normalizeStyleDeclarations(currentCell)))
                        }
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                        currentCell = mutableListOf()
                        tokenStream.advance()
                        context.popScope()
                        context.pushScope()
                        rowGaps.add(consumeOptionalRowSpacing())
                    }

                    else -> {
                        val node = context.parseExpression()
                        if (node != null) {
                            if (handleSpecialNodes && (node is LatexNode.HLine || node is LatexNode.CLine)) {
                                // 先保存当前累积的内容
                                if (currentCell.hasVisibleContent()) {
                                    currentRow.add(LatexNode.Group(context.normalizeStyleDeclarations(currentCell)))
                                    currentCell = mutableListOf()
                                }
                                if (currentRow.isNotEmpty()) {
                                    rows.add(currentRow)
                                    currentRow = mutableListOf()
                                }
                                rows.add(listOf(node))
                            } else {
                                currentCell.add(node)
                            }
                        }
                    }
                }
            }

            if (currentCell.hasVisibleContent()) currentRow.add(LatexNode.Group(context.normalizeStyleDeclarations(currentCell)))
            if (currentRow.isNotEmpty()) rows.add(currentRow)
            return RowColumnStructure(rows, rowGaps)
        } finally { context.popScope() }
    }

    private fun List<LatexNode>.hasVisibleContent(): Boolean = any { it !is LatexNode.Space }

    private fun consumeOptionalRowSpacing(): LatexNode.RowGap? {
        if (tokenStream.peek() !is LatexToken.LeftBracket) {
            return null
        }

        var offset = 1
        while (true) {
            when (tokenStream.peek(offset)) {
                is LatexToken.RightBracket -> break
                is LatexToken.EOF,
                is LatexToken.EndEnvironment,
                is LatexToken.NewLine,
                null,
                -> return null
                else -> offset++
            }
        }

        tokenStream.advance()
        val value = buildString {
            repeat(offset - 1) {
                when (val token = tokenStream.advance()) {
                    is LatexToken.Text -> append(token.content)
                    is LatexToken.Whitespace -> append(token.content)
                    else -> Unit
                }
            }
        }
        tokenStream.advance()

        val match = ROW_GAP_PATTERN.matchEntire(value.trim()) ?: return null
        return LatexNode.RowGap(
            number = match.groupValues[1].toDouble(),
            unit = match.groupValues[2]
        )
    }

    // ====================================================================
    // 各环境解析方法（委托 parseRowColumnStructure）
    // ====================================================================

    /**
     * 解析矩阵
     */
    private fun parseMatrix(envName: String, isSmall: Boolean = false): LatexNode.Matrix {
        val stretch = arrayStretch()
        val separation = context.lengths["arraycolsep"]
        val baseEnvName = envName.removeSuffix("*")
        val matrixType = when (baseEnvName) {
            "pmatrix" -> LatexNode.Matrix.MatrixType.PAREN
            "bmatrix" -> LatexNode.Matrix.MatrixType.BRACKET
            "Bmatrix" -> LatexNode.Matrix.MatrixType.BRACE
            "vmatrix" -> LatexNode.Matrix.MatrixType.VBAR
            "Vmatrix" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
            else -> LatexNode.Matrix.MatrixType.PLAIN
        }

        val alignment = if (envName.endsWith("*")) parseOptionalBracketText() else null
        val structure = parseRowColumnStructure()
        return LatexNode.Matrix(
            structure.rows,
            matrixType,
            isSmall,
            rowGaps = structure.rowGaps,
            alignment = alignment, rowStretch = stretch, columnSep = separation
        )
    }

    /**
     * 解析数组环境（array）
     */
    private fun parseArray(): LatexNode.Array {
        val stretch = arrayStretch()
        val separation = context.lengths["arraycolsep"]
        val alignment = parseAlignmentSpec()
        val structure = parseRowColumnStructure(handleSpecialNodes = true)
        return LatexNode.Array(structure.rows, alignment.columns, rowGaps = structure.rowGaps,
            rowStretch = stretch, columnSep = separation, tightColumnBoundaries = alignment.tightBoundaries)
    }

    private fun parseSubarray(): LatexNode.Array {
        val alignment = parseAlignmentSpec()
        val structure = parseRowColumnStructure()
        return LatexNode.Array(structure.rows, alignment.columns, rowGaps = structure.rowGaps,
            tightColumnBoundaries = alignment.tightBoundaries)
    }

    /**
     * 解析 tabular 环境
     */
    private fun parseTabular(): LatexNode.Tabular {
        val stretch = arrayStretch()
        val separation = context.lengths["arraycolsep"]
        val alignment = parseAlignmentSpec()
        val structure = parseRowColumnStructure(handleSpecialNodes = true)
        return LatexNode.Tabular(structure.rows, alignment.columns, rowGaps = structure.rowGaps,
            rowStretch = stretch, columnSep = separation, tightColumnBoundaries = alignment.tightBoundaries)
    }

    /**
     * 解析对齐环境
     */
    private fun parseAligned(envName: String): LatexNode.Aligned {
        val structure = parseRowColumnStructure()
        return LatexNode.Aligned(
            structure.rows,
            envName = envName,
            rowGaps = structure.rowGaps
        )
    }

    /**
     * 解析 alignat / alignat* 环境
     */
    private fun parseAlignat(envName: String): LatexNode.Aligned {
        // 跳过可选的 {n} 参数（列对数）
        if (tokenStream.peek() is LatexToken.LeftBrace) {
            context.parseArgument()
        }
        return parseAligned(envName)
    }

    /**
     * 解析 split 环境
     */
    private fun parseSplit(): LatexNode.Split {
        val structure = parseRowColumnStructure()
        return LatexNode.Split(structure.rows, rowGaps = structure.rowGaps)
    }

    /**
     * 解析 multline 环境
     */
    private fun parseMultline(envName: String): LatexNode.Multline {
        var position: LatexNode.Multline.Position? = null
        var width: String? = null
        if (envName == "multlined") {
            position = LatexNode.Multline.Position.CENTER
            val option = tokenStream.readOptionalTokens()?.text()?.trim()
            when (option) {
                "t" -> position = LatexNode.Multline.Position.TOP
                "b" -> position = LatexNode.Multline.Position.BOTTOM
                "c", null -> Unit
                else -> width = option.takeIf(ROW_GAP_PATTERN::matches)
            }
            if (option in setOf("t", "c", "b")) {
                width = tokenStream.readOptionalTokens()?.text()?.trim()?.takeIf(ROW_GAP_PATTERN::matches)
            }
        }
        // multline 没有列分隔符，只有行分隔
        val lines = mutableListOf<LatexNode>()
        val rowGaps = mutableListOf<LatexNode.RowGap?>()
        var currentLine = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF() && !tokenStream.peek().isScopeCloser()) {
            if ((tokenStream.peek() as? LatexToken.Command)?.name.let { it == "cr" || it == "newline" }) {
                if (currentLine.isNotEmpty()) {
                    lines.add(LatexNode.Group(context.normalizeStyleDeclarations(currentLine)))
                }
                currentLine = mutableListOf()
                tokenStream.advance()
                rowGaps.add(null)
                continue
            }
            when (tokenStream.peek()) {
                is LatexToken.NewLine -> {
                    if (currentLine.isNotEmpty()) {
                        lines.add(LatexNode.Group(context.normalizeStyleDeclarations(currentLine)))
                    }
                    currentLine = mutableListOf()
                    tokenStream.advance()
                    rowGaps.add(consumeOptionalRowSpacing())
                }

                else -> {
                    val node = context.parseExpression()
                    if (node != null) {
                        currentLine.add(node)
                    }
                }
            }
        }

        if (currentLine.isNotEmpty()) lines.add(LatexNode.Group(context.normalizeStyleDeclarations(currentLine)))
        return LatexNode.Multline(lines, envName = envName, rowGaps = rowGaps, position = position, width = width)
    }

    /**
     * 解析 eqnarray 环境
     */
    private fun parseEqnarray(envName: String): LatexNode.Eqnarray {
        val structure = parseRowColumnStructure()
        return LatexNode.Eqnarray(
            structure.rows,
            envName = envName,
            rowGaps = structure.rowGaps
        )
    }

    /**
     * 解析 subequations 环境
     */
    private fun parseSubequations(): LatexNode.Subequations {
        val content = parseEnvironmentContent()
        return LatexNode.Subequations(content)
    }

    /**
     * 解析 cases / dcases / rcases 环境
     */
    private fun parseCases(style: LatexNode.Cases.CasesStyle): LatexNode.Cases {
        val cases = mutableListOf<Pair<LatexNode, LatexNode>>()
        val rowGaps = mutableListOf<LatexNode.RowGap?>()
        var expression = mutableListOf<LatexNode>()
        var condition = mutableListOf<LatexNode>()
        var isCondition = false

        while (!tokenStream.isEOF() && !tokenStream.peek().isScopeCloser()) {
            if ((tokenStream.peek() as? LatexToken.Command)?.name.let { it == "cr" || it == "newline" }) {
                if (expression.isNotEmpty()) {
                    cases.add(
                        LatexNode.Group(context.normalizeStyleDeclarations(expression)) to
                            LatexNode.Group(context.normalizeStyleDeclarations(condition))
                    )
                }
                expression = mutableListOf()
                condition = mutableListOf()
                isCondition = false
                tokenStream.advance()
                rowGaps.add(null)
                continue
            }
            when (tokenStream.peek()) {
                is LatexToken.Ampersand -> {
                    isCondition = true
                    tokenStream.advance()
                }

                is LatexToken.NewLine -> {
                    if (expression.isNotEmpty()) {
                        cases.add(
                            LatexNode.Group(context.normalizeStyleDeclarations(expression)) to
                                LatexNode.Group(context.normalizeStyleDeclarations(condition))
                        )
                    }
                    expression = mutableListOf()
                    condition = mutableListOf()
                    isCondition = false
                    tokenStream.advance()
                    rowGaps.add(consumeOptionalRowSpacing())
                }

                else -> {
                    val node = context.parseExpression()
                    if (node != null) {
                        if (isCondition) {
                            condition.add(node)
                        } else {
                            expression.add(node)
                        }
                    }
                }
            }
        }

        if (expression.hasVisibleContent() || condition.hasVisibleContent()) {
            cases.add(LatexNode.Group(context.normalizeStyleDeclarations(expression)) to
                LatexNode.Group(context.normalizeStyleDeclarations(condition)))
        }
        return LatexNode.Cases(cases, style, rowGaps = rowGaps)
    }

    // ====================================================================
    // 辅助方法
    // ====================================================================

    private data class ColumnSpec(val columns: String, val tightBoundaries: Set<Int> = emptySet())

    private fun parseAlignmentSpec(): ColumnSpec {
        tokenStream.skipWhitespace()
        if (tokenStream.peek() !is LatexToken.LeftBrace) return ColumnSpec("c")
        val start = tokenStream.currentSourceOffset()
        val tokens = tokenStream.readArgumentTokens()
        val spec = (if (tokens.any { it is LatexToken.Command }) context.expandTokens(tokens) else tokens).text()
        val columns = StringBuilder()
        val tightBoundaries = mutableSetOf<Int>()
        var columnCount = 0
        var offset = 0

        fun group(): String? {
            while (spec.getOrNull(offset)?.isWhitespace() == true) offset++
            if (spec.getOrNull(offset) != '{') return null
            val body = ++offset
            var depth = 1
            while (offset < spec.length) {
                when (spec[offset++]) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) return spec.substring(body, offset - 1)
                }
            }
            return null
        }

        fun read(end: Int, depth: Int): Boolean {
            if (depth > 32) return false
            while (offset < end) {
                if (columns.length >= 1024) return false
                when (val ch = spec[offset++]) {
                    'l', 'c', 'r' -> { columns.append(ch); columnCount++ }
                    '|', ':' -> columns.append(ch)
                    '@' -> {
                        if (group()?.isBlank() != true) return false
                        tightBoundaries.add(columnCount)
                    }
                    '*' -> {
                        val count = group()?.trim()?.toIntOrNull() ?: return false
                        if (count !in 1..1024) return false
                        val body = group() ?: return false
                        val bodyEnd = offset - 1
                        val columnStart = columnCount
                        val outputStart = columns.length
                        val boundariesBefore = tightBoundaries.toSet()
                        offset = bodyEnd - body.length
                        if (!read(bodyEnd, depth + 1)) return false
                        offset = bodyEnd + 1
                        val template = columns.substring(outputStart)
                        if (columns.length + template.length * (count - 1) > 1024) return false
                        val boundaries = tightBoundaries.filter { it !in boundariesBefore || it == columnStart }
                        val step = columnCount - columnStart
                        for (i in 1 until count) {
                            columns.append(template)
                            for (boundary in boundaries) tightBoundaries.add(boundary + step * i)
                        }
                        columnCount += step * (count - 1)
                    }
                    else -> if (!ch.isWhitespace()) return false
                }
            }
            return offset == end
        }

        if (!read(spec.length, 0)) {
            context.diagnostics.add(ParseDiagnostic(tokenStream.rangeFrom(start),
                "Unsupported or invalid array column specification", ParseDiagnostic.Severity.WARNING,
                ParseDiagnostic.Category.INVALID_ARGUMENT))
        }
        return ColumnSpec(columns.toString().ifEmpty { "c" }, tightBoundaries)
    }

    private fun parseOptionalBracketText(): String? {
        if (tokenStream.peek() !is LatexToken.LeftBracket) return null
        tokenStream.advance()
        val result = StringBuilder()
        while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBracket) {
            when (val token = tokenStream.advance()) {
                is LatexToken.Text -> result.append(token.content)
                is LatexToken.Command -> result.append(token.name)
                else -> Unit
            }
        }
        tokenStream.consumeClosing { it is LatexToken.RightBracket }
        return result.toString().trim().ifEmpty { null }
    }

    /**
     * 解析环境内容
     */
    private fun parseEnvironmentContent(): List<LatexNode> {
        val content = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF() && !tokenStream.peek().isScopeCloser()) {
            val node = context.parseExpression()
            if (node != null) {
                content.add(node)
            }
        }

        return context.normalizeStyleDeclarations(content)
    }

    private fun arrayStretch(): Float {
        val macro = context.customCommands["arraystretch"] ?: return 1f
        val value = context.expandTokens(macro.tokens).text().toFloatOrNull()
        if (value != null && value.isFinite() && value > 0) return value
        context.diagnostics.add(com.hrm.latex.parser.ParseDiagnostic(
            tokenStream.peek(-1)?.range ?: SourceRange.EMPTY, "arraystretch must be positive",
            com.hrm.latex.parser.ParseDiagnostic.Severity.ERROR,
            com.hrm.latex.parser.ParseDiagnostic.Category.INVALID_ARGUMENT
        ))
        return 1f
    }

}
