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

package com.hrm.latex.renderer.utils.opentype

/**
 * CFF (Compact Font Format) 表解析器。
 *
 * 解析 OTF 字体中 CFF 表的结构，提取字形轮廓数据 (CharStrings)。
 * 用于在 TextMeasurer 无法正确渲染 MATH 变体字形时，
 * 直接从字体文件提取 Path 数据进行 Canvas 绘制。
 *
 * CFF 表结构（简化）:
 * ```
 * Header
 * Name INDEX
 * Top DICT INDEX
 * String INDEX
 * Global Subr INDEX
 * [Encodings / Charsets / CharStrings / Local Subr INDEX]
 * ```
 *
 * 参考：Adobe Technical Note #5176 "The Compact Font Format Specification"
 */
internal class CffParser(
    private val data: ByteArray,
    private val tableOffset: Int
) {
    private var pos: Int = 0

    /**
     * CFF 解析结果。
     *
     * @property charStrings 每个 glyphId 对应的 CharString 字节数据
     * @property globalSubrs 全局子程序 INDEX
     * @property localSubrs 本地子程序 INDEX（从 Private DICT 引用）
     * @property defaultWidthX 默认字形宽度（设计空间单位）
     * @property nominalWidthX 名义字形宽度（设计空间单位）
     */
    class CffData(
        val charStrings: List<ByteArray>,
        val globalSubrs: List<ByteArray>,
        val localSubrs: List<ByteArray>,
        val defaultWidthX: Float,
        val nominalWidthX: Float
    )

    /**
     * 解析 CFF 表，提取 CharString 数据。
     */
    fun parse(): CffData? {
        return try {
            pos = tableOffset

            // CFF Header
            val major = readCard8()
            val minor = readCard8()
            val hdrSize = readCard8()
            val offSize = readCard8()

            // 跳转到 Name INDEX 起始
            pos = tableOffset + hdrSize

            // Name INDEX — 跳过
            skipIndex()

            // Top DICT INDEX
            val topDictIndex = readIndex()
            if (topDictIndex.isEmpty()) return null
            val topDict = parseDict(topDictIndex[0])

            // String INDEX — 跳过
            skipIndex()

            // Global Subr INDEX
            val globalSubrs = readIndex()

            // 从 Top DICT 获取 CharStrings 偏移
            val charStringsOffset = topDict[CHARSTRINGS_OP]?.firstOrNull()?.toInt() ?: return null

            // 定位并读取 CharStrings INDEX
            pos = tableOffset + charStringsOffset
            val charStrings = readIndex()

            // 从 Top DICT 获取 Private DICT
            var defaultWidthX = 0f
            var nominalWidthX = 0f
            var localSubrs = emptyList<ByteArray>()

            val privateEntry = topDict[PRIVATE_OP]
            if (privateEntry != null && privateEntry.size >= 2) {
                val privateSize = privateEntry[0].toInt()
                val privateOffset = privateEntry[1].toInt()
                pos = tableOffset + privateOffset
                val privateDictData = ByteArray(privateSize)
                for (i in 0 until privateSize) {
                    privateDictData[i] = data[pos + i]
                }
                val privateDict = parseDict(privateDictData)

                defaultWidthX = privateDict[DEFAULT_WIDTH_X_OP]?.firstOrNull() ?: 0f
                nominalWidthX = privateDict[NOMINAL_WIDTH_X_OP]?.firstOrNull() ?: 0f

                // Local Subr INDEX
                val subrsOffset = privateDict[SUBRS_OP]?.firstOrNull()?.toInt()
                if (subrsOffset != null) {
                    pos = tableOffset + privateOffset + subrsOffset
                    localSubrs = readIndex()
                }
            }

            CffData(charStrings, globalSubrs, localSubrs, defaultWidthX, nominalWidthX)
        } catch (e: Exception) {
            null
        }
    }

    // ─── INDEX 读取 ──────────────────────────────────────────────

    /**
     * 读取 CFF INDEX 结构。
     *
     * INDEX 结构：count(2) + offSize(1) + offsets[count+1] + data
     * 每个元素的数据 = data[offsets[i]..offsets[i+1]]
     */
    private fun readIndex(): List<ByteArray> {
        val count = readCard16()
        if (count == 0) return emptyList()

        val offSize = readCard8()
        val offsets = IntArray(count + 1)
        for (i in 0..count) {
            offsets[i] = readOffset(offSize)
        }

        val dataStart = pos - 1 // offsets 从 1 开始
        val result = mutableListOf<ByteArray>()
        for (i in 0 until count) {
            val start = dataStart + offsets[i]
            val end = dataStart + offsets[i + 1]
            val len = end - start
            if (len > 0 && start >= 0 && end <= data.size) {
                val element = ByteArray(len)
                data.copyInto(element, 0, start, end)
                result.add(element)
            } else {
                result.add(ByteArray(0))
            }
        }
        pos = dataStart + offsets[count]
        return result
    }

    /**
     * 跳过一个 INDEX 结构。
     */
    private fun skipIndex() {
        val count = readCard16()
        if (count == 0) return

        val offSize = readCard8()
        // 跳到最后一个 offset 读取它的值，得到数据段的终点
        val lastOffsetPos = pos + count * offSize
        pos = lastOffsetPos
        val lastOffset = readOffset(offSize)
        // 数据段起始 = offsets 结束后 - 1（因为 offset 从 1 开始）
        pos = lastOffsetPos + offSize - 1 + lastOffset
    }

    // ─── DICT 解析 ──────────────────────────────────────────────

    /**
     * 解析 CFF DICT 数据。
     *
     * DICT 是 operator-operand 格式：
     * 先是一系列操作数（数值），然后是一个操作符。
     * 操作符是 DICT 条目的 key，操作数列表是 value。
     */
    private fun parseDict(dictData: ByteArray): Map<Int, List<Float>> {
        val result = mutableMapOf<Int, List<Float>>()
        val operands = mutableListOf<Float>()
        var i = 0

        while (i < dictData.size) {
            val b0 = dictData[i].toInt() and 0xFF

            when {
                // Operator: 0-21
                b0 in 0..21 -> {
                    val op = if (b0 == 12 && i + 1 < dictData.size) {
                        // Two-byte operator
                        i++
                        val b1 = dictData[i].toInt() and 0xFF
                        1200 + b1
                    } else {
                        b0
                    }
                    result[op] = operands.toList()
                    operands.clear()
                    i++
                }
                // Integer: 32-246 → value = b0 - 139
                b0 in 32..246 -> {
                    operands.add((b0 - 139).toFloat())
                    i++
                }
                // Integer: 247-250 → value = (b0 - 247) * 256 + b1 + 108
                b0 in 247..250 -> {
                    if (i + 1 < dictData.size) {
                        val b1 = dictData[i + 1].toInt() and 0xFF
                        operands.add(((b0 - 247) * 256 + b1 + 108).toFloat())
                        i += 2
                    } else i++
                }
                // Integer: 251-254 → value = -(b0 - 251) * 256 - b1 - 108
                b0 in 251..254 -> {
                    if (i + 1 < dictData.size) {
                        val b1 = dictData[i + 1].toInt() and 0xFF
                        operands.add((-(b0 - 251) * 256 - b1 - 108).toFloat())
                        i += 2
                    } else i++
                }
                // 4-byte integer: b0 == 28
                b0 == 28 -> {
                    if (i + 2 < dictData.size) {
                        val b1 = dictData[i + 1].toInt() and 0xFF
                        val b2 = dictData[i + 2].toInt() and 0xFF
                        val value = (b1 shl 8) or b2
                        operands.add(
                            (if (value >= 0x8000) value - 0x10000 else value).toFloat()
                        )
                        i += 3
                    } else i++
                }
                // 4-byte integer: b0 == 29
                b0 == 29 -> {
                    if (i + 4 < dictData.size) {
                        val b1 = dictData[i + 1].toInt() and 0xFF
                        val b2 = dictData[i + 2].toInt() and 0xFF
                        val b3 = dictData[i + 3].toInt() and 0xFF
                        val b4 = dictData[i + 4].toInt() and 0xFF
                        val value = (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                        operands.add(value.toFloat())
                        i += 5
                    } else i++
                }
                // Real number: b0 == 30
                b0 == 30 -> {
                    val (real, bytesConsumed) = readRealNumber(dictData, i + 1)
                    operands.add(real)
                    i += 1 + bytesConsumed
                }
                else -> i++ // 跳过未知字节
            }
        }
        return result
    }

    /**
     * 读取 CFF real number 编码。
     *
     * 每个字节的高 4 位和低 4 位各编码一个半字节 (nibble)：
     * 0-9: 数字, a: '.', b: 'E', c: 'E-', d: reserved, e: '-', f: 终止
     */
    private fun readRealNumber(dictData: ByteArray, startPos: Int): Pair<Float, Int> {
        val sb = StringBuilder()
        var pos = startPos
        var done = false

        while (pos < dictData.size && !done) {
            val byte = dictData[pos].toInt() and 0xFF
            for (shift in intArrayOf(4, 0)) {
                val nibble = (byte shr shift) and 0x0F
                when (nibble) {
                    in 0..9 -> sb.append(nibble)
                    0xa -> sb.append('.')
                    0xb -> sb.append('E')
                    0xc -> sb.append("E-")
                    0xe -> sb.append('-')
                    0xf -> { done = true; break }
                }
            }
            pos++
        }

        val value = sb.toString().toFloatOrNull() ?: 0f
        return value to (pos - startPos)
    }

    // ─── 基本类型读取 ──────────────────────────────────────────

    private fun readCard8(): Int {
        return data[pos++].toInt() and 0xFF
    }

    private fun readCard16(): Int {
        val b1 = data[pos++].toInt() and 0xFF
        val b2 = data[pos++].toInt() and 0xFF
        return (b1 shl 8) or b2
    }

    private fun readOffset(offSize: Int): Int {
        var value = 0
        for (i in 0 until offSize) {
            value = (value shl 8) or (data[pos++].toInt() and 0xFF)
        }
        return value
    }

    companion object {
        // CFF DICT 操作符常量
        const val CHARSTRINGS_OP = 17      // CharStrings 偏移
        const val PRIVATE_OP = 18          // Private DICT (size, offset)
        const val SUBRS_OP = 19            // Local Subrs 偏移 (相对于 Private DICT)
        const val DEFAULT_WIDTH_X_OP = 20  // defaultWidthX
        const val NOMINAL_WIDTH_X_OP = 21  // nominalWidthX
    }
}
