package com.hrm.latex.renderer.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import latex.latex_renderer.generated.resources.Res

// ===== Base 基础数学字体 =====
import latex.latex_renderer.generated.resources.cmex10   // Computer Modern Extension
import latex.latex_renderer.generated.resources.cmmi10   // Computer Modern Math Italic
// import latex.latex_renderer.generated.resources.cmmib10  // Computer Modern Math Italic Bold

// ===== Latin 拉丁字母字体 =====
import latex.latex_renderer.generated.resources.cmr10    // Computer Modern Roman
import latex.latex_renderer.generated.resources.cmti10   // Computer Modern Text Italic
import latex.latex_renderer.generated.resources.cmss10   // Computer Modern Sans Serif
import latex.latex_renderer.generated.resources.cmssi10  // Computer Modern Sans Serif Italic
import latex.latex_renderer.generated.resources.cmtt10   // Computer Modern Typewriter
// import latex.latex_renderer.generated.resources.bx10     // Bold Extended (简化版 cmbx10)
// import latex.latex_renderer.generated.resources.bi10     // Bold Italic
// import latex.latex_renderer.generated.resources.sb10     // Sans Bold
// import latex.latex_renderer.generated.resources.sbi10    // Sans Bold Italic

// ===== Math 数学符号字体 =====
import latex.latex_renderer.generated.resources.cmsy10   // Computer Modern Symbol
// import latex.latex_renderer.generated.resources.cmbsy10  // Computer Modern Bold Symbol
import latex.latex_renderer.generated.resources.msam10   // Math Symbol A (AMS)
import latex.latex_renderer.generated.resources.msbm10   // Math Symbol B (AMS)
// import latex.latex_renderer.generated.resources.stmary10 // St Mary Road symbols
// import latex.latex_renderer.generated.resources.special  // Special symbols

// ===== Euler 欧拉字体 =====
import latex.latex_renderer.generated.resources.eufm10   // Euler Fraktur Medium
// import latex.latex_renderer.generated.resources.eufb10   // Euler Fraktur Bold

// ===== Script 手写体字体 =====
import latex.latex_renderer.generated.resources.rsfs10   // Ralph Smith Formal Script

import org.jetbrains.compose.resources.Font

