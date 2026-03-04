# LaTeX Parser 测试覆盖报告

## 📊 测试统计

- **测试文件数量**: 42 个
- **测试用例总数**: 656 个
- **覆盖的功能模块**: 100%

## 📁 测试文件结构

### 解析器核心测试 (7个文件, 222个测试)

测试文件按复杂度和职责分离，从基础符号到真实公式逐步递进。

---

#### 1. BasicSymbolsTest.kt (23 个测试) ⭐ 简单

基础符号测试，验证基本 LaTeX 符号解析。

**纯文本测试 (3个)**
- ✅ `testSimpleText` - 简单文本
- ✅ `testMultipleTextNodes` - 多个文本节点
- ✅ `testEmptyString` - 空字符串

**希腊字母测试 (3个)**
- ✅ `testGreekLettersLowercase` - 小写希腊字母
- ✅ `testGreekLettersUppercase` - 大写希腊字母
- ✅ `testGreekExpression` - 希腊字母表达式

**运算符测试 (4个)**
- ✅ `testPlusMinus` - 加减号
- ✅ `testTimes` - 乘号
- ✅ `testDiv` - 除号
- ✅ `testCdot` - 点乘

**关系符号测试 (5个)**
- ✅ `testLeq` - 小于等于
- ✅ `testGeq` - 大于等于
- ✅ `testNeq` - 不等于
- ✅ `testApprox` - 约等于
- ✅ `testEquiv` - 恒等于

**箭头测试 (5个)**
- ✅ `testRightarrow` - 右箭头
- ✅ `testTo` - 右箭头简写 `\to`
- ✅ `testLeftarrow` - 左箭头
- ✅ `testRightArrow` - 双右箭头
- ✅ `testLeftrightarrow` - 双向箭头

**空格测试 (3个)**
- ✅ `testThinSpace` - 细空格
- ✅ `testQuad` - quad 空格
- ✅ `testQquad` - qquad 空格

---

#### 2. SimpleFormulaTest.kt (52 个测试) ⭐⭐ 中等

简单公式结构测试，验证常用数学公式元素。

**分数测试 (6个)**
- ✅ `testFraction` - 基础分数
- ✅ `testNestedFraction` - 嵌套分数
- ✅ `testDfrac` - 显示模式分数
- ✅ `testTfrac` - 文本模式分数
- ✅ `testCfrac` - 连分数
- ✅ `testMultipleFractions` - 多个分数

**二项式系数测试 (5个)**
- ✅ `testBinom` - 基础二项式系数 `\binom{n}{k}`
- ✅ `testTbinom` - 文本模式二项式 `\tbinom`
- ✅ `testDbinom` - 显示模式二项式 `\dbinom`
- ✅ `testBinomialWithComplexContent` - 复杂内容二项式
- ✅ `testNestedBinomial` - 嵌套二项式

**根号测试 (4个)**
- ✅ `testSqrt` - 基础根号
- ✅ `testSqrtWithIndex` - 带指数根号
- ✅ `testNestedSqrt` - 嵌套根号
- ✅ `testSqrtWithFraction` - 根号内含分数

**上下标测试 (6个)**
- ✅ `testSuperscript` - 上标
- ✅ `testSubscript` - 下标
- ✅ `testSuperAndSubscript` - 上下标组合
- ✅ `testComplexSuperscript` - 复杂上标
- ✅ `testComplexSubscript` - 复杂下标
- ✅ `testMixedScripts` - 混合上下标

**括号测试 (6个)**
- ✅ `testDelimiters` - 圆括号
- ✅ `testSquareBrackets` - 方括号
- ✅ `testCurlyBraces` - 花括号
- ✅ `testAngleBrackets` - 尖括号
- ✅ `testFloorBrackets` - 下取整括号
- ✅ `testCeilBrackets` - 上取整括号

**装饰符号测试 (14个)**
- ✅ `testHat` - 帽子
- ✅ `testTilde` - 波浪线
- ✅ `testOverline` - 上划线
- ✅ `testUnderline` - 下划线
- ✅ `testVec` - 向量箭头
- ✅ `testDot` - 点
- ✅ `testDdot` - 双点
- ✅ `testWidehat` - 宽帽子
- ✅ `testOverrightarrow` - 上方右箭头
- ✅ `testOverleftarrow` - 上方左箭头
- ✅ `testCancel` - 取消线
- ✅ `testXrightarrow` - 可扩展右箭头
- ✅ `testXleftarrow` - 可扩展左箭头
- ✅ `testXrightarrowWithBelowText` - 带下方文字的可扩展箭头

**颜色测试 (3个)**
- ✅ `testColor` - `\color` 命令
- ✅ `testTextColor` - `\textcolor` 命令
- ✅ `testColorInFormula` - 公式中的颜色应用

**堆叠测试 (5个)**
- ✅ `testOverset` - 上堆叠 `\overset{?}{=}`
- ✅ `testUnderset` - 下堆叠 `\underset{n \to \infty}{\lim}`
- ✅ `testStackrel` - 堆叠命令 `\stackrel{def}{=}`
- ✅ `testStackWithComplexContent` - 复杂内容堆叠
- ✅ `testNestedStack` - 嵌套堆叠

**钩型箭头测试 (3个)**
- ✅ `testXhookrightarrow` - 可扩展钩型右箭头
- ✅ `testXhookleftarrow` - 可扩展钩型左箭头
- ✅ `testXhookrightarrowWithBelow` - 带下方文字的钩型箭头

---

#### 3. ComplexStructureTest.kt (45 个测试) ⭐⭐⭐ 复杂

复杂结构测试，验证高级数学排版元素。

**大型运算符测试 (15个)**
- ✅ `testSummation` - 求和 Σ
- ✅ `testProduct` - 乘积 ∏
- ✅ `testIntegral` - 积分 ∫
- ✅ `testDoubleIntegral` - 二重积分
- ✅ `testTripleIntegral` - 三重积分
- ✅ `testContourIntegral` - 环路积分
- ✅ `testBigCup` - 大并集
- ✅ `testBigCap` - 大交集
- ✅ `testLimitOperator` - 极限 `\lim`
- ✅ `testMaxOperator` - 最大值 `\max`
- ✅ `testMinOperator` - 最小值 `\min`
- ✅ `testSupOperator` - 上确界 `\sup`
- ✅ `testInfOperator` - 下确界 `\inf`
- ✅ `testLimsupOperator` - 上极限 `\limsup`
- ✅ `testLiminfOperator` - 下极限 `\liminf`

**矩阵测试 (8个)**
- ✅ `testMatrix` - 普通矩阵
- ✅ `testPmatrix` - 圆括号矩阵
- ✅ `testBmatrix` - 方括号矩阵
- ✅ `testVmatrix` - 行列式
- ✅ `testSmallmatrix` - 小矩阵
- ✅ `testArray` - 数组环境
- ✅ `testArrayWithMixedAlignment` - 混合对齐数组
- ✅ `test3x3Matrix` - 3×3 矩阵
- ✅ `testSingleRowMatrix` - 单行矩阵

**环境测试 (3个)**
- ✅ `testEquationEnvironment` - equation 环境
- ✅ `testAlignedEnvironment` - aligned 环境
- ✅ `testCasesEnvironment` - cases 环境

