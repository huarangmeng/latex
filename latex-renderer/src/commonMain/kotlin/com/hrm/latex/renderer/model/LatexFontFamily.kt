package com.hrm.latex.renderer.model

// ===== Base 基础数学字体 =====
// import latex.latex_renderer.generated.resources.cmmib10  // Computer Modern Math Italic Bold

// ===== Latin 拉丁字母字体 =====
// import latex.latex_renderer.generated.resources.bx10     // Bold Extended (简化版 cmbx10)
// import latex.latex_renderer.generated.resources.bi10     // Bold Italic
// import latex.latex_renderer.generated.resources.sb10     // Sans Bold
// import latex.latex_renderer.generated.resources.sbi10    // Sans Bold Italic

// ===== Math 数学符号字体 =====
// import latex.latex_renderer.generated.resources.cmbsy10  // Computer Modern Bold Symbol
// import latex.latex_renderer.generated.resources.stmary10 // St Mary Road symbols
// import latex.latex_renderer.generated.resources.special  // Special symbols

// ===== Euler 欧拉字体 =====
// import latex.latex_renderer.generated.resources.eufb10   // Euler Fraktur Bold

// ===== Script 手写体字体 =====

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import latex.latex_renderer.generated.resources.Res
import latex.latex_renderer.generated.resources.cmex10
import latex.latex_renderer.generated.resources.cmmi10
import latex.latex_renderer.generated.resources.cmr10
import latex.latex_renderer.generated.resources.cmss10
import latex.latex_renderer.generated.resources.cmssi10
import latex.latex_renderer.generated.resources.cmsy10
import latex.latex_renderer.generated.resources.cmti10
import latex.latex_renderer.generated.resources.cmtt10
import latex.latex_renderer.generated.resources.eufm10
import latex.latex_renderer.generated.resources.msam10
import latex.latex_renderer.generated.resources.msbm10
import latex.latex_renderer.generated.resources.rsfs10
import org.jetbrains.compose.resources.Font

/**
 * LaTeX 字体家族配置
 * LaTeX Font Families Configuration
 *
 * 定义了 LaTeX 渲染所需的 10 种核心字体家族。
 * Defines 10 core font families required for LaTeX rendering.
 *
 * ## 使用场景速查 | Quick Reference
 *
 * | 字段 Field | 实际字体 Font | LaTeX 命令 | 使用场景 Use Case |
 * |-----------|--------------|-----------|------------------|
 * | `roman` | cmr10, cmti10 | `\text{}`, `\mathrm{}` | 正文文本、函数名 Text, function names |
 * | `sansSerif` | cmss10, cmssi10 | `\textsf{}`, `\mathsf{}` | 无衬线文本 Sans-serif text |
 * | `monospace` | cmtt10 | `\texttt{}`, `\mathtt{}` | 代码、等宽 Code, monospace |
 * | `mathItalic` | cmmi10 | (默认 default) | 数学变量: x, y, α, β |
 * | `symbol` | cmsy10 | (自动 auto) | 运算符、括号: +, ×, ≤, ( ) |
 * | `extension` | cmex10 | (自动 auto) | 大型符号: ∑, ∫, √, { } |
 * | `blackboardBold` | msbm10 | `\mathbb{}` ✅ | 数集: ℝ, ℕ, ℤ, ℂ |
 * | `calligraphic` | msam10 | `\mathcal{}` ✅ | 花体: 𝓕, 𝓣 (集合论) |
 * | `fraktur` | eufm10 | `\mathfrak{}` ✅ | 哥特体: 𝔤 (李代数) |
 * | `script` | rsfs10 | `\mathscr{}` ✅ | 手写体: 𝓛, ℋ (物理) |
 *
 * **✅ = 已实现并经过测试 Implemented and tested**
 * - Parser 支持: `\mathbb`, `\mathcal`, `\mathfrak`, `\mathscr` 命令
 * - Renderer 支持: 通过 `RenderContext.applyStyle()` 应用相应字体
 * - 测试覆盖: 见 `ComplexStructureTest.kt` 中的 `testMathBB/Cal/Frak()` 测试
 *
 * ## 使用示例 | Examples
 *
 * ```kotlin
 * // 1. 使用默认字体 Use default fonts
 * Latex(
 *     latexString = "\\mathbb{R} \\times \\mathcal{F} = \\mathfrak{g}",
 *     config = LatexConfig(fontFamilies = defaultLatexFontFamilies())
 * )
 *
 * // 2. 自定义字体 Custom fonts
 * val customFonts = LatexFontFamilies(
 *     roman = FontFamily(Font(...)),
 *     mathItalic = FontFamily(Font(...)),
 *     blackboardBold = FontFamily(Font(...)),  // 自定义 \mathbb 字体
 *     // ... 其他字体 other fonts
 * )
 * Latex(
 *     latexString = "x \\in \\mathbb{R}",
 *     config = LatexConfig(fontFamilies = customFonts)
 * )
 * ```
 *
 * **注意 Note**: 如不提供 fontFamilies,将使用系统默认字体,非 Computer Modern 字体。
 * If fontFamilies is not provided, system default fonts will be used instead of Computer Modern.
 */
