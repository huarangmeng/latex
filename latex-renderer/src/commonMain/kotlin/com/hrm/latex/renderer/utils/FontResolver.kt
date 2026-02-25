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

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.RenderContext

/**
 * 字体类别，用于选择对应的字体家族
 */
enum class FontCategory {
    ROMAN,           // 正文文本 (cmr10)
    MATH_ITALIC,     // 数学变量 (cmmi10)
    SYMBOL,          // 运算符、小型定界符 (cmsy10)
    EXTENSION,       // 大型运算符、大型定界符 (cmex10)
    SANS_SERIF,
    MONOSPACE,
    BLACKBOARD_BOLD,
    CALLIGRAPHIC,
    FRAKTUR,
    SCRIPT
}

/**
 * 符号渲染信息：包含 TTF cmap 中的 Unicode codepoint 字符、字体类别和字体样式
 *
 * CM 字体的 TTF 版本使用标准 ASCII/Latin-1 cmap 表。MicroTeX 项目的
 * `.def.cpp` 文件中定义了每个符号对应的 char code（即 TTF cmap 中的
 * Unicode codepoint）。本类直接使用这些 char code，无需额外偏移计算。
 *
 * @param texGlyph TTF cmap 中的 Unicode codepoint 对应的字符（用于实际渲染）
 * @param fontCategory 应使用的字体类别（决定使用 cmsy10/cmmi10/cmex10/cmr10）
 * @param fontStyle 字体样式（Normal 或 Italic）
 */
data class SymbolRenderInfo(
    val texGlyph: String,
    val fontCategory: FontCategory,
    val fontStyle: FontStyle = FontStyle.Normal
)

/**
 * 集中管理字体选择和符号字体路由。
 *
 * 所有字体选择逻辑归口此处，Measurer 中禁止直接选择字体。
 *
 * ## 符号字体路由
 *
 * CM 字体的 TTF 版本使用标准 ASCII/Latin-1 cmap 表，而非 TeX 内部编码。
 * 映射数据来源于 MicroTeX 项目的 `src/res/sym/base.def.cpp`，
 * 其中 `E(fontId, charCode, symbolName)` 定义了每个符号在 TTF 中的 char code。
 *
 * Font ID 对应关系（参考 MicroTeX `builtin_font_reg.cpp`）：
 * - 1  = cmex10 (Extension)
 * - 2  = cmmi10 (Math Italic)
 * - 5  = cmmi10_unchanged（与 cmmi10 共用同一 TTF 文件）
 * - 8  = cmsy10 (Symbol)
 * - 18 = cmr10  (Roman)
 *
 * 所有平台统一使用 CM 字体 + 正确的 char code 渲染，无需平台判断。
 */
internal object FontResolver {

    // =========================================================================
    // 符号命令名 → (charCode, fontCategory) 映射表
    //
    // charCode 直接来自 MicroTeX `base.def.cpp` 中的 E(fontId, charCode, name)
    // 即 TTF cmap 表中的 Unicode codepoint，直接转为 Char 即可渲染。
    // =========================================================================

    /**
     * 快捷构造 cmsy10 (FontCategory.SYMBOL) 的 SymbolRenderInfo
     * @param charCode MicroTeX base.def.cpp 中 E(8, charCode, name) 的 charCode
     */
    private fun sy(charCode: Int): SymbolRenderInfo =
        SymbolRenderInfo(charCode.toChar().toString(), FontCategory.SYMBOL, FontStyle.Normal)

    /**
     * 快捷构造 cmmi10 (FontCategory.MATH_ITALIC) 的 SymbolRenderInfo
     * @param charCode MicroTeX base.def.cpp 中 E(2/5, charCode, name) 的 charCode
     */
    private fun mi(charCode: Int, style: FontStyle = FontStyle.Italic): SymbolRenderInfo =
        SymbolRenderInfo(charCode.toChar().toString(), FontCategory.MATH_ITALIC, style)

