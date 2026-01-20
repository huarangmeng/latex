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
import kotlin.test.assertTrue

class SpaceTest {

    @Test
    fun should_parse_negative_thin_space_correctly() {
        // Arrange
        val input = "a \\! b"
        val parser = LatexParser()

        // Act
        val result = parser.parse(input)

        // Assert
        // a, space, \!, space, b
        // The parser converts tokenizer whitespaces to LatexNode.Space(NORMAL) if handled in parseFactor
        // Let's verify structure
        val children = result.children
        // Expected: Text(a), Space(NORMAL), Space(NEGATIVE_THIN), Space(NORMAL), Text(b)
        // Wait, LatexParser parseFactor:
        // is LatexToken.Whitespace -> LatexNode.Space(LatexNode.Space.SpaceType.NORMAL)
        
        // Let's print to see what we get or inspect children
        assertTrue(children.isNotEmpty())
        
        val negativeSpaceNode = children.find { 
            it is LatexNode.Space && it.type == LatexNode.Space.SpaceType.NEGATIVE_THIN 
        }
        
        assertTrue(negativeSpaceNode != null, "Should contain negative thin space")
    }

    @Test
    fun should_parse_hspace_correctly() {
        // Arrange
        val input = "a \\hspace{1cm} b"
        val parser = LatexParser()

        // Act
        val result = parser.parse(input)

        // Assert
        val children = result.children
        val hSpaceNode = children.find { it is LatexNode.HSpace }
        
        assertTrue(hSpaceNode != null, "Should contain HSpace")
        assertEquals("1cm", (hSpaceNode as LatexNode.HSpace).dimension)
    }
    
    @Test
    fun should_parse_hspace_with_different_units() {
        val parser = LatexParser()
        
        // Test pt
        val res1 = parser.parse("\\hspace{10pt}")
        assertEquals("10pt", (res1.children.first() as LatexNode.HSpace).dimension)
        
        // Test em
        val res2 = parser.parse("\\hspace{2em}")
        assertEquals("2em", (res2.children.first() as LatexNode.HSpace).dimension)
        
        // Test negative
        val res3 = parser.parse("\\hspace{-5mm}")
        assertEquals("-5mm", (res3.children.first() as LatexNode.HSpace).dimension)
    }
}
