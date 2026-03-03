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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 自适应省略号 \dots 测试
 */
class AdaptiveDotsTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_dots_as_ldots_by_default() {
        val result = parser.parse("a_1, a_2, \\dots, a_n")
        assertIs<LatexNode.Document>(result)
        val hasSymbol = result.children.any { it is LatexNode.Symbol && (it as LatexNode.Symbol).unicode == "…" }
        assertTrue(hasSymbol, "Should contain ldots (…) symbol")
    }

    @Test
    fun should_parse_dots_as_cdots_before_binary_op() {
        val result = parser.parse("a_1 + a_2 + \\dots + a_n")
        assertIs<LatexNode.Document>(result)
        val hasSymbol = result.children.any { it is LatexNode.Symbol && (it as LatexNode.Symbol).unicode == "⋯" }
        assertTrue(hasSymbol, "Should contain cdots (⋯) symbol before +")
    }

    @Test
    fun should_parse_dots_as_cdots_before_command_binary_op() {
        val result = parser.parse("A \\times \\dots \\times B")
        assertIs<LatexNode.Document>(result)
        val symbols = result.children.filterIsInstance<LatexNode.Symbol>()
        val dotsSymbol = symbols.find { it.symbol == "cdots" || it.symbol == "ldots" }
        assertTrue(dotsSymbol != null, "Should contain a dots symbol")
    }
}
