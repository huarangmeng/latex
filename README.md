# Kotlin Multiplatform LaTeX Rendering Library

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android%20API-23%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.huarangmeng/latex-base?filter=!*-kt*)](https://central.sonatype.com/search?q=io.github.huarangmeng.latex)

A high-performance LaTeX mathematical formula parsing and rendering library developed based on Kotlin Multiplatform (KMP). It supports consistent rendering effects on Android, iOS, Desktop (JVM), and Web (Wasm/JS) platforms.

[中文版本](./README_zh.md)

## 🌟 Key Features

- **High-Performance Parsing**: AST-based recursive descent parser with support for incremental updates.
- **Multi-platform Consistency**: Uses Compose Multiplatform for consistent rendering on Android, iOS, Desktop (JVM), and Web (Wasm/JS).
- **Automatic Line Breaking**: Smart line wrapping for long formulas at logical breakpoints (operators, relations).
- **Raster & Vector Export**: Export formulas as PNG/JPEG/WEBP images or resolution-independent SVG vectors.
- **Inline Math API**: Create Compose `InlineTextContent` directly with `LatexMeasurerState.inlineContent()`, while retaining precise pre-measurement support.
- **Accessibility**: Built-in screen reader support with MathSpeak-style formula descriptions (MathSpeak).
- **LaTeX → MathML**: Convert LaTeX AST to Presentation MathML output.
- **Formula Highlight**: Highlight sub-expressions within formulas via `HighlightConfig`.
- **Animation**: Animated formula transitions (crossfade / slide / fade+slide).
- **WYSIWYG Editor** *(Experimental)*: Built-in LaTeX editor with cursor positioning, tap-to-place, and real-time rendered preview.
- **Structured Diagnostics**: `parseWithDiagnostics()` provides 8-category structured diagnostics with severity filtering.
- **RTL Support**: Complete right-to-left text direction support (`\RLE`, `\LRE`, RTL/LTR environments, nesting).

## 📐 Supported LaTeX Features (450+)

<details>
<summary><b>Math Formulas</b> — fractions, roots, binomials, scripts</summary>

`\frac`, `\dfrac`, `\tfrac`, `\cfrac`, `\genfrac`, `\splitfrac`, `\splitdfrac`, `\binom`, `\tbinom`, `\dbinom`, and TeX infix forms `\over`, `\atop`, `\choose`, `\above`; plus `\sqrt`, `\sqrt[n]{x}`, `x_i`, `x^2`. Unbraced scripts consume exactly one character.
</details>

<details>
<summary><b>Symbols (130+)</b> — Greek letters, operators, arrows, AMS symbols</summary>

- **Greek letters**: all lowercase (α–ω, including `\omicron`), uppercase (Γ–Ω), KaTeX uppercase aliases (`\Alpha`, `\Beta`, `\Epsilon`, `\Omicron`, ...), and variants (ε/ϵ, θ/ϑ, φ/ϕ, `\varGamma`–`\varOmega`, etc.)
- **Operators**: `+`, `-`, `\times`, `\div`, `\pm`, `\mp`, `\cdot`, `\oplus`, `\otimes`, …
- **Relations**: `=`, `\neq`, `<`, `>`, `\leq`, `\geq`, `\approx`, `\equiv`, `\sim`, `\coloneqq`, `\eqqcolon`, …
- **Set theory**: `\in`, `\notin`, `\subset`, `\cup`, `\cap`, `\emptyset`, `\mathbb{R}`, …
- **Logic**: `\land`, `\lor`, `\neg`, `\Rightarrow`, `\Leftrightarrow`, `\forall`, `\exists`
- **Arrows**: `\to`, `\rightarrow`, `\leftarrow`, `\leftrightarrow`, `\Rightarrow`, `\hookrightarrow`, harpoons, …
- **Ellipsis**: `\ldots`, `\cdots`, `\vdots`, `\ddots`, `\dots` (auto-adaptive)
- **Negation**: `\not=`, `\not\in`, `\nleq`, `\ngeq`, `\ncong`, `\nmid`, … (30+ AMS negated relations)
- **AMS extras**: `\checkmark`, `\complement`, `\blacksquare`, `\aleph`, `\measuredangle`, geometric symbols, double-headed arrows, …
</details>

<details>
<summary><b>Large Operators (28)</b> — sums, integrals, limits, modular arithmetic</summary>

- **Sums/Integrals**: `\sum`, `\prod`, `\int`, `\oint`, `\iint`, `\iiint`, `\bigcup`, `\bigcap`, `\bigvee`, `\bigwedge`, `\coprod`, `\bigoplus`, `\bigotimes`, `\bigsqcup`, `\bigodot`, `\biguplus`
- **Limits**: `\lim`, `\max`, `\min`, `\sup`, `\inf`, `\limsup`, `\liminf`
- **Custom operators**: `\operatorname{name}`, `\operatorname*{name}`, `\DeclareMathOperator{\Tr}{Tr}`, `\mathop{content}`
- **Multi-line subscripts**: `\substack{cond1 \\ cond2}`
- **Modular arithmetic**: `\bmod` (binary), `\pmod{n}` (parenthesized), `\mod` (wide spacing)
- **Math class codes**: `\mathord`, `\mathbin`, `\mathrel`, `\mathopen`, `\mathclose`, `\mathpunct`, `\mathinner` control TeX atom class and inter-atom spacing
</details>

<details>
<summary><b>Matrices</b> — standard, starred, and compact environments</summary>

`matrix`, `pmatrix`, `bmatrix`, `Bmatrix`, `vmatrix`, `Vmatrix`, `smallmatrix`, their mathtools starred forms with `[l|c|r]`, plus `array` and `subarray`.
</details>

<details>
<summary><b>Delimiters</b> — auto-scaling & manual sizing</summary>

- **Auto-scaling**: `\left( \right)`, `\left[ \right]`, `\left\{ \right\}`, `\left| \right|`, and `\middle` inside a delimited expression; angle, floor, ceiling, and vertical-bar variants
- **Asymmetric**: `\left. \right|` (evaluation bar), `\left\{ \right.` (piecewise)
- **Manual sizing**: `\big`, `\Big`, `\bigg`, `\Bigg` with `\bigl`, `\bigr`, `\bigm` variants
</details>

<details>
<summary><b>Accents & Decorations (42+)</b> — accents, cancels, extensible arrows, stacking, bracket annotations</summary>

- **Accents**: `\hat`, `\tilde`, `\bar`, `\overline`, `\underline`, `\dot`, `\ddot`, `\dddot`, `\grave`, `\acute`, `\check`, `\widecheck`, `\breve`, `\ring`/`\mathring`, `\vec`, `\widehat`, `\overparen`, `\underparen`
- **Brace annotations**: `\overbrace{...}^{text}`, `\underbrace{...}_{text}`, `\overbracket{...}`, `\underbracket{...}`
- **Arrow decorations**: `\overrightarrow`, `\overleftarrow`, `\overleftrightarrow`, `\underleftarrow`, `\underrightarrow`
- **Cancel lines**: `\cancel`, `\bcancel` (reverse), `\xcancel` (cross)
- **Extensible arrows and equals**: `\xrightarrow`, `\xleftarrow`, `\xhookrightarrow`, `\xhookleftarrow`, `\xRightarrow`, `\xLeftarrow`, `\xLeftrightarrow`, `\xmapsto`, `\xlongequal`
- **Stacking**: `\overset`, `\underset`, `\stackrel`
</details>

<details>
<summary><b>Font Styles (17)</b></summary>

`\mathbf`, `\mathit`, `\mathrm`, `\mathsf`, `\mathtt`, `\mathbb`, `\mathfrak`, `\mathcal`, `\mathscr`, `\boldsymbol`, `\bm`, `\text`, `\mbox`, `\textnormal`, `\textup`, `\textmd`, `\textsc`, `\textsl`, `\emph`, and Unicode-math style aliases.
</details>

<details>
<summary><b>Font Sizes (10)</b></summary>

`\tiny`, `\scriptsize`, `\footnotesize`, `\small`, `\normalsize`, `\large`, `\Large`, `\LARGE`, `\huge`, `\Huge`
</details>

<details>
<summary><b>Math Mode Switching</b></summary>

`\displaystyle`, `\textstyle`, `\scriptstyle`, `\scriptscriptstyle`, `$...$` (inline), `$$...$$` (display)
</details>

<details>
<summary><b>Environments (21)</b> — alignment, piecewise, matrices, tables</summary>

- **Equation environments**: `equation(*)`, `displaymath`
- **Alignment environments**: `align(*)`, `aligned`, `flalign(*)`, `alignat(*)`, `alignedat`
- **Centering environments**: `gather(*)`, `gathered`
- **Piecewise functions**: `cases`, `dcases` (displaystyle), `rcases` (right brace), including starred forms
- **Multi-line/splitting**: `split`, `multline(*)`
- **Others**: `eqnarray(*)`, `subequations`, `tabular` (l/c/r column alignment)
- **Row controls and numbering**: `\\`, `\cr`, `\intertext`, `\shortintertext`, `\notag`, `\nonumber`, `\tag`/`\tag*`, `\label`/`\ref`/`\eqref`
</details>

<details>
<summary><b>Spacing</b></summary>

`\ `, named math spaces, `\hspace{...}`, `\kern`, `\mkern`, and `\allowbreak`. Escaped special characters such as `\{`, `\}`, `\$`, `\%`, `\#`, `\&`, and `\_` are also supported. `\|` produces a double vertical bar, equivalent to `\Vert`.
</details>

<details>
<summary><b>Colors & Background</b></summary>

- **Text color**: `\color{red}{...}`, `\textcolor{#FF5733}{...}` (named + hex)
- **Background color**: `\colorbox{yellow}{text}`, `\fcolorbox{borderColor}{bgColor}{text}`
</details>

<details>
<summary><b>Chemical Formulas (13)</b> — mhchem package</summary>

`\ce{H2O}`, ions, coefficients, isotope notation, single/double/triple bonds, and reaction arrows with `[above][below]` annotations; `\bond{...}` and `\pu{...}` are also accepted.
</details>

<details>
<summary><b>Special Effects & Layout (17)</b></summary>

- **Boxes**: `\boxed{E=mc^2}`, `\fbox{text}`
- **Menclose / enclose**: `\enclose{circle}{x}`, `\enclose{circle,box}{x}`, `\enclose{updiagonalstrike downdiagonalstrike}{x}`, `\circled{1}`
- **Supported notations**: `box`, `roundedbox`, `circle`, `left`, `right`, `top`, `bottom`, `updiagonalstrike`, `downdiagonalstrike`, `verticalstrike`, `horizontalstrike`
- **Supported attributes**: `mathcolor`, `mathbackground`
- **Phantoms & spacing**: `\phantom`, `\smash`, `\vphantom`, `\hphantom`, `\mathstrut`
- **Zero-width overlaps**: `\mathclap{content}`, `\mathllap{content}`, `\mathrlap{content}`
- **TeX layout**: `\clap`, `\llap`, `\rlap`, `\raisebox`, `\rule`
- **mathtools**: `\Aboxed`, `\MoveEqLeft`, `\splitfrac`, `\splitdfrac`, definition relations, and `\DeclarePairedDelimiter`
</details>

<details>
<summary><b>Physics & SI Units</b> — common physics and siunitx package subsets</summary>

- **Derivatives**: `\dv{x}`, `\dv{f}{x}`, `\dv[2]{f}{x}`, and the corresponding `\pdv` forms
- **Dirac notation and operators**: `\bra`/`\Bra`, `\ket`/`\Ket`, `\braket`/`\Braket`, `\comm`, `\anticomm`, `\eval`, `\vb`, `\va`, `\abs`, `\norm`
- **Numbers and units**: legacy `\SI`, `\si`; modern `\qty`, `\unit`, `\numrange`, `\qtyrange`, `\ang`; common SI prefix/unit macros
</details>

<details>
<summary><b>Advanced Annotations (6)</b> — hyperlinks, tensors, four-corner scripts</summary>

- **Hyperlinks**: `\href{url}{text}`, `\url{url}` (blue underline, click callback)
- **Four-corner scripts**: `\sideset{_a^b}{_c^d}{\sum}`
- **Pre-scripts**: `\prescript{A}{Z}{X}` (isotope notation)
- **Tensor indices**: `\tensor{T}{^a_b^c}`, `\indices{^a_b}`
</details>

<details>
<summary><b>Custom Commands & Macros (9)</b></summary>

`\newcommand`, `\renewcommand`, `\def` (0–9 parameters, optional argument defaults), `\newenvironment`, `\renewenvironment`, `\DeclarePairedDelimiter`
</details>

<details>
<summary><b>Section Structure Commands</b></summary>

`\section`, `\subsection`, `\subsubsection`, `\paragraph`, `\subparagraph` (with starred variants)
</details>

<details>
<summary><b>RTL Text Direction</b></summary>

- **Commands**: `\RLE{...}`, `\LRE{...}`, `\textarabic{...}`, `\texthebrew{...}`
- **Environments**: `\begin{RTL}...\end{RTL}`, `\begin{LTR}...\end{LTR}`
- **Nesting**: supports RTL inside LTR and vice versa
</details>

<details>
<summary><b>Labels & References</b></summary>

`\label`, `\ref`, `\eqref`, `\tag{1}`, `\tag*{A}`
</details>

<details>
<summary><b>Error Handling</b></summary>

- Unrecognized commands rendered in error color instead of silent failure
- `parseWithDiagnostics()` provides structured diagnostics (8 categories, filter by severity)
</details>

## 📸 Rendering Preview

The project includes a Demo App (`composeApp`/`androidApp`) showcasing various complex LaTeX scenarios. The preview dataset also contains a dedicated `Enclose / menclose` group covering circles, boxes, combined borders, strike-throughs, and color/background attributes:

| Basic Math | Chemical Formulas | Incremental Parsing |
| :---: | :---: | :---: |
| ![Basic Math](images/normal_latex.png) | ![Chemical Formulas](images/chemical_latex.png) | ![Incremental Parsing](images/incremental_latex.png) |
| Basic Math Rendering | Supports `\ce{...}` syntax | Real-time preview for incomplete input |

## 🛠️ Usage

In a Compose Multiplatform project, you can use the `Latex` component directly. The component handles incremental parsing automatically and supports real-time preview:

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

### Math Fonts

The bundled KaTeX TTF set is the renderer's only math-font pipeline. Its Main,
Math, AMS, style and Size1–Size4 fonts are loaded together with matching KaTeX
metrics, so measurement and drawing always use the same font data.

```kotlin
// Default: bundled KaTeX TTF fonts
LatexConfig()
```

External font injection is intentionally unsupported because KaTeX's layout
metrics are specific to the bundled font files.

### Theme Configuration

Use `LatexTheme` to control formula foreground/background colors:

```kotlin
import com.hrm.latex.renderer.model.LatexTheme

// Follow system light/dark mode
LatexConfig(theme = LatexTheme.auto())

// Fixed light theme
LatexConfig(theme = LatexTheme.light())

// Fixed dark theme
LatexConfig(theme = LatexTheme.dark())

// Follow current Material 3 ColorScheme
LatexConfig(theme = LatexTheme.material3())
```

If you need custom colors, build them from `LatexThemeColors`:

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

### Automatic Line Wrapping

For long formulas that need to wrap within the container width, use `LatexAutoWrap`:

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

Line breaks occur at mathematically valid points: relation operators (`=`, `<`, `>`), then additive operators (`+`, `-`), then multiplicative operators (`×`, `÷`). Atomic structures like fractions, roots, and matrices are never broken.

### Raster and SVG Export

Export rendered LaTeX formulas as PNG, JPEG, or WEBP images. Use `rememberLatexExporter()` in a Composable scope, then call `export()` on a background thread:

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
            // Export as PNG (default, 2x resolution)
            val result = exporter.export("E = mc^2")
            val pngBytes = result?.bytes       // PNG byte array
            val bitmap = result?.imageBitmap    // For in-app display

            // Export as JPEG (3x resolution, quality 85)
            val jpegResult = exporter.export(
                latex = "\\frac{a}{b}",
                exportConfig = ExportConfig(
                    scale = 3f,
                    format = ImageFormat.JPEG,
                    quality = 85
                )
            )

            // Export with transparent background (PNG only)
            val transparentResult = exporter.export(
                latex = "x^2 + y^2 = r^2",
                exportConfig = ExportConfig(transparentBackground = true)
            )
        }
    }) {
        Text("Export")
    }
}
```

`ExportConfig` parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `scale` | `Float` | `2f` | Resolution multiplier (1x, 2x, 3x, etc.) |
| `format` | `ImageFormat` | `PNG` | `ImageFormat.PNG`, `ImageFormat.JPEG`, or `ImageFormat.WEBP` |
| `transparentBackground` | `Boolean` | `false` | Use transparent background (PNG and WEBP only; JPEG always uses opaque background) |
| `quality` | `Int` | `90` | Compression quality (1–100) for JPEG and WEBP; ignored for PNG |

For print, responsive Web embedding, or resolution-independent output, use `exportSvg()`. It
reuses the exact same measure-and-draw pipeline as on-screen rendering and emits vector paths,
lines, and shapes—never a raster image wrapped in SVG:

```kotlin
val svgResult = exporter.exportSvg(
    latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
    exportConfig = SvgExportConfig(
        transparentBackground = true,
        textMode = SvgTextMode.PATH
    )
)

