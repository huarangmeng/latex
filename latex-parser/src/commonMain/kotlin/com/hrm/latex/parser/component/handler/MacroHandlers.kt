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

import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.component.CustomCommand
import com.hrm.latex.parser.component.CustomEnvironment
import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

/** Definitions read raw tokens; no command in a replacement body executes at definition time. */
internal fun CommandRegistry.installMacroHandlers() {
    register("newcommand", "renewcommand", "providecommand") { kind, ctx, stream -> stream.raw {
        consumeStar(stream)
        val name = readCommandName(stream) ?: return@raw LatexNode.Text("")
        val countTokens = stream.readOptionalTokens()
        val count = if (countTokens == null) 0 else countTokens.text().toIntOrNull() ?: -1
        val default = if (count > 0) stream.readOptionalTokens() else null
        val body = stream.readArgumentTokens()
        if (count !in 0..9) return@raw invalid(ctx, stream, "Macro argument count must be between 0 and 9")
        if (kind == "providecommand" && ctx.isCommandDefined(name)) return@raw LatexNode.Text("")
        ctx.defineCommand(CustomCommand(name, count, body, default), takeGlobal(ctx))
        LatexNode.NewCommand(name, count, body, default?.text())
    } }

    register("def", "gdef", "edef", "xdef") { kind, ctx, stream -> stream.raw {
        stream.skipWhitespace()
        val name = (stream.readAtom() as? LatexToken.Command)?.name
            ?: return@raw invalid(ctx, stream, "Expected a command after \\$kind")
        val parameters = mutableListOf<LatexToken>()
        while (!stream.isEOF() && stream.peek() !is LatexToken.LeftBrace) {
            parameters.add(stream.readAtom() ?: break)
        }
        val numbers = parameters.windowed(2).mapNotNull {
            if ((it[0] as? LatexToken.Text)?.content == "#") (it[1] as? LatexToken.Text)?.content?.toIntOrNull() else null
        }
        if (numbers != (1..numbers.size).toList() || numbers.size > 9) return@raw invalid(ctx, stream, "Macro parameters must be numbered consecutively")
        var body = stream.readArgumentTokens()
        if (kind == "edef" || kind == "xdef") body = ctx.expandTokens(body)
        val global = takeGlobal(ctx) || kind == "gdef" || kind == "xdef"
        ctx.defineCommand(CustomCommand(name, numbers.size, body, parameterText = parameters), global)
        LatexNode.NewCommand(name, numbers.size, body)
    } }

    register("let") { _, ctx, stream -> stream.raw {
        stream.skipWhitespace()
        val name = (stream.readAtom() as? LatexToken.Command)?.name
            ?: return@raw invalid(ctx, stream, "Expected a command after \\let")
        stream.skipWhitespace()
        if ((stream.peek() as? LatexToken.Text)?.content == "=") { stream.advance(); stream.skipWhitespace() }
        val value = stream.readAtom() ?: return@raw invalid(ctx, stream, "Missing \\let value")
        val existing = (value as? LatexToken.Command)?.let { ctx.customCommands[it.name] }
        val definition = existing?.copy(name = name) ?: CustomCommand(name, 0, listOf(
            if (value is LatexToken.Command) value.copy(builtin = true) else value
        ), isAlias = true)
        ctx.defineCommand(definition, takeGlobal(ctx))
        LatexNode.NewCommand(name, definition.numArgs, definition.tokens)
    } }
    register("global") { _, ctx, stream ->
        ctx.globalAssignment = true
        try {
            val assignment = ctx.parseFactor() ?: LatexNode.Text("")
            if (ctx.globalAssignment) invalid(ctx, stream, "Expected an assignment after \\global")
            assignment
        } finally { ctx.globalAssignment = false }
    }
    register("relax") { _, _, _ -> LatexNode.Text("") }

    register("DeclareMathOperator") { _, ctx, stream -> stream.raw {
        val starred = consumeStar(stream)
        val name = readCommandName(stream) ?: return@raw invalid(ctx, stream, "Missing operator command")
        val body = stream.readArgumentTokens()
        val replacement = listOf(LatexToken.Command("operatorname")) +
            (if (starred) listOf(LatexToken.Text("*")) else emptyList()) +
            LatexToken.LeftBrace() + body + LatexToken.RightBrace()
        ctx.defineCommand(CustomCommand(name, 0, replacement), takeGlobal(ctx))
        LatexNode.NewCommand(name, 0, replacement)
    } }

    register("DeclarePairedDelimiter") { _, ctx, stream -> stream.raw {
        val name = readCommandName(stream) ?: return@raw invalid(ctx, stream, "Missing delimiter command")
        val left = stream.readArgumentTokens()
        val right = stream.readArgumentTokens()
        val body = listOf(LatexToken.Command("left")) + left + listOf(LatexToken.Text("#"), LatexToken.Text("1"), LatexToken.Command("right")) + right
        ctx.defineCommand(CustomCommand(name, 1, body, acceptsDelimiterModifier = true), takeGlobal(ctx))
        LatexNode.NewCommand(name, 1, body)
    } }

    register("newenvironment", "renewenvironment") { _, ctx, stream -> stream.raw {
        consumeStar(stream)
        val name = stream.readArgumentTokens().text().trim()
        val countTokens = stream.readOptionalTokens()
        val count = if (countTokens == null) 0 else countTokens.text().toIntOrNull() ?: -1
        val default = if (count > 0) stream.readOptionalTokens() else null
        val begin = stream.readArgumentTokens()
        val end = stream.readArgumentTokens()
        if (count !in 0..9) return@raw invalid(ctx, stream, "Environment argument count must be between 0 and 9")
        ctx.defineEnvironment(CustomEnvironment(name, count, begin, end, default))
        LatexNode.NewEnvironment(name, count, begin, end, default?.text())
    } }
}

private fun consumeStar(stream: LatexTokenStream): Boolean {
    stream.skipWhitespace()
    val star = (stream.peek() as? LatexToken.Text)?.content?.startsWith("*") == true
    if (star) stream.readAtom()
    return star
}

private fun readCommandName(stream: LatexTokenStream): String? =
    (stream.readArgumentTokens().firstOrNull { it !is LatexToken.Whitespace } as? LatexToken.Command)?.name

private fun takeGlobal(ctx: LatexParserContext): Boolean = ctx.globalAssignment.also { ctx.globalAssignment = false }

private fun invalid(ctx: LatexParserContext, stream: LatexTokenStream, message: String): LatexNode {
    ctx.diagnostics.add(ParseDiagnostic(stream.peek(-1)?.range ?: SourceRange.EMPTY, message,
        ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.MACRO_ERROR))
    return LatexNode.Text("")
}

internal fun List<LatexToken>.text(): String = joinToString("") {
    when (it) {
        is LatexToken.Text -> it.content
        is LatexToken.Whitespace -> " "
        is LatexToken.Command -> "\\${it.name}"
        is LatexToken.LeftBrace -> "{"
        is LatexToken.RightBrace -> "}"
        is LatexToken.LeftBracket -> "["
        is LatexToken.RightBracket -> "]"
        else -> ""
    }
}
