# AGENTS.md

Brief guide for coding agents working in this repository.

## Project

Kotlin Multiplatform LaTeX parsing and rendering library with demo apps.

Core pipeline:

`String -> Tokenizer -> Parser -> AST -> Visitors / Renderer -> Compose UI`

Main modules:

- `latex-base`: shared base types and SDK entry points
- `latex-parser`: tokenizer, parser, AST, incremental parsing, diagnostics, visitors
- `latex-renderer`: Compose measurement and rendering engine
- `latex-preview`: local preview/sample dataset
- `latex-benchmark`: benchmark-only module
- `composeApp`: Compose Multiplatform demo app
- `androidapp` / `iosApp`: native app entry points

## Commands

Use exact commands unless the user asks otherwise:

- Android build: `./gradlew :androidapp:assembleDebug`
- Desktop demo: `./gradlew :composeApp:run`
- Preview module: `./gradlew :latex-preview:run`
- Web demo (Wasm): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- Web demo (JS): `./gradlew :composeApp:jsBrowserDevelopmentRun`
- Parser tests: `./run_parser_tests.sh`
- Parser tests direct: `./gradlew :latex-parser:cleanJvmTest :latex-parser:jvmTest`
- One parser test: `./gradlew :latex-parser:jvmTest --tests "*SpecialEffectTest"`
- Renderer JVM compile: `./gradlew :latex-renderer:compileKotlinJvm`
- Preview JVM compile: `./gradlew :latex-preview:compileKotlinJvm`
- Full checks: `./gradlew check`
- Clean: `./gradlew clean`

Do not invent lint/format commands unless confirmed in the repo.

## Code Map

Parser:

- `latex-parser/.../LatexParser.kt`
- `latex-parser/.../IncrementalLatexParser.kt`
- `latex-parser/.../tokenizer/`
- `latex-parser/.../model/LatexNode.kt`
- `latex-parser/.../component/`
- `latex-parser/.../visitor/`
- `latex-parser/.../util/LatexPrinter.kt`

Renderer:

- `latex-renderer/.../Latex.kt`
- `latex-renderer/.../layout/`
- `latex-renderer/.../editor/`
- `latex-renderer/.../export/`

Preview:

- `latex-preview/.../preview/BasicLatexPreview.kt`
- `latex-preview/.../preview/PreviewGroupList.kt`
- `composeApp/src/commonMain/kotlin/com/hrm/latex/App.kt`

## Core Rules

- Keep the whole pipeline aligned. New LaTeX features usually need parser, AST, visitors, renderer/preview, tests, and docs together.
- Prefer reusable AST abstractions over command-specific hacks.
- Keep shared logic in `commonMain`; do not move parser/layout logic into platform source sets.
- Preserve module direction: `latex-parser` must not depend on renderer code.

## Parser Rules

- Register commands in the appropriate handler under `latex-parser/.../component/handler/`.
- Font/style aliases must stay centralized in `StyleHandlers.kt`.
- If you add a new semantic `LatexNode`, update at least:
  - `LatexNode.kt`
  - `LatexVisitor.kt`
  - `MathMLVisitor.kt`
  - `AccessibilityVisitor.kt`
  - `LatexPrinter.kt`
- If the node affects rendering, update renderer measurer/layout code too.

## Preview Rules

- Any user-visible feature should get preview coverage in `latex-preview`.
- Reuse an existing group when it fits; create a dedicated group when the feature is substantial.

## Testing

- Add tests to the nearest feature-specific file under `latex-parser/src/commonTest/...`.
- Prefer existing focused files such as:
  - `SpecialEffectTest.kt`
  - `MathMLVisitorTest.kt`
  - `AccessibilityVisitorTest.kt`
  - `ComplexStructureTest.kt`
  - `DelimiterTest.kt`
  - `SectionHeadingTest.kt`
- For renderer changes, run at least `:latex-renderer:compileKotlinJvm`.
- For visible changes, verify with `:composeApp:run` or `:latex-preview:run`.

Recommended validation order:

1. Focused tests for the touched area
2. Owning module compile/test command
3. Preview/demo run for visible changes
4. `./gradlew check` only when justified by scope

## Docs Sync

User-visible feature support must keep docs in sync. Review:

- `README.md`
- `README_zh.md`
- `latex-parser/PARSER_COVERAGE_ANALYSIS.md`
- `latex-preview/README.md`

Typical pattern:

- parser feature -> coverage analysis
- rendered feature -> README
- visual feature -> preview samples

## Release Notes

- Follow Conventional Commits in `CONTRIBUTING.md`.
- Recommended scopes: `parser`, `renderer`, `preview`, `base`, `build`, `android`, `ios`, `app`.
- Maven Central versions come from release tags in `.github/workflows/publish.yml`.
- Never add secrets, tokens, signing keys, or local credentials.

## Working Style

- Read the relevant module files first.
- Explain the plan before large refactors or cross-module changes.
- Prefer small, coherent edits over speculative broad changes.
- Validate with the narrowest useful command first.
- Do not overwrite or revert user changes unless explicitly asked.

## Avoid

- Adding parser support without visitor support for a new AST node
- Adding user-visible features without preview coverage
- Forgetting to update docs after adding command support
- Putting shared parsing/layout logic into platform source sets
- Scattering style aliases outside `StyleHandlers.kt`
- Guessing commands that are not defined by the repository
