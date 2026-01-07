package com.hrm.latex.renderer.utils

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.base.LatexConstants
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
            fontSizePx * LatexConstants.MATH_AXIS_HEIGHT_RATIO
        }
    }
}