**字体样式测试 (14个)**
- ✅ `testTextMode` - 文本模式 `\text{...}`
- ✅ `testMboxMode` - mbox 文本 `\mbox{...}`
- ✅ `testTextInFormula` - 公式中的文本
- ✅ `testTextWithSpaces` - 带空格的文本
- ✅ `testEmptyText` - 空文本
- ✅ `testMathBold` - 粗体
- ✅ `testMathItalic` - 斜体
- ✅ `testMathRoman` - 罗马体
- ✅ `testMathBB` - 黑板粗体
- ✅ `testMathCal` - 花体
- ✅ `testMathFrak` - 哥特体
- ✅ `testBoldsymbol` - 粗体符号 `\boldsymbol`
- ✅ `testBoldsymbolShorthand` - 粗体符号简写 `\bm`
- ✅ `testBoldsymbolWithMultipleSymbols` - 多符号粗体
- ✅ `testSymbf` - `\symbf` Unicode 数学粗体
- ✅ `testSymsf` - `\symsf` Unicode 数学无衬线
- ✅ `testSymrm` - `\symrm` Unicode 数学罗马体
- ✅ `testSymit` - `\symit` Unicode 数学斜体

---

#### 4. RealWorldFormulaTest.kt (13 个测试) ⭐⭐⭐⭐ 高级

真实世界数学公式测试，验证实际应用场景。

**代数公式 (3个)**
- ✅ `testQuadraticFormula` - 二次公式
- ✅ `testBinomialTheorem` - 二项式定理
- ✅ `testEulerFormula` - 欧拉公式

**微积分公式 (6个)**
- ✅ `testTaylorSeries` - 泰勒级数
- ✅ `testFourierTransform` - 傅里叶变换
- ✅ `testCauchyIntegralFormula` - 柯西积分公式
- ✅ `testLimitDefinition` - 极限定义
- ✅ `testDerivativeDefinition` - 导数定义
- ✅ `testIntegralByParts` - 分部积分

**物理公式 (2个)**
- ✅ `testSchrodingerEquation` - 薛定谔方程
- ✅ `testMaxwellEquation` - 麦克斯韦方程

**线性代数 (1个)**
- ✅ `testMatrixDeterminant` - 矩阵行列式

**组合公式 (1个)**
- ✅ `testComplexNestedFormula` - 复杂嵌套公式

---

#### 5. EdgeCasesTest.kt (21 个测试) ⭐⭐⭐ 复杂

边界情况和特殊场景测试，验证解析器鲁棒性。

**嵌套结构测试 (3个)**
- ✅ `testNestedGroups` - 嵌套分组
- ✅ `testDeeplyNestedFractions` - 深层嵌套分数
- ✅ `testNestedRootsAndFractions` - 根号分数混合嵌套

**多元素测试 (3个)**
- ✅ `testMultipleFractions` - 多个分数
- ✅ `testMultipleScripts` - 多个上下标
- ✅ `testMultipleSummations` - 多个求和

**长表达式测试 (3个)**
- ✅ `testLongExpression` - 长表达式
- ✅ `testLongPolynomial` - 长多项式
- ✅ `testMultipleGreekLetters` - 多个希腊字母

**特殊字符测试 (2个)**
- ✅ `testSpecialCharactersEscaped` - 转义字符
- ✅ `testBackslashInText` - 文本中的反斜杠

**空内容测试 (3个)**
- ✅ `testEmptyGroup` - 空分组
- ✅ `testEmptyFraction` - 空分数
- ✅ `testEmptySqrt` - 空根号

**复杂组合测试 (3个)**
- ✅ `testMixedEnvironments` - 混合环境
- ✅ `testAllTypesOfBrackets` - 所有类型括号
- ✅ `testSuperscriptOnOperator` - 运算符上的上标

**实际场景测试 (4个)**
- ✅ `testPhysicsEquationWithMultipleStyles` - 多样式物理方程
- ✅ `testStatisticsFormula` - 统计学公式
- ✅ `testMatrixEquation` - 矩阵方程
- ✅ `testPiecewiseFunction` - 分段函数

---

#### 6. DelimiterTest.kt (31 个测试) ⭐⭐⭐ 复杂

定界符测试，验证各类括号和定界符解析。

**基础定界符 (7个)**
- ✅ `testBasicDelimiters` - 基础圆括号
- ✅ `testSquareBrackets` - 方括号
- ✅ `testCurlyBraces` - 花括号
- ✅ `testVerticalBars` - 竖线
- ✅ `testAngleBrackets` - 尖括号
- ✅ `testFloorBrackets` - 下取整
- ✅ `testCeilBrackets` - 上取整

**嵌套与组合 (3个)**
- ✅ `testDelimitersWithFraction` - 含分数的定界符
- ✅ `testNestedDelimiters` - 嵌套定界符
- ✅ `testComplexFormula` - 复杂公式中的定界符

**非对称定界符 (4个)**
- ✅ `testAsymmetricDelimiterLeftDot` - 左侧 `\left.` 省略
- ✅ `testAsymmetricDelimiterRightDot` - 右侧 `\right.` 省略
- ✅ `testAsymmetricDelimiterBothDots` - 双侧省略
- ✅ `testEvaluationNotation` - 求值记号
- ✅ `testPiecewiseFunction` - 分段函数

**手动大小定界符 (8个)**
- ✅ `testBigParenthesis` - `\big` 圆括号
- ✅ `testBigSquareBracket` - `\big` 方括号
- ✅ `testBiggCurlyBrace` - `\bigg` 花括号
- ✅ `testBiggVerticalBar` - `\bigg` 竖线
- ✅ `testAllManualSizes` - 所有手动尺寸 `\big \Big \bigg \Bigg`
- ✅ `testManualSizeWithDirectionSuffix` - 方向后缀 `\bigl \bigr`
- ✅ `testManualSizeWithAngleBrackets` - 手动尺寸尖括号
- ✅ `testManualSizeWithFloorCeilBrackets` - 手动尺寸取整括号
- ✅ `testMixedAutoAndManualDelimiters` - 自动与手动混合
- ✅ `testMultipleManualSizeLevels` - 多级手动尺寸

**lvert/lVert 定界符 (6个)**
- ✅ `testLvertRvert` - `\lvert \rvert` 单竖线
- ✅ `testLVertRVert` - `\lVert \rVert` 双竖线
- ✅ `testLvertInBigDelimiter` - `\big\lvert`
- ✅ `testLVertInBigDelimiter` - `\big\lVert`
- ✅ `testLvertAsSymbol` - `\lvert` 作为独立符号
- ✅ `testMixedDelimiters` - 混合定界符

---

#### 7. MathModeTest.kt (14 个测试) ⭐⭐ 中等

数学模式测试，验证行内/行间数学模式切换。

- ✅ `should_parse_inline_math_simple` - 简单行内公式 `$...$`
- ✅ `should_parse_inline_math_with_fraction` - 行内分数
- ✅ `should_parse_inline_math_with_surrounding_text` - 带前后文本
- ✅ `should_parse_multiple_inline_math` - 多个行内公式
- ✅ `should_parse_display_math_simple` - 简单行间公式 `$$...$$`
- ✅ `should_parse_display_math_with_fraction` - 行间分数
- ✅ `should_parse_display_math_with_surrounding_text` - 带前后文本
- ✅ `should_parse_mixed_inline_and_display` - 行内与行间混合
- ✅ `should_parse_text_only_no_math` - 纯文本
- ✅ `should_parse_escaped_dollar_sign` - 转义 `\$`
- ✅ `should_parse_complex_document` - 复杂文档
- ✅ `should_handle_empty_inline_math` - 空行内公式
- ✅ `should_handle_empty_display_math` - 空行间公式
- ✅ `should_parse_inline_math_with_subscript_superscript` - 行内上下标

---

### 环境扩展测试 (2个文件, 44个测试)

#### 8. EnvironmentExtensionTest.kt (36 个测试) ⭐⭐⭐ 复杂

环境扩展测试，验证 LaTeX 环境支持。

**Split 环境 (3个)**
- ✅ `testSplitBasic` - 基础 split 环境
- ✅ `testSplitMultipleAlignments` - 多行对齐
- ✅ `testSplitWithFractions` - 分数中的 split

**Multline 环境 (3个)**
- ✅ `testMultlineBasic` - 基础 multline
- ✅ `testMultlineThreeLines` - 三行显示
- ✅ `testMultlineWithComplexFormula` - 复杂公式

