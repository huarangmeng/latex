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

    @Test
    fun should_honor_explicit_math_class() {
        val bin = LatexNode.MathClass(listOf(LatexNode.Text("x")), LatexNode.MathClass.AtomClass.BIN)
        val rel = LatexNode.MathClass(listOf(LatexNode.Text("R")), LatexNode.MathClass.AtomClass.REL)
        val open = LatexNode.MathClass(listOf(LatexNode.Text("[")), LatexNode.MathClass.AtomClass.OPEN)

        assertEquals(AtomType.BIN, MathSpacing.classifyNode(bin))
        assertEquals(AtomType.REL, MathSpacing.classifyNode(rel))
        assertEquals(AtomType.OPEN, MathSpacing.classifyNode(open))
    }

    @Test
    fun explicit_bin_class_between_operands_stays_binary() {
        val nodes = listOf(
            LatexNode.Text("a"),
            LatexNode.MathClass(listOf(LatexNode.Text("R")), LatexNode.MathClass.AtomClass.BIN),
            LatexNode.Text("b")
        )

        // 显式 \mathbin 在两个普通原子之间应保持二元运算符间距
        assertEquals(AtomType.BIN, MathSpacing.effectiveAtomType(nodes, 1))
    }
}
