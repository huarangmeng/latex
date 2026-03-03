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

import com.hrm.latex.base.log.HLog
import com.hrm.latex.renderer.model.LatexFontFamilies

/**
 * MathFontProvider 工厂。
 *
 * 根据 [MathFont] 配置创建对应的 [MathFontProvider] 实例。
 */
internal object MathFontProviderFactory {
    private const val TAG = "MathFontProviderFactory"

    /**
     * 根据 MathFont 配置创建 Provider。
     *
     * @param mathFont 用户配置的数学字体
     * @param defaultFontFamilies 内置 KaTeX 字体家族（用于 Default 和 fallback）
     * @return MathFontProvider 实例
     */
    fun create(
        mathFont: MathFont,
        defaultFontFamilies: LatexFontFamilies
    ): MathFontProvider {
        HLog.i(TAG, "create: mathFont=${mathFont::class.simpleName}")
        return when (mathFont) {
            is MathFont.Default -> {
                // Default 的 OTF 尚未加载完成时，走 TTF 降级
                TtfFontSetProvider(defaultFontFamilies)
            }

            is MathFont.KaTeXTTF -> {
                TtfFontSetProvider(defaultFontFamilies)
            }

            is MathFont.OTF -> {
                val otfFontBytes = defaultFontFamilies.mainBytes
                val otfFontFamily = mathFont.fontFamily
                if (otfFontBytes != null && otfFontFamily != null) {
                    try {
                        val provider = OtfMathFontProvider(otfFontBytes, otfFontFamily)
                        HLog.i(TAG, "OtfMathFontProvider created successfully, " +
                                "bytesSize=${otfFontBytes.size}")
                        provider
                    } catch (e: Exception) {
                        HLog.e(TAG, "OtfMathFontProvider creation failed, " +
                                "fallback to TtfFontSetProvider", e)
                        TtfFontSetProvider(defaultFontFamilies)
                    }
                } else {
                    // OTF 字节未提供（不应发生，MathFont.OTF 构造时必须传入 fontBytes）
                    HLog.w(TAG, "OTF bytes not available in fontFamilies, " +
                            "using TtfFontSetProvider as fallback")
                    TtfFontSetProvider(defaultFontFamilies)
                }
            }

            is MathFont.TTF -> {
                TtfFontSetProvider(mathFont.fontFamilies)
            }
        }
    }
}
