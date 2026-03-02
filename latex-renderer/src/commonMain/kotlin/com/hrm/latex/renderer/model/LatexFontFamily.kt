package com.hrm.latex.renderer.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hrm.latex.base.log.HLog
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
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.concurrent.Volatile

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

    // === 字体字节数据 Font Bytes (用于精确 glyph bounds 测量 & OTF MATH 表解析) ===
    /** 主字体字节数据 — 精确墨水边界测量 & OTF MATH 表解析所需 */
    val mainBytes: ByteArray? = null,
    /** 数学字体字节数据 — 数学斜体精确测量所需 */
    val mathBytes: ByteArray? = null,
    /** Size1 字体字节数据 — 定界符精确测量所需 */
    val size1Bytes: ByteArray? = null,

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LatexFontFamilies

        if (main != other.main) return false
        if (math != other.math) return false
        if (ams != other.ams) return false
        if (sansSerif != other.sansSerif) return false
        if (monospace != other.monospace) return false
        if (caligraphic != other.caligraphic) return false
        if (fraktur != other.fraktur) return false
        if (script != other.script) return false
        if (size1 != other.size1) return false
        if (size2 != other.size2) return false
        if (size3 != other.size3) return false
        if (size4 != other.size4) return false
        if (!mainBytes.contentEquals(other.mainBytes)) return false
        if (!mathBytes.contentEquals(other.mathBytes)) return false
        if (!size1Bytes.contentEquals(other.size1Bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = main.hashCode()
        result = 31 * result + math.hashCode()
        result = 31 * result + ams.hashCode()
        result = 31 * result + sansSerif.hashCode()
        result = 31 * result + monospace.hashCode()
        result = 31 * result + caligraphic.hashCode()
        result = 31 * result + fraktur.hashCode()
        result = 31 * result + script.hashCode()
        result = 31 * result + size1.hashCode()
        result = 31 * result + size2.hashCode()
        result = 31 * result + size3.hashCode()
        result = 31 * result + size4.hashCode()
        result = 31 * result + (mainBytes?.contentHashCode() ?: 0)
        result = 31 * result + (mathBytes?.contentHashCode() ?: 0)
        result = 31 * result + (size1Bytes?.contentHashCode() ?: 0)
        return result
    }
}

fun createLatexFontFamilies(fonts: Font, fontBytes: ByteArray): LatexFontFamilies {
    val fontFamily = FontFamily(fonts)
    return LatexFontFamilies(
        main = fontFamily,
        math = fontFamily,
        ams = fontFamily,
        sansSerif = fontFamily,
        monospace = fontFamily,
        caligraphic = fontFamily,
        fraktur = fontFamily,
        script = fontFamily,
        size1 = fontFamily,
        size2 = fontFamily,
        size3 = fontFamily,
        size4 = fontFamily,
        mainBytes = fontBytes,
        mathBytes = fontBytes,
        size1Bytes = fontBytes
    )
}

private const val TAG = "LatexFontFamily"

/**
 * 全局默认 LatexFontFamilies 状态。
 *
 * 使用 Compose [mutableStateOf] 作为全局可观察状态：
 * - 首次调用 [defaultLatexFontFamilies] 时初始化（FontFamily 对象 + bytes=null）
 * - 异步加载 bytes 完成后更新为带 bytes 的版本
 * - 所有 Composable（包括不同的 Latex 实例）自动感知状态变化并重组
 *
 * 使用 mutableStateOf 而非普通变量的原因：当 bytes 异步加载完成后更新此状态，
 * 所有读取它的 Composable 会自动触发重组，获得带 bytes 的新版本。
 */
private var defaultFontFamiliesState by mutableStateOf<LatexFontFamilies?>(null)

/** 标记 bytes 异步加载是否已完成，避免多个 Composable 重复触发加载 */
@Volatile
private var fontBytesLoaded = false

/**
 * 获取默认的 LaTeX 字体家族 (KaTeX 字体) — 全局单例
 *
 * 首次调用时通过 @Composable Font() 加载字体资源并构建 LatexFontFamilies，
 * 同时启动异步加载字体 bytes（用于 InkBoundsEstimator 精确测量）。
 * bytes 加载完成后更新全局状态，所有 Composable 自动重组获得完整数据。
 *
 * 所有 Latex composable 实例共享同一对象，避免重复创建 FontFamily 导致内存增长。
 *
 * KaTeX 字体使用标准 Unicode 编码，所有平台行为一致，
 * 不存在 CM 字体的 TeX 编码兼容性问题。
 */
