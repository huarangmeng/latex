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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.renderer.Latex
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * 增量解析的实际应用演示
 */

@Preview
@Composable
fun Preview_Demo_RealTimeInput() {
    IncrementalPreviewCard("实际应用: 实时输入预览 (Debug)") {
        var userInput by remember { mutableStateOf("") }
        var debugInfo by remember { mutableStateOf("") }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("模拟用户输入:", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = userInput.ifEmpty { "(正在输入...)" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = debugInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Text("实时渲染结果:", style = MaterialTheme.typography.bodyMedium)
            Latex(
                latex = userInput,
                isDarkTheme = false
            )

            // 模拟用户逐步输入
            LaunchedEffect(Unit) {
                // 验证基础解析器
                try {
                    val parser = LatexParser()
                    parser.parse("\\int_{-\\infty}^{\\infty}")
                    debugInfo = "Base Parser Check: OK"
                } catch (e: Exception) {
                    debugInfo = "Base Parser Check Failed: ${e.message}"
                }

                val fullFormula = "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"
                userInput = ""
                delay(500)

                fullFormula.forEach { char ->
                    delay(150)
                    userInput += char
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview_Demo_ProgressTracking() {
    IncrementalPreviewCard("实际应用: 解析进度追踪") {
        var currentText by remember { mutableStateOf("") }
        val formula = "\\sum_{n=0}^{\\infty} \\frac{x^n}{n!} = e^x"
        val progress = if (formula.isEmpty()) 0f else currentText.length.toFloat() / formula.length

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 进度条
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Text(
                text = "解析进度: ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // 渲染结果
            Latex(
                latex = currentText,
                isDarkTheme = false
            )

            // 模拟流式输入并更新进度
            LaunchedEffect(Unit) {
                currentText = ""
                formula.forEach { char ->
                    delay(120)
                    currentText += char
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview_Demo_ErrorRecovery() {
    IncrementalPreviewCard("实际应用: 错误恢复") {
        val testCases = listOf(
            "完整公式" to "x^2 + y^2 = r^2",
            "未闭合括号" to "\\frac{1",
            "未完成上标" to "x^",
            "未完成下标" to "a_",
            "不完整命令" to "\\int",
            "部分积分" to "\\int_{0}^{1"
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            testCases.forEach { (label, latex) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "输入: $latex",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Latex(
                                latex = latex,
                                isDarkTheme = false
                            )
                        }
                    }
                }

                if (label != testCases.last().first) {
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview_Demo_MultipleFormulas() {
    IncrementalPreviewCard("实际应用: 多个公式同时渲染") {
        val formulas = listOf(
            "勾股定理" to "a^2 + b^2 = c^2",
            "二次方程" to "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
            "欧拉公式" to "e^{i\\pi} + 1 = 0",
            "高斯积分" to "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"
        )

        var currentIndices by remember { mutableStateOf(List(formulas.size) { 0 }) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            formulas.forEachIndexed { index, (title, fullFormula) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Latex(
                        latex = fullFormula.substring(0, currentIndices[index]),
                        isDarkTheme = false
                    )
                }
            }
        }

        // 同时开始所有公式的打字效果
        LaunchedEffect(Unit) {
            delay(500)

            while (currentIndices.any { it < formulas.maxOf { f -> f.second.length } }) {
                delay(100) // 增加延迟
                currentIndices = currentIndices.mapIndexed { index, currentIndex ->
                    val formula = formulas[index].second
                    minOf(currentIndex + 1, formula.length)
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview_Demo_ComparisonWithStandard() {
    IncrementalPreviewCard("对比: 标准 vs 增量渲染") {
        val formula = "\\sum_{i=1}^{n} i^2 = \\frac{n(n+1)(2n+1)}{6}"
        var incrementalText by remember { mutableStateOf("") }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 标准渲染（完整公式）
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "标准渲染（需要完整公式）:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Latex(
                            latex = formula,
                            isDarkTheme = false
                        )
                    }
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // 增量渲染（逐步输入）
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "增量渲染（支持部分公式）:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = "当前输入: ${incrementalText.ifEmpty { "(空)" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Latex(
                            latex = incrementalText,
                            isDarkTheme = false
                        )
                    }
                }
            }

            // 模拟逐步输入
            LaunchedEffect(Unit) {
                incrementalText = ""
                delay(1000)

                formula.forEach { char ->
                    delay(150) // 增加延迟
                    incrementalText += char
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview_Demo_All() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("增量解析实际应用演示", style = MaterialTheme.typography.headlineMedium)

        Preview_Demo_RealTimeInput()
        Preview_Demo_ProgressTracking()
        Preview_Demo_ErrorRecovery()
        Preview_Demo_MultipleFormulas()
        Preview_Demo_ComparisonWithStandard()
    }
}
