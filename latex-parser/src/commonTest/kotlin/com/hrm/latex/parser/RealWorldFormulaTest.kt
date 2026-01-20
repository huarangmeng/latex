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
import kotlin.test.assertTrue

/**
 * 真实数学公式测试
 * 包含13个经典的数学、物理公式，验证解析器在实际场景中的表现
 * - 代数公式（二次公式、二项式定理等）
 * - 微积分公式（泰勒级数、傅里叶变换等）
 * - 物理公式（薛定谔方程、麦克斯韦方程等）
 * - 线性代数（矩阵行列式）
 */
class RealWorldFormulaTest {
    
    private val parser = LatexParser()
    
    // ========== 代数公式 ==========
    
    @Test
    fun testQuadraticFormula() {
        // 二次公式
        val doc = parser.parse("\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}")
        assertTrue(doc.children[0] is LatexNode.Fraction)
    }
    
    @Test
    fun testBinomialTheorem() {
        // 二项式定理
        val doc = parser.parse("(a+b)^n = \\sum_{k=0}^{n} \\binom{n}{k} a^{n-k} b^k")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testEulerFormula() {
        // 欧拉公式
        val doc = parser.parse("e^{i\\pi} + 1 = 0")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 微积分公式 ==========
    
    @Test
    fun testTaylorSeries() {
        // 泰勒级数
        val doc = parser.parse("f(x) = \\sum_{n=0}^{\\infty} \\frac{f^{(n)}(a)}{n!}(x-a)^n")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testFourierTransform() {
        // 傅里叶变换
        val doc = parser.parse("F(\\omega) = \\int_{-\\infty}^{\\infty} f(t) e^{-i\\omega t} dt")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testCauchyIntegralFormula() {
        // 柯西积分公式
        val doc = parser.parse("f(a) = \\frac{1}{2\\pi i} \\oint_{\\gamma} \\frac{f(z)}{z-a} dz")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testLimitDefinition() {
        // 极限定义
        val doc = parser.parse("\\lim_{x \\to \\infty} f(x) = L")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testDerivativeDefinition() {
        // 导数定义
        val doc = parser.parse("f'(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testIntegralByParts() {
        // 分部积分法
        val doc = parser.parse("\\int u\\,dv = uv - \\int v\\,du")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 物理公式 ==========
    
    @Test
    fun testSchrodingerEquation() {
        // 薛定谔方程
        val doc = parser.parse("i\\hbar\\frac{\\partial}{\\partial t}\\Psi = \\hat{H}\\Psi")
        assertTrue(doc.children.isNotEmpty())
    }
    
    @Test
    fun testMaxwellEquation() {
        // 麦克斯韦方程（法拉第电磁感应定律）
        val doc = parser.parse("\\nabla \\times \\vec{E} = -\\frac{\\partial \\vec{B}}{\\partial t}")
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 线性代数 ==========
    
    @Test
    fun testMatrixDeterminant() {
        // 2x2矩阵行列式
        val doc = parser.parse("""
            \det(A) = \begin{vmatrix}
            a & b \\
            c & d
            \end{vmatrix} = ad - bc
        """.trimIndent())
        assertTrue(doc.children.isNotEmpty())
    }
    
    // ========== 组合公式 ==========
    
    @Test
    fun testComplexNestedFormula() {
        // 复杂嵌套公式：包含分数、根号、上下标、求和
        val doc = parser.parse("""
            \sqrt{\frac{\sum_{i=1}^{n} (x_i - \bar{x})^2}{n-1}}
        """.trimIndent())
        assertTrue(doc.children[0] is LatexNode.Root)
    }
}
