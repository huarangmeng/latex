package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.utils.AtomType
import com.hrm.latex.renderer.utils.MathSpacing
import kotlin.test.Test
import kotlin.test.assertEquals

class MathSpacingTest {

    @Test
    fun should_treat_leading_minus_as_unary_ord() {
        val nodes = listOf(
            LatexNode.Text("-"),
            LatexNode.Superscript(LatexNode.Text("x"), LatexNode.Text("2"))
        )

        assertEquals(AtomType.ORD, MathSpacing.effectiveAtomType(nodes, 0))
        assertEquals(AtomType.ORD, MathSpacing.effectiveAtomType(nodes, 1))
    }

    @Test
    fun should_keep_middle_minus_as_binary_operator() {
        val nodes = listOf(
            LatexNode.Text("x"),
            LatexNode.Text("-"),
            LatexNode.Text("y")
        )

        assertEquals(AtomType.BIN, MathSpacing.effectiveAtomType(nodes, 1))
    }
}