val svg = svgResult?.svg       // SVG text for Web embedding
val svgBytes = svgResult?.bytes // UTF-8 bytes for saving or sharing
```

`SvgExportConfig` is intentionally separate from raster options:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `scale` | `Float` | `1f` | Changes the SVG viewport and formula size; SVG remains resolution-independent |
| `transparentBackground` | `Boolean` | `true` | Transparent output, or the resolved `LatexConfig` background |
| `textMode` | `SvgTextMode` | `PATH` | `PATH` embeds portable glyph outlines; `TEXT` is smaller/selectable but requires matching fonts |
| `prettyPrint` | `Boolean` | `true` | Format the generated XML |

### Accessibility

The library provides built-in accessibility support for screen readers. When enabled, each `Latex` component exposes a MathSpeak-style natural language description via Compose semantics, making math formulas readable by TalkBack (Android), VoiceOver (iOS), and other assistive technologies.

```kotlin
Latex(
    latex = "\\frac{1}{2}",
    config = LatexConfig(accessibilityEnabled = true)
)
// Screen reader reads: "fraction: 1 over 2"
```

The `AccessibilityVisitor` converts the LaTeX AST into descriptive text covering fractions, roots, superscripts/subscripts, matrices, Greek letters, operators, and more.

### Inline Math Support

`inlineContent()` encapsulates formula pre-measurement, `Placeholder` sizing, and rendering for direct use with Compose `Text`:

```kotlin
val measurer = rememberLatexMeasurer(config)
val formula = measurer.inlineContent("\\frac{a}{b}", config)

