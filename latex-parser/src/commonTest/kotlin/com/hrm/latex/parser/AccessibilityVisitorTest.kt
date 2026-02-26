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

import com.hrm.latex.parser.visitor.AccessibilityVisitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccessibilityVisitorTest {

    private val parser = LatexParser()

    @Test
    fun testSimpleText() {
        val doc = parser.parse("hello")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("hello"))
    }

    @Test
    fun testFraction() {
        val doc = parser.parse("\\frac{1}{2}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("fraction"))
        assertTrue(desc.contains("1"))
        assertTrue(desc.contains("over"))
        assertTrue(desc.contains("2"))
    }

    @Test
    fun testSuperscriptSquared() {
        val doc = parser.parse("x^2")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("squared"))
    }

    @Test
    fun testSuperscriptCubed() {
        val doc = parser.parse("x^3")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("cubed"))
    }

    @Test
    fun testSuperscriptGeneral() {
        val doc = parser.parse("x^n")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("to the power of"))
    }

    @Test
    fun testSubscript() {
        val doc = parser.parse("a_i")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("sub"))
    }

    @Test
    fun testSquareRoot() {
        val doc = parser.parse("\\sqrt{x}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("square root"))
    }

    @Test
    fun testNthRoot() {
        val doc = parser.parse("\\sqrt[3]{x}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("root"))
        assertTrue(desc.contains("3"))
    }

    @Test
    fun testBigOperator() {
        val doc = parser.parse("\\sum_{i=1}^{n} i")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("sum"))
        assertTrue(desc.contains("from"))
        assertTrue(desc.contains("to"))
    }

    @Test
    fun testIntegral() {
        val doc = parser.parse("\\int_0^1 x dx")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("integral"))
    }

    @Test
    fun testGreekSymbol() {
        val doc = parser.parse("\\alpha + \\beta")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("alpha"))
        assertTrue(desc.contains("beta"))
    }

    @Test
    fun testMatrix() {
        val doc = parser.parse("\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("matrix"))
        assertTrue(desc.contains("2 by 2"))
    }

    @Test
    fun testCases() {
        val doc = parser.parse("\\begin{cases} 1 & x > 0 \\\\ 0 & x = 0 \\end{cases}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("cases"))
    }

    @Test
    fun testBinomial() {
        val doc = parser.parse("\\binom{n}{k}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("choose"))
    }

    @Test
    fun testAccentHat() {
        val doc = parser.parse("\\hat{x}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("hat"))
    }

    @Test
    fun testAccentVec() {
        val doc = parser.parse("\\vec{v}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("vector"))
    }

    @Test
    fun testDelimited() {
        val doc = parser.parse("\\left( x \\right)")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("paren"))
    }

    @Test
    fun testExtensibleArrow() {
        val doc = parser.parse("\\xrightarrow{f}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("right arrow"))
    }

    @Test
    fun testHookArrow() {
        val doc = parser.parse("\\xhookrightarrow{f}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("hook right arrow"))
    }

    @Test
    fun testTensor() {
        val doc = parser.parse("\\tensor{T}{^a_b}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("upper"))
        assertTrue(desc.contains("lower"))
    }

    @Test
    fun testSideSet() {
        val doc = parser.parse("\\sideset{_a^b}{_c^d}{\\sum}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("left sub"))
        assertTrue(desc.contains("right sub"))
    }

    @Test
    fun testLabel() {
        val doc = parser.parse("\\label{eq:1}")
        val desc = AccessibilityVisitor.describe(doc)
        // label produces empty output
        assertEquals("", desc)
    }

    @Test
    fun testRef() {
        val doc = parser.parse("\\ref{eq:1}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("reference"))
        assertTrue(desc.contains("eq:1"))
    }

    @Test
    fun testEqRef() {
        val doc = parser.parse("\\eqref{eq:1}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("eq:1"))
    }

    @Test
    fun testBoxed() {
        val doc = parser.parse("\\boxed{x}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("boxed"))
    }

    @Test
    fun testEmptyDocument() {
        val doc = parser.parse("")
        val desc = AccessibilityVisitor.describe(doc)
        assertEquals("", desc)
    }

    @Test
    fun testComplexExpression() {
        val doc = parser.parse("E = mc^2")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("squared"))
        assertTrue(desc.isNotEmpty())
    }

    @Test
    fun testTabular() {
        val doc = parser.parse("\\begin{tabular}{cc} a & b \\\\ c & d \\end{tabular}")
        val desc = AccessibilityVisitor.describe(doc)
        assertTrue(desc.contains("table"))
        assertTrue(desc.contains("2 by 2"))
    }
}
