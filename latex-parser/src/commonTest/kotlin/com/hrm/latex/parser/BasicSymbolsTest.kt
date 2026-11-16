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
import com.hrm.latex.parser.model.SourceRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 基础符号测试
 * - 纯文本
 * - 希腊字母（大小写）
 * - 运算符（加减乘除等）
 * - 关系符号（等于、小于、大于等）
 * - 箭头符号
 * - 空格命令
 */
class BasicSymbolsTest {
    
    private val parser = LatexParser()

    private fun LatexNode.descendants(): List<LatexNode> = listOf(this) + children().flatMap { it.descendants() }
    
    // ========== 纯文本测试 ==========
    
    @Test
    fun testSimpleText() {
        val doc = parser.parse("hello")
        assertEquals(1, doc.children.size)
        val text = doc.children[0] as LatexNode.Text
        assertEquals("hello", text.content)
    }
    
    @Test
    fun testMultipleTextNodes() {
        val doc = parser.parse("hello world")
        assertTrue(doc.children.size >= 2)
    }
    
    @Test
    fun testEmptyString() {
        val doc = parser.parse("")
        assertEquals(0, doc.children.size)
    }
    
    // ========== 希腊字母测试 ==========
    
    @Test
    fun testGreekLettersLowercase() {
        val tests = listOf("\\alpha", "\\beta", "\\gamma", "\\delta", "\\epsilon")
        tests.forEach { latex ->
            val doc = parser.parse(latex)
            assertTrue(doc.children[0] is LatexNode.Symbol, "Failed for $latex")
        }
    }
    
    @Test
    fun testGreekLettersUppercase() {
        val tests = listOf("\\Gamma", "\\Delta", "\\Theta", "\\Lambda", "\\Pi")
        tests.forEach { latex ->
            val doc = parser.parse(latex)
            assertTrue(doc.children[0] is LatexNode.Symbol, "Failed for $latex")
        }
    }
    
    @Test
    fun testGreekExpression() {
        val doc = parser.parse("\\alpha + \\beta = \\gamma")
        assertTrue(doc.children.size >= 5)
    }
    
    // ========== 运算符测试 ==========

    @Test
    fun asciiMinusUsesMathSymbolInBinaryUnaryAndScriptPositions() {
        val source = "-x + [A^-] - \\mathrm{-}"
        val result = parser.parseWithDiagnostics(source)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        val minusNodes = result.document.descendants().filterIsInstance<LatexNode.Symbol>()
        assertEquals(4, minusNodes.size)
        assertTrue(minusNodes.all { it.symbol == "minus" && it.unicode == "−" })
        assertEquals(source.indices.filter { source[it] == '-' }.map { SourceRange(it, it + 1) },
            minusNodes.map { it.sourceRange })
    }

    @Test
    fun textAndVerbatimHyphensStayLiteralWhileNestedMathUsesMinus() {
        val result = parser.parseWithDiagnostics(
            """\text{well-known \textbf{a-b} ${'$'}x-y${'$'}} \mbox{a-b} \verb|a-b|"""
        )
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        val nodes = result.document.descendants()
        val text = nodes.filterIsInstance<LatexNode.TextMode>().joinToString("") { it.text }
        assertTrue(text.startsWith("well-known a-b "), text)
        assertEquals(4, text.count { it == '-' })
        assertEquals("−", nodes.filterIsInstance<LatexNode.Symbol>().single().unicode)
    }

    @Test
    fun explicitMathcodeOverridesTheDefaultMinusMappingOnlyInItsScope() {
        val doc = parser.parse("""{\mathcode`-="002D -} -""")
        val custom = doc.descendants().filterIsInstance<LatexNode.MathClass>().single()
        assertEquals(LatexNode.MathClass.AtomClass.ORD, custom.atomClass)
        assertEquals("-", assertIs<LatexNode.Text>(custom.content.single()).content)
        assertEquals("−", doc.descendants().filterIsInstance<LatexNode.Symbol>().single().unicode)
    }

