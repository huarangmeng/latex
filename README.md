# Kotlin Multiplatform LaTeX Rendering Library

这是一个基于 Kotlin Multiplatform (KMP) 开发的高性能 LaTeX 数学公式解析与渲染库。支持在 Android, iOS, Desktop (JVM) 和 Web (Wasm/JS) 平台上实现一致的渲染效果。

## 🌟 核心特性

- **完整语法支持**：涵盖 100+ 希腊字母、基础算术、矩阵、环境（align, cases, array 等）。
- **自定义命令 (New!)**：支持 `\newcommand` 宏定义，包括参数替换（#1-#9）、嵌套定义和命令覆盖。
- **高性能解析**：基于 AST 的递归下降解析器，支持增量更新。
- **多平台一致性**：使用 Compose Multiplatform 实现跨平台 UI 渲染。
- **化学公式支持**：内置 `\ce{...}` 插件支持。
- **样式定制**：支持颜色（`\color`）、方框（`\boxed`）和数学模式切换（`\displaystyle` 等）。

## 📸 渲染预览

项目包含一个演示 App (`composeApp`/`androidApp`)，展示了各种复杂的 LaTeX 场景：

- **基础数学**：`\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}`
- **矩阵与对齐**：`\begin{pmatrix} a & b \\ c & d \end{pmatrix}`
- **自定义宏**：`\newcommand{\R}{\mathbb{R}} x \in \R`

## 📦 安装

在 `gradle/libs.versions.toml` 中添加依赖：

```toml
[versions]
latex = "0.0.2"

[libraries]
latex-base = { module = "io.github.huarangmeng:latex-base", version.ref = "latex" }
latex-parser = { module = "io.github.huarangmeng:latex-parser", version.ref = "latex" }
latex-renderer = { module = "io.github.huarangmeng:latex-renderer", version.ref = "latex" }
```

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

## 📊 路线图与功能覆盖

详细的功能支持列表请参阅：[PARSER_COVERAGE_ANALYSIS.md](./latex-parser/PARSER_COVERAGE_ANALYSIS.md)