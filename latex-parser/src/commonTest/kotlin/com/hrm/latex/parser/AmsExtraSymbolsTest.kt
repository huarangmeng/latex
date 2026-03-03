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

class AmsExtraSymbolsTest {

    private val parser = LatexParser()

    private fun parseSymbol(command: String, expectedUnicode: String) {
        val result = parser.parse("\\$command")
        val children = result.children
        assertEquals(1, children.size, "\\$command should parse to exactly 1 node")
        val node = children[0]
        assertIs<LatexNode.Symbol>(node, "\\$command should parse as Symbol node")
        assertEquals(expectedUnicode, node.unicode, "\\$command should have unicode '$expectedUnicode'")
    }

    // === 杂项符号 ===

    @Test
    fun should_parse_checkmark() = parseSymbol("checkmark", "✓")

    @Test
    fun should_parse_complement() = parseSymbol("complement", "∁")

    @Test
    fun should_parse_eth() = parseSymbol("eth", "ð")

    @Test
    fun should_parse_mho() = parseSymbol("mho", "℧")

    // === 双头箭头 ===

    @Test
    fun should_parse_twoheadrightarrow() = parseSymbol("twoheadrightarrow", "↠")

    @Test
    fun should_parse_twoheadleftarrow() = parseSymbol("twoheadleftarrow", "↞")

    // === 双线箭头 ===

    @Test
    fun should_parse_leftleftarrows() = parseSymbol("leftleftarrows", "⇇")

    @Test
    fun should_parse_rightrightarrows() = parseSymbol("rightrightarrows", "⇉")

    @Test
    fun should_parse_leftrightarrows() = parseSymbol("leftrightarrows", "⇆")

    @Test
    fun should_parse_rightleftarrows() = parseSymbol("rightleftarrows", "⇄")

    // === 弯曲箭头 ===

    @Test
    fun should_parse_curvearrowright() = parseSymbol("curvearrowright", "↷")

    @Test
    fun should_parse_curvearrowleft() = parseSymbol("curvearrowleft", "↶")

    @Test
    fun should_parse_circlearrowright() = parseSymbol("circlearrowright", "↻")

    @Test
    fun should_parse_circlearrowleft() = parseSymbol("circlearrowleft", "↺")

    // === 特殊关系符 ===

    @Test
    fun should_parse_lessdot() = parseSymbol("lessdot", "⋖")

    @Test
    fun should_parse_gtrdot() = parseSymbol("gtrdot", "⋗")

    @Test
    fun should_parse_lll() = parseSymbol("lll", "⋘")

    @Test
    fun should_parse_ggg() = parseSymbol("ggg", "⋙")

    // === 几何符号 ===

    @Test
    fun should_parse_blacksquare() = parseSymbol("blacksquare", "■")

    @Test
    fun should_parse_square() = parseSymbol("square", "□")

    @Test
    fun should_parse_lozenge() = parseSymbol("lozenge", "◊")

    @Test
    fun should_parse_blacktriangle() = parseSymbol("blacktriangle", "▲")

    @Test
    fun should_parse_blacktriangledown() = parseSymbol("blacktriangledown", "▼")

    // === 希伯来字母 ===

    @Test
    fun should_parse_beth() = parseSymbol("beth", "ℶ")

    @Test
    fun should_parse_gimel() = parseSymbol("gimel", "ℷ")

    @Test
    fun should_parse_daleth() = parseSymbol("daleth", "ℸ")

    // === 角度 ===

    @Test
    fun should_parse_measuredangle() = parseSymbol("measuredangle", "∡")

    @Test
    fun should_parse_sphericalangle() = parseSymbol("sphericalangle", "∢")

    // === 可访问名称 ===

    @Test
    fun should_have_accessible_names_for_extra_symbols() {
        val symbolsToCheck = listOf(
            "checkmark", "complement", "eth", "mho",
            "twoheadrightarrow", "blacksquare", "square",
            "beth", "gimel", "daleth", "measuredangle"
        )
        for (name in symbolsToCheck) {
            val accessible = SymbolMap.getAccessibleName(name)
            assertNotNull(accessible, "\\$name should have an accessible name")
        }
    }

    // === 组合使用 ===

    @Test
    fun should_parse_symbols_in_expression() {
        val result = parser.parse("\\checkmark \\complement A")
        val symbols = result.children.filterIsInstance<LatexNode.Symbol>()
        assertEquals(2, symbols.size, "Should have 2 symbol nodes")
        assertEquals("✓", symbols[0].unicode)
        assertEquals("∁", symbols[1].unicode)
    }
}
