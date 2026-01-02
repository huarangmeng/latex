package com.hrm.latex.parser

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * LaTeX 语法解析器
 */
class LatexParser {
    private lateinit var tokens: List<LatexToken>
    private var position = 0

    companion object {
        private const val TAG = "LatexParser"
    }

    /**
     * 解析 LaTeX 字符串
     * @throws ParseException 解析失败时抛出异常
     */
    fun parse(input: String): LatexNode.Document {
        HLog.d(TAG, "开始解析 LaTeX: $input")

        // 词法分析
        val tokenizer = LatexTokenizer(input)
        tokens = tokenizer.tokenize()
        position = 0

        // 语法分析
        val children = mutableListOf<LatexNode>()
        while (!isEOF()) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        val document = LatexNode.Document(children)
        HLog.d(TAG, "解析成功，生成 ${children.size} 个节点")
        return document
    }

    private fun peek(offset: Int = 0): LatexToken? {
        val pos = position + offset
        return if (pos < tokens.size) tokens[pos] else null
    }

    private fun advance(): LatexToken? {
        val token = peek()
        position++
        return token
    }

    private fun isEOF(): Boolean {
        return peek() is LatexToken.EOF
    }

    private fun expect(type: String): LatexToken {
        val token = peek()
        if (token == null) {
            throw ParseException("期望 $type，但到达文件末尾")
        }
        advance()
        return token
    }

    /**
     * 解析表达式（处理上标下标）
     */
    private fun parseExpression(): LatexNode? {
        var node = parseFactor() ?: return null

        while (true) {
            val token = peek()
            if (token is LatexToken.Superscript) {
                advance()
                val exponent = parseScriptContent()
                node = LatexNode.Superscript(node, exponent)
            } else if (token is LatexToken.Subscript) {
                advance()
                val index = parseScriptContent()
                node = LatexNode.Subscript(node, index)
            } else {
                break
            }
        }
        return node
    }

    /**
     * 解析基本因子（不含上标下标）
     */
    private fun parseFactor(): LatexNode? {
        when (val token = peek()) {
            is LatexToken.Text -> {
                advance()
                return LatexNode.Text(token.content)
            }

            is LatexToken.Command -> {
                return parseCommand()
            }

            is LatexToken.BeginEnvironment -> {
                return parseEnvironment()
            }

            is LatexToken.LeftBrace -> {
                return parseGroup()
            }

            is LatexToken.Superscript, is LatexToken.Subscript -> {
                // 如果直接遇到上标下标，说明没有 Base，可能是语法错误
                // 这里我们跳过它以避免死循环，并返回 null
                advance()
                return null
            }

            is LatexToken.Whitespace -> {
                advance()
                return LatexNode.Space(LatexNode.Space.SpaceType.NORMAL)
            }

            is LatexToken.NewLine -> {
                advance()
                return LatexNode.NewLine
            }

            is LatexToken.EOF -> return null
            else -> {
                advance()
                return null
            }
        }
    }

