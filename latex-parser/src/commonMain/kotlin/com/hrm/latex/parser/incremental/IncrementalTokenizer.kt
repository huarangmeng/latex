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

package com.hrm.latex.parser.incremental

import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * 增量分词器。
 *
 * 每次扫描记录词法单元的起点，包括被跳过的注释和还未产出 token 的命令。
 * 一个单元可以产生多个 token，例如超链接命令和它的 URL 参数。
 * 编辑发生时保留未受影响的前缀，从受影响单元的起点重扫。
 * 新旧检查点在未修改的后缀上对齐后，后续扫描状态和源文本一致，可以直接复用后缀。
 */
class IncrementalTokenizer {

    private class Lexeme(val token: LatexToken, val restart: Int)

    private var lexemes: List<Lexeme> = emptyList()
    private var eofRestart = 0
    private val textBuffer = StringBuilder()
    private var cachedResult: List<LatexToken>? = null

    fun tokenize(text: String): List<LatexToken> {
        lexemes = emptyList()
        return collect(text, 0, 0, TextEdit(0, 0, text.length))
    }

    fun update(newText: String, edit: TextEdit): List<LatexToken> {
        if (edit.oldLength == 0 && edit.newLength == 0) return getCurrentTokens()
        if (lexemes.isEmpty()) return tokenize(newText)

        // 编辑点可能扩展前一个文本或命令 token，必须包含结束位置恰好等于编辑点的 token。
        var prefix = firstAffected(edit.startOffset)
        val restart = lexemes.getOrNull(prefix)?.restart ?: eofRestart
        while (prefix > 0 && lexemes[prefix - 1].restart >= restart) prefix--
        val suffix = firstCheckpointAtOrAfter(edit.oldEndOffset, prefix)
        return collect(newText, restart, prefix, edit, suffix)
    }

    private fun collect(
        text: String,
        restart: Int,
        prefix: Int,
        edit: TextEdit,
        suffix: Int = lexemes.size,
    ): List<LatexToken> {
        val old = lexemes
        val result = ArrayList<Lexeme>(old.size + 4)
        for (index in 0 until prefix) result.add(old[index])

        val scanner = LatexTokenizer(text, restart)
        var candidate = suffix
        while (true) {
            val token = scanner.nextToken()
            val checkpoint = scanner.restartOffset
            if (token is LatexToken.EOF) {
                eofRestart = checkpoint
                break
            }
            if (scanner.firstInUnit && checkpoint >= edit.newEndOffset) {
                while (candidate < old.size && old[candidate].restart + edit.delta < checkpoint) candidate++
                if (candidate < old.size && old[candidate].restart + edit.delta == checkpoint) {
                    // 检查点在编辑区间之外，两边从同一状态扫描相同源码，可以停止重扫。
                    for (index in candidate until old.size) {
                        val lexeme = old[index]
                        result.add(if (edit.delta == 0) lexeme else Lexeme(
                            lexeme.token.withRange(SourceRange(
                                lexeme.token.range.start + edit.delta, lexeme.token.range.end + edit.delta
                            )),
                            lexeme.restart + edit.delta,
                        ))
                    }
                    eofRestart += edit.delta
                    break
                }
            }
            result.add(Lexeme(token, checkpoint))
        }
        lexemes = result
        textBuffer.clear()
        textBuffer.append(text)
        cachedResult = null
        return getCurrentTokens()
    }

    private fun firstAffected(offset: Int): Int {
        var low = 0
        var high = lexemes.size
        while (low < high) {
            val mid = (low + high) / 2
            if (lexemes[mid].token.range.end < offset) low = mid + 1 else high = mid
        }
        return low
    }

    private fun firstCheckpointAtOrAfter(offset: Int, prefix: Int): Int {
        var low = prefix
        var high = lexemes.size
        while (low < high) {
            val mid = (low + high) / 2
            if (lexemes[mid].restart < offset) low = mid + 1 else high = mid
        }
        return low
    }

    fun getCurrentText(): String = textBuffer.toString()

    fun appendText(text: String): Int {
        textBuffer.append(text)
        return textBuffer.length
    }

    fun getTextLength(): Int = textBuffer.length

    fun getCurrentTokens(): List<LatexToken> {
        cachedResult?.let { return it }
        val result = ArrayList<LatexToken>(lexemes.size + 1)
        for (lexeme in lexemes) result.add(lexeme.token)
        result.add(LatexToken.EOF(SourceRange(textBuffer.length, textBuffer.length)))
        cachedResult = result
        return result
    }
}
