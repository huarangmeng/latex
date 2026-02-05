# Kotlin Multiplatform LaTeX Rendering Library

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android%20API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.huarangmeng/latex-base.svg)](https://central.sonatype.com/search?q=io.github.huarangmeng.latex)

A high-performance LaTeX mathematical formula parsing and rendering library developed based on Kotlin Multiplatform (KMP). It supports consistent rendering effects on Android, iOS, Desktop (JVM), and Web (Wasm/JS) platforms.

[中文版本](./README_zh.md)

## 🌟 Key Features

- **Full Syntax Support**: Covers 100+ Greek letters, basic arithmetic, matrices, and environments (align, cases, array, etc.).
- **Custom Commands (New!)**: Supports `\newcommand` macro definitions, including parameter replacement (#1-#9), nested definitions, and command overriding.
- **High-Performance Parsing**: AST-based recursive descent parser with support for incremental updates.
- **Multi-platform Consistency**: Uses Compose Multiplatform for cross-platform UI rendering.
- **Chemical Formula Support**: Built-in support for the `\ce{...}` plugin.
- **Style Customization**: Supports colors (`\color`), boxes (`\boxed`), and math mode switching (`\displaystyle`, etc.).
- **Automatic Line Breaking**: Smart line wrapping for long formulas at logical breakpoints (operators, relations).

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

## 📦 Installation

Add dependencies in `gradle/libs.versions.toml`:

```toml
[versions]
latex = "1.0.3"

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
