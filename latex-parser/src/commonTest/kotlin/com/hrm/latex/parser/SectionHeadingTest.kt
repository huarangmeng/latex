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
 * 章节标题命令测试
 * 测试 \section, \subsection, \subsubsection, \paragraph, \subparagraph
 */
class SectionHeadingTest {
    private val parser = LatexParser()

    // ========== \section 测试 ==========

    @Test
    fun testSectionBasic() {
        val doc = parser.parse("\\section{Introduction}")
        assertEquals(1, doc.children.size)
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SECTION, heading.level)
        assertEquals(false, heading.starred)
        assertTrue(heading.content.isNotEmpty())
    }

    @Test
    fun testSectionStarred() {
        val doc = parser.parse("\\section*{Introduction}")
        val headings = doc.children.filterIsInstance<LatexNode.SectionHeading>()
        assertEquals(1, headings.size)
        val heading = headings[0]
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SECTION, heading.level)
        assertEquals(true, heading.starred)
    }

    @Test
    fun testSectionWithMath() {
        val doc = parser.parse("\\section{The \\alpha-\\beta Algorithm}")
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertTrue(heading.content.size > 1, "Content should have multiple nodes for mixed text and symbols")
    }

    // ========== \subsection 测试 ==========

    @Test
    fun testSubsection() {
        val doc = parser.parse("\\subsection{Background}")
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SUBSECTION, heading.level)
    }

    @Test
    fun testSubsectionStarred() {
        val doc = parser.parse("\\subsection*{Background}")
        val headings = doc.children.filterIsInstance<LatexNode.SectionHeading>()
        assertEquals(1, headings.size)
        val heading = headings[0]
        assertEquals(true, heading.starred)
    }

    // ========== \subsubsection 测试 ==========

    @Test
    fun testSubsubsection() {
        val doc = parser.parse("\\subsubsection{Details}")
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SUBSUBSECTION, heading.level)
    }

    // ========== \paragraph 测试 ==========

    @Test
    fun testParagraph() {
        val doc = parser.parse("\\paragraph{Note}")
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.PARAGRAPH, heading.level)
    }

    // ========== \subparagraph 测试 ==========

    @Test
    fun testSubparagraph() {
        val doc = parser.parse("\\subparagraph{Remark}")
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SUBPARAGRAPH, heading.level)
    }

    // ========== 混合使用测试 ==========

    @Test
    fun testMultipleSections() {
        val doc = parser.parse("\\section{A} \\subsection{B} \\subsubsection{C}")
        val headings = doc.children.filterIsInstance<LatexNode.SectionHeading>()
        assertEquals(3, headings.size)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SECTION, headings[0].level)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SUBSECTION, headings[1].level)
        assertEquals(LatexNode.SectionHeading.HeadingLevel.SUBSUBSECTION, headings[2].level)
    }

    @Test
    fun testSectionWithContent() {
        val doc = parser.parse("\\section{Title} Some text after the title.")
        val headings = doc.children.filterIsInstance<LatexNode.SectionHeading>()
        assertEquals(1, headings.size)
        // 标题后的文本应该是独立节点
        assertTrue(doc.children.size > 1, "Should have heading + text content")
    }

    // ========== 可访问性 ==========

    @Test
    fun testSectionAccessibility() {
        val doc = parser.parse("\\section{Introduction}")
        val description = com.hrm.latex.parser.visitor.AccessibilityVisitor.describe(doc)
        assertTrue(description.contains("section"), "Accessibility should mention 'section'")
        assertTrue(description.contains("Introduction"), "Accessibility should mention title text")
    }

    // ========== 边界条件 ==========

    @Test
    fun testSectionEmptyTitle() {
        val doc = parser.parse("\\section{}")
        val heading = doc.children[0]
        assertIs<LatexNode.SectionHeading>(heading)
        assertTrue(heading.content.isEmpty() || heading.content.all {
            it is LatexNode.Text && it.content.isBlank()
        })
    }
}
