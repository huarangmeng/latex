package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.parser.visitor.MathMLVisitor
import kotlin.test.*

class TexCompatibilityTest {
    private val parser = LatexParser()
    private fun parse(source: String): LatexNode.Document {
        val result = parser.parseWithDiagnostics(source)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        return result.document
    }
    private fun visible(source: String): String = AccessibilityVisitor.describe(parse(source)).replace(" ", "")
    private fun LatexNode.descendants(): List<LatexNode> = listOf(this) + children().flatMap { it.descendants() }

    @Test fun forwardReferencesExpandAtInvocation() {
        assertEquals("x", visible("""\def\a{\b}\def\b{x}\a"""))
        assertEquals("xy", visible("""\def\a{\b}\def\b{x}\a\def\b{y}\a"""))
    }
    @Test fun definitionsInsideBodiesRunOnlyWhenCalled() {
        assertEquals("aba", visible("""\def\a{a}\def\change{\def\a{b}}\a{\change\a}\a"""))
    }
    @Test fun argumentTokensAreSubstitutedBeforeParsing() {
        assertEquals("fraction:xover2", visible("""\def\f#1{\frac{#1}{2}}\f{x}"""))
        assertEquals("b+a", visible("""\def\swap#1,#2;{#2+#1}\swap a,b;"""))
        assertEquals("a,b+c", visible("""\def\take#1;{#1}\take {a,b}+c;"""))
    }
    @Test fun macroExpansionDoesNotIntroduceScope() {
        assertEquals("b", visible("""\def\a{a}\def\change{\def\a{b}}\change\a"""))
    }
    @Test fun provideCommandSkipsDefinedBodyWithoutExecutingIt() {
        assertEquals("a", visible("""\def\a{a}\providecommand{\a}{\def\a{b}}\a"""))
        assertEquals("x+y", visible("""\providecommand*{\pair}[2][x]{#1+#2}\pair{y}"""))
        val document = parse("""\providecommand{\frac}{bad}\frac{1}{2}""")
        assertTrue(document.children.any { it is LatexNode.Fraction })
    }
    @Test fun starredDefinitionsAndOperators() {
        assertEquals("y", visible("""\newcommand*{\a}{x}\renewcommand*{\a}{y}\a"""))
        val op = parse("""\DeclareMathOperator*{\argmax}{argmax}\argmax_{x} f""").children.filterIsInstance<LatexNode.BigOperator>().single()
        assertTrue(op.limitsInDisplay)
        assertNotNull(op.subscript)
    }
    @Test fun letSnapshotsMacrosBuiltinsAndCharacters() {
        assertEquals("ab", visible("""\def\a{a}\let\saved=\a\def\a{b}\saved\a"""))
        assertEquals("fraction:1over2", visible("""\let\f\frac\def\frac{bad}\f12"""))
        assertEquals("x", visible("""\let\a=x\a"""))
    }
    @Test fun globalDefinitionsSurviveEveryScope() {
        assertEquals("c", visible("""\def\a{a}{\def\a{b}{\gdef\a{c}}}\a"""))
        assertEquals("b", visible("""{\global\def\a{b}}\a"""))
        assertEquals("x", visible("""{\def\b{x}\global\let\a\b}\a"""))
    }
    @Test fun edefAndNoexpandControlTiming() {
        assertEquals("xy", visible("""\def\b{x}\edef\a{\b}\edef\c{\noexpand\b}\def\b{y}\a\c"""))
        assertEquals("x", visible("""{\def\b{x}\xdef\a{\b}}\a"""))
    }
    @Test fun expandafterAndCsnameDefineDynamicNames() {
        assertEquals("yes", visible("""\expandafter\ifx\csname undefined\endcsname\relax yes\else no\fi"""))
        assertEquals("x", visible("""\expandafter\def\csname foo\endcsname{x}\foo"""))
    }
    @Test fun conditionalsSkipSideEffectsAndSupportNesting() {
        assertEquals("ac", visible("""\def\a{a}\ifdefined\missing\def\a{bad}\else\ifdefined\a\a\else b\fi\fi\ifx\missing\other c\else d\fi"""))
        assertEquals("yes", visible("""\def\a{x}\let\b\a\ifx\a\b yes\else no\fi"""))
    }
    @Test fun customEnvironmentExpandsBeginAndEndInOneStream() {
        val source = """\newenvironment*{bold}{\begingroup\bf}{\endgroup}\begin{bold}x+y\end{bold}+z"""
        val document = parse(source)
        assertTrue(document.descendants().any { it is LatexNode.Style })
        assertEquals("x+y+z", visible(source))
        assertEquals("important", visible("""\newenvironment{emphasis}{\textbf\bgroup}{\egroup}\begin{emphasis}important\end{emphasis}"""))
    }
    @Test fun recursiveExpansionProducesDiagnosticInsteadOfOverflow() {
        val result = parser.parseWithDiagnostics("""\def\loop{\loop}\loop+x""")
        assertTrue(result.errors.any { it.category == ParseDiagnostic.Category.MACRO_ERROR })
        assertTrue(AccessibilityVisitor.describe(result.document).contains("x"))
    }
    @Test fun colorsAreDeclarationsButEmptyTextcolorIsNot() {
        val document = parse("""{\color{red}a+b}+c""")
        val color = document.descendants().filterIsInstance<LatexNode.Color>().single()
        assertEquals("a+b", AccessibilityVisitor.describe(LatexNode.Group(color.content)).replace(" ", ""))
        val empty = parse("""\textcolor{red}{}x""")
        assertIs<LatexNode.Text>(empty.children.last())
        assertTrue(empty.children.filterIsInstance<LatexNode.Color>().single().content.isEmpty())
    }
    @Test fun namedColorsAndModelsAreScoped() {
        val document = parse("""\definecolor{brand}{HTML}{123456}{\definecolor{brand}{RGB}{255,0,0}\textcolor{brand}{a}}\textcolor{brand}{b}\color[rgb]{0,0.5,1}c""")
        assertEquals(listOf("#ff0000", "#123456", "#0080ff"), document.descendants().filterIsInstance<LatexNode.Color>().map { it.color })
        assertTrue(MathMLVisitor.convert(document).contains("#0080ff"))
        assertEquals("#123456", parse("""\definecolor{a}{HTML}{123456}\colorlet{b}{a}\definecolor{a}{HTML}{ffffff}\textcolor{b}{x}""").descendants().filterIsInstance<LatexNode.Color>().single().color)
    }
    @Test fun mathChoiceTracksAllSizesAndCramping() {
        val choice = """\mathchoice{D}{T}{S}{Q}"""
        assertEquals("D", visible(choice))
        assertEquals("T", visible("\\textstyle$choice"))
        assertEquals("S", visible("\\crampedscriptstyle$choice"))
        assertEquals("Q", visible("\\scriptscriptstyle$choice"))
        assertTrue(MathMLVisitor.convert(parse("x^{y^{$choice}}" )).contains("<mi>Q</mi>"))
        assertTrue(MathMLVisitor.convert(parse(choice), displayMode = false).contains("<mi>T</mi>"))
    }
    @Test fun mathPalettePassesTheCurrentStyle() {
        val source = """\def\show#1#2{#1\mathchoice{D}{T}{S}{Q}#2}\scriptstyle\mathpalette\show{x}"""
        assertEquals("Sx", visible(source))
    }
    @Test fun textPreservesFormattingAndInlineMath() {
        val document = parse("""\text{normal \textbf{bold} and ${'$'}x^2${'$'}}""")
        val text = assertIs<LatexNode.TextMode>(document.children.single())
        assertTrue(text.content.isNotEmpty())
        val mathml = MathMLVisitor.convert(document)
        assertTrue(mathml.contains("mathvariant=\"bold\""))
        assertTrue(mathml.contains("<msup>"))
        assertTrue(mathml.contains("<mtext>normal"))
    }
    @Test fun verbPreservesRawCharactersAndStarredSpaces() {
        assertEquals("a%{\\b}", assertIs<LatexNode.TextMode>(parse("""\verb|a%{\b}|""").children.single()).text)
        assertEquals("a␣b", assertIs<LatexNode.TextMode>(parse("""\verb*|a b|""").children.single()).text)
    }
    @Test fun arraySettingsAndDashedLinesReachAstAndMathml() {
        val source = """\def\arraystretch{1.5}\setlength{\arraycolsep}{4pt}\begin{array}{cc}a&b\\\hdashline c&d\end{array}"""
        val array = parse(source).children.filterIsInstance<LatexNode.Array>().single()
        assertEquals(1.5f, array.rowStretch)
        assertEquals("4pt", array.columnSep)
        assertTrue(array.rows.flatten().filterIsInstance<LatexNode.HLine>().single().dashed)
        assertTrue(MathMLVisitor.convert(array).contains("dashed"))
    }
    @Test fun letSnapshotsExpandableBuiltins() {
        assertEquals("yes", visible("""\let\f\frac\ifx\f\frac yes\else no\fi"""))
        assertEquals("x", visible("""\let\saved\csname\def\csname{bad}\expandafter\def\saved foo\endcsname{x}\foo"""))
        assertEquals("fraction:1over2", visible("""\let\saved\frac\edef\f{\saved}\def\frac{bad}\f12"""))
    }
    @Test fun noexpandOutsideDefinitionsActsAsRelaxForMacros() {
        assertEquals("y", visible("""\def\a{x}\noexpand\a y"""))
    }
    @Test fun localWriteAfterGlobalWriteRestoresGlobalValue() {
        assertEquals("dc", visible("""\def\a{a}{\def\a{b}\gdef\a{c}\def\a{d}\a}\a"""))
    }
    @Test fun matrixMathChoiceUsesTextOrScriptStyle() {
        assertTrue(MathMLVisitor.convert(parse("""\begin{matrix}\mathchoice{D}{T}{S}{Q}\end{matrix}""")).contains("<mi>T</mi>"))
    }
    @Test fun invalidColorReportsErrorWithoutInventingBlack() {
        val result = parser.parseWithDiagnostics("""\textcolor[RGB]{300,0,0}{x}""")
        assertTrue(result.errors.any { it.category == ParseDiagnostic.Category.INVALID_ARGUMENT })
        assertTrue(result.document.descendants().none { it is LatexNode.Color })
    }

