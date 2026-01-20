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
import kotlin.test.assertTrue

/**
 * 特殊效果测试（boxed, phantom）
 */
class SpecialEffectTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_boxed_with_simple_content() {
        val input = "\\boxed{E = mc^2}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertEquals(1, result.children.size)

        val boxed = result.children[0]
        assertIs<LatexNode.Boxed>(boxed)
        assertTrue(boxed.content.isNotEmpty(), "Boxed content should not be empty")
    }

    @Test
    fun should_parse_boxed_with_complex_formula() {
        val input = "\\boxed{\\frac{a}{b} + \\sqrt{x}}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertEquals(1, result.children.size)

        val boxed = result.children[0]
        assertIs<LatexNode.Boxed>(boxed)
        assertTrue(boxed.content.isNotEmpty())
        
        // 验证内容包含分数和根号
        val hasContent = boxed.content.any { 
            it is LatexNode.Fraction || it is LatexNode.Root 
        }
        assertTrue(hasContent || boxed.content.size > 1, "Boxed should contain complex formula")
    }

    @Test
    fun should_parse_phantom_with_simple_content() {
        val input = "\\phantom{x}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertEquals(1, result.children.size)

        val phantom = result.children[0]
        assertIs<LatexNode.Phantom>(phantom)
        assertTrue(phantom.content.isNotEmpty(), "Phantom content should not be empty")
    }

    @Test
    fun should_parse_phantom_with_formula() {
        val input = "x + \\phantom{+ y} = z"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertTrue(result.children.size >= 3, "Should have x, phantom, and z parts")

        // 查找 phantom 节点
        val hasPhantom = result.children.any { it is LatexNode.Phantom }
        assertTrue(hasPhantom, "Should contain phantom node")
    }

    @Test
    fun should_parse_phantom_for_alignment() {
        // 常见用法：对齐多行公式
        val input = "\\begin{align}x &= 1 \\\\ \\phantom{x} &= 2\\end{align}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        // 验证解析成功（具体结构取决于环境解析实现）
        assertTrue(result.children.isNotEmpty())
    }

    @Test
    fun should_parse_nested_boxed() {
        val input = "\\boxed{\\boxed{x}}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertEquals(1, result.children.size)

        val outer = result.children[0]
        assertIs<LatexNode.Boxed>(outer)
        
        // 内层也应该是 boxed
        val hasInnerBoxed = outer.content.any { it is LatexNode.Boxed }
        assertTrue(hasInnerBoxed, "Should support nested boxed")
    }

    @Test
    fun should_parse_boxed_with_color() {
        val input = "\\boxed{\\color{red}{x + y}}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertEquals(1, result.children.size)

        val boxed = result.children[0]
        assertIs<LatexNode.Boxed>(boxed)
        assertTrue(boxed.content.isNotEmpty())
        
        // 验证内容包含颜色节点
        val hasColor = boxed.content.any { it is LatexNode.Color }
        assertTrue(hasColor, "Boxed should contain colored content")
    }

    @Test
    fun should_parse_combined_boxed_and_phantom() {
        val input = "\\boxed{a} + \\phantom{b} = c"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertTrue(result.children.size >= 3)

        val hasBoxed = result.children.any { it is LatexNode.Boxed }
        val hasPhantom = result.children.any { it is LatexNode.Phantom }
        
        assertTrue(hasBoxed, "Should contain boxed node")
        assertTrue(hasPhantom, "Should contain phantom node")
    }

    @Test
    fun should_parse_boxed_in_equation() {
        val input = "y = \\boxed{mx + b}"
        val result = parser.parse(input)

        assertIs<LatexNode.Document>(result)
        assertTrue(result.children.size >= 2)

        val hasBoxed = result.children.any { it is LatexNode.Boxed }
        assertTrue(hasBoxed, "Should contain boxed node in equation")
    }
}
