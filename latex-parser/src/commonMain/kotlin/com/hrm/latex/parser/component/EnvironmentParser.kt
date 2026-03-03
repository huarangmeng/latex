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
import com.hrm.latex.parser.tokenizer.LatexToken

internal class EnvironmentParser(private val context: LatexParserContext) {
    private val tokenStream get() = context.tokenStream

    companion object {
        private const val TAG = "EnvironmentParser"
    }

    /**
     * 解析环境
     */
    fun parseEnvironment(): LatexNode? {
        val beginToken = tokenStream.advance() as? LatexToken.BeginEnvironment ?: return null
        val envName = beginToken.name
        val startOffset = beginToken.range.start

        HLog.d(TAG, "解析环境: $envName")

        val result = when (envName) {
            "matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix" -> parseMatrix(
                envName,
                isSmall = false
            )

            "smallmatrix" -> parseMatrix("matrix", isSmall = true)
            "array" -> parseArray()
            "tabular" -> parseTabular()
            "align", "aligned", "align*", "gather", "gathered", "gather*",
            "flalign", "flalign*" -> parseAligned(envName)
            "alignat", "alignat*" -> parseAlignat(envName)
            "split" -> parseSplit()
            "multline", "multline*" -> parseMultline(envName)
            "eqnarray", "eqnarray*" -> parseEqnarray(envName)
            "subequations" -> parseSubequations()
            "cases" -> parseCases(envName, LatexNode.Cases.CasesStyle.NORMAL)
            "dcases" -> parseCases(envName, LatexNode.Cases.CasesStyle.DISPLAY)
            "rcases" -> parseCases(envName, LatexNode.Cases.CasesStyle.RIGHT)
            "equation", "equation*", "displaymath" -> {
                val content = parseEnvironmentContent(envName)
                LatexNode.Environment(envName, content)
            }

            else -> {
                val content = parseEnvironmentContent(envName)
                LatexNode.Environment(envName, content)
            }
        }

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
    ): List<List<LatexNode>> {
        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName) {
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        tokenStream.advance()
                        break
                    } else {
                        HLog.w(TAG, "mismatched end environment: expected $envName, got ${token.name}")
                        tokenStream.advance()
                    }
                }

                is LatexToken.Ampersand -> {
                    currentRow.add(LatexNode.Group(currentCell))
                    currentCell = mutableListOf()
                    tokenStream.advance()
                }

                is LatexToken.NewLine -> {
                    if (currentCell.isNotEmpty()) {
                        currentRow.add(LatexNode.Group(currentCell))
                    }
                    if (currentRow.isNotEmpty()) {
                        rows.add(currentRow)
                    }
                    currentRow = mutableListOf()
                    currentCell = mutableListOf()
                    tokenStream.advance()
                }

                else -> {
                    val node = context.parseExpression()
                    if (node != null) {
                        if (handleSpecialNodes && (node is LatexNode.HLine || node is LatexNode.CLine)) {
                            // 先保存当前累积的内容
                            if (currentCell.isNotEmpty()) {
                                currentRow.add(LatexNode.Group(currentCell))
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

        return rows
    }

    // ====================================================================
    // 各环境解析方法（委托 parseRowColumnStructure）
    // ====================================================================

    /**
     * 解析矩阵
     */
    private fun parseMatrix(envName: String, isSmall: Boolean = false): LatexNode.Matrix {
        val matrixType = when (envName) {
            "pmatrix" -> LatexNode.Matrix.MatrixType.PAREN
            "bmatrix" -> LatexNode.Matrix.MatrixType.BRACKET
            "Bmatrix" -> LatexNode.Matrix.MatrixType.BRACE
            "vmatrix" -> LatexNode.Matrix.MatrixType.VBAR
            "Vmatrix" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
            else -> LatexNode.Matrix.MatrixType.PLAIN
        }

        val actualEnvName = if (isSmall) "smallmatrix" else envName
        val rows = parseRowColumnStructure(actualEnvName)
        return LatexNode.Matrix(rows, matrixType, isSmall)
    }

    /**
     * 解析数组环境（array）
     */
    private fun parseArray(): LatexNode.Array {
        val alignment = parseAlignmentSpec()
        val rows = parseRowColumnStructure("array", handleSpecialNodes = true)
        return LatexNode.Array(rows, alignment)
    }

    /**
     * 解析 tabular 环境
     */
    private fun parseTabular(): LatexNode.Tabular {
        val alignment = parseAlignmentSpec()
        val rows = parseRowColumnStructure("tabular", handleSpecialNodes = true)
        return LatexNode.Tabular(rows, alignment)
    }

    /**
     * 解析对齐环境
     */
    private fun parseAligned(envName: String): LatexNode.Aligned {
        val rows = parseRowColumnStructure(envName)
        return LatexNode.Aligned(rows)
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
        val rows = parseRowColumnStructure("split")
        return LatexNode.Split(rows)
    }

    /**
     * 解析 multline 环境
     */
    private fun parseMultline(envName: String): LatexNode.Multline {
        // multline 没有列分隔符，只有行分隔
        val lines = mutableListOf<LatexNode>()
        var currentLine = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName) {
                        if (currentLine.isNotEmpty()) {
                            lines.add(LatexNode.Group(currentLine))
                        }
                        tokenStream.advance()
                        break
                    } else {
                        tokenStream.advance()
                    }
                }

                is LatexToken.NewLine -> {
                    if (currentLine.isNotEmpty()) {
                        lines.add(LatexNode.Group(currentLine))
                    }
                    currentLine = mutableListOf()
                    tokenStream.advance()
                }

                else -> {
                    val node = context.parseExpression()
                    if (node != null) {
                        currentLine.add(node)
                    }
                }
            }
        }

        return LatexNode.Multline(lines)
    }

    /**
     * 解析 eqnarray 环境
     */
    private fun parseEqnarray(envName: String): LatexNode.Eqnarray {
        val rows = parseRowColumnStructure(envName)
        return LatexNode.Eqnarray(rows)
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
        var expression = mutableListOf<LatexNode>()
        var condition = mutableListOf<LatexNode>()
        var isCondition = false

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName) {
                        if (expression.isNotEmpty()) {
                            cases.add(
                                LatexNode.Group(expression) to LatexNode.Group(condition)
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
                            LatexNode.Group(expression) to LatexNode.Group(condition)
                        )
                    }
                    expression = mutableListOf()
                    condition = mutableListOf()
                    isCondition = false
                    tokenStream.advance()
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

        return LatexNode.Cases(cases, style)
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

        return content
    }
}
