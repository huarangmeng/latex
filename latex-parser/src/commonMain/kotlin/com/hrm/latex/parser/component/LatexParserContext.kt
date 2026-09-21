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

import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 自定义命令定义
 * @param name 命令名（不含反斜杠）
 * @param numArgs 参数个数（0-9）
 * @param tokens 未展开的宏体 token，与定义节点共享，不重复解析。
 * @param defaultTokens 第一个参数的默认值（可选参数语法 \newcommand{\cmd}[2][default]{body}）
 */
data class CustomCommand(
    val name: String,
    val numArgs: Int,
    val tokens: List<LatexToken>,
    val defaultTokens: List<LatexToken>? = null,
    val parameterText: List<LatexToken> = emptyList(),
    val acceptsDelimiterModifier: Boolean = false,
    val isAlias: Boolean = false
)

/** Environment definitions remain unexpanded until the environment is entered. */
data class CustomEnvironment(
    val name: String,
    val numArgs: Int,
    val beginTokens: List<LatexToken>,
    val endTokens: List<LatexToken>,
    val defaultTokens: List<LatexToken>? = null
)

/**
 * 解析器上下文接口，用于解决循环依赖和提供通用解析能力。
 *
 * 定义表只读；所有写入通过 define 方法登记，以统一维护局部和全局作用域。
 *
 * [diagnostics] 收集解析过程中的非致命诊断信息。
 */
internal interface LatexParserContext {
    val tokenStream: LatexTokenStream
    val customCommands: Map<String, CustomCommand>
    val customEnvironments: Map<String, CustomEnvironment>
    val lengths: Map<String, String>
    fun defineLength(name: String, value: String)
    val colors: Map<String, String>
    val diagnostics: MutableList<ParseDiagnostic>
    var globalAssignment: Boolean
    fun expandTokens(tokens: List<LatexToken>): List<LatexToken>
    fun isCommandDefined(name: String): Boolean
    fun defineCommand(command: CustomCommand, global: Boolean = false)
    val scopeDepth: Int
    fun pushScope()
    fun popScope()
    fun defineEnvironment(environment: CustomEnvironment)
    fun defineColor(name: String, value: String)

    fun parseExpression(): LatexNode?
    fun parseFactor(): LatexNode?
    fun parseArgument(): LatexNode?
    fun parseGroup(): LatexNode.Group
    /** Opening command has already been consumed by CommandParser. */
    fun parseCommandGroup(command: String): LatexNode.Group
    fun normalizeStyleDeclarations(nodes: List<LatexNode>): List<LatexNode>
}