    @Test fun cellDefinitionsDoNotLeakToTheNextCell() {
        val document = parse("""\def\a{a}\begin{matrix}\def\a{b}\a & \a\end{matrix}\a""")
        assertEquals(listOf("b", "a", "a"), document.descendants().filterIsInstance<LatexNode.Text>().map { it.content }.filter { it.isNotBlank() })
    }
    @Test fun charactersHaveDefinedMeaning() {
        assertEquals("yes", visible("""\ifdefined x yes\else no\fi"""))
    }

    @Test fun incrementalEditsRespectDefinitionDependencies() {
        val incremental = IncrementalLatexParser()
        incremental.setInput("""\def\a{x}\a""")
        incremental.append("""+\a""")
        assertEquals("x+x", AccessibilityVisitor.describe(incremental.getCurrentDocument()).replace(" ", ""))
        incremental.setInput("""\def\a{y}\a+\a""")
        assertEquals("y+y", AccessibilityVisitor.describe(incremental.getCurrentDocument()).replace(" ", ""))
        incremental.setInput("""\definecolor{a}{HTML}{123456}\textcolor{a}{x}""")
        incremental.setInput("""\definecolor{a}{HTML}{abcdef}\textcolor{a}{x}""")
        assertTrue(MathMLVisitor.convert(incremental.getCurrentDocument()).contains("#abcdef"))
    }

