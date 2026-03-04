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

package com.hrm.latex.renderer.measure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.font.rememberResolvedMathFont
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.defaultLatexFontFamilies
import com.hrm.latex.renderer.model.toContext

private const val TAG = "LatexMeasurer"

/**
 * LaTeX 公式的预测量尺寸结果。
 *
 * 用于在 Compose `InlineTextContent` 场景中，提前获取公式的精确渲染尺寸，
 * 以构建正确大小的 `Placeholder`。
 *
 * 所有尺寸单位为像素（px），可通过 [Density] 转换为 `sp` 或 `dp`。
 *
 * @property widthPx 公式完整渲染宽度（像素，含内边距）
 * @property heightPx 公式完整渲染高度（像素，含内边距）
 * @property baselinePx 基线距顶部的距离（像素，含垂直内边距偏移），
 *   对 `PlaceholderVerticalAlign.TextBottom` 或 `TextCenter` 对齐至关重要。
 * @property contentWidthPx 公式内容宽度（像素，不含内边距）
 * @property contentHeightPx 公式内容高度（像素，不含内边距）
 * @property contentBaselinePx 内容基线距内容顶部的距离（像素，不含内边距）
 */
data class LatexDimensions(
    val widthPx: Float,
    val heightPx: Float,
    val baselinePx: Float,
    val contentWidthPx: Float,
    val contentHeightPx: Float,
    val contentBaselinePx: Float
)

/**
 * LaTeX 预测量器。
 *
 * 在 Composable 作用域中通过 [rememberLatexMeasurer] 创建实例。
 * 提供同步的 [measure] 方法，返回公式的精确渲染尺寸，
 * 与 [Latex][com.hrm.latex.renderer.Latex] Composable 和
 * [LatexExporterState][com.hrm.latex.renderer.export.LatexExporterState]
 * 共享完全相同的测量逻辑。
 *
 * 典型用途：为 Compose `InlineTextContent` 的 `Placeholder` 提供精确尺寸，
 * 实现行内数学公式渲染。
 *
 * 使用示例：
 * ```kotlin
 * val measurer = rememberLatexMeasurer()
 * val density = LocalDensity.current
 *
 * val dims = measurer.measure("\\frac{a}{b}") ?: return
 * val widthSp = with(density) { dims.widthPx.toSp() }
 * val heightSp = with(density) { dims.heightPx.toSp() }
 *
 * val placeholder = Placeholder(
 *     width = widthSp,
 *     height = heightSp,
 *     placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
 * )
 * ```
 */
class LatexMeasurerState internal constructor(
    private val density: Density,
    private val textMeasurer: TextMeasurer,
    private val fontFamilies: LatexFontFamilies
) {
    private val parser = IncrementalLatexParser()

    /**
     * 同步测量 LaTeX 字符串的精确渲染尺寸。
     *
     * 内部复用 [LatexRenderer.measure]，与 Latex Composable 和导出器使用完全相同的
     * 测量逻辑，确保尺寸结果 100% 一致。
     *
     * @param latex LaTeX 字符串
     * @param config 渲染配置（字号、颜色、字体等）
     * @param isDarkTheme 是否深色模式
     * @return [LatexDimensions]，包含完整渲染尺寸和基线信息；
     *   空输入或解析/测量失败时返回 null
     */
    fun measure(
        latex: String,
        config: LatexConfig = LatexConfig(),
        isDarkTheme: Boolean = false
    ): LatexDimensions? {
        if (latex.isBlank()) return null

        return try {
            val document = parseLatex(latex)
            if (document.children.isEmpty()) return null

            val resolvedFontFamilies = config.mathFont.fontFamiliesOrNull() ?: fontFamilies
            val context = config.toContext(isDarkTheme, resolvedFontFamilies)

            val renderResult = LatexRenderer.measure(
                document.children, context, textMeasurer, density
            )

            if (renderResult.layout.width <= 0f || renderResult.layout.height <= 0f) return null

            LatexDimensions(
                widthPx = renderResult.canvasWidth,
                heightPx = renderResult.canvasHeight,
                baselinePx = renderResult.layout.baseline + renderResult.verticalPadding,
                contentWidthPx = renderResult.layout.width,
                contentHeightPx = renderResult.layout.height,
                contentBaselinePx = renderResult.layout.baseline
            )
        } catch (e: Exception) {
            HLog.e(TAG, "测量失败", e)
            null
        }
    }

    /**
     * 批量测量多个 LaTeX 字符串。
     *
     * 复用同一个解析器实例，减少重复创建开销。
     *
     * @param formulas LaTeX 字符串列表
     * @param config 渲染配置
     * @param isDarkTheme 是否深色模式
     * @return 与输入列表等长的 [LatexDimensions] 列表，测量失败的条目为 null
     */
    fun measureBatch(
        formulas: List<String>,
        config: LatexConfig = LatexConfig(),
        isDarkTheme: Boolean = false
    ): List<LatexDimensions?> {
        return formulas.map { measure(it, config, isDarkTheme) }
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
 * 创建并记住 [LatexMeasurerState] 实例。
 *
 * 必须在 Composable 作用域中调用，以获取 [TextMeasurer] 和 [Density]。
 * 支持 [MathFont.OTF][com.hrm.latex.renderer.font.MathFont.OTF] 的
 * FontResource 异步加载：加载前使用 TTF 降级，加载完成后自动重组。
 *
 * @param config 可选的渲染配置（用于提前加载字体）
 * @return [LatexMeasurerState] 实例
 */
@Composable
fun rememberLatexMeasurer(
    config: LatexConfig = LatexConfig()
): LatexMeasurerState {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val effectiveMathFont = rememberResolvedMathFont(config.mathFont)
    val fontFamilies = effectiveMathFont.fontFamiliesOrNull() ?: defaultLatexFontFamilies()

    return remember(density, textMeasurer, fontFamilies) {
        LatexMeasurerState(
            density = density,
            textMeasurer = textMeasurer,
            fontFamilies = fontFamilies
        )
    }
}