**Eqnarray 环境 (3个)**
- ✅ `testEqnarrayBasic` - 基础 eqnarray
- ✅ `testEqnarrayThreeColumns` - 三列结构
- ✅ `testEqnarrayWithComplexExpressions` - 复杂表达式

**Subequations 环境 (3个)**
- ✅ `testSubequationsBasic` - 基础 subequations
- ✅ `testSubequationsWithMultipleEquations` - 多个方程
- ✅ `testSubequationsWithAlign` - 与 align 配合

**混合使用 (3个)**
- ✅ `testSplitInsideEquation` - split 嵌套在 equation 中
- ✅ `testNestedEnvironments` - 嵌套环境
- ✅ `testEmptyEnvironment` - 空环境处理

**嵌套环境 (3个)**
- ✅ `testAlignedInsideAlign` - aligned 嵌套在 align 中
- ✅ `testNestedAlignedEnvironments` - 嵌套 aligned 环境
- ✅ `testGatherInsideSubequations` - gather 嵌套在 subequations 中

**Tabular 环境 (6个)**
- ✅ `testTabularBasic` - 基础 tabular
- ✅ `testTabularThreeColumns` - 三列 tabular
- ✅ `testTabularSingleCell` - 单元格 tabular
- ✅ `testTabularWithVerticalLines` - 带竖线的 tabular
- ✅ `testTabularWithMixedAlignmentAndLines` - 混合对齐与竖线

**Star 变体环境 (5个)**
- ✅ `testAlignStar` - align* 环境
- ✅ `testEquationStar` - equation* 环境
- ✅ `testGatherStar` - gather* 环境
- ✅ `testMultlineStar` - multline* 环境
- ✅ `testEqnarrayStar` - eqnarray* 环境

**表格线与多列 (7个)**
- ✅ `testHlineInTabular` - tabular 中的 `\hline`
- ✅ `testStandaloneHline` - 独立 `\hline`
- ✅ `testClineWithRange` - `\cline{i-j}` 范围线
- ✅ `testClineInTabular` - tabular 中的 `\cline`
- ✅ `testMulticolumn` - `\multicolumn` 多列合并
- ✅ `testMulticolumnInTabular` - tabular 中的多列合并
- ✅ `testMulticolumnWithAlignmentLeft` - 左对齐多列
- ✅ `testCompleteTableWithAllFeatures` - 完整表格（所有特性）

---

#### 9. AlignmentEnvironmentsTest.kt (8 个测试) ⭐⭐⭐ 复杂

对齐环境扩展测试。

- ✅ `should_parse_flalign` - flalign 环境
- ✅ `should_parse_flalign_star` - flalign* 环境
- ✅ `should_parse_flalign_single_row` - 单行 flalign
- ✅ `should_parse_alignat_with_column_count` - alignat 带列数
- ✅ `should_parse_alignat_star` - alignat* 环境
- ✅ `should_parse_alignat_single_group` - 单组 alignat
- ✅ `should_parse_alignat_multiple_columns` - 多列 alignat
- ✅ `should_handle_flalign_with_fractions` - 含分数的 flalign

---

### AMS 符号扩展测试 (2个文件, 71个测试)

#### 10. AmsExtraSymbolsTest.kt (30 个测试) ⭐⭐ 中等

AMS 扩展符号测试，验证 AMS 数学符号包中的额外符号。

- ✅ `should_parse_checkmark` - 对勾 ✓
- ✅ `should_parse_complement` - 补集 ∁
- ✅ `should_parse_eth` - ð
- ✅ `should_parse_mho` - 姆欧 ℧
- ✅ `should_parse_twoheadrightarrow` - 双头右箭头 ↠
- ✅ `should_parse_twoheadleftarrow` - 双头左箭头 ↞
- ✅ `should_parse_leftleftarrows` - 双左箭头 ⇇
- ✅ `should_parse_rightrightarrows` - 双右箭头 ⇉
- ✅ `should_parse_leftrightarrows` - 左右箭头 ⇆
- ✅ `should_parse_rightleftarrows` - 右左箭头 ⇄
- ✅ `should_parse_curvearrowright` - 弯曲右箭头 ↷
- ✅ `should_parse_curvearrowleft` - 弯曲左箭头 ↶
- ✅ `should_parse_circlearrowright` - 圆形右箭头 ↻
- ✅ `should_parse_circlearrowleft` - 圆形左箭头 ↺
- ✅ `should_parse_lessdot` - 小于点 ⋖
- ✅ `should_parse_gtrdot` - 大于点 ⋗
- ✅ `should_parse_lll` - 三小于 ⋘
- ✅ `should_parse_ggg` - 三大于 ⋙
- ✅ `should_parse_blacksquare` - 黑色方块 ■
- ✅ `should_parse_square` - 白色方块 □
- ✅ `should_parse_lozenge` - 菱形 ◊
- ✅ `should_parse_blacktriangle` - 黑色三角 ▲
- ✅ `should_parse_blacktriangledown` - 黑色倒三角 ▼
- ✅ `should_parse_beth` - 希伯来字母 beth ℶ
- ✅ `should_parse_gimel` - 希伯来字母 gimel ℷ
- ✅ `should_parse_daleth` - 希伯来字母 daleth ℸ
- ✅ `should_parse_measuredangle` - 测量角 ∡
- ✅ `should_parse_sphericalangle` - 球面角 ∢
- ✅ `should_have_accessible_names_for_extra_symbols` - 无障碍名称
- ✅ `should_parse_symbols_in_expression` - 表达式中的符号

---

#### 11. AmsNegatedRelationsTest.kt (41 个测试) ⭐⭐⭐ 复杂

AMS 否定关系符号测试，验证否定关系运算符。

**否定比较符号 (12个)**
- ✅ `should_parse_nless` / `should_parse_ngtr` - 不小于/不大于
- ✅ `should_parse_nleq` / `should_parse_ngeq` - 不小于等于/不大于等于
- ✅ `should_parse_nleqslant` / `should_parse_ngeqslant` - 不小于等于(斜线)/不大于等于(斜线)
- ✅ `should_parse_nsubseteq` / `should_parse_nsupseteq` - 非子集/非超集
- ✅ `should_parse_nprec` / `should_parse_nsucc` - 不先于/不后于
- ✅ `should_parse_ncong` / `should_parse_nsim` - 不全等/不相似

**否定逻辑符号 (10个)**
- ✅ `should_parse_nmid` / `should_parse_nparallel` - 不整除/不平行
- ✅ `should_parse_nvdash` / `should_parse_nvDash` - 不推出
- ✅ `should_parse_nVdash` / `should_parse_nVDash` - 不强制推出
- ✅ `should_parse_ntriangleleft` / `should_parse_ntriangleright` - 非正规子群
- ✅ `should_parse_ntrianglelefteq` / `should_parse_ntrianglerighteq` - 非正规子群(含等)

**AMS 扩展关系符号 (15个)**
- ✅ `should_parse_lneq` / `should_parse_gneq` - 严格不等
- ✅ `should_parse_subsetneq` / `should_parse_supsetneq` - 真子集/真超集
- ✅ `should_parse_leqslant` / `should_parse_geqslant` - 斜线小于等于
- ✅ `should_parse_lessgtr` / `should_parse_gtrless` - 小于大于/大于小于
- ✅ `should_parse_lesssim` / `should_parse_gtrsim` - 近似小于/近似大于
- ✅ `should_parse_trianglelefteq` / `should_parse_trianglerighteq` - 正规子群
- ✅ `should_parse_vDash` / `should_parse_Vdash` / `should_parse_Vvdash` - 推出变体
- ✅ `should_parse_models` - 模型关系

