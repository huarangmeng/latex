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

package com.hrm.latex.renderer.utils

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.RenderContext

/**
 * 字体类别，用于选择对应的字体家族
 */
enum class FontCategory {
    ROMAN,           // 正文文本
    MATH_ITALIC,     // 数学变量
    SYMBOL,          // 运算符、小型定界符
    EXTENSION,       // 大型运算符、大型定界符
    SANS_SERIF,
    MONOSPACE,
    BLACKBOARD_BOLD,
    CALLIGRAPHIC,
    FRAKTUR,
    SCRIPT
}

/**
 * 集中管理字体回退策略。
 *
 * 所有字体选择逻辑归口此处，Measurer 中禁止直接判断平台选择字体。
 * 新增平台字体缺陷只需在 [knownDefects] 中加一行，无需修改多个 Measurer 文件。
 */
internal object FontResolver {

    /**
     * 已知在特定平台有渲染缺陷的字形映射。
     * key = 字形字符串, value = 需要回退的平台集合。
     *
     * cmsy10 使用 TeX 内部编码，桌面端字体引擎解码方式不同，
     * 导致 ( ) [ ] 在 JVM/JS/WASM 平台字形错误。
     */
    private val knownDefects: Map<String, Set<PlatformType>> = mapOf(
        "(" to setOf(PlatformType.JVM, PlatformType.JS, PlatformType.WASM),
        ")" to setOf(PlatformType.JVM, PlatformType.JS, PlatformType.WASM),
        "[" to setOf(PlatformType.JVM, PlatformType.JS, PlatformType.WASM),
        "]" to setOf(PlatformType.JVM, PlatformType.JS, PlatformType.WASM),
    )

    /**
     * 解析给定字形应使用的字体家族
     *
     * @param glyph 要渲染的字形字符串
     * @param category 字体类别（决定使用哪个字体槽位）
     * @param fontFamilies 已加载的字体家族集合
     * @param platform 当前运行平台
     * @return 应使用的 FontFamily
     */
    fun resolve(
        glyph: String,
        category: FontCategory,
        fontFamilies: LatexFontFamilies?,
        platform: PlatformType = getCurrentPlatform()
    ): FontFamily {
        // 1. 检查已知缺陷
        if (knownDefects[glyph]?.contains(platform) == true) {
            return getFallbackFont(platform)
        }
        // 2. 使用内嵌字体
        return getFont(category, fontFamilies) ?: getFallbackFont(platform)
    }

    /**
     * 获取定界符渲染上下文
     *
     * 统一替代散落在 MathMeasurer/DelimiterMeasurer/MatrixMeasurer 中的 delimiterContext 方法
     */
    fun delimiterContext(
        context: RenderContext,
        delimiter: String = "(",
        scale: Float = 1.0f
    ): RenderContext {
        val fontWeight = compensatedFontWeight(400, scale)
        val fontFamily = resolve(delimiter, FontCategory.SYMBOL, context.fontFamilies)

        return context.copy(
            fontStyle = FontStyle.Normal,
            fontFamily = fontFamily,
            fontWeight = fontWeight
        )
    }

    /**
     * 根据缩放比例计算补偿后的 FontWeight
     *
     * 字号放大后笔画变粗，需降低 weight 补偿：
     * - scaleFactor = 1.0 → weight 不变
     * - scaleFactor = 2.0 → weight 降至约 100
     */
    fun compensatedFontWeight(baseWeight: Int, scaleFactor: Float): FontWeight {
        val compensated = when {
            scaleFactor <= 1.0f -> baseWeight
            scaleFactor >= 2.0f -> 100
            else -> {
                val t = (scaleFactor - 1.0f) / 1.0f
                (baseWeight - t * (baseWeight - 100)).toInt().coerceIn(100, baseWeight)
            }
        }
        return FontWeight(compensated)
    }

    private fun getFont(category: FontCategory, fontFamilies: LatexFontFamilies?): FontFamily? {
        if (fontFamilies == null) return null
        return when (category) {
            FontCategory.ROMAN -> fontFamilies.roman
            FontCategory.MATH_ITALIC -> fontFamilies.mathItalic
            FontCategory.SYMBOL -> fontFamilies.symbol
            FontCategory.EXTENSION -> fontFamilies.extension
            FontCategory.SANS_SERIF -> fontFamilies.sansSerif
            FontCategory.MONOSPACE -> fontFamilies.monospace
            FontCategory.BLACKBOARD_BOLD -> fontFamilies.blackboardBold
            FontCategory.CALLIGRAPHIC -> fontFamilies.calligraphic
            FontCategory.FRAKTUR -> fontFamilies.fraktur
            FontCategory.SCRIPT -> fontFamilies.script
        }
    }

    private fun getFallbackFont(platform: PlatformType): FontFamily {
        return when (platform) {
            PlatformType.ANDROID, PlatformType.IOS -> FontFamily.Serif
            else -> FontFamily.SansSerif
        }
    }
}
