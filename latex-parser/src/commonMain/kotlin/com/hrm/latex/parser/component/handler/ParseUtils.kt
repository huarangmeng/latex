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

import com.hrm.latex.parser.SymbolMap
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.component.isScopeCloser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * Handler 共享的解析工具函数。
 *
 * 从 CommandParser 中提取的通用辅助方法，避免每个 handler 文件重复实现。
 */
internal object ParseUtils {

    private val dimensionPattern = Regex(
        """[+-]?\s*(?:\d+(?:\.\d*)?|\.\d+)\s*(?:em|ex|mu|pt|px|mm|cm|in|bp|pc|dd|cc|sp)""",
        RegexOption.IGNORE_CASE
    )

    fun isDimension(value: String): Boolean = dimensionPattern.matches(value)

    /**
     * 从节点中提取纯文本
     */
    fun extractText(nodes: List<LatexNode>): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is LatexNode.Text -> node.content
                is LatexNode.TextMode -> if (node.content.isEmpty()) node.text else extractText(node.content)
                is LatexNode.TextAccent -> {
                    val text = extractText(listOf(node.content))
                    if (text.isEmpty()) "" else text.take(1).replace('ı', 'i').replace('ȷ', 'j') +
                        node.accent.combining + text.drop(1)
                }
                is LatexNode.Group -> extractText(node.children)
                is LatexNode.Style -> extractText(node.content)
                is LatexNode.Space -> " "
                // Reconstruct command arguments (dimensions, ranges, keys) using source punctuation.
                is LatexNode.Symbol -> when (node.symbol) {
                    "prime" -> "'"
                    "minus" -> "-"
                    else -> node.unicode
                }
                is LatexNode.Superscript -> {
                    val primes = node.exponent.primeCount()
                    if (primes == null) "" else extractText(listOf(node.base)) + "'".repeat(primes)
                }
                else -> ""
            }
        }
    }

    private fun LatexNode.primeCount(): Int? =
        (this as? LatexNode.Group)
            ?.children
            ?.takeIf { children ->
                children.isNotEmpty() &&
                    children.all { child -> child is LatexNode.Symbol && child.symbol == "prime" }
            }?.size

    /**
     * 从节点中提取颜色名称字符串
     */
    fun extractColorName(node: LatexNode): String = extractArgumentText(node) ?: "black"

    /** Text of a plain-text command argument: one character, a braced group or a `\verb` atom. */
    fun extractArgumentText(node: LatexNode): String? = when (node) {
        is LatexNode.Text -> node.content
        is LatexNode.Group -> extractText(node.children)
        is LatexNode.TextMode -> extractText(listOf(node))
        else -> null
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
     * Extracts a delimiter character from a parsed argument.
     * Both literal delimiters and command forms such as \langle/\rVert are accepted.
     */
    fun extractDelimiter(node: LatexNode): String {
        val raw = when (node) {
            is LatexNode.Group -> node.children.singleOrNull()?.let(::extractDelimiter)
                ?: extractText(node.children)
            is LatexNode.Command -> node.name
            is LatexNode.Symbol -> node.unicode
            is LatexNode.Text -> node.content
            else -> extractText(listOf(node))
        }.trim()
        return delimiterFromName(raw)
    }

    fun delimiterFromName(raw: String): String = when (SymbolMap.canonicalName(raw.removePrefix("\\"))) {
        "." -> ""
        "langle", "lt" -> "⟨"
        "rangle", "gt" -> "⟩"
        "lparen" -> "("
        "rparen" -> ")"
        "lBrace" -> "⦃"
        "rBrace" -> "⦄"
        "ulcorner" -> "⌜"
        "urcorner" -> "⌝"
        "llcorner" -> "⌞"
        "lrcorner" -> "⌟"
        "lfloor" -> "⌊"
        "rfloor" -> "⌋"
        "lceil" -> "⌈"
        "rceil" -> "⌉"
        "llbracket" -> "⟦"
        "lgroup" -> "⟮"
        "rrbracket" -> "⟧"
        "rgroup" -> "⟯"
        "lmoustache" -> "⎰"
        "rmoustache" -> "⎱"
        "lvert", "rvert", "vert" -> "|"
        "lVert", "rVert", "Vert" -> "‖"
        "|" -> "|"
        "lbrace", "{" -> "{"
        "rbrace", "}" -> "}"
        else -> raw.removePrefix("\\")
    }

    /**
     * 读取花括号或裸尺寸。裸尺寸允许符号、数字和单位之间有空白，
     * 单位后的源码属于下一项，即使它与单位共用同一个文本 token。
     */
    fun parseDimension(ctx: LatexParserContext, stream: LatexTokenStream): String {
        stream.skipWhitespace()
        if (stream.peek() is LatexToken.LeftBrace) {
            val arg = ctx.parseArgument() ?: return "0pt"
            val raw = extractText(listOf(arg)).trim()
            return if (isDimension(raw)) raw.filterNot { it.isWhitespace() } else raw.ifEmpty { "0pt" }
        }

        val raw = buildString {
            val sign = stream.peek() as? LatexToken.Text
            if (sign?.content == "+" || sign?.content == "-") {
                append(sign.content)
                stream.advance()
                stream.skipWhitespace()
            }
            var hasDot = false
            while (true) {
                val token = stream.peek() as? LatexToken.Text ?: break
                if (token.literal) break
                var length = 0
                for (char in token.content) {
                    if (char == '.' && !hasDot) hasDot = true
                    else if (char !in '0'..'9') break
                    length++
                }
                if (length == 0) break
                append(stream.consumeTextPrefix(token, length).content)
                if (length < token.content.length) break
            }
            if (any { it in '0'..'9' }) {
                stream.skipWhitespace()
                val unitToken = stream.peek() as? LatexToken.Text
                val unit = unitToken?.content?.take(2).orEmpty()
                if (isDimension(toString() + unit) && unitToken != null) {
                    append(stream.consumeTextPrefix(unitToken, unit.length).content)
                }
            }
        }
        return raw.ifEmpty { "0pt" }
    }

    /**
     * 解析上下标内容（单因子或花括号组）
     */
    fun parseScriptContent(ctx: LatexParserContext): LatexNode =
        ctx.parseArgument() ?: LatexNode.Text("")

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

        while (!stream.isEOF()) {
            val token = stream.peek()
            when (token) {
                is LatexToken.Whitespace -> {
                    val nextToken = stream.peekSkipping {
                        it is LatexToken.Whitespace
                    }
                    val shouldContinue = when {
                        nextToken is LatexToken.Command && (nextToken.name == "limits" || nextToken.name == "nolimits") -> true
                        nextToken is LatexToken.Subscript && subscript == null -> true
                        nextToken is LatexToken.Superscript && superscript == null -> true
                        else -> false
                    }
                    if (!shouldContinue) {
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
                    subscript = parseScriptContent(ctx)
                }

                is LatexToken.Superscript if superscript == null -> {
                    stream.advance()
                    superscript = parseScriptContent(ctx)
                }

                else -> break
            }
        }

        return Triple(subscript, superscript, limitsMode)
    }

    /** 方括号只在调用方需要可选参数时打开结构，普通数学列表的方括号仍是符号。 */
    fun parseBracketedArgument(ctx: LatexParserContext, stream: LatexTokenStream): List<LatexNode>? {
        val opening = stream.openOptionalArgument() ?: return null
        val nodes = mutableListOf<LatexNode>()
        while (!stream.isEOF() && stream.peek().let { it !is LatexToken.RightBracket && !it.isScopeCloser() }) {
            ctx.parseExpression()?.let(nodes::add)
        }
        if (!stream.consumeClosing { it is LatexToken.RightBracket }) {
            ctx.diagnostics.add(ParseDiagnostic(
                opening.range, "Missing ] in optional argument",
                ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.MISSING_BRACKET
            ))
        }
        return nodes
    }

    /**
     * 解析脚本组 {_a^b}，返回 (sub, sup)
     */
    fun parseScriptGroup(ctx: LatexParserContext, stream: LatexTokenStream): Pair<LatexNode?, LatexNode?> {
        var sub: LatexNode? = null
        var sup: LatexNode? = null
        readScriptGroup(ctx, stream) { superscript, content ->
            if (superscript) sup = content else sub = content
        }
        return Pair(sub, sup)
    }

    /**
     * 从花括号 token 流中解析张量指标列表
     */
    fun parseTensorIndicesGroup(ctx: LatexParserContext, stream: LatexTokenStream): List<Pair<Boolean, LatexNode>> {
        val indices = mutableListOf<Pair<Boolean, LatexNode>>()
        readScriptGroup(ctx, stream) { superscript, content ->
            if (content != null) indices.add(superscript to content)
        }
        return indices
    }

    /** 指标组只读取上下标项，遇到结束标记时交回拥有该标记的解析器。 */
    private inline fun readScriptGroup(
        ctx: LatexParserContext,
        stream: LatexTokenStream,
        accept: (superscript: Boolean, content: LatexNode?) -> Unit,
    ) {
        if (stream.peek() !is LatexToken.LeftBrace) return
        stream.advance()
        while (!stream.isEOF() && !stream.peek().isScopeCloser()) {
            val marker = stream.advance()
            if (marker is LatexToken.Superscript || marker is LatexToken.Subscript) {
                val content = if (stream.peek().isScopeCloser()) null else ctx.parseFactor()
                accept(marker is LatexToken.Superscript, content)
            }
        }
        stream.consumeClosing { it is LatexToken.RightBrace }
    }
}
