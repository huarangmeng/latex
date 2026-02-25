package com.hrm.latex.renderer.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import latex.latex_renderer.generated.resources.Res
import latex.latex_renderer.generated.resources.katex_ams_regular
import latex.latex_renderer.generated.resources.katex_caligraphic_bold
import latex.latex_renderer.generated.resources.katex_caligraphic_regular
import latex.latex_renderer.generated.resources.katex_fraktur_bold
import latex.latex_renderer.generated.resources.katex_fraktur_regular
import latex.latex_renderer.generated.resources.katex_main_bold
import latex.latex_renderer.generated.resources.katex_main_bolditalic
import latex.latex_renderer.generated.resources.katex_main_italic
import latex.latex_renderer.generated.resources.katex_main_regular
import latex.latex_renderer.generated.resources.katex_math_bolditalic
import latex.latex_renderer.generated.resources.katex_math_italic
import latex.latex_renderer.generated.resources.katex_sansserif_bold
import latex.latex_renderer.generated.resources.katex_sansserif_italic
import latex.latex_renderer.generated.resources.katex_sansserif_regular
import latex.latex_renderer.generated.resources.katex_script_regular
import latex.latex_renderer.generated.resources.katex_size1_regular
import latex.latex_renderer.generated.resources.katex_size2_regular
import latex.latex_renderer.generated.resources.katex_size3_regular
import latex.latex_renderer.generated.resources.katex_size4_regular
import latex.latex_renderer.generated.resources.katex_typewriter_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

/**
 * LaTeX 字体家族配置（基于 KaTeX 字体）
 * LaTeX Font Families Configuration (KaTeX-based)
 *
 * 定义了 LaTeX 渲染所需的核心字体家族。
 * KaTeX 字体使用标准 Unicode 编码，无需 TeX 编码映射。
 *
 * ## 使用场景速查 | Quick Reference
 *
 * | 字段 Field | 实际字体 Font | LaTeX 命令 | 使用场景 Use Case |
 * |-----------|--------------|-----------|------------------|
 * | `main` | KaTeX_Main (Regular/Bold/Italic/BoldItalic) | `\text{}`, `\mathrm{}` | 正文文本、数字、标点、运算符 |
 * | `math` | KaTeX_Math (Italic/BoldItalic) | (默认) | 数学变量: x, y, α, β |
 * | `ams` | KaTeX_AMS-Regular | `\mathbb{}` | AMS 符号、黑板粗体: ℝ, ℕ, ℤ |
 * | `sansSerif` | KaTeX_SansSerif (Regular/Bold/Italic) | `\mathsf{}` | 无衬线文本 |
 * | `monospace` | KaTeX_Typewriter-Regular | `\mathtt{}` | 等宽打字机体 |
 * | `caligraphic` | KaTeX_Caligraphic (Regular/Bold) | `\mathcal{}` | 花体 |
 * | `fraktur` | KaTeX_Fraktur (Regular/Bold) | `\mathfrak{}` | 哥特体 |
 * | `script` | KaTeX_Script-Regular | `\mathscr{}` | 手写花体 |
 * | `size1~size4` | KaTeX_Size1~4 | `\big`, `\Big`, `\bigg`, `\Bigg` | 定界符尺寸 |
 */
