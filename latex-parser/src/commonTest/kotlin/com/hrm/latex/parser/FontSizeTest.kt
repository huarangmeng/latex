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

class FontSizeTest {
    private val parser = LatexParser()

    @Test
    fun testAllFontSizeCommands() {
        val input = "\\tiny a \\scriptsize b \\footnotesize c \\small d \\normalsize e " +
            "\\large f \\Large g \\LARGE h \\huge i \\Huge j"
        val doc = parser.parse(input)

        val sizes = doc.children.filterIsInstance<LatexNode.FontSize>().map { it.sizeType }

        assertEquals(
            listOf(
                LatexNode.FontSize.SizeType.TINY,
                LatexNode.FontSize.SizeType.SCRIPT_SIZE,
                LatexNode.FontSize.SizeType.FOOTNOTE_SIZE,
                LatexNode.FontSize.SizeType.SMALL,
                LatexNode.FontSize.SizeType.NORMAL_SIZE,
                LatexNode.FontSize.SizeType.LARGE,
                LatexNode.FontSize.SizeType.LARGE_2,
                LatexNode.FontSize.SizeType.LARGE_3,
                LatexNode.FontSize.SizeType.HUGE,
                LatexNode.FontSize.SizeType.HUGE_2,
            ),
            sizes
        )
    }

    @Test
    fun testFontSizeDeclarationScopesToFollowingGroupContent() {
        val doc = parser.parse("{\\small x + y}")
        val group = assertIs<LatexNode.Group>(doc.children[0])
        val fontSize = assertIs<LatexNode.FontSize>(group.children[0])

        assertEquals(LatexNode.FontSize.SizeType.SMALL, fontSize.sizeType)
        assertTrue(fontSize.content.isNotEmpty())
        assertEquals("x", assertIs<LatexNode.Text>(fontSize.content[0]).content)
        assertEquals("y", assertIs<LatexNode.Text>(fontSize.content.last()).content)
    }

    @Test
    fun testExplicitGroupArgument() {
        val doc = parser.parse("\\Huge{x + y}")
        val fontSize = assertIs<LatexNode.FontSize>(doc.children[0])

        assertEquals(LatexNode.FontSize.SizeType.HUGE_2, fontSize.sizeType)
        assertIs<LatexNode.Group>(fontSize.content[0])
    }

    @Test
    fun testBracedContentDoesNotEndDeclarationScope() {
        val doc = parser.parse("{\\small{x} y}")
        val group = assertIs<LatexNode.Group>(doc.children[0])
        val fontSize = assertIs<LatexNode.FontSize>(group.children[0])
        val content = fontSize.content

        assertEquals(LatexNode.FontSize.SizeType.SMALL, fontSize.sizeType)
        assertEquals(3, content.size)
        assertIs<LatexNode.Group>(content[0])
        assertEquals("y", (content[2] as LatexNode.Text).content)
    }

    @Test
    fun testFontSizeSourceRangeCoversScopedContent() {
        val input = "{\\small x + y}"
        val doc = parser.parse(input)
        val group = assertIs<LatexNode.Group>(doc.children[0])
        val fontSize = assertIs<LatexNode.FontSize>(group.children[0])
        val range = fontSize.sourceRange

        assertTrue(range != null)
        assertEquals(1, range.start)
        assertEquals(input.length - 1, range.end)
    }

    @Test
    fun testFontSizeDeclarationInEquationEnvironment() {
        val doc = parser.parse("\\begin{equation}\\small x\\end{equation}")
        val environment = assertIs<LatexNode.Environment>(doc.children[0])
        val fontSize = assertIs<LatexNode.FontSize>(environment.content[0])

        assertEquals(LatexNode.FontSize.SizeType.SMALL, fontSize.sizeType)
        val content = assertIs<LatexNode.Text>(fontSize.content[0])
        assertEquals("x", content.content)
    }

    @Test
    fun testFontSizeDeclarationInMatrixCell() {
        val doc = parser.parse("\\begin{matrix}\\small x & y\\end{matrix}")
        val matrix = assertIs<LatexNode.Matrix>(doc.children[0])
        val firstCell = assertIs<LatexNode.Group>(matrix.rows[0][0])
        val fontSize = assertIs<LatexNode.FontSize>(firstCell.children[0])

        assertEquals(LatexNode.FontSize.SizeType.SMALL, fontSize.sizeType)
        assertEquals("x", assertIs<LatexNode.Text>(fontSize.content[0]).content)
    }

    @Test
    fun testFontSizeDeclarationInCasesExpression() {
        val doc = parser.parse("\\begin{cases}\\small x & x > 0\\end{cases}")
        val cases = assertIs<LatexNode.Cases>(doc.children[0])
        val expression = assertIs<LatexNode.Group>(cases.cases[0].first)
        val fontSize = assertIs<LatexNode.FontSize>(expression.children[0])

        assertEquals(LatexNode.FontSize.SizeType.SMALL, fontSize.sizeType)
        assertEquals("x", assertIs<LatexNode.Text>(fontSize.content[0]).content)
    }
}
