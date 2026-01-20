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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import com.hrm.latex.parser.model.LatexNode

class ChemicalParserTest {

    @Test
    fun should_parse_simple_formula() {
        // \ce{H2O} -> Subscript(H, 2), O
        val input = "\\ce{H2O}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        // Structure: Document -> Group -> [Sub(H, 2), O]
        val group = result.children.first() as LatexNode.Group
        assertEquals(2, group.children.size)
        
        assertTrue(group.children[0] is LatexNode.Subscript) // H2
        val h2 = group.children[0] as LatexNode.Subscript
        // 化学式字母现在使用 TextMode（正体）
        assertEquals("H", (h2.base as LatexNode.TextMode).text)
        assertEquals("2", (h2.index as LatexNode.Text).content)
        
        assertTrue(group.children[1] is LatexNode.TextMode) // O
        assertEquals("O", (group.children[1] as LatexNode.TextMode).text)
    }

    @Test
    fun should_parse_charge() {
        // \ce{Fe3+} -> Fe, Super(Fe, 3+)
        // 但由于 Fe 是双字母元素，先添加 TextMode("Fe")
        // 然后 3+ 会附着到 Fe 上，形成 Super(TextMode("Fe"), Text("3+"))
        val input = "\\ce{Fe3+}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        assertEquals(1, group.children.size)
        
        val superNode = group.children[0] as LatexNode.Superscript
        // 化学式字母现在使用 TextMode（正体）
        assertEquals("Fe", (superNode.base as LatexNode.TextMode).text)
        assertEquals("3+", (superNode.exponent as LatexNode.Text).content)
    }

    @Test
    fun should_parse_reaction_arrow() {
        // \ce{A -> B}
        val input = "\\ce{A -> B}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        // A, Space, ->, Space, B
        // Text(A), Space, Symbol(rightarrow, →), Space, Text(B)
        
        // 验证 -> 被转换为 Symbol
        val arrow = group.children.find { 
            it is LatexNode.Symbol && (it.symbol == "rightarrow" || it.unicode == "→")
        }
        assertTrue(arrow != null, "Arrow not found")
    }

    @Test
    fun should_parse_reversible_arrow() {
        // \ce{A <-> B} 可逆反应
        val input = "\\ce{A <-> B}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        // Text(A), Space, Symbol(leftrightarrow, ↔), Space, Text(B)
        
        val arrow = group.children.find { 
            it is LatexNode.Symbol && (it.symbol == "leftrightarrow" || it.unicode == "↔")
        }
        assertTrue(arrow != null, "Reversible arrow <-> not found")
    }

    @Test
    fun should_parse_equilibrium_arrow() {
        // \ce{A <=> B} 平衡反应（双线箭头）
        val input = "\\ce{A <=> B}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        // Text(A), Space, Symbol(Leftrightarrow, ⇔), Space, Text(B)
        
        val arrow = group.children.find { 
            it is LatexNode.Symbol && (it.symbol == "Leftrightarrow" || it.unicode == "⇔")
        }
        assertTrue(arrow != null, "Equilibrium arrow <=> not found")
    }

    @Test
    fun should_parse_left_arrow() {
        // \ce{A <- B} 左箭头（不太常见但支持）
        val input = "\\ce{A <- B}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        
        val arrow = group.children.find { 
            it is LatexNode.Symbol && (it.symbol == "leftarrow" || it.unicode == "←")
        }
        assertTrue(arrow != null, "Left arrow <- not found")
    }

    @Test
    fun should_parse_double_right_arrow() {
        // \ce{A => B} 双线右箭头
        val input = "\\ce{A => B}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        
        val arrow = group.children.find { 
            it is LatexNode.Symbol && (it.symbol == "Rightarrow" || it.unicode == "⇒")
        }
        assertTrue(arrow != null, "Double right arrow => not found")
    }

    @Test
    fun should_parse_complex_ion() {
        // \ce{SO4^2-}
        val input = "\\ce{SO4^2-}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        // S, O, Sub(4), Super(2-)
        // O should have Sub(4) attached first? No, list order: S, O, Sub(4), Super(2-)
        // wait, parser logic: attachSubscript removes last.
        // S
        // O
        // 4 -> remove O, add Sub(O, 4)
        // ^2- -> remove Sub(O, 4), add Super(Sub(O, 4), 2-)
        
        assertEquals(2, group.children.size)
        // 化学式字母现在使用 TextMode（正体）
        assertEquals("S", (group.children[0] as LatexNode.TextMode).text)
        
        val complexNode = group.children[1] // Super(Sub(O, 4), 2-)
        assertTrue(complexNode is LatexNode.Superscript)
        assertEquals("2-", (complexNode.exponent as LatexNode.Text).content)
        
        val base = complexNode.base as LatexNode.Subscript
        assertEquals("4", (base.index as LatexNode.Text).content)
        assertEquals("O", (base.base as LatexNode.TextMode).text)
    }

    @Test
    fun should_parse_crystallization_water() {
        // \ce{CuSO4*5H2O}
        val input = "\\ce{CuSO4*5H2O}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        // nodes: Cu, S, O4, cdot, 5, H2, O
        // Wait, current logic: CuSO4, cdot, 5, H2, O
        
        // Find cdot
        val cdot = group.children.find { it is LatexNode.Symbol && it.symbol == "cdot" }
        assertNotNull(cdot, "Should find cdot symbol")
        
        // Check 5 (coefficient after cdot)
        val index = group.children.indexOf(cdot)
        val five = group.children[index + 1] as LatexNode.Text
        assertEquals("5", five.content)
    }

    @Test
    fun should_remove_space_before_gas_symbol() {
        // \ce{CO2 ^}
        val input = "\\ce{CO2 ^}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        // Should be [C, O, Sub(2), uparrow]
        // NO Space node should be present
        val spaceNode = group.children.find { it is LatexNode.Space }
        assertTrue(spaceNode == null, "Space should be removed before gas symbol")
        
        val lastNode = group.children.last()
        assertTrue(lastNode is LatexNode.Symbol && lastNode.symbol == "uparrow")
    }
}
