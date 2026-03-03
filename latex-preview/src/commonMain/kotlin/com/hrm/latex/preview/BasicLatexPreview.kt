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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.AnimatedLatex
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.LatexAnimationConfig
import com.hrm.latex.renderer.LatexTransition
import com.hrm.latex.renderer.model.HighlightConfig
import com.hrm.latex.renderer.model.HighlightRange
import com.hrm.latex.renderer.model.LatexConfig
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
        title = "1. 基础级别",
        description = "简单文本、上下标、分数",
        items = listOf(
            PreviewItem("1", "简单文本", "Hello LaTeX"),
            PreviewItem("2", "简单上标", "x^2"),
            PreviewItem("3", "简单下标", "a_i"),
            PreviewItem("4", "上标+下标", "x_i^2"),
            PreviewItem("5", "简单分数", "\\frac{1}{2}"),
        )
    ),
    PreviewGroup(
        id = "elementary",
        title = "2. 初级级别",
        description = "多项式、方程、简单求和积分",
        items = listOf(
            PreviewItem("1", "多项式", "ax^2 + bx + c = 0"),
            PreviewItem("2", "勾股定理", "a^2 + b^2 = c^2"),
            PreviewItem("3", "二次方程解", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"),
            PreviewItem("4", "简单求和", "\\sum_{i=1}^{n} i"),
            PreviewItem("5", "简单积分", "\\int_0^1 x dx"),
            PreviewItem("6", "dots (ldots)", "a_1, a_2, \\dots, a_n"),
            PreviewItem("7", "dots (cdots)", "a_1 + a_2 + \\dots + a_n"),
        )
    ),
    PreviewGroup(
        id = "intermediate",
        title = "3. 中级级别",
        description = "嵌套结构、根式、复杂运算",
        items = listOf(
            PreviewItem("1", "嵌套分数", "\\frac{1}{2 + \\frac{1}{3}}"),
            PreviewItem("2", "复杂分数", "\\frac{a + b}{c + d}"),
            PreviewItem("3", "平方根", "\\sqrt{x^2 + y^2}"),
            PreviewItem("4", "复杂求和", "\\sum_{i=1}^{n} i^2 = \\frac{n(n+1)(2n+1)}{6}"),
            PreviewItem("5", "定积分", "\\int_{0}^{\\infty} e^{-x} dx = 1"),
            PreviewItem("6", "连乘", "\\prod_{i=1}^{n} x_i"),
            PreviewItem("7", "coprod", "\\coprod_{i=1}^{n} A_i"),
            PreviewItem("8", "bigoplus", "\\bigoplus_{k=1}^{n} V_k"),
            PreviewItem("9", "bigotimes", "\\bigotimes_{i=1}^{m} W_i"),
            PreviewItem("10", "bigsqcup", "\\bigsqcup_{j \\in J} S_j"),
            PreviewItem("11", "bigodot", "\\bigodot_{i} R_i"),
            PreviewItem("12", "biguplus", "\\biguplus_{k} T_k"),
            PreviewItem("13", "极限", "\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1"),
            PreviewItem("14", "导数", "\\frac{d}{dx}(x^n) = nx^{n-1}"),
            PreviewItem(
                "15",
                "导数 (\\left \\right)",
                "\\frac{d}{dx}\\left(x^n\\right) = nx^{n-1}"
            ),
            PreviewItem(
                "16",
                "括号中的积分",
                "\\left[ \\int_0^1 \\frac{dx}{\\sqrt{1-x^2}} \\right] = \\frac{\\pi}{2}"
            ),
            PreviewItem("17", "bmod", "a \\bmod b"),
            PreviewItem("18", "pmod", "x \\equiv y \\pmod{n}"),
            PreviewItem("19", "mod", "a \\mod b"),
        )
    ),
    PreviewGroup(
        id = "advanced",
        title = "4. 高级级别",
        description = "连分数、级数、复杂表达式",
        items = listOf(
            PreviewItem("1", "连分数", "1 + \\frac{1}{1 + \\frac{1}{1 + \\frac{1}{1 + x}}}"),
            PreviewItem("2", "复杂指数", "e^{i\\pi} + 1 = 0"),
            PreviewItem("3", "嵌套根式", "\\sqrt{1 + \\sqrt{1 + \\sqrt{1 + x}}}"),
            PreviewItem(
                "4",
                "行列式表示",
                "\\det(A) = \\sum_{\\sigma} \\text{sgn}(\\sigma) \\prod_{i=1}^{n} a_{i,\\sigma(i)}"
            ),
            PreviewItem("5", "二重积分", "\\int_{0}^{1} \\int_{0}^{1} x^2 + y^2 dx dy"),
            PreviewItem(
                "6",
                "泰勒级数",
                "f(x) = \\sum_{n=0}^{\\infty} \\frac{f^{(n)}(a)}{n!}(x-a)^n"
            ),
            PreviewItem("7", "复杂求和", "\\sum_{k=1}^{n} \\frac{1}{k^2} = \\frac{\\pi^2}{6}"),
            PreviewItem("8", "operatorname 基础", "\\operatorname{Tr}(A)"),
            PreviewItem(
                "9",
                "operatorname 带下标",
                "\\operatorname{argmax}_{x \\in \\mathbb{R}} f(x)"
            ),
            PreviewItem("10", "operatorname + limits", "\\operatorname{Res}\\limits_{z=0} f(z)"),
        )
    ),
    PreviewGroup(
        id = "expert",
        title = "5. 专家级别",
        description = "物理公式、复变函数、高级积分",
        items = listOf(
            PreviewItem(
                "1",
                "柯西积分公式",
                "f(z) = \\frac{1}{2\\pi i} \\oint_{\\gamma} \\frac{f(\\zeta)}{\\zeta - z} d\\zeta"
            ),
            PreviewItem(
                "2",
                "傅里叶变换",
                "F(\\omega) = \\int_{-\\infty}^{\\infty} f(t) e^{-i\\omega t} dt"
            ),
            PreviewItem("3", "高斯积分", "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"),
            PreviewItem(
                "4",
                "黎曼ζ函数",
                "\\zeta(s) = \\sum_{n=1}^{\\infty} \\frac{1}{n^s} = \\prod_{p} \\frac{1}{1-p^{-s}}"
            ),
            PreviewItem(
                "5",
                "斯托克斯定理",
                "\\int_{\\partial \\Omega} \\omega = \\int_{\\Omega} d\\omega"
            ),
            PreviewItem(
                "6",
                "向量场",
                "v(x, t_\\theta)"
            )
        )
    ),
    PreviewGroup(
        id = "extreme",
        title = "6. 极其复杂级别",
        description = "量子力学、相对论、终极表达式",
        items = listOf(
            PreviewItem(
                "1",
                "超级连分数",
                "\\frac{1}{a + \\frac{b}{c + \\frac{d}{e + \\frac{f}{g + \\frac{h}{i + j}}}}}"
            ),
            PreviewItem(
                "2",
                "深度嵌套根式",
                "\\sqrt{x + \\sqrt{y + \\sqrt{z + \\sqrt{w + \\sqrt{v + u}}}}}"
            ),
            PreviewItem(
                "3",
                "多层次混合",
                "\\sum_{n=1}^{\\infty} \\frac{(-1)^n}{n} \\int_0^1 x^n \\left(\\frac{1}{1+x^2}\\right)^{\\frac{1}{2}} dx"
            ),
            PreviewItem(
                "4",
                "薛定谔方程",
                "i\\hbar\\frac{\\partial}{\\partial t}\\Psi(\\vec{r},t) = \\left[-\\frac{\\hbar^2}{2m}\\nabla^2 + V(\\vec{r},t)\\right]\\Psi(\\vec{r},t)"
            ),
            PreviewItem(
                "5",
                "路径积分",
                "\\langle x_f | e^{-iHt/\\hbar} | x_i \\rangle = \\int \\mathcal{D}[x(t)] e^{iS[x]/\\hbar}"
            ),
            PreviewItem(
                "6",
                "爱因斯坦场方程",
                "R_{\\mu\\nu} - \\frac{1}{2}Rg_{\\mu\\nu} + \\Lambda g_{\\mu\\nu} = \\frac{8\\pi G}{c^4}T_{\\mu\\nu}"
            ),
            PreviewItem(
                "7",
                "配分函数",
                "Z = \\sum_{n=0}^{\\infty} e^{-\\beta E_n} = \\text{Tr}\\left(e^{-\\beta \\hat{H}}\\right)"
            ),
            PreviewItem(
                "8",
                "费曼传播子",
                "G(x-y) = \\int \\frac{d^4p}{(2\\pi)^4} \\frac{e^{-ip(x-y)}}{p^2 - m^2 + i\\epsilon}"
            ),
            PreviewItem(
                "9",
                "杨-米尔斯拉氏量",
                "\\mathcal{L} = -\\frac{1}{4}F_{\\mu\\nu}^a F^{a\\mu\\nu} + \\bar{\\psi}(i\\gamma^\\mu D_\\mu - m)\\psi"
            ),
            PreviewItem(
                "10",
                "终极复杂表达式",
                "\\sum_{n=0}^{\\infty} \\frac{1}{n!} \\int_{-\\infty}^{\\infty} \\left(\\frac{d}{dx}\\right)^n \\left[\\frac{\\sqrt{\\pi}}{\\sqrt{1+x^2}} \\cdot e^{-\\frac{x^2}{2\\sigma^2}} \\cdot \\prod_{k=1}^{n} \\left(1 + \\frac{x^k}{k!}\\right)\\right] dx"
            ),
        )
    ),
    PreviewGroup(
        id = "delimiters",
        title = "7. 分隔符专题",
        description = "括号、自动伸缩、手动大小控制",
        items = listOf(
            PreviewItem(
                "1",
                "基础括号",
                "\\left( x + y \\right) \\quad \\left[ a + b \\right] \\quad \\left\\{ c + d \\right\\}"
            ),
            PreviewItem(
                "2",
                "括号自动伸缩",
                "\\left( \\frac{a}{b} \\right) + \\left[ \\frac{x^2}{y^2} \\right]"
            ),
            PreviewItem(
                "3",
                "求值符号（不对称分隔符）",
                "\\left. \\frac{d}{dx}x^2 \\right|_{x=0} = 0"
            ),
            PreviewItem("4", "分段函数（不对称分隔符）", "f(x) = \\left\\{ x^2, x > 0 \\right."),
            PreviewItem(
                "5",
                "复杂求值",
                "\\left. \\frac{d^2}{dx^2} \\left( x^3 + 2x^2 - x + 1 \\right) \\right|_{x=1} = 10"
            ),
            PreviewItem(
                "6",
                "手动大小 \\big",
                "\\big( \\frac{1}{2} \\big) \\quad \\big[ x + y \\big] \\quad \\big\\{ a, b \\big\\}"
            ),
            PreviewItem(
                "7",
                "手动大小 \\Big",
                "\\Big( \\frac{a}{b} \\Big) \\quad \\Big[ \\frac{x^2}{y^2} \\Big] \\quad \\Big| x \\Big|"
            ),
            PreviewItem(
                "8",
                "手动大小 \\bigg",
                "\\bigg( \\sum_{i=1}^n x_i \\bigg) \\quad \\bigg\\{ \\frac{a+b}{c+d} \\bigg\\}"
            ),
            PreviewItem(
                "9",
                "手动大小 \\Bigg",
                "\\Bigg[ \\int_0^1 \\frac{dx}{\\sqrt{1-x^2}} \\Bigg] = \\frac{\\pi}{2}"
            ),
            PreviewItem(
                "10",
                "所有手动大小对比",
                "\\big| \\Big| \\bigg| \\Bigg| x \\Bigg| \\bigg| \\Big| \\big|"
            ),
            PreviewItem(
                "11",
                "特殊分隔符",
                "\\left\\langle \\psi \\right\\rangle \\quad \\left\\lfloor x \\right\\rfloor \\quad \\left\\lceil y \\right\\rceil"
            ),
            PreviewItem(
                "12",
                "混合使用",
                "\\Bigg( \\left. \\frac{df}{dx} \\right|_{x=0} + \\Big[ \\sum_{i=1}^n x_i \\Big] \\Bigg)"
            ),
            PreviewItem(
                "13",
                "嵌套不对称",
                "\\left\\{ \\left. x^2 \\right|_{x=1}, \\left. y^2 \\right|_{y=2} \\right\\}"
            ),
            PreviewItem(
                "14",
                "绝对值与范数",
                "\\big| x \\big| \\quad \\Big\\| \\mathbf{v} \\Big\\| \\quad \\left| \\frac{a}{b} \\right|"
            ),
            PreviewItem(
                "15",
                "量子态（狄拉克符号）",
                "\\big\\langle \\psi \\big| \\hat{H} \\big| \\phi \\big\\rangle = E"
            ),
            PreviewItem("16", "lvert/rvert 定界符", "\\left\\lvert x \\right\\rvert"),
            PreviewItem("17", "lVert/rVert 定界符", "\\left\\lVert v \\right\\rVert"),
            PreviewItem("18", "Big lvert", "\\Big\\lvert x \\Big\\rvert"),
        )
    ),
    PreviewGroup(
        id = "accents",
        title = "8. 装饰符号专题",
        description = "上标装饰、箭头、帽子、取消线等",
        items = listOf(
            PreviewItem("1", "简单帽子", "\\hat{x}"),
            PreviewItem("2", "波浪线", "\\tilde{y}"),
            PreviewItem("3", "上划线", "\\overline{AB}"),
            PreviewItem("4", "下划线", "\\underline{text}"),
            PreviewItem("5", "向量箭头", "\\vec{v}"),
            PreviewItem("6", "单点", "\\dot{x}"),
            PreviewItem("7", "双点", "\\ddot{x}"),
            PreviewItem("8", "上大括号", "\\overbrace{a+b+c}"),
            PreviewItem("9", "下大括号", "\\underbrace{x+y+z}"),
            PreviewItem("10", "宽帽子", "\\widehat{ABC}"),
            PreviewItem("11", "右箭头", "\\overrightarrow{AB}"),
            PreviewItem("12", "左箭头", "\\overleftarrow{BA}"),
            PreviewItem("13", "取消线", "\\cancel{x+y}"),
            PreviewItem("14", "可扩展右箭头", "\\xrightarrow{f}"),
            PreviewItem("15", "可扩展左箭头", "\\xleftarrow{g}"),
            PreviewItem("16", "带下标箭头", "\\xrightarrow[n\\to\\infty]{\\text{极限}}"),
            PreviewItem("17", "上堆叠", "A \\overset{?}{=} B"),
            PreviewItem("18", "下堆叠", "\\underset{n \\to \\infty}{\\lim} f(x)"),
            PreviewItem("19", "stackrel", "x \\stackrel{def}{=} y + 1"),
            PreviewItem(
                "20",
                "堆叠与箭头组合",
                "A \\overset{f}{\\to} B \\underset{g}{\\to} C"
            ),
            PreviewItem(
                "21",
                "同一箭头上下堆叠",
                "A \\overset{f}{\\underset{g}{\\to}} B"
            ),
            PreviewItem(
                "22",
                "复杂装饰组合",
                "\\widehat{ABC} + \\overrightarrow{PQ} + \\cancel{X}"
            ),
            PreviewItem(
                "23",
                "物理学中的应用",
                "\\vec{F} = m\\vec{a} \\quad \\cancel{E_1} + E_2"
            ),
            PreviewItem("24", "钩右箭头", "\\xhookrightarrow{f}"),
            PreviewItem("25", "钩左箭头", "\\xhookleftarrow{g}"),
            PreviewItem("26", "钩箭头带下标", "\\xhookrightarrow[n\\to\\infty]{\\text{inclusion}}"),
            PreviewItem("27", "grave 重音", "\\grave{a}"),
            PreviewItem("28", "acute 重音", "\\acute{e}"),
            PreviewItem("29", "check 重音", "\\check{s}"),
            PreviewItem("30", "breve 重音", "\\breve{u}"),
            PreviewItem("31", "ring 重音", "\\ring{A}"),
            PreviewItem("32", "dddot 重音", "\\dddot{x}"),
        )
    ),
    PreviewGroup(
        id = "colors",
        title = "9 颜色专题",
        description = "文本颜色、公式着色",
        items = listOf(
            PreviewItem("1", "基础颜色", "\\color{red}{红色} + \\color{blue}{蓝色}"),
            PreviewItem("2", "textcolor 命令", "\\textcolor{green}{绿色文字}"),
            PreviewItem("3", "公式中着色", "x + \\color{red}{y^2} = \\color{blue}{z}"),
            PreviewItem("4", "分数着色", "\\frac{\\color{red}{a}}{\\color{blue}{b}}"),
            PreviewItem(
                "5",
                "多种颜色",
                "\\color{red}{R} \\color{orange}{O} \\color{yellow}{Y} \\color{green}{G} \\color{blue}{B}"
            ),
            PreviewItem("6", "强调重点", "E = mc^2 \\quad \\color{red}{(爱因斯坦质能方程)}"),
            PreviewItem("7", "十六进制颜色", "\\color{#FF5733}{橙红色} \\color{#33FF57}{青绿色}"),
        )
    ),
    PreviewGroup(
        id = "special_effects",
        title = "10. 特殊效果",
        description = "方框（boxed）、幻影（phantom）、取消线变体、否定修饰",
        items = listOf(
            PreviewItem("1", "简单方框", "\\boxed{E = mc^2}"),
            PreviewItem("2", "方框中的分数", "\\boxed{\\frac{a + b}{c}}"),
            PreviewItem("3", "方框中的求和", "\\boxed{\\sum_{i=1}^{n} x_i}"),
            PreviewItem("4", "多个方框", "\\boxed{x} + \\boxed{y} = \\boxed{z}"),
            PreviewItem("5", "方框+颜色", "\\boxed{\\color{red}{x^2} + \\color{blue}{y^2}} = r^2"),
            PreviewItem("6", "嵌套方框", "\\boxed{\\boxed{a} + b}"),
            PreviewItem(
                "7",
                "幻影对齐",
                "\\begin{aligned} x &= 1234 \\\\ \\phantom{x} &= 5678 \\end{aligned}"
            ),
            PreviewItem("8", "幻影占位", "a + \\phantom{bbb} = c"),
            PreviewItem("9", "复杂幻影", "\\frac{a}{\\phantom{a}b\\phantom{a}}"),
            PreviewItem("10", "方框+幻影组合", "\\boxed{x} + \\phantom{+ y} = z"),
            PreviewItem("11", "反向取消线", "\\bcancel{x+y}"),
            PreviewItem("12", "交叉取消线", "\\xcancel{abc}"),
            PreviewItem("13", "取消线对比", "\\cancel{a} + \\bcancel{b} + \\xcancel{c}"),
            PreviewItem("14", "否定等于", "a \\not= b"),
            PreviewItem("15", "否定属于", "x \\not\\in S"),
            PreviewItem("16", "否定子集", "A \\not\\subset B"),
            PreviewItem("17", "smash 消除高度", "x + \\smash{\\frac{a}{b}} + y"),
            PreviewItem("18", "vphantom 垂直占位", "\\left(\\vphantom{\\frac{a}{b}} x\\right)"),
            PreviewItem("19", "hphantom 水平占位", "a + \\hphantom{bbb} + c"),
            PreviewItem("20", "substack 多行条件", "\\sum_{\\substack{i<n \\\\ j<m}} x_{ij}"),
            PreviewItem("21", "公式编号 tag", "E = mc^2 \\tag{1}"),
            PreviewItem("22", "公式编号 tag*", "F = ma \\tag*{Newton}"),
            PreviewItem("23", "underbrace 标注", "\\underbrace{x+y+z}_{n}"),
            PreviewItem("24", "overbrace 标注", "\\overbrace{a+b+c}^{3\\text{ terms}}"),
            PreviewItem("25", "label+eqref", "\\label{eq:1} E = mc^2 \\eqref{eq:1}"),
            PreviewItem("26", "ref 引用", "See \\ref{eq:1}"),
            PreviewItem("27", "sideset 四角", "\\sideset{_a^b}{_c^d}{\\sum}"),
            PreviewItem("28", "sideset 部分", "\\sideset{_1}{^n}{\\prod}"),
            PreviewItem("29", "tensor 基础", "\\tensor{T}{^a_b}"),
            PreviewItem("30", "tensor 多指标", "\\tensor{R}{^\\mu_{\\nu\\rho\\sigma}}"),
            PreviewItem(
                "31", "高亮子表达式(pattern)", "E = mc^2",
                content = {
                    Latex(
                        latex = "E = mc^2",
                        config = LatexConfig(
                            highlight = HighlightConfig(
                                ranges = listOf(
                                    HighlightRange(
                                        pattern = "mc",
                                        color = Color(0x4400AAFF),
                                        borderColor = Color(0xFF0088FF)
                                    )
                                )
                            )
                        ),
                        isDarkTheme = false
                    )
                }
            ),
            PreviewItem(
                "32", "高亮子表达式(indices)", "\\frac{a+b}{c} + x^2",
                content = {
                    Latex(
                        latex = "\\frac{a+b}{c} + x^2",
                        config = LatexConfig(
                            highlight = HighlightConfig(
                                ranges = listOf(
                                    HighlightRange(
                                        nodeIndices = 0..0,
                                        color = Color(0x33FF6600),
                                        borderColor = Color(0xFFFF6600)
                                    )
                                )
                            )
                        ),
                        isDarkTheme = false
                    )
                }
            ),
            PreviewItem(
                "33", "多区域高亮", "a + b + c + d",
                content = {
                    Latex(
                        latex = "a + b + c + d",
                        config = LatexConfig(
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
                        ),
                        isDarkTheme = false
                    )
                }
            ),
        )
    ),
    PreviewGroup(
        id = "custom_commands",
        title = "11. 自定义命令",
        description = "newcommand 定义和使用",
        items = listOf(
            PreviewItem("1", "无参数命令", "\\newcommand{\\R}{\\mathbb{R}} x \\in \\R"),
            PreviewItem("2", "单参数命令", "\\newcommand{\\diff}[1]{\\frac{d}{d#1}} \\diff{x}"),
            PreviewItem(
                "3",
                "双参数命令",
                "\\newcommand{\\pdiff}[2]{\\frac{\\partial #1}{\\partial #2}} \\pdiff{f}{x}"
            ),
            PreviewItem(
                "4",
                "多个自定义命令",
                "\\newcommand{\\N}{\\mathbb{N}} \\newcommand{\\Z}{\\mathbb{Z}} \\N \\subset \\Z"
            ),
            PreviewItem("5", "嵌套命令", "\\newcommand{\\abs}[1]{\\left|#1\\right|} \\abs{x}"),
            PreviewItem("6", "参数在文本中", "\\newcommand{\\test}[1]{a#1b} \\test{x}"),
            PreviewItem(
                "7",
                "复杂定义",
                "\\newcommand{\\myvec}[1]{\\boldsymbol{#1}} \\myvec{v} = \\myvec{u} + \\myvec{w}"
            ),
            PreviewItem(
                "8",
                "数学符号",
                "\\newcommand{\\R}{\\mathbb{R}} \\newcommand{\\C}{\\mathbb{C}} \\R + \\C"
            ),
            PreviewItem(
                "9",
                "组合其他命令",
                "\\newcommand{\\norm}[1]{\\left\\|#1\\right\\|} \\norm{x} = \\norm{\\vec{v}}"
            ),
            PreviewItem(
                "10",
                "递归定义",
                "\\newcommand{\\fact}[1]{#1!} \\fact{n} = \\frac{\\fact{2n}}{(2n)!!}"
            ),
        )
    ),
    PreviewGroup(
        id = "spaces",
        title = "12. 间距专题",
        description = "负空格、自定义空格、水平间距",
        items = listOf(
            PreviewItem("1", "标准空格对比", "a \\, b \\: c \\; d \\quad e \\qquad f"),
            PreviewItem("2", "负空格", "a \\! b (tight)"),
            PreviewItem("3", "自定义空格 (cm)", "a \\hspace{1cm} b"),
            PreviewItem("4", "自定义空格 (pt)", "a \\hspace{20pt} b"),
            PreviewItem("5", "自定义空格 (em)", "a \\hspace{2em} b"),
            PreviewItem("6", "负自定义空格", "a \\hspace{-0.5em} b"),
        )
    ),
    PreviewGroup(
        id = "mathstyle",
        title = "13. 数学模式切换",
        description = "displaystyle, textstyle, scriptstyle, scriptscriptstyle",
        items = listOf(
            PreviewItem(
                "1",
                "displaystyle 分数",
                "\\frac{a}{b} \\quad \\displaystyle{\\frac{a}{b}}"
            ),
            PreviewItem(
                "2",
                "displaystyle 求和",
                "\\sum_{i=1}^{n} \\quad \\displaystyle{\\sum_{i=1}^{n}}"
            ),
            PreviewItem(
                "3",
                "scriptstyle 求和",
                "x\\sum_{i=1}^{n} \\quad x\\scriptstyle{\\sum_{i=1}^{n}}"
            ),
            PreviewItem(
                "4",
                "求和作为上标",
                "x^{\\sum_{i=1}^{n}} \\quad x^{\\scriptstyle{\\sum_{i=1}^{n}}}"
            ),
            PreviewItem("5", "scriptscriptstyle", "\\scriptscriptstyle{x + y + z}"),
            PreviewItem(
                "6",
                "分数中的模式",
                "\\frac{\\displaystyle{\\sum_{i=1}^{n}}}{\\textstyle{n}}"
            ),
            PreviewItem("7", "嵌套模式", "\\displaystyle{\\frac{\\sum}{n}}"),
            PreviewItem("8", "symbf 粗体", "\\symbf{x} + \\symbf{\\alpha}"),
            PreviewItem("9", "symsf 无衬线", "\\symsf{ABC}"),
            PreviewItem("10", "symrm 罗马体", "\\symrm{dx}"),
        )
    ),
    PreviewGroup(
        id = "environments",
        title = "14. 环境专题",
        description = "split、multline、eqnarray、subequations、cases 环境",
        items = listOf(
            PreviewItem("1", "split 基础", "\\begin{split} x &= a + b \\\\ &= c \\end{split}"),
            PreviewItem(
                "2",
                "split 多行",
                "\\begin{split} a &= b + c \\\\ &= d + e \\\\ &= f \\end{split}"
            ),
            PreviewItem(
                "3",
                "multline 基础",
                "\\begin{multline} a + b + c \\\\ + d + e \\end{multline}"
            ),
            PreviewItem(
                "4",
                "multline 三行",
                "\\begin{multline} \\text{Left} \\\\ \\text{Center} \\\\ \\text{Right} \\end{multline}"
            ),
            PreviewItem(
                "5",
                "eqnarray 基础",
                "\\begin{eqnarray} x &=& 1 \\\\ y &=& 2 \\end{eqnarray}"
            ),
            PreviewItem(
                "6",
                "eqnarray 三列",
                "\\begin{eqnarray} a + b &=& c \\\\ d - e &=& f \\end{eqnarray}"
            ),
            PreviewItem("7", "subequations", "\\begin{subequations} a = b \\end{subequations}"),
            PreviewItem("8", "混合环境", "\\begin{align} x &= 1 \\\\ y &= 2 \\end{align}"),
            PreviewItem(
                "9",
                "cases 基础",
                "f(x) = \\begin{cases} x^2 & \\text{if } x > 0 \\\\ 0 & \\text{if } x = 0 \\\\ -x^2 & \\text{if } x < 0 \\end{cases}"
            ),
            PreviewItem(
                "10",
                "cases 简单",
                "y = \\begin{cases} 1 & x > 0 \\\\ 0 & x = 0 \\\\ -1 & x < 0 \\end{cases}"
            ),
            PreviewItem(
                "11",
                "cases 嵌套分数",
                "|x| = \\begin{cases} \\frac{x}{1} & x \\geq 0 \\\\ \\frac{-x}{1} & x < 0 \\end{cases}"
            ),
            PreviewItem(
                "12",
                "tabular 基础",
                "\\begin{tabular}{cc} a & b \\\\ c & d \\end{tabular}"
            ),
            PreviewItem(
                "13",
                "tabular 三列",
                "\\begin{tabular}{lcr} left & center & right \\\\ 1 & 2 & 3 \\end{tabular}"
            ),
            PreviewItem(
                "14",
                "align* 环境",
                "\\begin{align*} a &= b + c \\\\ d &= e + f \\end{align*}"
            ),
            PreviewItem(
                "15",
                "gather* 环境",
                "\\begin{gather*} x + y = z \\\\ a^2 + b^2 = c^2 \\end{gather*}"
            ),
            PreviewItem(
                "16",
                "multline* 环境",
                "\\begin{multline*} a + b + c \\\\ + d + e + f \\end{multline*}"
            ),
            PreviewItem(
                "17",
                "eqnarray* 环境",
                "\\begin{eqnarray*} x &=& 1 \\\\ y &=& 2 \\end{eqnarray*}"
            ),
            PreviewItem("18", "equation* 环境", "\\begin{equation*} E = mc^2 \\end{equation*}"),
        )
    ),
    PreviewGroup(
        id = "animated",
        title = "15. 动画过渡",
        description = "AnimatedLatex 公式切换动画",
        items = listOf(
            PreviewItem(
                "1", "淡入淡出 (Crossfade)", "E = mc^2 ↔ a^2+b^2=c^2",
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
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.CROSSFADE,
                            durationMillis = 500
                        ),
                        isDarkTheme = false
                    )
                }
            ),
            PreviewItem(
                "2", "上滑切换 (Slide Up)", "\\sum ↔ \\int",
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
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.SLIDE_UP,
                            durationMillis = 400
                        ),
                        isDarkTheme = false
                    )
                }
            ),
            PreviewItem(
                "3", "下滑切换 (Slide Down)", "\\frac{a}{b} ↔ \\sqrt{x}",
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
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.SLIDE_DOWN,
                            durationMillis = 400
                        ),
                        isDarkTheme = false
                    )
                }
            ),
            PreviewItem(
                "4", "淡入+滑动 (Fade Slide)", "多公式循环",
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
                        animationConfig = LatexAnimationConfig(
                            transition = LatexTransition.FADE_SLIDE,
                            durationMillis = 600
                        ),
                        isDarkTheme = false
                    )
                }
            ),
            PreviewItem(
                "5", "四种动画对比", "同时展示所有过渡类型",
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
                                animationConfig = LatexAnimationConfig(
                                    transition = transition,
                                    durationMillis = 400
                                ),
                                isDarkTheme = false
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            ),
        )
    ),
)

@Preview
@Composable
fun BasicLatexPreview(onBack: () -> Unit) {
    PreviewCategoryScreen(
        title = "基础 LaTeX",
        groups = basicLatexPreviewGroups,
        onBack = onBack
    )
}
