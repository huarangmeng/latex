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

package com.hrm.latex.renderer.export

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SvgCanvasTest {
    @Test
    fun recordsCanvasCommandsAsVectorElements() = runTest {
        awaitGraphicsReady()
        val bytes = renderToSvg(
            width = 120f,
            height = 48f,
            textAsPath = true,
            prettyPrint = true
        ) { canvas ->
            val paint = Paint().apply {
                color = Color(0xff336699)
                strokeWidth = 2f
            }
            canvas.drawLine(Offset(4f, 8f), Offset(116f, 40f), paint)
        }

        val svg = assertNotNull(bytes).decodeToString()
        assertContains(svg, "<svg")
        assertContains(svg, "viewBox=\"0 0 120 48\"")
        assertFalse("data:image" in svg, "SVG export must not embed a raster snapshot")
    }
}

internal expect suspend fun awaitGraphicsReady()
