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
 * LaTeX 基础模块常量
 * 
 * 仅包含解析器相关的配置参数。
 * 渲染相关的排版常量已迁移至 latex-renderer 模块的 MathConstants 对象。
 */
object LatexConstants {

    // ========== 性能参数 ==========

    /** 增量解析时，第一阶段精细回退的字符数范围 */
    const val INCREMENTAL_PARSE_FINE_BACKTRACK_RANGE = 100

    /** 增量解析时，第二阶段快速回退的步进大小 */
    const val INCREMENTAL_PARSE_FAST_BACKTRACK_STEP = 5

    /** 增量解析快速路径的最大输入长度（超过此长度使用完整解析） */
    const val INCREMENTAL_PARSE_FAST_PATH_MAX_LENGTH = 5
}