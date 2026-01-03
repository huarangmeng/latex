package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 简单公式测试
 * - 分数（各种类型）
 * - 根号（普通和带次数）
 * - 上下标
 * - 括号（各种分隔符）
 * - 装饰符号（帽子、波浪线等）
 */
class SimpleFormulaTest {
    
    private val parser = LatexParser()
    
    // ========== 分数测试 ==========
    
    @Test
    fun testFraction() {
        val doc = parser.parse("\\frac{a}{b}")
        assertEquals(1, doc.children.size)
        assertTrue(doc.children[0] is LatexNode.Fraction)
    }
    
    @Test
    fun testNestedFraction() {
        val doc = parser.parse("\\frac{\\frac{a}{b}}{c}")
        val frac = doc.children[0] as LatexNode.Fraction
        assertTrue(frac.numerator is LatexNode.Group)
    }
    
    @Test
    fun testDfrac() {
        val doc = parser.parse("\\dfrac{1}{2}")
        assertTrue(doc.children[0] is LatexNode.Fraction)
    }
    
    @Test
    fun testTfrac() {
        val doc = parser.parse("\\tfrac{1}{2}")
        assertTrue(doc.children[0] is LatexNode.Fraction)
    }
    
    @Test
    fun testCfrac() {
        val doc = parser.parse("\\cfrac{1}{2}")
        assertTrue(doc.children[0] is LatexNode.Fraction)
    }
    
    @Test
    fun testMultipleFractions() {
        val doc = parser.parse("\\frac{1}{2} + \\frac{3}{4}")
        assertTrue(doc.children.size >= 2)
    }
    
    // ========== 二项式系数测试 ==========
    
    @Test
    fun testBinom() {
        val doc = parser.parse("\\binom{n}{k}")
        assertEquals(1, doc.children.size)
        assertTrue(doc.children[0] is LatexNode.Binomial)
        val binom = doc.children[0] as LatexNode.Binomial
        assertEquals(LatexNode.Binomial.BinomialStyle.NORMAL, binom.style)
    }
    
    @Test
    fun testTbinom() {
        val doc = parser.parse("\\tbinom{n}{k}")
        assertTrue(doc.children[0] is LatexNode.Binomial)
        val binom = doc.children[0] as LatexNode.Binomial
        assertEquals(LatexNode.Binomial.BinomialStyle.TEXT, binom.style)
    }
    
    @Test
    fun testDbinom() {
        val doc = parser.parse("\\dbinom{n}{k}")
        assertTrue(doc.children[0] is LatexNode.Binomial)
        val binom = doc.children[0] as LatexNode.Binomial
        assertEquals(LatexNode.Binomial.BinomialStyle.DISPLAY, binom.style)
    }
    
    @Test
    fun testBinomialWithComplexContent() {
        val doc = parser.parse("\\binom{n+1}{k-1}")
        val binom = doc.children[0] as LatexNode.Binomial
        assertTrue(binom.top is LatexNode.Group)
        assertTrue(binom.bottom is LatexNode.Group)
    }
    
    @Test
    fun testNestedBinomial() {
        val doc = parser.parse("\\binom{\\binom{n}{k}}{m}")
        val binom = doc.children[0] as LatexNode.Binomial
        assertTrue(binom.top is LatexNode.Group)
    }
    
    // ========== 根号测试 ==========
    
    @Test
    fun testSqrt() {
        val doc = parser.parse("\\sqrt{x}")
        assertEquals(1, doc.children.size)
        assertTrue(doc.children[0] is LatexNode.Root)
    }
    
    @Test
    fun testSqrtWithIndex() {
        val doc = parser.parse("\\sqrt[3]{x}")
        val root = doc.children[0] as LatexNode.Root
        assertNotNull(root.index)
    }
    
    @Test
    fun testNestedSqrt() {
        val doc = parser.parse("\\sqrt{\\sqrt{x}}")
        val root = doc.children[0] as LatexNode.Root
        assertTrue(root.content is LatexNode.Group)
    }
    
    @Test
    fun testSqrtWithFraction() {
        val doc = parser.parse("\\sqrt{\\frac{a}{b}}")
        assertTrue(doc.children[0] is LatexNode.Root)
    }
    
    // ========== 上下标测试 ==========
    
