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
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import com.hrm.latex.parser.util.LatexPrinter
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.parser.visitor.MathMLVisitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GroupCommandTest {
    private val parser = LatexParser()

    private fun assertEquivalent(expected: String, actual: String) {
        val reference = parser.parse(expected)
        val result = parser.parseWithDiagnostics(actual)
        assertTrue(result.diagnostics.isEmpty(), result.diagnostics.toString())
        // Compare expanded content; equivalent spellings keep different raw definition tokens.
        val expectedContent = reference.copy(children = reference.children.filterNot { it is LatexNode.NewCommand })
        val actualContent = result.document.copy(children = result.document.children.filterNot { it is LatexNode.NewCommand })
        assertEquals(LatexPrinter().visit(expectedContent), LatexPrinter().visit(actualContent))
        assertEquals(MathMLVisitor.convert(reference), MathMLVisitor.convert(result.document))
        assertEquals(AccessibilityVisitor.describe(reference), AccessibilityVisitor.describe(result.document))
    }

    @Test
    fun groupSpellingsShareExistingVisitorsAndStyleScope() {
        for ((open, close) in listOf(
            "\\begingroup" to "\\endgroup", "{" to "}"
        )) {
            assertEquivalent("{\\bf x+{\\it y}+z}+w", "$open\\bf x+{\\it y}+z$close+w")
            assertEquivalent("{}", "$open$close")
        }
    }

    @Test
    fun nestedGroupsSupportFractionsScriptsAndSourceRanges() {
        assertEquivalent("{{ x}+y}+z", "\\begingroup\\begingroup x\\endgroup+y\\endgroup+z")
        val input = "\\begingroup{ a\\over b}\\endgroup^2"
        assertEquivalent("{{ a\\over b}}^2", input)
        val script = assertIs<LatexNode.Superscript>(parser.parse(input).children.single())
        assertEquals(SourceRange(0, input.length), script.sourceRange)
        val outer = assertIs<LatexNode.Group>(script.base)
        assertEquals(SourceRange(0, input.indexOf('^')), outer.sourceRange)
        val inner = assertIs<LatexNode.Group>(outer.children.single())
        assertEquals(SourceRange(input.indexOf('{'), input.indexOf("\\endgroup")), inner.sourceRange)
    }

    @Test
    fun balancedGroupInsideMacroBodyExpandsArguments() {
        assertEquivalent(
            "\\newcommand{\\bold}[1]{{\\bf #1}}\\bold{x}+y",
            "\\newcommand{\\bold}[1]{\\begingroup\\bf #1\\endgroup}\\bold{x}+y"
        )
    }

    @Test
    fun localDefinitionsRestoreOuterMacrosForEveryGroupSpelling() {
        for ((open, close) in listOf(
            "{" to "}", "\\begingroup" to "\\endgroup"
        )) {
            val result = parser.parseWithDiagnostics(
                "\\def\\value{a}$open\\def\\value{b}\\value$close\\value"
            )
            assertTrue(result.diagnostics.isEmpty())
            val group = assertIs<LatexNode.Group>(result.document.children[1])
            assertEquals("b", assertIs<LatexNode.Text>(group.children.last()).content)
            val outside = assertIs<LatexNode.Text>(result.document.children.last())
            assertEquals("a", outside.content)
        }
    }

    @Test
    fun nestedScopesRemoveNewDefinitionsAndRestoreEachLevel() {
        val result = parser.parse("\\begingroup\\def\\local{a}{\\def\\local{b}\\local}\\local\\endgroup\\local")
        val outer = assertIs<LatexNode.Group>(result.children.first())
        val inner = assertIs<LatexNode.Group>(outer.children[1])
        assertEquals("b", assertIs<LatexNode.Text>(inner.children.last()).content)
        assertEquals("a", assertIs<LatexNode.Text>(outer.children.last()).content)
        assertEquals("local", assertIs<LatexNode.Command>(result.children.last()).name)
    }

    @Test
    fun localEnvironmentDefinitionsAreRestored() {
        val input = """\newenvironment{outer}{a}{b}\begingroup\renewenvironment{outer}{c}{d}\newenvironment{local}{x}{y}\endgroup"""
        val session = ParseSession(LatexTokenizer(input).tokenize(), input.length)
        session.parse()
        assertEquals(setOf("outer"), session.customEnvironments.keys)
        assertEquals("a", assertIs<com.hrm.latex.parser.tokenizer.LatexToken.Text>(session.customEnvironments.getValue("outer").beginTokens.single()).content)
    }

    @Test
    fun incompleteAndMismatchedGroupsReportDiagnosticsWithoutLosingFollowingText() {
        for (input in listOf("\\begingroup x", "\\begingroup x}", "{x\\endgroup")) {
            val result = parser.parseWithDiagnostics(input)
            assertTrue(result.diagnostics.any { it.category == ParseDiagnostic.Category.MISSING_BRACE }, input)
            assertTrue(AccessibilityVisitor.describe(result.document).contains("x"))
        }
        for (close in listOf("\\endgroup")) {
            val result = parser.parseWithDiagnostics("$close+x")
            assertEquals(ParseDiagnostic.Category.UNEXPECTED_TOKEN, result.errors.single().category)
            assertEquals(SourceRange(0, close.length), result.errors.single().range)
            assertTrue(AccessibilityVisitor.describe(result.document).contains("x"))
        }
        val recovered = parser.parseWithDiagnostics("{\\begingroup x}+y")
        assertEquals(1, recovered.errors.size)
        assertTrue(AccessibilityVisitor.describe(recovered.document).contains("y"))
    }

    @Test
    fun incrementalCharacterInputMatchesFullParse() {
        val input = "\\begingroup\\def\\a{x}\\a\\endgroup+y"
        val incremental = IncrementalLatexParser()
        input.forEach { incremental.append(it.toString()) }
        assertEquals(parser.parse(input), incremental.getCurrentDocument())
    }
}