    @Test
    fun minusMappingPreservesSyntaxInDimensionsRangesKeysAndUrls() {
        val result = parser.parseWithDiagnostics(
            """\hspace{-5mm}\kern{-2pt}\cline{1-3}\label{eq-a}\ref{eq-a}\url{https://example.com/a-b}"""
        )
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        val nodes = result.document.children
        assertEquals(listOf("-5mm", "-2pt"), nodes.filterIsInstance<LatexNode.HSpace>().map { it.dimension })
        val line = nodes.filterIsInstance<LatexNode.CLine>().single()
        assertEquals(1, line.startCol)
        assertEquals(3, line.endCol)
        assertEquals("eq-a", nodes.filterIsInstance<LatexNode.Label>().single().key)
        assertEquals("eq-a", nodes.filterIsInstance<LatexNode.Ref>().single().key)
        assertEquals("https://example.com/a-b", nodes.filterIsInstance<LatexNode.Hyperlink>().single().url)
    }
    
    @Test
    fun testPlusMinus() {
        val doc = parser.parse("\\pm")
        assertTrue(doc.children[0] is LatexNode.Symbol)
    }
    
    @Test
    fun testTimes() {
        val doc = parser.parse("\\times")
        assertTrue(doc.children[0] is LatexNode.Symbol)
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("×", symbol.unicode)
    }
    
    @Test
    fun testDiv() {
        val doc = parser.parse("\\div")
        assertTrue(doc.children[0] is LatexNode.Symbol)
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("÷", symbol.unicode)
    }
    
    @Test
    fun testCdot() {
        val doc = parser.parse("\\cdot")
        assertTrue(doc.children[0] is LatexNode.Symbol)
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("⋅", symbol.unicode)
    }
    
    // ========== 关系符号测试 ==========
    
    @Test
    fun testLeq() {
        val doc = parser.parse("\\leq")
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("≤", symbol.unicode)
    }
    
    @Test
    fun testGeq() {
        val doc = parser.parse("\\geq")
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("≥", symbol.unicode)
    }
    
    @Test
    fun testNeq() {
        val doc = parser.parse("\\neq")
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("≠", symbol.unicode)
    }
    
    @Test
    fun testApprox() {
        val doc = parser.parse("\\approx")
        assertTrue(doc.children[0] is LatexNode.Symbol)
    }
    
    @Test
    fun testEquiv() {
        val doc = parser.parse("\\equiv")
        assertTrue(doc.children[0] is LatexNode.Symbol)
    }
    
    // ========== 箭头测试 ==========
    
    @Test
    fun testRightarrow() {
        val doc = parser.parse("\\rightarrow")
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("→", symbol.unicode)
    }
    
    @Test
    fun testTo() {
        // \to 是 \rightarrow 的简写
        val doc = parser.parse("\\to")
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("→", symbol.unicode)
        assertEquals("to", symbol.symbol)
    }
    
    @Test
    fun testLeftarrow() {
        val doc = parser.parse("\\leftarrow")
        assertTrue(doc.children[0] is LatexNode.Symbol)
    }
    
    @Test
    fun testRightArrow() {
        val doc = parser.parse("\\Rightarrow")
        val symbol = doc.children[0] as LatexNode.Symbol
        assertEquals("⇒", symbol.unicode)
    }
    
    @Test
    fun testLeftrightarrow() {
        val doc = parser.parse("\\leftrightarrow")
        assertTrue(doc.children[0] is LatexNode.Symbol)
    }
    
    // ========== 空格测试 ==========
    
    @Test
    fun testThinSpace() {
        val doc = parser.parse("a\\,b")
        assertTrue(doc.children.any { it is LatexNode.Space })
    }
    
    @Test
    fun testQuad() {
        val doc = parser.parse("a\\quad b")
        val space = doc.children.find { it is LatexNode.Space } as? LatexNode.Space
        assertTrue(space != null)
        assertEquals(LatexNode.Space.SpaceType.QUAD, space.type)
    }
    
    @Test
    fun testQquad() {
        val doc = parser.parse("a\\qquad b")
        val space = doc.children.find { it is LatexNode.Space } as? LatexNode.Space
        assertTrue(space != null)
        assertEquals(LatexNode.Space.SpaceType.QQUAD, space.type)
    }
}
