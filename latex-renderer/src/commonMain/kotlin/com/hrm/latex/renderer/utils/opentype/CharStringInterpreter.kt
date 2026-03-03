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

import kotlin.math.max
import kotlin.math.min

/**
 * CFF Type 2 CharString 解释器。
 *
 * 将 CFF CharString 字节码解释为抽象的路径指令序列（moveTo/lineTo/curveTo/closePath）。
 * 输出的坐标在字体设计空间 (design units)，调用方需要按 fontSizePx / unitsPerEm 缩放
 * 并进行 y 轴翻转（字体坐标系 y 向上，Canvas 坐标系 y 向下）。
 *
 * 支持的操作：
 * - 路径构建：rmoveto, rlineto, rrcurveto, hlineto, vlineto, hhcurveto, vvcurveto,
 *   rlinecurveto, rcurveline, hvcurveto, vhcurveto
 * - 子程序调用：callsubr, callgsubr, return
 * - 提示 (hints)：hstem, vstem, hstemhm, vstemhm, hintmask, cntrmask（跳过提示数据）
 * - 结束：endchar
 * - 算术：不实现（blend 等高级指令在数学字体中不常见）
 *
 * 参考：Adobe Technical Note #5177 "The Type 2 Charstring Format"
 */
internal class CharStringInterpreter(
    private val globalSubrs: List<ByteArray>,
    private val localSubrs: List<ByteArray>,
    private val defaultWidthX: Float,
    private val nominalWidthX: Float
) {
    /** 路径指令 */
    sealed class PathCommand {
        data class MoveTo(val x: Float, val y: Float) : PathCommand()
        data class LineTo(val x: Float, val y: Float) : PathCommand()
        data class CurveTo(
            val x1: Float, val y1: Float,
            val x2: Float, val y2: Float,
            val x3: Float, val y3: Float
        ) : PathCommand()
        data object ClosePath : PathCommand()
    }

    /**
     * 字形轮廓数据。
     *
     * @property commands 路径指令列表（设计空间坐标）
     * @property width 字形前进宽度（设计空间单位）
     * @property bounds 字形墨水边界 (minX, minY, maxX, maxY)，设计空间坐标
     */
    class GlyphOutline(
        val commands: List<PathCommand>,
        val width: Float,
        val bounds: GlyphRect
    )

    /**
     * 字形边界矩形。
     *
     * 字体坐标系：y 轴向上为正，原点在基线左端。
     * minY 通常为负值（descent 方向），maxY 通常为正值（ascent 方向）。
     */
    data class GlyphRect(
        val minX: Float, val minY: Float,
        val maxX: Float, val maxY: Float
    ) {
        val width: Float get() = maxX - minX
        val height: Float get() = maxY - minY
    }

    private val stack = FloatArray(48)
    private var stackSize = 0
    private val commands = mutableListOf<PathCommand>()
    private var currentX = 0f
    private var currentY = 0f
    private var glyphWidth: Float = Float.NaN
    private var numHints = 0
    private var hasSeenMoveTo = false

    /** Transient array for put/get operators (Type 2 spec allows up to 32 entries) */
    private val transientArray = FloatArray(32)

    // Bounds tracking
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE

    /**
     * 解释 CharString 字节码，生成路径指令。
     *
     * @param charString CharString 原始字节数据
     * @return 字形轮廓数据，解析失败返回 null
     */
    fun interpret(charString: ByteArray): GlyphOutline? {
        reset()
        return try {
            execute(charString)

            // 如果没有显式关闭最后一个路径，关闭它
            if (hasSeenMoveTo && commands.isNotEmpty() && commands.last() !is PathCommand.ClosePath) {
                commands.add(PathCommand.ClosePath)
            }

            val width = if (glyphWidth.isNaN()) defaultWidthX else glyphWidth

            // 处理空字形（如空格）
            if (minX == Float.MAX_VALUE) {
                return GlyphOutline(commands.toList(), width, GlyphRect(0f, 0f, 0f, 0f))
            }

            GlyphOutline(commands.toList(), width, GlyphRect(minX, minY, maxX, maxY))
        } catch (e: Exception) {
            com.hrm.latex.base.log.HLog.d(
                "CharStringInterpreter",
                "interpret() exception: ${e::class.simpleName}: ${e.message}, " +
                        "charStringLen=${charString.size}, commandsSoFar=${commands.size}"
            )
            null
        }
    }

    private fun reset() {
        stackSize = 0
        commands.clear()
        currentX = 0f
        currentY = 0f
        glyphWidth = Float.NaN
        numHints = 0
        hasSeenMoveTo = false
        transientArray.fill(0f)
        minX = Float.MAX_VALUE
        minY = Float.MAX_VALUE
        maxX = Float.MIN_VALUE
        maxY = Float.MIN_VALUE
    }

    private fun execute(charStringData: ByteArray) {
        var i = 0
        while (i < charStringData.size) {
            val b0 = charStringData[i].toInt() and 0xFF
            when {
                // ── 操作符 ──
                b0 == 1 || b0 == 3 -> { // hstem, vstem
                    handleHintOperator()
                    i++
                }
                b0 == 4 -> { // vmoveto
                    handleVMoveTo()
                    i++
                }
                b0 == 5 -> { // rlineto
                    handleRLineTo()
                    i++
                }
                b0 == 6 -> { // hlineto
                    handleHLineTo()
                    i++
                }
                b0 == 7 -> { // vlineto
                    handleVLineTo()
                    i++
                }
                b0 == 8 -> { // rrcurveto
                    handleRRCurveTo()
                    i++
                }
                b0 == 10 -> { // callsubr
                    val index = popStack().toInt()
                    val biased = biasedIndex(index, localSubrs.size)
                    if (biased in localSubrs.indices) {
                        execute(localSubrs[biased])
                    }
                    i++
                }
                b0 == 11 -> { // return
                    return
                }
                b0 == 14 -> { // endchar
                    if (stackSize > 0 && !hasSeenMoveTo) {
                        // 可能有宽度参数
                        if (glyphWidth.isNaN() && stackSize % 2 == 1) {
                            glyphWidth = popBottom() + nominalWidthX
                        }
                    }
                    if (hasSeenMoveTo) {
                        commands.add(PathCommand.ClosePath)
                    }
                    return
                }
                b0 == 18 || b0 == 23 -> { // hstemhm, vstemhm
                    handleHintOperator()
                    i++
                }
                b0 == 19 || b0 == 20 -> { // hintmask, cntrmask
                    handleHintOperator()
                    // 跳过 hintmask/cntrmask 的数据字节
                    val maskBytes = (numHints + 7) / 8
                    i += 1 + maskBytes
                }
                b0 == 21 -> { // rmoveto
                    handleRMoveTo()
                    i++
                }
                b0 == 22 -> { // hmoveto
                    handleHMoveTo()
                    i++
                }
                b0 == 24 -> { // rcurveline
                    handleRCurveLine()
                    i++
                }
                b0 == 25 -> { // rlinecurve
                    handleRLineCurve()
                    i++
                }
                b0 == 26 -> { // vvcurveto
                    handleVVCurveTo()
                    i++
                }
                b0 == 27 -> { // hhcurveto
                    handleHHCurveTo()
                    i++
                }
                b0 == 29 -> { // callgsubr
                    val index = popStack().toInt()
                    val biased = biasedIndex(index, globalSubrs.size)
                    if (biased in globalSubrs.indices) {
                        execute(globalSubrs[biased])
                    }
                    i++
                }
                b0 == 30 -> { // vhcurveto
                    handleVHCurveTo()
                    i++
                }
                b0 == 31 -> { // hvcurveto
                    handleHVCurveTo()
                    i++
                }
                b0 == 12 -> { // Two-byte operators (escape)
                    i++
                    if (i < charStringData.size) {
                        val b1 = charStringData[i].toInt() and 0xFF
                        handleEscapedOperator(b1)
                    }
                    i++
                }
                // ── 数值操作数 ──
                b0 == 28 -> {
                    if (i + 2 < charStringData.size) {
                        val b1 = charStringData[i + 1].toInt() and 0xFF
                        val b2 = charStringData[i + 2].toInt() and 0xFF
                        val value = (b1 shl 8) or b2
                        pushStack(
                            (if (value >= 0x8000) value - 0x10000 else value).toFloat()
                        )
                        i += 3
                    } else i++
                }
                b0 in 32..246 -> {
                    pushStack((b0 - 139).toFloat())
                    i++
                }
                b0 in 247..250 -> {
                    if (i + 1 < charStringData.size) {
                        val b1 = charStringData[i + 1].toInt() and 0xFF
                        pushStack(((b0 - 247) * 256 + b1 + 108).toFloat())
                        i += 2
                    } else i++
                }
                b0 in 251..254 -> {
                    if (i + 1 < charStringData.size) {
                        val b1 = charStringData[i + 1].toInt() and 0xFF
                        pushStack((-(b0 - 251) * 256 - b1 - 108).toFloat())
                        i += 2
                    } else i++
                }
                b0 == 255 -> {
                    // 16.16 fixed-point number
                    if (i + 4 < charStringData.size) {
                        val b1 = charStringData[i + 1].toInt() and 0xFF
                        val b2 = charStringData[i + 2].toInt() and 0xFF
                        val b3 = charStringData[i + 3].toInt() and 0xFF
                        val b4 = charStringData[i + 4].toInt() and 0xFF
                        val intPart = (b1 shl 8) or b2
                        val fracPart = (b3 shl 8) or b4
                        val signedInt = if (intPart >= 0x8000) intPart - 0x10000 else intPart
                        pushStack(signedInt + fracPart / 65536f)
                        i += 5
                    } else i++
                }
                else -> i++ // 跳过未知
            }
        }
    }

    // ─── 操作符处理 ──────────────────────────────────────────────

    private fun handleHintOperator() {
        // 如果有未消费的宽度参数
        if (glyphWidth.isNaN() && stackSize % 2 == 1) {
            glyphWidth = popBottom() + nominalWidthX
        }
        numHints += stackSize / 2
        stackSize = 0
    }

    private fun handleRMoveTo() {
        if (hasSeenMoveTo) {
            commands.add(PathCommand.ClosePath)
        }
        if (glyphWidth.isNaN() && stackSize > 2) {
            glyphWidth = popBottom() + nominalWidthX
        }
        val dy = popStack()
        val dx = popStack()
        currentX += dx
        currentY += dy
        commands.add(PathCommand.MoveTo(currentX, currentY))
        updateBounds(currentX, currentY)
        hasSeenMoveTo = true
        stackSize = 0
    }

    private fun handleHMoveTo() {
        if (hasSeenMoveTo) {
            commands.add(PathCommand.ClosePath)
        }
        if (glyphWidth.isNaN() && stackSize > 1) {
            glyphWidth = popBottom() + nominalWidthX
        }
        val dx = popStack()
        currentX += dx
        commands.add(PathCommand.MoveTo(currentX, currentY))
        updateBounds(currentX, currentY)
        hasSeenMoveTo = true
        stackSize = 0
    }

    private fun handleVMoveTo() {
        if (hasSeenMoveTo) {
            commands.add(PathCommand.ClosePath)
        }
        if (glyphWidth.isNaN() && stackSize > 1) {
            glyphWidth = popBottom() + nominalWidthX
        }
        val dy = popStack()
        currentY += dy
        commands.add(PathCommand.MoveTo(currentX, currentY))
        updateBounds(currentX, currentY)
        hasSeenMoveTo = true
        stackSize = 0
    }

    private fun handleRLineTo() {
        var idx = 0
        while (idx + 1 < stackSize) {
            currentX += stack[idx]
            currentY += stack[idx + 1]
            commands.add(PathCommand.LineTo(currentX, currentY))
            updateBounds(currentX, currentY)
            idx += 2
        }
        stackSize = 0
    }

    private fun handleHLineTo() {
        var idx = 0
        var isHorizontal = true
        while (idx < stackSize) {
            if (isHorizontal) {
                currentX += stack[idx]
            } else {
                currentY += stack[idx]
            }
            commands.add(PathCommand.LineTo(currentX, currentY))
            updateBounds(currentX, currentY)
            isHorizontal = !isHorizontal
            idx++
        }
        stackSize = 0
    }

    private fun handleVLineTo() {
        var idx = 0
        var isVertical = true
        while (idx < stackSize) {
            if (isVertical) {
                currentY += stack[idx]
            } else {
                currentX += stack[idx]
            }
            commands.add(PathCommand.LineTo(currentX, currentY))
            updateBounds(currentX, currentY)
            isVertical = !isVertical
            idx++
        }
        stackSize = 0
    }

    private fun handleRRCurveTo() {
        var idx = 0
        while (idx + 5 < stackSize) {
            val x1 = currentX + stack[idx]
            val y1 = currentY + stack[idx + 1]
            val x2 = x1 + stack[idx + 2]
            val y2 = y1 + stack[idx + 3]
            val x3 = x2 + stack[idx + 4]
            val y3 = y2 + stack[idx + 5]
            commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
            updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)
            currentX = x3
            currentY = y3
            idx += 6
        }
        stackSize = 0
    }

    private fun handleHHCurveTo() {
        var idx = 0
        var dy1 = 0f
        if (stackSize % 4 == 1) {
            dy1 = stack[idx++]
        }
        while (idx + 3 < stackSize) {
            val x1 = currentX + stack[idx]
            val y1 = currentY + dy1
            val x2 = x1 + stack[idx + 1]
            val y2 = y1 + stack[idx + 2]
            val x3 = x2 + stack[idx + 3]
            val y3 = y2
            commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
            updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)
            currentX = x3
            currentY = y3
            dy1 = 0f
            idx += 4
        }
        stackSize = 0
    }

    private fun handleVVCurveTo() {
        var idx = 0
        var dx1 = 0f
        if (stackSize % 4 == 1) {
            dx1 = stack[idx++]
        }
        while (idx + 3 < stackSize) {
            val x1 = currentX + dx1
            val y1 = currentY + stack[idx]
            val x2 = x1 + stack[idx + 1]
            val y2 = y1 + stack[idx + 2]
            val x3 = x2
            val y3 = y2 + stack[idx + 3]
            commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
            updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)
            currentX = x3
            currentY = y3
            dx1 = 0f
            idx += 4
        }
        stackSize = 0
    }

    private fun handleHVCurveTo() {
        var idx = 0
        while (idx < stackSize) {
            if (idx + 3 < stackSize) {
                // Horizontal start
                val x1 = currentX + stack[idx]
                val y1 = currentY
                val x2 = x1 + stack[idx + 1]
                val y2 = y1 + stack[idx + 2]
                val x3 = x2
                val y3 = y2 + stack[idx + 3]
                // 如果是最后一组且有额外参数
                val finalDx = if (idx + 4 == stackSize - 1) stack[idx + 4] else 0f
                val actualX3 = x3 + finalDx
                commands.add(PathCommand.CurveTo(x1, y1, x2, y2, actualX3, y3))
                updateCurveBounds(currentX, currentY, x1, y1, x2, y2, actualX3, y3)
                currentX = actualX3
                currentY = y3
                idx += if (finalDx != 0f) 5 else 4
            } else break

            if (idx + 3 < stackSize) {
                // Vertical start
                val x1 = currentX
                val y1 = currentY + stack[idx]
                val x2 = x1 + stack[idx + 1]
                val y2 = y1 + stack[idx + 2]
                val y3 = y2
                val x3 = x2 + stack[idx + 3]
                val finalDy = if (idx + 4 == stackSize - 1) stack[idx + 4] else 0f
                val actualY3 = y3 + finalDy
                commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, actualY3))
                updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, actualY3)
                currentX = x3
                currentY = actualY3
                idx += if (finalDy != 0f) 5 else 4
            } else break
        }
        stackSize = 0
    }

    private fun handleVHCurveTo() {
        var idx = 0
        while (idx < stackSize) {
            if (idx + 3 < stackSize) {
                // Vertical start
                val x1 = currentX
                val y1 = currentY + stack[idx]
                val x2 = x1 + stack[idx + 1]
                val y2 = y1 + stack[idx + 2]
                val y3 = y2
                val x3 = x2 + stack[idx + 3]
                val finalDy = if (idx + 4 == stackSize - 1) stack[idx + 4] else 0f
                val actualY3 = y3 + finalDy
                commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, actualY3))
                updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, actualY3)
                currentX = x3
                currentY = actualY3
                idx += if (finalDy != 0f) 5 else 4
            } else break

            if (idx + 3 < stackSize) {
                // Horizontal start
                val x1 = currentX + stack[idx]
                val y1 = currentY
                val x2 = x1 + stack[idx + 1]
                val y2 = y1 + stack[idx + 2]
                val x3 = x2
                val y3 = y2 + stack[idx + 3]
                val finalDx = if (idx + 4 == stackSize - 1) stack[idx + 4] else 0f
                val actualX3 = x3 + finalDx
                commands.add(PathCommand.CurveTo(x1, y1, x2, y2, actualX3, y3))
                updateCurveBounds(currentX, currentY, x1, y1, x2, y2, actualX3, y3)
                currentX = actualX3
                currentY = y3
                idx += if (finalDx != 0f) 5 else 4
            } else break
        }
        stackSize = 0
    }

    private fun handleRCurveLine() {
        // rrcurveto 部分 (stackSize - 2 个参数用于曲线)
        var idx = 0
        val curveArgs = stackSize - 2
        while (idx + 5 < curveArgs) {
            val x1 = currentX + stack[idx]
            val y1 = currentY + stack[idx + 1]
            val x2 = x1 + stack[idx + 2]
            val y2 = y1 + stack[idx + 3]
            val x3 = x2 + stack[idx + 4]
            val y3 = y2 + stack[idx + 5]
            commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
            updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)
            currentX = x3
            currentY = y3
            idx += 6
        }
        // rlineto 部分 (最后 2 个参数)
        if (idx + 1 < stackSize) {
            currentX += stack[idx]
            currentY += stack[idx + 1]
            commands.add(PathCommand.LineTo(currentX, currentY))
            updateBounds(currentX, currentY)
        }
        stackSize = 0
    }

    private fun handleRLineCurve() {
        // rlineto 部分 (stackSize - 6 个参数)
        var idx = 0
        val lineArgs = stackSize - 6
        while (idx + 1 < lineArgs) {
            currentX += stack[idx]
            currentY += stack[idx + 1]
            commands.add(PathCommand.LineTo(currentX, currentY))
            updateBounds(currentX, currentY)
            idx += 2
        }
        // rrcurveto 部分 (最后 6 个参数)
        if (idx + 5 < stackSize) {
            val x1 = currentX + stack[idx]
            val y1 = currentY + stack[idx + 1]
            val x2 = x1 + stack[idx + 2]
            val y2 = y1 + stack[idx + 3]
            val x3 = x2 + stack[idx + 4]
            val y3 = y2 + stack[idx + 5]
            commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
            updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)
            currentX = x3
            currentY = y3
        }
        stackSize = 0
    }

    private fun handleEscapedOperator(b1: Int) {
        when (b1) {
            // ── 算术与逻辑操作符（Type 2 CharString 规范） ──
            0 -> { /* dotsection — deprecated, 无操作 */ stackSize = 0 }
            3 -> { // and: a b → (a != 0 && b != 0) ? 1 : 0
                if (stackSize >= 2) {
                    val b = popStack()
                    val a = popStack()
                    pushStack(if (a != 0f && b != 0f) 1f else 0f)
                }
            }
            4 -> { // or: a b → (a != 0 || b != 0) ? 1 : 0
                if (stackSize >= 2) {
                    val b = popStack()
                    val a = popStack()
                    pushStack(if (a != 0f || b != 0f) 1f else 0f)
                }
            }
            5 -> { // not: a → (a == 0) ? 1 : 0
                if (stackSize >= 1) {
                    val a = popStack()
                    pushStack(if (a == 0f) 1f else 0f)
                }
            }
            9 -> { // abs: a → |a|
                if (stackSize >= 1) {
                    val a = popStack()
                    pushStack(kotlin.math.abs(a))
                }
            }
            10 -> { // add: a b → a + b
                if (stackSize >= 2) {
                    val b = popStack()
                    val a = popStack()
                    pushStack(a + b)
                }
            }
            11 -> { // sub: a b → a - b
                if (stackSize >= 2) {
                    val b = popStack()
                    val a = popStack()
                    pushStack(a - b)
                }
            }
            12 -> { // div: a b → a / b
                if (stackSize >= 2) {
                    val b = popStack()
                    val a = popStack()
                    pushStack(if (b != 0f) a / b else 0f)
                }
            }
            14 -> { // neg: a → -a
                if (stackSize >= 1) {
                    val a = popStack()
                    pushStack(-a)
                }
            }
            15 -> { // eq: a b → (a == b) ? 1 : 0
                if (stackSize >= 2) {
                    val b = popStack()
                    val a = popStack()
                    pushStack(if (a == b) 1f else 0f)
                }
            }
            18 -> { // drop: a → (remove top)
                if (stackSize >= 1) {
                    popStack()
                }
            }
            20 -> { // put: val i → (store val in transient array[i])
                if (stackSize >= 2) {
                    val i = popStack().toInt()
                    val v = popStack()
                    if (i in transientArray.indices) transientArray[i] = v
                }
            }
            21 -> { // get: i → transientArray[i]
                if (stackSize >= 1) {
                    val i = popStack().toInt()
                    pushStack(if (i in transientArray.indices) transientArray[i] else 0f)
                }
            }
            22 -> { // ifelse: s1 s2 v1 v2 → (v1 <= v2) ? s1 : s2
                if (stackSize >= 4) {
                    val v2 = popStack()
                    val v1 = popStack()
                    val s2 = popStack()
                    val s1 = popStack()
                    pushStack(if (v1 <= v2) s1 else s2)
                }
            }
            23 -> { // random: → (pseudo-random 0 < r < 1)
                pushStack(0.5f) // 确定性近似值，字形渲染中几乎不会使用
            }
            26 -> { // sqrt: a → sqrt(a)
                if (stackSize >= 1) {
                    val a = popStack()
                    pushStack(kotlin.math.sqrt(kotlin.math.abs(a)))
                }
            }
            // ── Flex 操作符 ──
            34 -> handleHFlex()
            35 -> handleFlex()
            36 -> handleHFlex1()
            37 -> handleFlex1()
            else -> stackSize = 0 // 真正未知的操作符，清空栈
        }
    }

    // ─── Flex 操作符 ──────────────────────────────────────────────

    private fun handleHFlex() {
        if (stackSize < 7) { stackSize = 0; return }
        val dx1 = stack[0]; val dx2 = stack[1]; val dy2 = stack[2]
        val dx3 = stack[3]; val dx4 = stack[4]; val dx5 = stack[5]
        val dx6 = stack[6]

        val x1 = currentX + dx1; val y1 = currentY
        val x2 = x1 + dx2; val y2 = y1 + dy2
        val x3 = x2 + dx3; val y3 = y2
        commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
        updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)

        val x4 = x3 + dx4; val y4 = y3
        val x5 = x4 + dx5; val y5 = currentY
        val x6 = x5 + dx6; val y6 = currentY
        commands.add(PathCommand.CurveTo(x4, y4, x5, y5, x6, y6))
        updateCurveBounds(x3, y3, x4, y4, x5, y5, x6, y6)

        currentX = x6; currentY = y6
        stackSize = 0
    }

    private fun handleFlex() {
        if (stackSize < 13) { stackSize = 0; return }
        val x1 = currentX + stack[0]; val y1 = currentY + stack[1]
        val x2 = x1 + stack[2]; val y2 = y1 + stack[3]
        val x3 = x2 + stack[4]; val y3 = y2 + stack[5]
        commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
        updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)

        val x4 = x3 + stack[6]; val y4 = y3 + stack[7]
        val x5 = x4 + stack[8]; val y5 = y4 + stack[9]
        val x6 = x5 + stack[10]; val y6 = y5 + stack[11]
        commands.add(PathCommand.CurveTo(x4, y4, x5, y5, x6, y6))
        updateCurveBounds(x3, y3, x4, y4, x5, y5, x6, y6)

        currentX = x6; currentY = y6
        stackSize = 0
    }

    private fun handleHFlex1() {
        if (stackSize < 9) { stackSize = 0; return }
        val x1 = currentX + stack[0]; val y1 = currentY + stack[1]
        val x2 = x1 + stack[2]; val y2 = y1 + stack[3]
        val x3 = x2 + stack[4]; val y3 = y2
        commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
        updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)

        val x4 = x3 + stack[5]; val y4 = y3
        val x5 = x4 + stack[6]; val y5 = y4 + stack[7]
        val x6 = x5 + stack[8]; val y6 = currentY
        commands.add(PathCommand.CurveTo(x4, y4, x5, y5, x6, y6))
        updateCurveBounds(x3, y3, x4, y4, x5, y5, x6, y6)

        currentX = x6; currentY = y6
        stackSize = 0
    }

    private fun handleFlex1() {
        if (stackSize < 11) { stackSize = 0; return }
        val x1 = currentX + stack[0]; val y1 = currentY + stack[1]
        val x2 = x1 + stack[2]; val y2 = y1 + stack[3]
        val x3 = x2 + stack[4]; val y3 = y2 + stack[5]
        val x4 = x3 + stack[6]; val y4 = y3 + stack[7]
        val x5 = x4 + stack[8]; val y5 = y4 + stack[9]
        commands.add(PathCommand.CurveTo(x1, y1, x2, y2, x3, y3))
        updateCurveBounds(currentX, currentY, x1, y1, x2, y2, x3, y3)

        val d = stack[10]
        val dx = x5 - currentX
        val dy = y5 - currentY
        val x6: Float
        val y6: Float
        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
            x6 = x5 + d; y6 = currentY
        } else {
            x6 = currentX; y6 = y5 + d
        }
        commands.add(PathCommand.CurveTo(x4, y4, x5, y5, x6, y6))
        updateCurveBounds(x3, y3, x4, y4, x5, y5, x6, y6)

        currentX = x6; currentY = y6
        stackSize = 0
    }

    // ─── 栈操作 ──────────────────────────────────────────────────

    private fun pushStack(value: Float) {
        if (stackSize < stack.size) {
            stack[stackSize++] = value
        }
    }

    private fun popStack(): Float {
        return if (stackSize > 0) stack[--stackSize] else 0f
    }

    private fun popBottom(): Float {
        if (stackSize <= 0) return 0f
        val value = stack[0]
        // 将剩余元素前移
        for (i in 0 until stackSize - 1) {
            stack[i] = stack[i + 1]
        }
        stackSize--
        return value
    }

    // ─── 边界追踪 ──────────────────────────────────────────────

    private fun updateBounds(x: Float, y: Float) {
        minX = min(minX, x)
        minY = min(minY, y)
        maxX = max(maxX, x)
        maxY = max(maxY, y)
    }

    private fun updateCurveBounds(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float
    ) {
        // 保守估计：取控制点的包围盒
        updateBounds(x0, y0)
        updateBounds(x1, y1)
        updateBounds(x2, y2)
        updateBounds(x3, y3)
    }

    companion object {
        /**
         * 计算子程序偏置索引。
         *
         * CFF 子程序使用偏置索引方案：
         * - count < 1240: bias = 107
         * - count < 33900: bias = 1131
         * - else: bias = 32768
         */
        fun biasedIndex(index: Int, count: Int): Int {
            val bias = when {
                count < 1240 -> 107
                count < 33900 -> 1131
                else -> 32768
            }
            return index + bias
        }
    }
}
