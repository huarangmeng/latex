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


package com.hrm.latex.parser.component

import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.SymbolMap
import com.hrm.latex.parser.component.handler.*
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

internal class CommandParser(
    private val context: LatexParserContext,
    private val chemicalParser: ChemicalParser
) {
    private var expansionStream: LatexTokenStream? = null
    private val tokenStream get() = expansionStream ?: context.tokenStream
    private var expandedTokenCount = 0
    private var expansionCount = 0

    fun isBuiltin(name: String): Boolean = registry.hasHandler(name) ||
        SymbolMap.getSymbol(name) != null || name in expansionCommands || name == "ce" || name == "cf"

    fun isDefined(name: String): Boolean {
        val macro = context.customCommands[name] ?: return isBuiltin(name)
        if (!macro.isAlias) return true
        val original = macro.tokens.singleOrNull()
        return original !is LatexToken.Command || isBuiltin(original.name)
    }



    companion object {
        private val expansionCommands = setOf(
        "expandafter", "noexpand", "csname", "endcsname", "ifdefined", "ifx", "else", "fi", "mathpalette"
    )

        private const val TAG = "CommandParser"

        /**
         * 命令注册表：所有已知 LaTeX 命令的分发中心。
         * 
         * 注册表是无状态的（只存储命令名→handler 的映射），
         * 所有 CommandParser 实例共享同一份，避免重复构建。
         */
        private val registry = CommandRegistry().apply {
            installFractionHandlers()
            installRootHandlers()
            installBigOperatorHandlers()
            installDelimiterHandlers()
            installStyleHandlers()
            installMathClassHandlers()
            installAccentHandlers()
            installArrowAndStackHandlers()
            installSpaceHandlers()
            installColorHandlers()
            installSpecialEffectHandlers()
            installHyperlinkHandlers()
            installMacroHandlers()
            installGroupHandlers()
            installTableHandlers()
            installReferenceHandlers()
            installAdvancedHandlers()
            installOperatorHandlers()
            installSectionHandlers()
            installTextDirectionHandlers()
            installPackageCommandHandlers()
        }
    }

    /**
     * 解析命令
     */
    fun parseCommand(cmdName: String): LatexNode? {
        HLog.d(TAG) { "解析命令: \\$cmdName" }

        // 2. 委托给注册表分发
        val registryResult = registry.dispatch(cmdName, context, tokenStream)
        if (registryResult != null) {
            return registryResult
        }

        // 3. 化学公式（需要 chemicalParser 实例，不适合放入通用 handler）
        if (cmdName == "ce" || cmdName == "cf") {
            return chemicalParser.parseChemicalArgument()
        }

        // 4. 回退：符号查找或通用命令
        return parseSymbolOrGenericCommand(cmdName)
    }

    private fun parseSymbolOrGenericCommand(cmdName: String): LatexNode {
        if (cmdName.isEmpty()) {
            val range = tokenStream.peek(-1)?.range ?: SourceRange(0, 0)
            context.diagnostics.add(
                ParseDiagnostic(
                    range = range,
                    message = "Trailing backslash does not form a control sequence",
                    severity = ParseDiagnostic.Severity.ERROR,
                    category = ParseDiagnostic.Category.UNKNOWN_COMMAND
                )
            )
            return LatexNode.Text("\\")
        }

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


    /** Invoked by the existing stream before its current token is observed. */
    private var expansionDepth = 0
    fun expandNext(): Boolean {
        if (expansionDepth >= 256) {
            val token = tokenStream.advance() ?: return false
            macroError(token.range, "Macro expansion nesting limit exceeded")
            return true
        }
        expansionDepth++
        return try { expandCurrent() } finally { expansionDepth-- }
    }

    private fun expandCurrent(): Boolean {
        val stream = tokenStream
        var token = stream.peek() ?: return false // callback runs in raw mode
        if (token is LatexToken.BeginEnvironment) {
            val environment = context.customEnvironments[token.name] ?: return false
            stream.advance()
            if (rejectNestedExpansion(token.range)) return true
            val args = readArguments(environment.numArgs, environment.defaultTokens)
            val body = mutableListOf<LatexToken>()
            var depth = 1
            while (!stream.isEOF()) {
                val next = stream.advance() ?: break
                if (next is LatexToken.BeginEnvironment && next.name == token.name) depth++
                if (next is LatexToken.EndEnvironment && next.name == token.name) depth--
                if (depth == 0) break
                body.add(next)
            }
            if (depth != 0) macroError(token.range, "Missing \\end{${token.name}}")
            emit(listOf(LatexToken.LeftBrace(token.range)) +
                substitute(environment.beginTokens, args, token.range) + body +
                substitute(environment.endTokens, args, token.range) +
                LatexToken.RightBrace(stream.peek(-1)?.range ?: token.range), token.range)
            return true
        }
        if (token !is LatexToken.Command || !token.expandable) return false
        var macro = if (token.builtin) null else context.customCommands[token.name]
        if (macro?.isAlias == true) {
            val original = macro.tokens.singleOrNull() as? LatexToken.Command
            if (original != null && original.name in expansionCommands) {
                token = original.copy(range = token.range)
                macro = null
            }
        }
        if (macro != null) {
            stream.advance()
            if (rejectNestedExpansion(token.range)) return true
            if (macro.acceptsDelimiterModifier) {
                stream.skipWhitespace()
                if ((stream.peek() as? LatexToken.Text)?.content?.startsWith("*") == true) stream.readAtom()
                stream.readOptionalTokens()
            }
            val args = if (macro.parameterText.isEmpty()) readArguments(macro.numArgs, macro.defaultTokens)
                else readDelimitedArguments(macro.parameterText, macro.numArgs, token.range)
            emit(substitute(macro.tokens, args, stream.rangeFrom(token.range.start)), token.range)
            return true
        }
        when (token.name) {
            "noexpand" -> {
                stream.advance()
                val next = stream.readAtom()
                if (next != null) {
                    val frozen = if (next is LatexToken.Command) {
                        if (expansionStream == null && (!isBuiltin(next.name) ||
                            (!next.builtin && context.customCommands.containsKey(next.name)) || next.name in expansionCommands))
                            LatexToken.Command("relax", next.range, builtin = true)
                        else next.copy(expandable = false)
                    } else next
                    emit(listOf(frozen), token.range)
                }
            }
            "expandafter" -> {
                stream.advance()
                val first = stream.readAtom() ?: return true
                expandNext()
                emit(listOf(first), token.range)
            }
            "csname" -> {
                stream.advance()
                val name = StringBuilder()
                var closed = false
                while (!stream.isEOF()) {
                    while ((stream.peek() as? LatexToken.Command)?.name != "endcsname" && expandNext()) { }
                    val next = stream.advance() ?: break
                    if (next is LatexToken.Command && next.name == "endcsname") { closed = true; break }
                    when (next) {
                        is LatexToken.Text -> name.append(next.content)
                        is LatexToken.Whitespace -> Unit
                        else -> macroError(next.range, "Expected character in \\csname")
                    }
                }
                if (!closed) macroError(token.range, "Missing \\endcsname")
                val commandName = name.toString()
                if (!context.isCommandDefined(commandName)) {
                    context.defineCommand(CustomCommand(commandName, 0, listOf(LatexToken.Command("relax", builtin = true)), isAlias = true))
                }
                emit(listOf(LatexToken.Command(commandName, stream.rangeFrom(token.range.start))), token.range)
            }
            "ifdefined", "ifx" -> {
                stream.advance()
                stream.skipWhitespace()
                val first = stream.readAtom()
                val condition = if (token.name == "ifdefined") {
                    first != null && (first !is LatexToken.Command || context.isCommandDefined(first.name))
                } else sameMeaning(first, stream.readAtom())
                val selected = mutableListOf<LatexToken>()
                var depth = 0
                var inTrueBranch = true
                var closed = false
                while (!stream.isEOF()) {
                    val next = stream.advance() ?: break
                    if (next is LatexToken.Command) {
                        if (next.name == "ifdefined" || next.name == "ifx") depth++
                        if (next.name == "fi") {
                            if (depth == 0) { closed = true; break }
                            depth--
                        }
                        if (next.name == "else" && depth == 0) { inTrueBranch = false; continue }
                    }
                    if (condition == inTrueBranch) selected.add(next)
                }
                if (!closed) macroError(token.range, "Missing \\fi")
                emit(selected, token.range)
            }
            "mathpalette" -> {
                stream.advance()
                val function = stream.readArgumentTokens()
                val argument = stream.readArgumentTokens()
                val result = mutableListOf<LatexToken>(LatexToken.Command("mathchoice", token.range))
                for (style in listOf("displaystyle", "textstyle", "scriptstyle", "scriptscriptstyle")) {
                    result.add(LatexToken.LeftBrace(token.range))
                    result.addAll(function)
                    result.add(LatexToken.Command(style, token.range))
                    result.add(LatexToken.LeftBrace(token.range))
                    result.addAll(argument)
                    result.add(LatexToken.RightBrace(token.range))
                    result.add(LatexToken.RightBrace(token.range))
                }
                emit(result, token.range)
            }
            "else", "fi", "endcsname" -> {
                stream.advance()
                macroError(token.range, "Unexpected \\${token.name}")
            }
            else -> return false
        }
        return true
    }

    private fun rejectNestedExpansion(range: SourceRange): Boolean {
        if (context.scopeDepth < 128) return false
        macroError(range, "Macro expansion group nesting limit exceeded")
        return true
    }

    private fun emit(tokens: List<LatexToken>, range: SourceRange) {
        expansionCount++
        expandedTokenCount += tokens.size
        if (expansionCount > 10_000 || expandedTokenCount > 100_000) {
            macroError(range, "Macro expansion limit exceeded")
            // Drop only this replacement; definitions and following source remain intact.
            return
        }
        tokenStream.insert(tokens)
    }

    private fun macroError(range: SourceRange, message: String) {
        context.diagnostics.add(ParseDiagnostic(range, message, ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.MACRO_ERROR))
    }

    private fun readArguments(count: Int, default: List<LatexToken>?): List<List<LatexToken>> =
        (0 until count).map { index ->
            if (index == 0 && default != null) tokenStream.readOptionalTokens() ?: default
            else tokenStream.readArgumentTokens()
        }

    private fun readDelimitedArguments(pattern: List<LatexToken>, count: Int, range: SourceRange): List<List<LatexToken>> {
        val args = MutableList(count) { emptyList<LatexToken>() }
        var index = 0
        while (index < pattern.size) {
            if ((pattern[index] as? LatexToken.Text)?.content != "#") {
                if (!sameToken(pattern[index], tokenStream.readAtom())) macroError(range, "Macro parameter prefix mismatch")
                index++
                continue
            }
            val number = (pattern.getOrNull(index + 1) as? LatexToken.Text)?.content?.toIntOrNull()
            if (number == null || number !in 1..count) { macroError(range, "Invalid macro parameter"); break }
            index += 2
            val delimiter = pattern.drop(index).takeWhile { (it as? LatexToken.Text)?.content != "#" }
            index += delimiter.size
            if (delimiter.isEmpty()) {
                args[number - 1] = tokenStream.readArgumentTokens()
            } else {
                val argument = mutableListOf<LatexToken>()
                var depth = 0
                var found = false
                while (!tokenStream.isEOF() && !found) {
                    val token = tokenStream.advance() ?: break
                    val text = (token as? LatexToken.Text)?.takeUnless { it.literal }?.content
                    var offset = 0
                    do {
                        val next = if (text != null) {
                            val end = offset + if (text[offset].isHighSurrogate() &&
                                offset + 1 < text.length && text[offset + 1].isLowSurrogate()) 2 else 1
                            LatexToken.Text(text.substring(offset, end), token.range).also { offset = end }
                        } else token
                        argument.add(next)
                        if (next is LatexToken.LeftBrace) depth++
                        if (next is LatexToken.RightBrace) depth--
                        if (depth == 0 && argument.size >= delimiter.size &&
                            delimiter.indices.all { sameToken(argument[argument.size - delimiter.size + it], delimiter[it]) }) {
                            repeat(delimiter.size) { argument.removeAt(argument.lastIndex) }
                            if (text != null && offset < text.length) {
                                tokenStream.insert(listOf(LatexToken.Text(text.substring(offset), token.range)))
                            }
                            found = true
                        }
                    } while (!found && text != null && offset < text.length)
                }
                if (!found) macroError(range, "Missing macro argument delimiter")
                args[number - 1] = if (argument.firstOrNull() is LatexToken.LeftBrace &&
                    argument.lastOrNull() is LatexToken.RightBrace && balancedOuterGroup(argument)) argument.drop(1).dropLast(1) else argument
            }
        }
        return args
    }

    private fun balancedOuterGroup(tokens: List<LatexToken>): Boolean {
        var depth = 0
        tokens.forEachIndexed { index, token ->
            if (token is LatexToken.LeftBrace) depth++
            if (token is LatexToken.RightBrace) depth--
            if (depth == 0 && index != tokens.lastIndex) return false
        }
        return depth == 0
    }

    internal fun substitute(tokens: List<LatexToken>, args: List<List<LatexToken>>, range: SourceRange): List<LatexToken> {
        val result = mutableListOf<LatexToken>()
        var index = 0
        while (index < tokens.size) {
            val token = tokens[index++]
            if (token is LatexToken.Text && token.content == "#") {
                val next = tokens.getOrNull(index) as? LatexToken.Text
                if (next?.content == "#") { result.add(token.withRange(range)); index++; continue }
                val number = next?.content?.toIntOrNull()
                if (number != null && number in 1..args.size) {
                    result.addAll(args[number - 1].map { it.withRange(range) }); index++; continue
                }
            }
            result.add(token.withRange(range))
        }
        return result
    }

    internal fun expandTokens(tokens: List<LatexToken>): List<LatexToken> {
        val previous = expansionStream
        val stream = LatexTokenStream(tokens + LatexToken.EOF())
        expansionStream = stream
        stream.commandParser = this
        return try {
            buildList {
                while (!stream.isEOF()) {
                    val token = stream.advance() ?: break
                    add(if (token is LatexToken.Command) token.copy(expandable = true) else token)
                }
            }
        } finally { expansionStream = previous }
    }

    private fun sameToken(a: LatexToken?, b: LatexToken?): Boolean = when (a) {
        is LatexToken.Text -> b is LatexToken.Text && a.content == b.content && a.literal == b.literal
        is LatexToken.Command -> b is LatexToken.Command && a.name == b.name &&
            a.expandable == b.expandable && a.builtin == b.builtin
        else -> a?.withRange(SourceRange.EMPTY) == b?.withRange(SourceRange.EMPTY)
    }

    private fun sameMeaning(a: LatexToken?, b: LatexToken?): Boolean {
        fun meaning(token: LatexToken?): Any? {
            if (token !is LatexToken.Command) return token?.withRange(SourceRange.EMPTY)
            val macro = context.customCommands[token.name]
            if (macro?.isAlias == true) {
                val original = macro.tokens.single()
                return if (original is LatexToken.Command) {
                    if (isBuiltin(original.name)) original.copy(range = SourceRange.EMPTY, expandable = true, builtin = false) else null
                } else original.withRange(SourceRange.EMPTY)
            }
            if (macro != null) return macro.copy(
                name = "", tokens = macro.tokens.map { it.withRange(SourceRange.EMPTY) },
                defaultTokens = macro.defaultTokens?.map { it.withRange(SourceRange.EMPTY) },
                parameterText = macro.parameterText.map { it.withRange(SourceRange.EMPTY) }
            )
            return if (isBuiltin(token.name)) token.copy(range = SourceRange.EMPTY, expandable = true, builtin = false) else null
        }
        return meaning(a) == meaning(b)
    }
}
