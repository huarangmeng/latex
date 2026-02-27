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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.export.ExportConfig
import com.hrm.latex.renderer.export.ExportResult
import com.hrm.latex.renderer.export.ImageFormat
import com.hrm.latex.renderer.export.rememberLatexExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 导出功能预览分组
 */
val exportPreviewGroups = listOf(
    PreviewGroup(
        id = "export_basic",
        title = "PNG 导出",
        description = "将 LaTeX 公式导出为 PNG 图片",
        items = listOf(
            PreviewItem(
                id = "export_simple",
                title = "简单公式导出",
                latex = "E = mc^2",
                content = { ExportPreviewCard(latex = "E = mc^2", title = "简单公式") }
            ),
            PreviewItem(
                id = "export_fraction",
                title = "分数公式导出",
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                content = {
                    ExportPreviewCard(
                        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                        title = "二次方程求根公式"
                    )
                }
            ),
            PreviewItem(
                id = "export_integral",
                title = "积分公式导出",
                latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                content = {
                    ExportPreviewCard(
                        latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                        title = "高斯积分"
                    )
                }
            ),
            PreviewItem(
                id = "export_matrix",
                title = "矩阵导出",
                latex = "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
                content = {
                    ExportPreviewCard(
                        latex = "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
                        title = "矩阵"
                    )
                }
            ),
            PreviewItem(
                id = "export_scale",
                title = "不同缩放倍率对比",
                latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}",
                content = { ExportScaleComparisonCard() }
            )
        )
    ),
    PreviewGroup(
        id = "export_jpeg",
        title = "JPEG 导出",
        description = "将 LaTeX 公式导出为 JPEG 图片（不支持透明背景）",
        items = listOf(
            PreviewItem(
                id = "export_jpeg_simple",
                title = "JPEG 简单公式导出",
                latex = "E = mc^2",
                content = {
                    ExportPreviewCard(
                        latex = "E = mc^2",
                        title = "简单公式 (JPEG)",
                        format = ImageFormat.JPEG
                    )
                }
            ),
            PreviewItem(
                id = "export_jpeg_fraction",
                title = "JPEG 分数公式导出",
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                content = {
                    ExportPreviewCard(
                        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                        title = "求根公式 (JPEG)",
                        format = ImageFormat.JPEG
                    )
                }
            ),
            PreviewItem(
                id = "export_format_comparison",
                title = "PNG vs JPEG 格式对比",
                latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}",
                content = { ExportFormatComparisonCard() }
            )
        )
    ),
    PreviewGroup(
        id = "export_webp",
        title = "WEBP 导出",
        description = "将 LaTeX 公式导出为 WEBP 图片（支持透明背景，文件体积更小）",
        items = listOf(
            PreviewItem(
                id = "export_webp_simple",
                title = "WEBP 简单公式导出",
                latex = "E = mc^2",
                content = {
                    ExportPreviewCard(
                        latex = "E = mc^2",
                        title = "简单公式 (WEBP)",
                        format = ImageFormat.WEBP
                    )
                }
            ),
            PreviewItem(
                id = "export_webp_fraction",
                title = "WEBP 分数公式导出",
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                content = {
                    ExportPreviewCard(
                        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                        title = "求根公式 (WEBP)",
                        format = ImageFormat.WEBP
                    )
                }
            ),
            PreviewItem(
                id = "export_all_format_comparison",
                title = "PNG vs JPEG vs WEBP 格式对比",
                latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}",
                content = { ExportAllFormatComparisonCard() }
            )
        )
    )
)

/**
 * 单个公式的导出预览卡片
 */
@Composable
private fun ExportPreviewCard(
    latex: String,
    title: String,
    format: ImageFormat = ImageFormat.PNG
) {
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    val formatName = when (format) {
        ImageFormat.PNG -> "PNG"
        ImageFormat.JPEG -> "JPEG"
        ImageFormat.WEBP -> "WEBP"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("原始渲染:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, isDarkTheme = false)

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isExporting = true
                    scope.launch {
                        exportResult = withContext(Dispatchers.Default) {
                            exporter.export(
                                latex = latex,
                                exportConfig = ExportConfig(scale = 2f, format = format)
                            )
                        }
                        isExporting = false
                    }
                },
                enabled = !isExporting
            ) {
                Text(if (isExporting) "导出中..." else "导出 $formatName (2x)")
            }
        }

        exportResult?.let { result ->
            Spacer(Modifier.height(4.dp))
            Text(
                "导出结果: ${result.width} x ${result.height} px" +
                        (result.bytes?.size?.let { " | $formatName: ${it / 1024} KB" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = result.imageBitmap,
                    contentDescription = "导出的 LaTeX 图片: $title",
                    modifier = Modifier.padding(8.dp),
                    contentScale = ContentScale.Inside
                )
            }
        }
    }
}

