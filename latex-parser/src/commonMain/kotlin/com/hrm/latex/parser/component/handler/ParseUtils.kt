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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * Handler 共享的解析工具函数。
 *
 * 从 CommandParser 中提取的通用辅助方法，避免每个 handler 文件重复实现。
 */
internal object ParseUtils {

    /**
     * 从节点中提取纯文本
     */
    fun extractText(nodes: List<LatexNode>): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is LatexNode.Text -> node.content
                is LatexNode.Group -> extractText(node.children)
                is LatexNode.Space -> " "
                else -> ""
            }
        }
    }

    /**
     * 从节点中提取颜色名称字符串
     */
    fun extractColorName(node: LatexNode): String {
        return when (node) {
            is LatexNode.Text -> node.content
            is LatexNode.Group -> extractText(node.children)
            else -> "black"
        }
    }

    /**
     * 从节点中提取命令名（去除反斜杠）
     */
    fun extractCommandName(node: LatexNode): String {
        return when (node) {
            is LatexNode.Text -> node.content.removePrefix("\\").removeSuffix(" ")
            is LatexNode.Group -> {
                if (node.children.isNotEmpty()) {
                    extractCommandName(node.children[0])
                } else {
                    ""
                }
            }
            is LatexNode.Command -> node.name
            is LatexNode.Symbol -> node.symbol
            else -> ""
        }
    }

    /**
     * 解析上下标内容（单因子或花括号组）
     */
    fun parseScriptContent(ctx: LatexParserContext, stream: LatexTokenStream): LatexNode {
        return when (stream.peek()) {
            is LatexToken.LeftBrace -> ctx.parseGroup()
            else -> ctx.parseFactor() ?: LatexNode.Text("")
        }
    }

    /**
     * 解析后续的 \limits/\nolimits 和上下标
     * 返回 (subscript, superscript, limitsMode) 三元组
     */
    fun parseScriptsAndLimits(
        ctx: LatexParserContext,
        stream: LatexTokenStream
    ): Triple<LatexNode?, LatexNode?, LatexNode.BigOperator.LimitsMode> {
        var subscript: LatexNode? = null
        var superscript: LatexNode? = null
        var limitsMode = LatexNode.BigOperator.LimitsMode.AUTO

        fun shouldContinueAfterWhitespace(token: LatexToken?): Boolean {
            return when {
                token is LatexToken.Command && (token.name == "limits" || token.name == "nolimits") -> true
                token is LatexToken.Subscript -> subscript == null
                token is LatexToken.Superscript -> superscript == null
                else -> false
            }
        }

        while (!stream.isEOF()) {
            val token = stream.peek()
            when (token) {
                is LatexToken.Whitespace -> {
                    val nextToken = stream.peekSkipping {
                        it is LatexToken.Whitespace
                    }
                    if (!shouldContinueAfterWhitespace(nextToken)) {
                        break
                    }
                    while (stream.peek() is LatexToken.Whitespace) {
                        stream.advance()
                    }
                }

                is LatexToken.Command if token.name == "limits" -> {
                    stream.advance()
                    limitsMode = LatexNode.BigOperator.LimitsMode.LIMITS
                }

                is LatexToken.Command if token.name == "nolimits" -> {
                    stream.advance()
                    limitsMode = LatexNode.BigOperator.LimitsMode.NOLIMITS
                }

                is LatexToken.Subscript if subscript == null -> {
                    stream.advance()
                    subscript = parseScriptContent(ctx, stream)
                }

                is LatexToken.Superscript if superscript == null -> {
                    stream.advance()
                    superscript = parseScriptContent(ctx, stream)
                }

                else -> break
            }
        }

        return Triple(subscript, superscript, limitsMode)
    }

    /**
     * 解析直到条件满足的节点列表
     */
    fun parseUntil(ctx: LatexParserContext, stream: LatexTokenStream, condition: (LatexToken) -> Boolean): List<LatexNode> {
        val nodes = mutableListOf<LatexNode>()
        while (!stream.isEOF()) {
            val token = stream.peek()
            if (token != null && condition(token)) {
                break
            }
            val node = ctx.parseExpression()
            if (node != null) {
                nodes.add(node)
            }
        }
        return nodes
    }

    /**
     * 解析脚本组 {_a^b}，返回 (sub, sup)
     */
    fun parseScriptGroup(ctx: LatexParserContext, stream: LatexTokenStream): Pair<LatexNode?, LatexNode?> {
        if (stream.peek() !is LatexToken.LeftBrace) {
            return Pair(null, null)
        }
        stream.advance() // consume '{'

        var sub: LatexNode? = null
        var sup: LatexNode? = null

        while (!stream.isEOF() && stream.peek() !is LatexToken.RightBrace) {
            when (stream.peek()) {
                is LatexToken.Subscript -> {
                    stream.advance()
                    sub = ctx.parseFactor()
                }
                is LatexToken.Superscript -> {
                    stream.advance()
                    sup = ctx.parseFactor()
                }
                is LatexToken.Whitespace -> {
                    stream.advance()
                }
                else -> {
                    stream.advance()
                }
            }
        }

        if (!stream.isEOF()) {
            stream.advance() // consume '}'
        }

        return Pair(sub, sup)
    }

    /**
     * 从花括号 token 流中解析张量指标列表
     */
    fun parseTensorIndicesGroup(ctx: LatexParserContext, stream: LatexTokenStream): List<Pair<Boolean, LatexNode>> {
        if (stream.peek() !is LatexToken.LeftBrace) {
            return emptyList()
        }
        stream.advance() // consume '{'

        val indices = mutableListOf<Pair<Boolean, LatexNode>>()

        while (!stream.isEOF() && stream.peek() !is LatexToken.RightBrace) {
            when (stream.peek()) {
                is LatexToken.Superscript -> {
                    stream.advance()
                    val content = ctx.parseFactor()
                    if (content != null) {
                        indices.add(Pair(true, content))
                    }
                }
                is LatexToken.Subscript -> {
                    stream.advance()
                    val content = ctx.parseFactor()
                    if (content != null) {
                        indices.add(Pair(false, content))
                    }
                }
                is LatexToken.Whitespace -> {
                    stream.advance()
                }
                else -> {
                    stream.advance()
                }
            }
        }

        if (!stream.isEOF()) {
            stream.advance() // consume '}'
        }

        return indices
    }
}
