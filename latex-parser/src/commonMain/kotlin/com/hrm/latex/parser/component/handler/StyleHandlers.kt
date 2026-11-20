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
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 字体样式 & 数学模式切换命令
 */
internal fun CommandRegistry.installStyleHandlers() {
    // 字体样式
    val styleCommandMapping = mapOf(
        "mathbf" to LatexNode.Style.StyleType.BOLD,
        "bold" to LatexNode.Style.StyleType.BOLD,
        "textbf" to LatexNode.Style.StyleType.TEXT_BOLD,
        "textmd" to LatexNode.Style.StyleType.TEXT_MEDIUM,
        "boldsymbol" to LatexNode.Style.StyleType.BOLD_SYMBOL,
        "bm" to LatexNode.Style.StyleType.BOLD_SYMBOL,
        "mathit" to LatexNode.Style.StyleType.ITALIC,
        "mathnormal" to LatexNode.Style.StyleType.MATH_NORMAL,
        "textit" to LatexNode.Style.StyleType.TEXT_ITALIC,
        "textsl" to LatexNode.Style.StyleType.TEXT_SLANTED,
        "emph" to LatexNode.Style.StyleType.TEXT_EMPHASIS,
        // The bundled font set uses the NFSS fallbacks for swash and spaced small caps.
        "textsw" to LatexNode.Style.StyleType.TEXT_ITALIC,
        "textssc" to LatexNode.Style.StyleType.SMALL_CAPS,
        "mathrm" to LatexNode.Style.StyleType.ROMAN,
        "textrm" to LatexNode.Style.StyleType.TEXT_ROMAN,
        "textnormal" to LatexNode.Style.StyleType.TEXT_NORMAL,
        "textup" to LatexNode.Style.StyleType.TEXT_UPRIGHT,
        "mathsf" to LatexNode.Style.StyleType.SANS_SERIF,
        "textsf" to LatexNode.Style.StyleType.TEXT_SANS_SERIF,
        "mathtt" to LatexNode.Style.StyleType.MONOSPACE,
        "texttt" to LatexNode.Style.StyleType.TEXT_MONOSPACE,
        "mathbb" to LatexNode.Style.StyleType.BLACKBOARD_BOLD,
        // AMSFonts legacy alias: \Bbb{R} / \Bbb R
        "Bbb" to LatexNode.Style.StyleType.BLACKBOARD_BOLD,
        "mathfrak" to LatexNode.Style.StyleType.FRAKTUR,
        "mathscr" to LatexNode.Style.StyleType.SCRIPT,
        "mathcal" to LatexNode.Style.StyleType.CALLIGRAPHIC,
        "textsc" to LatexNode.Style.StyleType.SMALL_CAPS,
        "textulc" to LatexNode.Style.StyleType.TEXT_UPPER_LOWER_CASE,
        // unicode-math
        "symbf" to LatexNode.Style.StyleType.BOLD_SYMBOL,
        "symit" to LatexNode.Style.StyleType.ITALIC,
        "symsf" to LatexNode.Style.StyleType.SANS_SERIF,
        "symrm" to LatexNode.Style.StyleType.ROMAN,
    )

    for ((cmd, styleType) in styleCommandMapping) {
        register(cmd) { _, ctx, _ ->
            val content = if (cmd.startsWith("text") || cmd == "emph") ctx.parseTextArgument() else ctx.parseArgument()
            LatexNode.Style(
                if (content != null) listOf(content) else emptyList(),
                styleType
            )
        }
    }

    // 常用数集简写与 \mathbb 共用黑板粗体样式，仍可被文档中的宏定义覆盖。
    val blackboardLetters = mapOf(
        "N" to "N", "natnums" to "N", "Z" to "Z",
        "R" to "R", "Reals" to "R", "reals" to "R",
        "Complex" to "C", "cnums" to "C",
    )
    register(*blackboardLetters.keys.toTypedArray()) { name, _, _ ->
        LatexNode.Style(listOf(LatexNode.Text(blackboardLetters.getValue(name))), LatexNode.Style.StyleType.BLACKBOARD_BOLD)
    }

    register("TextOrMath") { _, ctx, stream ->
        // 按当前模式处理所选参数；另一分支只读取 token，避免展开宏或执行定义。
        if (ctx.inTextMode) {
            val text = ctx.parseArgument()
            stream.readArgumentTokens()
            text ?: LatexNode.Text("")
        } else {
            stream.readArgumentTokens()
            ctx.parseArgument() ?: LatexNode.Text("")
        }
    }

    register("copyright", "textcopyright") { _, _, _ -> LatexNode.TextMode("©") }

    val styleDeclarationMapping = mapOf(
        "bf" to LatexNode.Style.StyleType.LEGACY_BOLD,
        "it" to LatexNode.Style.StyleType.LEGACY_ITALIC,
        "rm" to LatexNode.Style.StyleType.LEGACY_ROMAN,
        "sf" to LatexNode.Style.StyleType.LEGACY_SANS_SERIF,
        "tt" to LatexNode.Style.StyleType.LEGACY_MONOSPACE,
        "cal" to LatexNode.Style.StyleType.CALLIGRAPHIC,
        "frak" to LatexNode.Style.StyleType.FRAKTUR,
        "scr" to LatexNode.Style.StyleType.SCRIPT,
        "sc" to LatexNode.Style.StyleType.LEGACY_SMALL_CAPS,
        "sl" to LatexNode.Style.StyleType.LEGACY_SLANTED,
        "em" to LatexNode.Style.StyleType.TEXT_EMPHASIS,
        "swshape" to LatexNode.Style.StyleType.TEXT_ITALIC,
        "sscshape" to LatexNode.Style.StyleType.SMALL_CAPS,
        "rmfamily" to LatexNode.Style.StyleType.TEXT_ROMAN,
        "sffamily" to LatexNode.Style.StyleType.TEXT_SANS_SERIF,
        "ttfamily" to LatexNode.Style.StyleType.TEXT_MONOSPACE,
        "bfseries" to LatexNode.Style.StyleType.TEXT_BOLD,
        "mdseries" to LatexNode.Style.StyleType.TEXT_MEDIUM,
        "itshape" to LatexNode.Style.StyleType.TEXT_ITALIC,
        "slshape" to LatexNode.Style.StyleType.TEXT_SLANTED,
        "upshape" to LatexNode.Style.StyleType.TEXT_UPRIGHT,
        "scshape" to LatexNode.Style.StyleType.SMALL_CAPS,
        "ulcshape" to LatexNode.Style.StyleType.TEXT_UPPER_LOWER_CASE,
        "normalshape" to LatexNode.Style.StyleType.TEXT_NORMAL_SHAPE,
        "normalfont" to LatexNode.Style.StyleType.TEXT_NORMAL,
    )

    for ((cmd, styleType) in styleDeclarationMapping) {
        register(cmd) { _, _, _ ->
            LatexNode.Style(emptyList(), styleType)
        }
    }

    register("mathchoice") { _, ctx, _ ->
        val branches = List(4) { ctx.parseArgument() ?: LatexNode.Group(emptyList()) }
        LatexNode.MathChoice(branches[0], branches[1], branches[2], branches[3])
    }

    // 数学模式切换
    val mathStyleMapping = mapOf(
        "displaystyle" to LatexNode.MathStyle.MathStyleType.DISPLAY,
        "textstyle" to LatexNode.MathStyle.MathStyleType.TEXT,
        "scriptstyle" to LatexNode.MathStyle.MathStyleType.SCRIPT,
        "scriptscriptstyle" to LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT,
        "crampeddisplaystyle" to LatexNode.MathStyle.MathStyleType.CRAMPED_DISPLAY,
        "crampedtextstyle" to LatexNode.MathStyle.MathStyleType.CRAMPED_TEXT,
        "crampedscriptstyle" to LatexNode.MathStyle.MathStyleType.CRAMPED_SCRIPT,
        "crampedscriptscriptstyle" to LatexNode.MathStyle.MathStyleType.CRAMPED_SCRIPT_SCRIPT,
    )

    for ((cmd, mathStyleType) in mathStyleMapping) {
        register(cmd) { _, _, _ ->
            LatexNode.MathStyle(emptyList(), mathStyleType)
        }
    }

    register("cramped") { _, ctx, stream ->
        val style = if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance()
            val option = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
            if (stream.peek() is LatexToken.RightBracket) stream.advance()
            when ((option.singleOrNull() as? LatexNode.MathStyle)?.mathStyleType) {
                LatexNode.MathStyle.MathStyleType.DISPLAY -> LatexNode.MathStyle.MathStyleType.CRAMPED_DISPLAY
                LatexNode.MathStyle.MathStyleType.TEXT -> LatexNode.MathStyle.MathStyleType.CRAMPED_TEXT
                LatexNode.MathStyle.MathStyleType.SCRIPT -> LatexNode.MathStyle.MathStyleType.CRAMPED_SCRIPT
                LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> LatexNode.MathStyle.MathStyleType.CRAMPED_SCRIPT_SCRIPT
                else -> LatexNode.MathStyle.MathStyleType.CRAMPED
            }
        } else {
            LatexNode.MathStyle.MathStyleType.CRAMPED
        }
        val content = ctx.parseArgument()
        LatexNode.MathStyle(if (content != null) listOf(content) else emptyList(), style)
    }

    val fontSizeMapping = mapOf(
        "tiny" to LatexNode.FontSize.SizeType.TINY,
        "sixptsize" to LatexNode.FontSize.SizeType.SIX_POINT,
        "scriptsize" to LatexNode.FontSize.SizeType.SCRIPT_SIZE,
        "footnotesize" to LatexNode.FontSize.SizeType.FOOTNOTE_SIZE,
        "small" to LatexNode.FontSize.SizeType.SMALL,
        "normalsize" to LatexNode.FontSize.SizeType.NORMAL_SIZE,
        "large" to LatexNode.FontSize.SizeType.LARGE,
        "Large" to LatexNode.FontSize.SizeType.LARGE_2,
        "LARGE" to LatexNode.FontSize.SizeType.LARGE_3,
        "huge" to LatexNode.FontSize.SizeType.HUGE,
        "Huge" to LatexNode.FontSize.SizeType.HUGE_2,
    )

    for ((cmd, sizeType) in fontSizeMapping) {
        register(cmd) { _, _, _ ->
            LatexNode.FontSize(emptyList(), sizeType)
        }
    }

    register("TeX", "LaTeX", "KaTeX") { name, _, stream ->
        // 标志仍由普通文本字形构成，按字偶调整间距并升降 A、E。
        stream.raw { stream.skipWhitespace() }
        val tex = listOf(
            LatexNode.TextMode("T"), LatexNode.HSpace("-.1667em"),
            LatexNode.Layout(LatexNode.Layout.LayoutType.RAISE_BOX, listOf(LatexNode.TextMode("E")), shift = "-.5ex"),
            LatexNode.HSpace("-.125em"), LatexNode.TextMode("X"),
        )
        val prefix = if (name == "TeX") emptyList() else listOf(
            LatexNode.TextMode(name.take(1)), LatexNode.HSpace(if (name == "LaTeX") "-.36em" else "-.17em"),
            LatexNode.Layout(LatexNode.Layout.LayoutType.RAISE_BOX,
                listOf(LatexNode.MathStyle(listOf(LatexNode.TextMode("A")), LatexNode.MathStyle.MathStyleType.SCRIPT)), shift = ".2em"),
            LatexNode.HSpace("-.15em"),
        )
        LatexNode.Style(prefix + tex, LatexNode.Style.StyleType.TEXT_ROMAN)
    }

    val textSymbols = mapOf(
        "AA" to "Å", "aa" to "å", "AE" to "Æ", "ae" to "æ",
        "OE" to "Œ", "oe" to "œ", "O" to "Ø", "o" to "ø", "ss" to "ß",
        "i" to "ı", "j" to "ȷ", "l" to "ł", "L" to "Ł",
        "lq" to "‘", "rq" to "’", "dag" to "†", "ddag" to "‡",
        "textdegree" to "°", "textemdash" to "—", "textendash" to "–",
        "textbackslash" to "\\", "textasciitilde" to "~", "textasciicircum" to "^",
        "textbraceleft" to "{", "textbraceright" to "}", "textbar" to "|",
        "textbardbl" to "‖", "textunderscore" to "_", "textdollar" to "$",
        "textless" to "<", "textgreater" to ">", "textellipsis" to "…",
        "textquoteleft" to "‘", "textquoteright" to "’",
        "textquotedblleft" to "“", "textquotedblright" to "”",
        "textdagger" to "†", "textdaggerdbl" to "‡", "textregistered" to "®", "textsterling" to "£"
    )
    for ((name, text) in textSymbols) {
        register(name) { _, _, stream ->
            // Ordinary whitespace terminates a control word. Explicit spaces remain separate tokens.
            stream.skipControlWordWhitespace()
            LatexNode.TextMode(text)
        }
    }

    for (accent in LatexNode.TextAccent.Kind.entries) {
        register(accent.command) { _, ctx, _ ->
            val argument = ctx.parseTextArgument() ?: LatexNode.Text("")
            accentTextContent(asTextContent(argument), accent)
        }
    }

    // Preserve structured text, converting only text-mode leaves; inline math keeps math semantics.
    register("text", "mbox", "hbox") { _, ctx, _ ->
        val argument = ctx.parseTextArgument() ?: LatexNode.Text("")
        val content = (if (argument is LatexNode.Group) argument.children else listOf(argument)).map(::asTextContent)
        val plain = content.all { it is LatexNode.TextMode && it.content.isEmpty() }
        if (plain) LatexNode.TextMode(content.joinToString("") { (it as LatexNode.TextMode).text })
        else LatexNode.TextMode("", content = content)
    }
}

