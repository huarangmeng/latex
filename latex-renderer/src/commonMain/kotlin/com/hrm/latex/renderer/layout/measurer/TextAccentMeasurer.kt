package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.TextAccent.Kind
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.GlyphBounds
import com.hrm.latex.renderer.utils.measureGlyphBounds
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/** Lay out the font's spacing-accent glyphs; no combining-character shaping is required. */
internal class TextAccentMeasurer : NodeMeasurer {
    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(LatexNode.TextAccent::class)

    override fun measure(
        node: LatexNode, context: RenderContext, measurer: TextMeasurer, density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.TextAccent
        val textContext = context.copy(
            inTextMode = true,
            fontFamily = context.fontFamily ?: context.fontFamilies?.main,
            fontStyle = context.fontStyle ?: FontStyle.Normal
        )
        val base = measureNode(node.content, textContext)
        val text = plainText(node.content)
        val smallCap = context.smallCaps && text.firstOrNull()?.isLowerCase() == true
        val markContext = if (smallCap) textContext.copy(fontSize = textContext.fontSize * 0.8f) else textContext
        val em = with(density) { markContext.fontSize.toPx() }
        val markText = textAccentGlyph(node.accent)
        val mark = measurer.measure(AnnotatedString(markText), markContext.textStyle())
        val bytes = context.fontFamilies?.mainBytes
        val markBounds = bytes?.let { measureGlyphBounds(markText, em, it) }
            ?: GlyphBounds(mark.firstBaseline, mark.size.height - mark.firstBaseline, mark.size.width.toFloat())
        val baseBounds = bytes?.let { measureGlyphBounds(if (smallCap) text.uppercase() else text, em, it) }
        val nested = hasAccent(node.content)
        val baseAscent = if (nested) base.baseline else baseBounds?.ascentPx ?: base.baseline
        val baseDepth = if (nested) base.height - base.baseline else baseBounds?.descentPx ?: 0f
        val xHeight = context.mathFontProvider?.xHeight(em) ?: em * 0.431f
        val under = node.accent == Kind.CEDILLA || node.accent == Kind.DOT_BELOW || node.accent == Kind.BAR_BELOW
        val gap = if (node.accent == Kind.CEDILLA) 0f else xHeight / 4f
        val markBaseline = when {
            under -> base.baseline + baseDepth + gap - markBounds.inkTopPx
            node.accent == Kind.TIE -> {
                val rule = context.mathFontProvider?.fractionRuleThickness(em) ?: em * 0.04f
                base.baseline - baseAscent - rule - markBounds.inkBottomPx
            }
            // TeX Rule 12: raise an accent by the base height less min(base height, x-height).
            else -> base.baseline - baseAscent + min(baseAscent, xHeight)
        }
        // The bundled fonts lack U+0361. Use their cap-shaped relation glyph as a spanning tie.
        val scaleX = if (node.accent == Kind.TIE && mark.size.width > 0) base.width / mark.size.width else 1f
        val markWidth = mark.size.width * scaleX
        val markLeft = (base.width - markWidth) / 2f
        val top = min(0f, markBaseline + markBounds.inkTopPx)
        val bottom = max(base.height, markBaseline + markBounds.inkBottomPx)
        val left = min(0f, markLeft)
        val right = max(base.width, markLeft + markWidth)
        return NodeLayout(right - left, bottom - top, base.baseline - top) { x, y ->
            base.draw(this, x - left, y - top)
            withTransform({
                translate(x + markLeft - left, y + markBaseline - top - mark.firstBaseline)
                scale(scaleX, 1f, pivot = Offset.Zero)
            }) {
                drawText(mark, topLeft = Offset.Zero)
            }
        }
    }

    private fun hasAccent(node: LatexNode): Boolean = node is LatexNode.TextAccent || node.children().any(::hasAccent)

    private fun plainText(node: LatexNode): String = when (node) {
        is LatexNode.Text -> node.content
        is LatexNode.TextMode -> if (node.content.isEmpty()) node.text else node.content.joinToString("") { plainText(it) }
        else -> node.children().joinToString("") { plainText(it) }
    }
}

/** Spacing glyphs present in KaTeX Main; these differ from the U+0300 combining block. */
internal fun textAccentGlyph(kind: Kind): String = when (kind) {
    Kind.ACUTE -> "\u02CA"
    Kind.GRAVE -> "\u02CB"
    Kind.CIRCUMFLEX -> "\u02C6"
    Kind.TILDE -> "\u02DC"
    Kind.MACRON, Kind.BAR_BELOW -> "\u02C9"
    Kind.BREVE -> "\u02D8"
    Kind.DOT -> "\u02D9"
    Kind.DIAERESIS -> "\u00A8"
    Kind.RING -> "\u02DA"
    Kind.DOUBLE_ACUTE -> "\u02DD"
    Kind.CARON -> "\u02C7"
    Kind.CEDILLA -> "\u00B8"
    Kind.DOT_BELOW -> "."
    Kind.TIE -> "\u2322"
}
