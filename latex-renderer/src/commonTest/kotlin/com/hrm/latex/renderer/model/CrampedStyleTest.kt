package com.hrm.latex.renderer.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrampedStyleTest {
    private val context = RenderContext(fontSize = 20.sp, color = Color.Black)

    @Test
    fun scriptsAndFractionsUseCrampedOnlyWhereTeXRequiresIt() {
        assertFalse(context.toScriptStyle().isCramped)
        assertTrue(context.toScriptStyle(isSubscript = true).isCramped)
        assertFalse(context.toFractionChildStyle().isCramped)
        assertTrue(context.toFractionChildStyle(isDenominator = true).isCramped)
        assertTrue(context.toLimitStyle(isLowerLimit = true).isCramped)
        assertTrue(context.copy(isCramped = true).toScriptStyle().isCramped)
        assertTrue(context.copy(isCramped = true).toFractionChildStyle().isCramped)
    }

    @Test
    fun explicitStylesResetOrForceCrampedState() {
        val cramped = context.applyMathStyle(LatexNode.MathStyle.MathStyleType.CRAMPED)
        assertTrue(cramped.isCramped)
        assertEquals(MathStyle.DISPLAY, cramped.mathStyle)
        val script = cramped.applyMathStyle(LatexNode.MathStyle.MathStyleType.CRAMPED_SCRIPT)
        assertTrue(script.isCramped)
        assertEquals(MathStyle.SCRIPT, script.mathStyle)
        assertFalse(script.applyMathStyle(LatexNode.MathStyle.MathStyleType.TEXT).isCramped)
    }
}