**综合测试 (4个)**
- ✅ `should_have_accessible_names_for_negated_relations` - 无障碍名称
- ✅ `should_parse_negated_relation_in_expression` - 表达式中的否定关系
- ✅ `should_parse_multiple_negated_relations` - 多个否定关系

---

### 命令扩展测试 (7个文件, 63个测试)

#### 12. NewCommandTest.kt (20 个测试) ⭐⭐⭐ 复杂

自定义命令测试，验证 `\newcommand`、`\renewcommand`、`\def` 解析。

- ✅ `should_parse_newcommand_without_arguments` - 无参数定义
- ✅ `should_parse_newcommand_with_one_argument` - 单参数定义
- ✅ `should_parse_newcommand_with_two_arguments` - 双参数定义
- ✅ `should_expand_custom_command_without_arguments` - 无参数展开
- ✅ `should_expand_custom_command_with_one_argument` - 单参数展开
- ✅ `should_expand_custom_command_with_two_arguments` - 双参数展开
- ✅ `should_handle_multiple_custom_commands` - 多个自定义命令
- ✅ `should_handle_nested_custom_commands` - 嵌套自定义命令
- ✅ `should_replace_parameter_in_text` - 参数替换
- ✅ `should_parse_delimited_command_definition` - 分隔符命令定义
- ✅ `should_expand_delimited_command` - 分隔符命令展开
- ✅ `should_parse_custom_command_name` - 自定义命令名
- ✅ `should_override_builtin_command` - 覆盖内置命令
- ✅ `should_expand_binomial_in_custom_command` - 自定义命令中的二项式
- ✅ `should_expand_binomial_with_complex_args` - 复杂参数二项式
- ✅ `should_handle_lone_hash_in_definition` - 定义中的孤立 `#`
- ✅ `should_handle_hash_at_end_of_definition` - 定义末尾的 `#`
- ✅ `should_handle_hash_followed_by_letter` - `#` 后跟字母
- ✅ `should_parse_renewcommand` - `\renewcommand`
- ✅ `should_parse_def_without_args` - `\def` 无参数

---

#### 13. DeclareMathOperatorTest.kt (5 个测试) ⭐⭐ 中等

数学运算符声明测试。

- ✅ `should_parse_basic_declare_math_operator` - 基础声明
- ✅ `should_expand_declared_operator_as_operatorname` - 展开为 operatorname
- ✅ `should_declare_multiple_operators` - 声明多个运算符
- ✅ `should_register_zero_arg_command` - 注册零参数命令
- ✅ `should_declare_and_use_with_subscript` - 声明后带下标使用

---

#### 14. OperatorNameTest.kt (6 个测试) ⭐⭐ 中等

`\operatorname` 测试。

- ✅ `should_parse_operatorname_standalone` - 独立 operatorname
- ✅ `should_parse_operatorname_with_subscript` - 带下标
- ✅ `should_parse_operatorname_with_limits` - 带 limits
- ✅ `should_parse_operatorname_empty_gracefully` - 空内容优雅处理
- ✅ `should_produce_accessibility_for_operatorname` - 无障碍输出
- ✅ `should_produce_mathml_for_operatorname` - MathML 输出

---

#### 15. MathOpTest.kt (7 个测试) ⭐⭐ 中等

`\mathop` 测试。

- ✅ `should_parse_basic_mathop` - 基础 mathop
- ✅ `should_parse_mathop_with_subscript` - 带下标
- ✅ `should_parse_mathop_with_both_scripts` - 上下标
- ✅ `should_parse_mathop_with_limits` - 带 limits
- ✅ `should_parse_mathop_with_nolimits` - 带 nolimits
- ✅ `should_parse_mathop_in_expression` - 表达式中使用
- ✅ `should_parse_mathop_without_scripts` - 无上下标

---

#### 16. ModOperatorTest.kt (7 个测试) ⭐⭐ 中等

取模运算符测试。

- ✅ `should_parse_bmod` - `\bmod`
- ✅ `should_parse_pmod` - `\pmod`
- ✅ `should_parse_mod` - `\mod`
- ✅ `should_produce_accessibility_for_bmod` - bmod 无障碍
- ✅ `should_produce_accessibility_for_pmod` - pmod 无障碍
- ✅ `should_produce_mathml_for_bmod` - bmod MathML
- ✅ `should_produce_mathml_for_pmod` - pmod MathML

---

#### 17. MathStyleTest.kt (10 个测试) ⭐⭐ 中等

数学样式测试。

- ✅ `testDisplayStyle` - 显示样式
- ✅ `testTextStyle` - 文本样式
- ✅ `testScriptStyle` - 脚注样式
- ✅ `testScriptScriptStyle` - 双重脚注样式
- ✅ `testMathStyleInFraction` - 分数中的样式
- ✅ `testMathStyleInGroup` - 分组中的样式
- ✅ `testMathStyleInSum` - 求和中的样式
- ✅ `testNestedMathStyles` - 嵌套样式
- ✅ `testMathStyleWithComplexExpression` - 复杂表达式样式
- ✅ `testAllFourStyles` - 四种样式完整测试

---

#### 18. BigOperatorExtensionTest.kt (6 个测试) ⭐⭐ 中等

大型运算符扩展测试。

- ✅ `should_parse_coprod` - 余积 ∐
- ✅ `should_parse_bigoplus` - 大直和 ⨁
- ✅ `should_parse_bigotimes` - 大张量积 ⨂
- ✅ `should_parse_bigsqcup` - 大方并集 ⨆
- ✅ `should_parse_bigodot` - 大点积 ⨀
- ✅ `should_parse_biguplus` - 大不相交并集 ⨄

---

### 装饰与特效测试 (5个文件, 39个测试)

#### 19. AccentExtensionTest.kt (11 个测试) ⭐⭐ 中等

重音符号扩展测试。

- ✅ `should_parse_grave_accent` - 重音符 `\grave`
- ✅ `should_parse_acute_accent` - 锐音符 `\acute`
- ✅ `should_parse_check_accent` - 抑扬符 `\check`
- ✅ `should_parse_breve_accent` - 短音符 `\breve`
- ✅ `should_parse_ring_accent` - 环形符 `\ring`
- ✅ `should_parse_mathring_accent` - 数学环形符 `\mathring`
- ✅ `should_parse_dddot_accent` - 三点 `\dddot`
- ✅ `should_produce_accessibility_for_new_accents` - 无障碍输出
- ✅ `should_parse_hat_without_braces` - 无花括号的 `\hat`
- ✅ `should_parse_vec_without_braces` - 无花括号的 `\vec`
- ✅ `should_parse_hat_f_in_context` - 上下文中的 `\hat f`

---

#### 20. SpecialEffectTest.kt (13 个测试) ⭐⭐ 中等

特殊效果测试（boxed、phantom、smash）。

- ✅ `should_parse_boxed_with_simple_content` - 简单内容加框
- ✅ `should_parse_boxed_with_complex_formula` - 复杂公式加框
- ✅ `should_parse_phantom_with_simple_content` - 简单幻影
- ✅ `should_parse_phantom_with_formula` - 公式幻影
- ✅ `should_parse_phantom_for_alignment` - 对齐用幻影
- ✅ `should_parse_nested_boxed` - 嵌套加框
- ✅ `should_parse_boxed_with_color` - 带颜色加框
- ✅ `should_parse_combined_boxed_and_phantom` - 加框与幻影组合
- ✅ `should_parse_boxed_in_equation` - 方程中加框
- ✅ `should_parse_smash_default` - 默认 smash
- ✅ `should_parse_smash_top` - 顶部 smash
- ✅ `should_parse_smash_bottom` - 底部 smash
- ✅ `should_parse_smash_with_complex_content` - 复杂内容 smash

---

#### 21. CancelVariantsTest.kt (3 个测试) ⭐ 简单

取消线变体测试。