/**
 * LaTeX 字体家族配置
 *
 * ## MicroTeX 字体体系完整说明
 *
 * ### 字体粗细等级（按 TeX 标准）
 *
 * | 等级 | 名称 | 后缀标识 | 说明 |
 * |------|------|---------|------|
 * | 1 | Light | - | 细体（无标准后缀） |
 * | 2 | Medium | (无) 或 r | 正常粗细（默认） |
 * | 3 | Semi-Bold | sb | 半粗 |
 * | 4 | Bold | b | 粗体 |
 * | 5 | Bold Extended | bx | 粗体加宽 |
 *
 * ### 字体形状
 *
 * | 形状 | 后缀 | 说明 |
 * |------|------|------|
 * | Normal | (无) | 正体 |
 * | Italic | i | 斜体 |
 * | Slanted | sl | 倾斜（未实现） |
 * | Small Caps | sc | 小型大写（未实现） |
 *
 * ### 字体家族
 *
 * | 家族 | 前缀 | 说明 |
 * |------|------|------|
 * | Roman | cmr/r | 衬线字体（正文默认） |
 * | Sans Serif | cmss/ss | 无衬线字体 |
 * | Typewriter | cmtt/tt | 等宽字体 |
 * | Math Italic | cmmi | 数学斜体 |
 * | Symbol | cmsy | 数学符号 |
 * | Extension | cmex | 可伸缩符号 |
 *
 * ---
 *
 * ## MicroTeX 字体文件详细列表
 *
 * ### 📁 base/ - 基础数学字体
 *
 * | 文件 | 大小 | 粗细 | 用途 |
 * |------|------|------|------|
 * | `cmmi10.ttf` | 26KB | Medium | 数学斜体变量：x, y, α, β, γ 等 |
 * | `cmmib10.ttf` | 24KB | Bold | 数学粗斜体：\boldsymbol{α} |
 * | `cmex10.ttf` | 19KB | Medium | 大型运算符(∑∫∏)、可伸缩定界符 |
 *
 * **说明**：
 * - `cmmi10` 是数学模式下变量的默认字体
 * - `cmex10` 用于根号、积分号、求和号等可缩放符号
 * - 这些字体**必须使用**，无法替代
 *
 * ---
 *
 * ### 📁 latin/ - 拉丁字母字体（核心文件）
 *
 * #### 简化版本（⚠️ 仅包含极少字符，不推荐使用）
 *
 * | 文件 | 大小 | 字符内容 | 用途 |
 * |------|------|---------|------|
 * | `r10.ttf` | 3.9KB | **仅括号等极少符号** | MicroTeX 内部使用，不适合文本 |
 * | `i10.ttf` | 3.0KB | 极少字符 | 不推荐 |
 * | `ss10.ttf` | 2.5KB | 极少字符 | 不推荐 |
 * | `si10.ttf` | 2.7KB | 极少字符 | 不推荐 |
 * | `tt10.ttf` | 2.6KB | 极少字符 | 不推荐 |
 * | `bx10.ttf` | 2.9KB | 极少字符 | 不推荐 |
 * | `bi10.ttf` | 3.0KB | 极少字符 | 不推荐 |
 * | `sb10.ttf` | 2.9KB | 极少字符 | 不推荐 |
 * | `sbi10.ttf` | 2.9KB | 极少字符 | 不推荐 |
 *
 * **简化版真相**：
 * - ⚠️ 这些字体**不是细体**，也不是完整字体的简化版
 * - 它们只包含特定的几个字符（如括号、百分号等）
 * - MicroTeX 用它们作为特殊符号的补充，而非主要文本字体
 * - **不要用作 \text{} 或 \mathrm{} 的字体！**
 *
 * #### 完整版本（latin/optional/，推荐使用）
 *
 * | 文件 | 大小 | 粗细 | 形状 | 用途 |
 * |------|------|------|------|------|
 * | `cmr10.ttf` | 25KB | Medium | Normal | ✅ 正文正体（完整拉丁字母） |
 * | `cmti10.ttf` | 32KB | Medium | Italic | ✅ 正文斜体（完整） |
 * | `cmss10.ttf` | 13KB | Medium | Normal | ✅ 无衬线正体（完整） |
 * | `cmssi10.ttf` | 14KB | Medium | Italic | ✅ 无衬线斜体（完整） |
 * | `cmtt10.ttf` | 28KB | Medium | Normal | ✅ 等宽字体（完整） |
 * | `cmbx10.ttf` | 18KB | Bold Extended | Normal | 粗体（完整） |
 * | `cmbxti10.ttf` | 24KB | Bold Extended | Italic | 粗斜体（完整） |
 * | `cmssbx10.ttf` | 25KB | Bold Extended | Normal | 无衬线粗体（完整） |
 *
 * **完整版特点**：
 * - ✅ 包含完整的拉丁字母表（a-z, A-Z）
 * - ✅ 包含数字和标点符号
 * - ✅ 包含连字（ligature）如 fi, fl
 * - ✅ 包含字距调整（kerning）信息
 * - ✅ 这才是 MicroTeX 实际使用的文本字体
 *
 * ---
 *
 * ### 📁 maths/ - 数学符号字体
 *
 * | 文件 | 大小 | 粗细 | 用途 |
 * |------|------|------|------|
 * | `cmsy10.ttf` | 22KB | Medium | 数学符号：+−×÷±≤≥≠→∈∪∩ 等<br>**小型定界符**：()[]⟨⟩ |
 * | `cmbsy10.ttf` | 19KB | Bold | 粗体数学符号 |
 * | `msam10.ttf` | 21KB | Medium | AMS 扩展符号 A：箭头、花体字母 |
 * | `msbm10.ttf` | 29KB | Medium | AMS 扩展符号 B：黑板粗体 ℝℕℤℚℂ |
 * | `rsfs10.ttf` | 10KB | Medium | Ralph Smith 手写体：𝒜ℬ𝒞 |
 * | `stmary10.ttf` | 17KB | Medium | St Mary Road 额外符号 |
 * | `dsrom10.ttf` | 9.1KB | Medium | 双线体黑板粗体（可选方案） |
 * | `special.ttf` | 2.7KB | - | 特殊符号集 |
 *
 * **关键说明**：
 * - **`cmsy10` 用于小括号 `()`**：根据实测，效果最好
 * - `msam10` 包含 \mathcal{} 花体
 * - `msbm10` 包含 \mathbb{} 黑板粗体
 *
 * ---
 *
 * ### 📁 euler/ - 欧拉字体（数学用）
 *
 * | 文件 | 大小 | 粗细 | 用途 |
 * |------|------|------|------|
 * | `eufm10.ttf` | 23KB | Medium | 欧拉哥特体：\mathfrak{g} 李代数 |
 * | `eufb10.ttf` | 23KB | Bold | 欧拉粗哥特体 |
 *
 * ---
 *
 * ### 📁 greek/ - 希腊语支持（多语言扩展）
 *
 * 包含希腊语文本模式的字体，用于希腊语排版（非数学符号）：
 * - `fcmrpg.ttf` - Greek Roman
 * - `fcsrpg.ttf` - Greek Sans Serif
 * - `fctrpg.ttf` - Greek Typewriter
 * - 等等...
 *
 * **注意**：数学模式的希腊字母（α β γ）使用 `cmmi10`，不用这些文件
 *
 * ---
 *
 * ### 📁 cyrillic/ - 西里尔字母支持（俄语等）
 *
 * 包含西里尔字母的字体，用于俄语等语言排版：
 * - `wnr10.ttf` - Cyrillic Roman
 * - `wnbx10.ttf` - Cyrillic Bold Extended
 * - 等等...
 *
 * ---
 *
 * ## 字体粗细总结
 *
 * ### 正常粗细（Medium）
 * - `r10`, `i10`, `ss10`, `si10`, `tt10`
 * - `cmr10`, `cmti10`, `cmss10`, `cmssi10`, `cmtt10`
 * - `cmmi10`, `cmsy10`, `cmex10`
 * - `msam10`, `msbm10`, `rsfs10`, `eufm10`
 *
 * ### 半粗（Semi-Bold）
 * - `sb10`, `sbi10`
 *
 * ### 粗体（Bold / Bold Extended）
 * - `bx10`, `bi10` (简化版)
 * - `cmbx10`, `cmbxti10`, `cmssbx10` (完整版)
 * - `cmmib10`, `cmbsy10`, `eufb10` (数学粗体)
 *
 * ### 重要结论
 *
 * 1. **r10 系列不是简化版**：只包含极少字符（括号等），不适合文本渲染
 * 2. **必须使用 cmr10 系列**：这才是完整的拉丁字母字体
 * 3. **文件大小 ≠ 粗细**：大文件包含完整字符集和排版信息
 * 4. **括号推荐 cmsy10**：根据实测效果最好
 * 5. **MicroTeX 的选择**：实际使用 `cmr10` 作为 `\mathrm{}` 字体
 */
