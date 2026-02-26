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


package com.hrm.latex.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SymbolMapTest {

    // ========== 希腊字母测试 ==========

    @Test
    fun testLowercaseGreekLetters() {
        assertEquals("α", SymbolMap.getSymbol("alpha"))
        assertEquals("β", SymbolMap.getSymbol("beta"))
        assertEquals("γ", SymbolMap.getSymbol("gamma"))
        assertEquals("δ", SymbolMap.getSymbol("delta"))
        assertEquals("ϵ", SymbolMap.getSymbol("epsilon"))
        assertEquals("ζ", SymbolMap.getSymbol("zeta"))
        assertEquals("η", SymbolMap.getSymbol("eta"))
        assertEquals("θ", SymbolMap.getSymbol("theta"))
        assertEquals("ι", SymbolMap.getSymbol("iota"))
        assertEquals("κ", SymbolMap.getSymbol("kappa"))
        assertEquals("λ", SymbolMap.getSymbol("lambda"))
        assertEquals("μ", SymbolMap.getSymbol("mu"))
        assertEquals("ν", SymbolMap.getSymbol("nu"))
        assertEquals("ξ", SymbolMap.getSymbol("xi"))
        assertEquals("π", SymbolMap.getSymbol("pi"))
        assertEquals("ρ", SymbolMap.getSymbol("rho"))
        assertEquals("σ", SymbolMap.getSymbol("sigma"))
        assertEquals("τ", SymbolMap.getSymbol("tau"))
        assertEquals("υ", SymbolMap.getSymbol("upsilon"))
        assertEquals("ϕ", SymbolMap.getSymbol("phi"))
        assertEquals("χ", SymbolMap.getSymbol("chi"))
        assertEquals("ψ", SymbolMap.getSymbol("psi"))
        assertEquals("ω", SymbolMap.getSymbol("omega"))
    }

    @Test
    fun testUppercaseGreekLetters() {
        assertEquals("Γ", SymbolMap.getSymbol("Gamma"))
        assertEquals("Δ", SymbolMap.getSymbol("Delta"))
        assertEquals("Θ", SymbolMap.getSymbol("Theta"))
        assertEquals("Λ", SymbolMap.getSymbol("Lambda"))
        assertEquals("Ξ", SymbolMap.getSymbol("Xi"))
        assertEquals("Π", SymbolMap.getSymbol("Pi"))
        assertEquals("Σ", SymbolMap.getSymbol("Sigma"))
        assertEquals("Υ", SymbolMap.getSymbol("Upsilon"))
        assertEquals("Φ", SymbolMap.getSymbol("Phi"))
        assertEquals("Ψ", SymbolMap.getSymbol("Psi"))
        assertEquals("Ω", SymbolMap.getSymbol("Omega"))
    }

    @Test
    fun testVariantGreekLetters() {
        assertEquals("ε", SymbolMap.getSymbol("varepsilon"))
        assertEquals("ϑ", SymbolMap.getSymbol("vartheta"))
        assertEquals("ϖ", SymbolMap.getSymbol("varpi"))
        assertEquals("ϱ", SymbolMap.getSymbol("varrho"))
        assertEquals("ς", SymbolMap.getSymbol("varsigma"))
        assertEquals("φ", SymbolMap.getSymbol("varphi"))
    }

    // ========== 运算符测试 ==========

    @Test
    fun testBasicOperators() {
        assertEquals("×", SymbolMap.getSymbol("times"))
        assertEquals("÷", SymbolMap.getSymbol("div"))
        assertEquals("±", SymbolMap.getSymbol("pm"))
        assertEquals("∓", SymbolMap.getSymbol("mp"))
        assertEquals("⋅", SymbolMap.getSymbol("cdot"))
        assertEquals("∗", SymbolMap.getSymbol("ast"))
        assertEquals("⋆", SymbolMap.getSymbol("star"))
        assertEquals("∘", SymbolMap.getSymbol("circ"))
        assertEquals("•", SymbolMap.getSymbol("bullet"))
    }

    @Test
    fun testCircledOperators() {
        assertEquals("⊕", SymbolMap.getSymbol("oplus"))
        assertEquals("⊖", SymbolMap.getSymbol("ominus"))
        assertEquals("⊗", SymbolMap.getSymbol("otimes"))
        assertEquals("⊘", SymbolMap.getSymbol("oslash"))
        assertEquals("⊙", SymbolMap.getSymbol("odot"))
    }

    // ========== 关系符号测试 ==========

    @Test
    fun testComparisonOperators() {
        assertEquals("≤", SymbolMap.getSymbol("leq"))
        assertEquals("≤", SymbolMap.getSymbol("le"))
        assertEquals("≥", SymbolMap.getSymbol("geq"))
        assertEquals("≥", SymbolMap.getSymbol("ge"))
        assertEquals("≠", SymbolMap.getSymbol("neq"))
        assertEquals("≠", SymbolMap.getSymbol("ne"))
        assertEquals("≪", SymbolMap.getSymbol("ll"))
        assertEquals("≫", SymbolMap.getSymbol("gg"))
    }

    @Test
    fun testEquivalenceOperators() {
        assertEquals("≡", SymbolMap.getSymbol("equiv"))
        assertEquals("≈", SymbolMap.getSymbol("approx"))
        assertEquals("≅", SymbolMap.getSymbol("cong"))
        assertEquals("∼", SymbolMap.getSymbol("sim"))
        assertEquals("≃", SymbolMap.getSymbol("simeq"))
        assertEquals("∝", SymbolMap.getSymbol("propto"))
    }

    @Test
    fun testSetRelations() {
        assertEquals("⊂", SymbolMap.getSymbol("subset"))
        assertEquals("⊃", SymbolMap.getSymbol("supset"))
        assertEquals("⊆", SymbolMap.getSymbol("subseteq"))
        assertEquals("⊇", SymbolMap.getSymbol("supseteq"))
        assertEquals("∈", SymbolMap.getSymbol("in"))
        assertEquals("∉", SymbolMap.getSymbol("notin"))
        assertEquals("∋", SymbolMap.getSymbol("ni"))
    }

    @Test
    fun testGeometricRelations() {
        assertEquals("⊥", SymbolMap.getSymbol("perp"))
        assertEquals("∥", SymbolMap.getSymbol("parallel"))
    }

    // ========== 箭头测试 ==========

    @Test
    fun testBasicArrows() {
        assertEquals("←", SymbolMap.getSymbol("leftarrow"))
        assertEquals("→", SymbolMap.getSymbol("rightarrow"))
        assertEquals("↔", SymbolMap.getSymbol("leftrightarrow"))
        assertEquals("↑", SymbolMap.getSymbol("uparrow"))
        assertEquals("↓", SymbolMap.getSymbol("downarrow"))
        assertEquals("↕", SymbolMap.getSymbol("updownarrow"))
    }

    @Test
    fun testDoubleArrows() {
        assertEquals("⇐", SymbolMap.getSymbol("Leftarrow"))
        assertEquals("⇒", SymbolMap.getSymbol("Rightarrow"))
        assertEquals("⇔", SymbolMap.getSymbol("Leftrightarrow"))
        assertEquals("⇑", SymbolMap.getSymbol("Uparrow"))
        assertEquals("⇓", SymbolMap.getSymbol("Downarrow"))
        assertEquals("⇕", SymbolMap.getSymbol("Updownarrow"))
    }

    @Test
    fun testSpecialArrows() {
        assertEquals("↦", SymbolMap.getSymbol("mapsto"))
        assertEquals("⟶", SymbolMap.getSymbol("longrightarrow"))
        assertEquals("⟵", SymbolMap.getSymbol("longleftarrow"))
        assertEquals("⟷", SymbolMap.getSymbol("longleftrightarrow"))
    }

    @Test
    fun testHookAndHarpoonArrows() {
        assertEquals("↪", SymbolMap.getSymbol("hookrightarrow"))
        assertEquals("↩", SymbolMap.getSymbol("hookleftarrow"))
        assertEquals("↼", SymbolMap.getSymbol("leftharpoonup"))
        assertEquals("↽", SymbolMap.getSymbol("leftharpoondown"))
        assertEquals("⇀", SymbolMap.getSymbol("rightharpoonup"))
        assertEquals("⇁", SymbolMap.getSymbol("rightharpoondown"))
    }

    // ========== 集合符号测试 ==========

    @Test
    fun testSetOperators() {
        assertEquals("∅", SymbolMap.getSymbol("emptyset"))
        assertEquals("∅", SymbolMap.getSymbol("varnothing"))
        assertEquals("∩", SymbolMap.getSymbol("cap"))
        assertEquals("∪", SymbolMap.getSymbol("cup"))
        assertEquals("∖", SymbolMap.getSymbol("setminus"))
    }

    @Test
    fun testQuantifiers() {
        assertEquals("∀", SymbolMap.getSymbol("forall"))
        assertEquals("∃", SymbolMap.getSymbol("exists"))
        assertEquals("∄", SymbolMap.getSymbol("nexists"))
    }

    // ========== 逻辑符号测试 ==========

    @Test
    fun testLogicalOperators() {
        assertEquals("¬", SymbolMap.getSymbol("neg"))
        assertEquals("∧", SymbolMap.getSymbol("land"))
        assertEquals("∧", SymbolMap.getSymbol("wedge"))
        assertEquals("∨", SymbolMap.getSymbol("lor"))
        assertEquals("∨", SymbolMap.getSymbol("vee"))
        assertEquals("⟹", SymbolMap.getSymbol("implies"))
        assertEquals("⟺", SymbolMap.getSymbol("iff"))
    }

    // ========== 微积分符号测试 ==========

    @Test
    fun testCalculusSymbols() {
        assertEquals("∞", SymbolMap.getSymbol("infty"))
        assertEquals("∂", SymbolMap.getSymbol("partial"))
        assertEquals("∇", SymbolMap.getSymbol("nabla"))
        assertEquals("∫", SymbolMap.getSymbol("int"))
        assertEquals("∬", SymbolMap.getSymbol("iint"))
        assertEquals("∭", SymbolMap.getSymbol("iiint"))
        assertEquals("∮", SymbolMap.getSymbol("oint"))
    }

    // ========== 特殊符号测试 ==========

    @Test
    fun testDots() {
        assertEquals("…", SymbolMap.getSymbol("ldots"))
        assertEquals("⋯", SymbolMap.getSymbol("cdots"))
        assertEquals("⋮", SymbolMap.getSymbol("vdots"))
        assertEquals("⋱", SymbolMap.getSymbol("ddots"))
    }

    @Test
    fun testMiscSymbols() {
        assertEquals("∴", SymbolMap.getSymbol("therefore"))
        assertEquals("∵", SymbolMap.getSymbol("because"))
        assertEquals("∠", SymbolMap.getSymbol("angle"))
        assertEquals("°", SymbolMap.getSymbol("degree"))
        assertEquals("′", SymbolMap.getSymbol("prime"))
        assertEquals("ℏ", SymbolMap.getSymbol("hbar"))
        assertEquals("ℓ", SymbolMap.getSymbol("ell"))
        assertEquals("℘", SymbolMap.getSymbol("wp"))
    }

    @Test
    fun testMathematicalSets() {
        assertEquals("ℜ", SymbolMap.getSymbol("Re"))
        assertEquals("ℑ", SymbolMap.getSymbol("Im"))
        assertEquals("ℵ", SymbolMap.getSymbol("aleph"))
    }

    // ========== 括号测试 ==========

    @Test
    fun testBrackets() {
        assertEquals("⟨", SymbolMap.getSymbol("langle"))
        assertEquals("⟩", SymbolMap.getSymbol("rangle"))
        assertEquals("⌊", SymbolMap.getSymbol("lfloor"))
        assertEquals("⌋", SymbolMap.getSymbol("rfloor"))
        assertEquals("⌈", SymbolMap.getSymbol("lceil"))
        assertEquals("⌉", SymbolMap.getSymbol("rceil"))
    }

    // ========== 大型运算符测试 ==========

    @Test
    fun testBigOperators() {
        assertEquals("∑", SymbolMap.getSymbol("sum"))
        assertEquals("∏", SymbolMap.getSymbol("prod"))
        assertEquals("∐", SymbolMap.getSymbol("coprod"))
        assertEquals("⋃", SymbolMap.getSymbol("bigcup"))
        assertEquals("⋂", SymbolMap.getSymbol("bigcap"))
        assertEquals("⋁", SymbolMap.getSymbol("bigvee"))
        assertEquals("⋀", SymbolMap.getSymbol("bigwedge"))
        assertEquals("⨁", SymbolMap.getSymbol("bigoplus"))
        assertEquals("⨂", SymbolMap.getSymbol("bigotimes"))
    }

    // ========== 边界情况测试 ==========

    @Test
    fun testNonExistentSymbol() {
        assertNull(SymbolMap.getSymbol("nonexistent"))
        assertNull(SymbolMap.getSymbol(""))
        assertNull(SymbolMap.getSymbol("xyz"))
    }

    @Test
    fun testGetAllSymbols() {
        val allSymbols = SymbolMap.getAllSymbols()
        assertTrue(allSymbols.isNotEmpty())
        assertTrue(allSymbols.size > 100) // 应该有100+个符号
    }

    @Test
    fun testSymbolConsistency() {
        // 测试同义符号
        assertEquals(
            SymbolMap.getSymbol("leq"),
            SymbolMap.getSymbol("le")
        )
        assertEquals(
            SymbolMap.getSymbol("geq"),
            SymbolMap.getSymbol("ge")
        )
        assertEquals(
            SymbolMap.getSymbol("neq"),
            SymbolMap.getSymbol("ne")
        )
    }

    @Test
    fun testAllGreekLettersExist() {
        val greekLetters = listOf(
            "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta",
            "iota", "kappa", "lambda", "mu", "nu", "xi", "pi", "rho", "sigma",
            "tau", "upsilon", "phi", "chi", "psi", "omega"
        )

        greekLetters.forEach { letter ->
            assertNotNull(SymbolMap.getSymbol(letter), "Missing Greek letter: $letter")
        }
    }

    @Test
    fun testCommonMathSymbolsExist() {
        val commonSymbols = listOf(
            "times", "div", "pm", "cdot", "leq", "geq", "neq",
            "in", "subset", "cup", "cap", "forall", "exists",
            "rightarrow", "Rightarrow", "infty", "partial", "nabla"
        )

        commonSymbols.forEach { symbol ->
            assertNotNull(SymbolMap.getSymbol(symbol), "Missing common symbol: $symbol")
        }
    }
}
