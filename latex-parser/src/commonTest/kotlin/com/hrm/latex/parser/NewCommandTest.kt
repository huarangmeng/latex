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
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewCommandTest {

    @Test
    fun should_parse_newcommand_without_arguments() {
        val parser = LatexParser()
        val input = "\\newcommand{\\R}{\\mathbb{R}}"
        val result = parser.parse(input)
        
        assertTrue(result.children.isNotEmpty(), "解析结果不应为空")
        val node = result.children[0]
        assertTrue(node is LatexNode.NewCommand, "应该解析为 NewCommand 节点")
        
        node as LatexNode.NewCommand
        assertEquals("R", node.commandName, "命令名应为 R")
        assertEquals(0, node.numArgs, "参数个数应为 0")
        assertTrue(node.definition.isNotEmpty(), "定义不应为空")
    }

    @Test
    fun should_parse_newcommand_with_one_argument() {
        val parser = LatexParser()
        val input = "\\newcommand{\\diff}[1]{\\frac{d}{d#1}}"
        val result = parser.parse(input)
        
        val node = result.children[0] as LatexNode.NewCommand
        assertEquals("diff", node.commandName)
        assertEquals(1, node.numArgs)
    }

    @Test
    fun should_parse_newcommand_with_two_arguments() {
        val parser = LatexParser()
        val input = "\\newcommand{\\pdiff}[2]{\\frac{\\partial #1}{\\partial #2}}"
        val result = parser.parse(input)
        
        val node = result.children[0] as LatexNode.NewCommand
        assertEquals("pdiff", node.commandName)
        assertEquals(2, node.numArgs)
    }

    @Test
    fun should_expand_custom_command_without_arguments() {
        val result = LatexParser().parse("\\newcommand{\\R}{\\mathbb{R}} x \\in \\R")
        assertIs<LatexNode.NewCommand>(result.children.first())
        assertEquals(LatexNode.Style.StyleType.BLACKBOARD_BOLD, assertIs<LatexNode.Style>(result.children.last()).styleType)
    }

    @Test
    fun should_expand_custom_command_with_one_argument() {
        val result = LatexParser().parse("\\newcommand{\\diff}[1]{\\frac{d}{d#1}} \\diff{x}")
        val fraction = assertIs<LatexNode.Fraction>(result.children.last())
        assertTrue(fraction.numerator.children().isNotEmpty())
        assertTrue(fraction.denominator.children().isNotEmpty())
    }

    @Test
    fun should_expand_custom_command_with_two_arguments() {
        val result = LatexParser().parse("\\newcommand{\\pdiff}[2]{\\frac{\\partial #1}{\\partial #2}} \\pdiff{f}{x}")
        val fraction = assertIs<LatexNode.Fraction>(result.children.last())
        assertTrue(fraction.numerator.children().isNotEmpty())
        assertTrue(fraction.denominator.children().isNotEmpty())
    }

    @Test
    fun should_handle_multiple_custom_commands() {
        val parser = LatexParser()
        val input = """
            \newcommand{\N}{\mathbb{N}}
            \newcommand{\Z}{\mathbb{Z}}
            \N + \Z
        """.trimIndent()
        val result = parser.parse(input)
        
        val newCmds = result.children.filterIsInstance<LatexNode.NewCommand>()
        assertTrue(newCmds.any { it.commandName == "N" }, "应该有 N 命令定义")
        assertTrue(newCmds.any { it.commandName == "Z" }, "应该有 Z 命令定义")
        assertEquals(2, newCmds.size)
    }

    @Test
    fun should_handle_nested_custom_commands() {
        val result = LatexParser().parse("\\newcommand{\\abs}[1]{\\left|#1\\right|} \\abs{x}")
        val delimited = assertIs<LatexNode.Delimited>(result.children.last())
        assertEquals("|", delimited.left)
        assertEquals("|", delimited.right)
        assertEquals("x", AccessibilityVisitor.describe(LatexNode.Group(delimited.content)))
    }

    @Test
    fun should_replace_parameter_in_text() {
        val result = LatexParser().parse("\\newcommand{\\test}[1]{a#1b} \\test{x}")
        assertEquals("axb", AccessibilityVisitor.describe(result).replace(" ", ""))
    }
    
    @Test
    fun should_parse_delimited_command_definition() {
        val parser = LatexParser()
        val input = "\\newcommand{\\abs}[1]{\\left|#1\\right|}"
        val result = parser.parse(input)
        
        val newCmd = result.children.filterIsInstance<LatexNode.NewCommand>()
            .first { it.commandName == "abs" }
        
        val commands = newCmd.definition.filterIsInstance<com.hrm.latex.parser.tokenizer.LatexToken.Command>()
        assertEquals(listOf("left", "right"), commands.map { it.name })
        val text = newCmd.definition.filterIsInstance<com.hrm.latex.parser.tokenizer.LatexToken.Text>().joinToString("") { it.content }
        assertEquals("|#1|", text)

    }
    
    @Test
    fun should_expand_delimited_command() {
        val result = LatexParser().parse("\\newcommand{\\abs}[1]{\\left|#1\\right|} \\abs{x}")
        val delimited = assertIs<LatexNode.Delimited>(result.children.last())
        assertEquals("|", delimited.left)
        assertEquals("|", delimited.right)
        assertEquals("x", AccessibilityVisitor.describe(LatexNode.Group(delimited.content)))
    }
    
    @Test
    fun should_parse_custom_command_name() {
        val parser = LatexParser()
        val input = "\\newcommand{\\myvec}[1]{\\boldsymbol{#1}}"
        val result = parser.parse(input)
        
        // 通过 AST 检查是否注册了自定义命令
        val newCmds = result.children.filterIsInstance<LatexNode.NewCommand>()
        assertTrue(newCmds.isNotEmpty(), "应该有自定义命令定义")
        
        // 打印所有命令名用于调试
        val commandNames = newCmds.joinToString(", ") { it.commandName }
        println("定义的自定义命令: $commandNames")
        
        // 检查具体的命令
        assertTrue(newCmds.any { it.commandName == "myvec" }, "应该找到 myvec 命令，实际找到: $commandNames")
    }
    
    @Test
    fun should_override_builtin_command() {
        val result = LatexParser().parse("\\newcommand{\\myvec}[1]{\\boldsymbol{#1}} \\myvec{v}")
        val style = assertIs<LatexNode.Style>(result.children.last())
        assertEquals(LatexNode.Style.StyleType.BOLD_SYMBOL, style.styleType)
        assertEquals("v", AccessibilityVisitor.describe(LatexNode.Group(style.content)))
    }

    @Test
    fun should_expand_binomial_in_custom_command() {
        val result = LatexParser().parse("\\newcommand{\\C}[2]{\\binom{#1}{#2}} \\C{n}{k}")
        val binomial = assertIs<LatexNode.Binomial>(result.children.last())
        assertEquals("n", AccessibilityVisitor.describe(binomial.top).replace(" ", ""))
        assertEquals("k", AccessibilityVisitor.describe(binomial.bottom).replace(" ", ""))
    }

    @Test
    fun should_expand_binomial_with_complex_args() {
        val result = LatexParser().parse("\\newcommand{\\C}[2]{\\binom{#1}{#2}} \\C{n+1}{k-1}")
        val binomial = assertIs<LatexNode.Binomial>(result.children.last())
        assertEquals("n+1", AccessibilityVisitor.describe(binomial.top).replace(" ", ""))
        assertEquals("k-1", AccessibilityVisitor.describe(binomial.bottom).replace(" ", ""))
    }

    @Test
    fun should_handle_lone_hash_in_definition() {
        // regression test: lone # or # not followed by digit should not cause infinite loop
        val parser = LatexParser()
        val input = "\\newcommand{\\test}{a # b} \\test"
        val result = parser.parse(input)

        // should complete without hanging
        assertTrue(result.children.isNotEmpty())
    }

    @Test
    fun should_handle_hash_at_end_of_definition() {
        val parser = LatexParser()
        val input = "\\newcommand{\\test}{text#} \\test"
        val result = parser.parse(input)

        assertTrue(result.children.isNotEmpty())
    }

    @Test
    fun should_handle_hash_followed_by_letter() {
        val parser = LatexParser()
        val input = "\\newcommand{\\test}{#a test} \\test"
        val result = parser.parse(input)

        assertTrue(result.children.isNotEmpty())
    }

    // ===================== \renewcommand =====================

    @Test
    fun should_parse_renewcommand() {
        val parser = LatexParser()
        val result = parser.parse("\\renewcommand{\\R}{\\mathbb{R}} x \\in \\R")
        assertTrue(result.children.size >= 2, "Should have newcommand + expanded usage")
    }

    // ===================== \def =====================

    @Test
    fun should_parse_def_without_args() {
        val parser = LatexParser()
        val result = parser.parse("\\def\\myvar{\\alpha} \\myvar")
        assertTrue(result.children.isNotEmpty())
    }

    // ===================== \newcommand 可选参数默认值 =====================

    @Test
    fun should_parse_newcommand_with_default_arg() {
        val parser = LatexParser()
        // \newcommand{\greet}[1][World]{Hello #1}
        val result = parser.parse("\\newcommand{\\greet}[1][World]{Hello #1} \\greet")
        assertTrue(result.children.isNotEmpty(), "应包含 newcommand 定义 + 展开")

        // 验证 NewCommand 节点的 defaultArg
        val newCmd = result.children.first()
        assertIs<LatexNode.NewCommand>(newCmd)
        assertEquals("greet", newCmd.commandName)
        assertEquals(1, newCmd.numArgs)
        assertEquals("World", newCmd.defaultArg)
    }

    @Test
    fun should_expand_with_default_arg_when_no_optional_provided() {
        val parser = LatexParser()
        // 定义有默认值的命令，使用时不传可选参数
        val result = parser.parse("\\newcommand{\\greet}[1][World]{Hello #1} \\greet{}")
        // 应将 "World" 作为第一个参数展开
        assertTrue(result.children.size >= 2)
    }

    @Test
    fun should_expand_with_explicit_optional_arg() {
        val parser = LatexParser()
        // 定义有默认值的命令，使用时传入可选参数 [LaTeX]
        val result = parser.parse("\\newcommand{\\greet}[1][World]{Hello #1} \\greet[LaTeX]")
        assertTrue(result.children.size >= 2)
    }

    @Test
    fun should_parse_two_arg_command_with_default() {
        val parser = LatexParser()
        // \newcommand{\cmd}[2][default]{#1 and #2}
        val result = parser.parse("\\newcommand{\\cmd}[2][default]{#1 and #2} \\cmd{second}")
        assertTrue(result.children.isNotEmpty())

        val newCmd = result.children.first()
        assertIs<LatexNode.NewCommand>(newCmd)
        assertEquals(2, newCmd.numArgs)
        assertEquals("default", newCmd.defaultArg)
    }

    @Test
    fun should_parse_two_arg_command_with_explicit_optional() {
        val parser = LatexParser()
        // 明确传入可选参数
        val result = parser.parse("\\newcommand{\\cmd}[2][x]{#1 + #2} \\cmd[y]{z}")
        assertTrue(result.children.isNotEmpty())
    }
}
