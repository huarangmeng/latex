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
 * \substack 多行下标条件测试
 */
class SubstackTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_substack() {
        val result = parser.parse("\\sum_{\\substack{i<n \\\\ j<m}} x_{ij}")
        assertIs<LatexNode.Document>(result)
        val hasSubstack = findNode(result) { it is LatexNode.Substack }
        assertTrue(hasSubstack, "Should contain substack node")
    }

    @Test
    fun should_parse_substack_with_multiple_rows() {
        val result = parser.parse("\\substack{a \\\\ b \\\\ c}")
        assertIs<LatexNode.Document>(result)
        val substack = result.children[0]
        assertIs<LatexNode.Substack>(substack)
        assertEquals(3, substack.rows.size)
    }

    @Test
    fun should_parse_substack_single_row() {
        val result = parser.parse("\\substack{a + b}")
        assertIs<LatexNode.Document>(result)
        val substack = result.children[0]
        assertIs<LatexNode.Substack>(substack)
        assertEquals(1, substack.rows.size)
    }

    private fun findNode(node: LatexNode, predicate: (LatexNode) -> Boolean): Boolean {
        if (predicate(node)) return true
        return when (node) {
            is LatexNode.Document -> node.children.any { findNode(it, predicate) }
            is LatexNode.Group -> node.children.any { findNode(it, predicate) }
            is LatexNode.Subscript -> findNode(node.base, predicate) || findNode(node.index, predicate)
            is LatexNode.Superscript -> findNode(node.base, predicate) || findNode(node.exponent, predicate)
            is LatexNode.BigOperator -> {
                (node.subscript?.let { findNode(it, predicate) } ?: false) ||
                        (node.superscript?.let { findNode(it, predicate) } ?: false)
            }
            else -> false
        }
    }
}
