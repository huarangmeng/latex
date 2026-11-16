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

import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.utils.MathAtomSequence
import com.hrm.latex.renderer.utils.MathSpacing

/** Shared drawing, hit-testing and wrapping metrics. Range queries remain O(1). */
internal class InlineRowMetrics(
    private val nodes: List<LatexNode>, layouts: List<NodeLayout>,
    private val context: RenderContext, density: Density,
    private val nodeContexts: List<RenderContext>? = null,
) {
    private val atoms = MathAtomSequence(nodes)
    private val fontSizePx = with(density) { context.fontSize.toPx() }
    private val spacings = FloatArray(nodes.size)
    private val widthPrefix = DoubleArray(nodes.size + 1)
    private val spacingPrefix = DoubleArray(nodes.size + 1)

    init {
        for (i in nodes.indices) {
            spacings[i] = spacing(i, 0, nodes.size)
            widthPrefix[i + 1] = widthPrefix[i] + layouts[i].width
            spacingPrefix[i + 1] = spacingPrefix[i] + spacings[i]
        }
    }

    fun width(start: Int, end: Int): Float {
        if (start >= end) return 0f
        var result = widthPrefix[end] - widthPrefix[start] + spacingPrefix[end - 1] - spacingPrefix[start]
        if (start == 0 && end == nodes.size) return result.toFloat()
        val first = atoms.first(start)
        val last = atoms.last(end)
        if (first > last) return result.toFloat()
        // Internal alternating bin edges stay 4mu; only these three edges change.
        val firstEdge = atoms.indices[first] - 1
        val afterRun = atoms.afterLeadingBins(start)
        val runEdge = if (atoms.isBin(first) && afterRun <= last) atoms.indices[afterRun] - 1 else -1
        val lastEdge = if (atoms.isBin(last)) atoms.indices[last] - 1 else -1
        fun correction(edge: Int): Double = if (edge >= start && edge < end - 1)
            (spacing(edge, start, end) - spacings[edge]).toDouble() else 0.0
        result += correction(firstEdge)
        if (runEdge != firstEdge) result += correction(runEdge)
        if (lastEdge != firstEdge && lastEdge != runEdge) result += correction(lastEdge)
        return result.toFloat()
    }

    fun spacing(index: Int, start: Int, end: Int): Float {
        if (context.inTextMode || index + 1 >= end) return 0f
        val right = atoms.ordinalAt[index + 1]
        if (right <= 0 || right == atoms.first(start)) return 0f
        val leftIndex = atoms.indices[right - 1]
        if (leftIndex < start) return 0f
        val rightContext = nodeContexts?.get(index + 1) ?: context
        val isScript = rightContext.mathStyle == MathStyle.SCRIPT || rightContext.mathStyle == MathStyle.SCRIPT_SCRIPT
        return MathSpacing.spaceBetween(
            atoms.typeAt(leftIndex, start, end), atoms.typeAt(index + 1, start, end), isScript
        ) * fontSizePx
    }
}
