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
 * 环境扩展测试
 * 测试 split, multline, eqnarray, subequations 环境
 */
class EnvironmentExtensionTest {
    private val parser = LatexParser()

    // ========== Split 环境测试 ==========

    @Test
    fun testSplitBasic() {
        val doc = parser.parse(
            """
            \begin{split}
            x &= a + b \\
            &= c
            \end{split}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Split)
        val split = doc.children[0] as LatexNode.Split
        assertEquals(2, split.rows.size)
    }

    @Test
    fun testSplitMultipleAlignments() {
        val doc = parser.parse(
            """
            \begin{split}
            a &= b + c \\
            &= d + e \\
            &= f
            \end{split}
        """.trimIndent()
        )
        val split = doc.children[0] as LatexNode.Split
        assertEquals(3, split.rows.size)
    }

    @Test
    fun testSplitWithFractions() {
        val doc = parser.parse(
            """
            \begin{split}
            \frac{x}{y} &= \frac{a}{b} \\
            &= \frac{c}{d}
            \end{split}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Split)
    }

    // ========== Multline 环境测试 ==========

    @Test
    fun testMultlineBasic() {
        val doc = parser.parse(
            """
            \begin{multline}
            a + b + c + d \\
            + e + f + g
            \end{multline}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Multline)
        val multline = doc.children[0] as LatexNode.Multline
        assertEquals(2, multline.lines.size)
    }

    @Test
    fun testMultlineThreeLines() {
        val doc = parser.parse(
            """
            \begin{multline}
            \text{First line (left)} \\
            \text{Second line (center)} \\
            \text{Third line (right)}
            \end{multline}
        """.trimIndent()
        )
        val multline = doc.children[0] as LatexNode.Multline
        assertEquals(3, multline.lines.size)
    }

    @Test
    fun testMultlineWithComplexFormula() {
        val doc = parser.parse(
            """
            \begin{multline}
            \int_0^1 x^2 dx \\
            + \sum_{i=1}^n i \\
            = \frac{1}{3} + \frac{n(n+1)}{2}
            \end{multline}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Multline)
    }

    // ========== Eqnarray 环境测试 ==========

    @Test
    fun testEqnarrayBasic() {
        val doc = parser.parse(
            """
            \begin{eqnarray}
            x &=& 1 \\
            y &=& 2
            \end{eqnarray}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Eqnarray)
        val eqnarray = doc.children[0] as LatexNode.Eqnarray
        assertEquals(2, eqnarray.rows.size)
    }

    @Test
    fun testEqnarrayThreeColumns() {
        val doc = parser.parse(
            """
            \begin{eqnarray}
            a + b &=& c \\
            d - e &=& f \\
            g \times h &=& i
            \end{eqnarray}
        """.trimIndent()
        )
        val eqnarray = doc.children[0] as LatexNode.Eqnarray
        assertEquals(3, eqnarray.rows.size)
        // 每行应该有3个单元格（左边、关系符、右边）
        assertTrue(eqnarray.rows[0].size <= 3)
    }

    @Test
    fun testEqnarrayWithComplexExpressions() {
        val doc = parser.parse(
            """
            \begin{eqnarray}
            \frac{x}{y} &=& \sqrt{z} \\
            x^2 + y^2 &=& z^2
            \end{eqnarray}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Eqnarray)
    }

    // ========== Subequations 环境测试 ==========

    @Test
    fun testSubequationsBasic() {
        val doc = parser.parse(
            """
            \begin{subequations}
            \begin{equation}
            x = 1
            \end{equation}
            \end{subequations}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Subequations)
        val subequations = doc.children[0] as LatexNode.Subequations
        assertTrue(subequations.content.isNotEmpty())
    }

    @Test
    fun testSubequationsWithMultipleEquations() {
        val doc = parser.parse(
            """
            \begin{subequations}
            \begin{equation}
            a = b
            \end{equation}
            \begin{equation}
            c = d
            \end{equation}
            \end{subequations}
        """.trimIndent()
        )
        val subequations = doc.children[0] as LatexNode.Subequations
        assertTrue(subequations.content.size >= 2)
    }

    @Test
    fun testSubequationsWithAlign() {
        val doc = parser.parse(
            """
            \begin{subequations}
            \begin{align}
            x &= 1 \\
            y &= 2
            \end{align}
            \end{subequations}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Subequations)
    }

