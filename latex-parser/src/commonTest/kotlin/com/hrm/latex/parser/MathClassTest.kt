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

class MathClassTest {

    private val parser = LatexParser()

    private fun firstNode(input: String): LatexNode {
        val children = parser.parse(input).children
        assertEquals(1, children.size)
        return children[0]
    }

    @Test
    fun should_parse_mathbin() {
        val node = firstNode("\\mathbin{R}")
        assertIs<LatexNode.MathClass>(node)
        assertEquals(LatexNode.MathClass.AtomClass.BIN, node.atomClass)
    }

    @Test
    fun should_parse_mathrel() {
        val node = firstNode("\\mathrel{R}")
        assertIs<LatexNode.MathClass>(node)
        assertEquals(LatexNode.MathClass.AtomClass.REL, node.atomClass)
    }

    @Test
    fun should_parse_all_math_class_commands() {
        val cases = mapOf(
            "mathord" to LatexNode.MathClass.AtomClass.ORD,
            "mathbin" to LatexNode.MathClass.AtomClass.BIN,
            "mathrel" to LatexNode.MathClass.AtomClass.REL,
            "mathopen" to LatexNode.MathClass.AtomClass.OPEN,
            "mathclose" to LatexNode.MathClass.AtomClass.CLOSE,
            "mathpunct" to LatexNode.MathClass.AtomClass.PUNCT,
            "mathinner" to LatexNode.MathClass.AtomClass.INNER,
        )
        for ((cmd, expected) in cases) {
            val node = firstNode("\\$cmd{x}")
            assertIs<LatexNode.MathClass>(node)
            assertEquals(expected, node.atomClass, "class for \\$cmd")
        }
    }

    @Test
    fun should_keep_content_children() {
        val node = firstNode("\\mathbin{x}")
        assertIs<LatexNode.MathClass>(node)
        val content = node.content
        assertEquals(1, content.size)
        val group = content[0]
        assertIs<LatexNode.Group>(group)
        val text = group.children.single()
        assertIs<LatexNode.Text>(text)
        assertEquals("x", text.content)
    }
}
