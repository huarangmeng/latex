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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SyntaxExpansionTest {
    private val parser = LatexParser()

    @Test
    fun parsesGeneralizedAndInfixFractions() {
        val generalized = parser.parse("\\genfrac{(}{)}{0pt}{0}{a}{b}")
        val delimited = assertIs<LatexNode.Delimited>(generalized.children.single())
        assertEquals("(", delimited.left)
        assertEquals(")", delimited.right)
        val fraction = assertIs<LatexNode.Fraction>(delimited.content.single())
        assertEquals(LatexNode.Fraction.FractionStyle.RULELESS, fraction.style)

        assertIs<LatexNode.Fraction>(parser.parse("{a \\over b}").descendants().first { it is LatexNode.Fraction })
        assertIs<LatexNode.Binomial>(parser.parse("{n \\choose k}").descendants().first { it is LatexNode.Binomial })
        val above = assertIs<LatexNode.Fraction>(
            parser.parse("{a \\above 0pt b}").descendants().first { it is LatexNode.Fraction }
        )
        assertEquals(LatexNode.Fraction.FractionStyle.RULELESS, above.style)
    }

    @Test
    fun parsesMiddleAndAdditionalAccents() {
        val doc = parser.parse(
            "\\left\\langle x \\middle| y \\right\\rangle " +
                "\\widecheck{x}\\overleftrightarrow{AB}\\underleftarrow{x}\\underrightarrow{x}" +
                "\\overparen{ab}\\underparen{ab}"
        )
        assertTrue(doc.descendants().any {
            it is LatexNode.ManualSizedDelimiter && it.delimiter == "|" && it.isMiddle
        })
        val types = doc.descendants().filterIsInstance<LatexNode.Accent>().map { it.accentType }.toSet()
        assertTrue(LatexNode.Accent.AccentType.WIDECHECK in types)
        assertTrue(LatexNode.Accent.AccentType.OVERLEFTRIGHTARROW in types)
        assertTrue(LatexNode.Accent.AccentType.UNDERLEFTARROW in types)
        assertTrue(LatexNode.Accent.AccentType.UNDERRIGHTARROW in types)
        assertTrue(LatexNode.Accent.AccentType.OVERPAREN in types)
        assertTrue(LatexNode.Accent.AccentType.UNDERPAREN in types)
    }

    @Test
    fun parsesAmsEnvironmentVariantsAndRowCommands() {
        val matrix = assertIs<LatexNode.Matrix>(
            parser.parse("\\begin{pmatrix*}[r]a&b\\\\c&d\\end{pmatrix*}").children.single()
        )
        assertEquals("r", matrix.alignment)
        assertEquals(LatexNode.Matrix.MatrixType.PAREN, matrix.type)

        assertTrue(parser.parse("\\begin{alignedat}{2}a&=b&c&=d\\end{alignedat}").descendants()
            .any { it is LatexNode.Aligned })
        assertTrue(parser.parse("\\begin{subarray}{c}i=1\\\\j=2\\end{subarray}").descendants()
            .any { it is LatexNode.Array })
        assertTrue(parser.parse("\\begin{cases*}x&if y\\end{cases*}").descendants()
            .any { it is LatexNode.Cases })
        val crMatrix = assertIs<LatexNode.Matrix>(
            parser.parse("\\begin{matrix}a&b\\cr c&d\\end{matrix}").children.single()
        )
        assertEquals(2, crMatrix.rows.size)

        val controls = parser.parse("a\\cr b\\notag\\intertext{where}c")
        assertTrue(controls.descendants().any { it is LatexNode.NewLine })
        assertTrue(controls.descendants().any { it is LatexNode.Tag && it.starred && it.label.children().isEmpty() })
        assertTrue(controls.descendants().any { it is LatexNode.TextMode && it.text == "where" })
    }

    @Test
    fun parsesLayoutAndTextAliases() {
        val doc = parser.parse(
            "\\kern1em\\mkern-2mu\\allowbreak\\raisebox{2pt}{x}\\rule[1pt]{3em}{.4pt}" +
                "\\llap{a}\\rlap{b}\\clap{c}" +
                "\\textnormal{n}\\textup{u}\\textmd{m}\\textsc{s}\\textsl{l}\\emph{e}"
        )
        val spaces = doc.descendants().filterIsInstance<LatexNode.HSpace>().map { it.dimension }
        assertTrue("1em" in spaces)
        assertTrue("-2mu" in spaces)
        assertTrue("0pt" in spaces)
        val layouts = doc.descendants().filterIsInstance<LatexNode.Layout>()
        assertTrue(layouts.any { it.layoutType == LatexNode.Layout.LayoutType.RAISE_BOX && it.shift == "2pt" })
        assertTrue(layouts.any { it.layoutType == LatexNode.Layout.LayoutType.RULE && it.width == "3em" })
        assertEquals(3, doc.descendants().filterIsInstance<LatexNode.MathLap>().size)
        assertTrue(doc.descendants().filterIsInstance<LatexNode.Style>().size >= 6)
    }

    @Test
    fun parsesSmallCapsWithArgumentAndDeclarationScopes() {
        for (source in listOf("""\textsc{Abc}def""", """{\sc Abc}def""", """{\scshape Abc}def""")) {
            val result = parser.parseWithDiagnostics(source)
            assertTrue(result.diagnostics.isEmpty(), source)
            val (styled, following) = result.document.children
            val style = styled.descendants().filterIsInstance<LatexNode.Style>().single()
            assertEquals(LatexNode.Style.StyleType.SMALL_CAPS, style.styleType, source)
            assertEquals("Abc", style.descendants().filterIsInstance<LatexNode.Text>().single().content)
            assertEquals("def", assertIs<LatexNode.Text>(following).content)
        }
    }

    @Test
    fun parsesMathtoolsPhysicsAndSiunitxSubset() {
        val paired = parser.parse(
            "\\DeclarePairedDelimiter{\\set}{\\lbrace}{\\rbrace}" +
                "\\set{x}\\set*{y}\\set[\\Big]{z}"
        )
        val delimiters = paired.children.drop(1)
            .flatMap { it.descendants() }
            .filterIsInstance<LatexNode.Delimited>()
        assertEquals(3, delimiters.size)
        assertEquals(listOf("x", "y", "z"), delimiters.map { delimiterNode ->
            delimiterNode.descendants().filterIsInstance<LatexNode.Text>().joinToString("") { it.content }
        })
        val delimiter = delimiters.last()
        assertEquals("{", delimiter.left)
        assertEquals("}", delimiter.right)
        assertTrue(delimiter.descendants().any { it is LatexNode.Text && it.content == "z" })

        assertTrue(parser.parse("\\coloneqq\\eqqcolon").children.all { it is LatexNode.Symbol })
        assertIs<LatexNode.Boxed>(parser.parse("\\Aboxed{x}").children.single())
        assertTrue(parser.parse("\\MoveEqLeft[3]x").children.first() is LatexNode.HSpace)
        assertTrue(parser.parse("\\splitfrac{a}{b}").children.single() is LatexNode.Fraction)

        val physics = parser.parse("\\Bra{x}\\Ket{y}\\Braket{x|y}\\comm{A}{B}\\anticomm{A}{B}\\eval{f}\\vb{v}")
        assertTrue(physics.descendants().filterIsInstance<LatexNode.Delimited>().size >= 6)
        assertTrue(physics.descendants().any { it is LatexNode.Style && it.styleType == LatexNode.Style.StyleType.BOLD_SYMBOL })

        val units = parser.parse("\\qty{1e3}{\\kilo\\metre\\per\\second}\\unit{\\newton}\\numrange{1}{3}\\qtyrange{2}{4}{\\metre}\\ang{90}\\pu{kg.m/s^2}")
        assertTrue(units.descendants().any { it is LatexNode.Superscript && it.base is LatexNode.Text && it.base.content == "10" })
        assertTrue(units.descendants().filterIsInstance<LatexNode.Style>().isNotEmpty())
        assertTrue(units.descendants().any { it is LatexNode.Symbol && it.unicode == "°" })
    }

    @Test
    fun parsesMhchemArrowAnnotationsAndBonds() {
        val chemistry = parser.parse("\\ce{C-C=C#N ->[heat][cat.] CO2}")
        val arrow = chemistry.descendants().filterIsInstance<LatexNode.ExtensibleArrow>().single()
        assertTrue(arrow.below != null)
        val symbols = chemistry.descendants().filterIsInstance<LatexNode.Symbol>().map { it.unicode }
        assertTrue("−" in symbols)
        assertTrue("=" in symbols)
        assertTrue("≡" in symbols)
    }

    @Test
    fun formatsPhysicalUnitsWithoutReplacingDecimalPoints() {
        val physicalUnit = parser.parse("\\pu{1.2e3 kg.m/s^2}")
        val texts = physicalUnit.descendants().filterIsInstance<LatexNode.Text>().map { it.content }
        val joinedText = texts.joinToString("")

        assertTrue("1.2" in texts)
        assertTrue(texts.none { "1·2" in it })
        assertTrue("kg·m/s" in joinedText)
        assertTrue(physicalUnit.descendants().any {
            it is LatexNode.Symbol && it.symbol == "times" && it.unicode == "×"
        })
        assertTrue(physicalUnit.descendants().any {
            it is LatexNode.Superscript &&
                it.base is LatexNode.Text && it.base.content == "10" &&
                it.exponent is LatexNode.Text && it.exponent.content == "3"
        })
    }
}

private fun LatexNode.descendants(): List<LatexNode> = buildList {
    add(this@descendants)
    this@descendants.children().forEach { addAll(it.descendants()) }
}
