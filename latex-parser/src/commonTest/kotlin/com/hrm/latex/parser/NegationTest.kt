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

/**
 * \not 否定修饰符测试
 */
class NegationTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_not_equals() {
        val result = parser.parse("\\not=")
        assertIs<LatexNode.Document>(result)
        val neg = result.children[0]
        assertIs<LatexNode.Negation>(neg)
    }

    @Test
    fun should_parse_not_in() {
        val result = parser.parse("\\not\\in")
        assertIs<LatexNode.Document>(result)
        val neg = result.children[0]
        assertIs<LatexNode.Negation>(neg)
        assertIs<LatexNode.Symbol>(neg.content)
    }

    @Test
    fun should_parse_not_subset() {
        val result = parser.parse("\\not\\subset")
        assertIs<LatexNode.Document>(result)
        val neg = result.children[0]
        assertIs<LatexNode.Negation>(neg)
    }
}
