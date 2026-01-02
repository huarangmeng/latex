package com.hrm.latex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.RenderStyle
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val samples = remember {
                listOf(
                    "\\frac{1}{2} + \\frac{1}{3}",
                    "\\sqrt{a+b}",
                    "\\sum_{i=1}^{n} i",
                    "\\int_{0}^{\\pi} \\sin(x) dx",
                    "\\begin{pmatrix}1 & 2 \\\\ 3 & 4\\end{pmatrix}",
                    "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
                    "1 + \\frac{1}{1 + \\frac{1}{1 + \\frac{1}{1 + x}}}"
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("LaTeX 渲染示例", style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    samples.forEach { formula ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "源码: $formula",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Latex(
                                latex = formula,
                                modifier = Modifier.fillMaxWidth(),
                                style = RenderStyle(fontSize = 20.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}