    // ========== 混合使用测试 ==========

    @Test
    fun testSplitInsideEquation() {
        val doc = parser.parse(
            """
            \begin{equation}
            \begin{split}
            a &= b + c \\
            &= d
            \end{split}
            \end{equation}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Environment)
        val env = doc.children[0] as LatexNode.Environment
        assertEquals("equation", env.name)
        assertTrue(env.content.any { it is LatexNode.Split })
    }

    @Test
    fun testNestedEnvironments() {
        val doc = parser.parse(
            """
            \begin{subequations}
            \begin{align}
            x &= 1 \\
            y &= 2
            \end{align}
            \begin{multline}
            a + b + c \\
            + d + e
            \end{multline}
            \end{subequations}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Subequations)
    }

    @Test
    fun testEmptyEnvironment() {
        val doc = parser.parse(
            """
            \begin{split}
            \end{split}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Split)
        val split = doc.children[0] as LatexNode.Split
        // 空环境可能产生0行或1个空行,都是合理的
        assertTrue(split.rows.size <= 1)
    }

    // ========== nested environment tests (regression for infinite loop fix) ==========

    @Test
    fun testAlignedInsideAlign() {
        // this used to cause infinite loop when parseAligned encountered mismatched EndEnvironment
        val doc = parser.parse(
            """
            \begin{align}
            \begin{aligned}
            x &= 1 \\
            y &= 2
            \end{aligned}
            \end{align}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Aligned)
    }