Text(
    text = buildAnnotatedString {
        append("The fraction ")
        appendInlineContent("formula", "a divided by b")
        append(" represents a divided by b.")
    },
    inlineContent = formula?.let { mapOf("formula" to it) }.orEmpty()
)
```

For blank input or measurement failure, `inlineContent()` returns `null`, allowing Compose to display the alternate text supplied to `appendInlineContent()`. Repeated calls with the same formula and configuration reuse the `LatexMeasurerState` cache.

For custom layouts, the lower-level `measure()` API remains available. `LatexDimensions` provides `widthPx`, `heightPx`, and `baselinePx` (including padding), plus corresponding content-only fields. Use `measureBatch()` for batch measurement.

### WYSIWYG Editor (Experimental)

> **Note**: The editor API is experimental and may change in future versions. All editor APIs require the `@ExperimentalComposeUiApi` annotation.

The library includes a built-in WYSIWYG (What You See Is What You Get) LaTeX editor component. Users can edit LaTeX source text and see the rendered formula in real-time, with cursor position synchronized between the source and the rendered output.

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MyEditor() {
    val editorState = rememberEditorState(initialText = "x^{2} + y^{2} = r^{2}")

    LatexEditor(
        editorState = editorState,
        config = LatexConfig(fontSize = 20.sp),
        showSourceText = true // Show source text input field
    )
}
```

