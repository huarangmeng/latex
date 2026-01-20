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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 复杂结构测试
 * - 大型运算符（求和、积分、并集等）
 * - 矩阵（各种类型）
 * - 环境（equation、aligned、cases）
 * - 字体样式（粗体、斜体、黑板粗体等）
 */
class ComplexStructureTest {
    
    private val parser = LatexParser()
    
    // ========== 大型运算符测试 ==========
    
    @Test
    fun testSummation() {
        val doc = parser.parse("\\sum_{i=1}^{n} i")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val sum = doc.children[0] as LatexNode.BigOperator
        assertEquals("sum", sum.operator)
        assertNotNull(sum.subscript)
        assertNotNull(sum.superscript)
    }
    
    @Test
    fun testProduct() {
        val doc = parser.parse("\\prod_{i=1}^{n} x_i")
        val prod = doc.children[0] as LatexNode.BigOperator
        assertEquals("prod", prod.operator)
    }
    
    @Test
    fun testIntegral() {
        val doc = parser.parse("\\int_{0}^{\\infty} f(x) dx")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
    }
    
    @Test
    fun testDoubleIntegral() {
        val doc = parser.parse("\\iint f(x,y) dxdy")
        val integral = doc.children[0] as LatexNode.BigOperator
        assertEquals("iint", integral.operator)
    }
    
    @Test
    fun testTripleIntegral() {
        val doc = parser.parse("\\iiint f(x,y,z) dxdydz")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
    }
    
    @Test
    fun testContourIntegral() {
        val doc = parser.parse("\\oint f(z) dz")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
    }
    
    @Test
    fun testBigCup() {
        val doc = parser.parse("\\bigcup_{i=1}^{n} A_i")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
    }
    
    @Test
    fun testBigCap() {
        val doc = parser.parse("\\bigcap_{i=1}^{n} B_i")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
    }
    
    @Test
    fun testLimitOperator() {
        val doc = parser.parse("\\lim_{x \\to \\infty} f(x)")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val lim = doc.children[0] as LatexNode.BigOperator
        assertEquals("lim", lim.operator)
        assertNotNull(lim.subscript)
    }
    
    @Test
    fun testMaxOperator() {
        val doc = parser.parse("\\max_{i} x_i")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val max = doc.children[0] as LatexNode.BigOperator
        assertEquals("max", max.operator)
    }
    
    @Test
    fun testMinOperator() {
        val doc = parser.parse("\\min_{x \\in S} f(x)")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val min = doc.children[0] as LatexNode.BigOperator
        assertEquals("min", min.operator)
    }
    
    @Test
    fun testSupOperator() {
        val doc = parser.parse("\\sup_{x \\in A} f(x)")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val sup = doc.children[0] as LatexNode.BigOperator
        assertEquals("sup", sup.operator)
    }
    
    @Test
    fun testInfOperator() {
        val doc = parser.parse("\\inf_{x \\in B} g(x)")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val inf = doc.children[0] as LatexNode.BigOperator
        assertEquals("inf", inf.operator)
    }
    
    @Test
    fun testLimsupOperator() {
        val doc = parser.parse("\\limsup_{n \\to \\infty} a_n")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val limsup = doc.children[0] as LatexNode.BigOperator
        assertEquals("limsup", limsup.operator)
    }
    
    @Test
    fun testLiminfOperator() {
        val doc = parser.parse("\\liminf_{n \\to \\infty} b_n")
        assertTrue(doc.children[0] is LatexNode.BigOperator)
        val liminf = doc.children[0] as LatexNode.BigOperator
        assertEquals("liminf", liminf.operator)
    }
    
    // ========== 矩阵测试 ==========
    
    @Test
    fun testMatrix() {
        val doc = parser.parse("""
            \begin{matrix}
            a & b \\
            c & d
            \end{matrix}
        """.trimIndent())
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(2, matrix.rows.size)
        assertEquals(LatexNode.Matrix.MatrixType.PLAIN, matrix.type)
    }
    
    @Test
    fun testPmatrix() {
        val doc = parser.parse("""
            \begin{pmatrix}
            1 & 2 \\
            3 & 4
            \end{pmatrix}
        """.trimIndent())
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(LatexNode.Matrix.MatrixType.PAREN, matrix.type)
    }
    
    @Test
    fun testBmatrix() {
        val doc = parser.parse("""
            \begin{bmatrix}
            x & y \\
            z & w
            \end{bmatrix}
        """.trimIndent())
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(LatexNode.Matrix.MatrixType.BRACKET, matrix.type)
    }
    
    @Test
    fun testVmatrix() {
        val doc = parser.parse("""
            \begin{vmatrix}
            a & b \\
            c & d
            \end{vmatrix}
        """.trimIndent())
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(LatexNode.Matrix.MatrixType.VBAR, matrix.type)
    }
    
    @Test
    fun testSmallmatrix() {
        val doc = parser.parse("""
            \begin{smallmatrix}
            a & b \\
            c & d
            \end{smallmatrix}
        """.trimIndent())
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(LatexNode.Matrix.MatrixType.PLAIN, matrix.type)
        assertTrue(matrix.isSmall)
    }
    
