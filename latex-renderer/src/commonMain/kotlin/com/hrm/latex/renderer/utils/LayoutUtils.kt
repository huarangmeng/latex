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

package com.hrm.latex.renderer.utils

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle

/**
 * 布局工具类，提供通用的测量与对齐辅助方法
 */
internal object LayoutUtils {

    /**
     * 获取当前字体的数学轴 (Math Axis) 高度。
     * 数学轴是分数线、算子（如 +）垂直居中的参考线。
     * 
     * 算法：测量减号 '-'，其垂直中心即为数学轴。
     * 
     * @return 数学轴相对于基线的偏移量（向上为正）
     */
    fun getAxisHeight(density: Density, context: RenderContext, measurer: TextMeasurer): Float {
        val style = context.textStyle()
        val minusResult = measurer.measure("-", style)
        
        // 数学轴是减号的垂直中心
        // axisHeight = baseline - (height / 2)
        val axisHeight = minusResult.firstBaseline - (minusResult.size.height / 2f)

        // 合理性检查：防止字体度量异常
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val minReasonable = fontSizePx * 0.1f
        val maxReasonable = fontSizePx * 0.5f

        return if (axisHeight in minReasonable..maxReasonable) {
            axisHeight
        } else {
            // 回退到默认比例
            fontSizePx * MathConstants.MATH_AXIS_HEIGHT_RATIO
        }
    }
}
