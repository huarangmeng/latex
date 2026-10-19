package com.hrm.latex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hrm.latex.fonts.KatexFonts
import com.hrm.latex.preview.LatexPreview

@Composable
@Preview
fun App() {
    // 演示应用保留内置 KaTeX 字体；正式宿主可省略这一步以使用系统字体。
    KatexFonts.install()
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LatexPreview()
        }
    }
}