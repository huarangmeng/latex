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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 四角标注测试 — \sideset
 */
class SideSetTest {

    private val parser = LatexParser()

    @Test
    fun testSideSetBasic() {
        val doc = parser.parse("\\sideset{_a^b}{_c^d}{\\sum}")
        assertEquals(1, doc.children.size)
        val sideSet = doc.children[0] as LatexNode.SideSet
        assertNotNull(sideSet.leftSub, "Left subscript should exist")
        assertNotNull(sideSet.leftSup, "Left superscript should exist")
        assertNotNull(sideSet.rightSub, "Right subscript should exist")
        assertNotNull(sideSet.rightSup, "Right superscript should exist")
    }

    @Test
    fun testSideSetPartial() {
        val doc = parser.parse("\\sideset{_a}{^d}{\\sum}")
        assertEquals(1, doc.children.size)
        val sideSet = doc.children[0] as LatexNode.SideSet
        assertNotNull(sideSet.leftSub)
        assertNull(sideSet.leftSup)
        assertNull(sideSet.rightSub)
        assertNotNull(sideSet.rightSup)
    }

    @Test
    fun testSideSetEmpty() {
        val doc = parser.parse("\\sideset{}{}{\\prod}")
        assertEquals(1, doc.children.size)
        val sideSet = doc.children[0] as LatexNode.SideSet
        assertNull(sideSet.leftSub)
        assertNull(sideSet.leftSup)
        assertNull(sideSet.rightSub)
        assertNull(sideSet.rightSup)
    }
}
