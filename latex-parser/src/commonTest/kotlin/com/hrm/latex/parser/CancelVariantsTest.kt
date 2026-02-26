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
 * \bcancel 和 \xcancel 取消线变体测试
 */
class CancelVariantsTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_bcancel() {
        val result = parser.parse("\\bcancel{x}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.BCANCEL, accent.accentType)
    }

    @Test
    fun should_parse_xcancel() {
        val result = parser.parse("\\xcancel{abc}")
        assertIs<LatexNode.Document>(result)
        val accent = result.children[0]
        assertIs<LatexNode.Accent>(accent)
        assertEquals(LatexNode.Accent.AccentType.XCANCEL, accent.accentType)
    }

    @Test
    fun should_parse_cancel_variants_in_expression() {
        val result = parser.parse("\\cancel{a} + \\bcancel{b} + \\xcancel{c}")
        assertIs<LatexNode.Document>(result)
        val accents = result.children.filterIsInstance<LatexNode.Accent>()
        assertEquals(3, accents.size)
        assertEquals(LatexNode.Accent.AccentType.CANCEL, accents[0].accentType)
        assertEquals(LatexNode.Accent.AccentType.BCANCEL, accents[1].accentType)
        assertEquals(LatexNode.Accent.AccentType.XCANCEL, accents[2].accentType)
    }
}
