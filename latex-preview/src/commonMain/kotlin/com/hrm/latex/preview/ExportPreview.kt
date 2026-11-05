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
import com.hrm.latex.renderer.export.SvgExportConfig
import com.hrm.latex.renderer.export.SvgExportResult
import com.hrm.latex.renderer.export.SvgTextMode
import com.hrm.latex.renderer.export.rememberLatexExporter
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 导出功能预览分组
 */
val exportPreviewGroups = listOf(
    PreviewGroup(
        id = "export_svg",
        title = "SVG export",
        description = "Export scalable SVG with portable glyph paths or selectable text",
        items = listOf(
            PreviewItem(
                id = "export_svg_modes",
                title = "SVG path and text modes",
                latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                content = { SvgExportPreviewCard() }
            )
        )
    ),
    PreviewGroup(
        id = "export_basic",
        title = "PNG export",
        description = "Export formulas as PNG images with transparent backgrounds",
        items = listOf(
            PreviewItem(
                id = "export_simple",
                title = "Export a simple formula",
                latex = "E = mc^2",
                content = { ExportPreviewCard(latex = "E = mc^2", title = "Simple formula") }
            ),
            PreviewItem(
                id = "export_fraction",
                title = "Export a fraction",
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                content = {
                    ExportPreviewCard(
                        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                        title = "Quadratic formula"
                    )
                }
            ),
            PreviewItem(
                id = "export_integral",
                title = "Export an integral",
                latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                content = {
                    ExportPreviewCard(
                        latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
                        title = "Gaussian integral"
                    )
                }
            ),
            PreviewItem(
                id = "export_matrix",
                title = "Export a matrix",
                latex = "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
                content = {
                    ExportPreviewCard(
                        latex = "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
                        title = "Matrix"
                    )
                }
            ),
            PreviewItem(
                id = "export_scale",
                title = "Compare export resolutions",
                latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}",
                content = { ExportScaleComparisonCard() }
            )
        )
    ),
    PreviewGroup(
        id = "export_jpeg",
        title = "JPEG export",
        description = "Export formulas as JPEG images with an opaque background",
        items = listOf(
            PreviewItem(
                id = "export_jpeg_simple",
                title = "Export a simple formula as JPEG",
                latex = "E = mc^2",
                content = {
                    ExportPreviewCard(
                        latex = "E = mc^2",
                        title = "Simple formula (JPEG)",
                        format = ImageFormat.JPEG
                    )
                }
            ),
            PreviewItem(
                id = "export_jpeg_fraction",
                title = "Export a fraction as JPEG",
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                content = {
                    ExportPreviewCard(
                        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                        title = "Quadratic formula (JPEG)",
                        format = ImageFormat.JPEG
                    )
                }
            ),
            PreviewItem(
                id = "export_format_comparison",
                title = "Compare PNG and JPEG",
                latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}",
                content = { ExportFormatComparisonCard() }
            )
        )
    ),
    PreviewGroup(
        id = "export_webp",
        title = "WebP export",
        description = "Export formulas as compact WebP images with transparent backgrounds",
        items = listOf(
            PreviewItem(
                id = "export_webp_simple",
                title = "Export a simple formula as WebP",
                latex = "E = mc^2",
                content = {
                    ExportPreviewCard(
                        latex = "E = mc^2",
                        title = "Simple formula (WebP)",
                        format = ImageFormat.WEBP
                    )
                }
            ),
            PreviewItem(
                id = "export_webp_fraction",
                title = "Export a fraction as WebP",
                latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                content = {
                    ExportPreviewCard(
                        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                        title = "Quadratic formula (WebP)",
                        format = ImageFormat.WEBP
                    )
                }
            ),
            PreviewItem(
                id = "export_all_format_comparison",
                title = "Compare PNG, JPEG, and WebP",
                latex = "\\sum_{k=1}^{n} k = \\frac{n(n+1)}{2}",
                content = { ExportAllFormatComparisonCard() }
            )
        )
    )
)

