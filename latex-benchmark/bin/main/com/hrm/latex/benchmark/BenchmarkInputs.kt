package com.hrm.latex.benchmark

/**
 * Benchmark 测试数据集：不同复杂度级别的 LaTeX 公式常量。
 * 所有公式均为合法 LaTeX，覆盖从简单到大规模的梯度场景。
 */
object BenchmarkInputs {

    // ── 简单公式 (~10 字符) ──
    const val SIMPLE = "E=mc^2"

    // ── 中等公式 (~50 字符): 二次公式 ──
    const val MEDIUM = "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"

    // ── 复杂公式 (~150 字符): 嵌套求和 + 分数 + 根号 + 上下标 ──
    const val COMPLEX = "\\sum_{i=1}^{n} \\frac{x_i - \\bar{x}}{\\sqrt{\\sum_{j=1}^{m} (y_j - \\hat{y}_j)^2}} + \\int_{0}^{\\infty} e^{-t^2} dt"

    // ── 矩阵公式：包含矩阵环境 ──
    const val MATRIX = "A = \\begin{pmatrix} a_{11} & a_{12} & \\cdots & a_{1n} \\\\ a_{21} & a_{22} & \\cdots & a_{2n} \\\\ \\vdots & \\vdots & \\ddots & \\vdots \\\\ a_{n1} & a_{n2} & \\cdots & a_{nn} \\end{pmatrix}"

    // ── 大规模输入: 重复拼接中等公式 (~1000+ 字符) ──
    val LARGE: String = buildString {
        repeat(20) { append("$MEDIUM + ") }
        append(MEDIUM)
    }

    // ── 增量解析：分块输入序列 ──
    val INCREMENTAL_CHUNKS: List<String> = listOf(
        "\\int_{-\\",
        "infty}^{\\infty}",
        " f(t)",
        " e^{-i\\omega t}",
        " dt",
        " = \\hat{f}(\\omega)",
    )

    // ── 增量解析：完整公式（所有块拼接） ──
    val INCREMENTAL_FULL: String = INCREMENTAL_CHUNKS.joinToString("")
}