    @Test fun delimitedArgumentsScanCoalescedTextAndKeepTheRemainder() {
        assertEquals("abc+z", visible("""\def\take#1END{#1}\take abcEND+z"""))
        assertEquals("😀+z", visible("""\def\take#1END{#1}\take 😀END+z"""))
        val text = "a".repeat(10_000)
        assertEquals(text + "+z", visible("\\def\\take#1END{#1}\\take ${text}END+z"))
    }
    @Test fun invalidArgumentCountIsNotTreatedAsZero() {
        assertTrue(parser.parseWithDiagnostics("""\newcommand{\a}[bad]{x}""").errors.isNotEmpty())
    }

    @Test fun recursiveMacrosInsideGroupsCannotOverflowTheParserStack() {
        val result = parser.parseWithDiagnostics("""\def\loop{\frac{1}{\loop}}\loop+x""")
        assertTrue(result.errors.any { it.category == ParseDiagnostic.Category.MACRO_ERROR })
        assertIs<LatexNode.Text>(result.document.children.last())
    }

    @Test fun invalidGlobalPrefixCannotLeakToALaterAssignment() {
        val result = parser.parseWithDiagnostics("""{\global x\def\a{y}}\ifdefined\a bad\else ok\fi""")
        assertTrue(result.errors.any { it.category == ParseDiagnostic.Category.MACRO_ERROR })
        assertEquals("xok", AccessibilityVisitor.describe(result.document).replace(" ", ""))
    }

}
