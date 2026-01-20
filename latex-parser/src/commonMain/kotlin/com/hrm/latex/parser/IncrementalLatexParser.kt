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

import com.hrm.latex.base.LatexConstants
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.model.LatexNode

/**
 * 增量 LaTeX 解析器
 * 支持逐步输入 LaTeX 内容并实时解析渲染
 *
 * 使用场景：
 * - 实时输入/打字效果
 * - 流式传输的 LaTeX 内容
 * - 实时预览
 *
 * 示例：
 * ```kotlin
 * val parser = IncrementalLatexParser()
 * parser.append("\\int_{-\\")
 * val result1 = parser.getCurrentDocument() // 解析已有内容
 * parser.append("infty}^{\\infty}")
 * val result2 = parser.getCurrentDocument() // 更新解析结果
 * ```
 */
class IncrementalLatexParser {

    /**
     * 累积的输入内容
     */
    private val buffer = StringBuilder()

    /**
     * 最后一次成功解析的位置
     */
    private var lastSuccessfulPosition = 0

    /**
     * 缓存的解析结果
     */
    private var cachedDocument: LatexNode.Document? = null

    /**
     * 基础解析器实例
     */
    private val baseParser = LatexParser()

    companion object {
        private const val TAG = "IncrementalLatexParser"
    }

    /**
     * 追加新的 LaTeX 内容
     * @param text 新增的文本内容
     */
    fun append(text: String) {
        buffer.append(text)
        HLog.d(TAG, "追加内容: $text, 当前缓冲区: $buffer")

        // 触发重新解析
        reparseFromLastPosition()
    }

    /**
     * 清空所有内容和状态
     */
    fun clear() {
        buffer.clear()
        lastSuccessfulPosition = 0
        cachedDocument = null
        HLog.d(TAG, "清空解析器状态")
    }

    /**
     * 获取当前可解析的文档
     * 会尽可能解析已有内容，即使内容不完整
     */
    fun getCurrentDocument(): LatexNode.Document {
        if (cachedDocument == null) {
            reparseFromLastPosition()
        }
        return cachedDocument ?: LatexNode.Document(emptyList())
    }

    /**
     * 获取当前缓冲区的完整内容
     */
    fun getCurrentInput(): String = buffer.toString()

    /**
     * 从上次成功位置重新解析
     */
    private fun reparseFromLastPosition() {
        val input = buffer.toString()
        if (input.isEmpty()) {
            cachedDocument = LatexNode.Document(emptyList())
            return
        }

        // 快速路径：如果输入很短，直接尝试解析
        if (input.length <= LatexConstants.INCREMENTAL_PARSE_FAST_PATH_MAX_LENGTH) {
            try {
                cachedDocument = baseParser.parse(input)
                lastSuccessfulPosition = input.length
                HLog.d(TAG, "快速路径解析成功")
                return
            } catch (e: Exception) {
                HLog.w(TAG, "快速路径解析失败: ${e.message}")
                cachedDocument = LatexNode.Document(emptyList())
                return
            }
        }

        try {
            // 尝试完整解析
            cachedDocument = baseParser.parse(input)
            lastSuccessfulPosition = input.length
            HLog.d(TAG, "完整解析成功")
        } catch (e: Exception) {
            // 完整解析失败，尝试部分解析
            HLog.d(TAG, "完整解析失败，尝试部分解析: ${e.message}")
            cachedDocument = parsePartial(input)
        }
    }

    /**
     * 部分解析：处理不完整的 LaTeX 内容
     * 
     * 策略：两阶段回退算法
     * 1. 精细回退阶段（最近 N 字符）：逐字符回退，适合处理末尾的增量输入错误
     * 2. 快速回退阶段（之前所有字符）：步进回退，牺牲精度换性能
     * 
     * 时间复杂度：O(n) 最坏情况，O(1) 平均情况（增量输入通常错误在末尾）
     * 
     * 注意：解析有效性不是单调的，不能使用二分查找
     * 示例：`\int_{` 解析失败，但 `\int` 解析成功
     * 
     * @param input 待解析的不完整 LaTeX 字符串
     * @return 可解析部分的文档节点
     */
    private fun parsePartial(input: String): LatexNode.Document {
        HLog.d(TAG, "开始部分解析，输入长度: ${input.length}")

        var length = input.length
        val firstStageLimit = maxOf(
            1, 
            input.length - LatexConstants.INCREMENTAL_PARSE_FINE_BACKTRACK_RANGE
        )

        // 第一阶段：精细回退（逐字符）
        while (length >= firstStageLimit) {
            try {
                val testInput = input.substring(0, length)
                val doc = baseParser.parse(testInput)
                lastSuccessfulPosition = length
                HLog.d(TAG, "部分解析成功，解析到位置: $length")
                return doc
            } catch (e: Exception) {
                // 预期的解析错误，继续回退
                length--
            }
        }

        // 第二阶段：快速回退（步进）
        while (length > 0) {
            try {
                val testInput = input.substring(0, length)
                val doc = baseParser.parse(testInput)
                lastSuccessfulPosition = length
                HLog.d(TAG, "快速回退解析成功，解析到位置: $length")
                return doc
            } catch (e: Exception) {
                length -= LatexConstants.INCREMENTAL_PARSE_FAST_BACKTRACK_STEP
                if (length < 0) length = 0
            }
        }

        HLog.w(TAG, "部分解析完全失败，返回空文档")
        return LatexNode.Document(emptyList())
    }

    /**
     * 获取解析进度（已成功解析的字符数 / 总字符数）
     */
    fun getProgress(): Float {
        val total = buffer.length
        return if (total > 0) {
            lastSuccessfulPosition.toFloat() / total
        } else {
            1f
        }
    }

    /**
     * 获取未解析的部分（可用于调试或显示）
     */
    fun getUnparsedContent(): String {
        val total = buffer.toString()
        return if (lastSuccessfulPosition < total.length) {
            total.substring(lastSuccessfulPosition)
        } else {
            ""
        }
    }
}
