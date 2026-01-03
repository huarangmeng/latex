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
        id = "chemical_complex",
        title = "4. 复杂结构",
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
