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

/**
 * 字体字节数据缓存。
 *
 * 持有从 Compose Resources 加载的字体 TTF 文件字节数据，
 * 供 [measureGlyphBounds] 在各平台创建原生字体对象进行精确墨水边界测量。
 *
 * 字节数据在 Composable 层通过 Res.readBytes() 异步加载后缓存于此。
 * Measurer 层通过 [RenderContext.fontBytesCache] 引用获取。
 *
 * 使用 class 而非 data class：字节数组不应参与 equals/hashCode。
 */
class FontBytesCache(
    /** cmex10.ttf — 大型运算符/定界符 (∑, ∫, ∏, √, 大括号) */
    val extensionBytes: ByteArray? = null,
    /** cmsy10.ttf — 运算符/小型定界符 */
    val symbolBytes: ByteArray? = null,
    /** cmmi10.ttf — 数学斜体变量/希腊字母 */
    val mathItalicBytes: ByteArray? = null,
    /** cmr10.ttf — 正文文本 */
    val romanBytes: ByteArray? = null
) {
    /** 是否已加载扩展字体（大型运算符所需） */
    val hasExtension: Boolean get() = extensionBytes != null
}
