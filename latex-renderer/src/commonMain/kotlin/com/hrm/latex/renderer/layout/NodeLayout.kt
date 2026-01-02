package com.hrm.latex.renderer.layout

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 节点布局结果
 * @property width 布局宽度
 * @property height 布局高度
 * @property baseline 基线距离顶部的距离
 * @property draw 绘制回调函数
 */
data class NodeLayout(
    val width: Float,
    val height: Float,
    val baseline: Float, // Distance from top to baseline
    val draw: DrawScope.(x: Float, y: Float) -> Unit
)
