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
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 扩展重音命令测试：
 * \grave, \acute, \check, \breve, \ring, \mathring, \dddot
 */
class AccentExtensionTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_grave_accent() {
        val result = parser.parse("\\grave{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.GRAVE, accent.accentType)
    }

    @Test
    fun should_parse_acute_accent() {
        val result = parser.parse("\\acute{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.ACUTE, accent.accentType)
    }

    @Test
    fun should_parse_check_accent() {
        val result = parser.parse("\\check{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.CHECK, accent.accentType)
    }

    @Test
    fun should_parse_breve_accent() {
        val result = parser.parse("\\breve{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.BREVE, accent.accentType)
    }

    @Test
    fun should_parse_ring_accent() {
        val result = parser.parse("\\ring{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.RING, accent.accentType)
    }

    @Test
    fun should_parse_mathring_accent() {
        val result = parser.parse("\\mathring{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.RING, accent.accentType)
    }

    @Test
    fun should_parse_dddot_accent() {
        val result = parser.parse("\\dddot{a}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.DDDOT, accent.accentType)
    }

    @Test
    fun should_produce_accessibility_for_new_accents() {
        val tests = mapOf(
            "\\grave{a}" to "grave",
            "\\acute{a}" to "acute",
            "\\check{a}" to "check",
            "\\breve{a}" to "breve",
            "\\ring{a}" to "ring",
            "\\dddot{a}" to "triple dot"
        )
        for ((input, expected) in tests) {
            val result = parser.parse(input)
            val desc = AccessibilityVisitor.describe(result)
            assertTrue(desc.contains(expected), "Accessibility for $input should contain '$expected', got: $desc")
        }
    }

    @Test
    fun should_parse_hat_without_braces() {
        // \hat f (with space, no braces) should parse same as \hat{f}
        val result = parser.parse("\\hat f")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.HAT, accent.accentType)
        // content should be Text("f"), not a Space
        val content = accent.content
        assertIs<LatexNode.Text>(content)
        assertEquals("f", content.content)
    }

    @Test
    fun should_parse_vec_without_braces() {
        // \vec v (with space, no braces) should parse same as \vec{v}
        val result = parser.parse("\\vec v")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.VEC, accent.accentType)
        val content = accent.content
        assertIs<LatexNode.Text>(content)
        assertEquals("v", content.content)
    }

    @Test
    fun should_parse_hat_f_in_context() {
        // \hat f(\xi) — hat should only capture f, not f(\xi)
        val result = parser.parse("\\hat f(x)")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.HAT, accent.accentType)
        val content = accent.content
        assertIs<LatexNode.Text>(content)
        assertEquals("f", content.content)
        // The rest "(x)" should be separate nodes
        assertTrue(result.children.size > 1, "Remaining tokens should be separate nodes")
    }

    // ============ \overbracket / \underbracket 方括号标注 ============

    @Test
    fun should_parse_overbracket() {
        val result = parser.parse("\\overbracket{x+y}")
        val accent = result.children.first()
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.OVERBRACKET, accent.accentType)
    }

    @Test
    fun should_parse_underbracket() {
        val result = parser.parse("\\underbracket{a+b}")
        val accent = result.children.first()
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.UNDERBRACKET, accent.accentType)
    }

    @Test
    fun should_parse_overbracket_with_nested_content() {
        val result = parser.parse("\\overbracket{\\frac{a}{b}}")
        val accent = result.children.first()
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.OVERBRACKET, accent.accentType)
    }

    // ============ 可扩展箭头家族：\xRightarrow, \xLeftarrow, \xLeftrightarrow, \xmapsto ============

    @Test
    fun should_parse_xRightarrow() {
        val doc = parser.parse("\\xRightarrow{f}")
        val arrow = doc.children.first()
        assertIs<LatexNode.ExtensibleArrow>(arrow)
        assertEquals(LatexNode.ExtensibleArrow.Direction.RIGHT_DOUBLE, arrow.direction)
    }

    @Test
    fun should_parse_xLeftarrow() {
        val doc = parser.parse("\\xLeftarrow{g}")
        val arrow = doc.children.first()
        assertIs<LatexNode.ExtensibleArrow>(arrow)
        assertEquals(LatexNode.ExtensibleArrow.Direction.LEFT_DOUBLE, arrow.direction)
    }

    @Test
    fun should_parse_xLeftrightarrow() {
        val doc = parser.parse("\\xLeftrightarrow{}")
        val arrow = doc.children.first()
        assertIs<LatexNode.ExtensibleArrow>(arrow)
        assertEquals(LatexNode.ExtensibleArrow.Direction.BOTH_DOUBLE, arrow.direction)
    }

    @Test
    fun should_parse_xmapsto() {
        val doc = parser.parse("\\xmapsto{f}")
        val arrow = doc.children.first()
        assertIs<LatexNode.ExtensibleArrow>(arrow)
        assertEquals(LatexNode.ExtensibleArrow.Direction.MAPSTO, arrow.direction)
    }

    @Test
    fun should_parse_xRightarrow_with_below() {
        val doc = parser.parse("\\xRightarrow[below]{above}")
        val arrow = doc.children.first()
        assertIs<LatexNode.ExtensibleArrow>(arrow)
        assertEquals(LatexNode.ExtensibleArrow.Direction.RIGHT_DOUBLE, arrow.direction)
        assertTrue(arrow.below != null, "应有下方文字")
    }

    @Test
    fun should_parse_xmapsto_with_below() {
        val doc = parser.parse("\\xmapsto[y]{x}")
        val arrow = doc.children.first()
        assertIs<LatexNode.ExtensibleArrow>(arrow)
        assertEquals(LatexNode.ExtensibleArrow.Direction.MAPSTO, arrow.direction)
        assertTrue(arrow.below != null)
    }
}
