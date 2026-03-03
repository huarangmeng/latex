/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.hrm.latex.parser.model

/**
 * LaTeX 抽象语法树节点
 *
 * 每个节点可选地携带 [sourceRange]，表示该节点在原始 LaTeX 输入字符串中的位置范围。
 * 默认为 null，表示位置未知（例如宏展开生成的节点）。
 *
 * ## 自描述协议
 * 每个节点实现三个自描述方法，消除外部对节点结构的重复 when 表达式：
 * - [children]：返回所有直接子节点（有序列表）
 * - [withSourceRange]：创建带有新 sourceRange 的副本
 * - [withChildren]：用新子节点列表重建自身（结构不变，子节点替换）
 */
sealed class LatexNode {
    abstract val sourceRange: SourceRange?

    /** 返回所有直接子节点（有序）。叶节点返回 emptyList()。 */
    abstract fun children(): List<LatexNode>

    /** 用新 sourceRange 创建副本 */
    abstract fun withSourceRange(range: SourceRange): LatexNode

    /**
     * 用新子节点列表重建自身。
     * 子节点的顺序和数量必须与 [children] 返回值一致。
     * 叶节点返回自身。
     */
    abstract fun withChildren(newChildren: List<LatexNode>): LatexNode

    /**
     * 文档根节点
     */
    data class Document(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
    }
    
