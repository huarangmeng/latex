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
 * OpenType 表目录解析结果。
 *
 * @property tables 表名 → 表偏移/长度 的映射
 * @property unitsPerEm 字体设计空间单位（从 head 表读取）
 */
internal class OpenTypeFontInfo(
    val tables: Map<String, TableRecord>,
    val unitsPerEm: Int
)

/**
 * OpenType 表记录
 *
 * @property tag 4 字符表标签（如 "MATH", "cmap", "head"）
 * @property offset 表在字体文件中的字节偏移
 * @property length 表的字节长度
 */
internal data class TableRecord(
    val tag: String,
    val offset: Int,
    val length: Int
)

/**
 * OpenType 字体文件顶层解析器。
 *
 * 解析字体文件头和表目录，提供对各个表的定位能力。
 * 支持 TrueType (.ttf) 和 OpenType (.otf) 格式。
 */
internal object OpenTypeFontParser {

    /**
     * 解析字体文件，获取表目录和基本信息。
     *
     * @param reader 字体文件二进制数据
     * @return 字体信息，包含表目录和 unitsPerEm
     * @throws IllegalArgumentException 如果文件格式不支持
     */
    fun parse(reader: BinaryReader): OpenTypeFontInfo {
        // 读取 sfVersion 或 signature
        val sfVersion = reader.readUInt32(0)

        // 验证格式：
        // 0x00010000 = TrueType
        // 0x4F54544F = "OTTO" (CFF/OpenType)
        // 0x74727565 = "true" (某些 TrueType)
        val validSignatures = setOf(0x00010000L, 0x4F54544FL, 0x74727565L)
        require(sfVersion in validSignatures) {
            "Unsupported font format: 0x${sfVersion.toString(16)}"
        }

        val numTables = reader.readUInt16(4)

        // 解析表目录（从偏移 12 开始，每条记录 16 字节）
        val tables = mutableMapOf<String, TableRecord>()
        for (i in 0 until numTables) {
            val recordOffset = 12 + i * 16
            val tag = reader.readTag(recordOffset)
            // checkSum at recordOffset + 4 (skip)
            val tableOffset = reader.readUInt32(recordOffset + 8).toInt()
            val tableLength = reader.readUInt32(recordOffset + 12).toInt()
            tables[tag] = TableRecord(tag, tableOffset, tableLength)
        }

        // 解析 head 表获取 unitsPerEm
        val unitsPerEm = tables["head"]?.let { head ->
            // unitsPerEm 在 head 表偏移 18 处 (uint16)
            reader.readUInt16(head.offset + 18)
        } ?: 1000 // 默认值

        return OpenTypeFontInfo(tables, unitsPerEm)
    }
}
