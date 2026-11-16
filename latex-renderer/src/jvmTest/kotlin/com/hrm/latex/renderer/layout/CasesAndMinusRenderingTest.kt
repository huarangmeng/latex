package com.hrm.latex.renderer.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.export.ExportConfig
import com.hrm.latex.renderer.export.LatexExporterState
import com.hrm.latex.renderer.font.loadKatexFonts
import com.hrm.latex.renderer.layout.measurer.MatrixMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.toContext
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CasesAndMinusRenderingTest {
    private val density = Density(1f)
    private val textMeasurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)
    private val config = LatexConfig(fontSize = 17.sp, theme = LatexTheme.light(backgroundColor = Color.White))
    private val context = config.toContext(false, fonts)

    @Test
    fun allCasesVariantsDrawOnlyTheInputCellsInTwoLeftAlignedColumns() {
        for (environment in listOf("cases", "cases*", "dcases", "dcases*", "rcases", "rcases*")) {
            assertCasesColumns(
                "\\begin{$environment}1 & \\text{if Female,} \\\\ 222 & \\text{Male}\\end{$environment}",
                listOf(10f, 90f, 30f, 40f),
            )
        }
    }

    @Test
    fun casesWithoutConditionsDoNotGenerateAnyText() {
        assertCasesColumns("""\begin{cases}1 \\ 222\end{cases}""", listOf(10f, 0f, 30f, 0f))
    }

    @Test
    fun asciiMinusRendersLikeExplicitMinusWithTheBundledKatexFonts() {
        val exporter = LatexExporterState(density, textMeasurer, fonts)
        for (source in listOf(
            "-", "a - b", "- x", "A^-", "x_{-1}", "\\mathrm{a - b}",
            """(140 - \text{age}) \times \text{weight}""",
            """\mathrm{pH} = \mathrm{p}K_a + \log_{10}\frac{[A^-]}{[HA]}""",
        )) {
            val actual = assertNotNull(exporter.exportSvg(source, config))
            val expected = assertNotNull(exporter.exportSvg(source.replace("-", "\\minus"), config))
            assertEquals(expected.svg, actual.svg, source)
        }
        assertNotEquals(
            assertNotNull(exporter.exportSvg("-", config)).svg,
            assertNotNull(exporter.exportSvg("\\text{-}", config)).svg,
        )
        // KaTeX's minus and plus have the same advance and binary spacing.
        assertEquals(
            assertNotNull(exporter.exportSvg("a+b", config)).width,
            assertNotNull(exporter.exportSvg("a-b", config)).width,
        )
    }

    @Test
    fun renderIssueExamplesForVisualInspection() {
        val exporter = LatexExporterState(density, textMeasurer, fonts)
        val samples = mapOf(
            "cases-values" to """S = \begin{cases} 0.85 & \text{if Female,} \\ 1.0 & \text{Male} \end{cases}""",
            "cases-conditions" to """f(x) = \begin{cases} 4x & \text{if } 0 \le x \le 10, \\ 60 + (x-20) & \text{if } x > 20 \end{cases}""",
            "minus-binary" to """(140 - \text{age}) \times \text{weight}""",
            "minus-superscript" to """\mathrm{pH} = \mathrm{p}K_a + \log_{10}\frac{[A^-]}{[HA]}""",
            "minus-text" to """a-b \quad {-x} \quad A^- \quad \text{well-known}""",
        )
        val directory = File("build/reports/cases-and-minus")
        assertTrue(directory.isDirectory || directory.mkdirs())
        for ((name, source) in samples) {
            val image = assertNotNull(exporter.export(source, config, ExportConfig(scale = 3f)))
            File(directory, "$name.png").writeBytes(assertNotNull(image.bytes))
        }
    }

    private fun assertCasesColumns(source: String, widths: List<Float>) {
        val node = assertIs<LatexNode.Cases>(LatexParser().parse(source).children.single())
        val cells = node.cases.flatMap { listOf(it.first, it.second) }
        val measured = mutableListOf<LatexNode>()
        val drawn = mutableMapOf<Int, Offset>()
        val layout = MatrixMeasurer().measure(
            node, context, textMeasurer, density,
            measureNode = { cell, _ ->
                val index = cells.indexOfFirst { it === cell }
                assertTrue(index >= 0, "The renderer must not insert cells: $cell")
                measured += cell
                NodeLayout(widths[index], 15f, 10f) { x, y -> drawn[index] = Offset(x, y) }
            },
            measureGroup = { _, _ -> error("Cases should measure their input cells") },
        )
        assertEquals(cells, measured, "Cells must be measured in input order")
        val bitmap = ImageBitmap(ceil(layout.width).toInt(), ceil(layout.height).toInt())
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
            layout.draw(this, 0f, 0f)
        }
        val first = assertNotNull(drawn[0])
        val condition = assertNotNull(drawn[1])
        val second = assertNotNull(drawn[2])
        val secondCondition = assertNotNull(drawn[3])
        assertTrue(condition.x > first.x + widths[0], "The condition follows the value")
        assertEquals(first.x, second.x, "The value column is left aligned")
        assertEquals(condition.x, secondCondition.x, "The condition column is left aligned")
        assertEquals(first.y, condition.y, "Cells share a row baseline")
        assertTrue(second.y > first.y)
        if (node.style == LatexNode.Cases.CasesStyle.RIGHT) assertEquals(0f, first.x)
        else assertTrue(first.x > 0f, "The left brace precedes the value column")
    }

    companion object {
        private val fonts by lazy { runBlocking { loadKatexFonts() } }
    }
}
