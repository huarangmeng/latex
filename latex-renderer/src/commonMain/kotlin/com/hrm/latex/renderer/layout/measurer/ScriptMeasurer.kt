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
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.toScriptStyle
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.math.min

/**
 * 上下标测量器 — 处理 a^b (Superscript) 和 a_b (Subscript)
 *
 * 支持：
 * - 单独上标/下标
 * - 同时上下标（如 x^{2}_{i}，解析为嵌套结构）
 *
 * 使用 MathStyle 状态机决定脚本字号：
 * - DISPLAY/TEXT → SCRIPT (0.7x)
 * - SCRIPT → SCRIPT_SCRIPT (0.5x)
 * - SCRIPT_SCRIPT → SCRIPT_SCRIPT (不再缩小)
 */
internal class ScriptMeasurer : NodeMeasurer<LatexNode> {

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val isSuper = node is LatexNode.Superscript
        val baseNode = if (isSuper) (node as LatexNode.Superscript).base else (node as LatexNode.Subscript).base
        val scriptNode = if (isSuper) node.exponent else (node as LatexNode.Subscript).index

        // 特殊处理: \underbrace{...}_{text} 和 \overbrace{...}^{text}
        // 文本标注应放在花括号的下方/上方，居中对齐
        val braceAnnotation = detectBraceAnnotation(isSuper, baseNode)
        if (braceAnnotation != null) {
            return measureBraceAnnotation(
                braceAnnotation, scriptNode, context, density, measureGlobal, measureGroup
            )
        }

        val hasBothScripts = when {
            isSuper && baseNode is LatexNode.Subscript -> true
            !isSuper && baseNode is LatexNode.Superscript -> true
            else -> false
        }

