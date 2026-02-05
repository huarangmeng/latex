# Kotlin Multiplatform LaTeX Rendering Library

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android%20API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.huarangmeng/latex-base.svg)](https://central.sonatype.com/search?q=io.github.huarangmeng.latex)

这是一个基于 Kotlin Multiplatform (KMP) 开发的高性能 LaTeX 数学公式解析与渲染库。支持在 Android, iOS, Desktop (JVM) 和 Web (Wasm/JS) 平台上实现一致的渲染效果。

[English Version](./README.md)

## 🌟 核心特性

- **完整语法支持**：涵盖 100+ 希腊字母、基础算术、矩阵、环境（align, cases, array 等）。
- **自定义命令 (New!)**：支持 `\newcommand` 宏定义，包括参数替换（#1-#9）、嵌套定义和命令覆盖。
- **高性能解析**：基于 AST 的递归下降解析器，支持增量更新。
- **多平台一致性**：使用 Compose Multiplatform 实现跨平台 UI 渲染。
- **化学公式支持**：内置 `\ce{...}` 插件支持。
- **样式定制**：支持颜色（`\color`）、方框（`\boxed`）和数学模式切换（`\displaystyle` 等）。
- **自动换行**：长公式在逻辑断点（运算符、关系符）处智能换行。

## 📸 渲染预览

项目包含一个演示 App (`composeApp`/`androidApp`)，展示了各种复杂的 LaTeX 场景：

| 基础数学 | 化学公式 | 增量解析 |
| :---: | :---: | :---: |
| ![基础数学](images/normal_latex.png) | ![化学公式](images/chemical_latex.png) | ![增量解析](images/incremental_latex.png) |
| 基础数学公式渲染 | 支持 `\ce{...}` 语法 | 支持不完整输入的实时预览 |

## 🛠️ 使用方法

在 Compose Multiplatform 项目中，你可以直接使用 `Latex` 组件。该组件会自动处理增量解析，支持实时预览：

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
            darkColor = Color.White // 自动支持深色模式
        )
    )
}
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

## 📦 安装

在 `gradle/libs.versions.toml` 中添加依赖：

```toml
[versions]
latex = "1.0.3"

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
