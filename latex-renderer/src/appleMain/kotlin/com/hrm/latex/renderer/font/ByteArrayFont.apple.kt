package com.hrm.latex.renderer.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font as PlatformFont

internal actual fun fontFromBytes(
    identity: String,
    bytes: ByteArray,
    weight: FontWeight,
    style: FontStyle,
): Font = PlatformFont(identity, bytes, weight, style)
