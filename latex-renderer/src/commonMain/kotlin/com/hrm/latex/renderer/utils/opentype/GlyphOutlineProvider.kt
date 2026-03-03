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

import androidx.compose.ui.graphics.Path

/**
 * 字形轮廓提供者。
 *
 * 从 OTF 字体文件中提取指定 Glyph ID 的矢量轮廓，
 * 转换为 Compose [Path] 对象，可直接用于 Canvas.drawPath() 渲染。
 *
 * 这是解决 OTF MATH 表中 display-size 变体字形无法通过 TextMeasurer 正确渲染的核心方案：
 * TextMeasurer 依赖 cmap 表将 Unicode 映射到 glyph，但 MATH 变体字形通常没有 Unicode 码位，
 * 因此 TextMeasurer 无法访问这些字形。GlyphOutlineProvider 直接通过 Glyph ID 从 CFF 表
 * 读取字形轮廓数据，绕过了 cmap → Unicode 的限制。
 *
 * 使用场景：
 * - 积分符号 ∫ 的 display-size 变体（MATH 表中的变体 glyph 没有 Unicode 映射）
 * - 其他大型运算符 (∑ ∏) 的 display-size 变体
 * - 定界符的连续尺寸变体
 *
 * @param fontBytes OTF 字体文件的完整字节数据
 */
internal class GlyphOutlineProvider(fontBytes: ByteArray) {

    private val reader = BinaryReader(fontBytes)
    private val fontInfo = OpenTypeFontParser.parse(reader)
    val unitsPerEm: Int = fontInfo.unitsPerEm

    /** CFF 表解析结果（懒加载，仅在首次需要时解析） */
    private val cffData: CffParser.CffData? by lazy {
        val cffRecord = fontInfo.tables["CFF "]
        if (cffRecord != null) {
            CffParser(fontBytes, cffRecord.offset).parse()
        } else null
    }

    /** CharString 解释器（依赖 CFF 数据） */
    private val interpreter: CharStringInterpreter? by lazy {
        cffData?.let {
            CharStringInterpreter(
                globalSubrs = it.globalSubrs,
                localSubrs = it.localSubrs,
                defaultWidthX = it.defaultWidthX,
                nominalWidthX = it.nominalWidthX
            )
        }
    }

    /** glyph outline 缓存，避免重复解析同一 glyph */
    private val outlineCache = mutableMapOf<Int, CharStringInterpreter.GlyphOutline?>()

    /**
     * 获取指定 Glyph ID 的轮廓数据（设计空间坐标）。
     *
     * @param glyphId 字体内部的 Glyph ID
     * @return 轮廓数据，不支持或解析失败返回 null
     */
    fun getOutline(glyphId: Int): CharStringInterpreter.GlyphOutline? {
        return outlineCache.getOrPut(glyphId) {
            val cff = cffData ?: return@getOrPut null
            val interp = interpreter ?: return@getOrPut null

            if (glyphId < 0 || glyphId >= cff.charStrings.size) return@getOrPut null

            val charString = cff.charStrings[glyphId]
            if (charString.isEmpty()) return@getOrPut null

            val result = interp.interpret(charString)
            if (result == null) {
                com.hrm.latex.base.log.HLog.d(
                    "GlyphOutlineProvider",
                    "interpret() returned null for glyphId=$glyphId, " +
                            "charStringLen=${charString.size}"
                )
            }
            result
        }
    }

