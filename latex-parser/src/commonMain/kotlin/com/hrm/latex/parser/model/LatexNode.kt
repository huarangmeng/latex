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
 */
sealed class LatexNode {
    /**
     * 文档根节点
     */
    data class Document(val children: List<LatexNode>) : LatexNode()
    
    /**
     * 文本节点
     */
    data class Text(val content: String) : LatexNode()
    
    /**
     * 命令节点（如 \frac, \sqrt 等）
     */
    data class Command(
        val name: String,
        val arguments: List<LatexNode> = emptyList(),
        val options: List<String> = emptyList()
    ) : LatexNode()
    
    /**
     * 环境节点（如 \begin{equation}...\end{equation}）
     */
    data class Environment(
        val name: String,
        val content: List<LatexNode>,
        val options: List<String> = emptyList()
    ) : LatexNode()
    
    /**
     * 分组节点（花括号包围的内容）
     */
    data class Group(val children: List<LatexNode>) : LatexNode()
    
    /**
     * 上标节点（^）
     */
    data class Superscript(val base: LatexNode, val exponent: LatexNode) : LatexNode()
    
    /**
     * 下标节点（_）
     */
    data class Subscript(val base: LatexNode, val index: LatexNode) : LatexNode()
    
    /**
     * 分数节点
     */
    data class Fraction(val numerator: LatexNode, val denominator: LatexNode) : LatexNode()
    
    /**
     * 根号节点
     */
    data class Root(val content: LatexNode, val index: LatexNode? = null) : LatexNode()
    
    /**
     * 矩阵节点
     */
    data class Matrix(
        val rows: List<List<LatexNode>>,
        val type: MatrixType = MatrixType.PLAIN,
        val isSmall: Boolean = false  // smallmatrix 标记
    ) : LatexNode() {
        enum class MatrixType {
            PLAIN,      // matrix
            PAREN,      // pmatrix ()
            BRACKET,    // bmatrix []
            BRACE,      // Bmatrix {}
            VBAR,       // vmatrix ||
            DOUBLE_VBAR // Vmatrix ||||
        }
    }
    
    /**
     * 数组节点（array环境，更通用的表格）
     */
    data class Array(
        val rows: List<List<LatexNode>>,
        val alignment: String  // 列对齐方式，如 "ccc", "rcl" 等
    ) : LatexNode()
    
    /**
     * 空格节点
     */
    data class Space(val type: SpaceType) : LatexNode() {
        enum class SpaceType {
            THIN,       // \,
            MEDIUM,     // \:
            THICK,      // \;
            QUAD,       // \quad
            QQUAD,      // \qquad
            NORMAL,     // normal space
            NEGATIVE_THIN // \!
        }
    }
    
    /**
     * 自定义水平空格节点 (\hspace)
     */
    data class HSpace(val dimension: String) : LatexNode()
    
    /**
     * 换行节点
     */
    data object NewLine : LatexNode()
    
    /**
     * 特殊符号节点（包括希腊字母、运算符符号、数学符号等）
     * 例如：α, β, ×, ÷, ≤, →
     */
    data class Symbol(val symbol: String, val unicode: String) : LatexNode()
    
