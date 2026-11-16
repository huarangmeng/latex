package com.hrm.latex.renderer.layout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
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
import com.hrm.latex.renderer.export.SvgExportConfig
import com.hrm.latex.renderer.export.SvgTextMode
import com.hrm.latex.renderer.font.loadKatexFonts
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.toContext
import com.hrm.latex.renderer.layout.measurer.textAccentGlyph
import com.hrm.latex.renderer.layout.measurer.BigOperatorMeasurer
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.utils.AtomType
import com.hrm.latex.renderer.utils.MathSpacing
import com.hrm.latex.renderer.utils.measureGlyphBounds
import kotlinx.coroutines.runBlocking
import java.io.File
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.impl.use
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlainTexRenderingTest {
    private val density = Density(1f)
    private val measurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)
    private val config = LatexConfig(fontSize = 40.sp, theme = LatexTheme.light(backgroundColor = Color.White))
    private val context = config.toContext(false, fonts)
    private val exporter = LatexExporterState(density, measurer, fonts)

    private fun measure(source: String): NodeLayout =
        measureGroup(LatexParser().parse(source).children, context, measurer, density)

    @Test
    fun plainTexSymbolsUseTheirDeclaredAtomClasses() {
        for ((name, type) in mapOf("surd" to AtomType.ORD, "ldotp" to AtomType.PUNCT,
            "colon" to AtomType.PUNCT, "doteq" to AtomType.REL,
            "relbar" to AtomType.REL, "Relbar" to AtomType.REL)) {
            assertEquals(type, MathSpacing.classifyNode(LatexParser().parse("\\$name").children.single()), name)
        }
        assertEquals(measure("a.b").width + 40f * 3f / 18f, measure("a\\ldotp b").width, 0.01f)
        assertEquals(measure("a:b").width - 40f * 7f / 18f, measure("a\\colon b").width, 0.01f)
    }

    @Test
    fun joinrelOverlapsAdjacentRelationsByThreeMu() {
        assertEquals(measure("a\\relbar\\rightarrow b").width - 40f * 3f / 18f,
            measure("a\\relbar\\joinrel\\rightarrow b").width, 0.01f)
    }

    @Test
    fun integralAliasesUseTheCorrectSizeAndLimits() {
        for ((alias, normal) in listOf("intop" to "int", "ointop" to "oint")) {
            assertEquals(svg("\\$normal\\limits_0^1"), svg("\\${alias}_0^1"))
            assertEquals(svg("\\$normal\\nolimits_0^1"), svg("\\$alias\\nolimits_0^1"))
        }
        assertTrue(measure("\\smallint").height < measure("\\intop").height * 0.75f)
        assertEquals(measure("\\smallint").height, measure("\\textstyle\\smallint").height, 0.01f)
    }

    @Test
    fun stackedIntegralLimitsFollowTheFontSlantInsteadOfOneSharedCenterLine() {
        // TeX Rule 13 / KaTeX assembleSupSub: upper +delta/2, lower -delta/2.
        // Expected corrections come from the Main/Size1/Size2 font metrics.
        for ((name, style, correction) in listOf(
            Triple("intop", MathStyle.DISPLAY, 0.44445f),
            Triple("ointop", MathStyle.DISPLAY, 0.44445f),
            Triple("intop", MathStyle.TEXT, 0.19445f),
            Triple("smallint", MathStyle.DISPLAY, 0.11111f),
            Triple("sum", MathStyle.DISPLAY, 0f)
        )) {
            val lower = LatexNode.Text("lower")
            val upper = LatexNode.Text("upper")
            val positions = mutableMapOf<LatexNode, Offset>()
            for (scriptWidth in listOf(10f, 100f)) {
                val operator = LatexNode.BigOperator(name, lower, upper,
                    limitsMode = LatexNode.BigOperator.LimitsMode.LIMITS)
                val layout = BigOperatorMeasurer().measure(operator, context.copy(mathStyle = style), measurer, density,
                    measureNode = { _, _ -> error("Unexpected direct node measurement") },
                    measureGroup = { children, _ ->
                        val script = children.single()
                        NodeLayout(scriptWidth, 10f, 8f) { x, y -> positions[script] = Offset(x, y) }
                    })
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(ImageBitmap(300, 300)), Size(300f, 300f)) {
                    layout.draw(this, 0f, 0f)
                }
                val upperPosition = assertNotNull(positions[upper])
                val lowerPosition = assertNotNull(positions[lower])
                assertEquals(40f * correction, upperPosition.x - lowerPosition.x, 0.001f, "$name / $style")
                assertTrue(upperPosition.y < lowerPosition.y)
                for (position in positions.values) {
                    assertTrue(position.x >= 0f && position.x + scriptWidth <= layout.width + 0.001f,
                        "Shifted limits must stay inside the exported image: $name")
                }
            }
        }
    }

    @Test
    fun textAccentsUseSpacingGlyphsPresentInTheBundledFont() {
        val base = svg("\\text{a}")
        Data.makeFromBytes(assertNotNull(fonts.mainBytes)).use { data ->
            assertNotNull(FontMgr.default.makeFromData(data)).use { typeface ->
                Font(typeface, 40f).use { font ->
                    for (kind in LatexNode.TextAccent.Kind.entries) {
                        val glyph = textAccentGlyph(kind)
                        assertTrue(font.getStringGlyphs(glyph).all { it.toInt() != 0 }, kind.name)
                        val source = "\\text{\\${kind.command}{a}}"
                        assertNotEquals(base, svg(source), kind.name)
                        val textSvg = assertNotNull(exporter.exportSvg(source, config,
                            SvgExportConfig(textMode = SvgTextMode.TEXT))).svg
                        assertContains(textSvg, glyph, message = kind.name)
                    }
                }
            }
        }
        assertNotEquals(svg("\\hat A"), svg("\\skew3\\hat A"))
        assertNotEquals(svg("\\vec x"), svg("\\skew3\\vec x"))
        val strokedLetters = svg("""\text{\l\L}""")
        assertNotEquals(svg("""\text{lL}"""), strokedLetters)
        assertContains(strokedLetters, "stroke=")
    }

    @Test
    fun signedOutlineBoundsPreserveTheEmptySpaceBelowAnAccentGlyph() {
        val bounds = assertNotNull(measureGlyphBounds("\u02CA", 40f, assertNotNull(fonts.mainBytes)))
        assertTrue(bounds.inkTopPx < bounds.inkBottomPx)
        assertTrue(bounds.inkBottomPx < 0f, "The acute glyph sits entirely above the baseline")
        assertEquals(0f, bounds.descentPx)
    }

    @Test
    fun renderIssue59Samples() {
        val directory = File("build/reports/plain-tex")
        assertTrue(directory.isDirectory || directory.mkdirs())
        val samples = mapOf(
            "symbols" to """\surd x\quad a\ldotp b\quad f\colon A\to B\quad a\doteq b""",
            "integrals" to """\smallint_a^b f(x)\,dx\quad\intop_a^b f(x)\,dx\quad\ointop_C f(z)\,dz\quad\intop\nolimits_a^b""",
            "arrows" to """A\relbar\joinrel\rightarrow B\quad A\Relbar\joinrel\Rightarrow B\quad A\leftarrow\joinrel\relbar B""",
            "skew" to """\hat A\quad\skew3\hat A\quad\skew{-3}\hat A\quad\skew4\vec x\quad\skew4\widehat{AB}""",
            "infix" to """{n\choose k}\quad{n\brack k}\quad{n\brace k}\quad{n+1\brack k_2}""",
            "text-above" to """\text{\'{e} \`{a} \^{o} \~{n} \={a} \u{a} \.{z} \"{o} \r{a} \H{o} \v{c} \t{oo}}""",
            "text-below" to """\text{\d{a}\ \b{b}\ \c{c}\ \'{\i}\ \textbf{\'{E}}\ \textit{\v{S}}\ \l\L\dag\ddag\lq\rq}""",
        )
        for ((name, source) in samples) {
            val result = assertNotNull(exporter.export(source, config, ExportConfig(scale = 2f)))
            File(directory, "$name.png").writeBytes(assertNotNull(result.bytes))
        }
    }

    private fun svg(source: String): String = assertNotNull(exporter.exportSvg(source, config)).svg

    companion object {
        private val fonts by lazy { runBlocking { loadKatexFonts() } }
    }
}
