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

import com.hrm.latex.parser.LatexParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlotNavigatorTest {

    private val parser = LatexParser()

    // ========== Fraction ==========

    @Test
    fun should_navigate_from_fraction_numerator_to_denominator() {
        // \frac{a}{b}
        // 0123456789...
        val text = "\\frac{a}{b}"
        val doc = parser.parse(text)

        // 光标在分子 'a' 之后 = offset 7 (在 'a' 和 '}' 之间)
        // 但 SourceRange 使用半开区间，Group {a} 的 range 约为 [5, 8)
        // 光标在 offset 7 = 在 'a' 之后，在 '}' 之前
        val cursorInNumerator = 7
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInNumerator, doc)
        assertNotNull(nextOffset, "Should find next slot for fraction numerator")

        // 下一个 slot 应该是分母 Group 内部（{b} 的 start + 1）
        // 分母 Group {b} 大约在 offset 8..11，内容起始 = 9
        assertTrue(nextOffset > cursorInNumerator, "Next slot should be after current position")
    }

    @Test
    fun should_navigate_from_fraction_denominator_to_after() {
        val text = "\\frac{a}{b}"
        val doc = parser.parse(text)

        // 光标在分母 'b' 的位置
        val cursorInDenominator = 10
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInDenominator, doc)
        assertNotNull(nextOffset, "Should find next slot for fraction denominator")

        // 最后一个 slot → 跳到分数之后
        assertEquals(text.length, nextOffset, "Should jump to after the fraction")
    }

    @Test
    fun should_navigate_empty_fraction_slots() {
        // \frac{}{}
        // 01234567
        val text = "\\frac{}{}"
        val doc = parser.parse(text)

        // 光标在空分子 {} 内部 = offset 6
        val cursorInEmptyNumerator = 6
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInEmptyNumerator, doc)
        assertNotNull(nextOffset, "Should navigate from empty numerator")

        // 应该跳到空分母 {} 内部
        assertTrue(nextOffset > cursorInEmptyNumerator, "Should move forward")
    }

    @Test
    fun should_navigate_full_fraction_cycle() {
        // 模拟完整的 Enter 键循环：分子 → 分母 → 分数后
        val text = "\\frac{abc}{xyz}"
        val doc = parser.parse(text)

        // 第一次 Enter：从分子内跳到分母
        val step1 = SlotNavigator.nextSlotOffset(7, doc)  // 在 'a' 之后
        assertNotNull(step1)
        assertTrue(step1 > 7)

        // 第二次 Enter：从分母跳到分数后
        val step2 = SlotNavigator.nextSlotOffset(step1, doc)
        assertNotNull(step2)
        assertEquals(text.length, step2)
    }

    // ========== Superscript ==========

    @Test
    fun should_navigate_from_superscript_to_after() {
        // x^{2}
        val text = "x^{2}"
        val doc = parser.parse(text)

        // 光标在上标 '2' 内
        val cursorInExponent = 4
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInExponent, doc)
        assertNotNull(nextOffset, "Should find next slot for superscript")
        assertEquals(text.length, nextOffset, "Single slot → jump to after superscript")
    }

    // ========== Subscript ==========

    @Test
    fun should_navigate_from_subscript_to_after() {
        // x_{i}
        val text = "x_{i}"
        val doc = parser.parse(text)

        val cursorInIndex = 4
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInIndex, doc)
        assertNotNull(nextOffset, "Should find next slot for subscript")
        assertEquals(text.length, nextOffset, "Single slot → jump to after subscript")
    }

    // ========== Non-compound ==========

    @Test
    fun should_return_null_for_plain_text() {
        val text = "abc"
        val doc = parser.parse(text)

        val result = SlotNavigator.nextSlotOffset(1, doc)
        assertNull(result, "Plain text has no compound structure, should return null")
    }

    @Test
    fun should_return_null_for_empty_document() {
        val text = ""
        val doc = parser.parse(text)

        val result = SlotNavigator.nextSlotOffset(0, doc)
        assertNull(result, "Empty document should return null")
    }

    // ========== Binomial ==========

    @Test
    fun should_navigate_binomial_top_to_bottom() {
        // \binom{n}{k}
        val text = "\\binom{n}{k}"
        val doc = parser.parse(text)

        // 光标在 top 'n' 内
        val cursorInTop = 8
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInTop, doc)
        assertNotNull(nextOffset, "Should navigate from binomial top to bottom")
        assertTrue(nextOffset > cursorInTop)
    }

    @Test
    fun should_navigate_binomial_bottom_to_after() {
        val text = "\\binom{n}{k}"
        val doc = parser.parse(text)

        val cursorInBottom = 11
        val nextOffset = SlotNavigator.nextSlotOffset(cursorInBottom, doc)
        assertNotNull(nextOffset)
        assertEquals(text.length, nextOffset)
    }

    // ========== EditorState integration ==========

    @Test
    fun should_editor_state_navigate_to_next_slot() {
        val state = EditorState("\\frac{}{}")
        // 光标在分子内部
        state.moveCursorTo(6)

        val result = state.navigateToNextSlot()
        assertTrue(result, "Should successfully navigate")
        assertTrue(state.cursorOffset > 6, "Cursor should move forward")
    }

    @Test
    fun should_editor_state_return_false_for_plain_text() {
        val state = EditorState("hello")
        state.moveCursorTo(2)

        val result = state.navigateToNextSlot()
        assertFalse(result, "Plain text should not navigate")
        assertEquals(2, state.cursorOffset, "Cursor should not move")
    }

    // ========== orderedSlots coverage ==========

    @Test
    fun should_return_correct_slot_count_for_fraction() {
        val text = "\\frac{a}{b}"
        val doc = parser.parse(text)
        val fraction = doc.children.first()
        val slots = SlotNavigator.orderedSlots(fraction)
        assertNotNull(slots)
        assertEquals(2, slots.size, "Fraction has 2 slots: numerator, denominator")
    }
}