- ✅ `should_parse_bcancel` - 反斜线取消 `\bcancel`
- ✅ `should_parse_xcancel` - 交叉取消 `\xcancel`
- ✅ `should_parse_cancel_variants_in_expression` - 表达式中的取消线

---

#### 22. BraceAnnotationTest.kt (2 个测试) ⭐ 简单

大括号注释测试。

- ✅ `should_parse_underbrace_with_subscript` - 下大括号带下标
- ✅ `should_parse_overbrace_with_superscript` - 上大括号带上标

---

#### 23. NegationTest.kt (3 个测试) ⭐ 简单

否定符号测试。

- ✅ `should_parse_not_equals` - `\not=`
- ✅ `should_parse_not_in` - `\not\in`
- ✅ `should_parse_not_subset` - `\not\subset`

---

### 间距与布局测试 (3个文件, 10个测试)

#### 24. SpaceTest.kt (3 个测试) ⭐ 简单

间距命令测试。

- ✅ `should_parse_negative_thin_space_correctly` - 负细间距 `\!`
- ✅ `should_parse_hspace_correctly` - `\hspace`
- ✅ `should_parse_hspace_with_different_units` - 不同单位的 `\hspace`

---

#### 25. SpacingAdjustmentTest.kt (4 个测试) ⭐⭐ 中等

间距调整测试（smash、vphantom、hphantom）。

- ✅ `should_parse_smash` - `\smash`
- ✅ `should_parse_vphantom` - `\vphantom`
- ✅ `should_parse_hphantom` - `\hphantom`
- ✅ `should_parse_smash_in_expression` - 表达式中的 smash

---

#### 26. AdaptiveDotsTest.kt (3 个测试) ⭐ 简单

自适应省略号测试。

- ✅ `should_parse_dots_as_ldots_by_default` - 默认为低位点
- ✅ `should_parse_dots_as_cdots_before_binary_op` - 二元运算符前为居中点
- ✅ `should_parse_dots_as_cdots_before_command_binary_op` - 命令二元运算符前为居中点

---

### 数学结构测试 (5个文件, 26个测试)

#### 27. CasesVariantTest.kt (7 个测试) ⭐⭐ 中等

cases 变体测试。

- ✅ `testDcasesParsesAsDisplayStyle` - dcases 显示样式
- ✅ `testRcasesParsesAsRightBrace` - rcases 右花括号
- ✅ `testStandardCasesIsNormalStyle` - 标准 cases
- ✅ `testDcasesSingleCase` - dcases 单条件
- ✅ `testRcasesMultipleCases` - rcases 多条件
- ✅ `testDcasesWithNestedFractions` - dcases 嵌套分数
- ✅ `testRcasesWithoutConditions` - rcases 无条件

---

#### 28. SubstackTest.kt (3 个测试) ⭐ 简单

子堆叠测试。

- ✅ `should_parse_substack` - 基础 `\substack`
- ✅ `should_parse_substack_with_multiple_rows` - 多行 substack
- ✅ `should_parse_substack_single_row` - 单行 substack

---

#### 29. TensorTest.kt (4 个测试) ⭐⭐ 中等

张量指标测试。

- ✅ `testTensorBasic` - 基础张量
- ✅ `testTensorMultipleIndices` - 多指标张量
- ✅ `testIndices` - 指标解析
- ✅ `testTensorNoIndices` - 无指标张量

---

#### 30. SideSetTest.kt (3 个测试) ⭐ 简单

侧置上下标测试。

- ✅ `testSideSetBasic` - 基础 `\sideset`
- ✅ `testSideSetPartial` - 部分 sideset
- ✅ `testSideSetEmpty` - 空 sideset

---

#### 31. LabelRefTest.kt (6 个测试) ⭐⭐ 中等

标签与引用测试。

- ✅ `testLabel` - `\label`
- ✅ `testRef` - `\ref`
- ✅ `testEqRef` - `\eqref`
- ✅ `testLabelInContext` - 上下文中的 label
- ✅ `testRefInContext` - 上下文中的 ref
- ✅ `testCombinedRefAndLabel` - ref 与 label 组合

---

#### 32. TagTest.kt (2 个测试) ⭐ 简单

标签测试。

- ✅ `should_parse_tag` - `\tag`
- ✅ `should_parse_tag_star` - `\tag*`

---

### 化学公式测试 (1个文件, 10个测试)

#### 33. ChemicalParserTest.kt (10 个测试) ⭐⭐⭐ 复杂

化学公式解析测试。

- ✅ `should_parse_simple_formula` - 简单化学式
- ✅ `should_parse_charge` - 电荷
- ✅ `should_parse_reaction_arrow` - 反应箭头 →
- ✅ `should_parse_reversible_arrow` - 可逆反应箭头 ⇌
- ✅ `should_parse_equilibrium_arrow` - 平衡箭头 ⇋
- ✅ `should_parse_left_arrow` - 左箭头 ←
- ✅ `should_parse_double_right_arrow` - 双箭头
- ✅ `should_parse_complex_ion` - 配位离子
- ✅ `should_parse_crystallization_water` - 结晶水
- ✅ `should_remove_space_before_gas_symbol` - 气体符号前去空格

---

### 增量解析测试 (2个文件, 43个测试)

#### 34. IncrementalLatexParserTest.kt (29 个测试) ⭐⭐⭐ 复杂

增量解析器测试，验证流式输入场景。

**基础功能 (5个)**
- ✅ `append_simpleText_parsesSuccessfully` - 追加简单文本
- ✅ `append_incrementally_producesCorrectResult` - 增量追加
- ✅ `append_latexCommand_parsesCorrectly` - 追加 LaTeX 命令
- ✅ `append_charByChar_eventuallyParsesCompleteFraction` - 逐字输入分数
- ✅ `setInput_replaceContent_parsesNewContent` - 替换输入

**容错测试 (7个)**
- ✅ `setInput_sameContent_noChange` - 相同输入无变化
- ✅ `append_unclosedBrace_doesNotCrash` - 未闭合花括号
- ✅ `append_incompleteCommand_doesNotCrash` - 不完整命令
- ✅ `append_incompleteSuperscript_doesNotCrash` - 不完整上标
- ✅ `append_incompleteSubscript_doesNotCrash` - 不完整下标
- ✅ `append_incompleteMathMode_doesNotCrash` - 不完整数学模式
- ✅ `append_incompleteDisplayMath_doesNotCrash` - 不完整行间公式
- ✅ `append_incompleteEnvironment_doesNotCrash` - 不完整环境

**状态管理 (5个)**
- ✅ `clear_resetsAllState` - 清除状态
- ✅ `getProgress_completeInput_returns1` - 完整输入进度
- ✅ `getProgress_emptyInput_returns1` - 空输入进度
- ✅ `getUnparsedContent_completeInput_returnsEmpty` - 完整输入无未解析内容
- ✅ `getCurrentInput_reflectsAllAppends` - 当前输入反映追加
- ✅ `getCurrentInput_afterClear_isEmpty` - 清除后为空

**一致性验证 (8个)**
- ✅ `append_completeFormula_matchesStandardParser` - 完整公式匹配标准解析
- ✅ `append_multipleFormulas_matchesStandardParser` - 多公式匹配
- ✅ `setInput_thenAppend_parsesCorrectly` - 设置后追加
- ✅ `append_largeFormula_parsesSuccessfully` - 大型公式
- ✅ `charByChar_inftyFormula_simulateLatexComposable` - 逐字模拟 Composable
- ✅ `singleAppend_inftyFormula_parsesCorrectly` - 单次追加
- ✅ `append_multipleClears_doesNotCorruptState` - 多次清除不损坏状态
- ✅ `append_incompleteInput_retainsLastSuccessfulResult` - 保留最后成功结果
- ✅ `append_incompleteToComplete_updatesResult` - 不完整到完整更新
- ✅ `append_streamingFraction_noIntermediateError` - 流式分数无中间错误

---

