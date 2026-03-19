# LaTeX 解析器功能覆盖分析

## 1. 基础结构

### ✅ 已支持
- ✅ 文本内容
- ✅ 分组 `{...}`
- ✅ 上标 `^`
- ✅ 下标 `_`
- ✅ 空格和换行
- ✅ 注释（词法层面）

### ❌ 缺失
- 无

**覆盖率**: 6/6 (100%)

---

## 2. 数学公式

### ✅ 已支持

#### 分数
- ✅ `\frac{分子}{分母}` 基础分数
- ✅ `\dfrac` 显示模式分数
- ✅ `\tfrac` 文本模式分数
- ✅ `\cfrac` 连分数
- ✅ `\binom{n}{k}` 二项式系数
- ✅ `\tbinom`, `\dbinom` 二项式系数样式变体

#### 根号
- ✅ `\sqrt{x}` 平方根
- ✅ `\sqrt[n]{x}` n次根

### ❌ 缺失
- 无

**覆盖率**: 7/7 (100%)

---

## 3. 符号系统

### ✅ 已支持

#### 希腊字母 (100+)
- ✅ 小写: α, β, γ, δ, ϵ, ζ, η, θ, ι, κ, λ, μ, ν, ξ, ο, π, ρ, σ, τ, υ, ϕ, χ, ψ, ω
- ✅ 大写: Γ, Δ, Θ, Λ, Ξ, Π, Σ, Υ, Φ, Ψ, Ω
- ✅ 变体: ε/ϵ, θ/ϑ, π/ϖ, ρ/ϱ, σ/ς, φ/ϕ

#### 运算符
- ✅ 基础: +, -, ×, ÷, ±, ∓, ⋅, ∗, ⊕, ⊗, ⊖, ⊘, ⊙
- ✅ 关系: =, ≠, <, >, ≤, ≥, ≈, ≡, ∼, ≃, ≅, ≪, ≫
- ✅ 集合: ∈, ∉, ⊂, ⊃, ⊆, ⊇, ∪, ∩, ∅, ℕ, ℤ, ℚ, ℝ, ℂ
- ✅ 逻辑: ∧, ∨, ¬, ⇒, ⇔, ∀, ∃
- ✅ 箭头: →, ←, ↔, ⇒, ⇐, ⇔, ↑, ↓, ↕, ↗, ↖, ↪, ↩
- ✅ 箭头简写: `\to` (等同于 `\rightarrow`)
- ✅ 鱼叉箭头: ↼, ↽, ⇀, ⇁

#### 否定修饰
- ✅ `\not=` 否定等号
- ✅ `\not\in` 否定属于
- ✅ `\not\subset` 否定子集（给关系符号加斜线表示否定）

#### 省略号
- ✅ `\ldots` 底部省略号 …
- ✅ `\cdots` 居中省略号 ⋯
- ✅ `\vdots` 垂直省略号 ⋮
- ✅ `\ddots` 对角省略号 ⋱
- ✅ `\dots` 自适应省略号（根据上下文自动选择 `\ldots` 或 `\cdots`）

#### 特殊符号
- ✅ 无穷: ∞
- ✅ 其他: ∂, ∇, ℓ, ℏ, ℜ, ℑ, ∠, ∟, ⊥, ∥, △

#### AMS 否定关系符号
- ✅ `\nleq`, `\ngeq` 否定不等式
- ✅ `\nsubseteq`, `\nsupseteq` 否定集合关系
- ✅ `\nprec`, `\nsucc` 否定序关系
- ✅ `\ncong`, `\nsim` 否定相似/全等
- ✅ `\nmid`, `\nparallel` 否定整除/平行
- ✅ `\nvdash`, `\nvDash`, `\nVdash`, `\nVDash` 否定推导
- ✅ `\ntriangleleft`, `\ntriangleright`, `\ntrianglelefteq`, `\ntrianglerighteq` 否定三角关系
- ✅ `\nless`, `\ngtr` 否定比较
- ✅ `\leqslant`, `\geqslant` 等 AMS 额外关系符号
- ✅ `\vDash`, `\Vdash`, `\Vvdash`, `\models` 逻辑推导

