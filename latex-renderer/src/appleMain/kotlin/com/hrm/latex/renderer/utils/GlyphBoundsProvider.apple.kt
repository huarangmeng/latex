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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hrm.latex.renderer.utils

import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.impl.use
import platform.Foundation.NSLock

private val glyphTypefaceLock = NSLock()
private val glyphTypefaceCache = FontResourceCache<Typeface>(
    create = { bytes -> Data.makeFromBytes(bytes).use { FontMgr.default.makeFromData(it) } },
    dispose = { it.close() }
)

/**
 * Apple 平台（iOS/macOS）：使用 Skia (org.jetbrains.skia via Skiko) API 获取精确墨水边界。
 *
 * 按字体字节数组身份复用有界 Typeface 缓存，临时 Data 和 Font 在使用后立即释放。
 * 缓存访问与整段测量共用一把锁，避免其它线程淘汰仍在使用的原生字体。
 */
actual fun measureGlyphBounds(
    text: String,
    fontSizePx: Float,
    fontBytes: ByteArray,
    fontWeightValue: Int
): GlyphBounds? {
    if (text.isEmpty()) return null

    return try {
        glyphTypefaceLock.lock()
        try {
            glyphTypefaceCache.withValue(fontBytes) { typeface ->
                Font(typeface, fontSizePx).use { font ->
                    val glyphIds = font.getStringGlyphs(text)
                    if (glyphIds.isEmpty()) return@use null

                    val glyphBoundsArray = font.getBounds(glyphIds)
                    if (glyphBoundsArray.isEmpty()) return@use null

                    var minTop = Float.MAX_VALUE
                    var maxBottom = Float.MIN_VALUE

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

                    GlyphBounds(ascentPx, descentPx, inkWidth)
                }
            }
        } finally {
            glyphTypefaceLock.unlock()
        }
    } catch (e: Exception) {
        null
    }
}
