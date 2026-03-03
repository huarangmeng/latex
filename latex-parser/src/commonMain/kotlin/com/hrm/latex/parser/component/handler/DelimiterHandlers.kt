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

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 定界符命令：\left...\right, \big, \Big, \bigg, \Bigg 等
 */
internal fun CommandRegistry.installDelimiterHandlers() {
    // \left...\right 自动伸缩
    register("left") { _, ctx, stream ->
        val leftToken = stream.advance()
        val left = when (leftToken) {
            is LatexToken.Text -> if (leftToken.content == ".") "" else leftToken.content
            is LatexToken.LeftBrace -> "{"
            is LatexToken.LeftBracket -> "["
            is LatexToken.Command -> when (leftToken.name) {
                "langle" -> "⟨"
                "lfloor" -> "⌊"
                "lceil" -> "⌈"
                "lvert" -> "|"
                "lVert" -> "‖"
                "{" -> "{"
                "." -> ""
                else -> leftToken.name
            }
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

        val rightToken = if (!stream.isEOF()) stream.advance() else null
        val right = when (rightToken) {
            null -> ")"
            is LatexToken.Text -> if (rightToken.content == ".") "" else rightToken.content
            is LatexToken.RightBrace -> "}"
            is LatexToken.RightBracket -> "]"
            is LatexToken.Command -> when (rightToken.name) {
                "rangle" -> "⟩"
                "rfloor" -> "⌋"
                "rceil" -> "⌉"
                "rvert" -> "|"
                "rVert" -> "‖"
                "}" -> "}"
                "." -> ""
                else -> rightToken.name
            }
            else -> ")"
        }

        LatexNode.Delimited(left, right, content, true)
    }

    // 手动大小控制
    val manualSizeHandler = CommandHandler { cmdName, _, stream ->
        val baseSizeCmd = when {
            cmdName.endsWith("l") || cmdName.endsWith("r") || cmdName.endsWith("m") ->
                cmdName.dropLast(1)
            else -> cmdName
        }

        val delimiterToken = if (!stream.isEOF()) stream.advance() else null
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
                "lvert", "rvert" -> "|"
                "lVert", "rVert" -> "‖"
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
