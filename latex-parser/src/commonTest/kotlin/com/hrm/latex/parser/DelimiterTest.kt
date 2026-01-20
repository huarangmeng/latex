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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 分隔符测试
 * 
 * 测试覆盖：
 * 1. 自动伸缩括号 (\left ... \right)
 * 2. 不对称分隔符 (\left. ... \right|)
 * 3. 手动大小控制 (\big, \Big, \bigg, \Bigg)
 */
class DelimiterTest {
    
    private val parser = LatexParser()
    
    // ========== 自动伸缩括号测试 ==========
    
    @Test
    fun testBasicDelimiters() {
        val doc = parser.parse("\\left( x + y \\right)")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("(", delim.left)
        assertEquals(")", delim.right)
        assertTrue(delim.scalable)
        assertEquals(null, delim.manualSize)
    }
    
    @Test
    fun testSquareBrackets() {
        val doc = parser.parse("\\left[ x \\right]")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("[", delim.left)
        assertEquals("]", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testCurlyBraces() {
        val doc = parser.parse("\\left\\{ x \\right\\}")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("{", delim.left)
        assertEquals("}", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testVerticalBars() {
        val doc = parser.parse("\\left| x \\right|")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("|", delim.left)
        assertEquals("|", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testAngleBrackets() {
        val doc = parser.parse("\\left\\langle x \\right\\rangle")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("⟨", delim.left)
        assertEquals("⟩", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testFloorBrackets() {
        val doc = parser.parse("\\left\\lfloor x \\right\\rfloor")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("⌊", delim.left)
        assertEquals("⌋", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testCeilBrackets() {
        val doc = parser.parse("\\left\\lceil x \\right\\rceil")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("⌈", delim.left)
        assertEquals("⌉", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testDelimitersWithFraction() {
        val doc = parser.parse("\\left( \\frac{a}{b} \\right)")
        val delim = doc.children[0] as LatexNode.Delimited
        assertTrue(delim.content.isNotEmpty())
        assertTrue(delim.content.any { it is LatexNode.Fraction })
    }
    
    @Test
    fun testNestedDelimiters() {
        val doc = parser.parse("\\left( \\left[ x \\right] \\right)")
        val outerDelim = doc.children[0] as LatexNode.Delimited
        assertEquals("(", outerDelim.left)
        assertEquals(")", outerDelim.right)
        
        val innerDelim = outerDelim.content.filterIsInstance<LatexNode.Delimited>().first()
        assertEquals("[", innerDelim.left)
        assertEquals("]", innerDelim.right)
    }
    
    // ========== 不对称分隔符测试（新增功能 ✨）==========
    
    @Test
    fun testAsymmetricDelimiterLeftDot() {
        // \left. \frac{df}{dx} \right|_{x=0}
        val doc = parser.parse("\\left. \\frac{df}{dx} \\right|")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("", delim.left, "左侧应该是空字符串（不显示）")
        assertEquals("|", delim.right)
        assertTrue(delim.scalable)
        assertTrue(delim.content.any { it is LatexNode.Fraction })
    }
    
    @Test
    fun testAsymmetricDelimiterRightDot() {
        // \left\{ x > 0 \right.
        val doc = parser.parse("\\left\\{ x > 0 \\right.")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("{", delim.left)
        assertEquals("", delim.right, "右侧应该是空字符串（不显示）")
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testAsymmetricDelimiterBothDots() {
        // 虽然不常见，但测试 \left. ... \right.
        val doc = parser.parse("\\left. x \\right.")
        val delim = doc.children[0] as LatexNode.Delimited
        assertEquals("", delim.left)
        assertEquals("", delim.right)
        assertTrue(delim.scalable)
    }
    
    @Test
    fun testEvaluationNotation() {
        // 求值符号：\left. \frac{df}{dx} \right|_{x=0}
        val doc = parser.parse("\\left. \\frac{d}{dx} x^2 \\right|_{x=0}")
        
        // 找到 Delimited 节点（可能在下标内部）
        val delim = doc.children.filterIsInstance<LatexNode.Subscript>().firstOrNull()?.base as? LatexNode.Delimited
            ?: doc.children.filterIsInstance<LatexNode.Delimited>().firstOrNull()
        
        assertNotNull(delim, "应该找到 Delimited 节点")
        assertEquals("", delim.left)
        assertEquals("|", delim.right)
        assertTrue(delim.content.isNotEmpty())
    }
    
    @Test
    fun testPiecewiseFunction() {
        // 分段函数：f(x) = \left\{ ... \right.
        val doc = parser.parse("f(x) = \\left\\{ x + 1 \\right.")
        assertTrue(doc.children.size >= 2) // f(x) = 和 分隔符
        val delim = doc.children.filterIsInstance<LatexNode.Delimited>().first()
        assertEquals("{", delim.left)
        assertEquals("", delim.right)
    }
    
    // ========== 手动大小控制测试（新增功能 ✨）==========
    
    @Test
    fun testBigParenthesis() {
        val doc = parser.parse("\\big( x \\big)")
        
        // 应该生成两个 ManualSizedDelimiter 节点（左括号和右括号）
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.size >= 1, "应该至少有一个手动大小分隔符")
        
        val leftDelim = delimiters[0]
        assertEquals("(", leftDelim.delimiter)
        assertEquals(1.2f, leftDelim.size, "\\big 应该是 1.2x")
    }
    
    @Test
    fun testBigSquareBracket() {
        val doc = parser.parse("\\Big[ x \\Big]")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.isNotEmpty())
        
        val leftDelim = delimiters[0]
        assertEquals("[", leftDelim.delimiter)
        assertEquals(1.8f, leftDelim.size, "\\Big 应该是 1.8x")
    }
    
    @Test
    fun testBiggCurlyBrace() {
        val doc = parser.parse("\\bigg\\{ x \\bigg\\}")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.isNotEmpty())
        
        val leftDelim = delimiters[0]
        assertEquals("{", leftDelim.delimiter)
        assertEquals(2.4f, leftDelim.size, "\\bigg 应该是 2.4x")
    }
    
    @Test
    fun testBiggVerticalBar() {
        val doc = parser.parse("\\Bigg| x \\Bigg|")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.isNotEmpty())
        
        val leftDelim = delimiters[0]
        assertEquals("|", leftDelim.delimiter)
        assertEquals(3.0f, leftDelim.size, "\\Bigg 应该是 3.0x")
    }
    
    @Test
    fun testAllManualSizes() {
        // 测试所有四个大小级别
        val testCases = listOf(
            "\\big(" to 1.2f,
            "\\Big[" to 1.8f,
            "\\bigg\\{" to 2.4f,
            "\\Bigg|" to 3.0f
        )
        
        for ((latex, expectedSize) in testCases) {
            val doc = parser.parse(latex)
            val delim = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>().first()
            assertEquals(expectedSize, delim.size, "大小不匹配: $latex")
        }
    }
    
    @Test
    fun testManualSizeWithDirectionSuffix() {
        // 测试方向后缀 \bigl( 和 \bigr)
        val doc = parser.parse("\\bigl( x \\bigr)")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.isNotEmpty(), "应该解析出分隔符")
        
        // 方向后缀（l, r, m）应该被跳过，不影响解析结果
        val leftDelim = delimiters[0]
        assertEquals("(", leftDelim.delimiter)
        assertEquals(1.2f, leftDelim.size)
    }
    
    @Test
    fun testManualSizeWithAngleBrackets() {
        val doc = parser.parse("\\big\\langle x \\big\\rangle")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.size >= 2)
        
        val leftDelim = delimiters[0]
        assertEquals("⟨", leftDelim.delimiter)
        assertEquals(1.2f, leftDelim.size)
    }
    
    @Test
    fun testManualSizeWithFloorCeilBrackets() {
        val doc = parser.parse("\\Big\\lfloor x \\Big\\rfloor \\Big\\lceil y \\Big\\rceil")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(delimiters.size >= 2, "应该至少有两对分隔符")
        
        // 检查 lfloor
        val floorDelim = delimiters.first { it.delimiter == "⌊" }
        assertEquals("⌊", floorDelim.delimiter)
        assertEquals(1.8f, floorDelim.size)
        
        // 检查 lceil
        val ceilDelim = delimiters.first { it.delimiter == "⌈" }
        assertEquals("⌈", ceilDelim.delimiter)
        assertEquals(1.8f, ceilDelim.size)
    }
    
    // ========== 混合场景测试 ==========
    
    @Test
    fun testMixedAutoAndManualDelimiters() {
        // 混合自动伸缩和手动大小
        val doc = parser.parse("\\Bigg( \\left. \\frac{df}{dx} \\right|_{x=0} \\Bigg)")
        
        // 递归查找所有 Delimited 节点
        fun findAllDelimited(node: LatexNode): List<LatexNode.Delimited> {
            val result = mutableListOf<LatexNode.Delimited>()
            if (node is LatexNode.Delimited) {
                result.add(node)
            }
            when (node) {
                is LatexNode.Subscript -> result.addAll(findAllDelimited(node.base))
                is LatexNode.Delimited -> node.content.forEach { result.addAll(findAllDelimited(it)) }
                is LatexNode.Group -> node.children.forEach { result.addAll(findAllDelimited(it)) }
                is LatexNode.Fraction -> {
                    result.addAll(findAllDelimited(node.numerator))
                    result.addAll(findAllDelimited(node.denominator))
                }
                else -> {}
            }
            return result
        }
        
        val allDelimiters = doc.children.flatMap { findAllDelimited(it) }
        
        // 应该有手动大小的分隔符（作为独立节点）
        val manualDelimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(manualDelimiters.isNotEmpty(), "应该有手动大小分隔符")
        
        // 应该有自动伸缩的内层括号
        val autoDelimiters = allDelimiters.filter { it.scalable }
        assertTrue(autoDelimiters.isNotEmpty(), "应该有自动伸缩分隔符")
    }
    
    @Test
    fun testComplexFormula() {
        // 复杂公式：\Bigg[ \sum_{i=1}^n \left| x_i \right| \Bigg]
        val doc = parser.parse("\\Bigg[ \\sum_{i=1}^n \\left| x_i \\right| \\Bigg]")
        
        // 手动大小分隔符
        val manualDelimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        assertTrue(manualDelimiters.isNotEmpty(), "应该有手动大小分隔符")
        
        // 自动伸缩分隔符（需要递归查找）
        fun findAllDelimited(node: LatexNode): List<LatexNode.Delimited> {
            val result = mutableListOf<LatexNode.Delimited>()
            if (node is LatexNode.Delimited) result.add(node)
            when (node) {
                is LatexNode.BigOperator -> {
                    node.subscript?.let { result.addAll(findAllDelimited(it)) }
                    node.superscript?.let { result.addAll(findAllDelimited(it)) }
                }
                is LatexNode.Group -> node.children.forEach { result.addAll(findAllDelimited(it)) }
                else -> {}
            }
            return result
        }
        
        val autoDelimiters = doc.children.flatMap { findAllDelimited(it) }
        assertTrue(autoDelimiters.isNotEmpty(), "应该有自动伸缩分隔符")
    }
    
    @Test
    fun testMultipleManualSizeLevels() {
        // 测试多级嵌套：\big( \Big[ \bigg\{ x \bigg\} \Big] \big)
        val doc = parser.parse("\\big( \\Big[ \\bigg\\{ x \\bigg\\} \\Big] \\big)")
        
        val delimiters = doc.children.filterIsInstance<LatexNode.ManualSizedDelimiter>()
        
        // 应该有多个不同大小的分隔符
        val sizes = delimiters.map { it.size }.toSet()
        assertTrue(sizes.size >= 2, "应该有多个不同大小级别")
        assertTrue(1.2f in sizes || 1.8f in sizes || 2.4f in sizes)
    }
}
