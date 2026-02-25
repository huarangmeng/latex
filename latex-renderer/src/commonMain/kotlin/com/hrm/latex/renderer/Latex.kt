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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.measureGroup
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LineBreakingConfig
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.defaultLatexFontFamilies
import com.hrm.latex.renderer.model.toContext
import com.hrm.latex.renderer.utils.FontBytesCache
import com.hrm.latex.renderer.utils.MathConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

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
 * @param config 渲染样式（包含颜色、背景色、深浅色模式配置、字体大小等）
 * @param isDarkTheme 是否为深色模式（默认跟随系统）
 */
@Composable
fun Latex(
    latex: String,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // 确定最终背景颜色
    val resolvedBackgroundColor = if (isDarkTheme) {
        config.darkBackgroundColor
    } else {
        config.backgroundColor
    }
    val fontFamilies = config.fontFamilies ?: defaultLatexFontFamilies()

    // 异步加载字体字节数据（用于精确 glyph bounds 测量）
    // 从 fontFamilies 中获取 FontResource，支持外部自定义字体
    var fontBytesCache by remember { mutableStateOf<FontBytesCache?>(null) }
    LaunchedEffect(fontFamilies) {
        if (fontBytesCache == null) {
            fontBytesCache = try {
                val environment = getSystemResourceEnvironment()
                val extensionBytes = fontFamilies.extensionResource?.let {
                    getFontResourceBytes(environment, it)
                }
                val symbolBytes = fontFamilies.symbolResource?.let {
                    getFontResourceBytes(environment, it)
                }
                val mathItalicBytes = fontFamilies.mathItalicResource?.let {
                    getFontResourceBytes(environment, it)
                }
                val romanBytes = fontFamilies.romanResource?.let {
                    getFontResourceBytes(environment, it)
                }
                FontBytesCache(extensionBytes, symbolBytes, mathItalicBytes, romanBytes)
            } catch (e: Exception) {
                HLog.e(TAG, "字体字节加载失败", e)
                null
            }
        }
    }

    // 构建初始渲染上下文
    val context = config.toContext(isDarkTheme, fontFamilies).let {
        if (fontBytesCache != null) it.copy(fontBytesCache = fontBytesCache) else it
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

    LatexDocument(
        modifier = modifier,
        children = document.children,
        context = context,
        backgroundColor = resolvedBackgroundColor
    )
}

/**
 * latex renderer with automatic line breaking based on container width
 *
 * this composable automatically wraps long equations to fit within the parent container.
 * line breaks occur at logical points: after relation symbols (=, <, >) and
 * binary operators (+, -, ×).
 *
 * @param latex latex string (supports incremental input)
 * @param modifier modifier
 * @param config rendering configuration (font size, colors, etc.)
 * @param isDarkTheme dark theme flag (defaults to system setting)
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
 * @param backgroundColor 背景颜色
 */
@Composable
private fun LatexDocument(
    modifier: Modifier = Modifier,
    children: List<LatexNode>,
    context: RenderContext,
    backgroundColor: Color = Color.Transparent
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val layout = remember(children, context, density) {
        measureGroup(children, context, measurer, density)
    }

    // 外层内边距：内容区与 Canvas 边缘的间距
    // 水平：补偿斜体字形残余溢出 + 视觉呼吸空间
    // 垂直：补偿 descender 尾端、装饰符号顶部等溢出
    val fontSizePx = with(density) { context.fontSize.toPx() }
    val horizontalPadding = fontSizePx * MathConstants.CANVAS_HORIZONTAL_PADDING
    val verticalPadding = fontSizePx * MathConstants.CANVAS_VERTICAL_PADDING

    val widthDp = with(density) { (layout.width + horizontalPadding * 2).toDp() }
    val heightDp = with(density) { (layout.height + verticalPadding * 2).toDp() }

    Canvas(modifier = modifier.size(widthDp, heightDp)) {
        // 绘制背景
        if (backgroundColor != Color.Unspecified && backgroundColor != Color.Transparent) {
            drawRect(color = backgroundColor)
        }
        // 内容从 (horizontalPadding, verticalPadding) 开始绘制
        layout.draw(this, horizontalPadding, verticalPadding)
    }
}
