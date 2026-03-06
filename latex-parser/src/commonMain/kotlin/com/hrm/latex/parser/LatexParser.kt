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
import com.hrm.latex.parser.component.ChemicalParser
import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.component.CustomCommand
import com.hrm.latex.parser.component.EnvironmentParser
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * LaTeX 语法解析器
 *
 * 组件化架构:
 * - [LatexTokenStream]: Token 流管理
 * - [EnvironmentParser]: 环境解析
 * - [CommandParser]: 命令解析
 */
class LatexParser {

    companion object {
        private const val TAG = "LatexParser"
    }

    /**
     * 解析 LaTeX 字符串
     */
    fun parse(input: String): LatexNode.Document {
        HLog.d(TAG) { "开始解析 LaTeX: $input" }
        val tokens = LatexTokenizer(input).tokenize()
        return ParseSession(tokens, input.length).parse()
    }

    /**
     * 解析 LaTeX 字符串，返回包含 AST 和结构化诊断信息的 ParseResult
     *
     * 相比 [parse] 方法，此方法额外返回解析过程中收集的所有诊断信息，
     * 支持诊断面板 API、错误过滤和分类查询。
     */
    fun parseWithDiagnostics(input: String): ParseResult {
        HLog.d(TAG) { "开始解析 LaTeX (带诊断): $input" }
        val tokens = LatexTokenizer(input).tokenize()
        val session = ParseSession(tokens, input.length)
        val document = session.parse()
        return ParseResult(document, session.diagnostics.toList())
    }

    /**
     * 从 token 列表直接解析（供增量解析器使用，避免二次分词）
     */
    fun parse(tokens: List<LatexToken>, inputLength: Int): LatexNode.Document {
        HLog.d(TAG) { "从 token 列表解析, token 数: ${tokens.size}" }
        return ParseSession(tokens, inputLength).parse()
    }

    class ParseException(message: String) : Exception(message)
}

/**
 * 解析会话：封装单次解析的所有可变状态。
 *
 * 每次 [LatexParser.parse] 创建一个新的 ParseSession，
 * 解析完成后即被丢弃，不存在跨调用的状态污染。
 */
internal class ParseSession(
    tokens: List<LatexToken>,
    private val inputLength: Int
) : LatexParserContext {

    companion object {
        private const val TAG = "ParseSession"
    }

    override val tokenStream = LatexTokenStream(tokens)
    override val customCommands: MutableMap<String, CustomCommand> = mutableMapOf()
    override val diagnostics: MutableList<ParseDiagnostic> = mutableListOf()

    private val environmentParser = EnvironmentParser(this)
    private val chemicalParser = ChemicalParser(this)
    private val commandParser = CommandParser(this, chemicalParser)

    fun parse(): LatexNode.Document {
        val children = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF()) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        val document = LatexNode.Document(
            children,
            sourceRange = SourceRange(0, inputLength)
        )
        HLog.d(TAG) { "解析成功，生成 ${children.size} 个节点, 诊断: ${diagnostics.size} 条" }
        return document
    }

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

    override fun parseFactor(): LatexNode? {
        when (val token = tokenStream.peek()) {
            is LatexToken.Text -> {
                tokenStream.advance()
                return LatexNode.Text(token.content, sourceRange = token.range)
            }

            is LatexToken.Command -> {
                val cmdStart = token.range.start
                tokenStream.advance()
                val result = commandParser.parseCommand(token.name)
                return if (result?.sourceRange == null && result != null) {
                    result.withSourceRange(tokenStream.rangeFrom(cmdStart))
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

            is LatexToken.MathShift -> {
                return parseMathMode(token)
            }

            is LatexToken.EOF -> return null
            else -> {
            // P1: 记录诊断而非静默丢弃
                val range = token?.range
                if (range != null) {
                    diagnostics.add(
                        ParseDiagnostic(
                            range = range,
                            message = "Unexpected token: $token",
                            severity = ParseDiagnostic.Severity.WARNING,
                            category = ParseDiagnostic.Category.UNEXPECTED_TOKEN
                        )
                    )
                }
                tokenStream.advance()
                return null
            }
        }
    }

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

    override fun parseArgument(): LatexNode? {
        while (tokenStream.peek() is LatexToken.Whitespace) {
            tokenStream.advance()
        }
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> parseFactor()
        }
    }

    private fun parseMathMode(openToken: LatexToken.MathShift): LatexNode {
        val startOffset = openToken.range.start
        val count = openToken.count
        tokenStream.advance()

        val children = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF()) {
            val next = tokenStream.peek()
            if (next is LatexToken.MathShift && next.count == count) {
                tokenStream.advance()
                break
            }
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        val range = tokenStream.rangeFrom(startOffset)
        return if (count == 2) {
            LatexNode.DisplayMath(children, sourceRange = range)
        } else {
            LatexNode.InlineMath(children, sourceRange = range)
        }
    }

    private fun parseScriptContent(): LatexNode {
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> parseFactor() ?: LatexNode.Text("")
        }
    }
}
