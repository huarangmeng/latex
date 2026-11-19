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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The RaTeX formula corpus in `test-corpus/ratex` (see its README) through the parser: every
 * formula and every prefix of it parses without throwing, and streaming a formula one character
 * at a time ends like a batch parse. Two baselines under `test-corpus/baselines` record the
 * formulas that do not parse cleanly yet and those that stream differently; each test fails when
 * its baseline is out of date in either direction, and `UPDATE_CORPUS_BASELINE=1` rewrites it.
 */
class CorpusTest {

    @Test
    fun everyFormulaAndEveryPrefixParsesWithoutThrowing() {
        val parser = LatexParser()
        val failures = formulas.flatMap { formula ->
            (1..formula.length).mapNotNull { end ->
                val input = formula.substring(0, end)
                runCatching { parser.parseWithDiagnostics(input) }.exceptionOrNull()?.let { "$it <- $input" }
            }
        }
        assertTrue(failures.isEmpty(), "${failures.size} inputs threw:\n" + failures.take(25).joinToString("\n"))
    }

    @Test
    fun streamingAFormulaEndsLikeABatchParse() {
        val parser = LatexParser()
        val mismatched = formulas.filter { formula ->
            val incremental = IncrementalLatexParser()
            formula.forEach { incremental.append(it.toString()) }
            incremental.getCurrentDocument() != parser.parse(formula)
        }
        checkBaseline(
            "incremental-mismatch.txt", mismatched,
            added = "stream differently from a batch parse", resolved = "stream like a batch parse",
        )
    }

    @Test
    fun formulasThatParseCleanlyKeepParsingCleanly() {
        val parser = LatexParser()
        val unsupported = formulas.filterNot { parsesCleanly(parser, it) }
        println("RaTeX corpus: ${formulas.size - unsupported.size}/${formulas.size} formulas parse cleanly")
        checkBaseline("unsupported.txt", unsupported, added = "fail to parse cleanly", resolved = "parse cleanly")
    }

    /** No error diagnostic, no error node and no unrecognized command (drawn as red `\name`). */
    private fun parsesCleanly(parser: LatexParser, formula: String): Boolean {
        val result = parser.parseWithDiagnostics(formula)
        if (result.errors.isNotEmpty()) return false
        val pending = ArrayDeque<LatexNode>(listOf(result.document))
        while (pending.isNotEmpty()) {
            val node = pending.removeLast()
            if (node is LatexNode.Error || node is LatexNode.Command) return false
            pending.addAll(node.children())
        }
        return true
    }

    private fun checkBaseline(name: String, actual: List<String>, added: String, resolved: String) {
        val file = File(CORPUS, "baselines/$name")
        if (System.getenv("UPDATE_CORPUS_BASELINE") == "1") {
            file.writeText(actual.sorted().joinToString("\n", postfix = "\n"))
            return
        }
        if (!file.exists()) fail("Missing ${file.path}; create it with UPDATE_CORPUS_BASELINE=1")
        val recorded = file.readLines().filter { it.isNotEmpty() }.toSet()
        val newEntries = actual.filterNot { it in recorded }
        val staleEntries = recorded - actual.toSet()
        if (newEntries.isEmpty() && staleEntries.isEmpty()) return
        fail(buildString {
            appendLine("baselines/$name is out of date; if intended, rerun with UPDATE_CORPUS_BASELINE=1.")
            if (newEntries.isNotEmpty()) {
                appendLine("${newEntries.size} formulas now $added:\n" + newEntries.joinToString("\n"))
            }
            if (staleEntries.isNotEmpty()) {
                appendLine("${staleEntries.size} recorded formulas now $resolved:\n" + staleEntries.joinToString("\n"))
            }
        })
    }

    private companion object {
        val FILES = listOf("main.txt", "parser.txt", "mhchem.txt", "centernot.txt", "prooftree.txt", "slashed.txt")

        val CORPUS: File by lazy {
            generateSequence(File("").absoluteFile) { it.parentFile }
                .map { File(it, "test-corpus") }
                .first { it.isDirectory }
        }

        val formulas: List<String> by lazy {
            FILES.flatMap { name ->
                File(CORPUS, "ratex/$name").readLines().filter {
                    val line = it.trim()
                    line.isNotEmpty() && line[0] != '#'
                }
            }.distinct()
        }
    }
}
