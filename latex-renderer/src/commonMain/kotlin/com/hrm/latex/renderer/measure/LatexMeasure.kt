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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.renderer.layout.LatexRenderResult
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.model.HighlightRange
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.resolveFontFamilies
import com.hrm.latex.renderer.model.resolveThemeColors
import com.hrm.latex.renderer.model.toContext

private const val TAG = "LatexMeasurer"
private const val MAX_CACHE_SIZE = 128

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
 * val formula = measurer.inlineContent("\\frac{a}{b}")
 * Text(
 *     text = buildAnnotatedString {
 *         append("fraction: ")
 *         appendInlineContent("formula", "a divided by b")
 *     },
 *     inlineContent = formula?.let { mapOf("formula" to it) }.orEmpty()
 * )
 * ```
 */
class LatexMeasurerState internal constructor(
    private val density: Density,
    private val textMeasurer: TextMeasurer,
    private val fontFamilies: LatexFontFamilies,
    private val defaultConfig: LatexConfig = LatexConfig()
) {
    private val parser = IncrementalLatexParser()
    private val dimensionsCache = linkedMapOf<MeasureCacheKey, LatexDimensions?>()
    private val inlineContentCache = linkedMapOf<InlineContentCacheKey, InlineTextContent?>()

    /**
     * 同步测量 LaTeX 字符串的精确渲染尺寸。
     *
     * 内部复用 [LatexRenderer.measure]，与 Latex Composable 和导出器使用完全相同的
     * 测量逻辑，确保尺寸结果 100% 一致。
     *
     * @param latex LaTeX 字符串
     * @param config 渲染配置（字号、主题、字体等）
     * @param isDarkTheme 当前环境是否为深色模式。
     * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板。
     * @return [LatexDimensions]，包含完整渲染尺寸和基线信息；
     *   空输入或解析/测量失败时返回 null
     */
    fun measure(
        latex: String,
        config: LatexConfig = defaultConfig,
        isDarkTheme: Boolean = false
    ): LatexDimensions? {
        if (latex.isBlank()) return null

        val cacheKey = MeasureCacheKey(latex, config, isDarkTheme)
        if (dimensionsCache.containsKey(cacheKey)) {
            return dimensionsCache[cacheKey]
        }

        val dimensions = measureForRender(latex, config, isDarkTheme)?.dimensions
        dimensionsCache.putBounded(cacheKey, dimensions)
        return dimensions
    }

    /**
     * 按浅色模式创建行内公式内容，等价于 `inlineContent(latex, config, isDarkTheme = false)`。
     *
     * 单独保留这个两参数版本而不是给 isDarkTheme 加默认值：按旧版本编译的调用方
     * 仍链接到原签名，升级后不会 NoSuchMethodError。
     */
    fun inlineContent(
        latex: String,
        config: LatexConfig = defaultConfig
    ): InlineTextContent? = inlineContent(latex, config, isDarkTheme = false)

    /**
     * 创建可直接传给 Compose `Text` / `BasicText` 的行内公式内容。
     *
     * 该方法封装公式预测量、[Placeholder] 构造和 [Latex][com.hrm.latex.renderer.Latex]
     * 渲染。占位尺寸与 `Latex` 的完整画布尺寸保持一致，避免内容被裁剪；公式默认相对
     * 前置文本垂直居中。相同公式与配置会在当前 [LatexMeasurerState] 中复用缓存。
     *
     * 调用方仍使用 Compose 标准的
     * [appendInlineContent][androidx.compose.foundation.text.appendInlineContent] 声明公式位置，
     * 并将返回值放入 `Text` 的 `inlineContent` map。
     *
     * @param latex LaTeX 字符串
     * @param config 渲染配置（字号、主题、字体等）
     * @param isDarkTheme 当前环境是否为深色模式，传入 `isSystemInDarkTheme()` 即可。
     * 与渲染时的系统主题一致时，占位内容直接绘制预测量结果，不再重复测量；
     * 不一致时（例如沿用默认值 false 而系统处于深色模式）会按系统主题再测量一次。
     * @return 可直接嵌入富文本的 [InlineTextContent]；空输入或测量失败时返回 null
     */
    fun inlineContent(
        latex: String,
        config: LatexConfig,
        isDarkTheme: Boolean
    ): InlineTextContent? {
        if (latex.isBlank()) return null

        val cacheKey = InlineContentCacheKey(latex, config, isDarkTheme)
        if (inlineContentCache.containsKey(cacheKey)) {
            return inlineContentCache[cacheKey]
        }

        val resolvedConfig = config.copy(fontFamilies = config.fontFamilies ?: fontFamilies)
        val inlineContent = if (config.onNodeClick != null || config.onHyperlinkClick != null) {
            // 点击命中依赖 Latex() 内部的 LayoutMap，交给它完整渲染
            measure(latex, config, isDarkTheme)?.toInlineTextContent(density, latex, resolvedConfig)
        } else {
            // 测量结果直接用于绘制，占位内容里不再重新解析和测量同一个公式
            measureForRender(latex, config, isDarkTheme, config.highlight.ranges)?.let { measured ->
                dimensionsCache.putBounded(MeasureCacheKey(latex, config, isDarkTheme), measured.dimensions)
                // 主题不一致时只可能是另一种主题，记住其结果，重新组合时不再重测
                var otherTheme: MeasuredLatex? = null
                measured.toInlineTextContent(density, isDarkTheme) { isDark ->
                    otherTheme ?: measureForRender(latex, config, isDark, config.highlight.ranges)
                        .also { otherTheme = it }
                }
            }
        }
        inlineContentCache.putBounded(cacheKey, inlineContent)
        return inlineContent
    }

    /**
     * 批量测量多个 LaTeX 字符串。
     *
     * 复用同一个解析器实例，减少重复创建开销。
     *
     * @param formulas LaTeX 字符串列表
     * @param config 渲染配置
     * @param isDarkTheme 当前环境是否为深色模式。
     * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板。
     * @return 与输入列表等长的 [LatexDimensions] 列表，测量失败的条目为 null
     */
    fun measureBatch(
        formulas: List<String>,
        config: LatexConfig = defaultConfig,
        isDarkTheme: Boolean = false
    ): List<LatexDimensions?> {
        return formulas.map { measure(it, config, isDarkTheme) }
    }

    /**
     * 解析并测量，同时保留可直接绘制的 [LatexRenderResult]。
     *
     * 空输入、空文档、零尺寸或解析/测量失败时返回 null。
     */
    private fun measureForRender(
        latex: String,
        config: LatexConfig,
        isDarkTheme: Boolean,
        highlightRanges: List<HighlightRange> = emptyList()
    ): MeasuredLatex? = try {
        val document = parseLatex(latex)
        if (document.children.isEmpty()) {
            null
        } else {
            val context = config.toContext(isDarkTheme, fontFamilies)
            val renderResult = LatexRenderer.measure(
                document.children, context, textMeasurer, density, highlightRanges
            )
            if (renderResult.layout.width <= 0f || renderResult.layout.height <= 0f) {
                null
            } else {
                MeasuredLatex(
                    dimensions = LatexDimensions(
                        widthPx = renderResult.canvasWidth,
                        heightPx = renderResult.canvasHeight,
                        baselinePx = renderResult.layout.baseline + renderResult.verticalPadding,
                        contentWidthPx = renderResult.layout.width,
                        contentHeightPx = renderResult.layout.height,
                        contentBaselinePx = renderResult.layout.baseline
                    ),
                    document = document,
                    renderResult = renderResult,
                    backgroundColor = config.resolveThemeColors(isDarkTheme).backgroundColor,
                    accessibilityEnabled = config.accessibilityEnabled
                )
            }
        }
    } catch (e: Exception) {
        HLog.e(TAG, "测量失败", e)
        null
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

    private data class MeasureCacheKey(
        val latex: String,
        val config: LatexConfig,
        val isDarkTheme: Boolean
    )

    private data class InlineContentCacheKey(
        val latex: String,
        val config: LatexConfig,
        val isDarkTheme: Boolean
    )
}

internal fun LatexDimensions.toInlineTextContent(
    density: Density,
    latex: String,
    config: LatexConfig
): InlineTextContent = InlineTextContent(
    placeholder = Placeholder(
        width = with(density) { widthPx.toSp() },
        height = with(density) { heightPx.toSp() },
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
    )
) {
    com.hrm.latex.renderer.Latex(
        latex = latex,
        config = config
    )
}

/**
 * 一次测量的完整产物：尺寸、AST 与可直接绘制的布局。
 */
internal class MeasuredLatex(
    val dimensions: LatexDimensions,
    val document: LatexNode.Document,
    val renderResult: LatexRenderResult,
    val backgroundColor: Color,
    val accessibilityEnabled: Boolean
)

/**
 * 用预测量结果构造行内内容：占位尺寸取自 [MeasuredLatex.dimensions]，绘制直接复用
 * [MeasuredLatex.renderResult]，与 [Latex][com.hrm.latex.renderer.Latex] 的绘制逻辑一致。
 *
 * @param measuredInDarkTheme 测量时使用的深色模式；渲染时系统主题与之不同，
 *   则通过 [remeasure] 按系统主题重新测量（颜色烘焙在布局里，尺寸不变）
 */
internal fun MeasuredLatex.toInlineTextContent(
    density: Density,
    measuredInDarkTheme: Boolean,
    remeasure: (isDarkTheme: Boolean) -> MeasuredLatex?
): InlineTextContent = InlineTextContent(
    placeholder = Placeholder(
        width = with(density) { dimensions.widthPx.toSp() },
        height = with(density) { dimensions.heightPx.toSp() },
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
    )
) {
    val isDark = isSystemInDarkTheme()
    // 以 this 为 key：调用方在同一组合位置换成另一个公式的内容时，不能沿用旧结果
    val measured = remember(this, isDark) {
        if (isDark == measuredInDarkTheme) this else remeasure(isDark) ?: this
    }
    val description = remember(measured) {
        if (measured.accessibilityEnabled) AccessibilityVisitor.describe(measured.document) else null
    }
    val canvasDensity = LocalDensity.current
    val sizeModifier = with(canvasDensity) {
        Modifier.size(measured.dimensions.widthPx.toDp(), measured.dimensions.heightPx.toDp())
    }
    val modifier = if (description != null) {
        Modifier.semantics { contentDescription = description }.then(sizeModifier)
    } else {
        sizeModifier
    }
    Canvas(modifier = modifier) {
        with(LatexRenderer) {
            draw(measured.renderResult, measured.backgroundColor)
        }
    }
}

private fun <K, V> MutableMap<K, V>.putBounded(key: K, value: V) {
    if (size >= MAX_CACHE_SIZE && !containsKey(key)) {
        remove(keys.first())
    }
    this[key] = value
}

/**
 * 创建并记住 [LatexMeasurerState] 实例。
 *
 * 必须在 Composable 作用域中调用，以获取 [TextMeasurer] 和 [Density]。
 * @param config 默认渲染配置，后续测量/导出调用可显式覆盖；字体也从此配置加载
 * @return [LatexMeasurerState] 实例
 */
@Composable
fun rememberLatexMeasurer(
    config: LatexConfig = LatexConfig()
): LatexMeasurerState {
    val fontFamilies = config.resolveFontFamilies()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    return remember(density, textMeasurer, fontFamilies, config) {
        LatexMeasurerState(
            density = density,
            textMeasurer = textMeasurer,
            fontFamilies = fontFamilies,
            defaultConfig = config
        )
    }
}
