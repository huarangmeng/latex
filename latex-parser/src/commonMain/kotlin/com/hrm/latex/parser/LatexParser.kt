package com.hrm.latex.parser

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.component.EnvironmentParser
import com.hrm.latex.parser.component.ChemicalParser
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer

/**
 * LaTeX 语法解析器
 *
 * Refactored to use component-based architecture:
 * - LatexTokenStream: Token management
 * - EnvironmentParser: Environment logic
 * - CommandParser: Command logic
 */
class LatexParser : LatexParserContext {

    override lateinit var tokenStream: LatexTokenStream
    override val customCommands: MutableMap<String, com.hrm.latex.parser.component.CustomCommand> = mutableMapOf()
    private lateinit var environmentParser: EnvironmentParser
    private lateinit var commandParser: CommandParser

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
        val tokens = tokenizer.tokenize()

        // 初始化组件
        tokenStream = LatexTokenStream(tokens)
        environmentParser = EnvironmentParser(this)
        val chemicalParser = ChemicalParser(this)
        commandParser = CommandParser(this, chemicalParser)

        // 语法分析
        val children = mutableListOf<LatexNode>()
        while (!tokenStream.isEOF()) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        val document = LatexNode.Document(children)
        HLog.d(TAG, "解析成功，生成 ${children.size} 个节点")
        return document
    }

    /**
     * 解析表达式（处理上标下标）
     */
    override fun parseExpression(): LatexNode? {
        var node = parseFactor() ?: return null

        while (true) {
            val token = tokenStream.peek()
            if (token is LatexToken.Superscript) {
                tokenStream.advance()
                val exponent = parseScriptContent()
                node = LatexNode.Superscript(node, exponent)
            } else if (token is LatexToken.Subscript) {
                tokenStream.advance()
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
    override fun parseFactor(): LatexNode? {
        when (val token = tokenStream.peek()) {
            is LatexToken.Text -> {
                tokenStream.advance()
                return LatexNode.Text(token.content)
            }

            is LatexToken.Command -> {
                tokenStream.advance() // Consume command token
                return commandParser.parseCommand(token.name)
            }

            is LatexToken.BeginEnvironment -> {
                return environmentParser.parseEnvironment()
            }

            is LatexToken.LeftBrace -> {
                return parseGroup()
            }

            is LatexToken.Superscript, is LatexToken.Subscript -> {
                // 如果直接遇到上标下标，说明没有 Base，可能是语法错误
                // 这里我们跳过它以避免死循环，并返回 null
                tokenStream.advance()
                return null
            }

            is LatexToken.Whitespace -> {
                tokenStream.advance()
                return LatexNode.Space(LatexNode.Space.SpaceType.NORMAL)
            }

            is LatexToken.NewLine -> {
                tokenStream.advance()
                return LatexNode.NewLine
            }

            is LatexToken.LeftBracket -> {
                tokenStream.advance()
                return LatexNode.Text("[")
            }

            is LatexToken.RightBracket -> {
                tokenStream.advance()
                return LatexNode.Text("]")
            }

            is LatexToken.Ampersand -> {
                tokenStream.advance()
                return LatexNode.Text("&")
            }

            is LatexToken.EOF -> return null
            else -> {
                tokenStream.advance()
                return null
            }
        }
    }

    /**
     * 解析分组 {...}
     */
    override fun parseGroup(): LatexNode.Group {
        tokenStream.expect("{")
        val children = mutableListOf<LatexNode>()

        while (!tokenStream.isEOF() && tokenStream.peek() !is LatexToken.RightBrace) {
            val node = parseExpression()
            if (node != null) {
                children.add(node)
            }
        }

        if (!tokenStream.isEOF()) {
            tokenStream.expect("}")
        }
        return LatexNode.Group(children)
    }

    /**
     * 解析命令参数 {...}
     */
    override fun parseArgument(): LatexNode? {
        return when (tokenStream.peek()) {
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
        return when (tokenStream.peek()) {
            is LatexToken.LeftBrace -> parseGroup()
            else -> parseExpression() ?: LatexNode.Text("")
        }
    }

    // Internal exception class mostly for backward compatibility or internal usage
    class ParseException(message: String) : Exception(message)
}
