package com.hrm.latex.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import com.hrm.latex.renderer.model.RenderStyle
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
 * @param style 渲染样式（包含颜色、背景色、深浅色模式配置、字体大小等）
 * @param isDarkTheme 是否为深色模式（默认跟随系统）
 */
@Composable
fun Latex(
    latex: String,
    modifier: Modifier = Modifier,
    style: RenderStyle = RenderStyle(),
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // 确定最终文本颜色
    val resolvedTextColor = if (isDarkTheme) {
        if (style.darkColor != Color.Unspecified) style.darkColor else Color.White
    } else {
        if (style.color != Color.Unspecified) style.color else Color.Black
    }

    // 确定最终背景颜色
    val resolvedBackgroundColor = if (isDarkTheme) {
        style.darkBackgroundColor
    } else {
        style.backgroundColor
    }

    // 构建最终样式
    val resolvedStyle = style.copy(
        color = resolvedTextColor,
        backgroundColor = resolvedBackgroundColor
    )

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
        style = resolvedStyle
    )
}

/**
 * Latex 文档渲染组件
 *
 * @param modifier 修饰符
 * @param children 文档根节点
 * @param style 渲染样式
 */
@Composable
fun LatexDocument(
    modifier: Modifier = Modifier,
    children: List<LatexNode>,
    style: RenderStyle = RenderStyle()
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val layout = remember(children, style, density) {
        measureGroup(children, style, measurer, density)
    }

    val widthDp = with(density) { layout.width.toDp() }
    val heightDp = with(density) { layout.height.toDp() }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.size(widthDp, heightDp)) {
            // 绘制背景
            if (style.backgroundColor != Color.Unspecified) {
                drawRect(color = style.backgroundColor)
            }
            // 绘制内容
            layout.draw(this, 0f, 0f)
        }
    }
}
