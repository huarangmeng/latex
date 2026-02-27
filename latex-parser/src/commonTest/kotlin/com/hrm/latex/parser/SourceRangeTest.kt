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
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceRangeTest {

    // ==========================================================
    // SourceRange 基础功能
    // ==========================================================

    @Test
    fun testSourceRange_contains() {
        val range = SourceRange(3, 7)
        assertTrue(range.contains(3))
        assertTrue(range.contains(5))
        assertTrue(range.contains(6))
        assertTrue(!range.contains(2))
        assertTrue(!range.contains(7))
    }

    @Test
    fun testSourceRange_merge() {
        val a = SourceRange(2, 5)
        val b = SourceRange(8, 12)
        val merged = a.merge(b)
        assertEquals(2, merged.start)
        assertEquals(12, merged.end)
    }

    @Test
    fun testSourceRange_length() {
        assertEquals(4, SourceRange(3, 7).length)
        assertEquals(0, SourceRange.EMPTY.length)
    }

    // ==========================================================
    // Tokenizer SourceRange
    // ==========================================================

    @Test
    fun testTokenizer_simpleText_hasCorrectRange() {
        val input = "hello"
        val tokens = LatexTokenizer(input).tokenize()
        val text = tokens[0] as LatexToken.Text
        assertEquals(SourceRange(0, 5), text.range)
        assertEquals(input, input.substring(text.range.start, text.range.end))
    }

    @Test
    fun testTokenizer_command_hasCorrectRange() {
        val input = "\\alpha"
        val tokens = LatexTokenizer(input).tokenize()
        val cmd = tokens[0] as LatexToken.Command
        assertEquals(SourceRange(0, 6), cmd.range)
        assertEquals("\\alpha", input.substring(cmd.range.start, cmd.range.end))
    }

    @Test
    fun testTokenizer_braces_haveCorrectRange() {
        val input = "{x}"
        val tokens = LatexTokenizer(input).tokenize()
        val lb = tokens[0] as LatexToken.LeftBrace
        val text = tokens[1] as LatexToken.Text
        val rb = tokens[2] as LatexToken.RightBrace
        assertEquals(SourceRange(0, 1), lb.range)
        assertEquals(SourceRange(1, 2), text.range)
        assertEquals(SourceRange(2, 3), rb.range)
    }

    @Test
    fun testTokenizer_superscript_subscript_range() {
        val input = "x^2_3"
        val tokens = LatexTokenizer(input).tokenize()
        // x, ^, 2, _, 3, EOF
        val x = tokens[0] as LatexToken.Text
        val sup = tokens[1] as LatexToken.Superscript
        val two = tokens[2] as LatexToken.Text
        val sub = tokens[3] as LatexToken.Subscript
        val three = tokens[4] as LatexToken.Text
        assertEquals(SourceRange(0, 1), x.range)
        assertEquals(SourceRange(1, 2), sup.range)
        assertEquals(SourceRange(2, 3), two.range)
        assertEquals(SourceRange(3, 4), sub.range)
        assertEquals(SourceRange(4, 5), three.range)
    }

    @Test
    fun testTokenizer_beginEnvironment_range() {
        val input = "\\begin{matrix}"
        val tokens = LatexTokenizer(input).tokenize()
        val begin = tokens[0] as LatexToken.BeginEnvironment
        assertEquals(SourceRange(0, 14), begin.range)
    }

    @Test
    fun testTokenizer_newLine_range() {
        val input = "\\\\"
        val tokens = LatexTokenizer(input).tokenize()
        val nl = tokens[0] as LatexToken.NewLine
        assertEquals(SourceRange(0, 2), nl.range)
    }

    @Test
    fun testTokenizer_whitespace_range() {
        val input = "x  y"
        val tokens = LatexTokenizer(input).tokenize()
        // x, whitespace, y, EOF
        val ws = tokens[1] as LatexToken.Whitespace
        assertEquals(SourceRange(1, 3), ws.range)
    }

    @Test
    fun testTokenizer_specialChars_range() {
        val input = "\\{"
        val tokens = LatexTokenizer(input).tokenize()
        val cmd = tokens[0] as LatexToken.Command
        assertEquals("{", cmd.name)
        assertEquals(SourceRange(0, 2), cmd.range)
    }

    @Test
    fun testTokenizer_complexExpression_ranges_cover_full_input() {
        val input = "\\frac{a}{b}"
        val tokens = LatexTokenizer(input).tokenize()
        // 验证所有非 EOF token 的范围连续覆盖整个输入
        val nonEof = tokens.filter { it !is LatexToken.EOF }
        assertEquals(0, nonEof.first().range.start)
        for (i in 1 until nonEof.size) {
            assertEquals(
                nonEof[i - 1].range.end, nonEof[i].range.start,
                "Gap between token ${i - 1} and $i"
            )
        }
        assertEquals(input.length, nonEof.last().range.end)
    }

    // ==========================================================
    // Parser SourceRange — 节点级
    // ==========================================================

    @Test
    fun testParser_document_coversFullInput() {
        val input = "x + y"
        val doc = LatexParser().parse(input)
        assertNotNull(doc.sourceRange)
        assertEquals(0, doc.sourceRange!!.start)
        assertEquals(input.length, doc.sourceRange!!.end)
    }

    @Test
    fun testParser_textNode_hasTokenRange() {
        val input = "hello"
        val doc = LatexParser().parse(input)
        val text = doc.children[0] as LatexNode.Text
        assertNotNull(text.sourceRange)
        assertEquals(0, text.sourceRange!!.start)
        assertEquals(5, text.sourceRange!!.end)
    }

    @Test
    fun testParser_superscript_coversBaseAndExponent() {
        val input = "x^2"
        val doc = LatexParser().parse(input)
        val sup = doc.children[0] as LatexNode.Superscript
        assertNotNull(sup.sourceRange)
        assertEquals(0, sup.sourceRange!!.start)
        assertEquals(3, sup.sourceRange!!.end)
    }

    @Test
    fun testParser_subscript_coversBaseAndIndex() {
        val input = "x_i"
        val doc = LatexParser().parse(input)
        val sub = doc.children[0] as LatexNode.Subscript
        assertNotNull(sub.sourceRange)
        assertEquals(0, sub.sourceRange!!.start)
        assertEquals(3, sub.sourceRange!!.end)
    }

    @Test
    fun testParser_group_coversFullBraces() {
        val input = "{abc}"
        val doc = LatexParser().parse(input)
        val group = doc.children[0] as LatexNode.Group
        assertNotNull(group.sourceRange)
        assertEquals(0, group.sourceRange!!.start)
        assertEquals(5, group.sourceRange!!.end)
    }

    @Test
    fun testParser_fraction_hasSourceRange() {
        val input = "\\frac{a}{b}"
        val doc = LatexParser().parse(input)
        val frac = doc.children[0]
        assertNotNull(frac.sourceRange, "Fraction node should have sourceRange")
        assertEquals(0, frac.sourceRange!!.start)
        assertEquals(input.length, frac.sourceRange!!.end)
    }

    @Test
    fun testParser_spaceNode_hasRange() {
        val input = "x y"
        val doc = LatexParser().parse(input)
        // x, space, y
        val space = doc.children[1] as LatexNode.Space
        assertNotNull(space.sourceRange)
        assertEquals(1, space.sourceRange!!.start)
        assertEquals(2, space.sourceRange!!.end)
    }

    @Test
    fun testParser_symbol_hasSourceRange() {
        val input = "\\alpha"
        val doc = LatexParser().parse(input)
        val node = doc.children[0]
        assertNotNull(node.sourceRange, "Symbol node should have sourceRange")
        assertEquals(0, node.sourceRange!!.start)
        assertEquals(6, node.sourceRange!!.end)
    }

    @Test
    fun testParser_environment_hasSourceRange() {
        val input = "\\begin{matrix}a\\end{matrix}"
        val doc = LatexParser().parse(input)
        val env = doc.children[0]
        assertNotNull(env.sourceRange, "Environment node should have sourceRange")
        assertEquals(0, env.sourceRange!!.start)
        assertEquals(input.length, env.sourceRange!!.end)
    }

    // ==========================================================
    // SourceMapper
    // ==========================================================

    @Test
    fun testSourceMapper_leafNodeAt_findsTextNode() {
        val input = "x+y"
        val doc = LatexParser().parse(input)
        val leaf = SourceMapper.leafNodeAt(doc, 0)
        assertNotNull(leaf)
        assertTrue(leaf is LatexNode.Text)
        assertEquals("x+y", (leaf as LatexNode.Text).content)
    }

    @Test
    fun testSourceMapper_leafNodeAt_outOfRange_returnsNull() {
        val input = "abc"
        val doc = LatexParser().parse(input)
        val leaf = SourceMapper.leafNodeAt(doc, 100)
        assertNull(leaf)
    }

    @Test
    fun testSourceMapper_nodePathAt_returnsRootToLeaf() {
        val input = "x^2"
        val doc = LatexParser().parse(input)
        // offset=2 is inside "2" (which is in the exponent position)
        val path = SourceMapper.nodePathAt(doc, 2)
        assertTrue(path.isNotEmpty(), "Path should not be empty")
        assertTrue(path.first() is LatexNode.Document, "First should be Document")
    }

    @Test
    fun testSourceMapper_collectLeaves_returnsAllLeafNodes() {
        val input = "x+y"
        val doc = LatexParser().parse(input)
        val leaves = SourceMapper.collectLeaves(doc)
        assertTrue(leaves.isNotEmpty())
    }

    @Test
    fun testSourceMapper_childrenOf_fraction() {
        val frac = LatexNode.Fraction(
            LatexNode.Text("a"),
            LatexNode.Text("b")
        )
        val children = SourceMapper.childrenOf(frac)
        assertEquals(2, children.size)
    }

    @Test
    fun testSourceMapper_childrenOf_leafNode() {
        val text = LatexNode.Text("hello")
        val children = SourceMapper.childrenOf(text)
        assertTrue(children.isEmpty())
    }
}