    /**
     * 解析命令
     */
    private fun parseCommand(): LatexNode? {
        val cmdToken = advance() as? LatexToken.Command ?: return null
        val cmdName = cmdToken.name

        HLog.d(TAG, "解析命令: \\$cmdName")

        return when (cmdName) {
            // 分数
            "frac", "dfrac", "tfrac", "cfrac" -> parseFraction()

            // 二项式系数
            "binom" -> parseBinomial(LatexNode.Binomial.BinomialStyle.NORMAL)
            "tbinom" -> parseBinomial(LatexNode.Binomial.BinomialStyle.TEXT)
            "dbinom" -> parseBinomial(LatexNode.Binomial.BinomialStyle.DISPLAY)

            // 根号
            "sqrt" -> parseRoot()

            // 文本模式
            "text", "mbox" -> parseTextMode()

            // 上下标（大型运算符）
            "sum", "prod", "int", "oint", "iint", "iiint",
            "bigcup", "bigcap", "bigvee", "bigwedge",
            "lim", "max", "min", "sup", "inf", "limsup", "liminf" -> parseBigOperator(cmdName)

            // 括号
            "left" -> parseDelimited()

            // 字体样式
            "mathbf", "textbf", "bf" -> parseStyle(LatexNode.Style.StyleType.BOLD)
            "boldsymbol", "bm" -> parseStyle(LatexNode.Style.StyleType.BOLD_SYMBOL)
            "mathit", "textit", "it" -> parseStyle(LatexNode.Style.StyleType.ITALIC)
            "mathrm", "textrm", "rm" -> parseStyle(LatexNode.Style.StyleType.ROMAN)
            "mathsf", "textsf", "sf" -> parseStyle(LatexNode.Style.StyleType.SANS_SERIF)
            "mathtt", "texttt", "tt" -> parseStyle(LatexNode.Style.StyleType.MONOSPACE)
            "mathbb" -> parseStyle(LatexNode.Style.StyleType.BLACKBOARD_BOLD)
            "mathfrak" -> parseStyle(LatexNode.Style.StyleType.FRAKTUR)
            "mathscr" -> parseStyle(LatexNode.Style.StyleType.SCRIPT)
            "mathcal" -> parseStyle(LatexNode.Style.StyleType.CALLIGRAPHIC)

            // 装饰
            "hat" -> parseAccent(LatexNode.Accent.AccentType.HAT)
            "tilde", "widetilde" -> parseAccent(LatexNode.Accent.AccentType.TILDE)
            "bar", "overline" -> parseAccent(LatexNode.Accent.AccentType.OVERLINE)
            "underline" -> parseAccent(LatexNode.Accent.AccentType.UNDERLINE)
            "dot" -> parseAccent(LatexNode.Accent.AccentType.DOT)
            "ddot" -> parseAccent(LatexNode.Accent.AccentType.DDOT)
            "vec" -> parseAccent(LatexNode.Accent.AccentType.VEC)
            "overbrace" -> parseAccent(LatexNode.Accent.AccentType.OVERBRACE)
            "underbrace" -> parseAccent(LatexNode.Accent.AccentType.UNDERBRACE)

            // 空格
            "," -> LatexNode.Space(LatexNode.Space.SpaceType.THIN)
            ":" -> LatexNode.Space(LatexNode.Space.SpaceType.MEDIUM)
            ";" -> LatexNode.Space(LatexNode.Space.SpaceType.THICK)
            "quad" -> LatexNode.Space(LatexNode.Space.SpaceType.QUAD)
            "qquad" -> LatexNode.Space(LatexNode.Space.SpaceType.QQUAD)

            // 特殊符号
            else -> parseSymbolOrGenericCommand(cmdName)
        }
    }

    /**
     * 解析分数
     */
    private fun parseFraction(): LatexNode.Fraction {
        val numerator = parseArgument() ?: LatexNode.Text("")
        val denominator = parseArgument() ?: LatexNode.Text("")
        return LatexNode.Fraction(numerator, denominator)
    }

    /**
     * 解析根号
     */
    private fun parseRoot(): LatexNode.Root {
        // 检查是否有可选参数 [n]
        val index = if (peek() is LatexToken.LeftBracket) {
            advance() // [
            val indexNode = parseUntil { it is LatexToken.RightBracket }
            expect("]")
            LatexNode.Group(indexNode)
        } else {
            null
        }

        val content = parseArgument() ?: LatexNode.Text("")
        return LatexNode.Root(content, index)
    }

    /**
     * 解析二项式系数 \binom{n}{k}
     */
    private fun parseBinomial(style: LatexNode.Binomial.BinomialStyle): LatexNode.Binomial {
        val top = parseArgument() ?: LatexNode.Text("")
        val bottom = parseArgument() ?: LatexNode.Text("")
        return LatexNode.Binomial(top, bottom, style)
    }

    /**
     * 解析文本模式 \text{...}
     */
    private fun parseTextMode(): LatexNode.TextMode {
        val content = parseArgument()
        // 提取文本内容
        val text = when (content) {
            is LatexNode.Text -> content.content
            is LatexNode.Group -> {
                // 从 Group 中提取所有文本
                extractText(content.children)
            }

            else -> ""
        }
        return LatexNode.TextMode(text)
    }

