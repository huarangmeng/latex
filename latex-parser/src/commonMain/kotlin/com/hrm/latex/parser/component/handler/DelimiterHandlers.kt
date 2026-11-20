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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 定界符命令：\left...\right, \big, \Big, \bigg, \Bigg 等
 */
internal fun CommandRegistry.installDelimiterHandlers() {
    fun LatexTokenStream.consumeDelimiterToken(): LatexToken? {
        while (peek() is LatexToken.Whitespace) {
            advance()
        }
        return if (!isEOF()) advance() else null
    }

    register("llbracket", "rrbracket") { name, _, _ ->
        LatexNode.ManualSizedDelimiter(ParseUtils.delimiterFromName(name), 1f)
    }

    register("Set", "set") { command, ctx, _ ->
        val argument = ctx.parseArgument() ?: LatexNode.Text("")
        val content = (argument as? LatexNode.Group)?.children ?: listOf(argument)
        val stretch = command == "Set"
        fun isBar(node: LatexNode): Boolean = node is LatexNode.Text && node.content == "|"
        val separator = content.indexOfFirst { isBar(it) || (stretch && it is LatexNode.Symbol && it.symbol == "|") }
        val normalized = if (separator < 0) content else {
            val explicitDouble = content[separator] is LatexNode.Symbol
            val adjacentBars = stretch && !explicitDouble && content.getOrNull(separator + 1)?.let(::isBar) == true
            val range = content[separator].sourceRange
            // 仅替换首个分隔符，使条件里的绝对值和范数保留原义。
            val middle = if (stretch) {
                LatexNode.ManualSizedDelimiter(if (explicitDouble || adjacentBars) "‖" else "|", 1.8f, true, range)
            } else LatexNode.Symbol("mid", "∣", range)
            content.take(separator) + middle + content.drop(separator + if (adjacentBars) 2 else 1)
        }
        if (stretch) LatexNode.Delimited("{", "}", normalized)
        else LatexNode.Group(listOf(LatexNode.Symbol("lbrace", "{")) + normalized + LatexNode.Symbol("rbrace", "}"))
    }

    // \left...\right 自动伸缩
    register("left") { _, ctx, stream ->
        val leftToken = stream.consumeDelimiterToken()
        val left = when (leftToken) {
            is LatexToken.Text -> if (leftToken.content == ".") "" else leftToken.content
            is LatexToken.LeftBrace -> "{"
            is LatexToken.LeftBracket -> "["
            is LatexToken.Command -> if (leftToken.name == "|") "‖" else ParseUtils.delimiterFromName(leftToken.name)
            else -> "("
        }

        val content = mutableListOf<LatexNode>()
        while (!stream.isEOF()) {
            if (stream.peek() is LatexToken.Command && (stream.peek() as LatexToken.Command).name == "right") {
                break
            }
            val node = ctx.parseExpression()
            if (node != null) {
                content.add(node)
            }
        }

        if (stream.peek() is LatexToken.Command && (stream.peek() as LatexToken.Command).name == "right") {
            stream.advance()
        }

        val rightToken = stream.consumeDelimiterToken()
        val right = when (rightToken) {
            null -> ")"
            is LatexToken.Text -> if (rightToken.content == ".") "" else rightToken.content
            is LatexToken.RightBrace -> "}"
            is LatexToken.RightBracket -> "]"
            is LatexToken.Command -> if (rightToken.name == "|") "‖" else ParseUtils.delimiterFromName(rightToken.name)
            else -> ")"
        }

        LatexNode.Delimited(left, right, content)
    }

    // A middle delimiter participates in a surrounding \left...\right group.
    // It is represented by a large manual delimiter so it remains visible even
    // when parsed outside such a group; the renderer uses the Size2 glyph.
    register("middle") { _, _, stream ->
        val token = stream.consumeDelimiterToken()
        val delimiter = when (token) {
            is LatexToken.Text -> ParseUtils.delimiterFromName(token.content)
            is LatexToken.LeftBrace -> "{"
            is LatexToken.RightBrace -> "}"
            is LatexToken.LeftBracket -> "["
            is LatexToken.RightBracket -> "]"
            is LatexToken.Command -> ParseUtils.delimiterFromName(token.name)
            else -> "|"
        }
        LatexNode.ManualSizedDelimiter(delimiter, 1.8f, isMiddle = true)
    }

    // 手动大小控制
    val manualSizeHandler = CommandHandler { cmdName, _, stream ->
        val baseSizeCmd = when {
            cmdName.endsWith("l") || cmdName.endsWith("r") || cmdName.endsWith("m") ->
                cmdName.dropLast(1)
            else -> cmdName
        }

        val delimiterToken = stream.consumeDelimiterToken()
        val delimiter = when (delimiterToken) {
            is LatexToken.Text -> delimiterToken.content
            is LatexToken.LeftBrace -> "{"
            is LatexToken.RightBrace -> "}"
            is LatexToken.LeftBracket -> "["
            is LatexToken.RightBracket -> "]"
            is LatexToken.Command -> if (delimiterToken.name == "|") "‖" else ParseUtils.delimiterFromName(delimiterToken.name)
            else -> "("
        }

        val scaleFactor = when (baseSizeCmd) {
            "big", "bigg" -> if (baseSizeCmd == "bigg") 2.4f else 1.2f
            "Big", "Bigg" -> if (baseSizeCmd == "Bigg") 3.0f else 1.8f
            else -> 1.0f
        }

        LatexNode.ManualSizedDelimiter(delimiter, scaleFactor)
    }

    register(
        "big", "Big", "bigg", "Bigg",
        "bigl", "Bigl", "biggl", "Biggl",
        "bigr", "Bigr", "biggr", "Biggr",
        "bigm", "Bigm", "biggm", "Biggm",
        handler = manualSizeHandler
    )
}