    /**
     * 快捷构造 cmex10 (FontCategory.EXTENSION) 的 SymbolRenderInfo
     * @param charCode MicroTeX base.def.cpp 中 E(1, charCode, name) 的 charCode
     */
    private fun ex(charCode: Int): SymbolRenderInfo =
        SymbolRenderInfo(charCode.toChar().toString(), FontCategory.EXTENSION, FontStyle.Normal)

    /**
     * 快捷构造 cmr10 (FontCategory.ROMAN) 的 SymbolRenderInfo
     * @param charCode MicroTeX base.def.cpp 中 E(18, charCode, name) 的 charCode
     */
    private fun ro(charCode: Int, style: FontStyle = FontStyle.Normal): SymbolRenderInfo =
        SymbolRenderInfo(charCode.toChar().toString(), FontCategory.ROMAN, style)

    /**
     * cmsy10 符号映射表 (Font ID 8 in MicroTeX)
     *
     * 数据来源: MicroTeX src/res/sym/base.def.cpp 中所有 E(8, ...) 条目
     */
    private val cmsy10Symbols: Map<String, SymbolRenderInfo> = mapOf(
        // --- 箭头 (charCode 33-46, 108-109, 195-196) ---
        "rightarrow" to sy(33),         // →
        "to" to sy(33),                 // → (别名)
        "uparrow" to sy(34),            // ↑
        "downarrow" to sy(35),          // ↓
        "leftrightarrow" to sy(36),     // ↔
        "nearrow" to sy(37),            // ↗
        "searrow" to sy(38),            // ↘
        "simeq" to sy(39),             // ≃
        "Leftarrow" to sy(40),          // ⇐
        "Rightarrow" to sy(41),         // ⇒
        "Uparrow" to sy(42),            // ⇑
        "Downarrow" to sy(43),          // ⇓
        "Leftrightarrow" to sy(44),     // ⇔
        "nwarrow" to sy(45),            // ↖
        "swarrow" to sy(46),            // ↙
        "updownarrow" to sy(108),       // ↕
        "Updownarrow" to sy(109),       // ⇕
        "leftarrow" to sy(195),         // ←
        "gets" to sy(195),              // ← (别名)

        // --- 杂项符号 (charCode 47-64) ---
        "propto" to sy(47),             // ∝
        "prime" to sy(48),              // ′
        "infty" to sy(49),              // ∞
        "in" to sy(50),                 // ∈
        "ni" to sy(51),                 // ∋
        "owns" to sy(51),               // ∋ (别名)
        "triangle" to sy(52),           // △
        "not" to sy(54),                // negation slash
        "mapstochar" to sy(55),         // ↦ char
        "forall" to sy(56),             // ∀
        "exists" to sy(57),             // ∃
        "neg" to sy(58),                // ¬
        "lnot" to sy(58),              // ¬ (别名)
        "emptyset" to sy(59),           // ∅
        "Re" to sy(60),                 // ℜ
        "Im" to sy(61),                 // ℑ
        "top" to sy(62),                // ⊤
        "bot" to sy(63),                // ⊥
        "perp" to sy(63),               // ⊥ (别名)
        "aleph" to sy(64),              // ℵ

        // --- 集合运算符 (charCode 91-95) ---
        "cup" to sy(91),                // ∪
        "cap" to sy(92),                // ∩
        "uplus" to sy(93),              // ⊎
        "wedge" to sy(94),              // ∧
        "land" to sy(94),               // ∧ (别名)
        "vee" to sy(95),                // ∨
        "lor" to sy(95),                // ∨ (别名)

        // --- 其他运算符和关系 (charCode 96-126) ---
        "vdash" to sy(96),              // ⊢
        "dashv" to sy(97),              // ⊣
        "lfloor" to sy(98),             // ⌊
        "rfloor" to sy(99),             // ⌋
        "lceil" to sy(100),             // ⌈
        "rceil" to sy(101),             // ⌉
        "lbrace" to sy(102),            // {
        "lacc" to sy(102),              // { (别名)
        "rbrace" to sy(103),            // }
        "racc" to sy(103),              // } (别名)
        "langle" to sy(104),            // ⟨
        "rangle" to sy(105),            // ⟩
        "vert" to sy(106),              // |
        "mid" to sy(106),               // | (别名)
        "Vert" to sy(107),              // ‖
        "parallel" to sy(107),          // ‖ (别名)
        "backslash" to sy(110),         // ∖
        "setminus" to sy(110),          // ∖ (别名)
        "wr" to sy(111),                // ≀
        "sqrt" to sy(112),              // √
        "surdsign" to sy(112),          // √ (别名)
        "amalg" to sy(113),             // ∐
        "nabla" to sy(114),             // ∇
        "smallint" to sy(115),          // ∫ (small)
        "sqcup" to sy(116),             // ⊔
        "sqcap" to sy(117),             // ⊓
        "sqsubseteq" to sy(118),        // ⊑
        "sqsupseteq" to sy(119),        // ⊒
        "S" to sy(120),                 // §
        "dagger" to sy(121),            // †
        "ddagger" to sy(122),           // ‡
        "P" to sy(123),                 // ¶
        "clubsuit" to sy(124),          // ♣
        "diamondsuit" to sy(125),       // ♢
        "heartsuit" to sy(126),         // ♡

        // --- 二元运算符 (charCode 161-178) ---
        "minus" to sy(161),             // −
        "cdotp" to sy(162),             // ⋅
        "cdot" to sy(162),              // ⋅ (别名)
        "times" to sy(163),             // ×
        "ast" to sy(164),               // ∗
        "div" to sy(165),               // ÷
        "diamond" to sy(166),           // ◇
        "pm" to sy(167),                // ±
        "mp" to sy(168),                // ∓
        "oplus" to sy(169),             // ⊕
        "ominus" to sy(170),            // ⊖
        "otimes" to sy(171),            // ⊗
        "oslash" to sy(174),            // ⊘
        "odot" to sy(175),              // ⊙
        "bigcirc" to sy(176),           // ○
        "circ" to sy(177),              // ∘
        "bullet" to sy(178),            // •

        // --- 关系符号 (charCode 179-196) ---
        "asymp" to sy(179),             // ≍
        "equiv" to sy(180),             // ≡
        "subseteq" to sy(181),          // ⊆
        "supseteq" to sy(182),          // ⊇
        "leq" to sy(183),              // ≤
        "le" to sy(183),               // ≤ (别名)
        "geq" to sy(184),              // ≥
        "ge" to sy(184),               // ≥ (别名)
        "preceq" to sy(185),            // ≼
        "succeq" to sy(186),            // ≽
        "sim" to sy(187),               // ∼
        "approx" to sy(188),            // ≈
        "subset" to sy(189),            // ⊂
        "supset" to sy(190),            // ⊃
        "ll" to sy(191),                // ≪
        "gg" to sy(192),                // ≫
        "prec" to sy(193),              // ≺
        "succ" to sy(194),              // ≻
        "spadesuit" to sy(196),         // ♠
    )

