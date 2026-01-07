# LaTeX Canvas Rendering Specification

## 1. 核心坐标系与度量 (Core Geometry)

在 Canvas 绘图中，所有节点的布局必须遵循以下度量标准：

- **Origin (0,0)**: 节点的左上角。
- **Width**: 节点占用的总宽度。
- **Height**: 节点占用的总高度。
- **Baseline**: 从顶部的距离，定义了文本或符号的基准线。
- **Math Axis (数学轴)**: 位于基线之上的一条虚构线。它是分式线、加减号以及算子（如 $\sum$）垂直居中的参考线。

## 2. 布局对齐规则 (Alignment Rules)

### 2.1 行内对齐 (Inline Alignment)
多个节点并排显示时，必须**基线对齐 (Baseline Alignment)**。
- 计算所有子节点中：`maxAscent = max(child.baseline)`。
- 总高度 `H = maxAscent + max(child.height - child.baseline)`。
- 每个子节点的绘制 y 坐标 = `ParentY + (maxAscent - child.baseline)`。

### 2.2 数学轴对齐 (Math Axis Alignment)
某些结构（如分式、矩阵、大型算子）必须相对于**数学轴**垂直居中。
- **动态轴高度检测**: 系统通过测量减号 `-` 的垂直中心来动态确定当前字体的数学轴高度（`AxisHeight`）。
- `AxisHeight` 定义为从基线向上到数学轴的距离。
- 分数线、根号横线应居中于 `y = Baseline - AxisHeight`。

## 3. 具体组件渲染逻辑

### 3.1 分式 (Fraction: \frac{num}{den})
1. **缩放**: 分子和分母缩小至 0.9x (`LatexConstants.FRACTION_SCALE_FACTOR`)。
2. **中心线**: 分数线 (Rule) 严格绘制在数学轴上。
3. **基线**: `Baseline = (lineY + ruleThickness / 2) + AxisHeight`。

### 3.2 二项式系数 (Binomial: \binom{n}{k})
1. **布局**: 类似于分式，但无中间横线。
2. **基线**: 内容整体的几何中心应与数学轴对齐。`Baseline = (num.height + gap / 2) + AxisHeight`。

### 3.3 大型运算符 (Big Operators: \sum, \int)
- **侧边模式 (Side Mode)**:
  - 符号垂直居中于数学轴：`opTopY = Baseline - AxisHeight - opHeight / 2`。
  - 积分符号 (`\int`) 具有特殊的倾斜补偿 (Tucking)，上限向左偏移，下限向左嵌入钩子内部。
- **显示模式 (Display Mode)**:
  - 符号垂直居中于数学轴，上下限分别置于正上方和正下方。
  - 符号的 `Baseline` 决定了整个结构的 `Baseline`。

### 3.4 根号 (Radical: \sqrt{content})
1. **高度**: 覆盖 `content.height + gap + ruleThickness`。
2. **横线**: 位于内容上方，垂直距离为 `gap`。

## 4. 实现保障 (Implementation Safeguards)

1. **LayoutUtils**: 统一封装了 `getAxisHeight` 逻辑，避免硬编码比例。
2. **RenderContext**: 传递 `bigOpHeightHint`，允许积分等运算符根据相邻内容（如大括号、分式）动态拉伸高度。
3. **NodeLayout**: 严格遵守 `(width, height, baseline)` 三元组，确保递归布局的正确性。
