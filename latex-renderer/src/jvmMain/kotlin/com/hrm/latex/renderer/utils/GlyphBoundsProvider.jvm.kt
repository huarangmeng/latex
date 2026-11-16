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

import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.impl.use

private val glyphTypefaceCache = GlyphTypefaceCache()

/**
 * JVM Desktop 平台：使用 Skia (org.jetbrains.skia) API 获取精确墨水边界。
 *
 * Compose for Desktop 底层使用 Skia 渲染引擎，可以直接访问 Skia API。
 * 按字体字节数组的身份复用 Typeface，避免为每个字形重新解析整份字体。
 * 临时 Data、Font 在本次调用结束时释放，Typeface 由有界缓存管理。
 */
actual fun measureGlyphBounds(
    text: String,
    fontSizePx: Float,
    fontBytes: ByteArray,
    fontWeightValue: Int
): GlyphBounds? {
    if (text.isEmpty()) return null

    return try {
        glyphTypefaceCache.measure(text, fontSizePx, fontBytes)
    } catch (e: Exception) {
        null
    }
}

/**
 * JVM 数组的 equals/hashCode 按身份比较，不扫描字体内容。
 * 字体字节在缓存存续期间必须保持不变；字号属于临时 Font，无需为每个字号保留 Typeface。
 * 整段测量和清理共用同一把锁，避免其它线程淘汰仍在使用的原生字体。
 */
internal class GlyphTypefaceCache(
    private val maxSize: Int = 32,
    private val createTypeface: (ByteArray) -> Typeface? = ::createGlyphTypeface
) {
    init {
        require(maxSize > 0) { "字体缓存容量必须大于 0" }
    }

    private val typefaces = LinkedHashMap<ByteArray, Typeface>(16, 0.75f, true)

    @Synchronized
    fun measure(text: String, fontSizePx: Float, fontBytes: ByteArray): GlyphBounds? {
        if (text.isEmpty()) return null
        val typeface = typefaces[fontBytes] ?: (createTypeface(fontBytes) ?: return null).also {
            typefaces[fontBytes] = it
            if (typefaces.size > maxSize) {
                val entries = typefaces.entries.iterator()
                val eldest = entries.next()
                entries.remove()
                eldest.value.close()
            }
        }
        return Font(typeface, fontSizePx).use { font ->
            // 获取字符串对应的 glyph IDs。
            val glyphIds = font.getStringGlyphs(text)
            if (glyphIds.isEmpty()) return@use null

            // 获取每个 glyph 的精确 bounding box。
            val glyphBoundsArray = font.getBounds(glyphIds)
            if (glyphBoundsArray.isEmpty()) return@use null

            // 合并多字符边界；坐标原点为基线左端，top 在基线上方为负值。
            var minTop = Float.MAX_VALUE
            var maxBottom = Float.NEGATIVE_INFINITY
            for (rect in glyphBoundsArray) {
                if (rect.width > 0 || rect.height > 0) {
                    minTop = minOf(minTop, rect.top)
                    maxBottom = maxOf(maxBottom, rect.bottom)
                }
            }
            if (minTop == Float.MAX_VALUE) return@use null

            val ascentPx = (-minTop).coerceAtLeast(0f)
            val descentPx = maxBottom.coerceAtLeast(0f)
            val inkWidth = font.measureTextWidth(text)
            GlyphBounds(ascentPx, descentPx, inkWidth, inkTopPx = minTop, inkBottomPx = maxBottom)
        }
    }

    @Synchronized
    fun clear() {
        for (typeface in typefaces.values) typeface.close()
        typefaces.clear()
    }
}

private fun createGlyphTypeface(fontBytes: ByteArray): Typeface? =
    Data.makeFromBytes(fontBytes).use { data -> FontMgr.default.makeFromData(data) }
