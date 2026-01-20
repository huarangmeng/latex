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


package com.hrm.latex.base

/**
 * LaTeX 渲染相关常量
 * 定义了排版过程中使用的各种缩放因子、偏移比例和间距参数
 */
object LatexConstants {

    // ========== 字体缩放因子 ==========

    /** 分数的分子和分母字体缩放因子 */
    const val FRACTION_SCALE_FACTOR = 0.9f

    /** 根号指数的字体缩放因子 */
    const val ROOT_INDEX_SCALE_FACTOR = 0.6f

    /** 上标和下标的字体缩放因子 */
    const val SCRIPT_SCALE_FACTOR = 0.7f

    /** 矩阵环境的字体缩放因子 */
    const val MATRIX_SCALE_FACTOR = 0.9f

    /** 小括号内容的字体缩放因子 */
    const val BINOMIAL_SCALE_FACTOR = 0.9f

    /** 大型运算符上下限的字体缩放因子 */
    const val OPERATOR_LIMIT_SCALE_FACTOR = 0.7f

    // ========== 布局比例参数 ==========

    /** 分数线粗细相对于字体大小的比例 */
    const val FRACTION_RULE_THICKNESS_RATIO = 0.05f

    /** 数学轴高度相对于字体大小的比例（通常位于字符中线） */
    const val MATH_AXIS_HEIGHT_RATIO = 0.25f

    /** 上标向上偏移相对于字体大小的比例 */
    const val SUPERSCRIPT_SHIFT_RATIO = 0.45f

    /** 下标向下偏移相对于字体大小的比例 */
    const val SUBSCRIPT_SHIFT_RATIO = 0.25f

    /** 上下标之间的最小间距比例 */
    const val SCRIPT_MIN_GAP_RATIO = 0.1f

    /** 根号钩子宽度相对于字体大小的比例 */
    const val ROOT_HOOK_WIDTH_RATIO = 0.3f

    /** 根号钩子高度相对于字体大小的比例 */
    const val ROOT_HOOK_HEIGHT_RATIO = 0.4f

    /** 根号指数偏移量相对于字体大小的比例 */
    const val ROOT_INDEX_OFFSET_RATIO = 0.5f

    /** 装饰符号（如帽子、波浪线）距离内容的间距比例 */
    const val ACCENT_GAP_RATIO = 0.05f

    // ========== 间距参数 ==========

    /** 矩阵/表格列之间的默认间距（相对于字体大小） */
    const val MATRIX_COLUMN_SPACING_RATIO = 0.5f

    /** 矩阵/表格行之间的默认间距（相对于字体大小） */
    const val MATRIX_ROW_SPACING_RATIO = 0.3f

    /** 分数线上方的额外间距比例 */
    const val FRACTION_TOP_PADDING_RATIO = 0.15f

    /** 分数线下方的额外间距比例 */
    const val FRACTION_BOTTOM_PADDING_RATIO = 0.15f

    /** 大型运算符与上下限之间的间距比例 */
    const val OPERATOR_LIMIT_GAP_RATIO = 0.1f

    /** 括号内部左右内边距比例 */
    const val BRACKET_INNER_PADDING_RATIO = 0.15f

    // ========== 性能参数 ==========

    /** 增量解析时，第一阶段精细回退的字符数范围 */
    const val INCREMENTAL_PARSE_FINE_BACKTRACK_RANGE = 100

    /** 增量解析时，第二阶段快速回退的步进大小 */
    const val INCREMENTAL_PARSE_FAST_BACKTRACK_STEP = 5

    /** 增量解析快速路径的最大输入长度（超过此长度使用完整解析） */
    const val INCREMENTAL_PARSE_FAST_PATH_MAX_LENGTH = 5

    // ========== 绘制参数 ==========

    /** 括号/分隔符的默认线条宽度（dp） */
    const val BRACKET_STROKE_WIDTH_DP = 1.5f

    /** 分数线/横线的默认线条宽度系数 */
    const val RULE_LINE_WIDTH_FACTOR = 1.0f

    /** 根号线条宽度（dp） */
    const val ROOT_STROKE_WIDTH_DP = 2.0f
}