#### AMS 额外常用符号
- ✅ `\checkmark` ✓, `\complement` ∁, `\eth` ð, `\mho` ℧
- ✅ `\twoheadrightarrow` ↠, `\twoheadleftarrow` ↞ 双头箭头
- ✅ `\leftleftarrows`, `\rightrightarrows`, `\leftrightarrows`, `\rightleftarrows` 双线箭头
- ✅ `\curvearrowright`, `\curvearrowleft`, `\circlearrowright`, `\circlearrowleft` 弯曲箭头
- ✅ `\lessdot`, `\gtrdot`, `\lll`, `\ggg` 特殊关系
- ✅ `\blacksquare`, `\square`, `\lozenge`, `\blacktriangle`, `\blacktriangledown` 几何符号
- ✅ `\aleph`, `\beth`, `\gimel`, `\daleth` 希伯来字母
- ✅ `\angle`, `\measuredangle`, `\sphericalangle` 角度符号

### ❌ 缺失
- 无

**覆盖率**: 130+/130+ (100%)

---

## 4. 大型运算符

### ✅ 已支持
- ✅ `\sum` 求和 Σ
- ✅ `\prod` 乘积 ∏
- ✅ `\int` 积分 ∫
- ✅ `\oint` 环路积分 ∮
- ✅ `\iint` 二重积分 ∬
- ✅ `\iiint` 三重积分 ∭
- ✅ `\bigcup` 大并集 ⋃
- ✅ `\bigcap` 大交集 ⋂
- ✅ `\bigvee` 大析取 ⋁
- ✅ `\bigwedge` 大合取 ⋀
- ✅ `\coprod` 余积 ∐
- ✅ `\bigoplus` 大直和 ⨁
- ✅ `\bigotimes` 大张量积 ⨂
- ✅ `\bigsqcup` 大方并 ⨆
- ✅ `\bigodot` 大圆点积 ⨀
- ✅ `\biguplus` 大多重并 ⨄
- ✅ `\lim` 极限
- ✅ `\max`, `\min` 最大值/最小值
- ✅ `\sup`, `\inf` 上确界/下确界
- ✅ `\limsup`, `\liminf` 上极限/下极限
- ✅ `\operatorname{名称}` 自定义运算符（正体渲染，支持 `\limits`/`\nolimits` 和上下标）
- ✅ `\DeclareMathOperator{\Tr}{Tr}` 声明式运算符定义（前言中定义自定义运算符）
- ✅ `\mathop{内容}` 将任意内容标记为大型运算符（可带上下限）

#### 多行下标条件
- ✅ `\substack{i<n \\ j<m}` 大型运算符上下限排列多行条件

#### 取模运算符
- ✅ `\bmod` 二元取模运算符（如 `a \bmod b`，渲染为 "a mod b"）
- ✅ `\pmod{n}` 括号取模（如 `a \equiv b \pmod{n}`，渲染为 "(mod n)"）
- ✅ `\mod` 取模运算符（如 `a \mod b`，渲染为 "mod b"，间距更宽）

### ❌ 缺失
- 无

**覆盖率**: 28/28 (100%) ✅

---

## 5. 矩阵

### ✅ 已支持
- ✅ `matrix` 无括号矩阵
- ✅ `pmatrix` 圆括号矩阵 ()
- ✅ `bmatrix` 方括号矩阵 []
- ✅ `Bmatrix` 花括号矩阵 {}
- ✅ `vmatrix` 单竖线矩阵 ||
- ✅ `Vmatrix` 双竖线矩阵 ||||
- ✅ `smallmatrix` 小矩阵（用于行内公式）
- ✅ `array` 数组环境（更通用的表格）

### ❌ 缺失
- 无

**覆盖率**: 8/8 (100%)

---

## 6. 括号和分隔符

### ✅ 已支持

