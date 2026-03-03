/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.renderer.font

import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.renderer.model.LatexFontFamilies
import org.jetbrains.compose.resources.FontResource

/**
 * 数学字体配置。决定排版参数和字体的来源。
 *
 * 使用示例：
 * ```kotlin
 * // 方式 1：默认 — 内置 Latin Modern Math OTF（高精度排版，推荐）
 * LatexConfig()
 *
 * // 方式 2：使用内置 KaTeX TTF 字体集
 * LatexConfig(mathFont = MathFont.KaTeXTTF)
 *
 * // 方式 3：使用自定义 OTF 字体（传入 FontResource，内部异步加载）
 * LatexConfig(mathFont = MathFont.OTF(Res.font.stix_two_math))
 *
 * // 方式 4：使用自定义 OTF 字体（预加载 — 已有 bytes 和 FontFamily）
 * val stixBytes = context.assets.open("STIXTwoMath-Regular.otf").readBytes()
 * val stixFamily = FontFamily(Font(stixBytes))
 * LatexConfig(mathFont = MathFont.OTF(stixBytes, stixFamily))
 *
 * // 方式 5：使用自定义 TTF 字体集
 * LatexConfig(mathFont = MathFont.TTF(customFontFamilies))
 * ```
 */
sealed class MathFont {

    /**
     * 使用内置的 Latin Modern Math OTF 字体（默认行为）。
     *
     * 内部自动异步加载 `latinmodern-math.otf` 字体文件：
     * - 加载前先用 KaTeX TTF 降级渲染
     * - 加载完成后自动重组升级到 OTF 高精度排版
     *
     * 用户无需处理任何异步逻辑，开箱即用。
     */
    data object Default : MathFont()

    /**
     * 使用内置的 KaTeX TTF 字体集。
     *
     * 适用于不需要 OTF MATH 表高精度排版的场景，
     * 或需要保持与旧版本行为一致的场景。
     */
    data object KaTeXTTF : MathFont()

    /**
     * 使用自定义的带 OpenType MATH 表的 OTF 字体文件。
     *
     * 单个 OTF 文件中包含数学排版所需的全部信息：
     * - MathConstants: ~60 个精确排版常量
     * - MathGlyphInfo: 逐字形斜体修正、重音附着点
     * - MathVariants: 定界符尺寸变体 + 字形组装部件
     *
     * 支持两种构造方式：
     *
     * **1. FontResource 方式（推荐）**：传入 Compose Resources 的字体资源引用，
     * bytes 由 [Latex] 组件内部异步加载。加载前先用 KaTeX TTF 渲染，
     * 加载完成后自动重组升级到 OTF 渲染，用户无需处理异步逻辑。
     *
     * **2. 预加载方式**：调用方已持有 fontBytes 和 fontFamily（如从 assets 直接读取），
     * 传入后立即可用，无需异步等待。
     */
    class OTF : MathFont {
        /** OTF 字体文件的字节数据。null 表示尚未加载（FontResource 方式）。 */
        val fontBytes: ByteArray?

        /** 从 OTF 文件创建的 Compose FontFamily。null 表示尚未加载。 */
        val fontFamily: FontFamily?

        /** Compose Resources 字体资源引用（FontResource 方式时非 null）。 */
        val fontResource: FontResource?

        /**
         * FontResource 方式：bytes 将由 Latex 组件内部异步加载。
         *
         * 加载前先用 KaTeX TTF 降级渲染，加载完成后自动升级到 OTF。
         */
        constructor(fontResource: FontResource) {
            this.fontBytes = null
            this.fontFamily = null
            this.fontResource = fontResource
        }

        /**
         * 预加载方式：调用方已持有 fontBytes 和 fontFamily。
         */
        constructor(fontBytes: ByteArray, fontFamily: FontFamily) {
            this.fontBytes = fontBytes
            this.fontFamily = fontFamily
            this.fontResource = null
        }

        /** bytes 是否已加载完成 */
        val isLoaded: Boolean get() = fontBytes != null && fontFamily != null
    }

    /**
     * 使用自定义的 TTF 字体集。
     *
     * 适用于用户想使用 KaTeX 以外的 TTF 字体组合的场景。
     *
     * @param fontFamilies 12 槽位的字体家族配置
     */
    data class TTF(
        val fontFamilies: LatexFontFamilies
    ) : MathFont()

    /**
     * 解析此配置中的 [LatexFontFamilies]。
     *
     * - [Default]：返回 null（内部由 rememberResolvedMathFont 处理异步 OTF 加载）
     * - [KaTeXTTF]：返回 null，调用方 fallback 到 [defaultLatexFontFamilies]
     * - [OTF]（已加载）：用 OTF 的 fontFamily 填充所有 12 个槽位
     * - [OTF]（未加载）：返回 null，调用方 fallback 到默认字体（TTF 降级）
     * - [TTF]：直接返回包装的 [LatexFontFamilies]
     */
    fun fontFamiliesOrNull(): LatexFontFamilies? = when (this) {
        is Default -> null
        is KaTeXTTF -> null
        is OTF -> {
            val ff = fontFamily
            val fb = fontBytes
            if (ff != null && fb != null) {
                LatexFontFamilies(
                    main = ff, math = ff, ams = ff,
                    sansSerif = ff, monospace = ff, caligraphic = ff,
                    fraktur = ff, script = ff,
                    size1 = ff, size2 = ff, size3 = ff, size4 = ff,
                    mainBytes = fb, mathBytes = fb, size1Bytes = fb
                )
            } else {
                null  // 尚未加载，降级到默认 TTF
            }
        }

        is TTF -> fontFamilies
    }
}