    /**
     * cmmi10 符号映射表 (Font ID 2/5 in MicroTeX)
     *
     * 数据来源: MicroTeX src/res/sym/base.def.cpp 中 E(2, ...) 和 E(5, ...) 条目
     * Font 2 = cmmi10, Font 5 = cmmi10_unchanged（共用同一 TTF 文件）
     */
    private val cmmi10Symbols: Map<String, SymbolRenderInfo> = buildMap {
        // --- 小写希腊字母 (Font 5, charCode 174-196) ---
        put("alpha", mi(174))
        put("beta", mi(175))
        put("gamma", mi(176))
        put("delta", mi(177))
        put("epsilon", mi(178))
        put("zeta", mi(179))
        put("eta", mi(180))
        put("theta", mi(181))
        put("iota", mi(182))
        put("kappa", mi(183))
        put("lambda", mi(184))
        put("mu", mi(185))
        put("nu", mi(186))
        put("xi", mi(187))
        put("pi", mi(188))
        put("rho", mi(189))
        put("sigma", mi(190))
        put("tau", mi(191))
        put("upsilon", mi(192))
        put("phi", mi(193))
        put("chi", mi(194))
        put("psi", mi(195))

        // --- 希腊变体 (Font 5, charCode 33-39) ---
        put("omega", mi(33))
        put("varepsilon", mi(34))
        put("vartheta", mi(35))
        put("varpi", mi(36))
        put("varrho", mi(37))
        put("varsigma", mi(38))
        put("varphi", mi(39))

        // --- 箭头钩 (Font 5, charCode 40-47) ---
        put("leftharpoonup", mi(40))
        put("leftharpoondown", mi(41))
        put("rightharpoonup", mi(42))
        put("rightharpoondown", mi(43))
        put("lhook", mi(44))
        put("rhook", mi(45))
        put("triangleright", mi(46))
        put("triangleleft", mi(47))

        // --- 标点和杂项 (Font 5) ---
        put("ldotp", mi(58, FontStyle.Normal))
        put("normaldot", mi(58, FontStyle.Normal))
        put("comma", mi(59, FontStyle.Normal))
        put("lt", mi(60, FontStyle.Normal))
        put("slash", mi(61, FontStyle.Normal))
        put("gt", mi(62, FontStyle.Normal))
        put("star", mi(63, FontStyle.Normal))
        put("partial", mi(64))
        put("flat", mi(91, FontStyle.Normal))
        put("natural", mi(92, FontStyle.Normal))
        put("sharp", mi(93, FontStyle.Normal))
        put("smile", mi(94, FontStyle.Normal))
        put("frown", mi(95, FontStyle.Normal))
        put("ell", mi(96))
        put("omicron", mi(111))
        put("imath", mi(123))
        put("jmath", mi(124))
        put("wp", mi(125))
        put("vec", mi(126))

        // --- 大写希腊斜体变体 (Font 2, charCode 161-171) ---
        // varGamma 等是 cmmi10 中的斜体大写希腊字母
        put("varGamma", mi(161, FontStyle.Normal))
        put("varDelta", mi(162, FontStyle.Normal))
        put("varTheta", mi(163, FontStyle.Normal))
        put("varLambda", mi(164, FontStyle.Normal))
        put("varXi", mi(165, FontStyle.Normal))
        put("varPi", mi(166, FontStyle.Normal))
        put("varSigma", mi(167, FontStyle.Normal))
        put("varUpsilon", mi(168, FontStyle.Normal))
        put("varPhi", mi(169, FontStyle.Normal))
        put("varPsi", mi(170, FontStyle.Normal))
        put("varOmega", mi(171, FontStyle.Normal))
    }