#### 自动伸缩括号
- ✅ `\left( ... \right)` 自动伸缩圆括号
- ✅ `\left[ ... \right]` 方括号
- ✅ `\left\{ ... \right\}` 花括号
- ✅ `\left| ... \right|` 竖线
- ✅ `\left\langle ... \right\rangle` 尖括号 ⟨⟩
- ✅ `\left\lfloor ... \right\rfloor` 下取整 ⌊⌋
- ✅ `\left\lceil ... \right\rceil` 上取整 ⌈⌉
- ✅ `\left\lvert ... \right\rvert` 单竖线（同 `|`）
- ✅ `\left\lVert ... \right\rVert` 双竖线（同 `‖`）
- ✅ `\left\lbrace ... \right\rbrace` 花括号别名（同 `\left\{ ... \right\}`）

#### 不对称分隔符
- ✅ `\left. ... \right|` 不对称分隔符（求值符号）
- ✅ `\left\{ ... \right.` 左侧分段函数
- ✅ `.` 表示不显示该侧分隔符

#### 手动大小控制
- ✅ `\big(` 小括号 (1.2x)
- ✅ `\Big[` 中括号 (1.8x)
- ✅ `\bigg\{` 大括号 (2.4x)
- ✅ `\Bigg|` 特大竖线 (3.0x)
- ✅ `\bigl`, `\bigr`, `\bigm` 方向后缀支持
- ✅ `\big\lvert`, `\big\rvert`, `\big\lVert`, `\big\rVert` 竖线变体手动大小
- ✅ 支持所有括号类型：`()`, `[]`, `\{\}`, `||`, `⟨⟩`, `⌊⌋`, `⌈⌉`

### ❌ 缺失
- 无

**覆盖率**: 11/11 (100%) ✅

---

## 7. 装饰符号

### ✅ 已支持
- ✅ `\hat{x}` 帽子 x̂
- ✅ `\tilde{x}` 波浪线 x̃
- ✅ `\bar{x}` 上划线 x̄
- ✅ `\overline{AB}` 长上划线
- ✅ `\underline{text}` 下划线
- ✅ `\dot{x}` 单点 ẋ
- ✅ `\ddot{x}` 双点 ẍ
- ✅ `\dddot{x}` 三点
- ✅ `\grave{x}` 重音符
- ✅ `\acute{x}` 锐音符
- ✅ `\check{x}` 抑扬符 ˇ
- ✅ `\breve{x}` 短音符 ˘
- ✅ `\ring{x}` / `\mathring{x}` 圆圈 ˚
- ✅ `\vec{v}` 向量箭头 v⃗
- ✅ `\overbrace{...}` 上大括号
- ✅ `\underbrace{...}` 下大括号
- ✅ `\widehat{ABC}` 宽帽子
- ✅ `\overrightarrow{AB}` 上箭头
- ✅ `\overleftarrow{AB}` 左上箭头
- ✅ `\cancel{x}` 取消线
- ✅ `\bcancel{x}` 反向取消线（从左下到右上）
- ✅ `\xcancel{x}` 交叉取消线（双对角线）
- ✅ `\xrightarrow{f}` 可扩展右箭头
- ✅ `\xleftarrow{g}` 可扩展左箭头
- ✅ `\xrightarrow[下]{上}` 带上下标的箭头
- ✅ `\xhookrightarrow{f}` 可扩展钩右箭头
- ✅ `\xhookleftarrow{g}` 可扩展钩左箭头
- ✅ `\xRightarrow{f}` 可扩展双线右箭头
- ✅ `\xLeftarrow{g}` 可扩展双线左箭头
- ✅ `\xLeftrightarrow{h}` 可扩展双线双向箭头
- ✅ `\xmapsto{f}` 可扩展映射箭头
- ✅ `\overset{上}{基础}` 上堆叠
- ✅ `\underset{下}{基础}` 下堆叠
- ✅ `\stackrel{上}{基础}` 上下堆叠（同 overset）
- ✅ `\underbrace{x+y}_{text}` 下大括号带下方标注文本
- ✅ `\overbrace{a+b}^{text}` 上大括号带上方标注文本
- ✅ `\overbracket{x+y}` 上方括号标注
- ✅ `\underbracket{a+b}` 下方括号标注

### ❌ 缺失
- 无

**覆盖率**: 35/35 (100%) ✅

---

## 8. 字体样式