    @Test
    fun testNestedAlignedEnvironments() {
        // regression test: mismatched EndEnvironment should not cause infinite loop
        val doc = parser.parse(
            """
            \begin{equation}
            \begin{aligned}
            a &= b \\
            c &= d
            \end{aligned}
            \end{equation}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Environment)
        val env = doc.children[0] as LatexNode.Environment
        assertEquals("equation", env.name)
        assertTrue(env.content.any { it is LatexNode.Aligned })
    }

    @Test
    fun testGatherInsideSubequations() {
        val doc = parser.parse(
            """
            \begin{subequations}
            \begin{gather}
            x = 1 \\
            y = 2
            \end{gather}
            \end{subequations}
        """.trimIndent()
        )
        assertTrue(doc.children[0] is LatexNode.Subequations)
    }

    // ========== tabular 环境 ==========

    @Test
    fun testTabularBasic() {
        val doc = parser.parse("\\begin{tabular}{cc} a & b \\\\ c & d \\end{tabular}")
        assertEquals(1, doc.children.size)
        val tabular = doc.children[0] as LatexNode.Tabular
        assertEquals("cc", tabular.alignment)
        assertEquals(2, tabular.rows.size)
        assertEquals(2, tabular.rows[0].size, "First row should have 2 cells")
        assertEquals(2, tabular.rows[1].size, "Second row should have 2 cells")
    }

    @Test
    fun testTabularThreeColumns() {
        val doc = parser.parse("\\begin{tabular}{lcr} left & center & right \\end{tabular}")
        assertEquals(1, doc.children.size)
        val tabular = doc.children[0] as LatexNode.Tabular
        assertEquals("lcr", tabular.alignment)
        assertEquals(1, tabular.rows.size)
        assertEquals(3, tabular.rows[0].size)
    }

    @Test
    fun testTabularSingleCell() {
        val doc = parser.parse("\\begin{tabular}{c} hello \\end{tabular}")
        assertEquals(1, doc.children.size)
        val tabular = doc.children[0] as LatexNode.Tabular
        assertEquals("c", tabular.alignment)
        assertEquals(1, tabular.rows.size)
    }

    // ========== 星号环境变体测试 ==========

    @Test
    fun testAlignStar() {
        val doc = parser.parse("\\begin{align*}a &= b \\\\ c &= d\\end{align*}")
        assertTrue(doc.children[0] is LatexNode.Aligned)
        val aligned = doc.children[0] as LatexNode.Aligned
        assertEquals(2, aligned.rows.size)
    }

    @Test
    fun testEquationStar() {
        val doc = parser.parse("\\begin{equation*}E=mc^2\\end{equation*}")
        assertTrue(doc.children.isNotEmpty())
    }

    @Test
    fun testGatherStar() {
        val doc = parser.parse("\\begin{gather*}a+b \\\\ c+d\\end{gather*}")
        assertTrue(doc.children[0] is LatexNode.Aligned)
    }

    @Test
    fun testMultlineStar() {
        val doc = parser.parse("\\begin{multline*}a+b \\\\ +c+d\\end{multline*}")
        assertTrue(doc.children[0] is LatexNode.Multline)
        val multline = doc.children[0] as LatexNode.Multline
        assertEquals(2, multline.lines.size)
    }

    @Test
    fun testEqnarrayStar() {
        val doc = parser.parse("\\begin{eqnarray*}a &=& b \\\\ c &=& d\\end{eqnarray*}")
        assertTrue(doc.children[0] is LatexNode.Eqnarray)
        val eqnarray = doc.children[0] as LatexNode.Eqnarray
        assertEquals(2, eqnarray.rows.size)
    }

    // ========== tabular 竖线 ==========

    @Test
    fun testTabularWithVerticalLines() {
        val doc = parser.parse(
            """
            \begin{tabular}{|c|c|c|}
            a & b & c
            \end{tabular}
        """.trimIndent()
        )
        val tabular = doc.children[0]
        assertIs<LatexNode.Tabular>(tabular)
        assertEquals("|c|c|c|", tabular.alignment)
    }

    @Test
    fun testTabularWithMixedAlignmentAndLines() {
        val doc = parser.parse(
            """
            \begin{tabular}{|l|c|r|}
            left & center & right
            \end{tabular}
        """.trimIndent()
        )
        val tabular = doc.children[0]
        assertIs<LatexNode.Tabular>(tabular)
        assertEquals("|l|c|r|", tabular.alignment)
    }

    // ========== \hline ==========

    @Test
    fun testHlineInTabular() {
        val doc = parser.parse(
            """
            \begin{tabular}{cc}
            \hline
            a & b \\
            \hline
            c & d \\
            \hline
            \end{tabular}
        """.trimIndent()
        )
        val tabular = doc.children[0]
        assertIs<LatexNode.Tabular>(tabular)
        assertTrue(tabular.rows.size >= 5, "Should have data rows + hline rows, got ${tabular.rows.size}")
    }

    @Test
    fun testStandaloneHline() {
        val doc = parser.parse("\\hline")
        assertIs<LatexNode.Document>(doc)
        val hline = doc.children[0]
        assertIs<LatexNode.HLine>(hline)
    }

    // ========== \cline ==========

    @Test
    fun testClineWithRange() {
        val doc = parser.parse("\\cline{1-3}")
        assertIs<LatexNode.Document>(doc)
        val cline = doc.children[0]
        assertIs<LatexNode.CLine>(cline)
        assertEquals(1, cline.startCol)
        assertEquals(3, cline.endCol)
    }

    @Test
    fun testClineInTabular() {
        val doc = parser.parse(
            """
            \begin{tabular}{ccc}
            a & b & c \\
            \cline{1-2}
            d & e & f
            \end{tabular}
        """.trimIndent()
        )
        val tabular = doc.children[0]
        assertIs<LatexNode.Tabular>(tabular)
        assertTrue(tabular.rows.size >= 3, "Should have data rows + cline rows")
    }

    // ========== \multicolumn ==========

    @Test
    fun testMulticolumn() {
        val doc = parser.parse("\\multicolumn{2}{c}{text}")
        assertIs<LatexNode.Document>(doc)
        val mc = doc.children[0]
        assertIs<LatexNode.Multicolumn>(mc)
        assertEquals(2, mc.columnCount)
        assertEquals("c", mc.alignment)
        assertTrue(mc.content.isNotEmpty())
    }

    @Test
    fun testMulticolumnInTabular() {
        val doc = parser.parse(
            """
            \begin{tabular}{|c|c|c|}
            \hline
            \multicolumn{2}{|c|}{merged} & c \\
            \hline
            a & b & c \\
            \hline
            \end{tabular}
        """.trimIndent()
        )
        val tabular = doc.children[0]
        assertIs<LatexNode.Tabular>(tabular)
        assertTrue(tabular.rows.isNotEmpty())
    }

    @Test
    fun testMulticolumnWithAlignmentLeft() {
        val doc = parser.parse("\\multicolumn{3}{l}{left aligned}")
        val mc = doc.children[0]
        assertIs<LatexNode.Multicolumn>(mc)
        assertEquals(3, mc.columnCount)
        assertEquals("l", mc.alignment)
    }

    // ========== 综合表格测试 ==========

    @Test
    fun testCompleteTableWithAllFeatures() {
        val doc = parser.parse(
            """
            \begin{tabular}{|c|c|c|}
            \hline
            \multicolumn{3}{|c|}{Title} \\
            \hline
            a & b & c \\
            \cline{1-2}
            d & e & f \\
            \hline
            \end{tabular}
        """.trimIndent()
        )
        val tabular = doc.children[0]
        assertIs<LatexNode.Tabular>(tabular)
        assertTrue(tabular.rows.size >= 4, "Should have multiple rows including hlines and clines")
    }

    // ========== 自定义环境 \newenvironment 测试 ==========

    @Test
    fun testNewEnvironmentBasic() {
        val doc = parser.parse(
            """
            \newenvironment{mybox}{\fbox\bgroup}{\egroup}
            \begin{mybox}hello\end{mybox}
        """.trimIndent()
        )
        // newenvironment 定义节点不渲染
        val defNode = doc.children[0]
        assertIs<LatexNode.NewEnvironment>(defNode)
        assertEquals("mybox", defNode.envName)
        assertEquals(0, defNode.numArgs)
    }

    @Test
    fun testNewEnvironmentWithArgs() {
        val doc = parser.parse(
            """
            \newenvironment{titled}[1]{\textbf{#1:}}{}
            \begin{titled}{Theorem}content\end{titled}
        """.trimIndent()
        )
        val defNode = doc.children[0]
        assertIs<LatexNode.NewEnvironment>(defNode)
        assertEquals("titled", defNode.envName)
        assertEquals(1, defNode.numArgs)
    }

    @Test
    fun testNewEnvironmentExpansion() {
        // 验证自定义环境展开后内容存在
        val doc = parser.parse(
            """
            \newenvironment{emphasis}{\textbf\bgroup}{\egroup}
            \begin{emphasis}important\end{emphasis}
        """.trimIndent()
        )
        assertTrue(doc.children.size >= 2, "Should have definition + expanded content")
        // 展开后的环境应变为 Group 节点
        val expanded = doc.children.drop(1).firstOrNull { it !is LatexNode.Space && it !is LatexNode.NewLine }
        assertIs<LatexNode.Group>(expanded)
    }

    @Test
    fun testNewEnvironmentWithDefaultArg() {
        val doc = parser.parse(
            """
            \newenvironment{note}[1][Note]{\textbf{#1:} }{}
            \begin{note}text\end{note}
        """.trimIndent()
        )
        val defNode = doc.children[0]
        assertIs<LatexNode.NewEnvironment>(defNode)
        assertEquals("Note", defNode.defaultArg)
    }

    @Test
    fun testRenewEnvironment() {
        val doc = parser.parse(
            """
            \renewenvironment{myenv}{\textbf\bgroup}{\egroup}
            \begin{myenv}text\end{myenv}
        """.trimIndent()
        )
        val defNode = doc.children[0]
        assertIs<LatexNode.NewEnvironment>(defNode)
        assertEquals("myenv", defNode.envName)
    }

    @Test
    fun testNewEnvironmentNoArgs_contentPreserved() {
        // 验证环境体内容在展开后得到保留
        val doc = parser.parse(
            """
            \newenvironment{simple}{}{}
            \begin{simple}x+y\end{simple}
        """.trimIndent()
        )
        // 展开后的 Group 应包含 body 内容
        val expanded = doc.children.filterIsInstance<LatexNode.Group>()
        assertTrue(expanded.isNotEmpty(), "Expanded environment should be a Group")
    }
}
