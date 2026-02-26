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
 * \underbrace{...}_{text} 和 \overbrace{...}^{text} 花括号标注测试
 */
class BraceAnnotationTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_underbrace_with_subscript() {
        val result = parser.parse("\\underbrace{x+y}_{n}")
        assertIs<LatexNode.Document>(result)
        val sub = result.children[0]
        assertIs<LatexNode.Subscript>(sub)
        val accent = sub.base
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.UNDERBRACE, accent.accentType)
    }

    @Test
    fun should_parse_overbrace_with_superscript() {
        val result = parser.parse("\\overbrace{a+b+c}^{3}")
        assertIs<LatexNode.Document>(result)
        val sup = result.children[0]
        assertIs<LatexNode.Superscript>(sup)
        val accent = sup.base
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.OVERBRACE, accent.accentType)
    }
}
