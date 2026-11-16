package com.hrm.latex.renderer.layout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.utils.MathSpacing
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineRowMetricsTest {
    @Test
    fun everySubrowMatchesIndependentTexSpacingAndWidthScan() {
        val fixtures = listOf(
            "x + y - z",
            "space + x + y hspace",
            "x space + hspace y - z",
            "x + space y - hspace z",
            "x + = y - ) z",
            "( + x - ) + y",
            "x , + y = - z",
            "x + hspace space y - space hspace z",
            "x + y = sin - z",
            "space hspace space",
            "+ x -",
            "( x + y ) = z",
        )
        for (fixture in fixtures) {
            val nodes = fixture.split(' ').map { token ->
                when (token) {
                    "space" -> LatexNode.Space(LatexNode.Space.SpaceType.THIN)
                    "hspace" -> LatexNode.HSpace("0.3em")
                    "sin" -> LatexNode.Operator("sin")
                    else -> LatexNode.Text(token)
                }
            }
            // 使用任意但不同的尺寸，无需字体或平台文本测量；只验证行度量的组合语义。
            val layouts = nodes.mapIndexed { index, _ ->
                NodeLayout(width = 7.25f + index * 0.75f, height = 12f, baseline = 9f) { _, _ -> }
            }
            for (style in MathStyle.entries) {
                for (density in listOf(Density(1f), Density(2.5f, 1.3f))) {
                    val context = RenderContext(fontSize = 18.sp, color = Color.Black, mathStyle = style)
                    val metrics = InlineRowMetrics(nodes, layouts, context, density)
                    val fontSizePx = with(density) { context.fontSize.toPx() }
                    val isScript = style == MathStyle.SCRIPT || style == MathStyle.SCRIPT_SCRIPT
                    for (start in 0..nodes.size) {
                        for (end in start..nodes.size) {
                            val row = nodes.subList(start, end)
                            var expectedWidth = layouts.subList(start, end).sumOf { it.width.toDouble() }
                            for (index in 0 until row.size - 1) {
                                // 每个候选子行独立调用原始间距规则，不复刻前缀和或边界修正算法。
                                val right = row[index + 1]
                                val leftIndex = (index downTo 0).firstOrNull { !MathSpacing.isNonAtom(row[it]) }
                                val expectedSpacing = if (leftIndex == null || MathSpacing.isNonAtom(right)) 0f else MathSpacing.spaceBetween(
                                    MathSpacing.effectiveAtomType(row, leftIndex),
                                    MathSpacing.effectiveAtomType(row, index + 1),
                                    isScript,
                                ) * fontSizePx
                                expectedWidth += expectedSpacing
                                assertEquals(
                                    expectedSpacing,
                                    metrics.spacing(start + index, start, end),
                                    0.001f,
                                    "$fixture [$start,$end) edge=$index style=$style",
                                )
                            }
                            assertEquals(
                                expectedWidth.toFloat(),
                                metrics.width(start, end),
                                0.001f,
                                "$fixture [$start,$end) style=$style density=${density.density}/${density.fontScale}",
                            )
                        }
                    }
                }
            }
        }
    }
}
