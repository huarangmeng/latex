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
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import kotlin.math.round

private fun Float.toFixed1(): String = (round(this * 10) / 10).toString()

/**
 * 行内数学公式预览分组
 */
val inlineMathPreviewGroups = listOf(
    PreviewGroup(
        id = "inline_basic",
        title = "Inline formulas",
        description = "Embed a formula in a sentence and align it with the text baseline",
        items = listOf(
            PreviewItem(
                id = "inline_simple",
                title = "Inline equation",
                latex = "E = mc^2",
                content = {
                    InlineMathCard(
                        title = "Inline equation",
                        textBefore = "Einstein's mass-energy equation ",
                        formula = "E = mc^2",
                        textAfter = " is one of the best-known equations in physics."
                    )
                }
            ),
            PreviewItem(
                id = "inline_fraction",
                title = "Inline fraction",
                latex = "\\frac{a}{b}",
                content = {
                    InlineMathCard(
                        title = "Fraction within a sentence",
                        textBefore = "The fraction ",
                        formula = "\\frac{a}{b}",
                        textAfter = " represents a divided by b."
                    )
                }
            ),
            PreviewItem(
                id = "inline_integral",
                title = "Inline integral",
                latex = "\\int_{0}^{\\infty} e^{-x} dx",
                content = {
                    InlineMathCard(
                        title = "Integral within a sentence",
                        textBefore = "The integral ",
                        formula = "\\int_{0}^{\\infty} e^{-x} dx",
                        textAfter = " evaluates to 1."
                    )
                }
            ),
            PreviewItem(
                id = "inline_sqrt",
                title = "Inline radical",
                latex = "\\sqrt{x^2 + y^2}",
                content = {
                    InlineMathCard(
                        title = "Radical within a sentence",
                        textBefore = "The vector length ",
                        formula = "\\sqrt{x^2 + y^2}",
                        textAfter = " is the distance from the origin."
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "inline_multi",
        title = "Multiple inline formulas",
        description = "Combine several formulas with running text in one paragraph",
        items = listOf(
            PreviewItem(
                id = "inline_multi_formulas",
                title = "Text with several formulas",
                latex = "a^2 + b^2 = c^2",
                content = {
                    MultiInlineMathCard(
                        title = "Text with several formulas",
                        segments = listOf(
                            TextSegment.Plain("By the Pythagorean theorem, "),
                            TextSegment.Math("a^2 + b^2 = c^2"),
                            TextSegment.Plain(", when "),
                            TextSegment.Math("c = 5"),
                            TextSegment.Plain(" and "),
                            TextSegment.Math("a = 3"),
                            TextSegment.Plain(", "),
                            TextSegment.Math("b = 4"),
                            TextSegment.Plain("."),
                        )
                    )
                }
            ),
            PreviewItem(
                id = "inline_euler",
                title = "Euler's identity in a sentence",
                latex = "e^{i\\pi} + 1 = 0",
                content = {
                    MultiInlineMathCard(
                        title = "Euler's identity in a sentence",
                        segments = listOf(
                            TextSegment.Plain("Euler's identity "),
                            TextSegment.Math("e^{i\\pi} + 1 = 0"),
                            TextSegment.Plain(" connects the five fundamental constants "),
                            TextSegment.Math("e"),
                            TextSegment.Plain(", "),
                            TextSegment.Math("i"),
                            TextSegment.Plain(", "),
                            TextSegment.Math("\\pi"),
                            TextSegment.Plain(", "),
                            TextSegment.Math("1"),
                            TextSegment.Plain(" and "),
                            TextSegment.Math("0"),
                            TextSegment.Plain("."),
                        )
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "inline_batch",
        title = "Batch measurement",
        description = "Measure several formulas together before laying out inline text",
        items = listOf(
            PreviewItem(
                id = "inline_batch_demo",
                title = "Measure multiple formulas",
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
    val config = LatexConfig(
        fontSize = 16.sp,
        theme = LatexTheme.material3()
    )
    val measurer = rememberLatexMeasurer(config)
    val formulaContent = measurer.inlineContent(formula, config)
    val dims = measurer.measure(formula, config)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            if (formulaContent != null) {
                val annotated = buildAnnotatedString {
                    append(textBefore)
                    appendInlineContent("formula", formula)
                    append(textAfter)
                }

                Text(
                    text = annotated,
                    inlineContent = mapOf("formula" to formulaContent),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text("Unable to measure this formula", color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider()

            Text(
                text = "LaTeX: $formula",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dims != null) {
                Text(
                    text = "Size: ${dims.widthPx.toFixed1()} x ${dims.heightPx.toFixed1()} px, baseline: ${dims.baselinePx.toFixed1()} px",
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
    val config = LatexConfig(
        fontSize = 16.sp,
        theme = LatexTheme.material3()
    )
    val measurer = rememberLatexMeasurer(config)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            val inlineContent = buildMap {
                var mathIndex = 0
                for (segment in segments) {
                    if (segment is TextSegment.Math) {
                        val id = "math_$mathIndex"
                        measurer.inlineContent(segment.latex, config)?.let { content ->
                            put(id, content)
                        }
                        mathIndex++
                    }
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
    val config = LatexConfig(
        fontSize = 16.sp,
        theme = LatexTheme.material3()
    )
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
            Text("Measure multiple formulas", style = MaterialTheme.typography.titleSmall)
            Text(
                "Measured ${formulas.size} formulas together with measureBatch()",
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
                            text = "→ ${dims.widthPx.toFixed1()} × ${dims.heightPx.toFixed1()} px (baseline: ${dims.baselinePx.toFixed1()} px)",
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
