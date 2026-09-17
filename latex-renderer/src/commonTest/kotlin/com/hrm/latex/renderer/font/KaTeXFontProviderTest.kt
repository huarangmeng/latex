package com.hrm.latex.renderer.font

import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.applyStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KaTeXFontProviderTest {
    private val families = LatexFontFamilies(
        main = FontFamily.Default,
        math = FontFamily.Serif,
        ams = FontFamily.Monospace,
        sansSerif = FontFamily.SansSerif,
        monospace = FontFamily.Monospace,
        caligraphic = FontFamily.Cursive,
        fraktur = FontFamily.Cursive,
        script = FontFamily.Cursive,
        size1 = FontFamily.Serif,
        size2 = FontFamily.Serif,
        size3 = FontFamily.Serif,
        size4 = FontFamily.Serif
    )

    @Test
    fun defaultUsesKaTeXTtfProvider() {
        assertIs<TtfFontSetProvider>(MathFontProviderFactory.create(families))
    }

    @Test
    fun exposesKaTeXTextStyleMetrics() {
        val provider = TtfFontSetProvider(families)
        val em = 100f

        assertEquals(25f, provider.axisHeight(em), 0.0001f)
        assertEquals(4f, provider.fractionRuleThickness(em), 0.0001f)
        assertEquals(67.7f, provider.fractionNumeratorShiftUp(em, true, true), 0.0001f)
        assertEquals(39.4f, provider.fractionNumeratorShiftUp(em, false, true), 0.0001f)
        assertEquals(44.4f, provider.fractionNumeratorShiftUp(em, false, false), 0.0001f)
        assertEquals(68.6f, provider.fractionDenominatorShiftDown(em, true), 0.0001f)
        assertEquals(41.3f, provider.superscriptShiftUp(em, true, false), 0.0001f)
        assertEquals(36.3f, provider.superscriptShiftUp(em, false, false), 0.0001f)
        assertEquals(28.9f, provider.superscriptShiftUp(em, false, true), 0.0001f)
        assertEquals(28.9f, provider.superscriptShiftUp(em, true, true), 0.0001f)
        assertEquals(24.7f, provider.subscriptShiftDown(em, true), 0.0001f)
        assertEquals(43.1f, provider.xHeight(em), 0.0001f)
        assertEquals(14.775f, provider.radicalDisplayVerticalGap(em), 0.0001f)
        assertEquals(5f, provider.radicalVerticalGap(em), 0.0001f)
        assertEquals(11.1f, provider.upperLimitGap(em, limitDepthPx = 10f), 0.0001f)
        assertEquals(30f, provider.lowerLimitGap(em, limitHeightPx = 30f), 0.0001f)
        assertEquals(19.445f, em * KaTeXFontMetrics.integralItalicCorrection(false), 0.0001f)
        assertEquals(44.445f, em * KaTeXFontMetrics.integralItalicCorrection(true), 0.0001f)
        assertEquals(5.556f, em * KaTeXFontMetrics.skew('r'.code), 0.0001f)
        assertEquals(
            69.444f,
            em * KaTeXFontMetrics.mainAccentVerticalMetrics('^'.code).height,
            0.0001f
        )
        assertEquals(
            35f,
            em * KaTeXFontMetrics.mainAccentVerticalMetrics('~'.code).depth,
            0.0001f
        )
        assertEquals(
            28.114f,
            provider.topAccentAttachment("r", em, glyphAdvancePx = 45.116f),
            0.0001f
        )
    }

    @Test
    fun styleCommandsSelectKaTeXFamiliesAndWeights() {
        val context = RenderContext(
            fontSize = 20.sp,
            color = Color.Black,
            mathStyle = MathStyle.TEXT,
            fontFamily = families.main,
            fontFamilies = families
        )

        val bold = context.applyStyle(LatexNode.Style.StyleType.BOLD)
        assertEquals(families.main, bold.fontFamily)
        assertEquals(FontWeight.Bold, bold.fontWeight)
        assertEquals(FontStyle.Normal, bold.fontStyle)

        val boldSymbol = context.applyStyle(LatexNode.Style.StyleType.BOLD_SYMBOL)
        assertEquals(FontWeight.Bold, boldSymbol.fontWeight)

        val italic = context.applyStyle(LatexNode.Style.StyleType.ITALIC)
        assertEquals(families.main, italic.fontFamily)
        assertEquals(FontStyle.Italic, italic.fontStyle)
    }
}
