package com.hrm.latex.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
        assertEquals("H", (h2.base as LatexNode.Text).content)
        assertEquals("2", (h2.index as LatexNode.Text).content)
        
        assertTrue(group.children[1] is LatexNode.Text) // O
        assertEquals("O", (group.children[1] as LatexNode.Text).content)
    }

    @Test
    fun should_parse_charge() {
        // \ce{Fe3+} -> Fe, Super(Fe, 3+)
        // 但由于 Fe 是双字母元素，先添加 Text("Fe")
        // 然后 3+ 会附着到 Fe 上，形成 Super(Text("Fe"), Text("3+"))
        val input = "\\ce{Fe3+}"
        val parser = LatexParser()
        val result = parser.parse(input)
        
        val group = result.children.first() as LatexNode.Group
        assertEquals(1, group.children.size)
        
        val superNode = group.children[0] as LatexNode.Superscript
        assertEquals("Fe", (superNode.base as LatexNode.Text).content)
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
        assertEquals("S", (group.children[0] as LatexNode.Text).content)
        
        val complexNode = group.children[1] // Super(Sub(O, 4), 2-)
        assertTrue(complexNode is LatexNode.Superscript)
        assertEquals("2-", (complexNode.exponent as LatexNode.Text).content)
        
        val base = complexNode.base as LatexNode.Subscript
        assertEquals("4", (base.index as LatexNode.Text).content)
        assertEquals("O", (base.base as LatexNode.Text).content)
    }
}