### ✅ 已支持
- ✅ `\mathbf{x}` 粗体
- ✅ `\mathit{x}` 斜体
- ✅ `\mathrm{x}` 罗马体
- ✅ `\mathsf{x}` 无衬线体
- ✅ `\mathtt{x}` 等宽体
- ✅ `\mathbb{R}` 黑板粗体 ℝ
- ✅ `\mathfrak{g}` 哥特体
- ✅ `\mathcal{F}` 花体
- ✅ `\mathscr{L}` 手写体
- ✅ `\boldsymbol{α}` 粗体符号
- ✅ `\bm{α}` 粗体符号简写
- ✅ `\text{普通文本}` 文本模式
- ✅ `\mbox{文本}` mbox模式
- ✅ `\symbf{x}` Unicode 数学粗体符号（同 `\boldsymbol`）
- ✅ `\symit{x}` Unicode 数学斜体
- ✅ `\symsf{x}` Unicode 数学无衬线体
- ✅ `\symrm{x}` Unicode 数学罗马体

### ❌ 缺失
- 无

**覆盖率**: 17/17 (100%) ✅

---

## 9. 数学模式切换

### ✅ 已支持
- ✅ `\displaystyle` 显示模式（最大，用于独立公式）
- ✅ `\textstyle` 文本模式（正常大小）
- ✅ `\scriptstyle` 脚本模式（上下标大小）
- ✅ `\scriptscriptstyle` 小脚本模式（二级上下标大小）
- ✅ `$...$` 行内数学模式（嵌入文本中的公式，使用 textstyle）
- ✅ `$$...$$` 展示数学模式（独立行公式，使用 displaystyle）

**特性说明：**
- 支持字体大小切换：displaystyle (1.0x) → textstyle (1.0x) → scriptstyle (0.7x) → scriptscriptstyle (0.5x)
- **大型运算符智能适配**：
  - displaystyle (fontSize ≥ 16)：求和符号放大 1.5x，上下标在**正上下方**
  - textstyle/scriptstyle (fontSize < 16)：求和符号保持 1.0x，上下标在**右侧**（节省空间）
  - 积分符号始终使用右侧模式

**使用示例：**
```latex
% displaystyle: 求和符号放大，上下标在上下方
\displaystyle{\sum_{i=1}^{n}}

% scriptstyle: 求和符号正常大小，上下标在右侧
\scriptstyle{\sum_{i=1}^{n}}

% 在分数中使用 displaystyle 使求和符号变大
\frac{\displaystyle{\sum_{i=1}^{n}}}{n}

% 求和符号作为上标时自动切换为紧凑模式
x^{\sum_{i=1}^{n}}
```

**覆盖率**: 6/6 (100%) ✅

---

## 10. 空格控制
    
### ✅ 已支持
- ✅ `\,` 细空格 (1/6 em)
- ✅ `\:` 中等空格 (2/9 em)
- ✅ `\;` 粗空格 (5/18 em)
- ✅ `\quad` quad空格 (1 em)
- ✅ `\qquad` 双quad空格 (2 em)
- ✅ 普通空格
- ✅ `\!` 负空格
- ✅ `\hspace{1cm}` 自定义空格
    
### ❌ 缺失
- 无
    
**覆盖率**: 8/8 (100%)

---

## 11. 环境

### ✅ 已支持
- ✅ `equation` 公式编号环境
- ✅ `displaymath` 展示数学环境
- ✅ `align`, `aligned` 对齐环境
- ✅ `gather`, `gathered` 居中环境
- ✅ `cases` 分段函数
- ✅ `split` 分割环境（用于单个方程内的多行分割）
- ✅ `multline` 多行环境（第一行左对齐,最后一行右对齐,中间行居中）
- ✅ `eqnarray` 方程数组（旧式语法,三列结构）
- ✅ `subequations` 子方程环境（用于相关方程组编号）
- ✅ `tabular` 文本模式表格（支持 l/c/r 列对齐）

#### 星号环境变体（无编号）
- ✅ `align*` 无编号对齐环境
- ✅ `equation*` 无编号公式环境
- ✅ `gather*` 无编号居中环境
- ✅ `multline*` 无编号多行环境
- ✅ `eqnarray*` 无编号方程数组

#### cases 变体环境
- ✅ `dcases` displaystyle cases（mathtools 包）
- ✅ `rcases` 右花括号 cases（mathtools 包）

