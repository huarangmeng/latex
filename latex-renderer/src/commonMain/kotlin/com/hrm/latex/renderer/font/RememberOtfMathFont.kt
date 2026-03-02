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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.base.log.HLog
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

private const val TAG = "RememberOtfMathFont"

/**
 * 异步加载 OTF 字体字节数据并创建 [MathFont.OTF]。
 *
 * **保证**：返回非 null 时，[MathFont.OTF.fontBytes] 一定是已加载完成的有效数据，
 * 不会出现空 ByteArray 导致解析崩溃的问题。
 *
 * 加载完成前返回 null，调用方应在 null 时不渲染或渲染占位符：
 * ```kotlin
 * val mathFont = rememberOtfMathFont(Res.font.latinmodern_math)
 * if (mathFont != null) {
 *     Latex(latex = formula, config = LatexConfig(mathFont = mathFont))
 * }
 * ```
 *
 * @param fontResource Compose Resources 中的 OTF 字体资源
 * @return [MathFont.OTF] 实例（bytes 已加载完成），加载中返回 null
 */
@Composable
fun rememberOtfMathFont(fontResource: FontResource): MathFont? {
    val fontFamily = FontFamily(Font(fontResource))

    var mathFont by remember(fontResource) { mutableStateOf<MathFont?>(null) }

    LaunchedEffect(fontResource) {
        try {
            val environment = getSystemResourceEnvironment()
            val bytes = getFontResourceBytes(environment, fontResource)
            if (bytes.isNotEmpty()) {
                mathFont = MathFont.OTF(bytes, fontFamily)
                HLog.i(TAG, "OTF font loaded successfully, bytesSize=${bytes.size}")
            } else {
                HLog.e(TAG, "OTF font bytes is empty for resource: $fontResource")
            }
        } catch (e: Exception) {
            HLog.e(TAG, "Failed to load OTF font bytes", e)
        }
    }

    return mathFont
}
