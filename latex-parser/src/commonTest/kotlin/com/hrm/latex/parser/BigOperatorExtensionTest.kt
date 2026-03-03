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

/**
 * BigOperator 补全测试：
 * \coprod, \bigoplus, \bigotimes, \bigsqcup, \bigodot, \biguplus
 */
class BigOperatorExtensionTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_coprod() {
        val result = parser.parse("\\coprod_{i=1}^{n}")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("coprod", bigOp.operator)
    }

    @Test
    fun should_parse_bigoplus() {
        val result = parser.parse("\\bigoplus_{k}")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("bigoplus", bigOp.operator)
    }

    @Test
    fun should_parse_bigotimes() {
        val result = parser.parse("\\bigotimes_{i=1}^{n}")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("bigotimes", bigOp.operator)
    }

    @Test
    fun should_parse_bigsqcup() {
        val result = parser.parse("\\bigsqcup_{j}")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("bigsqcup", bigOp.operator)
    }

    @Test
    fun should_parse_bigodot() {
        val result = parser.parse("\\bigodot")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("bigodot", bigOp.operator)
    }

    @Test
    fun should_parse_biguplus() {
        val result = parser.parse("\\biguplus_{i}")
        assertIs<LatexNode.Document>(result)
        val bigOp = result.children[0]
        assertIs<LatexNode.BigOperator>(bigOp)
        assertEquals("biguplus", bigOp.operator)
    }
}