    /**
     * 从节点列表中提取纯文本
     */
    private fun extractText(nodes: List<LatexNode>): String {
        return nodes.joinToString("") { node ->
            when (node) {
                is LatexNode.Text -> node.content
                is LatexNode.Group -> extractText(node.children)
                is LatexNode.Space -> " "
                else -> ""
            }
        }
    }

    /**
     * 解析大型运算符
     */
    private fun parseBigOperator(operator: String): LatexNode {
        var subscript: LatexNode? = null
        var superscript: LatexNode? = null

        // 检查下标
        if (peek() is LatexToken.Subscript) {
            advance()
            subscript = parseScriptContent()
        }

        // 检查上标
        if (peek() is LatexToken.Superscript) {
            advance()
            superscript = parseScriptContent()
        }

        // 也可能是上标在前
        if (subscript == null && peek() is LatexToken.Subscript) {
            advance()
            subscript = parseScriptContent()
        }

        return LatexNode.BigOperator(operator, subscript, superscript)
    }

    /**
     * 解析可伸缩括号
     */
    private fun parseDelimited(): LatexNode.Delimited {
        // 读取左括号
        val leftToken = advance()
        val left = when (leftToken) {
            is LatexToken.Text -> leftToken.content
            is LatexToken.LeftBrace -> "{"
            is LatexToken.Command -> {
                when (leftToken.name) {
                    "langle" -> "⟨"
                    "lfloor" -> "⌊"
                    "lceil" -> "⌈"
                    else -> leftToken.name
                }
            }

            else -> "("
        }

        // 解析内容，直到遇到 \right
        val content = mutableListOf<LatexNode>()
        while (!isEOF()) {
            if (peek() is LatexToken.Command && (peek() as LatexToken.Command).name == "right") {
                break
            }
            val node = parseExpression()
            if (node != null) {
                content.add(node)
            }
        }

        // 读取 \right
        if (peek() is LatexToken.Command && (peek() as LatexToken.Command).name == "right") {
            advance()
        }

        // 读取右括号
        val rightToken = advance()
        val right = when (rightToken) {
            is LatexToken.Text -> rightToken.content
            is LatexToken.RightBrace -> "}"
            is LatexToken.Command -> {
                when (rightToken.name) {
                    "rangle" -> "⟩"
                    "rfloor" -> "⌋"
                    "rceil" -> "⌉"
                    else -> rightToken.name
                }
            }

            else -> ")"
        }

        return LatexNode.Delimited(left, right, content, true)
    }

    /**
     * 解析字体样式
     */
    private fun parseStyle(styleType: LatexNode.Style.StyleType): LatexNode.Style {
        val content = parseArgument()
        return LatexNode.Style(
            if (content != null) listOf(content) else emptyList(),
            styleType
        )
    }

    /**
     * 解析装饰
     */
    private fun parseAccent(accentType: LatexNode.Accent.AccentType): LatexNode.Accent {
        val content = parseArgument() ?: LatexNode.Text("")
        return LatexNode.Accent(content, accentType)
    }

    /**
     * 解析符号或通用命令
     */
    private fun parseSymbolOrGenericCommand(cmdName: String): LatexNode {
        // 符号查找（包括希腊字母、运算符符号、特殊符号等）
        val unicode = SymbolMap.getSymbol(cmdName)
        if (unicode != null) {
            return LatexNode.Symbol(cmdName, unicode)
        }

        // 通用命令
        val arguments = mutableListOf<LatexNode>()
        while (peek() is LatexToken.LeftBrace) {
            val arg = parseArgument()
            if (arg != null) {
                arguments.add(arg)
            } else {
                break
            }
        }

        return LatexNode.Command(cmdName, arguments)
    }

