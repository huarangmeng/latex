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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.model.LatexNode

/**
 * 数学类代码命令：\mathord, \mathbin, \mathrel, \mathopen, \mathclose,
 * \mathpunct, \mathinner
 *
 * 这些控制序列把参数子公式强制归入指定的 TeX 原子类，从而改变它与相邻
 * 原子之间的数学间距（参见 TeXbook 第 18 章）。
 *
 * 注意：`\mathop` 由 [installOperatorHandlers] 处理为 [LatexNode.BigOperator]，
 * 以保留大型运算符的上下标（limits）渲染行为。
 */
internal fun CommandRegistry.installMathClassHandlers() {
    val classCommandMapping = mapOf(
        "mathord" to LatexNode.MathClass.AtomClass.ORD,
        "mathbin" to LatexNode.MathClass.AtomClass.BIN,
        "mathrel" to LatexNode.MathClass.AtomClass.REL,
        "mathopen" to LatexNode.MathClass.AtomClass.OPEN,
        "mathclose" to LatexNode.MathClass.AtomClass.CLOSE,
        "mathpunct" to LatexNode.MathClass.AtomClass.PUNCT,
        "mathinner" to LatexNode.MathClass.AtomClass.INNER,
    )

    for ((cmd, atomClass) in classCommandMapping) {
        register(cmd) { _, ctx, _ ->
            val content = ctx.parseArgument()
            LatexNode.MathClass(
                if (content != null) listOf(content) else emptyList(),
                atomClass
            )
        }
    }
}
