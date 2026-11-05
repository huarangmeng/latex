package com.hrm.latex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import latex.composeapp.generated.resources.Res
import latex.composeapp.generated.resources.noto_emoji
import latex.composeapp.generated.resources.noto_sans_sc
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "compose-root") {
        WebApp()
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun WebApp() {
    val chineseFont by preloadFont(Res.font.noto_sans_sc)
    val emojiFont by preloadFont(Res.font.noto_emoji)
    val fontFamilyResolver = LocalFontFamilyResolver.current
    var fontsReady by remember { mutableStateOf(false) }

    LaunchedEffect(chineseFont, emojiFont, fontFamilyResolver) {
        val chinese = chineseFont ?: return@LaunchedEffect
        val emoji = emojiFont ?: return@LaunchedEffect
        // Register each family separately so both can serve as glyph fallbacks.
        // Compose Web 1.10 does not have the browser's system-font fallback.
        fontFamilyResolver.preload(FontFamily(chinese))
        fontFamilyResolver.preload(FontFamily(emoji))
        fontsReady = true
    }

    if (fontsReady) {
        App()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
