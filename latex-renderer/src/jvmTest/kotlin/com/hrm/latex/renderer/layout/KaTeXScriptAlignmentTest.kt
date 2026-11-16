package com.hrm.latex.renderer.layout

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
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.font.loadKatexFonts
import com.hrm.latex.renderer.layout.measurer.ScriptMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.toContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KaTeXScriptAlignmentTest {
    private val density = Density(1f)
    private val measurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)

    @Test
    fun superscriptOriginsMatchKaTeXFontMetricsAndRule18() {
        // KaTeX 0.16.22 __renderToHTMLTree: base advance + italic correction;
        // sup1 = .413em (display), sup2 = .363em (text), script size = .7em.
        for (em in listOf(20, 40)) for (style in listOf(MathStyle.DISPLAY, MathStyle.TEXT)) {
            val context = LatexConfig(fontSize = em.sp).toContext(false, fonts).copy(mathStyle = style)
            for ((letter, referenceAdvance) in listOf("x" to 0.57153f, "f" to 0.59723f, "A" to 0.75f)) {
                val base = LatexNode.Text(letter)
                val script = LatexNode.Text("2")
                val origins = mutableMapOf<LatexNode, Offset>()
                val baselines = mutableMapOf<LatexNode, Float>()
                val layout = ScriptMeasurer().measure(LatexNode.Superscript(base, script), context, measurer, density,
                    measureNode = { child, childContext ->
                        val measured = measureNode(child, childContext, measurer, density)
                        baselines[child] = measured.baseline
                        NodeLayout(measured.width, measured.height, measured.baseline, measured.italicCorrection) { x, y ->
                            origins[child] = Offset(x, y)
                            measured.draw(this, x, y)
                        }
                    }, measureGroup = { _, _ -> error("Unexpected group measurement") })
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(ImageBitmap(200, 200)), Size(200f, 200f)) {
                    layout.draw(this, 0f, 0f)
                }
                val baseOrigin = assertNotNull(origins[base])
                val scriptOrigin = assertNotNull(origins[script])
                val baselineShift = baseOrigin.y + assertNotNull(baselines[base]) -
                    scriptOrigin.y - assertNotNull(baselines[script])
                // Compose rounds text advances to whole pixels; placement may differ by < 1px.
                assertEquals(em * referenceAdvance, scriptOrigin.x - baseOrigin.x, 1f, "$letter / $style / $em")
                assertEquals(em * if (style == MathStyle.DISPLAY) 0.413f else 0.363f,
                    baselineShift, 0.001f, "$letter / $style / $em")
            }
        }
    }

    companion object {
        private val fonts by lazy { runBlocking { loadKatexFonts() } }
    }
}
