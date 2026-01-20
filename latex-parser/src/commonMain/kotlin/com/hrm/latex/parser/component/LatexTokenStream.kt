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

import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 封装 Token 流的操作，如 peek, advance, expect
 */
class LatexTokenStream(private val tokens: List<LatexToken>) {
    private var position = 0

    fun peek(offset: Int = 0): LatexToken? {
        val pos = position + offset
        return if (pos < tokens.size) tokens[pos] else null
    }

    fun advance(): LatexToken? {
        val token = peek()
        position++
        return token
    }

    fun isEOF(): Boolean {
        val token = peek()
        return token == null || token is LatexToken.EOF
    }

    fun expect(type: String, message: String? = null): LatexToken {
        val token = peek()
        if (token == null) {
            throw Exception(message ?: "期望 $type，但到达文件末尾")
        }
        advance()
        return token
    }
    
    fun reset() {
        position = 0
    }
}
