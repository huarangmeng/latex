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

package com.hrm.latex.renderer.layout

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext

/**
 * 行级布局后处理器。
 *
 * 在 measureGroup 的初步测量完成后，对特定节点（如积分运算符）进行
 * 基于上下文的二次调整。这些逻辑从 measureGroup 中提取出来，使
 * measureGroup 保持简洁的"测量 → 间距 → 组合"职责。
 */
internal object GroupLayoutPostProcessor {

    /**
     * 积分高度推断：根据积分右侧相邻内容的高度调整积分符号的目标高度。
     *
     * 仅在 DISPLAY 模式下生效。避免积分被拉伸到与同行其他大型结构
     * （如求和+分数）一样高，而是仅匹配其作用域内的被积函数高度。
     *
     * @param nodes 行内节点列表
     * @param initialLayouts 初步测量结果（与 nodes 一一对应）
     * @param context 渲染上下文
     * @param measurer 文本测量器
     * @param density 密度
     * @return 调整后的布局列表（未命中积分的节点保持原布局）
     */
    fun adjustIntegralHeights(
        nodes: List<LatexNode>,
        initialLayouts: List<NodeLayout>,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): List<NodeLayout> {
        val hasIntegrals = nodes.any { it is LatexNode.BigOperator && it.operator.contains("int") }
        if (!hasIntegrals || context.mathStyle != MathStyle.DISPLAY) {
            return initialLayouts
        }

        return nodes.mapIndexed { index, node ->
            val isIntegral = node is LatexNode.BigOperator && node.operator.contains("int")
            if (isIntegral) {
                val rightContentHeight = computeRightContentHeight(nodes, initialLayouts, index)
                if (rightContentHeight > 0) {
                    measureNode(
                        node,
                        context.copy(
                            layoutHints = context.layoutHints.copy(bigOpHeightHint = rightContentHeight)
                        ),
                        measurer,
                        density
                    )
                } else {
                    initialLayouts[index]
                }
            } else {
                initialLayouts[index]
            }
        }
    }

    /**
     * 计算指定积分右侧相邻非运算符内容的总高度。
     *
     * 扫描范围：从 index+1 开始，遇到下一个大型运算符停止，
     * 跳过 Space/HSpace 空白节点。
     */
    private fun computeRightContentHeight(
        nodes: List<LatexNode>,
        layouts: List<NodeLayout>,
        integralIndex: Int
    ): Float {
        var rightContentAscent = 0f
        var rightContentDescent = 0f
        for (j in (integralIndex + 1) until nodes.size) {
            val rightNode = nodes[j]
            if (rightNode is LatexNode.BigOperator) break
            if (rightNode is LatexNode.Space || rightNode is LatexNode.HSpace) continue
            val layout = layouts[j]
            if (layout.baseline > rightContentAscent) rightContentAscent = layout.baseline
            if (layout.height - layout.baseline > rightContentDescent) {
                rightContentDescent = layout.height - layout.baseline
            }
        }
        return rightContentAscent + rightContentDescent
    }
}