    /**
     * cmex10 符号映射表 (Font ID 1 in MicroTeX)
     *
     * 数据来源: MicroTeX src/res/sym/base.def.cpp 中 E(1, ...) 条目
     * 大型运算符主要由 BigOperatorMeasurer 通过 mapBigOp() 直接处理，
     * 此处补充通过符号命令名路由的条目。
     */
    private val cmex10Symbols: Map<String, SymbolRenderInfo> = mapOf(
        "lgroup" to ex(58),
        "rgroup" to ex(59),
        "bracevert" to ex(62),
        "bigsqcup" to ex(70),
        "oint" to ex(72),
        "bigodot" to ex(74),
        "bigoplus" to ex(76),
        "bigotimes" to ex(78),
        "sum" to ex(80),
        "prod" to ex(81),
        "int" to ex(82),
        "bigcup" to ex(83),
        "bigcap" to ex(84),
        "biguplus" to ex(85),
        "bigwedge" to ex(86),
        "bigvee" to ex(87),
        "coprod" to ex(96),
        "widehat" to ex(98),
        "widetilde" to ex(101),
    )

    /**
     * cmr10 符号映射表 (Font ID 18 in MicroTeX)
     *
     * 数据来源: MicroTeX src/res/sym/base.def.cpp 中 E(18, ...) 条目
     * 大写希腊字母在 cmr10 中是直立体（非斜体）。
     */
    private val cmr10Symbols: Map<String, SymbolRenderInfo> = mapOf(
        // --- 大写希腊字母 (charCode 161-171) ---
        "Gamma" to ro(161),
        "Delta" to ro(162),
        "Theta" to ro(163),
        "Lambda" to ro(164),
        "Xi" to ro(165),
        "Pi" to ro(166),
        "Sigma" to ro(167),
        "Upsilon" to ro(168),
        "Phi" to ro(169),
        "Psi" to ro(170),
        "Omega" to ro(171),

        // --- 标点和杂项 ---
        "faculty" to ro(33),             // !
        "mathsharp" to ro(35),           // #
        "textdollar" to ro(36),          // $
        "textpercent" to ro(37),         // %
        "textampersand" to ro(38),       // &
        "textapos" to ro(39),            // '
        "lbrack" to ro(40),              // (
        "rbrack" to ro(41),              // )
        "plus" to ro(43),                // +
        "textminus" to ro(45),           // -
        "textfractionsolidus" to ro(47), // /
        "slashdel" to ro(47),            // / (别名)
        "colon" to ro(58),               // :
        "semicolon" to ro(59),           // ;
        "Relbar" to ro(61),              // =
        "equals" to ro(61),              // = (别名)
        "questiondown" to ro(62),        // ¿
        "question" to ro(63),            // ?
        "matharobase" to ro(64),         // @
        "lsqbrack" to ro(91),            // [
        "rsqbrack" to ro(93),            // ]
        "hat" to ro(94),                 // ^
        "dot" to ro(95),                 // ˙
        "mathlapos" to ro(96),           // `
        "textendash" to ro(123),         // –
        "textemdash" to ro(124),         // —
        "doubleacute" to ro(125),        // ˝
        "tilde" to ro(126),              // ~

        // --- 装饰重音符号 ---
        "grave" to ro(181),
        "acute" to ro(182),
        "check" to ro(183),
        "breve" to ro(184),
        "bar" to ro(185),
        "mathring" to ro(186),
        "bmathring" to ro(186),
        "polishlcross" to ro(195),
        "ddot" to ro(196),
        "ogonek" to ro(197),
    )

