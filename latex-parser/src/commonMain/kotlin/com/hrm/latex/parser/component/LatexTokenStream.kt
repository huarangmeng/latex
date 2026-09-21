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

import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 封装 Token 流的操作，如 peek, advance, expect
 */
class LatexTokenStream(private val initialTokens: List<LatexToken>) {
    private var position = 0
    private var head: LatexToken? = initialTokens.firstOrNull()
    private var pending: ArrayDeque<LatexToken>? = null
    private var consumed: MutableList<LatexToken>? = null
    private var previous: LatexToken? = null
    private var headNormalized = head !is LatexToken.Command && head !is LatexToken.BeginEnvironment
    internal var diagnostics: MutableList<ParseDiagnostic>? = null
    internal var commandParser: CommandParser? = null
    private var readingRaw = false

    internal fun <T> raw(block: () -> T): T {
        val previous = readingRaw
        readingRaw = true
        return try { block() } finally { readingRaw = previous }
    }

    /** Prepend a replacement in O(replacement size), without moving the remaining source. */
    internal fun insert(tokens: List<LatexToken>) {
        if (tokens.isEmpty()) return
        val queue = pending ?: ArrayDeque<LatexToken>().also { pending = it }
        if (consumed == null) consumed = initialTokens.take(position).toMutableList()
        for (index in tokens.indices.reversed()) queue.addFirst(tokens[index])
        head = queue.first()
        headNormalized = head !is LatexToken.Command && head !is LatexToken.BeginEnvironment
    }

    internal fun skipWhitespace() {
        while (peek() is LatexToken.Whitespace) advance()
    }

    /** Read one unexpanded TeX token, splitting the tokenizer's coalesced text. */
    internal fun readAtom(): LatexToken? = raw {
        if (isEOF()) null
        else if (peek() is LatexToken.Text && !(peek() as LatexToken.Text).literal) consumeTextAtom()
        else advance()
    }

    /** Balanced, unexpanded argument. Delimiters are consumed, not returned. */
    internal fun readArgumentTokens(): List<LatexToken> = raw {
        skipWhitespace()
        if (peek() !is LatexToken.LeftBrace) {
            if (isEOF() || peek() is LatexToken.RightBrace) {
                reportMissing("Missing macro argument", ParseDiagnostic.Category.INVALID_ARGUMENT)
                return@raw emptyList()
            }
            return@raw listOfNotNull(readAtom())
        }
        advance()
        val result = mutableListOf<LatexToken>()
        var depth = 1
        while (!isEOF()) {
            val token = advance() ?: break
            if (token is LatexToken.LeftBrace) depth++
            if (token is LatexToken.RightBrace) depth--
            if (depth == 0) break
            appendReplacementToken(result, token)
        }
        if (depth != 0) reportMissing("Missing } in macro argument", ParseDiagnostic.Category.MISSING_BRACE)
        result
    }

    internal fun readOptionalTokens(): List<LatexToken>? = raw {
        skipWhitespace()
        if (peek() !is LatexToken.LeftBracket) return@raw null
        advance()
        val result = mutableListOf<LatexToken>()
        var depth = 0
        var closed = false
        while (!isEOF()) {
            val token = advance() ?: break
            if (token is LatexToken.RightBracket && depth == 0) { closed = true; break }
            if (token is LatexToken.LeftBrace) depth++
            if (token is LatexToken.RightBrace) depth--
            appendReplacementToken(result, token)
        }
        if (!closed) reportMissing("Missing ] in macro argument", ParseDiagnostic.Category.MISSING_BRACKET)
        result
    }

    private fun appendReplacementToken(result: MutableList<LatexToken>, token: LatexToken) {
        if (token !is LatexToken.Text || token.literal || '#' !in token.content) {
            result.add(token)
            return
        }
        var index = 0
        while (index < token.content.length) {
            val length = if (token.content[index].isHighSurrogate() &&
                index + 1 < token.content.length && token.content[index + 1].isLowSurrogate()) 2 else 1
            result.add(LatexToken.Text(token.content.substring(index, index + length), token.range))
            index += length
        }
    }

    private fun reportMissing(message: String, category: ParseDiagnostic.Category) {
        diagnostics?.add(ParseDiagnostic(peek()?.range ?: peek(-1)?.range ?: SourceRange.EMPTY,
            message, ParseDiagnostic.Severity.ERROR, category))
    }

