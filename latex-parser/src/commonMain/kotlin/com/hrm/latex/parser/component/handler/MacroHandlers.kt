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

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.component.CustomCommand
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

private const val TAG = "MacroHandlers"

/**
 * 宏定义命令：\newcommand, \renewcommand, \def, \DeclareMathOperator
 */
internal fun CommandRegistry.installMacroHandlers() {
    // \newcommand, \renewcommand
    val newCommandHandler = CommandHandler { _, ctx, stream ->
        val nameArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val commandName = ParseUtils.extractCommandName(nameArg)

        // 可选参数 [numArgs]
        var numArgs = 0
        if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance() // [
            val numNodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
            if (!stream.isEOF()) {
                stream.expect("]")
            }
            numArgs = ParseUtils.extractText(numNodes).toIntOrNull() ?: 0
        }

        // 定义 {definition}
        val defArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val definition = when (defArg) {
            is LatexNode.Group -> defArg.children
            else -> listOf(defArg)
        }

        ctx.customCommands[commandName] = CustomCommand(commandName, numArgs, definition)
        HLog.d(TAG, "注册自定义命令: \\$commandName[$numArgs]")
        LatexNode.NewCommand(commandName, numArgs, definition)
    }

    register("newcommand", "renewcommand", handler = newCommandHandler)

    // \def
    register("def") { _, ctx, stream ->
        val nameToken = if (!stream.isEOF()) stream.advance() else null
        val commandName = when (nameToken) {
            is LatexToken.Command -> nameToken.name
            is LatexToken.Text -> nameToken.content.removePrefix("\\")
            else -> return@register LatexNode.Text("\\def")
        }

        // 计算参数个数
        var numArgs = 0
        while (!stream.isEOF()) {
            val token = stream.peek()
            if (token is LatexToken.Text && token.content.startsWith("#")) {
                stream.advance()
                val argNum = token.content.removePrefix("#").toIntOrNull()
                if (argNum != null && argNum > numArgs) numArgs = argNum
            } else {
                break
            }
        }

        // 解析定义 {body}
        val defArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\def")
        val definition = when (defArg) {
            is LatexNode.Group -> defArg.children
            else -> listOf(defArg)
        }

        ctx.customCommands[commandName] = CustomCommand(commandName, numArgs, definition)
        HLog.d(TAG, "注册自定义命令 (def): \\$commandName[$numArgs]")
        LatexNode.NewCommand(commandName, numArgs, definition)
    }

    // \DeclareMathOperator
    register("DeclareMathOperator") { _, ctx, _ ->
        val nameArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val commandName = ParseUtils.extractCommandName(nameArg)

        val opArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val operatorName = when (opArg) {
            is LatexNode.Text -> opArg.content
            is LatexNode.Group -> ParseUtils.extractText(opArg.children)
            else -> ""
        }

        val definition = listOf(LatexNode.OperatorName(operatorName))
        ctx.customCommands[commandName] = CustomCommand(commandName, 0, definition)
        HLog.d(TAG, "注册运算符: \\$commandName → operatorname{$operatorName}")
        LatexNode.NewCommand(commandName, 0, definition)
    }
}
