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

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.LatexParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 公式编号自动计算测试
 *
 * 测试 [EquationNumbering.buildLabelMap] 的编号分配和标签映射逻辑。
 */
class EquationNumberingTest {

    private val parser = LatexParser()

    // ===================================================================
    // 正常路径 (Happy Path)
    // ===================================================================

    @Test
    fun should_assign_number_to_equation_environment() {
        val doc = parser.parse("\\begin{equation} E = mc^2 \\label{eq:1} \\end{equation}")
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:1"], "equation 环境中的 label 应映射到编号 1")
    }

    @Test
    fun should_assign_sequential_numbers_to_multiple_equations() {
        val input = """
            \begin{equation} a = b \label{eq:a} \end{equation}
            \begin{equation} c = d \label{eq:c} \end{equation}
            \begin{equation} e = f \label{eq:e} \end{equation}
        """.trimIndent()
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:a"])
        assertEquals("2", labelMap["eq:c"])
        assertEquals("3", labelMap["eq:e"])
    }

    @Test
    fun should_assign_number_to_align_environment() {
        val input = "\\begin{align} x &= 1 \\label{eq:align} \\end{align}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:align"], "align 环境中的 label 应映射到编号 1")
    }

    @Test
    fun should_assign_number_to_gather_environment() {
        val input = "\\begin{gather} x + y \\label{eq:gather} \\end{gather}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:gather"], "gather 环境中的 label 应映射到编号 1")
    }

    @Test
    fun should_assign_number_to_multline_environment() {
        val input = "\\begin{multline} a + b \\\\ c + d \\label{eq:multi} \\end{multline}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:multi"], "multline 环境中的 label 应映射到编号 1")
    }

    @Test
    fun should_assign_number_to_eqnarray_environment() {
        val input = "\\begin{eqnarray} a &=& b \\label{eq:eqn} \\end{eqnarray}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:eqn"], "eqnarray 环境中的 label 应映射到编号 1")
    }

    // ===================================================================
    // 星号变体（不编号）
    // ===================================================================

    @Test
    fun should_not_number_equation_star_environment() {
        val input = "\\begin{equation*} E = mc^2 \\label{eq:no} \\end{equation*}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertTrue(labelMap.isEmpty(), "equation* 环境不应分配编号")
    }

    @Test
    fun should_not_number_align_star_environment() {
        val input = "\\begin{align*} x &= 1 \\label{eq:no} \\end{align*}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertTrue(labelMap.isEmpty(), "align* 环境不应分配编号")
    }

    @Test
    fun should_mix_numbered_and_star_environments() {
        val input = """
            \begin{equation} a = b \label{eq:1} \end{equation}
            \begin{equation*} c = d \label{eq:no} \end{equation*}
            \begin{equation} e = f \label{eq:2} \end{equation}
        """.trimIndent()
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:1"], "第一个 equation 应编号 1")
        assertNull(labelMap["eq:no"], "equation* 中的 label 不应有编号")
        assertEquals("2", labelMap["eq:2"], "第二个 equation 应编号 2（跳过 equation*）")
    }

    // ===================================================================
    // 手动 \tag 跳过自动编号
    // ===================================================================

    @Test
    fun should_skip_auto_numbering_when_manual_tag_present() {
        val input = "\\begin{equation} E = mc^2 \\tag{Einstein} \\label{eq:tag} \\end{equation}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertTrue(labelMap.isEmpty(), "有手动 \\tag 的环境不应自动分配编号给 label")
    }

    @Test
    fun should_not_affect_counter_when_manual_tag_present() {
        val input = """
            \begin{equation} a = b \label{eq:1} \end{equation}
            \begin{equation} c = d \tag{X} \label{eq:x} \end{equation}
            \begin{equation} e = f \label{eq:2} \end{equation}
        """.trimIndent()
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:1"], "第一个 equation 应编号 1")
        assertNull(labelMap["eq:x"], "有 \\tag 的 equation 中 label 不应有自动编号")
        assertEquals("2", labelMap["eq:2"], "第三个 equation 应编号 2（手动 tag 不消耗编号）")
    }

    // ===================================================================
    // 边界条件
    // ===================================================================

    @Test
    fun should_return_empty_map_for_no_environments() {
        val doc = parser.parse("x^2 + y^2 = z^2")
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertTrue(labelMap.isEmpty(), "没有环境时应返回空映射")
    }

    @Test
    fun should_return_empty_map_for_environment_without_label() {
        val doc = parser.parse("\\begin{equation} E = mc^2 \\end{equation}")
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertTrue(labelMap.isEmpty(), "环境内无 label 时映射应为空")
    }

    @Test
    fun should_not_number_aligned_inner_environment() {
        // aligned 是内嵌环境，不独立编号
        val input = "\\begin{equation} \\begin{aligned} x &= 1 \\\\ y &= 2 \\end{aligned} \\label{eq:1} \\end{equation}"
        val doc = parser.parse(input)
        val labelMap = EquationNumbering.buildLabelMap(doc.children)
        assertEquals("1", labelMap["eq:1"], "外层 equation 应编号 1")
        assertEquals(1, labelMap.size, "应只有一个编号映射")
    }

    @Test
    fun should_handle_numbering_state_counter() {
        val labelMap = mapOf("eq:1" to "1", "eq:2" to "2")
        val state = EquationNumberingState(labelMap)

        assertEquals("1", state.resolveLabel("eq:1"))
        assertEquals("2", state.resolveLabel("eq:2"))
        assertNull(state.resolveLabel("eq:unknown"))

        // 测试计数器
        assertEquals("1", state.nextNumber())
        assertEquals("2", state.nextNumber())
        assertEquals("3", state.nextNumber())
    }
}
