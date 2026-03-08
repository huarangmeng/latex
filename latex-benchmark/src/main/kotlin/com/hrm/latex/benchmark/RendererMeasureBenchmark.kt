package com.hrm.latex.benchmark

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.benchmark.MeasureBenchmarkHelper
import com.hrm.latex.renderer.model.RenderContext
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import java.util.concurrent.TimeUnit

/**
 * Renderer measure 阶段 benchmark：测量不同复杂度 LaTeX 公式的布局耗时。
 *
 * 覆盖维度：
 * - 公式复杂度梯度：简单 → 中等 → 复杂 → 矩阵 → 大规模
 * - measure 完整流程（LatexRenderer.measure）含缓存清除和 padding 计算
 * - measureGroup 核心测量（不含外层封装）
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class RendererMeasureBenchmark {

    private lateinit var parser: LatexParser
    private lateinit var textMeasurer: TextMeasurer
    private lateinit var density: Density
    private lateinit var context: RenderContext

    // 预解析的 AST
    private lateinit var simpleAst: List<LatexNode>
    private lateinit var mediumAst: List<LatexNode>
    private lateinit var complexAst: List<LatexNode>
    private lateinit var matrixAst: List<LatexNode>
    private lateinit var largeAst: List<LatexNode>

    @Setup
    fun setup() {
        parser = LatexParser()

        // 创建 Density（模拟标准 2x 密度屏幕）
        density = Density(2f, 1f)

        // 创建 TextMeasurer（JVM Desktop API，无需 Composable 作用域）
        val fontFamilyResolver = createFontFamilyResolver()
        textMeasurer = TextMeasurer(fontFamilyResolver, density, LayoutDirection.Ltr)

        // 创建 RenderContext（使用系统默认字体，专注测量布局算法性能）
        context = MeasureBenchmarkHelper.createContext()

        // 预解析所有输入为 AST（排除 parser 耗时）
        simpleAst = parser.parse(BenchmarkInputs.SIMPLE).children
        mediumAst = parser.parse(BenchmarkInputs.MEDIUM).children
        complexAst = parser.parse(BenchmarkInputs.COMPLEX).children
        matrixAst = parser.parse(BenchmarkInputs.MATRIX).children
        largeAst = parser.parse(BenchmarkInputs.LARGE).children
    }

    // ═══════════════════════════════════════════════════════════
    // 组 1: LatexRenderer.measure 完整流程
    // ═══════════════════════════════════════════════════════════

    @Benchmark
    fun measure_full_simple() {
        MeasureBenchmarkHelper.measureFull(simpleAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measure_full_medium() {
        MeasureBenchmarkHelper.measureFull(mediumAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measure_full_complex() {
        MeasureBenchmarkHelper.measureFull(complexAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measure_full_matrix() {
        MeasureBenchmarkHelper.measureFull(matrixAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measure_full_large() {
        MeasureBenchmarkHelper.measureFull(largeAst, context, textMeasurer, density)
    }

    // ═══════════════════════════════════════════════════════════
    // 组 2: measureGroup 核心测量（不含 LatexRenderer 外层封装）
    // ═══════════════════════════════════════════════════════════

    @Benchmark
    fun measureGroup_simple() {
        MeasureBenchmarkHelper.measureGroup(simpleAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measureGroup_medium() {
        MeasureBenchmarkHelper.measureGroup(mediumAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measureGroup_complex() {
        MeasureBenchmarkHelper.measureGroup(complexAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measureGroup_matrix() {
        MeasureBenchmarkHelper.measureGroup(matrixAst, context, textMeasurer, density)
    }

    @Benchmark
    fun measureGroup_large() {
        MeasureBenchmarkHelper.measureGroup(largeAst, context, textMeasurer, density)
    }

    // ═══════════════════════════════════════════════════════════
    // 组 3: 端到端 parse + measure（真实使用场景）
    // ═══════════════════════════════════════════════════════════

    @Benchmark
    fun endToEnd_simple() {
        val ast = parser.parse(BenchmarkInputs.SIMPLE).children
        MeasureBenchmarkHelper.measureFull(ast, context, textMeasurer, density)
    }

    @Benchmark
    fun endToEnd_medium() {
        val ast = parser.parse(BenchmarkInputs.MEDIUM).children
        MeasureBenchmarkHelper.measureFull(ast, context, textMeasurer, density)
    }

    @Benchmark
    fun endToEnd_complex() {
        val ast = parser.parse(BenchmarkInputs.COMPLEX).children
        MeasureBenchmarkHelper.measureFull(ast, context, textMeasurer, density)
    }

    @Benchmark
    fun endToEnd_matrix() {
        val ast = parser.parse(BenchmarkInputs.MATRIX).children
        MeasureBenchmarkHelper.measureFull(ast, context, textMeasurer, density)
    }

    @Benchmark
    fun endToEnd_large() {
        val ast = parser.parse(BenchmarkInputs.LARGE).children
        MeasureBenchmarkHelper.measureFull(ast, context, textMeasurer, density)
    }
}
