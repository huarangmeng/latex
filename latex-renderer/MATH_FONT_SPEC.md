# 数学字体架构设计：OTF MATH 表 + TTF 双轨支持

> 本文档定义 latex-renderer 模块的**终极字体架构**，目标是同时支持两种数学字体体系：
> - **OTF 数学字体**：单个 `.otf` 文件，内含 OpenType MATH 表（如 STIX Two Math, Latin Modern Math, XITS Math, Cambria Math）
> - **TTF 字体集**：多个 `.ttf` 文件组成的字体族（如当前的 KaTeX 20 个 TTF）
>
> 设计不受当前实现约束，追求理论最优。

---

## 1. 为什么需要支持 OTF MATH 表

### 1.1 OTF MATH 表是什么

OpenType MATH 表（[规范](https://learn.microsoft.com/en-us/typography/opentype/spec/math)）是 OpenType 字体中的一个专用数据表，由微软为 Cambria Math 首次实现，现已成为数学排版的工业标准。它在**单个字体文件**中包含数学排版所需的全部信息：

| 子表 | 内容 | 条目数 |
|------|------|--------|
| **MathConstants** | 全局排版常量（轴高度、分数线粗细、上下标偏移、根号间距等） | ~60 个精确值 |
| **MathGlyphInfo** | 每个字形的斜体修正、重音附着点、数学字距 | 逐字形精确值 |
| **MathVariants** | 定界符/运算符的尺寸变体列表 + 字形组装部件 | 支持任意高度拼装 |

### 1.2 带 MATH 表的主流数学字体

| 字体 | 许可证 | 风格 | 特点 |
|------|--------|------|------|
| **STIX Two Math** | OFL | Times 风格 | 符号最全（~3600 数学字形），学术出版标准 |
| **Latin Modern Math** | GUST | Computer Modern 风格 | 与经典 LaTeX 输出一致 |
| **XITS Math** | OFL | 基于 STIX 改良 | 较早的完整 MATH 表实现 |
| **Cambria Math** | 微软商用 | 现代衬线 | 首个 MATH 表字体，Office 标准 |
| **Fira Math** | OFL | 无衬线 | 适合演示/幻灯片 |
| **Garamond Math** | OFL | Garamond 风格 | 古典优雅 |
| **Asana Math** | OFL | Palatino 风格 | 高质量排版 |

### 1.3 OTF MATH 表 vs TTF 字体集的根本差异

| 维度 | TTF 字体集 (KaTeX) | OTF MATH 表 |
|------|-------------------|-------------|
| **文件数量** | 20 个 TTF | **1 个 OTF** |
| **排版参数来源** | 代码中硬编码（~70 个 `const val`） | **字体文件内嵌**（字体设计师精确调校） |
| **斜体修正** | 启发式估算（`fontSizePx * 0.15f`） | **逐字形精确值**（MathItalicsCorrectionInfo） |
| **重音定位** | 居中 + 粗略修正 | **逐字形精确附着点**（MathTopAccentAttachment） |
| **定界符伸缩** | 5 级离散跳跃 (Main→Size1→Size2→Size3→Size4) + fontSize 兜底 | **连续尺寸变体 + 字形组装**（任意高度，笔画一致） |
| **字形组装** | ❌ 不支持 | ✅ 任意高度/宽度的括号、根号顶线、水平花括号 |
| **数学字距** | ❌ 无 | ✅ 高度敏感的动态字距（MathKernInfo） |
| **字体生态** | 仅 KaTeX 字体 | 所有带 MATH 表的字体（10+ 种风格） |
| **排版质量上限** | 受限于硬编码精度 | **字体级精度**（与 XeTeX/LuaTeX 同级） |

---

## 2. 架构设计：MathFontProvider 抽象层

### 2.1 核心思想

引入 `MathFontProvider` 接口作为**排版参数的唯一来源**。所有 Measurer 不再直接引用硬编码常量或字体文件，而是通过此接口获取排版所需的全部信息。两种字体体系各自实现此接口。

### 2.2 接口定义

```kotlin
/**
 * 数学字体排版参数的统一抽象。
 *
 * 这是整个渲染引擎的"字体知识"入口。所有排版决策（间距、对齐、伸缩）
 * 都通过此接口获取字体特定的精确值，而非硬编码。
 *
 * 两种实现：
 * - OtfMathFontProvider: 从 OTF 文件的 MATH 表动态读取
 * - TtfFontSetProvider: 基于 KaTeX TTF 字体集，提供与当前等价的硬编码值
 */
interface MathFontProvider {

    // ─── 全局排版常量（对应 MATH 表的 MathConstants） ───

    /** 数学轴高度（px），分数线、运算符的垂直对称中心 */
    fun axisHeight(fontSizePx: Float): Float

    /** 分数线粗细（px） */
    fun fractionRuleThickness(fontSizePx: Float): Float

    /** 分数分子与分数线的最小间距（px），Display 模式 */
    fun fractionNumeratorDisplayGap(fontSizePx: Float): Float

    /** 分数分子与分数线的最小间距（px），Text 模式 */
    fun fractionNumeratorGap(fontSizePx: Float): Float

    /** 分数分母与分数线的最小间距（px），Display 模式 */
    fun fractionDenominatorDisplayGap(fontSizePx: Float): Float

    /** 分数分母与分数线的最小间距（px），Text 模式 */
    fun fractionDenominatorGap(fontSizePx: Float): Float

    /** 上标上移量（px） */
    fun superscriptShiftUp(fontSizePx: Float): Float

    /** 下标下移量（px） */
    fun subscriptShiftDown(fontSizePx: Float): Float

    /** 上下标之间最小间距（px） */
    fun subSuperscriptGapMin(fontSizePx: Float): Float

    /** SCRIPT 样式缩放百分比 (如 80 表示 80%) */
    fun scriptPercentScaleDown(): Int

    /** SCRIPT_SCRIPT 样式缩放百分比 (如 60 表示 60%) */
    fun scriptScriptPercentScaleDown(): Int

    /** 根号内容顶部到横线的最小间距（px），Display 模式 */
    fun radicalDisplayVerticalGap(fontSizePx: Float): Float

    /** 根号内容顶部到横线的最小间距（px），非 Display 模式 */
    fun radicalVerticalGap(fontSizePx: Float): Float

    /** 根号横线粗细（px） */
    fun radicalRuleThickness(fontSizePx: Float): Float

    /** 上限与运算符之间的最小间距（px） */
    fun upperLimitGapMin(fontSizePx: Float): Float

    /** 下限与运算符之间的最小间距（px） */
    fun lowerLimitGapMin(fontSizePx: Float): Float

    /** 上划线/overbar 间距（px） */
    fun overbarVerticalGap(fontSizePx: Float): Float

    /** 下划线/underbar 间距（px） */
    fun underbarVerticalGap(fontSizePx: Float): Float

    /** 重音符号在此高度以上的基础字形不抬升（px） */
    fun accentBaseHeight(fontSizePx: Float): Float

    /** 堆叠元素最小间距（px），Display 模式 */
    fun stackDisplayGapMin(fontSizePx: Float): Float

    /** 堆叠元素最小间距（px），非 Display 模式 */
    fun stackGapMin(fontSizePx: Float): Float

    // ─── 逐字形信息（对应 MATH 表的 MathGlyphInfo） ───

    /**
     * 获取指定字形的斜体修正值（px）。
     *
     * 用于确定上标相对于基础字形的水平偏移，
     * 防止斜体字形的突出部分与上标碰撞。
     *
     * @param glyphChar Unicode 字符
     * @param fontSizePx 字号（px）
     * @return 斜体修正值，无数据时返回 0
     */
    fun italicCorrection(glyphChar: String, fontSizePx: Float): Float

    /**
     * 获取指定字形的重音附着点 x 坐标（px，相对于字形左边缘）。
     *
     * 用于 \hat, \tilde 等重音符号在基础字形上方的精确水平定位。
     *
     * @param glyphChar Unicode 字符
     * @param fontSizePx 字号（px）
     * @return 附着点 x 坐标，无数据时返回字形宽度的一半（居中）
     */
    fun topAccentAttachment(glyphChar: String, fontSizePx: Float): Float

    /**
     * 获取数学字距调整（px）。
     *
     * 数学字距与普通字距不同：它根据相邻字形的**高度**动态调整，
     * 例如上标较矮时可以更靠近基础字形。
     *
     * @param glyphChar 基础字形
     * @param height 邻接元素在该高度处的位置（px）
     * @param fontSizePx 字号（px）
     * @param isRight true=右侧字距, false=左侧字距
     * @return 字距值（正值=推开，负值=拉近）
     */
    fun mathKern(glyphChar: String, height: Float, fontSizePx: Float, isRight: Boolean): Float

    // ─── 字形变体与组装（对应 MATH 表的 MathVariants） ───

    /**
     * 获取指定字形的垂直尺寸变体列表。
     *
     * 返回从小到大排列的预设变体，每个包含 glyphChar 和对应高度。
     * 用于定界符（括号、花括号等）和大型运算符（积分、求和等）的伸缩。
     *
     * @param glyphChar 基础字形的 Unicode 字符（如 "("）
     * @param fontSizePx 字号（px）
     * @return 从小到大的变体列表，空列表表示无变体
     */
    fun verticalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant>

    /**
     * 获取指定字形的水平尺寸变体列表。
     *
     * 用于 \widehat、\widetilde、水平花括号等需要水平伸缩的装饰。
     */
    fun horizontalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant>

    /**
     * 获取指定字形的垂直组装部件。
     *
     * 当所有预设变体都不够大时，使用组装部件拼出任意高度的字形。
     * 组装由顶部件、底部件、中间件（可选）和扩展件（可重复）组成。
     *
     * @param glyphChar 基础字形
     * @param fontSizePx 字号（px）
     * @return 组装描述，null 表示该字形不支持组装
     */
    fun verticalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly?

    /**
     * 获取指定字形的水平组装部件。
     */
    fun horizontalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly?

    // ─── 字体访问 ───

    /**
     * 获取用于渲染指定数学角色的 FontFamily。
     *
     * OTF 实现：始终返回同一个 FontFamily（单字体文件包含一切）
     * TTF 实现：根据角色路由到不同的 FontFamily（main/math/ams/size1~4 等）
     */
    fun fontFamilyFor(role: MathFontRole): FontFamily

    /**
     * 获取渲染指定变体字形所需的 FontFamily。
     *
     * OTF 实现：返回主 FontFamily（变体字形在同一字体中）
     * TTF 实现：根据变体级别返回对应 Size 字体
     */
    fun fontFamilyForVariant(glyphChar: String, variantIndex: Int): FontFamily

    /**
     * 获取字体字节数据（用于 GlyphBoundsProvider 精确测量）。
     */
    fun fontBytes(role: MathFontRole): ByteArray?
}
```

### 2.3 辅助数据类型

```kotlin
/**
 * 字形尺寸变体。
 */
data class GlyphVariant(
    /** 变体字形的 Unicode 字符（或 glyph ID 的字符串表示） */
    val glyphChar: String,
    /** 该变体的高度/宽度（px，取决于方向） */
    val advanceMeasurement: Float,
    /** 渲染此变体所需的 FontFamily */
    val fontFamily: FontFamily
)

/**
 * 字形组装描述。
 *
 * 定义如何用多个部件拼装出任意大小的字形（如超高括号、超宽花括号）。
 */
data class GlyphAssembly(
    /** 组装部件列表（按从上到下或从左到右的顺序） */
    val parts: List<GlyphPart>,
    /** 部件之间的最小连接器重叠量（px） */
    val minConnectorOverlap: Float
)

/**
 * 字形组装的单个部件。
 */
data class GlyphPart(
    /** 部件字形 */
    val glyphChar: String,
    /** 起始连接器长度（px）— 与前一个部件重叠的区域 */
    val startConnectorLength: Float,
    /** 结束连接器长度（px）— 与后一个部件重叠的区域 */
    val endConnectorLength: Float,
    /** 部件的完整前进宽度/高度（px） */
    val fullAdvance: Float,
    /** 是否为扩展部件（可重复以增加总长度） */
    val isExtender: Boolean,
    /** 渲染此部件所需的 FontFamily */
    val fontFamily: FontFamily
)

/**
 * 数学字体中的字形角色。
 *
 * 用于 fontFamilyFor() 的路由。OTF 字体中这些角色都在同一字体文件中；
 * TTF 字体集中不同角色对应不同的字体文件。
 */
enum class MathFontRole {
    /** 正体文本、运算符、标点 */
    ROMAN,
    /** 数学斜体变量 */
    MATH_ITALIC,
    /** 黑板粗体 (ℝℕℤ) */
    BLACKBOARD_BOLD,
    /** 花体 */
    CALLIGRAPHIC,
    /** 哥特体 */
    FRAKTUR,
    /** 手写体 */
    SCRIPT,
    /** 无衬线 */
    SANS_SERIF,
    /** 等宽 */
    MONOSPACE,
    /** 大型运算符 (∑∫∏) */
    LARGE_OPERATOR,
    /** 定界符 (默认尺寸) */
    DELIMITER,
}
```

### 2.4 架构全景

```
┌──────────────────────────────────────────────────────────────────┐
│                        用户接口 (LatexConfig)                     │
│                                                                  │
│   方式 A: mathFont = MathFont.OTF(otfBytes)     // 单个 OTF 文件  │
│   方式 B: mathFont = MathFont.TTF(fontFamilies)  // KaTeX TTF 集  │
│   方式 C: mathFont = MathFont.Default            // 内置 KaTeX     │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                ┌────────────▼────────────────┐
                │     MathFontProvider        │  ← 统一抽象层
                │  (排版参数 + 字形变体 + 字体) │
                └──────┬──────────────┬───────┘
                       │              │
          ┌────────────▼───┐  ┌───────▼────────────┐
          │ TtfFontSet     │  │ OtfMathFont        │
          │ Provider       │  │ Provider           │
          ├────────────────┤  ├────────────────────┤
          │ 硬编码常量      │  │ MATH 表解析器      │
          │ symbolMap 路由  │  │  ├ MathConstants   │
          │ 12 FontFamily  │  │  ├ MathGlyphInfo   │
          │ Size1~4 逐级   │  │  └ MathVariants    │
          │ (当前方案适配)   │  │ cmap 表解析器      │
          └────────────────┘  │ 单个 FontFamily    │
                              └────────────────────┘
                                       │
                              ┌─────────▼─────────┐
                              │ OpenType 二进制     │
                              │ 解析器 (纯 Kotlin)  │
                              ├───────────────────┤
                              │ • OTF/TTF 文件头    │
                              │ • cmap 表 (字符→ID) │
                              │ • MATH 表 (3 子表)  │
                              │ • head 表 (UPM)    │
                              └───────────────────┘
```

---

## 3. OTF 二进制解析器设计

### 3.1 为什么选择纯 Kotlin 解析

| 方案 | 优势 | 劣势 |
|------|------|------|
| **纯 Kotlin 解析** | 全平台统一、无外部依赖、与 `FontBytesCache` 天然对接 | 需要实现二进制解析逻辑 |
| 平台 expect/actual (HarfBuzz/CoreText) | 利用成熟库 | 5 个平台各自实现，API 差异大，维护成本高 |
| Skia API | 复用底层引擎 | Skiko 未暴露 MATH 表 API，不可控 |

**结论：纯 Kotlin 解析是最优选择。** MATH 表的二进制结构是公开标准，解析逻辑虽然繁琐但不复杂（全是固定偏移的整数读取），一次实现即可在 Android/iOS/JVM/JS/WASM 全部平台运行。

### 3.2 需要解析的 OTF 表

| 表 | 用途 | 复杂度 |
|----|------|--------|
| **`head`** | 获取 `unitsPerEm`（设计空间单位到像素的转换基准） | 低 |
| **`cmap`** | Unicode 字符 → Glyph ID 映射（MATH 表以 Glyph ID 索引） | 中 |
| **`MATH`** | 数学排版核心数据 | 中高 |
| `OS/2`（可选） | 字重、宽度等元信息 | 低 |

### 3.3 MATH 表解析细节

MATH 表是一个树状结构，从 Header 开始通过偏移量引用子表：

```
MATH Header
├── MathConstants (60+ 值)
│   ├── 简单值: int16/uint16 → 直接读取
│   └── MathValueRecord: value (int16) + deviceTable offset
│
├── MathGlyphInfo
│   ├── MathItalicsCorrectionInfo
│   │   ├── Coverage (哪些 glyph 有数据)
│   │   └── ItalicsCorrection[] (MathValueRecord 数组)
│   │
│   ├── MathTopAccentAttachment
│   │   ├── Coverage
│   │   └── TopAccentAttachment[] (MathValueRecord 数组)
│   │
│   ├── ExtendedShapeCoverage (哪些 glyph 是"扩展形状")
│   │
│   └── MathKernInfo
│       ├── Coverage
│       └── MathKernInfoRecord[] (4 个方向的 kern 表引用)
│
└── MathVariants
    ├── minConnectorOverlap (uint16)
    ├── VertGlyphCoverage + VertGlyphConstruction[]
    │   ├── GlyphAssembly (部件列表)
    │   └── MathGlyphVariantRecord[] (预设变体列表)
    │
    └── HorizGlyphCoverage + HorizGlyphConstruction[]
        ├── GlyphAssembly
        └── MathGlyphVariantRecord[]
```

### 3.4 设计空间单位转换

MATH 表中所有尺寸值以**设计空间单位 (design units)** 表示，需转换为像素：

```kotlin
fun designToPx(designUnits: Int, fontSizePx: Float, unitsPerEm: Int): Float {
    return designUnits.toFloat() * fontSizePx / unitsPerEm.toFloat()
}
```

例如 STIX Two Math 的 `unitsPerEm = 1000`，`axisHeight = 250 design units`：
- fontSizePx = 20px → axisHeight = 250 * 20 / 1000 = **5.0px**

### 3.5 Coverage 表解析

MATH 表大量使用 Coverage 表来指定"哪些 Glyph ID 拥有某项数据"。Coverage 有两种格式：

- **Format 1**: Glyph ID 列表（直接枚举）
- **Format 2**: Glyph ID 范围列表（起止区间）

两者都返回一个 Coverage Index，用于索引对应的数据数组。

---

## 4. 字形变体与组装算法

### 4.1 定界符渲染的统一流程

无论 OTF 还是 TTF，定界符渲染统一为以下流程：

```
需要高度 targetHeight 的定界符 "("
         │
         ▼
  ① 查询 verticalVariants("(", fontSizePx)
         │
         ▼ 返回 [variant1(h=12), variant2(h=18), variant3(h=24), ...]
         │
  ② 找到第一个 height >= targetHeight 的变体
         │
    ┌────┴────┐
    │ 找到了   │ 没找到
    ▼         ▼
  直接渲染   ③ 查询 verticalAssembly("(", fontSizePx)
  该变体         │
              ┌──┴──┐
              │ 有   │ 无
              ▼      ▼
          执行组装  兜底：对最大变体做 fontSize 缩放
          算法
```

### 4.2 字形组装算法（核心）

字形组装是 MATH 表最有价值的能力——用若干部件拼出任意高度的定界符：

```
以左大括号 "{" 为例，组装部件：

  ┌───┐  ← 顶部件 (non-extender)
  │   │
  ├╌╌╌┤  ← 扩展件 (extender, 可重复 N 次)
  │   │
  ├───┤  ← 中间件 (non-extender, 可选)
  │   │
  ├╌╌╌┤  ← 扩展件 (extender, 可重复 N 次)
  │   │
  └───┘  ← 底部件 (non-extender)
```

算法步骤：

```
输入: parts[], minConnectorOverlap, targetSize
输出: 每个部件的 glyphId + 绘制偏移

1. 分离 non-extender 和 extender 部件
2. 计算 non-extender 的固定总长度（减去最大允许重叠）
3. 需要额外长度 = targetSize - 固定长度
4. 计算 extender 重复次数 N：
   N = ceil(需要额外长度 / (extender.fullAdvance - minConnectorOverlap))
5. 计算实际总长度（含 N 次扩展件）
6. 均匀分配多余长度到各连接器重叠中（在 minConnectorOverlap 和 maxOverlap 之间）
7. 输出每个部件的位置偏移
```

### 4.3 组装件的渲染

每个组装部件是一个 Glyph ID，需要用字体引擎渲染为独立的文本，然后在 Canvas 上按计算好的偏移拼接：

```kotlin
// draw lambda 中
draw = { x, y ->
    withTransform({ translate(x, y) }) {
        for (placed in placedParts) {
            drawText(
                placed.textLayoutResult,
                topLeft = Offset(0f, placed.offset)
            )
        }
    }
}
```

**关键约束**：组装部件之间的连接器区域会产生重叠（overlap），需要确保相邻部件的墨水在重叠区域无缝衔接——这是 MATH 表 `minConnectorOverlap` 值存在的意义。

---

## 5. 两种 Provider 的实现策略

### 5.1 OtfMathFontProvider

```kotlin
class OtfMathFontProvider(
    fontBytes: ByteArray,
    fontFamily: FontFamily
) : MathFontProvider {

    private val parser = OpenTypeMathParser(fontBytes)
    private val mathTable = parser.parseMathTable()
    private val cmapTable = parser.parseCmapTable()
    private val unitsPerEm = parser.parseHeadTable().unitsPerEm

    // ── MathConstants ──
    override fun axisHeight(fontSizePx: Float): Float {
        return designToPx(mathTable.constants.axisHeight, fontSizePx, unitsPerEm)
    }

    override fun fractionRuleThickness(fontSizePx: Float): Float {
        return designToPx(mathTable.constants.fractionRuleThickness, fontSizePx, unitsPerEm)
    }

    override fun scriptPercentScaleDown(): Int {
        return mathTable.constants.scriptPercentScaleDown  // 如 80
    }

    // ── MathGlyphInfo ──
    override fun italicCorrection(glyphChar: String, fontSizePx: Float): Float {
        val glyphId = cmapTable.charToGlyphId(glyphChar.codePointAt(0))
        val correction = mathTable.glyphInfo.getItalicCorrection(glyphId) ?: return 0f
        return designToPx(correction, fontSizePx, unitsPerEm)
    }

    // ── MathVariants ──
    override fun verticalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant> {
        val glyphId = cmapTable.charToGlyphId(glyphChar.codePointAt(0))
        return mathTable.variants.getVerticalVariants(glyphId)?.map { record ->
            val variantChar = cmapTable.glyphIdToChar(record.variantGlyph)
            GlyphVariant(
                glyphChar = variantChar,
                advanceMeasurement = designToPx(record.advanceMeasurement, fontSizePx, unitsPerEm),
                fontFamily = fontFamily  // OTF: 所有变体在同一字体中
            )
        } ?: emptyList()
    }

    override fun verticalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly? {
        val glyphId = cmapTable.charToGlyphId(glyphChar.codePointAt(0))
        val assembly = mathTable.variants.getVerticalAssembly(glyphId) ?: return null
        return GlyphAssembly(
            parts = assembly.parts.map { part ->
                GlyphPart(
                    glyphChar = cmapTable.glyphIdToChar(part.glyphId),
                    startConnectorLength = designToPx(part.startConnectorLength, fontSizePx, unitsPerEm),
                    endConnectorLength = designToPx(part.endConnectorLength, fontSizePx, unitsPerEm),
                    fullAdvance = designToPx(part.fullAdvance, fontSizePx, unitsPerEm),
                    isExtender = part.partFlags and 0x0001 != 0,
                    fontFamily = fontFamily
                )
            },
            minConnectorOverlap = designToPx(
                mathTable.variants.minConnectorOverlap, fontSizePx, unitsPerEm
            )
        )
    }

    // ── 字体访问 ──
    override fun fontFamilyFor(role: MathFontRole): FontFamily {
        // OTF 数学字体：所有角色都在同一字体中
        return fontFamily
    }
}
```

### 5.2 TtfFontSetProvider（适配当前 KaTeX 方案）

```kotlin
class TtfFontSetProvider(
    private val fontFamilies: LatexFontFamilies,
    private val fontBytesCache: FontBytesCache?
) : MathFontProvider {

    // ── 常量：映射自当前 MathConstants 的硬编码值 ──
    override fun axisHeight(fontSizePx: Float): Float {
        // 保持当前的动态计算逻辑（测量减号中心）
        // 或使用 MathConstants.MATH_AXIS_HEIGHT_RATIO * fontSizePx 作为近似
        return fontSizePx * 0.25f
    }

    override fun fractionRuleThickness(fontSizePx: Float): Float {
        return fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
    }

    override fun scriptPercentScaleDown(): Int = 70   // 0.7 * 100
    override fun scriptScriptPercentScaleDown(): Int = 50  // 0.5 * 100

    // ── 斜体修正：启发式估算 ──
    override fun italicCorrection(glyphChar: String, fontSizePx: Float): Float {
        // 沿用当前的分类估算逻辑
        return when {
            glyphChar.first().isUpperCase() -> fontSizePx * 0.15f
            glyphChar.first().isLowerCase() -> fontSizePx * 0.12f
            else -> fontSizePx * 0.08f
        }
    }

    // ── 变体：映射到 Size1~4 ──
    override fun verticalVariants(glyphChar: String, fontSizePx: Float): List<GlyphVariant> {
        // 返回 Main, Size1, Size2, Size3, Size4 五级变体
        // 每级的高度通过 TextMeasurer 预计算（或缓存）
        return listOf(
            GlyphVariant(glyphChar, mainHeight, fontFamilies.main),
            GlyphVariant(glyphChar, size1Height, fontFamilies.size1),
            GlyphVariant(glyphChar, size2Height, fontFamilies.size2),
            GlyphVariant(glyphChar, size3Height, fontFamilies.size3),
            GlyphVariant(glyphChar, size4Height, fontFamilies.size4),
        )
    }

    // ── 组装：TTF 不支持 ──
    override fun verticalAssembly(glyphChar: String, fontSizePx: Float): GlyphAssembly? = null

    // ── 字体路由 ──
    override fun fontFamilyFor(role: MathFontRole): FontFamily {
        return when (role) {
            MathFontRole.ROMAN -> fontFamilies.main
            MathFontRole.MATH_ITALIC -> fontFamilies.math
            MathFontRole.BLACKBOARD_BOLD -> fontFamilies.ams
            MathFontRole.CALLIGRAPHIC -> fontFamilies.caligraphic
            MathFontRole.FRAKTUR -> fontFamilies.fraktur
            MathFontRole.SCRIPT -> fontFamilies.script
            MathFontRole.SANS_SERIF -> fontFamilies.sansSerif
            MathFontRole.MONOSPACE -> fontFamilies.monospace
            MathFontRole.LARGE_OPERATOR -> fontFamilies.main
            MathFontRole.DELIMITER -> fontFamilies.main
        }
    }
}
```

---

## 6. 用户接口设计

### 6.1 LatexConfig 扩展

```kotlin
data class LatexConfig(
    val fontSize: TextUnit = 20.sp,
    val color: Color = Color.Black,
    // ... 现有字段 ...

    /**
     * 数学字体配置。决定排版参数和字体的来源。
     *
     * - MathFont.Default：使用内置的 KaTeX TTF 字体集
     * - MathFont.OTF(bytes)：使用带 MATH 表的 OTF 字体
     * - MathFont.TTF(families)：使用自定义的 TTF 字体集
     */
    val mathFont: MathFont = MathFont.Default,
)

sealed class MathFont {
    /** 使用内置的 KaTeX TTF 字体集 */
    object Default : MathFont()

    /**
     * 使用带 OpenType MATH 表的 OTF 字体文件。
     *
     * @param fontBytes OTF 文件的字节数据
     * @param fontFamily 对应的 Compose FontFamily（由用户从 OTF 创建）
     */
    data class OTF(
        val fontBytes: ByteArray,
        val fontFamily: FontFamily
    ) : MathFont()

    /**
     * 使用自定义的 TTF 字体集。
     *
     * @param fontFamilies 12 槽位的字体家族配置
     */
    data class TTF(
        val fontFamilies: LatexFontFamilies
    ) : MathFont()
}
```

### 6.2 使用示例

```kotlin
// 方式 1：默认（KaTeX 内置字体）
Latex(
    latex = "\\frac{a}{b}",
    config = LatexConfig()
)

// 方式 2：使用 STIX Two Math OTF 字体
val stixBytes = context.assets.open("STIXTwoMath-Regular.otf").readBytes()
val stixFamily = FontFamily(Font(stixBytes))

Latex(
    latex = "\\int_0^\\infty e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}",
    config = LatexConfig(
        mathFont = MathFont.OTF(
            fontBytes = stixBytes,
            fontFamily = stixFamily
        )
    )
)
```

---

## 7. Measurer 层的改造

### 7.1 改造原则

所有 Measurer 当前直接引用 `MathConstants.XXX` 的地方，改为通过 `RenderContext.mathFontProvider.xxx(fontSizePx)` 获取。

### 7.2 改造前后对比

**FractionMeasurer 改造示例**：

```kotlin
// ── 改造前 ──
val ruleThickness = fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
val gap = fontSizePx * MathConstants.FRACTION_GAP

// ── 改造后 ──
val provider = context.mathFontProvider
val ruleThickness = provider.fractionRuleThickness(fontSizePx)
val gap = provider.fractionNumeratorDisplayGap(fontSizePx)  // 区分 Display/Text 模式
```

**DelimiterRenderer 改造示例**：

```kotlin
// ── 改造前 ──
// 硬编码的 5 级 Size 字体逐级尝试
for (level in sizeLevels) { ... }
// Size4 不够 → fontSize 缩放兜底

// ── 改造后 ──
val provider = context.mathFontProvider

// 1. 查询预设变体
val variants = provider.verticalVariants(delimiter, fontSizePx)
for (variant in variants) {
    if (variant.advanceMeasurement >= targetHeight) {
        return measureText(variant.glyphChar, variant.fontFamily, context, measurer)
    }
}

// 2. 尝试字形组装
val assembly = provider.verticalAssembly(delimiter, fontSizePx)
if (assembly != null) {
    return assembleGlyph(assembly, targetHeight, context, measurer)
}

// 3. 最终兜底：对最大变体做 fontSize 缩放
val largest = variants.lastOrNull() ?: return fallbackMeasure(...)
val scale = targetHeight / largest.advanceMeasurement
return measureText(largest.glyphChar, largest.fontFamily, context.withScaledFontSize(scale), measurer)
```

**ScriptMeasurer 改造示例**：

```kotlin
// ── 改造前 ──
val supShift = fontSizePx * MathConstants.SUPERSCRIPT_SHIFT
val subShift = fontSizePx * MathConstants.SUBSCRIPT_SHIFT

// ── 改造后 ──
val provider = context.mathFontProvider
val supShift = provider.superscriptShiftUp(fontSizePx)
val subShift = provider.subscriptShiftDown(fontSizePx)
// OTF 字体的值更精确，TTF 沿用现有近似值
```

---

## 8. 实施路线图

### Phase 1: OpenType 二进制解析器（基础设施）

**目标**：能从 OTF/TTF 字节数据中读取 MATH 表的全部信息。

| 任务 | 说明 |
|------|------|
| OTF 文件头解析 | 表目录 (Table Directory)，定位各表偏移 |
| `head` 表解析 | 获取 `unitsPerEm` |
| `cmap` 表解析 | Format 4 (BMP) + Format 12 (Full Unicode) |
| MATH `MathConstants` 解析 | ~60 个值，含 MathValueRecord 处理 |
| MATH `MathGlyphInfo` 解析 | Coverage + ItalicCorrection + TopAccent + MathKern |
| MATH `MathVariants` 解析 | GlyphConstruction + GlyphAssembly + 变体列表 |
| 单元测试 | 用已知字体（如 Latin Modern Math）验证解析正确性 |

### Phase 2: MathFontProvider 抽象层

**目标**：定义接口 + 实现 TtfFontSetProvider（适配现有逻辑，保证零回归）。

| 任务 | 说明 |
|------|------|
| `MathFontProvider` 接口定义 | 本文档第 2.2 节 |
| `TtfFontSetProvider` 实现 | 将 `MathConstants` + `FontResolver` 的逻辑封装 |
| `RenderContext` 集成 | 新增 `mathFontProvider` 字段 |
| 所有 Measurer 迁移 | 将 `MathConstants.XXX` 引用替换为 `provider.xxx()` |
| 回归测试 | 确保所有现有渲染结果不变 |

### Phase 3: OtfMathFontProvider 实现

**目标**：用 OTF MATH 表数据驱动渲染。

| 任务 | 说明 |
|------|------|
| `OtfMathFontProvider` 实现 | 连接 Phase 1 解析器与 Phase 2 接口 |
| MathConstants 驱动渲染 | axisHeight、fractionRule 等从字体读取 |
| MathGlyphInfo 驱动渲染 | 精确斜体修正、重音附着 |
| 用户接口 (`MathFont.OTF`) | LatexConfig 扩展 |

### Phase 4: 字形组装引擎

**目标**：支持 MathVariants 的预设变体查询 + 字形组装拼接，实现任意高度定界符。

| 任务 | 说明 |
|------|------|
| 变体查询集成 | DelimiterRenderer 使用 `verticalVariants()` |
| 字形组装算法 | 计算部件重复次数、重叠量分配 |
| 组装件渲染 | 在 Canvas 上拼接多个 glyph 部件 |
| 水平组装 | \widehat, \overbrace 等水平伸缩 |
| 视觉测试 | 各种极端高度的定界符渲染对比 |

---

## 9. 关键技术风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| cmap 表格式多样 | 部分字体使用 Format 6/10/13/14 | 优先实现 Format 4 (BMP) + Format 12 (Full)，覆盖 99% 的数学字体 |
| GlyphID → 字符的反向映射 | 组装部件以 GlyphID 标识，需转换为可渲染的字符 | 构建 cmap 反向映射表；对于 PUA 区 glyph，使用 Skia 的 `getStringGlyphs()` 反向验证 |
| Compose TextMeasurer 不支持 GlyphID 直接渲染 | 组装部件可能是 PUA 区字形 | 使用 Skia 底层 API (GlyphBoundsProvider 已有先例) 直接按 GlyphID 绘制 |
| DeviceTable 修正 | MATH 表中的 MathValueRecord 含可选的 DeviceTable（亚像素修正） | 初版忽略 DeviceTable（影响极小），后续版本按需实现 |
| 字体文件大小 | STIX Two Math ~800KB，Latin Modern Math ~500KB | 不内置 OTF，由用户通过 `MathFont.OTF(bytes)` 注入；或提供 Gradle 插件自动打包 |

---

## 10. 两种字体架构的共存保障

### 10.1 兼容性矩阵

| 场景 | TTF (KaTeX) | OTF (MATH 表) |
|------|------------|---------------|
| 基本文本/符号 | ✅ FontResolver 路由 | ✅ 单字体包含全部 |
| 分数 | ✅ 硬编码参数 | ✅ MathConstants |
| 上下标 | ✅ 硬编码参数 | ✅ MathConstants + MathKern |
| 根号 | ✅ Path 手绘 + 硬编码间距 | ✅ Path 手绘 + MathConstants 间距 |
| 斜体修正 | ⚠️ 启发式 | ✅ 逐字形精确值 |
| 重音定位 | ⚠️ 居中+粗略修正 | ✅ 逐字形精确附着点 |
| 定界符（小/中） | ✅ Size1~4 字体 | ✅ 预设变体 |
| 定界符（极大） | ⚠️ fontSize 缩放兜底 | ✅ 字形组装（任意高度） |
| 花括号/水平伸缩 | ⚠️ Path 手绘 | ✅ 水平字形组装 |
| 字体风格切换 | ❌ 仅 KaTeX 风格 | ✅ 10+ 种字体风格 |

### 10.2 渐进增强策略

- 默认行为不变：`MathFont.Default` 使用 KaTeX TTF，所有现有渲染结果不受影响
- OTF 是**增量能力**：用户选择 OTF 字体后，自动获得更高精度的排版
- 共享代码最大化：Measurer 逻辑不变，只是参数来源从硬编码变为 Provider
- Path 手绘仍保留：根号 V 形钩、分数线等动态宽度图形两种体系都需要

### 10.3 测试策略

| 测试类型 | 方法 |
|---------|------|
| 解析正确性 | 用 Latin Modern Math / STIX Two Math 的已知 MATH 表值做断言 |
| 渲染回归 | TTF Provider 的输出值与当前 `MathConstants` 完全一致 |
| 组装正确性 | 各种目标高度的括号/花括号，验证部件数量和重叠量 |
| 跨平台一致性 | 纯 Kotlin 解析器天然保证（与平台无关） |
| 视觉对比 | 同一公式在 KaTeX TTF / STIX OTF / Latin Modern OTF 下的渲染截图对比 |
