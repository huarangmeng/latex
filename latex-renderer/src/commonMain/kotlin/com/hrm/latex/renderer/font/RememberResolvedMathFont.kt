package com.hrm.latex.renderer.font

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.base.log.HLog
import latex.latex_renderer.generated.resources.Res
import latex.latex_renderer.generated.resources.latinmodern_math
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.concurrent.Volatile

private const val TAG = "Latex-font"

// ========== 内置 OTF 全局缓存 ==========

/**
 * 内置 Latin Modern Math OTF 字体的全局状态。
 *
 * 使用 Compose [mutableStateOf] 作为全局可观察状态：
 * - 值为 null 时表示尚未加载（使用 KaTeX TTF 降级渲染）
 * - 加载完成后更新为 [MathFont.OTF]（bytes + fontFamily）
 * - 所有使用 [MathFont.Default] 的 Latex 实例自动感知状态变化并重组升级
 */
private var defaultOtfState by mutableStateOf<MathFont?>(null)

/** 标记内置 OTF bytes 异步加载是否已启动，避免多个 Composable 重复触发加载 */
@Volatile
private var defaultOtfLoadStarted = false

/**
 * 解析 [MathFont] 配置，处理 OTF FontResource 的异步加载。
 *
 * - [MathFont.Default]：使用内置 `latinmodern-math.otf`，全局缓存，
 *   所有 Latex 实例共享同一加载结果。加载前返回 [MathFont.KaTeXTTF]（TTF 降级），
 *   加载完成后全局状态更新，所有读取者自动重组升级到 OTF。
 * - [MathFont.OTF]（FontResource 方式）：per-resource 缓存，异步加载自定义 OTF 字体。
 * - [MathFont.KaTeXTTF]、[MathFont.TTF]、[MathFont.OTF]（预加载方式）：直接透传。
 */
@Composable
internal fun rememberResolvedMathFont(mathFont: MathFont): MathFont {
    return when (mathFont) {
        is MathFont.Default -> {
            // 全局缓存：所有 MathFont.Default 的 Latex 实例共享
            rememberDefaultOtf()
        }

        is MathFont.OTF -> {
            if (mathFont.fontResource != null) {
                // 自定义 OTF — FontResource 方式，per-resource 缓存
                rememberCustomOtfAsync(mathFont.fontResource)
            } else {
                // 预加载方式，直接透传
                mathFont
            }
        }

        // KaTeXTTF 和 TTF 直接透传
        else -> mathFont
    }
}

/**
 * 内置 OTF 字体全局加载 — 全局单例模式。
 *
 * 与 [defaultLatexFontFamilies] 采用相同的全局缓存策略：
 * - 首次 Composable 调用时通过 `@Composable Font()` 创建 FontFamily
 * - 异步加载 bytes 后更新全局 [defaultOtfState]
 * - 所有读取者自动重组获得完整的 [MathFont.OTF]
 * - 加载仅触发一次，多个 Latex 实例共享同一结果
 */
@Composable
private fun rememberDefaultOtf(): MathFont {
    // 快速路径：已加载完成，直接返回全局缓存
    val cached = defaultOtfState
    if (cached != null) {
        return cached
    }

    // 首次调用：创建 FontFamily（需要 @Composable 上下文）
    val fontFamily = FontFamily(Font(Res.font.latinmodern_math))

    // 启动异步加载 bytes（仅触发一次）
    if (!defaultOtfLoadStarted) {
        LaunchedEffect(Unit) {
            loadDefaultOtfBytes(fontFamily)
        }
    }

    // bytes 尚未就绪，降级返回 KaTeXTTF
    return MathFont.KaTeXTTF
}

/**
 * 异步加载内置 Latin Modern Math OTF bytes，更新全局缓存。
 *
 * 加载完成后通过 [mutableStateOf] 触发所有使用 [MathFont.Default] 的 Composable 重组。
 */
private suspend fun loadDefaultOtfBytes(fontFamily: FontFamily) {
    if (defaultOtfLoadStarted) return
    defaultOtfLoadStarted = true

    try {
        val environment = getSystemResourceEnvironment()
        val bytes = getFontResourceBytes(environment, Res.font.latinmodern_math)
        if (bytes.isNotEmpty()) {
            defaultOtfState = MathFont.OTF(bytes, fontFamily)
            HLog.i(TAG, "Default OTF font loaded: ${bytes.size} bytes")
        } else {
            HLog.e(TAG, "Default OTF font bytes empty, staying with TTF fallback")
        }
    } catch (e: Exception) {
        HLog.e(TAG, "Default OTF font load failed, staying with TTF fallback", e)
    }
}

// ========== 自定义 OTF per-resource 缓存 ==========

/**
 * 自定义 OTF FontResource 异步加载 — per-resource 缓存。
 *
 * 每个不同的 [fontResource] 独立缓存。
 * 加载前返回 [MathFont.KaTeXTTF]（TTF 降级渲染），
 * 加载完成后返回 [MathFont.OTF]（bytes + fontFamily），触发重组升级。
 */
@Composable
private fun rememberCustomOtfAsync(fontResource: FontResource): MathFont {
    val fontFamily = FontFamily(Font(fontResource))

    var resolved by remember(fontResource) { mutableStateOf<MathFont>(MathFont.KaTeXTTF) }

    LaunchedEffect(fontResource) {
        try {
            val environment = getSystemResourceEnvironment()
            val bytes = getFontResourceBytes(environment, fontResource)
            if (bytes.isNotEmpty()) {
                resolved = MathFont.OTF(bytes, fontFamily)
                HLog.i(TAG, "Custom OTF font loaded: ${bytes.size} bytes")
            } else {
                HLog.e(TAG, "Custom OTF font bytes empty, staying with TTF fallback")
            }
        } catch (e: Exception) {
            HLog.e(TAG, "Custom OTF font load failed, staying with TTF fallback", e)
        }
    }

    return resolved
}
