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
 * 特殊效果命令：\boxed, \phantom, \smash, \vphantom, \hphantom, \not
 */
fun CommandRegistry.installSpecialEffectHandlers() {
    register("boxed") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.Boxed(content)
    }

    register("phantom") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.Phantom(content)
    }

    register("smash") { _, ctx, stream ->
        val smashType = if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance() // consume [
            val typeStr = StringBuilder()
            while (!stream.isEOF() && stream.peek() !is LatexToken.RightBracket) {
                val t = stream.peek()
                if (t is LatexToken.Text) {
                    typeStr.append(t.content)
                }
                stream.advance()
            }
            if (stream.peek() is LatexToken.RightBracket) {
                stream.advance() // consume ]
            }
            when (typeStr.toString().trim()) {
                "t" -> LatexNode.Smash.SmashType.TOP
                "b" -> LatexNode.Smash.SmashType.BOTTOM
                else -> LatexNode.Smash.SmashType.BOTH
            }
        } else {
            LatexNode.Smash.SmashType.BOTH
        }

        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.Smash(content, smashType)
    }

    register("vphantom") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.VPhantom(content)
    }

    register("hphantom") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.HPhantom(content)
    }

    // 否定修饰 \not
    register("not") { _, ctx, stream ->
        val next = if (!stream.isEOF()) {
            ctx.parseFactor() ?: LatexNode.Text("")
        } else {
            LatexNode.Text("")
        }
        LatexNode.Negation(next)
    }
}
