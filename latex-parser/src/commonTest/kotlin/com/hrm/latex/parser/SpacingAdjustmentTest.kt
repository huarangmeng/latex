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
 * \smash、\vphantom、\hphantom 间距微调命令测试
 */
class SpacingAdjustmentTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_smash() {
        val result = parser.parse("\\smash{\\frac{a}{b}}")
        assertIs<LatexNode.Document>(result)
        val smash = result.children[0]
        assertIs<LatexNode.Smash>(smash)
        assertTrue(smash.content.isNotEmpty())
    }

    @Test
    fun should_parse_vphantom() {
        val result = parser.parse("\\vphantom{\\frac{a}{b}}")
        assertIs<LatexNode.Document>(result)
        val vphantom = result.children[0]
        assertIs<LatexNode.VPhantom>(vphantom)
        assertTrue(vphantom.content.isNotEmpty())
    }

    @Test
    fun should_parse_hphantom() {
        val result = parser.parse("\\hphantom{xyz}")
        assertIs<LatexNode.Document>(result)
        val hphantom = result.children[0]
        assertIs<LatexNode.HPhantom>(hphantom)
        assertTrue(hphantom.content.isNotEmpty())
    }

    @Test
    fun should_parse_smash_in_expression() {
        val result = parser.parse("x + \\smash{\\frac{a}{b}} + y")
        assertIs<LatexNode.Document>(result)
        val hasSmash = result.children.any { it is LatexNode.Smash }
        assertTrue(hasSmash, "Should contain smash node")
    }
}
