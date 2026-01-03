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

    private fun parseAccent(accentType: LatexNode.Accent.AccentType): LatexNode.Accent {
        val content = context.parseArgument() ?: LatexNode.Text("")
        return LatexNode.Accent(content, accentType)
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
