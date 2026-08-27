# Kotlin Multiplatform LaTeX Rendering Library

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android%20API-23%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.huarangmeng/latex-base?filter=!*-kt*)](https://central.sonatype.com/search?q=io.github.huarangmeng.latex)

这是一个基于 Kotlin Multiplatform (KMP) 开发的高性能 LaTeX 数学公式解析与渲染库。支持在 Android, iOS, Desktop (JVM) 和 Web (Wasm/JS) 平台上实现一致的渲染效果。

[English Version](./README.md)

## 🌟 核心特性

- **高性能解析**：基于 AST 的递归下降解析器，支持增量更新。
- **多平台一致性**：使用 Compose Multiplatform 在 Android、iOS、Desktop (JVM) 和 Web (Wasm/JS) 平台上实现一致渲染。
- **自动换行**：长公式在逻辑断点（运算符、关系符）处智能换行。
- **光栅与矢量导出**：支持 PNG/JPEG/WEBP 图片和与分辨率无关的 SVG 矢量输出。
- **行内公式 API**：通过 `LatexMeasurerState.inlineContent()` 直接生成 Compose `InlineTextContent`，同时保留精确尺寸预测量能力。
- **无障碍支持**：内置屏幕阅读器支持，基于 MathSpeak 风格生成公式自然语言描述。
- **LaTeX → MathML**：将 LaTeX AST 转换为 Presentation MathML 输出。
- **公式高亮**：通过 `HighlightConfig` 高亮子表达式。
- **动画过渡**：公式切换动画（crossfade / slide / fade+slide）。
- **所见即所得编辑器** *（实验性）*：内置 LaTeX 编辑器，支持光标定位、点击定位和实时渲染预览。
- **结构化诊断**：`parseWithDiagnostics()` 提供 8 种分类的结构化诊断信息，按严重级别过滤。
- **RTL 支持**：完整的从右到左文本方向支持（`\RLE`、`\LRE`、RTL/LTR 环境，支持嵌套）。

## 📐 已支持的 LaTeX 功能（450+）

<details>
<summary><b>数学公式</b> — 分数、根号、二项式、上下标</summary>

`\frac`, `\dfrac`, `\tfrac`, `\cfrac`, `\genfrac`, `\splitfrac`, `\splitdfrac`, `\binom`, `\tbinom`, `\dbinom`，以及 `\over`, `\atop`, `\choose`, `\above` 等 TeX 中缀形式；同时支持根号与上下标。
</details>

<details>
<summary><b>符号系统（130+）</b> — 希腊字母、运算符、箭头、AMS 符号</summary>

- **希腊字母**：全部小写 (α–ω，含 `\omicron`)、大写 (Γ–Ω)、KaTeX 风格大写别名（如 `\Alpha`、`\Beta`、`\Epsilon`、`\Omicron`）及变体 (ε/ϵ, θ/ϑ, φ/ϕ, `\varGamma`–`\varOmega` 等)
- **运算符**：`+`, `-`, `\times`, `\div`, `\pm`, `\mp`, `\cdot`, `\oplus`, `\otimes` 等
- **关系符**：`=`, `\neq`, `<`, `>`, `\leq`, `\geq`, `\approx`, `\equiv`, `\sim`, `\coloneqq`, `\eqqcolon` 等
- **集合论**：`\in`, `\notin`, `\subset`, `\cup`, `\cap`, `\emptyset`, `\mathbb{R}` 等
- **逻辑**：`\land`, `\lor`, `\neg`, `\Rightarrow`, `\Leftrightarrow`, `\forall`, `\exists`
- **箭头**：`\to`, `\rightarrow`, `\leftarrow`, `\leftrightarrow`, `\Rightarrow`, `\hookrightarrow`, 鱼叉箭头等
- **省略号**：`\ldots`, `\cdots`, `\vdots`, `\ddots`, `\dots`（自适应）
- **否定修饰**：`\not=`, `\not\in`, `\nleq`, `\ngeq`, `\ncong`, `\nmid` 等（30+ AMS 否定关系符）
- **AMS 额外符号**：`\checkmark`, `\complement`, `\blacksquare`, `\aleph`, `\measuredangle`, 几何符号, 双头箭头等
</details>

<details>
<summary><b>大型运算符（28）</b> — 求和、积分、极限、取模</summary>

- **求和/积分**：`\sum`, `\prod`, `\int`, `\oint`, `\iint`, `\iiint`, `\bigcup`, `\bigcap`, `\bigvee`, `\bigwedge`, `\coprod`, `\bigoplus`, `\bigotimes`, `\bigsqcup`, `\bigodot`, `\biguplus`
- **极限类**：`\lim`, `\max`, `\min`, `\sup`, `\inf`, `\limsup`, `\liminf`
- **自定义运算符**：`\operatorname{名称}`, `\operatorname*{名称}`, `\DeclareMathOperator{\Tr}{Tr}`, `\mathop{内容}`
- **多行下标**：`\substack{条件1 \\ 条件2}`
- **取模运算**：`\bmod`（二元取模）, `\pmod{n}`（括号取模）, `\mod`（宽间距取模）
- **数学类代码**：`\mathord`, `\mathbin`, `\mathrel`, `\mathopen`, `\mathclose`, `\mathpunct`, `\mathinner` 控制 TeX 原子类与原子间距
</details>

<details>
<summary><b>矩阵</b> — 标准、星号与紧凑矩阵环境</summary>

`matrix`, `pmatrix`, `bmatrix`, `Bmatrix`, `vmatrix`, `Vmatrix`, `smallmatrix`，支持 mathtools 星号形式及 `[l|c|r]` 对齐；另支持 `array`, `subarray`。
</details>

<details>
<summary><b>括号与分隔符</b> — 自动伸缩与手动大小</summary>

- **自动伸缩**：`\left...\right`、内部 `\middle`，以及尖括号、取整括号、单双竖线变体
- **不对称分隔符**：`\left. \right|`（求值符号）、`\left\{ \right.`（分段函数）
- **手动大小**：`\big`, `\Big`, `\bigg`, `\Bigg` 及 `\bigl`, `\bigr`, `\bigm` 变体
</details>

<details>
<summary><b>装饰符号（42+）</b> — 重音、取消线、可扩展箭头、堆叠、括号标注</summary>

- **重音符号**：原有重音外，支持 `\widecheck`, `\overparen`, `\underparen`
- **大括号标注**：`\overbrace{...}^{text}`, `\underbrace{...}_{text}`, `\overbracket{...}`, `\underbracket{...}`
- **箭头装饰**：`\overrightarrow`, `\overleftarrow`, `\overleftrightarrow`, `\underleftarrow`, `\underrightarrow`
- **取消线**：`\cancel`, `\bcancel`（反向）, `\xcancel`（交叉）
- **可扩展箭头与等号**：`\xrightarrow`, `\xleftarrow`, `\xhookrightarrow`, `\xhookleftarrow`, `\xRightarrow`, `\xLeftarrow`, `\xLeftrightarrow`, `\xmapsto`, `\xlongequal`
- **堆叠**：`\overset`, `\underset`, `\stackrel`
</details>

<details>
<summary><b>字体样式（17）</b></summary>

`\mathbf`, `\mathit`, `\mathrm`, `\mathsf`, `\mathtt`, `\mathbb`, `\mathfrak`, `\mathcal`, `\mathscr`, `\boldsymbol`, `\bm`, `\text`, `\mbox`, `\textnormal`, `\textup`, `\textmd`, `\textsc`, `\textsl`, `\emph` 及 Unicode-math 样式别名。
</details>

<details>
<summary><b>字号（10）</b></summary>

`\tiny`, `\scriptsize`, `\footnotesize`, `\small`, `\normalsize`, `\large`, `\Large`, `\LARGE`, `\huge`, `\Huge`
</details>

<details>
<summary><b>数学模式切换</b></summary>

`\displaystyle`, `\textstyle`, `\scriptstyle`, `\scriptscriptstyle`, `$...$`（行内）, `$$...$$`（展示）
</details>

<details>
<summary><b>环境（21）</b> — 对齐、分段、矩阵、表格</summary>

- **公式环境**：`equation(*)`, `displaymath`
- **对齐环境**：`align(*)`, `aligned`, `flalign(*)`, `alignat(*)`, `alignedat`
- **居中环境**：`gather(*)`, `gathered`
- **分段函数**：`cases`, `dcases`（displaystyle）, `rcases`（右花括号）及星号形式
- **多行/分割**：`split`, `multline(*)`
- **其他**：`eqnarray(*)`, `subequations`, `tabular`（l/c/r 列对齐）
- **行控制与编号**：`\\`, `\cr`, `\intertext`, `\shortintertext`, `\notag`, `\nonumber`, `\tag`/`\tag*`, `\label`/`\ref`/`\eqref`
</details>

<details>
<summary><b>空格控制</b></summary>

支持原有命名数学空格、`\hspace{...}`，并新增 `\kern`, `\mkern`, `\allowbreak`；同时支持常用特殊字符转义。
</details>

<details>
<summary><b>颜色与背景色</b></summary>

- **文本颜色**：`\color{red}{...}`, `\textcolor{#FF5733}{...}`（命名颜色 + 十六进制）
- **背景色**：`\colorbox{yellow}{文本}`, `\fcolorbox{borderColor}{bgColor}{文本}`
</details>

<details>
<summary><b>化学公式（13）</b> — mhchem 宏包</summary>

`\ce{...}` 支持分子、离子、系数、同位素、单/双/三键，以及带 `[上方][下方]` 标注的反应箭头；另支持 `\bond{...}` 与 `\pu{...}`。
</details>

<details>
<summary><b>特殊效果与布局（17）</b></summary>

- **方框**：`\boxed{E=mc^2}`, `\fbox{text}`
- **Menclose / enclose**：`\enclose{circle}{x}`, `\enclose{circle,box}{x}`, `\enclose{updiagonalstrike downdiagonalstrike}{x}`, `\circled{1}`
- **已支持 notation**：`box`, `roundedbox`, `circle`, `left`, `right`, `top`, `bottom`, `updiagonalstrike`, `downdiagonalstrike`, `verticalstrike`, `horizontalstrike`
- **已支持 attributes**：`mathcolor`, `mathbackground`
- **幻影与间距**：`\phantom`, `\smash`, `\vphantom`, `\hphantom`, `\mathstrut`
- **零宽叠加**：`\mathclap{内容}`, `\mathllap{内容}`, `\mathrlap{内容}`
- **TeX 布局**：`\clap`, `\llap`, `\rlap`, `\raisebox`, `\rule`
- **mathtools**：`\Aboxed`, `\MoveEqLeft`, `\splitfrac`, `\splitdfrac`、定义关系符与 `\DeclarePairedDelimiter`
</details>

<details>
<summary><b>物理与 SI 单位</b> — physics 与 siunitx 常用子集</summary>

- **导数简写**：`\dv{x}`, `\dv{f}{x}`, `\dv[2]{f}{x}` 以及对应的 `\pdv` 形式
- **狄拉克符号与算子**：`\bra`/`\Bra`, `\ket`/`\Ket`, `\braket`/`\Braket`, `\comm`, `\anticomm`, `\eval`, `\vb`, `\va`, `\abs`, `\norm`
- **数字与单位**：兼容 `\SI`, `\si`，新增 `\qty`, `\unit`, `\numrange`, `\qtyrange`, `\ang` 与常用 SI 前缀/单位命令
</details>

<details>
<summary><b>高级标注（6）</b> — 超链接、张量、四角标</summary>

- **超链接**：`\href{url}{text}`, `\url{url}`（蓝色下划线，支持点击回调）
- **四角标注**：`\sideset{_a^b}{_c^d}{\sum}`
- **前置上下标**：`\prescript{A}{Z}{X}`（同位素标记）
- **张量指标**：`\tensor{T}{^a_b^c}`, `\indices{^a_b}`
</details>

<details>
<summary><b>自定义命令与宏定义（9）</b></summary>

`\newcommand`, `\renewcommand`, `\def`（0–9 个参数，支持可选参数默认值）, `\newenvironment`, `\renewenvironment`, `\DeclarePairedDelimiter`
</details>

<details>
<summary><b>章节结构命令</b></summary>

`\section`, `\subsection`, `\subsubsection`, `\paragraph`, `\subparagraph`（含星号变体）
</details>

<details>
<summary><b>RTL 文本方向</b></summary>

- **命令**：`\RLE{...}`, `\LRE{...}`, `\textarabic{...}`, `\texthebrew{...}`
- **环境**：`\begin{RTL}...\end{RTL}`, `\begin{LTR}...\end{LTR}`
- **嵌套**：支持 RTL 内嵌 LTR，反之亦然
</details>

<details>
<summary><b>标签与引用</b></summary>

`\label`, `\ref`, `\eqref`, `\tag{1}`, `\tag*{A}`
</details>

<details>
<summary><b>错误处理</b></summary>

- 无法识别的命令以错误颜色渲染，而非静默忽略
- `parseWithDiagnostics()` 提供结构化诊断（8 种分类，按严重级别过滤）
</details>

## 📸 渲染预览

项目包含一个演示 App (`composeApp`/`androidApp`)，展示了各种复杂的 LaTeX 场景。预览数据中也新增了独立的 `Enclose / menclose` 分组，用于展示圆圈、方框、组合边框、删除线以及颜色/背景属性效果：

| 基础数学 | 化学公式 | 增量解析 |
| :---: | :---: | :---: |
| ![基础数学](images/normal_latex.png) | ![化学公式](images/chemical_latex.png) | ![增量解析](images/incremental_latex.png) |
| 基础数学公式渲染 | 支持 `\ce{...}` 语法 | 支持不完整输入的实时预览 |

## 🛠️ 使用方法

在 Compose Multiplatform 项目中，你可以直接使用 `Latex` 组件。该组件会自动处理增量解析，支持实时预览：

```kotlin
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import androidx.compose.ui.unit.sp

@Composable
fun MyScreen() {
    Latex(
        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
        config = LatexConfig(
            fontSize = 20.sp,
            theme = LatexTheme.auto()
        )
    )
}
```

### 数学字体

内置 KaTeX TTF 字体集是渲染器唯一的数学字体管线。Main、Math、AMS、各风格
字体、Size1–Size4 与 KaTeX metrics 会作为一个整体加载，保证测量与绘制使用
同一组字体数据。

```kotlin
// 默认：内置 KaTeX TTF 字体
LatexConfig()
```

不支持外部字体注入，因为 KaTeX 排版 metrics 只适用于配套的内置字体文件。

### 主题配置

使用 `LatexTheme` 控制公式的前景色和背景色：

```kotlin
import com.hrm.latex.renderer.model.LatexTheme

// 跟随系统深浅色
LatexConfig(theme = LatexTheme.auto())

// 固定浅色主题
LatexConfig(theme = LatexTheme.light())

// 固定深色主题
LatexConfig(theme = LatexTheme.dark())

// 跟随当前 Material 3 ColorScheme
LatexConfig(theme = LatexTheme.material3())
```

如果需要自定义颜色，可以通过 `LatexThemeColors` 组合：

```kotlin
import androidx.compose.ui.graphics.Color
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.LatexThemeColors

LatexConfig(
    theme = LatexTheme.auto(
        light = LatexThemeColors(
            color = Color(0xFF111111),
            backgroundColor = Color.Transparent
        ),
        dark = LatexThemeColors(
            color = Color(0xFFF5F5F5),
            backgroundColor = Color.Transparent
        )
    )
)
```

### 自动换行

对于需要在容器宽度内自动换行的长公式，使用 `LatexAutoWrap`：

```kotlin
import com.hrm.latex.renderer.LatexAutoWrap

@Composable
fun MyScreen() {
    LatexAutoWrap(
        latex = "E = mc^2 + \\frac{p^2}{2m} + V(x) + \\frac{1}{2}kx^2",
        modifier = Modifier.fillMaxWidth(),
        config = LatexConfig(fontSize = 20.sp)
    )
}
```

换行发生在数学上有效的位置：关系运算符（`=`、`<`、`>`），然后是加法运算符（`+`、`-`），然后是乘法运算符（`×`、`÷`）。分数、根号、矩阵等原子结构不会被拆分。

### 光栅图片与 SVG 矢量导出

将渲染后的 LaTeX 公式导出为 PNG、JPEG 或 WEBP 图片。在 Composable 作用域中使用 `rememberLatexExporter()` 创建导出器，然后在后台线程中调用 `export()` 方法：

```kotlin
import com.hrm.latex.renderer.export.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MyScreen() {
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch(Dispatchers.Default) {
            // 导出为 PNG（默认，2 倍分辨率）
            val result = exporter.export("E = mc^2")
            val pngBytes = result?.bytes       // PNG 字节数组
            val bitmap = result?.imageBitmap    // 可直接在 Compose 中展示

            // 导出为 JPEG（3 倍分辨率，质量 85）
            val jpegResult = exporter.export(
                latex = "\\frac{a}{b}",
                exportConfig = ExportConfig(
                    scale = 3f,
                    format = ImageFormat.JPEG,
                    quality = 85
                )
            )

            // 导出透明背景（仅 PNG 支持）
            val transparentResult = exporter.export(
                latex = "x^2 + y^2 = r^2",
                exportConfig = ExportConfig(transparentBackground = true)
            )
        }
    }) {
        Text("导出")
    }
}
```

`ExportConfig` 参数说明：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `scale` | `Float` | `2f` | 分辨率倍率（1x、2x、3x 等） |
| `format` | `ImageFormat` | `PNG` | `ImageFormat.PNG`、`ImageFormat.JPEG` 或 `ImageFormat.WEBP` |
| `transparentBackground` | `Boolean` | `false` | 是否使用透明背景（PNG 和 WEBP 支持；JPEG 始终使用不透明背景） |
| `quality` | `Int` | `90` | JPEG 和 WEBP 的压缩质量（1–100，PNG 忽略此参数） |

高清印刷、响应式 Web 嵌入或其他无损缩放场景使用 `exportSvg()`。它与屏幕渲染复用同一套
测量和绘制管线，输出 path、线条等原生矢量元素，不会把位图包装进 SVG：

```kotlin
val svgResult = exporter.exportSvg(
    latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
    exportConfig = SvgExportConfig(
        transparentBackground = true,
        textMode = SvgTextMode.PATH
    )
)

val svg = svgResult?.svg        // 可直接嵌入 Web 的 SVG 文本
val svgBytes = svgResult?.bytes // 可保存或分享的 UTF-8 字节
```

`SvgExportConfig` 与光栅参数刻意分离：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `scale` | `Float` | `1f` | 调整 SVG viewport 和公式尺寸；结果仍与分辨率无关 |
| `transparentBackground` | `Boolean` | `true` | 透明背景；关闭时使用 `LatexConfig` 解析后的背景色 |
| `textMode` | `SvgTextMode` | `PATH` | `PATH` 输出可移植字形轮廓；`TEXT` 更小且可选择，但需由使用方提供匹配字体 |
| `prettyPrint` | `Boolean` | `true` | 是否格式化生成的 XML |

### 无障碍支持

本库内置了屏幕阅读器无障碍支持。启用后，每个 `Latex` 组件会通过 Compose Semantics 暴露 MathSpeak 风格的自然语言描述，使数学公式能够被 TalkBack（Android）、VoiceOver（iOS）等辅助技术正确朗读。

```kotlin
Latex(
    latex = "\\frac{1}{2}",
    config = LatexConfig(accessibilityEnabled = true)
)
// 屏幕阅读器朗读: "fraction: 1 over 2"
```

`AccessibilityVisitor` 会将 LaTeX AST 转换为描述性文本，覆盖分数、根号、上下标、矩阵、希腊字母、运算符等结构。

### 行内数学公式支持

`inlineContent()` 封装了公式预测量、`Placeholder` 尺寸和公式渲染，可直接与 Compose `Text` 配合：

```kotlin
val measurer = rememberLatexMeasurer(config)
val formula = measurer.inlineContent("\\frac{a}{b}", config)

Text(
    text = buildAnnotatedString {
        append("分数 ")
        appendInlineContent("formula", "a 除以 b")
        append(" 表示 a 除以 b。")
    },
    inlineContent = formula?.let { mapOf("formula" to it) }.orEmpty()
)
```

空输入或测量失败时 `inlineContent()` 返回 `null`，此时 Compose 会显示 `appendInlineContent()` 提供的替代文本。相同公式和配置会复用 `LatexMeasurerState` 内部缓存。

需要自定义布局时仍可使用 `measure()`：`LatexDimensions` 提供 `widthPx`、`heightPx`、`baselinePx`（含内边距）及对应的纯内容尺寸字段。批量测量可使用 `measureBatch()`。

### 所见即所得编辑器（实验性）

> **注意**：编辑器 API 目前处于实验阶段，后续版本中可能会发生变更。所有编辑器 API 需要添加 `@ExperimentalComposeUiApi` 注解。

本库内置了所见即所得（WYSIWYG）LaTeX 编辑器组件。用户可以编辑 LaTeX 源文本并实时查看渲染结果，光标位置在源文本和渲染输出之间保持同步。

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MyEditor() {
    val editorState = rememberEditorState(initialText = "x^{2} + y^{2} = r^{2}")

    LatexEditor(
        editorState = editorState,
        config = LatexConfig(fontSize = 20.sp),
        showSourceText = true // 显示源文本输入框
    )
}
```

## 📦 安装

### 版本兼容性

本库为每个版本发布两个变体，以支持不同的 Kotlin/Compose 版本：

| 变体 | Kotlin | Compose Multiplatform | 制品版本号 |
|------|--------|-----------------------|-----------|
| **标准版** | 2.3.10 | 1.10.3 | `1.5.0` |
| **Kotlin 2.1.0 兼容版** | 2.1.0 | 1.9.3 | `1.5.0-kt2.1.0` |

> 请选择与你项目 Kotlin 版本匹配的变体。如果你的项目使用 **Kotlin 2.1.0**，请使用带 `-kt2.1.0` 后缀的版本。

### 标准版（Kotlin 2.3.10）

在 `gradle/libs.versions.toml` 中添加依赖：

```toml
[versions]
latex = "1.5.0"

[libraries]
latex-base = { module = "io.github.huarangmeng:latex-base", version.ref = "latex" }
latex-parser = { module = "io.github.huarangmeng:latex-parser", version.ref = "latex" }
latex-renderer = { module = "io.github.huarangmeng:latex-renderer", version.ref = "latex" }
```

### Kotlin 2.1.0 兼容版

如果你的项目使用 Kotlin 2.1.0，请使用带 `-kt2.1.0` 后缀的制品：

```toml
[versions]
latex = "1.5.0-kt2.1.0"

[libraries]
latex-base = { module = "io.github.huarangmeng:latex-base", version.ref = "latex" }
latex-parser = { module = "io.github.huarangmeng:latex-parser", version.ref = "latex" }
latex-renderer = { module = "io.github.huarangmeng:latex-renderer", version.ref = "latex" }
```

### 添加到模块

在模块的 `build.gradle.kts` 中引用：

```kotlin
dependencies {
    implementation(libs.latex.base) // 基础日志
    implementation(libs.latex.renderer) // 渲染逻辑
    implementation(libs.latex.parser) // 解析逻辑
}
```

## 🏗️ 项目结构

- `:latex-base`: 基础数据结构和接口。
- `:latex-parser`: 核心解析引擎，负责将 LaTeX 字符串转换为 AST。
- `:latex-renderer`: 负责将 AST 渲染为 Compose UI 组件。
- `:latex-preview`: 预览组件和示例数据集。
- `:composeApp`: 跨平台 Demo 应用程序。
- `:androidApp`: Android Demo 应用程序。

## 🚀 快速开始

### 运行 Demo App

- **Android**: `./gradlew :androidApp:assembleDebug`
- **Desktop**: `./gradlew :composeApp:run`
- **Web (Wasm)**: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- **iOS**: 在 Xcode 中打开 `iosApp/iosApp.xcworkspace` 运行。

### 运行测试

```bash
./run_parser_tests.sh
```

## 📊 功能覆盖

详细的功能支持列表及归入各章节的后续事项请参阅：[PARSER_COVERAGE_ANALYSIS.md](./latex-parser/PARSER_COVERAGE_ANALYSIS.md)

## 🙏 致谢

- [KaTeX](https://github.com/KaTeX/KaTeX) — 本项目使用 KaTeX v0.16.11 字体文件进行数学公式渲染；来源、署名和许可证详情见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。

## 💡 推荐项目

- [Markdown](https://github.com/huarangmeng/Markdown) — 同作者开发的 Kotlin Multiplatform Markdown 解析与渲染库。如果你的项目同时需要 LaTeX 和 Markdown 渲染能力，推荐一起使用！

## 📄 开源协议

本项目采用 MIT License 开源协议 - 详见 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2026 huarangmeng

特此免费授予任何获得本软件及相关文档文件（"软件"）副本的人不受限制地处理
软件的权利，包括但不限于使用、复制、修改、合并、发布、分发、再许可和/或
销售软件副本的权利，以及允许获得软件的人这样做，但须符合以下条件：

上述版权声明和本许可声明应包含在软件的所有副本或主要部分中。

本软件按"原样"提供，不提供任何形式的明示或暗示保证，包括但不限于对适销性、
特定用途的适用性和非侵权性的保证。在任何情况下，作者或版权持有人均不对
因软件或软件的使用或其他交易而产生的任何索赔、损害或其他责任承担责任，
无论是在合同诉讼、侵权行为还是其他方面。
```
