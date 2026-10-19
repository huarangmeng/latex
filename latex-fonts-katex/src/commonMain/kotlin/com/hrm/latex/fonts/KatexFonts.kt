package com.hrm.latex.fonts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hrm.latex.base.log.HLog
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.LatexFontRegistry
import kotlin.concurrent.Volatile
import latex.latex_fonts_katex.generated.resources.Res
import latex.latex_fonts_katex.generated.resources.katex_ams_regular
import latex.latex_fonts_katex.generated.resources.katex_caligraphic_bold
import latex.latex_fonts_katex.generated.resources.katex_caligraphic_regular
import latex.latex_fonts_katex.generated.resources.katex_fraktur_bold
import latex.latex_fonts_katex.generated.resources.katex_fraktur_regular
import latex.latex_fonts_katex.generated.resources.katex_main_bold
import latex.latex_fonts_katex.generated.resources.katex_main_bolditalic
import latex.latex_fonts_katex.generated.resources.katex_main_italic
import latex.latex_fonts_katex.generated.resources.katex_main_regular
import latex.latex_fonts_katex.generated.resources.katex_math_bolditalic
import latex.latex_fonts_katex.generated.resources.katex_math_italic
import latex.latex_fonts_katex.generated.resources.katex_sansserif_bold
import latex.latex_fonts_katex.generated.resources.katex_sansserif_italic
import latex.latex_fonts_katex.generated.resources.katex_sansserif_regular
import latex.latex_fonts_katex.generated.resources.katex_script_regular
import latex.latex_fonts_katex.generated.resources.katex_size1_regular
import latex.latex_fonts_katex.generated.resources.katex_size2_regular
import latex.latex_fonts_katex.generated.resources.katex_size3_regular
import latex.latex_fonts_katex.generated.resources.katex_size4_regular
import latex.latex_fonts_katex.generated.resources.katex_typewriter_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

private const val TAG = "KatexFonts"

/**
 * 内置 KaTeX 字体模块：字体资源不再随 latex-renderer 发布，需要完整 KaTeX 字形的宿主
 * 依赖本模块并调用 [KatexFonts.install] 注册；不依赖本模块时渲染器退回系统字体。
 */
public object KatexFonts {
    /** 把内置 KaTeX 字体注册为渲染器默认字体。只需调用一次，建议放在 Application 初始化处。 */
    public fun install() {
        LatexFontRegistry.provider = { katexFontFamilies() }
    }
}

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
 */
@Composable
public fun katexFontFamilies(): LatexFontFamilies {
    val cached = defaultFontFamiliesState
    if (cached != null) {
        if (!fontBytesLoaded) {
            LaunchedEffect(Unit) {
                loadDefaultFontBytes()
            }
        }
        return cached
    }

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

    LaunchedEffect(Unit) {
        loadDefaultFontBytes()
    }

    return families
}

/** 异步加载默认 KaTeX TTF 的 bytes，供 InkBoundsEstimator 精确测量；失败不影响核心渲染。 */
private suspend fun loadDefaultFontBytes() {
    if (fontBytesLoaded) return
    val current = defaultFontFamiliesState ?: return

    try {
        val environment = getSystemResourceEnvironment()
        val mainBytes = getFontResourceBytes(environment, Res.font.katex_main_regular)
        val mathBytes = getFontResourceBytes(environment, Res.font.katex_math_italic)
        val amsBytes = getFontResourceBytes(environment, Res.font.katex_ams_regular)
        val size1Bytes = getFontResourceBytes(environment, Res.font.katex_size1_regular)
        val size2Bytes = getFontResourceBytes(environment, Res.font.katex_size2_regular)
        val size3Bytes = getFontResourceBytes(environment, Res.font.katex_size3_regular)
        val size4Bytes = getFontResourceBytes(environment, Res.font.katex_size4_regular)

        defaultFontFamiliesState = current.copy(
            mainBytes = mainBytes,
            mathBytes = mathBytes,
            amsBytes = amsBytes,
            size1Bytes = size1Bytes,
            size2Bytes = size2Bytes,
            size3Bytes = size3Bytes,
            size4Bytes = size4Bytes
        )
        fontBytesLoaded = true
    } catch (e: Exception) {
        HLog.e(TAG, "KaTeX TTF 字体字节加载失败", e)
        fontBytesLoaded = true
    }
}
