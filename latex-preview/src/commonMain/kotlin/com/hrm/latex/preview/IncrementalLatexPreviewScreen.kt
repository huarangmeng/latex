package com.hrm.latex.preview

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * 增量 LaTeX 预览示例
 * 展示增量解析和渲染能力
 */

val incrementalLatexPreviewGroups = listOf(
    PreviewGroup(
        id = "incremental_realtime",
        title = "1. 实时输入演示",
        description = "模拟用户逐字输入，实时渲染",
        items = listOf(
            PreviewItem("rt1", "实时输入预览", "", content = { Preview_Demo_RealTimeInput() })
        )
    ),
    PreviewGroup(
        id = "incremental_progress",
        title = "2. 解析进度追踪",
        description = "显示解析进度条，展示增量解析能力",
        items = listOf(
            PreviewItem("pg1", "进度追踪演示", "", content = { Preview_Demo_ProgressTracking() })
        )
    ),
    PreviewGroup(
        id = "incremental_error",
        title = "3. 错误恢复",
        description = "展示不完整公式的错误处理",
        items = listOf(
            PreviewItem("err1", "错误恢复演示", "", content = { Preview_Demo_ErrorRecovery() })
        )
    ),
    PreviewGroup(
        id = "incremental_multiple",
        title = "4. 多公式同时渲染",
        description = "多个公式同时进行增量渲染",
        items = listOf(
            PreviewItem("mul1", "多公式演示", "", content = { Preview_Demo_MultipleFormulas() })
        )
    ),
    PreviewGroup(
        id = "incremental_comparison",
        title = "5. 标准 vs 增量对比",
        description = "对比标准渲染和增量渲染的差异",
        items = listOf(
            PreviewItem("cmp1", "对比演示", "", content = { Preview_Demo_ComparisonWithStandard() })
        )
    ),
)

@Preview
@Composable
fun IncrementalLatexPreview(onBack: () -> Unit) {
    PreviewCategoryScreen(
        title = "增量 LaTeX",
        groups = incrementalLatexPreviewGroups,
        onBack = onBack
    )
}
