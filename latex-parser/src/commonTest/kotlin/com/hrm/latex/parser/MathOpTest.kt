/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MathOpTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_basic_mathop() {
        val result = parser.parse("\\mathop{T}")
        val children = result.children
        assertEquals(1, children.size)
        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals("T", node.operator)
        assertNull(node.subscript)
        assertNull(node.superscript)
    }

    @Test
    fun should_parse_mathop_with_subscript() {
        val result = parser.parse("\\mathop{max}_{x}")
        val children = result.children
        assertEquals(1, children.size)
        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals("max", node.operator)
        assertNotNull(node.subscript)
        assertNull(node.superscript)
    }

    @Test
    fun should_parse_mathop_with_both_scripts() {
        val result = parser.parse("\\mathop{op}_{i=0}^{n}")
        val children = result.children
        assertEquals(1, children.size)
        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals("op", node.operator)
        assertNotNull(node.subscript)
        assertNotNull(node.superscript)
    }

    @Test
    fun should_parse_mathop_with_limits() {
        val result = parser.parse("\\mathop{op}\\limits_{a}^{b}")
        val children = result.children
        assertEquals(1, children.size)
        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals("op", node.operator)
        assertEquals(LatexNode.BigOperator.LimitsMode.LIMITS, node.limitsMode)
        assertNotNull(node.subscript)
        assertNotNull(node.superscript)
    }

    @Test
    fun should_parse_mathop_with_nolimits() {
        val result = parser.parse("\\mathop{op}\\nolimits_{a}")
        val children = result.children
        assertEquals(1, children.size)
        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals(LatexNode.BigOperator.LimitsMode.NOLIMITS, node.limitsMode)
    }

    @Test
    fun should_parse_mathop_in_expression() {
        val result = parser.parse("\\mathop{Res}_{z=0} f(z)")
        val children = result.children
        // First: BigOperator, then text content
        assertIs<LatexNode.BigOperator>(children[0])
        assertEquals("Res", (children[0] as LatexNode.BigOperator).operator)
    }

    @Test
    fun should_parse_mathop_without_scripts() {
        val result = parser.parse("\\mathop{custom} x")
        val children = result.children
        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals("custom", node.operator)
        assertEquals(LatexNode.BigOperator.LimitsMode.AUTO, node.limitsMode)
    }

    @Test
    fun should_parse_mathop_wrapping_existing_big_operator() {
        val result = parser.parse("\\mathop \\prod \\limits_{i = 1}^n")
        val children = result.children
        assertEquals(1, children.size)

        val node = children[0]
        assertIs<LatexNode.BigOperator>(node)
        assertEquals("prod", node.operator)
        assertEquals(LatexNode.BigOperator.LimitsMode.LIMITS, node.limitsMode)
        assertNotNull(node.subscript)
        assertNotNull(node.superscript)
    }
}
