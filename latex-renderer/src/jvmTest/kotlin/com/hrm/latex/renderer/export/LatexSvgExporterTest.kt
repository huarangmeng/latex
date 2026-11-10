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

package com.hrm.latex.renderer.export

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexFontFamilies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class LatexSvgExporterTest {
    @Test
    fun exportsMeasuredFormulaAsPortablePaths() {
        val exporter = createExporter()
        val result = assertNotNull(
            exporter.exportSvg(
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                config = LatexConfig(fontSize = 24.sp)
            )
        )

        assertContains(result.svg, "<svg")
        assertContains(result.svg, "<path")
        assertContains(result.svg, "viewBox=")
        assertFalse("<image" in result.svg)
        assertFalse("<text" in result.svg, "PATH mode must not depend on installed fonts")
    }

    @Test
    fun sequentialExportsResetParserInput() {
        val exporter = createExporter()
        val first = assertNotNull(exporter.exportSvg("x^2"))
        val second = assertNotNull(exporter.exportSvg("\\sqrt{y}"))

        assertNotEquals(first.svg, second.svg)
    }

    @Test
    fun mathChoiceUsesActualLayoutStyleInSvg() {
        val exporter = createExporter()
        val choice = "\\mathchoice{WWWW}{xx}{i}{j}"
        for ((prefix, expected) in listOf(
            "\\displaystyle" to "WWWW", "\\textstyle" to "xx",
            "\\scriptstyle" to "i", "\\scriptscriptstyle" to "j",
            "\\crampedscriptstyle" to "i"
        )) {
            val actual = assertNotNull(exporter.exportSvg("$prefix$choice"))
            val reference = assertNotNull(exporter.exportSvg("$prefix $expected"))
            assertEquals(reference.width, actual.width, prefix)
            assertEquals(reference.height, actual.height, prefix)
        }
        val nested = assertNotNull(exporter.exportSvg("x^{$choice}"))
        val reference = assertNotNull(exporter.exportSvg("x^{i}"))
        assertEquals(reference.width, nested.width)
        assertEquals(reference.height, nested.height)
    }

    @Test
    fun matrixMathChoiceMatchesTextAndSmallMatrixStyle() {
        val exporter = createExporter()
        for ((environment, branch) in listOf("matrix" to "xx", "smallmatrix" to "i")) {
            val actual = assertNotNull(exporter.exportSvg("\\begin{$environment}\\mathchoice{WWWW}{xx}{i}{j}\\end{$environment}"))
            val expected = assertNotNull(exporter.exportSvg("\\begin{$environment}$branch\\end{$environment}"))
            assertEquals(expected.width, actual.width)
            assertEquals(expected.height, actual.height)
        }
    }

    @Test
    fun namedColorAndDeclarationExportLikeExplicitTextcolor() {
        val exporter = createExporter()
        val actual = assertNotNull(exporter.exportSvg("\\definecolor{brand}{HTML}{123456}\\color{brand}a+b"))
        val reference = assertNotNull(exporter.exportSvg("\\textcolor{#123456}{a+b}"))
        assertEquals(reference.svg, actual.svg)
    }

    @Test
    fun structuredTextKeepsNestedFontChangesAndMath() {
        val exporter = createExporter()
        val source = "\\text{normal \\textbf{bold} \\textit{italic} and ${'$'}x^2${'$'}}"
        val result = assertNotNull(exporter.exportSvg(source))
        val plain = assertNotNull(exporter.exportSvg("\\text{normal bold italic and x2}"))
        assertNotEquals(plain.svg, result.svg)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
    }

    @Test
    fun smallCapsRendersMixedCaseAndScopedDeclarations() {
        val exporter = createExporter()
        val config = LatexConfig(fontSize = 40.sp)
        fun export(source: String) = assertNotNull(exporter.exportSvg(source, config = config))

        val mixed = export("""\textsc{WWww1}+\textrm{outside}""")
        assertEquals(mixed.svg, export("""{\sc WWww1}+\textrm{outside}""").svg)
        assertEquals(mixed.svg, export("""{\scshape WWww1}+\textrm{outside}""").svg)
        assertEquals(mixed.svg, export("""\text{\textsc{WWww1}}+\textrm{outside}""").svg)
        assertNotEquals(mixed.svg, export("""\textrm{WWww1}+\textrm{outside}""").svg)
        assertTrue(mixed.width < export("""\textrm{WWWW1}+\textrm{outside}""").width)
        assertTrue(mixed.width > export("""\textsc{wwww1}+\textrm{outside}""").width)
        assertEquals(export("""\textrm{WW1}""").svg, export("""\textsc{WW1}""").svg)
    }

    @Test
    fun textFontDeclarationsComposeLikeNestedTextCommands() {
        val exporter = createExporter()
        fun svg(source: String) = assertNotNull(exporter.exportSvg(source)).svg

        val styled = svg("""\textsf{\textbf{\textit{\textsc{Abc}}}}""")
        assertEquals(styled, svg("""{\sffamily\bfseries\itshape\scshape Abc}"""))
        assertEquals(styled, svg("""{\scshape\itshape\bfseries\sffamily Abc}"""))
        assertEquals(styled, svg("""\textsf{\text{\textbf{\textit{\textsc{Abc}}}}}"""))
        assertNotEquals(styled, svg("""{\rmfamily\bfseries\itshape\scshape Abc}"""))
        assertNotEquals(styled, svg("""{\sffamily\mdseries\itshape\scshape Abc}"""))
        assertNotEquals(styled, svg("""{\sffamily\bfseries\upshape\scshape Abc}"""))
        assertEquals(
            svg("""\textsf{\textbf{\textsc{Abc} Def \textsc{Ghi}}}"""),
            svg("""{\sffamily\bfseries\scshape Abc \textulc{Def} Ghi}""")
        )
        assertEquals(svg("""{\ttfamily\slshape Abc}"""), svg("""\texttt{\textsl{Abc}}"""))
    }

    @Test
    fun textFontResetsChangeOnlyTheRequestedAttributes() {
        val exporter = createExporter()
        fun svg(source: String) = assertNotNull(exporter.exportSvg(source)).svg

        assertEquals(
            svg("""{\sffamily\bfseries\itshape Abc}"""),
            svg("""{\sffamily\bfseries\itshape\scshape{\ulcshape Abc}}""")
        )
        assertEquals(
            svg("""{\sffamily\bfseries\scshape Abc}"""),
            svg("""{\sffamily\bfseries\itshape\scshape \textup{Abc}}""")
        )
        assertEquals(
            svg("""{\sffamily\bfseries Abc}"""),
            svg("""{\sffamily\bfseries\itshape\scshape{\normalshape Abc}}""")
        )
        assertEquals(
            svg("""{\sffamily\itshape\scshape Abc}"""),
            svg("""{\sffamily\bfseries\itshape\scshape \textmd{Abc}}""")
        )
        assertEquals(svg("""\textnormal{Abc}"""), svg("""{\sffamily\bfseries\itshape\scshape{\normalfont Abc}}"""))
        assertEquals(svg("""\textnormal{Abc}"""), svg("""{\sffamily\bfseries\itshape\scshape \textnormal{Abc}}"""))
        assertEquals(svg("""\textsc{Abc}"""), svg("""{\sffamily\bfseries\itshape{\sc Abc}}"""))
    }

    @Test
    fun uprightCommandsUseTheCurrentShapeAndKeepFamilyAndWeight() {
        val exporter = createExporter()
        fun svg(source: String) = assertNotNull(exporter.exportSvg(source)).svg

        val upright = svg("""{\sffamily\bfseries Abc}""")
        val smallCaps = svg("""{\sffamily\bfseries\scshape Abc}""")
        assertEquals(upright, svg("""{\sffamily\bfseries\scshape\upshape Abc}"""))
        assertEquals(upright, svg("""{\sffamily\bfseries\scshape \textup{Abc}}"""))
        assertEquals(smallCaps, svg("""{\sffamily\bfseries\itshape\scshape\upshape Abc}"""))
        assertEquals(upright, svg("""{\sffamily\bfseries\itshape\scshape\upshape\upshape Abc}"""))
        assertEquals(upright, svg("""{\sffamily\bfseries\upshape\scshape\upshape Abc}"""))
        assertEquals(smallCaps, svg("""\textit{\textsf{\textbf{\scshape\upshape Abc}}}"""))
        assertEquals(
            svg("""{\sffamily\bfseries\itshape\scshape Abc}"""),
            svg("""\textit{\textsf{\textbf{\scshape\upshape\itshape Abc}}}""")
        )
        assertEquals(svg("""\textbf{\textsc{Abc}}"""), svg("""{\sffamily\bfseries\scshape \textrm{Abc}}"""))
        assertNotEquals(upright, svg("""{\sffamily\bfseries\scshape \textrm{Abc}}"""))
    }

    @Test
    fun emphasisTogglesTheCurrentSlopeWithoutChangingOtherAttributes() {
        val exporter = createExporter()
        fun svg(source: String) = assertNotNull(exporter.exportSvg(source)).svg

        assertEquals(
            svg("""\textsf{\textit{Abc \textup{Def} Ghi}}"""),
            svg("""\textsf{\emph{Abc \emph{Def} Ghi}}""")
        )
        assertEquals(
            svg("""\textsf{\textbf{\textsc{\textit{Abc}}}}"""),
            svg("""{\sffamily\bfseries\scshape\em Abc}""")
        )
        assertEquals(
            svg("""\textsf{\textbf{\textsc{Abc}}}"""),
            svg("""{\sffamily\bfseries\scshape\itshape\em Abc}""")
        )
        assertEquals(
            svg("""\textit{\textsc{Abc}}"""),
            svg("""\textit{\textsc{\em\em Abc}}""")
        )
        assertEquals(
            svg("""\textsf{Abc}"""),
            svg("""{\sffamily\scshape\em\em\upshape Abc}""")
        )
    }

    @Test
    fun legacyTextFontSwitchesResetAndRestoreTheFont() {
        val exporter = createExporter()
        fun svg(source: String) = assertNotNull(exporter.exportSvg(source)).svg

        val equivalents = mapOf(
            "bf" to """\textbf{Abc}""",
            "it" to """\textit{Abc}""",
            "rm" to """\textrm{Abc}""",
            "sf" to """\textsf{Abc}""",
            "tt" to """\texttt{Abc}""",
            "sl" to """\textsl{Abc}"""
        )
        for ((legacy, modern) in equivalents) {
            assertEquals(
                svg("""\textsf{\textbf{\textsc{\textnormal{$modern}}}}"""),
                svg("""\textsf{\textbf{\textsc{{\$legacy Abc}}}}"""),
                legacy
            )
        }
        assertEquals(
            svg("""\textsc{Abc \textnormal{\textbf{Def}} Ghi}"""),
            svg("""\textsc{Abc {\bf Def} Ghi}""")
        )
        assertEquals(svg("""\mathbf{x}"""), svg("""{\bf x}"""))
    }

    @Test
    fun swashAndSpacedSmallCapsUseTheBundledFontFallbacks() {
        val exporter = createExporter()
        fun svg(source: String) = assertNotNull(exporter.exportSvg(source)).svg

        val italic = svg("""\textsf{\textit{Abc}}""")
        assertEquals(italic, svg("""\textsf{\textsw{Abc}}"""))
        assertEquals(italic, svg("""{\sffamily\swshape Abc}"""))
        val smallCaps = svg("""\textsf{\textsc{Abc}}""")
        assertEquals(smallCaps, svg("""\textsf{\textssc{Abc}}"""))
        assertEquals(smallCaps, svg("""{\sffamily\sscshape Abc}"""))
    }

    @Test
    fun arraySpacingAndDashRulesAffectExportedLayout() {
        val exporter = createExporter()
        val body = "\\begin{array}{cc}a&b\\\\\\hline c&d\\end{array}"
        val normal = assertNotNull(exporter.exportSvg(body))
        val wide = assertNotNull(exporter.exportSvg("\\setlength{\\arraycolsep}{20pt}$body"))
        val tall = assertNotNull(exporter.exportSvg("\\def\\arraystretch{2}$body"))
        val dashed = assertNotNull(exporter.exportSvg(body.replace("hline", "hdashline")))
        assertTrue(wide.width > normal.width)
        assertTrue(tall.height > normal.height)
        assertEquals(normal.width, dashed.width)
        assertNotEquals(normal.svg, dashed.svg)
    }

    @Test
    fun exportsCompatibilityLayoutsWithExpectedGeometry() {
        val exporter = createExporter()
        val loose = assertNotNull(exporter.exportSvg("""\begin{array}{rr}1&2\end{array}"""))
        val repeated = assertNotNull(exporter.exportSvg("""\begin{array}{*{2}{r}}1&2\end{array}"""))
        val tight = assertNotNull(exporter.exportSvg("""\begin{array}{r@{}r}1&2\end{array}"""))
        assertEquals(loose.svg, repeated.svg)
        assertTrue(tight.width < loose.width)

        val small = assertNotNull(exporter.exportSvg("""\llbracket x\rrbracket"""))
        val stretched = assertNotNull(exporter.exportSvg("""\left\llbracket\frac{x}{y}\right\rrbracket"""))
        assertTrue(stretched.height > small.height)

        val normal = assertNotNull(exporter.exportSvg("""A\xrightarrow[\text{back}]{\text{forward}}B"""))
        val harpoons = assertNotNull(exporter.exportSvg("""A\xrightleftharpoons[\text{back}]{\text{forward}}B"""))
        assertEquals(normal.width, harpoons.width)
        assertNotEquals(normal.svg, harpoons.svg)

        val mixed = assertNotNull(exporter.exportSvg("""\textcolor{red!30!blue}{x}"""))
        val explicit = assertNotNull(exporter.exportSvg("""\textcolor{#4d00b3}{x}"""))
        assertEquals(explicit.svg, mixed.svg)
    }

    private fun createExporter(): LatexExporterState {
        val density = Density(1f)
        val family = FontFamily.Serif
        val families = LatexFontFamilies(
            main = family,
            math = family,
            ams = family,
            sansSerif = FontFamily.SansSerif,
            monospace = FontFamily.Monospace,
            caligraphic = family,
            fraktur = family,
            script = family,
            size1 = family,
            size2 = family,
            size3 = family,
            size4 = family
        )
        return LatexExporterState(
            density = density,
            textMeasurer = TextMeasurer(
                defaultFontFamilyResolver = createFontFamilyResolver(),
                defaultDensity = density,
                defaultLayoutDirection = LayoutDirection.Ltr
            ),
            fontFamilies = families
        )
    }
}
