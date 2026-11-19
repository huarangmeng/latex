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

import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Nesting limits that keep the parser, and anything recursing over its tree, off the end of the stack. */
class NestingDepthTest {

    private fun wrap(open: String, close: String, depth: Int) = open.repeat(depth) + "x" + close.repeat(depth)

    /** Enough nesting to overflow a default thread stack without a limit. */
    private val overflowing = listOf(
        wrap("{", "}", 10_000),
        wrap("\\sqrt{", "}", 10_000),
        wrap("\\frac{", "}{y}", 10_000),
        wrap("x^{", "}", 10_000),
        wrap("\\left(", "\\right)", 10_000),
        wrap("\\begin{matrix}", "\\end{matrix}", 10_000),
        wrap("\\sqrt", "", 10_000),
        "x" + "_1".repeat(10_000),
        "x" + "_1".repeat(100_000) + "^2",
        "\\ce{H" + "_1".repeat(10_000) + "}",
        "\\ce{A" + "^+".repeat(10_000) + "}",
        wrap("{\\rm\\color{red}\\displaystyle\\large ", "}", 10_000),
        // Commands that convert their argument while parsing, before the final depth check.
        "\\text{\\ce{H" + "_1".repeat(10_000) + "}}",
        "\\SI{\\ce{H" + "_1".repeat(10_000) + "}}{m}",
    )

    @Test
    fun should_reject_overflowing_nesting_and_keep_the_source() {
        for (input in overflowing) {
            val result = LatexParser().parseWithDiagnostics(input)
            val error = assertIs<LatexNode.Error>(result.document.children.single(), input.take(40))
            assertEquals(listOf(LatexNode.TextMode(input, sourceRange = SourceRange(0, input.length))), error.recovered)
            assertTrue(result.errors.any { it.message.startsWith("Nesting deeper") }, input.take(40))
        }
    }

    @Test
    fun should_reject_a_chemical_chain_before_any_command_walks_it() {
        val chain = "\\ce{H" + "_1".repeat(10_000) + "}"
        for (command in CommandParser.registeredCommands()) {
            LatexParser().parse("\\$command{$chain}{$chain}")
        }
    }

    @Test
    fun should_reject_the_whole_formula_when_an_edit_nests_too_deep() {
        val deep = wrap("{", "}", 130)
        val source = "a+b + $deep"
        val streamed = IncrementalLatexParser().apply {
            append("a+b")
            append(" + $deep")
        }.getCurrentDocument()
        val edited = IncrementalLatexParser().apply {
            append("a+b + c")
            setInput(source)
        }.getCurrentDocument()
        for (document in listOf(streamed, edited)) {
            val error = assertIs<LatexNode.Error>(document.children.single())
            assertEquals(SourceRange(0, source.length), error.sourceRange)
            assertEquals(listOf(LatexNode.TextMode(source, sourceRange = SourceRange(0, source.length))), error.recovered)
            assertEquals(LatexParser().parse(source), document)
        }
    }

    @Test
    fun should_reject_overflowing_nesting_when_streamed() {
        val input = wrap("\\sqrt{", "}", 300)
        val parser = IncrementalLatexParser()
        input.chunked(7).forEach { parser.append(it) }
        assertIs<LatexNode.Error>(parser.getCurrentDocument().children.single())
    }

    @Test
    fun should_accept_nesting_up_to_the_limit() {
        // The benchmark's deep input.
        assertTrue(LatexParser().parseWithDiagnostics(wrap("{", "}", 96)).errors.isEmpty())
        assertTrue(LatexParser().parseWithDiagnostics(wrap("{", "}", 126)).errors.isEmpty())
        assertTrue(LatexParser().parseWithDiagnostics(wrap("{", "}", 127)).errors.isNotEmpty())
        for (input in listOf(wrap("\\frac{", "}{y}", 32), wrap("x^{", "}", 32), wrap("\\sqrt", "", 32))) {
            assertTrue(LatexParser().parseWithDiagnostics(input).errors.isEmpty(), input)
        }
    }

