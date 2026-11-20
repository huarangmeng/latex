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
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import com.hrm.latex.parser.visitor.MathMLVisitor
import java.io.File
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

internal val testCorpusDirectory: File by lazy {
    generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "test-corpus") }
        .first { it.isDirectory }
}

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
    fun everyStreamingPrefixMatchesBatchTokenizationAndParsing() {
        val failures = mutableListOf<String>()
        var checked = 0
        for (formula in stabilityFormulas()) {
            val parser = IncrementalLatexParser()
            val tokenizer = IncrementalTokenizer()
            for (index in formula.indices) {
                val chunk = formula.substring(index, index + 1)
                val end = tokenizer.appendText(chunk)
                val source = tokenizer.getCurrentText()
                val tokens = tokenizer.update(source, TextEdit.fromAppend(index, end))
                parser.append(chunk)
                checked++
                val mismatch = compareWithBatch(source, tokens, parser)
                if (mismatch != null) {
                    failures.add("append($index) $mismatch in ${formula.quote()}")
                    break
                }
            }
        }
        println("流式中间状态检查 $checked 次")
        assertAuditPassed(failures)
    }

    @Test
    fun seededEditSequencesMatchBatchTokenizationAndParsing() {
        val failures = mutableListOf<String>()
        val random = Random(817)
        val insertions = listOf(
            "", "x", "{}", "\\", "\\left(", "\\right)", "\\verb|", """\href{a%20b}""",
            "$", "%r", "\r", "\n", "\r\n", "&", "_", "^", "'", "α", "中", "😀", "\uD800",
        )
        var checked = 0
        for (formula in stabilityFormulas()) {
            val parser = IncrementalLatexParser()
            val tokenizer = IncrementalTokenizer()
            var previous = formula
            parser.setInput(previous)
            tokenizer.tokenize(previous)
            repeat(32) { step ->
                val start = random.nextInt(previous.length + 1)
                val end = random.nextInt(start, minOf(start + 6, previous.length) + 1)
                // 定期还原完整公式，保留对词法边界和已有语法的反复编辑。
                val source = when (step % 8) {
                    0 -> formula
                    1 -> if (start == previous.length) previous + "x" else previous.replaceRange(start, start + 1, "x")
                    2 -> previous.removeRange(start, end)
                    3 -> previous.replaceRange(start, start, insertions.random(random))
                    else -> previous.replaceRange(start, end, insertions.random(random))
                }
                val tokens = tokenizer.update(source, TextEdit.diff(previous, source))
                parser.setInput(source)
                checked++
                val mismatch = compareWithBatch(source, tokens, parser)
                if (mismatch != null && failures.size < 40) {
                    failures.add("edit($step) ${previous.quote()} → $mismatch")
                }
                previous = source
            }
        }
        println("确定性编辑状态检查 $checked 次")
        assertAuditPassed(failures)
    }

    @Test
    fun corpusMathMlIsWellFormedXml() {
        val parser = LatexParser()
        val xml = XMLInputFactory.newFactory().apply {
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
        }
        val sources = stabilityFormulas() + listOf(
            "😀\uD800\u0000\\text{🙂\uDC00}",
            "\\text{标题 😀}\r\n\\href{a%20b}{🙂}",
        )
        for (formula in sources) {
            try {
                val reader = xml.createXMLStreamReader(StringReader(MathMLVisitor.convert(parser.parse(formula))))
                try {
                    while (reader.hasNext()) reader.next()
                } finally {
                    reader.close()
                }
            } catch (error: XMLStreamException) {
                throw AssertionError("MathML 导出无法作为 XML 读取 @ ${formula.quote()}", error)
            }
        }
    }

    private fun compareWithBatch(
        source: String,
        tokens: List<LatexToken>,
        parser: IncrementalLatexParser,
    ): String? {
        val expectedTokens = LatexTokenizer(source).tokenize()
        if (expectedTokens != tokens) {
            val index = expectedTokens.indices.firstOrNull { expectedTokens[it] != tokens.getOrNull(it) } ?: expectedTokens.size
            return "token[$index] ${expectedTokens.getOrNull(index)} != ${tokens.getOrNull(index)} @ ${source.quote()}"
        }
        val session = ParseSession(expectedTokens, source.length, source)
        val expected = session.parse()
        val actual = parser.getCurrentDocument()
        if (expected != actual) {
            val index = expected.children.indices.firstOrNull { expected.children[it] != actual.children.getOrNull(it) }
                ?: expected.children.size
            return "AST[$index] ${expected.children.getOrNull(index)?.toString()?.take(440)} != " +
                "${actual.children.getOrNull(index)?.toString()?.take(440)} @ ${source.quote()}"
        }
        val badRange = actual.fold<LatexNode?>(null) { found, node ->
            found ?: node.takeIf { it.sourceRange?.isWithin(source.length) == false }
        }
        if (badRange != null) return "${badRange::class.simpleName} 的范围 ${badRange.sourceRange} @ ${source.quote()}"
        val badDiagnostic = session.diagnostics.firstOrNull { !it.range.isWithin(source.length) }
        if (badDiagnostic != null) return "诊断范围无效 $badDiagnostic @ ${source.quote()}"
        return null
    }

    private fun SourceRange.isWithin(length: Int): Boolean = start in 0..end && end <= length

    private fun stabilityFormulas() = (formulas + syntaxCoverageCases().map { it.source }).distinct()

    private fun String.quote() = replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")

    private fun assertAuditPassed(failures: List<String>) {
        assertTrue(failures.isEmpty(), failures.take(25).joinToString("\n"))
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
        val file = testCorpusDirectory.resolve("baselines/$name")
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

        val formulas: List<String> by lazy {
            FILES.flatMap { name ->
                testCorpusDirectory.resolve("ratex/$name").readLines().filter {
                    val line = it.trim()
                    line.isNotEmpty() && line[0] != '#'
                }
            }.distinct()
        }

        val expectedDiagnostics: Map<String, ParseDiagnostic.Category> by lazy {
            val rows = testCorpusDirectory.resolve("baselines/expected-diagnostics.tsv").readLines().filter { it.isNotBlank() }
            rows.associate { row ->
                val (category, formula) = row.split('\t', limit = 2)
                formula to ParseDiagnostic.Category.valueOf(category)
            }.also { require(it.size == rows.size) { "诊断基线包含重复公式" } }
        }
    }
}
