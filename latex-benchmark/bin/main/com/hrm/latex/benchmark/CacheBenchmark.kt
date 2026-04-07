package com.hrm.latex.benchmark

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.benchmark.MeasureBenchmarkHelper
import com.hrm.latex.renderer.layout.LayoutCache
import com.hrm.latex.renderer.layout.measureGroup
import com.hrm.latex.renderer.model.RenderContext
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import java.util.concurrent.TimeUnit

/**
 * LayoutCache 性能基准：缓存冷启动 vs 热命中 vs 无缓存 对比。
 *
 * 覆盖维度：
 * - 冷缓存（空缓存下首次测量）vs 热缓存（第二次全部命中）vs 无缓存（baseline）
 * - 不同复杂度公式的缓存收益差异
 * - 增量 AST 追加场景的缓存命中效果
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class CacheBenchmark {

    private lateinit var parser: LatexParser
    private lateinit var textMeasurer: TextMeasurer
    private lateinit var density: Density
    private lateinit var context: RenderContext

    private lateinit var mediumAst: List<LatexNode>
    private lateinit var complexAst: List<LatexNode>
    private lateinit var largeAst: List<LatexNode>

    @Setup
    fun setup() {
        parser = LatexParser()
        density = Density(2f, 1f)

        val fontFamilyResolver = createFontFamilyResolver()
        textMeasurer = TextMeasurer(fontFamilyResolver, density, LayoutDirection.Ltr)

        context = MeasureBenchmarkHelper.createContext()

        mediumAst = parser.parse(BenchmarkInputs.MEDIUM).children
        complexAst = parser.parse(BenchmarkInputs.COMPLEX).children
        largeAst = parser.parse(BenchmarkInputs.LARGE).children
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 1: Medium 公式 — 无缓存 vs 冷缓存 vs 热缓存
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun medium_noCache(bh: Blackhole) {
        bh.consume(MeasureBenchmarkHelper.measureGroup(mediumAst, context, textMeasurer, density))
    }

    @Benchmark
    fun medium_cacheCold(bh: Blackhole) {
        val cache = LayoutCache()
        bh.consume(measureGroup(mediumAst, context, textMeasurer, density, cache = cache))
    }

    @Benchmark
    fun medium_cacheWarm(bh: Blackhole) {
        val cache = LayoutCache()
        measureGroup(mediumAst, context, textMeasurer, density, cache = cache)
        bh.consume(measureGroup(mediumAst, context, textMeasurer, density, cache = cache))
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 2: Complex 公式
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun complex_noCache(bh: Blackhole) {
        bh.consume(MeasureBenchmarkHelper.measureGroup(complexAst, context, textMeasurer, density))
    }

    @Benchmark
    fun complex_cacheCold(bh: Blackhole) {
        val cache = LayoutCache()
        bh.consume(measureGroup(complexAst, context, textMeasurer, density, cache = cache))
    }

    @Benchmark
    fun complex_cacheWarm(bh: Blackhole) {
        val cache = LayoutCache()
        measureGroup(complexAst, context, textMeasurer, density, cache = cache)
        bh.consume(measureGroup(complexAst, context, textMeasurer, density, cache = cache))
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 3: Large 公式 — 缓存收益最显著
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun large_noCache(bh: Blackhole) {
        bh.consume(MeasureBenchmarkHelper.measureGroup(largeAst, context, textMeasurer, density))
    }

    @Benchmark
    fun large_cacheCold(bh: Blackhole) {
        val cache = LayoutCache()
        bh.consume(measureGroup(largeAst, context, textMeasurer, density, cache = cache))
    }

    @Benchmark
    fun large_cacheWarm(bh: Blackhole) {
        val cache = LayoutCache()
        measureGroup(largeAst, context, textMeasurer, density, cache = cache)
        bh.consume(measureGroup(largeAst, context, textMeasurer, density, cache = cache))
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 4: 增量追加场景 — 缓存 vs 全量重新测量
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun incrementalAppend_medium_withCache(bh: Blackhole) {
        val cache = LayoutCache()
        // 首次测量（填充缓存）
        measureGroup(mediumAst, context, textMeasurer, density, cache = cache)
        // 追加后测量（前缀节点命中缓存）
        val appended = parser.parse(BenchmarkInputs.MEDIUM + " + \\alpha^2").children
        bh.consume(measureGroup(appended, context, textMeasurer, density, cache = cache))
    }

    @Benchmark
    fun incrementalAppend_medium_noCache(bh: Blackhole) {
        // 对照：两次无缓存测量
        MeasureBenchmarkHelper.measureGroup(mediumAst, context, textMeasurer, density)
        val appended = parser.parse(BenchmarkInputs.MEDIUM + " + \\alpha^2").children
        bh.consume(MeasureBenchmarkHelper.measureGroup(appended, context, textMeasurer, density))
    }

    @Benchmark
    fun incrementalAppend_large_withCache(bh: Blackhole) {
        val cache = LayoutCache()
        measureGroup(largeAst, context, textMeasurer, density, cache = cache)
        val appended = parser.parse(BenchmarkInputs.LARGE + " + \\beta").children
        bh.consume(measureGroup(appended, context, textMeasurer, density, cache = cache))
    }

    @Benchmark
    fun incrementalAppend_large_noCache(bh: Blackhole) {
        MeasureBenchmarkHelper.measureGroup(largeAst, context, textMeasurer, density)
        val appended = parser.parse(BenchmarkInputs.LARGE + " + \\beta").children
        bh.consume(MeasureBenchmarkHelper.measureGroup(appended, context, textMeasurer, density))
    }
}
