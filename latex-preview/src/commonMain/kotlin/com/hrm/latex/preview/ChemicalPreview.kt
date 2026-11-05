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
import androidx.compose.ui.tooling.preview.Preview

/**
 * 化学公式预览示例
 * 使用 mhchem 包的 \ce 命令
 */

val chemicalPreviewGroups = listOf(
    PreviewGroup(
        id = "chemical_basic",
        title = "1. Molecules",
        description = "Common molecules and chemical compounds",
        items = listOf(
            PreviewItem("chem1", "Water", "\\ce{H2O}"),
            PreviewItem("chem2", "Carbon dioxide", "\\ce{CO2}"),
            PreviewItem("chem3", "Sulfuric acid", "\\ce{H2SO4}"),
            PreviewItem("chem4", "Ammonia", "\\ce{NH3}"),
            PreviewItem("chem5", "Methane", "\\ce{CH4}"),
        )
    ),
    PreviewGroup(
        id = "chemical_ions",
        title = "2. Ions",
        description = "Positive, negative, and polyatomic ions",
        items = listOf(
            PreviewItem("ion1", "Sodium ion", "\\ce{Na+}"),
            PreviewItem("ion2", "Chloride ion", "\\ce{Cl-}"),
            PreviewItem("ion3", "Iron ion", "\\ce{Fe^{3+}}"),
            PreviewItem("ion4", "Sulfate ion", "\\ce{SO4^{2-}}"),
            PreviewItem("ion5", "Ammonium ion", "\\ce{NH4+}"),
            PreviewItem("ion6", "Permanganate ion", "\\ce{MnO4-}"),
        )
    ),
    PreviewGroup(
        id = "chemical_reactions",
        title = "3. Reactions",
        description = "Balanced chemical equations and ionic reactions",
        items = listOf(
            PreviewItem("rxn1", "Hydrogen combustion", "\\ce{2H2 + O2 -> 2H2O}"),
            PreviewItem("rxn2", "Carbonic acid decomposition", "\\ce{H2CO3 -> H2O + CO2 ^}"),
            PreviewItem("rxn3", "Neutralization", "\\ce{HCl + NaOH -> NaCl + H2O}"),
            PreviewItem("rxn4", "Precipitation", "\\ce{Ba^{2+} + SO4^{2-} -> BaSO4 v}"),
            PreviewItem("rxn5", "Oxidation and reduction", "\\ce{Zn + 2HCl -> ZnCl2 + H2 ^}"),
            PreviewItem(
                "rxn6",
                "Multistep reaction",
                "\\ce{2KMnO4 + 16HCl -> 2KCl + 2MnCl2 + 5Cl2 ^ + 8H2O}"
            ),
        )
    ),
    PreviewGroup(
        id = "chemical_reversible",
        title = "4. Reversible reactions",
        description = "Chemical equilibrium and reversible reactions",
        items = listOf(
            PreviewItem("rev1", "Ammonia synthesis", "\\ce{N2 + 3H2 <-> 2NH3}"),
            PreviewItem("rev2", "Esterification", "\\ce{CH3COOH + C2H5OH <=> CH3COOC2H5 + H2O}"),
            PreviewItem("rev3", "Water ionization", "\\ce{H2O <-> H+ + OH-}"),
            PreviewItem("rev4", "Weak acid ionization", "\\ce{CH3COOH <-> CH3COO- + H+}"),
            PreviewItem("rev5", "Carbon dioxide dissolution", "\\ce{CO2 + H2O <-> H2CO3}"),
            PreviewItem("rev6", "Copper sulfate hydration", "\\ce{CuSO4 + 5H2O <-> CuSO4*5H2O}"),
        )
    ),
    PreviewGroup(
        id = "chemical_complex",
        title = "5. Complex structures",
        description = "Coordination compounds and organic molecules",
        items = listOf(
            PreviewItem("comp1", "Coordination compound", "\\ce{[Cu(NH3)4]^{2+}}"),
            PreviewItem("comp2", "Ethanol", "\\ce{CH3CH2OH}"),
            PreviewItem("comp3", "Acetic acid", "\\ce{CH3COOH}"),
            PreviewItem("comp4", "Glucose", "\\ce{C6H12O6}"),
        )
    ),
    PreviewGroup(
        id = "chemical_annotations_bonds",
        title = "6. Bonds and reaction labels",
        description = "Reaction conditions, catalysts, and single, double, and triple bonds",
        items = listOf(
            PreviewItem("bond1", "Bond types", "\\ce{C-C=C#N}"),
            PreviewItem("bond2", "Labels above and below a reaction arrow", "\\ce{A ->[heat][catalyst] B}"),
            PreviewItem("bond3", "Explicit bonds", "\\ce{C\\bond{#}N}"),
            PreviewItem("bond4", "Physical units", "\\pu{1.2e3 kg.m/s^2}"),
        )
    ),
)

@Preview
@Composable
fun ChemicalPreview(onBack: () -> Unit) {
    PreviewCategoryScreen(
        title = "Chemistry",
        groups = chemicalPreviewGroups,
        onBack = onBack
    )
}
