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
import kotlin.test.assertTrue

/**
 * 数学模式切换命令测试
 * 
 * 测试 \displaystyle, \textstyle, \scriptstyle, \scriptscriptstyle 等命令
 */
class MathStyleTest {
    private val parser = LatexParser()

    @Test
    fun testDisplayStyle() {
        val doc = parser.parse("\\displaystyle x + y")
        val mathStyle = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.DISPLAY, mathStyle.mathStyleType)
        assertTrue(mathStyle.content.isNotEmpty())
    }

    @Test
    fun testTextStyle() {
        val doc = parser.parse("\\textstyle a + b")
        val mathStyle = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.TEXT, mathStyle.mathStyleType)
    }

    @Test
    fun testScriptStyle() {
        val doc = parser.parse("\\scriptstyle \\alpha")
        val mathStyle = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT, mathStyle.mathStyleType)
    }

    @Test
    fun testScriptScriptStyle() {
        val doc = parser.parse("\\scriptscriptstyle \\beta")
        val mathStyle = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT, mathStyle.mathStyleType)
    }

    @Test
    fun testMathStyleInFraction() {
        // \frac{\displaystyle a}{\textstyle b}
        val doc = parser.parse("\\frac{\\displaystyle a}{\\textstyle b}")
        val fraction = doc.children[0] as LatexNode.Fraction
        
        // 分子应该是 displaystyle
        val numerator = fraction.numerator as LatexNode.Group
        val numeratorStyle = numerator.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.DISPLAY, numeratorStyle.mathStyleType)
        
        // 分母应该是 textstyle
        val denominator = fraction.denominator as LatexNode.Group
        val denominatorStyle = denominator.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.TEXT, denominatorStyle.mathStyleType)
    }

    @Test
    fun testMathStyleInGroup() {
        // {\displaystyle x + y} + z
        val doc = parser.parse("{\\displaystyle x + y} + z")
        
        // 第一个元素应该是 Group，包含 MathStyle
        val group = doc.children[0] as LatexNode.Group
        val mathStyle = group.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.DISPLAY, mathStyle.mathStyleType)
        
        // 后面还有 + 和 z（不受 displaystyle 影响）
        assertTrue(doc.children.size > 1)
    }

    @Test
    fun testMathStyleInSum() {
        // \sum_{\scriptstyle i=1}^{\scriptstyle n}
        val doc = parser.parse("\\sum_{\\scriptstyle i=1}^{\\scriptstyle n}")
        val bigOp = doc.children[0] as LatexNode.BigOperator
        
        // 下标
        val subscript = bigOp.subscript as? LatexNode.Group
        val subscriptStyle = subscript?.children?.get(0) as? LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT, subscriptStyle?.mathStyleType)
        
        // 上标
        val superscript = bigOp.superscript as? LatexNode.Group
        val superscriptStyle = superscript?.children?.get(0) as? LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT, superscriptStyle?.mathStyleType)
    }

    @Test
    fun testNestedMathStyles() {
        // {\displaystyle {\scriptstyle x}}
        val doc = parser.parse("\\displaystyle{\\scriptstyle{x}}")
        val displayStyle = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.DISPLAY, displayStyle.mathStyleType)
        
        // 内层应该是包含 scriptstyle 的 Group
        val innerGroup = displayStyle.content[0] as LatexNode.Group
        val scriptStyle = innerGroup.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT, scriptStyle.mathStyleType)
    }

    @Test
    fun testMathStyleWithComplexExpression() {
        // \displaystyle{\sum_{i=1}^{n}\frac{1}{i^2}}
        val doc = parser.parse("\\displaystyle{\\sum_{i=1}^{n}\\frac{1}{i^2}}")
        val mathStyle = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.DISPLAY, mathStyle.mathStyleType)
        
        // 内容应该包含求和和分数
        assertTrue(mathStyle.content.isNotEmpty())
    }

    @Test
    fun testAllFourStyles() {
        val input = "\\displaystyle{A}\\textstyle{B}\\scriptstyle{C}\\scriptscriptstyle{D}"
        val doc = parser.parse(input)
        
        // 应该有4个 MathStyle 节点
        assertEquals(4, doc.children.size)
        
        val display = doc.children[0] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.DISPLAY, display.mathStyleType)
        
        val text = doc.children[1] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.TEXT, text.mathStyleType)
        
        val script = doc.children[2] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT, script.mathStyleType)
        
        val scriptscript = doc.children[3] as LatexNode.MathStyle
        assertEquals(LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT, scriptscript.mathStyleType)
    }
}
