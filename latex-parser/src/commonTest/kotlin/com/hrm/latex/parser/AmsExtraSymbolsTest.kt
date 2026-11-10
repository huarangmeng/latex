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

    private fun parseSymbols(vararg cases: Pair<String, String>) {
        for ((command, expectedUnicode) in cases) {
            val children = parser.parse("\\$command").children
            assertEquals(1, children.size, "\\$command should parse to exactly 1 node")
            val node = assertIs<LatexNode.Symbol>(children[0], "\\$command should parse as Symbol node")
            assertEquals(expectedUnicode, node.unicode, "\\$command should have unicode '$expectedUnicode'")
        }
    }

    @Test
    fun parsesMiscellaneousSymbols() = parseSymbols(
        "checkmark" to "✓",
        "complement" to "∁",
        "eth" to "ð",
        "mho" to "℧"
    )

    @Test
    fun parsesDoubleHeadedArrows() = parseSymbols(
        "twoheadrightarrow" to "↠",
        "twoheadleftarrow" to "↞"
    )

    @Test
    fun parsesPairedArrows() = parseSymbols(
        "leftleftarrows" to "⇇",
        "rightrightarrows" to "⇉",
        "leftrightarrows" to "⇆",
        "rightleftarrows" to "⇄"
    )

    @Test
    fun parsesCurvedArrows() = parseSymbols(
        "curvearrowright" to "↷",
        "curvearrowleft" to "↶",
        "circlearrowright" to "↻",
        "circlearrowleft" to "↺"
    )

    @Test
    fun parsesSpecialRelations() = parseSymbols(
        "lessdot" to "⋖",
        "gtrdot" to "⋗",
        "lll" to "⋘",
        "ggg" to "⋙"
    )

    @Test
    fun parsesGeometricSymbols() = parseSymbols(
        "blacksquare" to "■",
        "square" to "□",
        "lozenge" to "◊",
        "blacktriangle" to "▲",
        "blacktriangledown" to "▼"
    )

    @Test
    fun parsesHebrewLetters() = parseSymbols(
        "beth" to "ℶ",
        "gimel" to "ℷ",
        "daleth" to "ℸ"
    )

    @Test
    fun parsesAngleSymbols() = parseSymbols(
        "measuredangle" to "∡",
        "sphericalangle" to "∢"
    )

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

    @Test
    fun parsesGreekLetterVariants() = parseSymbols(
        "digamma" to "ϝ",
        "varkappa" to "ϰ"
    )

    @Test
    fun parsesBinaryOperators() = parseSymbols(
        "dotplus" to "∔",
        "smallsetminus" to "∖",
        "barwedge" to "⌅",
        "veebar" to "⌆",
        "doublebarwedge" to "⩞",
        "boxminus" to "⊟",
        "boxplus" to "⊞",
        "boxtimes" to "⊠",
        "boxdot" to "⊡",
        "leftthreetimes" to "⋋",
        "rightthreetimes" to "⋌",
        "curlywedge" to "⋏",
        "curlyvee" to "⋎",
        "circleddash" to "⊝",
        "circledast" to "⊛",
        "circledcirc" to "⊚",
        "centerdot" to "·",
        "intercal" to "⊺",
        "divideontimes" to "⋇",
        "rtimes" to "⋊",
        "ltimes" to "⋉"
    )

    @Test
    fun parsesAdditionalRelations() = parseSymbols(
        "eqslantless" to "⪕",
        "eqslantgtr" to "⪖",
        "lessapprox" to "⪅",
        "gtrapprox" to "⪆",
        "precsim" to "≾",
        "succsim" to "≿",
        "precapprox" to "⪷",
        "succapprox" to "⪸"
    )

    @Test
    fun parsesNegatedSetRelations() = parseSymbols(
        "varsubsetneq" to "⊊",
        "varsupsetneq" to "⊋",
        "subsetneqq" to "⫋",
        "supsetneqq" to "⫌",
        "nsubset" to "⊄",
        "nsupset" to "⊅",
        "nsubseteqq" to "⊈",
        "nsupseteqq" to "⊉"
    )

    @Test
    fun parsesNegatedArrows() = parseSymbols(
        "nleftarrow" to "↚",
        "nrightarrow" to "↛",
        "nLeftarrow" to "⇍",
        "nRightarrow" to "⇏",
        "nLeftrightarrow" to "⇎",
        "nleftrightarrow" to "↮"
    )

    @Test
    fun parsesAdditionalArrows() = parseSymbols(
        "Rrightarrow" to "⇛",
        "Lleftarrow" to "⇚",
        "twoheadrightarrowtail" to "⤳",
        "leftrightharpoons" to "⇋"
    )

    @Test
    fun parsesAdditionalLargeOperators() = parseSymbols(
        "bigtriangleup" to "△",
        "bigtriangledown" to "▽",
        "iiiint" to "⨌",
        "oiint" to "∬",
        "oiiint" to "∭"
    )

    @Test
    fun parsesAdditionalDelimiters() = parseSymbols(
        "lgroup" to "⟮",
        "rgroup" to "⟯",
        "lmoustache" to "⎰",
        "rmoustache" to "⎱"
    )

    @Test
    fun parsesAdditionalMiscellaneousSymbols() = parseSymbols(
        "blacklozenge" to "⧫",
        "Bbbk" to "𝕜"
    )
}
