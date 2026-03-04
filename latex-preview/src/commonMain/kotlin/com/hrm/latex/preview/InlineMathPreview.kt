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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig

/**
 * 行内数学公式预览分组
 */
val inlineMathPreviewGroups = listOf(
    PreviewGroup(
        id = "inline_basic",
        title = "基础行内公式",
        description = "在文本段落中嵌入数学公式",
        items = listOf(
            PreviewItem(
                id = "inline_simple",
                title = "简单行内公式",
                latex = "E = mc^2",
                content = {
                    InlineMathCard(
                        title = "简单行内公式",
                        textBefore = "爱因斯坦的质能方程 ",
                        formula = "E = mc^2",
                        textAfter = " 是物理学中最著名的公式之一。"
                    )
                }
            ),
            PreviewItem(
                id = "inline_fraction",
                title = "分数行内",
                latex = "\\frac{a}{b}",
                content = {
                    InlineMathCard(
                        title = "分数行内嵌入",
                        textBefore = "分数 ",
                        formula = "\\frac{a}{b}",
                        textAfter = " 表示 a 除以 b。"
                    )
                }
            ),
            PreviewItem(
                id = "inline_integral",
                title = "积分行内",
                latex = "\\int_{0}^{\\infty} e^{-x} dx",
                content = {
                    InlineMathCard(
                        title = "积分行内嵌入",
                        textBefore = "积分 ",
                        formula = "\\int_{0}^{\\infty} e^{-x} dx",
                        textAfter = " 的结果为 1。"
                    )
                }
            ),
            PreviewItem(
                id = "inline_sqrt",
                title = "根号行内",
                latex = "\\sqrt{x^2 + y^2}",
                content = {
                    InlineMathCard(
                        title = "根号行内嵌入",
                        textBefore = "向量的模 ",
                        formula = "\\sqrt{x^2 + y^2}",
                        textAfter = " 表示到原点的距离。"
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "inline_multi",
        title = "多公式段落",
        description = "一段文本中嵌入多个数学公式",
        items = listOf(
            PreviewItem(
                id = "inline_multi_formulas",
                title = "多公式混排",
                latex = "a^2 + b^2 = c^2",
                content = {
                    MultiInlineMathCard(
                        title = "多公式混排",
                        segments = listOf(
                            TextSegment.Plain("勾股定理 "),
                            TextSegment.Math("a^2 + b^2 = c^2"),
                            TextSegment.Plain(" 中，当 "),
                            TextSegment.Math("c = 5"),
                            TextSegment.Plain(" 且 "),
                            TextSegment.Math("a = 3"),
                            TextSegment.Plain(" 时，"),
                            TextSegment.Math("b = 4"),
                            TextSegment.Plain("。"),
                        )
                    )
                }
            ),
            PreviewItem(
                id = "inline_euler",
                title = "欧拉公式段落",
                latex = "e^{i\\pi} + 1 = 0",
                content = {
                    MultiInlineMathCard(
                        title = "欧拉公式段落",
                        segments = listOf(
                            TextSegment.Plain("欧拉恒等式 "),
                            TextSegment.Math("e^{i\\pi} + 1 = 0"),
                            TextSegment.Plain(" 将五个基本常数 "),
                            TextSegment.Math("e"),
                            TextSegment.Plain("、"),
                            TextSegment.Math("i"),
                            TextSegment.Plain("、"),
                            TextSegment.Math("\\pi"),
                            TextSegment.Plain("、"),
                            TextSegment.Math("1"),
                            TextSegment.Plain(" 和 "),
                            TextSegment.Math("0"),
                            TextSegment.Plain(" 联系在了一起。"),
                        )
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "inline_batch",
        title = "批量测量",
        description = "使用 measureBatch 批量预测量公式",
        items = listOf(
            PreviewItem(
                id = "inline_batch_demo",
                title = "批量测量演示",
                latex = "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}",
                content = { BatchMeasureCard() }
            ),
        )
    ),
)

// ========== 文本段落模型 ==========

sealed class TextSegment {
    data class Plain(val text: String) : TextSegment()
    data class Math(val latex: String) : TextSegment()
}

// ========== 演示 Composable ==========

/**
 * 单个行内公式演示卡片
 */
@Composable
fun InlineMathCard(
    title: String,
    textBefore: String,
    formula: String,
    textAfter: String
) {
    val config = LatexConfig(fontSize = 16.sp)
    val measurer = rememberLatexMeasurer(config)
    val density = LocalDensity.current

    val dims = measurer.measure(formula, config)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            if (dims != null) {
                val widthSp = with(density) { dims.widthPx.toSp() }
                val heightSp = with(density) { dims.heightPx.toSp() }

                val inlineContent = mapOf(
                    "formula" to InlineTextContent(
                        placeholder = Placeholder(
                            width = widthSp,
                            height = heightSp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        Latex(latex = formula, config = config)
                    }
                )

                val annotated = buildAnnotatedString {
                    append(textBefore)
                    appendInlineContent("formula", formula)
                    append(textAfter)
                }

                Text(
                    text = annotated,
                    inlineContent = inlineContent,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text("测量失败", color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider()

            Text(
                text = "LaTeX: $formula",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dims != null) {
                Text(
                    text = "尺寸: %.1f × %.1f px, 基线: %.1f px".format(
                        dims.widthPx, dims.heightPx, dims.baselinePx
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 多公式混排演示卡片
 */
@Composable
fun MultiInlineMathCard(
    title: String,
    segments: List<TextSegment>
) {
    val config = LatexConfig(fontSize = 16.sp)
    val measurer = rememberLatexMeasurer(config)
    val density = LocalDensity.current

    val mathSegments = segments.filterIsInstance<TextSegment.Math>()
    val dimensions = measurer.measureBatch(
        mathSegments.map { it.latex }, config
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            val dimMap = mutableMapOf<String, Int>()
            val inlineContent = mutableMapOf<String, InlineTextContent>()

            var mathIndex = 0
            for (segment in segments) {
                if (segment is TextSegment.Math) {
                    val dims = dimensions[mathIndex]
                    val id = "math_$mathIndex"
                    if (dims != null) {
                        val widthSp = with(density) { dims.widthPx.toSp() }
                        val heightSp = with(density) { dims.heightPx.toSp() }
                        val latex = segment.latex
                        inlineContent[id] = InlineTextContent(
                            placeholder = Placeholder(
                                width = widthSp,
                                height = heightSp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            Latex(latex = latex, config = config)
                        }
                    }
                    dimMap[segment.latex] = mathIndex
                    mathIndex++
                }
            }

            val annotated = buildAnnotatedString {
                var mi = 0
                for (segment in segments) {
                    when (segment) {
                        is TextSegment.Plain -> append(segment.text)
                        is TextSegment.Math -> {
                            val id = "math_$mi"
                            appendInlineContent(id, segment.latex)
                            mi++
                        }
                    }
                }
            }

            Text(
                text = annotated,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 批量测量演示卡片
 */
@Composable
fun BatchMeasureCard() {
    val config = LatexConfig(fontSize = 16.sp)
    val measurer = rememberLatexMeasurer(config)

    val formulas = listOf(
        "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}",
        "e^{i\\pi} + 1 = 0",
        "\\int_0^1 x^2 dx = \\frac{1}{3}",
        "\\sqrt{2} \\approx 1.414",
    )

    val results = measurer.measureBatch(formulas, config)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("批量测量演示", style = MaterialTheme.typography.titleSmall)
            Text(
                "使用 measureBatch() 一次测量 ${formulas.size} 个公式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            formulas.forEachIndexed { index, latex ->
                val dims = results[index]
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Latex(latex = latex, config = config)
                    if (dims != null) {
                        Text(
                            text = "→ %.1f × %.1f px (baseline: %.1f px)".format(
                                dims.widthPx, dims.heightPx, dims.baselinePx
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (index < formulas.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
