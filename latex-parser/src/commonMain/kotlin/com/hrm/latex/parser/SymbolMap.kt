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
        "epsilon" to "ϵ",
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
        "phi" to "ϕ",
        "varphi" to "φ",
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
        "cdotp" to "⋅",
        "ast" to "∗",
        "star" to "⋆",
        "circ" to "∘",
        "bullet" to "•",
        "oplus" to "⊕",
        "ominus" to "⊖",
        "otimes" to "⊗",
        "oslash" to "⊘",
        "odot" to "⊙",
        "diamond" to "◇",
        "bigcirc" to "○",
        "minus" to "−",
        "uplus" to "⊎",
        "sqcup" to "⊔",
        "sqcap" to "⊓",
        "wr" to "≀",
        "amalg" to "∐",
        
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
        "sqsubseteq" to "⊑",
        "sqsupseteq" to "⊒",
        "in" to "∈",
        "notin" to "∉",
        "ni" to "∋",
        "owns" to "∋",
        "perp" to "⊥",
        "parallel" to "∥",
        "asymp" to "≍",
        "prec" to "≺",
        "succ" to "≻",
        "preceq" to "≼",
        "succeq" to "≽",
        "vdash" to "⊢",
        "dashv" to "⊣",
        "top" to "⊤",
        "bot" to "⊥",
        
        // 箭头
        "leftarrow" to "←",
        "rightarrow" to "→",
        "to" to "→",  // \to 是 \rightarrow 的简写
        "gets" to "←", // \gets 是 \leftarrow 的简写
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
        "Longleftarrow" to "⟸",
        "Longrightarrow" to "⟹",
        "Longleftrightarrow" to "⟺",
        "longmapsto" to "⟼",
        "nearrow" to "↗",
        "searrow" to "↘",
        "nwarrow" to "↖",
        "swarrow" to "↙",
        "leftharpoonup" to "↼",
        "leftharpoondown" to "↽",
        "rightharpoonup" to "⇀",
        "rightharpoondown" to "⇁",
        
        // 集合符号
        "emptyset" to "∅",
        "varnothing" to "∅",
        "cap" to "∩",
        "cup" to "∪",
        "setminus" to "∖",
        "backslash" to "∖",
        "forall" to "∀",
        "exists" to "∃",
        "nexists" to "∄",
        
        // 逻辑符号
        "neg" to "¬",
        "lnot" to "¬",
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
        "triangle" to "△",
        "imath" to "ı",
        "jmath" to "ȷ",
        "flat" to "♭",
        "natural" to "♮",
        "sharp" to "♯",
        "smile" to "⌣",
        "frown" to "⌢",
        "S" to "§",
        "P" to "¶",
        "dagger" to "†",
        "ddagger" to "‡",
        "clubsuit" to "♣",
        "diamondsuit" to "♢",
        "heartsuit" to "♡",
        "spadesuit" to "♠",
        
        // 括号和分隔符
        "langle" to "⟨",
        "rangle" to "⟩",
        "lfloor" to "⌊",
        "rfloor" to "⌋",
        "lceil" to "⌈",
        "rceil" to "⌉",
        "vert" to "|",
        "Vert" to "‖",
        "lvert" to "|",
        "rvert" to "|",
        "lVert" to "‖",
        "rVert" to "‖",
        "lbrace" to "{",
        "rbrace" to "}",
        
        // 其他
        "sum" to "∑",
        "prod" to "∏",
        "coprod" to "∐",
        "bigcup" to "⋃",
        "bigcap" to "⋂",
        "bigvee" to "⋁",
        "bigwedge" to "⋀",
        "bigoplus" to "⨁",
        "bigotimes" to "⨂",
        "bigsqcup" to "⨆",
        "bigodot" to "⨀",
        "biguplus" to "⨄",
        "triangleright" to "▷",
        "triangleleft" to "◁",
        
        // 钩箭头
        "hookrightarrow" to "↪",
        "hookleftarrow" to "↩",
        
        // 半箭头（鱼叉箭头）
        "leftharpoonup" to "↼",
        "leftharpoondown" to "↽",
        "rightharpoonup" to "⇀",
        "rightharpoondown" to "⇁",
        
        // 缺失的关系符号
        "mid" to "∣",
        "owns" to "∋"
    )
    
    /**
     * 符号的英文可读名称映射（命令名 → 英文名）
     * 用于无障碍文本生成（屏幕阅读器朗读）
     */
    private val accessibleNames = mapOf(
        // 希腊字母
        "alpha" to "alpha", "beta" to "beta", "gamma" to "gamma",
        "delta" to "delta", "epsilon" to "epsilon", "varepsilon" to "epsilon",
        "zeta" to "zeta", "eta" to "eta", "theta" to "theta", "vartheta" to "theta",
        "iota" to "iota", "kappa" to "kappa", "lambda" to "lambda",
        "mu" to "mu", "nu" to "nu", "xi" to "xi",
        "pi" to "pi", "varpi" to "pi",
        "rho" to "rho", "varrho" to "rho",
        "sigma" to "sigma", "varsigma" to "sigma",
        "tau" to "tau", "upsilon" to "upsilon",
        "phi" to "phi", "varphi" to "phi",
        "chi" to "chi", "psi" to "psi", "omega" to "omega",
        // 大写希腊字母
        "Gamma" to "Gamma", "Delta" to "Delta", "Theta" to "Theta",
        "Lambda" to "Lambda", "Xi" to "Xi", "Pi" to "Pi",
        "Sigma" to "Sigma", "Upsilon" to "Upsilon",
        "Phi" to "Phi", "Psi" to "Psi", "Omega" to "Omega",
        // 微积分 & 特殊符号
        "infty" to "infinity", "partial" to "partial",
        "nabla" to "nabla", "hbar" to "h bar",
        "ell" to "ell", "wp" to "Weierstrass p",
        "Re" to "real part", "Im" to "imaginary part",
        "aleph" to "aleph",
        // 运算符
        "times" to "times", "div" to "divided by",
        "pm" to "plus or minus", "mp" to "minus or plus",
        "cdot" to "dot", "ast" to "asterisk", "star" to "star",
        "circ" to "circle", "bullet" to "bullet",
        "oplus" to "circled plus", "otimes" to "circled times",
        "odot" to "circled dot", "minus" to "minus",
        // 点号
        "cdots" to "dots", "ldots" to "dots",
        "vdots" to "vertical dots", "ddots" to "diagonal dots",
        // 关系符号
        "leq" to "less than or equal to", "le" to "less than or equal to",
        "geq" to "greater than or equal to", "ge" to "greater than or equal to",
        "neq" to "not equal to", "ne" to "not equal to",
        "approx" to "approximately", "equiv" to "equivalent to",
        "sim" to "similar to", "simeq" to "similar or equal to",
        "cong" to "congruent to", "propto" to "proportional to",
        "ll" to "much less than", "gg" to "much greater than",
        "prec" to "precedes", "succ" to "succeeds",
        "perp" to "perpendicular", "parallel" to "parallel to",
        "mid" to "divides",
        // 箭头
        "rightarrow" to "right arrow", "leftarrow" to "left arrow",
        "to" to "to", "gets" to "gets",
        "leftrightarrow" to "if and only if",
        "Rightarrow" to "implies", "Leftarrow" to "is implied by",
        "Leftrightarrow" to "if and only if",
        "uparrow" to "up arrow", "downarrow" to "down arrow",
        "mapsto" to "maps to",
        "longrightarrow" to "long right arrow", "longleftarrow" to "long left arrow",
        "hookrightarrow" to "hook right arrow", "hookleftarrow" to "hook left arrow",
        // 集合
        "in" to "in", "notin" to "not in", "ni" to "contains",
        "subset" to "subset of", "supset" to "superset of",
        "subseteq" to "subset of or equal to", "supseteq" to "superset of or equal to",
        "cup" to "union", "cap" to "intersection",
        "setminus" to "set minus", "emptyset" to "empty set", "varnothing" to "empty set",
        // 逻辑
        "forall" to "for all", "exists" to "there exists", "nexists" to "there does not exist",
        "neg" to "not", "lnot" to "not",
        "land" to "and", "lor" to "or",
        "wedge" to "and", "vee" to "or",
        "implies" to "implies", "iff" to "if and only if",
        // 其他
        "therefore" to "therefore", "because" to "because",
        "angle" to "angle", "degree" to "degree", "prime" to "prime",
        "triangle" to "triangle",
        "dagger" to "dagger", "ddagger" to "double dagger",
        "vdash" to "proves", "dashv" to "is proved by",
        "top" to "top", "bot" to "bottom",
    )

    /**
     * Unicode 字符 → 英文可读名称 的反向映射（由 symbols + accessibleNames 自动生成）
     */
    private val unicodeAccessibleNames: Map<String, String> by lazy {
        buildMap {
            for ((cmd, name) in accessibleNames) {
                val unicode = symbols[cmd] ?: continue
                put(unicode, name)
            }
        }
    }

    fun getSymbol(name: String): String? = symbols[name]
    
    fun getAllSymbols(): Map<String, String> = symbols

    /**
     * 根据 LaTeX 命令名获取英文可读名称（用于无障碍文本）
     */
    fun getAccessibleName(commandName: String): String? = accessibleNames[commandName]

    /**
     * 根据 Unicode 字符获取英文可读名称（用于无障碍文本）
     */
    fun getAccessibleNameByUnicode(unicode: String): String? = unicodeAccessibleNames[unicode]
}
