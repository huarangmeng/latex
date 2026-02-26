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

import com.hrm.latex.parser.visitor.MathMLVisitor
import kotlin.test.Test
import kotlin.test.assertTrue

class MathMLVisitorTest {

    private val parser = LatexParser()

    @Test
    fun testMathMLWrapper() {
        val doc = parser.parse("x")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.startsWith("<math xmlns=\"http://www.w3.org/1998/Math/MathML\""))
        assertTrue(result.endsWith("</math>"))
    }

    @Test
    fun testDisplayMode() {
        val doc = parser.parse("x")
        val block = MathMLVisitor.convert(doc, displayMode = true)
        assertTrue(block.contains("display=\"block\""))

        val inline = MathMLVisitor.convert(doc, displayMode = false)
        assertTrue(inline.contains("display=\"inline\""))
    }

    @Test
    fun testTextNode() {
        val doc = parser.parse("abc123")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mi>a</mi>"))
        assertTrue(result.contains("<mi>b</mi>"))
        assertTrue(result.contains("<mi>c</mi>"))
        assertTrue(result.contains("<mn>1</mn>"))
        assertTrue(result.contains("<mn>2</mn>"))
        assertTrue(result.contains("<mn>3</mn>"))
    }

    @Test
    fun testFraction() {
        val doc = parser.parse("\\frac{1}{2}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mfrac>"))
        assertTrue(result.contains("</mfrac>"))
    }

    @Test
    fun testSuperscript() {
        val doc = parser.parse("x^2")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<msup>"))
    }

    @Test
    fun testSubscript() {
        val doc = parser.parse("a_i")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<msub>"))
    }

    @Test
    fun testSquareRoot() {
        val doc = parser.parse("\\sqrt{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<msqrt>"))
    }

    @Test
    fun testNthRoot() {
        val doc = parser.parse("\\sqrt[3]{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mroot>"))
    }

    @Test
    fun testSymbol() {
        val doc = parser.parse("\\alpha")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mi>") || result.contains("<mo>"))
    }

    @Test
    fun testOperator() {
        val doc = parser.parse("a + b")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mo>+</mo>"))
    }

    @Test
    fun testMatrix() {
        val doc = parser.parse("\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mtable>"))
        assertTrue(result.contains("<mtr>"))
        assertTrue(result.contains("<mtd>"))
        assertTrue(result.contains("<mo>(</mo>"))
        assertTrue(result.contains("<mo>)</mo>"))
    }

    @Test
    fun testBigOperator() {
        val doc = parser.parse("\\sum_{i=1}^{n} i")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<munderover>"))
    }

    @Test
    fun testDelimited() {
        val doc = parser.parse("\\left( x \\right)")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("stretchy"))
    }

    @Test
    fun testAccent() {
        val doc = parser.parse("\\hat{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mover>"))
    }

    @Test
    fun testCancel() {
        val doc = parser.parse("\\cancel{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<menclose"))
        assertTrue(result.contains("updiagonalstrike"))
    }

    @Test
    fun testStyle() {
        val doc = parser.parse("\\mathbf{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mstyle"))
        assertTrue(result.contains("bold"))
    }

    @Test
    fun testColor() {
        val doc = parser.parse("\\color{red}{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("mathcolor"))
        assertTrue(result.contains("red"))
    }

    @Test
    fun testCases() {
        val doc = parser.parse("\\begin{cases} 1 & x > 0 \\\\ 0 & x = 0 \\end{cases}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mo>{</mo>"))
        assertTrue(result.contains("<mtable>"))
    }

    @Test
    fun testBinomial() {
        val doc = parser.parse("\\binom{n}{k}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mfrac"))
        assertTrue(result.contains("linethickness=\"0\""))
    }

    @Test
    fun testTextMode() {
        val doc = parser.parse("\\text{hello}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mtext>hello</mtext>"))
    }

    @Test
    fun testSpace() {
        val doc = parser.parse("a \\quad b")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mspace"))
    }

    @Test
    fun testBoxed() {
        val doc = parser.parse("\\boxed{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<menclose"))
        assertTrue(result.contains("box"))
    }

    @Test
    fun testPhantom() {
        val doc = parser.parse("\\phantom{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mphantom>"))
    }

    @Test
    fun testExtensibleArrow() {
        val doc = parser.parse("\\xrightarrow{f}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("→"))
    }

    @Test
    fun testHookArrow() {
        val doc = parser.parse("\\xhookrightarrow{f}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("↪"))
    }

    @Test
    fun testSideSet() {
        val doc = parser.parse("\\sideset{_a^b}{_c^d}{\\sum}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mmultiscripts>"))
        assertTrue(result.contains("<mprescripts/>"))
    }

    @Test
    fun testTensor() {
        val doc = parser.parse("\\tensor{T}{^a_b}")
        val result = MathMLVisitor.convert(doc)
        // tensor creates msub/msup/msubsup elements
        assertTrue(result.contains("<msub") || result.contains("<msup") || result.contains("<msubsup"))
    }

    @Test
    fun testTabular() {
        val doc = parser.parse("\\begin{tabular}{cc} a & b \\\\ c & d \\end{tabular}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mtable>"))
    }

    @Test
    fun testRef() {
        val doc = parser.parse("\\ref{eq:1}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mtext>eq:1</mtext>"))
    }

    @Test
    fun testEqRef() {
        val doc = parser.parse("\\eqref{eq:1}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mo>(</mo>"))
        assertTrue(result.contains("<mtext>eq:1</mtext>"))
    }

    @Test
    fun testXmlEscaping() {
        val doc = parser.parse("a < b")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("&lt;"))
    }

    @Test
    fun testEmptyDocument() {
        val doc = parser.parse("")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.startsWith("<math"))
        assertTrue(result.endsWith("</math>"))
    }

    @Test
    fun testSmash() {
        val doc = parser.parse("\\smash{x}")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<mpadded"))
    }

    @Test
    fun testNegation() {
        val doc = parser.parse("\\not= b")
        val result = MathMLVisitor.convert(doc)
        assertTrue(result.contains("<menclose"))
    }
}
