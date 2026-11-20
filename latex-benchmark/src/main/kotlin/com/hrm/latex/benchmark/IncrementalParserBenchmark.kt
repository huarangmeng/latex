package com.hrm.latex.benchmark

import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.util.LatexPrinter
import com.hrm.latex.parser.visitor.MathMLVisitor
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * 单次编辑对照：每次 invocation 的初始解析放在 Setup，计时只包含编辑后一次解析。
 * 三类编辑均验证最终输入及语义输出一致；宏输入有上下文依赖，允许产品正确地回退全量解析。
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class IncrementalParserBenchmark {
    @Param("medium", "large", "macro")
    lateinit var input: String

    @Param("append", "replace", "delete")
    lateinit var edit: String

    private lateinit var source: String
    private lateinit var edited: String
    private lateinit var parser: LatexParser
    private lateinit var incremental: IncrementalLatexParser
    private val suffix = " + \\alpha^2"

    @Setup(Level.Trial)
    fun setup() {
        source = BenchmarkInputs.formula(input)
        parser = LatexParser()
        BenchmarkInputs.validatedDocument(input, parser)
        edited = when (edit) {
            "append" -> source + suffix
            "replace" -> {
                val index = middleAtomIndex()
                source.replaceRange(index, index + 1, "z")
            }
            "delete" -> {
                if (input == "macro") {
                    // 删除完整宏调用及其右侧加号，避免人为制造空实参或孤立上标。
                    val term = "\\benchterm{x} + "
                    val index = source.indices.filter { source.startsWith(term, it) }
                        .minBy { abs(it - source.length / 2) }
                    source.removeRange(index, index + term.length)
                } else {
                    val index = middleAtomIndex()
                    val length = if (source.startsWith("^2", index + 1)) 3 else 1
                    source.removeRange(index, index + length)
                }
            }
            else -> error("未知编辑类型：$edit")
        }
        val expected = parser.parseWithDiagnostics(edited)
        check(expected.diagnostics.isEmpty()) { "$input/$edit 编辑后存在意外诊断：${expected.diagnostics}" }
        val probe = prebuilt()
        applyEdit(probe)
        check(probe.getCurrentInput() == edited)
        checkEquivalent(expected.document, probe.getCurrentDocument(), "$input/$edit")
    }

    /** 每次恢复同一旧输入，防止重复 invocation 退化为无修改查询或累计追加。 */
    @Setup(Level.Invocation)
    fun restoreInitialDocument() {
        incremental = prebuilt()
    }

    private fun middleAtomIndex(): Int {
        val atom = if (input == "macro") 'x' else 'b'
        return source.indices.filter { source[it] == atom }.minBy { abs(it - source.length / 2) }
    }

    private fun prebuilt(): IncrementalLatexParser = IncrementalLatexParser().also {
        it.setInput(source)
        it.getCurrentDocument()
    }

    private fun applyEdit(target: IncrementalLatexParser) {
        if (edit == "append") target.append(suffix) else target.setInput(edited)
    }

    @Benchmark
    fun incremental(bh: Blackhole) {
        applyEdit(incremental)
        bh.consume(incremental.getCurrentDocument())
    }

    @Benchmark
    fun fullParse(bh: Blackhole) {
        bh.consume(parser.parse(edited))
    }
}

/**
 * 每个 op 是从空输入开始、处理全部分块的一整轮流式解析，包含初始化和输入累积。
 * 两条路径处理相同的分块序列。增量路径读取实时预览，输入补全后的 AST 与整式解析一致。
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class StreamingParserBenchmark {
    @Param("medium", "large")
    lateinit var input: String

    @Param("1", "16")
    @JvmField
    final var chunkSize: Int = 0

    private lateinit var source: String
    private lateinit var chunks: List<String>

    @Setup(Level.Trial)
    fun setup() {
        source = BenchmarkInputs.formula(input)
        val expected = BenchmarkInputs.validatedDocument(input)
        chunks = source.chunked(chunkSize)
        check(chunks.joinToString("") == source)
        val probe = IncrementalLatexParser()
        val full = LatexParser()
        val accumulated = StringBuilder(source.length)
        for (chunk in chunks) {
            probe.append(chunk)
            probe.getPreviewDocument()
            accumulated.append(chunk)
            full.parse(accumulated.toString())
        }
        check(probe.getCurrentInput() == source)
        checkEquivalent(expected, probe.getPreviewDocument(), "$input/chunkSize=$chunkSize")
    }

    @Benchmark
    fun incremental(bh: Blackhole) {
        val parser = IncrementalLatexParser()
        for (chunk in chunks) {
            parser.append(chunk)
            bh.consume(parser.getPreviewDocument())
        }
    }

    @Benchmark
    fun fullParse(bh: Blackhole) {
        val parser = LatexParser()
        val accumulated = StringBuilder(source.length)
        for (chunk in chunks) {
            accumulated.append(chunk)
            bh.consume(parser.parse(accumulated.toString()))
        }
    }
}

/** 比较可见结构和 MathML，忽略增量节点来源位置的差异；验证不计入采样。 */
private fun checkEquivalent(expected: LatexNode.Document, actual: LatexNode.Document, label: String) {
    check(LatexPrinter().print(expected) == LatexPrinter().print(actual)) { "$label 增量 AST 结构与全量解析不一致" }
    check(MathMLVisitor.convert(expected) == MathMLVisitor.convert(actual)) { "$label 增量语义输出与全量解析不一致" }
}
