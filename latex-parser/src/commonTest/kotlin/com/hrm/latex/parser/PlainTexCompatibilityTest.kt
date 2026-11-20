package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.component.handler.ParseUtils
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.parser.visitor.MathMLVisitor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlainTexCompatibilityTest {

    @Test
    fun characterCodesSupportBasesAndSupplementaryCodePoints() {
        val pairs = mapOf(
            """\char37""" to "%",
            """\char'45""" to "%",
            """\char"263a""" to "☺",
            """\char`\%""" to "%",
            """\char"1d7d9""" to "𝟙",
        )
        for ((formula, expected) in pairs) {
            val node = assertIs<LatexNode.TextMode>(parse(formula).children.single())
            assertEquals(expected, node.text, formula)
        }
        assertTrue(LatexParser().parseWithDiagnostics("""\char"d800""").errors.isNotEmpty())
    }

    @Test
    fun skipsAndNonBreakingSpaceUseExistingSpacingNodes() {
        val skips = parse("""\hskip1em \mskip{2mu}""").children.filterIsInstance<LatexNode.HSpace>()
        assertEquals(listOf("1em", "2mu"), skips.map { it.dimension })
        val space = assertIs<LatexNode.Space>(parse("""\nobreakspace""").children.single())
        assertEquals(LatexNode.Space.SpaceType.NON_BREAKING, space.type)
    }
    private val parser = LatexParser()

    @Test
    fun parsesPlainTexMathCharacters() {
        val document = parse("""\surd\ldotp\colon\doteq\relbar\Relbar""")
        assertEquals(listOf("√", ".", ":", "≐", "−", "="),
            document.children.map { assertIs<LatexNode.Symbol>(it).unicode })
    }

    @Test
    fun integralOperatorsDefaultToLimitsAndAllowOverrides() {
        for (name in listOf("smallint", "intop", "ointop")) {
            val operator = assertIs<LatexNode.BigOperator>(parse("\\$name\\sb{a}\\sp{b}").children.single())
            assertEquals(name, operator.operator)
            assertEquals(LatexNode.BigOperator.LimitsMode.LIMITS, operator.limitsMode)
            assertNotNull(operator.subscript)
            assertNotNull(operator.superscript)
            val side = assertIs<LatexNode.BigOperator>(parse("\\$name\\nolimits_0^1").children.single())
            assertEquals(LatexNode.BigOperator.LimitsMode.NOLIMITS, side.limitsMode)
            assertContains(MathMLVisitor.convert(parse("\\${name}_0^1")), if (name == "ointop") "∮" else "∫")
            assertContains(MathMLVisitor.convert(parse("\\$name\\nolimits_0^1")), "<msubsup>")
            assertContains(AccessibilityVisitor.describe(parse("\\${name}_0^1")), "integral")
        }
    }

    @Test
    fun scriptAliasesWorkThroughMacrosAndLetWithSourceRanges() {
        val source = """\def\power{\sp}\let\index=\sb x\index i\power 23"""
        val document = parse(source)
        val scripted = document.children.filterIsInstance<LatexNode.Superscript>().single()
        assertIs<LatexNode.Subscript>(scripted.base)
        assertEquals("2", assertIs<LatexNode.Text>(scripted.exponent).content)
        assertEquals("3", assertIs<LatexNode.Text>(document.children.last()).content)
        assertEquals(source.indexOf("x\\index"), scripted.sourceRange?.start)
        assertEquals(source.indexOf("23") + 1, scripted.sourceRange?.end)
        assertTrue(parse("""\def\sp{+}x\sp y""").children.none { it is LatexNode.Superscript })
    }

    @Test
    fun bracketAndBraceInfixFormsKeepNestedGroupsAndScripts() {
        for ((command, delimiters) in listOf("brack" to ("[" to "]"), "brace" to ("{" to "}"))) {
            val document = parse("{n+1 \\$command k_2}^3")
            val delimited = document.nodes<LatexNode.Delimited>().single()
            assertEquals(delimiters, delimited.left to delimited.right)
            val fraction = assertIs<LatexNode.Fraction>(delimited.content.single())
            assertEquals(LatexNode.Fraction.FractionStyle.RULELESS, fraction.style)
            assertEquals(1, fraction.denominator.nodes<LatexNode.Subscript>().size)
            assertIs<LatexNode.Superscript>(document.children.single())
            assertContains(MathMLVisitor.convert(document), "linethickness=\"0\"")
        }
        assertEquals(2, parse("""{n\brack {k\brace j}}""").nodes<LatexNode.Delimited>().size)
    }

    @Test
    fun joinrelIsANegativeRelationKernAndSkewMovesOnlyTheAccent() {
        val relation = parse("""a\relbar\joinrel\rightarrow b""").nodes<LatexNode.MathClass>().single()
        assertEquals(LatexNode.MathClass.AtomClass.REL, relation.atomClass)
        assertEquals("-3mu", assertIs<LatexNode.HSpace>(relation.content.single()).dimension)
        assertContains(MathMLVisitor.convert(relation), "em\"")
        val accents = parse("""\skew3\hat A\skew{-4}{\vec}{x}\def\cap#1{\hat{#1}}\skew{2}\cap y""")
            .nodes<LatexNode.Accent>()
        assertEquals(listOf(1.5f, -2f, 1f), accents.map { it.skewMu })
        assertContains(MathMLVisitor.convert(parse("""\skew3\hat A""")), "lspace=")
    }

    @Test
    fun textSymbolsAndEveryTextAccentWorkInsideAndOutsideText() {
        val symbols = assertIs<LatexNode.TextMode>(parse("""\text{\lq\l\L\dag\ddag\rq}""").children.single())
        assertEquals("‘łŁ†‡’", symbols.text)
        for (kind in LatexNode.TextAccent.Kind.entries) {
            for (source in listOf("\\${kind.command}{a}", "\\text{\\${kind.command}{a}}")) {
                val accent = parse(source).nodes<LatexNode.TextAccent>().single()
                assertEquals(kind, accent.accent)
                assertEquals("a", accent.content.nodes<LatexNode.TextMode>().single().text)
                assertNotNull(accent.sourceRange)
                val mathML = MathMLVisitor.convert(parse(source))
                assertContains(mathML, kind.combining.toString())
                assertContains(AccessibilityVisitor.describe(parse(source)), kind.name.lowercase().replace('_', ' '))
            }
        }
    }

    @Test
    fun textAccentsPreserveNestedStyleAndPlainTextExtraction() {
        val document = parse("""\text{caf\'{e} \t{oo} \d{a} \b{b} \H{\textbf{o}} \v{\'a}}""")
        val text = assertIs<LatexNode.TextMode>(document.children.single())
        assertContains(ParseUtils.extractText(listOf(text)), "cafe\u0301")
        assertEquals(1, document.nodes<LatexNode.Style>().size)
        assertEquals(7, document.nodes<LatexNode.TextAccent>().size)
        assertContains(MathMLVisitor.convert(document), "accentunder=\"true\"")
        val word = parse("""\text{\'{ab}}""")
        assertEquals("a", word.nodes<LatexNode.TextAccent>().single().content.nodes<LatexNode.TextMode>().single().text)
        assertEquals("a\u0301b", ParseUtils.extractText(listOf(word.children.single())))
    }

    private fun parse(source: String): LatexNode.Document {
        val result = parser.parseWithDiagnostics(source)
        assertTrue(result.diagnostics.isEmpty(), result.diagnostics.toString())
        assertTrue(result.document.nodes<LatexNode.Command>().isEmpty(), source)
        return result.document
    }

    private inline fun <reified T : LatexNode> LatexNode.nodes(): List<T> =
        fold(mutableListOf<T>()) { found, node -> found.apply { if (node is T) add(node) } }
}