/** TeX text accents attach to the first character; a tie spans its two-letter argument. */
private fun accentTextContent(node: LatexNode, accent: LatexNode.TextAccent.Kind): LatexNode {
    if (accent == LatexNode.TextAccent.Kind.TIE || node is LatexNode.TextAccent) {
        return LatexNode.TextAccent(node, accent, node.sourceRange)
    }
    if (node is LatexNode.TextMode && node.content.isEmpty() && node.text.length > 1) {
        val size = if (node.text[0].isHighSurrogate() && node.text[1].isLowSurrogate()) 2 else 1
        val firstRange = node.sourceRange?.let { it.copy(end = minOf(it.end, it.start + size)) }
        val restRange = node.sourceRange?.let { it.copy(start = minOf(it.end, it.start + size)) }
        return LatexNode.Group(listOf(
            LatexNode.TextAccent(node.copy(text = node.text.take(size), sourceRange = firstRange), accent, firstRange),
            node.copy(text = node.text.drop(size), sourceRange = restRange)
        ), node.sourceRange)
    }
    val children = node.children()
    if (children.isNotEmpty()) {
        return node.withChildren(listOf(accentTextContent(children.first(), accent)) + children.drop(1))
    }
    return LatexNode.TextAccent(node, accent, node.sourceRange)
}

private fun asTextContent(node: LatexNode): LatexNode = when (node) {
    is LatexNode.Superscript -> {
        val exponent = node.exponent as? LatexNode.Group
        if (exponent != null && exponent.children.all { it is LatexNode.Symbol && it.symbol == "prime" }) {
            LatexNode.TextMode(ParseUtils.extractText(listOf(node)), node.sourceRange)
        } else node.withChildren(node.children().map(::asTextContent))
    }
    is LatexNode.Text -> LatexNode.TextMode(node.content, node.sourceRange)
    is LatexNode.Space -> LatexNode.TextMode(" ", node.sourceRange)
    is LatexNode.Symbol -> LatexNode.TextMode(if (node.symbol == "prime") "'" else node.unicode, node.sourceRange)
    is LatexNode.InlineMath, is LatexNode.DisplayMath, is LatexNode.TextMode -> node
    else -> node.withChildren(node.children().map(::asTextContent))
}
