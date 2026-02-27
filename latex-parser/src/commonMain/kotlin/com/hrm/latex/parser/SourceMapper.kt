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

package com.hrm.latex.parser

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.SourceRange

/**
 * AST 源码位置导航工具
 *
 * 提供从源码偏移量到 AST 节点的双向映射能力，
 * 是编辑器集成（光标定位、选区高亮）的核心基础设施。
 */
object SourceMapper {

    /**
     * 获取包含指定偏移量的节点路径（从根到叶）
     *
     * @param root 文档根节点
     * @param offset 源码字符偏移量
     * @return 从根到最深匹配叶节点的路径，如果 offset 不在任何节点范围内则返回空列表
     */
    fun nodePathAt(root: LatexNode, offset: Int): List<LatexNode> {
        val path = mutableListOf<LatexNode>()
        findPath(root, offset, path)
        return path
    }

    /**
     * 获取包含指定偏移量的最深叶节点
     *
     * @param root 文档根节点
     * @param offset 源码字符偏移量
     * @return 最深的匹配节点，如果 offset 不在任何节点范围内则返回 null
     */
    fun leafNodeAt(root: LatexNode, offset: Int): LatexNode? {
        return nodePathAt(root, offset).lastOrNull()
    }

    /**
     * 获取指定节点在其父节点子列表中的前一个兄弟节点
     */
    fun previousSibling(parent: LatexNode, child: LatexNode): LatexNode? {
        val children = childrenOf(parent)
        val index = children.indexOf(child)
        return if (index > 0) children[index - 1] else null
    }

    /**
     * 获取指定节点在其父节点子列表中的后一个兄弟节点
     */
    fun nextSibling(parent: LatexNode, child: LatexNode): LatexNode? {
        val children = childrenOf(parent)
        val index = children.indexOf(child)
        return if (index in 0 until children.lastIndex) children[index + 1] else null
    }

    /**
     * 收集 AST 中所有带 sourceRange 的叶节点（扁平化）
     * 按 sourceRange.start 排序
     */
    fun collectLeaves(root: LatexNode): List<LatexNode> {
        val leaves = mutableListOf<LatexNode>()
        collectLeavesImpl(root, leaves)
        return leaves.sortedBy { it.sourceRange?.start ?: 0 }
    }

    // --- 内部实现 ---

    private fun findPath(node: LatexNode, offset: Int, path: MutableList<LatexNode>): Boolean {
        val range = node.sourceRange ?: return findInChildren(node, offset, path)

        if (!range.contains(offset)) return false

        path.add(node)

        // 尝试在子节点中找到更深的匹配
        findInChildren(node, offset, path)
        return true
    }

    private fun findInChildren(node: LatexNode, offset: Int, path: MutableList<LatexNode>): Boolean {
        for (child in childrenOf(node)) {
            if (findPath(child, offset, path)) return true
        }
        return false
    }

    private fun collectLeavesImpl(node: LatexNode, result: MutableList<LatexNode>) {
        val children = childrenOf(node)
        if (children.isEmpty()) {
            if (node.sourceRange != null) {
                result.add(node)
            }
        } else {
            for (child in children) {
                collectLeavesImpl(child, result)
            }
        }
    }

    /**
     * 获取节点的直接子节点列表
     */
    fun childrenOf(node: LatexNode): List<LatexNode> = when (node) {
        is LatexNode.Document -> node.children
        is LatexNode.Group -> node.children
        is LatexNode.Fraction -> listOf(node.numerator, node.denominator)
        is LatexNode.Root -> listOfNotNull(node.content, node.index)
        is LatexNode.Superscript -> listOf(node.base, node.exponent)
        is LatexNode.Subscript -> listOf(node.base, node.index)
        is LatexNode.Accent -> listOf(node.content)
        is LatexNode.Style -> node.content
        is LatexNode.MathStyle -> node.content
        is LatexNode.Color -> node.content
        is LatexNode.Boxed -> node.content
        is LatexNode.Phantom -> node.content
        is LatexNode.Smash -> node.content
        is LatexNode.VPhantom -> node.content
        is LatexNode.HPhantom -> node.content
        is LatexNode.Delimited -> node.content
        is LatexNode.ExtensibleArrow -> listOfNotNull(node.content, node.below)
        is LatexNode.Stack -> listOfNotNull(node.base, node.above, node.below)
        is LatexNode.BigOperator -> listOfNotNull(node.subscript, node.superscript)
        is LatexNode.Negation -> listOf(node.content)
        is LatexNode.Tag -> listOf(node.label)
        is LatexNode.Binomial -> listOf(node.top, node.bottom)
        is LatexNode.Environment -> node.content
        is LatexNode.Matrix -> node.rows.flatten()
        is LatexNode.Array -> node.rows.flatten()
        is LatexNode.Tabular -> node.rows.flatten()
        is LatexNode.Aligned -> node.rows.flatten()
        is LatexNode.Cases -> node.cases.flatMap { listOf(it.first, it.second) }
        is LatexNode.Split -> node.rows.flatten()
        is LatexNode.Multline -> node.lines
        is LatexNode.Eqnarray -> node.rows.flatten()
        is LatexNode.Subequations -> node.content
        is LatexNode.Substack -> node.rows.flatten()
        is LatexNode.NewCommand -> node.definition
        is LatexNode.SideSet -> listOfNotNull(node.leftSub, node.leftSup, node.rightSub, node.rightSup, node.base)
        is LatexNode.Tensor -> listOf(node.base) + node.indices.map { it.second }
        is LatexNode.Command -> node.arguments
        // 叶节点
        is LatexNode.Text -> emptyList()
        is LatexNode.Symbol -> emptyList()
        is LatexNode.Operator -> emptyList()
        is LatexNode.Space -> emptyList()
        is LatexNode.HSpace -> emptyList()
        is LatexNode.NewLine -> emptyList()
        is LatexNode.TextMode -> emptyList()
        is LatexNode.ManualSizedDelimiter -> emptyList()
        is LatexNode.Label -> emptyList()
        is LatexNode.Ref -> emptyList()
        is LatexNode.EqRef -> emptyList()
    }
}