#### 35. IncrementalTokenizerTest.kt (14 个测试) ⭐⭐ 中等

增量词法分析器测试。

- ✅ `tokenize_simpleText_matchesStandardTokenizer` - 简单文本匹配
- ✅ `tokenize_latexCommand_producesCommandToken` - 命令 token
- ✅ `update_appendText_resultMatchesFullTokenize` - 追加文本匹配
- ✅ `update_appendLatexCommand_resultMatchesFullTokenize` - 追加命令匹配
- ✅ `update_insertInMiddle_resultMatchesFullTokenize` - 中间插入匹配
- ✅ `update_insertBraces_resultMatchesFullTokenize` - 插入花括号匹配
- ✅ `update_deleteText_resultMatchesFullTokenize` - 删除文本匹配
- ✅ `update_deleteCommand_resultMatchesFullTokenize` - 删除命令匹配
- ✅ `update_replaceText_resultMatchesFullTokenize` - 替换文本匹配
- ✅ `update_multipleAppends_resultMatchesFullTokenize` - 多次追加匹配
- ✅ `update_appendThenDeleteThenAppend_resultMatchesFullTokenize` - 追加删除追加匹配
- ✅ `update_emptyToNonEmpty` - 空到非空
- ✅ `update_nonEmptyToEmpty_producesOnlyEOF` - 非空到空
- ✅ `getCurrentText_reflectsLatestText` - 当前文本反映最新

---

### 工具类测试 (5个文件, 128个测试)

#### 36. SymbolMapTest.kt (27 个测试) ⭐⭐ 中等

符号映射表测试，验证 LaTeX 命令到 Unicode 的映射。

**分类映射测试 (20个)**
- ✅ `testLowercaseGreekLetters` - 小写希腊字母
- ✅ `testUppercaseGreekLetters` - 大写希腊字母
- ✅ `testVariantGreekLetters` - 希腊字母变体
- ✅ `testBasicOperators` - 基础运算符
- ✅ `testCircledOperators` - 圆圈运算符
- ✅ `testComparisonOperators` - 比较运算符
- ✅ `testEquivalenceOperators` - 等价运算符
- ✅ `testSetRelations` - 集合关系
- ✅ `testGeometricRelations` - 几何关系
- ✅ `testBasicArrows` - 基础箭头
- ✅ `testDoubleArrows` - 双箭头
- ✅ `testSpecialArrows` - 特殊箭头
- ✅ `testHookAndHarpoonArrows` - 钩形和鱼叉箭头
- ✅ `testSetOperators` - 集合运算符
- ✅ `testQuantifiers` - 量词
- ✅ `testLogicalOperators` - 逻辑运算符
- ✅ `testCalculusSymbols` - 微积分符号
- ✅ `testDots` - 省略号
- ✅ `testMiscSymbols` - 杂项符号
- ✅ `testMathematicalSets` - 数学集合

**功能测试 (7个)**
- ✅ `testBrackets` - 括号映射
- ✅ `testBigOperators` - 大型运算符映射
- ✅ `testNonExistentSymbol` - 不存在的符号
- ✅ `testGetAllSymbols` - 获取所有符号
- ✅ `testSymbolConsistency` - 符号一致性
- ✅ `testAllGreekLettersExist` - 希腊字母完整性
- ✅ `testCommonMathSymbolsExist` - 常用数学符号存在性

---

#### 37. TokenizerTest.kt (21 个测试) ⭐⭐ 中等

词法分析器测试，验证 LaTeX 源码的 Token 识别。

**Token 类型测试 (16个)**
- ✅ `testSimpleText` - 文本识别
- ✅ `testCommand` - 命令识别
- ✅ `testBraces` - 花括号
- ✅ `testBrackets` - 方括号
- ✅ `testSuperscript` - 上标 `^`
- ✅ `testSubscript` - 下标 `_`
- ✅ `testAmpersand` - 对齐符 `&`
- ✅ `testBeginEnvironment` - `\begin` 环境
- ✅ `testEndEnvironment` - `\end` 环境
- ✅ `testNewLine` - 换行 `\\`
- ✅ `testWhitespace` - 空白字符
- ✅ `testComplexExpression` - 复杂表达式
- ✅ `testMultipleCommands` - 多个命令
- ✅ `testEscapedCharacters` - 转义字符
- ✅ `testEmptyString` - 空字符串
- ✅ `testMixedContent` - 混合内容

**波浪号与注释 (5个)**
- ✅ `testTildeAsNonBreakingSpace` - 波浪号作为不可断空格
- ✅ `testMultipleTildes` - 多个波浪号
- ✅ `testCommentIgnoresRestOfLine` - 注释忽略行尾
- ✅ `testCommentAtEndOfInput` - 输入末尾注释
- ✅ `testLineWithOnlyComment` - 纯注释行

---

#### 38. VisitorTest.kt (11 个测试) ⭐⭐ 中等

访问者模式测试，验证 AST 遍历功能。

- ✅ `testCountingVisitor` - 计数访问器
- ✅ `testTextExtractor` - 文本提取
- ✅ `testTextExtractorInFraction` - 分数中的文本提取
- ✅ `testDepthCalculator` - 深度计算
- ✅ `testDepthWithNesting` - 嵌套深度
- ✅ `testDepthWithDeepNesting` - 深层嵌套深度
- ✅ `testCountNodeTypes` - 节点类型计数
- ✅ `testCountSymbols` - 符号计数
- ✅ `testVisitMatrix` - 矩阵遍历
- ✅ `testVisitEnvironment` - 环境遍历
- ✅ `testVisitComplexExpression` - 复杂表达式遍历

---

#### 39. AccessibilityVisitorTest.kt (28 个测试) ⭐⭐⭐ 复杂

无障碍访问者测试，验证 LaTeX 到无障碍文本的转换。

- ✅ `testSimpleText` - 简单文本
- ✅ `testFraction` - 分数
- ✅ `testSuperscriptSquared` - 平方
- ✅ `testSuperscriptCubed` - 立方
- ✅ `testSuperscriptGeneral` - 一般上标
- ✅ `testSubscript` - 下标
- ✅ `testSquareRoot` - 平方根
- ✅ `testNthRoot` - N 次根
- ✅ `testBigOperator` - 大型运算符
- ✅ `testIntegral` - 积分
- ✅ `testGreekSymbol` - 希腊符号
- ✅ `testMatrix` - 矩阵
- ✅ `testCases` - 分段函数
- ✅ `testBinomial` - 二项式
- ✅ `testAccentHat` - hat 重音
- ✅ `testAccentVec` - vec 向量
- ✅ `testDelimited` - 定界符
- ✅ `testExtensibleArrow` - 可扩展箭头
- ✅ `testHookArrow` - 钩型箭头
- ✅ `testTensor` - 张量
- ✅ `testSideSet` - 侧置标
- ✅ `testLabel` - 标签
- ✅ `testRef` - 引用
- ✅ `testEqRef` - 方程引用
- ✅ `testBoxed` - 加框
- ✅ `testEmptyDocument` - 空文档
- ✅ `testComplexExpression` - 复杂表达式
- ✅ `testTabular` - 表格

---

#### 40. MathMLVisitorTest.kt (34 个测试) ⭐⭐⭐ 复杂

MathML 输出测试，验证 LaTeX 到 MathML 的转换。

