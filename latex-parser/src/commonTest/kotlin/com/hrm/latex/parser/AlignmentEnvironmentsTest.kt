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

class AlignmentEnvironmentsTest {

    private val parser = LatexParser()

    // === flalign ===

    @Test
    fun should_parse_flalign() {
        val result = parser.parse("\\begin{flalign} a &= b \\\\ c &= d \\end{flalign}")
        val children = result.children
        assertEquals(1, children.size)
        assertIs<LatexNode.Aligned>(children[0])
        val aligned = children[0] as LatexNode.Aligned
        assertEquals(2, aligned.rows.size, "flalign should have 2 rows")
    }

    @Test
    fun should_parse_flalign_star() {
        val result = parser.parse("\\begin{flalign*} x &= 1 \\\\ y &= 2 \\end{flalign*}")
        val children = result.children
        assertEquals(1, children.size)
        assertIs<LatexNode.Aligned>(children[0])
        val aligned = children[0] as LatexNode.Aligned
        assertEquals(2, aligned.rows.size)
    }

    @Test
    fun should_parse_flalign_single_row() {
        val result = parser.parse("\\begin{flalign} a &= b \\end{flalign}")
        val children = result.children
        assertEquals(1, children.size)
        assertIs<LatexNode.Aligned>(children[0])
        val aligned = children[0] as LatexNode.Aligned
        assertEquals(1, aligned.rows.size)
    }

    // === alignat ===

    @Test
    fun should_parse_alignat_with_column_count() {
        val result = parser.parse("\\begin{alignat}{2} a &= b & c &= d \\\\ e &= f & g &= h \\end{alignat}")
        val children = result.children
        assertEquals(1, children.size)
        assertIs<LatexNode.Aligned>(children[0])
        val aligned = children[0] as LatexNode.Aligned
        assertEquals(2, aligned.rows.size, "alignat should have 2 rows")
    }

    @Test
    fun should_parse_alignat_star() {
        val result = parser.parse("\\begin{alignat*}{3} a &= b \\\\ c &= d \\end{alignat*}")
        val children = result.children
        assertEquals(1, children.size)
        assertIs<LatexNode.Aligned>(children[0])
    }

    @Test
    fun should_parse_alignat_single_group() {
        val result = parser.parse("\\begin{alignat}{1} x &= 1 \\end{alignat}")
        val children = result.children
        assertEquals(1, children.size)
        assertIs<LatexNode.Aligned>(children[0])
        val aligned = children[0] as LatexNode.Aligned
        assertEquals(1, aligned.rows.size)
    }

    @Test
    fun should_parse_alignat_multiple_columns() {
        val result = parser.parse("\\begin{alignat}{2} a &= b & \\quad c &= d \\end{alignat}")
        val children = result.children
        assertIs<LatexNode.Aligned>(children[0])
        val aligned = children[0] as LatexNode.Aligned
        assertEquals(1, aligned.rows.size)
        // Should have 4 cells (2 alignment groups × 2 columns each)
    }

    // === mixed ===

    @Test
    fun should_handle_flalign_with_fractions() {
        val result = parser.parse("\\begin{flalign*} \\frac{a}{b} &= c \\\\ d &= \\frac{e}{f} \\end{flalign*}")
        val children = result.children
        assertIs<LatexNode.Aligned>(children[0])
        assertEquals(2, (children[0] as LatexNode.Aligned).rows.size)
    }
}
