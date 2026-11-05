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


package com.hrm.latex.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * 增量 LaTeX 预览示例
 * 展示增量解析和渲染能力
 */

val incrementalLatexPreviewGroups = listOf(
    PreviewGroup(
        id = "incremental_realtime",
        title = "1. Live typing",
        description = "Watch a formula render as each character is typed",
        items = listOf(
            PreviewItem("rt1", "Live typing preview", "", content = { Preview_Demo_RealTimeInput() })
        )
    ),
    PreviewGroup(
        id = "incremental_progress",
        title = "2. Parsing progress",
        description = "Track parsing progress while a formula is being entered",
        items = listOf(
            PreviewItem("pg1", "Parsing progress", "", content = { Preview_Demo_ProgressTracking() })
        )
    ),
    PreviewGroup(
        id = "incremental_error",
        title = "3. Incomplete input",
        description = "See how unfinished commands and missing delimiters are handled",
        items = listOf(
            PreviewItem("err1", "Error recovery", "", content = { Preview_Demo_ErrorRecovery() })
        )
    ),
    PreviewGroup(
        id = "incremental_multiple",
        title = "4. Multiple formulas",
        description = "Render several formulas incrementally at the same time",
        items = listOf(
            PreviewItem("mul1", "Multiple formulas", "", content = { Preview_Demo_MultipleFormulas() })
        )
    ),
    PreviewGroup(
        id = "incremental_comparison",
        title = "5. Complete and incremental rendering",
        description = "Compare a complete formula with its progressively typed version",
        items = listOf(
            PreviewItem("cmp1", "Rendering comparison", "", content = { Preview_Demo_ComparisonWithStandard() })
        )
    ),
)

@Preview
@Composable
fun IncrementalLatexPreview(onBack: () -> Unit) {
    PreviewCategoryScreen(
        title = "Incremental rendering",
        groups = incrementalLatexPreviewGroups,
        onBack = onBack
    )
}
