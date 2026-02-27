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

package com.hrm.latex.renderer.editor

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.renderer.layout.LayoutMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CursorCalculatorTest {

    private fun createTestLayoutMap(): LayoutMap {
        val layoutMap = LayoutMap()
        // 模拟 "x + y" 的布局：3 个节点，水平排列
        // "x" at [0, 1), relX=0, width=10
        // "+" at [2, 3), relX=15, width=10
        // "y" at [4, 5), relX=30, width=10
        layoutMap.add(
            node = LatexNode.Text("x", sourceRange = SourceRange(0, 1)),
            relX = 0f, relY = 0f,
            width = 10f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Operator("+", sourceRange = SourceRange(2, 3)),
            relX = 15f, relY = 0f,
            width = 10f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Text("y", sourceRange = SourceRange(4, 5)),
            relX = 30f, relY = 0f,
            width = 10f, height = 20f, baseline = 15f
        )
        return layoutMap
    }

    // ========== calculate ==========

    @Test
    fun should_calculate_cursor_at_node_start() {
        val layoutMap = createTestLayoutMap()
        val pos = CursorCalculator.calculate(
            cursorOffset = 0,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // offset 0 is at start of "x" node: relX=0, ratio=0/1=0, x = 0 + 10*0 + 5 = 5
        assertEquals(5f, pos.x, 0.1f)
        assertEquals(3f, pos.y, 0.1f)
        assertEquals(20f, pos.height, 0.1f)
    }

    @Test
    fun should_calculate_cursor_at_node_end_region() {
        val layoutMap = createTestLayoutMap()
        val pos = CursorCalculator.calculate(
            cursorOffset = 4,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // offset 4 is at start of "y" node: range [4,5), ratio=0/1=0
        assertEquals(30f + 5f, pos.x, 0.1f)
    }

    @Test
    fun should_return_null_for_empty_layout_map() {
        val layoutMap = LayoutMap()
        val pos = CursorCalculator.calculate(
            cursorOffset = 0,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNull(pos)
    }

    @Test
    fun should_find_nearest_position_for_offset_between_nodes() {
        val layoutMap = createTestLayoutMap()
        // offset 1 is between "x" [0,1) and "+" [2,3) — not contained in any node
        // offset 1 == "x".sourceRange.end, so should use right-edge matching
        val pos = CursorCalculator.calculate(
            cursorOffset = 1,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // "x" node: relX=0, width=10, right edge = 10, + padding 5 = 15
        assertEquals(15f, pos.x, 0.1f)
    }

    @Test
    fun should_position_cursor_at_right_edge_of_last_node() {
        val layoutMap = createTestLayoutMap()
        // offset 5 == "y".sourceRange.end, cursor at end of last node
        val pos = CursorCalculator.calculate(
            cursorOffset = 5,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // "y" node: relX=30, width=10, right edge = 40, + padding 5 = 45
        assertEquals(45f, pos.x, 0.1f)
        assertEquals(3f, pos.y, 0.1f)
        assertEquals(20f, pos.height, 0.1f)
    }

    @Test
    fun should_position_cursor_after_equals_sign() {
        // 模拟 "f(x) = " 中 "=" 后的空格后面
        val layoutMap = LayoutMap()
        layoutMap.add(
            node = LatexNode.Text("f", sourceRange = SourceRange(0, 1)),
            relX = 0f, relY = 0f,
            width = 8f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Text("(", sourceRange = SourceRange(1, 2)),
            relX = 8f, relY = 0f,
            width = 5f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Text("x", sourceRange = SourceRange(2, 3)),
            relX = 13f, relY = 0f,
            width = 8f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Text(")", sourceRange = SourceRange(3, 4)),
            relX = 21f, relY = 0f,
            width = 5f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Operator("=", sourceRange = SourceRange(5, 6)),
            relX = 30f, relY = 0f,
            width = 10f, height = 20f, baseline = 15f
        )
        layoutMap.add(
            node = LatexNode.Space(type = LatexNode.Space.SpaceType.NORMAL, sourceRange = SourceRange(6, 7)),
            relX = 40f, relY = 0f,
            width = 4f, height = 20f, baseline = 15f
        )

        // cursorOffset = 7 (at end of text, after trailing space)
        val pos = CursorCalculator.calculate(
            cursorOffset = 7,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // Space node: relX=40, width=4, right edge=44, + padding 5 = 49
        assertEquals(49f, pos.x, 0.1f)
    }

    @Test
    fun should_position_cursor_past_all_nodes() {
        val layoutMap = createTestLayoutMap()
        // offset 10, well past all nodes — should still find nearest left node
        val pos = CursorCalculator.calculate(
            cursorOffset = 10,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // "y" is closest (end=5, smallest distance), cursor at its right edge
        // relX=30, width=10 → 40 + padding 5 = 45
        assertEquals(45f, pos.x, 0.1f)
    }

    // ========== hitTestToOffset ==========

    @Test
    fun should_hit_test_inside_first_node() {
        val layoutMap = createTestLayoutMap()
        val offset = CursorCalculator.hitTestToOffset(
            px = 10f, // 5 padding + 5 into node
            py = 13f, // 3 padding + 10 into node
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 5
        )
        // contentX = 5, in "x" node (relX=0, width=10), ratio = 5/10 = 0.5
        // offset = 0 + (0.5 * 1).toInt() = 0
        assertEquals(0, offset)
    }

    @Test
    fun should_hit_test_inside_last_node() {
        val layoutMap = createTestLayoutMap()
        val offset = CursorCalculator.hitTestToOffset(
            px = 40f, // 5 padding + 35 into content
            py = 13f,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 5
        )
        // contentX = 35, in "y" node (relX=30, width=10), ratio = 5/10 = 0.5
        // offset = 4 + (0.5 * 1).toInt() = 4
        assertEquals(4, offset)
    }

    @Test
    fun should_return_text_length_for_hit_outside_all_nodes() {
        val layoutMap = createTestLayoutMap()
        val offset = CursorCalculator.hitTestToOffset(
            px = 100f, // 远超布局范围
            py = 100f,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 5
        )
        assertEquals(5, offset)
    }

    // ========== 复合节点 hit-test ==========

    @Test
    fun should_hit_test_fraction_numerator() {
        // 模拟 \frac{ab}{xy}：sourceRange = [0, 14)
        // numerator "ab": sourceRange = [6, 8)
        // denominator "xy": sourceRange = [9, 11)
        val layoutMap = LayoutMap()
        val fracNode = LatexNode.Fraction(
            numerator = LatexNode.Text("ab", sourceRange = SourceRange(6, 8)),
            denominator = LatexNode.Text("xy", sourceRange = SourceRange(9, 11)),
            sourceRange = SourceRange(0, 14)
        )
        layoutMap.add(
            node = fracNode,
            relX = 0f, relY = 0f,
            width = 40f, height = 60f, baseline = 30f
        )

        // 点击上半区域（分子）：py = 3 + 10 = 13（在分数上半区，即 localY=10 < 30=midY）
        val offset = CursorCalculator.hitTestToOffset(
            px = 25f,  // 5 padding + 20 into node
            py = 13f,  // 3 padding + 10 (上半区)
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 14
        )
        // 应映射到分子 "ab" 的 sourceRange [6, 8)
        assertTrue(offset in 6..8, "Expected offset in numerator range [6,8], got $offset")
    }

    @Test
    fun should_hit_test_fraction_denominator() {
        val layoutMap = LayoutMap()
        val fracNode = LatexNode.Fraction(
            numerator = LatexNode.Text("ab", sourceRange = SourceRange(6, 8)),
            denominator = LatexNode.Text("xy", sourceRange = SourceRange(9, 11)),
            sourceRange = SourceRange(0, 14)
        )
        layoutMap.add(
            node = fracNode,
            relX = 0f, relY = 0f,
            width = 40f, height = 60f, baseline = 30f
        )

        // 点击下半区域（分母）：py = 3 + 45 = 48（localY=45 > 30=midY）
        val offset = CursorCalculator.hitTestToOffset(
            px = 25f,
            py = 48f,  // 3 padding + 45 (下半区)
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 14
        )
        // 应映射到分母 "xy" 的 sourceRange [9, 11)
        assertTrue(offset in 9..11, "Expected offset in denominator range [9,11], got $offset")
    }

    @Test
    fun should_hit_test_superscript_exponent() {
        // 模拟 x^{2}：sourceRange = [0, 5)
        // base "x": sourceRange = [0, 1)
        // exponent "2": sourceRange = [3, 4)
        val layoutMap = LayoutMap()
        val supNode = LatexNode.Superscript(
            base = LatexNode.Text("x", sourceRange = SourceRange(0, 1)),
            exponent = LatexNode.Text("2", sourceRange = SourceRange(3, 4)),
            sourceRange = SourceRange(0, 5)
        )
        layoutMap.add(
            node = supNode,
            relX = 0f, relY = 0f,
            width = 20f, height = 20f, baseline = 15f
        )

        // 点击右侧区域（上标）：px = 5 + 15 = 20（localX=15 > 20*0.6=12）
        val offset = CursorCalculator.hitTestToOffset(
            px = 20f,
            py = 8f,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 5
        )
        // 应映射到上标 "2" 的 sourceRange [3, 4)
        assertTrue(offset in 3..4, "Expected offset in exponent range [3,4], got $offset")
    }

    @Test
    fun should_hit_test_superscript_base() {
        val layoutMap = LayoutMap()
        val supNode = LatexNode.Superscript(
            base = LatexNode.Text("x", sourceRange = SourceRange(0, 1)),
            exponent = LatexNode.Text("2", sourceRange = SourceRange(3, 4)),
            sourceRange = SourceRange(0, 5)
        )
        layoutMap.add(
            node = supNode,
            relX = 0f, relY = 0f,
            width = 20f, height = 20f, baseline = 15f
        )

        // 点击左侧区域（基底）：px = 5 + 5 = 10（localX=5 < 20*0.6=12）
        val offset = CursorCalculator.hitTestToOffset(
            px = 10f,
            py = 13f,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f,
            textLength = 5
        )
        // 应映射到基底 "x" 的 sourceRange [0, 1)
        assertTrue(offset in 0..1, "Expected offset in base range [0,1], got $offset")
    }

    // ========== 复合节点光标定位 ==========

    @Test
    fun should_calculate_cursor_in_fraction_numerator() {
        val layoutMap = LayoutMap()
        val fracNode = LatexNode.Fraction(
            numerator = LatexNode.Text("ab", sourceRange = SourceRange(6, 8)),
            denominator = LatexNode.Text("xy", sourceRange = SourceRange(9, 11)),
            sourceRange = SourceRange(0, 14)
        )
        layoutMap.add(
            node = fracNode,
            relX = 0f, relY = 0f,
            width = 40f, height = 60f, baseline = 30f
        )

        // cursorOffset = 7，在分子 "ab" [6,8) 中间
        val pos = CursorCalculator.calculate(
            cursorOffset = 7,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // 应在上半区域（分子区域高度 = 60/2 = 30）
        assertEquals(3f, pos.y, 0.1f) // relY=0 + yStart=0 + padding=3
        assertEquals(30f, pos.height, 0.1f) // 上半区高度
    }

    @Test
    fun should_calculate_cursor_in_fraction_denominator() {
        val layoutMap = LayoutMap()
        val fracNode = LatexNode.Fraction(
            numerator = LatexNode.Text("ab", sourceRange = SourceRange(6, 8)),
            denominator = LatexNode.Text("xy", sourceRange = SourceRange(9, 11)),
            sourceRange = SourceRange(0, 14)
        )
        layoutMap.add(
            node = fracNode,
            relX = 0f, relY = 0f,
            width = 40f, height = 60f, baseline = 30f
        )

        // cursorOffset = 10，在分母 "xy" [9,11) 中间
        val pos = CursorCalculator.calculate(
            cursorOffset = 10,
            layoutMap = layoutMap,
            horizontalPadding = 5f,
            verticalPadding = 3f
        )
        assertNotNull(pos)
        // 应在下半区域
        assertEquals(33f, pos.y, 0.1f) // relY=0 + midY=30 + padding=3
        assertEquals(30f, pos.height, 0.1f) // 下半区高度
    }
}