    @Test
    fun testSuperscript() {
        val doc = parser.parse("x^2")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testSubscript() {
        val doc = parser.parse("x_i")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testSuperAndSubscript() {
        val doc = parser.parse("x_i^2")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testComplexSuperscript() {
        val doc = parser.parse("x^{n+1}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testComplexSubscript() {
        val doc = parser.parse("a_{i,j}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testMixedScripts() {
        val doc = parser.parse("x_1^2 + x_2^2")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 括号测试 ==========
    
    @Test
    fun testDelimiters() {
        val doc = parser.parse("\\left( x + y \\right)")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("(", delim.left)
        assertEquals(")", delim.right)
    }
    
    @Test
    fun testSquareBrackets() {
        val doc = parser.parse("\\left[ x \\right]")
        assertTrue(doc.children[0] is LatexNode.Delimited)
    }
    
    @Test
    fun testCurlyBraces() {
        val doc = parser.parse("\\left\\{ x \\right\\}")
        assertTrue(doc.children[0] is LatexNode.Delimited)
    }
    
    @Test
    fun testAngleBrackets() {
        val doc = parser.parse("\\left\\langle x \\right\\rangle")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("⟨", delim.left)
        assertEquals("⟩", delim.right)
    }
    
    @Test
    fun testFloorBrackets() {
        val doc = parser.parse("\\left\\lfloor x \\right\\rfloor")
        assertTrue(doc.children[0] is LatexNode.Delimited)
    }
    
    @Test
    fun testCeilBrackets() {
        val doc = parser.parse("\\left\\lceil x \\right\\rceil")
        assertTrue(doc.children[0] is LatexNode.Delimited)
    }
    
    // ========== 装饰符号测试 ==========
    
    @Test
    fun testHat() {
        val doc = parser.parse("\\hat{x}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.HAT, accent.accentType)
    }
    
    @Test
    fun testTilde() {
        val doc = parser.parse("\\tilde{x}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.TILDE, accent.accentType)
    }
    
    @Test
    fun testOverline() {
        val doc = parser.parse("\\overline{AB}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.OVERLINE, accent.accentType)
    }
    
    @Test
    fun testUnderline() {
        val doc = parser.parse("\\underline{text}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.UNDERLINE, accent.accentType)
    }
    
    @Test
    fun testVec() {
        val doc = parser.parse("\\vec{v}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.VEC, accent.accentType)
    }
    
    @Test
    fun testDot() {
        val doc = parser.parse("\\dot{x}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.DOT, accent.accentType)
    }
    
    @Test
    fun testDdot() {
        val doc = parser.parse("\\ddot{x}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.DDOT, accent.accentType)
    }
    
    @Test
    fun testWidehat() {
        val doc = parser.parse("\\widehat{ABC}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.WIDEHAT, accent.accentType)
    }
    
    @Test
    fun testOverrightarrow() {
        val doc = parser.parse("\\overrightarrow{AB}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.OVERRIGHTARROW, accent.accentType)
    }
    
    @Test
    fun testOverleftarrow() {
        val doc = parser.parse("\\overleftarrow{BA}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.OVERLEFTARROW, accent.accentType)
    }

    @Test
    fun testCancel() {
        val doc = parser.parse("\\cancel{x}")
        val accent = doc.children[0] as LatexNode.Accent
        assertEquals(LatexNode.Accent.AccentType.CANCEL, accent.accentType)
        // parseArgument() 返回 Group 节点
        assertTrue(accent.content is LatexNode.Group)
        val group = accent.content as LatexNode.Group
        assertTrue(group.children.isNotEmpty())
        assertTrue(group.children[0] is LatexNode.Text)
        assertEquals("x", (group.children[0] as LatexNode.Text).content)
    }

    @Test
    fun testXrightarrow() {
        val doc = parser.parse("\\xrightarrow{f}")
        val arrow = doc.children[0] as LatexNode.ExtensibleArrow
        assertEquals(LatexNode.ExtensibleArrow.Direction.RIGHT, arrow.direction)
        // parseArgument() 返回 Group 节点
        assertTrue(arrow.content is LatexNode.Group)
        val group = arrow.content as LatexNode.Group
        assertTrue(group.children.isNotEmpty())
        assertTrue(group.children[0] is LatexNode.Text)
        assertEquals("f", (group.children[0] as LatexNode.Text).content)
        assertEquals(null, arrow.below)
    }

    @Test
    fun testXleftarrow() {
        val doc = parser.parse("\\xleftarrow{g}")
        val arrow = doc.children[0] as LatexNode.ExtensibleArrow
        assertEquals(LatexNode.ExtensibleArrow.Direction.LEFT, arrow.direction)
        // parseArgument() 返回 Group 节点
        assertTrue(arrow.content is LatexNode.Group)
        val group = arrow.content as LatexNode.Group
        assertTrue(group.children.isNotEmpty())
        assertTrue(group.children[0] is LatexNode.Text)
        assertEquals("g", (group.children[0] as LatexNode.Text).content)
    }

    @Test
    fun testXrightarrowWithBelowText() {
        val doc = parser.parse("\\xrightarrow[n\\to\\infty]{\\text{极限}}")
        val arrow = doc.children[0] as LatexNode.ExtensibleArrow
        assertEquals(LatexNode.ExtensibleArrow.Direction.RIGHT, arrow.direction)
        assertTrue(arrow.below != null, "Below text should not be null")
    }
}
