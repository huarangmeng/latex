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
            NORMAL      // normal space
        }
    }
    
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
            WIDEHAT, OVERRIGHTARROW, OVERLEFTARROW
        }
    }
    
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
     * 大型运算符（求和、积分、乘积等）
     */
    data class BigOperator(
        val operator: String,
        val subscript: LatexNode? = null,
        val superscript: LatexNode? = null
    ) : LatexNode()
    
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
}
