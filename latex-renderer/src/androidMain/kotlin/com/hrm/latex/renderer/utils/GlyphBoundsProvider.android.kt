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

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import java.io.File
import java.nio.ByteBuffer
import java.util.WeakHashMap

/**
 * Android 平台：使用 android.graphics.Paint.getTextBounds() 获取精确墨水边界。
 *
 * android.graphics.Paint.getTextBounds() 返回的是字形的精确像素边界矩形 (tight bounding box)，
 * 不包含行框的 ascent/descent 空白。
 *
 * Rect 坐标系：baseline 在 y=0，上方为负值，下方为正值。
 * - rect.top 为负值（baseline 以上）
 * - rect.bottom 为正值或零（baseline 以下）
 *
 * Typeface 由 [GlyphTypefaceCache] 按字体字节数组缓存：同一份 KaTeX 字体会被大型运算符、
 * 定界符的测量反复请求，每次都重建 Typeface（低版本还要先写临时文件）会让每个公式的
 * 首次测量在主线程上付一次磁盘 I/O。
 */
actual fun measureGlyphBounds(
    text: String,
    fontSizePx: Float,
    fontBytes: ByteArray,
    fontWeightValue: Int
): GlyphBounds? {
    if (text.isEmpty()) return null
    val typeface = GlyphTypefaceCache.get(fontBytes) ?: return null

    return try {
        val paint = Paint().apply {
            this.typeface = typeface
            this.textSize = fontSizePx
            this.isAntiAlias = true
        }

        val rect = Rect()
        paint.getTextBounds(text, 0, text.length, rect)

        // rect.top 是负值（baseline 以上的部分），取绝对值得到 ascent
        // rect.bottom 是正值或零（baseline 以下的部分）
        val ascentPx = (-rect.top).toFloat().coerceAtLeast(0f)
        val descentPx = rect.bottom.toFloat().coerceAtLeast(0f)
        val inkWidth = rect.width().toFloat().coerceAtLeast(0f)

        GlyphBounds(ascentPx, descentPx, inkWidth, inkTopPx = rect.top.toFloat(), inkBottomPx = rect.bottom.toFloat())
    } catch (e: Exception) {
        null
    }
}

/**
 * 字体字节数组 → Typeface 的缓存。
 *
 * 以字节数组的身份作为 key：ByteArray 不重写 equals/hashCode，[WeakHashMap] 对它就是
 * 身份比较，既省掉对几百 KB 字体做内容哈希，又不会把调用方已经丢弃的自定义字体一直钉在内存里。
 * 渲染侧的内置字体字节由 LatexFontFamilies 持有、生命周期内不变，所以同一字体只会创建一次。
 *
 * 创建失败的字体也记为 null，避免对损坏的字节反复重试。
 */
private object GlyphTypefaceCache {
    private val typefaces = WeakHashMap<ByteArray, Typeface?>()

    @Synchronized
    fun get(bytes: ByteArray): Typeface? {
        if (typefaces.containsKey(bytes)) return typefaces[bytes]
        val typeface = try {
            create(bytes)
        } catch (e: Exception) {
            null
        }
        typefaces[bytes] = typeface
        return typeface
    }

    private fun create(bytes: ByteArray): Typeface? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ 可以直接从内存建字体，不再落盘。Font.Builder 要求 direct ByteBuffer，
            // 且构建出的 Font 会持有这块缓冲区，因此缓冲区的生命周期跟随缓存中的 Typeface。
            val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes)
            buffer.rewind()
            val font = Font.Builder(buffer).build()
            val family = FontFamily.Builder(font).build()
            return Typeface.CustomFallbackBuilder(family).build()
        }

        // 低版本没有从内存建 Typeface 的 API，仍需临时文件，但只在首次创建时写一次
        val tempFile = File.createTempFile("glyph_font_", ".ttf")
        return try {
            tempFile.writeBytes(bytes)
            Typeface.createFromFile(tempFile)
        } finally {
            tempFile.delete()
        }
    }
}
