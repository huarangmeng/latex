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

package com.hrm.latex.renderer.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.defaultLatexFontFamilies
import com.hrm.latex.renderer.model.toContext
import com.hrm.latex.renderer.utils.FontBytesCache
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.math.ceil

private const val TAG = "LatexExporter"

/**
 * LaTeX 图片导出器
 *
 * 在 Composable 作用域中使用 [rememberLatexExporter] 创建实例。
 * 提供统一的 [export] 方法将 LaTeX 渲染并编码为图片。
 *
 * 导出原理：使用放大后的 fontSize 重新执行完整的 Measure → Draw 流程，
 * 通过 [LatexRenderer] 与 Latex Composable 共享完全相同的测量和绘制逻辑，
 * 确保导出结果与屏幕显示 100% 一致。后续对渲染逻辑的任何修改只需改一处。
 *
 * 使用示例：
 * ```kotlin
 * val exporter = rememberLatexExporter()
 * val scope = rememberCoroutineScope()
 *
 * Button(onClick = {
 *     scope.launch(Dispatchers.Default) {
 *         // 导出 PNG（默认 2x 分辨率）
 *         val result = exporter.export("E = mc^2")
 *         val pngBytes = result?.bytes
 *
 *         // 导出 JPEG（3x 分辨率，质量 85）
 *         val jpegResult = exporter.export(
 *             latex = "\\frac{a}{b}",
 *             exportConfig = ExportConfig(
 *                 scale = 3f,
 *                 format = ImageFormat.JPEG,
 *                 quality = 85
 *             )
 *         )
 *
 *         // 直接在 Compose 中展示
 *         jpegResult?.imageBitmap
 *         // 文件大小
 *         jpegResult?.bytes?.size
 *     }
 * }) { Text("导出") }
 * ```
 */
class LatexExporterState internal constructor(
    private val density: Density,
    private val textMeasurer: androidx.compose.ui.text.TextMeasurer,
    private val fontFamilies: LatexFontFamilies,
    internal var fontBytesCache: FontBytesCache?
) {
    private val parser = IncrementalLatexParser()

    /**
     * 将 LaTeX 字符串导出为图片
     *
     * 一次调用同时产出 [ImageBitmap]（可直接展示）和编码后的 [ByteArray]（可保存/分享），
     * 避免重复渲染。
     *
     * 实现原理：
     * 1. 将 `config.fontSize * scale` 作为实际渲染字号
     * 2. 调用 [LatexRenderer.measure] 进行测量（与 Latex Composable 同一份代码）
     * 3. 调用 [LatexRenderer.draw] 绘制到离屏 ImageBitmap（与 Latex Composable 同一份代码）
     * 4. 使用 [encodeToFormat] 编码为目标格式字节数组
     *
     * @param latex LaTeX 字符串
     * @param config 渲染配置（字号、颜色、背景色等）
     * @param exportConfig 导出配置（缩放倍率、格式、质量、背景透明度）
     * @param isDarkTheme 是否深色模式
     * @return [ExportResult]，包含 ImageBitmap、编码字节、尺寸信息；解析或渲染失败时返回 null
     */
    fun export(
        latex: String,
        config: LatexConfig = LatexConfig(),
        exportConfig: ExportConfig = ExportConfig(),
        isDarkTheme: Boolean = false
    ): ExportResult? {
        if (latex.isBlank()) return null

        return try {
            val document = parseLatex(latex)
            if (document.children.isEmpty()) return null

            val scale = exportConfig.scale.coerceIn(0.5f, 8f)
            val format = exportConfig.format
            val quality = exportConfig.quality.coerceIn(1, 100)
            val resolvedFontFamilies = config.fontFamilies ?: fontFamilies

            // 核心：用放大后的 fontSize 创建渲染上下文，而非用 Canvas scale
            // 这样 TextMeasurer 会以目标字号测量文本，字体引擎以正确字号光栅化
            val scaledConfig = config.copy(fontSize = config.fontSize * scale)
            val context = scaledConfig.toContext(isDarkTheme, resolvedFontFamilies).let {
                if (fontBytesCache != null) it.copy(fontBytesCache = fontBytesCache) else it
            }

            // 使用 LatexRenderer 共享逻辑进行测量（与 LatexDocument 同一份代码）
            val renderResult = LatexRenderer.measure(
                document.children, context, textMeasurer, density
            )

            if (renderResult.layout.width <= 0f || renderResult.layout.height <= 0f) return null

            val bitmapWidth = ceil(renderResult.canvasWidth).toInt().coerceAtLeast(1)
            val bitmapHeight = ceil(renderResult.canvasHeight).toInt().coerceAtLeast(1)

            val imageBitmap = ImageBitmap(bitmapWidth, bitmapHeight)
            val canvas = Canvas(imageBitmap)
            val drawScope = CanvasDrawScope()

            // 解析背景颜色（JPEG 不支持透明，自动使用不透明背景）
            val useTransparent = exportConfig.transparentBackground &&
                    format != ImageFormat.JPEG
            val backgroundColor = if (useTransparent) {
                Color.Transparent
            } else if (isDarkTheme) {
                scaledConfig.darkBackgroundColor
            } else {
                scaledConfig.backgroundColor
            }

            // 使用 LatexRenderer 共享逻辑进行绘制（与 LatexDocument 同一份代码）
            drawScope.draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(
                    renderResult.canvasWidth,
                    renderResult.canvasHeight
                )
            ) {
                with(LatexRenderer) {
                    draw(renderResult, backgroundColor)
                }
            }

            // 一次性编码，避免调用方重复渲染
            val bytes = imageBitmap.encodeToFormat(format, quality)

            ExportResult(
                imageBitmap = imageBitmap,
                bytes = bytes,
                format = format
            )
        } catch (e: Exception) {
            HLog.e(TAG, "渲染失败", e)
            null
        }
    }

    private fun parseLatex(latex: String): LatexNode.Document {
        return try {
            parser.clear()
            parser.append(latex)
            parser.getCurrentDocument()
        } catch (e: Exception) {
            HLog.e(TAG, "解析失败", e)
            LatexNode.Document(emptyList())
        }
    }
}

/**
 * 创建并记住 [LatexExporterState] 实例
 *
 * 必须在 Composable 作用域中调用，以获取 [TextMeasurer] 和 [Density]。
 *
 * @param config 可选的渲染配置（用于提前加载字体）
 * @return [LatexExporterState] 实例
 */
@Composable
fun rememberLatexExporter(
    config: LatexConfig = LatexConfig()
): LatexExporterState {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val fontFamilies = config.fontFamilies ?: defaultLatexFontFamilies()

    val exporter = remember(density, textMeasurer, fontFamilies) {
        LatexExporterState(
            density = density,
            textMeasurer = textMeasurer,
            fontFamilies = fontFamilies,
            fontBytesCache = null
        )
    }

    // 异步加载字体字节数据
    LaunchedEffect(fontFamilies) {
        if (exporter.fontBytesCache == null) {
            exporter.fontBytesCache = try {
                val environment = getSystemResourceEnvironment()
                val mainBytes = fontFamilies.mainResource?.let {
                    getFontResourceBytes(environment, it)
                }
                val mathBytes = fontFamilies.mathResource?.let {
                    getFontResourceBytes(environment, it)
                }
                val size1Bytes = fontFamilies.size1Resource?.let {
                    getFontResourceBytes(environment, it)
                }
                FontBytesCache(mainBytes, mathBytes, size1Bytes)
            } catch (e: Exception) {
                HLog.e(TAG, "字体字节加载失败", e)
                null
            }
        }
    }

    return exporter
}
