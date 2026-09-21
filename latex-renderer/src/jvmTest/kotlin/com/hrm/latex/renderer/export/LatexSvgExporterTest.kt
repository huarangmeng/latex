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
