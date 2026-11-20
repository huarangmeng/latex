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

import com.hrm.latex.parser.incremental.IncrementalTokenizer
import com.hrm.latex.parser.incremental.TextEdit
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/** 用 RaTeX 语料检查容错、不同切块方式的等价性和解析支持范围。 */
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
            val expected = parser.parse(formula)
            val expectedTokens = LatexTokenizer(formula).tokenize()
            listOf(1, 2, 3, 5, 8, 16, formula.length).distinct().any { size ->
                val incremental = IncrementalLatexParser()
                val tokenizer = IncrementalTokenizer()
                for (chunk in formula.chunked(size)) {
                    incremental.append(chunk)
                    val previous = tokenizer.getTextLength()
                    val length = tokenizer.appendText(chunk)
                    tokenizer.update(tokenizer.getCurrentText(), TextEdit.fromAppend(previous, length))
                }
                expectedTokens != tokenizer.getCurrentTokens() || expected != incremental.getCurrentDocument()
            }
        }
        assertTrue(
            mismatched.isEmpty(),
            "${mismatched.size} 条公式的流式分词或 AST 与整式解析不同\n" + mismatched.take(25).joinToString("\n"),
        )
    }

    @Test
    fun editingAFormulaMatchesBatchTokenizationAndParsing() {
        val parser = LatexParser()
        val failures = mutableListOf<String>()
        for (formula in formulas) {
            val incremental = IncrementalLatexParser()
            val tokenizer = IncrementalTokenizer()
            incremental.setInput(formula)
            tokenizer.tokenize(formula)
            val offset = formula.length / 2
            var previous = formula
            for (edited in listOf(
                formula.replaceRange(offset, offset, "x"),
                formula.replaceRange(offset, offset + 1, "%a\n"),
                formula.removeRange(offset, offset + 1),
                formula,
            )) {
                val actualTokens = tokenizer.update(edited, TextEdit.diff(previous, edited))
                incremental.setInput(edited)
                if (LatexTokenizer(edited).tokenize() != actualTokens || parser.parse(edited) != incremental.getCurrentDocument()) {
                    failures.add("$previous → $edited")
                }
                previous = edited
            }
        }
        assertTrue(failures.isEmpty(), "${failures.size} 条编辑结果与整式解析不同\n" + failures.take(25).joinToString("\n"))
    }

    @Test
    fun formulasThatParseCleanlyKeepParsingCleanly() {
        val parser = LatexParser()
        val unsupported = formulas.filter { it !in expectedDiagnostics && !parsesCleanly(parser, it) }
        println("RaTeX corpus: ${formulas.size - unsupported.size - expectedDiagnostics.size}/${formulas.size} formulas parse cleanly")
        checkBaseline("unsupported.txt", unsupported, added = "fail to parse cleanly", resolved = "parse cleanly")
    }

    @Test
    fun expectedErrorsKeepTheirDiagnostics() {
        val parser = LatexParser()
        for ((formula, category) in expectedDiagnostics) {
            assertTrue(formula in formulas, "诊断基线包含未知语料 $formula")
            val errors = parser.parseWithDiagnostics(formula).errors
            assertTrue(errors.any { it.category == category }, "$formula 缺少 $category 诊断，实际为 $errors")
        }
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
            file.writeText(actual.sorted().joinToString("\n", postfix = if (actual.isEmpty()) "" else "\n"))
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

        val expectedDiagnostics: Map<String, ParseDiagnostic.Category> by lazy {
            val rows = File(CORPUS, "baselines/expected-diagnostics.tsv").readLines().filter { it.isNotBlank() }
            rows.associate { row ->
                val (category, formula) = row.split('\t', limit = 2)
                formula to ParseDiagnostic.Category.valueOf(category)
            }.also { require(it.size == rows.size) { "诊断基线包含重复公式" } }
        }
    }
}