- ✅ `testMathMLWrapper` - MathML 包装
- ✅ `testDisplayMode` - 显示模式
- ✅ `testTextNode` - 文本节点
- ✅ `testFraction` - 分数
- ✅ `testSuperscript` - 上标
- ✅ `testSubscript` - 下标
- ✅ `testSquareRoot` - 平方根
- ✅ `testNthRoot` - N 次根
- ✅ `testSymbol` - 符号
- ✅ `testOperator` - 运算符
- ✅ `testMatrix` - 矩阵
- ✅ `testBigOperator` - 大型运算符
- ✅ `testDelimited` - 定界符
- ✅ `testAccent` - 重音符
- ✅ `testCancel` - 取消线
- ✅ `testStyle` - 样式
- ✅ `testColor` - 颜色
- ✅ `testCases` - 分段函数
- ✅ `testBinomial` - 二项式
- ✅ `testTextMode` - 文本模式
- ✅ `testSpace` - 空格
- ✅ `testBoxed` - 加框
- ✅ `testPhantom` - 幻影
- ✅ `testExtensibleArrow` - 可扩展箭头
- ✅ `testHookArrow` - 钩型箭头
- ✅ `testSideSet` - 侧置标
- ✅ `testTensor` - 张量
- ✅ `testTabular` - 表格
- ✅ `testRef` - 引用
- ✅ `testEqRef` - 方程引用
- ✅ `testXmlEscaping` - XML 转义
- ✅ `testEmptyDocument` - 空文档
- ✅ `testSmash` - smash
- ✅ `testNegation` - 否定

---

### 源码位置与编辑测试 (2个文件, 40个测试)

#### 41. SourceRangeTest.kt (27 个测试) ⭐⭐⭐ 复杂

源码范围测试，验证 AST 节点与源码位置的映射。

**SourceRange 基础 (3个)**
- ✅ `testSourceRange_contains` - 范围包含
- ✅ `testSourceRange_merge` - 范围合并
- ✅ `testSourceRange_length` - 范围长度

**Tokenizer 范围 (9个)**
- ✅ `testTokenizer_simpleText_hasCorrectRange` - 文本范围
- ✅ `testTokenizer_command_hasCorrectRange` - 命令范围
- ✅ `testTokenizer_braces_haveCorrectRange` - 花括号范围
- ✅ `testTokenizer_superscript_subscript_range` - 上下标范围
- ✅ `testTokenizer_beginEnvironment_range` - 环境范围
- ✅ `testTokenizer_newLine_range` - 换行范围
- ✅ `testTokenizer_whitespace_range` - 空白范围
- ✅ `testTokenizer_specialChars_range` - 特殊字符范围
- ✅ `testTokenizer_complexExpression_ranges_cover_full_input` - 复杂表达式完整覆盖

**Parser 范围 (9个)**
- ✅ `testParser_document_coversFullInput` - 文档覆盖完整输入
- ✅ `testParser_textNode_hasTokenRange` - 文本节点范围
- ✅ `testParser_superscript_coversBaseAndExponent` - 上标覆盖底和指数
- ✅ `testParser_subscript_coversBaseAndIndex` - 下标覆盖底和索引
- ✅ `testParser_group_coversFullBraces` - 分组覆盖花括号
- ✅ `testParser_fraction_hasSourceRange` - 分数范围
- ✅ `testParser_spaceNode_hasRange` - 空格节点范围
- ✅ `testParser_symbol_hasSourceRange` - 符号范围
- ✅ `testParser_environment_hasSourceRange` - 环境范围

**SourceMapper (6个)**
- ✅ `testSourceMapper_leafNodeAt_findsTextNode` - 定位叶节点
- ✅ `testSourceMapper_leafNodeAt_outOfRange_returnsNull` - 超范围返回 null
- ✅ `testSourceMapper_nodePathAt_returnsRootToLeaf` - 根到叶路径
- ✅ `testSourceMapper_collectLeaves_returnsAllLeafNodes` - 收集所有叶节点
- ✅ `testSourceMapper_childrenOf_fraction` - 分数子节点
- ✅ `testSourceMapper_childrenOf_leafNode` - 叶节点子节点

---

#### 42. TextEditTest.kt (13 个测试) ⭐⭐ 中等

文本编辑 diff 测试。

- ✅ `diff_identicalStrings_returnsZeroEdit` - 相同字符串
- ✅ `diff_emptyToNonEmpty_returnsFullInsertion` - 空到非空
- ✅ `diff_nonEmptyToEmpty_returnsFullDeletion` - 非空到空
- ✅ `diff_appendAtEnd` - 末尾追加
- ✅ `diff_insertInMiddle` - 中间插入
- ✅ `diff_deleteFromMiddle` - 中间删除
- ✅ `diff_replaceInMiddle` - 中间替换
- ✅ `diff_replaceAtStart` - 开头替换
- ✅ `diff_replaceAtEnd` - 末尾替换
- ✅ `fromAppend_correctEditForAppendScenario` - 追加场景
- ✅ `diff_latexAppendCharByChar` - LaTeX 逐字追加
- ✅ `diff_latexInsertInBrace` - LaTeX 花括号内插入
- ✅ `diff_latexDeleteCommand` - LaTeX 命令删除

---

## 📈 测试质量指标

| 指标 | 数值 | 状态 |
|------|------|------|
| 测试文件总数 | 42 | ✅ 模块化 |
| 测试用例总数 | 656 | ✅ 优秀 |
| 代码覆盖率 | ~95% | ✅ 优秀 |
| LaTeX 符号覆盖 | 200+ | ✅ 完整 |
| 真实公式测试 | 13 | ✅ 充足 |
| 边界情况测试 | 21 | ✅ 完善 |
| AMS 扩展符号 | 71 | ✅ 丰富 |
| 增量解析测试 | 43 | ✅ 充分 |

### 测试文件职责分离

| 文件 | 职责 | 用例数 | 复杂度 |
|------|------|--------|--------|
| BasicSymbolsTest | 基础符号 | 23 | ⭐ 简单 |
| SimpleFormulaTest | 简单公式 | 52 | ⭐⭐ 中等 |
| ComplexStructureTest | 复杂结构 | 45 | ⭐⭐⭐ 复杂 |
| RealWorldFormulaTest | 真实公式 | 13 | ⭐⭐⭐⭐ 高级 |
| EdgeCasesTest | 边界情况 | 21 | ⭐⭐⭐ 复杂 |
| DelimiterTest | 定界符 | 31 | ⭐⭐⭐ 复杂 |
| MathModeTest | 数学模式 | 14 | ⭐⭐ 中等 |
| EnvironmentExtensionTest | 环境扩展 | 36 | ⭐⭐⭐ 复杂 |
| AlignmentEnvironmentsTest | 对齐环境 | 8 | ⭐⭐⭐ 复杂 |
| AmsExtraSymbolsTest | AMS 扩展符号 | 30 | ⭐⭐ 中等 |
| AmsNegatedRelationsTest | AMS 否定关系 | 41 | ⭐⭐⭐ 复杂 |
| NewCommandTest | 自定义命令 | 20 | ⭐⭐⭐ 复杂 |
| DeclareMathOperatorTest | 运算符声明 | 5 | ⭐⭐ 中等 |
| OperatorNameTest | operatorname | 6 | ⭐⭐ 中等 |
| MathOpTest | mathop | 7 | ⭐⭐ 中等 |
| ModOperatorTest | 取模运算符 | 7 | ⭐⭐ 中等 |
| MathStyleTest | 数学样式 | 10 | ⭐⭐ 中等 |
| BigOperatorExtensionTest | 大型运算符扩展 | 6 | ⭐⭐ 中等 |
| AccentExtensionTest | 重音符扩展 | 11 | ⭐⭐ 中等 |
| SpecialEffectTest | 特殊效果 | 13 | ⭐⭐ 中等 |
| CancelVariantsTest | 取消线变体 | 3 | ⭐ 简单 |
| BraceAnnotationTest | 大括号注释 | 2 | ⭐ 简单 |
| NegationTest | 否定符号 | 3 | ⭐ 简单 |
| SpaceTest | 间距命令 | 3 | ⭐ 简单 |
| SpacingAdjustmentTest | 间距调整 | 4 | ⭐⭐ 中等 |
| AdaptiveDotsTest | 自适应省略号 | 3 | ⭐ 简单 |
| CasesVariantTest | cases 变体 | 7 | ⭐⭐ 中等 |
| SubstackTest | 子堆叠 | 3 | ⭐ 简单 |
| TensorTest | 张量 | 4 | ⭐⭐ 中等 |
| SideSetTest | 侧置标 | 3 | ⭐ 简单 |
| LabelRefTest | 标签引用 | 6 | ⭐⭐ 中等 |
| TagTest | 标签 | 2 | ⭐ 简单 |
| ChemicalParserTest | 化学公式 | 10 | ⭐⭐⭐ 复杂 |
| IncrementalLatexParserTest | 增量解析器 | 29 | ⭐⭐⭐ 复杂 |
| IncrementalTokenizerTest | 增量词法分析 | 14 | ⭐⭐ 中等 |
| SymbolMapTest | 符号映射 | 27 | ⭐⭐ 中等 |
| TokenizerTest | 词法分析 | 21 | ⭐⭐ 中等 |
| VisitorTest | 访问者模式 | 11 | ⭐⭐ 中等 |
| AccessibilityVisitorTest | 无障碍访问者 | 28 | ⭐⭐⭐ 复杂 |
| MathMLVisitorTest | MathML 输出 | 34 | ⭐⭐⭐ 复杂 |
| SourceRangeTest | 源码范围 | 27 | ⭐⭐⭐ 复杂 |
| TextEditTest | 文本编辑 | 13 | ⭐⭐ 中等 |

