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

import com.hrm.latex.parser.incremental.IncrementalTokenizer
import com.hrm.latex.parser.incremental.TextEdit
import com.hrm.latex.parser.tokenizer.LatexToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IncrementalTokenizerTest {

    // ========== 全量分词 ==========

    @Test
    fun tokenize_simpleText_matchesStandardTokenizer() {
        val tokenizer = IncrementalTokenizer()
        val tokens = tokenizer.tokenize("hello world")

        assertTrue(tokens.any { it is LatexToken.Text && it.content == "hello" })
        assertTrue(tokens.any { it is LatexToken.Whitespace })
        assertTrue(tokens.any { it is LatexToken.Text && it.content == "world" })
        assertTrue(tokens.last() is LatexToken.EOF)
    }

    @Test
    fun tokenize_latexCommand_producesCommandToken() {
        val tokenizer = IncrementalTokenizer()
        val tokens = tokenizer.tokenize("\\alpha")

        val commands = tokens.filterIsInstance<LatexToken.Command>()
        assertEquals(1, commands.size)
        assertEquals("alpha", commands[0].name)
    }

    // ========== 增量更新：追加 ==========

    @Test
    fun update_appendText_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("ab")

        val edit = TextEdit.diff("ab", "abc")
        val incrementalTokens = tokenizer.update("abc", edit)

        // 验证与全量分词结果一致
        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("abc")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    @Test
    fun update_appendLatexCommand_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("x+")

        val edit = TextEdit.diff("x+", "x+\\alpha")
        val incrementalTokens = tokenizer.update("x+\\alpha", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("x+\\alpha")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    // ========== 增量更新：插入 ==========

    @Test
    fun update_insertInMiddle_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("x+y")

        val edit = TextEdit.diff("x+y", "x+z+y")
        val incrementalTokens = tokenizer.update("x+z+y", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("x+z+y")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    @Test
    fun update_insertBraces_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("\\frac ab")

        val edit = TextEdit.diff("\\frac ab", "\\frac {a}{b}")
        val incrementalTokens = tokenizer.update("\\frac {a}{b}", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("\\frac {a}{b}")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    // ========== 增量更新：删除 ==========

    @Test
    fun update_deleteText_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("x+y+z")

        val edit = TextEdit.diff("x+y+z", "x+z")
        val incrementalTokens = tokenizer.update("x+z", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("x+z")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    @Test
    fun update_deleteCommand_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("\\alpha+\\beta")

        val edit = TextEdit.diff("\\alpha+\\beta", "\\alpha+")
        val incrementalTokens = tokenizer.update("\\alpha+", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("\\alpha+")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    // ========== 增量更新：替换 ==========

    @Test
    fun update_replaceText_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("\\frac{a}{b}")

        val edit = TextEdit.diff("\\frac{a}{b}", "\\frac{x}{y}")
        val incrementalTokens = tokenizer.update("\\frac{x}{y}", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("\\frac{x}{y}")

        assertTokensEquivalent(fullTokens, incrementalTokens)
    }

    // ========== 多次连续编辑 ==========

    @Test
    fun update_multipleAppends_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("")

        // 模拟逐字输入 \int
        val steps = listOf("\\", "\\i", "\\in", "\\int")
        var currentText = ""

        for (step in steps) {
            val edit = TextEdit.diff(currentText, step)
            tokenizer.update(step, edit)
            currentText = step
        }

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("\\int")

        assertTokensEquivalent(fullTokens, tokenizer.getCurrentTokens())
    }

    @Test
    fun update_appendThenDeleteThenAppend_resultMatchesFullTokenize() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("x+y")

        // 追加 +z
        var edit = TextEdit.diff("x+y", "x+y+z")
        tokenizer.update("x+y+z", edit)

        // 删除 +y
        edit = TextEdit.diff("x+y+z", "x+z")
        tokenizer.update("x+z", edit)

        // 追加 +w
        edit = TextEdit.diff("x+z", "x+z+w")
        tokenizer.update("x+z+w", edit)

        val fullTokenizer = IncrementalTokenizer()
        val fullTokens = fullTokenizer.tokenize("x+z+w")

        assertTokensEquivalent(fullTokens, tokenizer.getCurrentTokens())
    }

    // ========== 边界情况 ==========

    @Test
    fun update_emptyToNonEmpty() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("")

        val edit = TextEdit.diff("", "\\alpha")
        val tokens = tokenizer.update("\\alpha", edit)

        val commands = tokens.filterIsInstance<LatexToken.Command>()
        assertEquals(1, commands.size)
        assertEquals("alpha", commands[0].name)
    }

    @Test
    fun update_nonEmptyToEmpty_producesOnlyEOF() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("hello")

        val edit = TextEdit.diff("hello", "")
        val tokens = tokenizer.update("", edit)

        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is LatexToken.EOF)
    }

    @Test
    fun getCurrentText_reflectsLatestText() {
        val tokenizer = IncrementalTokenizer()
        tokenizer.tokenize("hello")
        assertEquals("hello", tokenizer.getCurrentText())

        tokenizer.update("hello world", TextEdit.diff("hello", "hello world"))
        assertEquals("hello world", tokenizer.getCurrentText())
    }

    // ========== 辅助方法 ==========

    /**
     * 断言两个 token 列表等价（类型和内容相同，位置相同）
     */
    private fun assertTokensEquivalent(expected: List<LatexToken>, actual: List<LatexToken>) {
        assertEquals(
            expected.size, actual.size,
            "Token count mismatch.\nExpected: ${expected.map { tokenSummary(it) }}\n" +
                    "Actual:   ${actual.map { tokenSummary(it) }}"
        )
        for (i in expected.indices) {
            val exp = expected[i]
            val act = actual[i]
            assertEquals(
                tokenSummary(exp), tokenSummary(act),
                "Token[$i] mismatch"
            )
        }
    }

    private fun tokenSummary(token: LatexToken): String = when (token) {
        is LatexToken.Text -> "Text(${token.content}, ${token.range})"
        is LatexToken.Command -> "Cmd(${token.name}, ${token.range})"
        is LatexToken.BeginEnvironment -> "BeginEnv(${token.name}, ${token.range})"
        is LatexToken.EndEnvironment -> "EndEnv(${token.name}, ${token.range})"
        is LatexToken.LeftBrace -> "LBrace(${token.range})"
        is LatexToken.RightBrace -> "RBrace(${token.range})"
        is LatexToken.LeftBracket -> "LBracket(${token.range})"
        is LatexToken.RightBracket -> "RBracket(${token.range})"
        is LatexToken.Superscript -> "Super(${token.range})"
        is LatexToken.Subscript -> "Sub(${token.range})"
        is LatexToken.Ampersand -> "Amp(${token.range})"
        is LatexToken.NewLine -> "NL(${token.range})"
        is LatexToken.Whitespace -> "WS(${token.range})"
        is LatexToken.MathShift -> "Math(${token.count}, ${token.range})"
        is LatexToken.EOF -> "EOF(${token.range})"
    }
}
