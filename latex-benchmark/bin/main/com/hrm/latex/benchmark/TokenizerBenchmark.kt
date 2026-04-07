package com.hrm.latex.benchmark

import com.hrm.latex.parser.tokenizer.LatexTokenizer
import kotlinx.benchmark.*

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
open class TokenizerBenchmark {

    @Benchmark
    fun tokenizeSimple(bh: Blackhole) {
        bh.consume(LatexTokenizer(BenchmarkInputs.SIMPLE).tokenize())
    }

    @Benchmark
    fun tokenizeMedium(bh: Blackhole) {
        bh.consume(LatexTokenizer(BenchmarkInputs.MEDIUM).tokenize())
    }

    @Benchmark
    fun tokenizeComplex(bh: Blackhole) {
        bh.consume(LatexTokenizer(BenchmarkInputs.COMPLEX).tokenize())
    }

    @Benchmark
    fun tokenizeMatrix(bh: Blackhole) {
        bh.consume(LatexTokenizer(BenchmarkInputs.MATRIX).tokenize())
    }

    @Benchmark
    fun tokenizeLarge(bh: Blackhole) {
        bh.consume(LatexTokenizer(BenchmarkInputs.LARGE).tokenize())
    }
}
