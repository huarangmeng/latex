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


package com.hrm.latex.parser

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.component.EnvironmentParser
import com.hrm.latex.parser.component.ChemicalParser
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * LaTeX 语法解析器
 *
 * Refactored to use component-based architecture:
 * - LatexTokenStream: Token management
 * - EnvironmentParser: Environment logic
 * - CommandParser: Command logic
 */
class LatexParser : LatexParserContext {

    override lateinit var tokenStream: LatexTokenStream
    override val customCommands: MutableMap<String, com.hrm.latex.parser.component.CustomCommand> = mutableMapOf()
    private lateinit var environmentParser: EnvironmentParser
    private lateinit var commandParser: CommandParser

    companion object {
        private const val TAG = "LatexParser"
    }

    /**
     * 解析 LaTeX 字符串
     * @throws ParseException 解析失败时抛出异常
     */
    fun parse(input: String): LatexNode.Document {
        HLog.d(TAG, "开始解析 LaTeX: $input")

        // 词法分析
        val tokenizer = LatexTokenizer(input)
        val tokens = tokenizer.tokenize()

        // 初始化组件
        tokenStream = LatexTokenStream(tokens)
        environmentParser = EnvironmentParser(this)
        val chemicalParser = ChemicalParser(this)
        commandParser = CommandParser(this, chemicalParser)

        // 语法分析
        val children = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF()) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        val document = LatexNode.Document(
            children,
            sourceRange = SourceRange(0, input.length)
        )
        HLog.d(TAG, "解析成功，生成 ${children.size} 个节点")
        return document
    }

    /**
     * 解析表达式（处理上标下标）
     */
    override fun parseExpression(): LatexNode? {
        val startOffset = tokenStream.currentSourceOffset()
        var node = parseFactor() ?: return null

        while (true) {
            val token = tokenStream.peek()
            if (token is LatexToken.Superscript) {
                tokenStream.advance()
                val exponent = parseScriptContent()
                node = LatexNode.Superscript(
                    node, exponent,
                    sourceRange = tokenStream.rangeFrom(startOffset)
                )
            } else if (token is LatexToken.Subscript) {
                tokenStream.advance()
                val index = parseScriptContent()
                node = LatexNode.Subscript(
                    node, index,
                    sourceRange = tokenStream.rangeFrom(startOffset)
                )
            } else {
                break
            }
        }
        return node
    }

    /**
     * 解析基本因子（不含上标下标）
     */
    override fun parseFactor(): LatexNode? {
        when (val token = tokenStream.peek()) {
            is LatexToken.Text -> {
                tokenStream.advance()
                return LatexNode.Text(token.content, sourceRange = token.range)
            }

            is LatexToken.Command -> {
                val cmdStart = token.range.start
                tokenStream.advance() // Consume command token
                val result = commandParser.parseCommand(token.name)
                // CommandParser 内部已填充 sourceRange，但对于简单命令可能未填充
                // 如果 result 没有 sourceRange，用命令开始到当前位置补充
                return if (result?.sourceRange == null && result != null) {
                    setSourceRange(result, tokenStream.rangeFrom(cmdStart))
                } else {
                    result
                }
            }

            is LatexToken.BeginEnvironment -> {
                return environmentParser.parseEnvironment()
            }

            is LatexToken.LeftBrace -> {
                return parseGroup()
            }

            is LatexToken.Superscript, is LatexToken.Subscript -> {
                tokenStream.advance()
                return null
            }

            is LatexToken.Whitespace -> {
                tokenStream.advance()
                return LatexNode.Space(LatexNode.Space.SpaceType.NORMAL, sourceRange = token.range)
            }

            is LatexToken.NewLine -> {
                tokenStream.advance()
                return LatexNode.NewLine(sourceRange = token.range)
            }

            is LatexToken.LeftBracket -> {
                tokenStream.advance()
                return LatexNode.Text("[", sourceRange = token.range)
            }

            is LatexToken.RightBracket -> {
                tokenStream.advance()
                return LatexNode.Text("]", sourceRange = token.range)
            }

            is LatexToken.Ampersand -> {
                tokenStream.advance()
                return LatexNode.Text("&", sourceRange = token.range)
            }

            is LatexToken.EOF -> return null
            else -> {
                tokenStream.advance()
                return null
            }
        }
    }

    /**
     * 解析分组 {...}
     */
    override fun parseGroup(): LatexNode.Group {
        val startOffset = tokenStream.currentSourceOffset()
        tokenStream.expect("{")
        val children = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBrace) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        if (!tokenStream.isEOF()) {
            tokenStream.expect("}")
        }
        return LatexNode.Group(children, sourceRange = tokenStream.rangeFrom(startOffset))
    }

    /**
     * 解析命令参数 {...}
     */
    override fun parseArgument(): LatexNode? {
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> {
                parseExpression()
            }
        }
    }

    /**
     * 解析上下标内容
     */
    private fun parseScriptContent(): LatexNode {
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> parseExpression() ?: LatexNode.Text("")
        }
    }

    /**
     * 为没有 sourceRange 的节点设置 sourceRange
     * 通过 copy() 方式创建新实例
     */
    private fun setSourceRange(node: LatexNode, range: SourceRange): LatexNode {
        return when (node) {
            is LatexNode.Text -> node.copy(sourceRange = range)
            is LatexNode.Command -> node.copy(sourceRange = range)
            is LatexNode.Symbol -> node.copy(sourceRange = range)
            is LatexNode.Operator -> node.copy(sourceRange = range)
            is LatexNode.Space -> node.copy(sourceRange = range)
            is LatexNode.HSpace -> node.copy(sourceRange = range)
            is LatexNode.Fraction -> node.copy(sourceRange = range)
            is LatexNode.Root -> node.copy(sourceRange = range)
            is LatexNode.Superscript -> node.copy(sourceRange = range)
            is LatexNode.Subscript -> node.copy(sourceRange = range)
            is LatexNode.Group -> node.copy(sourceRange = range)
            is LatexNode.Style -> node.copy(sourceRange = range)
            is LatexNode.MathStyle -> node.copy(sourceRange = range)
            is LatexNode.Accent -> node.copy(sourceRange = range)
            is LatexNode.BigOperator -> node.copy(sourceRange = range)
            is LatexNode.Delimited -> node.copy(sourceRange = range)
            is LatexNode.ManualSizedDelimiter -> node.copy(sourceRange = range)
            is LatexNode.ExtensibleArrow -> node.copy(sourceRange = range)
            is LatexNode.Stack -> node.copy(sourceRange = range)
            is LatexNode.Color -> node.copy(sourceRange = range)
            is LatexNode.Binomial -> node.copy(sourceRange = range)
            is LatexNode.TextMode -> node.copy(sourceRange = range)
            is LatexNode.Boxed -> node.copy(sourceRange = range)
            is LatexNode.Phantom -> node.copy(sourceRange = range)
            is LatexNode.Negation -> node.copy(sourceRange = range)
            is LatexNode.Tag -> node.copy(sourceRange = range)
            is LatexNode.Substack -> node.copy(sourceRange = range)
            is LatexNode.Smash -> node.copy(sourceRange = range)
            is LatexNode.VPhantom -> node.copy(sourceRange = range)
            is LatexNode.HPhantom -> node.copy(sourceRange = range)
            is LatexNode.Label -> node.copy(sourceRange = range)
            is LatexNode.Ref -> node.copy(sourceRange = range)
            is LatexNode.EqRef -> node.copy(sourceRange = range)
            is LatexNode.SideSet -> node.copy(sourceRange = range)
            is LatexNode.Tensor -> node.copy(sourceRange = range)
            is LatexNode.NewCommand -> node.copy(sourceRange = range)
            is LatexNode.Document -> node.copy(sourceRange = range)
            is LatexNode.Environment -> node.copy(sourceRange = range)
            is LatexNode.Matrix -> node.copy(sourceRange = range)
            is LatexNode.Array -> node.copy(sourceRange = range)
            is LatexNode.Aligned -> node.copy(sourceRange = range)
            is LatexNode.Cases -> node.copy(sourceRange = range)
            is LatexNode.Split -> node.copy(sourceRange = range)
            is LatexNode.Multline -> node.copy(sourceRange = range)
            is LatexNode.Eqnarray -> node.copy(sourceRange = range)
            is LatexNode.Subequations -> node.copy(sourceRange = range)
            is LatexNode.Tabular -> node.copy(sourceRange = range)
            is LatexNode.NewLine -> node.copy(sourceRange = range)
        }
    }

    // Internal exception class mostly for backward compatibility or internal usage
    class ParseException(message: String) : Exception(message)
}