#### 其他对齐环境
- ✅ `flalign`, `flalign*` 全宽对齐环境
- ✅ `alignat`, `alignat*` 指定列数对齐环境

#### 公式自动编号
- ✅ 自动编号计数器（equation/align/gather/multline/eqnarray 非星号变体自增）
- ✅ `\ref`/`\eqref` 渲染为实际编号
- ✅ 有 `\tag` 时跳过自动编号
- ✅ 星号环境（`equation*` 等）不参与编号

### ❌ 缺失
- 无

**覆盖率**: 21/21 (100%)

---

## 12. 颜色与背景色

### ✅ 已支持

#### 颜色命令
- ✅ `\color{red}{文本}` 颜色命令
- ✅ `\textcolor{red}{文本}` 文本颜色
- ✅ 支持常见颜色名称: red, blue, green, yellow, orange, purple, cyan, magenta, pink, brown, lime, navy, teal, violet
- ✅ 支持十六进制颜色: `\color{#FF5733}{文本}`

#### 背景色
- ✅ `\colorbox{color}{text}` 背景色（给文本加背景色）
- ✅ `\fcolorbox{borderColor}{bgColor}{text}` 带边框的背景色

### ❌ 缺失
- 无

**覆盖率**: 6/6 (100%) ✅

---

## 13. 化学公式

### ✅ 已支持
- ✅ `\ce{H2O}` 化学式（基础分子）
- ✅ `\ce{H2SO4}` 化学式（多原子）
- ✅ `\ce{Na+}` 离子（正离子）
- ✅ `\ce{SO4^{2-}}` 离子（负离子，带电荷标注）
- ✅ `\ce{Fe^{3+}}` 离子（多价离子）
- ✅ `\ce{A + B -> C}` 化学反应（单向箭头 `→`）
- ✅ `\ce{A <- B}` 化学反应（左箭头 `←`）
- ✅ `\ce{A <-> B}` 化学反应（可逆箭头 `↔`）
- ✅ `\ce{A => B}` 化学反应（双线箭头 `⇒`）
- ✅ `\ce{A <=> B}` 化学平衡（双线可逆箭头 `⇔`）
- ✅ 系数解析（如 `\ce{2H2 + O2 -> 2H2O}`）
- ✅ 上标和下标混合（如 `\ce{^{235}_{92}U}`）
- ✅ 复杂配合物（如 `\ce{[Cu(NH3)4]^{2+}}`）

### ❌ 缺失
- 无

**覆盖率**: 13/13 (100%) ✅

---

## 14. 标签、引用与公式标签

### ✅ 已支持

#### 标签与引用
- ✅ `\label{eq:1}` 标签定义（不参与渲染）
- ✅ `\ref{eq:1}` 引用标签（渲染为实际编号）
- ✅ `\eqref{eq:1}` 公式引用（渲染为带括号的实际编号）

#### 公式标签
- ✅ `\tag{1}` 公式编号标签（右侧显示 `(1)`）
- ✅ `\tag*{A}` 无括号公式标签（右侧显示 `A`）

### ❌ 缺失
- 无

**覆盖率**: 5/5 (100%) ✅

---

## 15. 自定义命令与宏定义

### ✅ 已支持
- ✅ `\newcommand{\R}{\mathbb{R}}` 自定义命令定义
- ✅ `\newcommand{\diff}[1]{\frac{d}{d#1}}` 单参数命令
- ✅ `\newcommand{\pdiff}[2]{\frac{\partial #1}{\partial #2}}` 多参数命令
- ✅ `\newcommand{\cmd}[2][default]{body}` 可选参数默认值（第一个参数可省略）
- ✅ `\renewcommand{\cmd}{def}` 重定义已有命令（语法同 `\newcommand`）
- ✅ `\def\name{body}` TeX 原始宏定义
- ✅ `\def\name#1#2{body}` 带参数的 TeX 宏定义
- ✅ `\newenvironment{name}[args]{begin-def}{end-def}` 自定义环境定义
- ✅ `\renewenvironment{name}{begin-def}{end-def}` 重定义环境