    /**
     * 数学运算符节点
     * 注意：当前解析器将运算符符号（如 \times, \div）解析为 Symbol
     * 此类型保留用于未来可能的语义区分需求
     */
    data class Operator(val op: String) : LatexNode()
    
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
        val manualSize: Float? = null
    ) : LatexNode()
    
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
        val size: Float
    ) : LatexNode()
    
    /**
     * 装饰节点（如上划线、下划线、箭头等）
     */
    data class Accent(
        val content: LatexNode,
        val accentType: AccentType
    ) : LatexNode() {
        enum class AccentType {
            HAT, TILDE, BAR, DOT, DDOT, VEC, OVERLINE, UNDERLINE, OVERBRACE, UNDERBRACE,
            WIDEHAT, OVERRIGHTARROW, OVERLEFTARROW, CANCEL, BCANCEL, XCANCEL
        }
    }
    
    /**
     * 可扩展箭头节点（箭头上方或下方可显示文字）
     */
    data class ExtensibleArrow(
        val content: LatexNode,  // 箭头上方的文字
        val below: LatexNode?,   // 箭头下方的文字（可选）
        val direction: Direction
    ) : LatexNode() {
        enum class Direction {
            RIGHT,   // \xrightarrow
            LEFT,    // \xleftarrow
            BOTH     // \xleftrightarrow
        }
    }
    
    /**
     * 堆叠节点（在基础内容上方或下方添加内容）
     */
    data class Stack(
        val base: LatexNode,     // 基础内容（必选）
        val above: LatexNode?,   // 上方内容（可选）
        val below: LatexNode?    // 下方内容（可选）
    ) : LatexNode()
    
    /**
     * 字体样式节点
     */
    data class Style(
        val content: List<LatexNode>,
        val styleType: StyleType
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
    }
    
    /**
     * 颜色节点
     */
    data class Color(
        val content: List<LatexNode>,
        val color: String
    ) : LatexNode()
    
    /**
     * 数学模式节点（控制公式大小）
     * 
     * @property content 内容
     * @property mathStyleType 数学模式类型
     */
    data class MathStyle(
        val content: List<LatexNode>,
        val mathStyleType: MathStyleType
    ) : LatexNode() {
        enum class MathStyleType {
            DISPLAY,         // \displaystyle - 显示模式（最大）
            TEXT,            // \textstyle - 文本模式（正常）
            SCRIPT,          // \scriptstyle - 脚本模式（上下标大小）
            SCRIPT_SCRIPT    // \scriptscriptstyle - 小脚本模式（二级上下标大小）
        }
    }
    
    /**
     * 大型运算符（求和、积分、乘积等）
     */
    data class BigOperator(
        val operator: String,
        val subscript: LatexNode? = null,
        val superscript: LatexNode? = null,
        val limitsMode: LimitsMode = LimitsMode.AUTO
    ) : LatexNode() {
        enum class LimitsMode {
            AUTO,       // 默认（根据 MathStyle 和运算符类型决定）
            LIMITS,     // 强制上下模式 (\limits)
            NOLIMITS    // 强制侧边模式 (\nolimits)
        }
    }
    
    /**
     * 对齐环境
     */
    data class Aligned(
        val rows: List<List<LatexNode>>,
        val alignType: AlignType = AlignType.CENTER
    ) : LatexNode() {
        enum class AlignType {
            LEFT, CENTER, RIGHT
        }
    }
    
    /**
     * 案例环境（cases）
     */
    data class Cases(val cases: List<Pair<LatexNode, LatexNode>>) : LatexNode()
    
    /**
     * Split 环境（用于单个方程内的多行分割）
     */
    data class Split(val rows: List<List<LatexNode>>) : LatexNode()
    
    /**
     * Multline 环境（多行单个方程）
     */
    data class Multline(val lines: List<LatexNode>) : LatexNode()
    
    /**
     * Eqnarray 环境（旧式方程数组）
     */
    data class Eqnarray(val rows: List<List<LatexNode>>) : LatexNode()
    
    /**
     * Subequations 环境（子方程编号）
     */
    data class Subequations(val content: List<LatexNode>) : LatexNode()
    
    /**
     * 二项式系数 \binom{n}{k}
     */
    data class Binomial(
        val top: LatexNode,
        val bottom: LatexNode,
        val style: BinomialStyle = BinomialStyle.NORMAL
    ) : LatexNode() {
        enum class BinomialStyle {
            NORMAL,   // \binom
            TEXT,     // \tbinom (text style)
            DISPLAY   // \dbinom (display style)
        }
    }
    
    /**
     * 文本模式（在数学公式中插入普通文本）
     */
    data class TextMode(val text: String) : LatexNode()
    
    /**
     * 方框节点（\boxed）
     * 在内容周围绘制边框
     */
    data class Boxed(val content: List<LatexNode>) : LatexNode()
    
    /**
     * 幻影节点（\phantom）
     * 占据空间但不显示内容，用于对齐
     */
    data class Phantom(val content: List<LatexNode>) : LatexNode()
    
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
        val definition: List<LatexNode>
    ) : LatexNode()

    /**
     * 否定修饰节点（\not）
     * 在关系符号上叠加斜线表示否定
     * 例如：\not= → ≠, \not\in → ∉
     */
    data class Negation(val content: LatexNode) : LatexNode()

    /**
     * 公式编号标签节点（\tag, \tag*）
     * @param label 标签内容
     * @param starred true = \tag*（不加括号），false = \tag（加括号）
     */
    data class Tag(
        val label: LatexNode,
        val starred: Boolean = false
    ) : LatexNode()

    /**
     * 多行下标条件节点（\substack）
     * 用于大型运算符上下限排列多行条件
     * @param rows 每行是一个节点列表
     */
    data class Substack(val rows: List<List<LatexNode>>) : LatexNode()

    /**
     * Smash 节点（\smash）
     * 将内容的高度或深度视为零，用于间距微调
     */
    data class Smash(val content: List<LatexNode>) : LatexNode()

    /**
     * 垂直幻影节点（\vphantom）
     * 只保留高度/基线，宽度为零
     */
    data class VPhantom(val content: List<LatexNode>) : LatexNode()

    /**
     * 水平幻影节点（\hphantom）
     * 只保留宽度，高度/基线使用最小值
     */
    data class HPhantom(val content: List<LatexNode>) : LatexNode()
}
