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

package com.hrm.latex.renderer.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class LatexTemplateTest {

    @Test
    fun should_expand_fraction_template() {
        val expansion = LatexTemplate.FRACTION.expand()
        assertEquals("\\frac{}{}", expansion.text)
        assertEquals(6, expansion.cursorDelta)
    }

    @Test
    fun should_expand_sqrt_template() {
        val expansion = LatexTemplate.SQRT.expand()
        assertEquals("\\sqrt{}", expansion.text)
        assertEquals(6, expansion.cursorDelta)
    }

    @Test
    fun should_expand_nthroot_template() {
        val expansion = LatexTemplate.NTHROOT.expand()
        assertEquals("\\sqrt[]{}", expansion.text)
        assertEquals(6, expansion.cursorDelta)
    }

    @Test
    fun should_expand_superscript_template() {
        val expansion = LatexTemplate.SUPERSCRIPT.expand()
        assertEquals("^{}", expansion.text)
        assertEquals(2, expansion.cursorDelta)
    }

    @Test
    fun should_expand_subscript_template() {
        val expansion = LatexTemplate.SUBSCRIPT.expand()
        assertEquals("_{}", expansion.text)
        assertEquals(2, expansion.cursorDelta)
    }

    @Test
    fun should_expand_sum_template() {
        val expansion = LatexTemplate.SUM.expand()
        assertEquals("\\sum_{}^{}", expansion.text)
        assertEquals(6, expansion.cursorDelta)
    }

    @Test
    fun should_expand_integral_template() {
        val expansion = LatexTemplate.INTEGRAL.expand()
        assertEquals("\\int_{}^{}", expansion.text)
        assertEquals(6, expansion.cursorDelta)
    }

    @Test
    fun should_expand_limit_template() {
        val expansion = LatexTemplate.LIMIT.expand()
        assertEquals("\\lim_{}", expansion.text)
        assertEquals(6, expansion.cursorDelta)
    }

    @Test
    fun should_expand_overline_template() {
        val expansion = LatexTemplate.OVERLINE.expand()
        assertEquals("\\overline{}", expansion.text)
        assertEquals(10, expansion.cursorDelta)
    }

    @Test
    fun should_expand_vec_template() {
        val expansion = LatexTemplate.VEC.expand()
        assertEquals("\\vec{}", expansion.text)
        assertEquals(5, expansion.cursorDelta)
    }

    @Test
    fun should_expand_hat_template() {
        val expansion = LatexTemplate.HAT.expand()
        assertEquals("\\hat{}", expansion.text)
        assertEquals(5, expansion.cursorDelta)
    }

    @Test
    fun should_have_all_templates_with_non_empty_names() {
        LatexTemplate.entries.forEach { template ->
            assert(template.displayName.isNotEmpty()) {
                "Template ${template.name} has empty displayName"
            }
            assert(template.icon.isNotEmpty()) {
                "Template ${template.name} has empty icon"
            }
        }
    }

    @Test
    fun should_expand_all_templates_to_non_empty_text() {
        LatexTemplate.entries.forEach { template ->
            val expansion = template.expand()
            assert(expansion.text.isNotEmpty()) {
                "Template ${template.name} expanded to empty text"
            }
            assert(expansion.cursorDelta >= 0) {
                "Template ${template.name} has negative cursorDelta"
            }
            assert(expansion.cursorDelta <= expansion.text.length) {
                "Template ${template.name} cursorDelta exceeds text length"
            }
        }
    }
}