    /**
     * 解析环境
     */
    private fun parseEnvironment(): LatexNode? {
        val beginToken = advance() as? LatexToken.BeginEnvironment ?: return null
        val envName = beginToken.name

        HLog.d(TAG, "解析环境: $envName")

        return when (envName) {
            "matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix" -> parseMatrix(
                envName,
                isSmall = false
            )

            "smallmatrix" -> parseMatrix("matrix", isSmall = true)
            "array" -> parseArray()
            "align", "aligned", "gather", "gathered" -> parseAligned()
            "cases" -> parseCases()
            "equation", "displaymath" -> {
                val content = parseEnvironmentContent(envName)
                LatexNode.Environment(envName, content)
            }

            else -> {
                val content = parseEnvironmentContent(envName)
                LatexNode.Environment(envName, content)
            }
        }
    }

    /**
     * 解析矩阵
     */
    private fun parseMatrix(envName: String, isSmall: Boolean = false): LatexNode.Matrix {
        val matrixType = when (envName) {
            "pmatrix" -> LatexNode.Matrix.MatrixType.PAREN
            "bmatrix" -> LatexNode.Matrix.MatrixType.BRACKET
            "Bmatrix" -> LatexNode.Matrix.MatrixType.BRACE
            "vmatrix" -> LatexNode.Matrix.MatrixType.VBAR
            "Vmatrix" -> LatexNode.Matrix.MatrixType.DOUBLE_VBAR
            else -> LatexNode.Matrix.MatrixType.PLAIN
        }

        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!isEOF()) {
            when (val token = peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == envName || (isSmall && token.name == "smallmatrix")) {
                        // 添加最后一个单元格和行
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        advance()
                        break
                    }
                }

                is LatexToken.Ampersand -> {
                    // 列分隔符
                    currentRow.add(LatexNode.Group(currentCell))
                    currentCell = mutableListOf()
                    advance()
                }

                is LatexToken.NewLine -> {
                    // 行分隔符
                    if (currentCell.isNotEmpty()) {
                        currentRow.add(LatexNode.Group(currentCell))
                    }
                    if (currentRow.isNotEmpty()) {
                        rows.add(currentRow)
                    }
                    currentRow = mutableListOf()
                    currentCell = mutableListOf()
                    advance()
                }