## 📦 Installation

### Version Compatibility

This library publishes two variants for each release to support different Kotlin/Compose versions:

| Variant | Kotlin | Compose Multiplatform | Artifact Version |
|---------|--------|-----------------------|------------------|
| **Standard** | 2.3.10 | 1.10.3 | `1.5.0`          |
| **Kotlin 2.1.0** | 2.1.0 | 1.9.3 | `1.5.0-kt2.1.0`  |

> Choose the variant that matches your project's Kotlin version. If your project uses **Kotlin 2.1.0**, use the `-kt2.1.0` suffixed version.

### Standard Version (Kotlin 2.3.10)

Add dependencies in `gradle/libs.versions.toml`:

```toml
[versions]
latex = "1.5.0"

[libraries]
latex-base = { module = "io.github.huarangmeng:latex-base", version.ref = "latex" }
latex-parser = { module = "io.github.huarangmeng:latex-parser", version.ref = "latex" }
latex-renderer = { module = "io.github.huarangmeng:latex-renderer", version.ref = "latex" }
```

### Kotlin 2.1.0 Compatible Version

If your project uses Kotlin 2.1.0, use the `-kt2.1.0` suffixed artifacts:

```toml
[versions]
latex = "1.5.0-kt2.1.0"

[libraries]
latex-base = { module = "io.github.huarangmeng:latex-base", version.ref = "latex" }
latex-parser = { module = "io.github.huarangmeng:latex-parser", version.ref = "latex" }
latex-renderer = { module = "io.github.huarangmeng:latex-renderer", version.ref = "latex" }
```

