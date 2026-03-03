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

class DeclareMathOperatorTest {

    private val parser = LatexParser()

    @Test
    fun should_parse_basic_declare_math_operator() {
        val result = parser.parse("\\DeclareMathOperator{\\Tr}{Tr} \\Tr")
        val children = result.children
        // First child: NewCommand (not rendered)
        assertIs<LatexNode.NewCommand>(children[0], "First node should be NewCommand")
        assertEquals("Tr", (children[0] as LatexNode.NewCommand).commandName)
        // Find the expanded OperatorName
        val opNodes = children.filterIsInstance<LatexNode.Group>()
            .flatMap { it.children }
            .filterIsInstance<LatexNode.OperatorName>()
        assertTrue(opNodes.isNotEmpty(), "Should have an OperatorName node from expansion")
        assertEquals("Tr", opNodes[0].name)
    }

    @Test
    fun should_expand_declared_operator_as_operatorname() {
        val result = parser.parse("\\DeclareMathOperator{\\sgn}{sgn} \\sgn(x)")
        val children = result.children
        // First: NewCommand
        assertIs<LatexNode.NewCommand>(children[0])
        // Find OperatorName in expanded nodes
        val allNodes = flattenNodes(children)
        val opNames = allNodes.filterIsInstance<LatexNode.OperatorName>()
        assertTrue(opNames.isNotEmpty(), "Should contain OperatorName node")
        assertEquals("sgn", opNames[0].name)
    }

    @Test
    fun should_declare_multiple_operators() {
        val result = parser.parse("\\DeclareMathOperator{\\Tr}{Tr} \\DeclareMathOperator{\\rank}{rank} \\Tr(A) + \\rank(B)")
        val newCmds = result.children.filterIsInstance<LatexNode.NewCommand>()
        assertEquals(2, newCmds.size, "Should have 2 NewCommand nodes")
        assertEquals("Tr", newCmds[0].commandName)
        assertEquals("rank", newCmds[1].commandName)

        val allNodes = flattenNodes(result.children)
        val opNames = allNodes.filterIsInstance<LatexNode.OperatorName>()
        assertTrue(opNames.size >= 2, "Should have at least 2 OperatorName nodes from expansion")
    }

    @Test
    fun should_register_zero_arg_command() {
        val result = parser.parse("\\DeclareMathOperator{\\diag}{diag}")
        val newCmd = result.children.filterIsInstance<LatexNode.NewCommand>().first()
        assertEquals(0, newCmd.numArgs, "DeclareMathOperator should register 0-arg command")
        assertEquals("diag", newCmd.commandName)
    }

    @Test
    fun should_declare_and_use_with_subscript() {
        val result = parser.parse("\\DeclareMathOperator{\\argmin}{arg\\,min} \\argmin_{x}")
        val allNodes = flattenNodes(result.children)
        // Should have a subscript containing the expanded command
        val subscripts = allNodes.filterIsInstance<LatexNode.Subscript>()
        assertTrue(subscripts.isNotEmpty() || allNodes.any { it is LatexNode.OperatorName },
            "Should have subscript or OperatorName")
    }

    private fun flattenNodes(nodes: List<LatexNode>): List<LatexNode> {
        val result = mutableListOf<LatexNode>()
        for (node in nodes) {
            result.add(node)
            when (node) {
                is LatexNode.Group -> result.addAll(flattenNodes(node.children))
                is LatexNode.Superscript -> {
                    result.addAll(flattenNodes(listOf(node.base)))
                    result.addAll(flattenNodes(listOf(node.exponent)))
                }
                is LatexNode.Subscript -> {
                    result.addAll(flattenNodes(listOf(node.base)))
                    result.addAll(flattenNodes(listOf(node.index)))
                }
                is LatexNode.NewCommand -> result.addAll(flattenNodes(node.definition))
                else -> {}
            }
        }
        return result
    }
}
