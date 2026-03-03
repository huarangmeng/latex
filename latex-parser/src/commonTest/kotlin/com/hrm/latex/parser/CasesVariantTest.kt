package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * dcases / rcases 变体环境测试
 */
class CasesVariantTest {
    private val parser = LatexParser()

    @Test
    fun testDcasesParsesAsDisplayStyle() {
        val doc = parser.parse(
            "\\begin{dcases} \\frac{1}{2} & x > 0 \\\\ 0 & x = 0 \\end{dcases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.DISPLAY, cases.style)
        assertEquals(2, cases.cases.size)
    }

    @Test
    fun testRcasesParsesAsRightBrace() {
        val doc = parser.parse(
            "\\begin{rcases} x^2 & x > 0 \\\\ -x^2 & x < 0 \\end{rcases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.RIGHT, cases.style)
        assertEquals(2, cases.cases.size)
    }

    @Test
    fun testStandardCasesIsNormalStyle() {
        val doc = parser.parse(
            "\\begin{cases} 1 & x > 0 \\\\ 0 & x = 0 \\end{cases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.NORMAL, cases.style)
    }

    @Test
    fun testDcasesSingleCase() {
        val doc = parser.parse(
            "\\begin{dcases} x & x > 0 \\end{dcases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.DISPLAY, cases.style)
        assertEquals(1, cases.cases.size)
    }

    @Test
    fun testRcasesMultipleCases() {
        val doc = parser.parse(
            "\\begin{rcases} a & p \\\\ b & q \\\\ c & r \\end{rcases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.RIGHT, cases.style)
        assertEquals(3, cases.cases.size)
    }

    @Test
    fun testDcasesWithNestedFractions() {
        val doc = parser.parse(
            "\\begin{dcases} \\frac{a}{b} & x > 0 \\\\ \\frac{c}{d} & x < 0 \\end{dcases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.DISPLAY, cases.style)
        assertEquals(2, cases.cases.size)
        // 验证表达式中包含 Fraction 节点
        val firstExpr = cases.cases[0].first
        assertTrue(firstExpr is LatexNode.Group)
        val group = firstExpr as LatexNode.Group
        assertTrue(group.children.any { it is LatexNode.Fraction })
    }

    @Test
    fun testRcasesWithoutConditions() {
        val doc = parser.parse(
            "\\begin{rcases} a \\\\ b \\\\ c \\end{rcases}"
        )
        val cases = doc.children[0] as LatexNode.Cases
        assertEquals(LatexNode.Cases.CasesStyle.RIGHT, cases.style)
        assertEquals(3, cases.cases.size)
    }
}
