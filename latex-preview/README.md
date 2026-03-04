# LaTeX Preview Module

这是一个独立的预览模块，仅用于本地开发和测试 LaTeX 渲染效果。

## 特性

- ✅ 实时预览 LaTeX 渲染效果
- ✅ 支持深色/浅色主题切换
- ✅ 提供常用示例快速测试
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