data class LatexFontFamilies(
    // === 文本字体 ===
    val roman: FontFamily,           // \text{}, \mathrm{}
    val sansSerif: FontFamily,       // \textsf{}, \mathsf{}
    val monospace: FontFamily,       // \texttt{}, \mathtt{}

    // === 核心数学字体 ===
    val mathItalic: FontFamily,      // 数学变量默认：x, y, α, β
    val symbol: FontFamily,          // 数学符号和小括号
    val extension: FontFamily,       // 大型运算符和大括号

    // === 特殊数学字体 ===
    val blackboardBold: FontFamily,  // \mathbb{R} → ℝ
    val calligraphic: FontFamily,    // \mathcal{A} → 𝓐
    val fraktur: FontFamily,         // \mathfrak{A} → 𝔄
    val script: FontFamily           // \mathscr{A} → 𝒜
)

/**
 * 创建默认的 LaTeX 字体家族
 *
 * **重要发现**：
 * - `r10/i10/ss10` 系列只包含极少字符（括号等），**不适合**作为文本字体
 * - **必须使用** `cmr10/cmti10/cmss10` 等完整版字体
 * - MicroTeX 的实际配置也是使用完整版字体
 */
@Composable
fun defaultLatexFontFamilies(): LatexFontFamilies {
    // === 文本字体：使用完整版 ===
    // cmr10 - Computer Modern Roman，包含完整拉丁字母表
    val roman = FontFamily(
        Font(Res.font.cmr10, style = FontStyle.Normal),   // 正体
        Font(Res.font.cmti10, style = FontStyle.Italic)   // 斜体
    )

    // cmss10 - Computer Modern Sans Serif
    val sansSerif = FontFamily(
        Font(Res.font.cmss10, style = FontStyle.Normal),  // 正体
        Font(Res.font.cmssi10, style = FontStyle.Italic)  // 斜体
    )

    // cmtt10 - Computer Modern Typewriter
    val monospace = FontFamily(
        Font(Res.font.cmtt10, style = FontStyle.Normal)
    )

    // === 核心数学字体：标准版 ===

    // cmmi10 - 数学斜体，所有数学变量的默认字体
    val mathItalic = FontFamily(Font(Res.font.cmmi10))

    // cmsy10 - 数学符号字体
    // 包含：
    // 1. 运算符：+, −, ×, ÷, ±, ∓, ⊕, ⊗
    // 2. 关系符：≤, ≥, ≠, ≈, ∼, ≡, ⊂, ⊃, ∈, ∉
    // 3. 箭头：→, ←, ↔, ⇒, ⇐, ⇔
    // 4. 小型定界符：(), [], ⟨⟩, ⌈⌉, ⌊⌋
    // 5. 集合：∪, ∩, ∅
    // **根据实测，cmsy10 的小括号效果最好**
    val symbol = FontFamily(Font(Res.font.cmsy10))

    // cmex10 - 扩展符号字体
    // 包含：
    // 1. 大型运算符：∑, ∫, ∏, ⋃, ⋂, ⋁, ⋀
    // 2. 可伸缩定界符：{}, ⟨⟩, |, ‖（根据内容自动缩放）
    // 3. 根号：√, ∛, ∜
    // 4. 上下箭头扩展：↑, ↓, ⇑, ⇓
    val extension = FontFamily(Font(Res.font.cmex10))

    // === 特殊数学字体 ===

    // msbm10 - AMS 黑板粗体
    // \mathbb{R} → ℝ (实数), \mathbb{N} → ℕ (自然数)
    // \mathbb{Z} → ℤ (整数), \mathbb{Q} → ℚ (有理数)
    // \mathbb{C} → ℂ (复数)
    val blackboardBold = FontFamily(Font(Res.font.msbm10))

    // msam10 - AMS 扩展符号 A
    // 包含 \mathcal{} 花体字母：𝓐𝓑𝓒...
    // 常用于集合论、拓扑学：\mathcal{F} (滤子), \mathcal{T} (拓扑)
    val calligraphic = FontFamily(Font(Res.font.msam10))

    // eufm10 - 欧拉哥特体
    // \mathfrak{g} → 𝔤 (李代数)
    // \mathfrak{sl} → 𝔰𝔩 (特殊线性李代数)
    val fraktur = FontFamily(Font(Res.font.eufm10))

    // rsfs10 - Ralph Smith 正式手写体
    // \mathscr{L} → 𝓛 (拉格朗日量), \mathscr{H} → ℋ (哈密顿量)
    val script = FontFamily(Font(Res.font.rsfs10))

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
        script = script
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