        return if (hasBothScripts) {
            measureBothScripts(isSuper, baseNode, scriptNode, context, measurer, density, measureGlobal)
        } else {
            measureSingleScript(isSuper, baseNode, scriptNode, context, measurer, density, measureGlobal)
        }
    }

    /**
     * 检测是否为花括号标注场景
     * - \underbrace{...}_{text} → Subscript(base=Accent(UNDERBRACE, ...), index=text)
     * - \overbrace{...}^{text} → Superscript(base=Accent(OVERBRACE, ...), exponent=text)
     */
    private fun detectBraceAnnotation(isSuper: Boolean, baseNode: LatexNode): LatexNode.Accent? {
        if (baseNode !is LatexNode.Accent) return null
        return when {
            !isSuper && baseNode.accentType == LatexNode.Accent.AccentType.UNDERBRACE -> baseNode
            isSuper && baseNode.accentType == LatexNode.Accent.AccentType.OVERBRACE -> baseNode
            else -> null
        }
    }

    /**
     * 花括号标注布局：文本放在花括号下方/上方，居中对齐
     */
    private fun measureBraceAnnotation(
        accentNode: LatexNode.Accent,
        annotationNode: LatexNode,
        context: RenderContext,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val isUnder = accentNode.accentType == LatexNode.Accent.AccentType.UNDERBRACE
        
        // 测量花括号+内容（作为整体）
        val braceLayout = measureGlobal(accentNode, context)
        
        // 标注文本使用较小字号
        val scriptStyle = context.toScriptStyle()
        val annotationLayout = measureGlobal(annotationNode, scriptStyle)
        
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val gap = fontSizePx * 0.08f
        
        val totalWidth = max(braceLayout.width, annotationLayout.width)
        
        if (isUnder) {
            // \underbrace{...}_{text}: 花括号在上，标注在下
            val totalHeight = braceLayout.height + gap + annotationLayout.height
            val baseline = braceLayout.baseline
            
            return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
                val braceX = x + (totalWidth - braceLayout.width) / 2f
                braceLayout.draw(this, braceX, y)
                val annX = x + (totalWidth - annotationLayout.width) / 2f
                annotationLayout.draw(this, annX, y + braceLayout.height + gap)
            }
        } else {
            // \overbrace{...}^{text}: 标注在上，花括号在下
            val totalHeight = annotationLayout.height + gap + braceLayout.height
            val baseline = annotationLayout.height + gap + braceLayout.baseline
            
            return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
                val annX = x + (totalWidth - annotationLayout.width) / 2f
                annotationLayout.draw(this, annX, y)
                val braceX = x + (totalWidth - braceLayout.width) / 2f
                braceLayout.draw(this, braceX, y + annotationLayout.height + gap)
            }
        }
    }

    private fun measureBothScripts(
        isSuper: Boolean,
        baseNode: LatexNode,
        scriptNode: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val (realBase, otherScriptNode) = if (isSuper) {
            val subNode = baseNode as LatexNode.Subscript
            Pair(subNode.base, subNode.index)
        } else {
            val superNode = baseNode as LatexNode.Superscript
            Pair(superNode.base, superNode.exponent)
        }

        val scriptStyle = context.toScriptStyle()
        val realBaseLayout = measureGlobal(realBase, context)
        val currentScriptLayout = measureGlobal(scriptNode, scriptStyle)
        val otherScriptLayout = measureGlobal(otherScriptNode, scriptStyle)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val superscriptShift = fontSizePx * MathConstants.SUPERSCRIPT_SHIFT
        val subscriptShift = fontSizePx * MathConstants.SUBSCRIPT_SHIFT
        val scriptX = realBaseLayout.width + with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }

        val superLayout = if (isSuper) currentScriptLayout else otherScriptLayout
        val subLayout = if (isSuper) otherScriptLayout else currentScriptLayout
        val superRelY = -superscriptShift
        val subRelY = subscriptShift

        val superTopRel = superRelY - superLayout.baseline
        val superBottomRel = superRelY + (superLayout.height - superLayout.baseline)
        val subTopRel = subRelY - subLayout.baseline
        val subBottomRel = subRelY + (subLayout.height - subLayout.baseline)
        val baseTopRel = -realBaseLayout.baseline
        val baseBottomRel = realBaseLayout.height - realBaseLayout.baseline

        val maxTopRel = minOf(superTopRel, subTopRel, baseTopRel)
        val maxBottomRel = maxOf(superBottomRel, subBottomRel, baseBottomRel)

        val totalHeight = maxBottomRel - maxTopRel
        val baseline = -maxTopRel
        val width = scriptX + maxOf(superLayout.width, subLayout.width)

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            realBaseLayout.draw(this, x, y + baseline - realBaseLayout.baseline)
            superLayout.draw(this, x + scriptX, y + baseline + superRelY - superLayout.baseline)
            subLayout.draw(this, x + scriptX, y + baseline + subRelY - subLayout.baseline)
        }
    }

    private fun measureSingleScript(
        isSuper: Boolean,
        baseNode: LatexNode,
        scriptNode: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGlobal: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val scriptStyle = context.toScriptStyle()
        val baseLayout = measureGlobal(baseNode, context)
        val scriptLayout = measureGlobal(scriptNode, scriptStyle)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val superscriptShift = fontSizePx * MathConstants.SUPERSCRIPT_SHIFT
        val subscriptShift = fontSizePx * MathConstants.SUBSCRIPT_SHIFT
        val scriptX = baseLayout.width + with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }
        val scriptRelY = if (isSuper) -superscriptShift else subscriptShift

        val scriptTopRel = scriptRelY - scriptLayout.baseline
        val scriptBottomRel = scriptRelY + (scriptLayout.height - scriptLayout.baseline)
        val baseTopRel = -baseLayout.baseline
        val baseBottomRel = baseLayout.height - baseLayout.baseline

        val maxTopRel = min(scriptTopRel, baseTopRel)
        val maxBottomRel = max(scriptBottomRel, baseBottomRel)

        val totalHeight = maxBottomRel - maxTopRel
        val baseline = -maxTopRel
        val width = scriptX + scriptLayout.width

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            baseLayout.draw(this, x, y + baseline - baseLayout.baseline)
            scriptLayout.draw(this, x + scriptX, y + baseline + scriptRelY - scriptLayout.baseline)
        }
    }
}