**特性说明：**
- 支持 0-9 个参数，使用 `#1` ~ `#9` 表示
- 支持嵌套和递归定义
- `\renewcommand` 覆盖已有定义
- `\def` 支持 TeX 原始语法

### ❌ 缺失
- 无

**覆盖率**: 9/9 (100%) ✅

---

## 16. 特殊效果与布局控制

### ✅ 已支持

#### 方框
- ✅ `\boxed{E = mc^2}` 方框（在公式周围绘制矩形边框）
- ✅ `\fbox{text}` 文本模式方框（同 `\boxed` 但使用 FBOX 样式）

#### 幻影与间距控制
- ✅ `\phantom{x}` 幻影空间（占据空间但不显示内容，用于对齐）
- ✅ `\smash{x}` 高度压缩（绘制内容但不占据垂直空间）
- ✅ `\vphantom{x}` 垂直幻影（只占据垂直空间，宽度为零）
- ✅ `\hphantom{x}` 水平幻影（只占据水平空间，高度为零）

#### 零宽叠加
- ✅ `\mathclap{内容}` 零宽居中叠加（绘制内容但宽度为0，内容居中对齐）
- ✅ `\mathllap{内容}` 零宽左叠加（内容向左扩展）
- ✅ `\mathrlap{内容}` 零宽右叠加（内容向右扩展）

### ❌ 缺失
- 无

**覆盖率**: 9/9 (100%) ✅

---

## 17. 高级标注

### ✅ 已支持

#### 超链接
- ✅ `\href{url}{text}` 超链接（蓝色下划线渲染，支持点击回调）
- ✅ `\url{url}` URL 链接（显示 URL 文本，蓝色下划线渲染）

#### 四角标注与前置上下标
- ✅ `\sideset{_a^b}{_c^d}{\sum}` 大型运算符四角上下标
- ✅ `\prescript{A}{Z}{X}` 前置上下标（同位素标记，原子前方标注质量数和原子序数）

#### 张量/指标
- ✅ `\tensor{T}{^a_b^c}` 张量指标排列
- ✅ `\indices{^a_b}` 独立指标（无基础符号）

### ❌ 缺失
- 无

**覆盖率**: 6/6 (100%) ✅

---

## 18. API 与工程能力

### ✅ 已支持
- ✅ `onNodeClick` 交互式子表达式回调（点击公式子部分触发回调，返回 SourceRange）
- ✅ `contentDescription` AccessibilityVisitor：MathSpeak 风格的屏幕阅读器描述
- ✅ `highlight API` HighlightConfig + HighlightRange 支持子表达式视觉高亮
- ✅ `conversion API` MathMLVisitor：Presentation MathML 输出
- ✅ `animation API` AnimatedLatex 组件：crossfade / slide / fade+slide 过渡
- ✅ `export API` rememberLatexExporter()：渲染结果导出为 PNG/JPEG/WEBP 图片格式
- ✅ `measure API` rememberLatexMeasurer()：预测量公式精确渲染尺寸，用于 Compose InlineTextContent 行内嵌入
- ✅ `cursor/input API` 所见即所得编辑器支持（位于 `latex-renderer/editor/` 子包）
- ✅ `diagnostics API` parseWithDiagnostics()：结构化诊断（8 种分类，按严重级别过滤）
- ✅ 错误指示渲染：用 `errorColor` 标记无法识别的命令，而非静默降级
- ✅ `NodeLayout 缓存` LayoutCache：相同 AST 子树 + 相同 RenderContext → 复用 layout，LRU 淘汰策略，避免重复测量（长公式、编辑器实时预览）
- ✅ `增量测量` IncrementalLatexParser + LayoutCache 协同：AST 部分变更时未变更前缀子树自动命中缓存，仅重新测量受影响节点（WYSIWYG 编辑器实时渲染）
- ✅ `基准测试套件` kotlinx-benchmark/JMH：标准化 benchmark 覆盖解析速度、测量速度、缓存冷热对比、增量测量端到端性能（持续集成回归检测）

### ❌ 缺失
- 无

**覆盖率**: 13/13 (100%) ✅

---

## 📊 总体覆盖率

