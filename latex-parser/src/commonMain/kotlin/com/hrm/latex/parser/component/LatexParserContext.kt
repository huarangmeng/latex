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

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 自定义命令定义
 * @param name 命令名（不含反斜杠）
 * @param numArgs 参数个数（0-9）
 * @param definition 定义内容（AST 节点列表）
 */
data class CustomCommand(
    val name: String,
    val numArgs: Int,
    val definition: List<LatexNode>
)

/**
 * 解析器上下文接口，用于解决循环依赖和提供通用解析能力
 */
interface LatexParserContext {
    val tokenStream: LatexTokenStream
    val customCommands: MutableMap<String, CustomCommand>

    fun parseExpression(): LatexNode?
    fun parseFactor(): LatexNode?
    fun parseArgument(): LatexNode?
    fun parseGroup(): LatexNode.Group
}
