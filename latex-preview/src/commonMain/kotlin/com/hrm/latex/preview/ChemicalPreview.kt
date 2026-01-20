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

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * 化学公式预览示例
 * 使用 mhchem 包的 \ce 命令
 */

val chemicalPreviewGroups = listOf(
    PreviewGroup(
        id = "chemical_basic",
        title = "1. 基础化学式",
        description = "简单分子、化合物",
        items = listOf(
            PreviewItem("chem1", "水分子", "\\ce{H2O}"),
            PreviewItem("chem2", "二氧化碳", "\\ce{CO2}"),
            PreviewItem("chem3", "硫酸", "\\ce{H2SO4}"),
            PreviewItem("chem4", "氨气", "\\ce{NH3}"),
            PreviewItem("chem5", "甲烷", "\\ce{CH4}"),
        )
    ),
    PreviewGroup(
        id = "chemical_ions",
        title = "2. 离子",
        description = "阳离子、阴离子、复杂离子",
        items = listOf(
            PreviewItem("ion1", "钠离子", "\\ce{Na+}"),
            PreviewItem("ion2", "氯离子", "\\ce{Cl-}"),
            PreviewItem("ion3", "铁离子", "\\ce{Fe^{3+}}"),
            PreviewItem("ion4", "硫酸根离子", "\\ce{SO4^{2-}}"),
            PreviewItem("ion5", "铵根离子", "\\ce{NH4+}"),
            PreviewItem("ion6", "高锰酸根", "\\ce{MnO4-}"),
        )
    ),
    PreviewGroup(
        id = "chemical_reactions",
        title = "3. 化学反应",
        description = "化学方程式、离子反应",
        items = listOf(
            PreviewItem("rxn1", "氢气燃烧", "\\ce{2H2 + O2 -> 2H2O}"),
            PreviewItem("rxn2", "碳酸分解", "\\ce{H2CO3 -> H2O + CO2 ^}"),
            PreviewItem("rxn3", "中和反应", "\\ce{HCl + NaOH -> NaCl + H2O}"),
            PreviewItem("rxn4", "沉淀反应", "\\ce{Ba^{2+} + SO4^{2-} -> BaSO4 v}"),
            PreviewItem("rxn5", "氧化还原", "\\ce{Zn + 2HCl -> ZnCl2 + H2 ^}"),
            PreviewItem("rxn6", "复杂反应", "\\ce{2KMnO4 + 16HCl -> 2KCl + 2MnCl2 + 5Cl2 ^ + 8H2O}"),
        )
    ),
    PreviewGroup(
        id = "chemical_reversible",
        title = "4. 可逆反应",
        description = "化学平衡、可逆反应",
        items = listOf(
            PreviewItem("rev1", "氮气与氢气", "\\ce{N2 + 3H2 <-> 2NH3}"),
            PreviewItem("rev2", "酯化反应", "\\ce{CH3COOH + C2H5OH <=> CH3COOC2H5 + H2O}"),
            PreviewItem("rev3", "水的电离", "\\ce{H2O <-> H+ + OH-}"),
            PreviewItem("rev4", "弱酸电离", "\\ce{CH3COOH <-> CH3COO- + H+}"),
            PreviewItem("rev5", "二氧化碳溶解", "\\ce{CO2 + H2O <-> H2CO3}"),
            PreviewItem("rev6", "硫酸铜水合", "\\ce{CuSO4 + 5H2O <-> CuSO4*5H2O}"),
        )
    ),
    PreviewGroup(
        id = "chemical_complex",
        title = "5. 复杂结构",
        description = "配合物、有机物",
        items = listOf(
            PreviewItem("comp1", "配合物", "\\ce{[Cu(NH3)4]^{2+}}"),
            PreviewItem("comp2", "乙醇", "\\ce{CH3CH2OH}"),
            PreviewItem("comp3", "乙酸", "\\ce{CH3COOH}"),
            PreviewItem("comp4", "葡萄糖", "\\ce{C6H12O6}"),
        )
    ),
)

@Preview
@Composable
fun ChemicalPreview(onBack: () -> Unit) {
    PreviewCategoryScreen(
        title = "化学公式",
        groups = chemicalPreviewGroups,
        onBack = onBack
    )
}
