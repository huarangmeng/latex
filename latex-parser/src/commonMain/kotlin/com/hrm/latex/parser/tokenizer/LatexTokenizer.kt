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

import com.hrm.latex.base.log.HLog

/**
 * LaTeX 词法分析器
 */
class LatexTokenizer(private val input: String) {
    private var position = 0
    private val tokens = mutableListOf<LatexToken>()

    companion object {
        private const val TAG = "LatexTokenizer"
    }

    /**
     * 执行词法分析
     */
    fun tokenize(): List<LatexToken> {
        HLog.d(TAG, "开始词法分析，输入长度: ${input.length}")

        while (position < input.length) {
            when (val char = peek()) {
                '\\' -> handleBackslash()
                '{' -> {
                    tokens.add(LatexToken.LeftBrace)
                    advance()
                }

                '}' -> {
                    tokens.add(LatexToken.RightBrace)
                    advance()
                }

                '[' -> {
                    tokens.add(LatexToken.LeftBracket)
                    advance()
                }

                ']' -> {
                    tokens.add(LatexToken.RightBracket)
                    advance()
                }

                '^' -> {
                    tokens.add(LatexToken.Superscript)
                    advance()
                }

                '_' -> {
                    tokens.add(LatexToken.Subscript)
                    advance()
                }

                '&' -> {
                    tokens.add(LatexToken.Ampersand)
                    advance()
                }
                
                '(', ')', '|' -> {
                    // 将括号和竖线作为单独的文本token
                    tokens.add(LatexToken.Text(char.toString()))
                    advance()
                }

                '\n', '\r' -> handleNewLine()
                ' ', '\t' -> handleWhitespace()
                else -> handleText()
            }
        }

        tokens.add(LatexToken.EOF)
        HLog.d(TAG, "词法分析完成，生成 ${tokens.size} 个 token")
        return tokens
    }

    private fun peek(offset: Int = 0): Char? {
        val pos = position + offset
        return if (pos < input.length) input[pos] else null
    }

    private fun advance(count: Int = 1): Char? {
        val char = peek()
        position += count
        return char
    }

    private fun handleBackslash() {
        advance() // 跳过 \

        if (peek() == '\\') {
            // \\ 表示换行
            tokens.add(LatexToken.NewLine)
            advance()
            return
        }

        // 读取命令名
        val commandName = buildString {
            while (position < input.length) {
                val char = peek() ?: break
                if (char.isLetter() || (isEmpty() && char == '@')) {
                    append(char)
                    advance()
                } else {
                    break
                }
            }
        }

        if (commandName.isEmpty()) {
            // 处理特殊符号，如 \{, \}, \$, \% 等
            val char = peek()
            if (char != null && !char.isWhitespace()) {
                tokens.add(LatexToken.Command(char.toString()))
                advance()
            }
            return
        }

        // 检查是否是环境开始或结束
        when (commandName) {
            "begin" -> {
                val envName = readEnvironmentName()
                if (envName != null) {
                    tokens.add(LatexToken.BeginEnvironment(envName))
                } else {
                    tokens.add(LatexToken.Command(commandName))
                }
            }

            "end" -> {
                val envName = readEnvironmentName()
                if (envName != null) {
                    tokens.add(LatexToken.EndEnvironment(envName))
                } else {
                    tokens.add(LatexToken.Command(commandName))
                }
            }

            else -> {
                tokens.add(LatexToken.Command(commandName))
            }
        }
    }

    private fun readEnvironmentName(): String? {
        skipWhitespace()
        if (peek() != '{') return null

        advance() // 跳过 {
        val name = buildString {
            while (position < input.length) {
                val char = peek() ?: break
                if (char == '}') break
                append(char)
                advance()
            }
        }

        if (peek() == '}') {
            advance() // 跳过 }
            return name
        }
        return null
    }

    private fun handleText() {
        val text = buildString {
            while (position < input.length) {
                val char = peek() ?: break
                // 停止字符：特殊符号、空白字符、括号等
                if (char in setOf('\\', '{', '}', '[', ']', '^', '_', '&', '\n', '\r', '(', ')', '|')) {
                    break
                }
                if (char == ' ' || char == '\t') {
                    break
                }
                append(char)
                advance()
            }
        }

        if (text.isNotEmpty()) {
            tokens.add(LatexToken.Text(text))
        }
    }

    private fun handleWhitespace() {
        // 跳过所有连续的空格和制表符
        while (position < input.length) {
            val char = peek() ?: break
            if (char != ' ' && char != '\t') break
            advance()
        }
        
        // 将所有连续空白符合并为单个空格
        tokens.add(LatexToken.Whitespace(" "))
    }

    /**
     * 处理普通换行符 (\n, \r)
     * 
     * 注意：区别于 LaTeX 命令 `\\`（在 handleBackslash 中处理）
     * 
     * 根据 LaTeX 规范：
     * - 单个换行符 → 视为空格
     * - 连续换行符（空行）→ 段落分隔（但在数学模式下通常被忽略）
     * 
     * 当前实现：**将所有连续换行符转换为单个空格**
     * 
     * 示例：
     * ```latex
     * x + y
     * + z     → 解析为 "x + y + z"（换行变空格）
     * 
     * a=1
     * 
     * b=2     → 解析为 "a=1 b=2"（空行也变单个空格）
     * ```
     * 
     * 优点：
     * - 符合 LaTeX 标准：换行视为空格
     * - 避免文本模式下空格丢失（如 "x\ny" 正确解析为 "x y"）
     * - 简化逻辑，由解析器统一处理空格
     */
    private fun handleNewLine() {
        // 跳过所有连续的换行符
        while (position < input.length) {
            val char = peek() ?: break
            if (char != '\n' && char != '\r') break
            advance()
        }
        
        // 将换行符转换为单个空格 token
        // 这样 "x\ny" 会正确解析为 "x y" 而不是 "xy"
        tokens.add(LatexToken.Whitespace(" "))
    }

    private fun skipWhitespace() {
        while (position < input.length) {
            val char = peek() ?: break
            if (!char.isWhitespace()) break
            advance()
        }
    }
}
