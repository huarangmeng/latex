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
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.component.handler.text
import com.hrm.latex.parser.tokenizer.LatexToken

internal class EnvironmentParser(private val context: LatexParserContext) {
    private val tokenStream get() = context.tokenStream

    companion object {
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
        val result = try {
            when (envName) {
                "matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix",
                "matrix*", "pmatrix*", "bmatrix*", "Bmatrix*", "vmatrix*", "Vmatrix*" ->
                    parseMatrix(envName, isSmall = false)

                "smallmatrix", "smallmatrix*" -> parseMatrix(envName, isSmall = true)
                "array" -> parseArray()
                "subarray" -> parseSubarray()
                "tabular" -> parseTabular()
                "align", "aligned", "align*", "gather", "gathered", "gather*",
                "flalign", "flalign*" -> parseAligned(envName)
                "alignat", "alignat*", "alignedat" -> parseAlignat(envName)
                "split" -> parseSplit()
                "multline", "multline*" -> parseMultline(envName)
                "eqnarray", "eqnarray*" -> parseEqnarray(envName)
                "subequations" -> parseSubequations()
                "cases", "cases*" -> parseCases(envName, LatexNode.Cases.CasesStyle.NORMAL)
                "dcases", "dcases*" -> parseCases(envName, LatexNode.Cases.CasesStyle.DISPLAY)
                "rcases", "rcases*" -> parseCases(envName, LatexNode.Cases.CasesStyle.RIGHT)
                "equation", "equation*", "displaymath" -> {
                    val content = parseEnvironmentContent(envName)
                    LatexNode.Environment(envName, content)
                }

                "RTL" -> {
                    val content = parseEnvironmentContent("RTL")
                    LatexNode.TextDirection(content, LatexNode.TextDirection.Direction.RTL)
                }

                "LTR" -> {
                    val content = parseEnvironmentContent("LTR")
                    LatexNode.TextDirection(content, LatexNode.TextDirection.Direction.LTR)
                }

                else -> {
                    val content = parseEnvironmentContent(envName)
                    LatexNode.Environment(envName, content)
                }
            }
        } finally { context.popScope() }

        // 为没有 sourceRange 的环境节点补充范围
        if (result.sourceRange == null) {
            val range = tokenStream.rangeFrom(startOffset)
            return result.withSourceRange(range)
        }
        return result
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
     * @param envName 环境名（用于匹配 EndEnvironment）
     * @param handleSpecialNodes 是否将 HLine/CLine 作为独立行标记处理（仅 array/tabular 需要）
     */
    private fun parseRowColumnStructure(
        envName: String,
        handleSpecialNodes: Boolean = false
    ): RowColumnStructure {
        val rows = mutableListOf<List<LatexNode>>()
        val rowGaps = mutableListOf<LatexNode.RowGap?>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        context.pushScope()
        try {
            while (!tokenStream.isEOF()) {
                if ((tokenStream.peek() as? LatexToken.Command)?.name == "cr") {
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
                when (val token = tokenStream.peek()) {
                    is LatexToken.EndEnvironment -> {
                        if (token.name == envName) {
                            if (currentCell.hasVisibleContent()) {
                                currentRow.add(LatexNode.Group(context.normalizeStyleDeclarations(currentCell)))
                            }
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow)
                            }
                            tokenStream.advance()
                            break
                        } else {
                            HLog.w(TAG) { "mismatched end environment: expected $envName, got ${token.name}" }
                            tokenStream.advance()
                        }
                    }

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
        val structure = parseRowColumnStructure(envName)
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
        val structure = parseRowColumnStructure("array", handleSpecialNodes = true)
        return LatexNode.Array(structure.rows, alignment, rowGaps = structure.rowGaps, rowStretch = stretch, columnSep = separation)
    }

    private fun parseSubarray(): LatexNode.Array {
        val alignment = parseAlignmentSpec()
        val structure = parseRowColumnStructure("subarray")
        return LatexNode.Array(structure.rows, alignment, rowGaps = structure.rowGaps)
    }

    /**
     * 解析 tabular 环境
     */
    private fun parseTabular(): LatexNode.Tabular {
        val stretch = arrayStretch()
        val separation = context.lengths["arraycolsep"]
        val alignment = parseAlignmentSpec()
        val structure = parseRowColumnStructure("tabular", handleSpecialNodes = true)
        return LatexNode.Tabular(structure.rows, alignment, rowGaps = structure.rowGaps, rowStretch = stretch, columnSep = separation)
    }

    /**
     * 解析对齐环境
     */
    private fun parseAligned(envName: String): LatexNode.Aligned {
        val structure = parseRowColumnStructure(envName)
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
        val structure = parseRowColumnStructure("split")
        return LatexNode.Split(structure.rows, rowGaps = structure.rowGaps)
    }

    /**
     * 解析 multline 环境
     */
    private fun parseMultline(envName: String): LatexNode.Multline {
        // multline 没有列分隔符，只有行分隔
        val lines = mutableListOf<LatexNode>()
        val rowGaps = mutableListOf<LatexNode.RowGap?>()
        var currentLine = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            if ((tokenStream.peek() as? LatexToken.Command)?.name == "cr") {
                if (currentLine.isNotEmpty()) {
                    lines.add(LatexNode.Group(context.normalizeStyleDeclarations(currentLine)))
                }
                currentLine = mutableListOf()
                tokenStream.advance()
                rowGaps.add(null)
                continue
            }
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName) {
                        if (currentLine.isNotEmpty()) {
                            lines.add(LatexNode.Group(context.normalizeStyleDeclarations(currentLine)))
                        }
                        tokenStream.advance()
                        break
                    } else {
                        tokenStream.advance()
                    }
                }

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

        return LatexNode.Multline(lines, envName = envName, rowGaps = rowGaps)
    }

    /**
     * 解析 eqnarray 环境
     */
    private fun parseEqnarray(envName: String): LatexNode.Eqnarray {
        val structure = parseRowColumnStructure(envName)
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
        val content = parseEnvironmentContent("subequations")
        return LatexNode.Subequations(content)
    }

    /**
     * 解析 cases / dcases / rcases 环境
     */
    private fun parseCases(envName: String, style: LatexNode.Cases.CasesStyle): LatexNode.Cases {
        val cases = mutableListOf<Pair<LatexNode, LatexNode>>()
        val rowGaps = mutableListOf<LatexNode.RowGap?>()
        var expression = mutableListOf<LatexNode>()
        var condition = mutableListOf<LatexNode>()
        var isCondition = false

        while (!tokenStream.isEOF()) {
            if ((tokenStream.peek() as? LatexToken.Command)?.name == "cr") {
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
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName) {
                        if (expression.hasVisibleContent() || condition.hasVisibleContent()) {
                            cases.add(
                                LatexNode.Group(context.normalizeStyleDeclarations(expression)) to
                                    LatexNode.Group(context.normalizeStyleDeclarations(condition))
                            )
                        }
                        tokenStream.advance()
                        break
                    } else {
                         tokenStream.advance()
                    }
                }

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

        return LatexNode.Cases(cases, style, rowGaps = rowGaps)
    }

    // ====================================================================
    // 辅助方法
    // ====================================================================

    /**
     * 解析列对齐规格（如 {ccc} 或 {rcl}）
     */
    private fun parseAlignmentSpec(): String {
        if (tokenStream.peek() !is LatexToken.LeftBrace) return "c"

        tokenStream.advance() // consume {
        val alignText = StringBuilder()
        while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBrace) {
            when (val token = tokenStream.peek()) {
                is LatexToken.Text -> alignText.append(token.content)
                else -> break
            }
            tokenStream.advance()
        }
        if (tokenStream.peek() is LatexToken.RightBrace) {
            tokenStream.advance() // consume }
        }
        return alignText.toString()
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
        if (tokenStream.peek() is LatexToken.RightBracket) tokenStream.advance()
        return result.toString().trim().ifEmpty { null }
    }

    /**
     * 解析环境内容
     */
    private fun parseEnvironmentContent(envName: String): List<LatexNode> {
        val content = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            if (tokenStream.peek() is LatexToken.EndEnvironment &&
                (tokenStream.peek() as LatexToken.EndEnvironment).name == envName
            ) {
                tokenStream.advance()
                break
            }

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