### Add to Your Module

Reference in your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.latex.base) // Basic logging
    implementation(libs.latex.renderer) // Rendering logic
    implementation(libs.latex.parser) // Parsing logic
}
```

## 🏗️ Project Structure

- `:latex-base`: Base data structures and interfaces.
- `:latex-parser`: Core parsing engine, responsible for converting LaTeX strings to AST.
- `:latex-renderer`: Responsible for rendering AST into Compose UI components.
- `:latex-preview`: Preview components and sample datasets.
- `:composeApp`: Cross-platform Demo application.
- `:androidApp`: Android Demo application.

## 🚀 Quick Start

### Running the Demo App

- **Android**: `./gradlew :androidApp:assembleDebug`
- **Desktop**: `./gradlew :composeApp:run`
- **Web (Wasm)**: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- **iOS**: Open `iosApp/iosApp.xcworkspace` in Xcode to run.

### Running Tests

```bash
./run_parser_tests.sh
```

## 📊 Coverage

For supported features and chapter-specific follow-up items, see [PARSER_COVERAGE_ANALYSIS.md](./latex-parser/PARSER_COVERAGE_ANALYSIS.md).

## 🙏 Acknowledgements

- [KaTeX](https://github.com/KaTeX/KaTeX) — This project uses the KaTeX v0.16.11 font files for mathematical formula rendering. See [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) for attribution and license details.

## 💡 Recommended

- [Markdown](https://github.com/huarangmeng/Markdown) — A Kotlin Multiplatform Markdown parsing and rendering library by the same author. If you need both LaTeX and Markdown rendering in your project, check it out!

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026 huarangmeng

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
