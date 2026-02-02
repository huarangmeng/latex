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

class EnvironmentParser(private val context: LatexParserContext) {
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

        HLog.d(TAG, "解析环境: $envName")

        return when (envName) {
            "matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix" -> parseMatrix(
                envName,
                isSmall = false
            )

            "smallmatrix" -> parseMatrix("matrix", isSmall = true)
            "array" -> parseArray()
            "align", "aligned", "gather", "gathered" -> parseAligned(envName)
            "split" -> parseSplit()
            "multline" -> parseMultline()
            "eqnarray" -> parseEqnarray()
            "subequations" -> parseSubequations()
            "cases" -> parseCases()
            "equation", "displaymath" -> {
                val content = parseEnvironmentContent(envName)
                LatexNode.Environment(envName, content)
            }

            else -> {
                val content = parseEnvironmentContent(envName)
                LatexNode.Environment(envName, content)
            }
        }
    }

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

        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName || (isSmall && token.name == "smallmatrix")) {
                        // 添加最后一个单元格和行
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        tokenStream.advance()
                        break
                    } else {
                        // Mismatched environment end, should ideally throw or handle error, but keeping original logic
                         tokenStream.advance() 
                    }
                }

                is LatexToken.Ampersand -> {
                    // 列分隔符
                    currentRow.add(LatexNode.Group(currentCell))
                    currentCell = mutableListOf()
                    tokenStream.advance()
                }

                is LatexToken.NewLine -> {
                    // 行分隔符
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
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Matrix(rows, matrixType, isSmall)
    }

    /**
     * 解析数组环境（array）
     */
    private fun parseArray(): LatexNode.Array {
        // array 环境需要指定列对齐方式，如 {ccc} 或 {rcl}
        val alignment = if (tokenStream.peek() is LatexToken.LeftBrace) {
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
            alignText.toString()
        } else {
            "c" // 默认居中对齐
        }

        val rows = parseTableStructure("array")
        return LatexNode.Array(rows, alignment)
    }

    /**
     * 解析对齐环境
     */
    private fun parseAligned(envName: String): LatexNode.Aligned {
        // Aligned parsing is slightly different in original code because it didn't check for EndEnvironment name strictly in loop?
        // Original code:
        /*
        while (!isEOF()) {
            when (val token = peek()) {
                is LatexToken.EndEnvironment -> {
                    if (currentCell.isNotEmpty()) ...
                    advance()
                    break
                }
         */
        // It breaks on ANY EndEnvironment. This seems risky if nested? But original code did this.
        // Let's use parseTableStructure but we need to be careful about which Env it closes.
        // Actually the original code for aligned was generic on EndEnvironment.
        
        // Let's replicate original logic carefully.
        
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
                        // mismatched end environment - advance to avoid infinite loop
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
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Aligned(rows)
    }

    /**
     * 解析 split 环境
     * split 用于在单个方程内分割多行,通常在 equation 内使用
     * 语法: x &= a + b \\
     *       &= c
     */
    private fun parseSplit(): LatexNode.Split {
        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == "split") {
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        tokenStream.advance()
                        break
                    } else {
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
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Split(rows)
    }

    /**
     * 解析 multline 环境
     * 用于需要多行显示的单个方程,第一行左对齐,最后一行右对齐,中间行居中
     */
    private fun parseMultline(): LatexNode.Multline {
        val lines = mutableListOf<LatexNode>()
        var currentLine = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == "multline") {
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
     * 用于对齐多个方程,类似 align 但是是旧式语法
     * 通常有三列:左边、关系符、右边
     */
    private fun parseEqnarray(): LatexNode.Eqnarray {
        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == "eqnarray") {
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        tokenStream.advance()
                        break
                    } else {
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
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Eqnarray(rows)
    }

    /**
     * 解析 subequations 环境
     * 用于对一组相关方程进行编号(如 1a, 1b, 1c)
     * 包含其他环境
     */
    private fun parseSubequations(): LatexNode.Subequations {
        val content = parseEnvironmentContent("subequations")
        return LatexNode.Subequations(content)
    }

    /**
     * 解析 cases 环境
     */
    private fun parseCases(): LatexNode.Cases {
        val cases = mutableListOf<Pair<LatexNode, LatexNode>>()
        var expression = mutableListOf<LatexNode>()
        var condition = mutableListOf<LatexNode>()
        var isCondition = false

        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == "cases") {
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

        return LatexNode.Cases(cases)
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
    
    /**
     * 通用表格结构解析器
     */
    private fun parseTableStructure(envName: String): List<List<LatexNode>> {
        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()
        
        while (!tokenStream.isEOF()) {
            when (val token = tokenStream.peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName) {
                        // 保存最后一个单元格
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        // 保存最后一行
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        tokenStream.advance() // 消费 \end{...}
                        break
                    } else {
                        // 不匹配的环境结束标记，继续解析
                        tokenStream.advance()
                    }
                }
                
                is LatexToken.Ampersand -> {
                    // 列分隔符：保存当前单元格，开始新单元格
                    currentRow.add(LatexNode.Group(currentCell))
                    currentCell = mutableListOf()
                    tokenStream.advance()
                }
                
                is LatexToken.NewLine -> {
                    // 行分隔符：保存当前单元格和行，开始新行
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
                    // 解析单元格内容
                    val node = context.parseExpression()
                    if (node != null) {
                        currentCell.add(node)
                    }
                }
            }
        }
        
        return rows
    }
}
