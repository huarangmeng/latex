package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 数学模式切换测试 ($...$ 和 $$...$$)
 */
class MathModeTest {

    private val parser = LatexParser()

    // ===== 行内数学模式 $...$ =====

    @Test
    fun should_parse_inline_math_simple() {
        val result = parser.parse("\$x^2\$")
        assertEquals(1, result.children.size)
        val inline = assertIs<LatexNode.InlineMath>(result.children[0])
        assertTrue(inline.children.isNotEmpty())
    }

    @Test
    fun should_parse_inline_math_with_fraction() {
        val result = parser.parse("\$\\frac{a}{b}\$")
        assertEquals(1, result.children.size)
        val inline = assertIs<LatexNode.InlineMath>(result.children[0])
        assertTrue(inline.children.isNotEmpty())
        val frac = inline.children.first()
        assertIs<LatexNode.Fraction>(frac)
    }

    @Test
    fun should_parse_inline_math_with_surrounding_text() {
        val result = parser.parse("The formula \$E=mc^2\$ is famous")
        // 应该有: Text("The"), Space, Text("formula"), Space, InlineMath, Space, Text("is"), Space, Text("famous")
        val inlineMathNodes = result.children.filterIsInstance<LatexNode.InlineMath>()
        assertEquals(1, inlineMathNodes.size)
        // InlineMath 之前应该有文本节点
        val textNodes = result.children.filterIsInstance<LatexNode.Text>()
        assertTrue(textNodes.isNotEmpty())
    }

    @Test
    fun should_parse_multiple_inline_math() {
        val result = parser.parse("\$a\$ and \$b\$")
        val inlineMathNodes = result.children.filterIsInstance<LatexNode.InlineMath>()
        assertEquals(2, inlineMathNodes.size)
    }

    // ===== 展示数学模式 $$...$$ =====

    @Test
    fun should_parse_display_math_simple() {
        val result = parser.parse("\$\$x^2 + y^2 = z^2\$\$")
        assertEquals(1, result.children.size)
        val display = assertIs<LatexNode.DisplayMath>(result.children[0])
        assertTrue(display.children.isNotEmpty())
    }

    @Test
    fun should_parse_display_math_with_fraction() {
        val result = parser.parse("\$\$\\frac{a}{b}\$\$")
        assertEquals(1, result.children.size)
        val display = assertIs<LatexNode.DisplayMath>(result.children[0])
        val frac = display.children.first()
        assertIs<LatexNode.Fraction>(frac)
    }

    @Test
    fun should_parse_display_math_bracket_delimiters() {
        val result = parser.parse("\\[\\frac{a}{b}\\]")
        assertEquals(1, result.children.size)
        val display = assertIs<LatexNode.DisplayMath>(result.children[0])
        val frac = display.children.first()
        assertIs<LatexNode.Fraction>(frac)
    }

    @Test
    fun should_parse_display_math_with_surrounding_text() {
        val result = parser.parse("Consider \$\$\\sum_{i=1}^{n} i\$\$ as shown")
        val displayNodes = result.children.filterIsInstance<LatexNode.DisplayMath>()
        assertEquals(1, displayNodes.size)
        val textNodes = result.children.filterIsInstance<LatexNode.Text>()
        assertTrue(textNodes.isNotEmpty())
    }

    @Test
    fun should_parse_display_math_brackets_with_surrounding_text() {
        val result = parser.parse("Consider \\[\\sum_{i=1}^{n} i\\] as shown")
        val displayNodes = result.children.filterIsInstance<LatexNode.DisplayMath>()
        assertEquals(1, displayNodes.size)
        val textNodes = result.children.filterIsInstance<LatexNode.Text>()
        assertTrue(textNodes.isNotEmpty())
    }

    // ===== 混合场景 =====

    @Test
    fun should_parse_mixed_inline_and_display() {
        val result = parser.parse("Let \$x\$ be such that \$\$x^2 = 4\$\$")
        val inlineMathNodes = result.children.filterIsInstance<LatexNode.InlineMath>()
        val displayNodes = result.children.filterIsInstance<LatexNode.DisplayMath>()
        assertEquals(1, inlineMathNodes.size)
        assertEquals(1, displayNodes.size)
    }

    @Test
    fun should_parse_text_only_no_math() {
        val result = parser.parse("Hello World")
        val inlineMathNodes = result.children.filterIsInstance<LatexNode.InlineMath>()
        val displayNodes = result.children.filterIsInstance<LatexNode.DisplayMath>()
        assertEquals(0, inlineMathNodes.size)
        assertEquals(0, displayNodes.size)
    }

    @Test
    fun should_parse_escaped_dollar_sign() {
        // \$ should NOT start math mode
        val result = parser.parse("Price is \\\$10")
        val inlineMathNodes = result.children.filterIsInstance<LatexNode.InlineMath>()
        assertEquals(0, inlineMathNodes.size)
    }

    @Test
    fun should_parse_complex_document() {
        val result = parser.parse("Given \$a > 0\$ and \$b > 0\$, we have \$\$a + b \\geq 2\\sqrt{ab}\$\$")
        val inlineMathNodes = result.children.filterIsInstance<LatexNode.InlineMath>()
        val displayNodes = result.children.filterIsInstance<LatexNode.DisplayMath>()
        assertEquals(2, inlineMathNodes.size)
        assertEquals(1, displayNodes.size)
    }

    // ===== 边界情况 =====

    @Test
    fun should_handle_empty_inline_math() {
        val result = parser.parse("\$\$")
        // $$ 被识别为 display math 的开始（MathShift count=2），到 EOF 时自动关闭
        // 这是正确的行为：$$ 是 display math 定界符
        val display = assertIs<LatexNode.DisplayMath>(result.children[0])
        assertTrue(display.children.isEmpty())
    }

    @Test
    fun should_handle_empty_display_math() {
        val result = parser.parse("\$\$\$\$")
        // 空的展示数学
        val display = assertIs<LatexNode.DisplayMath>(result.children[0])
        assertTrue(display.children.isEmpty())
    }

    @Test
    fun should_parse_inline_math_with_subscript_superscript() {
        val result = parser.parse("\$x_1^2 + x_2^2\$")
        val inline = assertIs<LatexNode.InlineMath>(result.children[0])
        assertTrue(inline.children.isNotEmpty())
    }
}
