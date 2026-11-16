package com.hrm.latex.renderer.layout

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.export.ExportConfig
import com.hrm.latex.renderer.export.LatexExporterState
import com.hrm.latex.renderer.font.loadKatexFonts
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.toContext
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MathSpacingRenderingTest {
    private val density = Density(1f)
    private val textMeasurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)
    private val config = LatexConfig(fontSize = 24.sp, theme = LatexTheme.light(backgroundColor = Color.White))
    private val context = config.toContext(false, fonts)

    private fun measure(source: String): NodeLayout =
        measureGroup(LatexParser().parse(source).children, context, textMeasurer, density)

    @Test
    fun sourceSpacesTabsAndNewlinesLeaveMathPixelsUnchanged() {
        val exporter = LatexExporterState(density, textMeasurer, fonts)
        for ((compact, spaced) in listOf(
            "a-b" to " a \n -\t b ", "-x" to " - x ",
            "a+b=c" to "a + b = c", "(a-b)" to " ( a - b ) ",
        )) assertEquals(
            assertNotNull(exporter.exportSvg(compact, config)).svg,
            assertNotNull(exporter.exportSvg(spaced, config)).svg,
            spaced,
        )
        assertTrue(measure("\\text{a b}").width > measure("\\text{ab}").width)
    }

    @Test
    fun explicitGlueAddsToAutomaticGlueAndBreakMarkersDoNotRemoveIt() {
        val normal = measure("a+b").width
        val thin = 24f * 3f / 18f
        assertEquals(normal + 2 * thin, measure("a\\,+\\,b").width, 0.01f)
        assertEquals(normal - thin, measure("a\\!+b").width, 0.01f)
        for (marker in listOf("\\allowbreak", "\\nobreak", "\\hspace{0pt}", "\\label{eq}")) {
            assertEquals(normal, measure("a${marker}+b").width, 0.01f, marker)
        }
        assertEquals(measure("a-b").width + 24f, measure("a\\quad-b").width, 0.01f)
        assertEquals(measure("\\quad a+b").width, measure("{\\displaystyle\\quad a+b}").width, 0.01f)
        assertEquals(measure("{\\scriptstyle a+b}").width + thin * 0.7f,
            measure("{\\scriptstyle\\,a+b}").width, 0.01f)
    }

    @Test
    fun colorAndStyleDeclarationsPreserveTheirAtomsAndSourceOwnership() {
        for ((plain, styled) in listOf(
            "a+b" to "a\\textcolor{red}{+b}",
            "a=b" to "a\\textcolor{red}{=}b",
            "a+b" to "a\\displaystyle+b",
            "a=b" to "a\\not =b",
            "a\\int_0^1 b" to "a\\textcolor{red}{\\int_0^1 b}",
        )) assertEquals(measure(plain).width, measure(styled).width, 0.01f, styled)

        val nodes = LatexParser().parse("a\\textcolor{red}{+b}").children
        val map = LayoutMap()
        LatexRenderer.measure(nodes, context, textMeasurer, density, layoutMap = map)
        assertEquals(nodes.size, map.size)
        for (entry in map.entries()) {
            assertSame(entry.node, assertNotNull(map.hitTest(entry.relX + entry.width / 2, entry.relY + entry.height / 2)).node)
        }
    }

    @Test
    fun operatorsDoNotAddASecondGapBeyondTheMathSpacingTable() {
        assertEquals(measure("\\mathrm{sin}").width, measure("\\sin").width, 0.01f)
        assertEquals(measure("\\mathrm{sin}(x)").width, measure("\\sin(x)").width, 0.01f)
        assertEquals(measure("\\mathrm{f}").width, measure("\\operatorname{f}").width, 0.01f)
        assertEquals(measure("\\mathrm{sin}x").width + 24f * 3f / 18f, measure("\\sin x").width, 0.01f)
    }

    @Test
    fun sourceWhitespaceDoesNotIntroduceNewAutomaticBreaks() {
        val wrapping = context.copy(layoutHints = context.layoutHints.copy(lineBreakingEnabled = true, maxLineWidth = 95f))
        fun wrapped(source: String) = measureGroup(LatexParser().parse(source).children, wrapping, textMeasurer, density)
        val compact = wrapped("a+b=c+d=e+f")
        val spaced = wrapped("a + b = c + d = e + f")
        assertTrue(compact.height > measure("a+b=c+d=e+f").height)
        assertEquals(compact.width, spaced.width, 0.01f)
        assertEquals(compact.height, spaced.height, 0.01f)
        assertEquals(compact.baseline, spaced.baseline, 0.01f)
    }

    @Test
    fun explicitLinesInsideColorDeclarationsKeepTheirLayout() {
        val plain = measure("a+b\\\\c+d")
        val colored = measure("a\\color{red}+b\\\\c+d")
        assertEquals(plain.width, colored.width, 0.01f)
        assertEquals(plain.height, colored.height, 0.01f)
        assertEquals(plain.baseline, colored.baseline, 0.01f)
        assertEquals(measure("a+b").height, measure("a+b\\\\").height, 0.01f)
    }

    @Test
    fun nonbreakingSpaceKeepsItsWidthAndBindsAdjacentAtoms() {
        val source = "a~b"
        val natural = measure(source)
        assertEquals(measure("a\\ b").width, natural.width, 0.01f)
        val wrapping = context.copy(layoutHints = context.layoutHints.copy(lineBreakingEnabled = true, maxLineWidth = natural.width / 2))
        val actual = measureGroup(LatexParser().parse(source).children, wrapping, textMeasurer, density)
        assertEquals(natural.width, actual.width, 0.01f)
        assertEquals(natural.height, actual.height, 0.01f)
    }

    @Test
    fun unicodeOperatorsAndCommandFormsHaveTheSameSpacing() {
        for ((literal, command) in listOf("≤" to "\\le", "∈" to "\\in", "⊗" to "\\otimes", "−" to "-")) {
            assertEquals(measure("a${command} b").width, measure("a${literal}b").width, 0.01f, literal)
        }
    }

    @Test
    fun renderSpacingAuditSamples() {
        val exporter = LatexExporterState(density, textMeasurer, fonts)
        val samples = mapOf(
            "source-whitespace" to """a-b \qquad a - b \qquad \text{well-known}""",
            "signs" to """{-x}\quad{a=-x}\quad{a--b}\quad{a+-b}\quad{a+++b}""",
            "explicit-glue" to """a+b\qquad a\,+\,b\qquad a\!+b""",
            "operators" to """\sin x\quad\sin(x)\quad\log_b x\quad a\sum_i b""",
            "wrappers" to """a+b\quad a\textcolor{red}{+b}\quad a\mathbf{+}b\quad a{+}b""",
            "scripts" to """x^{a+b}\quad x^{a,b}\quad x^{a\sin b}\quad x^{\frac{a}{b}c}""",
            "relations" to """a=b\quad a:b\quad a,=b\quad a≤b\quad a\ntrianglelefteq b""",
            "quad-context" to """a-b\quad -x\qquad a-b\quad {-x}""",
            "fractions-matrices-delimiters" to """a\frac{x}{y}b\quad a\begin{matrix}1\end{matrix}b\quad a\left(x\right)b""",
            "style-glue" to """{\displaystyle\quad a+b}\qquad{\scriptstyle\,a+b}""",
        )
        val directory = File("build/reports/math-spacing")
        assertTrue(directory.isDirectory || directory.mkdirs())
        for ((name, source) in samples) {
            val result = assertNotNull(exporter.export(source, config, ExportConfig(scale = 2f)))
            File(directory, "$name.png").writeBytes(assertNotNull(result.bytes))
        }
    }

    companion object {
        private val fonts by lazy { runBlocking { loadKatexFonts() } }
    }
}
