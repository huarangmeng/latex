package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
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
