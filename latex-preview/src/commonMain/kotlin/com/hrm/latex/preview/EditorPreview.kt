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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.editor.LatexEditor
import com.hrm.latex.renderer.editor.LatexTemplate
import com.hrm.latex.renderer.editor.rememberEditorState
import com.hrm.latex.renderer.model.LatexConfig

/**
 * 编辑器预览分组
 */
val editorPreviewGroups: List<PreviewGroup> = listOf(
    PreviewGroup(
        id = "editor-basic",
        title = "基础编辑器",
        description = "LaTeX 编辑器的基础使用场景",
        items = listOf(
            PreviewItem(
                id = "editor-empty",
                title = "空编辑器",
                latex = "",
                content = { EditorEmptyPreview() }
            ),
            PreviewItem(
                id = "editor-simple",
                title = "简单公式编辑",
                latex = "x^2 + y^2 = z^2",
                content = { EditorSimplePreview() }
            ),
            PreviewItem(
                id = "editor-complex",
                title = "复杂公式编辑",
                latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                content = { EditorComplexPreview() }
            ),
            PreviewItem(
                id = "editor-templates",
                title = "模板插入",
                latex = "",
                content = { EditorTemplatePreview() }
            ),
            PreviewItem(
                id = "editor-source",
                title = "显示源码",
                latex = "\\frac{a}{b}",
                content = { EditorSourcePreview() }
            ),
        )
    )
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditorEmptyPreview() {
    val state = rememberEditorState("")
    LatexEditor(
        editorState = state,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        config = LatexConfig()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditorSimplePreview() {
    val state = rememberEditorState("x^2 + y^2 = z^2")
    LatexEditor(
        editorState = state,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        config = LatexConfig()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditorComplexPreview() {
    val state = rememberEditorState("\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}")
    LatexEditor(
        editorState = state,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        config = LatexConfig()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditorTemplatePreview() {
    val state = rememberEditorState("f(x) = ")

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "点击模板按钮插入 LaTeX 结构",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 模板按钮
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LatexTemplate.entries.forEach { template ->
                AssistChip(
                    onClick = { state.insertTemplate(template) },
                    label = {
                        Text("${template.icon} ${template.displayName}")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LatexEditor(
            editorState = state,
            config = LatexConfig()
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditorSourcePreview() {
    val state = rememberEditorState("\\frac{a}{b}")
    LatexEditor(
        editorState = state,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        config = LatexConfig(),
        showSourceText = true
    )
}
