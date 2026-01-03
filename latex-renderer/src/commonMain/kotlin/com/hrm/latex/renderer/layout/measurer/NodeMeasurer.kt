package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderStyle

/**
 * 节点测量器接口
 *
 * 定义了测量特定类型 LaTeX 节点的通用契约。
 * 所有的具体测量器（如 [TextContentMeasurer], [MathMeasurer] 等）都实现此接口。
 *
 * @param T 此测量器可以处理的节点类型
 */
internal interface NodeMeasurer<T : LatexNode> {
    /**
     * 测量节点并返回布局结果
     *
     * @param node 要测量的节点
     * @param style 渲染样式（包含字体大小、颜色等）
     * @param measurer Compose 的文本测量器
     * @param density 屏幕密度，用于 dp/sp 转 px
     * @param measureGlobal 回调函数，用于递归测量通用节点（当节点包含子节点时使用）
     * @param measureGroup 回调函数，用于递归测量节点组（当处理节点列表时使用）
     * @return [NodeLayout] 包含尺寸、基线和绘制逻辑的布局对象
     */
    fun measure(
        node: T,
        style: RenderStyle,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderStyle) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderStyle) -> NodeLayout
    ): NodeLayout
}
