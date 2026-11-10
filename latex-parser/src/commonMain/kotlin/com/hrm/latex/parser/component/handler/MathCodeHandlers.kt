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
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

internal const val ACTIVE_MATH_CODE = 0x8000

internal fun activeMathName(character: Char): String = "math-active:" + character.code

internal fun LatexToken.mathCharacter(): Char? = when (this) {
    is LatexToken.Text -> if (literal) null else content.firstOrNull()
    is LatexToken.Whitespace -> '~'.takeIf { content == "\u00a0" }
    is LatexToken.LeftBracket -> '['
    is LatexToken.RightBracket -> ']'
    else -> null
}

internal fun CommandRegistry.installMathCodeHandlers() {
    register("mathcode") { _, ctx, stream -> stream.raw {
        stream.skipWhitespace()
        val character = if (stream.startsWith('\u0060')) {
            stream.readAtom()
            val atom = stream.readAtom()
            (atom as? LatexToken.Command)?.name?.singleOrNull() ?: atom?.mathCharacter()
        } else {
            stream.readMathInteger(255)?.toChar()
        }
        stream.skipWhitespace()
        if (stream.startsWith('=')) stream.readAtom()
        stream.skipWhitespace()
        val code = stream.readMathInteger(ACTIVE_MATH_CODE)
        if (character == null || code == null || character.code > 255) {
            ctx.diagnostics.add(ParseDiagnostic(
                stream.peek(-1)?.range ?: SourceRange.EMPTY, "Invalid \\mathcode assignment",
                ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.INVALID_ARGUMENT
            ))
        } else {
            ctx.defineMathCode(character, code)
            stream.skipWhitespace()
        }
        LatexNode.Text("")
    } }
}

private fun LatexTokenStream.startsWith(char: Char): Boolean =
    (peek() as? LatexToken.Text)?.content?.firstOrNull() == char

private fun LatexTokenStream.readMathInteger(max: Int): Int? {
    val radix = when {
        startsWith('"') -> { readAtom(); 16 }
        peek() is LatexToken.Prime -> { readAtom(); 8 }
        else -> 10
    }
    var value = 0
    var hasDigits = false
    while (true) {
        val token = peek() as? LatexToken.Text ?: break
        var length = 0
        for (character in token.content) {
            val digit = character.digitToIntOrNull(radix) ?: break
            if (value <= max) value = value * radix + digit
            length++
        }
        if (length == 0) break
        consumeTextPrefix(token, length)
        hasDigits = true
    }
    return value.takeIf { hasDigits && it <= max }
}
