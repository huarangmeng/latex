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
 * OpenType 字体文件二进制读取器。
 *
 * OpenType/TrueType 字体使用 Big-Endian 字节序。
 * 此类提供在字节数组上的定位读取能力，不维护内部游标状态，
 * 所有读取操作显式传入偏移量，便于在不同表之间跳转。
 */
internal class BinaryReader(private val data: ByteArray) {

    val size: Int get() = data.size

    fun readUInt8(offset: Int): Int {
        checkBounds(offset, 1)
        return data[offset].toInt() and 0xFF
    }

    fun readInt16(offset: Int): Int {
        checkBounds(offset, 2)
        val value = ((data[offset].toInt() and 0xFF) shl 8) or
                (data[offset + 1].toInt() and 0xFF)
        // 符号扩展：如果最高位为 1，则为负数
        return if (value >= 0x8000) value - 0x10000 else value
    }

    fun readUInt16(offset: Int): Int {
        checkBounds(offset, 2)
        return ((data[offset].toInt() and 0xFF) shl 8) or
                (data[offset + 1].toInt() and 0xFF)
    }

    fun readInt32(offset: Int): Int {
        checkBounds(offset, 4)
        return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
    }

    fun readUInt32(offset: Int): Long {
        checkBounds(offset, 4)
        return (((data[offset].toLong() and 0xFF) shl 24) or
                ((data[offset + 1].toLong() and 0xFF) shl 16) or
                ((data[offset + 2].toLong() and 0xFF) shl 8) or
                (data[offset + 3].toLong() and 0xFF))
    }

    fun readTag(offset: Int): String {
        checkBounds(offset, 4)
        return String(
            charArrayOf(
                data[offset].toInt().toChar(),
                data[offset + 1].toInt().toChar(),
                data[offset + 2].toInt().toChar(),
                data[offset + 3].toInt().toChar()
            )
        )
    }

    private fun checkBounds(offset: Int, length: Int) {
        if (offset < 0 || offset + length > data.size) {
            throw IndexOutOfBoundsException(
                "Read at offset $offset with length $length exceeds data size ${data.size}"
            )
        }
    }
}
