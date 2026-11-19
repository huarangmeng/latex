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

class ArgumentAtomTest {

    private val parser = LatexParser()

    @Test
    fun unbracedFractionArgumentsConsumeOneTextAtomEach() {
        val document = parser.parse("\\frac12x")

        assertEquals(2, document.children.size)
        val fraction = assertIs<LatexNode.Fraction>(document.children[0])
        assertEquals("1", assertIs<LatexNode.Text>(fraction.numerator).content)
        assertEquals(SourceRange(5, 6), fraction.numerator.sourceRange)
        assertEquals("2", assertIs<LatexNode.Text>(fraction.denominator).content)
        assertEquals(SourceRange(6, 7), fraction.denominator.sourceRange)
        assertEquals(SourceRange(0, 7), fraction.sourceRange)
        assertEquals("x", assertIs<LatexNode.Text>(document.children[1]).content)
        assertEquals(SourceRange(7, 8), document.children[1].sourceRange)
    }

    @Test
    fun unbracedBinomialArgumentsConsumeOneTextAtomEach() {
        val document = parser.parse("\\binom12x")

        assertEquals(2, document.children.size)
        val binomial = assertIs<LatexNode.Binomial>(document.children[0])
        assertEquals("1", assertIs<LatexNode.Text>(binomial.top).content)
        assertEquals("2", assertIs<LatexNode.Text>(binomial.bottom).content)
        assertEquals("x", assertIs<LatexNode.Text>(document.children[1]).content)
    }

    @Test
    fun unbracedRootArgumentPreservesFollowingUnicodeText() {
        val document = parser.parse("\\sqrt😀x")

        assertEquals(2, document.children.size)
        val root = assertIs<LatexNode.Root>(document.children[0])
        assertEquals("😀", assertIs<LatexNode.Text>(root.content).content)
        assertEquals(SourceRange(5, 7), root.content.sourceRange)
        assertEquals("x", assertIs<LatexNode.Text>(document.children[1]).content)
        assertEquals(SourceRange(7, 8), document.children[1].sourceRange)
    }

    @Test
    fun unbracedArgumentsAcceptControlSequencesAndSkipWhitespace() {
        val commandDocument = parser.parse("\\frac\\alpha\\beta")
        val commandFraction = assertIs<LatexNode.Fraction>(commandDocument.children.single())
        assertEquals("alpha", assertIs<LatexNode.Symbol>(commandFraction.numerator).symbol)
        assertEquals("beta", assertIs<LatexNode.Symbol>(commandFraction.denominator).symbol)

        val spacedDocument = parser.parse("\\frac 1 2")
        val spacedFraction = assertIs<LatexNode.Fraction>(spacedDocument.children.single())
        assertEquals("1", assertIs<LatexNode.Text>(spacedFraction.numerator).content)
        assertEquals("2", assertIs<LatexNode.Text>(spacedFraction.denominator).content)
    }

    @Test
    fun bigOperatorScriptsConsumeOneTextAtom() {
        val document = parser.parse("\\int_12\\sum^n2")

        assertEquals(4, document.children.size)
        val integral = assertIs<LatexNode.BigOperator>(document.children[0])
        assertEquals("1", assertIs<LatexNode.Text>(integral.subscript).content)
        assertEquals("2", assertIs<LatexNode.Text>(document.children[1]).content)

        val sum = assertIs<LatexNode.BigOperator>(document.children[2])
        assertEquals("n", assertIs<LatexNode.Text>(sum.superscript).content)
        assertEquals("2", assertIs<LatexNode.Text>(document.children[3]).content)
    }

    @Test
    fun regularScriptsAndBigOperatorScriptsShareWhitespaceBehavior() {
        val document = parser.parse("x_ 1y\\int_ 2z")

        assertEquals(4, document.children.size)
        val regular = assertIs<LatexNode.Subscript>(document.children[0])
        assertEquals("1", assertIs<LatexNode.Text>(regular.index).content)
        assertEquals("y", assertIs<LatexNode.Text>(document.children[1]).content)

        val integral = assertIs<LatexNode.BigOperator>(document.children[2])
        assertEquals("2", assertIs<LatexNode.Text>(integral.subscript).content)
        assertEquals("z", assertIs<LatexNode.Text>(document.children[3]).content)
    }

    @Test
    fun unbracedVerbArgumentIsOneAtom() {
        for (verb in listOf("", "a", "ab")) {
            val fraction = assertIs<LatexNode.Fraction>(parser.parse("\\frac\\verb|$verb|c").children.single())
            assertEquals(verb, assertIs<LatexNode.TextMode>(fraction.numerator).text)
            assertEquals("c", assertIs<LatexNode.Text>(fraction.denominator).content)
            val script = assertIs<LatexNode.Superscript>(parser.parse("x^\\verb|$verb|").children.single())
            assertEquals(verb, assertIs<LatexNode.TextMode>(script.exponent).text)
        }
        // The parser runs an unterminated \verb to the end of the input.
        val accent = assertIs<LatexNode.Accent>(parser.parse("\\hat\\verb|ab").children.single())
        assertEquals("ab", assertIs<LatexNode.TextMode>(accent.content).text)
    }

    @Test
    fun unbracedVerbArgumentReadsLikeItsBracedForm() {
        assertEquals("x", assertIs<LatexNode.Text>(parser.parse("\\operatorname\\verb|P|x").children.last()).content)
        for (verb in listOf("", "P", "ab")) {
            assertEquals(
                only<LatexNode.OperatorName>("\\operatorname{$verb}x")?.name,
                only<LatexNode.OperatorName>("\\operatorname\\verb|$verb|x")?.name,
            )
        }
        assertEquals("P", only<LatexNode.OperatorName>("\\operatorname\\verb|P|x")?.name)

        val multicolumn = only<LatexNode.Multicolumn>("\\multicolumn\\verb|2|\\verb|l|x")!!
        assertEquals(2 to "l", multicolumn.columnCount to multicolumn.alignment)
        for ((verb, columns) in listOf("2" to (2 to 2), "2-3" to (2 to 3))) {
            val braced = only<LatexNode.CLine>("\\cline{$verb}")!!
            val cline = only<LatexNode.CLine>("\\cline\\verb|$verb|")!!
            assertEquals(columns, braced.startCol to braced.endCol)
            assertEquals(columns, cline.startCol to cline.endCol)
        }

        val red = "\\definecolor{r}{HTML}{ff0000}"
        val braced = only<LatexNode.ColorBox>("$red\\colorbox{r}x")!!
        assertEquals(braced.backgroundColor, only<LatexNode.ColorBox>("$red\\colorbox\\verb|r|x")!!.backgroundColor)
        val framed = only<LatexNode.ColorBox>("$red\\fcolorbox\\verb|r|\\verb|r|x")!!
        assertEquals(braced.backgroundColor to braced.backgroundColor, framed.borderColor to framed.backgroundColor)

        assertEquals("ab", only<LatexNode.Label>("\\label\\verb|ab|")?.key)
        assertEquals("ab", only<LatexNode.Ref>("\\ref\\verb|ab|")?.key)
        assertEquals("ab", only<LatexNode.EqRef>("\\eqref\\verb|ab|")?.key)
        assertEquals("2em", only<LatexNode.HSpace>("\\hspace\\verb|2em|")?.dimension)
    }

    private inline fun <reified T : LatexNode> only(input: String): T? =
        parser.parse(input).children.filterIsInstance<T>().singleOrNull()
}
