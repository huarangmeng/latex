package com.hrm.latex.parser.component

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.SymbolMap
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

class CommandParser(
    private val context: LatexParserContext,
    private val chemicalParser: ChemicalParser
) {
    private val tokenStream get() = context.tokenStream

    companion object {
        private const val TAG = "CommandParser"
    }

    /**
     * 解析命令
     */
    fun parseCommand(cmdName: String): LatexNode? {
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

            // 括号（自动伸缩）
            "left" -> parseDelimited()
            
            // 括号（手动大小控制）
            "big", "Big", "bigg", "Bigg",
            "bigl", "Bigl", "biggl", "Biggl",
            "bigr", "Bigr", "biggr", "Biggr",
            "bigm", "Bigm", "biggm", "Biggm" -> parseManualSizedDelimiter(cmdName)

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

            // 数学模式切换
            "displaystyle" -> parseMathStyle(LatexNode.MathStyle.MathStyleType.DISPLAY)
            "textstyle" -> parseMathStyle(LatexNode.MathStyle.MathStyleType.TEXT)
            "scriptstyle" -> parseMathStyle(LatexNode.MathStyle.MathStyleType.SCRIPT)
            "scriptscriptstyle" -> parseMathStyle(LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT)

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
            "widehat" -> parseAccent(LatexNode.Accent.AccentType.WIDEHAT)
            "overrightarrow" -> parseAccent(LatexNode.Accent.AccentType.OVERRIGHTARROW)
            "overleftarrow" -> parseAccent(LatexNode.Accent.AccentType.OVERLEFTARROW)
            "cancel" -> parseAccent(LatexNode.Accent.AccentType.CANCEL)

            // 可扩展箭头
            "xrightarrow" -> parseExtensibleArrow(LatexNode.ExtensibleArrow.Direction.RIGHT)
            "xleftarrow" -> parseExtensibleArrow(LatexNode.ExtensibleArrow.Direction.LEFT)
            "xleftrightarrow" -> parseExtensibleArrow(LatexNode.ExtensibleArrow.Direction.BOTH)

            // 堆叠
            "overset" -> parseStack(hasAbove = true, hasBelow = false)
            "underset" -> parseStack(hasAbove = false, hasBelow = true)
            "stackrel" -> parseStack(hasAbove = true, hasBelow = false)

            // 颜色
            "color" -> parseColor()
            "textcolor" -> parseTextColor()

            // 空格
            "," -> LatexNode.Space(LatexNode.Space.SpaceType.THIN)
            ":" -> LatexNode.Space(LatexNode.Space.SpaceType.MEDIUM)
            ";" -> LatexNode.Space(LatexNode.Space.SpaceType.THICK)
            "quad" -> LatexNode.Space(LatexNode.Space.SpaceType.QUAD)
            "qquad" -> LatexNode.Space(LatexNode.Space.SpaceType.QQUAD)
            "!" -> LatexNode.Space(LatexNode.Space.SpaceType.NEGATIVE_THIN)
            "hspace" -> parseHSpace()

            // 化学公式
            "ce", "cf" -> chemicalParser.parseChemicalArgument()

            // 特殊符号
            else -> parseSymbolOrGenericCommand(cmdName)
        }
    }

    private fun parseHSpace(): LatexNode {
        val arg = context.parseArgument()
        val dimension = when (arg) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> extractText(arg.children)
            else -> "0pt"
        }
        return LatexNode.HSpace(dimension)
    }

    private fun parseFraction(): LatexNode.Fraction {
        val numerator = context.parseArgument() ?: LatexNode.Text("")
        val denominator = context.parseArgument() ?: LatexNode.Text("")
        return LatexNode.Fraction(numerator, denominator)
    }

    private fun parseRoot(): LatexNode.Root {
        val index = if (tokenStream.peek() is LatexToken.LeftBracket) {
            tokenStream.advance() // [
            val indexNode = parseUntil { it is LatexToken.RightBracket }
            if (!tokenStream.isEOF()) {
                tokenStream.expect("]")
            }
            LatexNode.Group(indexNode)
        } else {
            null
        }

        val content = context.parseArgument() ?: LatexNode.Text("")
        return LatexNode.Root(content, index)
    }

    private fun parseBinomial(style: LatexNode.Binomial.BinomialStyle): LatexNode.Binomial {
        val top = context.parseArgument() ?: LatexNode.Text("")
        val bottom = context.parseArgument() ?: LatexNode.Text("")
        return LatexNode.Binomial(top, bottom, style)
    }

    private fun parseTextMode(): LatexNode.TextMode {
        val content = context.parseArgument()
        val text = when (content) {
            is LatexNode.Text -> content.content
            is LatexNode.Group -> extractText(content.children)
            else -> ""
        }
        return LatexNode.TextMode(text)
    }

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

    private fun parseBigOperator(operator: String): LatexNode {
        var subscript: LatexNode? = null
        var superscript: LatexNode? = null

        if (tokenStream.peek() is LatexToken.Subscript) {
            tokenStream.advance()
            subscript = parseScriptContent()
        }

        if (tokenStream.peek() is LatexToken.Superscript) {
            tokenStream.advance()
            superscript = parseScriptContent()
        }

        if (subscript == null && tokenStream.peek() is LatexToken.Subscript) {
            tokenStream.advance()
            subscript = parseScriptContent()
        }

        return LatexNode.BigOperator(operator, subscript, superscript)
    }

    private fun parseScriptContent(): LatexNode {
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> context.parseGroup()
            else -> context.parseExpression() ?: LatexNode.Text("")
        }
    }

    private fun parseDelimited(): LatexNode.Delimited {
        val leftToken = tokenStream.advance()
        val left = when (leftToken) {
            is LatexToken.Text -> if (leftToken.content == ".") "" else leftToken.content
            is LatexToken.LeftBrace -> "{"
            is LatexToken.LeftBracket -> "["
            is LatexToken.Command -> when (leftToken.name) {
                "langle" -> "⟨"
                "lfloor" -> "⌊"
                "lceil" -> "⌈"
                "{" -> "{"
                "." -> ""
                else -> leftToken.name
            }
            else -> "("
        }

        val content = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF()) {
            if (tokenStream.peek() is LatexToken.Command && (tokenStream.peek() as LatexToken.Command).name == "right") {
                break
            }
            val node = context.parseExpression()
            if (node != null) {
                content.add(node)
            }
        }

        if (tokenStream.peek() is LatexToken.Command && (tokenStream.peek() as LatexToken.Command).name == "right") {
            tokenStream.advance()
        }

        val rightToken = if (!tokenStream.isEOF()) tokenStream.advance() else null
        val right = when (rightToken) {
            null -> ")"
            is LatexToken.Text -> if (rightToken.content == ".") "" else rightToken.content
            is LatexToken.RightBrace -> "}"
            is LatexToken.RightBracket -> "]"
            is LatexToken.Command -> when (rightToken.name) {
                "rangle" -> "⟩"
                "rfloor" -> "⌋"
                "rceil" -> "⌉"
                "}" -> "}"
                "." -> ""
                else -> rightToken.name
            }
            else -> ")"
        }

        return LatexNode.Delimited(left, right, content, true)
    }

    private fun parseManualSizedDelimiter(sizeCmd: String): LatexNode {
        val baseSizeCmd = when {
            sizeCmd.endsWith("l") || sizeCmd.endsWith("r") || sizeCmd.endsWith("m") -> 
                sizeCmd.dropLast(1)
            else -> sizeCmd
        }
        
        val delimiterToken = if (!tokenStream.isEOF()) tokenStream.advance() else null
        val delimiter = when (delimiterToken) {
            is LatexToken.Text -> delimiterToken.content
            is LatexToken.LeftBrace -> "{"
            is LatexToken.RightBrace -> "}"
            is LatexToken.LeftBracket -> "["
            is LatexToken.RightBracket -> "]"
            is LatexToken.Command -> when (delimiterToken.name) {
                "langle" -> "⟨"
                "rangle" -> "⟩"
                "lfloor" -> "⌊"
                "rfloor" -> "⌋"
                "lceil" -> "⌈"
                "rceil" -> "⌉"
                "|" -> "|"
                "\\" -> "\\"
                "{" -> "{"
                "}" -> "}"
                else -> delimiterToken.name
            }
            else -> "("
        }
        
        val scaleFactor = when (baseSizeCmd) {
            "big", "bigg" -> if (baseSizeCmd == "bigg") 2.4f else 1.2f
            "Big", "Bigg" -> if (baseSizeCmd == "Bigg") 3.0f else 1.8f
            else -> 1.0f
        }
        
        return LatexNode.ManualSizedDelimiter(delimiter, scaleFactor)
    }

    private fun parseStyle(styleType: LatexNode.Style.StyleType): LatexNode.Style {
        val content = context.parseArgument()
        return LatexNode.Style(
            if (content != null) listOf(content) else emptyList(),
            styleType
        )
    }

    /**
     * 解析数学模式切换命令
     * 
     * 注意：\displaystyle 等命令不需要参数，它们影响后续所有内容直到组结束
     * 例如：{\displaystyle x + y} 或 \frac{\displaystyle a}{b}
     */
    private fun parseMathStyle(mathStyleType: LatexNode.MathStyle.MathStyleType): LatexNode.MathStyle {
        // 数学模式命令后面可能紧跟一个参数组，或者影响当前组的剩余内容
        val content = context.parseArgument()
        return LatexNode.MathStyle(
            if (content != null) listOf(content) else emptyList(),
            mathStyleType
        )
    }

    private fun parseAccent(accentType: LatexNode.Accent.AccentType): LatexNode.Accent {
        val content = context.parseArgument() ?: LatexNode.Text("")
        return LatexNode.Accent(content, accentType)
    }

    /**
     * 解析可扩展箭头命令
     * 
     * 语法: \xrightarrow[下方文字]{上方文字}
     * 例如: \xrightarrow{f}  或  \xrightarrow[下]{上}
     */
    private fun parseExtensibleArrow(direction: LatexNode.ExtensibleArrow.Direction): LatexNode.ExtensibleArrow {
        // 检查是否有可选参数（下方文字）
        val below = if (context.tokenStream.peek() is LatexToken.LeftBracket) {
            context.tokenStream.advance() // 消费 [
            val nodes = parseUntil { it is LatexToken.RightBracket }
            context.tokenStream.advance() // 消费 ]
            if (nodes.isEmpty()) null else LatexNode.Group(nodes)
        } else {
            null
        }
        
        // 解析必选参数（上方文字）
        val above = context.parseArgument() ?: LatexNode.Text("")
        
        return LatexNode.ExtensibleArrow(above, below, direction)
    }

    /**
     * 解析堆叠命令
     * 
     * 语法:
     * - \overset{上方内容}{基础内容}
     * - \underset{下方内容}{基础内容}
     * - \stackrel{上方内容}{基础内容}（与 overset 等效）
     * 
     * 例如: \overset{?}{=}  或  \underset{n \to \infty}{=}
     */
    private fun parseStack(hasAbove: Boolean, hasBelow: Boolean): LatexNode.Stack {
        // 第一个参数是上方或下方内容
        val firstArg = context.parseArgument() ?: LatexNode.Text("")
        
        // 第二个参数是基础内容
        val base = context.parseArgument() ?: LatexNode.Text("")
        
        return if (hasAbove) {
            LatexNode.Stack(base = base, above = firstArg, below = null)
        } else {
            LatexNode.Stack(base = base, above = null, below = firstArg)
        }
    }

    /**
     * 解析 \color{颜色名}{内容} 命令
     * 
     * 语法: \color{red}{文本}
     * 例如: \color{blue}{蓝色文字}
     */
    private fun parseColor(): LatexNode {
        // 解析颜色名称参数
        val colorArg = context.parseArgument() ?: return LatexNode.Text("")
        val colorName = extractColorName(colorArg)
        
        // 解析内容参数
        val contentArg = context.parseArgument() ?: return LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }
        
        return LatexNode.Color(content, colorName)
    }

    /**
     * 解析 \textcolor{颜色名}{内容} 命令
     * 
     * 语法: \textcolor{red}{文本}
     * 例如: \textcolor{green}{绿色文字}
     */
    private fun parseTextColor(): LatexNode {
        // \textcolor 和 \color 语法相同，都是两个参数
        return parseColor()
    }

    /**
     * 从节点中提取颜色名称字符串
     */
    private fun extractColorName(node: LatexNode): String {
        return when (node) {
            is LatexNode.Text -> node.content
            is LatexNode.Group -> extractText(node.children)
            else -> "black"
        }
    }

    private fun parseSymbolOrGenericCommand(cmdName: String): LatexNode {
        val unicode = SymbolMap.getSymbol(cmdName)
        if (unicode != null) {
            return LatexNode.Symbol(cmdName, unicode)
        }

        val arguments = mutableListOf<LatexNode>()
        while (tokenStream.peek() is LatexToken.LeftBrace) {
            val arg = context.parseArgument()
            if (arg != null) {
                arguments.add(arg)
            } else {
                break
            }
        }

        return LatexNode.Command(cmdName, arguments)
    }
    
    private fun parseUntil(condition: (LatexToken) -> Boolean): List<LatexNode> {
        val nodes = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF()) {
            val token = tokenStream.peek()
            if (token != null && condition(token)) {
                break
            }
            val node = context.parseExpression()
            if (node != null) {
                nodes.add(node)
            }
        }
        return nodes
    }
}