    /**
     * 合并的完整符号映射表（命令名 → SymbolRenderInfo）
     *
     * 查找优先级: cmr10 (大写希腊等) > cmsy10 (运算符、箭头) > cmmi10 (希腊变量) > cmex10 (大型运算符)
     * 注意: cmr10 中的 Gamma/Delta 等大写希腊字母覆盖 cmmi10 中的 varGamma/varDelta 等
     */
    private val texSymbolMap: Map<String, SymbolRenderInfo> = buildMap {
        putAll(cmex10Symbols)
        putAll(cmmi10Symbols)
        putAll(cmsy10Symbols)
        putAll(cmr10Symbols)    // cmr10 最高优先级（大写希腊直立体）
    }

    /**
     * Unicode 字符 → SymbolRenderInfo 的反向映射表
     *
     * 基于 SymbolMap 中 "命令名 → Unicode" 的对应关系，构建 "Unicode → SymbolRenderInfo"。
     * 这样当 LatexNode.Symbol 的 unicode 字段是 Unicode 字符时，也能正确路由到 CM 字体。
     *
     * 注意：多个命令名可能映射到同一个 Unicode（如 "le" 和 "leq" 都映射到 "≤"），
     * 这里取最后一个覆盖，因为它们指向同一个 SymbolRenderInfo。
     */
    private val unicodeToSymbolMap: Map<String, SymbolRenderInfo> by lazy {
        // 命令名 → Unicode 的映射（与 SymbolMap 保持一致）
        val cmdToUnicode = mapOf(
            // 希腊字母（小写）
            "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
            "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
            "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
            "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ",
            "pi" to "π", "varpi" to "ϖ", "rho" to "ρ", "varrho" to "ϱ",
            "sigma" to "σ", "varsigma" to "ς", "tau" to "τ", "upsilon" to "υ",
            "phi" to "φ", "varphi" to "ϕ", "chi" to "χ", "psi" to "ψ", "omega" to "ω",
            // 希腊字母（大写）
            "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ",
            "Xi" to "Ξ", "Pi" to "Π", "Sigma" to "Σ", "Upsilon" to "Υ",
            "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω",
            // 运算符
            "times" to "×", "div" to "÷", "pm" to "±", "mp" to "∓",
            "cdot" to "⋅", "ast" to "∗", "star" to "⋆", "circ" to "∘",
            "bullet" to "•", "oplus" to "⊕", "ominus" to "⊖", "otimes" to "⊗",
            "oslash" to "⊘", "odot" to "⊙", "diamond" to "◇", "bigcirc" to "○",
            // 关系符号
            "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥",
            "neq" to "≠", "ne" to "≠", "equiv" to "≡", "approx" to "≈",
            "cong" to "≅", "sim" to "∼", "simeq" to "≃", "propto" to "∝",
            "ll" to "≪", "gg" to "≫", "subset" to "⊂", "supset" to "⊃",
            "subseteq" to "⊆", "supseteq" to "⊇", "in" to "∈", "notin" to "∉",
            "ni" to "∋", "perp" to "⊥", "parallel" to "∥",
            "prec" to "≺", "succ" to "≻", "preceq" to "≼", "succeq" to "≽",
            "asymp" to "≍",
            "sqsubseteq" to "⊑", "sqsupseteq" to "⊒",
            // 箭头
            "leftarrow" to "←", "rightarrow" to "→", "to" to "→",
            "leftrightarrow" to "↔", "Leftarrow" to "⇐", "Rightarrow" to "⇒",
            "Leftrightarrow" to "⇔", "uparrow" to "↑", "downarrow" to "↓",
            "updownarrow" to "↕", "Uparrow" to "⇑", "Downarrow" to "⇓",
            "Updownarrow" to "⇕", "mapsto" to "↦",
            "longrightarrow" to "⟶", "longleftarrow" to "⟵",
            "longleftrightarrow" to "⟷",
            "Longleftarrow" to "⟸", "Longrightarrow" to "⟹",
            "Longleftrightarrow" to "⟺", "longmapsto" to "⟼",
            "nearrow" to "↗", "searrow" to "↘", "nwarrow" to "↖", "swarrow" to "↙",
            "gets" to "←",
            // 集合/逻辑符号
            "emptyset" to "∅", "varnothing" to "∅",
            "cap" to "∩", "cup" to "∪", "setminus" to "∖",
            "forall" to "∀", "exists" to "∃", "nexists" to "∄",
            "neg" to "¬", "lnot" to "¬",
            "land" to "∧", "lor" to "∨", "wedge" to "∧", "vee" to "∨",
            "implies" to "⟹", "iff" to "⟺",
            // 微积分
            "infty" to "∞", "partial" to "∂", "nabla" to "∇",
            "int" to "∫", "iint" to "∬", "iiint" to "∭", "oint" to "∮",
            // 特殊符号
            "prime" to "′", "hbar" to "ℏ", "ell" to "ℓ", "wp" to "℘",
            "Re" to "ℜ", "Im" to "ℑ", "aleph" to "ℵ",
            "top" to "⊤", "bot" to "⊥",
            "vdash" to "⊢", "dashv" to "⊣",
            "dagger" to "†", "ddagger" to "‡",
            "clubsuit" to "♣", "diamondsuit" to "♢", "heartsuit" to "♡", "spadesuit" to "♠",
            // 定界符
            "langle" to "⟨", "rangle" to "⟩",
            "lfloor" to "⌊", "rfloor" to "⌋",
            "lceil" to "⌈", "rceil" to "⌉",
            "lbrace" to "{", "rbrace" to "}",
            // 大型运算符
            "sum" to "∑", "prod" to "∏", "coprod" to "∐",
            "bigcup" to "⋃", "bigcap" to "⋂", "bigvee" to "⋁", "bigwedge" to "⋀",
            "bigoplus" to "⨁", "bigotimes" to "⨂", "bigsqcup" to "⨆", "bigodot" to "⨀",
            "biguplus" to "⨄",
            // 其他杂项
            "minus" to "−", "cdotp" to "⋅",
            "backslash" to "∖",
            "wr" to "≀", "amalg" to "∐",
            "sqcup" to "⊔", "sqcap" to "⊓",
            "uplus" to "⊎",
            "triangle" to "△",
            // lbrack/rbrack 是 ( )，属于普通定界符，不放入 unicodeToSymbolMap
            // 避免其他节点的 unicode 字段恰好是 "(" 时被错误路由
        )

        buildMap {
            for ((cmdName, unicode) in cmdToUnicode) {
                val info = texSymbolMap[cmdName]
                if (info != null) {
                    put(unicode, info)
                }
            }
        }
    }