    /** The parse skips the exact depth walk while [ParseSession.deepestLevel] says the tree is shallow. */
    @Test
    fun should_bound_the_tree_depth_while_parsing() {
        val declarations = listOf("", "\\rm\\color{red}\\displaystyle\\large ", "\\bf\\it\\sf\\tt\\color{red}\\Huge\\em ")
        val opens = listOf("{%", "\\sqrt{%", "x^{%", "x_{%", "\\begin{matrix}%", "\\frac{%", "\\left(%", "{a\\over %", "\\overbrace{%")
        val closes = listOf("}", "}", "}", "}", "\\end{matrix}", "}{y}", "\\right)", "}", "}^{n}")
        for (depth in 1..60) {
            for (chain in listOf("\\ce{H" + "_1".repeat(depth) + "}", "\\ce{A" + "^+_2".repeat(depth) + "}")) {
                assertDepthBound(chain)
            }
        }
        for (depth in 1..60) for (declaration in declarations) for (i in opens.indices) {
            assertDepthBound(wrap(opens[i].replace("%", declaration), closes[i], depth) + "_1_2^3")
        }
    }

    /** A command added without charging the levels it builds fails here. */
    @Test
    fun should_bound_the_tree_depth_for_every_registered_command() {
        val commands = CommandParser.registeredCommands().sorted()
        assertTrue(commands.size > 100)
        for (command in commands) for (depth in listOf(1, 20, 70)) {
            for (tail in listOf("", "_1^2", "'_1_2^3")) {
                assertDepthBound(wrap("\\$command{", "}{y}{z}$tail", depth))
                assertDepthBound(wrap("\\$command ", tail, depth))
                assertDepthBound(wrap("{\\$command ", "}$tail", depth))
            }
        }
    }

    @Test
    fun should_bound_the_tree_depth_for_random_mixes() {
        val parts = listOf(
            "{" to "}", "\\sqrt{" to "}", "\\sqrt" to "", "\\frac{" to "}{y}", "x^{" to "}", "x_{" to "}_1",
            "{a\\over " to "}", "\\left(" to "\\right)", "\\overbrace{" to "}^{n}", "\\underbrace{" to "}_{n}",
            "\\begin{pmatrix}" to "\\end{pmatrix}^2_1", "\\begin{cases}" to "\\end{cases}",
            "\\begin{aligned}" to "\\end{aligned}", "\\begin{array}{c}" to "\\end{array}",
            "{\\rm\\color{red}" to "}", "{\\displaystyle\\large " to "}", "\\hat{" to "}'",
            "\\text{a$" to "$}", "\\ce{H_2" to "^+}", "\\boxed{" to "}", "\\color{red}{" to "}",
            "{\\bf\\it\\sf\\tt\\color{red}\\Huge\\em " to "}", "{\\rm a\\over\\bf " to "}", "a\\over " to "",
            "{a\\atop{" to "}}", "{\\it " to "\\over b}", "x^{a\\over " to "}", "{a\\over b}^{" to "}",
            "\\frac{a\\over " to "}{y}", "\\sqrt{a\\choose " to "}",
        )
        val declarations = listOf("", "", "\\rm\\color{red}\\displaystyle\\large ", "\\bf\\it\\sf\\tt\\color{red}\\Huge\\em ")
        val random = Random(1)
        repeat(20_000) {
            val chosen = List(random.nextInt(1, 50)) {
                val (open, close) = parts[random.nextInt(parts.size)]
                open + declarations.random(random) to close
            }
            val tail = listOf("", "_1", "^2_1", "'''").random(random)
            assertDepthBound(chosen.joinToString("") { it.first } + "x" + chosen.reversed().joinToString("") { it.second + tail })
        }
    }

    private fun assertDepthBound(input: String) {
        val session = ParseSession(LatexTokenizer(input).tokenize(), input.length, input)
        val document = session.parse()
        if (document.children.singleOrNull() is LatexNode.Error) return
        val depth = depth(document)
        assertTrue(
            depth <= session.deepestLevel + ParseSession.UNCOUNTED_LEVELS,
            "depth $depth > ${session.deepestLevel} + ${ParseSession.UNCOUNTED_LEVELS}: ${input.take(200)}",
        )
        assertTrue(depth <= ParseSession.MAX_TREE_DEPTH, "accepted depth $depth: ${input.take(200)}")
    }

    private fun depth(root: LatexNode): Int {
        var deepest = 0
        val pending = ArrayDeque(listOf(root to 1))
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            deepest = maxOf(deepest, depth)
            node.children().forEach { pending.addLast(it to depth + 1) }
        }
        return deepest
    }
}