@Composable
internal fun defaultLatexFontFamilies(): LatexFontFamilies {
    // 快速路径：已初始化则直接返回全局状态
    val cached = defaultFontFamiliesState
    if (cached != null) {
        // bytes 尚未加载时，启动异步加载（仅触发一次）
        if (!fontBytesLoaded) {
            LaunchedEffect(Unit) {
                loadDefaultFontBytes()
            }
        }
        return cached
    }

    // 慢速路径：首次调用，通过 @Composable Font() 加载字体资源
    val mainRegular = Font(Res.font.katex_main_regular, weight = FontWeight.Normal, style = FontStyle.Normal)
    val mainBold = Font(Res.font.katex_main_bold, weight = FontWeight.Bold, style = FontStyle.Normal)
    val mainItalic = Font(Res.font.katex_main_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
    val mainBoldItalic = Font(Res.font.katex_main_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic)
    val mathItalic = Font(Res.font.katex_math_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
    val mathBoldItalic = Font(Res.font.katex_math_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic)
    val amsRegular = Font(Res.font.katex_ams_regular)
    val sansSerifRegular = Font(Res.font.katex_sansserif_regular, weight = FontWeight.Normal, style = FontStyle.Normal)
    val sansSerifBold = Font(Res.font.katex_sansserif_bold, weight = FontWeight.Bold, style = FontStyle.Normal)
    val sansSerifItalic = Font(Res.font.katex_sansserif_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
    val typewriterRegular = Font(Res.font.katex_typewriter_regular)
    val caligraphicRegular = Font(Res.font.katex_caligraphic_regular, weight = FontWeight.Normal, style = FontStyle.Normal)
    val caligraphicBold = Font(Res.font.katex_caligraphic_bold, weight = FontWeight.Bold, style = FontStyle.Normal)
    val frakturRegular = Font(Res.font.katex_fraktur_regular, weight = FontWeight.Normal, style = FontStyle.Normal)
    val frakturBold = Font(Res.font.katex_fraktur_bold, weight = FontWeight.Bold, style = FontStyle.Normal)
    val scriptRegular = Font(Res.font.katex_script_regular)
    val size1Regular = Font(Res.font.katex_size1_regular)
    val size2Regular = Font(Res.font.katex_size2_regular)
    val size3Regular = Font(Res.font.katex_size3_regular)
    val size4Regular = Font(Res.font.katex_size4_regular)

    val families = LatexFontFamilies(
        main = FontFamily(mainRegular, mainBold, mainItalic, mainBoldItalic),
        math = FontFamily(mathItalic, mathBoldItalic),
        ams = FontFamily(amsRegular),
        sansSerif = FontFamily(sansSerifRegular, sansSerifBold, sansSerifItalic),
        monospace = FontFamily(typewriterRegular),
        caligraphic = FontFamily(caligraphicRegular, caligraphicBold),
        fraktur = FontFamily(frakturRegular, frakturBold),
        script = FontFamily(scriptRegular),
        size1 = FontFamily(size1Regular),
        size2 = FontFamily(size2Regular),
        size3 = FontFamily(size3Regular),
        size4 = FontFamily(size4Regular),
    )
    defaultFontFamiliesState = families

    // 启动异步加载字体 bytes
    LaunchedEffect(Unit) {
        loadDefaultFontBytes()
    }

    return families
}

/**
 * 异步加载默认 KaTeX TTF 字体的 bytes 数据，更新全局缓存。
 *
 * bytes 用于 InkBoundsEstimator 精确墨水边界测量，不影响核心渲染。
 * 加载完成后通过 [mutableStateOf] 触发所有依赖 Composable 重组。
 */
private suspend fun loadDefaultFontBytes() {
    if (fontBytesLoaded) return
    val current = defaultFontFamiliesState ?: return

    try {
        val environment = getSystemResourceEnvironment()
        val mainBytes = getFontResourceBytes(environment, Res.font.katex_main_regular)
        val mathBytes = getFontResourceBytes(environment, Res.font.katex_math_italic)
        val size1Bytes = getFontResourceBytes(environment, Res.font.katex_size1_regular)

        defaultFontFamiliesState = current.copy(
            mainBytes = mainBytes,
            mathBytes = mathBytes,
            size1Bytes = size1Bytes
        )
        fontBytesLoaded = true
    } catch (e: Exception) {
        HLog.e(TAG, "KaTeX TTF 字体字节加载失败", e)
        fontBytesLoaded = true
    }
}
