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

/**
 * 标签引用测试 — \label, \ref, \eqref
 */
class LabelRefTest {

    private val parser = LatexParser()

    @Test
    fun testLabel() {
        val doc = parser.parse("\\label{eq:1}")
        assertEquals(1, doc.children.size)
        val label = doc.children[0] as LatexNode.Label
        assertEquals("eq:1", label.key)
    }

    @Test
    fun testRef() {
        val doc = parser.parse("\\ref{eq:1}")
        assertEquals(1, doc.children.size)
        val ref = doc.children[0] as LatexNode.Ref
        assertEquals("eq:1", ref.key)
    }

    @Test
    fun testEqRef() {
        val doc = parser.parse("\\eqref{eq:1}")
        assertEquals(1, doc.children.size)
        val eqRef = doc.children[0] as LatexNode.EqRef
        assertEquals("eq:1", eqRef.key)
    }

    @Test
    fun testLabelInContext() {
        val doc = parser.parse("E = mc^2 \\label{einstein}")
        val labelNode = doc.children.filterIsInstance<LatexNode.Label>().firstOrNull()
        assertNotNull(labelNode, "Label node should exist")
        assertEquals("einstein", labelNode.key)
    }

    @Test
    fun testRefInContext() {
        val doc = parser.parse("See equation \\ref{eq:1}")
        val refNode = doc.children.filterIsInstance<LatexNode.Ref>().firstOrNull()
        assertNotNull(refNode, "Ref node should exist")
        assertEquals("eq:1", refNode.key)
    }

    @Test
    fun testCombinedRefAndLabel() {
        val doc = parser.parse("\\label{eq:euler} e^{i\\pi} + 1 = 0 \\eqref{eq:euler}")
        val labels = doc.children.filterIsInstance<LatexNode.Label>()
        val eqRefs = doc.children.filterIsInstance<LatexNode.EqRef>()
        assertEquals(1, labels.size, "Should have one label")
        assertEquals(1, eqRefs.size, "Should have one eqref")
        assertEquals("eq:euler", labels[0].key)
        assertEquals("eq:euler", eqRefs[0].key)
    }
}
