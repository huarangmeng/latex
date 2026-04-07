package com.hrm.latex.benchmark

import com.hrm.latex.parser.LatexParser
import kotlinx.benchmark.*

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class ParserBenchmark {

    private lateinit var parser: LatexParser

    @Setup
    fun setup() {
        parser = LatexParser()
    }

    // ── parse() 基准测试 ──

    @Benchmark
    fun parseSimple(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.SIMPLE))
    }

    @Benchmark
    fun parseMedium(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.MEDIUM))
    }

    @Benchmark
    fun parseComplex(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.COMPLEX))
    }

    @Benchmark
    fun parseMatrix(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.MATRIX))
    }

    @Benchmark
    fun parseLarge(bh: Blackhole) {
        bh.consume(parser.parse(BenchmarkInputs.LARGE))
    }

    // ── parseWithDiagnostics() 基准测试 ──

    @Benchmark
    fun parseWithDiagnosticsSimple(bh: Blackhole) {
        bh.consume(parser.parseWithDiagnostics(BenchmarkInputs.SIMPLE))
    }

    @Benchmark
    fun parseWithDiagnosticsMedium(bh: Blackhole) {
        bh.consume(parser.parseWithDiagnostics(BenchmarkInputs.MEDIUM))
    }

    @Benchmark
    fun parseWithDiagnosticsComplex(bh: Blackhole) {
        bh.consume(parser.parseWithDiagnostics(BenchmarkInputs.COMPLEX))
    }
}