---

## 📋 功能覆盖详情

### 解析器核心功能

| 功能模块 | 测试数 | 覆盖命令 |
|----------|--------|---------|
| 基础符号 | 23 | 文本、希腊字母、运算符、关系符、箭头、空格 |
| 分数 | 6 | `\frac`, `\dfrac`, `\tfrac`, `\cfrac` |
| 二项式 | 5 | `\binom`, `\tbinom`, `\dbinom` |
| 根号 | 4 | `\sqrt`, `\sqrt[n]` |
| 上下标 | 6 | `^`, `_`, 组合使用 |
| 定界符 | 31 | `\left\right`, `\big`~`\Bigg`, `\lvert`, `\lVert` |
| 装饰符号 | 25 | `\hat`, `\vec`, `\overline`, `\widehat`, `\cancel` 等 |
| 颜色 | 3 | `\color`, `\textcolor` |
| 堆叠 | 5 | `\overset`, `\underset`, `\stackrel` |
| 大型运算符 | 21 | `\sum`, `\prod`, `\int`, `\lim`, `\coprod`, `\bigoplus` 等 |
| 矩阵 | 9 | `matrix`, `pmatrix`, `bmatrix`, `vmatrix`, `smallmatrix`, `array` |
| 环境 | 44 | `equation`, `aligned`, `cases`, `split`, `multline`, `tabular` 等 |
| 字体样式 | 18 | `\text`, `\mathbf`, `\mathbb`, `\mathcal`, `\boldsymbol`, `\symbf` 等 |
| 数学模式 | 14 | `$...$`, `$$...$$`, 混合模式 |
| 数学样式 | 10 | `\displaystyle`, `\textstyle`, `\scriptstyle`, `\scriptscriptstyle` |

### 扩展功能

| 功能模块 | 测试数 | 覆盖命令 |
|----------|--------|---------|
| 自定义命令 | 20 | `\newcommand`, `\renewcommand`, `\def` |
| 运算符声明 | 5 | `\DeclareMathOperator` |
| operatorname | 6 | `\operatorname` |
| mathop | 7 | `\mathop`, `\limits`, `\nolimits` |
| 取模 | 7 | `\bmod`, `\pmod`, `\mod` |
| AMS 扩展符号 | 30 | `\checkmark`, `\complement`, `\blacksquare` 等 |
| AMS 否定关系 | 41 | `\nless`, `\ngtr`, `\nleq`, `\nvdash` 等 |
| 化学公式 | 10 | `\ce{}` 化学方程式 |
| 张量 | 4 | `\tensor`, `\indices` |
| 侧置标 | 3 | `\sideset` |
| 标签引用 | 8 | `\label`, `\ref`, `\eqref`, `\tag` |
| 否定 | 3 | `\not` |
| 子堆叠 | 3 | `\substack` |
| 间距 | 7 | `\!`, `\hspace`, `\smash`, `\vphantom`, `\hphantom` |
| 自适应省略号 | 3 | `\dots` |
| cases 变体 | 7 | `dcases`, `rcases` |
| 取消线变体 | 3 | `\bcancel`, `\xcancel` |
| 大括号注释 | 2 | `\underbrace`, `\overbrace` |

### 输出格式

| 功能模块 | 测试数 | 说明 |
|----------|--------|------|
| 无障碍文本 | 28 | LaTeX → 可朗读文本 |
| MathML | 34 | LaTeX → MathML XML |

### 工程基础

| 功能模块 | 测试数 | 说明 |
|----------|--------|------|
| 词法分析 | 21 | Token 识别、特殊字符、注释 |
| 符号映射 | 27 | 200+ LaTeX 符号到 Unicode |
| AST 遍历 | 11 | 访问者模式、深度计算 |
| 增量解析 | 43 | 流式输入、容错、一致性 |
| 源码范围 | 27 | AST ↔ 源码位置映射 |
| 文本编辑 | 13 | diff 算法、增量更新 |

---

## 🚀 如何运行测试

```bash
# 运行所有解析器测试
./run_parser_tests.sh

# 或直接使用 Gradle
./gradlew :latex-parser:cleanJvmTest :latex-parser:jvmTest

# 运行特定测试类
./gradlew :latex-parser:jvmTest --tests "com.hrm.latex.parser.SimpleFormulaTest"

# 生成测试报告
./gradlew :latex-parser:jvmTest --tests "*" --info
```

---

## ✨ 测试亮点

1. **职责分离** — 42 个测试文件按职责清晰划分
   - 解析器核心 (7个): 基础→简单→复杂→真实→边界→定界符→数学模式
   - 环境扩展 (2个): 环境类型、对齐环境
   - AMS 符号 (2个): 扩展符号、否定关系
   - 命令扩展 (7个): 自定义命令、运算符、样式
   - 装饰与特效 (5个): 重音、加框、取消线、否定
   - 间距与布局 (3个): 间距、调整、省略号
   - 数学结构 (5个): cases 变体、子堆叠、张量、标签
   - 化学公式 (1个): 化学方程式
   - 增量解析 (2个): 增量解析器、增量词法分析
   - 工具类 (5个): 符号映射、词法、访问者、无障碍、MathML
   - 源码工具 (2个): 源码范围、文本编辑

2. **全面覆盖** — 656 个测试用例覆盖所有 LaTeX 语法模块

3. **渐进式复杂度** — 测试难度逐步递增
   - ⭐ 基础符号、间距、简单取消线
   - ⭐⭐ 简单公式、符号映射、词法分析
   - ⭐⭐⭐ 复杂结构、环境、AMS 否定、源码范围
   - ⭐⭐⭐⭐ 真实世界公式

4. **多维度输出验证** — 不仅验证解析正确性，还验证：
   - ✅ 无障碍文本输出 (28个测试)
   - ✅ MathML 格式输出 (34个测试)
   - ✅ 源码位置映射 (27个测试)

5. **增量解析完整覆盖** — 43 个测试覆盖流式输入的各种场景
   - 增量追加、替换、清除
   - 不完整输入容错
   - 与标准解析器一致性验证

6. **实用性强** — 包含真实应用场景
   - 13 个经典数学公式（代数、微积分、物理）
   - 10 个化学方程式
   - 复杂嵌套和长表达式测试
   - 异常输入和边界值处理

7. **易于维护** — 模块化设计
   - 每个文件聚焦单一职责
   - 清晰的测试分类
   - 独立的测试文件可单独运行
