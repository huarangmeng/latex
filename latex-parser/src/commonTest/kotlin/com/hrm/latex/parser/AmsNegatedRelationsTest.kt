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

class AmsNegatedRelationsTest {

    private val parser = LatexParser()

    private fun parseSymbols(vararg cases: Pair<String, String>) {
        for ((command, expectedUnicode) in cases) {
            val children = parser.parse("\\$command").children
            assertEquals(1, children.size, "\\$command should parse to exactly 1 node")
            val node = assertIs<LatexNode.Symbol>(children[0], "\\$command should parse as Symbol node")
            assertEquals(expectedUnicode, node.unicode, "\\$command should have unicode '$expectedUnicode'")
        }
    }

    @Test
    fun parsesNegatedRelations() = parseSymbols(
        "nless" to "≮",
        "ngtr" to "≯",
        "nleq" to "≰",
        "ngeq" to "≱",
        "nleqslant" to "≰",
        "ngeqslant" to "≱",
        "nsubseteq" to "⊈",
        "nsupseteq" to "⊉",
        "nprec" to "⊀",
        "nsucc" to "⊁",
        "ncong" to "≇",
        "nsim" to "≁",
        "nmid" to "∤",
        "nparallel" to "∦",
        "nvdash" to "⊬",
        "nvDash" to "⊭",
        "nVdash" to "⊮",
        "nVDash" to "⊯",
        "ntriangleleft" to "⋪",
        "ntriangleright" to "⋫",
        "ntrianglelefteq" to "⋬",
        "ntrianglerighteq" to "⋭",
        "lneq" to "⪇",
        "gneq" to "⪈",
        "subsetneq" to "⊊",
        "supsetneq" to "⊋"
    )

    @Test
    fun parsesAdditionalRelations() = parseSymbols(
        "leqslant" to "⩽",
        "geqslant" to "⩾",
        "lessgtr" to "≶",
        "gtrless" to "≷",
        "lesssim" to "≲",
        "gtrsim" to "≳",
        "trianglelefteq" to "⊴",
        "trianglerighteq" to "⊵",
        "vDash" to "⊨",
        "Vdash" to "⊩",
        "Vvdash" to "⊪",
        "models" to "⊧"
    )

    // === 可访问名称测试 ===

    @Test
    fun should_have_accessible_names_for_negated_relations() {
        val symbolsToCheck = listOf(
            "nleq", "ngeq", "nsubseteq", "nprec", "ncong", "nvdash",
            "leqslant", "geqslant", "vDash", "models"
        )
        for (name in symbolsToCheck) {
            val accessible = SymbolMap.getAccessibleName(name)
            assertNotNull(accessible, "\\$name should have an accessible name")
        }
    }

    // === 组合使用测试 ===

    @Test
    fun should_parse_negated_relation_in_expression() {
        val result = parser.parse("a \\nleq b")
        val children = result.children
        // Should have: text "a", space, symbol "≰", space, text "b" (or similar)
        val symbols = children.filterIsInstance<LatexNode.Symbol>()
        assertEquals(1, symbols.size, "Should have exactly 1 symbol node")
        assertEquals("≰", symbols[0].unicode)
    }

    @Test
    fun should_parse_multiple_negated_relations() {
        val result = parser.parse("a \\nleq b \\ngeq c")
        val symbols = result.children.filterIsInstance<LatexNode.Symbol>()
        assertEquals(2, symbols.size, "Should have 2 symbol nodes")
        assertEquals("≰", symbols[0].unicode)
        assertEquals("≱", symbols[1].unicode)
    }
}