    /**
     * 解析符号对应的 TTF char code 和字体信息
     *
     * 这是符号渲染的核心入口。支持两种查找方式：
     * 1. 通过命令名查找（如 "alpha", "Rightarrow", "cup"）
     * 2. 通过 Unicode 字符反向查找（如 "α", "→", "∪"）
     *
     * 只有当 fontFamilies 不为 null（即加载了 CM 字体文件）时才返回映射结果，
     * 否则返回 null 让调用方回退到 Unicode 渲染。
     *
     * @param symbolName LaTeX 符号命令名（不含反斜杠）或 Unicode 字符
     * @param fontFamilies 已加载的字体家族集合，null 时回退到 Unicode 渲染
     * @return 符号渲染信息，null 表示该符号无需特殊字体路由（使用 Unicode 渲染）
     */
    fun resolveSymbol(
        symbolName: String,
        fontFamilies: LatexFontFamilies?
    ): SymbolRenderInfo? {
        // 只有使用内嵌 CM 字体时才走 TeX 编码映射，外部自定义字体使用标准 Unicode
        if (fontFamilies == null || !fontFamilies.isDefaultCM) return null
        // 1. 优先按命令名查找
        texSymbolMap[symbolName]?.let { return it }
        // 2. 按 Unicode 字符反向查找
        return unicodeToSymbolMap[symbolName]
    }