@Composable
private fun SvgExportPreviewCard() {
    val latex = "\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}"
    val config = LatexConfig(theme = LatexTheme.material3())
    val exporter = rememberLatexExporter(config)
    val scope = rememberCoroutineScope()
    var pathResult by remember { mutableStateOf<SvgExportResult?>(null) }
    var textResult by remember { mutableStateOf<SvgExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Original rendering:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, config = config)

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    pathResult = withContext(Dispatchers.Default) {
                        exporter.exportSvg(latex = latex, config = config)
                    }
                    textResult = withContext(Dispatchers.Default) {
                        exporter.exportSvg(
                            latex = latex,
                            config = config,
                            exportConfig = SvgExportConfig(textMode = SvgTextMode.TEXT)
                        )
                    }
                    isExporting = false
                }
            },
            enabled = !isExporting
        ) {
            Text(if (isExporting) "Exporting..." else "Export SVG paths and text")
        }

        pathResult?.let { result -> SvgResultSummary("PATH: portable glyph outlines", result) }
        textResult?.let { result -> SvgResultSummary("TEXT: selectable text", result) }
    }
}

@Composable
private fun SvgResultSummary(label: String, result: SvgExportResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "$label: ${result.width} x ${result.height} | ${result.bytes.size} bytes",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                result.svg.take(180).replace('\n', ' ') + if (result.svg.length > 180) "…" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 单个公式的导出预览卡片
 */
@Composable
private fun ExportPreviewCard(
    latex: String,
    title: String,
    format: ImageFormat = ImageFormat.PNG
) {
    val config = LatexConfig(theme = LatexTheme.material3())
    val exporter = rememberLatexExporter(config)
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
        Text("Original rendering:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, config = config)

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
                                config = config,
                                exportConfig = ExportConfig(scale = 2f, format = format)
                            )
                        }
                        isExporting = false
                    }
                },
                enabled = !isExporting
            ) {
                Text(if (isExporting) "Exporting..." else "Export $formatName (2x)")
            }
        }

        exportResult?.let { result ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Exported image: ${result.width} x ${result.height} px" +
                        (result.bytes?.size?.let { " | $formatName: ${it / 1024} KB" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = result.imageBitmap,
                    contentDescription = "Exported LaTeX image: $title",
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
    val config = LatexConfig(theme = LatexTheme.material3())
    val exporter = rememberLatexExporter(config)
    val scope = rememberCoroutineScope()
    var pngResult by remember { mutableStateOf<ExportResult?>(null) }
    var jpegResult by remember { mutableStateOf<ExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Original rendering:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, config = config)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    pngResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            config = config,
                            exportConfig = ExportConfig(scale = 2f, format = ImageFormat.PNG)
                        )
                    }
                    jpegResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            config = config,
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
            Text(if (isExporting) "Exporting..." else "Compare PNG and JPEG at 2x")
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
    val config = LatexConfig(theme = LatexTheme.material3())
    val exporter = rememberLatexExporter(config)
    val scope = rememberCoroutineScope()
    var pngResult by remember { mutableStateOf<ExportResult?>(null) }
    var jpegResult by remember { mutableStateOf<ExportResult?>(null) }
    var webpResult by remember { mutableStateOf<ExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Original rendering:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, config = config)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    pngResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            config = config,
                            exportConfig = ExportConfig(scale = 2f, format = ImageFormat.PNG)
                        )
                    }
                    jpegResult = withContext(Dispatchers.Default) {
                        exporter.export(
                            latex = latex,
                            config = config,
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
                            config = config,
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
            Text(if (isExporting) "Exporting..." else "Compare PNG, JPEG, and WebP at 2x")
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
    val config = LatexConfig(theme = LatexTheme.material3())
    val exporter = rememberLatexExporter(config)
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<Map<Float, ExportResult>>(emptyMap()) }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Original rendering:", style = MaterialTheme.typography.labelMedium)
        Latex(latex = latex, config = config)

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
                                config = config,
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
            Text(if (isExporting) "Exporting..." else "Compare exports at 1x, 2x, and 3x")
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
