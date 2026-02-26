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
import kotlin.test.assertTrue

/**
 * 张量/指标测试 — \tensor, \indices
 */
class TensorTest {

    private val parser = LatexParser()

    @Test
    fun testTensorBasic() {
        val doc = parser.parse("\\tensor{T}{^a_b}")
        assertEquals(1, doc.children.size)
        val tensor = doc.children[0] as LatexNode.Tensor
        assertEquals(2, tensor.indices.size)
        assertTrue(tensor.indices[0].first, "First index should be upper (^a)")
        assertTrue(!tensor.indices[1].first, "Second index should be lower (_b)")
    }

    @Test
    fun testTensorMultipleIndices() {
        val doc = parser.parse("\\tensor{R}{^a_b^c_d}")
        assertEquals(1, doc.children.size)
        val tensor = doc.children[0] as LatexNode.Tensor
        assertEquals(4, tensor.indices.size)
        assertTrue(tensor.indices[0].first)   // ^a
        assertTrue(!tensor.indices[1].first)  // _b
        assertTrue(tensor.indices[2].first)   // ^c
        assertTrue(!tensor.indices[3].first)  // _d
    }

    @Test
    fun testIndices() {
        val doc = parser.parse("T\\indices{^a_b}")
        val tensorNode = doc.children.filterIsInstance<LatexNode.Tensor>().firstOrNull()
        assertNotNull(tensorNode, "Tensor node should exist from \\indices")
        assertTrue(tensorNode.indices.isNotEmpty())
    }

    @Test
    fun testTensorNoIndices() {
        val doc = parser.parse("\\tensor{T}{}")
        assertEquals(1, doc.children.size)
        val tensor = doc.children[0] as LatexNode.Tensor
        assertEquals(0, tensor.indices.size)
    }
}
