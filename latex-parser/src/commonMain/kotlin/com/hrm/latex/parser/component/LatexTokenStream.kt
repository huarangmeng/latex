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
import com.hrm.latex.parser.tokenizer.inheritExpansion
import com.hrm.latex.parser.util.codePointWidthAt

/** 内容读到外层作用域的结束标记时停止，由拥有该标记的解析器消费。 */
internal fun LatexToken?.isScopeCloser(): Boolean =
    this is LatexToken.RightBrace || this is LatexToken.EndEnvironment ||
        this is LatexToken.Command && (name == "endgroup" || name == "egroup")

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

    /** 增量解析记录实际前瞻过的最远源码 token，EOF 也可能影响解析决定。 */
    internal var trackLookahead = false
    internal var observedThrough = -1
        private set
    internal var hasIncompleteStructure = false
    internal val sourceIndex get() = position
    internal val hasPendingTokens get() = pending?.isNotEmpty() == true
    internal val previousToken get() = previous

    internal fun resumeAt(index: Int, previousToken: LatexToken?, lookahead: Int) {
        position = index
        head = initialTokens.getOrNull(index)
        previous = previousToken
        observedThrough = lookahead
        invalidateExpansion()
    }

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
        invalidateExpansion()
    }

    /** Split the current token without moving the parser's previous source position. */
    internal fun replaceHead(tokens: List<LatexToken>) {
        if (consumed == null) consumed = initialTokens.take(position).toMutableList()
        val queue = pending
        if (queue != null && queue.isNotEmpty()) queue.removeFirst() else position++
        insert(tokens)
    }

    internal fun skipWhitespace() {
        while (peek() is LatexToken.Whitespace) advance()
    }

    /** 语法解析器已经消费起始标记时，统一确认结束标记；不匹配的 token 留给外层处理。 */
    internal inline fun consumeClosing(matches: (LatexToken?) -> Boolean): Boolean {
        if (matches(peek())) {
            advance()
            return true
        }
        hasIncompleteStructure = true
        return false
    }

    /** 控制词后的源码空白仅用于终止名称，显式间距仍留在 token 流中。 */
    internal fun skipControlWordWhitespace() = raw {
        while ((peek() as? LatexToken.Whitespace)?.content == " ") advance()
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
        if (depth != 0) {
            hasIncompleteStructure = true
            reportMissing("Missing } in macro argument", ParseDiagnostic.Category.MISSING_BRACE)
        }
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
        if (!closed) {
            hasIncompleteStructure = true
            reportMissing("Missing ] in macro argument", ParseDiagnostic.Category.MISSING_BRACKET)
        }
        result
    }

    private fun appendReplacementToken(result: MutableList<LatexToken>, token: LatexToken) {
        if (token !is LatexToken.Text || token.literal || '#' !in token.content) {
            result.add(token)
            return
        }
        var index = 0
        while (index < token.content.length) {
            val length = token.content.codePointWidthAt(index)
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
        if (trackLookahead) observedThrough = maxOf(observedThrough, position.coerceAtMost(initialTokens.lastIndex))
        return head
    }

    fun peek(offset: Int): LatexToken? {
        if (offset < 0) return lookBehind(offset)
        val current = peek()
        if (offset == 0) return current
        if (trackLookahead) observedThrough = maxOf(
            observedThrough, (position + offset).coerceAtMost(initialTokens.lastIndex)
        )
        return rawPeek(offset)
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

    internal fun invalidateExpansion() {
        val token = head
        headNormalized = token !is LatexToken.Command && token !is LatexToken.BeginEnvironment &&
            (token !is LatexToken.Text && token !is LatexToken.Whitespace || commandParser?.hasMathCodes != true)
    }

    private fun normalizeHead() {
        readingRaw = true
        try {
            while (commandParser?.expandNext() == true) { /* expansion updates head */ }
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
        // 流耗尽后的空读不消费源码，也不能抹掉最后一个已消费 token 的位置。
        val token = peek() ?: return null
        val queue = pending
        if (queue != null && queue.isNotEmpty()) queue.removeFirst() else position++
        head = queue?.firstOrNull() ?: initialTokens.getOrNull(position)
        consumed?.add(token)
        previous = token
        invalidateExpansion()
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
        return consumeTextPrefix(token, token.content.codePointWidthAt(0))
    }

    internal fun consumeTextPrefix(token: LatexToken.Text, length: Int): LatexToken.Text {
        val atomEnd = (token.range.start + length).coerceAtMost(token.range.end)
        val atom = LatexToken.Text(
            token.content.substring(0, length),
            SourceRange(token.range.start, atomEnd)
        ).inheritExpansion(token)

        advance()
        if (length < token.content.length) {
            insert(listOf(token.copy(content = token.content.substring(length), range = SourceRange(atomEnd, token.range.end)).inheritExpansion(token)))
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
        pending = null
        consumed = null
        hasIncompleteStructure = false
        resumeAt(0, null, -1)
    }
}