    @Test
    fun testArray() {
        val doc = parser.parse("""
            \begin{array}{ccc}
            a & b & c \\
            d & e & f
            \end{array}
        """.trimIndent())
        assertTrue(doc.children[0] is LatexNode.Array)
        val array = doc.children[0] as LatexNode.Array
        assertEquals("ccc", array.alignment)
        assertEquals(2, array.rows.size)
        assertEquals(3, array.rows[0].size)
    }
    
    @Test
    fun testArrayWithMixedAlignment() {
        val doc = parser.parse("""
            \begin{array}{rcl}
            x &=& 1 \\
            y &=& 2
            \end{array}
        """.trimIndent())
        val array = doc.children[0] as LatexNode.Array
        assertEquals("rcl", array.alignment)
        assertEquals(2, array.rows.size)
    }
    
    @Test
    fun test3x3Matrix() {
        val doc = parser.parse("""
            \begin{matrix}
            1 & 2 & 3 \\
            4 & 5 & 6 \\
            7 & 8 & 9
            \end{matrix}
        """.trimIndent())
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(3, matrix.rows.size)
    }
    
    @Test
    fun testSingleRowMatrix() {
        val doc = parser.parse("\\begin{matrix} 1 & 2 & 3 \\end{matrix}")
        val matrix = doc.children[0] as LatexNode.Matrix
        assertEquals(1, matrix.rows.size)
    }
    
    // ========== 环境测试 ==========
    
    @Test
    fun testEquationEnvironment() {
        val doc = parser.parse("""
            \begin{equation}
            E = mc^2
            \end{equation}
        """.trimIndent())
        val env = doc.children[0] as LatexNode.Environment
        assertEquals("equation", env.name)
    }
    
    @Test
    fun testAlignedEnvironment() {
        val doc = parser.parse("""
            \begin{aligned}
            x &= 1 \\
            y &= 2
            \end{aligned}
        """.trimIndent())
        assertTrue(doc.children[0] is LatexNode.Aligned)
    }
    
    @Test
    fun testCasesEnvironment() {
        val doc = parser.parse("""
            \begin{cases}
            x & \text{if } x > 0 \\
            0 & \text{otherwise}
            \end{cases}
        """.trimIndent())
        assertTrue(doc.children[0] is LatexNode.Cases)
    }
    
    // ========== 字体样式测试 ==========
    
    @Test
    fun testTextMode() {
        val doc = parser.parse("\\text{Hello World}")
        assertTrue(doc.children[0] is LatexNode.TextMode)
        val text = doc.children[0] as LatexNode.TextMode
        assertEquals("Hello World", text.text)
    }
    
    @Test
    fun testMboxMode() {
        val doc = parser.parse("\\mbox{text content}")
        assertTrue(doc.children[0] is LatexNode.TextMode)
        val text = doc.children[0] as LatexNode.TextMode
        assertEquals("text content", text.text)
    }
    
    @Test
    fun testTextInFormula() {
        val doc = parser.parse("x^2 \\text{ when } x > 0")
        assertTrue(doc.children.any { it is LatexNode.TextMode })
    }
    
    @Test
    fun testTextWithSpaces() {
        val doc = parser.parse("\\text{  multiple   spaces  }")
        val text = doc.children[0] as LatexNode.TextMode
        assertTrue(text.text.contains("multiple"))
    }
    
    @Test
    fun testEmptyText() {
        val doc = parser.parse("\\text{}")
        assertTrue(doc.children[0] is LatexNode.TextMode)
        val text = doc.children[0] as LatexNode.TextMode
        assertEquals("", text.text)
    }
    
    @Test
    fun testMathBold() {
        val doc = parser.parse("\\mathbf{x}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.BOLD, style.styleType)
    }
    
    @Test
    fun testMathItalic() {
        val doc = parser.parse("\\mathit{ABC}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.ITALIC, style.styleType)
    }
    
    @Test
    fun testMathRoman() {
        val doc = parser.parse("\\mathrm{sin}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.ROMAN, style.styleType)
    }
    
    @Test
    fun testMathBB() {
        val doc = parser.parse("\\mathbb{R}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.BLACKBOARD_BOLD, style.styleType)
    }
    
    @Test
    fun testMathCal() {
        val doc = parser.parse("\\mathcal{F}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.CALLIGRAPHIC, style.styleType)
    }
    
    @Test
    fun testMathFrak() {
        val doc = parser.parse("\\mathfrak{g}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.FRAKTUR, style.styleType)
    }
    
    @Test
    fun testBoldsymbol() {
        val doc = parser.parse("\\boldsymbol{\\alpha}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.BOLD_SYMBOL, style.styleType)
    }
    
    @Test
    fun testBoldsymbolShorthand() {
        val doc = parser.parse("\\bm{\\beta}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.BOLD_SYMBOL, style.styleType)
    }
    
    @Test
    fun testBoldsymbolWithMultipleSymbols() {
        val doc = parser.parse("\\boldsymbol{\\alpha + \\beta}")
        val style = doc.children[0] as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.BOLD_SYMBOL, style.styleType)
        assertTrue(style.content.isNotEmpty())
    }
}
