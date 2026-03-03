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

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.LatexAutoWrap

/**
 * line breaking and autowrap preview
 * demonstrates automatic line breaking for long formulas
 */

val lineBreakingPreviewGroups = listOf(
    PreviewGroup(
        id = "autowrap_basic",
        title = "1. basic autowrap",
        description = "long formulas with automatic line breaking",
        items = listOf(
            PreviewItem(
                "01",
                "long polynomial",
                "a + b + c + d + e + f + g + h + i + j + k + l + m + n = 0",
                content = {
                    LatexAutoWrap(
                        latex = "a + b + c + d + e + f + g + h + i + j + k + l + m + n = 0",
                        modifier = Modifier.width(200.dp)
                    )
                }
            ),
            PreviewItem(
                "02",
                "long equation",
                "x_1 + x_2 + x_3 + x_4 + x_5 + x_6 + x_7 + x_8 = y_1 + y_2 + y_3 + y_4",
                content = {
                    LatexAutoWrap(
                        latex = "x_1 + x_2 + x_3 + x_4 + x_5 + x_6 + x_7 + x_8 = y_1 + y_2 + y_3 + y_4",
                        modifier = Modifier.width(250.dp)
                    )
                }
            ),
            PreviewItem(
                "03",
                "break at relation",
                "f(x) = ax^2 + bx + c = 0",
                content = {
                    LatexAutoWrap(
                        latex = "f(x) = ax^2 + bx + c = 0",
                        modifier = Modifier.width(150.dp)
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "autowrap_complex",
        title = "2. complex autowrap",
        description = "complex formulas with fractions and operators",
        items = listOf(
            PreviewItem(
                "04",
                "fractions in long formula",
                "\\frac{1}{2} + \\frac{1}{3} + \\frac{1}{4} + \\frac{1}{5} + \\frac{1}{6} = \\frac{29}{20}",
                content = {
                    LatexAutoWrap(
                        latex = "\\frac{1}{2} + \\frac{1}{3} + \\frac{1}{4} + \\frac{1}{5} + \\frac{1}{6} = \\frac{29}{20}",
                        modifier = Modifier.width(200.dp)
                    )
                }
            ),
            PreviewItem(
                "05",
                "sum with long content",
                "\\sum_{i=1}^{n} x_i + \\sum_{j=1}^{m} y_j = \\sum_{k=1}^{p} z_k",
                content = {
                    LatexAutoWrap(
                        latex = "\\sum_{i=1}^{n} x_i + \\sum_{j=1}^{m} y_j = \\sum_{k=1}^{p} z_k",
                        modifier = Modifier.width(180.dp)
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "binomial_newcommand",
        title = "3. binomial in newcommand",
        description = "custom commands with binomial coefficients",
        items = listOf(
            PreviewItem(
                "06",
                "basic binomial command",
                "\\newcommand{\\mychoose}[2]{\\binom{#1}{#2}} \\mychoose{n}{k}"
            ),
            PreviewItem(
                "07",
                "binomial with expressions",
                "\\newcommand{\\C}[2]{\\binom{#1}{#2}} \\C{n+1}{k-1} + \\C{n}{k}"
            ),
            PreviewItem(
                "08",
                "multiple binomials",
                "\\newcommand{\\choose}[2]{\\binom{#1}{#2}} \\choose{5}{2} = \\choose{5}{3}"
            ),
        )
    ),
    PreviewGroup(
        id = "nested_environments",
        title = "4. nested environments",
        description = "environments inside other environments",
        items = listOf(
            PreviewItem(
                "09",
                "aligned inside equation",
                "\\begin{equation} \\begin{aligned} a &= b \\\\ c &= d \\end{aligned} \\end{equation}"
            ),
            PreviewItem(
                "10",
                "split inside equation",
                "\\begin{equation} \\begin{split} x &= 1 + 2 \\\\ &= 3 \\end{split} \\end{equation}"
            ),
            PreviewItem(
                "11",
                "gather inside subequations",
                "\\begin{subequations} \\begin{gather} a = 1 \\\\ b = 2 \\end{gather} \\end{subequations}"
            ),
        )
    ),
    PreviewGroup(
        id = "edge_cases",
        title = "5. edge cases",
        description = "fixed edge cases and regression tests",
        items = listOf(
            PreviewItem(
                "12",
                "lone hash in command",
                "\\newcommand{\\test}{a # b} \\test"
            ),
            PreviewItem(
                "13",
                "hash at end",
                "\\newcommand{\\end}{text#} \\end"
            ),
            PreviewItem(
                "14",
                "hash followed by letter",
                "\\newcommand{\\foo}{#x test} \\foo"
            ),
            PreviewItem(
                "15",
                "invalid param reference",
                "\\newcommand{\\bar}[1]{#1 #3 end} \\bar{x}"
            ),
        )
    ),
)

@Preview
@Composable
fun LineBreakingPreview(onBack: () -> Unit = {}) {
    PreviewCategoryScreen(
        title = "line breaking & fixes",
        groups = lineBreakingPreviewGroups,
        onBack = onBack
    )
}