| 类别 | 已支持 | 缺失 | 覆盖率 |
|-----|--------|------|--------|
| 基础结构 | 6/6 | 0 | 100% |
| 数学公式 | 7/7 | 0 | 100% |
| 符号系统 | 130+/130+ | 0 | 100% |
| 大型运算符 | 28/28 | 0 | 100% |
| 矩阵 | 8/8 | 0 | 100% |
| 括号分隔符 | 11/11 | 0 | 100% |
| 装饰符号 | 35/35 | 0 | 100% |
| 字体样式 | 17/17 | 0 | 100% |
| 数学模式切换 | 6/6 | 0 | 100% |
| 空格控制 | 8/8 | 0 | 100% |
| 环境 | 21/21 | 0 | 100% |
| 颜色与背景色 | 6/6 | 0 | 100% |
| 化学公式 | 13/13 | 0 | 100% |
| 标签与引用 | 5/5 | 0 | 100% |
| 自定义命令 | 9/9 | 0 | 100% |
| 特殊效果与布局 | 9/9 | 0 | 100% |
| 高级标注 | 6/6 | 0 | 100% |
| API 与工程能力 | 13/13 | 0 | 100% |
| 章节结构 | 6/6 | 0 | 100% |
| RTL 文本方向 | 7/7 | 0 | 100% |
| **总体** | **375+/375+** | **0** | **100%** |

---

## 🎯 结论

当前解析器在**数学公式核心功能**方面达到 **100% 覆盖**，涵盖 20 个功能类别、375+ 项已实现特性，无功能缺失。

**核心能力一览**：
- 完整的数学排版基础（分数、根号、上下标、矩阵、括号、装饰符号）
- 130+ 数学符号（希腊字母、运算符、关系符、箭头、AMS 扩展）
- 28 种大型运算符（含智能 displaystyle/textstyle 布局适配）
- 21 种环境（equation/align/gather/cases/multline 等，含星号变体和自动编号）
- 化学公式（`\ce{...}`，支持分子、离子、反应箭头）
- 自定义宏（`\newcommand`/`\renewcommand`/`\def`/`\newenvironment`/`\renewenvironment`，支持可选参数默认值）
- 章节结构命令（`\section`/`\subsection`/`\subsubsection`/`\paragraph`/`\subparagraph`，含星号变体）
- 丰富的 API（交互点击、可访问性、高亮、MathML 转换、动画、图片导出、编辑器集成、结构化诊断）

**推荐用于**：数学教科书排版、学术论文公式、物理公式、化学方程式、数学笔记、多行方程组、无障碍场景、公式高亮标注。

---

## 19. 章节结构命令

### ✅ 已支持
- ✅ `\section{title}` 一级标题（粗体 1.4x 字号）
- ✅ `\subsection{title}` 二级标题（粗体 1.2x 字号）
- ✅ `\subsubsection{title}` 三级标题（粗体 1.1x 字号）
- ✅ `\paragraph{title}` 段落标题（粗体正常字号）
- ✅ `\subparagraph{title}` 子段落标题（正常字号）
- ✅ 星号变体支持（`\section*{}` 等，无编号）

### ❌ 缺失
- 无

**覆盖率**: 6/6 (100%) ✅

---

## 20. RTL 文本方向

### ✅ 已支持

#### 命令
- ✅ `\RLE{...}` 强制从右到左排列
- ✅ `\LRE{...}` 强制从左到右排列（嵌套在 RTL 中使用）
- ✅ `\textarabic{...}` 阿拉伯语文本（RTL 方向）
- ✅ `\texthebrew{...}` 希伯来语文本（RTL 方向）

#### 环境
- ✅ `\begin{RTL}...\end{RTL}` RTL 环境
- ✅ `\begin{LTR}...\end{LTR}` LTR 环境（嵌套在 RTL 中使用）

#### 嵌套支持
- ✅ 支持嵌套方向切换（RTL 内嵌套 LTR，反之亦然）

### ❌ 缺失
- 无

**覆盖率**: 7/7 (100%) ✅

---

## 21. 功能扩展规划（Roadmap）

以下为待实现的功能。每项完成后应将状态标记为 ✅ 并移至对应章节。

---

### 🔵 P0 — 增值能力 / 工程级扩展