    /**
     * 将字形轮廓转换为 Compose [Path]，以 (0,0) 为原点构建。
     *
     * 坐标变换：
     * - 字体坐标系：y 轴向上，原点在基线左端
     * - Canvas 坐标系：y 轴向下，原点在左上角
     *
     * 输出 Path 的坐标系：
     * - x: 0 → width (从左到右)
     * - y: 0 = 字形墨水顶部, 向下增长
     * - 已完成 y 轴翻转和 offsetY 偏移
     *
     * @param glyphId Glyph ID
     * @param fontSizePx 目标字号（像素）
     * @return 缩放到目标字号的 Path 和尺寸信息，失败返回 null
     */
    fun getPath(glyphId: Int, fontSizePx: Float): GlyphPathData? {
        val outline = getOutline(glyphId) ?: return null
        if (outline.commands.isEmpty()) {
            com.hrm.latex.base.log.HLog.d(
                "GlyphOutlineProvider",
                "getPath: glyphId=$glyphId has empty commands"
            )
            return null
        }

        val scale = fontSizePx / unitsPerEm.toFloat()
        val bounds = outline.bounds

        // 退化边界检查（宽高为零的字形无法渲染）
        if (bounds.width <= 0f || bounds.height <= 0f) {
            com.hrm.latex.base.log.HLog.d(
                "GlyphOutlineProvider",
                "getPath: glyphId=$glyphId has degenerate bounds: " +
                        "w=${bounds.width}, h=${bounds.height}, commands=${outline.commands.size}"
            )
            return null
        }

        // 字体坐标系中：maxY 是最高点（ascent 方向），minY 是最低点（descent 方向）
        // Canvas 坐标系中需要翻转 y 轴：
        // canvasY = -fontY * scale + offsetY
        // 其中 offsetY 确保字形从 y=0 开始（墨水顶部）
        val offsetY = bounds.maxY * scale  // 将字体坐标的 maxY 映射到 canvas 的 y=0

        val path = Path()
        for (cmd in outline.commands) {
            when (cmd) {
                is CharStringInterpreter.PathCommand.MoveTo -> {
                    path.moveTo(cmd.x * scale, offsetY - cmd.y * scale)
                }
                is CharStringInterpreter.PathCommand.LineTo -> {
                    path.lineTo(cmd.x * scale, offsetY - cmd.y * scale)
                }
                is CharStringInterpreter.PathCommand.CurveTo -> {
                    path.cubicTo(
                        cmd.x1 * scale, offsetY - cmd.y1 * scale,
                        cmd.x2 * scale, offsetY - cmd.y2 * scale,
                        cmd.x3 * scale, offsetY - cmd.y3 * scale
                    )
                }
                is CharStringInterpreter.PathCommand.ClosePath -> {
                    path.close()
                }
            }
        }

        // 计算像素空间的尺寸
        val width = bounds.width * scale
        val height = bounds.height * scale
        val advanceWidth = outline.width * scale

        // baseline 在 Canvas 坐标系中的 y 位置
        // 字体坐标系中 baseline = y=0，Canvas 中 baseline = offsetY - 0 * scale = offsetY
        val baselineY = offsetY

        return GlyphPathData(
            path = path,
            width = width,
            height = height,
            advanceWidth = advanceWidth,
            baselineY = baselineY,
            minX = bounds.minX * scale,
            maxX = bounds.maxX * scale,
            ascent = bounds.maxY * scale,   // baseline 以上的高度（正值）
            descent = -bounds.minY * scale  // baseline 以下的高度（正值）
        )
    }

    /**
     * 检查此字体是否支持 glyph outline 提取（即是否包含 CFF 表）。
     */
    val isSupported: Boolean get() = fontInfo.tables.containsKey("CFF ")
}

/**
 * 字形 Path 数据（像素空间）。
 *
 * 所有坐标和尺寸值以像素 (px) 为单位，已完成设计空间→像素的缩放和 y 轴翻转。
 * Path 以 (0,0) 为原点构建（墨水区域左上角），使用时通过 translate(x, y) 偏移。
 *
 * @property path 字形轮廓 Path（以 (0,0) 为原点）
 * @property width 墨水边界宽度
 * @property height 墨水边界高度
 * @property advanceWidth 前进宽度（用于水平排列）
 * @property baselineY baseline 在 Path 坐标系中的 y 位置
 * @property minX 墨水左边界 x 坐标
 * @property maxX 墨水右边界 x 坐标
 * @property ascent baseline 以上的高度（正值）
 * @property descent baseline 以下的高度（正值）
 */
data class GlyphPathData(
    val path: Path,
    val width: Float,
    val height: Float,
    val advanceWidth: Float,
    val baselineY: Float,
    val minX: Float,
    val maxX: Float,
    val ascent: Float,
    val descent: Float
)
