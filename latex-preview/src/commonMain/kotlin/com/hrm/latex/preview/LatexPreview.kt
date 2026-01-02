package com.hrm.latex.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.Latex
import org.jetbrains.compose.ui.tooling.preview.Preview

// ========== 1. 基础级别 ==========

@Preview
@Composable
fun Preview_01_SimpleText() {
    PreviewCard("01. 简单文本") {
        Latex(latex = "Hello LaTeX", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_02_SimpleSuperscript() {
    PreviewCard("02. 简单上标") {
        Latex(latex = "x^2", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_03_SimpleSubscript() {
    PreviewCard("03. 简单下标") {
        Latex(latex = "a_i", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_04_SuperAndSubscript() {
    PreviewCard("04. 上标+下标") {
        Latex(latex = "x_i^2", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_05_SimpleFraction() {
    PreviewCard("05. 简单分数") {
        Latex(latex = "\\frac{1}{2}", isDarkTheme = false)
    }
}

// ========== 2. 初级级别 ==========

@Preview
@Composable
fun Preview_06_Polynomial() {
    PreviewCard("06. 多项式") {
        Latex(latex = "ax^2 + bx + c = 0", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_07_PythagoreanTheorem() {
    PreviewCard("07. 勾股定理") {
        Latex(latex = "a^2 + b^2 = c^2", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_08_QuadraticFormula() {
    PreviewCard("08. 二次方程解") {
        Latex(latex = "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_09_SimpleSum() {
    PreviewCard("09. 简单求和") {
        Latex(latex = "\\sum_{i=1}^{n} i", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_10_SimpleIntegral() {
    PreviewCard("10. 简单积分") {
        Latex(latex = "\\int_0^1 x dx", isDarkTheme = false)
    }
}

// ========== 3. 中级级别 ==========

@Preview
@Composable
fun Preview_11_NestedFractions() {
    PreviewCard("11. 嵌套分数") {
        Latex(latex = "\\frac{1}{2 + \\frac{1}{3}}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_12_ComplexFraction() {
    PreviewCard("12. 复杂分数") {
        Latex(latex = "\\frac{a + b}{c + d}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_13_SquareRoot() {
    PreviewCard("13. 平方根") {
        Latex(latex = "\\sqrt{x^2 + y^2}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_14_ComplexSum() {
    PreviewCard("14. 复杂求和") {
        Latex(latex = "\\sum_{i=1}^{n} i^2 = \\frac{n(n+1)(2n+1)}{6}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_15_DefiniteIntegral() {
    PreviewCard("15. 定积分") {
        Latex(latex = "\\int_{0}^{\\infty} e^{-x} dx = 1", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_16_Product() {
    PreviewCard("16. 连乘") {
        Latex(latex = "\\prod_{i=1}^{n} x_i", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_17_Limit() {
    PreviewCard("17. 极限") {
        Latex(latex = "\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_18_Derivative() {
    PreviewCard("18. 导数") {
        Latex(latex = "\\frac{d}{dx}(x^n) = nx^{n-1}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_18b_DerivativeWithLeft() {
    PreviewCard("18b. 导数 (使用 \\left \\right)") {
        Latex(latex = "\\frac{d}{dx}\\left(x^n\\right) = nx^{n-1}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_Debug_Parentheses() {
    PreviewCard("Debug: 括号测试") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Latex(latex = "(x)", isDarkTheme = false)
            Latex(latex = "(x^2)", isDarkTheme = false)
            Latex(latex = "(x^{10})", isDarkTheme = false)
            Latex(latex = "\\left(x^{10}\\right)", isDarkTheme = false)
        }
    }
}

@Preview
@Composable
fun Preview_Debug_TextInSubscript() {
    PreviewCard("Debug: 下标中的文本测试") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Latex(latex = "\\prod_{p}", isDarkTheme = false)
            Latex(latex = "\\prod_{p prime}", isDarkTheme = false)
            Latex(latex = "\\prod_{p \\text{ prime}}", isDarkTheme = false)
            Latex(latex = "\\prod_{p \\text{ is prime}}", isDarkTheme = false)
            Latex(latex = "\\sum_{i \\text{ even}}", isDarkTheme = false)
        }
    }
}

// ========== 4. 高级级别 ==========

@Preview
@Composable
fun Preview_19_ContinuedFraction() {
    PreviewCard("19. 连分数") {
        Latex(latex = "1 + \\frac{1}{1 + \\frac{1}{1 + \\frac{1}{1 + x}}}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_20_ComplexExponent() {
    PreviewCard("20. 复杂指数") {
        Latex(latex = "e^{i\\pi} + 1 = 0", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_21_NestedRadicals() {
    PreviewCard("21. 嵌套根式") {
        Latex(latex = "\\sqrt{1 + \\sqrt{1 + \\sqrt{1 + x}}}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_22_MatrixDeterminant() {
    PreviewCard("22. 行列式表示") {
        Latex(
            latex = "\\det(A) = \\sum_{\\sigma} \\text{sgn}(\\sigma) \\prod_{i=1}^{n} a_{i,\\sigma(i)}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_23_DoubleIntegral() {
    PreviewCard("23. 二重积分") {
        Latex(latex = "\\int_{0}^{1} \\int_{0}^{1} x^2 + y^2 dx dy", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_24_TaylorSeries() {
    PreviewCard("24. 泰勒级数") {
        Latex(
            latex = "f(x) = \\sum_{n=0}^{\\infty} \\frac{f^{(n)}(a)}{n!}(x-a)^n",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_25_ComplexSum() {
    PreviewCard("25. 复杂求和") {
        Latex(latex = "\\sum_{k=1}^{n} \\frac{1}{k^2} = \\frac{\\pi^2}{6}", isDarkTheme = false)
    }
}

// ========== 5. 专家级别 ==========

@Preview
@Composable
fun Preview_26_CauchyIntegral() {
    PreviewCard("26. 柯西积分公式") {
        Latex(
            latex = "f(z) = \\frac{1}{2\\pi i} \\oint_{\\gamma} \\frac{f(\\zeta)}{\\zeta - z} d\\zeta",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_27_FourierTransform() {
    PreviewCard("27. 傅里叶变换") {
        Latex(
            latex = "F(\\omega) = \\int_{-\\infty}^{\\infty} f(t) e^{-i\\omega t} dt",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_28_GaussianIntegral() {
    PreviewCard("28. 高斯积分") {
        Latex(latex = "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}", isDarkTheme = false)
    }
}

@Preview
@Composable
fun Preview_29_RiemannZeta() {
    PreviewCard("29. 黎曼ζ函数") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 版本1: 最简洁，约定俗成的写法
            Latex(
                latex = "\\zeta(s) = \\sum_{n=1}^{\\infty} \\frac{1}{n^s} = \\prod_{p} \\frac{1}{1-p^{-s}}",
                isDarkTheme = false
            )
            // 版本2: 使用文本模式
            Latex(
                latex = "\\zeta(s) = \\prod_{p \\text{ prime}} \\frac{1}{1-p^{-s}}",
                isDarkTheme = false
            )
        }
    }
}

@Preview
@Composable
fun Preview_30_StokesTheorem() {
    PreviewCard("30. 斯托克斯定理") {
        Latex(
            latex = "\\int_{\\partial \\Omega} \\omega = \\int_{\\Omega} d\\omega",
            isDarkTheme = false
        )
    }
}

// ========== 6. 极其复杂级别 ==========

@Preview
@Composable
fun Preview_31_ComplexNestedFraction() {
    PreviewCard("31. 超级连分数") {
        Latex(
            latex = "\\frac{1}{a + \\frac{b}{c + \\frac{d}{e + \\frac{f}{g + \\frac{h}{i + j}}}}}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_32_DeepNestedRadicals() {
    PreviewCard("32. 深度嵌套根式") {
        Latex(
            latex = "\\sqrt{x + \\sqrt{y + \\sqrt{z + \\sqrt{w + \\sqrt{v + u}}}}}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_33_ComplexMultiLevel() {
    PreviewCard("33. 多层次混合") {
        Latex(
            latex = "\\sum_{n=1}^{\\infty} \\frac{(-1)^n}{n} \\int_0^1 x^n \\left(\\frac{1}{1+x^2}\\right)^{\\frac{1}{2}} dx",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_34_SchrodingerEquation() {
    PreviewCard("34. 薛定谔方程") {
        Latex(
            latex = "i\\hbar\\frac{\\partial}{\\partial t}\\Psi(\\vec{r},t) = \\left[-\\frac{\\hbar^2}{2m}\\nabla^2 + V(\\vec{r},t)\\right]\\Psi(\\vec{r},t)",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_35_PathIntegral() {
    PreviewCard("35. 路径积分") {
        Latex(
            latex = "\\langle x_f | e^{-iHt/\\hbar} | x_i \\rangle = \\int \\mathcal{D}[x(t)] e^{iS[x]/\\hbar}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_36_EinsteinFieldEquations() {
    PreviewCard("36. 爱因斯坦场方程") {
        Latex(
            latex = "R_{\\mu\\nu} - \\frac{1}{2}Rg_{\\mu\\nu} + \\Lambda g_{\\mu\\nu} = \\frac{8\\pi G}{c^4}T_{\\mu\\nu}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_37_PartitionFunction() {
    PreviewCard("37. 配分函数") {
        Latex(
            latex = "Z = \\sum_{n=0}^{\\infty} e^{-\\beta E_n} = \\text{Tr}\\left(e^{-\\beta \\hat{H}}\\right)",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_38_FeynmanDiagram() {
    PreviewCard("38. 费曼传播子") {
        Latex(
            latex = "G(x-y) = \\int \\frac{d^4p}{(2\\pi)^4} \\frac{e^{-ip(x-y)}}{p^2 - m^2 + i\\epsilon}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_39_YangMillsLagrangian() {
    PreviewCard("39. 杨-米尔斯拉氏量") {
        Latex(
            latex = "\\mathcal{L} = -\\frac{1}{4}F_{\\mu\\nu}^a F^{a\\mu\\nu} + \\bar{\\psi}(i\\gamma^\\mu D_\\mu - m)\\psi",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_40_UltraComplex() {
    PreviewCard("40. 终极复杂表达式") {
        Latex(
            latex = "\\sum_{n=0}^{\\infty} \\frac{1}{n!} \\int_{-\\infty}^{\\infty} \\left(\\frac{d}{dx}\\right)^n \\left[\\frac{\\sqrt{\\pi}}{\\sqrt{1+x^2}} \\cdot e^{-\\frac{x^2}{2\\sigma^2}} \\cdot \\prod_{k=1}^{n} \\left(1 + \\frac{x^k}{k!}\\right)\\right] dx",
            isDarkTheme = false
        )
    }
}

// ========== 7. 分隔符专题（新增 ✨）==========

@Preview
@Composable
fun Preview_41_BasicDelimiters() {
    PreviewCard("41. 基础括号") {
        Latex(
            latex = "\\left( x + y \\right) \\quad \\left[ a + b \\right] \\quad \\left\\{ c + d \\right\\}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_42_DelimitersWithFraction() {
    PreviewCard("42. 括号自动伸缩") {
        Latex(
            latex = "\\left( \\frac{a}{b} \\right) + \\left[ \\frac{x^2}{y^2} \\right]",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_43_AsymmetricDelimiterEvaluation() {
    PreviewCard("43. 求值符号（不对称分隔符）") {
        Latex(
            latex = "\\left. \\frac{d}{dx}x^2 \\right|_{x=0} = 0",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_44_AsymmetricDelimiterPiecewise() {
    PreviewCard("44. 分段函数（不对称分隔符）") {
        Latex(
            latex = "f(x) = \\left\\{ x^2, x > 0 \\right.",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_45_ComplexEvaluation() {
    PreviewCard("45. 复杂求值") {
        Latex(
            latex = "\\left. \\frac{d^2}{dx^2} \\left( x^3 + 2x^2 - x + 1 \\right) \\right|_{x=1} = 10",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_46_ManualSizeBig() {
    PreviewCard("46. 手动大小 \\big") {
        Latex(
            latex = "\\big( \\frac{1}{2} \\big) \\quad \\big[ x + y \\big] \\quad \\big\\{ a, b \\big\\}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_47_ManualSizeBigG() {
    PreviewCard("47. 手动大小 \\Big") {
        Latex(
            latex = "\\Big( \\frac{a}{b} \\Big) \\quad \\Big[ \\frac{x^2}{y^2} \\Big] \\quad \\Big| x \\Big|",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_48_ManualSizeBigg() {
    PreviewCard("48. 手动大小 \\bigg") {
        Latex(
            latex = "\\bigg( \\sum_{i=1}^n x_i \\bigg) \\quad \\bigg\\{ \\frac{a+b}{c+d} \\bigg\\}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_49_ManualSizeBiggG() {
    PreviewCard("49. 手动大小 \\Bigg") {
        Latex(
            latex = "\\Bigg[ \\int_0^1 \\frac{dx}{\\sqrt{1-x^2}} \\Bigg] = \\frac{\\pi}{2}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_50_AllManualSizes() {
    PreviewCard("50. 所有手动大小对比") {
        Latex(
            latex = "\\big| \\Big| \\bigg| \\Bigg| x \\Bigg| \\bigg| \\Big| \\big|",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_51_SpecialDelimiters() {
    PreviewCard("51. 特殊分隔符") {
        Latex(
            latex = "\\left\\langle \\psi \\right\\rangle \\quad \\left\\lfloor x \\right\\rfloor \\quad \\left\\lceil y \\right\\rceil",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_52_MixedDelimiters() {
    PreviewCard("52. 混合使用") {
        Latex(
            latex = "\\Bigg( \\left. \\frac{df}{dx} \\right|_{x=0} + \\Big[ \\sum_{i=1}^n x_i \\Big] \\Bigg)",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_53_NestedAsymmetric() {
    PreviewCard("53. 嵌套不对称") {
        Latex(
            latex = "\\left\\{ \\left. x^2 \\right|_{x=1}, \\left. y^2 \\right|_{y=2} \\right\\}",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_54_AbsoluteValue() {
    PreviewCard("54. 绝对值与范数") {
        Latex(
            latex = "\\big| x \\big| \\quad \\Big\\| \\mathbf{v} \\Big\\| \\quad \\left| \\frac{a}{b} \\right|",
            isDarkTheme = false
        )
    }
}

@Preview
@Composable
fun Preview_55_QuantumBraKet() {
    PreviewCard("55. 量子态（狄拉克符号）") {
        Latex(
            latex = "\\big\\langle \\psi \\big| \\hat{H} \\big| \\phi \\big\\rangle = E",
            isDarkTheme = false
        )
    }
}

// ========== 数据模型 ==========

data class PreviewGroup(
    val id: String,
    val title: String,
    val description: String,
    val items: List<PreviewItem>
)

data class PreviewItem(
    val id: String,
    val title: String,
    val latex: String,
    val content: @Composable () -> Unit = { Latex(latex = latex, isDarkTheme = false) }
)

// ========== 分组数据 ==========

val previewGroups = listOf(
    PreviewGroup(
        id = "basic",
        title = "1. 基础级别",
        description = "简单文本、上下标、分数",
        items = listOf(
            PreviewItem("01", "简单文本", "Hello LaTeX"),
            PreviewItem("02", "简单上标", "x^2"),
            PreviewItem("03", "简单下标", "a_i"),
            PreviewItem("04", "上标+下标", "x_i^2"),
            PreviewItem("05", "简单分数", "\\frac{1}{2}"),
        )
    ),
    PreviewGroup(
        id = "elementary",
        title = "2. 初级级别",
        description = "多项式、方程、简单求和积分",
        items = listOf(
            PreviewItem("06", "多项式", "ax^2 + bx + c = 0"),
            PreviewItem("07", "勾股定理", "a^2 + b^2 = c^2"),
            PreviewItem("08", "二次方程解", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"),
            PreviewItem("09", "简单求和", "\\sum_{i=1}^{n} i"),
            PreviewItem("10", "简单积分", "\\int_0^1 x dx"),
        )
    ),
    PreviewGroup(
        id = "intermediate",
        title = "3. 中级级别",
        description = "嵌套结构、根式、复杂运算",
        items = listOf(
            PreviewItem("11", "嵌套分数", "\\frac{1}{2 + \\frac{1}{3}}"),
            PreviewItem("12", "复杂分数", "\\frac{a + b}{c + d}"),
            PreviewItem("13", "平方根", "\\sqrt{x^2 + y^2}"),
            PreviewItem("14", "复杂求和", "\\sum_{i=1}^{n} i^2 = \\frac{n(n+1)(2n+1)}{6}"),
            PreviewItem("15", "定积分", "\\int_{0}^{\\infty} e^{-x} dx = 1"),
            PreviewItem("16", "连乘", "\\prod_{i=1}^{n} x_i"),
            PreviewItem("17", "极限", "\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1"),
            PreviewItem("18", "导数", "\\frac{d}{dx}(x^n) = nx^{n-1}"),
            PreviewItem(
                "18b",
                "导数 (\\left \\right)",
                "\\frac{d}{dx}\\left(x^n\\right) = nx^{n-1}"
            ),
            PreviewItem("debug1", "括号测试", "(x)", content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Latex(latex = "(x)", isDarkTheme = false)
                    Latex(latex = "(x^2)", isDarkTheme = false)
                    Latex(latex = "(x^{10})", isDarkTheme = false)
                    Latex(latex = "\\left(x^{10}\\right)", isDarkTheme = false)
                }
            }),
            PreviewItem("debug2", "下标中的文本测试", "\\prod_{p}", content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Latex(latex = "\\prod_{p}", isDarkTheme = false)
                    Latex(latex = "\\prod_{p prime}", isDarkTheme = false)
                    Latex(latex = "\\prod_{p \\text{ prime}}", isDarkTheme = false)
                    Latex(latex = "\\prod_{p \\text{ is prime}}", isDarkTheme = false)
                    Latex(latex = "\\sum_{i \\text{ even}}", isDarkTheme = false)
                }
            }),
        )
    ),
    PreviewGroup(
        id = "advanced",
        title = "4. 高级级别",
        description = "连分数、级数、复杂表达式",
        items = listOf(
            PreviewItem("19", "连分数", "1 + \\frac{1}{1 + \\frac{1}{1 + \\frac{1}{1 + x}}}"),
            PreviewItem("20", "复杂指数", "e^{i\\pi} + 1 = 0"),
            PreviewItem("21", "嵌套根式", "\\sqrt{1 + \\sqrt{1 + \\sqrt{1 + x}}}"),
            PreviewItem(
                "22",
                "行列式表示",
                "\\det(A) = \\sum_{\\sigma} \\text{sgn}(\\sigma) \\prod_{i=1}^{n} a_{i,\\sigma(i)}"
            ),
            PreviewItem("23", "二重积分", "\\int_{0}^{1} \\int_{0}^{1} x^2 + y^2 dx dy"),
            PreviewItem(
                "24",
                "泰勒级数",
                "f(x) = \\sum_{n=0}^{\\infty} \\frac{f^{(n)}(a)}{n!}(x-a)^n"
            ),
            PreviewItem("25", "复杂求和", "\\sum_{k=1}^{n} \\frac{1}{k^2} = \\frac{\\pi^2}{6}"),
        )
    ),
    PreviewGroup(
        id = "expert",
        title = "5. 专家级别",
        description = "物理公式、复变函数、高级积分",
        items = listOf(
            PreviewItem(
                "26",
                "柯西积分公式",
                "f(z) = \\frac{1}{2\\pi i} \\oint_{\\gamma} \\frac{f(\\zeta)}{\\zeta - z} d\\zeta"
            ),
            PreviewItem(
                "27",
                "傅里叶变换",
                "F(\\omega) = \\int_{-\\infty}^{\\infty} f(t) e^{-i\\omega t} dt"
            ),
            PreviewItem("28", "高斯积分", "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}"),
            PreviewItem(
                "29",
                "黎曼ζ函数",
                "\\zeta(s) = \\sum_{n=1}^{\\infty} \\frac{1}{n^s} = \\prod_{p} \\frac{1}{1-p^{-s}}",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Latex(
                            latex = "\\zeta(s) = \\sum_{n=1}^{\\infty} \\frac{1}{n^s} = \\prod_{p} \\frac{1}{1-p^{-s}}",
                            isDarkTheme = false
                        )
                        Latex(
                            latex = "\\zeta(s) = \\prod_{p \\text{ prime}} \\frac{1}{1-p^{-s}}",
                            isDarkTheme = false
                        )
                    }
                }),
            PreviewItem(
                "30",
                "斯托克斯定理",
                "\\int_{\\partial \\Omega} \\omega = \\int_{\\Omega} d\\omega"
            ),
        )
    ),
    PreviewGroup(
        id = "extreme",
        title = "6. 极其复杂级别",
        description = "量子力学、相对论、终极表达式",
        items = listOf(
            PreviewItem(
                "31",
                "超级连分数",
                "\\frac{1}{a + \\frac{b}{c + \\frac{d}{e + \\frac{f}{g + \\frac{h}{i + j}}}}}"
            ),
            PreviewItem(
                "32",
                "深度嵌套根式",
                "\\sqrt{x + \\sqrt{y + \\sqrt{z + \\sqrt{w + \\sqrt{v + u}}}}}"
            ),
            PreviewItem(
                "33",
                "多层次混合",
                "\\sum_{n=1}^{\\infty} \\frac{(-1)^n}{n} \\int_0^1 x^n \\left(\\frac{1}{1+x^2}\\right)^{\\frac{1}{2}} dx"
            ),
            PreviewItem(
                "34",
                "薛定谔方程",
                "i\\hbar\\frac{\\partial}{\\partial t}\\Psi(\\vec{r},t) = \\left[-\\frac{\\hbar^2}{2m}\\nabla^2 + V(\\vec{r},t)\\right]\\Psi(\\vec{r},t)"
            ),
            PreviewItem(
                "35",
                "路径积分",
                "\\langle x_f | e^{-iHt/\\hbar} | x_i \\rangle = \\int \\mathcal{D}[x(t)] e^{iS[x]/\\hbar}"
            ),
            PreviewItem(
                "36",
                "爱因斯坦场方程",
                "R_{\\mu\\nu} - \\frac{1}{2}Rg_{\\mu\\nu} + \\Lambda g_{\\mu\\nu} = \\frac{8\\pi G}{c^4}T_{\\mu\\nu}"
            ),
            PreviewItem(
                "37",
                "配分函数",
                "Z = \\sum_{n=0}^{\\infty} e^{-\\beta E_n} = \\text{Tr}\\left(e^{-\\beta \\hat{H}}\\right)"
            ),
            PreviewItem(
                "38",
                "费曼传播子",
                "G(x-y) = \\int \\frac{d^4p}{(2\\pi)^4} \\frac{e^{-ip(x-y)}}{p^2 - m^2 + i\\epsilon}"
            ),
            PreviewItem(
                "39",
                "杨-米尔斯拉氏量",
                "\\mathcal{L} = -\\frac{1}{4}F_{\\mu\\nu}^a F^{a\\mu\\nu} + \\bar{\\psi}(i\\gamma^\\mu D_\\mu - m)\\psi"
            ),
            PreviewItem(
                "40",
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
                "41",
                "基础括号",
                "\\left( x + y \\right) \\quad \\left[ a + b \\right] \\quad \\left\\{ c + d \\right\\}"
            ),
            PreviewItem(
                "42",
                "括号自动伸缩",
                "\\left( \\frac{a}{b} \\right) + \\left[ \\frac{x^2}{y^2} \\right]"
            ),
            PreviewItem(
                "43",
                "求值符号（不对称分隔符）",
                "\\left. \\frac{d}{dx}x^2 \\right|_{x=0} = 0"
            ),
            PreviewItem("44", "分段函数（不对称分隔符）", "f(x) = \\left\\{ x^2, x > 0 \\right."),
            PreviewItem(
                "45",
                "复杂求值",
                "\\left. \\frac{d^2}{dx^2} \\left( x^3 + 2x^2 - x + 1 \\right) \\right|_{x=1} = 10"
            ),
            PreviewItem(
                "46",
                "手动大小 \\big",
                "\\big( \\frac{1}{2} \\big) \\quad \\big[ x + y \\big] \\quad \\big\\{ a, b \\big\\}"
            ),
            PreviewItem(
                "47",
                "手动大小 \\Big",
                "\\Big( \\frac{a}{b} \\Big) \\quad \\Big[ \\frac{x^2}{y^2} \\Big] \\quad \\Big| x \\Big|"
            ),
            PreviewItem(
                "48",
                "手动大小 \\bigg",
                "\\bigg( \\sum_{i=1}^n x_i \\bigg) \\quad \\bigg\\{ \\frac{a+b}{c+d} \\bigg\\}"
            ),
            PreviewItem(
                "49",
                "手动大小 \\Bigg",
                "\\Bigg[ \\int_0^1 \\frac{dx}{\\sqrt{1-x^2}} \\Bigg] = \\frac{\\pi}{2}"
            ),
            PreviewItem(
                "50",
                "所有手动大小对比",
                "\\big| \\Big| \\bigg| \\Bigg| x \\Bigg| \\bigg| \\Big| \\big|"
            ),
            PreviewItem(
                "51",
                "特殊分隔符",
                "\\left\\langle \\psi \\right\\rangle \\quad \\left\\lfloor x \\right\\rfloor \\quad \\left\\lceil y \\right\\rceil"
            ),
            PreviewItem(
                "52",
                "混合使用",
                "\\Bigg( \\left. \\frac{df}{dx} \\right|_{x=0} + \\Big[ \\sum_{i=1}^n x_i \\Big] \\Bigg)"
            ),
            PreviewItem(
                "53",
                "嵌套不对称",
                "\\left\\{ \\left. x^2 \\right|_{x=1}, \\left. y^2 \\right|_{y=2} \\right\\}"
            ),
            PreviewItem(
                "54",
                "绝对值与范数",
                "\\big| x \\big| \\quad \\Big\\| \\mathbf{v} \\Big\\| \\quad \\left| \\frac{a}{b} \\right|"
            ),
            PreviewItem(
                "55",
                "量子态（狄拉克符号）",
                "\\big\\langle \\psi \\big| \\hat{H} \\big| \\phi \\big\\rangle = E"
            ),
        )
    ),
    PreviewGroup(
        id = "accents",
        title = "8. 装饰符号专题",
        description = "上标装饰、箭头、帽子等",
        items = listOf(
            PreviewItem("56", "简单帽子", "\\hat{x}"),
            PreviewItem("57", "波浪线", "\\tilde{y}"),
            PreviewItem("58", "上划线", "\\overline{AB}"),
            PreviewItem("59", "下划线", "\\underline{text}"),
            PreviewItem("60", "向量箭头", "\\vec{v}"),
            PreviewItem("61", "单点", "\\dot{x}"),
            PreviewItem("62", "双点", "\\ddot{x}"),
            PreviewItem("63", "上大括号", "\\overbrace{a+b+c}"),
            PreviewItem("64", "下大括号", "\\underbrace{x+y+z}"),
            PreviewItem("65", "宽帽子", "\\widehat{ABC}"),
            PreviewItem("66", "右箭头", "\\overrightarrow{AB}"),
            PreviewItem("67", "左箭头", "\\overleftarrow{BA}"),
            PreviewItem(
                "68",
                "复杂装饰组合",
                "\\widehat{ABC} + \\overrightarrow{PQ} + \\overleftarrow{XY}"
            ),
            PreviewItem(
                "69",
                "物理学中的应用",
                "\\vec{F} = m\\vec{a} \\quad \\widehat{H}\\psi = E\\psi"
            ),
        )
    ),
)

// ========== 主界面 ==========

@Composable
fun LatexPreview() {
    var selectedGroup by remember { mutableStateOf<PreviewGroup?>(null) }

    if (selectedGroup == null) {
        PreviewGroupList(
            groups = previewGroups,
            onGroupClick = { selectedGroup = it }
        )
    } else {
        PreviewItemList(
            group = selectedGroup!!,
            onBack = { selectedGroup = null }
        )
    }
}

// ========== 示例列表页 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewItemList(
    group: PreviewGroup,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(group.items) { item ->
                PreviewCard(item.title) {
                    item.content()
                }
            }
        }
    }
}

// ========== 预览卡片 ==========

@Preview
@Composable
fun PreviewAll() {
    LatexPreview()
}
