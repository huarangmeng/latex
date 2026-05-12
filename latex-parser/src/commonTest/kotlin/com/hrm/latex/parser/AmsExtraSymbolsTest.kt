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

    // === AMS 希腊字母变体 ===

    @Test
    fun should_parse_digamma() = parseSymbol("digamma", "ϝ")

    @Test
    fun should_parse_varkappa() = parseSymbol("varkappa", "ϰ")

    // === AMS 二元运算符 ===

    @Test
    fun should_parse_dotplus() = parseSymbol("dotplus", "∔")

    @Test
    fun should_parse_smallsetminus() = parseSymbol("smallsetminus", "∖")

    @Test
    fun should_parse_barwedge() = parseSymbol("barwedge", "⌅")

    @Test
    fun should_parse_veebar() = parseSymbol("veebar", "⌆")

    @Test
    fun should_parse_doublebarwedge() = parseSymbol("doublebarwedge", "⩞")

    @Test
    fun should_parse_boxminus() = parseSymbol("boxminus", "⊟")

    @Test
    fun should_parse_boxplus() = parseSymbol("boxplus", "⊞")

    @Test
    fun should_parse_boxtimes() = parseSymbol("boxtimes", "⊠")

    @Test
    fun should_parse_boxdot() = parseSymbol("boxdot", "⊡")

    @Test
    fun should_parse_leftthreetimes() = parseSymbol("leftthreetimes", "⋋")

    @Test
    fun should_parse_rightthreetimes() = parseSymbol("rightthreetimes", "⋌")

    @Test
    fun should_parse_curlywedge() = parseSymbol("curlywedge", "⋏")

    @Test
    fun should_parse_curlyvee() = parseSymbol("curlyvee", "⋎")

    @Test
    fun should_parse_circleddash() = parseSymbol("circleddash", "⊝")

    @Test
    fun should_parse_circledast() = parseSymbol("circledast", "⊛")

    @Test
    fun should_parse_circledcirc() = parseSymbol("circledcirc", "⊚")

    @Test
    fun should_parse_centerdot() = parseSymbol("centerdot", "·")

    @Test
    fun should_parse_intercal() = parseSymbol("intercal", "⊺")

    @Test
    fun should_parse_divideontimes() = parseSymbol("divideontimes", "⋇")

    @Test
    fun should_parse_rtimes() = parseSymbol("rtimes", "⋊")

    @Test
    fun should_parse_ltimes() = parseSymbol("ltimes", "⋉")

    // === AMS 额外关系符号 ===

    @Test
    fun should_parse_eqslantless() = parseSymbol("eqslantless", "⪕")

    @Test
    fun should_parse_eqslantgtr() = parseSymbol("eqslantgtr", "⪖")

    @Test
    fun should_parse_lessapprox() = parseSymbol("lessapprox", "⪅")

    @Test
    fun should_parse_gtrapprox() = parseSymbol("gtrapprox", "⪆")

    @Test
    fun should_parse_precsim() = parseSymbol("precsim", "≾")

    @Test
    fun should_parse_succsim() = parseSymbol("succsim", "≿")

    @Test
    fun should_parse_precapprox() = parseSymbol("precapprox", "⪷")

    @Test
    fun should_parse_succapprox() = parseSymbol("succapprox", "⪸")

    // === AMS 否定集合关系 ===

    @Test
    fun should_parse_varsubsetneq() = parseSymbol("varsubsetneq", "⊊")

    @Test
    fun should_parse_varsupsetneq() = parseSymbol("varsupsetneq", "⊋")

    @Test
    fun should_parse_subsetneqq() = parseSymbol("subsetneqq", "⫋")

    @Test
    fun should_parse_supsetneqq() = parseSymbol("supsetneqq", "⫌")

    @Test
    fun should_parse_nsubset() = parseSymbol("nsubset", "⊄")

    @Test
    fun should_parse_nsupset() = parseSymbol("nsupset", "⊅")

    @Test
    fun should_parse_nsubseteqq() = parseSymbol("nsubseteqq", "⊈")

    @Test
    fun should_parse_nsupseteqq() = parseSymbol("nsupseteqq", "⊉")

    // === AMS 否定箭头 ===

    @Test
    fun should_parse_nleftarrow() = parseSymbol("nleftarrow", "↚")

    @Test
    fun should_parse_nrightarrow() = parseSymbol("nrightarrow", "↛")

    @Test
    fun should_parse_nLeftarrow() = parseSymbol("nLeftarrow", "⇍")

    @Test
    fun should_parse_nRightarrow() = parseSymbol("nRightarrow", "⇏")

    @Test
    fun should_parse_nLeftrightarrow() = parseSymbol("nLeftrightarrow", "⇎")

    @Test
    fun should_parse_nleftrightarrow() = parseSymbol("nleftrightarrow", "↮")

    // === AMS 额外箭头 ===

    @Test
    fun should_parse_Rrightarrow() = parseSymbol("Rrightarrow", "⇛")

    @Test
    fun should_parse_Lleftarrow() = parseSymbol("Lleftarrow", "⇚")

    @Test
    fun should_parse_twoheadrightarrowtail() = parseSymbol("twoheadrightarrowtail", "⤳")

    @Test
    fun should_parse_leftrightharpoons() = parseSymbol("leftrightharpoons", "⇋")

    // === 大型运算符扩展 ===

    @Test
    fun should_parse_bigtriangleup() = parseSymbol("bigtriangleup", "△")

    @Test
    fun should_parse_bigtriangledown() = parseSymbol("bigtriangledown", "▽")

    @Test
    fun should_parse_iiiint() = parseSymbol("iiiint", "⨌")

    @Test
    fun should_parse_oiint() = parseSymbol("oiint", "∬")

    @Test
    fun should_parse_oiiint() = parseSymbol("oiiint", "∭")

    // === 额外定界符 ===

    @Test
    fun should_parse_lgroup() = parseSymbol("lgroup", "⟮")

    @Test
    fun should_parse_rgroup() = parseSymbol("rgroup", "⟯")

    @Test
    fun should_parse_lmoustache() = parseSymbol("lmoustache", "⎰")

    @Test
    fun should_parse_rmoustache() = parseSymbol("rmoustache", "⎱")

    // === AMS 杂项扩展 ===

    @Test
    fun should_parse_blacklozenge() = parseSymbol("blacklozenge", "⧫")

    @Test
    fun should_parse_Bbbk() = parseSymbol("Bbbk", "𝕜")
}