data class LatexFontFamilies(
    // === 主字体 Main Font (文本 + 基本数学符号) ===
    val main: FontFamily,             // KaTeX_Main — \text{}, \mathrm{}, 数字, 标点, 运算符, 定界符
    // === 数学字体 Math Font ===
    val math: FontFamily,             // KaTeX_Math — 数学变量默认字体 (斜体)
    // === AMS 符号字体 ===
    val ams: FontFamily,              // KaTeX_AMS — \mathbb{}, AMS 扩展符号
    // === 无衬线 & 等宽 ===
    val sansSerif: FontFamily,        // KaTeX_SansSerif — \mathsf{}
    val monospace: FontFamily,        // KaTeX_Typewriter — \mathtt{}
    // === 装饰字体 ===
    val caligraphic: FontFamily,      // KaTeX_Caligraphic — \mathcal{}
    val fraktur: FontFamily,          // KaTeX_Fraktur — \mathfrak{}
    val script: FontFamily,           // KaTeX_Script — \mathscr{}
    // === 定界符尺寸字体 ===
    val size1: FontFamily,            // KaTeX_Size1 — \big
    val size2: FontFamily,            // KaTeX_Size2 — \Big
    val size3: FontFamily,            // KaTeX_Size3 — \bigg
    val size4: FontFamily,            // KaTeX_Size4 — \Bigg

    // === 字体资源 Font Resources (用于精确 glyph bounds 测量) ===
    /** KaTeX_Main 字体资源 — 主字体精确墨水边界测量所需 */
    val mainResource: FontResource? = null,
    /** KaTeX_Math 字体资源 — 数学斜体精确测量所需 */
    val mathResource: FontResource? = null,
    /** KaTeX_Size1 字体资源 — 定界符精确测量所需 */
    val size1Resource: FontResource? = null,

) {
    /** 根据字体类别获取对应的 FontFamily */
    fun getFont(category: String): FontFamily? = when (category) {
        "main" -> main
        "math" -> math
        "ams" -> ams
        "sansSerif" -> sansSerif
        "monospace" -> monospace
        "caligraphic" -> caligraphic
        "fraktur" -> fraktur
        "script" -> script
        "size1" -> size1
        "size2" -> size2
        "size3" -> size3
        "size4" -> size4
        else -> null
    }
}

/**
 * 创建默认的 LaTeX 字体家族 (KaTeX 字体)
 * Creates default LaTeX font families (KaTeX fonts)
 *
 * KaTeX 字体使用标准 Unicode 编码，所有平台行为一致，
 * 不存在 CM 字体的 TeX 编码兼容性问题。
 */
@Composable
internal fun defaultLatexFontFamilies(): LatexFontFamilies {
    // === 主字体 Main Font — 包含 4 种字重/样式 ===
    val main = FontFamily(
        Font(Res.font.katex_main_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.katex_main_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
        Font(Res.font.katex_main_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.katex_main_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic),
    )

    // === 数学字体 Math Font — 斜体变量 ===
    val math = FontFamily(
        Font(Res.font.katex_math_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.katex_math_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic),
    )

    // === AMS 符号字体 ===
    val ams = FontFamily(Font(Res.font.katex_ams_regular))

    // === 无衬线字体 ===
    val sansSerif = FontFamily(
        Font(Res.font.katex_sansserif_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.katex_sansserif_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
        Font(Res.font.katex_sansserif_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    )

    // === 等宽字体 ===
    val monospace = FontFamily(Font(Res.font.katex_typewriter_regular))

    // === 装饰字体 ===
    val caligraphic = FontFamily(
        Font(Res.font.katex_caligraphic_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.katex_caligraphic_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    )
    val fraktur = FontFamily(
        Font(Res.font.katex_fraktur_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.katex_fraktur_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    )
    val script = FontFamily(Font(Res.font.katex_script_regular))

    // === 定界符尺寸字体 ===
    val size1 = FontFamily(Font(Res.font.katex_size1_regular))
    val size2 = FontFamily(Font(Res.font.katex_size2_regular))
    val size3 = FontFamily(Font(Res.font.katex_size3_regular))
    val size4 = FontFamily(Font(Res.font.katex_size4_regular))

    return LatexFontFamilies(
        main = main,
        math = math,
        ams = ams,
        sansSerif = sansSerif,
        monospace = monospace,
        caligraphic = caligraphic,
        fraktur = fraktur,
        script = script,
        size1 = size1,
        size2 = size2,
        size3 = size3,
        size4 = size4,
        mainResource = Res.font.katex_main_regular,
        mathResource = Res.font.katex_math_italic,
        size1Resource = Res.font.katex_size1_regular,
    )
}
