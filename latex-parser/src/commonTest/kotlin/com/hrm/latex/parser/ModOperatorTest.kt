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
 * 取模运算符测试：\bmod, \pmod, \mod
 */
class ModOperatorTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_bmod() {
        val result = parser.parse("a \\bmod b")
        assertIs<LatexNode.Document>(result)
        val mod = result.children.find { it is LatexNode.ModOperator }
        assertIs<LatexNode.ModOperator>(mod)
        assertEquals(LatexNode.ModOperator.ModStyle.BMOD, mod.modStyle)
    }

    @Test
    fun should_parse_pmod() {
        val result = parser.parse("a \\pmod{n}")
        assertIs<LatexNode.Document>(result)
        val mod = result.children.find { it is LatexNode.ModOperator }
        assertIs<LatexNode.ModOperator>(mod)
        assertEquals(LatexNode.ModOperator.ModStyle.PMOD, mod.modStyle)
        assertTrue(mod.content != null, "pmod should have content argument")
    }

    @Test
    fun should_parse_mod() {
        val result = parser.parse("a \\mod b")
        assertIs<LatexNode.Document>(result)
        val mod = result.children.find { it is LatexNode.ModOperator }
        assertIs<LatexNode.ModOperator>(mod)
        assertEquals(LatexNode.ModOperator.ModStyle.MOD, mod.modStyle)
    }

    @Test
    fun should_produce_accessibility_for_bmod() {
        val result = parser.parse("a \\bmod b")
        val desc = AccessibilityVisitor.describe(result)
        assertTrue(desc.contains("mod"), "Accessibility should contain 'mod', got: $desc")
    }

    @Test
    fun should_produce_accessibility_for_pmod() {
        val result = parser.parse("a \\pmod{n}")
        val desc = AccessibilityVisitor.describe(result)
        assertTrue(desc.contains("mod"), "Accessibility should contain 'mod', got: $desc")
    }

    @Test
    fun should_produce_mathml_for_bmod() {
        val result = parser.parse("a \\bmod b")
        val mathml = MathMLVisitor.convert(result)
        assertTrue(mathml.contains("<mo>mod</mo>"), "MathML should contain <mo>mod</mo>, got: $mathml")
    }

    @Test
    fun should_produce_mathml_for_pmod() {
        val result = parser.parse("a \\pmod{n}")
        val mathml = MathMLVisitor.convert(result)
        assertTrue(mathml.contains("<mo>mod</mo>"), "MathML should contain <mo>mod</mo>, got: $mathml")
    }
}