#### ❌ LaTeX → SVG 矢量导出
- **方向**: 将渲染结果导出为 SVG 矢量格式，补充已有的 PNG/JPEG/WEBP 光栅导出能力
- **场景**: 高清印刷、Web 嵌入、矢量图形保真输出

#### ❌ TikZ/PGF 基础绘图（简化子集）
- **场景**: 在公式中嵌入简单的坐标系、函数图像
- **范围**: 仅支持 `\draw` 线段、`\node` 文本、坐标点等核心子集
- **难度**: 需要新的 AST 节点和独立的 TikZ 解析器 + Canvas 绘制流水线

---

### 🟢 P1 — 渲染引擎优化与性能提升

#### ❌ Path 缓存池
- **方向**: 相同尺寸的根号/大括号 Path 复用
- **场景**: 矩阵中大量相同结构

#### ❌ 虚拟化/懒加载
- **方向**: 超长文档中只测量和绘制可视区域内的公式
- **场景**: 论文级多页公式文档

---

### 🟡 P2 — 新 LaTeX 功能扩展

#### ❌ physics 包常用命令
- **范围**: `\dv`/`\pdv`（导数简写）、`\bra`/`\ket`/`\braket`（狄拉克符号）、`\abs`/`\norm`（绝对值/范数简写）
- **场景**: 物理学公式排版

#### ❌ siunitx 包简化子集
- **范围**: `\SI{9.8}{m/s^2}` 国际单位制排版、`\si{kg.m/s^2}` 单位排版、`\num{1.23e4}` 数字格式化
- **场景**: 工程/物理场景高频需求

#### ❌ `\xlongequal` 可扩展长等号
- **范围**: 类似 `\xrightarrow` 的模式，支持上下标注的可扩展等号
- **场景**: 推导过程中的条件等式

#### ❌ 装饰符号扩展
- **范围**: `\circled{1}` 圆圈数字、`\enclose` 圆角边框/圆圈等扩展边框样式
- **场景**: 教学标注、重点标记

#### ❌ `\mathstrut` 最小高度保证
- **范围**: 保证一行中的最小高度一致
- **场景**: 多项式对齐等精细排版

#### ❌ 列表环境
- **范围**: `\begin{enumerate}`/`\begin{itemize}` 基本有序/无序列表
- **场景**: 非纯公式的混合文档排版

#### ❌ `\includegraphics` 简化子集
- **范围**: 支持在文档中嵌入图片（传入 ImageBitmap），支持 width/height 参数
- **场景**: 图文混排

---

### 🟠 P3 — API 能力与工程能力增强

#### ❌ LaTeX → Unicode 纯文本转换
- **方向**: 将公式转为 Unicode 近似文本（如 `\frac{1}{2}` → `½`，`x^2` → `x²`）
- **场景**: 复制到剪贴板、IM 消息发送

#### ❌ Compose 富文本混排 API
- **方向**: `InlineLatex` 组件 — 在普通 Compose `Text` 中无缝嵌入 LaTeX 公式片段（基于已有 `rememberLatexMeasurer` 进一步封装）
- **场景**: 聊天应用、笔记应用中的行内公式

#### ❌ 语法补全/提示 API
- **方向**: 为 WYSIWYG 编辑器提供命令自动补全、括号匹配提示
- **场景**: 编辑器用户体验提升

#### ❌ 错误恢复增强
- **方向**: 解析出错时增加"你是否想输入 X？"的修正建议（基于已有 diagnostics API 扩展）
- **场景**: 用户输入纠错

#### ❌ 主题系统
- **方向**: 预定义多套渲染主题（经典黑白、彩色教学、护眼模式、论文印刷），一键切换
- **场景**: 产品化集成

#### ❌ 剪贴板支持
- **方向**: 长按公式 → 复制 LaTeX 源码 / 复制为图片 / 复制为 MathML
- **场景**: 用户交互

#### ❌ 公式比较/Diff API
- **方向**: 比较两个 LaTeX AST 的差异，高亮变化部分
- **场景**: 数学批改、版本对比、教育科技

#### ❌ AST 序列化/反序列化
- **方向**: AST 的 JSON/Protobuf 序列化，支持缓存和网络传输
- **场景**: 前后端协作、离线缓存
