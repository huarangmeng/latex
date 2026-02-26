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
        val parser = LatexParser()
        val input = "\\newcommand{\\R}{\\mathbb{R}} x \\in \\R"
        val result = parser.parse(input)
        
        // 验证命令被注册
        assertTrue(parser.customCommands.containsKey("R"))
        
        // 验证展开结果包含 mathbb{R}
        val lastNode = result.children.last()
        assertTrue(lastNode is LatexNode.Group, "自定义命令应展开为 Group")
    }

    @Test
    fun should_expand_custom_command_with_one_argument() {
        val parser = LatexParser()
        val input = "\\newcommand{\\diff}[1]{\\frac{d}{d#1}} \\diff{x}"
        val result = parser.parse(input)
        
        assertTrue(parser.customCommands.containsKey("diff"))
        
        // 最后一个节点应该是展开的分数
        val lastNode = result.children.last()
        assertTrue(lastNode is LatexNode.Group)
        val group = lastNode as LatexNode.Group
        assertTrue(group.children.isNotEmpty())
        // 展开后应包含 Fraction 节点
        val frac = group.children.find { it is LatexNode.Fraction }
        assertTrue(frac != null, "展开后应包含 Fraction")
    }

    @Test
    fun should_expand_custom_command_with_two_arguments() {
        val parser = LatexParser()
        val input = "\\newcommand{\\pdiff}[2]{\\frac{\\partial #1}{\\partial #2}} \\pdiff{f}{x}"
        val result = parser.parse(input)
        
        assertTrue(parser.customCommands.containsKey("pdiff"))
        
        val lastNode = result.children.last()
        assertTrue(lastNode is LatexNode.Group)
        val group = lastNode as LatexNode.Group
        val frac = group.children.find { it is LatexNode.Fraction }
        assertTrue(frac != null, "展开后应包含 Fraction")
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
        
        assertTrue(parser.customCommands.containsKey("N"))
        assertTrue(parser.customCommands.containsKey("Z"))
        assertEquals(2, parser.customCommands.size)
    }

    @Test
    fun should_handle_nested_custom_commands() {
        val parser = LatexParser()
        val input = "\\newcommand{\\abs}[1]{\\left|#1\\right|} \\abs{x}"
        val result = parser.parse(input)
        
        val lastNode = result.children.last()
        assertTrue(lastNode is LatexNode.Group)
        val group = lastNode as LatexNode.Group
        // 应包含 Delimited 节点（left/right）
        val delimited = group.children.find { it is LatexNode.Delimited }
        assertTrue(delimited != null, "展开后应包含 Delimited")
    }

    @Test
    fun should_replace_parameter_in_text() {
        val parser = LatexParser()
        val input = "\\newcommand{\\test}[1]{a#1b} \\test{x}"
        val result = parser.parse(input)
        
        val lastNode = result.children.last() as LatexNode.Group
        // 展开后应该包含 a, x, b
        assertTrue(lastNode.children.size >= 3)
    }
    
    @Test
    fun should_parse_delimited_command_definition() {
        val parser = LatexParser()
        val input = "\\newcommand{\\abs}[1]{\\left|#1\\right|}"
        val result = parser.parse(input)
        
        assertTrue(parser.customCommands.containsKey("abs"))
        val customCmd = parser.customCommands["abs"]!!
        
        // 检查定义内容
        assertEquals(1, customCmd.definition.size, "定义应包含一个节点（Delimited）")
        val defNode = customCmd.definition[0]
        assertTrue(defNode is LatexNode.Delimited, "定义应该是 Delimited 节点")
        
        val delimited = defNode as LatexNode.Delimited
        assertEquals("|", delimited.left, "左分隔符应该是 |")
        assertEquals("|", delimited.right, "右分隔符应该是 |")
        
        // 内容应该是 Text 节点 "#1"
        assertTrue(delimited.content.isNotEmpty(), "内容不应为空")
        val content = delimited.content.first()
        assertTrue(content is LatexNode.Text, "内容应该是 Text 节点")
        assertEquals("#1", (content as LatexNode.Text).content, "内容应该是 '#1'")
    }
    
    @Test
    fun should_expand_delimited_command() {
        val parser = LatexParser()
        val input = "\\newcommand{\\abs}[1]{\\left|#1\\right|} \\abs{x}"
        val result = parser.parse(input)
        
        assertTrue(parser.customCommands.containsKey("abs"))
        
        val lastNode = result.children.last()
        assertTrue(lastNode is LatexNode.Group, "自定义命令应展开为 Group")
        
        val group = lastNode as LatexNode.Group
        // 应包含 Delimited 节点（left/right）
        val delimited = group.children.find { it is LatexNode.Delimited }
        assertTrue(delimited != null, "展开后应包含 Delimited")
        
        // 验证 Delimited 节点的内容
        val delim = delimited as LatexNode.Delimited
        assertEquals("|", delim.left, "左分隔符应该是 |")
        assertEquals("|", delim.right, "右分隔符应该是 |")
        
        // Delimited.content 是 List<LatexNode>
        assertTrue(delim.content.isNotEmpty(), "内容不应为空")
        val firstChild = delim.content.first()
        
        // 内容可能是 Text 或 Group，取决于替换逻辑
        when (firstChild) {
            is LatexNode.Text -> {
                assertEquals("x", firstChild.content, "内容应该是 'x' 而不是 '#1'")
            }
            is LatexNode.Group -> {
                // 如果是 Group，检查第一个子节点
                assertTrue(firstChild.children.isNotEmpty(), "Group 不应为空")
                val subChild = firstChild.children.first()
                assertTrue(subChild is LatexNode.Text, "Group 的子节点应该是 Text")
                assertEquals("x", (subChild as LatexNode.Text).content, "内容应该是 'x' 而不是 '#1'")
            }
            else -> {
                assertTrue(false, "未预期的内容类型: ${firstChild::class.simpleName}")
            }
        }
    }
    
    @Test
    fun should_parse_custom_command_name() {
        val parser = LatexParser()
        val input = "\\newcommand{\\myvec}[1]{\\boldsymbol{#1}}"
        val result = parser.parse(input)
        
        // 简单检查是否注册了任何自定义命令
        assertTrue(parser.customCommands.isNotEmpty(), "应该有自定义命令注册，实际有 ${parser.customCommands.size} 个")
        
        // 打印所有注册的命令名用于调试
        val commandNames = parser.customCommands.keys.joinToString(", ")
        println("注册的自定义命令: $commandNames")
        
        // 检查具体的命令
        assertTrue(parser.customCommands.containsKey("myvec"), "应该找到 myvec 命令，实际找到: $commandNames")
    }
    
    @Test
    fun should_override_builtin_command() {
        val parser = LatexParser()
        val input = "\\newcommand{\\myvec}[1]{\\boldsymbol{#1}} \\myvec{v}"
        val result = parser.parse(input)
        
        assertTrue(parser.customCommands.containsKey("myvec"), "应该注册自定义的 myvec 命令")
        
        // 应该有至少两个节点：NewCommand 定义和展开的 Group（可能还有空格）
        assertTrue(result.children.size >= 2, "应该至少有两个节点")
        
        // 找到最后一个非 NewCommand 节点
        val expandedNode = result.children.last { it !is LatexNode.NewCommand }
        assertTrue(expandedNode is LatexNode.Group, "最后一个非NewCommand节点应该是 Group")
        
        // Group 中应该包含 Style 节点
        val group = expandedNode as LatexNode.Group
        val styleNode = group.children.find { it is LatexNode.Style }
        assertTrue(styleNode != null, "应该包含 Style 节点")
        
        val style = styleNode as LatexNode.Style
        assertEquals(LatexNode.Style.StyleType.BOLD_SYMBOL, style.styleType, "应该是 BOLD_SYMBOL 样式")
    }

    @Test
    fun should_expand_binomial_in_custom_command() {
        val parser = LatexParser()
        val input = "\\newcommand{\\mychoose}[2]{\\binom{#1}{#2}} \\mychoose{n}{k}"
        val result = parser.parse(input)

        assertTrue(parser.customCommands.containsKey("mychoose"))

        val lastNode = result.children.last()
        assertTrue(lastNode is LatexNode.Group, "custom command should expand to Group")

        val group = lastNode as LatexNode.Group
        val binomial = group.children.find { it is LatexNode.Binomial }
        assertTrue(binomial != null, "expanded result should contain Binomial")

        val binom = binomial as LatexNode.Binomial
        // verify parameters were replaced (not #1 and #2)
        fun extractText(node: LatexNode): String = when (node) {
            is LatexNode.Text -> node.content
            is LatexNode.Group -> node.children.joinToString("") { extractText(it) }
            else -> ""
        }
        val topText = extractText(binom.top)
        val bottomText = extractText(binom.bottom)
        assertEquals("n", topText, "top should be 'n' not '#1'")
        assertEquals("k", bottomText, "bottom should be 'k' not '#2'")
    }

    @Test
    fun should_expand_binomial_with_complex_args() {
        val parser = LatexParser()
        val input = "\\newcommand{\\C}[2]{\\binom{#1}{#2}} \\C{n+1}{k-1}"
        val result = parser.parse(input)

        val lastNode = result.children.last() as LatexNode.Group
        val binomial = lastNode.children.find { it is LatexNode.Binomial }
        assertTrue(binomial != null, "should contain Binomial after expansion")
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
}
