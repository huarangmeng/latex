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


package com.hrm.latex.parser.tokenizer

import com.hrm.latex.parser.model.SourceRange

/**
 * LaTeX 词法单元
 *
 * 每个 token 携带 [range] 记录其在原始输入字符串中的位置
 */
sealed class LatexToken {
    abstract val range: SourceRange

    data class Text(val content: String, override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class Command(val name: String, override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class BeginEnvironment(val name: String, override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class EndEnvironment(val name: String, override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class LeftBrace(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class RightBrace(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class LeftBracket(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class RightBracket(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class Superscript(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class Subscript(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class Ampersand(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class NewLine(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class Whitespace(val content: String, override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
    data class EOF(override val range: SourceRange = SourceRange.EMPTY) : LatexToken()
}