    /**
     * 根据 SymbolRenderInfo 获取实际的 FontFamily
     */
    fun getFontForSymbol(
        info: SymbolRenderInfo,
        fontFamilies: LatexFontFamilies?
    ): FontFamily? {
        return getFont(info.fontCategory, fontFamilies)
    }

    /**
     * 解析给定字体类别应使用的字体家族
     *
     * 所有平台统一使用内嵌 CM 字体，不做平台判断。
     *
     * @param category 字体类别（决定使用哪个字体槽位）
     * @param fontFamilies 已加载的字体家族集合
     * @return 应使用的 FontFamily，null 表示使用默认字体
     */
    fun resolve(
        category: FontCategory,
        fontFamilies: LatexFontFamilies?
    ): FontFamily? {
        return getFont(category, fontFamilies)
    }

    /**
     * cmsy10 中使用 TeX 内部编码、Unicode codepoint 与实际字形不对应的定界符。
     * 这些字符必须使用 cmr10（Roman）渲染，因为 cmr10 的 cmap 表中
     * ( ) [ ] 的 Unicode codepoint 与标准 ASCII 一致。
     *
     * cmsy10 中可正常渲染的定界符（如 { } | ‖ ⟨ ⟩ ⌊ ⌋ ⌈ ⌉）不在此集合中。
     */
    private val romanDelimiters = setOf("(", ")", "[", "]")

