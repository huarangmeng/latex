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


package com.hrm.latex.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.AnimatedLatex
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.LatexAnimationConfig
import com.hrm.latex.renderer.LatexTransition
import com.hrm.latex.renderer.model.HighlightConfig
import com.hrm.latex.renderer.model.HighlightRange
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.systemLatexFontFamilies
import kotlinx.coroutines.delay

/**
 * 基础 LaTeX 预览示例
 * 涵盖所有基础功能,包括:
 * - 基础级别: 简单文本、上下标、分数
 * - 初级级别: 多项式、方程、简单求和积分
 * - 中级级别: 嵌套结构、根式、复杂运算
 * - 高级级别: 连分数、级数、复杂表达式
 * - 专家级别: 物理公式、复变函数、高级积分
 * - 极其复杂级别: 量子力学、相对论、终极表达式
 * - 分隔符专题: 括号、自动伸缩、手动大小控制
 * - 装饰符号专题: 上标装饰、箭头、帽子等
 * - 间距专题: 负空格、自定义空格、水平间距
 */

// ========== 数据模型 ==========

val basicLatexPreviewGroups = listOf(
    PreviewGroup(
        id = "basic",
        title = "1. Basic notation",
        description = "Text, superscripts, subscripts, and fractions",
        items = listOf(
            PreviewItem("1", "Plain text", "Hello LaTeX"),
            PreviewItem("2", "Superscripts", "x^2"),
            PreviewItem("3", "Subscripts", "a_i"),
            PreviewItem("4", "Combined scripts", "x_i^2"),
            PreviewItem("5", "Simple fractions", "\\frac{1}{2}"),
            PreviewItem(
                "6",
                "Issue #41: unbraced single-token arguments",
                "\\frac12 \\quad \\binom12 \\quad \\int_12"
            ),
        )
    ),
    PreviewGroup(
        id = "elementary",
        title = "2. Elementary formulas",
        description = "Polynomials, equations, sums, and integrals",
        items = listOf(
            PreviewItem("1", "Polynomial", "ax^2 + bx + c = 0"),
            PreviewItem("2", "Pythagorean theorem", "a^2 + b^2 = c^2"),
            PreviewItem("3", "Quadratic formula", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"),
            PreviewItem("4", "Summation", "\\sum_{i=1}^{n} i"),
            PreviewItem("5", "Integral", "\\int_0^1 x dx"),
            PreviewItem("6", "dots (ldots)", "a_1, a_2, \\dots, a_n"),
            PreviewItem("7", "dots (cdots)", "a_1 + a_2 + \\dots + a_n"),
        )
    ),
    PreviewGroup(
        id = "intermediate",
        title = "3. Nested expressions",
        description = "Nested fractions, radicals, and compound expressions",
        items = listOf(
            PreviewItem("1", "Nested fractions", "\\frac{1}{2 + \\frac{1}{3}}"),
            PreviewItem("2", "Compound fraction", "\\frac{a + b}{c + d}"),
            PreviewItem("3", "Square root", "\\sqrt{x^2 + y^2}"),
            PreviewItem("4", "Nested summation", "\\sum_{i=1}^{n} i^2 = \\frac{n(n+1)(2n+1)}{6}"),
            PreviewItem("5", "Definite integral", "\\int_{0}^{\\infty} e^{-x} dx = 1"),
            PreviewItem("6", "Product", "\\prod_{i=1}^{n} x_i"),
            PreviewItem("7", "coprod", "\\coprod_{i=1}^{n} A_i"),
            PreviewItem("8", "bigoplus", "\\bigoplus_{k=1}^{n} V_k"),
            PreviewItem("9", "bigotimes", "\\bigotimes_{i=1}^{m} W_i"),
            PreviewItem("10", "bigsqcup", "\\bigsqcup_{j \\in J} S_j"),
            PreviewItem("11", "bigodot", "\\bigodot_{i} R_i"),
            PreviewItem("12", "biguplus", "\\biguplus_{k} T_k"),
            PreviewItem("13", "Limit", "\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1"),
            PreviewItem("14", "Derivative", "\\frac{d}{dx}(x^n) = nx^{n-1}"),
            PreviewItem(
                "15",
                "Derivative with scalable delimiters",
                "\\frac{d}{dx}\\left(x^n\\right) = nx^{n-1}"
            ),
            PreviewItem(
                "16",
                "Integral inside parentheses",
                "\\left[ \\int_0^1 \\frac{dx}{\\sqrt{1-x^2}} \\right] = \\frac{\\pi}{2}"
            ),
            PreviewItem("17", "bmod", "a \\bmod b"),
            PreviewItem("18", "pmod", "x \\equiv y \\pmod{n}"),
            PreviewItem("19", "mod", "a \\mod b"),
        )
    ),
    PreviewGroup(
        id = "advanced",
        title = "4. Advanced formulas",
        description = "Continued fractions, series, and advanced notation",
        items = listOf(
            PreviewItem("1", "Continued fraction", "1 + \\frac{1}{1 + \\frac{1}{1 + \\frac{1}{1 + x}}}"),
            PreviewItem("2", "Nested exponents", "e^{i\\pi} + 1 = 0"),
            PreviewItem("3", "Nested radicals", "\\sqrt{1 + \\sqrt{1 + \\sqrt{1 + x}}}"),
            PreviewItem(
                "4",
                "Determinant",
                "\\det(A) = \\sum_{\\sigma \\in S_n} \\operatorname{sgn}(\\sigma) \\prod_{i=1}^{n} a_{i,\\sigma(i)}"
            ),
            PreviewItem("5", "Double integral", "\\int_{0}^{1} \\int_{0}^{1} x^2 + y^2 dx dy"),
            PreviewItem(
                "6",
                "Taylor series",
                "f(x) = \\sum_{n=0}^{\\infty} \\frac{f^{(n)}(a)}{n!}(x-a)^n"
            ),
            PreviewItem("7", "Nested summation", "\\sum_{k=1}^{n} \\frac{1}{k^2} = \\frac{\\pi^2}{6}"),
            PreviewItem("8", "Named operators", "\\operatorname{Tr}(A)"),
            PreviewItem(
                "9",
                "Named operators with subscripts",
                "\\operatorname{argmax}_{x \\in \\mathbb{R}} f(x)"
            ),
            PreviewItem(
                "9-star",
                "Named operators with limits",
                "\\operatorname*{argmax}_{x \\in \\mathbb{R}} f(x)"
            ),
            PreviewItem("10", "operatorname + limits", "\\operatorname{Res}\\limits_{z=0} f(z)"),
            PreviewItem("11", "mathop + limits", "\\[\\int_0^\\infty  {1 - \\mathop \\prod \\limits_{i = 1}^n \\left( {1 - {e^{ - {p_i}t}}} \\right){\\text{d}}t} \\]"),
            PreviewItem("12", "Blackboard bold: Bbb alias", "\\Bbb{R} \\quad \\mathbb{R} \\quad \\Bbb R"),
            PreviewItem("13", "cal/frak/scr (legacy)", "\\cal A \\quad \\frak g \\quad \\scr F"),
            PreviewItem("14", "Font size scale", "\\tiny x \\scriptsize x \\footnotesize x \\small x \\normalsize x \\large x \\Large x \\LARGE x \\huge x \\Huge x"),
            PreviewItem("15", "Font size scopes", "{\\small a + b} \\quad {\\Huge c + d}"),
            PreviewItem("16", "Generalized and infix fractions", "\\genfrac{(}{)}{0pt}{0}{a+b}{c} \\quad {n \\choose k}"),
            PreviewItem("17", "Split fractions", "\\splitdfrac{a+b+c}{d+e}"),
        )
    ),
    PreviewGroup(
        id = "expert",
        title = "5. Mathematical physics",
        description = "Physics, complex analysis, and advanced integrals",
        items = listOf(
            PreviewItem(
                "1",
                "Cauchy integral formula",
                "f(z) = \\frac{1}{2\\pi i} \\oint_{\\gamma} \\frac{f(\\zeta)}{\\zeta - z} d\\zeta"
            ),
            PreviewItem(
                "2",
                "Fourier transform",
                "F(\\omega) = \\int_{-\\infty}^{\\infty} f(t) e^{-i\\omega t} dt"
            ),
            PreviewItem("3", "Gaussian integral", "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"),
            PreviewItem(
                "4",
                "Riemann zeta function",
                "\\zeta(s) = \\sum_{n=1}^{\\infty} \\frac{1}{n^s} = \\prod_{p} \\frac{1}{1-p^{-s}}"
            ),
            PreviewItem(
                "5",
                "Stokes' theorem",
                "\\int_{\\partial \\Omega} \\omega = \\int_{\\Omega} d\\omega"
            ),
            PreviewItem(
                "6",
                "Vector field",
                "v(x, t_\\theta)"
            )
        )
    ),
    PreviewGroup(
        id = "extreme",
        title = "6. Complex expressions",
        description = "Quantum mechanics, relativity, and deeply nested formulas",
        items = listOf(
            PreviewItem(
                "1",
                "Extended continued fraction",
                "\\frac{1}{a + \\frac{b}{c + \\frac{d}{e + \\frac{f}{g + \\frac{h}{i + j}}}}}"
            ),
            PreviewItem(
                "2",
                "Deeply nested radicals",
                "\\sqrt{x + \\sqrt{y + \\sqrt{z + \\sqrt{w + \\sqrt{v + u}}}}}"
            ),
            PreviewItem(
                "3",
                "Mixed nested expressions",
                "\\sum_{n=1}^{\\infty} \\frac{(-1)^n}{n} \\int_0^1 x^n \\left(\\frac{1}{1+x^2}\\right)^{\\frac{1}{2}} dx"
            ),
            PreviewItem(
                "4",
                "Schrodinger equation",
                "i\\hbar\\frac{\\partial}{\\partial t}\\Psi(\\vec{r},t) = \\left[-\\frac{\\hbar^2}{2m}\\nabla^2 + V(\\vec{r},t)\\right]\\Psi(\\vec{r},t)"
            ),
            PreviewItem(
                "5",
                "Path integral",
                "\\langle x_f | e^{-iHt/\\hbar} | x_i \\rangle = \\int \\mathcal{D}[x(t)] e^{iS[x]/\\hbar}"
            ),
            PreviewItem(
                "6",
                "Einstein field equations",
                "R_{\\mu\\nu} - \\frac{1}{2}Rg_{\\mu\\nu} + \\Lambda g_{\\mu\\nu} = \\frac{8\\pi G}{c^4}T_{\\mu\\nu}"
            ),
            PreviewItem(
                "7",
                "Partition function",
                "Z = \\sum_{n=0}^{\\infty} e^{-\\beta E_n} = \\text{Tr}\\left(e^{-\\beta \\hat{H}}\\right)"
            ),
            PreviewItem(
                "8",
                "Feynman propagator",
                "G(x-y) = \\int \\frac{d^4p}{(2\\pi)^4} \\frac{e^{-ip(x-y)}}{p^2 - m^2 + i\\epsilon}"
            ),
            PreviewItem(
                "9",
                "Yang-Mills Lagrangian",
                "\\mathcal{L} = -\\frac{1}{4}F_{\\mu\\nu}^a F^{a\\mu\\nu} + \\bar{\\psi}(i\\gamma^\\mu D_\\mu - m)\\psi"
            ),
            PreviewItem(
                "10",
                "Combined advanced expression",
                "\\sum_{n=0}^{\\infty} \\frac{1}{n!} \\int_{-\\infty}^{\\infty} \\left(\\frac{d}{dx}\\right)^n \\left[\\frac{\\sqrt{\\pi}}{\\sqrt{1+x^2}} \\cdot e^{-\\frac{x^2}{2\\sigma^2}} \\cdot \\prod_{k=1}^{n} \\left(1 + \\frac{x^k}{k!}\\right)\\right] dx"
            ),
            PreviewItem("11", "Radial Schrodinger equation", "\\dfrac{1}{r^2}\\dfrac{\\partial }{\\partial r}\\left (r^2\\dfrac{\\partial }{\\partial r} \\right )  R(r)+\\left[\\dfrac{2\\mu}{\\hbar^2}(E-V(r)) -\\dfrac{\\alpha}{r^2} \\right ]R(r)=0")
        )
    ),
    PreviewGroup(
        id = "radicals",
        title = "7. Radicals",
        description = "Radical sizes, nested roots, and indexed roots",
        items = listOf(
            PreviewItem("1", "Square root", "\\sqrt{2}"),
            PreviewItem("2", "Root of a variable", "\\sqrt{x^2 + y^2}"),
            PreviewItem("3", "Root of a fraction", "\\sqrt{\\frac{a}{b}}"),
            PreviewItem("4", "Cube root", "\\sqrt[3]{8} = 2"),
            PreviewItem("5", "Indexed root", "\\sqrt[n]{x^n} = |x|"),
            PreviewItem("6", "Nested radicals", "\\sqrt{1 + \\sqrt{1 + \\sqrt{1 + x}}}"),
            PreviewItem(
                "7",
                "Root of a large expression",
                "\\sqrt{\\frac{a^2 + b^2}{c^2 + d^2}}"
            ),
            PreviewItem(
                "8",
                "Radicals and fractions",
                "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"
            ),
            PreviewItem(
                "9",
                "Radical in a Gaussian integral",
                "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"
            ),
            PreviewItem(
                "10",
                "Deeply nested roots",
                "\\sqrt{x + \\sqrt{y + \\sqrt{z + \\sqrt{w + \\sqrt{v + u}}}}}"
            ),
            PreviewItem(
                "11",
                "Integral under a radical",
                "\\sqrt{\\int_0^1 f(x) dx}"
            ),
            PreviewItem(
                "12",
                "Radicals with scripts",
                "\\sqrt{a_1^2 + a_2^2 + \\cdots + a_n^2}"
            ),
        )
    ),
    PreviewGroup(
        id = "delimiters",
        title = "8. Delimiters",
        description = "Parentheses, automatic scaling, and manual sizes",
        items = listOf(
            PreviewItem(
                "1",
                "Basic delimiters",
                "\\left( x + y \\right) \\quad \\left[ a + b \\right] \\quad \\left\\{ c + d \\right\\}"
            ),
            PreviewItem(
                "2",
                "Automatically scaled delimiters",
                "\\left( \\frac{a}{b} \\right) + \\left[ \\frac{x^2}{y^2} \\right]"
            ),
            PreviewItem(
                "3",
                "Evaluation bar: asymmetric delimiters",
                "\\left. \\frac{d}{dx}x^2 \\right|_{x=0} = 0"
            ),
            PreviewItem("4", "Piecewise function: asymmetric delimiters", "f(x) = \\left\\{ x^2, x > 0 \\right."),
            PreviewItem(
                "5",
                "Evaluation of a compound expression",
                "\\left. \\frac{d^2}{dx^2} \\left( x^3 + 2x^2 - x + 1 \\right) \\right|_{x=1} = 10"
            ),
            PreviewItem(
                "6",
                "Manual size: big",
                "\\big( \\frac{1}{2} \\big) \\quad \\big[ x + y \\big] \\quad \\big\\{ a, b \\big\\}"
            ),
            PreviewItem(
                "7",
                "Manual size: Big",
                "\\Big( \\frac{a}{b} \\Big) \\quad \\Big[ \\frac{x^2}{y^2} \\Big] \\quad \\Big| x \\Big|"
            ),
            PreviewItem(
                "8",
                "Manual size: bigg",
                "\\bigg( \\sum_{i=1}^n x_i \\bigg) \\quad \\bigg\\{ \\frac{a+b}{c+d} \\bigg\\}"
            ),
            PreviewItem(
                "9",
                "Manual size: Bigg",
                "\\Bigg[ \\int_0^1 \\frac{dx}{\\sqrt{1-x^2}} \\Bigg] = \\frac{\\pi}{2}"
            ),
            PreviewItem(
                "10",
                "Manual delimiter size comparison",
                "\\big| \\Big| \\bigg| \\Bigg| x \\Bigg| \\bigg| \\Big| \\big|"
            ),
            PreviewItem(
                "11",
                "Special delimiters",
                "\\left\\langle \\psi \\right\\rangle \\quad \\left\\lfloor x \\right\\rfloor \\quad \\left\\lceil y \\right\\rceil"
            ),
            PreviewItem(
                "12",
                "Mixed delimiter styles",
                "\\Bigg( \\left. \\frac{df}{dx} \\right|_{x=0} + \\Big[ \\sum_{i=1}^n x_i \\Big] \\Bigg)"
            ),
            PreviewItem(
                "13",
                "Nested asymmetric delimiters",
                "\\left\\{ \\left. x^2 \\right|_{x=1}, \\left. y^2 \\right|_{y=2} \\right\\}"
            ),
            PreviewItem(
                "14",
                "Absolute values and norms",
                "\\big| x \\big| \\quad \\Big\\| \\mathbf{v} \\Big\\| \\quad \\left| \\frac{a}{b} \\right|"
            ),
            PreviewItem(
                "15",
                "Quantum states: Dirac notation",
                "\\big\\langle \\psi \\big| \\hat{H} \\big| \\phi \\big\\rangle = E"
            ),
            PreviewItem("16", "Single vertical bars", "\\left\\lvert x \\right\\rvert"),
            PreviewItem("17", "Double vertical bars", "\\left\\lVert v \\right\\rVert"),
            PreviewItem("18", "Big lvert", "\\Big\\lvert x \\Big\\rvert"),
            PreviewItem("19", "Brace aliases", "\\left{123\\right} \\left\\lbrace12345\\right\\rbrace"),
            PreviewItem("20", "Middle delimiters", "\\left\\langle x \\middle| y \\right\\rangle"),
        )
    ),
    PreviewGroup(
        id = "accents",
        title = "9. Accents and decorations",
        description = "Accents, arrows, braces, stacking, and strike-throughs",
        items = listOf(
            PreviewItem("1", "Hat accent", "\\hat{x}"),
            PreviewItem("2", "Tilde accent", "\\tilde{y}"),
            PreviewItem("3", "Overline", "\\overline{AB}"),
            PreviewItem("4", "Underline", "\\underline{text}"),
            PreviewItem("5", "Vector accent", "\\vec{v}"),
            PreviewItem("6", "Dot accent", "\\dot{x}"),
            PreviewItem("7", "Double-dot accent", "\\ddot{x}"),
            PreviewItem("8", "Overbrace", "\\overbrace{a+b+c}"),
            PreviewItem("9", "Underbrace", "\\underbrace{x+y+z}"),
            PreviewItem("10", "Wide hat", "\\widehat{ABC}"),
            PreviewItem("11", "Right arrow accent", "\\overrightarrow{AB}"),
            PreviewItem("12", "Left arrow accent", "\\overleftarrow{BA}"),
            PreviewItem("13", "Cancellation", "\\cancel{x+y}"),
            PreviewItem("14", "Extensible right arrow", "\\xrightarrow{f}"),
            PreviewItem("15", "Extensible left arrow", "\\xleftarrow{g}"),
            PreviewItem("16", "Arrow with a lower label", "\\xrightarrow[n\\to\\infty]{\\text{limit}}"),
            PreviewItem("17", "Stacked annotation above", "A \\overset{?}{=} B"),
            PreviewItem("18", "Stacked annotation below", "\\underset{n \\to \\infty}{\\lim} f(x)"),
            PreviewItem("19", "stackrel", "x \\stackrel{def}{=} y + 1"),
            PreviewItem(
                "20",
                "Arrows with stacked annotations",
                "A \\overset{f}{\\to} B \\underset{g}{\\to} C"
            ),
            PreviewItem(
                "21",
                "Annotations above and below an arrow",
                "A \\overset{f}{\\underset{g}{\\to}} B"
            ),
            PreviewItem(
                "22",
                "Combined decorations",
                "\\widehat{ABC} + \\overrightarrow{PQ} + \\cancel{X}"
            ),
            PreviewItem(
                "23",
                "Physics notation",
                "\\vec{F} = m\\vec{a} \\quad \\cancel{E_1} + E_2"
            ),
            PreviewItem("24", "Right hook arrow", "\\xhookrightarrow{f}"),
            PreviewItem("25", "Left hook arrow", "\\xhookleftarrow{g}"),
            PreviewItem("26", "Hook arrow with a lower label", "\\xhookrightarrow[n\\to\\infty]{\\text{inclusion}}"),
            PreviewItem("27", "Grave accent", "\\grave{a}"),
            PreviewItem("28", "Acute accent", "\\acute{e}"),
            PreviewItem("29", "Check accent", "\\check{s}"),
            PreviewItem("30", "Breve accent", "\\breve{u}"),
            PreviewItem("31", "Ring accent", "\\ring{A}"),
            PreviewItem("32", "Triple-dot accent", "\\dddot{x}"),
            PreviewItem("33", "Overbracket", "\\overbracket{x + y}"),
            PreviewItem("34", "Underbracket", "\\underbracket{a + b}"),
            PreviewItem("35", "Nested brackets", "\\overbracket{\\frac{a}{b} + c}"),
            PreviewItem("36", "overbrace vs overbracket", "\\overbrace{x+y} \\quad \\overbracket{x+y}"),
            PreviewItem("37", "Double right arrow", "A \\xRightarrow{f} B"),
            PreviewItem("38", "Double left arrow", "A \\xLeftarrow{g} B"),
            PreviewItem("39", "Double bidirectional arrow", "A \\xLeftrightarrow{\\sim} B"),
            PreviewItem("40", "Mapping arrow", "x \\xmapsto{f} f(x)"),
            PreviewItem("41", "Double arrow with a lower label", "A \\xRightarrow[below]{above} B"),
            PreviewItem("42", "Extensible arrow comparison", "\\xrightarrow{f} \\quad \\xRightarrow{f} \\quad \\xmapsto{f}"),
            PreviewItem("43", "Wide accents and arrow decorations", "\\widecheck{ABC} \\quad \\overleftrightarrow{AB} \\quad \\underparen{xy}"),
        )
    ),
    PreviewGroup(
        id = "colors",
        title = "10. Colors and backgrounds",
        description = "Text colors, formula highlights, and colored backgrounds",
        items = listOf(
            PreviewItem("1", "Basic colors", "\\color{red}{red} + \\color{blue}{blue}"),
            PreviewItem("2", "Text color", "\\textcolor{green}{\\text{Green text}}"),
            PreviewItem("3", "Color within a formula", "x + \\color{red}{y^2} = \\color{blue}{z}"),
            PreviewItem("4", "Colored fractions", "\\frac{\\color{red}{a}}{\\color{blue}{b}}"),
            PreviewItem(
                "5",
                "Multiple colors",
                "\\color{red}{R} \\color{orange}{O} \\color{yellow}{Y} \\color{green}{G} \\color{blue}{B}"
            ),
            PreviewItem("6", "Emphasizing part of a formula", "E = mc^2 \\quad \\color{red}{\\text{Mass-energy equivalence}}"),
            PreviewItem("7", "Hexadecimal colors", "\\color{#FF5733}{orange} \\color{#33FF57}{green}"),
            PreviewItem("8", "Background color", "\\colorbox{yellow}{Important}"),
            PreviewItem("9", "Colored frame and background", "\\fcolorbox{red}{yellow}{Important}"),
            PreviewItem("10", "Blue background", "\\colorbox{cyan}{x^2 + y^2}"),
            PreviewItem("11", "Combined colored frames", "\\fcolorbox{blue}{#FFFFCC}{E = mc^2}"),
        )
    ),
    PreviewGroup(
        id = "special_effects",
        title = "11. Boxes and layout effects",
        description = "Boxes, invisible spacing, cancellation, negation, height control, and text direction",
        items = listOf(
            PreviewItem("1", "Boxed expression", "\\boxed{E = mc^2}"),
            PreviewItem("2", "Boxed fraction", "\\boxed{\\frac{a + b}{c}}"),
            PreviewItem("3", "Boxed summation", "\\boxed{\\sum_{i=1}^{n} x_i}"),
            PreviewItem("4", "Multiple boxes", "\\boxed{x} + \\boxed{y} = \\boxed{z}"),
            PreviewItem("5", "Colored boxes", "\\boxed{\\color{red}{x^2} + \\color{blue}{y^2}} = r^2"),
            PreviewItem("6", "Nested boxes", "\\boxed{\\boxed{a} + b}"),
            PreviewItem(
                "7",
                "Alignment with invisible content",
                "\\begin{aligned} x &= 1234 \\\\ \\phantom{x} &= 5678 \\end{aligned}"
            ),
            PreviewItem("8", "Invisible placeholder", "a + \\phantom{bbb} = c"),
            PreviewItem("9", "Compound invisible content", "\\frac{a}{\\phantom{a}b\\phantom{a}}"),
            PreviewItem("10", "Boxes with invisible spacing", "\\boxed{x} + \\phantom{+ y} = z"),
            PreviewItem("11", "Reverse cancellation", "\\bcancel{x+y}"),
            PreviewItem("12", "Crossed cancellation", "\\xcancel{abc}"),
            PreviewItem("13", "Cancellation styles", "\\cancel{a} + \\bcancel{b} + \\xcancel{c}"),
            PreviewItem("14", "Not equal", "a \\not= b"),
            PreviewItem("15", "Not an element", "x \\not\\in S"),
            PreviewItem("16", "Not a subset", "A \\not\\subset B"),
            PreviewItem("17", "Remove vertical extent", "x + \\smash{\\frac{a}{b}} + y"),
            PreviewItem("18", "Remove top extent", "x + \\smash[t]{\\frac{a}{b}} + y"),
            PreviewItem("19", "Remove bottom extent", "x + \\smash[b]{\\frac{a}{b}} + y"),
            PreviewItem("20", "Invisible vertical extent", "\\left(\\vphantom{\\frac{a}{b}} x\\right)"),
            PreviewItem("21", "Invisible horizontal extent", "a + \\hphantom{bbb} + c"),
            PreviewItem("22", "Multiline conditions", "\\sum_{\\substack{i<n \\\\ j<m}} x_{ij}"),
            PreviewItem("23", "Framed text", "\\fbox{Important}"),
            PreviewItem("24", "Framed math", "\\fbox{x^2 + y^2 = r^2}"),
            PreviewItem("25", "fbox vs boxed", "\\fbox{a} \\quad \\boxed{a}"),
            PreviewItem("26", "Underbrace annotation", "\\underbrace{x+y+z}_{n}"),
            PreviewItem("27", "Overbrace annotation", "\\overbrace{a+b+c}^{3\\text{ terms}}"),
            PreviewItem("28", "Right-to-left direction", "\\RLE{مرحبا بالعالم}"),
            PreviewItem("29", "Left-to-right content inside RTL", "\\RLE{مرحبا \\LRE{Hello} عالم}"),
            PreviewItem("30", "Right-to-left environment", "\\begin{RTL}مرحبا بالعالم\\end{RTL}"),
            PreviewItem("31", "Math in right-to-left text", "\\RLE{x^2 + y^2 = r^2}"),
            PreviewItem("32", "Vertical offsets and rules", "\\raisebox{3pt}{x} \\quad \\rule{2em}{.1em}"),
            PreviewItem("33", "Aligned boxes and equation offsets", "\\Aboxed{x=1} \\quad \\MoveEqLeft[2] y=2"),
        )
    ),
    PreviewGroup(
        id = "enclose_menclose",
        title = "12. Enclose / menclose",
        description = "Circles, frames, combined borders, strike-throughs, and foreground/background colors",
        items = listOf(
            PreviewItem("1", "Circle enclosure", "\\enclose{circle}{x}"),
            PreviewItem("2", "Box enclosure", "\\enclose{box}{x+y}"),
            PreviewItem("3", "roundedbox + mathcolor", "\\enclose{roundedbox}[mathcolor=\"red\"]{\\frac{a}{b}}"),
            PreviewItem("4", "circle + box + background", "\\enclose{circle,box}[mathbackground=\"yellow\"]{x}"),
            PreviewItem("5", "Crossed diagonal strikes", "\\enclose{updiagonalstrike downdiagonalstrike}{x}"),
            PreviewItem("6", "Combined borders", "\\enclose{left,right,top,bottom}{a+b=c}"),
            PreviewItem("7", "Horizontal and vertical strikes", "\\enclose{horizontalstrike verticalstrike}{T}"),
        )
    ),
    PreviewGroup(
        id = "labels_refs",
        title = "13. Labels, references, and numbering",
        description = "Manual tags, cross-references, and automatic equation numbering",
        items = listOf(
            PreviewItem("1", "Equation tag", "E = mc^2 \\tag{1}"),
            PreviewItem("2", "Unparenthesized equation tag", "F = ma \\tag*{Newton}"),
            PreviewItem("3", "label+eqref", "\\label{eq:1} E = mc^2 \\eqref{eq:1}"),
            PreviewItem("4", "Label references", "See \\ref{eq:1}"),
            PreviewItem("5", "Automatic equation numbering", "\\begin{equation} E = mc^2 \\label{eq:einstein} \\end{equation}"),
            PreviewItem("6", "Multiple numbered equations", "\\begin{equation} a^2 + b^2 = c^2 \\label{eq:pyth} \\end{equation} \\begin{equation} e^{i\\pi} + 1 = 0 \\label{eq:euler} \\end{equation}"),
            PreviewItem("7", "Numbered equations and references", "\\begin{equation} F = ma \\label{eq:f} \\end{equation} See \\eqref{eq:f}"),
            PreviewItem("8", "Unnumbered equation", "\\begin{equation*} x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a} \\end{equation*}"),
            PreviewItem("9", "Automatic numbering with manual tags", "\\begin{equation} a = b \\label{eq:a} \\end{equation} \\begin{equation} c = d \\tag{★} \\end{equation} \\begin{equation} e = f \\label{eq:e} \\end{equation} See \\eqref{eq:a} and \\eqref{eq:e}"),
        )
    ),
    PreviewGroup(
        id = "advanced_annotations",
        title = "14. Advanced annotations",
        description = "Corner scripts, tensors, prescripts, and zero-width overlaps",
        items = listOf(
            PreviewItem("1", "Scripts at all four corners", "\\sideset{_a^b}{_c^d}{\\sum}"),
            PreviewItem("2", "Partial corner scripts", "\\sideset{_1}{^n}{\\prod}"),
            PreviewItem("3", "Tensor notation", "\\tensor{T}{^a_b}"),
            PreviewItem("4", "Tensor with multiple indices", "\\tensor{R}{^\\mu_{\\nu\\rho\\sigma}}"),
            PreviewItem("5", "Prescripts", "\\prescript{A}{Z}{X}"),
            PreviewItem("6", "Isotope prescripts", "\\prescript{235}{92}{U}"),
            PreviewItem("7", "Partial prescripts", "\\prescript{14}{}{C}"),
            PreviewItem("8", "Centered zero-width overlap", "\\sum_{\\mathclap{1 \\le i \\le n}} x_i"),
            PreviewItem("9", "Right zero-width overlap", "\\mathrlap{\\overbrace{\\phantom{abc}}}abc"),
            PreviewItem("10", "Left zero-width overlap", "abc\\mathllap{\\underbrace{\\phantom{abc}}}"),
        )
    ),
    PreviewGroup(
        id = "hyperlinks_api",
        title = "15. Links, highlights, and diagnostics",
        description = "Clickable links, highlighted subexpressions, diagnostics, comments, and nonbreaking spaces",
        items = listOf(
            PreviewItem("1", "Hyperlink", "\\href{https://example.com}{Click here}"),
            PreviewItem("2", "URL", "\\url{https://example.com}"),
            PreviewItem("3", "Linked math", "\\href{https://wiki.org}{E = mc^2}"),
            PreviewItem(
                "4", "Hyperlink with click callback", "\\href{https://example.com}{Click here}",
                content = {
                    Latex(
                        latex = "\\href{https://example.com}{Click here}",
                        config = LatexConfig(
                            theme = LatexTheme.light(),
                            onHyperlinkClick = { url ->
                                println("Link clicked: $url")
                            }
                        )
                    )
                }
            ),
            PreviewItem("5", "Nonbreaking spaces", "Fig.~1 Eq.~2"),
            PreviewItem("6", "Comments", "x^2 + y^2 % comment\n= z^2"),
            PreviewItem(
                "7", "Highlight by pattern", "E = mc^2",
                content = {
                    Latex(
                        latex = "E = mc^2",
                        config = LatexConfig(
                            theme = LatexTheme.light(),
                            highlight = HighlightConfig(
                                ranges = listOf(
                                    HighlightRange(
                                        pattern = "mc",
                                        color = Color(0x4400AAFF),
                                        borderColor = Color(0xFF0088FF)
                                    )
                                )
                            )
                        )
                    )
                }
            ),
            PreviewItem(
                "8", "Highlight by index", "\\frac{a+b}{c} + x^2",
                content = {
                    Latex(
                        latex = "\\frac{a+b}{c} + x^2",
                        config = LatexConfig(
                            theme = LatexTheme.light(),
                            highlight = HighlightConfig(
                                ranges = listOf(
                                    HighlightRange(
                                        nodeIndices = 0..0,
                                        color = Color(0x33FF6600),
                                        borderColor = Color(0xFFFF6600)
                                    )
                                )
                            )
                        )
                    )
                }
            ),
            PreviewItem(
                "9", "Highlight multiple ranges", "a + b + c + d",
                content = {
                    Latex(
                        latex = "a + b + c + d",
                        config = LatexConfig(
                            theme = LatexTheme.light(),
                            highlight = HighlightConfig(
                                ranges = listOf(
                                    HighlightRange(
                                        pattern = "a",
                                        color = Color(0x44FF0000)
                                    ),
                                    HighlightRange(
                                        pattern = "c",
                                        color = Color(0x4400FF00)
                                    )
                                )
                            )
                        )
                    )
                }
            ),
            PreviewItem("10", "Unknown-command diagnostic", "x + \\unknowncmd + y"),
            PreviewItem("11", "Diagnostics in a compound expression", "\\frac{a}{b} + \\notacommand + \\sqrt{c}"),
        )
    ),
    PreviewGroup(
        id = "custom_commands",
        title = "16. Custom commands",
        description = "Define and expand reusable LaTeX commands",
        items = listOf(
            PreviewItem("1", "Command without arguments", "\\newcommand{\\R}{\\mathbb{R}} x \\in \\R"),
            PreviewItem("2", "Command with one argument", "\\newcommand{\\diff}[1]{\\frac{d}{d#1}} \\diff{x}"),
            PreviewItem(
                "3",
                "Command with two arguments",
                "\\newcommand{\\pdiff}[2]{\\frac{\\partial #1}{\\partial #2}} \\pdiff{f}{x}"
            ),
            PreviewItem(
                "4",
                "Multiple custom commands",
                "\\newcommand{\\N}{\\mathbb{N}} \\newcommand{\\Z}{\\mathbb{Z}} \\N \\subset \\Z"
            ),
            PreviewItem("5", "Nested commands", "\\newcommand{\\abs}[1]{\\left|#1\\right|} \\abs{x}"),
            PreviewItem("6", "Arguments within text", "\\newcommand{\\test}[1]{a#1b} \\test{x}"),
            PreviewItem(
                "7",
                "Compound command definition",
                "\\newcommand{\\myvec}[1]{\\boldsymbol{#1}} \\myvec{v} = \\myvec{u} + \\myvec{w}"
            ),
            PreviewItem(
                "8",
                "Custom math symbols",
                "\\newcommand{\\R}{\\mathbb{R}} \\newcommand{\\C}{\\mathbb{C}} \\R + \\C"
            ),
            PreviewItem(
                "9",
                "Combining commands",
                "\\newcommand{\\norm}[1]{\\left\\|#1\\right\\|} \\norm{x} = \\norm{\\vec{v}}"
            ),
            PreviewItem(
                "10",
                "Recursive definition",
                "\\newcommand{\\fact}[1]{#1!} \\fact{n} = \\frac{\\fact{2n}}{(2n)!!}"
            ),
            PreviewItem(
                "11",
                "Declare a math operator",
                "\\DeclareMathOperator{\\Tr}{Tr} \\Tr(A)"
            ),
            PreviewItem(
                "12",
                "Declare multiple math operators",
                "\\DeclareMathOperator{\\rank}{rank} \\DeclareMathOperator{\\sgn}{sgn} \\rank(A) + \\sgn(x)"
            ),
            PreviewItem(
                "13",
                "Custom operator expression",
                "\\mathop{Res}_{z=0} f(z)"
            ),
            PreviewItem(
                "14",
                "mathop + limits",
                "\\mathop{op}\\limits_{i=0}^{n} x_i"
            ),
            PreviewItem(
                "15",
                "Optional argument with a default",
                "\\newcommand{\\greet}[1][World]{Hello\\ #1} \\greet \\quad \\greet[LaTeX]"
            ),
            PreviewItem(
                "16",
                "Two arguments with a default",
                "\\newcommand{\\pair}[2][x]{#1+#2} \\pair{y} \\quad \\pair[a]{b}"
            ),
            PreviewItem(
                "17",
                "Custom environment",
                "\\newenvironment{mybox}{[}{]} \\begin{mybox}hello\\end{mybox}"
            ),
            PreviewItem(
                "18",
                "Custom environment with arguments",
                "\\newenvironment{titled}[1]{\\textbf{#1:}~}{} \\begin{titled}{Theorem}P \\implies Q\\end{titled}"
            ),
            PreviewItem(
                "19",
                "DeclarePairedDelimiter",
                "\\DeclarePairedDelimiter{\\set}{\\lbrace}{\\rbrace} \\set{x \\in A}"
            ),
            PreviewItem(
                "20",
                "Issue #46: explicit groups and style restoration",
                "a+\\begingroup\\bf x+{\\it y}+z\\endgroup+w"
            ),
            PreviewItem(
                "21",
                "Local macro redefinition and restoration",
                "\\def\\value{a}\\value+\\begingroup\\def\\value{b}\\value\\endgroup+\\value"
            ),
            PreviewItem(
                "22",
                "Explicit groups within macros",
                "\\newcommand{\\bold}[1]{\\begingroup\\bf #1\\endgroup}\\bold{x+y}+z"
            ),
            PreviewItem("23", "Deferred expansion and meaning snapshots", "\\def\\a{\\b}\\def\\b{x}\\let\\saved\\b\\def\\b{y}\\a+\\saved"),
            PreviewItem("24", "Expansion control and conditional branches", "\\def\\name{result}\\expandafter\\def\\csname\\name\\endcsname{42}\\ifdefined\\result\\result\\else 0\\fi"),
            PreviewItem("25", "Macros with delimited arguments", "\\def\\pair#1,#2;{\\frac{#1}{#2}}\\pair a+b,c+d;"),
            PreviewItem("26", "Groups across environment boundaries", "\\newenvironment{boldgroup}{\\begingroup\\bf}{\\endgroup}\\begin{boldgroup}x+y\\end{boldgroup}+z"),
            PreviewItem("27", "Branches by math style", "\\def\\pick{\\mathchoice{D}{T}{S}{Q}}\\pick+x^{\\pick^{\\pick}}"),
            PreviewItem("28", "Named colors and declaration scopes", "\\definecolor{accent}{HTML}{3A7BD5}{\\color{accent}a+b}+\\textcolor[RGB]{220,50,70}{c}"),
            PreviewItem("29", "Structured and verbatim text", "\\text{normal \\textbf{bold} and \$x^2\$}+\\verb|a%{b}|"),
            PreviewItem("30", "Array spacing and dashed lines", "\\def\\arraystretch{1.5}\\setlength{\\arraycolsep}{8pt}\\begin{array}{cc}a&b\\\\\\hdashline c&d\\end{array}"),
            PreviewItem(
                "31",
                "Small caps and group scope",
                "\\textrm{Abc123}\\quad\\textsc{Abc123}\\quad{\\sc Abc}\\quad{\\scshape Abc}\\quad\\text{outside}"
            ),
            PreviewItem(
                "32",
                "Restore ordinary case within small caps",
                "{\\scshape Abc \\textulc{Def} Ghi}\\quad\\text{outside}"
            ),
            PreviewItem(
                "33",
                "Combine font declarations and reset shape",
                "\\textnormal{Abc}\\quad{\\sffamily\\bfseries\\scshape Abc}\\quad{\\sffamily\\bfseries\\scshape\\normalshape Abc}"
            ),
            PreviewItem(
                "34",
                "Upright shape and roman family inside small caps",
                "\\textsf{\\textsc{Abc \\textup{Def} Ghi \\textrm{Jkl}}}"
            ),
            PreviewItem(
                "35",
                "Two upright switches from italic small caps",
                "{\\sffamily\\itshape\\scshape Abc \\upshape Def \\upshape Ghi}"
            ),
            PreviewItem(
                "36",
                "Nested emphasis in a sans-serif family",
                "\\textsf{\\emph{Abc \\emph{Def} Ghi}}\\quad{\\sffamily\\em Jkl}"
            ),
            PreviewItem(
                "37",
                "Legacy font switches inside small caps",
                "\\textsf{\\textsc{Abc {\\bf Def} {\\sl Ghi} Jkl}}"
            ),
            PreviewItem(
                "38",
                "Font fallbacks for swash and spaced small caps",
                "\\textsw{Abc}\\quad{\\swshape Def}\\quad\\textssc{Ghi}\\quad{\\sscshape Jkl}"
            ),
        )
    ),
    PreviewGroup(
        id = "spaces",
        title = "17. Spacing",
        description = "Negative spaces, custom lengths, and horizontal spacing",
        items = listOf(
            PreviewItem("1", "Standard spacing comparison", "a \\, b \\: c \\; d \\quad e \\qquad f"),
            PreviewItem("2", "Negative spacing", "a \\! b (tight)"),
            PreviewItem("3", "Custom spacing in centimeters", "a \\hspace{1cm} b"),
            PreviewItem("4", "Custom spacing in points", "a \\hspace{20pt} b"),
            PreviewItem("5", "Custom spacing in em", "a \\hspace{2em} b"),
            PreviewItem("6", "Custom negative spacing", "a \\hspace{-0.5em} b"),
            PreviewItem("7", "Named spaces", "a \\space b \\thinspace c \\medspace d \\thickspace e"),
            PreviewItem("8", "Additional space commands", "a \\enspace b \\enskip c \\negmedspace d \\negthickspace e"),
            PreviewItem("9", "Escaped special characters", "\\{ \\} \\$ \\% \\# \\& \\_"),
            PreviewItem("10", "Double vertical bar command", "\\| \\quad \\left\\| x \\right\\| \\quad \\big\\|"),
            PreviewItem("11", "Text and math kerning", "a\\kern1em b \\quad x\\mkern6mu y"),
        )
    ),
    PreviewGroup(
        id = "mathstyle",
        title = "18. Math styles and metrics",
        description = "Display, text, script, and cramped styles with script positioning",
        items = listOf(
            PreviewItem(
                "1",
                "Display-style fraction",
                "\\frac{a}{b} \\quad \\displaystyle{\\frac{a}{b}}"
            ),
            PreviewItem(
                "2",
                "Fraction style comparison",
                "\\textstyle{\\frac{1}{1+x}} \\quad \\dfrac{1}{1+x} \\quad \\tfrac{1}{1+x}"
            ),
            PreviewItem(
                "3",
                "frac",
                "\\frac{1}{1+\\frac{1}{1+\\frac{1}{1+\\frac{1}{1+\\frac{1}{\\cdots}}}}}"
            ),
            PreviewItem(
                "4",
                "Display-style continued fraction",
                "\\dfrac{1}{1+\\dfrac{1}{1+\\dfrac{1}{1+\\dfrac{1}{1+\\dfrac{1}{\\cdots}}}}}"
            ),
            PreviewItem(
                "5",
                "Display-style summation",
                "\\sum_{i=1}^{n} \\quad \\displaystyle{\\sum_{i=1}^{n}}"
            ),
            PreviewItem(
                "6",
                "Script-style summation",
                "x\\sum_{i=1}^{n} \\quad x\\scriptstyle{\\sum_{i=1}^{n}}"
            ),
            PreviewItem(
                "7",
                "Summation in a superscript",
                "x^{\\sum_{i=1}^{n}} \\quad x^{\\scriptstyle{\\sum_{i=1}^{n}}}"
            ),
            PreviewItem("8", "scriptscriptstyle", "\\scriptscriptstyle{x + y + z}"),
            PreviewItem(
                "9",
                "Math styles within fractions",
                "\\frac{\\displaystyle{\\sum_{i=1}^{n}}}{\\textstyle{n}}"
            ),
            PreviewItem("10", "Nested math styles", "\\displaystyle{\\frac{\\sum}{n}}"),
            PreviewItem("11", "Bold math symbols", "\\symbf{x} + \\symbf{\\alpha}"),
            PreviewItem("12", "Sans-serif math symbols", "\\symsf{ABC}"),
            PreviewItem("13", "Upright math symbols", "\\symrm{dx}"),
            PreviewItem("14", "Default KaTeX font selection", "xyzABC012 \\quad \\alpha\\beta\\Gamma"),
            PreviewItem("15", "KaTeX font families", "\\mathrm{Ab0} \\quad \\mathbf{Ab0} \\quad \\boldsymbol{x\\alpha}"),
            PreviewItem("16", "KaTeX decorative fonts", "\\mathbb{R} \\quad \\mathcal{L} \\quad \\mathfrak{g} \\quad \\mathscr{F}"),
            PreviewItem("17", "Baselines and scripts", "x_i^2 + f_j^{n+1} = A_k^m"),
            PreviewItem("18", "TeX Rule 15: fraction alignment", "a + \\frac{x^2}{1+y} + b \\quad \\dfrac{x^2}{1+y}"),
            PreviewItem("19", "Delimiters and the math axis", "a + \\left(\\dfrac{x_i^2}{1+y}\\right) + b"),
            PreviewItem("20", "Integral limits", "\\int_{-\\infty}^{\\infty} f(t)e^{-i\\omega t}\\,dt"),
            PreviewItem("21", "Vector accent positioning", "\\hbar\\frac{\\partial}{\\partial t}\\Psi(\\vec r,t)=V(\\vec r,t)\\Psi(\\vec r,t)"),
            PreviewItem("22", "Accent ink bounds", "\\hat{x} \\quad \\tilde{y} \\quad \\bar{z} \\quad \\dot{x}"),
            PreviewItem("23", "Normal and cramped superscripts", "x^2 \\quad \\cramped{x^2}"),
            PreviewItem("24", "Numerator and denominator styles", "\\frac{x^2}{x^2}"),
            PreviewItem("25", "Radicals and accents", "x^2 \\quad \\sqrt{x^2} \\quad \\hat{x^2} \\quad \\overline{x^2}"),
            PreviewItem("26", "Superscript and subscript expressions", "x_{a^2}^{a^2} \\quad \\sum_{a^2}^{a^2}"),
            PreviewItem("27", "Explicit cramped math styles", "\\crampeddisplaystyle x^2 \\quad \\crampedtextstyle x^2 \\quad \\crampedscriptstyle x^2 \\quad \\crampedscriptscriptstyle x^2"),
            PreviewItem("28", "Optional mathtools styles", "\\cramped{x^2} \\quad \\cramped[\\scriptstyle]{x^2}"),
        )
    ),
    PreviewGroup(
        id = "environments",
        title = "19. Math environments",
        description = "Multiline equations, aligned expressions, and piecewise functions",
        items = listOf(
            PreviewItem("1", "Split equation", "\\begin{split} x &= a + b \\\\ &= c \\end{split}"),
            PreviewItem(
                "2",
                "Multiline split equation",
                "\\begin{split} a &= b + c \\\\ &= d + e \\\\ &= f \\end{split}"
            ),
            PreviewItem(
                "3",
                "Multline environment",
                "\\begin{multline} a + b + c \\\\ + d + e \\end{multline}"
            ),
            PreviewItem(
                "4",
                "Three-line equation",
                "\\begin{multline} \\text{Left} \\\\ \\text{Center} \\\\ \\text{Right} \\end{multline}"
            ),
            PreviewItem(
                "5",
                "Equation array",
                "\\begin{eqnarray} x &=& 1 \\\\ y &=& 2 \\end{eqnarray}"
            ),
            PreviewItem(
                "6",
                "Three-column equation array",
                "\\begin{eqnarray} a + b &=& c \\\\ d - e &=& f \\end{eqnarray}"
            ),
            PreviewItem("7", "subequations", "\\begin{subequations} a = b \\end{subequations}"),
            PreviewItem("8", "Combined environments", "\\begin{align} x &= 1 \\\\ y &= 2 \\end{align}"),
            PreviewItem(
                "9",
                "Piecewise function",
                "f(x) = \\begin{cases} x^2 & \\text{if } x > 0 \\\\ 0 & \\text{if } x = 0 \\\\ -x^2 & \\text{if } x < 0 \\end{cases}"
            ),
            PreviewItem(
                "10",
                "Simple cases",
                "y = \\begin{cases} 1 & x > 0 \\\\ 0 & x = 0 \\\\ -1 & x < 0 \\end{cases}"
            ),
            PreviewItem(
                "11",
                "Cases with nested fractions",
                "|x| = \\begin{cases} \\frac{x}{1} & x \\geq 0 \\\\ \\frac{-x}{1} & x < 0 \\end{cases}"
            ),
            PreviewItem(
                "11a",
                "dcases (displaystyle)",
                "f(x) = \\begin{dcases} \\frac{1}{2} & x > 0 \\\\ \\frac{-1}{2} & x < 0 \\end{dcases}"
            ),
            PreviewItem(
                "11b",
                "Cases with a right brace",
                "\\begin{rcases} x^2 & x > 0 \\\\ -x^2 & x < 0 \\end{rcases} = |x| \\cdot x"
            ),
            PreviewItem(
                "11c",
                "Piecewise function with one brace",
                "f(x)=\\left\\{\\begin{array}{l}-x^2-2ax-a, x<0\\\\\\epsilon^x+\\ln(x+1), x\\ge 0\\end{array}\\right."
            ),
            PreviewItem(
                "12",
                "Basic table",
                "\\begin{tabular}{cc} a & b \\\\ c & d \\end{tabular}"
            ),
            PreviewItem(
                "13",
                "Three-column table",
                "\\begin{tabular}{lcr} left & center & right \\\\ 1 & 2 & 3 \\end{tabular}"
            ),
            PreviewItem(
                "14",
                "Unnumbered aligned equations",
                "\\begin{align*} a &= b + c \\\\ d &= e + f \\end{align*}"
            ),
            PreviewItem(
                "15",
                "Unnumbered gathered equations",
                "\\begin{gather*} x + y = z \\\\ a^2 + b^2 = c^2 \\end{gather*}"
            ),
            PreviewItem(
                "16",
                "Unnumbered multiline equation",
                "\\begin{multline*} a + b + c \\\\ + d + e + f \\end{multline*}"
            ),
            PreviewItem(
                "17",
                "Unnumbered equation array",
                "\\begin{eqnarray*} x &=& 1 \\\\ y &=& 2 \\end{eqnarray*}"
            ),
            PreviewItem("18", "Unnumbered equation", "\\begin{equation*} E = mc^2 \\end{equation*}"),
            PreviewItem(
                "19",
                "Table with vertical rules",
                "\\begin{tabular}{|c|c|c|} a & b & c \\\\ d & e & f \\end{tabular}"
            ),
            PreviewItem(
                "20",
                "tabular hline",
                "\\begin{tabular}{|c|c|c|} \\hline a & b & c \\\\ \\hline d & e & f \\\\ \\hline \\end{tabular}"
            ),
            PreviewItem(
                "21",
                "tabular cline",
                "\\begin{tabular}{ccc} a & b & c \\\\ \\cline{1-2} d & e & f \\end{tabular}"
            ),
            PreviewItem(
                "22",
                "multicolumn",
                "\\begin{tabular}{|c|c|c|} \\hline \\multicolumn{3}{|c|}{Title} \\\\ \\hline a & b & c \\\\ \\hline \\end{tabular}"
            ),
            PreviewItem(
                "23",
                "Complete table",
                "\\begin{tabular}{|l|c|r|} \\hline \\multicolumn{3}{|c|}{Student Scores} \\\\ \\hline Name & Subject & Score \\\\ \\hline Alice & Math & 95 \\\\ Bob & English & 88 \\\\ \\hline \\end{tabular}"
            ),
            PreviewItem(
                "24",
                "Full-width aligned equations",
                "\\begin{flalign*} a &= b + c \\\\ d &= e + f \\end{flalign*}"
            ),
            PreviewItem(
                "25",
                "Aligned equation pairs",
                "\\begin{alignat}{2} a &= b & \\quad c &= d \\\\ e &= f & \\quad g &= h \\end{alignat}"
            ),
            PreviewItem("26", "Matrix column alignment", "\\begin{pmatrix*}[r] a&b \\\\ c&d \\end{pmatrix*}"),
        )
    ),
    PreviewGroup(
        id = "ams_relations",
        title = "20. AMS symbols",
        description = "AMS symbols",
        items = listOf(
            PreviewItem("1", "Negated inequalities", "a \\nleq b \\quad c \\ngeq d"),
            PreviewItem("2", "Negated set relations", "A \\nsubseteq B \\quad C \\nsupseteq D"),
            PreviewItem("3", "Negated order relations", "a \\nprec b \\quad c \\nsucc d"),
            PreviewItem("4", "Negated similarity and congruence", "a \\ncong b \\quad c \\nsim d"),
            PreviewItem("5", "Negated divisibility and parallelism", "a \\nmid b \\quad c \\nparallel d"),
            PreviewItem(
                "6",
                "Negated implication",
                "\\nvdash \\quad \\nvDash \\quad \\nVdash \\quad \\nVDash"
            ),
            PreviewItem(
                "7",
                "Negated triangle relations",
                "\\ntriangleleft \\quad \\ntriangleright \\quad \\ntrianglelefteq \\quad \\ntrianglerighteq"
            ),
            PreviewItem("8", "Negated comparisons", "a \\nless b \\quad c \\ngtr d"),
            PreviewItem("9", "Additional AMS relations", "a \\leqslant b \\quad c \\geqslant d"),
            PreviewItem("10", "Logical implication", "\\vDash \\quad \\Vdash \\quad \\Vvdash \\quad \\models"),
            PreviewItem(
                "11",
                "Combined example",
                "\\forall x \\in A, \\; x \\nleq 0 \\implies x \\nsubseteq B"
            ),
            PreviewItem(
                "12",
                "Miscellaneous symbols",
                "\\checkmark \\quad \\complement \\quad \\eth \\quad \\mho"
            ),
            PreviewItem("13", "Two-headed arrows", "\\twoheadrightarrow \\quad \\twoheadleftarrow"),
            PreviewItem(
                "14",
                "Double arrows",
                "\\leftleftarrows \\quad \\rightrightarrows \\quad \\leftrightarrows \\quad \\rightleftarrows"
            ),
            PreviewItem(
                "15",
                "Curved arrows",
                "\\curvearrowright \\quad \\curvearrowleft \\quad \\circlearrowright \\quad \\circlearrowleft"
            ),
            PreviewItem("16", "Special relations", "\\lessdot \\quad \\gtrdot \\quad \\lll \\quad \\ggg"),
            PreviewItem(
                "17",
                "Geometric symbols",
                "\\blacksquare \\quad \\square \\quad \\lozenge \\quad \\blacktriangle \\quad \\blacktriangledown"
            ),
            PreviewItem("18", "Hebrew letters", "\\aleph \\quad \\beth \\quad \\gimel \\quad \\daleth"),
            PreviewItem("19", "Angle symbols", "\\angle \\quad \\measuredangle \\quad \\sphericalangle"),
            PreviewItem("20", "Proportionality symbols", "a \\propto b \\quad c \\varpropto d"),
            PreviewItem("21", "AMS Greek variants", "\\digamma \\quad \\varkappa"),
            PreviewItem(
                "21a",
                "Uppercase Greek aliases",
                "\\Alpha \\quad \\Beta \\quad \\Epsilon \\quad \\Zeta \\quad \\Eta \\quad \\Iota \\quad \\Kappa \\quad \\Mu \\quad \\Nu \\quad \\Omicron \\quad \\Rho \\quad \\Tau \\quad \\Chi"
            ),
            PreviewItem(
                "21b",
                "Uppercase Greek variants",
                "\\varGamma \\quad \\varDelta \\quad \\varTheta \\quad \\varLambda \\quad \\varXi \\quad \\varPi \\quad \\varSigma \\quad \\varUpsilon \\quad \\varPhi \\quad \\varPsi \\quad \\varOmega"
            ),
            PreviewItem(
                "22",
                "AMS binary operators (1)",
                "\\dotplus \\quad \\smallsetminus \\quad \\barwedge \\quad \\veebar \\quad \\doublebarwedge"
            ),
            PreviewItem(
                "23",
                "AMS binary operators (2)",
                "\\boxminus \\quad \\boxplus \\quad \\boxtimes \\quad \\boxdot"
            ),
            PreviewItem(
                "24",
                "AMS binary operators (3)",
                "\\leftthreetimes \\quad \\rightthreetimes \\quad \\curlywedge \\quad \\curlyvee"
            ),
            PreviewItem(
                "25",
                "AMS binary operators (4)",
                "\\circleddash \\quad \\circledast \\quad \\circledcirc \\quad \\centerdot \\quad \\intercal"
            ),
            PreviewItem(
                "26",
                "AMS binary operators (5)",
                "\\divideontimes \\quad \\rtimes \\quad \\ltimes"
            ),
            PreviewItem(
                "27",
                "AMS slanted and approximate relations",
                "\\eqslantless \\quad \\eqslantgtr \\quad \\lessapprox \\quad \\gtrapprox"
            ),
            PreviewItem(
                "28",
                "AMS precedence relations",
                "\\precsim \\quad \\succsim \\quad \\precapprox \\quad \\succapprox"
            ),
            PreviewItem(
                "29",
                "AMS negated subset variants",
                "\\varsubsetneq \\quad \\varsupsetneq \\quad \\subsetneqq \\quad \\supsetneqq"
            ),
            PreviewItem(
                "30",
                "AMS negated subsets",
                "\\nsubset \\quad \\nsupset \\quad \\nsubseteqq \\quad \\nsupseteqq"
            ),
            PreviewItem(
                "31",
                "AMS negated arrows",
                "\\nleftarrow \\quad \\nrightarrow \\quad \\nLeftarrow \\quad \\nRightarrow \\quad \\nLeftrightarrow \\quad \\nleftrightarrow"
            ),
            PreviewItem(
                "32",
                "Additional AMS arrows",
                "\\Rrightarrow \\quad \\Lleftarrow \\quad \\twoheadrightarrowtail"
            ),
            PreviewItem(
                "33",
                "Equilibrium arrows",
                "\\rightleftharpoons \\quad \\leftrightharpoons"
            ),
            PreviewItem(
                "34",
                "Additional large operators",
                "\\bigtriangleup \\quad \\bigtriangledown \\quad \\iiiint \\quad \\oiint \\quad \\oiiint"
            ),
            PreviewItem(
                "35",
                "Additional delimiters",
                "\\left\\langle x \\right\\rangle \\quad \\left\\lgroup a \\right\\rgroup \\quad \\left\\lmoustache b \\right\\rmoustache"
            ),
            PreviewItem(
                "36",
                "Additional AMS symbols",
                "\\blacklozenge \\quad \\Bbbk"
            ),
            PreviewItem("37", "Mathtools definition relations", "f \\coloneqq x^2 \\quad y \\eqqcolon g(x)"),
        )
    ),
    PreviewGroup(
        id = "math_mode",
        title = "21. Inline and display math",
        description = "Inline math with dollar delimiters and display math with double dollars or brackets",
        items = listOf(
            PreviewItem("1", "Inline math", "The formula \$E=mc^2\$ is famous"),
            PreviewItem("2", "Display math", "\$\$\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}\$\$"),
            PreviewItem(
                "3",
                "Display math in brackets",
                "\\[\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}\\]"
            ),
            PreviewItem(
                "4",
                "Mixed text and math",
                "Given \$a > 0\$ and \$b > 0\$, we have \$\$a + b \\geq 2\\sqrt{ab}\$\$"
            ),
            PreviewItem(
                "5",
                "Multiple inline formulas",
                "Let \$x \\in \\mathbb{R}\$, then \$x^2 \\geq 0\$ for all \$x\$"
            ),
            PreviewItem("6", "Escaped dollar signs", "Price is \\\$10 and \$x = 5\$"),
        )
    ),
    PreviewGroup(
        id = "animated",
        title = "22. Animated transitions",
        description = "Crossfade and slide animations between formulas",
        items = listOf(
            PreviewItem(
                "1", "Crossfade", "E = mc^2 ↔ a^2+b^2=c^2",
                content = {
                    val formulas =
                        listOf("E = mc^2", "a^2 + b^2 = c^2", "\\frac{d}{dx}x^n = nx^{n-1}")
                    var index by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2000)
                            index = (index + 1) % formulas.size
                        }
                    }
                    AnimatedLatex(
                        latex = formulas[index],
                        config = LatexConfig(theme = LatexTheme.light()),
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.CROSSFADE,
                            durationMillis = 500
                        )
                    )
                }
            ),
            PreviewItem(
                "2", "Slide up", "\\sum ↔ \\int",
                content = {
                    val formulas =
                        listOf("\\sum_{i=1}^{n} i^2", "\\int_0^1 x^2 dx", "\\prod_{k=1}^{n} k")
                    var index by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2000)
                            index = (index + 1) % formulas.size
                        }
                    }
                    AnimatedLatex(
                        latex = formulas[index],
                        config = LatexConfig(theme = LatexTheme.light()),
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.SLIDE_UP,
                            durationMillis = 400
                        )
                    )
                }
            ),
            PreviewItem(
                "3", "Slide down", "\\frac{a}{b} ↔ \\sqrt{x}",
                content = {
                    val formulas = listOf("\\frac{a+b}{c+d}", "\\sqrt{x^2 + y^2}", "x^{n+1}")
                    var index by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2000)
                            index = (index + 1) % formulas.size
                        }
                    }
                    AnimatedLatex(
                        latex = formulas[index],
                        config = LatexConfig(theme = LatexTheme.light()),
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.SLIDE_DOWN,
                            durationMillis = 400
                        )
                    )
                }
            ),
            PreviewItem(
                "4", "Fade and slide", "Cycle through formulas",
                content = {
                    val formulas = listOf(
                        "e^{i\\pi} + 1 = 0",
                        "\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1",
                        "\\zeta(s) = \\sum_{n=1}^{\\infty} \\frac{1}{n^s}"
                    )
                    var index by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2500)
                            index = (index + 1) % formulas.size
                        }
                    }
                    AnimatedLatex(
                        latex = formulas[index],
                        config = LatexConfig(theme = LatexTheme.light()),
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.FADE_SLIDE,
                            durationMillis = 600
                        )
                    )
                }
            ),
            PreviewItem(
                "5", "Compare four transitions", "All transition styles",
                content = {
                    val formulas = listOf("x^2", "\\frac{a}{b}", "\\sqrt{c}")
                    var index by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(2000)
                            index = (index + 1) % formulas.size
                        }
                    }
                    Column {
                        LatexTransition.entries.forEach { transition ->
                            AnimatedLatex(
                                latex = formulas[index],
                                config = LatexConfig(theme = LatexTheme.light()),
                                animationConfig = LatexAnimationConfig(
                                    transition = transition,
                                    durationMillis = 400
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            ),
        )
    ),
    PreviewGroup(
        id = "themes",
        title = "24. Themes and fonts",
        description = "Automatic, light, dark, and Material3 themes with custom font settings",
        items = listOf(
            PreviewItem(
                "1", "Follow the system theme", "E = mc^2",
                content = {
                    Latex(
                        latex = "E = mc^2",
                        config = LatexConfig(theme = LatexTheme.auto())
                    )
                }
            ),
            PreviewItem(
                "2", "Light theme", "\\frac{a+b}{c+d}",
                content = {
                    Latex(
                        latex = "\\frac{a+b}{c+d}",
                        config = LatexConfig(theme = LatexTheme.light())
                    )
                }
            ),
            PreviewItem(
                "3", "Dark theme", "\\int_0^1 x^2 dx = \\frac{1}{3}",
                content = {
                    Latex(
                        latex = "\\int_0^1 x^2 dx = \\frac{1}{3}",
                        config = LatexConfig(theme = LatexTheme.dark())
                    )
                }
            ),
            PreviewItem(
                "4", "Current Material3 theme", "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}",
                content = {
                    Latex(
                        latex = "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}",
                        config = LatexConfig(theme = LatexTheme.material3())
                    )
                }
            ),
            PreviewItem(
                "5", "System fonts", "E = mc^2",
                content = {
                    Latex(
                        latex = "E = mc^2",
                        config = LatexConfig(fontFamilies = systemLatexFontFamilies())
                    )
                }
            ),
            PreviewItem(
                "6", "Monospace fonts", "\\text{WWWWiiii} + x^2",
                content = {
                    val fonts = systemLatexFontFamilies().copy(
                        main = FontFamily.Monospace,
                        math = FontFamily.Monospace
                    )
                    Latex(
                        latex = "\\text{WWWWiiii} + x^2",
                        config = LatexConfig(fontFamilies = fonts)
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "section_headings",
        title = "23. Section headings",
        description = "section, subsection, subsubsection, paragraph, subparagraph",
        items = listOf(
            PreviewItem("1", "section", "\\section{Introduction}"),
            PreviewItem("2", "subsection", "\\subsection{Background}"),
            PreviewItem("3", "subsubsection", "\\subsubsection{Details}"),
            PreviewItem("4", "paragraph", "\\paragraph{Note}"),
            PreviewItem("5", "subparagraph", "\\subparagraph{Remark}"),
            PreviewItem("6", "Unnumbered section headings", "\\section*{No Numbering}"),
            PreviewItem("7", "Mixed heading levels", "\\section{Title} \\subsection{Subtitle}"),
        )
    ),
    PreviewGroup(
        id = "package_subsets",
        title = "25. Physics and SI units",
        description = "Annotated equals, circled text, derivatives, Dirac notation, and SI units",
        items = listOf(
            PreviewItem("1", "Extensible equals", "A \\xlongequal[below]{condition} B"),
            PreviewItem("2", "Circled text and math struts", "\\circled{1} + a\\mathstrut + b"),
            PreviewItem("3", "Ordinary derivatives", "\\dv[2]{f}{x} + \\dv{x}"),
            PreviewItem("4", "Partial derivatives", "\\pdv{f}{x} + \\pdv[2]{f}{y}"),
            PreviewItem("5", "Dirac notation", "\\bra{\\psi} \\ket{\\phi} \\braket{\\psi|\\phi}"),
            PreviewItem("6", "Absolute values and norms", "\\abs{x} + \\norm{v}"),
            PreviewItem("7", "SI values and units", "\\SI{9.8}{m/s^2}"),
            PreviewItem("8", "Units and scientific notation", "\\si{kg.m/s^2} \\quad \\num{1.23e4}"),
            PreviewItem("9", "siunitx v3", "\\qty{1.5}{\\kilo\\metre\\per\\second} \\quad \\qtyrange{2}{4}{\\metre}"),
            PreviewItem("10", "Commutators and vectors", "\\comm{A}{B} + \\anticomm{A}{B} \\quad \\vb{v}, \\va{a}"),
        )
    ),
    PreviewGroup(
        id = "mathclass",
        title = "Math atom classes",
        description = "Control inter-atom spacing with mathord, mathbin, mathrel, and related commands",
        items = listOf(
            PreviewItem("1", "Treat a symbol as a binary operator", "a \\mathbin{R} b"),
            PreviewItem("2", "Treat a symbol as a relation", "a \\mathrel{R} b"),
            PreviewItem("3", "Slash as a binary operator", "a \\mathbin{/} b"),
            PreviewItem("4", "Default spacing vs. a binary operator", "a / b \\quad a \\mathbin{/} b"),
            PreviewItem("5", "Custom opening and closing delimiters", "\\mathopen{[} x \\mathclose{]}"),
            PreviewItem("6", "Punctuation spacing", "a \\mathpunct{;} b"),
            PreviewItem("7", "Inner atom spacing", "a + \\mathinner{x} + b"),
            PreviewItem("8", "Remove operator spacing with an ordinary atom", "a \\mathord{+} b"),
            PreviewItem("9", "Combined example", "f \\mathrel{:=} \\mathopen{\\langle} x, y \\mathclose{\\rangle}"),
        )
    ),
)

@Preview
@Composable
fun BasicLatexPreview(onBack: () -> Unit = {}) {
    PreviewCategoryScreen(
        title = "Math formulas",
        groups = basicLatexPreviewGroups,
        onBack = onBack
    )
}
