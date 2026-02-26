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
import kotlin.test.assertTrue

/**
 * \tag 和 \tag* 公式编号标签测试
 */
class TagTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_tag() {
        val result = parser.parse("E = mc^2 \\tag{1}")
        assertIs<LatexNode.Document>(result)
        val hasTag = result.children.any { it is LatexNode.Tag }
        assertTrue(hasTag, "Should contain tag node")
        val tag = result.children.filterIsInstance<LatexNode.Tag>().first()
        assertEquals(false, tag.starred)
    }

    @Test
    fun should_parse_tag_star() {
        val result = parser.parse("E = mc^2 \\tag*{A}")
        assertIs<LatexNode.Document>(result)
        val hasTag = result.children.any { it is LatexNode.Tag }
        assertTrue(hasTag, "Should contain tag* node")
        val tag = result.children.filterIsInstance<LatexNode.Tag>().first()
        assertEquals(true, tag.starred)
    }
}
