package com.hrm.latex.renderer.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Space.SpaceType
import com.hrm.latex.renderer.model.RenderStyle

/**
 * 分割多行内容
 */
fun splitLines(nodes: List<LatexNode>): List<List<LatexNode>> {
    val result = mutableListOf<MutableList<LatexNode>>()
    var current = mutableListOf<LatexNode>()
    nodes.forEach { node ->
        if (node is LatexNode.NewLine) {
            result.add(current)
            current = mutableListOf()
        } else {
            current.add(node)
        }
    }
    if (current.isNotEmpty() || result.isEmpty()) result.add(current)
    return result
}

/**
 * 计算行间距
 */
fun lineSpacingPx(style: RenderStyle, density: Density): Float =
    with(density) { (style.fontSize * 0.25f).toPx() }

/**
 * 计算空白宽度
 */
fun spaceWidthPx(style: RenderStyle, type: SpaceType, density: Density): Float {
    val factor = when (type) {
        SpaceType.THIN -> 0.166f
        SpaceType.MEDIUM -> 0.222f
        SpaceType.THICK -> 0.277f
        SpaceType.QUAD -> 1f
        SpaceType.QQUAD -> 2f
        SpaceType.NORMAL -> 0.25f
    }
    return with(density) { (style.fontSize * factor).toPx() }
}

/**
 * 大型运算符符号映射
 */
fun mapBigOp(op: String): String {
    val name = op.trim()
    return when (name) {
        "sum" -> "∑"
        "prod" -> "∏"
        "coprod" -> "∐"
        "int" -> "∫"
        "oint" -> "∮"
        "iint" -> "∬"
        "iiint" -> "∭"
        "bigcap" -> "⋂"
        "bigcup" -> "⋃"
        "bigsqcup" -> "⨆"
        "bigvee" -> "⋁"
        "bigwedge" -> "⋀"
        "bigoplus" -> "⨁"
        "bigotimes" -> "⨂"
        "biguplus" -> "⨄"
        else -> name
    }
}

/**
 * 解析颜色字符串
 */
fun parseColor(color: String): Color? {
    val trimmed = color.trim().removePrefix("#").lowercase()
    if (trimmed.isEmpty()) return null
    return when (trimmed.length) {
        6 -> runCatching { trimmed.toLong(16) }.getOrNull()
            ?.let { Color((0xFF000000L or it).toULong()) }

        8 -> runCatching { trimmed.toLong(16) }.getOrNull()?.let { Color(it.toULong()) }
        else -> when (trimmed) {
            "red" -> Color.Red
            "blue" -> Color.Blue
            "green" -> Color.Green
            "black" -> Color.Black
            "white" -> Color.White
            "gray", "grey" -> Color.Gray
            else -> null
        }
    }
}

// 括号绘制相关

enum class Side { LEFT, RIGHT }

/**
 * 绘制各种类型的括号/定界符
 */
fun DrawScope.drawBracket(
    type: LatexNode.Matrix.MatrixType,
    side: Side,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    stroke: Float,
    color: Color
) {
    val path = Path()
    when (type) {
        LatexNode.Matrix.MatrixType.PAREN -> {
            // Curve
            val x0 = if (side == Side.LEFT) x + width else x
            val x1 = if (side == Side.LEFT) x else x + width
            path.moveTo(x0, y)
            path.quadraticBezierTo(x1, y + height / 2, x0, y + height)
        }

        LatexNode.Matrix.MatrixType.BRACKET -> {
            val x0 = if (side == Side.LEFT) x + width else x
            val x1 = if (side == Side.LEFT) x + stroke else x + width - stroke
            path.moveTo(x0, y)
            path.lineTo(x1, y)
            path.lineTo(x1, y + height)
            path.lineTo(x0, y + height)
        }

        LatexNode.Matrix.MatrixType.BRACE -> {
            val x0 = if (side == Side.LEFT) x + width else x
            val xMid = if (side == Side.LEFT) x else x + width
            path.moveTo(x0, y)
            path.quadraticBezierTo(x0, y + height * 0.25f, xMid, y + height * 0.5f)
            path.quadraticBezierTo(x0, y + height * 0.75f, x0, y + height)
        }

        LatexNode.Matrix.MatrixType.VBAR -> {
            val mx = x + width / 2
            path.moveTo(mx, y)
            path.lineTo(mx, y + height)
        }

        LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> {
            val mx1 = x + width / 3
            val mx2 = x + width * 2 / 3
            path.moveTo(mx1, y); path.lineTo(mx1, y + height)
            path.moveTo(mx2, y); path.lineTo(mx2, y + height)
        }

        LatexNode.Matrix.MatrixType.PLAIN -> {}
    }
    drawPath(path, color, style = Stroke(stroke))
}
