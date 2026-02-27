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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorStateTest {

    // ========== 初始化 ==========

    @Test
    fun should_initialize_with_empty_text() {
        val state = EditorState()
        assertEquals("", state.text)
        assertEquals(0, state.cursorOffset)
        assertNull(state.selection)
        assertTrue(state.parseSuccess)
    }

    @Test
    fun should_initialize_with_given_text() {
        val state = EditorState("x^2")
        assertEquals("x^2", state.text)
        assertEquals(3, state.cursorOffset)
        assertTrue(state.parseSuccess)
    }

    @Test
    fun should_parse_initial_text_successfully() {
        val state = EditorState("\\frac{a}{b}")
        assertTrue(state.parseSuccess)
        assertTrue(state.document.children.isNotEmpty())
    }

    // ========== 文本操作 ==========

    @Test
    fun should_update_text_and_reparse() {
        val state = EditorState()
        state.updateText("x + y")
        assertEquals("x + y", state.text)
        assertEquals(5, state.cursorOffset)
        assertTrue(state.parseSuccess)
    }

    @Test
    fun should_insert_at_cursor_position() {
        val state = EditorState("ab")
        state.moveCursorTo(1)
        state.insert("X")
        assertEquals("aXb", state.text)
        assertEquals(2, state.cursorOffset)
    }

    @Test
    fun should_insert_at_beginning() {
        val state = EditorState("ab")
        state.moveCursorTo(0)
        state.insert("X")
        assertEquals("Xab", state.text)
        assertEquals(1, state.cursorOffset)
    }

    @Test
    fun should_insert_at_end() {
        val state = EditorState("ab")
        state.insert("X")
        assertEquals("abX", state.text)
        assertEquals(3, state.cursorOffset)
    }

    // ========== 删除操作 ==========

    @Test
    fun should_backspace_delete_character_before_cursor() {
        val state = EditorState("abc")
        state.moveCursorTo(2)
        state.backspace()
        assertEquals("ac", state.text)
        assertEquals(1, state.cursorOffset)
    }

    @Test
    fun should_backspace_do_nothing_at_start() {
        val state = EditorState("abc")
        state.moveCursorTo(0)
        state.backspace()
        assertEquals("abc", state.text)
        assertEquals(0, state.cursorOffset)
    }

    @Test
    fun should_delete_character_after_cursor() {
        val state = EditorState("abc")
        state.moveCursorTo(1)
        state.delete()
        assertEquals("ac", state.text)
        assertEquals(1, state.cursorOffset)
    }

    @Test
    fun should_delete_do_nothing_at_end() {
        val state = EditorState("abc")
        state.delete()
        assertEquals("abc", state.text)
        assertEquals(3, state.cursorOffset)
    }

    // ========== 光标移动 ==========

    @Test
    fun should_move_cursor_to_valid_offset() {
        val state = EditorState("hello")
        state.moveCursorTo(3)
        assertEquals(3, state.cursorOffset)
    }

    @Test
    fun should_clamp_cursor_to_text_bounds() {
        val state = EditorState("hi")
        state.moveCursorTo(-5)
        assertEquals(0, state.cursorOffset)
        state.moveCursorTo(100)
        assertEquals(2, state.cursorOffset)
    }

    @Test
    fun should_move_cursor_left() {
        val state = EditorState("ab")
        state.moveCursorTo(2)
        state.moveCursorLeft()
        assertEquals(1, state.cursorOffset)
    }

    @Test
    fun should_not_move_cursor_left_past_start() {
        val state = EditorState("ab")
        state.moveCursorTo(0)
        state.moveCursorLeft()
        assertEquals(0, state.cursorOffset)
    }

    @Test
    fun should_move_cursor_right() {
        val state = EditorState("ab")
        state.moveCursorTo(0)
        state.moveCursorRight()
        assertEquals(1, state.cursorOffset)
    }

    @Test
    fun should_not_move_cursor_right_past_end() {
        val state = EditorState("ab")
        state.moveCursorRight()
        assertEquals(2, state.cursorOffset)
    }

    // ========== 选区 ==========

    @Test
    fun should_set_valid_selection() {
        val state = EditorState("hello")
        state.updateSelection(1..3)
        assertNotNull(state.selection)
        assertEquals(1..3, state.selection)
    }

    @Test
    fun should_clear_selection_on_null() {
        val state = EditorState("hello")
        state.updateSelection(1..3)
        state.updateSelection(null)
        assertNull(state.selection)
    }

    @Test
    fun should_clear_selection_on_cursor_move() {
        val state = EditorState("hello")
        state.updateSelection(1..3)
        state.moveCursorTo(2)
        assertNull(state.selection)
    }

    @Test
    fun should_clamp_selection_to_text_bounds() {
        val state = EditorState("hi")
        state.updateSelection(-1..10)
        assertNotNull(state.selection)
        assertEquals(0..2, state.selection)
    }

    // ========== 模板插入 ==========

    @Test
    fun should_insert_fraction_template() {
        val state = EditorState()
        state.insertTemplate(LatexTemplate.FRACTION)
        assertEquals("\\frac{}{}", state.text)
        // 光标在第一个 {} 内
        assertEquals(6, state.cursorOffset)
    }

    @Test
    fun should_insert_sqrt_template() {
        val state = EditorState()
        state.insertTemplate(LatexTemplate.SQRT)
        assertEquals("\\sqrt{}", state.text)
        assertEquals(6, state.cursorOffset)
    }

    @Test
    fun should_insert_template_at_cursor_position() {
        val state = EditorState("f(x) = ")
        state.insertTemplate(LatexTemplate.FRACTION)
        assertEquals("f(x) = \\frac{}{}", state.text)
        assertEquals(13, state.cursorOffset) // 7 + 6
    }

    // ========== 解析 ==========

    @Test
    fun should_parse_valid_latex() {
        val state = EditorState("x^2")
        assertTrue(state.parseSuccess)
        assertTrue(state.document.children.isNotEmpty())
    }

    @Test
    fun should_handle_empty_text() {
        val state = EditorState("")
        assertTrue(state.parseSuccess)
        assertTrue(state.document.children.isEmpty())
    }
}
