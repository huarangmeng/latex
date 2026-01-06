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
