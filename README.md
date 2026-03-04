# Kotlin Multiplatform LaTeX Rendering Library

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android%20API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.huarangmeng/latex-base.svg)](https://central.sonatype.com/search?q=io.github.huarangmeng.latex)

A high-performance LaTeX mathematical formula parsing and rendering library developed based on Kotlin Multiplatform (KMP). It supports consistent rendering effects on Android, iOS, Desktop (JVM), and Web (Wasm/JS) platforms.

[中文版本](./README_zh.md)

## 🌟 Key Features

- **High-Performance Parsing**: AST-based recursive descent parser with support for incremental updates.
- **Multi-platform Consistency**: Uses Compose Multiplatform for consistent rendering on Android, iOS, Desktop (JVM), and Web (Wasm/JS).
- **Automatic Line Breaking**: Smart line wrapping for long formulas at logical breakpoints (operators, relations).
- **Image Export**: Export rendered formulas as PNG/JPEG/WEBP images with configurable resolution scaling.
- **Pre-measurement API**: Synchronous pre-measurement of formula dimensions (width/height/baseline) for Compose `InlineTextContent` inline math embedding.
- **Accessibility**: Built-in screen reader support with MathSpeak-style formula descriptions (MathSpeak).
- **LaTeX → MathML**: Convert LaTeX AST to Presentation MathML output.
- **Formula Highlight**: Highlight sub-expressions within formulas via `HighlightConfig`.
- **Animation**: Animated formula transitions (crossfade / slide / fade+slide).
- **WYSIWYG Editor** *(Experimental)*: Built-in LaTeX editor with cursor positioning, tap-to-place, and real-time rendered preview.

## 📐 Supported LaTeX Features (333+)

<details>
<summary><b>Math Formulas</b> — fractions, roots, binomials</summary>

`\frac`, `\dfrac`, `\tfrac`, `\cfrac`, `\binom`, `\tbinom`, `\dbinom`, `\sqrt`, `\sqrt[n]{x}`
</details>

<details>
<summary><b>Symbols (130+)</b> — Greek letters, operators, arrows, AMS symbols</summary>

- **Greek letters**: all lowercase (α–ω), uppercase (Γ–Ω), and variants (ε/ϵ, θ/ϑ, φ/ϕ, etc.)
- **Operators**: `+`, `-`, `\times`, `\div`, `\pm`, `\mp`, `\cdot`, `\oplus`, `\otimes`, …
- **Relations**: `=`, `\neq`, `<`, `>`, `\leq`, `\geq`, `\approx`, `\equiv`, `\sim`, `\ll`, `\gg`, …
- **Set theory**: `\in`, `\notin`, `\subset`, `\cup`, `\cap`, `\emptyset`, `\mathbb{R}`, …
- **Logic**: `\land`, `\lor`, `\neg`, `\Rightarrow`, `\Leftrightarrow`, `\forall`, `\exists`
- **Arrows**: `\to`, `\rightarrow`, `\leftarrow`, `\leftrightarrow`, `\Rightarrow`, `\hookrightarrow`, harpoons, …
- **Ellipsis**: `\ldots`, `\cdots`, `\vdots`, `\ddots`, `\dots` (auto-adaptive)
- **Negation**: `\not=`, `\not\in`, `\nleq`, `\ngeq`, `\ncong`, `\nmid`, … (30+ AMS negated relations)
- **AMS extras**: `\checkmark`, `\complement`, `\blacksquare`, `\aleph`, `\measuredangle`, geometric symbols, double-headed arrows, …
</details>

<details>
<summary><b>Large Operators (28)</b> — sums, integrals, limits</summary>

`\sum`, `\prod`, `\int`, `\oint`, `\iint`, `\iiint`, `\bigcup`, `\bigcap`, `\bigvee`, `\bigwedge`, `\coprod`, `\bigoplus`, `\bigotimes`, `\bigsqcup`, `\bigodot`, `\biguplus`, `\lim`, `\max`, `\min`, `\sup`, `\inf`, `\limsup`, `\liminf`, `\operatorname`, `\substack`, `\DeclareMathOperator`, `\mathop`
</details>

<details>
<summary><b>Matrices (8)</b> — all standard matrix environments</summary>

`matrix`, `pmatrix`, `bmatrix`, `Bmatrix`, `vmatrix`, `Vmatrix`, `smallmatrix`, `array`
</details>

<details>
<summary><b>Delimiters</b> — auto-scaling & manual sizing</summary>

- **Auto-scaling**: `\left( \right)`, `\left[ \right]`, `\left\{ \right\}`, `\left| \right|`, `\langle`, `\rangle`, `\lfloor`, `\rfloor`, `\lceil`, `\rceil`, `\lvert`, `\rvert`, `\lVert`, `\rVert`
- **Asymmetric**: `\left. \right|` (evaluation bar), `\left\{ \right.` (piecewise)
- **Manual sizing**: `\big`, `\Big`, `\bigg`, `\Bigg` with `\bigl`, `\bigr`, `\bigm` variants
</details>

<details>
<summary><b>Accents & Decorations (31)</b></summary>

`\hat`, `\tilde`, `\bar`, `\overline`, `\underline`, `\dot`, `\ddot`, `\dddot`, `\grave`, `\acute`, `\check`, `\breve`, `\ring`, `\vec`, `\overbrace`, `\underbrace`, `\widehat`, `\overrightarrow`, `\overleftarrow`, `\cancel`, `\bcancel`, `\xcancel`, `\xrightarrow`, `\xleftarrow`, `\xhookrightarrow`, `\xhookleftarrow`, `\xleftrightarrow`, `\overset`, `\underset`, `\stackrel`
</details>

<details>
<summary><b>Font Styles (17)</b></summary>

`\mathbf`, `\mathit`, `\mathrm`, `\mathsf`, `\mathtt`, `\mathbb`, `\mathfrak`, `\mathcal`, `\mathscr`, `\boldsymbol`, `\bm`, `\text`, `\mbox`, `\symbf`, `\symit`, `\symsf`, `\symrm`
</details>

<details>
<summary><b>Math Mode Switching</b></summary>

`\displaystyle`, `\textstyle`, `\scriptstyle`, `\scriptscriptstyle`, `$...$` (inline), `$$...$$` (display)
</details>

<details>
<summary><b>Environments (21)</b></summary>

`equation(*)`, `displaymath`, `align(*)`, `aligned`, `gather(*)`, `gathered`, `cases`, `dcases`, `rcases`, `split`, `multline(*)`, `eqnarray(*)`, `subequations`, `tabular`, `flalign(*)`, `alignat(*)`
</details>

<details>
<summary><b>Spacing</b></summary>

`\,`, `\:`, `\;`, `\quad`, `\qquad`, `\!`, `\hspace{...}`, normal spaces
</details>

<details>
<summary><b>Advanced Features</b></summary>

- **Colors**: `\color{red}{...}`, `\textcolor{#FF5733}{...}` (named + hex)
- **Chemical formulas**: `\ce{H2O}`, `\ce{A + B -> C}`, `\ce{A <=> B}`, ions, coefficients
- **Boxes & phantoms**: `\boxed`, `\phantom`, `\smash`, `\vphantom`, `\hphantom`
- **Tags**: `\tag{1}`, `\tag*{A}`
- **Custom commands**: `\newcommand`, `\renewcommand`, `\def` (0–9 parameters)
- **Labels & refs**: `\label`, `\ref`, `\eqref`
- **Tensor notation**: `\sideset`, `\tensor`, `\indices`
- **Modular arithmetic**: `\bmod`, `\pmod`, `\mod`
- **Error handling**: Unrecognized commands rendered in error color instead of silent failure
</details>

## 📸 Rendering Preview

The project includes a Demo App (`composeApp`/`androidApp`) showcasing various complex LaTeX scenarios:

| Basic Math | Chemical Formulas | Incremental Parsing |
| :---: | :---: | :---: |
| ![Basic Math](images/normal_latex.png) | ![Chemical Formulas](images/chemical_latex.png) | ![Incremental Parsing](images/incremental_latex.png) |
| Basic Math Rendering | Supports `\ce{...}` syntax | Real-time preview for incomplete input |

## 🛠️ Usage

In a Compose Multiplatform project, you can use the `Latex` component directly. The component handles incremental parsing automatically and supports real-time preview:

```kotlin
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun MyScreen() {
    Latex(
        latex = "\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}",
        config = LatexConfig(
            fontSize = 20.sp,
            color = Color.Black,
            darkColor = Color.White // Automatic dark mode support
        )
    )
}
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

### Image Export

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

### Pre-measurement API (Inline Math Support)

Pre-measure formula dimensions for embedding inline math via `InlineTextContent`:

```kotlin
val measurer = rememberLatexMeasurer(config)
val dims = measurer.measure("\\frac{a}{b}", config) ?: return

val placeholder = Placeholder(
    width = with(density) { dims.widthPx.toSp() },
    height = with(density) { dims.heightPx.toSp() },
    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
)
```

`LatexDimensions` provides `widthPx`, `heightPx`, `baselinePx` (with padding) and their content-only counterparts. Use `measureBatch()` for multiple formulas.

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

Add dependencies in `gradle/libs.versions.toml`:

```toml
[versions]
latex = "1.2.1"

[libraries]
latex-base = { module = "io.github.huarangmeng:latex-base", version.ref = "latex" }
latex-parser = { module = "io.github.huarangmeng:latex-parser", version.ref = "latex" }
latex-renderer = { module = "io.github.huarangmeng:latex-renderer", version.ref = "latex" }
```

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

## 📊 Roadmap & Coverage

For a detailed list of supported features, please refer to: [PARSER_COVERAGE_ANALYSIS.md](./latex-parser/PARSER_COVERAGE_ANALYSIS.md)

## 🙏 Acknowledgements

- [KaTeX](https://github.com/KaTeX/KaTeX) — This project uses font files from KaTeX for mathematical formula rendering. KaTeX is licensed under the [MIT License](https://github.com/KaTeX/KaTeX/blob/main/LICENSE).

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
