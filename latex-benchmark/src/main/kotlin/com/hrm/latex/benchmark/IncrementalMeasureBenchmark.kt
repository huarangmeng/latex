package com.hrm.latex.benchmark

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.LatexParser
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
 * 增量测量端到端 Benchmark：IncrementalParser + LayoutCache 联合优化。
 *
 * 模拟真实编辑器场景：
 * 1. 流式输入：AI 逐块输出 LaTeX，每块追加后立即 parse + measure
 * 2. 逐字输入：用户逐字符输入，每字符都需要更新渲染
 * 3. 对比：增量 pipeline（IncrementalParser + Cache）vs 全量 pipeline（LatexParser + 无缓存）
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class IncrementalMeasureBenchmark {

    private lateinit var parser: LatexParser
    private lateinit var textMeasurer: TextMeasurer
    private lateinit var density: Density
    private lateinit var context: RenderContext

    @Setup
    fun setup() {
        parser = LatexParser()
        density = Density(2f, 1f)

        val fontFamilyResolver = createFontFamilyResolver()
        textMeasurer = TextMeasurer(fontFamilyResolver, density, LayoutDirection.Ltr)

        context = MeasureBenchmarkHelper.createContext()
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 1: 流式分块输入 — IncrementalParser + Cache vs LatexParser + 无缓存
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun streaming_incremental_withCache(bh: Blackhole) {
        val inc = IncrementalLatexParser()
        val cache = LayoutCache()

        for (chunk in BenchmarkInputs.INCREMENTAL_CHUNKS) {
            inc.append(chunk)
            val doc = inc.getCurrentDocument()
            bh.consume(measureGroup(doc.children, context, textMeasurer, density, cache = cache))
        }
    }

    @Benchmark
    fun streaming_fullParse_noCache(bh: Blackhole) {
        val sb = StringBuilder()
        for (chunk in BenchmarkInputs.INCREMENTAL_CHUNKS) {
            sb.append(chunk)
            val doc = parser.parse(sb.toString())
            bh.consume(MeasureBenchmarkHelper.measureGroup(doc.children, context, textMeasurer, density))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 2: 逐字符输入 — medium 公式
    //   每输入一个字符都进行一次 parse + measure
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun charByChar_medium_incremental_withCache(bh: Blackhole) {
        val inc = IncrementalLatexParser()
        val cache = LayoutCache()

        for (char in BenchmarkInputs.MEDIUM) {
            inc.append(char.toString())
            val doc = inc.getCurrentDocument()
            bh.consume(measureGroup(doc.children, context, textMeasurer, density, cache = cache))
        }
    }

    @Benchmark
    fun charByChar_medium_fullParse_noCache(bh: Blackhole) {
        val sb = StringBuilder(BenchmarkInputs.MEDIUM.length)
        for (char in BenchmarkInputs.MEDIUM) {
            sb.append(char)
            val doc = parser.parse(sb.toString())
            bh.consume(MeasureBenchmarkHelper.measureGroup(doc.children, context, textMeasurer, density))
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 组 3: 多次追加 — 每次追加一个项
    //   模拟逐步构建长公式
    // ════════════════════════════════════════════════════════════════════

    @Benchmark
    fun multiAppend_incremental_withCache(bh: Blackhole) {
        val inc = IncrementalLatexParser()
        val cache = LayoutCache()
        val terms = listOf("a", " + b", " + c^2", " + \\frac{d}{e}", " + \\sqrt{f}", " + g_i")

        for (term in terms) {
            inc.append(term)
            val doc = inc.getCurrentDocument()
            bh.consume(measureGroup(doc.children, context, textMeasurer, density, cache = cache))
        }
    }

    @Benchmark
    fun multiAppend_fullParse_noCache(bh: Blackhole) {
        val sb = StringBuilder()
        val terms = listOf("a", " + b", " + c^2", " + \\frac{d}{e}", " + \\sqrt{f}", " + g_i")

        for (term in terms) {
            sb.append(term)
            val doc = parser.parse(sb.toString())
            bh.consume(MeasureBenchmarkHelper.measureGroup(doc.children, context, textMeasurer, density))
        }
    }
}