    /**
     * 文本节点
     */
    data class Text(
        val content: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 命令节点（如 \frac, \sqrt 等）
     */
    data class Command(
        val name: String,
        val arguments: List<LatexNode> = emptyList(),
        val options: List<String> = emptyList(),
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = arguments
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(arguments = newChildren)
    }
    
    /**
     * 环境节点（如 \begin{equation}...\end{equation}）
     */
    data class Environment(
        val name: String,
        val content: List<LatexNode>,
        val options: List<String> = emptyList(),
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 分组节点（花括号包围的内容）
     */
    data class Group(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
    }
    
    /**
     * 上标节点（^）
     */
    data class Superscript(
        val base: LatexNode,
        val exponent: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(base, exponent)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(base = newChildren[0], exponent = newChildren[1])
    }
    
    /**
     * 下标节点（_）
     */
    data class Subscript(
        val base: LatexNode,
        val index: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(base, index)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(base = newChildren[0], index = newChildren[1])
    }
    
    /**
     * 分数节点
     */
    data class Fraction(
        val numerator: LatexNode,
        val denominator: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(numerator, denominator)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(numerator = newChildren[0], denominator = newChildren[1])
    }
    
    /**
     * 根号节点
     */
    data class Root(
        val content: LatexNode,
        val index: LatexNode? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(content, index)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newContent = newChildren[0]
            val newIndex = if (index != null) newChildren.getOrNull(1) else null
            return copy(content = newContent, index = newIndex)
        }
    }
    
    /**
     * 矩阵节点
     */
    data class Matrix(
        val rows: List<List<LatexNode>>,
        val type: MatrixType = MatrixType.PLAIN,
        val isSmall: Boolean = false,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class MatrixType {
            PLAIN,      // matrix
            PAREN,      // pmatrix ()
            BRACKET,    // bmatrix []
            BRACE,      // Bmatrix {}
            VBAR,       // vmatrix ||
            DOUBLE_VBAR // Vmatrix ||||
        }

        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            // 按原始行结构重建 rows
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }
    
    /**
     * 数组节点（array环境，更通用的表格）
     */
    data class Array(
        val rows: List<List<LatexNode>>,
        val alignment: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }
    
    /**
     * 空格节点
     */
    data class Space(
        val type: SpaceType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class SpaceType {
            THIN,       // \,
            MEDIUM,     // \:
            THICK,      // \;
            QUAD,       // \quad
            QQUAD,      // \qquad
            NORMAL,     // normal space
            NEGATIVE_THIN // \!
        }

        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 自定义水平空格节点 (\hspace)
     */
    data class HSpace(
        val dimension: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 换行节点
     */
    data class NewLine(
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 特殊符号节点（包括希腊字母、运算符符号、数学符号等）
     * 例如：α, β, ×, ÷, ≤, →
     */
    data class Symbol(
        val symbol: String,
        val unicode: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 数学运算符节点
     * 注意：当前解析器将运算符符号（如 \times, \div）解析为 Symbol
     * 此类型保留用于未来可能的语义区分需求
     */
    data class Operator(
        val op: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 括号节点（自动伸缩）
     * 
     * 使用场景：
     * 1. 自动伸缩：`\left( ... \right)` - scalable=true
     * 2. 不对称分隔符：`\left. ... \right|` - left="" 或 right=""
     * 
     * @param left 左分隔符（空字符串表示不显示）
     * @param right 右分隔符（空字符串表示不显示）
     * @param content 括号内的内容
     * @param scalable true=自动伸缩（始终为 true，保留用于向后兼容）
     * @param manualSize 已弃用，保留用于向后兼容
     */
    data class Delimited(
        val left: String,
        val right: String,
        val content: List<LatexNode>,
        val scalable: Boolean = true,
        val manualSize: Float? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 手动大小分隔符节点
     * 
     * 与 Delimited 不同，这是一个独立的符号，不包裹内容
     * 
     * 使用场景：
     * - `\big(` - 生成一个 1.2x 大小的左括号符号
     * - `\Big[` - 生成一个 1.8x 大小的左方括号符号
     * - `\bigg\{` - 生成一个 2.4x 大小的左花括号符号
     * 
     * @param delimiter 分隔符符号（如 "(", "[", "|" 等）
     * @param size 缩放因子（1.2f, 1.8f, 2.4f, 3.0f）
     */
    data class ManualSizedDelimiter(
        val delimiter: String,
        val size: Float,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 装饰节点（如上划线、下划线、箭头等）
     */
    data class Accent(
        val content: LatexNode,
        val accentType: AccentType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class AccentType {
            HAT, TILDE, BAR, DOT, DDOT, DDDOT, VEC, OVERLINE, UNDERLINE, OVERBRACE, UNDERBRACE,
            WIDEHAT, OVERRIGHTARROW, OVERLEFTARROW, CANCEL, BCANCEL, XCANCEL,
            GRAVE, ACUTE, CHECK, BREVE, RING
        }

        override fun children() = listOf(content)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren[0])
    }
    
    /**
     * 可扩展箭头节点（箭头上方或下方可显示文字）
     */
    data class ExtensibleArrow(
        val content: LatexNode,
        val below: LatexNode?,
        val direction: Direction,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class Direction {
            RIGHT,       // \xrightarrow
            LEFT,        // \xleftarrow
            BOTH,        // \xleftrightarrow
            HOOK_RIGHT,  // \xhookrightarrow
            HOOK_LEFT    // \xhookleftarrow
        }

        override fun children() = listOfNotNull(content, below)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newContent = newChildren[0]
            val newBelow = if (below != null) newChildren.getOrNull(1) else null
            return copy(content = newContent, below = newBelow)
        }
    }
    
    /**
     * 堆叠节点（在基础内容上方或下方添加内容）
     */
    data class Stack(
        val base: LatexNode,
        val above: LatexNode?,
        val below: LatexNode?,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(base, above, below)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newBase = newChildren[idx++]
            val newAbove = if (above != null) newChildren.getOrNull(idx++)  else null
            val newBelow = if (below != null) newChildren.getOrNull(idx) else null
            return copy(base = newBase, above = newAbove, below = newBelow)
        }
    }
    
    /**
     * 字体样式节点
     */
    data class Style(
        val content: List<LatexNode>,
        val styleType: StyleType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class StyleType {
            BOLD,              // \mathbf - 文本粗体
            BOLD_SYMBOL,       // \boldsymbol - 符号粗体（包括希腊字母）
            ITALIC,            // \mathit
            ROMAN,             // \mathrm
            SANS_SERIF,        // \mathsf
            MONOSPACE,         // \mathtt
            BLACKBOARD_BOLD,   // \mathbb
            FRAKTUR,           // \mathfrak
            SCRIPT,            // \mathscr
            CALLIGRAPHIC       // \mathcal
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 颜色节点
     */
    data class Color(
        val content: List<LatexNode>,
        val color: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 数学模式节点（控制公式大小）
     * 
     * @property content 内容
     * @property mathStyleType 数学模式类型
     */
    data class MathStyle(
        val content: List<LatexNode>,
        val mathStyleType: MathStyleType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class MathStyleType {
            DISPLAY,         // \displaystyle - 显示模式（最大）
            TEXT,            // \textstyle - 文本模式（正常）
            SCRIPT,          // \scriptstyle - 脚本模式（上下标大小）
            SCRIPT_SCRIPT    // \scriptscriptstyle - 小脚本模式（二级上下标大小）
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 大型运算符（求和、积分、乘积等）
     */
    data class BigOperator(
        val operator: String,
        val subscript: LatexNode? = null,
        val superscript: LatexNode? = null,
        val limitsMode: LimitsMode = LimitsMode.AUTO,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class LimitsMode {
            AUTO,       // 默认（根据 MathStyle 和运算符类型决定）
            LIMITS,     // 强制上下模式 (\limits)
            NOLIMITS    // 强制侧边模式 (\nolimits)
        }

        override fun children() = listOfNotNull(subscript, superscript)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newSub = if (subscript != null) newChildren.getOrNull(idx++) else null
            val newSup = if (superscript != null) newChildren.getOrNull(idx) else null
            return copy(subscript = newSub, superscript = newSup)
        }
    }
    
    /**
     * 对齐环境
     */
    data class Aligned(
        val rows: List<List<LatexNode>>,
        val alignType: AlignType = AlignType.CENTER,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class AlignType {
            LEFT, CENTER, RIGHT
        }

        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }
    
    /**
     * 案例环境（cases / dcases / rcases）
     * @param cases 每个 case 是 (表达式, 条件) 对
     * @param style 变体风格：NORMAL=cases, DISPLAY=dcases(displaystyle), RIGHT=rcases(右花括号)
     */
    data class Cases(
        val cases: List<Pair<LatexNode, LatexNode>>,
        val style: CasesStyle = CasesStyle.NORMAL,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class CasesStyle {
            NORMAL,   // cases — 标准左花括号
            DISPLAY,  // dcases — 使用 displaystyle 的左花括号
            RIGHT     // rcases — 右花括号
        }

        override fun children() = cases.flatMap { listOf(it.first, it.second) }
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newCases = mutableListOf<Pair<LatexNode, LatexNode>>()
            var idx = 0
            for (_case in cases) {
                newCases.add(newChildren[idx++] to newChildren[idx++])
            }
            return copy(cases = newCases)
        }
    }
    
    /**
     * Split 环境（用于单个方程内的多行分割）
     */
    data class Split(
        val rows: List<List<LatexNode>>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }
    
    /**
     * Multline 环境（多行单个方程）
     */
    data class Multline(
        val lines: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = lines
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(lines = newChildren)
    }
    
    /**
     * Eqnarray 环境（旧式方程数组）
     */
    data class Eqnarray(
        val rows: List<List<LatexNode>>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }
    
    /**
     * Subequations 环境（子方程编号）
     */
    data class Subequations(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 二项式系数 \binom{n}{k}
     */
    data class Binomial(
        val top: LatexNode,
        val bottom: LatexNode,
        val style: BinomialStyle = BinomialStyle.NORMAL,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class BinomialStyle {
            NORMAL,   // \binom
            TEXT,     // \tbinom (text style)
            DISPLAY   // \dbinom (display style)
        }

        override fun children() = listOf(top, bottom)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(top = newChildren[0], bottom = newChildren[1])
    }
    
    /**
     * 文本模式（在数学公式中插入普通文本）
     */
    data class TextMode(
        val text: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }
    
    /**
     * 方框节点（\boxed）
     * 在内容周围绘制边框
     */
    data class Boxed(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 幻影节点（\phantom）
     * 占据空间但不显示内容，用于对齐
     */
    data class Phantom(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }
    
    /**
     * 自定义命令定义节点（\newcommand）
     * 该节点不参与渲染，仅用于记录命令定义
     * @param commandName 命令名（不含反斜杠）
     * @param numArgs 参数个数（0-9）
     * @param definition 命令定义（AST 节点列表）
     */
    data class NewCommand(
        val commandName: String,
        val numArgs: Int,
        val definition: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = definition
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(definition = newChildren)
    }

    /**
     * 否定修饰节点（\not）
     * 在关系符号上叠加斜线表示否定
     * 例如：\not= → ≠, \not\in → ∉
     */
    data class Negation(
        val content: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(content)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren[0])
    }

    /**
     * 公式编号标签节点（\tag, \tag*）
     * @param label 标签内容
     * @param starred true = \tag*（不加括号），false = \tag（加括号）
     */
    data class Tag(
        val label: LatexNode,
        val starred: Boolean = false,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(label)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(label = newChildren[0])
    }

    /**
     * 多行下标条件节点（\substack）
     * 用于大型运算符上下限排列多行条件
     * @param rows 每行是一个节点列表
     */
    data class Substack(
        val rows: List<List<LatexNode>>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }

    /**
     * Smash 节点（\smash）
     * 将内容的高度或深度视为零，用于间距微调
     * @param content 内容
     * @param smashType 压缩类型：BOTH=全部，TOP=只压顶部，BOTTOM=只压底部
     */
    data class Smash(
        val content: List<LatexNode>,
        val smashType: SmashType = SmashType.BOTH,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class SmashType {
            BOTH,   // \smash{x} — 压缩高度和深度
            TOP,    // \smash[t]{x} — 只压顶部（ascent）
            BOTTOM  // \smash[b]{x} — 只压底部（descent）
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }

    /**
     * 垂直幻影节点（\vphantom）
     * 只保留高度/基线，宽度为零
     */
    data class VPhantom(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }

    /**
     * 水平幻影节点（\hphantom）
     * 只保留宽度，高度/基线使用最小值
     */
    data class HPhantom(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }

    /**
     * 标签定义节点（\label{key}）
     * 该节点不参与渲染，仅记录标签定义
     * @param key 标签键名
     */
    data class Label(
        val key: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }

    /**
     * 引用节点（\ref{key}）
     * 渲染时显示引用的标签编号
     * @param key 引用的标签键名
     */
    data class Ref(
        val key: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }

    /**
     * 公式引用节点（\eqref{key}）
     * 渲染时显示带括号的引用标签编号
     * @param key 引用的标签键名
     */
    data class EqRef(
        val key: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }

    /**
     * 四角标注节点（\sideset{_a^b}{_c^d}{\sum}）
     * 在大型运算符的四个角放置上下标
     * @param leftSub 左下标
     * @param leftSup 左上标
     * @param rightSub 右下标
     * @param rightSup 右上标
     * @param base 基础运算符
     */
    data class SideSet(
        val leftSub: LatexNode?,
        val leftSup: LatexNode?,
        val rightSub: LatexNode?,
        val rightSup: LatexNode?,
        val base: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(leftSub, leftSup, rightSub, rightSup, base)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newLeftSub = if (leftSub != null) newChildren[idx++] else null
            val newLeftSup = if (leftSup != null) newChildren[idx++] else null
            val newRightSub = if (rightSub != null) newChildren[idx++] else null
            val newRightSup = if (rightSup != null) newChildren[idx++] else null
            val newBase = newChildren[idx]
            return copy(leftSub = newLeftSub, leftSup = newLeftSup, rightSub = newRightSub, rightSup = newRightSup, base = newBase)
        }
    }

    /**
     * 张量/指标节点（\tensor{T}{^a_b^c}）
     * 物理学中用于排列多个上下标指标
     * @param base 基础符号
     * @param indices 指标列表（每个是 Pair<Boolean, LatexNode>，Boolean=true 表示上标）
     */
    data class Tensor(
        val base: LatexNode,
        val indices: List<Pair<Boolean, LatexNode>>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(base) + indices.map { it.second }
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newBase = newChildren[0]
            val newIndices = indices.mapIndexed { i, (isSuper, _) ->
                isSuper to newChildren[i + 1]
            }
            return copy(base = newBase, indices = newIndices)
        }
    }

    /**
     * 表格环境节点（\begin{tabular}{ccc}...）
     * 文本模式下的表格
     * @param rows 行列表，每行是单元格列表
     * @param alignment 列对齐方式字符串（如 "ccc", "lcr", "|c|c|c|"）
     */
    data class Tabular(
        val rows: List<List<LatexNode>>,
        val alignment: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newRows = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in rows) {
                val newRow = mutableListOf<LatexNode>()
                for (_cell in row) {
                    newRow.add(newChildren[idx++])
                }
                newRows.add(newRow)
            }
            return copy(rows = newRows)
        }
    }

    /**
     * 水平线节点（\hline）
     * 用于表格中绘制整行水平线
     */
    data class HLine(
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }

    /**
     * 部分水平线节点（\cline{start-end}）
     * 用于表格中绘制跨指定列的水平线
     * @param startCol 起始列（从1开始）
     * @param endCol 结束列（从1开始）
     */
    data class CLine(
        val startCol: Int,
        val endCol: Int,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }

    /**
     * 合并单元格节点（\multicolumn{num}{align}{content}）
     * @param columnCount 合并的列数
     * @param alignment 对齐方式字符串（如 "c", "|c|"）
     * @param content 单元格内容
     */
    data class Multicolumn(
        val columnCount: Int,
        val alignment: String,
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
    }

    /**
     * 自定义运算符名称节点（\operatorname{Tr}）
     * 以正体渲染运算符名，可带上下标
     * @param name 运算符名称
     */
    data class OperatorName(
        val name: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
    }

    /**
     * 取模运算符节点
     * @param content 可选的参数内容（如 \pmod{n} 中的 n）
     * @param modStyle 取模风格
     */
    data class ModOperator(
        val content: LatexNode?,
        val modStyle: ModStyle,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class ModStyle {
            BMOD,  // \bmod — 二元运算符 "mod"
            PMOD,  // \pmod{n} — (mod n)
            MOD    // \mod — 间距更大的 "mod"
        }

        override fun children() = listOfNotNull(content)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newContent = if (content != null) newChildren.firstOrNull() else null
            return copy(content = newContent)
        }
    }

    /**
     * 行内数学模式节点（$...$）
     * 用于在文本中嵌入行内数学公式
     * @param children 数学内容
     */
    data class InlineMath(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
    }

    /**
     * 展示数学模式节点（$$...$$）
     * 用于独立行的数学公式
     * @param children 数学内容
     */
    data class DisplayMath(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
    }
}
