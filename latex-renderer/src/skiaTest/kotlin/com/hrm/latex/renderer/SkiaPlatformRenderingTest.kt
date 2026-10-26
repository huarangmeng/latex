@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.hrm.latex.renderer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.export.ExportConfig
import com.hrm.latex.renderer.export.ImageFormat
import com.hrm.latex.renderer.export.LatexExporterState
import com.hrm.latex.renderer.export.SvgExportConfig
import com.hrm.latex.renderer.font.loadKatexFonts
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.utils.measureGlyphBounds
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.readResourceBytes
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.svg.SVGDOM
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Runs the same byte-font, glyph and export contracts on JVM and Apple Native. */
class SkiaPlatformRenderingTest {
    @Test
    fun byteFontsMeasureGlyphsAndRenderPortableSvgAndEncodedImages() = runTest {
        val resources = mutableListOf<String>()
        val fonts = loadKatexFonts { path ->
            resources += path
            readResourceBytes(path).also { bytes ->
                // Every bundled KaTeX resource is a TrueType font, not an empty fixture.
                assertTrue(bytes.size > 4, path)
                assertEquals(listOf<Byte>(0, 1, 0, 0), bytes.take(4), path)
            }
        }
        assertEquals(20, resources.toSet().size)
        val bytes = assertNotNull(fonts.mainBytes)
        val glyph = assertNotNull(measureGlyphBounds("Ag", 24f, bytes))
        assertTrue(glyph.ascentPx > 0f)
        assertTrue(glyph.descentPx > 0f)
        assertTrue(glyph.inkWidth > 0f)
        val largeGlyph = assertNotNull(measureGlyphBounds("Ag", 48f, bytes))
        assertTrue(largeGlyph.inkHeight > glyph.inkHeight * 1.5f)
        assertTrue(largeGlyph.inkWidth > glyph.inkWidth * 1.5f)
        val density = Density(1f)
        val exporter = LatexExporterState(
            density,
            TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr),
            fonts,
        )
        val formula = """\frac{a}{b} + \sqrt{x} + \mathbb{R}"""
        val config = LatexConfig(fontSize = 24.sp)
        val svg = assertNotNull(
            exporter.exportSvg(formula, config, SvgExportConfig(transparentBackground = true)),
        )
        assertContains(svg.svg, "<path")
        assertFalse("<text" in svg.svg, "Portable SVG must embed byte-font outlines")
        assertFalse("<image" in svg.svg)
        val width = svg.width
        val height = svg.height
        Data.makeFromBytes(svg.bytes).use { data ->
            SVGDOM(data).use { dom ->
                Surface.makeRasterN32Premul(width, height).use { surface ->
                    surface.canvas.clear(Color.WHITE)
                    dom.setContainerSize(width.toFloat(), height.toFloat())
                    dom.render(surface.canvas)
                    surface.makeImageSnapshot().use { assertFormulaPixels(it) }
                }
            }
        }
        for (format in ImageFormat.entries) {
            val result = assertNotNull(
                exporter.export(formula, config, ExportConfig(scale = 1f, format = format)),
            )
            Image.makeFromEncoded(assertNotNull(result.bytes)).use { image ->
                assertEquals(result.width, image.width)
                assertEquals(result.height, image.height)
                assertFormulaPixels(image)
            }
        }
    }

    /** Decode and inspect actual ink; nonempty XML/encoded bytes cannot prove rendering. */
    private fun assertFormulaPixels(image: Image) {
        Bitmap().use { bitmap ->
            assertTrue(bitmap.allocN32Pixels(image.width, image.height))
            assertTrue(image.readPixels(bitmap))
            var inkPixels = 0
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getColor(x, y)
                    if (Color.getA(pixel) > 128 && Color.getR(pixel) < 128 &&
                        Color.getG(pixel) < 128 && Color.getB(pixel) < 128
                    ) inkPixels++
                }
            }
            assertTrue(inkPixels > 20, "Expected formula ink, found $inkPixels pixels")
        }
    }
}
