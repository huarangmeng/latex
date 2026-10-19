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


package com.hrm.latex.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.font.MathFontProviderFactory
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.layout.LayoutCache
import com.hrm.latex.renderer.layout.LayoutMap
import com.hrm.latex.renderer.model.HighlightRange
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LineBreakingConfig
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.defaultLatexFontFamilies
import com.hrm.latex.renderer.model.resolveThemeColors
import com.hrm.latex.renderer.model.toContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Latex"

/**
 * Latex 渲染组件
 *
 * 自动支持增量解析能力，可以安全处理不完整的 LaTeX 输入
 *
 * 性能优化：
 * - 复用解析器实例，避免重复创建
 * - 异步解析，不阻塞主线程
 * - 防抖机制，避免重复解析相同内容
 *
 * @param latex LaTeX 字符串（支持增量输入，会自动解析可解析部分）
 * @param modifier 修饰符
 * @param config 渲染配置（包含主题、字体大小等）
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板；
 * 固定主题和 `LatexTheme.material3()` 会直接使用 theme 中的颜色。
 * @param fontFamilies 公式字体集。默认使用内置 KaTeX 字体；传入自定义字体集
 * （例如系统字体）即可完全不加载内置字体资源。
 */
@Composable
fun Latex(
    latex: String,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    fontFamilies: LatexFontFamilies = defaultLatexFontFamilies()
) {
    val resolvedThemeColors = config.resolveThemeColors(isDarkTheme)

    // 缓存 MathFontProvider 实例：Provider 不是 data class，每次创建新实例会导致
    // RenderContext 的 equals 失效，进而使下游 remember(context) 永远 miss。
    val provider = remember(fontFamilies) {
        MathFontProviderFactory.create(fontFamilies)
    }

    // 构建渲染上下文（fontFamilies 由全局单例管理，bytes 异步加载完成后自动触发重组）
    val context = remember(config, isDarkTheme, fontFamilies, provider) {
        config.toContext(isDarkTheme, fontFamilies, provider)
    }

    // 复用解析器实例以支持真正的增量解析
    val parser = remember { IncrementalLatexParser() }

    // 使用 State 来保存解析结果
    var document by remember { mutableStateOf(LatexNode.Document(emptyList())) }

    // 记录上次解析的内容，避免重复解析
    var lastParsedLatex by remember { mutableStateOf("") }

    // 当 latex 变化时更新解析（在后台线程执行，避免阻塞主线程）
    LaunchedEffect(latex) {
        // 防抖：如果内容没变，跳过
        if (latex == lastParsedLatex) {
            return@LaunchedEffect
        }

        lastParsedLatex = latex

        // 切换到 Default 调度器执行解析
        val result = withContext(Dispatchers.Default) {
            try {
                // 优化：计算增量部分
                val currentInput = parser.getCurrentInput()

                if (latex.startsWith(currentInput) && latex.length > currentInput.length) {
                    // 增量追加：只解析新增部分
                    val delta = latex.substring(currentInput.length)
                    parser.append(delta)
                } else {
                    // 完全替换：清空后重新解析
                    parser.clear()
                    parser.append(latex)
                }

                parser.getCurrentDocument()
            } catch (e: Exception) {
                HLog.e(TAG, "增量解析失败", e)
                // 解析失败时返回空文档
                LatexNode.Document(emptyList())
            }
        }

        // 回到主线程更新 UI
        document = result
    }

    // 无障碍描述：当启用时，使用 AccessibilityVisitor 生成屏幕阅读器文本
    val accessibilityDescription = if (config.accessibilityEnabled) {
        remember(document) {
            AccessibilityVisitor.describe(document)
        }
    } else null

    LatexDocument(
        modifier = modifier,
        children = document.children,
        context = context,
        highlightRanges = config.highlight.ranges,
        backgroundColor = resolvedThemeColors.backgroundColor,
        contentDescription = accessibilityDescription,
        onNodeClick = config.onNodeClick,
        onHyperlinkClick = config.onHyperlinkClick,
        enableLayoutCache = config.enableLayoutCache,
        latex = latex
    )
}

