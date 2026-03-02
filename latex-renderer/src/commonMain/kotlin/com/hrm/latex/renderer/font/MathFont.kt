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

/**
 * 数学字体配置。决定排版参数和字体的来源。
 *
 * 使用示例：
 * ```kotlin
 * // 方式 1：默认（KaTeX 内置字体）
 * LatexConfig(mathFont = MathFont.Default)
 *
 * // 方式 2：使用带 MATH 表的 OTF 字体
 * val stixBytes = context.assets.open("STIXTwoMath-Regular.otf").readBytes()
 * val stixFamily = FontFamily(Font(stixBytes))
 * LatexConfig(mathFont = MathFont.OTF(stixBytes, stixFamily))
 *
 * // 方式 3：使用自定义 TTF 字体集
 * LatexConfig(mathFont = MathFont.TTF(customFontFamilies))
 * ```
 */
sealed class MathFont {

    /**
     * 使用内置的 KaTeX TTF 字体集（默认行为）。
     *
     * 等价于当前的渲染方案，不改变任何现有行为。
     */
    data object Default : MathFont()

    /**
     * 使用带 OpenType MATH 表的 OTF 字体文件。
     *
     * 单个 OTF 文件中包含数学排版所需的全部信息：
     * - MathConstants: ~60 个精确排版常量
     * - MathGlyphInfo: 逐字形斜体修正、重音附着点
     * - MathVariants: 定界符尺寸变体 + 字形组装部件
     *
     * @param fontBytes OTF 字体文件的字节数据（同步传入，避免异步加载时序问题）
     * @param fontFamily 从 OTF 文件创建的 Compose FontFamily
     */
    class OTF(
        val fontBytes: ByteArray,
        val fontFamily: FontFamily
    ) : MathFont()

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
     * - [Default]：返回 null，调用方应 fallback 到 [defaultLatexFontFamilies]
     * - [OTF]：用 OTF 的 fontFamily 填充所有 12 个槽位（单字体包含全部字形）
     * - [TTF]：直接返回包装的 [LatexFontFamilies]
     */
    fun fontFamiliesOrNull(): LatexFontFamilies? = when (this) {
        is Default -> null
        is OTF -> LatexFontFamilies(
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
        is TTF -> fontFamilies
    }
}
