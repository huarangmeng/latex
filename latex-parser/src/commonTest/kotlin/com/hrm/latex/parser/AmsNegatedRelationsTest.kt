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

    private fun parseSymbol(command: String, expectedUnicode: String) {
        val result = parser.parse("\\$command")
        val children = result.children
        assertEquals(1, children.size, "\\$command should parse to exactly 1 node")
        val node = children[0]
        assertIs<LatexNode.Symbol>(node, "\\$command should parse as Symbol node")
        assertEquals(expectedUnicode, node.unicode, "\\$command should have unicode '$expectedUnicode'")
    }

    // === AMS 否定关系符号 ===

    @Test
    fun should_parse_nless() = parseSymbol("nless", "≮")

    @Test
    fun should_parse_ngtr() = parseSymbol("ngtr", "≯")

    @Test
    fun should_parse_nleq() = parseSymbol("nleq", "≰")

    @Test
    fun should_parse_ngeq() = parseSymbol("ngeq", "≱")

    @Test
    fun should_parse_nleqslant() = parseSymbol("nleqslant", "≰")

    @Test
    fun should_parse_ngeqslant() = parseSymbol("ngeqslant", "≱")

    @Test
    fun should_parse_nsubseteq() = parseSymbol("nsubseteq", "⊈")

    @Test
    fun should_parse_nsupseteq() = parseSymbol("nsupseteq", "⊉")

    @Test
    fun should_parse_nprec() = parseSymbol("nprec", "⊀")

    @Test
    fun should_parse_nsucc() = parseSymbol("nsucc", "⊁")

    @Test
    fun should_parse_ncong() = parseSymbol("ncong", "≇")

    @Test
    fun should_parse_nsim() = parseSymbol("nsim", "≁")

    @Test
    fun should_parse_nmid() = parseSymbol("nmid", "∤")

    @Test
    fun should_parse_nparallel() = parseSymbol("nparallel", "∦")

    @Test
    fun should_parse_nvdash() = parseSymbol("nvdash", "⊬")

    @Test
    fun should_parse_nvDash() = parseSymbol("nvDash", "⊭")

    @Test
    fun should_parse_nVdash() = parseSymbol("nVdash", "⊮")

    @Test
    fun should_parse_nVDash() = parseSymbol("nVDash", "⊯")

    @Test
    fun should_parse_ntriangleleft() = parseSymbol("ntriangleleft", "⋪")

    @Test
    fun should_parse_ntriangleright() = parseSymbol("ntriangleright", "⋫")

    @Test
    fun should_parse_ntrianglelefteq() = parseSymbol("ntrianglelefteq", "⋬")

    @Test
    fun should_parse_ntrianglerighteq() = parseSymbol("ntrianglerighteq", "⋭")

    @Test
    fun should_parse_lneq() = parseSymbol("lneq", "⪇")

    @Test
    fun should_parse_gneq() = parseSymbol("gneq", "⪈")

    @Test
    fun should_parse_subsetneq() = parseSymbol("subsetneq", "⊊")

    @Test
    fun should_parse_supsetneq() = parseSymbol("supsetneq", "⊋")

    // === AMS 额外关系符号 ===

    @Test
    fun should_parse_leqslant() = parseSymbol("leqslant", "⩽")

    @Test
    fun should_parse_geqslant() = parseSymbol("geqslant", "⩾")

    @Test
    fun should_parse_lessgtr() = parseSymbol("lessgtr", "≶")

    @Test
    fun should_parse_gtrless() = parseSymbol("gtrless", "≷")

    @Test
    fun should_parse_lesssim() = parseSymbol("lesssim", "≲")

    @Test
    fun should_parse_gtrsim() = parseSymbol("gtrsim", "≳")

    @Test
    fun should_parse_trianglelefteq() = parseSymbol("trianglelefteq", "⊴")

    @Test
    fun should_parse_trianglerighteq() = parseSymbol("trianglerighteq", "⊵")

    @Test
    fun should_parse_vDash() = parseSymbol("vDash", "⊨")

    @Test
    fun should_parse_Vdash() = parseSymbol("Vdash", "⊩")

    @Test
    fun should_parse_Vvdash() = parseSymbol("Vvdash", "⊪")

    @Test
    fun should_parse_models() = parseSymbol("models", "⊧")

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
