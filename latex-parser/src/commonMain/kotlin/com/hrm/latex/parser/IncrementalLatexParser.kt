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
import com.hrm.latex.parser.incremental.IncrementalTokenizer
import com.hrm.latex.parser.incremental.TextEdit
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 增量 LaTeX 解析器。
 *
 * 追加和编辑共用 [LatexParser] 的分词与数学列表规则。分词器先重新扫描受编辑影响的区间。
 * 解析器仅复用已消费和已前瞻的 token 都保持相同的顶层表达式，从最后一个有效检查点
 * 继续解析。等长编辑的后缀若在原检查点重新对齐，也可以复用。
 * 预览的闭合状态来自词法单元和解析规则，当前 AST 仍保留未闭合输入的容错结果。
 * [getCurrentDocument] 返回当前源码的解析结果，[getPreviewDocument] 在结构未闭合时
 * 保留上一次完整的预览。
 */
class IncrementalLatexParser {

    private val tokenizer = IncrementalTokenizer()
    private var parsedTokens: List<LatexToken> = emptyList()
    private var checkpoints: List<ExpressionCheckpoint> = emptyList()
    private var cachedDocument: LatexNode.Document? = null
    private var previewDocument: LatexNode.Document? = null
    private var lastSuccessfulPosition = 0

    fun append(text: String) {
        if (text.isEmpty()) return
        val oldLength = tokenizer.getTextLength()
        val newLength = tokenizer.appendText(text)
        val source = tokenizer.getCurrentText()
        val tokens = if (oldLength == 0) tokenizer.tokenize(source)
            else tokenizer.update(source, TextEdit.fromAppend(oldLength, newLength))
        updateDocument(source, tokens)
    }

    fun setInput(newText: String) {
        val oldText = tokenizer.getCurrentText()
        if (oldText == newText) return
        if (newText.isEmpty()) {
            clear()
            return
        }
        val edit = TextEdit.diff(oldText, newText)
        lastSuccessfulPosition = minOf(lastSuccessfulPosition, edit.startOffset)
        val tokens = if (oldText.isEmpty()) tokenizer.tokenize(newText) else tokenizer.update(newText, edit)
        updateDocument(newText, tokens)
    }

    private fun updateDocument(source: String, tokens: List<LatexToken>) {
        cachedDocument = null
        val unfinishedLexeme = tokenizer.unfinishedUnitStart
        // 词法单元在文件末尾尚未结束，已有预览可以直接保留，当前 AST 按需解析。
        if (unfinishedLexeme != null && previewDocument != null) return

        val parsed = parse(source, tokens)
        cachedDocument = parsed?.document
        if (parsed == null) return
        if (!parsed.incomplete && unfinishedLexeme == null) {
            updatePreview(parsed.document, source.length)
        } else if (previewDocument == null) {
            // 首个不完整结果只预览解析器确认闭合的前缀，当前输入的检查点仍供后续编辑使用。
            val end = minOf(parsed.completePrefixEnd, unfinishedLexeme ?: source.length)
            if (end > 0) parseSafely { LatexParser().parse(source.substring(0, end)) }?.let { updatePreview(it, end) }
        }
    }

    private fun updatePreview(document: LatexNode.Document, end: Int) {
        previewDocument = document
        lastSuccessfulPosition = end
    }

    private data class ParsedDocument(val document: LatexNode.Document, val incomplete: Boolean, val completePrefixEnd: Int)

    private fun parse(source: String, tokens: List<LatexToken>): ParsedDocument? {
        var unchanged = 0
        if (checkpoints.isNotEmpty()) {
            val limit = minOf(parsedTokens.size, tokens.size)
            while (unchanged < limit && parsedTokens[unchanged] == tokens[unchanged]) unchanged++
        }
        // 前瞻可能超过表达式的源范围，例如 \dots 会查看后续非空白 token。
        // 从第一个读取过变化 token 的表达式开始重解析。
        val reused = ArrayList<ExpressionCheckpoint>()
        for (checkpoint in checkpoints) {
            if (checkpoint.nextIndex > unchanged || checkpoint.observedThrough >= unchanged) break
            reused.add(checkpoint)
        }

        val suffix = reusableSuffix(tokens, unchanged)
        return parseSafely {
            val session = ParseSession(tokens, source.length, source, reused, suffix)
            val document = session.parse()
            parsedTokens = tokens
            checkpoints = if (document.children.singleOrNull() is LatexNode.Error) emptyList() else reused
            ParsedDocument(document, session.hasIncompleteStructure, session.completePrefixEnd)
        }
    }

    private inline fun <T> parseSafely(parse: () -> T): T? =
        try {
            parse()
        } catch (error: Exception) {
            HLog.w("IncrementalLatexParser") { "解析失败: ${error.message}" }
            null
        }

    private fun reusableSuffix(tokens: List<LatexToken>, unchanged: Int): List<ExpressionCheckpoint> {
        if (checkpoints.isEmpty() || parsedTokens.size != tokens.size || parsedTokens.last() != tokens.last()) {
            return emptyList()
        }
        var start = tokens.size
        while (start > unchanged && parsedTokens[start - 1] == tokens[start - 1]) start--
        val first = checkpoints.indexOfFirst { it.startIndex >= start }
        return if (first >= 0) checkpoints.subList(first, checkpoints.size) else emptyList()
    }

    fun clear() {
        tokenizer.tokenize("")
        parsedTokens = emptyList()
        checkpoints = emptyList()
        cachedDocument = null
        previewDocument = null
        lastSuccessfulPosition = 0
    }

    /** 返回当前缓冲区的 AST，未闭合输入也使用与 [LatexParser] 相同的容错规则。 */
    fun getCurrentDocument(): LatexNode.Document {
        cachedDocument?.let { return it }
        val document = parse(tokenizer.getCurrentText(), tokenizer.getCurrentTokens())?.document
        cachedDocument = document
        return document ?: LatexNode.Document(emptyList())
    }

    /** 返回用于实时显示的结果，输入尚未闭合时保持上一次完整预览。 */
    fun getPreviewDocument(): LatexNode.Document = previewDocument ?: LatexNode.Document(emptyList())

    fun getCurrentInput(): String = tokenizer.getCurrentText()

    fun getProgress(): Float {
        val length = tokenizer.getTextLength()
        return if (length == 0) 1f else lastSuccessfulPosition.coerceAtMost(length).toFloat() / length
    }

    fun getUnparsedContent(): String {
        val source = tokenizer.getCurrentText()
        return source.substring(lastSuccessfulPosition.coerceAtMost(source.length))
    }
}
