package com.hrm.latex.parser

import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.component.EnvironmentParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyntaxCoverageTest {
    private val cases by lazy { syntaxCoverageCases() }

    @Test
    fun coversEverySupportedCommandSymbolAndEnvironment() {
        assertEquals(cases.size, cases.map { it.id }.toSet().size, "用例 ID 重复")
        val expectedCommands = CommandParser.syntaxCommands() + SymbolMap.getAllSymbols().keys
        val expectedEnvironments = EnvironmentParser.supportedEnvironments()
        assertInventory("命令", expectedCommands, cases.flatMap { it.commands }.toSet())
        assertInventory("环境", expectedEnvironments, cases.mapNotNull { it.environment }.toSet())

        for (case in cases) {
            val tokens = LatexTokenizer(case.source).tokenize()
            val names = tokens.filterIsInstance<LatexToken.Command>().map { it.name }.toSet()
            assertTrue(names.containsAll(case.commands), case.id + " 的输入缺少声明的命令 " + (case.commands - names))
            case.environment?.let { name ->
                assertTrue(tokens.any { it is LatexToken.BeginEnvironment && it.name == name }, case.id)
                assertTrue(tokens.any { it is LatexToken.EndEnvironment && it.name == name }, case.id)
            }
        }
        println("语法覆盖 ${cases.size} 条用例，${expectedCommands.size} 个命令和符号，${expectedEnvironments.size} 个环境")
    }

    @Test
    fun examplesProduceTheExpectedAstAndDiagnostics() {
        val parser = LatexParser()
        forEachCase { case ->
            val result = parser.parseWithDiagnostics(case.source)
            if (case.error == null) {
                assertTrue(result.errors.isEmpty(), result.errors.toString())
                val failed = result.document.fold(false) { found, node ->
                    found || node is LatexNode.Command || node is LatexNode.Error
                }
                assertTrue(!failed, result.document.toString())
            } else {
                assertTrue(result.errors.any { it.category == case.error }, "诊断为 " + result.errors)
            }
            case.verify(result)
        }
    }

    @Test
    fun completeExamplesReplaceThePreviousPreview() {
        val parser = LatexParser()
        forEachCase { case ->
            if (case.error == null) {
                val incremental = IncrementalLatexParser()
                incremental.setInput("a+b")
                incremental.setInput(case.source)
                assertEquals(parser.parse(case.source), incremental.getPreviewDocument())
            }
        }
    }

    @Test
    fun streamingAndEditsPreserveTheAstOfEveryExample() {
        val parser = LatexParser()
        forEachCase { case ->
            val expected = parser.parse(case.source)
            for (chunkSize in listOf(1, 3, 8)) {
                val streamed = IncrementalLatexParser()
                for (chunk in case.source.chunked(chunkSize)) streamed.append(chunk)
                assertEquals(expected, streamed.getCurrentDocument(), "分块大小为 $chunkSize")
            }

            val incremental = IncrementalLatexParser()
            incremental.setInput(case.source)
            val offset = case.source.length / 2
            val edited = case.source.replaceRange(offset, offset, "x")
            incremental.setInput(edited)
            assertEquals(parser.parse(edited), incremental.getCurrentDocument(), "插入后")
            incremental.setInput(case.source)
            assertEquals(expected, incremental.getCurrentDocument(), "恢复后")
        }
    }

    private inline fun forEachCase(run: (SyntaxCoverageCase) -> Unit) {
        val failures = mutableListOf<String>()
        for (case in cases) {
            try {
                run(case)
            } catch (failure: AssertionError) {
                failures.add(case.id + " [" + case.source + "] " + failure.message)
            }
        }
        assertTrue(
            failures.isEmpty(),
            failures.take(35).joinToString("\n", prefix = failures.size.toString() + " 条用例失败\n")
        )
    }

    private fun assertInventory(label: String, expected: Set<String>, covered: Set<String>) {
        val missing = (expected - covered).sorted()
        val obsolete = (covered - expected).sorted()
        assertTrue(missing.isEmpty() && obsolete.isEmpty(), "$label 缺少 $missing，失效 $obsolete")
    }
}
