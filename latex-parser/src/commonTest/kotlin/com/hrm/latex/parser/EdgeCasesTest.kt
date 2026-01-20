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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 边界情况和特殊场景测试
 * - 嵌套结构
 * - 长表达式
 * - 特殊字符转义
 * - 多个相同元素
 * - 空内容处理
 */
class EdgeCasesTest {
    
    private val parser = LatexParser()
    
    // ========== 嵌套结构测试 ==========
    
    @Test
    fun testNestedGroups() {
        val doc = parser.parse("{{a}}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testDeeplyNestedFractions() {
        // 三层嵌套分数
        val doc = parser.parse("\\frac{\\frac{\\frac{a}{b}}{c}}{d}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testNestedRootsAndFractions() {
        // 根号和分数混合嵌套
        val doc = parser.parse("\\sqrt{\\frac{\\sqrt{x}}{\\sqrt{y}}}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 多元素测试 ==========
    
    @Test
    fun testMultipleFractions() {
        val doc = parser.parse("\\frac{1}{2} + \\frac{3}{4} + \\frac{5}{6}")
        assertTrue(doc.children.size >= 3)
    }
    
    @Test
    fun testMultipleScripts() {
        val doc = parser.parse("x_1^2 + x_2^2 + x_3^2 + x_4^2")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testMultipleSummations() {
        val doc = parser.parse("\\sum_{i=1}^{n} \\sum_{j=1}^{m} a_{ij}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 长表达式测试 ==========
    
    @Test
    fun testLongExpression() {
        val doc = parser.parse("a + b + c + d + e + f + g + h + i + j")
        assertTrue(doc.children.size >= 10)
    }
    
    @Test
    fun testLongPolynomial() {
        // 长多项式
        val doc = parser.parse("x^5 + 2x^4 - 3x^3 + 4x^2 - 5x + 6")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testMultipleGreekLetters() {
        // 多个希腊字母
        val doc = parser.parse(
            "\\alpha \\beta \\gamma \\delta \\epsilon \\zeta \\eta \\theta"
        )
        assertTrue(doc.children.size >= 8)
    }
    
    // ========== 特殊字符测试 ==========
    
    @Test
    fun testSpecialCharactersEscaped() {
        val doc = parser.parse("\\{ \\} \\$ \\%")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testBackslashInText() {
        val doc = parser.parse("\\backslash")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 空内容和边界值 ==========
    
    @Test
    fun testEmptyGroup() {
        val doc = parser.parse("{}")
        assertTrue(doc.children.size >= 0)
    }
    
    @Test
    fun testEmptyFraction() {
        val doc = parser.parse("\\frac{}{}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testEmptySqrt() {
        val doc = parser.parse("\\sqrt{}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 复杂组合测试 ==========
    
    @Test
    fun testMixedEnvironments() {
        // 混合矩阵和cases环境
        val doc = parser.parse("""
            f(x) = \begin{cases}
            \begin{matrix} 1 & 0 \\ 0 & 1 \end{matrix} & x > 0 \\
            0 & x \leq 0
            \end{cases}
        """.trimIndent())
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testAllTypesOfBrackets() {
        // 所有类型括号混用
        val doc = parser.parse(
            "\\left( \\left[ \\left\\{ \\left\\langle x \\right\\rangle \\right\\} \\right] \\right)"
        )
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testSuperscriptOnOperator() {
        // 运算符上的上标
        val doc = parser.parse("\\sum^{\\infty} \\int^{b}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 实际可能出现的复杂场景 ==========
    
    @Test
    fun testPhysicsEquationWithMultipleStyles() {
        // 包含多种样式的物理方程
        val doc = parser.parse(
            "\\mathbf{F} = m\\vec{a} = \\frac{d\\mathbf{p}}{dt}"
        )
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testStatisticsFormula() {
        // 统计学公式（标准差）
        val doc = parser.parse("""
            \sigma = \sqrt{\frac{1}{N}\sum_{i=1}^{N}(x_i - \mu)^2}
        """.trimIndent())
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testMatrixEquation() {
        // 矩阵方程
        val doc = parser.parse("""
            \begin{bmatrix} a & b \\ c & d \end{bmatrix} 
            \begin{bmatrix} x \\ y \end{bmatrix} = 
            \begin{bmatrix} e \\ f \end{bmatrix}
        """.trimIndent())
        assertTrue(doc.children.size >= 3)
    }
    
    @Test
    fun testPiecewiseFunction() {
        // 分段函数（复杂版）
        val doc = parser.parse("""
            f(x) = \begin{cases}
            x^2 & \text{if } x < 0 \\
            \sqrt{x} & \text{if } 0 \leq x < 1 \\
            \frac{1}{x} & \text{if } x \geq 1
            \end{cases}
        """.trimIndent())
        assertTrue(doc.children.isNotEmpty())
    }
}
