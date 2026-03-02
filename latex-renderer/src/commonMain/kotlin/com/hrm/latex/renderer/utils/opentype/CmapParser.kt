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
 * OpenType cmap 表解析器。
 *
 * cmap 表将 Unicode 码位映射到 Glyph ID。MATH 表的所有数据以 Glyph ID 索引，
 * 因此 cmap 是连接 Unicode 字符和 MATH 表数据的桥梁。
 *
 * 支持的子表格式：
 * - Format 4: BMP (U+0000 ~ U+FFFF)，最常见
 * - Format 12: Full Unicode (包括补充平面)，处理 U+10000+ 的数学符号
 */
internal class CmapParser(
    private val reader: BinaryReader,
    private val tableOffset: Int
) {
    /** Unicode 码位 → Glyph ID 的正向映射 */
    private val charToGlyph = mutableMapOf<Int, Int>()

    /** Glyph ID → Unicode 码位 的反向映射（取最小码位） */
    private val glyphToChar = mutableMapOf<Int, Int>()

    /**
     * 解析 cmap 表，构建双向映射。
     *
     * 优先使用 platformID=3 (Windows) 或 platformID=0 (Unicode) 的子表。
     * 优先使用 Format 12（Full Unicode），其次 Format 4（BMP）。
     */
    fun parse(): CmapTable {
        val version = reader.readUInt16(tableOffset)
        val numSubtables = reader.readUInt16(tableOffset + 2)

        // 收集候选子表，按优先级排序
        data class SubtableInfo(val platformID: Int, val encodingID: Int, val offset: Int)

        val subtables = mutableListOf<SubtableInfo>()
        for (i in 0 until numSubtables) {
            val recordOffset = tableOffset + 4 + i * 8
            val platformID = reader.readUInt16(recordOffset)
            val encodingID = reader.readUInt16(recordOffset + 2)
            val subtableOffset = reader.readUInt32(recordOffset + 4).toInt()
            subtables.add(SubtableInfo(platformID, encodingID, tableOffset + subtableOffset))
        }

        // 优先级：Unicode Full (format 12) > Windows Unicode BMP (format 4)
        // platformID=0 (Unicode) 或 platformID=3 (Windows)
        val candidates = subtables.filter { it.platformID == 0 || it.platformID == 3 }

        // 先尝试找 Format 12 子表
        for (sub in candidates) {
            val format = reader.readUInt16(sub.offset)
            if (format == 12) {
                parseFormat12(sub.offset)
                break
            }
        }

        // 如果 Format 12 没找到或没有数据，尝试 Format 4
        if (charToGlyph.isEmpty()) {
            for (sub in candidates) {
                val format = reader.readUInt16(sub.offset)
                if (format == 4) {
                    parseFormat4(sub.offset)
                    break
                }
            }
        }

        return CmapTable(charToGlyph.toMap(), glyphToChar.toMap())
    }

    /**
     * 解析 Format 4 子表 (BMP 区域)
     *
     * 结构：segCount 个区间，每个区间 [startCode, endCode] 映射到一段 Glyph ID
     */
    private fun parseFormat4(offset: Int) {
        // format(2) + length(2) + language(2) + segCountX2(2) + ...
        val segCountX2 = reader.readUInt16(offset + 6)
        val segCount = segCountX2 / 2

        // 四个并行数组的起始偏移
        val endCodesStart = offset + 14
        val startCodesStart = endCodesStart + segCount * 2 + 2  // +2 for reservedPad
        val idDeltasStart = startCodesStart + segCount * 2
        val idRangeOffsetsStart = idDeltasStart + segCount * 2

        for (i in 0 until segCount) {
            val endCode = reader.readUInt16(endCodesStart + i * 2)
            val startCode = reader.readUInt16(startCodesStart + i * 2)
            val idDelta = reader.readInt16(idDeltasStart + i * 2)
            val idRangeOffset = reader.readUInt16(idRangeOffsetsStart + i * 2)

            if (startCode == 0xFFFF) break

            for (code in startCode..endCode) {
                val glyphId = if (idRangeOffset == 0) {
                    (code + idDelta) and 0xFFFF
                } else {
                    val rangeOffsetLocation = idRangeOffsetsStart + i * 2
                    val glyphIdOffset = rangeOffsetLocation + idRangeOffset + (code - startCode) * 2
                    val rawGlyphId = reader.readUInt16(glyphIdOffset)
                    if (rawGlyphId == 0) 0 else (rawGlyphId + idDelta) and 0xFFFF
                }

                if (glyphId != 0) {
                    addMapping(code, glyphId)
                }
            }
        }
    }

    /**
     * 解析 Format 12 子表 (Full Unicode)
     *
     * 结构：nGroups 个 SequentialMapGroup，每组 [startCharCode, endCharCode, startGlyphID]
     */
    private fun parseFormat12(offset: Int) {
        // format(2) + reserved(2) + length(4) + language(4) + numGroups(4)
        val numGroups = reader.readUInt32(offset + 12).toInt()

        val groupsStart = offset + 16
        for (i in 0 until numGroups) {
            val groupOffset = groupsStart + i * 12
            val startCharCode = reader.readUInt32(groupOffset).toInt()
            val endCharCode = reader.readUInt32(groupOffset + 4).toInt()
            val startGlyphID = reader.readUInt32(groupOffset + 8).toInt()

            for (j in 0..(endCharCode - startCharCode)) {
                val charCode = startCharCode + j
                val glyphId = startGlyphID + j
                if (glyphId != 0) {
                    addMapping(charCode, glyphId)
                }
            }
        }
    }

    private fun addMapping(charCode: Int, glyphId: Int) {
        charToGlyph[charCode] = glyphId
        // 反向映射保留最小的码位（避免 PUA 覆盖标准码位）
        if (!glyphToChar.containsKey(glyphId) || charCode < glyphToChar[glyphId]!!) {
            glyphToChar[glyphId] = charCode
        }
    }
}