                else -> {
                    val node = parseExpression()
                    if (node != null) {
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Matrix(rows, matrixType, isSmall)
    }

    /**
     * 解析数组环境（array）
     */
    private fun parseArray(): LatexNode.Array {
        // array 环境需要指定列对齐方式，如 {ccc} 或 {rcl}
        val alignment = if (peek() is LatexToken.LeftBrace) {
            advance() // consume {
            val alignText = StringBuilder()
            while (!isEOF() && peek() !is LatexToken.RightBrace) {
                when (val token = peek()) {
                    is LatexToken.Text -> alignText.append(token.content)
                    else -> break
                }
                advance()
            }
            if (peek() is LatexToken.RightBrace) {
                advance() // consume }
            }
            alignText.toString()
        } else {
            "c" // 默认居中对齐
        }

        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!isEOF()) {
            when (val token = peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == "array") {
                        // 保存最后一个单元格和行
                        if (currentCell.isNotEmpty()) {
                            currentRow.add(LatexNode.Group(currentCell))
                        }
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                        advance() // consume \end{array}
                        break
                    } else {
                        advance()
                    }
                }

                is LatexToken.Ampersand -> {
                    // 列分隔符
                    currentRow.add(LatexNode.Group(currentCell))
                    currentCell = mutableListOf()
                    advance()
                }

                is LatexToken.NewLine -> {
                    // 行分隔符
                    if (currentCell.isNotEmpty()) {
                        currentRow.add(LatexNode.Group(currentCell))
                    }
                    if (currentRow.isNotEmpty()) {
                        rows.add(currentRow)
                    }
                    currentRow = mutableListOf()
                    currentCell = mutableListOf()
                    advance()
                }

                else -> {
                    val node = parseExpression()
                    if (node != null) {
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Array(rows, alignment)
    }

    /**
     * 解析对齐环境
     */
    private fun parseAligned(): LatexNode.Aligned {
        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()
        var currentCell = mutableListOf<LatexNode>()

        while (!isEOF()) {
            when (val token = peek()) {
                is LatexToken.EndEnvironment -> {
                    if (currentCell.isNotEmpty()) {
                        currentRow.add(LatexNode.Group(currentCell))
                    }
                    if (currentRow.isNotEmpty()) {
                        rows.add(currentRow)
                    }
                    advance()
                    break
                }

                is LatexToken.Ampersand -> {
                    currentRow.add(LatexNode.Group(currentCell))
                    currentCell = mutableListOf()
                    advance()
                }

                is LatexToken.NewLine -> {
                    if (currentCell.isNotEmpty()) {
                        currentRow.add(LatexNode.Group(currentCell))
                    }
                    if (currentRow.isNotEmpty()) {
                        rows.add(currentRow)
                    }
                    currentRow = mutableListOf()
                    currentCell = mutableListOf()
                    advance()
                }

                else -> {
                    val node = parseExpression()
                    if (node != null) {
                        currentCell.add(node)
                    }
                }
            }
        }

        return LatexNode.Aligned(rows)
    }

    /**
     * 解析 cases 环境
     */
    private fun parseCases(): LatexNode.Cases {
        val cases = mutableListOf<Pair<LatexNode, LatexNode>>()
        var expression = mutableListOf<LatexNode>()
        var condition = mutableListOf<LatexNode>()
        var isCondition = false

        while (!isEOF()) {
            when (val token = peek()) {
                is LatexToken.EndEnvironment -> {
                    if (token.name == "cases") {
                        if (expression.isNotEmpty()) {
                            cases.add(
                                LatexNode.Group(expression) to LatexNode.Group(condition)
                            )
                        }
                        advance()
                        break
                    }
                }

                is LatexToken.Ampersand -> {
                    isCondition = true
                    advance()
                }

                is LatexToken.NewLine -> {
                    if (expression.isNotEmpty()) {
                        cases.add(
                            LatexNode.Group(expression) to LatexNode.Group(condition)
                        )
                    }
                    expression = mutableListOf()
                    condition = mutableListOf()
                    isCondition = false
                    advance()
                }

                else -> {
                    val node = parseExpression()
                    if (node != null) {
                        if (isCondition) {
                            condition.add(node)
                        } else {
                            expression.add(node)
                        }
                    }
                }
            }
        }

        return LatexNode.Cases(cases)
    }

    /**
     * 解析环境内容
     */
    private fun parseEnvironmentContent(envName: String): List<LatexNode> {
        val content = mutableListOf<LatexNode>()

        while (!isEOF()) {
            if (peek() is LatexToken.EndEnvironment &&
                (peek() as LatexToken.EndEnvironment).name == envName
            ) {
                advance()
                break
            }

            val node = parseExpression()
            if (node != null) {
                content.add(node)
            }
        }

        return content
    }

    /**
     * 解析分组 {...}
     */
    private fun parseGroup(): LatexNode.Group {
        expect("{")
        val children = mutableListOf<LatexNode>()

        while (!isEOF() && peek() !is LatexToken.RightBrace) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        expect("}")
        return LatexNode.Group(children)
    }

    /**
     * 解析命令参数 {...}
     */
    private fun parseArgument(): LatexNode? {
        return when (peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> {
                // 单个字符或命令作为参数
                parseExpression()
            }
        }
    }

    /**
     * 解析上下标内容
     */
    private fun parseScriptContent(): LatexNode {
        return when (peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> parseExpression() ?: LatexNode.Text("")
        }
    }

    /**
     * 解析直到满足条件
     */
    private fun parseUntil(condition: (LatexToken) -> Boolean): List<LatexNode> {
        val nodes = mutableListOf<LatexNode>()

        while (!isEOF()) {
            val token = peek()
            if (token != null && condition(token)) {
                break
            }

            val node = parseExpression()
            if (node != null) {
                nodes.add(node)
            }
        }

        return nodes
    }

    class ParseException(message: String) : Exception(message)
}