data class LatexFontFamilies(
    // === 文本字体 Text Fonts ===
    val roman: FontFamily,           // cmr10/cmti10 - \text{}, \mathrm{}
    val sansSerif: FontFamily,       // cmss10/cmssi10 - \textsf{}, \mathsf{}
    val monospace: FontFamily,       // cmtt10 - \texttt{}, \mathtt{}

    // === 核心数学字体 Core Math Fonts ===
    val mathItalic: FontFamily,      // cmmi10 - 数学变量默认: x, y, α, β
    val symbol: FontFamily,          // cmsy10 - 运算符、括号: +, ×, ( )
    val extension: FontFamily,       // cmex10 - 大型符号: ∑, ∫, √, { }

    // === 特殊数学字体 Special Math Fonts (已实现 Implemented) ===
    val blackboardBold: FontFamily,  // msbm10 - \mathbb{R} → ℝ (数集)
    val calligraphic: FontFamily,    // msam10 - \mathcal{F} → 𝓕 (花体)
    val fraktur: FontFamily,         // eufm10 - \mathfrak{g} → 𝔤 (哥特体)
    val script: FontFamily,          // rsfs10 - \mathscr{L} → 𝓛 (手写体)

    /**
     * 是否为内嵌 Computer Modern 字体（默认字体）。
     *
     * 仅当使用 [defaultLatexFontFamilies] 创建时为 true。
     * CM 字体的 TTF 使用特殊的 TeX 编码（cmap 表中的 codepoint 与标准 Unicode 不同），
     * 需要通过 FontResolver 的 TeX 编码映射表来路由正确的 char code。
     * 外部传入的自定义字体通常使用标准 Unicode 编码，不需要此映射。
     */
    val isDefaultCM: Boolean = false
)

/**
 * 创建默认的 LaTeX 字体家族 (Computer Modern 字体)
 * Creates default LaTeX font families (Computer Modern fonts)
 *
 * **字体列表 Font List:**
 * - 文本: cmr10 (正体), cmti10 (斜体), cmss10/cmssi10 (无衬线), cmtt10 (等宽)
 * - 数学: cmmi10 (变量), cmsy10 (符号), cmex10 (大型符号)
 * - 特殊: msbm10 (黑板粗体), cmsy10 (花体), eufm10 (哥特体), rsfs10 (手写体)
 */
@Composable
internal fun defaultLatexFontFamilies(): LatexFontFamilies {
    // === 文本字体 Text Fonts ===
    val roman = FontFamily(
        Font(Res.font.cmr10, style = FontStyle.Normal),
        Font(Res.font.cmti10, style = FontStyle.Italic)
    )

    val sansSerif = FontFamily(
        Font(Res.font.cmss10, style = FontStyle.Normal),
        Font(Res.font.cmssi10, style = FontStyle.Italic)
    )

    val monospace = FontFamily(
        Font(Res.font.cmtt10, style = FontStyle.Normal)
    )

    // === 核心数学字体 Core Math Fonts ===
    val mathItalic = FontFamily(Font(Res.font.cmmi10))  // 数学变量默认
    val symbol = FontFamily(Font(Res.font.cmsy10))      // 运算符、括号
    val extension = FontFamily(Font(Res.font.cmex10))   // 大型符号: ∑∫√{}

    // === 特殊数学字体 Special Math Fonts ===
    val blackboardBold = FontFamily(Font(Res.font.msbm10))  // \mathbb{R}
    val calligraphic = FontFamily(Font(Res.font.cmsy10))    // \mathcal{L}
    val fraktur = FontFamily(Font(Res.font.eufm10))         // \mathfrak{g}
    val script = FontFamily(Font(Res.font.rsfs10))          // \mathscr{L}

    return LatexFontFamilies(
        roman = roman,
        sansSerif = sansSerif,
        monospace = monospace,
        mathItalic = mathItalic,
        symbol = symbol,
        extension = extension,
        blackboardBold = blackboardBold,
        calligraphic = calligraphic,
        fraktur = fraktur,
        script = script,
        isDefaultCM = true
    )
}

/*
 * ===== LaTeX 命令到字体的映射 =====
 *
 * | LaTeX 命令 | 字体 | 示例输出 |
 * |-----------|------|---------|
 * | 默认数学变量 | cmmi10 | $x, y, \alpha, \beta$ |
 * | \mathrm{} | r10 | $\mathrm{sin}, \mathrm{d}x$ |
 * | \mathit{} | i10 | $\mathit{text}$ |
 * | \mathbf{} | bx10 | $\mathbf{v}, \mathbf{A}$ |
 * | \mathsf{} | ss10 | $\mathsf{ABC}$ |
 * | \mathtt{} | tt10 | $\mathtt{code}$ |
 * | \mathbb{} | msbm10 | $\mathbb{R}, \mathbb{N}$ |
 * | \mathcal{} | msam10 | $\mathcal{A}, \mathcal{F}$ |
 * | \mathfrak{} | eufm10 | $\mathfrak{g}, \mathfrak{su}$ |
 * | \mathscr{} | rsfs10 | $\mathscr{L}, \mathscr{H}$ |
 * | () [] | cmsy10 | $(a+b), [x]$ |
 * | {} | cmex10 | $\{x \mid x > 0\}$ |
 * | ∑∫∏ | cmex10 | $\sum_{i=1}^n, \int_0^1$ |
 * | +−×÷ | cmsy10 | $a + b \times c$ |
 */
