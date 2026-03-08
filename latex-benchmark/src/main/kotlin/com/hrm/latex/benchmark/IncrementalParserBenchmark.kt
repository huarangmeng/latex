package com.hrm.latex.benchmark

import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.LatexParser
import kotlinx.benchmark.*

/**
 * 增量解析 vs 全量解析 性能对比 Benchmark。
 *
 * 设计原则：每组对比包含一个增量用例和一个等效全量用例，
 * 确保对比公平（相同输入、相同输出）。
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class IncrementalParserBenchmark {

    private lateinit var parser: LatexParser

    // ── 预拼接的完整字符串 ──
    private lateinit var mediumAppended: String
    private lateinit var largeAppended: String
    private lateinit var complexAppended: String
    private lateinit var mediumModified: String
    private lateinit var largeModified: String

    private val appendSuffix = " + \\alpha^2"
    private val modifySuffix = " + \\gamma"

    @Setup
    fun setup() {
        parser = LatexParser()

        mediumAppended = BenchmarkInputs.MEDIUM + appendSuffix
        largeAppended = BenchmarkInputs.LARGE + appendSuffix
        complexAppended = BenchmarkInputs.COMPLEX + appendSuffix
        mediumModified = BenchmarkInputs.MEDIUM.dropLast(1) + modifySuffix + "}"
        largeModified = BenchmarkInputs.LARGE.dropLast(1) + modifySuffix + "}"
    }

    /** 创建已完成首次解析的增量解析器 */
    private fun prebuilt(input: String): IncrementalLatexParser {
        return IncrementalLatexParser().also {
            it.setInput(input)
            it.getCurrentDocument()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 1: 追加场景 — 在已解析公式末尾追加少量内容
    //   增量：复用旧 AST 前缀，只重解析尾部
    //   全量：从头解析完整字符串
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun append_medium_incremental(bh: Blackhole) {
        val inc = prebuilt(BenchmarkInputs.MEDIUM)
        inc.append(appendSuffix)
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun append_medium_fullParse(bh: Blackhole) {
        bh.consume(parser.parse(mediumAppended))
    }

    @Benchmark
    fun append_large_incremental(bh: Blackhole) {
        val inc = prebuilt(BenchmarkInputs.LARGE)
        inc.append(appendSuffix)
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun append_large_fullParse(bh: Blackhole) {
        bh.consume(parser.parse(largeAppended))
    }

    @Benchmark
    fun append_complex_incremental(bh: Blackhole) {
        val inc = prebuilt(BenchmarkInputs.COMPLEX)
        inc.append(appendSuffix)
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun append_complex_fullParse(bh: Blackhole) {
        bh.consume(parser.parse(complexAppended))
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 2: setInput 替换场景 — 在已解析基础上替换为修改版
    //   增量：diff + 增量分词 + 条件增量AST
    //   全量：从头解析修改后的字符串
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun setInput_medium_incremental(bh: Blackhole) {
        val inc = prebuilt(BenchmarkInputs.MEDIUM)
        inc.setInput(mediumModified)
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun setInput_medium_fullParse(bh: Blackhole) {
        bh.consume(parser.parse(mediumModified))
    }

    @Benchmark
    fun setInput_large_incremental(bh: Blackhole) {
        val inc = prebuilt(BenchmarkInputs.LARGE)
        inc.setInput(largeModified)
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun setInput_large_fullParse(bh: Blackhole) {
        bh.consume(parser.parse(largeModified))
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 3: 逐字输入场景 — 逐字符从头输入完整公式
    //   增量：逐字 append（累计 N 次增量解析）
    //   全量：每输入一个字符都全量重解析（累计 N 次全量解析）
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun charByChar_medium_incremental(bh: Blackhole) {
        val inc = IncrementalLatexParser()
        for (char in BenchmarkInputs.MEDIUM) {
            inc.append(char.toString())
        }
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun charByChar_medium_fullParse(bh: Blackhole) {
        val sb = StringBuilder(BenchmarkInputs.MEDIUM.length)
        for (char in BenchmarkInputs.MEDIUM) {
            sb.append(char)
            bh.consume(parser.parse(sb.toString()))
        }
    }

    @Benchmark
    fun charByChar_large_incremental(bh: Blackhole) {
        val inc = IncrementalLatexParser()
        for (char in BenchmarkInputs.LARGE) {
            inc.append(char.toString())
        }
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun charByChar_large_fullParse(bh: Blackhole) {
        val sb = StringBuilder(BenchmarkInputs.LARGE.length)
        for (char in BenchmarkInputs.LARGE) {
            sb.append(char)
            bh.consume(parser.parse(sb.toString()))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 4: 分块流式输入场景
    //   增量：逐块 append
    //   全量：每块拼接后全量重解析
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun chunkByChunk_incremental(bh: Blackhole) {
        val inc = IncrementalLatexParser()
        for (chunk in BenchmarkInputs.INCREMENTAL_CHUNKS) {
            inc.append(chunk)
        }
        bh.consume(inc.getCurrentDocument())
    }

    @Benchmark
    fun chunkByChunk_fullParse(bh: Blackhole) {
        val sb = StringBuilder()
        for (chunk in BenchmarkInputs.INCREMENTAL_CHUNKS) {
            sb.append(chunk)
            bh.consume(parser.parse(sb.toString()))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 5: 纯基线 — 单次全量解析（参考值）
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun baseline_medium(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.MEDIUM))
    }

    @Benchmark
    fun baseline_large(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.LARGE))
    }

    @Benchmark
    fun baseline_complex(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.COMPLEX))
    }
}
