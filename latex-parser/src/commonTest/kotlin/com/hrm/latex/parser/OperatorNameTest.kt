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
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.parser.visitor.MathMLVisitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * \operatorname 自定义运算符测试
 */
class OperatorNameTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_operatorname_standalone() {
        val result = parser.parse("\\operatorname{Tr}")
        assertIs<LatexNode.Document>(result)
        val opName = result.children[0]
        assertIs<LatexNode.OperatorName>(opName)
        assertEquals("Tr", opName.name)
    }

    @Test
    fun should_parse_operatorname_with_subscript() {
        val result = parser.parse("\\operatorname{Tr}_A")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("Tr", bigOp.operator)
    }

    @Test
    fun should_parse_operatorname_with_limits() {
        val result = parser.parse("\\operatorname{argmax}\\limits_{x}")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("argmax", bigOp.operator)
        assertEquals(LatexNode.BigOperator.LimitsMode.LIMITS, bigOp.limitsMode)
    }

    @Test
    fun should_parse_operatorname_empty_gracefully() {
        val result = parser.parse("\\operatorname{}")
        assertIs<LatexNode.Document>(result)
        assertTrue(result.children.isNotEmpty())
    }

    @Test
    fun should_produce_accessibility_for_operatorname() {
        val result = parser.parse("\\operatorname{Tr}")
        val desc = AccessibilityVisitor.describe(result)
        assertTrue(desc.contains("Tr"))
    }

    @Test
    fun should_produce_mathml_for_operatorname() {
        val result = parser.parse("\\operatorname{Tr}")
        val mathml = MathMLVisitor.convert(result)
        assertTrue(mathml.contains("<mo>Tr</mo>"))
    }
}