    /**
     * 获取定界符渲染上下文
     *
     * 统一替代散落在 MathMeasurer/DelimiterMeasurer/MatrixMeasurer 中的 delimiterContext 方法。
     *
     * - 使用内嵌 CM 字体（isDefaultCM）时，根据定界符字符选择正确的字体槽位：
     *   - ( ) [ ] → cmr10 (Roman)，因为 cmsy10 的 TeX 编码与 Unicode 不对应
     *   - { } | ‖ ⟨ ⟩ ⌊ ⌋ ⌈ ⌉ 等 → cmsy10 (Symbol)
     * - 使用外部自定义字体时，不切换字体槽位，保持 context 原有字体
     */
    fun delimiterContext(
        context: RenderContext,
        delimiter: String = "(",
        scale: Float = 1.0f
    ): RenderContext {
        val fontWeight = compensatedFontWeight(400, scale)

        // 外部自定义字体：不切换字体槽位，使用标准 Unicode 渲染
        val fontFamilies = context.fontFamilies
        if (fontFamilies == null || !fontFamilies.isDefaultCM) {
            return context.copy(
                fontStyle = FontStyle.Normal,
                fontWeight = fontWeight
            )
        }

        // 内嵌 CM 字体：根据定界符选择正确的字体槽位
        val fontCategory = if (delimiter in romanDelimiters) {
            FontCategory.ROMAN
        } else {
            FontCategory.SYMBOL
        }
        val fontFamily = getFont(fontCategory, fontFamilies)

        return context.copy(
            fontStyle = FontStyle.Normal,
            fontFamily = fontFamily ?: context.fontFamily,
            fontWeight = fontWeight
        )
    }

    /**
     * 根据缩放比例计算补偿后的 FontWeight
     *
     * 字号放大后笔画变粗，需降低 weight 补偿：
     * - scaleFactor = 1.0 → weight 不变
     * - scaleFactor = 2.0 → weight 降至约 100
     */
    fun compensatedFontWeight(baseWeight: Int, scaleFactor: Float): FontWeight {
        val compensated = when {
            scaleFactor <= 1.0f -> baseWeight
            scaleFactor >= 2.0f -> 100
            else -> {
                val t = (scaleFactor - 1.0f) / 1.0f
                (baseWeight - t * (baseWeight - 100)).toInt().coerceIn(100, baseWeight)
            }
        }
        return FontWeight(compensated)
    }

    // =========================================================================
    // 私有工具方法
    // =========================================================================

    private fun getFont(category: FontCategory, fontFamilies: LatexFontFamilies?): FontFamily? {
        if (fontFamilies == null) return null
        return when (category) {
            FontCategory.ROMAN -> fontFamilies.roman
            FontCategory.MATH_ITALIC -> fontFamilies.mathItalic
            FontCategory.SYMBOL -> fontFamilies.symbol
            FontCategory.EXTENSION -> fontFamilies.extension
            FontCategory.SANS_SERIF -> fontFamilies.sansSerif
            FontCategory.MONOSPACE -> fontFamilies.monospace
            FontCategory.BLACKBOARD_BOLD -> fontFamilies.blackboardBold
            FontCategory.CALLIGRAPHIC -> fontFamilies.calligraphic
            FontCategory.FRAKTUR -> fontFamilies.fraktur
            FontCategory.SCRIPT -> fontFamilies.script
        }
    }
}