/**
 * PNG vs JPEG 格式对比卡片
 */
@Composable
private fun ExportFormatComparisonCard() {
    val latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}"
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()
    var pngResult by remember { mutableStateOf<ExportResult?>(null) }
    var jpegResult by remember { mutableStateOf<ExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("原始渲染:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, isDarkTheme = false)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    pngResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            exportConfig = ExportConfig(scale = 2f, format = ImageFormat.PNG)
                        )
                    }
                    jpegResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            exportConfig = ExportConfig(
                                scale = 2f,
                                format = ImageFormat.JPEG,
                                quality = 90
                            )
                        )
                    }
                    isExporting = false
                }
            },
            enabled = !isExporting
        ) {
            Text(if (isExporting) "导出中..." else "PNG vs JPEG 对比 (2x)")
        }

        pngResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "PNG: ${result.width} x ${result.height} px" +
                                (result.bytes?.size?.let { " | ${it / 1024} KB" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        bitmap = result.imageBitmap,
                        contentDescription = "PNG export",
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }

        jpegResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "JPEG (quality=90): ${result.width} x ${result.height} px" +
                                (result.bytes?.size?.let { " | ${it / 1024} KB" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        bitmap = result.imageBitmap,
                        contentDescription = "JPEG export",
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }
    }
}

/**
 * PNG vs JPEG vs WEBP 三种格式对比卡片
 */
@Composable
private fun ExportAllFormatComparisonCard() {
    val latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}"
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()
    var pngResult by remember { mutableStateOf<ExportResult?>(null) }
    var jpegResult by remember { mutableStateOf<ExportResult?>(null) }
    var webpResult by remember { mutableStateOf<ExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("原始渲染:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, isDarkTheme = false)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    pngResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            exportConfig = ExportConfig(scale = 2f, format = ImageFormat.PNG)
                        )
                    }
                    jpegResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            exportConfig = ExportConfig(
                                scale = 2f,
                                format = ImageFormat.JPEG,
                                quality = 90
                            )
                        )
                    }
                    webpResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            exportConfig = ExportConfig(
                                scale = 2f,
                                format = ImageFormat.WEBP,
                                quality = 90
                            )
                        )
                    }
                    isExporting = false
                }
            },
            enabled = !isExporting
        ) {
            Text(if (isExporting) "导出中..." else "PNG vs JPEG vs WEBP 对比 (2x)")
        }

        pngResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "PNG: ${result.width} x ${result.height} px" +
                                (result.bytes?.size?.let { " | ${it / 1024} KB" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        bitmap = result.imageBitmap,
                        contentDescription = "PNG export",
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }

        jpegResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "JPEG (quality=90): ${result.width} x ${result.height} px" +
                                (result.bytes?.size?.let { " | ${it / 1024} KB" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        bitmap = result.imageBitmap,
                        contentDescription = "JPEG export",
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }

        webpResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "WEBP (quality=90): ${result.width} x ${result.height} px" +
                                (result.bytes?.size?.let { " | ${it / 1024} KB" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        bitmap = result.imageBitmap,
                        contentDescription = "WEBP export",
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }
    }
}

/**
 * 不同缩放倍率对比卡片
 */
@Composable
private fun ExportScaleComparisonCard() {
    val latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}"
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<Map<Float, ExportResult>>(emptyMap()) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("原始渲染:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, isDarkTheme = false)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    val scales = listOf(1f, 2f, 3f)
                    val newResults = mutableMapOf<Float, ExportResult>()
                    for (scale in scales) {
                        val result = withContext(Dispatchers.Default) {
                            exporter.export(
                                latex = latex,
                                exportConfig = ExportConfig(scale = scale)
                            )
                        }
                        result?.let { newResults[scale] = it }
                    }
                    results = newResults
                    isExporting = false
                }
            },
            enabled = !isExporting
        ) {
            Text(if (isExporting) "导出中..." else "导出 1x / 2x / 3x 对比")
        }

        results.forEach { (scale, result) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "${scale}x: ${result.width} x ${result.height} px",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        bitmap = result.imageBitmap,
                        contentDescription = "LaTeX ${scale}x export",
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }
    }
}
