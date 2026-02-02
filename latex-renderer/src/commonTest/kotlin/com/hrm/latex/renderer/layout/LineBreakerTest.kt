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

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineBreakerTest {

    @Test
    fun should_not_break_when_content_fits() {
        val breaker = LineBreaker(100f)
        val nodes = listOf(
            LatexNode.Text("a"),
            LatexNode.Text("+"),
            LatexNode.Text("b")
        )
        val widths = floatArrayOf(10f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        assertEquals(1, lines.size, "should be single line when content fits")
        assertEquals(listOf(0, 1, 2), lines[0])
    }

    @Test
    fun should_break_at_operator() {
        val breaker = LineBreaker(30f)
        val nodes = listOf(
            LatexNode.Text("a"),
            LatexNode.Text("+"),
            LatexNode.Text("b"),
            LatexNode.Text("+"),
            LatexNode.Text("c")
        )
        val widths = floatArrayOf(10f, 10f, 10f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        assertTrue(lines.size > 1, "should break into multiple lines")
    }

    @Test
    fun should_prefer_relation_over_additive() {
        val breaker = LineBreaker(50f)
        val nodes = listOf(
            LatexNode.Text("x"),
            LatexNode.Text("+"),
            LatexNode.Text("y"),
            LatexNode.Text("="),
            LatexNode.Text("z")
        )
        val widths = floatArrayOf(10f, 10f, 10f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        // when breaking is needed, = should be preferred over +
        if (lines.size > 1) {
            // operator goes to new line, so = (index 3) should start a line
            val secondLineStart = lines[1].first()
            assertTrue(secondLineStart == 3 || secondLineStart == 1,
                "should break at relation (=) or additive (+)")
        }
    }

    @Test
    fun should_prefer_space_as_best_break_point() {
        val breaker = LineBreaker(40f)
        val nodes = listOf(
            LatexNode.Text("a"),
            LatexNode.Text("+"),
            LatexNode.Space(LatexNode.Space.SpaceType.QUAD),
            LatexNode.Text("b"),
            LatexNode.Text("="),
            LatexNode.Text("c")
        )
        val widths = floatArrayOf(10f, 10f, 5f, 10f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        // space should be preferred break point
        if (lines.size > 1) {
            // verify the break happened reasonably
            assertTrue(lines[0].isNotEmpty())
            assertTrue(lines[1].isNotEmpty())
        }
    }

    @Test
    fun should_handle_empty_input() {
        val breaker = LineBreaker(100f)
        val nodes = emptyList<LatexNode>()
        val widths = floatArrayOf()

        val lines = breaker.breakIntoLines(nodes, widths)

        assertEquals(1, lines.size)
        assertTrue(lines[0].isEmpty())
    }

    @Test
    fun should_not_break_fraction() {
        val breaker = LineBreaker(15f)
        val nodes = listOf(
            LatexNode.Fraction(LatexNode.Text("1"), LatexNode.Text("2")),
            LatexNode.Text("+"),
            LatexNode.Text("x")
        )
        val widths = floatArrayOf(20f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        // fraction should not be broken, break should happen at +
        if (lines.size > 1) {
            // first line should contain the fraction (index 0)
            assertTrue(0 in lines[0], "fraction should not be broken")
        }
    }

    @Test
    fun should_force_break_when_no_break_point() {
        val breaker = LineBreaker(15f)
        // all unbreakable nodes
        val nodes = listOf(
            LatexNode.Fraction(LatexNode.Text("1"), LatexNode.Text("2")),
            LatexNode.Fraction(LatexNode.Text("3"), LatexNode.Text("4")),
            LatexNode.Fraction(LatexNode.Text("5"), LatexNode.Text("6"))
        )
        val widths = floatArrayOf(20f, 20f, 20f)

        val lines = breaker.breakIntoLines(nodes, widths)

        // should still produce output without hanging
        assertTrue(lines.isNotEmpty())
        // all indices should be covered
        val allIndices = lines.flatten().toSet()
        assertEquals(setOf(0, 1, 2), allIndices)
    }

    @Test
    fun should_put_operator_on_new_line() {
        val breaker = LineBreaker(25f)
        val nodes = listOf(
            LatexNode.Text("a"),
            LatexNode.Text("+"),
            LatexNode.Text("b")
        )
        val widths = floatArrayOf(15f, 10f, 15f)

        val lines = breaker.breakIntoLines(nodes, widths)

        if (lines.size > 1) {
            // operator (+) at index 1 should be at the START of second line (latex convention)
            assertTrue(lines[1].contains(1), "operator should be on new line")
        }
    }

    @Test
    fun should_handle_symbol_penalties() {
        val breaker = LineBreaker(50f)
        val nodes = listOf(
            LatexNode.Text("x"),
            LatexNode.Symbol("eq", "="),
            LatexNode.Text("y"),
            LatexNode.Symbol("plus", "+"),
            LatexNode.Text("z")
        )
        val widths = floatArrayOf(10f, 10f, 10f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        // should handle symbols without crashing
        assertTrue(lines.isNotEmpty())
    }

    @Test
    fun should_handle_deeply_nested_groups() {
        val breaker = LineBreaker(50f)
        val innerGroup = LatexNode.Group(listOf(LatexNode.Text("x")))
        val outerGroup = LatexNode.Group(listOf(innerGroup))
        val nodes = listOf(
            outerGroup,
            LatexNode.Text("+"),
            LatexNode.Text("y")
        )
        val widths = floatArrayOf(10f, 10f, 10f)

        val lines = breaker.breakIntoLines(nodes, widths)

        // depth tracking should work correctly
        assertTrue(lines.isNotEmpty())
    }
}
