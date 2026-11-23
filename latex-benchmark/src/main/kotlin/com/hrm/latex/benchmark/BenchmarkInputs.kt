package com.hrm.latex.benchmark

import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.model.LatexNode

/**
 * 解析和渲染共用的命名语料；malformed 专门覆盖未知命令和缺少闭合符号的恢复路径。
 * 原有常量继续供流式与渲染基准使用，避免不同测试各自拼接同名但内容不同的公式。
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

    val MACRO: String = "\\newcommand{\\benchterm}[1]{\\frac{#1^2+1}{#1+2}}" +
            List(100) { "\\benchterm{x}" }.joinToString(" + ")

    val DEEP: String = "{".repeat(96) + "x" + "}".repeat(96)

    const val TEXT = "\\text{Energy balance: } E_{\\text{total}} = mc^2 + \\text{温度 } T"

    const val MULTILINE = "\\begin{aligned} x+y &= 1 \\\\ x-y &= \\frac{1}{2} \\\\ z &= \\sqrt{x^2+y^2} \\end{aligned}"

    private val LONG_ENVIRONMENT =
        "\\begin{aligned}" + "\\frac{x_i}{y}+z&=u^2\\\\".repeat(62) + "\\end{aligned}"

    const val MALFORMED = "a + \\unknownbenchmark{x} + \\frac{x+1}{\\sqrt{y"

    fun formula(name: String): String = when (name) {
        "simple" -> SIMPLE
        "medium" -> MEDIUM
        "complex" -> COMPLEX
        "matrix" -> MATRIX
        "large" -> LARGE
        "macro" -> MACRO
        "deep" -> DEEP
        "text" -> TEXT
        "multiline" -> MULTILINE
        "environment" -> LONG_ENVIRONMENT
        "malformed" -> MALFORMED
        else -> error("未知性能测试语料：$name")
    }

    /** 仅供 Trial setup 使用，防止把静默失败或失去错误恢复覆盖的输入当作有效采样。 */
    internal fun validatedDocument(name: String, parser: LatexParser = LatexParser()): LatexNode.Document {
        val result = parser.parseWithDiagnostics(formula(name))
        check(result.document.children.isNotEmpty()) { "$name 语料未产生 AST" }
        if (name == "malformed") {
            check(result.diagnostics.any { it.severity == ParseDiagnostic.Severity.ERROR }) {
                "malformed 语料必须进入错误恢复路径"
            }
        } else {
            check(result.diagnostics.isEmpty()) { "$name 语料存在意外诊断：${result.diagnostics}" }
        }
        return result.document
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
