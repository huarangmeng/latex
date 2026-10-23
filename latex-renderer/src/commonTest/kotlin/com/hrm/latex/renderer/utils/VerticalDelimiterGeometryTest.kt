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

package com.hrm.latex.renderer.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerticalDelimiterGeometryTest {

    @Test
    fun stretchingSingleBarDoesNotScaleStrokeWidth() {
        val regular = geometry(lineCount = 1, targetHeight = 20f)
        val stretched = geometry(lineCount = 1, targetHeight = 80f)

        assertEquals(0.8f, regular.strokeWidth)
        assertEquals(regular.strokeWidth, stretched.strokeWidth)
        assertEquals(80f, stretched.height)
        assertEquals(1, stretched.lineCenters.size)
        assertEquals(stretched.width / 2f, stretched.lineCenters.single())
    }

    @Test
    fun doubleBarUsesTwoThinSymmetricLines() {
        val geometry = geometry(lineCount = 2, targetHeight = 60f)

        assertEquals(0.8f, geometry.strokeWidth)
        assertEquals(2, geometry.lineCenters.size)
        assertTrue(geometry.lineCenters[0] < geometry.width / 2f)
        assertTrue(geometry.lineCenters[1] > geometry.width / 2f)
        assertEquals(
            geometry.width,
            geometry.lineCenters[0] + geometry.lineCenters[1],
            absoluteTolerance = 1e-5f
        )
    }

    private fun geometry(lineCount: Int, targetHeight: Float): VerticalDelimiterGeometry =
        DelimiterRenderer.calculateVerticalDelimiterGeometry(
            lineCount = lineCount,
            advanceWidth = 5.56f,
            targetHeight = targetHeight,
            fontSizePx = 20f,
            strokeWidth = 0.8f
        )
}