    fun peek(): LatexToken? {
        if (!headNormalized && !readingRaw) normalizeHead()
        return head
    }

    fun peek(offset: Int): LatexToken? {
        if (offset < 0) return lookBehind(offset)
        val current = peek()
        return if (offset == 0) current else rawPeek(offset)
    }

    private fun rawPeek(offset: Int): LatexToken? {
        val queue = pending ?: return initialTokens.getOrNull(position + offset)
        return if (offset < queue.size) queue[offset] else initialTokens.getOrNull(position + offset - queue.size)
    }

    private fun lookBehind(offset: Int): LatexToken? {
        if (offset == -1) return previous
        val history = consumed
        return if (history != null) history.getOrNull(history.size + offset)
            else initialTokens.getOrNull(position + offset)
    }

    private fun normalizeHead() {
        readingRaw = true
        try {
            while ((head is LatexToken.Command || head is LatexToken.BeginEnvironment) && commandParser?.expandNext() == true) { /* expansion updates head */ }
            headNormalized = true
        } finally { readingRaw = false }
    }

    /**
     * 向前查看跳过满足条件的 token，返回第一个不匹配的 token
     * 不改变当前位置
     */
    fun peekSkipping(skip: (LatexToken) -> Boolean): LatexToken? {
        var offset = 0
        while (true) {
            val token = peek(offset) ?: return null
            if (token is LatexToken.EOF) return null
            if (!skip(token)) return token
            offset++
        }
    }

    fun advance(): LatexToken? {
        val token = peek()
        val queue = pending
        if (queue != null && queue.isNotEmpty()) queue.removeFirst() else position++
        head = queue?.firstOrNull() ?: initialTokens.getOrNull(position)
        if (token != null) consumed?.add(token)
        previous = token
        headNormalized = head !is LatexToken.Command && head !is LatexToken.BeginEnvironment
        return token
    }

    /**
     * 消费当前文本 token 的一个 Unicode 字符。
     *
     * TeX 的无花括号参数只消费后面的一个 token；普通文本在分词阶段会合并，
     * 因此解析命令参数或上下标时需要从合并后的文本 token 中仅取出第一个字符。
     */
    internal fun consumeTextAtom(): LatexToken.Text? {
        val token = peek() as? LatexToken.Text ?: return null
        val atomLength = if (
            token.content.length > 1 &&
            token.content[0].isHighSurrogate() &&
            token.content[1].isLowSurrogate()
        ) {
            2
        } else {
            1
        }
        val atomEnd = (token.range.start + atomLength).coerceAtMost(token.range.end)
        val atom = LatexToken.Text(
            token.content.substring(0, atomLength),
            SourceRange(token.range.start, atomEnd)
        )

        advance()
        if (atomLength < token.content.length) {
            insert(listOf(LatexToken.Text(token.content.substring(atomLength), SourceRange(atomEnd, token.range.end))))
        }
        previous = atom
        consumed?.let { if (it.isNotEmpty()) it[it.lastIndex] = atom }
        return atom
    }

    fun isEOF(): Boolean {
        val token = peek()
        return token == null || token is LatexToken.EOF
    }

    fun expect(type: String, message: String? = null): LatexToken {
        val token = peek()
        if (token == null) {
            throw Exception(message ?: "期望 $type，但到达文件末尾")
        }
        advance()
        return token
    }

    /**
     * 获取当前 token 的源码位置起始偏移
     * 用于 Parser 记录节点的 sourceRange.start
     */
    fun currentSourceOffset(): Int {
        return peek()?.range?.start ?: (peek(-1)?.range?.end ?: 0)
    }

    /**
     * 获取上一个已消费 token 的结束偏移
     * 用于 Parser 记录节点的 sourceRange.end
     */
    fun previousEndOffset(): Int = previous?.range?.end ?: 0

    /**
     * 构建从 start 到当前已消费位置的 SourceRange
     */
    fun rangeFrom(startOffset: Int): SourceRange {
        return SourceRange(startOffset, previousEndOffset())
    }
    
    fun reset() {
        position = 0
        pending = null
        consumed = null
        previous = null
        headNormalized = head !is LatexToken.Command && head !is LatexToken.BeginEnvironment
    }
}