/**
 * cmap 表解析结果：Unicode ↔ Glyph ID 双向映射。
 */
internal class CmapTable(
    private val charToGlyph: Map<Int, Int>,
    private val glyphToChar: Map<Int, Int>
) {
    /** Unicode 码位 → Glyph ID */
    fun charToGlyphId(codePoint: Int): Int = charToGlyph[codePoint] ?: 0

    /** Unicode 字符串 → Glyph ID（取第一个字符） */
    fun stringToGlyphId(text: String): Int {
        if (text.isEmpty()) return 0
        val codePoint = text.codePointAt(0)
        return charToGlyphId(codePoint)
    }

    /** Glyph ID → Unicode 字符串 */
    fun glyphIdToString(glyphId: Int): String {
        val codePoint = glyphToChar[glyphId] ?: return ""
        return String(Character.toChars(codePoint))
    }

    /** Glyph ID → Unicode 码位，无映射时返回 -1 */
    fun glyphIdToCodePoint(glyphId: Int): Int = glyphToChar[glyphId] ?: -1

    /** 检查是否有 MATH 相关的 glyph 映射 */
    val isNotEmpty: Boolean get() = charToGlyph.isNotEmpty()
}

// Kotlin/JVM compatible codePointAt
private fun String.codePointAt(index: Int): Int {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < this.length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return Character.toCodePoint(high, low)
        }
    }
    return high.code
}

// Kotlin Multiplatform compatible Character helpers
private object Character {
    fun toCodePoint(high: Char, low: Char): Int {
        return ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
    }

    fun toChars(codePoint: Int): CharArray {
        return if (codePoint < 0x10000) {
            charArrayOf(codePoint.toChar())
        } else {
            val offset = codePoint - 0x10000
            charArrayOf(
                ((offset ushr 10) + 0xD800).toChar(),
                ((offset and 0x3FF) + 0xDC00).toChar()
            )
        }
    }
}
