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


package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.isCenteredSymbol
import kotlin.math.max

/**
 * 堆叠渲染器，用于 \overset、\underset、\stackrel 等命令
 */
internal class StackMeasurer : NodeMeasurer<LatexNode.Stack> {

    override fun measure(
        node: LatexNode.Stack,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        // 先对 Stack 结构做规范化 ：\overset{a}{\underset{b}{X}} 会产生 Stack 套 Stack
        // 如果不扁平化，会导致定位/贴合基于"已经堆叠后的盒子"，从而出现上下异常。
        fun unwrapSingleGroup(node0: LatexNode): LatexNode {
            var n = node0
            while (n is LatexNode.Group && n.children.size == 1) {
                n = n.children[0]
            }
            return n
        }

        fun merge(a: LatexNode?, b: LatexNode?): LatexNode? {
            if (a == null) return b
            if (b == null) return a
            return LatexNode.Group(listOf(a, b))
        }

        var baseNode: LatexNode = unwrapSingleGroup(node.base)
        var aboveNode: LatexNode? = node.above?.let { unwrapSingleGroup(it) }
        var belowNode: LatexNode? = node.below?.let { unwrapSingleGroup(it) }

        while (baseNode is LatexNode.Stack) {
            val inner = baseNode
            aboveNode = merge(aboveNode, inner.above?.let { unwrapSingleGroup(it) })
            belowNode = merge(belowNode, inner.below?.let { unwrapSingleGroup(it) })
            baseNode = unwrapSingleGroup(inner.base)
        }

        // 测量基础内容（保持原样式）- 包装成List确保作为整体渲染
        var baseLayout = measureGroup(listOf(baseNode), context)

        // 对于 centered 符号（箭头等），如果有上下堆叠，需要拉长箭头以匹配上下内容的宽度
        val isCenteredBase =
            (baseNode as? LatexNode.Symbol)?.symbol?.let { isCenteredSymbol(it) } == true

        // 上下内容使用较小字体（0.7倍）
        val scriptStyle = context.shrink(MathConstants.STACK_SCRIPT_SCALE)

        // 测量上方内容（如果有）- 包装成List
        val aboveLayout = aboveNode?.let { measureGroup(listOf(it), scriptStyle) }

        // 测量下方内容（如果有）- 包装成List
        val belowLayout = belowNode?.let { measureGroup(listOf(it), scriptStyle) }

        // 如果是 centered 符号且有上下堆叠，拉长基础符号以匹配上下内容宽度
        if (isCenteredBase && (aboveLayout != null || belowLayout != null)) {
            val maxScriptWidth = max(aboveLayout?.width ?: 0f, belowLayout?.width ?: 0f)
            if (maxScriptWidth > baseLayout.width) {
                // 拉长箭头：增加更多额外宽度避免重叠
                val extraWidth = maxScriptWidth * MathConstants.STACK_ARROW_EXTRA_WIDTH
                val targetWidth = maxScriptWidth + extraWidth
                baseLayout =
                    NodeLayout(targetWidth, baseLayout.height, baseLayout.baseline, baseLayout.draw)
            }
        }

        // 计算总宽度（取最宽的元素）
        val totalWidth = max(
            baseLayout.width,
            max(aboveLayout?.width ?: 0f, belowLayout?.width ?: 0f)
        )

        // 不要用间距，尽量贴合
        val gap = 0f

        // centered 符号（如箭头/等号）在 TextContentMeasurer 里会把 baseline 调到 height*0.85 来"上移居中"。
        // 这会导致：如果我们用 height*0.5 当附着点，实际上是在贴"行高盒子"，而不是贴符号的墨迹。
        // 这里用一个更稳定的启发式：从被抬高的 baseline 反推视觉中线。
        // - 对箭头：baseline≈0.85h => 视觉中线更靠上，可用 (baseline - 0.45h) 近似
        // - 对非 centered：仍按顶/底堆叠
        val axis =
            if (isCenteredBase) baseLayout.baseline - baseLayout.height * MathConstants.STACK_CENTER_AXIS else 0f

        // centered 符号的行高盒子上下留白很大，优化上下间距
        // - 上方: 0.15 (减小拉近力度，让上方内容离箭头远一些，往上移)
        // - 下方: 0.58 (极限拉近，让下方内容紧贴箭头)
        val aboveTighten = if (isCenteredBase) MathConstants.STACK_ABOVE_TIGHTEN else 0f
        val belowLift = if (isCenteredBase) MathConstants.STACK_BELOW_LIFT else 0f

        val attachAbove = if (isCenteredBase) axis else 0f
        // 下方附着点用 baseline 附近更稳定：对 \to 这类符号 baseline 被抬高，接近符号下缘
        val attachBelow =
            if (isCenteredBase) baseLayout.baseline + baseLayout.height * MathConstants.STACK_BELOW_ATTACH_NUDGE else baseLayout.height

        val baseY = 0f
        // 上方：贴底后向下拉近；下方：贴顶后向上轻微拉近，并做最小下移防重叠
        val aboveY = aboveLayout?.let { (attachAbove - gap - it.height) + it.height * aboveTighten }
        val belowY = if (belowLayout != null) {
            val raw = (attachBelow + gap) - belowLayout.height * belowLift
            // 极低防重叠阈值: 0.20，让下方内容极限靠近箭头
            if (isCenteredBase) maxOf(
                raw,
                baseLayout.height * MathConstants.STACK_MIN_BELOW_THRESHOLD
            ) else raw
        } else null

        val top = minOf(
            baseY,
            aboveY ?: baseY,
            belowY ?: baseY
        )
        val bottom = maxOf(
            baseY + baseLayout.height,
            (aboveY ?: baseY) + (aboveLayout?.height ?: 0f),
            (belowY ?: baseY) + (belowLayout?.height ?: 0f)
        )

        val totalHeight = bottom - top
        val baseline = (baseY + baseLayout.baseline) - top

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            val offsetY = y - top

            // 绘制基础内容（居中对齐）
            val baseX = x + (totalWidth - baseLayout.width) / 2
            baseLayout.draw(this, baseX, offsetY + baseY)

            // 绘制上方内容（如果有，居中对齐，贴合附着点）
            // 对于 centered 符号，上下内容稍微往左偏移一点点，避免与箭头头部重叠
            val scriptOffset =
                if (isCenteredBase) totalWidth * MathConstants.STACK_SCRIPT_HORIZONTAL_OFFSET else 0f

            aboveLayout?.let { layout ->
                val aboveX = x + (totalWidth - layout.width) / 2 + scriptOffset
                layout.draw(this, aboveX, offsetY + (aboveY ?: 0f))
            }

            // 绘制下方内容（如果有，居中对齐，贴合附着点）
            belowLayout?.let { layout ->
                val belowX = x + (totalWidth - layout.width) / 2 + scriptOffset
                layout.draw(this, belowX, offsetY + (belowY ?: 0f))
            }
        }
    }
}
