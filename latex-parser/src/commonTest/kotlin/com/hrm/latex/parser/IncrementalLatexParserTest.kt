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

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IncrementalLatexParserTest {

    // ========== 基本功能：追加 ==========

    @Test
    fun append_simpleText_parsesSuccessfully() {
        val parser = IncrementalLatexParser()
        parser.append("hello")
        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isNotEmpty())
    }

    @Test
    fun append_incrementally_producesCorrectResult() {
        val parser = IncrementalLatexParser()
        parser.append("x+")
        parser.append("y")
        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isNotEmpty())
        // x+y 应该产生 Text("x"), Text("+"), Text("y") 或类似结构
    }

    @Test
    fun append_latexCommand_parsesCorrectly() {
        val parser = IncrementalLatexParser()
        parser.append("\\frac{a}{b}")
        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isNotEmpty())

        // 应该包含 Fraction 节点
        val hasFraction = doc.children.any { it is LatexNode.Fraction }
        assertTrue(hasFraction, "Should contain a Fraction node")
    }

    @Test
    fun append_charByChar_eventuallyParsesCompleteFraction() {
        val parser = IncrementalLatexParser()
        val formula = "\\frac{a}{b}"

        // 逐字符追加
        formula.forEach { ch ->
            parser.append(ch.toString())
        }

        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isNotEmpty())

        // 完整输入后应能解析出 Fraction
        val hasFraction = doc.children.any { it is LatexNode.Fraction }
        assertTrue(hasFraction, "Complete input should produce Fraction node")
    }

    // ========== setInput（完全替换） ==========

    @Test
    fun setInput_replaceContent_parsesNewContent() {
        val parser = IncrementalLatexParser()
        parser.append("x+y")
        parser.setInput("a-b")

        val doc = parser.getCurrentDocument()
        assertEquals("a-b", parser.getCurrentInput())
        assertTrue(doc.children.isNotEmpty())
    }

    @Test
    fun setInput_sameContent_noChange() {
        val parser = IncrementalLatexParser()
        parser.append("x+y")
        val doc1 = parser.getCurrentDocument()

        parser.setInput("x+y")
        val doc2 = parser.getCurrentDocument()

        // 内容不变，结果应该相同
        assertEquals(doc1.children.size, doc2.children.size)
    }

    // ========== 容错：不完整输入 ==========

    @Test
    fun append_unclosedBrace_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("\\frac{a")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc, "Should return a document even with unclosed brace")
    }

    @Test
    fun append_incompleteCommand_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("\\")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc)
    }

    @Test
    fun append_incompleteSuperscript_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("x^")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc)
    }

    @Test
    fun append_incompleteSubscript_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("x_")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc)
    }

    @Test
    fun append_incompleteMathMode_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("\$x+y")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc)
    }

    @Test
    fun append_incompleteDisplayMath_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("\$\$x+y")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc)
    }

    @Test
    fun append_incompleteEnvironment_doesNotCrash() {
        val parser = IncrementalLatexParser()
        parser.append("\\begin{equation}x+y")
        val doc = parser.getCurrentDocument()
        assertNotNull(doc)
    }

    // ========== clear ==========

    @Test
    fun clear_resetsAllState() {
        val parser = IncrementalLatexParser()
        parser.append("x+y")
        assertTrue(parser.getCurrentInput().isNotEmpty())

        parser.clear()
        assertEquals("", parser.getCurrentInput())
        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isEmpty())
    }

    // ========== 进度追踪 ==========

    @Test
    fun getProgress_completeInput_returns1() {
        val parser = IncrementalLatexParser()
        parser.append("x+y")
        assertEquals(1f, parser.getProgress())
    }

    @Test
    fun getProgress_emptyInput_returns1() {
        val parser = IncrementalLatexParser()
        assertEquals(1f, parser.getProgress())
    }

    @Test
    fun getUnparsedContent_completeInput_returnsEmpty() {
        val parser = IncrementalLatexParser()
        parser.append("x+y")
        assertEquals("", parser.getUnparsedContent())
    }

    // ========== getCurrentInput ==========

    @Test
    fun getCurrentInput_reflectsAllAppends() {
        val parser = IncrementalLatexParser()
        parser.append("abc")
        assertEquals("abc", parser.getCurrentInput())
        parser.append("def")
        assertEquals("abcdef", parser.getCurrentInput())
    }

    @Test
    fun getCurrentInput_afterClear_isEmpty() {
        val parser = IncrementalLatexParser()
        parser.append("test")
        parser.clear()
        assertEquals("", parser.getCurrentInput())
    }

    // ========== 与标准解析器的一致性 ==========

    @Test
    fun append_completeFormula_matchesStandardParser() {
        val formula = "\\frac{1}{2} + \\sqrt{x}"

        // 标准解析器
        val standardParser = LatexParser()
        val standardDoc = standardParser.parse(formula)

        // 增量解析器
        val incrementalParser = IncrementalLatexParser()
        incrementalParser.append(formula)
        val incrementalDoc = incrementalParser.getCurrentDocument()

        // 节点数量应相同
        assertEquals(
            standardDoc.children.size,
            incrementalDoc.children.size,
            "Standard and incremental parser should produce same number of top-level nodes"
        )

        // 节点类型应相同
        for (i in standardDoc.children.indices) {
            assertEquals(
                standardDoc.children[i]::class,
                incrementalDoc.children[i]::class,
                "Node[$i] type mismatch"
            )
        }
    }

    @Test
    fun append_multipleFormulas_matchesStandardParser() {
        val formulas = listOf(
            "x^2 + y^2 = z^2",
            "e^{i\\pi} + 1 = 0",
            "\\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6}",
            "\\int_0^1 x^2 dx"
        )

        for (formula in formulas) {
            val standardDoc = LatexParser().parse(formula)

            val incrementalParser = IncrementalLatexParser()
            incrementalParser.append(formula)
            val incrementalDoc = incrementalParser.getCurrentDocument()

            assertEquals(
                standardDoc.children.size,
                incrementalDoc.children.size,
                "Mismatch for formula: $formula"
            )
        }
    }

    // ========== 复杂场景：编辑后再追加 ==========

    @Test
    fun setInput_thenAppend_parsesCorrectly() {
        val parser = IncrementalLatexParser()
        parser.append("x+")
        parser.setInput("x+y")
        parser.append("+z")

        assertEquals("x+y+z", parser.getCurrentInput())
        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isNotEmpty())
    }

    @Test
    fun append_largeFormula_parsesSuccessfully() {
        val parser = IncrementalLatexParser()
        val formula = "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"
        parser.append(formula)

        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.isNotEmpty())
    }

    @Test
    fun append_multipleClears_doesNotCorruptState() {
        val parser = IncrementalLatexParser()

        parser.append("x+y")
        parser.clear()
        parser.append("a-b")
        parser.clear()
        parser.append("\\frac{1}{2}")

        val doc = parser.getCurrentDocument()
        assertTrue(doc.children.any { it is LatexNode.Fraction })
    }

    // ========== 核心场景：不完整输入保留旧结果（无异常闪烁） ==========

    @Test
    fun append_incompleteInput_retainsLastSuccessfulResult() {
        val parser = IncrementalLatexParser()

        // 第一步：完整输入 x+y → 解析成功
        parser.append("x+y")
        val doc1 = parser.getCurrentDocument()
        assertTrue(doc1.children.isNotEmpty(), "Complete input should parse")
        val childCount1 = doc1.children.size

        // 第二步：追加不完整内容 +\frac{ → 解析失败
        // 期望：保留上次成功的 "x+y" 解析结果，不展示异常
        parser.append("+\\frac{")
        val doc2 = parser.getCurrentDocument()
        assertEquals(
            childCount1, doc2.children.size,
            "Incomplete input should retain last successful result, not show error"
        )
    }

    @Test
    fun append_incompleteToComplete_updatesResult() {
        val parser = IncrementalLatexParser()

        // 完整输入 → 成功
        parser.append("x+y")
        val doc1 = parser.getCurrentDocument()
        assertTrue(doc1.children.isNotEmpty())

        // 追加不完整 → 保留旧结果
        parser.append("+\\frac{a")
        val docMid = parser.getCurrentDocument()
        assertNotNull(docMid)

        // 补全 → 更新为新结果
        parser.append("}{b}")
        val doc2 = parser.getCurrentDocument()
        assertTrue(doc2.children.isNotEmpty())
        // 完整输入后应包含 Fraction
        val hasFraction = doc2.children.any { it is LatexNode.Fraction }
        assertTrue(hasFraction, "After completing input, should contain Fraction")
    }

    @Test
    fun append_streamingFraction_noIntermediateError() {
        val parser = IncrementalLatexParser()
        val formula = "\\frac{a}{b}"

        // 逐字符追加，记录每一步的文档
        var lastSuccessfulChildCount = 0

        formula.forEach { ch ->
            parser.append(ch.toString())
            val doc = parser.getCurrentDocument()

            // 核心断言：在整个输入过程中，不应出现包含原始 LaTeX 源码的 Text 节点
            // （如 "\frac{a" 这样的文本）
            for (node in doc.children) {
                if (node is LatexNode.Text) {
                    val text = node.content
                    assertFalse(
                        text.contains("\\frac"),
                        "Should never render raw LaTeX command as text: '$text'"
                    )
                }
            }

            // 节点数只增不减（或保持不变），不应因中间状态而清空
            if (doc.children.isNotEmpty()) {
                assertTrue(
                    doc.children.size >= lastSuccessfulChildCount || lastSuccessfulChildCount == 0,
                    "Document should not shrink during streaming"
                )
                lastSuccessfulChildCount = doc.children.size
            }
        }

        // 最终应包含 Fraction
        val finalDoc = parser.getCurrentDocument()
        assertTrue(
            finalDoc.children.any { it is LatexNode.Fraction },
            "Final document should contain Fraction"
        )
    }
}