/**
 * 自动换行的 LaTeX 渲染组件。
 *
 * 该组件会根据父容器宽度自动包装长公式。
 * 换行优先发生在关系运算符（`=`、`<`、`>`）和二元运算符（`+`、`-`、`×`）之后。
 *
 * @param latex LaTeX 字符串（支持增量输入）
 * @param modifier 修饰符
 * @param config 渲染配置（包含主题、字体大小等）
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时参与主题解析。
 */
@Composable
fun LatexAutoWrap(
    latex: String,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }

        val wrappingConfig = config.copy(
            lineBreaking = LineBreakingConfig(
                enabled = true,
                maxWidth = maxWidthPx
            )
        )

        Latex(
            latex = latex,
            modifier = Modifier,
            config = wrappingConfig,
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Latex 文档渲染组件
 *
 * @param modifier 修饰符
 * @param children 文档根节点
 * @param context 渲染上下文
 * @param highlightRanges 高亮区域配置（不参与渲染树遍历）
 * @param backgroundColor 背景颜色
 * @param contentDescription 无障碍描述文本（非空时启用 semantics）
 */
@Composable
private fun LatexDocument(
    modifier: Modifier = Modifier,
    children: List<LatexNode>,
    context: RenderContext,
    highlightRanges: List<HighlightRange> = emptyList(),
    backgroundColor: Color = Color.Transparent,
    contentDescription: String? = null,
    onNodeClick: ((startOffset: Int, endOffset: Int, latex: String) -> Unit)? = null,
    onHyperlinkClick: ((url: String) -> Unit)? = null,
    enableLayoutCache: Boolean = false,
    latex: String = ""
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 当启用点击交互时，使用 LayoutMap 记录节点位置
    val layoutMap = remember(onNodeClick, onHyperlinkClick) {
        if (onNodeClick != null || onHyperlinkClick != null) LayoutMap() else null
    }

    // NodeLayout 缓存：仅在 config 启用时创建，跨渲染周期复用相同 AST 子树+上下文的测量结果
    val layoutCache = remember(enableLayoutCache) {
        if (enableLayoutCache) LayoutCache() else null
    }

    // 使用 LatexRenderer 共享逻辑进行测量（与导出路径共用同一份代码）
    val renderResult = remember(children, context, density, highlightRanges) {
        layoutMap?.clear()
        LatexRenderer.measure(children, context, measurer, density, highlightRanges, layoutMap, layoutCache)
    }

    val widthDp = with(density) { renderResult.canvasWidth.toDp() }
    val heightDp = with(density) { renderResult.canvasHeight.toDp() }

    val canvasModifier = if (contentDescription != null) {
        modifier
            .semantics { this.contentDescription = contentDescription }
            .size(widthDp, heightDp)
    } else {
        modifier.size(widthDp, heightDp)
    }.let { mod ->
        if ((onNodeClick != null || onHyperlinkClick != null) && layoutMap != null) {
            mod.pointerInput(layoutMap, latex, onHyperlinkClick) {
                detectTapGestures { offset ->
                    // 将点击坐标转换为内容区相对坐标
                    val contentX = offset.x - renderResult.horizontalPadding
                    val contentY = offset.y - renderResult.verticalPadding
                    val hit = layoutMap.hitTest(contentX, contentY)
                    if (hit != null) {
                        // 超链接专用回调：命中 Hyperlink 节点时直接返回 URL
                        if (onHyperlinkClick != null && hit.node is LatexNode.Hyperlink) {
                            onHyperlinkClick(hit.node.url)
                        }
                        // 通用节点点击回调
                        if (onNodeClick != null) {
                            val range = hit.node.sourceRange
                            if (range != null) {
                                onNodeClick(range.start, range.end, latex)
                            }
                        }
                    }
                }
            }
        } else mod
    }

    Canvas(modifier = canvasModifier) {
        // 使用 LatexRenderer 共享逻辑进行绘制（与导出路径共用同一份代码）
        with(LatexRenderer) {
            draw(renderResult, backgroundColor)
        }
    }
}
