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

/**
 * LaTeX 符号到 Unicode 的映射表
 */
object SymbolMap {
    private val symbols = mapOf(
        // 希腊字母（小写）
        "alpha" to "α",
        "beta" to "β",
        "gamma" to "γ",
        "delta" to "δ",
        "epsilon" to "ε",
        "varepsilon" to "ε",
        "zeta" to "ζ",
        "eta" to "η",
        "theta" to "θ",
        "vartheta" to "ϑ",
        "iota" to "ι",
        "kappa" to "κ",
        "lambda" to "λ",
        "mu" to "μ",
        "nu" to "ν",
        "xi" to "ξ",
        "pi" to "π",
        "varpi" to "ϖ",
        "rho" to "ρ",
        "varrho" to "ϱ",
        "sigma" to "σ",
        "varsigma" to "ς",
        "tau" to "τ",
        "upsilon" to "υ",
        "phi" to "φ",
        "varphi" to "ϕ",
        "chi" to "χ",
        "psi" to "ψ",
        "omega" to "ω",
        
        // 希腊字母（大写）
        "Gamma" to "Γ",
        "Delta" to "Δ",
        "Theta" to "Θ",
        "Lambda" to "Λ",
        "Xi" to "Ξ",
        "Pi" to "Π",
        "Sigma" to "Σ",
        "Upsilon" to "Υ",
        "Phi" to "Φ",
        "Psi" to "Ψ",
        "Omega" to "Ω",
        
        // 运算符
        "times" to "×",
        "div" to "÷",
        "pm" to "±",
        "mp" to "∓",
        "cdot" to "⋅",
        "ast" to "∗",
        "star" to "⋆",
        "circ" to "∘",
        "bullet" to "•",
        "oplus" to "⊕",
        "ominus" to "⊖",
        "otimes" to "⊗",
        "oslash" to "⊘",
        "odot" to "⊙",
        
        // 关系符号
        "leq" to "≤",
        "le" to "≤",
        "geq" to "≥",
        "ge" to "≥",
        "neq" to "≠",
        "ne" to "≠",
        "equiv" to "≡",
        "approx" to "≈",
        "cong" to "≅",
        "sim" to "∼",
        "simeq" to "≃",
        "propto" to "∝",
        "ll" to "≪",
        "gg" to "≫",
        "subset" to "⊂",
        "supset" to "⊃",
        "subseteq" to "⊆",
        "supseteq" to "⊇",
        "in" to "∈",
        "notin" to "∉",
        "ni" to "∋",
        "perp" to "⊥",
        "parallel" to "∥",
        
        // 箭头
        "leftarrow" to "←",
        "rightarrow" to "→",
        "to" to "→",  // \to 是 \rightarrow 的简写
        "leftrightarrow" to "↔",
        "Leftarrow" to "⇐",
        "Rightarrow" to "⇒",
        "Leftrightarrow" to "⇔",
        "uparrow" to "↑",
        "downarrow" to "↓",
        "updownarrow" to "↕",
        "Uparrow" to "⇑",
        "Downarrow" to "⇓",
        "Updownarrow" to "⇕",
        "mapsto" to "↦",
        "longrightarrow" to "⟶",
        "longleftarrow" to "⟵",
        "longleftrightarrow" to "⟷",
        
        // 集合符号
        "emptyset" to "∅",
        "varnothing" to "∅",
        "cap" to "∩",
        "cup" to "∪",
        "setminus" to "∖",
        "forall" to "∀",
        "exists" to "∃",
        "nexists" to "∄",
        
        // 逻辑符号
        "neg" to "¬",
        "land" to "∧",
        "lor" to "∨",
        "wedge" to "∧",
        "vee" to "∨",
        "implies" to "⟹",
        "iff" to "⟺",
        
        // 微积分符号
        "infty" to "∞",
        "partial" to "∂",
        "nabla" to "∇",
        "int" to "∫",
        "iint" to "∬",
        "iiint" to "∭",
        "oint" to "∮",
        
        // 特殊符号
        "ldots" to "…",
        "cdots" to "⋯",
        "vdots" to "⋮",
        "ddots" to "⋱",
        "therefore" to "∴",
        "because" to "∵",
        "angle" to "∠",
        "degree" to "°",
        "prime" to "′",
        "hbar" to "ℏ",
        "ell" to "ℓ",
        "wp" to "℘",
        "Re" to "ℜ",
        "Im" to "ℑ",
        "aleph" to "ℵ",
        
        // 括号和分隔符
        "langle" to "⟨",
        "rangle" to "⟩",
        "lfloor" to "⌊",
        "rfloor" to "⌋",
        "lceil" to "⌈",
        "rceil" to "⌉",
        
        // 其他
        "sum" to "∑",
        "prod" to "∏",
        "coprod" to "∐",
        "bigcup" to "⋃",
        "bigcap" to "⋂",
        "bigvee" to "⋁",
        "bigwedge" to "⋀",
        "bigoplus" to "⨁",
        "bigotimes" to "⨂"
    )
    
    fun getSymbol(name: String): String? = symbols[name]
    
    fun getAllSymbols(): Map<String, String> = symbols
}
