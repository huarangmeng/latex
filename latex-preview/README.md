# LaTeX Preview Module

这是一个独立的预览模块，仅用于本地开发和测试 LaTeX 渲染效果。

## 特性

- ✅ 实时预览 LaTeX 渲染效果
- ✅ 支持 `LatexTheme.auto()` / `light()` / `dark()` / `material3()` 四种主题写法
- ✅ 提供常用示例快速测试
- ✅ 提供 `inlineContent()` 单公式及多公式 Compose 富文本混排示例
- ✅ 提供 PNG/JPEG/WEBP 光栅与 SVG PATH/TEXT 矢量导出预览
- ✅ 不参与 SDK 最终打包

## 运行

```bash
# 运行桌面预览应用
./gradlew :latex-preview:run
```

## 注意事项

- 此模块仅用于开发测试，不会被包含在 `latex-sdk` 的发布版本中
- 只支持 JVM/Desktop 平台
- 可以在这里测试各种 LaTeX 表达式的渲染效果
- `BasicLatexPreview` 中包含独立的主题示例分组，可直接验证 `LatexTheme` 新 API 的表现
- `数学模式切换` 分组包含 KaTeX 字体路由、Rule 15/18、积分上下限、重音符号和定界符数学轴回归样例
- `physics / siunitx 常用子集` 分组覆盖导数、狄拉克符号、对易子、向量和现代 SI 单位排版
- `数学类代码` 分组演示 `\mathord/\mathbin/\mathrel/\mathopen/\mathclose/\mathpunct/\mathinner` 对原子间距的影响
- `SVG 矢量导出` 分组可比较可移植 PATH 模式与可选择 TEXT 模式的尺寸和文件体积
