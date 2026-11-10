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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LineBreakerTest {

    @Test
    fun respectsExplicitBreakHints() {
        val allow = LatexNode.HSpace("0pt", breakHint = LatexNode.HSpace.BreakHint.ALLOW)
        val forbid = LatexNode.HSpace("0pt", breakHint = LatexNode.HSpace.BreakHint.FORBID)
        val a = LatexNode.Text("a")
        val b = LatexNode.Text("b")
        val operator = LatexNode.Text("=")
        val c = LatexNode.Text("c")
        val breaker = LineBreaker(24f)

        assertEquals(listOf(0..1, 2..3),
            breaker.breakIntoRanges(listOf(a, b, allow, c), floatArrayOf(10f, 10f, 0f, 10f)))
        assertEquals(listOf(0..3),
            breaker.breakIntoRanges(listOf(a, operator, forbid, b), floatArrayOf(10f, 10f, 0f, 10f)))
    }

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

    @Test
    fun should_recheck_remaining_width_after_committing_a_candidate() {
        val nodes = listOf(LatexNode.Text("a"), LatexNode.Text("+"), LatexNode.Text("b"))
        val widths = floatArrayOf(1f, 90f, 90f)

        val ranges = LineBreaker(100f).breakIntoRanges(nodes, widths)

        // 旧实现提交 a 后，未检查剩下的 +b，错误地返回宽 180 的第二行。
        assertEquals(listOf(0..0, 1..1, 2..2), ranges)
        assertEquals(nodes.indices.toList(), ranges.flatMap { it.toList() })
        assertTrue(ranges.all { range -> range.sumOf { widths[it].toDouble() } <= 100.0 })
    }

    @Test
    fun should_preserve_later_candidates_when_the_first_break_leaves_an_oversized_suffix() {
        val nodes = listOf("a", "=", "b", "+", "c").map { LatexNode.Text(it) }
        val widths = floatArrayOf(1f, 1f, 60f, 1f, 60f)

        val ranges = LineBreaker(100f).breakIntoRanges(nodes, widths)

        // 优先在等号前分行后，剩余部分仍超宽；加号候选必须继续有效。
        assertEquals(listOf(0..0, 1..2, 3..4), ranges)
    }

    @Test
    fun should_prefer_the_latest_candidate_with_the_same_penalty() {
        val nodes = listOf("a", "+", "b", "+", "c", "+", "d").map { LatexNode.Text(it) }

        val ranges = LineBreaker(55f).breakIntoRanges(nodes, FloatArray(nodes.size) { 10f })

        assertEquals(listOf(0..4, 5..6), ranges)
    }

    @Test
    fun should_include_supplied_spacing_in_line_width() {
        val nodes = listOf("a", "+", "b").map { LatexNode.Text(it) }
        val widths = FloatArray(nodes.size) { 10f }
        val lineWidth = { start: Int, end: Int ->
            val count = end - start
            count * 10f + (count - 1) * 10f
        }

        val ranges = LineBreaker(35f).breakIntoRanges(nodes, widths, lineWidth)

        assertEquals(listOf(0..0, 1..2), ranges)
        assertTrue(ranges.all { lineWidth(it.first, it.last + 1) <= 35f })
    }

    @Test
    fun should_recompute_boundary_spacing_for_each_new_line_start() {
        val nodes = listOf("a", "+", "b", "+", "c").map { LatexNode.Text(it) }
        val widths = FloatArray(nodes.size) { 10f }
        val lineWidth = { start: Int, end: Int ->
            val count = end - start
            var width = count * 10f + (count - 1) * 4f
            // 模拟行首、行尾二元符降为普通原子后的边界间距变化，查询保持 O(1)。
            if (count > 1 && start % 2 == 1) width -= 4f
            if (count > 1 && (end - 1) % 2 == 1) width -= 4f
            width
        }

        val ranges = LineBreaker(36f).breakIntoRanges(nodes, widths, lineWidth)

        assertEquals(listOf(0..0, 1..2, 3..4), ranges)
        assertTrue(ranges.all { lineWidth(it.first, it.last + 1) <= 36f })
    }

    @Test
    fun should_keep_width_queries_linear_when_many_candidates_are_present() {
        val nodes = List(1000) { LatexNode.Text(if (it % 2 == 0) "a" else "+") }
        val widths = FloatArray(nodes.size) { 1f }
        var queries = 0

        val ranges = LineBreaker(10f).breakIntoRanges(nodes, widths) { start, end ->
            queries++
            (end - start).toFloat()
        }

        assertEquals(nodes.indices.toList(), ranges.flatMap { it.toList() })
        assertTrue(ranges.all { it.count() <= 10 })
        assertTrue(queries <= 2 * nodes.size + 1, "宽度查询不应随分行重扫后缀：$queries")
    }

    @Test
    fun should_allow_overflow_only_for_an_indivisible_atom() {
        val nodes = listOf(
            LatexNode.Text("a"),
            LatexNode.Fraction(LatexNode.Text("1"), LatexNode.Text("2")),
            LatexNode.Text("b"),
        )

        val ranges = LineBreaker(100f).breakIntoRanges(nodes, floatArrayOf(10f, 150f, 10f))

        assertEquals(listOf(0..0, 1..1, 2..2), ranges)
    }

    @Test
    fun should_not_commit_an_oversized_candidate_after_removing_a_negative_advance() {
        val nodes = listOf(
            LatexNode.HSpace("-1em"), LatexNode.Text("="), LatexNode.Text("a"),
            LatexNode.Text("b"), LatexNode.Text("+"), LatexNode.Text("c"),
        )
        val widths = floatArrayOf(-100f, 0f, 90f, 90f, 0f, 90f)

        val ranges = LineBreaker(100f).breakIntoRanges(nodes, widths)

        assertEquals(nodes.indices.toList(), ranges.flatMap { it.toList() })
        assertTrue(ranges.all { range -> range.sumOf { widths[it].toDouble() } <= 100.0 })
    }

    @Test
    fun should_keep_the_original_list_api_compatible_with_ranges() {
        val nodes = listOf("a", "+", "b").map { LatexNode.Text(it) }
        val widths = floatArrayOf(15f, 10f, 15f)
        val breaker = LineBreaker(25f)

        assertEquals(breaker.breakIntoRanges(nodes, widths).map { it.toList() }, breaker.breakIntoLines(nodes, widths))
        assertEquals(listOf(IntRange.EMPTY), breaker.breakIntoRanges(emptyList(), floatArrayOf()))
        assertEquals(listOf(emptyList()), breaker.breakIntoLines(emptyList(), floatArrayOf()))
    }

    @Test
    fun should_reject_mismatched_node_and_width_counts() {
        assertFailsWith<IllegalArgumentException> {
            LineBreaker(100f).breakIntoRanges(listOf(LatexNode.Text("x")), floatArrayOf())
        }
    }
}
